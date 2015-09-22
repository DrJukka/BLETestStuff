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
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by juksilve on 9.7.2015.
 */
public class BLEValueReader {
    private final BLEValueReader that = this;


    // 180 second after we saw last peer, we could determine that we have seen all we have around us
    // with devices doing advertising, we need to keep this timeout long, since there are such a many errors happening
    // with devices only doing scanning we could drop this at least to 60 seconds, maybe even smaller
    private final CountDownTimer peerDiscoveryTimer = new CountDownTimer(60000, 1000) {
        public void onTick(long millisUntilFinished) {}

        public void onFinish() {
            if (connectBack != null) {
                connectBack.gotServicesList(myServiceList);
            }

            if(myServiceList.size() > 0){
                peerDiscoveryTimer.start();
            }

            // as we just told what we see now, we clear the list, so we can have new view on next update
            myServiceList.clear();
            // we might have a device on the list we can not connect to,
            // thus lets clear the list to take them out
            myDeviceList.clear();

            Log.i("BLEValueReader", "gotServicesList called ");
        }
    };

    // list of all devices we have discovered so far, so we can use previous info if we see same peer again
    private final CopyOnWriteArrayList<ServiceItem> myDevicesDiscoveredList = new CopyOnWriteArrayList<ServiceItem>();

    //currently seen list, that gets cleared everytime we give current list out
    private final CopyOnWriteArrayList<ServiceItem> myServiceList = new CopyOnWriteArrayList<ServiceItem>();

    //devices seen by scanner, which have not been yet discovered
    private final CopyOnWriteArrayList<BluetoothDevice> myDeviceList = new CopyOnWriteArrayList<BluetoothDevice>();

    private final Context context;
    private final DiscoveryCallback connectBack;
    private final BluetoothAdapter btAdapter;
    private final Handler mHandler;
    private BluetoothGatt bluetoothGatt = null;

    public BLEValueReader(Context Context, DiscoveryCallback CallBack, BluetoothAdapter adapter) {
        this.context = Context;
        this.connectBack = CallBack;
        this.btAdapter = adapter;
        this.mHandler = new Handler(this.context.getMainLooper());
    }

    public void Stop() {
        myDeviceList.clear();
        myServiceList.clear();
        myDevicesDiscoveredList.clear();
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

        // then whether we actually already did process the device
        for (ServiceItem item : myServiceList) {
            if (item != null && item.deviceAddress.equalsIgnoreCase(device.getAddress())) {
                return;
            }
        }

        /*
        // its not on currently seen list, thus lets check whether we have seen it earlier
        for(ServiceItem foundOne: myDevicesDiscoveredList){
            if(foundOne != null && foundOne.deviceAddress.equalsIgnoreCase(device.getAddress())) {

                long ageOfDiscovery = (System.currentTimeMillis() - foundOne.discoveredTime);
                if (ageOfDiscovery > 86400000)//more than 24 hours old
                {
                    // this service was discovered over 24 hours ago, so lets go and re-fresh it
                    myDevicesDiscoveredList.remove(foundOne);
                    break;
                }else {
                    // we have discovered this earlier, so we can use values from there
                    // and we don't need to do new connection rounds
                    myServiceList.add(foundOne);

                    // then do remember to inform that we have found a peer
                    if (that.connectBack != null) {
                        that.connectBack.foundService(foundOne);
                    }
                    //we do the full list update with timeout after we have discovered last peer we see
                    //thus we also need to do this in here.
                    restartFullListTimer();
                    return;
                }
            }
        }*/
        //removed for debugging purposes

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
                    that.bluetoothGatt = tmpDevice.connectGatt(that.context, false,  myBluetoothGattCallback);
                } else {
                    Log.i("BLEValueReader", "All devices processed");
                }
                alreadyDoingConnect = false;
            }
        }, 1000);
    }

    private void PeerDisoveryFinished(String error) {

        long nextRoundDelay = 1000;
        if (error.length() > 0) {
            Log.i("BLEValueReader", "PeerDisoveryFinished  err: " + error);

            // with error situations, we should have longer delay here
            nextRoundDelay = 10000;
        } else {
            Log.i("BLEValueReader", "PeerDisoveryFinished OK");
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BluetoothGatt tmpgat = that.bluetoothGatt;
                that.bluetoothGatt = null;
                if (tmpgat != null) {
                    tmpgat.close();
                }

                peerDiscoveryTimer.cancel();
                peerDiscoveryTimer.start();
                doNextRound();
            }
        },nextRoundDelay);
    }

    private void PeerDisovered(ServiceItem peer) {

        Log.i("BLEValueReader", "PeerDisovered : " + peer.peerName);

        // lets first add it to the Currently discovered list
        // which gets fired in timeout, and plugin will get idea of all device we currently see
        boolean alreadyDiscovered = false;
        for (ServiceItem item : that.myServiceList) {
            if (item != null && item.deviceAddress.equalsIgnoreCase(peer.deviceAddress)) {
                alreadyDiscovered = true;
                break;
            }
        }
        if (!alreadyDiscovered) {
            // we need to save the peer, so we can determine devices that went away with timer.
            that.myServiceList.add(peer);
        }

        //lets then also cache all peers we find, so we don't need to poll them again
        //this saves battery and reduces errors, since we do loads less work
        boolean alreadyInTheList = false;
        for (ServiceItem foundOne : that.myDevicesDiscoveredList) {
            if (foundOne != null && foundOne.deviceAddress.equalsIgnoreCase(peer.deviceAddress)) {
                alreadyInTheList = true;
                break;
            }
        }

        if (!alreadyInTheList) {
            //see whether we had it there with other BLE address, i.e. it was re-started
            for (ServiceItem foundOne : that.myDevicesDiscoveredList) {
                if (foundOne != null && foundOne.peerAddress.equalsIgnoreCase(peer.peerAddress)) {
                    that.myDevicesDiscoveredList.remove(foundOne);
                }
            }
            that.myDevicesDiscoveredList.add(peer);
        }

        //lets also remember to remove it from our current device to discover list
        // so we wont be processing this again
        for (BluetoothDevice device : that.myDeviceList) {
            if (device != null) {
                if (device.getAddress().equalsIgnoreCase(peer.deviceAddress)) {
                    that.myDeviceList.remove(device);
                    break;
                }
            }
        }

        // then do remember to inform that we have found a peer
        if (that.connectBack != null) {
            that.connectBack.foundService(peer);
        }
    }

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
    enum State {
        Idle,
        Connecting,
        Discovering,
        Reading
    }

    private int errorCounter = 0;
    private int roundCounter = 0;
    private State myState = State.Idle;

    final BluetoothGattCallback  myBluetoothGattCallback = new BluetoothGattCallback() {

        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (status != BluetoothGatt.GATT_SUCCESS) {
                errorCounter++;
                if (that.connectBack != null) {
                    that.connectBack.debugData("CST err: " + status + ", c:" + errorCounter + ", rounds: " + roundCounter);
                }
            }

            Log.i("MY-GattCallback", "onConnectionStateChange status : " + BLEBase.getGATTStatusAsString(status) + ", state: " + BLEBase.getConnectionStateAsString(newState));

            //if we fail to get anything started in here, then we'll do the next round with timeout timer
            switch (newState) {
                case BluetoothProfile.STATE_DISCONNECTED:
                    myState = State.Idle;
                    Log.i("MY-GattCallback", "we are disconnected");
                    if (gatt != null) {
                        gatt.close();
                    }
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        PeerDisoveryFinished("Disconnected : " + BLEBase.getGATTStatusAsString(status));
                    }else{
                        PeerDisoveryFinished("");
                    }
                    //tell back that we are done now
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    if (!gatt.discoverServices()) {
                        myState = State.Idle;
                        Log.i("MY-GattCallback", "discoverServices return FALSE");
                        that.connectBack.debugData("discoverServices return FALSE");
                        gatt.disconnect();
                        return;
                    }
                    Log.i("MY-GattCallback", "discoverServices to : " + that.bluetoothGatt.getDevice().getAddress());
                    myState = State.Discovering;
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                case BluetoothProfile.STATE_DISCONNECTING:
                default:
                    // we can ignore any other than actual connected/disconnected state
                    break;
            }
        }

        /*
        0x81 / 129-
         */

        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {

            if (gatt == null) {
                //tell back that we are done now
                PeerDisoveryFinished("onServicesDiscovered given NULL Gatt");
                return;
            }

            if (gatt.getDevice() == null) {
                gatt.disconnect();
                return;
            }

            Log.i("MY-GattCallback", "onServicesDiscovered device: " + gatt.getDevice().getAddress() + ", status : " + BLEBase.getGATTStatusAsString(status));

            List<BluetoothGattService> services = gatt.getServices();
            if (services != null) {
                for (BluetoothGattService item : services) {
                    outerLoop:
                    if (item != null && item.getUuid().toString().equalsIgnoreCase(BLEBase.SERVICE_UUID_1)) {
                        List<BluetoothGattCharacteristic> charList = item.getCharacteristics();
                        if (charList != null) {
                            for (BluetoothGattCharacteristic charItem : charList) {
                                if (charItem != null && charItem.getUuid().toString().equalsIgnoreCase(BLEBase.CharacteristicsUID1)) {
                                    if (!gatt.readCharacteristic(charItem)) {
                                        Log.i("MY-GattCallback", "readCharacteristic return FALSE");
                                        myState = State.Idle;
                                        gatt.disconnect();
                                        return;
                                    }

                                    Log.i("MY-GattCallback", "readCharacteristic to : " + gatt.getDevice().getAddress());
                                    myState = State.Reading;
                                    break outerLoop;
                                }
                            }
                        }
                    }
                }
            }
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (gatt == null) {
                //tell back that we are done now
                PeerDisoveryFinished("onCharacteristicRead given NULL Gatt");
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
                that.PeerDisovered(tmpSrv);

            } catch (JSONException e) {
                Log.i("MY-GattCallback", "Decrypting instance failed , :" + e.toString());
            }

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
