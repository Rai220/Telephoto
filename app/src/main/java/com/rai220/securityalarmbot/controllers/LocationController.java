package com.rai220.securityalarmbot.controllers;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.rai220.securityalarmbot.listeners.GpsLocationListener;
import com.rai220.securityalarmbot.utils.L;

import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 *
 */

public class LocationController implements GpsLocationListener.OnChangeCallback {

    private final Object monitor = new Object();
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLastLocation;
    private volatile int callUpdateCount = 0;
    private Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                int i = 1;
                while (!Thread.currentThread().isInterrupted() && i <= 60) {
                    Thread.sleep(1000);
                    i++;
                }
                stop();
            } catch (InterruptedException ignore) {
            }
        }
    });


    @Override
    public void updateLocation(Location location) {
        mLastLocation = location;
        callUpdateCount++;
        L.i("accuracy: " + location.getAccuracy());
        if (mLastLocation.getAccuracy() < 100 || callUpdateCount >= 7) {
            stop();
        }
    }

    public void init(Context context) {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        updateLastLocation();
        L.i("coarse: " + mLocationManager.getLastKnownLocation(ACCESS_COARSE_LOCATION));
        L.i("fine: " + mLocationManager.getLastKnownLocation(ACCESS_FINE_LOCATION));
        L.i("best location: " + mLastLocation);
    }

    public void start() {
        synchronized (monitor) {
            if (mLocationManager != null && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                if (mLocationListener == null) {
                    mLocationListener = new GpsLocationListener(this);
                    try {
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, 10, mLocationListener);
                        if (!thread.isAlive()) {
                            thread.start();
                        }
                    } catch (Exception ex) {
                        L.e(ex);
                    }
                }
            }
        }
    }

    public void stop() {
        synchronized (monitor) {
            if (mLocationListener != null && mLocationManager != null) {
                try {
                    if (thread.isAlive()) {
                        thread.interrupt();
                    }
                    mLocationManager.removeUpdates(mLocationListener);
                } catch (Exception ex) {
                    L.e(ex);
                }
                mLocationListener = null;
            }
        }
    }

    public Location getActual() {
        updateLastLocation();
        Location bestLocation = getBestLocation();

        Location result;
        if (mLastLocation != null && bestLocation != null) {
            if ((mLastLocation.getTime() - bestLocation.getTime()) > (60 * 60 * 1000)) {
                result = mLastLocation;
                L.i("return lastLocation");
            } else {
                result = bestLocation;
                L.i("return best location");
            }
        } else {
            result = bestLocation == null ? mLastLocation : bestLocation;
            L.i(bestLocation == null ? "return lastLocation, because best is null" :
                    "return bestLocation, because last is null");
        }
        return result;
    }

    public void updateLastLocation() {
        List<String> providers = mLocationManager.getProviders(true);
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (mLastLocation == null || mLastLocation.getTime() < l.getTime()) {
                mLastLocation = l;
            }
        }
    }

    private Location getBestLocation() {
        Location bestLocation = null;
        List<String> providers = mLocationManager.getProviders(true);
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        return bestLocation;
    }

}
