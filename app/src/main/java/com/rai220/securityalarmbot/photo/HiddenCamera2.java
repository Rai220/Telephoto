package com.rai220.securityalarmbot.photo;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.FabricUtils;
import com.rai220.securityalarmbot.utils.L;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.rai220.securityalarmbot.photo.HiddenCamera2.CameraState.CAMERA_DESTROYED;
import static com.rai220.securityalarmbot.photo.HiddenCamera2.CameraState.CAMERA_STARTING;
import static com.rai220.securityalarmbot.photo.HiddenCamera2.CameraState.CAMERA_STARTING_ERROR;
import static com.rai220.securityalarmbot.photo.HiddenCamera2.CameraState.IMAGE_GOT;
import static com.rai220.securityalarmbot.photo.HiddenCamera2.CameraState.PLACING_VIEW;
import static com.rai220.securityalarmbot.photo.HiddenCamera2.CameraState.READY;
import static com.rai220.securityalarmbot.photo.HiddenCamera2.CameraState.VIEW_DESTROYED;
import static com.rai220.securityalarmbot.photo.HiddenCamera2.CameraState.VIEW_PLACED;

/**
 * Created by rai220 on 11/3/16.
 */
public class HiddenCamera2 implements Runnable, SurfaceHolder.Callback, Camera.PreviewCallback, Camera.ErrorCallback {
    public enum CameraState {
        READY,
        PLACING_VIEW,
        VIEW_PLACED,
        CAMERA_STARTING,
        IMAGE_GOT,
        CAMERA_DESTROYED,
        VIEW_DESTROYED,
        CAMERA_STARTING_ERROR
    }

    private static final int MAX_CAMERA_TASKS_QUEUE = 10;

    /** Количество кадров, которое мы пропускаем перед тем, как сделать фото */
    //private volatile int FRAMES_TO_SKIP = 5;
    private volatile int FRAMES_TO_SKIP = 7;

    public static final long MD_DELAY = 2500;

    private final BlockingQueue<CameraTask> tasksQueue = new LinkedBlockingQueue<>(MAX_CAMERA_TASKS_QUEUE);
    private volatile Thread cameraProcessingThread = null;
    private final AtomicReference<CameraState> state = new AtomicReference<>(READY);

    private Context context;
    private volatile View view;
    private volatile SurfaceHolder surfaceHolder;
    private volatile WindowManager windowManager;
    private volatile WindowManager.LayoutParams layoutParams;
    private Handler handler;

    private volatile Camera mCamera = null;
    private volatile Camera.Parameters cameraParameters = null;

    private volatile byte[] lastPhoto;

    private final HashMap<Integer, Camera.Parameters> parametersHashMap = new HashMap<>();

    private final ExecutorService es = Executors.newCachedThreadPool();

    /** Количество фреймов, которое мы пропускаем до того, как взять кадр */
    private final AtomicInteger framesSkipped = new AtomicInteger(0);
    private final BotService botService;

    private final AtomicBoolean initializationFinished = new AtomicBoolean(false);

    public HiddenCamera2(BotService botService) {
        this.botService = botService;
    }

    public void init(Context context) {
        initializationFinished.set(false);
        this.context = context;
        handler = new Handler(Looper.getMainLooper());

        state.set(READY);
        this.view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.layout_camera_preview, null);

        surfaceHolder = ((SurfaceView) this.view.findViewById(R.id.surfaceView)).getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams(-2, -2, 2006, 1152, -3);
        layoutParams.gravity = 53;

        cameraProcessingThread = new Thread(this, "HiddenCameraProcessingThread");
        cameraProcessingThread.start();

        final AtomicInteger initCounter = new AtomicInteger(0);
        for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
            final int fCameraId = cameraId;
            addTask(new CameraTask(fCameraId, 1, 1) {
                @Override
                public void processResult(ImageShot shot) {
                    shot.getParameters();
                    parametersHashMap.put(fCameraId, shot.getParameters());
                    L.i("Camera " + fCameraId + " was initialized!");
                    if (initCounter.incrementAndGet() >= Camera.getNumberOfCameras()) {
                        initializationFinished.set(true);
                        L.i("Hidden camera was initialized.");
                    }
                }
            }, true);
        }

        botService.getDetector().start(PrefsController.instance.getPrefs().mdSwitchType);
    }

    public boolean isInitializationFineshed() {
        return initializationFinished.get();
    }

    public void destroy() {
        try {
            tasksQueue.clear();
            FabricUtils.interruptThread(cameraProcessingThread);
            stopCamera(true);
            removeView(true);
            framesSkipped.set(0);
        } catch (Throwable ex) {
            L.e(ex);
        }
    }

    public Camera.Parameters getParameters(int cameraId) {
        return parametersHashMap.get(cameraId);
    }

    public boolean addTask(CameraTask task, boolean initTask) {
        if (!initTask && !isInitializationFineshed()) {
            return false;
        } else {
            return tasksQueue.offer(task);
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                CameraTask task = tasksQueue.take();
                processCameraTask(task);

                synchronized (this) {
                    this.wait(MD_DELAY);
                }
            }
        } catch (InterruptedException ignore) {
        } catch (Throwable ex) {
            L.e(ex);
        }
    }

    private void waitForState(CameraState... states) throws InterruptedException {
        synchronized (this.state) {
            int conter = 0;
            while (!Sets.newHashSet(states).contains(this.state.get())) {
                L.i("Waiting for states: " + Joiner.on(", ").join(states) + ", current state is: " + this.state);
                this.state.wait(5000);
                conter++;

                // Если ждем больше 5 попыток, то делаем перезапоск
                if (conter > 5) {
                    globalCameraRestart();
                }
            }
        }
    }

    private void globalCameraRestart() {
        L.e("Hidden camera hangs!!! Restarting!");
        botService.getTelegramService().sendMessageToAll(context.getString(R.string.error_to_all_camera_hangs));
        handler.post(new Runnable() {
            @Override
            public void run() {
                destroy();
                // Нужно дождаться, чтобы все удалилось (5 сек), потом можно стартовать с нуля
                es.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(5000);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    init(context);
                                }
                            });
                        } catch (InterruptedException ignore) {
                        }
                    }
                });
            }
        });
    }

    private void setState(CameraState state) {
        setState(state, false);
    }

    private void setState(final CameraState state, final boolean force) {
        synchronized (this.state) {
            // State-машина всех допустимых переходов
            if (this.state.get() == state || force
                    || this.state.get() == READY && state == PLACING_VIEW
                    || this.state.get() == PLACING_VIEW && state == VIEW_PLACED
                    || this.state.get() == VIEW_PLACED && state == CAMERA_STARTING
                    || this.state.get() == CAMERA_STARTING && state == IMAGE_GOT
                    || this.state.get() == IMAGE_GOT && state == CAMERA_DESTROYED
                    || this.state.get() == CAMERA_DESTROYED && state == VIEW_DESTROYED
                    || this.state.get() == VIEW_DESTROYED && state == READY
                    || this.state.get() == VIEW_DESTROYED && state == READY
                    || this.state.get() == CAMERA_STARTING && state == CAMERA_STARTING_ERROR
                    || this.state.get() == CAMERA_STARTING_ERROR && state == CAMERA_STARTING
                    || this.state.get() == CAMERA_DESTROYED && state == READY) {
                L.i("Hidden camera state: " + state);
                this.state.set(state);
                this.state.notifyAll();
                // Бессмысленные переходы - не делаем, но и не ошибка.
            } else if (this.state.get() == CAMERA_DESTROYED && state == CAMERA_STARTING_ERROR
                    || this.state.get() == CAMERA_DESTROYED && state == VIEW_PLACED) {
                this.state.notifyAll();
            } else {
                throw new IllegalStateException("Incorrect try to set state! Was: " + this.state + " try to set: " + state);
            }
        }
    }

    private boolean checkState(CameraState state) {
        synchronized (this.state) {
            return state == this.state.get();
        }
    }

    private void processCameraTask(CameraTask task) throws InterruptedException {
        waitForState(READY);
        placeView();
        startRecord(task);
        getImage(task);
        stopCamera(false);
        removeView(false);
        processResult(task);
    }

    private void placeView() throws InterruptedException {
        waitForState(READY);
        handler.post(new Runnable() {
            @Override
            public void run() {
                setState(PLACING_VIEW);
                windowManager.addView(view, layoutParams);
            }
        });
    }

    private void startRecord(final CameraTask cameraTask) throws InterruptedException {
        waitForState(VIEW_PLACED);
        handler.post(new Runnable() {
            @Override
            public void run() {
                int cameraId = cameraTask.getCameraId();
                try {
                    setState(CAMERA_STARTING);
                    framesSkipped.set(0);
                    mCamera = Camera.open(cameraTask.getCameraId());

                    Prefs.CameraPrefs cprefs = PrefsController.instance.getPrefs().getCameraPrefs(cameraId);

                    Camera.Parameters params = mCamera.getParameters();
                    List<Camera.Size> sizes = params.getSupportedPreviewSizes();
                    Camera.Size s = sizes.get(CameraUtils.closest(sizes, cameraTask.getW(), cameraTask.getH()));
                    params.setPreviewSize(s.width, s.height);

                    List<String> supportedFlashModes = params.getSupportedFlashModes();
                    if (supportedFlashModes != null && supportedFlashModes.contains("off")) {
                        params.setFlashMode("off");
                    }
                    if (!cameraTask.isMd()) {
                        if ("On".equals(cprefs.flashMode) &&
                                supportedFlashModes != null && supportedFlashModes.contains("torch")) {
                            params.setFlashMode("torch");
                        }
                    }
                    if (!Strings.isNullOrEmpty(cprefs.wb)) {
                        params.setWhiteBalance(cprefs.wb);
                    }

                    params.setPreviewFormat(ImageFormat.NV21);
                    mCamera.setParameters(params);

                    if (surfaceHolder == null) {
                        throw new IllegalStateException("Surface holder is null!");
                    } else if (surfaceHolder.getSurface() == null) {
                        throw new IllegalStateException("Surface is null!");
                    }
                    mCamera.setPreviewDisplay(surfaceHolder);
                    //mCamera.setOneShotPreviewCallback(HiddenCamera2.this);
                    mCamera.setPreviewCallback(HiddenCamera2.this);
                    mCamera.setErrorCallback(HiddenCamera2.this);

                    mCamera.startPreview();
                } catch (RuntimeException ex) {
                    L.i("Camera creation runtime error for id: " + cameraId);
                    setState(CAMERA_STARTING_ERROR);
                } catch (Throwable ex) {
                    L.e(ex);
                    setState(CAMERA_STARTING_ERROR);
                }
            }
        });
    }

    private volatile int stopCount = 0;

    private void getImage(final CameraTask cameraTask) throws InterruptedException {
        do {
            waitForState(IMAGE_GOT, CAMERA_STARTING_ERROR);
            if (state.get() == CAMERA_STARTING_ERROR) {
                L.i("Restarting camera...");
                stopCamera(true);
                removeView(false);

                stopCount++;
                if (stopCount > 5) {
                    // Если прошло 5 попыток завести камеру, но она не инициализируется, то всё - пипец ей
                    globalCameraRestart();
                } else {
                    // Ничего не отправляет, но возвращат state-машину в исходное состояние
                    processResult(null);
                    placeView();
                    startRecord(cameraTask);
                }
            } else {
                stopCount = 0;
            }
        } while (state.get() != IMAGE_GOT);
    }

    private void stopCamera(final boolean force) throws InterruptedException {
        if (!force) {
            waitForState(IMAGE_GOT);
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mCamera != null) {
                        mCamera.stopPreview();
                        mCamera.setPreviewCallback(null);
                        mCamera.setPreviewDisplay(null);
                        mCamera.release();
                        mCamera = null;
                    }
                } catch (Throwable ex) {
                    L.e(ex);
                }
                setState(CAMERA_DESTROYED, force);
            }
        });
    }

    private void removeView(boolean force) throws InterruptedException {
        if (!force) {
            waitForState(CAMERA_DESTROYED);
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    windowManager.removeView(view);
                } catch (IllegalArgumentException ignore) {
                    setState(VIEW_DESTROYED);
                }
            }
        });
    }

    private void processResult(CameraTask task) throws InterruptedException {
        waitForState(VIEW_DESTROYED);
        if (task != null) {
            ImageShot shot = new ImageShot(lastPhoto, cameraParameters, task.getCameraId());
            try {
                task.processResult(shot);
            } catch (Throwable ex) {
                L.e(ex);
            }
        }
        setState(READY);
    }

    // --------------------------------------------------------------------------------------------
    // UI-callbacks
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        makePauseAndExecuteUI(new Runnable() {
            @Override
            public void run() {
                setState(VIEW_PLACED);
            }
        });
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        makePauseAndExecuteUI(new Runnable() {
            @Override
            public void run() {
                setState(VIEW_DESTROYED);
            }
        });
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (bytes != null && bytes.length > 0 && camera != null && camera.getParameters() != null) {
            if (checkState(CameraState.CAMERA_STARTING)) {
                if (framesSkipped.get() >= FRAMES_TO_SKIP) {
                    cameraParameters = camera.getParameters();
                    lastPhoto = new byte[bytes.length];
                    System.arraycopy(bytes, 0, lastPhoto, 0, bytes.length);
                    setState(IMAGE_GOT);
                } else {
                    framesSkipped.incrementAndGet();
                }
            }
        }
    }

    @Override
    public void onError(int error, Camera camera) {
        L.e("Camera error: " + error);
        makePauseAndExecuteUI(new Runnable() {
            @Override
            public void run() {
                setState(CAMERA_STARTING_ERROR);
            }
        });
    }
    // UI-callbacks
    // --------------------------------------------------------------------------------------------

    private void makePauseAndExecuteUI(final Runnable task) {
        es.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    handler.post(task);
                } catch (Throwable ex) {
                    L.e(ex);
                }
            }
        });
    }
}
