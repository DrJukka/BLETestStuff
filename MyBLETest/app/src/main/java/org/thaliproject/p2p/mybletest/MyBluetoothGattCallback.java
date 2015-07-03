package org.thaliproject.p2p.mybletest;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.util.List;

/**
 * Created by juksilve on 2.7.2015.
 */
public class MyBluetoothGattCallback extends BluetoothGattCallback {

    MyBluetoothGattCallback that = this;
    private BLEBase.CallBack callBack = null;
    public MyBluetoothGattCallback(BLEBase.CallBack Callback){
        that.callBack = Callback;
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        // this will get called anytime you perform a read or write characteristic operation
        that.callBack.Debug("BLE","onCharacteristicChanged Uuid: " + characteristic.getUuid() + ", value: " + characteristic.getValue());
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        // this will get called when a device connects or disconnects
        that.callBack.Debug("BLE","onConnectionStateChange status: " + BLEBase.getGATTStatus(status) + ", newsState: " + BLEBase.getConnectionState(newState));

        if(newState == BluetoothProfile.STATE_CONNECTED && gatt != null){
            that.callBack.Debug("ADAPTER", "Starting to discover services");
            gatt.discoverServices();
            //gatt.readRemoteRssi();
        }
    }

    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
        // this will get called after the client initiates a            BluetoothGatt.discoverServices() call
        that.callBack.Debug("BLE","onServicesDiscovered, status: " + BLEBase.getGATTStatus(status));

        boolean readingCharactes = false;
        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService service : services) {
            //lets first find our service
            if(MainActivity.SERVICE_UUID_1.equalsIgnoreCase(service.getUuid().toString())) {
                that.callBack.Debug("BLE", "found service");

                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    if (characteristic != null) {
                        // lets find our own characteristics
                        that.callBack.Debug("BLE", "chara : " +characteristic.getUuid().toString());
                        if(MainActivity.CharacteristicsUID1.equalsIgnoreCase(characteristic.getUuid().toString())) {
                            that.callBack.Debug("BLE", "found characteristic, permissions: " + characteristic.getPermissions());
                            gatt.setCharacteristicNotification(characteristic, true);
                            gatt.readCharacteristic(characteristic);
                            readingCharactes = true;

                            for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                                if (descriptor != null) {
                                    // lets find our own characteristics
                                    if (MainActivity.DescriptorUID.equalsIgnoreCase(descriptor.getUuid().toString())) {
                                        that.callBack.Debug("BLE", "found descriptor 1");
                                        // gatt.readDescriptor(descriptor);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(readingCharactes){
                break;
            }
        }
    }

    public void onCharacteristicRead(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {
        that.callBack.Debug("BLE","onCharacteristicRead: " + characteristic.getValue() + "(" + characteristic.getUuid()  + ") status: " + BLEBase.getGATTStatus(status));

    /*    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
            if (descriptor != null) {
                // lets find our own characteristics
                if(MainActivity.DescriptorUID.equalsIgnoreCase(descriptor.getUuid().toString())) {
                    that.callBack.Debug("BLE", "found descriptor");
                    gatt.readDescriptor(descriptor);
                    break;
                }
            }
        }*/
    }

    public void onCharacteristicWrite(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {
        that.callBack.Debug("BLE","onCharacteristicWrite: " + characteristic.getValue() + "(" + characteristic.getUuid()  + ") status: " + BLEBase.getGATTStatus(status));
    }

    public void onDescriptorRead(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattDescriptor descriptor, int status) {
        that.callBack.Debug("BLE","onDescriptorRead: " +  descriptor.getValue() + "(" + descriptor.getUuid()  + ") status: " + BLEBase.getGATTStatus(status));
    }

    public void onDescriptorWrite(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattDescriptor descriptor, int status) {
        that.callBack.Debug("BLE","onDescriptorWrite: " + descriptor.getValue() + "(" + descriptor.getUuid()  + ") status: " + BLEBase.getGATTStatus(status));
    }

    public void onReliableWriteCompleted(android.bluetooth.BluetoothGatt gatt, int status) {
        that.callBack.Debug("BLE","onReliableWriteCompleted: , status: " + BLEBase.getGATTStatus(status));
    }

    public void onReadRemoteRssi(android.bluetooth.BluetoothGatt gatt, int rssi, int status) {
        that.callBack.Debug("BLE","onReadRemoteRssi :" + rssi + ", status: " + BLEBase.getGATTStatus(status));
    }

    public void onMtuChanged(android.bluetooth.BluetoothGatt gatt, int mtu, int status) {
        that.callBack.Debug("BLE","onMtuChanged :" + mtu + ", status: " + BLEBase.getGATTStatus(status));
    }
}
