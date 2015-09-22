package com.example.myapplication2.app;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;
import android.os.Handler;
import java.util.List;
import android.os.CountDownTimer;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by juksilve on 20.4.2015.
 */

// 40:30 , 1:01:00 - connectable
//https://www.youtube.com/watch?feature=player_detailpage&v=qx55Sa8UZAQ#t=1712

class BLEScannerKitKat  {

    private final BLEScannerKitKat that = this;
    private final Context context;
    private final PeerDiscoveredCallback mDiscoveryCallback;
    private final BluetoothAdapter mBluetoothAdapter;
    private final CopyOnWriteArrayList<BLEDeviceListItem> mBLEDeviceList = new CopyOnWriteArrayList<BLEDeviceListItem>();
    private final Handler mHandler;
    private final String mInstanceString;
    private BLEValueReader mBLEValueReader = null;

    private final CountDownTimer reTryStartScanningTimer = new CountDownTimer(5000, 1000) {
        public void onTick(long millisUntilFinished) {}
        public void onFinish () {
                Log.i("SCAN-NER", "re-try starting scanning");
                StartScanning();
        }
    };

    public BLEScannerKitKat(Context Context, PeerDiscoveredCallback CallBack,BluetoothManager Manager,String instanceString) {
        this.context = Context;
        this.mDiscoveryCallback = CallBack;
        this.mBluetoothAdapter = Manager.getAdapter();
        this.mInstanceString = instanceString;
        this.mHandler = new Handler(this.context.getMainLooper());
    }

    public void Start() {
        Stop();
        BLEValueReader tmpValueReader = new BLEValueReader(this.context,this.mDiscoveryCallback,mBluetoothAdapter,this.mInstanceString);
        mBLEValueReader = tmpValueReader;
        StartScanning();
    }

    public void StartScanning() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                boolean retValue = that.mBluetoothAdapter.startLeScan(that.leScanCallback);
                Log.i("SCAN-NER", "start now : " + retValue);
                if(!retValue){
                    that.mDiscoveryCallback.debugData("SCANNER reTry");
                    reTryStartScanningTimer.start();
                }
            }
        });
    }

    public void Stop() {
        Log.i("SCAN-NER", "stop now");
        reTryStartScanningTimer.cancel();
        that.mBLEDeviceList.clear();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(leScanCallback);
            }
        });
        BLEValueReader tmp = mBLEValueReader;
        mBLEValueReader = null;
        if (tmp != null) {
            tmp.Stop();
        }
    }

    public void reStartScanning() {
        Log.i("SCAN-NER", "reStartScanning now");
        mBLEDeviceList.clear();

        // supposedly we need to stp & re-start in order to be sure we do get the devices again.
        // http://stackoverflow.com/questions/19502853/android-4-3-ble-filtering-behaviour-of-startlescan
        // I did not have the devices mentioned in the list to actually test this, but supposedly starting & stopping should work just fine
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(leScanCallback);
                StartScanning();
            }
        });
    }

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

            if (device == null || scanRecord == null || mBLEValueReader == null) {
                return;
            }
            BLEDeviceListItem itemTmp = null;
            for (BLEDeviceListItem item : mBLEDeviceList) {
                if (item != null && item.getDevice() != null) {
                    if (item.getDevice().getAddress().equalsIgnoreCase(device.getAddress())) {
                        itemTmp = item;
                    }
                }
            }

            //seen earlier, lets return
            if (itemTmp != null) {
                return;
            }

            itemTmp = new BLEDeviceListItem(device, scanRecord);
            mBLEDeviceList.add(itemTmp);

            if (!itemTmp.getUUID().equalsIgnoreCase(BLEBase.SERVICE_UUID_1)) {
                //its not our service, so we are not interested on it anymore.
                // but to faster ignore it later scan results, we'll add it to the list
                // but we don't give it to the value reader for any further processing
                return;
            }

            //lets ask if we have seen this earlier already
            ServiceItem foundPeer = that.mDiscoveryCallback.isPeerDiscovered(device);
            if(foundPeer != null){
                that.mDiscoveryCallback.PeerDiscovered(foundPeer,true);
                return;
            }

            Log.i("SCAN-NER", "AddDevice : " + device.getAddress());

            //Add device will actually start the discovery process if there is no previous discovery on progress
            // if there is not, then we will start discovery process with this device
            mBLEValueReader.AddDevice(device);
        }
    };
}
