// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package com.example.myapplication2.app;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.CountDownTimer;

import android.util.Log;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by juksilve on 22.06.2015.
 */
public class BTConnector_Discovery implements AdvertiserCallback {

    private final BTConnector_Discovery that = this;
    private BLEScannerKitKat mSearchKitKat = null;
    private BLEScannerLollipop mBLEScannerLollipop = null;
    private BLEAdvertiserLollipop mBLEAdvertiserLollipop = null;

    private final Context context;
    private final String mSERVICE_TYPE;
    private final String mInstanceString;

    private final DiscoveryCallback callback;
    private final BluetoothManager mBluetoothManager;
    private final BluetoothGattService mFirstService;

    // list of all devices we have discovered so far, so we can use previous info if we see same peer again
    private final CopyOnWriteArrayList<ServiceItem> myDevicesDiscoveredList = new CopyOnWriteArrayList<ServiceItem>();

    //currently seen list, that gets cleared everytime we give current list out
    private final CopyOnWriteArrayList<ServiceItem> myServiceList = new CopyOnWriteArrayList<ServiceItem>();

    // 180 second after we saw last peer, we could determine that we have seen all we have around us
    // with devices doing advertising, we need to keep this timeout long, since there are such a many errors happening
    // with devices only doing scanning we could drop this at least to 60 seconds, maybe even smaller
    private final CountDownTimer peerDiscoveryTimer = new CountDownTimer(60000, 1000) {

        public void onTick(long millisUntilFinished) {}

        public void onFinish() {

            that.callback.gotServicesList(myServiceList);

            if(myServiceList.size() > 0){
                peerDiscoveryTimer.start();
            }

            Log.i("BLEValueReader", "gotServicesList called ");

            // do restart the scanners with clear list
            BLEScannerKitKat tmpScanner = mSearchKitKat;
            if(tmpScanner != null){
                tmpScanner.reStartScanning();
            }

            BLEScannerLollipop tmpLollipopScan = mBLEScannerLollipop;
            if(tmpLollipopScan != null){
                tmpLollipopScan.reStartScanning();
            }

            // as we just told what we see now, we clear the list, so we can have new view on next update
            myServiceList.clear();
        }
    };

    public BTConnector_Discovery(Context Context, DiscoveryCallback Callback, String ServiceType, String instanceLine){
        this.context = Context;
        this.mSERVICE_TYPE = ServiceType;
        this.callback = Callback;
        this.mInstanceString = instanceLine;
        this.mBluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mFirstService = new BluetoothGattService(UUID.fromString(BLEBase.SERVICE_UUID_1),BluetoothGattService.SERVICE_TYPE_PRIMARY);


        BluetoothGattCharacteristic firstServiceChar = new BluetoothGattCharacteristic(UUID.fromString(BLEBase.CharacteristicsUID1),BluetoothGattCharacteristic.PROPERTY_READ,BluetoothGattCharacteristic.PERMISSION_READ );
        firstServiceChar.setValue(instanceLine.getBytes());

        BluetoothGattCharacteristic secondServiceChar = new BluetoothGattCharacteristic(UUID.fromString(BLEBase.CharacteristicsUID2),BluetoothGattCharacteristic.PROPERTY_WRITE,BluetoothGattCharacteristic.PERMISSION_WRITE );

        this.mFirstService.addCharacteristic(firstServiceChar);
        this.mFirstService.addCharacteristic(secondServiceChar);
    }

    public void Start() {
        Log.i("Connector_Discovery", "starting-");
        StartAdvertiser();
        StartScanner();
        this.callback.StateChanged(DiscoveryCallback.State.DiscoveryFindingPeers);
    }

    public void Stop() {
        Log.i("Connector_Discovery", "Stopping");
        StopAdvertiser();
        StopScanner();
        this.callback.StateChanged(DiscoveryCallback.State.DiscoveryIdle);
        myDevicesDiscoveredList.clear();
        myServiceList.clear();
    }

    private void StartAdvertiser(){
        StopAdvertiser();

        //API is not available before Lollipop
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BLEAdvertiserLollipop tmpAdvertiserLollipop = new BLEAdvertiserLollipop(that.context, that, mBluetoothManager);
            tmpAdvertiserLollipop.addService(mFirstService);
            tmpAdvertiserLollipop.Start(mBluetoothManager.getAdapter().getAddress());
            mBLEAdvertiserLollipop = tmpAdvertiserLollipop;
        }
    }

    private void StopAdvertiser(){
        BLEAdvertiserLollipop tmpAdvertiser = mBLEAdvertiserLollipop;
        mBLEAdvertiserLollipop = null;
        if(tmpAdvertiser != null){
            tmpAdvertiser.Stop();
        }
    }

    private void StartScanner() {
        StopScanner();

        //API is not available before Lollipop
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.i("Connector_Discovery", "Start Lollipop scanner");
            BLEScannerLollipop tmpScannerLollipop = new BLEScannerLollipop(that.context, that, that.mBluetoothManager,this.mInstanceString);
            tmpScannerLollipop.Start();
            mBLEScannerLollipop= tmpScannerLollipop;

        }else {
            Log.i("Connector_Discovery", "Start kitkat scanner");
            BLEScannerKitKat tmpScannerKitKat = new BLEScannerKitKat(that.context, that, that.mBluetoothManager,this.mInstanceString);
            tmpScannerKitKat.Start();
            mSearchKitKat = tmpScannerKitKat;
        }

    }

    private void StopScanner() {
        BLEScannerKitKat tmpScanner = mSearchKitKat;
        mSearchKitKat = null;
        if(tmpScanner != null){
            tmpScanner.Stop();
        }

        BLEScannerLollipop tmpLollipopScan = mBLEScannerLollipop;
        mBLEScannerLollipop = null;
        if(tmpLollipopScan != null){
            tmpLollipopScan.Stop();
        }
    }

    @Override
    public void onAdvertisingStarted(String error) {
        //todo should we have a way on reporting advertising prolems ?
        Log.i("Connector_Discovery", "Started err : " + error);
    }

    @Override
    public void onAdvertisingStopped(String error) {
        Log.i("Connector_Discovery", "Stopped err : " + error);
    }

    @Override
    public void PeerDiscovered(ServiceItem peer,boolean cachedValue) {

        Log.i("Connector_Discovery", "PeerDiscovered : " + peer.peerName);

        //lets first tell upstairs that we found one
        that.callback.foundService(peer);

        // then first add it to the Currently discovered list
        // which gets fired in timeout, and plugin will get idea of all device we currently see
        if (isPeerDiscovered(peer.deviceAddress)) {
            // we have seen this before
            return;
        }
        that.myServiceList.add(peer);

        //lets reset the full-list timeout here
        peerDiscoveryTimer.cancel();
        peerDiscoveryTimer.start();

        // we need to save the peer, so we can determine devices that went away with timer.
        if(cachedValue){
            // this is already cached, so no need to check
            return;
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
    }

    @Override
    public boolean isPeerDiscovered(String deviceAddress) {

        for (ServiceItem item : that.myServiceList) {
            if (item != null && item.deviceAddress.equalsIgnoreCase(deviceAddress)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ServiceItem haveWeSeenPeerEarlier(final BluetoothDevice device) {

        for (ServiceItem foundOne : myDevicesDiscoveredList) {
            if (foundOne != null && foundOne.deviceAddress.equalsIgnoreCase(device.getAddress())) {
                long ageOfDiscovery = (System.currentTimeMillis() - foundOne.discoveredTime);
                if (ageOfDiscovery > 86400000){//more than 24 hours old
                    // this service was discovered over 24 hours ago, so lets go and re-fresh it
                    myDevicesDiscoveredList.remove(foundOne);
                    return null;
                }

                return foundOne;
            }
        }

        return null;
    }

    @Override
    public void debugData(String data) {
        that.callback.debugData(data);
    }
}
