package com.art4l.btsppscan.config;

import android.support.annotation.WorkerThread;

import io.reactivex.Observable;

public class DeviceConfigHelper {
    /*
     * Manager responsible for getting the DeviceConfig from the device
     */
    private static final String FILE_NAME = "config_scanner.xml";
    private DeviceConfig mDeviceConfig;

    public DeviceConfigHelper() {
    }

    public Observable<DeviceConfig> getDeviceConfig() {
        if (mDeviceConfig != null) {
            return Observable.just(mDeviceConfig);
        } else {
            return Observable.just(FILE_NAME)
                    .map(DeviceConfigReader::readFromConfig)
                    .cache()
                    .doOnNext(deviceConfig -> mDeviceConfig = deviceConfig);
        }
    }

    @WorkerThread
    public DeviceConfig getDeviceConfigSync() {
        if (mDeviceConfig != null) {
            return mDeviceConfig;
        } else {
            mDeviceConfig = DeviceConfigReader.readFromConfig(FILE_NAME);
            return mDeviceConfig;
        }
    }

    @WorkerThread
    public String getNetworkConfigSync() {
        return DeviceConfigReader.readFromConfigNetwork(FILE_NAME);
    }

}
