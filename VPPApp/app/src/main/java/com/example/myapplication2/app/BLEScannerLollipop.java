package com.example.myapplication2.app;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by juksilve on 22.9.2015.
 */

// https://code.google.com/p/android/issues/detail?id=82463

@TargetApi(18)
@SuppressLint("NewApi")
public class BLEScannerLollipop {

    BLEScannerLollipop that = this;
    private final Context context;
    private final PeerDiscoveredCallback mDiscoveryCallback;
    private final BluetoothAdapter mBluetoothAdapter;
    private final CopyOnWriteArrayList<BluetoothDevice> mBLEDeviceList = new CopyOnWriteArrayList<BluetoothDevice>();
    private final Handler mHandler;
    private final String mInstanceString;
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

    public BLEScannerLollipop(Context Context, PeerDiscoveredCallback CallBack, BluetoothManager Manager,String instanceString) {
        this.context = Context;
        this.mDiscoveryCallback = CallBack;
        this.mBluetoothAdapter = Manager.getAdapter();
        this.mInstanceString = instanceString;
        this.scanner = this.mBluetoothAdapter.getBluetoothLeScanner();

        this.mHandler = new Handler(this.context.getMainLooper());
    }

    public void Start() {
        Stop();
        BLEValueReader tmpValueReader = new BLEValueReader(this.context, this.mDiscoveryCallback, mBluetoothAdapter,this.mInstanceString);
        mBLEValueReader = tmpValueReader;
        StartScanning();
    }

    public void StartScanning() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i("SCAN-NER", "Start scanner now");
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

    public void reStartScanning() {
        Log.i("SCAN-NER", "reStartScanning called");
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

    private void foudDevice(ScanResult result) {

        if (result == null) {
            return;
        }

        BluetoothDevice device = result.getDevice();
        List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();



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
        if (itemTmp == null) {
            mBLEDeviceList.add(device);
        }

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
        Log.i("SCAN-NER", "foudDevice with ourService: " + device.getAddress());

        // we already have it in the currently discovered list
        if(that.mDiscoveryCallback.isPeerDiscovered(device.getAddress())){
            return;
        }

        // the UID does not always match
        // for example I was getting 000000a1-0000-1000-8000-00805f9b34fb for 010500a1-00b0-1000-8000-00805f9b34fb

        String peerBluetoothAddress = "";

        Map<ParcelUuid, byte[]> srvData = result.getScanRecord().getServiceData();
        if(srvData != null && srvData.size() > 0){
            for (ParcelUuid key : srvData.keySet()) {
                byte[] srvBuffer = srvData.get(key);
                // we are expecting Buetooth address, thus the length should be 6
                if (srvBuffer != null && srvBuffer.length == 6) {
                    StringBuilder bluetoothAddress = new StringBuilder(srvBuffer.length * 3);
                    bluetoothAddress.append(String.format("%02X", srvBuffer[0]));
                    for (int i = 1; i < srvBuffer.length; i++) {
                        bluetoothAddress.append(String.format(":%02X", srvBuffer[i]));
                    }

                    peerBluetoothAddress = bluetoothAddress.toString();
                    break;
                }
            }
        }

        //lets ask if we have seen this earlier already
        ServiceItem foundPeer = that.mDiscoveryCallback.haveWeSeenPeerEarlier(device);
        if(foundPeer != null){
            // so we have seen it earlier, but if we are all the way here, its not yet in current list of peers we see
            that.mDiscoveryCallback.PeerDiscovered(foundPeer,true);
            return;
        }

        Log.i("SCAN-NER", "AddDevice : " + device.getAddress() + ", BT-Address: " + peerBluetoothAddress);
        //Add device will actually start the discovery process if there is no previous discovery on progress
        // if there is not, then we will start discovery process with this device
        mBLEValueReader.AddDevice(device,peerBluetoothAddress);
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
            that.mDiscoveryCallback.debugData("onScanFailed : " + errorCode);
            Log.i("SCAN-NER", "onScanFailed : " + errorCode);
            reStartScanning();
        }
    };
}
