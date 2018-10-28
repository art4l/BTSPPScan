package com.art4l.btsppscan.socket;


import com.art4l.btsppscan.scanner.ColorCode;
import com.art4l.btsppscan.scanner.ScannerMode;
import com.squareup.moshi.Json;

import java.util.ArrayList;
import java.util.List;

public class MqttMessage {

    public MqttMessage() {
    }

    public MqttMessage(String command) {
        this.command = command;
    }
    @Json(name = "fromdevice")
    public String fromDevice;
    @Json(name = "command")
    @MqttCommand public String command;
    @Json(name="processInstanceId")
    public String processInstanceId;
    @Json(name = "spanId")
    public String spanId;
    @Json(name= "scanToDevice")
    public String scanToDevice;
    @Json(name = "input")
    public Input input;
    @Json(name = "value")
    public String value;
    @Json(name = "scanmode")
    @ScannerMode public int mode;
    @Json(name="screenColorCode")
    @ColorCode public String screenColorCode;
    @Json (name = "textToSpeechCommand")
    public String textToSpeechCommand;
}
