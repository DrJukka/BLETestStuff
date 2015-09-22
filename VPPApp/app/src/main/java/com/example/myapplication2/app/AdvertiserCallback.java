package com.example.myapplication2.app;

/**
 * Created by juksilve on 27.8.2015.
 */
interface AdvertiserCallback extends PeerDiscoveredCallback{
    void onAdvertisingStarted(String error);
    void onAdvertisingStopped(String error);
}
