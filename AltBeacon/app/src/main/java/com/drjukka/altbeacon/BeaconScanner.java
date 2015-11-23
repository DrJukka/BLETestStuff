package com.drjukka.altbeacon;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.os.Handler;
import android.os.CountDownTimer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by juksilve on 20.4.2015.
 */

@TargetApi(18)
@SuppressLint("NewApi")

class BeaconScanner {

    private final String TAG = "BeaconScanner";
    private final BeaconScanner that = this;
    private final BeaconScannerCallback mCallback;
    private final BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler;

    //lollipop specific stuff
    private final BluetoothLeScanner scanner;

    private final CountDownTimer reTryStartScanningTimer = new CountDownTimer(5000, 1000) {
        public void onTick(long millisUntilFinished) {}
        public void onFinish () {
                Log.i(TAG, "re-try starting scanning");
                StartScanning();
        }
    };

    public BeaconScanner(Context Context, BeaconScannerCallback CallBack, BluetoothManager Manager) {
        this.mCallback = CallBack;
        this.mBluetoothAdapter = Manager.getAdapter();
        this.mHandler = new Handler(Context.getMainLooper());

        //With lollipop & newer we can use the new API
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.scanner = this.mBluetoothAdapter.getBluetoothLeScanner();
        }else{
            this.scanner = null;
        }
    }

    public void Start() {
        Stop();
        StartScanning();
    }

    public void StartScanning() {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (that.scanner != null) {
                    Log.i(TAG, "Start Lollipop scanner now");
                    ScanSettings settings = new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                            .build();

                    List<ScanFilter> filters = new ArrayList<ScanFilter>();
                    doSetScanCallback();
                    scanner.startScan(filters, settings, mScanCallback);
                } else { //KitKat
                    boolean retValue = that.mBluetoothAdapter.startLeScan(that.leScanCallback);
                    Log.i(TAG, "start KitKat scanner now : " + retValue);
                    if (!retValue) {
                        that.mCallback.debugData("SCANNER reTry");
                        reTryStartScanningTimer.start();
                    }
                }
            }
        });
    }

    public void Stop() {
        Log.i(TAG, "stop now");
        reTryStartScanningTimer.cancel();
        if (that.scanner != null) {
            scanner.stopScan(mScanCallback);
        } else { //KitKat
            mBluetoothAdapter.stopLeScan(leScanCallback);
        }
    }

    public void reStartScanning() {
        Log.i(TAG, "reStartScanning now");
        Stop();
        StartScanning();
    }

    private void foundBeacon(final BluetoothDevice device, final int rssi, final byte[] scanRecord){

        if (device == null || scanRecord == null) {
            return;
        }

        //not ours (length is not enough, or type byte is wrong)
        if (!AltBeaconFactory.isLengthAndTypeOk(scanRecord)) {
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                that.mCallback.BeaconDiscovered(AltBeaconFactory.getBeaconFromScanrecord(device, scanRecord, rssi));
            }
        });
    }

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                that.foundBeacon(device, rssi, scanRecord);
            }
        };

    ScanCallback mScanCallback = null;

    private void doSetScanCallback() {
        mScanCallback = new ScanCallback() {
            public void onScanResult(int callbackType, ScanResult result) {

                if (result != null && result.getScanRecord() != null) {
                    that.foundBeacon(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                }
            }

            public void onBatchScanResults(List<ScanResult> results) {

                for (ScanResult result : results) {
                    if (result != null && result.getScanRecord() != null) {
                        that.foundBeacon(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                    }
                }
            }

            public void onScanFailed(int errorCode) {

                final int errorCodeTmp = errorCode;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        that.mCallback.debugData("onScanFailed : " + errorCodeTmp);
                    }
                });
                Log.i(TAG, "onScanFailed : " + errorCode);
                reStartScanning();
            }
        };
    }
}
