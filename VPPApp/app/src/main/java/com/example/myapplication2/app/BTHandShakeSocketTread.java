// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package com.example.myapplication2.app;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by juksilve on 11.3.2015.
 */

class BTHandShakeSocketTread extends Thread {

    public interface HandShakeCallback{
        void handShakeMessageRead(byte[] buffer, int size, BTHandShakeSocketTread who);
        void handShakeMessageWrite(byte[] buffer, int size, BTHandShakeSocketTread who);
        void handShakeDisconnected(String error, BTHandShakeSocketTread who);
    }

    private String peerIdentifier = "";
    private String peerName = "";
    private String peerAddress = "";

    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final HandShakeCallback mHandler;

    public BTHandShakeSocketTread(BluetoothSocket socket, HandShakeCallback handler)  throws IOException {
        Log.i("BTHandShakeSocketTread", "Creating BTHandShakeSocketTread");
        mHandler = handler;
        mmSocket = socket;
        mmInStream = mmSocket.getInputStream();
        mmOutStream = mmSocket.getOutputStream();
    }

    public BluetoothSocket getSocket(){
        return mmSocket;
    }

    public  String getPeerId(){ return peerIdentifier;}
    public  String getPeerName(){ return peerName;}
    public  String getPeerAddress(){ return peerAddress;}
    public  void setPeerId(String value){ peerIdentifier = value;}
    public  void setPeerName(String value ){ peerName = value;}
    public  void setPeerAddress(String value ){ peerAddress = value;}

    public void run() {
        Log.i("BTHandShakeSocketTread", "BTHandShakeSocketTread started");
        byte[] buffer = new byte[255];
        int bytes;

        try {
            bytes = mmInStream.read(buffer);
            //Log.d(TAG, "ConnectedThread read data: " + bytes + " bytes");
            mHandler.handShakeMessageRead(buffer, bytes,this);
        } catch (IOException e) {
            Log.i("BTHandShakeSocketTread", "BTHandShakeSocketTread disconnected: " + e.toString());
            mHandler.handShakeDisconnected(e.toString(),this);
        }
        Log.i("BTHandShakeSocketTread", "BTHandShakeSocketTread fully stopped");
    }
    /**
     * Write to the connected OutStream.
     * @param buffer The bytes to write
     */
    public void write(byte[] buffer) {

        if (mmOutStream == null) {
            return;
        }

        try {
            mmOutStream.write(buffer);
            mHandler.handShakeMessageWrite(buffer, buffer.length,this);
        } catch (IOException e) {
            // when write fails, the timeout for handshake will clear things out eventually.
            Log.i("BTHandShakeSocketTread", "BTHandShakeSocketTread  write failed: " + e.toString());
        }
    }

    public void CloseSocket() {

        if (mmInStream != null) {
            try {mmInStream.close();} catch (IOException e) {e.printStackTrace();}
        }

        if (mmOutStream != null) {
            try {mmOutStream.close();} catch (IOException e) {e.printStackTrace();}
        }

        if (mmSocket != null) {
            try {mmSocket.close();} catch (IOException e) {e.printStackTrace();}
        }
    }
}
