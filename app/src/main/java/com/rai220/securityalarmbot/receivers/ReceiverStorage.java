package com.rai220.securityalarmbot.receivers;

/**
 *
 */

public class ReceiverStorage {
    private static ReceiverStorage instance = new ReceiverStorage();

    private volatile Float batteryTemperature;
    private volatile Float batteryLevel;

    private ReceiverStorage() {
    }

    public static ReceiverStorage getInstance() {
        if (instance == null) {
            instance = new ReceiverStorage();
        }
        return instance;
    }

    public Float getBatteryTemperature() {
        return batteryTemperature;
    }

    public void setBatteryTemperature(float batteryTemperature) {
        this.batteryTemperature = batteryTemperature;
    }

    public Float getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Float batteryLevel) {
        this.batteryLevel = batteryLevel;
    }
}
