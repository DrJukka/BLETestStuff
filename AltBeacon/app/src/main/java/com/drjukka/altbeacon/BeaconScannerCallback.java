package com.drjukka.altbeacon;

/**
 * Created by juksilve on 18.11.2015.
 */

interface BeaconScannerCallback {
    void BeaconDiscovered(AltBeacon beacon);
    void debugData(String data);
}

