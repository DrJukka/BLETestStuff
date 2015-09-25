// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package com.example.myapplication2.app;

/**
 * Created by juksilve on 12.3.2015.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;


class BTListenerThread extends Thread implements BTHandShakeSocketTread.HandShakeCallback {

    private final BTListenerThread that = this;

    public interface  BtListenCallback{
        void GotConnection(BluetoothSocket socket, String peerId, String peerName, String peerAddress);
        void ListeningFailed(String reason);
    }

    private final CopyOnWriteArrayList<BTHandShakeSocketTread> mBTHandShakerList = new CopyOnWriteArrayList<BTHandShakeSocketTread>();
    private final String mInstanceString;

    private final BtListenCallback callback;
    private final BluetoothServerSocket mSocket;

    private boolean mStopped = false;

    public BTListenerThread(BtListenCallback Callback,BluetoothAdapter bta,UUID BtUuid, String btName, String InstanceString)  throws IOException {
        callback = Callback;
        mInstanceString = InstanceString;
        mSocket = bta.listenUsingInsecureRfcommWithServiceRecord(btName, BtUuid);
    }

    public void run() {
        //    while (!this.interrupted()) {
        if (callback == null || mSocket == null) {
            Log.i("BTListenerThread", "failed to run");
            return;
        }
        Log.i("BTListenerThread", "starting to listen");

        while (!mStopped) {
            try {
                Log.i("BTListenerThread", "waiting to accept incoming Connection");
                BluetoothSocket acceptedSocket = mSocket.accept();
                if (acceptedSocket != null) {
                    Log.i("BTListenerThread", "we got incoming connection");
                    BTHandShakeSocketTread handShake = new BTHandShakeSocketTread(acceptedSocket, this);
                    mBTHandShakerList.add(handShake);

                    handShake.setDefaultUncaughtExceptionHandler(that.getUncaughtExceptionHandler());
                    handShake.start();
                } else if (!mStopped) {
                    mStopped = true;
                    callback.ListeningFailed("Socket is null");
                }
            } catch (IOException e) {
                if (!mStopped) {
                    mStopped = true;
                    //return failure
                    Log.i("BTListenerThread", "accept socket failed: " + e.toString());
                    callback.ListeningFailed(e.toString());
                }
            }
        }
    }

    private void HandShakeOk(BluetoothSocket socket, String peerId, String peerName, String peerAddress){
        callback.GotConnection(socket, peerId,peerName,peerAddress);
    }

    private void HandShakeFailed(String reason) {
        Log.i("BTListenerThread", "HandShakeFailed: " + reason);
       // callback.ListeningFailed("handshake: " + reason);
    }

    public void Stop() {
        Log.i("BTListenerThread", "Stopped");

        for(BTHandShakeSocketTread tmp : mBTHandShakerList) {
            if(tmp != null) {
                tmp.CloseSocket();
            }
        }
        mBTHandShakerList.clear();
        mStopped = true;
        try {
            if(mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            Log.i("BTListenerThread", "closing socket failed: " + e.toString());
        }
    }

    @Override
    public void handShakeMessageRead(byte[] buffer, int size, BTHandShakeSocketTread who) {
        Log.i("BTListenerThread", "got MESSAGE_READ " + size + " bytes.");

        try {
            String JsonLine = new String(buffer);
            Log.i("BTListenerThread", "Got JSON from encryption:" + JsonLine);
            JSONObject jObject = new JSONObject(JsonLine);

            //set that we got the identifications right from remote peer
            who.setPeerId(jObject.getString(MainActivity.JSON_ID_PEERID));
            who.setPeerName(jObject.getString(MainActivity.JSON_ID_PEERNAME));
            who.setPeerAddress(jObject.getString(MainActivity.JSON_ID_BTADRRES));

            //and lets return our identification back to the remote peer
            who.write(mInstanceString.getBytes());

        } catch (JSONException e) {
            for(BTHandShakeSocketTread tmp : mBTHandShakerList) {
                if(tmp != null && tmp.getId() == who.getId()) {
                    mBTHandShakerList.remove(tmp);
                    tmp.CloseSocket();
                    HandShakeFailed("Decrypting instance failed , :" + e.toString());
                    return;
                }
            }
        }
    }

    @Override
    public void handShakeMessageWrite(byte[] buffer, int size, BTHandShakeSocketTread who) {
        Log.i("BTListenerThread", "MESSAGE_WRITE " + size + " bytes.");
        for(BTHandShakeSocketTread tmp : mBTHandShakerList) {
            if(tmp != null && tmp.getId() == who.getId()) {
                mBTHandShakerList.remove(tmp);
                //tmp.interrupt();
                break;
            }
        }

        HandShakeOk(who.getSocket(),who.getPeerId(),who.getPeerName(),who.getPeerAddress());
    }

    @Override
    public void handShakeDisconnected(String error, BTHandShakeSocketTread who) {
        // if we get disconnected after we were succccesfull,
        // then the who is not on the list anymore
        // so if it is there, then we have error situation
        for(BTHandShakeSocketTread tmp : mBTHandShakerList) {
            if(tmp != null && tmp.getId() == who.getId()) {
                mBTHandShakerList.remove(tmp);
                tmp.CloseSocket();
                HandShakeFailed("SOCKET_DISCONNECTED");
                return;
            }
        }
    }
}