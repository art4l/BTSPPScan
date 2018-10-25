package com.art4l.btsppscan.api.model;

import com.art4l.btsppscan.scanner.ColorCode;

public class DeviceInitializationDto {
    private String locationName;
    private String camundaFlow;
    private String deviceType;
    private String deviceId;
    private String colorCode;
    @ColorCode private String deviceScanto;

    public DeviceInitializationDto(){}

    public DeviceInitializationDto(String locationName, String camundaFlow, String deviceType, String deviceId, String colorCode, String deviceScanTo) {
        this.locationName = locationName;
        this.camundaFlow = camundaFlow;
        this.deviceType = deviceType;
        this.deviceId = deviceId;
        this.colorCode = colorCode;
        this.deviceScanto = deviceScanTo;
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

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(@ColorCode String colorCode) {
        this.colorCode = colorCode;
    }

    public String getDeviceScanto() {
        return deviceScanto;
    }

    public void setDeviceScanto(String deviceScanto) {
        this.deviceScanto = deviceScanto;
    }
}
