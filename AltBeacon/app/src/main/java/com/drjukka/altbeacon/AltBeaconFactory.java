package com.drjukka.altbeacon;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by juksilve on 19.11.2015.
 */
public class AltBeaconFactory {

    public static boolean isBLESupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static double calculateDistanceFromRssi(double rawSignalStrengthInDBm, int measuredPower)
    {
        double distance = 0d;
        double near = rawSignalStrengthInDBm / measuredPower;

        if (near < 1.0f)
        {
            distance = Math.pow(near, 10);
        }
        else
        {
            distance = ((0.89976f) * Math.pow(near, 7.7095f) + 0.111f);
        }

        return distance;
    }

    static public boolean isLengthAndTypeOk(byte[] scanRecord){
        if(scanRecord == null || scanRecord.length  < 26){
            return false;
        }

        //if the actual data record length is smaller than what we expect
        // or if the tyoe is set to something else than what we expect
        if(scanRecord[0] < 26 && scanRecord[1] != 0xFF){
            return false;
        }

        return true;
    }

    static public String getManufacturer(byte[] scanRecord){
        return getStringPart(3, 3, scanRecord) + getStringPart(2, 2, scanRecord); //little endian
    }

    static public String getBeaconCode(byte[] scanRecord){
        return getStringPart(4, 5, scanRecord);
    }

    /// The expected specification of the data is as follows:
    ///
    /// Byte(s)     Name
    /// --------------------------
    /// 0-1         Manufacturer ID (16-bit unsigned integer, big endian)
    /// 2-3         Beacon code (two 8-bit unsigned integers, but can be considered as one 16-bit unsigned integer in little endian)
    /// 4-19        ID1 (UUID)
    /// 20-21       ID2 (16-bit unsigned integer, big endian)
    /// 22-23       ID3 (16-bit unsigned integer, big endian)
    /// 24          Measured Power (signed 8-bit integer)
    /// 25          Reserved for use by the manufacturer to implement special features (optional)
    ///
    /// For more details on the beacon specifications see https://github.com/AltBeacon/spec

    public static AltBeacon getBeaconFromScanrecord(BluetoothDevice device,byte[] scanRecord,final int rssi) {

        String id1 = getStringPart(6, 9, scanRecord) + "-" + getStringPart(10, 11, scanRecord) + "-" + getStringPart(12, 13, scanRecord) + "-" + getStringPart(14, 15, scanRecord) + "-" + getStringPart(16, 21, scanRecord);
        int id2 = ((scanRecord[23] & 0xFF) + ((scanRecord[22] & 0xFF) << 8)); // Uint16
        int id3 = ((scanRecord[25] & 0xFF) + ((scanRecord[24] & 0xFF) << 8)); // Uint16

        long Distance = Math.round(AltBeaconFactory.calculateDistanceFromRssi(rssi, scanRecord[26]));
        return new AltBeacon(device,scanRecord[1],getManufacturer(scanRecord),getBeaconCode(scanRecord),id1,id2,id3,scanRecord[26],scanRecord[27],Distance);
    }

    /*
    WE are adding the beacon as manufacturer data (type == 0xff),
    Android appears to add the manufacturer right after the type, and then add the data we give
    thus we need to start the Beacon code
     */
    public static  byte[]  getAdvertDataFromBeacon(AltBeacon beacon) {
        byte[] ret = new byte[24];
        //beaconCode
        int beaconCode = Integer.decode("0x"+ beacon.getBeaconCode());
        ret[0] = (byte)((beaconCode >> 8) & 0xff);
        ret[1] = (byte)(beaconCode & 0xff);
        // id1 2 - 17
        String tmpString = beacon.getId1().replaceAll("[\\s\\-]", "");
        int index = 2;
        for(int i = 2; i <= tmpString.length(); i = i + 2){
            if(i <= 32){
                ret[index] = (byte) (Integer.decode("0x" + tmpString.substring((i - 2), i)) & 0xff);
                index++;
            }
        }
        //id2
        int id2 = beacon.getId2();
        ret[18] = (byte)((id2 >> 8) & 0xff);
        ret[19] = (byte)(id2 & 0xff);
        //id3
        int id3 = beacon.getId3();
        ret[20] = (byte)((id3 >> 8) & 0xff);
        ret[21] = (byte)(id3 & 0xff);
        //refRSSI
        ret[22] = (byte)beacon.getRefRSSI();
        ret[23] = (byte)beacon.getManufacturerReserved();
        return ret;
    }

    static private String getStringPart(int start, int end, byte[] record){

        end = end + 1;
        if(start < 0 || end < start){
            return null;
        }

        if(record.length < end){
            return null;
        }

        StringBuilder hex = new StringBuilder((end - start) * 2);

        for(int i = start; i < end ; i++){
            hex.append(String.format("%02X", record[i]));
        }

        return hex.toString();
    }
}
