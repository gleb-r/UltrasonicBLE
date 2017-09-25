package com.example.gleb.ultrasonicble;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Binder;

import java.util.Locale;


public class OdometerService extends Service {
    private final IBinder binder = new OdometerBinder();

    private Location lastLocation = null;


    public OdometerService() {
    }

    @Override
    public void onCreate() {
        LocationListener listener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {

                lastLocation = location;
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
        LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,1,listener);
    }



    public float getSpeed() {
        if (lastLocation == null)
        {
            return 0;
        } else {
            return this.lastLocation.getSpeed();
        }
    }




    public class OdometerBinder extends Binder {
        OdometerService getOdometer() {
            return OdometerService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
