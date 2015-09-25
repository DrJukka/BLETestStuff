// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package com.example.myapplication2.app;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by juksilve on 12.3.2015.
 */
class BTConnectToThread extends Thread implements BTHandShakeSocketTread.HandShakeCallback {

    //todo, remember on plugin integration that I took away the timeout timer
    public interface  BtConnectToCallback{
        void Connected(BluetoothSocket socket, String peerId, String peerName, String peerAddress);
        void ConnectionFailed(String reason);
    }

    private final BTConnectToThread that = this;
    private BTHandShakeSocketTread mBTHandShakeSocketTread = null;
    private final String mInstanceString;
    private final BtConnectToCallback callback;
    private final BluetoothSocket mSocket;

    public BTConnectToThread(BtConnectToCallback Callback, BluetoothDevice device, UUID BtUUID, String InstanceString)  throws IOException {
        callback = Callback;
        mInstanceString = InstanceString;
        mSocket = device.createInsecureRfcommSocketToServiceRecord(BtUUID);

    }
    public void run() {
        Log.i("BTConnectToThread", "Starting to connect");

        try {
            mSocket.connect();
            //return when success

            Log.i("BTConnectToThread", "Starting to handshake");
            mBTHandShakeSocketTread = new BTHandShakeSocketTread(mSocket, this);
            mBTHandShakeSocketTread.setDefaultUncaughtExceptionHandler(that.getUncaughtExceptionHandler());
            mBTHandShakeSocketTread.start();

            mBTHandShakeSocketTread.write(mInstanceString.getBytes());
        } catch (IOException e) {
            Log.i("BTConnectToThread", "socket connect failed: " + e.toString());
            try {
                mSocket.close();
            } catch (IOException ee) {
                Log.i("BTConnectToThread", "closing socket 2 failed: " + ee.toString());
            }
            callback.ConnectionFailed(e.toString());
        }
    }

    private void HandShakeOk(BluetoothSocket socket, String peerId, String peerName, String peerAddress) {
        Log.i("BTConnectToThread", "HandShakeOk : " + peerName);
        mBTHandShakeSocketTread = null;
        // on successful handshake, we'll pass the socket for further processing, so do not close it here
        callback.Connected(socket, peerId,peerName,peerAddress);
    }

    private void HandShakeFailed(String reason) {
        Log.i("BTConnectToThread", "HandShakeFailed : " + reason);
        BTHandShakeSocketTread tmp = mBTHandShakeSocketTread;
        mBTHandShakeSocketTread = null;
        if(tmp != null) {
            tmp.CloseSocket();
        }
        callback.ConnectionFailed("handshake: " + reason);
    }

    public void Stop() {
        BTHandShakeSocketTread tmp = mBTHandShakeSocketTread;
        mBTHandShakeSocketTread = null;
        if(tmp != null) {
            tmp.CloseSocket();
        }
        try {
            mSocket.close();
        } catch (IOException e) {
            Log.i("BTConnectToThread", "closing socket failed: " + e.toString());
        }
    }

    @Override
    public void handShakeMessageRead(byte[] buffer, int size, BTHandShakeSocketTread who) {
        Log.i("BTConnectToThread", "got MESSAGE_READ " + size + " bytes.");
        try {

            String JsonLine = new String(buffer);
            Log.i("BTConnectToThread", "Got JSON from encryption:" + JsonLine);
            JSONObject jObject = new JSONObject(JsonLine);

            //set that we got the identifications right from remote peer
            who.setPeerId(jObject.getString(MainActivity.JSON_ID_PEERID));
            who.setPeerName(jObject.getString(MainActivity.JSON_ID_PEERNAME));
            who.setPeerAddress(jObject.getString(MainActivity.JSON_ID_BTADRRES));

            HandShakeOk(who.getSocket(),who.getPeerId(),who.getPeerName(),who.getPeerAddress());
        } catch (JSONException e) {
            HandShakeFailed("Decrypting instance failed , :" + e.toString());
        }
    }

    @Override
    public void handShakeMessageWrite(byte[] buffer, int size, BTHandShakeSocketTread who) {
        Log.i("BTConnectToThread", "MESSAGE_WRITE " + size + " bytes.");
    }

    @Override
    public void handShakeDisconnected(String error, BTHandShakeSocketTread who) {
        // we got disconnected after we were succccesfull
        if(mBTHandShakeSocketTread != null) {
            HandShakeFailed("SOCKET_DISCONNECTED");
        }
    }
}
