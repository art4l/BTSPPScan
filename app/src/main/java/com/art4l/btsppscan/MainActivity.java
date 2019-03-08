package com.art4l.btsppscan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.art4l.btsppscan.api.ApiService;
import com.art4l.btsppscan.api.NetworkModule;
import com.art4l.btsppscan.api.model.DeviceInitializationDto;
import com.art4l.btsppscan.config.DeviceConfig;
import com.art4l.btsppscan.config.DeviceConfigHelper;
import com.art4l.btsppscan.power.PowerManager;
import com.art4l.btsppscan.scanner.BTServer;
import com.art4l.btsppscan.scanner.ColorCode;
import com.art4l.btsppscan.scanner.ScannerMode;
import com.art4l.btsppscan.socket.CamundaSocket;
import com.art4l.btsppscan.socket.MqttCommand;
import com.art4l.btsppscan.socket.MqttMessage;
import com.art4l.btsppscan.socket.MqttSocket;
import com.art4l.btsppscan.socket.SocketListener;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    public final static int REQUEST_COARSE_LOCATION = 1;
    public static MainActivity mInstance;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean permission;

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.art4l.visualpicking.dev.debug");
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launchIntent);

//        setContentView(R.layout.activity_main);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex){

        }

    }



    @Override
    protected void onResume() {
        super.onResume();

        Timber.d("On Resume");
        //check if the permissions are set to find the BT devices, if not it is handled by onRequestPermissionsResult
        if (checkLocationPermission()){
            //start the scanners
//            startScanners();
        }

        ScannerThread scannerThread = new ScannerThread(this);
        scannerThread.init();

    }

    @Override
    public void onStop() {
        super.onStop();


    }

    @Override
    public void onDestroy(){
        super.onDestroy();

    }

    public void reboot(){
        PowerManager powerManager= new PowerManager();
        powerManager.reboot(1);

    }

    /**
     * Check if the permission is set to look for the BT Devices
     *
     * @return
     */
    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION);
            return false;
        }
        return true;
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {

            case REQUEST_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    break;
                }
            }

        }
    }


}