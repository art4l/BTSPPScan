package com.art4l.btsppscan;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;

import timber.log.Timber;

public class BTSPPScan extends Application {



    @Override
    public void onCreate(){
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.uprootAll();
            Timber.plant(new Timber.DebugTree());
        }


    }


}
