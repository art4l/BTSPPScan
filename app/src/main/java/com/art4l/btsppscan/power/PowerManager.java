package com.art4l.btsppscan.power;

import android.os.Handler;

import com.google.android.things.device.DeviceManager;

public class PowerManager {

    private Handler mHandler = new Handler();

    private Runnable mRebootDevice = () -> {
        try {
            DeviceManager deviceManager = DeviceManager.getInstance();
            deviceManager.reboot();
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    public PowerManager() {
    }

    public void reboot(long inMinutes) {
        // remove previous handlers
        mHandler.removeCallbacks(mRebootDevice);
        // set delay
        mHandler.postDelayed(mRebootDevice, inMinutes * 60L * 1000L);
    }
}
