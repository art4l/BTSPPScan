package com.art4l.btsppscan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;

import com.art4l.btsppscan.api.ApiService;
import com.art4l.btsppscan.api.NetworkModule;
import com.art4l.btsppscan.api.model.DeviceInitializationDto;
import com.art4l.btsppscan.config.DeviceConfig;
import com.art4l.btsppscan.config.DeviceConfigHelper;
import com.art4l.btsppscan.scandevice.BTSPPScanner;
import com.art4l.btsppscan.scandevice.HoneywellBTScanner;
import com.art4l.btsppscan.scanner.ScannerMode;
import com.art4l.btsppscan.socket.CamundaSocket;
import com.art4l.btsppscan.socket.MqttCommand;
import com.art4l.btsppscan.socket.MqttMessage;
import com.art4l.btsppscan.socket.MqttSocket;
import com.art4l.btsppscan.socket.SocketListener;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ScannerThread  implements BTSPPScanner.OnConnectionStatus, BTSPPScanner.OnMessageReceived, SocketListener {


    DeviceConfigHelper mDeviceConfigHelper;
    DeviceConfig mDeviceConfig;

    List<BTSPPScanner> mBTSPPScanners = new ArrayList<>();
    ApiService mApiService;
    MqttSocket mqttSocket;

    private Context mContext;

    public ScannerThread(Context context){
        mContext = context;

    }


    public void init(){

        if (!isConnected()){
            //brute exit !!!
            //network check should be done with Broadcast
            System.exit(0);
            return;
        }
        mDeviceConfigHelper = new DeviceConfigHelper();
        //create the socket
        mqttSocket = new CamundaSocket();


        mDeviceConfig = mDeviceConfigHelper.getDeviceConfigSync();
        mApiService = NetworkModule.ProvideProjectorService(mDeviceConfig.flowengineIp);

        //initialize the MQTT Connection, make sure network is up & running

        mqttSocket.setSocketSession(mDeviceConfig,mContext);
        mqttSocket.connect(this);

            //initialize the scanners
        initiateScanners(mDeviceConfig);

        
    }


    private boolean isConnected(){


        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

    }


    @Override
    public void messageReceived(String colorCode, ScanResult message) {
        Timber.d("Barcode Received: " + message.getBarcodeMessage());
        //create a MQTT Message and send to Camunda Flow
        MqttMessage mqttMessage = new MqttMessage(MqttCommand.SEND_INPUT);
        mqttMessage.input = message.getBarcodeMessage();
        mqttMessage.scanToDevice = mDeviceConfig.deviceScanTo;
        mqttMessage.fromDevice = mDeviceConfig.deviceType + "/"+ mDeviceConfig.deviceId;

        mqttMessage.processInstanceId = mDeviceConfig.processInstanceId;
        mqttMessage.spanId = mDeviceConfig.spanId;
        mqttMessage.screenColorCode = colorCode;

        mqttSocket.sendMessage(mqttMessage);



    }

    @Override
    public void errorReceived(String type, String errorMessage) {
        Timber.d("Error Received: " + errorMessage);


    }

    @Override
    public void onConnected(String colorCode) {

    }

    @Override
    public void onDisconnected(String colorCode, boolean isRetrying) {

    }

    @Override
    public void onMessage(MqttMessage message) {
        // store the last process & spanId
        mDeviceConfig.processInstanceId = message.processInstanceId;
        mDeviceConfig.spanId = message.spanId;

        switch (message.command) {
            case MqttCommand.INITIALIZE:
                //initialize received, and put device in initial scanmodus.
                BTSPPScanner btsppScanner = getScanner(message.screenColorCode);
                if (message.mode == ScannerMode.ERROR) btsppScanner.sendCommand("");
                if (message.mode == ScannerMode.CONTINUOUS_INPUT) btsppScanner.sendCommand("");
                if (message.mode == ScannerMode.TILE_INPUT) btsppScanner.sendCommand("");

                break;
            case MqttCommand.SET_SCANNER_MODE:
                //check which device to set into scannermode
                btsppScanner = getScanner(message.screenColorCode);
                if (message.mode == ScannerMode.ERROR) btsppScanner.sendCommand("");
                if (message.mode == ScannerMode.CONTINUOUS_INPUT) btsppScanner.sendCommand("");
                if (message.mode == ScannerMode.TILE_INPUT) btsppScanner.sendCommand("");

                break;
        }


    }

    @Override
    public void onError(String error) {

    }

    /**
     * Create and Initiate the scanners, defined in the config file
     *
     * @param deviceConfig
     */

    @SuppressLint("CheckResult")
    private void initiateScanners(DeviceConfig deviceConfig){

        //Initiate 4 scanners maximum
        if (deviceConfig.scannerName1 != null && deviceConfig.scannerName1.equalsIgnoreCase("HONEYWELL_8670")){
            final BTSPPScanner btsppScanner = new HoneywellBTScanner(mContext);
            btsppScanner.initiateScanner(deviceConfig.scannerMacAddress_Color1,deviceConfig.color1);
            btsppScanner.setColorCode(deviceConfig.color1);
            btsppScanner.setMessageListener(this);
            btsppScanner.setConnectionListener(this);
            //do a rest call for the first scanner
            final DeviceInitializationDto initializationDto = new DeviceInitializationDto();

            initializationDto.setLocationName(deviceConfig.location);
            initializationDto.setCamundaFlow(deviceConfig.camundaFlow);
            initializationDto.setDeviceType(deviceConfig.deviceType);
            initializationDto.setDeviceId(deviceConfig.deviceId);
            initializationDto.setDeviceScanto(deviceConfig.deviceScanTo);

            mApiService.getInitializationData(initializationDto)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(initData -> {
                        // download finished, add the scanner
                        mBTSPPScanners.add(btsppScanner);
                        startScanners();                        //do this when all scanners are configured
                    }, error -> {
                        Timber.e(error.getLocalizedMessage());
                        error.printStackTrace();
                        //add the scanner anyway
                        mBTSPPScanners.add(btsppScanner);
                        startScanners();

                    });

        }
        if (deviceConfig.scannerName2 != null && deviceConfig.scannerName2.equalsIgnoreCase("HONEYWELL_8670")){
            BTSPPScanner btsppScanner = new HoneywellBTScanner(mContext);
            btsppScanner.initiateScanner(deviceConfig.scannerMacAddress_Color2,deviceConfig.color2);
            btsppScanner.setMessageListener(this);
            btsppScanner.setConnectionListener(this);

            //do a rest call for the second scanner

            mBTSPPScanners.add(btsppScanner);
        }

        if (deviceConfig.scannerName3 !=null && deviceConfig.scannerName3.equalsIgnoreCase("HONEYWELL_8670")){
            BTSPPScanner btsppScanner = new HoneywellBTScanner(mContext);
            btsppScanner.initiateScanner(deviceConfig.scannerMacAddress_Color3,deviceConfig.color3);
            btsppScanner.setMessageListener(this);
            btsppScanner.setConnectionListener(this);

            //do a rest call for the third scanner

            mBTSPPScanners.add(btsppScanner);
        }

        if (deviceConfig.scannerName4 !=null && deviceConfig.scannerName4.equalsIgnoreCase("HONEYWELL_8670")){
            BTSPPScanner btsppScanner = new HoneywellBTScanner(mContext);
            btsppScanner.initiateScanner(deviceConfig.scannerMacAddress_Color4,deviceConfig.color4);
            btsppScanner.setMessageListener(this);
            btsppScanner.setConnectionListener(this);

            //do a rest call for the forth scanner

            mBTSPPScanners.add(btsppScanner);
        }



    }

    /**
     * Run as async tasks for ever....
     *
     */
    private void startScanners() {




        for (BTSPPScanner btsppScanner : mBTSPPScanners) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    btsppScanner.startScanner();
                    Looper.loop();
                }
            }).start();

        }


    }

    //find the scanner with the appropriate colorcode

    private BTSPPScanner getScanner(String colorCode){
        for (BTSPPScanner btsppScanner:mBTSPPScanners){
            if (btsppScanner.getColorCode().equals(colorCode)) return btsppScanner;
        }

        return null;
    }


}
