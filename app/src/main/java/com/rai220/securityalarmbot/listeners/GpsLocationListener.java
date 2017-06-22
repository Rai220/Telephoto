package com.rai220.securityalarmbot.listeners;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.rai220.securityalarmbot.utils.L;

/**
 *
 */

public class GpsLocationListener implements LocationListener {
    private OnChangeCallback callback;

    public GpsLocationListener(OnChangeCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onLocationChanged(Location location) {
        L.i(String.format("Latitude: %.3f,  Longitude: %.3f", location.getLatitude(), location.getLongitude()));
        callback.updateLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public interface OnChangeCallback {
        void updateLocation(Location location);
    }
}
