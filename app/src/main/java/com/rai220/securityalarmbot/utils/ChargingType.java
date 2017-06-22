package com.rai220.securityalarmbot.utils;

import android.os.BatteryManager;

/**
 *
 */

public enum ChargingType {

    AC(BatteryManager.BATTERY_PLUGGED_AC),
    USB(BatteryManager.BATTERY_PLUGGED_USB),
    WIRELESS(BatteryManager.BATTERY_PLUGGED_WIRELESS);

    private int id;

    ChargingType(int id) {
        this.id = id;
    }

    public static ChargingType getType(int typeId) {
        for (ChargingType type : values()) {
            if (type.id == typeId) {
                return type;
            }
        }
        return null;
    }
}
