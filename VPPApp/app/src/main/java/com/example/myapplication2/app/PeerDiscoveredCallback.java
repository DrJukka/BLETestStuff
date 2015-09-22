package com.example.myapplication2.app;

import android.bluetooth.BluetoothDevice;

/**
 * Created by juksilve on 22.9.2015.
 */
interface PeerDiscoveredCallback {
    void PeerDiscovered(ServiceItem peer, boolean cachedValue);
    ServiceItem isPeerDiscovered(final BluetoothDevice device);
    void debugData(String data);
}
