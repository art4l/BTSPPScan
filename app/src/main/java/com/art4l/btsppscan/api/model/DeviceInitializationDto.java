package com.art4l.btsppscan.api.model;

import com.art4l.btsppscan.scanner.ColorCode;

public class DeviceInitializationDto {
    private String locationName;
    private String camundaFlow;
    private String deviceType;
    private String deviceId;
    private String screenColorCode;
    @ColorCode private String scanToDevice;

    public DeviceInitializationDto(){}

    public DeviceInitializationDto(String locationName, String camundaFlow, String deviceType, String deviceId, String colorCode, String deviceScanTo) {
        this.locationName = locationName;
        this.camundaFlow = camundaFlow;
        this.deviceType = deviceType;
        this.deviceId = deviceId;
        this.screenColorCode = colorCode;
        this.scanToDevice = deviceScanTo;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getCamundaFlow() {
        return camundaFlow;
    }

    public void setCamundaFlow(String camundaFlow) {
        this.camundaFlow = camundaFlow;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getScreenColorCode() {
        return screenColorCode;
    }

    public void setScreenColorCode(@ColorCode String colorCode) {
        this.screenColorCode = colorCode;
    }

    public String getScanToDevice() {
        return scanToDevice;
    }

    public void setScanToDevice(String scanToDevice) {
        this.scanToDevice = scanToDevice;
    }
}
