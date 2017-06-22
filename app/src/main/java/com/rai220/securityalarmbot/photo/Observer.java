package com.rai220.securityalarmbot.photo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.telegram.ISenderService;
import com.rai220.securityalarmbot.utils.FabricUtils;
import com.rai220.securityalarmbot.utils.L;

import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 */

public class Observer {

    private ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
    private ScheduledFuture future;
    private HiddenCamera2 hiddenCamera2;
    private ISenderService senderService;
    private final LinkedList<ImageShot> images = new LinkedList<>();
    private final int MAX_QUEUE_SIZE = 1000;

    public Observer() {
    }

    public byte[] getGif() {
        synchronized (images) {
            return BotService.buildGif(images, null);
        }
    }

    public void init(HiddenCamera2 camera, ISenderService service) {
        hiddenCamera2 = camera;
        senderService = service;
    }

    public void start(int minutes) {
        if (isAlive()) {
            stop();
        }
        if (minutes > 0) {
            future = es.scheduleAtFixedRate(new ObserverRunnable(), minutes, minutes, TimeUnit.MINUTES);
        }
    }

    public void stop() {
        if (future != null) {
            future.cancel(false);
        }
    }

    public boolean isAlive() {
        return future != null && !future.isDone();
    }

    private class ObserverRunnable implements Runnable {
        @Override
        public void run() {
            Prefs prefs = PrefsController.instance.getPrefs();
            if (prefs.hasEventListeners()) {
                try {
                    L.i("Observer: started");
                    int[] cameraIds = FabricUtils.getSelectedCameras();
                    for (final int cameraId : cameraIds) {
                        hiddenCamera2.addTask(new CameraTask(cameraId) {
                            @Override
                            public void processResult(ImageShot shot) {
                                if (shot != null) {
                                    senderService.sendPhotoToAll(shot.toYuvByteArray());
                                    synchronized (images) {
                                        images.addLast(shot);
                                        if (images.size() > MAX_QUEUE_SIZE) {
                                            images.removeFirst();
                                        }
                                    }
                                }
                            }
                        }, false);
                    }
                } catch (Throwable ex) {
                    L.e(ex);
                }
            } else {
                L.i("Observer: no listeners!");
            }
        }
    }
}
