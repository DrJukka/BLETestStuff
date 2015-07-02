package org.thaliproject.p2p.mybletest;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by juksilve on 20.4.2015.
 */
public class BLEBase {

    interface CallBack{
        public void Debug(String who,String text);
    }

    public static boolean isBLESupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static String getConnectionState(int State) {
        switch (State) {
            case BluetoothProfile.STATE_DISCONNECTED:
                return "DisConnected";
            case BluetoothProfile.STATE_CONNECTING:
                return "Connecting";
            case BluetoothProfile.STATE_CONNECTED:
                return "Connected";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "Disconnecting";
            default:
                return "Uknown";

        }
    }

    public static String getGATTStatus(int State) {
        switch (State) {
            case BluetoothGatt.GATT_SUCCESS:
                return "SUCCESS";
            case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                return "READ_NOT_PERMITTED";
            case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                return "WRITE_NOT_PERMITTED";
            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                return "INSUFFICIENT_AUTHENTICATION";
            case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                return "REQUEST_NOT_SUPPORTED";
            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                return "INSUFFICIENT_ENCRYPTION";
            case BluetoothGatt.GATT_INVALID_OFFSET:
                return "INVALID_OFFSET";
            case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
                return "INVALID_ATTRIBUTE_LENGTH";
            case BluetoothGatt.GATT_CONNECTION_CONGESTED:
                return "CONNECTION_CONGESTED";
            case BluetoothGatt.GATT_FAILURE:
                return "FAILURE";
            default:
                return "Uknown(" + String.format("%02X", State )+ ").";
        }
    }
}
