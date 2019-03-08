package com.art4l.btsppscan;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.art4l.btsppscan.api.ApiService;
import com.art4l.btsppscan.api.NetworkModule;
import com.art4l.btsppscan.api.model.DeviceInitializationDto;
import com.art4l.btsppscan.config.DeviceConfig;
import com.art4l.btsppscan.config.DeviceConfigHelper;
import com.art4l.btsppscan.power.PowerManager;
import com.art4l.btsppscan.scandevice.BTSPPScanner;
import com.art4l.btsppscan.scandevice.HoneywellBTScanner;
import com.art4l.btsppscan.scanner.ColorCode;
import com.art4l.btsppscan.scanner.ScannerMode;
import com.art4l.btsppscan.socket.BlockingQueueSocketListener;
import com.art4l.btsppscan.socket.CamundaFlow;
import com.art4l.btsppscan.socket.CamundaSocket;
import com.art4l.btsppscan.socket.Input;
import com.art4l.btsppscan.socket.MqttCommand;
import com.art4l.btsppscan.socket.MqttMessage;
import com.art4l.btsppscan.socket.MqttSocket;
import com.art4l.btsppscan.socket.SocketListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Main object to handle the scandevices & the MQTT Connection
 *
 *
 */
public class ScannerThread  implements BTSPPScanner.OnConnectionStatus, BTSPPScanner.OnMessageReceived, SocketListener {


    DeviceConfigHelper mDeviceConfigHelper;
    DeviceConfig mDeviceConfig;

    List<BTSPPScanner> mBTSPPScanners = new ArrayList<>();
    List<CamundaFlow> mCamundaFlows = new ArrayList<>();

    ApiService mApiService;
    MqttSocket mqttSocket;

    private Context mContext;

    private BlockingQueueSocketListener mBlockingQueueSocketListener;
    private BlockingQueue<Message>  mBlockQueue;

    private int mConnectedDevices = 0;



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

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mReceiver, filter);


        mDeviceConfigHelper = new DeviceConfigHelper();
        //create the socket
        mqttSocket = new CamundaSocket();


        mDeviceConfig = mDeviceConfigHelper.getDeviceConfigSync();
/*
        mDeviceConfig.scannerMacAddress_Color1 ="00:10:20:D5:4C:A3";
        mDeviceConfig.scannerMacAddress_Color2 ="";
        mDeviceConfig.scannerMacAddress_Color3 ="00:10:20:D7:A5:2C";
        mDeviceConfig.scannerMacAddress_Color4 ="";
        mDeviceConfig.scannerName1 ="HONEYWELL_8670";
        mDeviceConfig.scannerName2 ="";
        mDeviceConfig.scannerName3 ="HONEYWELL_8670";
        mDeviceConfig.scannerName4 ="";
        mDeviceConfig.color1="RED";
        mDeviceConfig.color2="GREEN";
        mDeviceConfig.color3="AMBER";
        mDeviceConfig.color4="WHITE";

        mDeviceConfig.mqqtPort="1883";
        mDeviceConfig.mqttHost="10.236.84.24";
        mDeviceConfig.deviceId="ap_01";
        mDeviceConfig.deviceType="scanner";
        mDeviceConfig.deviceScanTo="projector/raspi_01";
        mDeviceConfig.location="venray";
        mDeviceConfig.camundaFlow="venray_sorting";
        mDeviceConfig.flowengineIp="https://art4l-poc.logistics.corp";

*/

        mApiService = NetworkModule.ProvideProjectorService(mDeviceConfig.flowengineIp);

        //initialize the MQTT Connection, make sure network is up & running

        mqttSocket.setSocketSession(mDeviceConfig,mContext);
        mqttSocket.connect(this);

        //initialize the blocking Queue (threadsafe)
        mBlockQueue = new LinkedBlockingDeque<>();
        mBlockingQueueSocketListener = new BlockingQueueSocketListener(this,mBlockQueue);
        mBlockingQueueSocketListener.start();

            //initialize the scanners
        initiateScanners(mDeviceConfig);

        Executors.newScheduledThreadPool(10).scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (mConnectedDevices == mBTSPPScanners.size()){
                    Timber.d("All devices connected");
                } else {
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    bluetoothAdapter.startDiscovery();
                }
            }
        },5,10, TimeUnit.SECONDS);


    }


    public void stop(){

        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException ex){

        }


    }



    /**
     *
     * Check if there is a network connection
     * @return
     */

    private boolean isConnected(){

        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

    }

    /**
     * Message from a scandevice
     *
     * @param colorCode
     * @param message
     */

    @Override
    public void messageReceived(String colorCode, ScanResult message) {

        //setting messages
        if (message.getBarcodeMessage().startsWith("BELB"))  return;
        if (message.getBarcodeMessage().startsWith("PAP")) return;

        if (message.getBarcodeMessage().startsWith("@REBOOT")){

            stop();
            ((MainActivity)mContext).reboot();
            return;
        }

        // if not initialized yet, no message received.
        if (getCamundaFlow(colorCode) == null) return;

        Timber.d("Barcode Received: " + message.getBarcodeMessage());
        //create a MQTT Message and send to Camunda Flow
        MqttMessage mqttMessage = new MqttMessage(MqttCommand.SEND_INPUT);
        Input input = new Input();
        input.value = message.getBarcodeMessage();
        input.key = "scanner";
        mqttMessage.input = input;

        mqttMessage.scanToDevice = mDeviceConfig.deviceScanTo;
        mqttMessage.fromDevice = mDeviceConfig.deviceType + "/"+ mDeviceConfig.deviceId;


        mqttMessage.processInstanceId = getCamundaFlow(colorCode).getProcessInstanceId();
        mqttMessage.spanId = getCamundaFlow(colorCode).getSpanId();
        mqttMessage.screenColorCode = colorCode;

        mqttSocket.sendMessage(mqttMessage);


    }


    @Override
    public void errorReceived(String type, String errorMessage) {
        Timber.d("Error Received: " + errorMessage);

    }

    /**
     * Connection from scanDevice
     *
     * @param colorCode
     */
    @SuppressLint("CheckResult")
    @Override
    public void onConnected(String colorCode) {
        Timber.d("Device connected: " + colorCode);
        // now do the REST Call for that device.
        mConnectedDevices++;

        final DeviceInitializationDto initializationDto = new DeviceInitializationDto();

        initializationDto.setLocationName(mDeviceConfig.location);
        initializationDto.setCamundaFlow(mDeviceConfig.camundaFlow);
        initializationDto.setDeviceType(mDeviceConfig.deviceType);
        initializationDto.setDeviceId(mDeviceConfig.deviceId+"_" + colorCode);
        initializationDto.setScanToDevice(mDeviceConfig.deviceScanTo);
        initializationDto.setScreenColorCode(colorCode);

        // this call opens the Camunda flow, it will also send back an initialize command.
        mApiService.getInitializationData(initializationDto)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(initData -> {
                    Timber.d("Device initialisation send");

                    CamundaFlow camundaFlow = getCamundaFlow(colorCode);
                    if (camundaFlow == null){
                        camundaFlow = new CamundaFlow();
                        camundaFlow.setColorCode(colorCode);
                        camundaFlow.setConnected(false);
                        mCamundaFlows.add(camundaFlow);
                    }
                }, error -> {
                    Timber.e(error.getLocalizedMessage());
                    error.printStackTrace();
                    //disconnect device

                    BTSPPScanner btsppScanner = getScanner(colorCode);
                    btsppScanner.sendErrorBeep();
                    //TODO REMOVE FOR PRODUCTION RELEASE
//                    btsppScanner.stopScanner();

                });
    }

    @Override
    public void onDisconnected(String colorCode, boolean isRetrying) {

        mConnectedDevices--;
        if (mConnectedDevices < 0 ) mConnectedDevices = 0;

        BTSPPScanner btsppScanner = getScanner(colorCode);

        btsppScanner.stopScanner();

    }

    /**
     * Message from MQTT
     *
     * @param message
     */
    @Override
    public void onMessage(MqttMessage message) {


        //check first if this a keep alive message
        if (!message.command.equals(MqttCommand.INITIALIZE) && !message.command.equals(MqttCommand.SET_SCANNER_MODE)) return;

        // store the last process & spanId
        boolean newCamundaFlow = false;
        CamundaFlow camundaFlow = getCamundaFlow(message.screenColorCode);
        if (camundaFlow == null){
            camundaFlow = new CamundaFlow();
            newCamundaFlow  = true;
        }
        camundaFlow.setColorCode(message.screenColorCode);
        camundaFlow.setProcessInstanceId(message.processInstanceId);
        camundaFlow.setSpanId(message.spanId);

        if (newCamundaFlow) mCamundaFlows.add(camundaFlow);

        switch (message.command) {
            case MqttCommand.INITIALIZE:
                //initialize received, and put device in initial scanmodus.
                BTSPPScanner btsppScanner = getScanner(message.screenColorCode);
                if (btsppScanner !=null) {

                    if (message.mode == ScannerMode.ERROR) btsppScanner.sendErrorBeep();
                    if (message.mode == ScannerMode.CONTINUOUS_INPUT) btsppScanner.setContinuousMode();
                    if (message.mode == ScannerMode.TILE_INPUT) btsppScanner.setSingleMode();
                } else{
                    Timber.d("INITIALIZE command for non active scanner with colorcode: %s", message.screenColorCode);
                }
                break;
            case MqttCommand.SET_SCANNER_MODE:
                //check which device to set into scannermode
                btsppScanner = getScanner(message.screenColorCode);
                if (btsppScanner !=null) {
                    if (message.mode == ScannerMode.ERROR) btsppScanner.sendErrorBeep();
                    if (message.mode == ScannerMode.CONTINUOUS_INPUT) btsppScanner.setContinuousMode();
                    if (message.mode == ScannerMode.TILE_INPUT) btsppScanner.setSingleMode();
                } else {
                    Timber.d("SET_SCANNER_MODE command for non active scanner with colorcode: %s", message.screenColorCode);

                }
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

        //Initiate 4 scanners maximum, this is for the Honywell scanner
        if (deviceConfig.scannerName1 != null && deviceConfig.scannerName1.equalsIgnoreCase("HONEYWELL_8670")){
            final BTSPPScanner btsppScanner = new HoneywellBTScanner(mContext);
            btsppScanner.initiateScanner(deviceConfig.scannerMacAddress_Color1,deviceConfig.color1);
            btsppScanner.setColorCode(deviceConfig.color1);
            btsppScanner.setMessageListener(this);
            btsppScanner.setConnectionListener(this);
            btsppScanner.setBlockingQueue(mBlockQueue);

            mBTSPPScanners.add(btsppScanner);

        }
        if (deviceConfig.scannerName2 != null && deviceConfig.scannerName2.equalsIgnoreCase("HONEYWELL_8670")){
            final BTSPPScanner btsppScanner = new HoneywellBTScanner(mContext);
            btsppScanner.initiateScanner(deviceConfig.scannerMacAddress_Color2,deviceConfig.color2);
            btsppScanner.setColorCode(deviceConfig.color2);
            btsppScanner.setMessageListener(this);
            btsppScanner.setConnectionListener(this);
            btsppScanner.setBlockingQueue(mBlockQueue);

            mBTSPPScanners.add(btsppScanner);


        }

        if (deviceConfig.scannerName3 !=null && deviceConfig.scannerName3.equalsIgnoreCase("HONEYWELL_8670")){
            final BTSPPScanner btsppScanner = new HoneywellBTScanner(mContext);
            btsppScanner.initiateScanner(deviceConfig.scannerMacAddress_Color3,deviceConfig.color3);
            btsppScanner.setColorCode(deviceConfig.color3);
            btsppScanner.setMessageListener(this);
            btsppScanner.setConnectionListener(this);
            btsppScanner.setBlockingQueue(mBlockQueue);


            mBTSPPScanners.add(btsppScanner);

        }

        if (deviceConfig.scannerName4 !=null && deviceConfig.scannerName4.equalsIgnoreCase("HONEYWELL_8670")){
            final BTSPPScanner btsppScanner = new HoneywellBTScanner(mContext);
            btsppScanner.initiateScanner(deviceConfig.scannerMacAddress_Color4,deviceConfig.color4);
            btsppScanner.setColorCode(deviceConfig.color4);
            btsppScanner.setMessageListener(this);
            btsppScanner.setConnectionListener(this);
            btsppScanner.setBlockingQueue(mBlockQueue);


            mBTSPPScanners.add(btsppScanner);

        }

    }

    /**
     * Run as async tasks for ever....
     *
     *
     */

    private void startScanners(final String macAddress) {


        for (BTSPPScanner btsppScanner : mBTSPPScanners) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (btsppScanner.getMacAddress().equalsIgnoreCase(macAddress) && !btsppScanner.isStarted()) {
                        Timber.d("StartScanner for macAddress: " + macAddress);
                        Looper.prepare();
                        btsppScanner.startScanner();
                        Looper.loop();
                    }
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

    private CamundaFlow getCamundaFlow(String colorCode){
        for (CamundaFlow camundaFlow:mCamundaFlows){
            if (camundaFlow.getColorCode().equals(colorCode)) return camundaFlow;

        }
        return null;
    }

    // The BroadcastReceiver that listens for discovered devices and
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            String action = intent.getAction();


            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Timber.e("Scanned devices: %s, Mac: %s",device.getName(),device.getAddress());

                ScannerThread.this.startScanners(device.getAddress());
            }

        }
    };

    /**
     * Make the device permanent discoverable
     *
     */
    private void makeDiscoverable(){

        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        mContext.startActivity(discoverableIntent);


    }
}
