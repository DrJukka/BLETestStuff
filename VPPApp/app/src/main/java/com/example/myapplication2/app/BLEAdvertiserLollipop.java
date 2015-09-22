package com.example.myapplication2.app;

import android.bluetooth.*;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by juksilve on 20.4.2015.
 */
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
    public boolean Start() {

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

        for (BluetoothGattService service: mBluetoothGattServices) {
            if (service != null) {
                mBluetoothGattServer.addService(service);
            }
        }

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

        dataBuilder.setIncludeTxPowerLevel(true);

        for (ParcelUuid uuid : serviceUuids) {
            dataBuilder.addServiceUuid(uuid);
        }

        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW);

//        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
  //      settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        settingsBuilder.setConnectable(true);

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
                if(callback != null) {
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
                if(callback != null) {
                    callback.onAdvertisingStopped(errorTmp);
                }
            }
        });
    }

    private final BluetoothGattServerCallback mGattServerCallback  = new BluetoothGattServerCallback() {

        @Override
        public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
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
        public void onDescriptorReadRequest(android.bluetooth.BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

            byte[] dataForResponse = new byte[]{};

            for (BluetoothGattService tmpService : mBluetoothGattServices) {
                outerLoop:
                if (tmpService != null) {
                    List<BluetoothGattCharacteristic> CharList = tmpService.getCharacteristics();
                    if (CharList != null) {
                        for (BluetoothGattCharacteristic chara : CharList) {
                            if (chara != null) {
                                List<BluetoothGattDescriptor> DescList = chara.getDescriptors();
                                if (DescList != null) {
                                    for (BluetoothGattDescriptor descriptorItem : DescList) {
                                        if (descriptorItem != null && descriptorItem.getUuid().compareTo(descriptor.getUuid()) == 0) {

                                            byte[] tmpString = descriptorItem.getValue();

                                            if (tmpString == null || offset > tmpString.length) {
                                                //lets just return the empty string we have made already
                                            } else {
                                                dataForResponse = new byte[tmpString.length - offset];
                                                for (int i = 0; i != (tmpString.length - offset); ++i) {
                                                    dataForResponse[i] = tmpString[offset + i];
                                                }
                                            }
                                            break outerLoop;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (mBluetoothGattServer != null) {
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, dataForResponse);
            }
        }

        private int disconCount = 0;
        private int connCount = 0;

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {

            Log.i("ADV-CB", "onConnectionStateChange status : " + BLEBase.getGATTStatusAsString(status) + ", state: " + BLEBase.getConnectionStateAsString(newState));

            switch (newState) {
                case BluetoothProfile.STATE_DISCONNECTED:
                    connCount++;
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    disconCount++;
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                case BluetoothProfile.STATE_DISCONNECTING:
                default:
                    // we can ignore any other than actual connected/disconnected state
                    break;
            }

            if(status != BluetoothGatt.GATT_SUCCESS){
                callback.debugData("Advert CST err: " + status + ", ConCount : " + connCount + ", disconCount: " + disconCount);
            }

            Log.i("ADV-CB", "onConnectionStateChange ConCount : " + connCount + ", disconCount: " + disconCount);

        }
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if(status != BluetoothGatt.GATT_SUCCESS){
                callback.debugData("Advert SAdd err: " + status);
            }
            Log.i("ADV-CB", "onServiceAdded status : " + BLEBase.getGATTStatusAsString(status));
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.i("ADV-CB", "onCharacteristicWriteRequest ");
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.i("ADV-CB", "onDescriptorWriteRequest ");
        }
        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.i("ADV-CB", "onExecuteWrite ");
        }
        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            if(status != BluetoothGatt.GATT_SUCCESS){
                callback.debugData("Advert NSend err: " + status);
            }

            if(device == null){
                return;
            }

            Log.i("ADV-CB", "onNotificationSent status : " + BLEBase.getGATTStatusAsString(status) + ", device : " + device.getAddress());
        }
        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            if(device == null){
                return;
            }

            Log.i("ADV-CB", "onMtuChanged newsize : " + mtu + ", device : " + device.getAddress());
        }
    };

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffec) {
            if(weAreStoppingNow) {
                callback.debugData("Stopped OK");
                Log.i("ADV-CB", "Stopped OK");
                Stopped(null);
            }else{
                callback.debugData("Advert Started OK");
                Log.i("ADV-CB", "Started OK");
                Started( null);
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
                Log.i("ADV-CB", "Started Err : " + errBuffer);
                callback.debugData("Advert Started Err : " + errBuffer);
                Started(errBuffer);
            }
        }
    };
}
