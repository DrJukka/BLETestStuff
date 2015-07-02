package org.thaliproject.p2p.mybletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by juksilve on 20.4.2015.
 */
public class AdvertiserLollipop {

    AdvertiserLollipop that = this;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private ArrayList<BluetoothGattService> mBluetoothGattServices;
    private List<ParcelUuid> serviceUuids;

    private Context context = null;
    private BLEBase.CallBack callBack = null;


    public AdvertiserLollipop(Context Context, BLEBase.CallBack CallBack) {
        this.context = Context;
        this.callBack = CallBack;

        mBluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothGattServer = mBluetoothManager.openGattServer(this.context, mGattCallback);
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mBluetoothGattServices = new ArrayList<BluetoothGattService>();
        serviceUuids = new ArrayList<ParcelUuid>();

        if(!mBluetoothAdapter.isMultipleAdvertisementSupported())
        {
            that.callBack.Debug("ADV-CB","MultipleAdvertisementSupported is NOT-Supported");
        }
    }
    public void Start() {

        if(mBluetoothGattServices != null
        && mBluetoothGattServer  != null
        && mBluetoothLeAdvertiser   != null) {
            for (int i = 0; i < mBluetoothGattServices.size(); i++) {
                mBluetoothGattServer.addService(mBluetoothGattServices.get(i));
            }

            AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
            AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

            dataBuilder.setIncludeTxPowerLevel(true);

            for (int ii = 0; ii < serviceUuids.size(); ii++) {
                dataBuilder.addServiceUuid(serviceUuids.get(ii));
            }

            settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
            settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
            settingsBuilder.setConnectable(true);

            mBluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), mAdvertiseCallback);
        }
    }

    public void Stop() {
        if(mBluetoothLeAdvertiser != null && mAdvertiseCallback != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
    }

    public void addService(BluetoothGattService service) {
        mBluetoothGattServices.add(service);
        serviceUuids.add(new ParcelUuid(service.getUuid()));
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffec) {
            that.callBack.Debug("ADV-CB","Advertising started successfully");
        }

        @Override
        public void onStartFailure(int result) {
            if (result == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE) {
                that.callBack.Debug("ADV-CB","Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.");
            }
            else if(result == AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS){
                that.callBack.Debug("ADV-CB","Failed to start advertising because no advertising instance is available.");
            }
            else if(result == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED){
                that.callBack.Debug("ADV-CB","Failed to start advertising as the advertising is already started.");
            }
            else if(result == AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR){
                that.callBack.Debug("ADV-CB","Operation failed due to an internal error.");
            }
            else if(result == AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED){
                that.callBack.Debug("ADV-CB","This feature is not supported on this platform.");
            }
            else {
                that.callBack.Debug("ADV-CB","There was unknown error.");
            }
        }
    };

    private BluetoothGattServerCallback mGattCallback = new BluetoothGattServerCallback() {

        public void onCharacteristicRead(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {
            that.callBack.Debug("ADV-CB","onCharacteristicRead: " + characteristic + ", status: " + status);
        }

        public void onCharacteristicWrite(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {
            that.callBack.Debug("ADV-CB","onCharacteristicWrite: " + characteristic + ", status: " + status);
        }

        public void onDescriptorRead(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattDescriptor descriptor, int status) {
            that.callBack.Debug("ADV-CB","onDescriptorRead: " + descriptor + ", status: " + status);
        }

        public void onDescriptorWrite(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattDescriptor descriptor, int status) {
            that.callBack.Debug("ADV-CB","onDescriptorWrite: " + descriptor + ", status: " + status);
        }

        public void onReliableWriteCompleted(android.bluetooth.BluetoothGatt gatt, int status) {
            that.callBack.Debug("ADV-CB","onReliableWriteCompleted: , status: " + status);
        }

        public void onReadRemoteRssi(android.bluetooth.BluetoothGatt gatt, int rssi, int status) {
            that.callBack.Debug("ADV-CB","onReadRemoteRssi :" + rssi + ", status: " + status);
        }

        public void onMtuChanged(android.bluetooth.BluetoothGatt gatt, int mtu, int status) {
            that.callBack.Debug("ADV-CB","onMtuChanged :" + mtu + ", status: " + status);
        }
    };
}
