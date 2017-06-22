package com.rai220.securityalarmbot.model;

import com.rai220.securityalarmbot.utils.Constants;

import org.joda.time.DateTime;

/**
 *
 */

public class TimeStats {

    private DateTime dateTime = DateTime.now();
    private Float batteryTemperature;
    private Float batteryLevel;

    public TimeStats() {
    }

    public TimeStats(Float batteryTemperature, Float batteryLevel) {
        this.batteryTemperature = batteryTemperature;
        this.batteryLevel = batteryLevel;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public DateTime getDateTime() {
        return dateTime;
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

    public void setBatteryLevel(float batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    @Override
    public String toString() {
        return "Date: " + dateTime.toString(Constants.DATE_TIME_PATTERN) +
                "\n --> Temperature: " + batteryTemperature +
                "\n --> Level: " + batteryLevel;
    }

}
