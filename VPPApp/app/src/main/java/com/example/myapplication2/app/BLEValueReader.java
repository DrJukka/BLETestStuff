package com.example.myapplication2.app;

import android.bluetooth.*;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by juksilve on 9.7.2015.
 */
/*
    https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/android-4.4.2_r1/stack/include/gatt_api.h

    0x08 / 8  : connection timeout
    - very few of these seen, appears to be ok, recovers on next call

    0x13 / 19 : connection terminate by peer user
    - very few of these seen, appears to be ok, recovers on next call

    0x16 / 22 : connection terminated by local host
    - https://code.google.com/p/android/issues/detail?id=180440, I have seen Samsung to recover from this normally

    0x85 / 133 GATT_ERROR
    - most common error seen, can be upto 20-25 % of all connections ending with this error
    - needs long delay before next call in order to recover from
    - https://code.google.com/p/android/issues/detail?id=156730
    - https://code.google.com/p/android/issues/detail?id=58381

    Lollipop 133
    - http://stackoverflow.com/questions/28018722/android-could-not-connect-to-bluetooth-device-on-lollipop

    nice summary:
    - http://stackoverflow.com/questions/17870189/android-4-3-bluetooth-low-energy-unstable

    Additionally older

     */

public class BLEValueReader implements BTListenerThread.BtListenCallback, BTConnectToThread.BtConnectToCallback{
    private final BLEValueReader that = this;

    private int errorCounter = 0;
    private int roundCounter = 0;

    private BTListenerThread mBTListenerThread = null;
    private BTConnectToThread mBTConnectToThread = null;

    private final CountDownTimer errorTimeOutTimer = new CountDownTimer(20000, 1000) {
        public void onTick(long millisUntilFinished) {}
        public void onFinish() {
            Log.i("BLEValueReader", "errorTimeOutTimer");
            doNextRound();
        }
    };

    // I have seen that gatt connect can sometimes result none of the callbacks being called,
    // thus we do need to cancel them if this happens
    private final CountDownTimer deviceDiscoveryTimeoutTimer = new CountDownTimer(60000, 1000) {
        public void onTick(long millisUntilFinished) { }
        public void onFinish() {
            //we got timeout, thus lets go for next round
            Log.i("BLEValueReader", "deviceDiscoveryTimeoutTimer");
            doNextRound();
        }
    };

    class peerToFindItem{
        public final BluetoothDevice device;
        public final String address;
        private int tryCount;
        public peerToFindItem(BluetoothDevice device,String address){
            this.device = device;
            this.address = address;
            this.tryCount = 0;
        }
        public void increaseTryCount(){ this.tryCount ++;}
        public int getTryCount(){ return tryCount;}
    }

    private peerToFindItem nowDiscoveringDevice = null;
    private peerToFindItem nextDeviceToDiscover = null;
    private final Context context;
    private final PeerDiscoveredCallback connectBack;
    private final BluetoothAdapter btAdapter;
    private final Handler mHandler;
    private BluetoothGatt bluetoothGatt = null;
    private final String mInstanceString;

    public BLEValueReader(Context Context, PeerDiscoveredCallback CallBack, BluetoothAdapter adapter,String instanceString) {
        this.context = Context;
        this.connectBack = CallBack;
        this.btAdapter = adapter;
        this.mInstanceString = instanceString;
        this.mHandler = new Handler(this.context.getMainLooper());
        StartListening();
    }

    public void Stop() {
        errorTimeOutTimer.cancel();
        deviceDiscoveryTimeoutTimer.cancel();

        BluetoothGatt tmpgat = bluetoothGatt;
        if (tmpgat != null) {
            tmpgat.disconnect();
        }

        BTListenerThread tmpListener = mBTListenerThread;
        mBTListenerThread = null;
        if (tmpListener != null) {
            tmpListener.Stop();
        }

        BTConnectToThread tmoConnector = mBTConnectToThread;
        mBTConnectToThread = null;
        if (tmoConnector != null) {
            tmoConnector.Stop();
        }
    }

    public void AddDevice(final BluetoothDevice device,String bluetoothAddress) {
        if(device == null){
            return;
        }

        peerToFindItem tmpDevice = nowDiscoveringDevice;
        if(tmpDevice != null && tmpDevice.device != null) {
            if(tmpDevice.device.getAddress().equalsIgnoreCase(device.getAddress())){
                //Avoid re-trying same device
                return;
            }
        }

        peerToFindItem tmpDevice2 = nextDeviceToDiscover;
        if(tmpDevice2 != null && tmpDevice2.device != null) {
            if(tmpDevice2.device.getAddress().equalsIgnoreCase(device.getAddress())){
                //Avoid re-creating instance for  same device
                return;
            }
        }

        Log.i("BLEValueReader", "Add to device list " + device.getAddress());
        nextDeviceToDiscover = new peerToFindItem(device,bluetoothAddress);

        if (that.bluetoothGatt != null || mBTConnectToThread != null) {
            //we are already running a discovery on peer
            // we'll do this one later0
            // we do need a timeout that cancels stuff if things goes bad here
            return;
        }

        doNextRound();
    }

    //needed to avoid cancelling connects while we have one already going on
    private boolean alreadyDoingConnect = false;

    private void doNextRound() {

        // if we get doNextRound twice before handler has started, we need to stop here
        if (alreadyDoingConnect) {
            return;
        }
        alreadyDoingConnect = true;

        //make sure we don't get the timeouts called for previous discovery
        errorTimeOutTimer.cancel();
        deviceDiscoveryTimeoutTimer.cancel();

        //see that we really get this closed
        BluetoothGatt tmpGatt = that.bluetoothGatt;
        that.bluetoothGatt = null;
        if (tmpGatt != null) {
            tmpGatt.close();
        }

        // as well as our previous Bluetooth discovery is really closed properly
        BTConnectToThread tmoConnector = mBTConnectToThread;
        mBTConnectToThread = null;
        if (tmoConnector != null) {
            tmoConnector.Stop();
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i("BLEValueReader", "Connecting in called context");

                if (that.connectBack == null || that.bluetoothGatt != null) {
                    alreadyDoingConnect = false;
                    return;
                }
                nowDiscoveringDevice = nextDeviceToDiscover;
                nextDeviceToDiscover = null;
                if (nowDiscoveringDevice != null && nowDiscoveringDevice.device != null) {
                    //do connection to the selected device
                    Log.i("BLEValueReader", "Connecting to next device : " + nowDiscoveringDevice.device.getAddress());

                    // in theory, we could get at least 7 outgoing connections same time
                    // then likely we would need to separate instances of callback for each
                    deviceDiscoveryTimeoutTimer.start();
                    that.bluetoothGatt = nowDiscoveringDevice.device.connectGatt(that.context, false, myBluetoothGattCallback);
                } else {
                    Log.i("BLEValueReader", "All devices processed");
                }
                alreadyDoingConnect = false;
            }
        }, 1000);
    }

    private void PeerDiscoveryFinished(String error) {
        deviceDiscoveryTimeoutTimer.cancel();

        if (error.length() > 0) {
            Log.i("BLEValueReader", "PeerDiscovery Finished  err: " + error);
            // with error situations, we should have longer delay here
            errorTimeOutTimer.start();

            // if you would just use BLE discovery, then just add return in here

            //BLE discovery failed, thus lest try doing it ove insecure Bluetooth connection
            if(!StartConnection()){
                //failed to start connection, thus lets let the timeout run
                return;
            }

            // we can now cancel the error timer since we continued the discovery with BT-Insecure connection
            errorTimeOutTimer.cancel();
            return;
        }

        roundCounter++;
        Log.i("BLEValueReader", "PeerDiscovery Finished OK");
        BluetoothGatt tmpgat = that.bluetoothGatt;
        that.bluetoothGatt = null;
        if (tmpgat != null) {
            tmpgat.close();
        }

        doNextRound();
    }

    public void StartListening() {

        BTListenerThread tmpList = mBTListenerThread;
        mBTListenerThread = null;
        if (tmpList != null) {
            tmpList.Stop();
        }

        Log.i("BLEValueReader", "StartBluetooth listener");
        try {
            tmpList = new BTListenerThread(that, btAdapter, UUID.fromString(BLEBase.BtDiscoveryUUID), "BLE", this.mInstanceString);
        }catch (IOException e){
            e.printStackTrace();
            Log.i("BLEValueReader", "failed to start listener : " + e.toString());
            // in this point of time we can not accept any incoming connections, thus what should we do ?
            return;
        }
        //tmpList.setDefaultUncaughtExceptionHandler(mThreadUncaughtExceptionHandler);

        Log.i("BLEValueReader", "start now listener");
        tmpList.start();
        mBTListenerThread = tmpList;
    }


    public boolean StartConnection() {

        if (nowDiscoveringDevice == null || (nowDiscoveringDevice.address.length() < 17)) {
            return false;
        }

        nowDiscoveringDevice.increaseTryCount();
        // if we have tried already enough times, we'll then give up
        if(nowDiscoveringDevice.getTryCount() > 3){
            return false;
        }

        //do connection to the selected device
        Log.i("BLEValueReader", "Connecting via BT-insecure to : " + nowDiscoveringDevice.address + ", BLE: " + nowDiscoveringDevice.device.getAddress());

        BluetoothDevice device = null;
        BTConnectToThread tmp = null;
        try {
            device = this.btAdapter.getRemoteDevice(nowDiscoveringDevice.address);
            tmp = new BTConnectToThread(that, device, UUID.fromString(BLEBase.BtDiscoveryUUID), this.mInstanceString);
        } catch (IOException e) {
            // connected thread got error
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            // BT-Address is not formed right
            e.printStackTrace();
            return false;
        }

        //todo do set the DefaultUncaughtExceptionHandler
        //tmp.setDefaultUncaughtExceptionHandler(mThreadUncaughtExceptionHandler);
        tmp.start();
        mBTConnectToThread = tmp;
        Log.i("", "Connecting to " + device.getName() + ", at " + device.getAddress());

        return true;
    }

        @Override
    public void Connected(BluetoothSocket socket, String peerId, String peerName, String peerAddress) {
            if (nowDiscoveringDevice != null && nowDiscoveringDevice.device != null) {
                ServiceItem tmpSrv = new ServiceItem(peerId, peerName, peerAddress, "BTInSecure", nowDiscoveringDevice.device.getAddress(), nowDiscoveringDevice.device.getAddress());
                that.connectBack.PeerDiscovered(tmpSrv, false);
            }

            // the socket would be connected & ok for any data transfer here, but since we got the data already, we can close it now.
            final BluetoothSocket socketTmp = socket;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        socketTmp.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 1000);

            BTConnectToThread tmoConnector = mBTConnectToThread;
            mBTConnectToThread = null;
            if (tmoConnector != null) {
                tmoConnector.Stop();
            }
            Log.i("BLEValueReader", "PeerDiscovery via BTInSecure Finished OK");
            doNextRound();
        }

    @Override
    public void ConnectionFailed(String reason) {
        Log.i("BLEValueReader","ConnectionFailed err: " + reason);
        BTConnectToThread tmoConnector = mBTConnectToThread;
        mBTConnectToThread = null;
        if (tmoConnector != null) {
            tmoConnector.Stop();
        }

        errorTimeOutTimer.start();

        if(!StartConnection()){
            //failed to start connection, thus lets let the timeout run
            return;
        }

        // we can now cancel the error timer since we continued the discovery with BT-Insecure connection
        errorTimeOutTimer.cancel();
    }

    @Override
    public void GotConnection(BluetoothSocket socket, String peerId, String peerName, String peerAddress) {
        Log.i("BLEValueReader", "incoming peer discovery-- id:" + peerId + ", name: " + peerName + ", address: " + peerAddress);

        // we don't know the BLE MAC address of that peer, so we don't want to use the values in here
      //  ServiceItem tmpSrv = new ServiceItem(peerId, peerName, peerAddress, "BTInSecure","N/A","N/A");
      //  that.connectBack.PeerDiscovered(tmpSrv, false);

        final BluetoothSocket socketTmp = socket;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    socketTmp.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }, 1000);
    }

    @Override
    public void ListeningFailed(String reason) {
        Log.i("BLEValueReader", "ListeningFailed err: " + reason);
        StartListening();
    }


    final BluetoothGattCallback  myBluetoothGattCallback = new BluetoothGattCallback() {

        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (status != BluetoothGatt.GATT_SUCCESS) {
                errorCounter++;
                that.connectBack.debugData("CSC err : " + status + ", state: " + BLEBase.getConnectionStateAsString(newState));

                if (gatt != null) {
                    gatt.close();
                }

                PeerDiscoveryFinished("Disconnected : " + BLEBase.getGATTStatusAsString(status));
                return;
            }

            Log.i("MY-GattCallback", "onConnectionStateChange status : " + BLEBase.getGATTStatusAsString(status) + ", state: " + BLEBase.getConnectionStateAsString(newState) + ", Err:" + errorCounter + ", rounds: " + roundCounter);

            //if we fail to get anything started in here, then we'll do the next round with timeout timer
            switch (newState) {
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i("MY-GattCallback", "we are disconnected");
                    if (gatt != null) {
                        gatt.close();
                    }
                    PeerDiscoveryFinished("");
                    //tell back that we are done now
                    return;
                case BluetoothProfile.STATE_CONNECTED:
                    if (!gatt.discoverServices()) {
                        Log.i("MY-GattCallback", "discoverServices return FALSE");
                        gatt.disconnect();
                        return;
                    }
                    Log.i("MY-GattCallback", "discoverServices to : " + that.bluetoothGatt.getDevice().getAddress());
                    return;
                case BluetoothProfile.STATE_CONNECTING:
                case BluetoothProfile.STATE_DISCONNECTING:
                default:
                    // we can ignore any other than actual connected/disconnected state
                    return;
            }
        }

        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {

            if (status != BluetoothGatt.GATT_SUCCESS) {
                that.connectBack.debugData("Disc err : " + status);
            }

            if (gatt == null) {
                //tell back that we are done now
                PeerDiscoveryFinished("onServicesDiscovered given NULL Gatt");
                return;
            }

            if (gatt.getDevice() == null) {
                gatt.disconnect();
                return;
            }

            Log.i("MY-GattCallback", "onServicesDiscovered device: " + gatt.getDevice().getAddress() + ", status : " + BLEBase.getGATTStatusAsString(status));

            List<BluetoothGattService> services = gatt.getServices();
            if (services == null) {
                gatt.disconnect();
                return;
            }
            BluetoothGattCharacteristic readItem = getCharacteristic(services,BLEBase.CharacteristicsUID1);
            if(readItem == null) {
                gatt.disconnect();
                return;
            }

            if (!gatt.readCharacteristic(readItem)) {
                Log.i("MY-GattCallback", "readCharacteristic return FALSE");
                gatt.disconnect();
                return;
            }

            Log.i("MY-GattCallback", "readCharacteristic to : " + gatt.getDevice().getAddress());
        }

        private BluetoothGattCharacteristic getCharacteristic(List<BluetoothGattService> services, String UIDValue){
            for (BluetoothGattService item : services) {
                outerLoop:
                if (item != null && item.getUuid().toString().equalsIgnoreCase(BLEBase.SERVICE_UUID_1)) {
                    List<BluetoothGattCharacteristic> charList = item.getCharacteristics();
                    if (charList != null) {
                        for (BluetoothGattCharacteristic charItem : charList) {
                            if (charItem != null && charItem.getUuid().toString().equalsIgnoreCase(UIDValue)) {
                                return charItem;
                            }
                        }
                    }
                }
            }

            return null;
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status != BluetoothGatt.GATT_SUCCESS) {
                that.connectBack.debugData("Read err : " + status);
            }

            if (gatt == null) {
                //tell back that we are done now
                PeerDiscoveryFinished("onCharacteristicRead given NULL Gatt");
                return;
            }

            if (gatt.getDevice() == null) {
                gatt.disconnect();
                return;
            }

            Log.i("MY-GattCallback", "onCharacteristicRead device: " + gatt.getDevice().getAddress() + ", status : " + BLEBase.getGATTStatusAsString(status));

            if (characteristic == null || characteristic.getValue() == null || characteristic.getValue().length <= 0) {
                //tell back that we are done now
                gatt.disconnect();
                return;
            }

            String jsonString = new String(characteristic.getValue());
            try {
                JSONObject jObject = new JSONObject(jsonString);

                String peerIdentifier = jObject.getString(MainActivity.JSON_ID_PEERID);
                String peerName = jObject.getString(MainActivity.JSON_ID_PEERNAME);
                String peerAddress = jObject.getString(MainActivity.JSON_ID_BTADRRES);

                Log.i("MY-GattCallback", "JsonLine: " + jsonString + " -- peerIdentifier:" + peerIdentifier + ", peerName: " + peerName + ", peerAddress: " + peerAddress);
                ServiceItem tmpSrv = new ServiceItem(peerIdentifier, peerName, peerAddress, "BLE", gatt.getDevice().getAddress(), gatt.getDevice().getName());
                that.connectBack.PeerDiscovered(tmpSrv,false);
                nowDiscoveringDevice = null;

            } catch (JSONException e) {
                Log.i("MY-GattCallback", "Decrypting instance failed , :" + e.toString());
            }

            List<BluetoothGattService> services = gatt.getServices();
            if (services == null) {
                gatt.disconnect();
                return;
            }
            BluetoothGattCharacteristic writeItem = getCharacteristic(services,BLEBase.CharacteristicsUID2);
            if(writeItem == null) {
                gatt.disconnect();
                return;
            }

            writeItem.setValue(that.mInstanceString);

            if (!gatt.writeCharacteristic(writeItem)) {
                Log.i("MY-GattCallback", "writeCharacteristic return FALSE");
                gatt.disconnect();
                return;
            }

            Log.i("MY-GattCallback", "writeCharacteristic to : " + gatt.getDevice().getAddress());
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status != BluetoothGatt.GATT_SUCCESS) {
                that.connectBack.debugData("Write err : " + status);
            }

            Log.i("MY-GattCallback", "onCharacteristicWrite status : " + BLEBase.getGATTStatusAsString(status));

            // we are fully done
            final BluetoothGatt gattTmp = gatt;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    gattTmp.disconnect();
                }
            }, 100);
        }
    };


}
