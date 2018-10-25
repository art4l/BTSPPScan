package com.art4l.btsppscan.socket;

import android.content.Context;

import com.art4l.btsppscan.config.DeviceConfig;


public interface MqttSocket {

    void connect(SocketListener listener);

    void disconnect();

    void sendMessage(MqttMessage message);

    default void setSocketSession(DeviceConfig deviceConfig, final Context context){
        //do nothing, for compatibility reasons.
    };

}
