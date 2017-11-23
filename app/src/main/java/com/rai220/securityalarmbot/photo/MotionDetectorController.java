package com.rai220.securityalarmbot.photo;

import android.graphics.Bitmap;

import com.crashlytics.android.Crashlytics;
import com.rai220.securityalarmbot.commands.types.MdSwitchType;
import com.rai220.securityalarmbot.commands.types.SensitivityType;
import com.rai220.securityalarmbot.motiondetector.MotionDetector;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.FabricUtils;
import com.rai220.securityalarmbot.utils.L;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import boofcv.android.VisualizeImageData;
import boofcv.core.encoding.ConvertYV12;
import boofcv.struct.image.GrayU8;

/**
 * Created by rai220 on 10/20/16.
 */
public class MotionDetectorController {
    private static final ExecutorService es = Executors.newCachedThreadPool();

    /** Для быстрых устройств */
    private static final int SHORT_TIMEOUT = 3 * 1000;
    //private static final int IMAGE_HISTORY_SIZE = 5;
    private static final int IMAGE_HISTORY_SIZE = 5;

    private HiddenCamera2 camera;
    private MotionDetector motionDetector = null;

    private volatile Thread motionDetectorThread = null;
    private MotionDetectorListener listener = null;

    /** Предыдущее фото (нужно для отладки! */
    private ImageShot oldShot = null;
    private LinkedList<ImageShot> oldShots = new LinkedList<>();

    private MdSwitchType mdType = null;
    private LinkedList<Long> processingTimes = new LinkedList<>();

    private int[][] matrix = {
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 1, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0}};

    public void init(HiddenCamera2 camera, MotionDetectorListener listener) {
        this.camera = camera;
        this.listener = listener;
        initializeMotionDetector();
    }

    private void initializeMotionDetector() {
        motionDetector = new MotionDetector();
        motionDetector.configure(PrefsController.instance.getPrefs().sensitivity.getMdValue());
    }

    public String setMatrix(int[][] matrix) {
        if (matrix == null || matrix.length < 3 || matrix.length > 10) {
            return "Incorrect matrix size. Please use matrix from 3*3 to 10*10";
        }
        for (int[] row : matrix) {
            if (row.length != matrix.length) {
                return "Incorrect matrix size! Please use square matrix!";
            }
        }
        this.matrix = matrix;
        return "OK";
    }

    public void start(final MdSwitchType mdType) {
        this.mdType = mdType;

        stop();
        if (mdType != MdSwitchType.OFF) {
            PrefsController.instance.updateLastMdDetected();

            oldShot = null;
            oldShots.clear();
            motionDetectorThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        L.i("MD thread started");
                        final BlockingQueue<ImageShot> queue = new LinkedBlockingQueue<>();
                        while (!Thread.currentThread().isInterrupted()) {
                            L.i("MD working...");
                            final int[] cameraIds = FabricUtils.getSelectedCamerasForMd();
                            boolean isOk = false;
                            if (camera != null) {
                                isOk = camera.addTask(
                                        new CameraTask(cameraIds[0], 100, 80, true) {
                                            @Override
                                            public void processResult(ImageShot shot) {
                                                try {
                                                    queue.put(shot);
                                                } catch (Throwable ex) {
                                                    L.e(ex);
                                                }
                                            }
                                        }, false);
                                if (isOk) {
                                    ImageShot shot = queue.take();
                                    isOk = detect(shot);
                                }
                            } else {
                                L.i("Camera is null yet. Let's wait some...");
                            }

                            if (!isOk) {
                                oldShot = null;
                                oldShots.clear();
                                L.i("Camera is busy for md. We will skip one shot.");
                                Thread.sleep(30 * 1000);
                            }
                        }
                    } catch (InterruptedException ignore) {
                    } catch (Throwable ex) {
                        L.e(ex);
                    }
                    L.i("Motion detector thread stopped.");
                }
            }, "Motion detector thread");
            motionDetectorThread.start();
        }
    }

    public MdSwitchType getMdType() {
        return mdType;
    }

    public void stop() {
        FabricUtils.interruptThread(motionDetectorThread);
    }

    public boolean isAlive() {
        return motionDetectorThread != null && motionDetectorThread.isAlive();
    }

    private void saveProcessingTime(long time) {
        L.i("Processing time: " + time);
        processingTimes.add(time);
        if (processingTimes.size() > 10) {
            long res = 0;
            for (Long l : processingTimes) {
                res += l;
            }
            res = res / processingTimes.size();
            processingTimes.clear();
            Crashlytics.setLong("Processing time", res);
        }
    }

    private boolean detect(ImageShot shot) {
        if (shot != null && shot.getParameters() != null && shot.getImage() != null) {
            long start = System.currentTimeMillis();

            byte[] image = shot.getImage();

            int width = shot.getParameters().getPreviewSize().width;
            int height = shot.getParameters().getPreviewSize().height;

            GrayU8 greyImage = new GrayU8(width, height);
            ConvertYV12.yu12ToGray(image, width, height, greyImage);

            GrayU8 segmented = motionDetector.addImage(greyImage, matrix);
            saveProcessingTime(System.currentTimeMillis() - start);

            if (segmented != null && oldShot != null) {
                long timeFromLast = System.currentTimeMillis() - PrefsController.instance.getLastMdDetected();
                // Первое движение за долгое время!
                boolean rareMotion = timeFromLast > 1000 * 60 * 60;
                if (MdSwitchType.ON == mdType || rareMotion) {
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    VisualizeImageData.binaryToBitmap(segmented, false, bitmap, null);

                    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    List<ImageShot> oldShotsCopy = new ArrayList<>();
                    oldShotsCopy.addAll(oldShots);

                    listener.motionDetected(
                            stream.toByteArray(), shot.toYuvByteArray(),
                            oldShot.toYuvByteArray(), rareMotion, oldShotsCopy);
                } else {
                    L.i("Motion detected, but not sent, because time from last motion: " + timeFromLast);
                }
                PrefsController.instance.updateLastMdDetected();
            }
            oldShot = shot;
            oldShots.addLast(shot);
            if (oldShots.size() > IMAGE_HISTORY_SIZE) {
                oldShots.removeFirst();
            }
            return true;
        } else {
            L.i("Motion detector got null photo!");
            oldShot = null;
            oldShots.clear();
            return false;
        }
    }

    public void setSensibility(SensitivityType sensibility) {
        motionDetector.configure(sensibility.getMdValue());
    }

    public interface MotionDetectorListener {
        void motionDetected(byte[] debugPhoto, byte[] realPhoto, byte[] oldRealPhoto, boolean rareMotion, List<ImageShot> oldShots);
    }
}
