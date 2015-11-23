package com.drjukka.altbeacon;

/**
 * Created by juksilve on 27.8.2015.
 */
interface BeaconAdvertiserCallback {
    void onAdvertisingStarted(String error);
    void onAdvertisingStopped(String error);
    void debugData(String data);
}
