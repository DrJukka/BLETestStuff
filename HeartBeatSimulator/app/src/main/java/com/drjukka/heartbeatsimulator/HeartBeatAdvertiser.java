package com.drjukka.heartbeatsimulator;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by juksilve on 20.4.2015.
 */
@TargetApi(18)
@SuppressLint("NewApi")

public class HeartBeatAdvertiser {

    public interface HeartBeatAdvertiserCallback {
        void onAdvertisingStarted(String error);
        void onAdvertisingStopped(String error);
    }

    private final HeartBeatAdvertiser that = this;
    private final String TAG = "HB_Advertiser";
    private final Context context;
    private final HeartBeatAdvertiserCallback callback;
    private final Handler mHandler;
    private final HeartBeatEngine mHeartBeatModel;
    private final BluetoothManager mBluetoothManager;
    private final BluetoothAdapter mBluetoothAdapter;

    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    private boolean weAreStoppingNow = false;

    public HeartBeatAdvertiser(Context Context, HeartBeatAdvertiserCallback CallBack, BluetoothManager btManager,HeartBeatEngine model) {
        this.context = Context;
        this.callback = CallBack;
        this.mHandler = new Handler(this.context.getMainLooper());
        this.mHeartBeatModel = model;
        this.mBluetoothManager = btManager;
        this.mBluetoothAdapter = mBluetoothManager.getAdapter();
    }
    public boolean Start() {

        if(mBluetoothManager == null || mBluetoothAdapter == null){
            Started("Bluetooth is NOT-Supported");
            return false;
        }

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Started("BLE is NOT-Supported");
            return false;
        }

        if(!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Started("MultipleAdvertisementSupported is NOT-Supported");
            return false;
        }

        mBluetoothGattServer = mBluetoothManager.openGattServer(this.context, mGattServerCallback);
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        if(mBluetoothGattServer == null) {
            Started("openGattServer returned null");
            return false;
        }

        if(mBluetoothLeAdvertiser == null) {
            Started("getBluetoothLeAdvertiser returned null");
            return false;
        }

        for (BluetoothGattService service: mHeartBeatModel.getServiceArray()) {
            if (service != null) {
                mBluetoothGattServer.addService(service);
            }
        }

        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        settingsBuilder.setConnectable(true);

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();

        dataBuilder.addServiceUuid(new ParcelUuid(mHeartBeatModel.getAdvertiseServiceUUID()));
        dataBuilder.addManufacturerData(mHeartBeatModel.getManufacturerId(), mHeartBeatModel.getManufacturerData());
        dataBuilder.setIncludeDeviceName(true);
        dataBuilder.setIncludeTxPowerLevel(true);

        weAreStoppingNow = false;
        mBluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(),mAdvertiseCallback );
        return true;
    }

    public void Stop() {
        BluetoothLeAdvertiser tmpLeAdvertiser = mBluetoothLeAdvertiser;
        mBluetoothLeAdvertiser = null;
        if(tmpLeAdvertiser != null) {
            Log.i(TAG, "Call Stop advert");
            weAreStoppingNow = true;
            tmpLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }

        BluetoothGattServer tmpGatServer = mBluetoothGattServer;
        mBluetoothGattServer = null;
        if (tmpGatServer != null) {
            tmpGatServer.clearServices();
            tmpGatServer.close();
        }

        mWriteList.clear();
    }

    private void Started(String error){
        final String errorTmp = error;

        that.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onAdvertisingStarted(errorTmp);
                }
            }
        });
    }

    public void notifyClientDevice(BluetoothDevice device,BluetoothGattCharacteristic characteristic){

        Log.i(TAG, "notifyClientDevice");
        try {
            if (mBluetoothGattServer != null && characteristic != null && device != null) {
                mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
            }
        }catch (Exception e){
            Log.i(TAG, "notifyCharacteristicChanged exception : " + e.toString() );
        }
    }

    private void Stopped(String error){
        final String errorTmp = error;

        that.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onAdvertisingStopped(errorTmp);
                }
            }
        });
    }

    private final BluetoothGattServerCallback mGattServerCallback  = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.i(TAG, "onConnectionStateChange  newState : " + newState);

            switch (newState) {
                case BluetoothProfile.STATE_DISCONNECTED:
                case BluetoothProfile.STATE_DISCONNECTING:
                    mHeartBeatModel.removeDeviceToNotify(device);
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    mHeartBeatModel.addDeviceToNotify(device);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {

            Log.i(TAG, "onCharacteristicReadRequest");

            byte[] dataForResponse = mHeartBeatModel.getCharacteristicsReadResponce(device, requestId, offset, characteristic);

            if (mBluetoothGattServer != null) {
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, dataForResponse);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

            Log.i(TAG, "onCharacteristicWriteRequest");

            if(preparedWrite) {
                addWriteItemByteBuffer(device.getAddress(), characteristic.getUuid().toString(), value, true);
            }else {
                onCharacterWrite(device.getAddress(),characteristic.getUuid().toString(),value);
            }

            if (mBluetoothGattServer != null &&responseNeeded) {
                // need to give the same values we got as an reply, in order to get next possible part.
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.i(TAG, "onDescriptorWriteRequest");

/*            if(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB").compareTo(descriptor.getUuid()) == 0){
                Log.i(TAG,"its our Notify descriptor");
                mHeartBeatModel.addDeviceToNotify(device);
            }
*/
            // now tell the connected device that this was all successfull
            if (mBluetoothGattServer != null &&responseNeeded) {
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);

            if (mBluetoothGattServer != null) {
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{});
            }

            executeWriteStorages(device.getAddress(),execute);
        }

        private void addWriteItemByteBuffer(String deviceAddress, String uuid,byte[] buffer, boolean isCharacter){
            WriteStorage  tmpitem = getWriteItem(deviceAddress,uuid);
            if(tmpitem != null){
                tmpitem.addData(buffer);
            }else{
                WriteStorage newItem = new WriteStorage(deviceAddress,uuid,isCharacter);
                mWriteList.add(newItem);
                newItem.addData(buffer);
            }
        }

        private WriteStorage getWriteItem(String deviceAddress, String uuid) {
            WriteStorage ret = null;
            for (WriteStorage storage : mWriteList){
                if(storage != null && storage.getUUID().equalsIgnoreCase(uuid) && storage.getDeviceAddress().equalsIgnoreCase(deviceAddress)){
                    ret = storage;
                    break;
                }
            }
            return ret;
        }

        private void executeWriteStorages(String deviceAddress, boolean execute){

            Log.i(TAG, "executeWriteStorages : " + execute);
            for(int i=mWriteList.size() - 1; i >= 0;i--){
                WriteStorage storage  = mWriteList.get(i);
                if(storage != null && storage.getDeviceAddress().equalsIgnoreCase(deviceAddress)){
                    if(execute){//if its not for executing, its then for cancelling it
                        if(storage.isCharacter()){
                            onCharacterWrite(storage.getDeviceAddress(), storage.getUUID(), storage.getFullData());
                        }
                    }

                    mWriteList.remove(storage);
                    //we are done with this item now.
                    storage.clearData();
                }
            }
        }

        private void onCharacterWrite(String deviceAddress, String uuid,byte[] value){
            mHeartBeatModel.onCharacterWrite(deviceAddress,uuid,value);
        }
    };

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffec) {
            if(weAreStoppingNow) {
                Log.i(TAG, "Stopped OK");
                Stopped(null);
            }else{
                Log.i(TAG, "Started OK");
                Started(null);
            }
        }

        @Override
        public void onStartFailure(int result) {
            String errBuffer = "";

            switch(result){
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    errBuffer = "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.";
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    errBuffer = "Failed to start advertising because no advertising instance is available.";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    errBuffer = "Failed to start advertising as the advertising is already started.";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    errBuffer = "Operation failed due to an internal error.";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    errBuffer = "This feature is not supported on this platform.";
                    break;
                default:
                    errBuffer = "There was unknown error(" + String.format("%02X", result) + ")";
                    break;
            }

            if(weAreStoppingNow) {
                Log.i(TAG, "Stopped Err: " + errBuffer);
                Stopped(errBuffer);
            }else{
                Log.i(TAG, "Started Err : " + errBuffer);
                Started(errBuffer);
            }
        }
    };


    public class WriteStorage{

        final private String mDeviceAddress;
        final private String mUUID;
        final private boolean mIsCharacter;
        private List<byte[]> byteArray;

        public WriteStorage(String address,String uuid, boolean isCharacter){
            mDeviceAddress = address;
            mUUID = uuid;
            byteArray = new ArrayList<byte[]>();
            mIsCharacter = isCharacter;
        }

        public boolean isCharacter(){return mIsCharacter;}
        public String getUUID(){return mUUID;}
        public String getDeviceAddress(){return mDeviceAddress;}
        public void clearData(){
            byteArray.clear();
        }
        public void addData(byte[] array){
            byteArray.add(array);
        }

        public byte[] getFullData(){

            int totalSize = 0;

            for(int i=0; i < byteArray.size();i++){
                totalSize = totalSize + byteArray.get(i).length;
            }

            if(totalSize <= 0) {
                return new byte[]{};
            }

            int copuCounter = 0;
            byte[] retArray = new byte[totalSize];
            for(int ii=0; ii < byteArray.size();ii++) {
                byte[] tmpArr = byteArray.get(ii);
                System.arraycopy(tmpArr, 0, retArray, copuCounter, tmpArr.length);
                copuCounter = copuCounter + tmpArr.length;
            }

            return retArray;
        }
    }

    private List<WriteStorage> mWriteList = new ArrayList<WriteStorage>();
}
