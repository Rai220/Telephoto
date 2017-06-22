package com.rai220.securityalarmbot.listeners;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.commands.types.SensitivityType;
import com.rai220.securityalarmbot.telegram.ISenderService;
import com.rai220.securityalarmbot.utils.L;

/**
 *
 */

public class SensorListener implements SensorEventListener {
    private static final int TIMEOUT = 10000; // 10sec
    private SensitivityType type;
    private Context context;
    private ISenderService service;
    private float mPosition = 0;
    private long alarmTime = 0;

    public void init(ISenderService service, Context context, SensitivityType type) {
        mPosition = 0;
        this.service = service;
        this.context = context;
        this.type = type;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float xy_angle = event.values[0]; //Плоскость XY
            float xz_angle = event.values[1]; //Плоскость XZ
            float zy_angle = event.values[2]; //Плоскость ZY

            float sum = (xy_angle + xz_angle + zy_angle);

            if (mPosition == 0) {
                mPosition = sum;
            }
            float vibrationLevel = Math.abs(mPosition - sum);
            if (vibrationLevel > 0.5) {
                L.d("Diff: " + vibrationLevel);
            }
            if (vibrationLevel > type.getShakeValue()) {
                L.i("Shake detected! Diff: " + vibrationLevel);
                long time = System.currentTimeMillis();
                if (alarmTime == 0 || (time - alarmTime) > TIMEOUT) {
                    if (service != null) {
                        service.sendMessageToAll(context.getString(R.string.alarm_shake));
                    }
                    alarmTime = time;
                }
            }
            mPosition = sum;
        } else {
            L.i(String.format("eventName: %s, eventType: %s", event.sensor.getName(), event.sensor.getType()));
            for (int i = 0; i < event.values.length; i++) {
                L.i(String.format("value %s: %s", i, event.values[i]));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
