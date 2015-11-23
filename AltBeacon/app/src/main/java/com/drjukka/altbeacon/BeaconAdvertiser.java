package com.drjukka.altbeacon;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.*;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.util.Log;


/**
 * Created by juksilve on 20.4.2015.
 */
@TargetApi(18)
@SuppressLint("NewApi")

public class BeaconAdvertiser {

    private final String TAG = "BeaconAdvertiser";

    private final BeaconAdvertiser that = this;
    private final BeaconAdvertiserCallback callback;
    private final Handler mHandler;
    private final boolean isBLESupported;
    private final BluetoothAdapter mBluetoothAdapter;
    private final String beaconCode;
    private final int referenceRssi;
    private final int manufacturer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private boolean weAreStoppingNow = false;

    public BeaconAdvertiser(Context Context, BeaconAdvertiserCallback CallBack, BluetoothManager btManager,String beaconCode, int referenceRssi, int manufacturer) {
        this.isBLESupported = AltBeaconFactory.isBLESupported(Context);
        this.callback = CallBack;
        this.mHandler = new Handler(Context.getMainLooper());
        this.mBluetoothAdapter = btManager.getAdapter();
        this.beaconCode = beaconCode;
        this.referenceRssi = referenceRssi;
        this.manufacturer = manufacturer;
    }
    public boolean Start(String id1, int id2, int id3) {

        if(mBluetoothAdapter == null){
            Started("Bluetooth is NOT-Supported");
            return false;
        }

        if (!isBLESupported) {
            Started("BLE is NOT-Supported");
            return false;
        }

        if(!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Started("MultipleAdvertisementSupported is NOT-Supported");
            return false;
        }

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if(mBluetoothLeAdvertiser == null) {
            Started("getBluetoothLeAdvertiser returned null");
            return false;
        }

        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        settingsBuilder.setConnectable(false);

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();

        AltBeacon tmp = new AltBeacon(null,0xFF,"",this.beaconCode,id1,id2,id3,referenceRssi,0,0);

        dataBuilder.addManufacturerData(manufacturer,AltBeaconFactory.getAdvertDataFromBeacon(tmp));
        dataBuilder.setIncludeDeviceName(false);
        dataBuilder.setIncludeTxPowerLevel(false);

        weAreStoppingNow = false;
        mBluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(),mAdvertiseCallback );
        return true;
    }

    public void Stop() {
        BluetoothLeAdvertiser tmpLeAdvertiser = mBluetoothLeAdvertiser;
        mBluetoothLeAdvertiser = null;
        if(tmpLeAdvertiser != null) {
            Log.i(TAG, "Call Stop advert");
            weAreStoppingNow = true;
            tmpLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
    }

    private void Started(String error) {
        final String errorTmp = error;

        that.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onAdvertisingStarted(errorTmp);
                }
            }
        });
    }
    private void Stopped(String error) {
        final String errorTmp = error;

        that.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onAdvertisingStopped(errorTmp);
                }
            }
        });
    }

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffec) {
            if(weAreStoppingNow) {
                Log.i(TAG, "Stopped OK");
                callback.debugData("Advertisement STOP ok");
                Stopped(null);
            }else{
                Log.i(TAG, "Started OK");
                callback.debugData("Advertisement started ok");
                Started(null);
            }
        }

        @Override
        public void onStartFailure(int result) {
            String errBuffer = "";

            switch(result){
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    errBuffer = "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.";
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    errBuffer = "Failed to start advertising because no advertising instance is available.";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    errBuffer = "Failed to start advertising as the advertising is already started.";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    errBuffer = "Operation failed due to an internal error.";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    errBuffer = "This feature is not supported on this platform.";
                    break;
                default:
                    errBuffer = "There was unknown error(" + String.format("%02X", result) + ")";
                    break;
            }

            if(weAreStoppingNow) {
                Log.i(TAG, "Stopped Err: " + errBuffer);
                Stopped(errBuffer);
            }else{
                callback.debugData("Advertisement failed: " + errBuffer);
                Log.i(TAG, "Started Err : " + errBuffer);
                Started(errBuffer);
            }
        }
    };
}
