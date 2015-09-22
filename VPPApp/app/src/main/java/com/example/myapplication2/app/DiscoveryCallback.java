package com.example.myapplication2.app;

import java.util.List;

/**
 * Created by juksilve on 27.8.2015.
 */
interface DiscoveryCallback {
    public enum State{
        DiscoveryIdle,
        DiscoveryNotInitialized,
        DiscoveryFindingPeers,
        DiscoveryFindingServices
    }
    void gotServicesList(List<ServiceItem> list);
    void foundService(ServiceItem item);
    void StateChanged(State newState);
    void debugData(String data);
}
