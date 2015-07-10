package org.thaliproject.p2p.mybletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;


import java.util.List;

/**
 * Created by juksilve on 9.7.2015.
 */
public class BLEConnector {
    BLEConnector that = this;

    BluetoothAdapter btAdapter = null;
    BluetoothGatt bluetoothGatt = null;

    interface BLEConnectCallback {
        public void onDeviceConnected(String deviceAddress, int status);
        public void onDeviceDisconnected(String deviceAddress, int status);
        public void onCharacteristicChanged(final BluetoothGattCharacteristic characteristic);
        public void onServicesDiscovered(final List<BluetoothGattService> services, final int status);
        public void onCharacterRead(android.bluetooth.BluetoothGattCharacteristic characteristic, int status);
        public void onDescriptorRead(android.bluetooth.BluetoothGattDescriptor descriptor, int status);
        public void onCharacterWrite(android.bluetooth.BluetoothGattCharacteristic characteristic, int status);
        public void onDescriptorWrite(android.bluetooth.BluetoothGattDescriptor descriptor, int status);
        public void onReliableWriteCompleted(String deviceAddress, int status);
        public void onReadRemoteRssi(String deviceAddress,int rssi, int status);
        public void onMtuChanged(String deviceAddress,int mtu, int status);
    }

    private Context context = null;
    private BLEConnectCallback connectBack = null;
    private Handler mHandler = null;

    public BLEConnector(Context Context, BLEConnectCallback CallBack) {
        this.context = Context;
        this.connectBack = CallBack;
        this.mHandler = new Handler(this.context.getMainLooper());
        BluetoothManager btManager = (BluetoothManager)this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
    }

    public boolean connect(final BluetoothDevice device) {
        boolean ret = false;
        if(device != null && bluetoothGatt == null && connectBack != null) {
            bluetoothGatt = device.connectGatt(that.context, false, gattCallback);
            ret = true;
        }
        return ret;
    }
    public boolean discoverServices() {
        boolean ret= false;
        if(bluetoothGatt != null){
            ret = bluetoothGatt.discoverServices();
        }
        return ret;
    }


    public void disConnect() {
        if(bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
    // we'll need maybe to implement beginReliableWrite later on

    public boolean readCharacter(BluetoothGattCharacteristic characteristic) {
        boolean ret= false;
        if(bluetoothGatt != null){
            ret= bluetoothGatt.readCharacteristic(characteristic);
        }
        return ret;
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        boolean ret= false;
        if(bluetoothGatt != null){
            ret= bluetoothGatt.writeCharacteristic(characteristic);
        }
        return ret;
    }

    public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
        boolean ret= false;
        if(bluetoothGatt != null){
            ret= bluetoothGatt.readDescriptor(descriptor);
        }
        return ret;
    }

    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        boolean ret= false;
        if(bluetoothGatt != null){
            ret= bluetoothGatt.writeDescriptor(descriptor);
        }
        return ret;
    }

    public boolean readRemoteRssi() {
        boolean ret= false;
        if(bluetoothGatt != null){
            ret= bluetoothGatt.readRemoteRssi();
        }
        return ret;
    }

    public boolean requestMtu(int mtu) {
        boolean ret= false;
        if(bluetoothGatt != null){
            ret= bluetoothGatt.requestMtu(mtu);
        }
        return ret;
    }


    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            that.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(that.connectBack != null) {
                        that.connectBack.onCharacteristicChanged(characteristic);
                    }
                }
            });
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {

            if(that.connectBack != null) {
                that.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        String devAddress = "";
                        if (gatt != null) {
                            if (gatt.getDevice() != null) {
                                devAddress = gatt.getDevice().getAddress();
                            }
                        }
                        if (newState == BluetoothProfile.STATE_CONNECTED && gatt != null) {
                            that.connectBack.onDeviceConnected(devAddress, status);
                            //gatt.discoverServices();
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            that.connectBack.onDeviceDisconnected(devAddress, status);
                        }
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if(gatt != null && that.connectBack != null) {
                that.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        that.connectBack.onServicesDiscovered(gatt.getServices(),status);
                    }
                });
            }
        }

        public void onCharacteristicRead(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {
            if(that.connectBack != null) {
                final android.bluetooth.BluetoothGattCharacteristic characteristicTmp = characteristic;
                final int statusTmp = status;

                that.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        that.connectBack.onCharacterRead(characteristicTmp, statusTmp);
                    }
                });

            }
        }

        public void onCharacteristicWrite(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {
            if(that.connectBack != null) {
                final android.bluetooth.BluetoothGattCharacteristic characteristicTmp = characteristic;
                final int statusTmp = status;
                that.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        that.connectBack.onCharacterWrite(characteristicTmp, statusTmp);
                    }
                });

            }
        }

        public void onDescriptorRead(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattDescriptor descriptor, int status) {
            if(that.connectBack != null) {
                final android.bluetooth.BluetoothGattDescriptor descriptorTmp = descriptor;
                final int statusTmp = status;
                that.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        that.connectBack.onDescriptorRead(descriptorTmp, statusTmp);
                    }
                });

            }
        }

        public void onDescriptorWrite(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattDescriptor descriptor, int status) {
            if(that.connectBack != null) {
                final android.bluetooth.BluetoothGattDescriptor descriptorTmp = descriptor;
                final int statusTmp = status;
                that.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        that.connectBack.onDescriptorWrite(descriptorTmp, statusTmp);
                    }
                });

            }
        }

        public void onReliableWriteCompleted(android.bluetooth.BluetoothGatt gatt, int status) {
            if (gatt != null && gatt.getDevice() != null && that.connectBack != null) {
                final String devAddress = gatt.getDevice().getAddress();
                final int statusTmp = status;
                that.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        that.connectBack.onReliableWriteCompleted(devAddress, statusTmp);
                    }
                });
            }
        }

        public void onReadRemoteRssi(android.bluetooth.BluetoothGatt gatt, int rssi, int status) {
            if (gatt != null && gatt.getDevice() != null && that.connectBack != null) {
                final String devAddress = gatt.getDevice().getAddress();
                final int statusTmp = status;
                final int rssiTmp = rssi;
                that.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        that.connectBack.onReadRemoteRssi(devAddress, rssiTmp, statusTmp);
                    }
                });
            }
        }

        public void onMtuChanged(android.bluetooth.BluetoothGatt gatt, int mtu, int status) {
            if (gatt != null && gatt.getDevice() != null && that.connectBack != null) {
                final String devAddress = gatt.getDevice().getAddress();
                final int statusTmp = status;
                final int mtuTmp = mtu;
                that.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        that.connectBack.onMtuChanged(devAddress, mtuTmp, statusTmp);
                    }
                });
            }
        }
    };

    private void debug_print(String who, String what){
        Log.i(who, what);
    }
}

    /*


    @Override
    public void onCharacterRead(android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {
             String reply = "000000000011111111112222222222333333333344444444445555555555666666666677777777778888888888999999999900000000001111111111222222222233333333334444444444555555555566666666667777777777888888888899999999990000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999000000000011111111112222222222333333333344444444445555555555666666666677777777778888888888999999999900000000001111111111222222222233333333334444444444555555555566666666667777777777888888888899999999990000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999";
            characteristic.setValue(reply.getBytes());
            gatt.writeCharacteristic(characteristic);

    }*/
