package org.thaliproject.p2p.mybletest;

import android.app.Application;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

/**
 * Created by juksilve on 9.7.2015.
 */
public class MyBLEHandlerApp extends Application implements BLEScannerKitKat.BLEScannerCallback, BLEAdvertiserLollipop.BLEAdvertiserCallback {

    static final public String DSS_BLESCAN_RESULT = "org.thaliproject.p2p.mybletest.DSS_BLESCAN_RESULT";
    static final public String DSS_BLESCAN_FOUNDCOUNT = "org.thaliproject.p2p.mybletest.DSS_BLESCAN_FOUNDCOUNT";

    static final public String DSS_ADVERT_STATUS  = "org.thaliproject.p2p.mybletest.DSS_ADVERT_STATUS";
    static final public String DSS_ADVERT_STATVAL = "org.thaliproject.p2p.mybletest.DSS_ADVERT_STATVAL";

    BLEScannerKitKat mSearchKitKat = null;
    List<BLEScannerKitKat.DeviceListItem> devScanList = null;

    BLEAdvertiserLollipop mBLEAdvertiserLollipop = null;

    @Override
    public void onTerminate() {
        super.onTerminate();
        stopBLEScan();
        stopBLEAdvertising();
    }

    public void startBLEScan(){
        stopBLEScan();
        mSearchKitKat = new BLEScannerKitKat(this, this);
        mSearchKitKat.Start();
    }

    public boolean isBLEScanActive(){
        boolean ret = false;
        if(mSearchKitKat != null){
            ret = true;
        }
        return ret;
    }

    @Override
    public void onScanListChanged(List<BLEScannerKitKat.DeviceListItem> scanList) {
        devScanList = scanList;
        int foundCount = 0;
        if(devScanList != null){
            foundCount = scanList.size();
        }
        // create event for scan result changed
        Intent intent = new Intent(DSS_BLESCAN_RESULT);
        intent.putExtra(DSS_BLESCAN_FOUNDCOUNT,foundCount);
        sendBroadcast(intent);
    }

    public List<BLEScannerKitKat.DeviceListItem> getScanList(){
        return devScanList;
    }

    private BLEScannerKitKat.DeviceListItem selectedItem = null;
    public void setSelectedItem(BLEScannerKitKat.DeviceListItem item){
        selectedItem =item;
    }
    public BLEScannerKitKat.DeviceListItem getSelectedItem(){
        return selectedItem;
    }

    public void stopBLEScan(){
        if (mSearchKitKat != null) {
            mSearchKitKat.Stop();
            mSearchKitKat = null;
        }
    }

   public void startBLEAdvertising(BluetoothGattService service){
       stopBLEAdvertising();
       mBLEAdvertiserLollipop = new BLEAdvertiserLollipop(this, this);
       mBLEAdvertiserLollipop.addService(service);
       mBLEAdvertiserLollipop.Start();
   }

    public boolean isBLEAdvertisingActive(){
        boolean ret = false;
        if(mBLEAdvertiserLollipop != null){
            ret = true;
        }
        return ret;
    }


    public void stopBLEAdvertising(){
        if (mBLEAdvertiserLollipop != null) {
            mBLEAdvertiserLollipop.Stop();
            mBLEAdvertiserLollipop = null;
        }
    }

    @Override
    public void onAdvertisingStarted(AdvertiseSettings settingsInEffec, String error) {

        String sendError = "";
        if (error != null) {
            sendError = error;
            debug_Text("Advert", "Start error " + error);
        } else {
            debug_Text("Advert", "Advertising started");
            sendError = "Advertising started";
        }

        Intent intent = new Intent(DSS_ADVERT_STATUS);
        intent.putExtra(DSS_ADVERT_STATVAL,sendError);
        sendBroadcast(intent);
    }

    @Override
    public void onAdvertisingStopped(String error) {

        String sendError = "";
        if (error != null) {
            sendError = error;
            debug_Text("Advert", "Stop error " + error);
        } else {
            debug_Text("Advert", "Advertising STOPPED");
            sendError = "Advertising STOPPED";
        }

        Intent intent = new Intent(DSS_ADVERT_STATUS);
        intent.putExtra(DSS_ADVERT_STATVAL,sendError);
        sendBroadcast(intent);
    }

    @Override
    public void onRemoteDeviceConnected(String deviceAddress, int status) {
        debug_Text("CONN", BLEBase.getDeviceNameOrAddress(deviceAddress, this) + " Connected with status: " + BLEBase.getGATTStatus(status));
    }

    @Override
    public void onRemoteDeviceDisconnected(String deviceAddress, int status) {
        debug_Text("CONN", BLEBase.getDeviceNameOrAddress(deviceAddress, this) + " Disconnected with status: " + BLEBase.getGATTStatus(status));
    }

    @Override
    public void onRemoteCharacterRead(String deviceAddress, String uuid) {
        debug_Text("Character", BLEBase.getDeviceNameOrAddress(deviceAddress, this) + " Read-Start for " + uuid);
    }

    @Override
    public void onRemoteDescriptorRead(String deviceAddress, String uuid) {
        debug_Text("Descriptor", BLEBase.getDeviceNameOrAddress(deviceAddress, this) + " Read-Start for " + uuid);
    }

    @Override
    public void onRemoteCharacterWrite(String deviceAddress, String uuid, byte[] value) {
        debug_Text("Character", BLEBase.getDeviceNameOrAddress(deviceAddress, this) + " wrote " + uuid);

        if (value != null && value.length > 0) {
            debug_Text("Character", "Data(" + value.length + " bytes): " + new String(value));
            mBLEAdvertiserLollipop.setCharacterValue(UUID.fromString(uuid),value);
        }
    }

    @Override
    public void onRemoteDescriptorWrite(String deviceAddress, String uuid, byte[] value) {
        debug_Text("Descriptor", BLEBase.getDeviceNameOrAddress(deviceAddress, this) + " wrote " + uuid);

        if (value != null && value.length > 0) {
            debug_Text("Descriptor", "Data(" + value.length + " bytes): " + new String(value));
            mBLEAdvertiserLollipop.setDescriptorValue(UUID.fromString(uuid),value);
        }
    }

    public void debug_Text(String Who, String text) {
        Log.d(Who, text);
    }
}
