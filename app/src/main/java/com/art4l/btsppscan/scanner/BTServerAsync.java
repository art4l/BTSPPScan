package com.art4l.btsppscan.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.art4l.btsppscan.scandevice.BTSPPScanner;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Dirk on 10/02/17.
 */

public class BTServerAsync  {

    // Debugging
    protected String TAG = "BTServerAsync";
    protected static final boolean D = false;


    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device, but not active yet
    public static final int STATE_ACTIVE =  4;	 // remote device is active
    public static final int STATE_LOST =  5;    // connections lost with remote device
    public static final int STATE_NOBT = 6;     //problem with BT connection



    private int mState = 0;

    public static final int STATE_BARCODE = 10;    // send scanned barcode
    public static final int REQ_RESTART_BTSERVER = 20;      //request to restart the BT Server
    public static final int REQ_MACADDRESS = 30;
    public static final int MACADDRESS = 40;
    public static final int WRITE_SETTING = 50;


    public BluetoothAdapter mBluetoothAdapter;
    protected static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    protected String macAddress;

    protected AcceptThread mAcceptThread;
    protected ConnectThread mConnectThread;
    protected ConnectedThread mConnectedThread;

    private BTSPPScanner mBTSPPSscanner;

    private Handler mHandler;

    public void onStartService(BTSPPScanner btsppScanner,String macAddress,Handler handler){
        if (D) Log.d(TAG,"Service Started");
        this.macAddress = macAddress;
        this.mBTSPPSscanner = btsppScanner;
        this.mHandler = handler;

        if (mState != STATE_CONNECTED) {
            mState = STATE_NONE;
            startBTServer();
        }


    }

    public void onServiceStarted() {
    }


    public void onStopService(){

        if (D) Log.d(TAG,"onStopService");

        setState(STATE_NONE);
        
        // Cancel any thread attempting to make a connection

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }


    }


    private void send(Message message){
        mHandler.handleMessage(message);
    }

    public void startBTServer(){

        if (D) Log.d(TAG,"Start BTServer");
        makeDiscoverable();
        // Cancel any thread attempting to make a connection
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }


        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            if (D)Log.d(TAG,"New AcceptThread created");
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        if (mAcceptThread.mmDevice !=null) {
            setState(STATE_LISTEN);
        }
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {

        if (mState == STATE_CONNECTED || mState == STATE_ACTIVE) {

            // Create temporary object
            ConnectedThread r;
            // Synchronize a copy of the ConnectedThread
            synchronized (this) {
                r = mConnectedThread;
            }
            // Perform the write unsynchronized
            r.write(out);
        }
    }

    private void makeDiscoverable(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            return;
        }
/*
        Intent discoverableIntent = new
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
*/

    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        String address = device.getAddress();

        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected( BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        try {
            mConnectedThread = new ConnectedThread(device);
            //check if there is a connection error
            if (getState() != STATE_NOBT) {
                mConnectedThread.start();
                setState(STATE_CONNECTED);
            }
        } catch (OutOfMemoryError error){
            Log.d(TAG,error.getMessage());
            System.exit(0);     //stop the application, it will restart automatically.
        }


    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_NOBT);

    }

    /**
    * Indicate that the connection was lost and notify the UI Activity.
    */
    private void connectionLost() {

        // Send a failure message back to the Activity
        setState(STATE_LOST);


    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        send(Message.obtain(null, state, null));

    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }


    /**
    * Make an explicit pairing of BT
    *
    * 
    */
   private BluetoothDevice pairedBT(){
   	
   	BluetoothAdapter bluetoothAdapter;
   	bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
   	BluetoothDevice myDevice = null;
   	
   	
	myDevice  = bluetoothAdapter.getRemoteDevice(macAddress);

	return myDevice;
   	   	
   }

    private class AcceptThread extends Thread{

//        private final BluetoothSocket mmServerSocket;
        private BluetoothDevice mmDevice;

        public AcceptThread() {
            BluetoothDevice tmp = null;
            
            
            tmp = pairedBT();
           
            // Create a new listening server socket
            
 //           try {
//                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID_SECURE);
 //           } catch (IOException e) {
 //               Log.e(TAG, "listen() failed", e);
//            }
            mmDevice = tmp;
        }
        public void run() {
            BluetoothSocket socket = null;
            while (mState != STATE_CONNECTED && mmDevice != null) {

                    synchronized (BTServerAsync.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                    // Situation normal. Start the connected thread.
                                connected(mmDevice);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                    // Either not ready or already connected. Terminate new socket.
                                try {
                                    if (socket != null) socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                            }
                        }
            }
        }

        public void cancel() {
            if (D) Log.d(TAG, "cancel accept Thread " + this);
            mmDevice = null;
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private  BluetoothSocket mmSocket;
        private  BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            
            

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            if (D) Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");


            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG,"unable to make a new connection to BT Socket:" + e);
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                BTServerAsync.this.startBTServer();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BTServerAsync.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmDevice);
        }

        public void cancel() {
            try {
                if (D) Log.d(TAG,"Connect thread cancelled");
                if (mmSocket!=null) mmSocket.close();
                mmSocket = null;
                mmDevice = null;
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {

        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;


        public ConnectedThread(BluetoothDevice device) {
            if (D) Log.d(TAG, "create ConnectedThread");

            mmSocket = null;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams

            try {
                // Always cancel discovery because it will slow down a connection
                if (mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();

                mmSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_SECURE);
                if (D) Log.d(TAG, "Connected to " + mmSocket.getRemoteDevice().getName());
                mmSocket.connect();
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();

            } catch (IOException e) {
                if (D) Log.e(TAG, "temp sockets not created");
            } catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block

				e.printStackTrace();
			} catch (NullPointerException e){
                if (D) Log.e(TAG, "Null pointer exception");

            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            if (mmInStream == null) {
                connectionFailed();
                return;
            }

            if (D) Log.i(TAG, "BEGIN mConnectedThread");
            setState(STATE_ACTIVE);
            byte[] buffer = new byte[1024];
            int bytes = 0;

            // Keep listening to the InputStream while connected
            while (true) {
                try {

                	int current = bytes;
                    if (D) Log.d(TAG, "do-while -- current: " + current);
                    
                    //blocking read
                    bytes = mmInStream.read(buffer);

                    if (D) Log.d(TAG, "bytesRead: =" + bytes);

                    if (bytes >= 0) current += bytes;

                    String scannedBarcode = new String(buffer).substring(0,current);
                    if (D) Log.d(TAG,"Scanned Data: " + scannedBarcode);

                    send(Message.obtain(null, STATE_BARCODE, scannedBarcode));

                    bytes = 0;


                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionFailed();
                    break;
                } catch (NullPointerException e){
                    Log.e(TAG, "failed connection", e);
                    connectionFailed();
                	break;
                }
            }
            try {
                mmInStream.close();
                mmOutStream.close();
                mmSocket.close();
            } catch(IOException e){

            }
        }

        /**
        * Write to the connected OutStream.
        *
                * @param buffer The bytes to write
        */

        public void write(byte[] buffer) {
            try {
                BufferedOutputStream chunck = new BufferedOutputStream(mmOutStream,1008);

                chunck.write(buffer);
                chunck.flush();


            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }


        public void cancel() {
            try {
                if (D) Log.d(TAG,"Connect thread cancelled");
                if (mmSocket!=null) mmSocket.close();
                if (mmInStream != null) mmInStream.close();
                if (mmOutStream != null) mmOutStream.close();

            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }



}
