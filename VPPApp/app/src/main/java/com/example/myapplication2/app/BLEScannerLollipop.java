package com.example.myapplication2.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by juksilve on 22.9.2015.
 */

// https://code.google.com/p/android/issues/detail?id=82463

public class BLEScannerLollipop implements DiscoveryCallback{

    BLEScannerLollipop that = this;
    private final Context context;
    private final DiscoveryCallback mDiscoveryCallback;
    private final BluetoothAdapter mBluetoothAdapter;
    private final CopyOnWriteArrayList<BluetoothDevice> mBLEDeviceList = new CopyOnWriteArrayList<BluetoothDevice>();
    private final Handler mHandler;

    private final BluetoothLeScanner scanner;

    private BLEValueReader mBLEValueReader = null;

    private final CountDownTimer reTryStartScanningTimer = new CountDownTimer(5000, 1000) {
        public void onTick(long millisUntilFinished) {
        }

        public void onFinish() {
            Log.i("SCAN-NER", "re-try starting scanning");
            StartScanning();
        }
    };

        public BLEScannerLollipop(Context Context, DiscoveryCallback CallBack, BluetoothManager Manager) {
            this.context = Context;
            this.mDiscoveryCallback = CallBack;
            this.mBluetoothAdapter = Manager.getAdapter();
            this.scanner = this.mBluetoothAdapter.getBluetoothLeScanner();

            this.mHandler = new Handler(this.context.getMainLooper());
        }

    public void Start() {
        Stop();
        BLEValueReader tmpValueReader = new BLEValueReader(this.context, this, mBluetoothAdapter);
        mBLEValueReader = tmpValueReader;
        StartScanning();
    }

    public void StartScanning() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ScanSettings settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                        .build();

                List<ScanFilter> filters = new ArrayList<ScanFilter>();
                scanner.startScan(filters, settings, mScanCallback);
            }
        });
    }

    public void Stop() {
        Log.i("SCAN-NER", "stop now");
        reTryStartScanningTimer.cancel();
        that.mBLEDeviceList.clear();

        scanner.stopScan(mScanCallback);

        BLEValueReader tmp = mBLEValueReader;
        mBLEValueReader = null;
        if (tmp != null) {
            tmp.Stop();
        }
    }

    @Override
    public void gotServicesList(List<ServiceItem> list) {
        Log.i("SCAN-NER", "gotServicesList size : " + list.size());

        that.mDiscoveryCallback.gotServicesList(list);
        mBLEDeviceList.clear();

        // supposedly we need to stp & re-start in order to be sure we do get the devices again.
        // http://stackoverflow.com/questions/19502853/android-4-3-ble-filtering-behaviour-of-startlescan
        // I did not have the devices mentioned in the list to actually test this, but supposedly starting & stopping should work just fine
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                scanner.stopScan(mScanCallback);
                StartScanning();
            }
        });
    }

    @Override
    public void foundService(ServiceItem item) {
        Log.i("SCAN-NER", "foundService : " + item.peerName);
        that.mDiscoveryCallback.foundService(item);
    }

    @Override
    public void StateChanged(com.example.myapplication2.app.DiscoveryCallback.State newState) {
        that.mDiscoveryCallback.StateChanged(newState);
    }

    @Override
    public void debugData(String data) {
        that.mDiscoveryCallback.debugData(data);
    }

    private void foudDevice(ScanResult result) {

        if (result == null) {
            return;
        }

        BluetoothDevice device = result.getDevice();
        List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();

        Log.i("SCAN-NER", "foudDevice : " + device.getAddress());

        if (device == null || uuids == null || mBLEValueReader == null) {
            return;
        }

        BluetoothDevice itemTmp = null;
        for (BluetoothDevice item : mBLEDeviceList) {
            if (item != null) {
                if (item.getAddress().equalsIgnoreCase(device.getAddress())) {
                    itemTmp = item;
                }
            }
        }

        //seen earlier, lets return
        if (itemTmp != null) {
            return;
        }
        mBLEDeviceList.add(device);

        boolean isOurService = false;
        for (ParcelUuid UID : uuids) {
            if (UID.toString().equalsIgnoreCase(BLEBase.SERVICE_UUID_1)) {
                isOurService = true;
            }
        }

        //its not our service, so we are not interested on it anymore.
        // but to faster ignore it later scan results, we'll add it to the list
        // but we don't give it to the value reader for any further processing
        if (!isOurService) {
            return;
        }

        Log.i("SCAN-NER", "AddDevice : " + device.getAddress());

        //Add device will actually start the discovery process if there is no previous discovery on progress
        // if there is not, then we will start discovery process with this device
        mBLEValueReader.AddDevice(device);
    }


    final private ScanCallback mScanCallback = new ScanCallback(){
        public void onScanResult(int callbackType, ScanResult result) {
            foudDevice(result);
        }

        public void onBatchScanResults(List<ScanResult> results) {
            for(ScanResult result : results){
                foudDevice(result);
            }
        }

        public void onScanFailed(int errorCode) {
            debugData("onScanFailed : " + errorCode);

        }
    };
}
