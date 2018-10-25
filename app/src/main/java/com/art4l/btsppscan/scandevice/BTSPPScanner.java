package com.art4l.btsppscan.scandevice;

import android.os.Message;

import com.art4l.btsppscan.ScanResult;
import com.art4l.btsppscan.scanner.ColorCode;

public interface BTSPPScanner {


    void initiateScanner(final String macAddress, @ColorCode String colorCode);
    void startScanner();
    void stopScanner();
    void sendCommand(String command);
    void setColorCode(String colorCode);
    String getColorCode();
    void setMessageListener(OnMessageReceived messageListener);
    void setConnectionListener(OnConnectionStatus connectionListener);

    /**
     *
     * Declare the interface to receive data
     *
     *
     */

    public interface OnMessageReceived {
        void messageReceived(String colorCode, ScanResult message);
        void errorReceived(String type, String errorMessage);
    }

    public interface OnConnectionStatus{
        void onConnected(String colorCode);
        void onDisconnected(String colorCode,boolean isRetrying);

    }

}
