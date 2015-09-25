package com.example.myapplication2.app;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.*;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by juksilve on 20.4.2015.
 */
@TargetApi(18)
@SuppressLint("NewApi")

public class BLEAdvertiserLollipop {

    private final BLEAdvertiserLollipop that = this;
    private final Context context;
    private final AdvertiserCallback callback;
    private final Handler mHandler;
    private final CopyOnWriteArrayList<BluetoothGattService> mBluetoothGattServices = new CopyOnWriteArrayList<BluetoothGattService>();
    private final CopyOnWriteArrayList<ParcelUuid> serviceUuids = new CopyOnWriteArrayList<ParcelUuid>();
    private final BluetoothManager mBluetoothManager;
    private final BluetoothAdapter mBluetoothAdapter;

    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    private boolean weAreStoppingNow = false;

    public BLEAdvertiserLollipop(Context Context, AdvertiserCallback CallBack,BluetoothManager btManager) {
        this.context = Context;
        this.callback = CallBack;
        this.mHandler = new Handler(this.context.getMainLooper());
        this.mBluetoothManager = btManager;
        this.mBluetoothAdapter = mBluetoothManager.getAdapter();
    }
    public boolean Start(String blueToothAddressString) {

        if(mBluetoothManager == null || mBluetoothAdapter == null){
            Started("Bluetooth is NOT-Supported");
            return false;
        }

        if (!BLEBase.isBLESupported(this.context)) {
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

        for (BluetoothGattService service: mBluetoothGattServices) {
            if (service != null) {
                mBluetoothGattServer.addService(service);
            }
        }

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

        dataBuilder.setIncludeTxPowerLevel(true);

        String[] splitted = blueToothAddressString.split(":");
        byte[] sendAddress = new byte[6];        // length == 6 bytes
        for(int i = 0; i < splitted.length; i++) {
            sendAddress[i] = Integer.decode("0x" + splitted[i]).byteValue();
        }

        //the list can actually only have one item, otherwise its making the scan record too big
        for (ParcelUuid uuid : serviceUuids) {
            dataBuilder.addServiceUuid(uuid);
            dataBuilder.addServiceData(uuid, sendAddress);
        }

        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW);

//        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
  //      settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        settingsBuilder.setConnectable(true);

        //make sure these are false, so we save some space on scan record
        dataBuilder.setIncludeDeviceName(false);
        dataBuilder.setIncludeTxPowerLevel(false);

        weAreStoppingNow = false;
        mBluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(),mAdvertiseCallback );
        return true;
    }

    public void Stop() {
        BluetoothLeAdvertiser tmpLeAdvertiser = mBluetoothLeAdvertiser;
        mBluetoothLeAdvertiser = null;
        if(tmpLeAdvertiser != null) {
            Log.i("ADV-CB", "Call Stop advert");
            weAreStoppingNow = true;
            tmpLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }

        BluetoothGattServer tmpGatServer = mBluetoothGattServer;
        mBluetoothGattServer = null;
        if (tmpGatServer != null) {
            for (BluetoothGattService service : mBluetoothGattServices) {
                tmpGatServer.removeService(service);
            }

            tmpGatServer.clearServices();
            tmpGatServer.close();
        }
        mBluetoothGattServices.clear();
        serviceUuids.clear();
        mWriteList.clear();
    }

    public boolean addService(BluetoothGattService service) {
        if (service == null || service.getUuid() == null) {
            return false;
        }

        mBluetoothGattServices.add(service);
        serviceUuids.add(new ParcelUuid(service.getUuid()));

        return true;
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
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            byte[] dataForResponse = new byte[]{};

            for (BluetoothGattService tmpService : mBluetoothGattServices) {
                outerLoop:
                if (tmpService != null) {
                    List<BluetoothGattCharacteristic> CharList = tmpService.getCharacteristics();
                    if (CharList != null) {
                        for (BluetoothGattCharacteristic chara : CharList) {
                            if (chara != null && chara.getUuid().compareTo(characteristic.getUuid()) == 0) {
                                // use offset to continue where the last read request ended
                                String tmpString = chara.getStringValue(offset);
                                if (tmpString != null && tmpString.length() > 0) {
                                    dataForResponse = tmpString.getBytes();
                                }
                                break outerLoop;
                            }
                        }
                    }
                }
            }
            if (mBluetoothGattServer != null) {
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, dataForResponse);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

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
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);

            if (mBluetoothGattServer != null) {
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[] {});
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

            Log.i("ADV-CB", "executeWriteStorages : " + execute);
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

            //disabled for debugging reasons
            String jsonString = new String(value);
            try {
                JSONObject jObject = new JSONObject(jsonString);

                String peerIdentifier = jObject.getString(MainActivity.JSON_ID_PEERID);
                String peerName = jObject.getString(MainActivity.JSON_ID_PEERNAME);
                String peerAddress = jObject.getString(MainActivity.JSON_ID_BTADRRES);

                Log.i("ADV-CB", "JsonLine: " + jsonString + " -- peerIdentifier:" + peerIdentifier + ", peerName: " + peerName + ", peerAddress: " + peerAddress);
                ServiceItem tmpSrv = new ServiceItem(peerIdentifier, peerName, peerAddress, "BLE", deviceAddress, deviceAddress);
                that.callback.PeerDiscovered(tmpSrv,false);

                callback.debugData("Incoming peer " + peerName);

            } catch (JSONException e) {
                Log.i("ADV-CB", "Decrypting instance failed , :" + e.toString());
            }
        }
    };

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffec) {
            if(weAreStoppingNow) {
                Log.i("ADV-CB", "Stopped OK");
                callback.debugData("Advertisement STOP ok");
                Stopped(null);
            }else{
                Log.i("ADV-CB", "Started OK");
                callback.debugData("Advertisement started ok");
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
                Log.i("ADV-CB", "Stopped Err: " + errBuffer);
                Stopped(errBuffer);
            }else{
                callback.debugData("Advertisement failed: " + errBuffer);
                Log.i("ADV-CB", "Started Err : " + errBuffer);
                Started(errBuffer);
            }
        }
    };


    public class WriteStorage{

        final private String  mDeviceAddress;
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
