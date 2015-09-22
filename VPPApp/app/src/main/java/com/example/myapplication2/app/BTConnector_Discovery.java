// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package com.example.myapplication2.app;

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

/**
 * Created by juksilve on 22.06.2015.
 */
public class BTConnector_Discovery implements AdvertiserCallback, DiscoveryCallback {

    private final BTConnector_Discovery that = this;
    private BLEScannerKitKat mSearchKitKat = null;
    private BLEScannerLollipop mBLEScannerLollipop = null;
    private BLEAdvertiserLollipop mBLEAdvertiserLollipop = null;

    private final Context context;
    private final String mSERVICE_TYPE;

    private final DiscoveryCallback callback;
    private final BluetoothManager mBluetoothManager;
    private final BluetoothGattService mFirstService;

    public BTConnector_Discovery(Context Context, DiscoveryCallback Callback, String ServiceType, String instanceLine){
        this.context = Context;
        this.mSERVICE_TYPE = ServiceType;
        this.callback = Callback;
        this.mBluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mFirstService = new BluetoothGattService(UUID.fromString(BLEBase.SERVICE_UUID_1),BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic firstServiceChar = new BluetoothGattCharacteristic(UUID.fromString(BLEBase.CharacteristicsUID1),BluetoothGattCharacteristic.PROPERTY_READ,BluetoothGattCharacteristic.PERMISSION_READ );
        firstServiceChar.setValue(instanceLine.getBytes());

        this.mFirstService.addCharacteristic(firstServiceChar);
    }

    public void Start() {
        Log.i("Connector_Discovery", "starting-");
        StartAdvertiser();
        StartScanner();
        StateChanged(State.DiscoveryFindingPeers);
    }

    public void Stop() {
        Log.i("Connector_Discovery", "Stopping");
        StopAdvertiser();
        StopScanner();
        StateChanged(State.DiscoveryIdle);
    }

    private void StartAdvertiser(){
        StopAdvertiser();

        //API is not available before Lollipop
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BLEAdvertiserLollipop tmpAdvertiserLollipop = new BLEAdvertiserLollipop(that.context, that, mBluetoothManager);
            tmpAdvertiserLollipop.addService(mFirstService);
            tmpAdvertiserLollipop.Start();
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
            debugData("Start Lollipop scanner");
            BLEScannerLollipop tmpScannerLollipop = new BLEScannerLollipop(that.context, that, that.mBluetoothManager);
            tmpScannerLollipop.Start();
            mBLEScannerLollipop= tmpScannerLollipop;

        }else {
            debugData("Start kitkat scanner");
            BLEScannerKitKat tmpScannerKitKat = new BLEScannerKitKat(that.context, that, that.mBluetoothManager);
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

    //we are simply forwarding thw calls for DiscoveryCallback to be handled in the ConnectorLib
    @Override
    public void gotServicesList(List<ServiceItem> list) {
        that.callback.gotServicesList(list);
    }

    @Override
    public void foundService(ServiceItem item) {
        that.callback.foundService(item);
    }

    public void StateChanged(State newState) {

        Log.i("Connector_Discovery", "StateChanged : " + newState);
        switch(newState){
            case DiscoveryIdle:
                that.callback.StateChanged(State.DiscoveryIdle);
                break;
            case DiscoveryNotInitialized:
                that.callback.StateChanged(State.DiscoveryNotInitialized);
                break;
            case DiscoveryFindingPeers:
                that.callback.StateChanged(State.DiscoveryFindingPeers);
                break;
            case DiscoveryFindingServices:
                that.callback.StateChanged(State.DiscoveryFindingServices);
                break;
            default:
                throw new RuntimeException("Invalid value for DiscoveryCallback.State = " + newState);
        }
    }

    @Override
    public void debugData(String data) {
        that.callback.debugData(data);
    }
}
