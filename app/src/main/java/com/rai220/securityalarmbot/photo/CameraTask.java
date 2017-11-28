package com.rai220.securityalarmbot.photo;

import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;

/**
 * Задача на съемку фото
 * Created by rai220 on 11/3/16.
 */
public abstract class CameraTask {
    private final int cameraId;
    private final int w;
    private final int h;
    private final boolean isMd;

    public CameraTask(int cameraId) {
        this(cameraId, -1, -1, false);
    }

    public CameraTask(int cameraId, int w, int h) {
        this(cameraId, w, h, false);
    }

    public CameraTask(int cameraId, int w, int h, boolean isMd) {
        this.cameraId = cameraId;
        this.isMd = isMd;

        if (w < 0 || h < 0) {
            Prefs prefs = PrefsController.instance.getPrefs();
            Prefs.CameraPrefs cameraPrefs = prefs.getCameraPrefs(cameraId);
            if (cameraPrefs != null && cameraPrefs.width != 0 && cameraPrefs.height != 0) {
                this.w = cameraPrefs.width;
                this.h = cameraPrefs.height;
            } else {
                this.w = 640;
                this.h = 480;
            }
        } else {
            this.w = w;
            this.h = h;
        }
    }

    public int getCameraId() {
        return cameraId;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    public boolean isMd() {
        return isMd;
    }

    abstract public void processResult(ImageShot shot);
}
