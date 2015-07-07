package org.thaliproject.p2p.mybletest;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.util.List;
import java.util.Map;

/**
 * Created by juksilve on 20.4.2015.
 */

/*
Disconnect 0x13 with Nexus 5 having lollipop
https://code.google.com/p/android/issues/detail?id=156730

 */

public class SearchKitKat {

    SearchKitKat that = this;

    BluetoothAdapter btAdapter = null;
    BluetoothGatt bluetoothGatt = null;

    private Context context = null;
    private BLEBase.CallBack callBack = null;

    public SearchKitKat(Context Context, BLEBase.CallBack CallBack) {
        this.context = Context;
        this.callBack = CallBack;

        BluetoothManager btManager = (BluetoothManager)this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
    }

    public void Start() {
        if(bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        btAdapter.startLeScan(leScanCallback);
    }

    public void Stop() {
        btAdapter.stopLeScan(leScanCallback);

        if(bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            // your implementation here
            that.callBack.debug("ADAPTER", "onLeScan device: " + device.getName() + ", RSSI: " + rssi);
            Map <Integer,String> parseRecord = BLEBase.ParseRecord(scanRecord);
            String uuidFound = BLEBase.getServiceUUID(parseRecord);
            that.callBack.debug("ADAPTER", "Service UID = " + uuidFound);

            if(uuidFound.equalsIgnoreCase(MainActivity.SERVICE_UUID_1)) {
                btAdapter.stopLeScan(leScanCallback);
                if (bluetoothGatt == null) {
                    that.callBack.debug("ADAPTER", "connectGatt");
                    bluetoothGatt = device.connectGatt(that.context, false, gattCallback);
                }
            }
        }
    };

    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            that.callBack.debug("BLE", "onCharacteristicChanged Uuid: " + characteristic.getUuid() + ", value: " + characteristic.getValue());
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
            that.callBack.debug("BLE", "onConnectionStateChange status: " + BLEBase.getGATTStatus(status) + ", newsState: " + BLEBase.getConnectionState(newState));

            if (newState == BluetoothProfile.STATE_CONNECTED && gatt != null) {
                that.callBack.debug("ADAPTER", "Starting to discover services");
                gatt.discoverServices();
                //gatt.readRemoteRssi();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a            BluetoothGatt.discoverServices() call
            that.callBack.debug("BLE", "onServicesDiscovered, status: " + BLEBase.getGATTStatus(status));

            boolean readingCharactes = false;
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                //lets first find our service
                if (MainActivity.SERVICE_UUID_1.equalsIgnoreCase(service.getUuid().toString())) {
                    that.callBack.debug("BLE", "found service");

                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                            for (BluetoothGattCharacteristic characteristic : characteristics) {
                                if (characteristic != null) {
                            // lets find our own characteristics
                            that.callBack.debug("BLE", "chara : " + characteristic.getUuid().toString());
                            if (MainActivity.CharacteristicsUID1.equalsIgnoreCase(characteristic.getUuid().toString())) {
                                that.callBack.debug("BLE", "found characteristic, permissions: " + characteristic.getPermissions());
                                gatt.setCharacteristicNotification(characteristic, true);
                                gatt.readCharacteristic(characteristic);
                                readingCharactes = true;
                                /*
                                 for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                                     if (descriptor != null) {
                                         // lets find our own characteristics
                                         if (MainActivity.DescriptorUID.equalsIgnoreCase(descriptor.getUuid().toString())) {
                                             that.callBack.Debug("BLE", "found descriptor");
                                             gatt.readDescriptor(descriptor);
                                             break;
                                         }
                                     }
                                 }*/
                            }
                        }
                    }
                }
                if (readingCharactes) {
                    break;
                }
            }
        }

        public void onCharacteristicRead(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {
            that.callBack.debug("BLE", "onCharacteristicRead: " + characteristic.getValue() + "(" + characteristic.getUuid() + ") status: " + BLEBase.getGATTStatus(status));

            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {

                String tempString = new String(data);

                that.callBack.debug("BLE", "Value(" + data.length + "): " + tempString);
            } else {
                if (data == null) {
                    that.callBack.debug("BLE", "Value : Is NULL");
                } else if (data.length <= 0) {
                    that.callBack.debug("BLE", "Value : Lenght is ZERO");
                }
            }

            String reply = "000000000011111111112222222222333333333344444444445555555555666666666677777777778888888888999999999900000000001111111111222222222233333333334444444444555555555566666666667777777777888888888899999999990000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999000000000011111111112222222222333333333344444444445555555555666666666677777777778888888888999999999900000000001111111111222222222233333333334444444444555555555566666666667777777777888888888899999999990000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999";
            characteristic.setValue(reply.getBytes());
            gatt.writeCharacteristic(characteristic);

        }

        public void onCharacteristicWrite(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {
            that.callBack.debug("BLE", "onCharacteristicWrite: " + characteristic.getValue() + "(" + characteristic.getUuid() + ") status: " + BLEBase.getGATTStatus(status));

            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                String tempString = new String(data);
                that.callBack.debug("BLE", "Value(" + data.length + ")" + tempString);
            } else {
                if (data == null) {
                    that.callBack.debug("BLE", "Value : Is NULL");
                } else if (data.length <= 0) {
                    that.callBack.debug("BLE", "Value : Lenght is ZERO");
                }
            }
        }

        public void onDescriptorRead(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattDescriptor descriptor, int status) {
            that.callBack.debug("BLE", "onDescriptorRead: " + descriptor.getValue() + "(" + descriptor.getUuid() + ") status: " + BLEBase.getGATTStatus(status));

            byte[] data = descriptor.getValue();
            if (data != null && data.length > 0) {
                String tempString = new String(data);
                that.callBack.debug("BLE", "Value(" + data.length + ")" + tempString);
            } else {
                if (data == null) {
                    that.callBack.debug("BLE", "Value : Is NULL");
                } else if (data.length <= 0) {
                    that.callBack.debug("BLE", "Value : Lenght is ZERO");
                }
            }
        }

        public void onDescriptorWrite(android.bluetooth.BluetoothGatt gatt, android.bluetooth.BluetoothGattDescriptor descriptor, int status) {
            that.callBack.debug("BLE", "onDescriptorWrite: " + descriptor.getValue() + "(" + descriptor.getUuid() + ") status: " + BLEBase.getGATTStatus(status));

            byte[] data = descriptor.getValue();
            if (data != null && data.length > 0) {
                String tempString = new String(data);
                that.callBack.debug("BLE", "Value(" + data.length + ")" + tempString);
            } else {
                if (data == null) {
                    that.callBack.debug("BLE", "Value : Is NULL");
                } else if (data.length <= 0) {
                    that.callBack.debug("BLE", "Value : Lenght is ZERO");
                }
            }
        }

        public void onReliableWriteCompleted(android.bluetooth.BluetoothGatt gatt, int status) {
            that.callBack.debug("BLE", "onReliableWriteCompleted: , status: " + BLEBase.getGATTStatus(status));
        }

        public void onReadRemoteRssi(android.bluetooth.BluetoothGatt gatt, int rssi, int status) {
            that.callBack.debug("BLE", "onReadRemoteRssi :" + rssi + ", status: " + BLEBase.getGATTStatus(status));
        }

        public void onMtuChanged(android.bluetooth.BluetoothGatt gatt, int mtu, int status) {
            that.callBack.debug("BLE", "onMtuChanged :" + mtu + ", status: " + BLEBase.getGATTStatus(status));
        }
    };
}
