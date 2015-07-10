package org.thaliproject.p2p.mybletest;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by juksilve on 20.4.2015.
 */

/*
Disconnect 0x13 with Nexus 5 having lollipop
https://code.google.com/p/android/issues/detail?id=156730

 */

public class BLEScannerKitKat {

    BLEScannerKitKat that = this;

    BluetoothAdapter btAdapter = null;

    interface BLEScannerCallback {
        public void onScanListChanged(final List<DeviceListItem> scanList);
    }

    private Context context = null;
    private BLEScannerCallback callBack = null;
    private Handler mHandler = null;

    public class DeviceListItem {
        private BluetoothDevice mDevice;
        private int mRssi;
        private Map<Integer, String> mParseRecord;
        private String mUuid;

        public DeviceListItem(BluetoothDevice device, int rssi, byte[] scanRecord) {
            this.mDevice =device;
            this.mRssi = rssi;
            this.mParseRecord = BLEBase.ParseRecord(scanRecord);
            this.mUuid = BLEBase.getServiceUUID(this.mParseRecord);
        }
        public void setRssi(int rssi){this.mRssi = rssi;}
        public int getRssi(){return mRssi;}
        public BluetoothDevice getDevice(){return mDevice;}
        public Map<Integer, String> getParseRecord(){return mParseRecord;}
        public String getUUID(){return mUuid;}
    }

    private List<DeviceListItem> devlist = new ArrayList<>();

    public BLEScannerKitKat(Context Context, BLEScannerCallback CallBack) {
        this.context = Context;
        this.callBack = CallBack;
        this.mHandler = new Handler(this.context.getMainLooper());
        BluetoothManager btManager = (BluetoothManager)this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
    }

    public List<DeviceListItem> getDeviceList(){return devlist;}

    public void Start() {
        Stop();
        btAdapter.startLeScan(leScanCallback);
    }

    public void Stop() {
        btAdapter.stopLeScan(leScanCallback);
        devlist.clear();
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

            if(device != null && scanRecord != null) {
                boolean notFound = true;
                for (DeviceListItem item : devlist) {
                    if (item != null && item.getDevice() != null) {
                        if(item.getDevice().getAddress().equalsIgnoreCase(device.getAddress())){
                           // debug_print("SCAN","Updated device : " + device.getAddress());
                            item.setRssi(rssi);
                            notFound = false;
                        }
                    }
                }

                if(notFound){
                    debug_print("SCAN","added new device : " + device.getAddress());
                    devlist.add(new DeviceListItem(device,rssi,scanRecord));
                }

                that.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //inform that the scan list has changed
                        if(callBack != null) {
                            callBack.onScanListChanged(devlist);
                        }
                    }
                });
            }
        }
    };

    private void debug_print(String who, String what){
        Log.i(who, what);
    }
}
