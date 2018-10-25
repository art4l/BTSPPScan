package com.art4l.btsppscan.socket;

public interface SocketListener {

    void onMessage(MqttMessage data);

    void onError(String error);

}
