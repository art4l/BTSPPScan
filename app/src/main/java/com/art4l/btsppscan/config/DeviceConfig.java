package com.art4l.btsppscan.config;

import com.art4l.btsppscan.scanner.ColorCode;

public class DeviceConfig {

    //only 4 scanners are allowed
    public String scannerName1;
    public String scannerName2;
    public String scannerName3;
    public String scannerName4;
    public String scannerMacAddress_Color1;
    public String scannerMacAddress_Color2;
    public String scannerMacAddress_Color3;
    public String scannerMacAddress_Color4;
    @ColorCode public String color1;
    @ColorCode public String color2;
    @ColorCode public String color3;
    @ColorCode public String color4;

    public String mqttHost;
    public String mqqtPort;

    public String deviceId;
    public String deviceType;
    public String deviceScanTo;
    public String location;
    public String camundaFlow;
    public String flowengineIp;

    public String processInstanceId;
    public String spanId;
}

