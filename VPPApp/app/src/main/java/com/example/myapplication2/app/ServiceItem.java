// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package com.example.myapplication2.app;

/**
 * Created by juksilve on 12.3.2015.
 */
public class ServiceItem{

    public ServiceItem(String pID,String pName,String btAddress,String type,String address, String name){
        this.peerId = pID;
        this.peerName = pName;
        this.peerAddress = btAddress;
        this.serviceType = type;
        this.deviceAddress = address;
        this.deviceName =  name;
        this.discoveredTime = System.currentTimeMillis();
    }
    final public String peerId;
    final public String peerName;
    final public String peerAddress;
    final public String serviceType;
    final public String deviceAddress;
    final public String deviceName;
    final public long discoveredTime;
}
