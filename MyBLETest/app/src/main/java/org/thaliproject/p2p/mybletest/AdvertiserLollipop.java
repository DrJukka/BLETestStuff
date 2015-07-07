package org.thaliproject.p2p.mybletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by juksilve on 20.4.2015.
 */
public class AdvertiserLollipop {

    AdvertiserLollipop that = this;

    interface BLEAdvertiserCallback{
        public void debug(String who, String what);
        public void Started(AdvertiseSettings settingsInEffec,String error);
        public void Stopped(String error);
        public void onDeviceConnected(android.bluetooth.BluetoothDevice device, int status);
        public void onDeviceDisconnected(android.bluetooth.BluetoothDevice device, int status);
        public void onCharacterRead(android.bluetooth.BluetoothDevice device,android.bluetooth.BluetoothGattCharacteristic characteristic);
        public void onDescriptorRead(android.bluetooth.BluetoothDevice device,android.bluetooth.BluetoothGattDescriptor characteristic);
    }

    private Context context = null;
    BLEAdvertiserCallback callback = null;
    private Handler mHandler = null;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private ArrayList<BluetoothGattService> mBluetoothGattServices;
    private List<ParcelUuid> serviceUuids;

    public AdvertiserLollipop(Context Context, BLEAdvertiserCallback CallBack) {
        this.context = Context;
        this.callback = CallBack;
        this.mHandler = new Handler(this.context.getMainLooper());

        mBluetoothGattServices = new ArrayList<BluetoothGattService>();
        serviceUuids = new ArrayList<ParcelUuid>();
    }
    public void Start() {

        if (BLEBase.isBLESupported(this.context)) {
            mBluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                mBluetoothAdapter = mBluetoothManager.getAdapter();
                if (mBluetoothAdapter != null && mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                    mBluetoothGattServer = mBluetoothManager.openGattServer(this.context, serverCallback);
                    mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

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

                    mBluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), new AdvertiseCallback() {

                        @Override
                        public void onStartSuccess(AdvertiseSettings settingsInEffec) {
                            Started(settingsInEffec, null);
                        }

                        @Override
                        public void onStartFailure(int result) {
                            if (result == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE) {
                                Started(null, "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.");
                            } else if (result == AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                                Started(null, "Failed to start advertising because no advertising instance is available.");
                            } else if (result == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
                                Started(null, "Failed to start advertising as the advertising is already started.");
                            } else if (result == AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR) {
                                Started(null, "Operation failed due to an internal error.");
                            } else if (result == AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                                Started(null, "This feature is not supported on this platform.");
                            } else {
                                Started(null, "There was unknown error(" + String.format("%02X", result) + ")");
                            }
                        }
                    });
                } else {
                    Started(null, "MultipleAdvertisementSupported is NOT-Supported");
                }
            } else {
                Started(null, "Bluetooth is NOT-Supported");
            }
        } else {
            Started(null, "BLE is NOT-Supported");
        }
    }

    public void Stop() {
        if(mBluetoothLeAdvertiser != null) {
            debug_print("ADV-CB", "Call Stop advert");
            mBluetoothLeAdvertiser.stopAdvertising(new AdvertiseCallback() {

                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffec) {
                    debug_print("ADV-CB", "Stopped OK");
                    Stopped(null);
                }

                @Override
                public void onStartFailure(int result) {
                    debug_print("ADV-CB", "Stopped with error");
                    if (result == AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR) {
                        Stopped("Operation failed due to an internal error.");
                    } else if (result == AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                        Stopped("This feature is not supported on this platform.");
                    } else {
                        Stopped("There was unknown error(" + String.format("%02X", result) + ")");
                    }
                }
            });
            mBluetoothLeAdvertiser = null;
        }

        if(mBluetoothGattServer != null){
            mBluetoothGattServer.clearServices();
            mBluetoothGattServer.close();
            mBluetoothGattServer = null;
        }

        if(mBluetoothGattServices != null){
            mBluetoothGattServices.clear();
        }

        if(serviceUuids != null){
            serviceUuids.clear();
        }
    }

    public boolean addService(BluetoothGattService service) {
        boolean ret = false;
        if(mBluetoothGattServices != null && serviceUuids != null && service != null && service.getUuid() != null) {
            mBluetoothGattServices.add(service);
            serviceUuids.add(new ParcelUuid(service.getUuid()));
            ret = true;
        }

        return ret;
    }

    private void Started(AdvertiseSettings settingsInEffec,String error){
        final AdvertiseSettings settingsInEffecTmp = settingsInEffec;
        final String errorTmp = error;

        that.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(callback != null) {
                    callback.Started(settingsInEffecTmp, errorTmp);
                }
            }
        });
    }
    private void Stopped(String error){
        final String errorTmp = error;

        that.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(callback != null) {
                    callback.Stopped(errorTmp);
                }
            }
        });
    }

    BluetoothGattServerCallback serverCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(android.bluetooth.BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            final android.bluetooth.BluetoothDevice deviceTmp = device;
            final int statusTmp = status;

            switch (newState) {
                case BluetoothProfile.STATE_DISCONNECTED:
                    that.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onDeviceDisconnected(deviceTmp, statusTmp);
                            }
                        }
                    });
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    that.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onDeviceConnected(deviceTmp, statusTmp);
                            }
                        }
                    });
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                case BluetoothProfile.STATE_CONNECTING:
                default:
                    break;
            }
         }

        @Override
        public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice device, int requestId, int offset, android.bluetooth.BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            if(offset == 0){
                final android.bluetooth.BluetoothDevice deviceTmp = device;
                final android.bluetooth.BluetoothGattCharacteristic characteristicTmp = characteristic;
                that.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(callback != null) {
                            callback.onCharacterRead(deviceTmp, characteristicTmp);
                        }
                    }
                });
            }

            byte[] dataForResponse = new byte[] {};
            if (mBluetoothGattServices != null) {
                for (BluetoothGattService tmpServ : mBluetoothGattServices) {
                    if(tmpServ != null){
                        List<BluetoothGattCharacteristic> CharList = tmpServ.getCharacteristics();
                        if(CharList != null){
                            for (BluetoothGattCharacteristic chara : CharList) {
                                if (chara != null && chara.getUuid().compareTo(characteristic.getUuid()) == 0 ) {
                                    String tmpString = chara.getStringValue(offset);
                                    if(tmpString != null && tmpString.length()>0) {
                                        dataForResponse = tmpString.getBytes();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (mBluetoothGattServer != null){
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, dataForResponse);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice device, int requestId, android.bluetooth.BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            debug_print("ADV-CB", " char-WriteRequest req: " + requestId + ", offset: " + offset + ", characteristic: " + characteristic.getUuid());

            String tmp = "";
            if(value != null && value.length > 0) {
                tmp = new String(value);
            }
            debug_print("ADV-CB", " char-WriteRequest prep: " + preparedWrite + ", responseNeeded: " + responseNeeded + ", value: " + tmp);

       /*     if (mBluetoothGattServices != null) {
                for (BluetoothGattService tmpServ : mBluetoothGattServices) {
                    if(tmpServ != null){
                        List<BluetoothGattCharacteristic> CharList = tmpServ.getCharacteristics();
                        if(CharList != null){
                            for (BluetoothGattCharacteristic chara : CharList) {
                                if (chara != null && chara.getUuid().compareTo(characteristic.getUuid()) == 0 ) {
                                    String tmpString = chara.getStringValue(offset);
                                    if(tmpString != null && tmpString.length()>0) {
                                        dataForResponse = tmpString.getBytes();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }*/

            if (mBluetoothGattServer != null &&responseNeeded) {
                // empty reply as response
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[] {});
            }
        }

        @Override
        public void onDescriptorReadRequest(android.bluetooth.BluetoothDevice device, int requestId, int offset, android.bluetooth.BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

            if(offset == 0){
                final android.bluetooth.BluetoothDevice deviceTmp = device;
                final android.bluetooth.BluetoothGattDescriptor descriptorTmp = descriptor;
                that.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(callback != null) {
                            callback.onDescriptorRead(deviceTmp, descriptorTmp);
                        }
                    }
                });
            }

            byte[] dataForResponse = new byte[] {};

            if (mBluetoothGattServices != null) {
                for (BluetoothGattService tmpServ : mBluetoothGattServices) {
                    if(tmpServ != null){
                        List<BluetoothGattCharacteristic> CharList = tmpServ.getCharacteristics();
                        if(CharList != null){
                            for (BluetoothGattCharacteristic chara : CharList) {
                                if(chara != null) {
                                    List<BluetoothGattDescriptor> DescList = chara.getDescriptors();
                                    if (DescList != null) {
                                        for (BluetoothGattDescriptor descr : DescList) {
                                            if(descr != null && descr.getUuid().compareTo(descriptor.getUuid()) == 0){

                                                byte[] tmpString = descr.getValue();

                                                if (tmpString == null || offset > tmpString.length) {
                                                    //lets just return the empty string we have made already
                                                }else {
                                                    dataForResponse = new byte[tmpString.length - offset];
                                                    for (int i = 0; i != (tmpString.length - offset); ++i) {
                                                        dataForResponse[i] = tmpString[offset + i];
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (mBluetoothGattServer != null){
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, dataForResponse);
            }
        }

        @Override
        public void onDescriptorWriteRequest(android.bluetooth.BluetoothDevice device, int requestId, android.bluetooth.BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            debug_print("ADV-CB", " desc-ReadRequest req: " + requestId + ", offset: " + offset + ", characteristic: " + descriptor.getUuid());

            String tmp = "";
            if(value != null && value.length > 0) {
                tmp = new String(value);
            }
            debug_print("ADV-CB", " desc-WriteRequest prep: " + preparedWrite  + ", responseNeeded: " + responseNeeded + ", value: " + tmp);


            if (mBluetoothGattServer != null && responseNeeded) {
                String data = "Desc wrote :" + new String(value);
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, data.getBytes());
            }
        }

        @Override
        public void onExecuteWrite(android.bluetooth.BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            debug_print("ADV-CB", "onExecuteWrite reg: " + requestId + ", execute: " + execute);

        }

        @Override
        public void onNotificationSent(android.bluetooth.BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            debug_print("ADV-CB", "onNotificationSent status: " + BLEBase.getGATTStatus(status));
        }

        @Override
        public void onMtuChanged(android.bluetooth.BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            debug_print("ADV-CB", "onMtuChanged mtu: " + mtu);
        }
    };

    private void debug_print(String who, String what){
        Log.i(who, what);

        final String whoTmp = who;
        final String whatTmp = what;
        that.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(callback != null) {
                    that.callback.debug(whoTmp, whatTmp);
                }
            }
        });
    }
}
