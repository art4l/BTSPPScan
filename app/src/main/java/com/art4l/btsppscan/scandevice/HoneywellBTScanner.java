package com.art4l.btsppscan.scandevice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.art4l.btsppscan.ScanResult;
import com.art4l.btsppscan.ServiceManager;
import com.art4l.btsppscan.scanner.BTServer;
import com.art4l.btsppscan.scanner.BTServerAsync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class HoneywellBTScanner implements BTSPPScanner{

    private static final String TAG = HoneywellBTScanner.class.getSimpleName();
    private static final boolean D = false;

    //Bluetooth service
//    private ServiceManager btService;
    private BTServerAsync btService;

    private int bTRetryCounter = 0;					//retry when there are problems with Bluetooth Channel

    private static final int MAXRETRY = -1;			//retry for eternity....
    private String mMacAddress;
    private Context mContext;
    private boolean isBTStarted = false;
    private BluetoothAdapter mBluetoothAdapter;

    private List<ServiceManager> mBtServices = new ArrayList<>();

    private String mColorCode;

    private Handler mBTHandler;


    // return event
    private OnMessageReceived mMessageListener;
    private OnConnectionStatus mConnectionListener;



    class BTHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case BTServer.STATE_NONE:
                case BTServer.STATE_LISTEN:
                    //give instruction to press on button
                    if (D) Log.d(TAG, "Maken van verbinding met Ringscanner..");
                    break;
                case BTServer.STATE_CONNECTED:
                    // show that scanner is connected

                    bTRetryCounter = 0;        //reset retry counter
                    isBTStarted = true;

                    mConnectionListener.onConnected(mColorCode);

                    break;
                case BTServer.STATE_BARCODE:                //scanned data returned
                    if (D) Log.d(TAG, "Barcode from BTServer received with Colorcode:" + mColorCode);
                    ScanResult scanResult = new ScanResult();
                    scanResult.setBarcodeType("generic barcode");
                    scanResult.setBarcodeMessage(((String) msg.obj).trim());
                    mMessageListener.messageReceived(mColorCode, scanResult);

                    break;
                case BTServer.STATE_LOST:
                    if (D) Log.d(TAG, "Connection lost");

                    break;
                case BTServer.STATE_NOBT:            //problem with BT COnnection
                    Log.d(TAG, "No BT Connection, retry for ColorCode:" + mColorCode);
                    mMessageListener.errorReceived("NOCONNECT", "Connection lost with scanner, retry to connect");

                    bTRetryCounter++;
                    if (bTRetryCounter == MAXRETRY) {
                        if (D) Log.d(TAG, "Connection Lost with scanner");
                        mMessageListener.errorReceived("NORETRY", "Connection lost with scanner, no retry");


                        isBTStarted = false;
                        proceedDiscovery();
                        //go back into discovery mode
                        mConnectionListener.onDisconnected(mColorCode, false);

                        break;

                    }
//                    try {
                        mConnectionListener.onDisconnected(mColorCode, true);

                        HoneywellBTScanner.this.stopScanner();


                        Executors.newScheduledThreadPool(10).schedule(new Runnable() {
                            @Override
                            public void run() {
                                Timber.d("Restart the service for colorcode: "+ mColorCode);
                                HoneywellBTScanner.this.startScanner();
                            }
                        },2, TimeUnit.SECONDS);

                    break;


                default:
                    super.handleMessage(msg);
            }

        }

    };



    public HoneywellBTScanner(Context context){
        mContext = context;


    }


    public void initiateScanner(final String macAddress, String colorCode){

        mMacAddress = macAddress;
        mColorCode = colorCode;

        mBTHandler = new BTHandler();

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mReceiver, filter);


    }

    public void startScanner(){
        if (mMacAddress == null || mMacAddress.isEmpty()) return;

        // Register for broadcasts when a device is discovered

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mReceiver, filter);
        btService = new BTServerAsync();


        btService.onStartService(this,mMacAddress, mBTHandler);

    }

    public void stopScanner() {
        //       btService.stop();
        if (btService != null) btService.onStopService();
        btService = null;

        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException ex){

        }
    }


    public void setMessageListener(OnMessageReceived messageListener){
        mMessageListener = messageListener;

    }

    public void setConnectionListener(OnConnectionStatus connectionListener){
        mConnectionListener = connectionListener;
    }



    private void proceedDiscovery() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        mBluetoothAdapter.startDiscovery();

    }

    @Override
    public void setColorCode(String colorCode){
        mColorCode = colorCode;
    }

    @Override
    public String getColorCode(){
        return mColorCode;
    }


    /**
     * Send a command to the scanner
     *
     * @param command
     */

    public void sendCommand(String command){

        byte[] bCommand = command.getBytes();
        byte[] writeOut = new byte[1008];


        byte[] name = ":XENON:".getBytes();

        writeOut[0] =22;
        writeOut[1] =77;
        writeOut[2] =13;
//      for (int i=0; i<name.length;i++){
//          writeOut[3+i] = name[i];
//      }

        for (int i=0; i<bCommand.length;i++){
            writeOut[3+i] = bCommand[i];
        }

        btService.write(writeOut);
/*
        try {
            btService.send(Message.obtain(null, BTServer.WRITE_SETTING,new String(writeOut)));
        }catch(RemoteException ex) {
        }
*/
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
                if (D) Log.d(TAG, "BT Received: " + device.getName() + " " + device.getAddress()+ " "+ isBTStarted);
                //check if this has the same address as the one expected and start service if is was not started yet.
                if (device.getAddress().equalsIgnoreCase(mMacAddress) && !isBTStarted) {
                    mBluetoothAdapter.cancelDiscovery();
                    HoneywellBTScanner.this.startScanner();
                }
            }

        }
    };




}
