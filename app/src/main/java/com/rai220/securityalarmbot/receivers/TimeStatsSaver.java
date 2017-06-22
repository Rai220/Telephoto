package com.rai220.securityalarmbot.receivers;

import com.rai220.securityalarmbot.model.TimeStats;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.L;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 */

public class TimeStatsSaver {
    private ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
    private ScheduledFuture future;

    public TimeStatsSaver() {
    }

    public void start() {
        if (isAlive()) {
            stop();
        }
        future = es.scheduleAtFixedRate(new TimeStatsRunnable(), 5, 30, TimeUnit.MINUTES);
    }

    public void stop() {
        if (future != null) {
            future.cancel(false);
        }
    }

    public boolean isAlive() {
        return future != null && !future.isDone();
    }


    private class TimeStatsRunnable implements Runnable {
        @Override
        public void run() {
            Float batteryTemperature = ReceiverStorage.getInstance().getBatteryTemperature();
            Float batteryLevel = ReceiverStorage.getInstance().getBatteryLevel();
            L.i("Start time stats thread." +
                    "\n --> Battery temperature: " + batteryTemperature +
                    "\n --> Battery level: " + batteryLevel);
            if (batteryTemperature != null || batteryLevel != null) {
                Prefs prefs = PrefsController.instance.getPrefs();
                prefs.addTimeStats(new TimeStats(batteryTemperature, batteryLevel));
                PrefsController.instance.setPrefs(prefs);
            }
        }
    }

}
