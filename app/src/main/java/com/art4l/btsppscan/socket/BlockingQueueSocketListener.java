package com.art4l.btsppscan.socket;

import android.os.Message;

import com.art4l.btsppscan.BTSPPScan;
import com.art4l.btsppscan.ScannerThread;
import com.art4l.btsppscan.scandevice.BTSPPScanner;

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockingQueueSocketListener extends Thread{


    BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private ScannerThread mScannerThread;


    public BlockingQueueSocketListener(ScannerThread scannerThread, BlockingQueue<Message> queue) {
        this.queue = queue;
        mScannerThread = scannerThread;
    }

    @Override
    public void run(){
        while(true){
            Message msg;
            while ((msg = queue.poll()) != null) {
                String colorCode = (String)msg.obj;
                mScannerThread.onDisconnected(colorCode,false);

            }
        }
    }


}
