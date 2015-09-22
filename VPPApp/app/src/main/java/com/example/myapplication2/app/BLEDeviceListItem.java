package com.example.myapplication2.app;

import android.bluetooth.BluetoothDevice;

/**
 * Created by juksilve on 27.8.2015.
 */
class BLEDeviceListItem {
    private final BluetoothDevice mDevice;
    private final String mUuid;

    public BLEDeviceListItem(BluetoothDevice device,byte[] scanRecord) {
        this.mDevice =device;
        this.mUuid = BLEBase.getServiceUUID(BLEBase.ParseRecord(scanRecord));
    }

    public BluetoothDevice getDevice(){return mDevice;}
    public String getUUID(){return mUuid;}
}
