package com.example.myapplication2.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
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

public class BLEValueReader {
    private final BLEValueReader that = this;

    private final CountDownTimer errorTimeOutTimer = new CountDownTimer(40000, 1000) {
        public void onTick(long millisUntilFinished) {
        }

        public void onFinish() {
            BluetoothGatt tmpgat = that.bluetoothGatt;
            that.bluetoothGatt = null;
            if (tmpgat != null) {
                tmpgat.close();
            }

            doNextRound();
        }
    };

    //devices seen by scanner, which have not been yet discovered
    private final CopyOnWriteArrayList<BluetoothDevice> myDeviceList = new CopyOnWriteArrayList<BluetoothDevice>();

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
    }

    public void Stop() {
        myDeviceList.clear();
        errorTimeOutTimer.cancel();

        BluetoothGatt tmpgat = bluetoothGatt;
        if (tmpgat != null) {
            tmpgat.disconnect();
        }
    }

    public void AddDevice(final BluetoothDevice device) {

        // check whether we already got this added to the to be discovered list
        for (BluetoothDevice addedDevice : myDeviceList) {
            if (addedDevice != null && addedDevice.getAddress().equalsIgnoreCase(device.getAddress())) {
                return;
            }
        }

        Log.i("BLEValueReader", "Add to device list " + device.getAddress());

        // we have new device we  have not discovered earlier
        // thus lets add it to the to be discovered list for further processing
        myDeviceList.add(device);

        if (that.bluetoothGatt != null) {
            //we are already running a discovery on peer
            // we'll do this one later
            return;
        }

        doNextRound();
    }

    //needed to avoid cancelling connects while we have one already going on
    private boolean alreadyDoingConnect = false;

    private void doNextRound() {

        if (alreadyDoingConnect) {
            return;
        }
        alreadyDoingConnect = true;

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i("BLEValueReader", "Connecting in called context");

                if (connectBack == null || bluetoothGatt != null) {
                    alreadyDoingConnect = false;
                    return;
                }

                BluetoothDevice tmpDevice = null;
                // how do I get first index item from the array in thread safe way
                // any alternative than doing this funny loop
                for (BluetoothDevice device : that.myDeviceList) {
                    if (device != null) {
                        if (that.myDeviceList.indexOf(device) == 0) {
                            // get the first, and add it as last, so next round will do different one
                            // we'll remove the device, once we get results for it.
                            that.myDeviceList.remove(device);
                            that.myDeviceList.add(device);
                            tmpDevice = device;
                            break;
                        }
                    }
                }

                if (tmpDevice != null) {
                    //do connection to the selected device
                    Log.i("BLEValueReader", "Connecting to next device : " + tmpDevice.getAddress());

                    // in theory, we could get at least 7 outgoing connections same time
                    // then likely we would need to separate instances of callback for each
                    that.bluetoothGatt = tmpDevice.connectGatt(that.context, false, myBluetoothGattCallback);
                } else {
                    Log.i("BLEValueReader", "All devices processed");
                }
                alreadyDoingConnect = false;
            }
        }, 1000);
    }

    private void PeerDiscoveryFinished(String error) {

        if (error.length() > 0) {
            Log.i("BLEValueReader", "PeerDiscovery Finished  err: " + error);
            // with error situations, we should have longer delay here
            errorTimeOutTimer.start();
            return;
        }

        Log.i("BLEValueReader", "PeerDiscovery Finished OK");
        BluetoothGatt tmpgat = that.bluetoothGatt;
        that.bluetoothGatt = null;
        if (tmpgat != null) {
            tmpgat.close();
        }

        doNextRound();
    }

    private int errorCounter = 0;
    private int roundCounter = 0;

    final BluetoothGattCallback  myBluetoothGattCallback = new BluetoothGattCallback() {

        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (status != BluetoothGatt.GATT_SUCCESS) {
                errorCounter++;
                that.connectBack.debugData("CSC err : " + status);
            }

            Log.i("MY-GattCallback", "onConnectionStateChange status : " + BLEBase.getGATTStatusAsString(status) + ", state: " + BLEBase.getConnectionStateAsString(newState) + ", Err:" + errorCounter + ", rounds: " + roundCounter);

            //if we fail to get anything started in here, then we'll do the next round with timeout timer
            switch (newState) {
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i("MY-GattCallback", "we are disconnected");
                    if (gatt != null) {
                        gatt.close();
                    }
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        PeerDiscoveryFinished("Disconnected : " + BLEBase.getGATTStatusAsString(status));
                    }else{
                        PeerDiscoveryFinished("");
                    }
                    //tell back that we are done now
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    if (!gatt.discoverServices()) {
                        Log.i("MY-GattCallback", "discoverServices return FALSE");
                        gatt.disconnect();
                        return;
                    }
                    Log.i("MY-GattCallback", "discoverServices to : " + that.bluetoothGatt.getDevice().getAddress());
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                case BluetoothProfile.STATE_DISCONNECTING:
                default:
                    // we can ignore any other than actual connected/disconnected state
                    break;
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

                //remove it from the device list
                for (BluetoothDevice device : that.myDeviceList) {
                    if (device != null) {
                        if (device.getAddress().equalsIgnoreCase(tmpSrv.deviceAddress)) {
                            that.myDeviceList.remove(device);
                            //just in case there are accidentally multiple instances
                            //break;
                        }
                    }
                }

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
