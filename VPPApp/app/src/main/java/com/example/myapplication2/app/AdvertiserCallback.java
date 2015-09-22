package com.example.myapplication2.app;

/**
 * Created by juksilve on 27.8.2015.
 */
interface AdvertiserCallback{
    void onAdvertisingStarted(String error);
    void onAdvertisingStopped(String error);
    void debugData(String data);
}
