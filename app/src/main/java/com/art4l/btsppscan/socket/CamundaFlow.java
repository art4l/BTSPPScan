package com.art4l.btsppscan.socket;

/**
 *
 * Storage for the Camunda flows of each scanner
 */

public class CamundaFlow {

    String processInstanceId;
    String spanId;
    String colorCode;
    boolean isConnected = false;

    public CamundaFlow() {
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }
}
