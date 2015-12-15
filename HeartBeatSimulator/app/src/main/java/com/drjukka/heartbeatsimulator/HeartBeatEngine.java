package com.drjukka.heartbeatsimulator;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by juksilve on 19.11.2015.
 */
public class HeartBeatEngine {

    private final String TAG = "HeartBeatEngine";
    private final UUID mAdvertiseUUID;
    private final BluetoothGattService heartRateService;
    private final BluetoothGattCharacteristic heaertBeatMeasurement;
    private final BluetoothGattCharacteristic bodySensorLocation;

    private final BluetoothGattService batteryLevelService;
    private final BluetoothGattCharacteristic batteryLevel;

    private final HeartBeatAdvertiser mHeartBeatAdvertiser;

    private CopyOnWriteArrayList<BluetoothGattService> mBluetoothGattServices = new CopyOnWriteArrayList<BluetoothGattService>();
    private CopyOnWriteArrayList<BluetoothDevice> mNotifyDevices = new CopyOnWriteArrayList<BluetoothDevice>();

    private boolean mAdvertisementRunning = false;

    public HeartBeatEngine(Context Context, HeartBeatAdvertiser.HeartBeatAdvertiserCallback CallBack) {

        this.mAdvertiseUUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");

         /* Heart Rate Service */

        heartRateService = new BluetoothGattService(this.mAdvertiseUUID,BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mBluetoothGattServices.add(heartRateService);

        //notification
        heaertBeatMeasurement = new BluetoothGattCharacteristic(UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb"),BluetoothGattCharacteristic.PROPERTY_NOTIFY,BluetoothGattCharacteristic.PROPERTY_NOTIFY);

        // http://stackoverflow.com/questions/24865120/any-way-to-implement-ble-notifications-in-android-l-preview/25508053#25508053
        BluetoothGattDescriptor gD = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"), BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
        heaertBeatMeasurement.addDescriptor(gD);

        heartRateService.addCharacteristic(heaertBeatMeasurement);

        //read
        bodySensorLocation = new BluetoothGattCharacteristic(UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb"),BluetoothGattCharacteristic.PROPERTY_READ,BluetoothGattCharacteristic.PERMISSION_READ );
        heartRateService.addCharacteristic(bodySensorLocation);

        batteryLevelService = new BluetoothGattService(UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb"),BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mBluetoothGattServices.add(batteryLevelService);

        batteryLevel = new BluetoothGattCharacteristic(UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb"),BluetoothGattCharacteristic.PROPERTY_READ,BluetoothGattCharacteristic.PERMISSION_READ);
        batteryLevelService.addCharacteristic(batteryLevel);

        mHeartBeatAdvertiser = new HeartBeatAdvertiser(Context, CallBack, (BluetoothManager) Context.getSystemService(Context.BLUETOOTH_SERVICE),this);
    }

    public void start() {
        mHeartBeatAdvertiser.Start();
        mAdvertisementRunning = true;
    }

    public boolean isStarted(){
        return mAdvertisementRunning;
    }

    public void stop(){
        mHeartBeatAdvertiser.Stop();
        mAdvertisementRunning = false;
        mNotifyDevices.clear();
    }

    /*
    Key	Value
        0	Other
        1	Chest
        2	Wrist
        3	Finger
        4	Hand
        5	Ear Lobe
        6	Foot
        7 - 255	Reserved for future use
     */
    public void setbodySensorLocation(int value){

        byte[] valueBuffer = new byte[1];
        valueBuffer[0] = (byte)(value & 0xFF);
        bodySensorLocation.setValue(valueBuffer);
    }

    public void setHeartbeatValue(int value) {

        byte[] valueBuffer = new byte[3];

        valueBuffer[0] = 0x01;
        valueBuffer[1] = (byte) (value & 0xFF);
        valueBuffer[2] = (byte) ((value >> 8) & 0xFF);
        heaertBeatMeasurement.setValue(valueBuffer);

        for (BluetoothDevice device : mNotifyDevices) {
            mHeartBeatAdvertiser.notifyClientDevice(device,heaertBeatMeasurement);
        }
    }

    public void setBatteryLevel(int value){

        byte[] valueBuffer = new byte[1];
        valueBuffer[0] = (byte)(value & 0xFF);
        batteryLevel.setValue(valueBuffer);
    }

    public void removeDeviceToNotify(BluetoothDevice device){

        if(device == null){
            return;
        }

        for (BluetoothDevice dev : mNotifyDevices) {
            if(dev.getAddress().equalsIgnoreCase(device.getAddress())){
                mNotifyDevices.remove(dev);
                return;
            }
        }
    }

    public void addDeviceToNotify(BluetoothDevice device){

        if(device == null){
            return;
        }

        for (BluetoothDevice dev : mNotifyDevices) {
            if(dev.getAddress().equalsIgnoreCase(device.getAddress())){
                return;
            }
        }

        mNotifyDevices.add(device);
    }

    public byte[] getCharacteristicsReadResponce(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic){

        Log.i(TAG, "getCharacteristicsReadResponce : " + characteristic.getUuid());

        for(BluetoothGattService service : mBluetoothGattServices) {
            if(service != null) {
                for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {

                    if (characteristic.getUuid().compareTo(chars.getUuid()) == 0) {

                        byte[] ret = chars.getValue();

                        Log.i(TAG, "found returning value : " + new String(ret));

                        return ret;
                    }
                }
            }
        }

        return (new String("")).getBytes();
    }

    public void onCharacterWrite(String deviceAddress, String characteristicsUUID,byte[] value) {
        for(BluetoothGattService service : mBluetoothGattServices) {
            if (service != null) {
                for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {

                    if (UUID.fromString(characteristicsUUID).compareTo(chars.getUuid()) == 0) {
                        chars.setValue(value);
                        return;
                    }
                }
            }
        }
    }

    public UUID getAdvertiseServiceUUID(){return mAdvertiseUUID;}
    public CopyOnWriteArrayList<BluetoothGattService> getServiceArray(){return mBluetoothGattServices;}

    public int getManufacturerId(){return 0x5855;};
    public byte[] getManufacturerData(){

        byte[] ret = new byte[4];
        ret[0] = 0x05;
        ret[1] = 0x2B;
        ret[2] = 0x00;
        ret[3] = 0x6B;

        return ret;
    }
}
