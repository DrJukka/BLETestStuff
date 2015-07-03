package org.thaliproject.p2p.mybletest;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import java.util.Map;

/**
 * Created by juksilve on 20.4.2015.
 */

/*
Disconnect 0x13 with Nexus 5 having lollipop
https://code.google.com/p/android/issues/detail?id=156730

 */

public class SearchKitKat {

    SearchKitKat that = this;

    BluetoothAdapter btAdapter = null;
    BluetoothGatt bluetoothGatt = null;

    private Context context = null;
    private BLEBase.CallBack callBack = null;

    public SearchKitKat(Context Context, BLEBase.CallBack CallBack) {
        this.context = Context;
        this.callBack = CallBack;

        BluetoothManager btManager = (BluetoothManager)this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
    }

    public void Start() {
        if(bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        btAdapter.startLeScan(leScanCallback);
    }

    public void Stop() {
        btAdapter.stopLeScan(leScanCallback);

        if(bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            // your implementation here
            that.callBack.Debug("ADAPTER", "onLeScan device: " + device.getName() + ", RSSI: " + rssi);

            Map <Integer,String> parseRecord = BLEScanRecordParser.ParseRecord(scanRecord);
            String uuidFound = BLEScanRecordParser.getServiceUUID(parseRecord);
            that.callBack.Debug("ADAPTER", "Service UID = " + uuidFound);
            if(uuidFound.equalsIgnoreCase(MainActivity.SERVICE_UUID_1)) {
                btAdapter.stopLeScan(leScanCallback);
                if (bluetoothGatt == null) {
                    that.callBack.Debug("ADAPTER", "connectGatt");
                    bluetoothGatt = device.connectGatt(that.context, false, new MyBluetoothGattCallback(that.callBack));
                    bluetoothGatt.connect();
                }
            }
        }
    };
}
