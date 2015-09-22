package com.example.myapplication2.app;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by juksilve on 20.4.2015.
 */
public class BLEBase {

    //this is out globally unique Service UUID, which is used for determining that the BLE device is running our service
    // the actual values are delivered through the characteristics
    static public final String SERVICE_UUID_1      = "010500a1-00b0-1000-8000-00805f9b34fb";
    static public final String CharacteristicsUID1 = "46651222-96e0-4aca-a710-8f35f7e702b9";
    static public final String CharacteristicsUID2 = "46651333-96e0-4aca-a710-8f35f7e702b9";

    public static boolean isBLESupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    @TargetApi(18)
    @SuppressLint("NewApi")
    public static boolean isBLEAdvertisingSupported(Context context) {

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            return false;
        }

        BluetoothManager tmpMan = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (tmpMan == null) {
            return false;
        }

        BluetoothAdapter tmpAdapter = tmpMan.getAdapter();
        if (tmpAdapter == null ) {
            return false;
        }
        return tmpAdapter.isMultipleAdvertisementSupported();
    }

    public static String getConnectionStateAsString(int State) {
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
                return "Uknown(" + String.format("%02X", State)+ ").";

        }
    }

    public static String getGATTStatusAsString(int State) {
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
                return "Uknown(" + String.format("%02X", State)+ ").";
        }
    }

    public static String getDeviceNameOrAddressAsString(String deviceAddress,Context context) {

        BluetoothManager tmpBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (tmpBluetoothManager == null) {
            return null;
        }

        BluetoothDevice tmpDev = tmpBluetoothManager.getAdapter().getRemoteDevice(deviceAddress);
        if (tmpDev != null && tmpDev.getName() != null) {
            return tmpDev.getName();
        }

        return deviceAddress;
    }

    /*
    BLE Scan record type IDs
    data from:
    https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile
    */
    static final int EBLE_FLAGS           = 0x01;//«Flags»	Bluetooth Core Specification:
    static final int EBLE_16BitUUIDInc    = 0x02;//«Incomplete List of 16-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_16BitUUIDCom    = 0x03;//«Complete List of 16-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_32BitUUIDInc    = 0x04;//«Incomplete List of 32-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_32BitUUIDCom    = 0x05;//«Complete List of 32-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_128BitUUIDInc   = 0x06;//«Incomplete List of 128-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_128BitUUIDCom   = 0x07;//«Complete List of 128-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_SHORTNAME       = 0x08;//«Shortened Local Name»	Bluetooth Core Specification:
    static final int EBLE_LOCALNAME       = 0x09;//«Complete Local Name»	Bluetooth Core Specification:
    static final int EBLE_TXPOWERLEVEL    = 0x0A;//«Tx Power Level»	Bluetooth Core Specification:
    static final int EBLE_DEVICECLASS     = 0x0D;//«Class of Device»	Bluetooth Core Specification:
    static final int EBLE_SIMPLEPAIRHASH  = 0x0E;//«Simple Pairing Hash C»	Bluetooth Core Specification:​«Simple Pairing Hash C-192»	​Core Specification Supplement, Part A, section 1.6
    static final int EBLE_SIMPLEPAIRRAND  = 0x0F;//«Simple Pairing Randomizer R»	Bluetooth Core Specification:​«Simple Pairing Randomizer R-192»	​Core Specification Supplement, Part A, section 1.6
    static final int EBLE_DEVICEID        = 0x10;//«Device ID»	Device ID Profile v1.3 or later,«Security Manager TK Value»	Bluetooth Core Specification:
    static final int EBLE_SECURITYMANAGER = 0x11;//«Security Manager Out of Band Flags»	Bluetooth Core Specification:
    static final int EBLE_SLAVEINTERVALRA = 0x12;//«Slave Connection Interval Range»	Bluetooth Core Specification:
    static final int EBLE_16BitSSUUID     = 0x14;//«List of 16-bit Service Solicitation UUIDs»	Bluetooth Core Specification:
    static final int EBLE_128BitSSUUID    = 0x15;//«List of 128-bit Service Solicitation UUIDs»	Bluetooth Core Specification:
    static final int EBLE_SERVICEDATA     = 0x16;//«Service Data»	Bluetooth Core Specification:​«Service Data - 16-bit UUID»	​Core Specification Supplement, Part A, section 1.11
    static final int EBLE_PTADDRESS       = 0x17;//«Public Target Address»	Bluetooth Core Specification:
    static final int EBLE_RTADDRESS       = 0x18;;//«Random Target Address»	Bluetooth Core Specification:
    static final int EBLE_APPEARANCE      = 0x19;//«Appearance»	Bluetooth Core Specification:
    static final int EBLE_DEVADDRESS      = 0x1B;//«​LE Bluetooth Device Address»	​Core Specification Supplement, Part A, section 1.16
    static final int EBLE_LEROLE          = 0x1C;//«​LE Role»	​Core Specification Supplement, Part A, section 1.17
    static final int EBLE_PAIRINGHASH     = 0x1D;//«​Simple Pairing Hash C-256»	​Core Specification Supplement, Part A, section 1.6
    static final int EBLE_PAIRINGRAND     = 0x1E;//«​Simple Pairing Randomizer R-256»	​Core Specification Supplement, Part A, section 1.6
    static final int EBLE_32BitSSUUID     = 0x1F;//​«List of 32-bit Service Solicitation UUIDs»	​Core Specification Supplement, Part A, section 1.10
    static final int EBLE_32BitSERDATA    = 0x20;//​«Service Data - 32-bit UUID»	​Core Specification Supplement, Part A, section 1.11
    static final int EBLE_128BitSERDATA   = 0x21;//​«Service Data - 128-bit UUID»	​Core Specification Supplement, Part A, section 1.11
    static final int EBLE_SECCONCONF      = 0x22;//​«​LE Secure Connections Confirmation Value»	​Core Specification Supplement Part A, Section 1.6
    static final int EBLE_SECCONRAND      = 0x23;//​​«​LE Secure Connections Random Value»	​Core Specification Supplement Part A, Section 1.6​
    static final int EBLE_3DINFDATA       = 0x3D;//​​«3D Information Data»	​3D Synchronization Profile, v1.0 or later
    static final int EBLE_MANDATA         = 0xFF;//«Manufacturer Specific Data»	Bluetooth Core Specification:

    /*
    BLE Scan record parsing
    inspired by:
    http://stackoverflow.com/questions/22016224/ble-obtain-uuid-encoded-in-advertising-packet
     */
    static public Map<Integer,String> ParseRecord(byte[] scanRecord){
        Map<Integer,String> ret = new HashMap<Integer,String>();
        int index = 0;
        while (index < scanRecord.length) {
            int length = scanRecord[index++];
            //Zero value indicates that we are done with the record now
            if (length == 0) break;

            int type = scanRecord[index];
            //if the type is zero, then we are pass the significant section of the data,
            // and we are thud done
            if (type == 0) break;

            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);
            if(data != null && data.length > 0) {
                StringBuilder hex = new StringBuilder(data.length * 2);
                // the data appears to be there backwards
                for (int bb = data.length- 1; bb >= 0; bb--){
                    hex.append(String.format("%02X", data[bb]));
                }
                ret.put(type,hex.toString());
            }
            index += length;
        }

        return ret;
    }

    static public String getServiceUUID(Map<Integer,String> record){
        String ret = "";
        // for example: 0105FACB00B01000800000805F9B34FB --> 010510ee-0000-1000-8000-00805f9b34fb
        if(record.containsKey(EBLE_128BitUUIDCom)){
            String tmpString= record.get(EBLE_128BitUUIDCom).toString();
            ret = tmpString.substring(0, 8) + "-" + tmpString.substring(8,12)+ "-" + tmpString.substring(12,16)+ "-" + tmpString.substring(16,20)+ "-" + tmpString.substring(20,tmpString.length());
            //010510EE --> 010510ee-0000-1000-8000-00805f9b34fb
        }else if(record.containsKey(EBLE_32BitUUIDCom)){
            ret = record.get(EBLE_32BitUUIDCom).toString() + "-0000-1000-8000-00805f9b34fb";
        }
        return ret;
    }
}
