package com.packtpub.odometer;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.google.android.gms.location.LocationRequest;

// TODO: implement asking for GPS and location updates in general according to following link:
// https://developer.android.com/training/location
public class OdometerService extends Service {

    private final IBinder mBinder = new OdometerBinder();
    private LocationListener mListener;
    private LocationManager mLocationManager;
    final static String PERMISSION_STRING = Manifest.permission.ACCESS_FINE_LOCATION;

    // When the activity is stopped & restarted, this service is destroyed and a new one is created.
    // Since a service does not have onSaveInstanceState(), the only way to maintain the data is to
    // use static fields, like the below!
    private static double mDistanceInMeters;
    private static Location mPreviousLocation;

    @Override
    public void onCreate() {
        super.onCreate();

        mListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (mPreviousLocation == null) {
                    mPreviousLocation = location;
                }
                mDistanceInMeters += location.distanceTo(mPreviousLocation);
                mPreviousLocation = location;
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

        };
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null && mListener != null) {
            mLocationManager.removeUpdates(mListener);
            mLocationManager = null;
            mListener = null;
        }
    }

    long getDistance() {
        return Math.round(mDistanceInMeters);
    }

    class OdometerBinder extends Binder {
        OdometerService getOdometer() {
            return OdometerService.this;
        }
    }
}
