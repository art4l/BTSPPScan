package com.art4l.btsppscan.socket;

import android.content.Context;


import com.art4l.btsppscan.config.DeviceConfig;
import com.art4l.btsppscan.config.DeviceConfigHelper;
import com.art4l.btsppscan.scanner.ColorCode;
import com.squareup.moshi.Moshi;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;

import timber.log.Timber;


public class CamundaSocket implements MqttSocket, SocketListener {


    private SocketListener mListener;
    private MqttAndroidClient mMqttClient;

    private String mLocation;
    private String mCamundaFlow;
    private String mFromDevice;
    private int mPrevhashValue = 0;

    public CamundaSocket() {
    }

    @Override
    public void connect(SocketListener listener) {
        // save listener
        this.mListener = listener;

    }

    @Override
    public void disconnect() {
        // disconnect
        this.mListener = null;
        Timber.d("Closing MQTTClient");
        mMqttClient.close();

    }

    @Override
    public void sendMessage(MqttMessage message) {

        publishMessage(mLocation+"/"+ mCamundaFlow, message);

    }


    @Override
    public void onMessage(MqttMessage data) {
        if (mListener != null) {
            mListener.onMessage(data);
        }
    }

    @Override
    public void onError(String error) {
        if (mListener != null) {
            mListener.onError(error);
        }
    }

    @Override
    public void setSocketSession(DeviceConfig deviceConfig, final Context context) {


        mqttConnect(context, deviceConfig.mqttHost, deviceConfig.mqqtPort, deviceConfig);


    }


    private void mqttConnect(Context context, String mqttHost, String mqttPort, final DeviceConfig deviceConfig) {


        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        options.setAutomaticReconnect(true);


        String connectUrl = "tcp://" + mqttHost + ":" + mqttPort;
        try {

            String clientId = "ID" + System.currentTimeMillis();

            mMqttClient = new MqttAndroidClient(context, connectUrl, clientId);
            mMqttClient.setCallback(new MqttEventCallbackExtended());

            mMqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Timber.d("MQTT Connected");


                    subscribeTopics(deviceConfig.location, deviceConfig.camundaFlow, deviceConfig.deviceType, deviceConfig.deviceId);

                    mCamundaFlow = deviceConfig.camundaFlow;
                    mLocation = deviceConfig.location;
                    mFromDevice = deviceConfig.deviceType+"/"+deviceConfig.deviceId;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Timber.e("MQTT Not connected, reason: " + exception);
                    onError("No MQTT Connection");
                }
            });


        } catch (MqttException ex) {
            Timber.e(ex);

        }

    }


    /**
     * Subscribe to the topics
     */
    private void subscribeTopics(String location, String camundaFlow, String deviceType, String deviceId) {

        String[] topicFilters = new String[8];
        int[] Qos = new int[8];


        topicFilters[0] = location;
        Qos[0] = 2;
        topicFilters[1] = location + "/" + camundaFlow+"/device";
        Qos[1] = 2;
        topicFilters[2] = location + "/" + camundaFlow + "/device/" + deviceType;
        Qos[2] = 2;
        topicFilters[3] = location + "/" + camundaFlow + "/device/" + deviceType + "/" + deviceId +"_"+ ColorCode.RED;
        Qos[3] = 2;
        topicFilters[4] = location + "/" + camundaFlow + "/device/" + deviceType + "/" + deviceId +"_"+ ColorCode.GREEN;
        Qos[4] = 2;
        topicFilters[5] = location + "/" + camundaFlow + "/device/" + deviceType + "/" + deviceId +"_"+ ColorCode.AMBER;
        Qos[5] = 2;
        topicFilters[6] = location + "/" + camundaFlow + "/device/" + deviceType + "/" + deviceId +"_"+ ColorCode.WHITE;
        Qos[6] = 2;
        topicFilters[7] = location + "/" + camundaFlow + "/device/" + deviceType + "/" + deviceId;
        Qos[7] = 2;


        try {
            mMqttClient.subscribe(topicFilters, Qos);
        } catch (MqttException ex) {


        } catch (IllegalArgumentException ex2) {

        }

    }



    private class MqttEventCallbackExtended implements MqttCallbackExtended {

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {

        }

        @Override
        public void connectionLost(Throwable arg0) {
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            try {
                Timber.d("Message Delivered with token:" + arg0.getMessage().toString());
            } catch (MqttException ex){

            }

        }

        @Override
        public void messageArrived(String topic, final org.eclipse.paho.client.mqttv3.MqttMessage msg) throws Exception {

            Timber.d("Message Arrived: " + new String(msg.getPayload()));
            int hashValue = new String(msg.getPayload()).hashCode();

            //check if message is a duplicate.
            if(mPrevhashValue != hashValue) {

                handleMessage(topic, new String(msg.getPayload()));
            }

            mPrevhashValue = hashValue;


        }

    }

    /**
     *
     *
     * @param topic
     * @param message
     */
    private void handleMessage(String topic, String message){

        Moshi moshi = new Moshi.Builder().build();


        try {
            MqttMessage mqttMessage = moshi.adapter(MqttMessage.class).fromJson(message);
            mListener.onMessage(mqttMessage);


            mqttMessage.command = MqttCommand.ACK;
            //send back an ack message

            publishMessage(mLocation+"/"+ mCamundaFlow, mqttMessage);

        } catch (IOException e) {
            e.printStackTrace();
            Timber.e(e);
        }

    }

    /**
     * Publish an MQTT Message
     *
     * @param topic
     * @param mqttMessage
     */

    private void publishMessage(String topic,MqttMessage mqttMessage){

        //add the from device to the message;
        mqttMessage.fromDevice = mFromDevice;

        Moshi moshi = new Moshi.Builder().build();

        String jSonMessage = moshi.adapter(MqttMessage.class).toJson(mqttMessage);
        try {
            mMqttClient.publish(topic, jSonMessage.getBytes(), 2, false);
        } catch (MqttException e){
            Timber.e(e);

        }

    }
}