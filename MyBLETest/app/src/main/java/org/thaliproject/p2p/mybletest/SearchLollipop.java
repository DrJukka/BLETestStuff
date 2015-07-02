package org.thaliproject.p2p.mybletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by juksilve on 20.4.2015.
 */
public class SearchLollipop {

    SearchLollipop that = this;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Context context = null;
    private BLEBase.CallBack callBack = null;


    public SearchLollipop(Context Context, BLEBase.CallBack CallBack) {
        this.context = Context;
        this.callBack = CallBack;

        BluetoothManager manager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    public void Start() {

      /*  ScanFilter beaconFilter = null;//new ScanFilter.Builder().setServiceUuid(TemperatureBeacon.THERM_SERVICE).build();
        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(beaconFilter);
*/
        ArrayList<ScanFilter> filters = null;

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        this.callBack.Debug("Search", "Starting Scan");
        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
    }

    public void Stop() {
        mBluetoothLeScanner.stopScan(mScanCallback);
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            that.callBack.Debug("Search", "onScanResult content: " + result.describeContents() + ", rssi: " + result.getRssi() + ", time : " + result.getTimestampNanos());

            BluetoothDevice dev = result.getDevice();
            ScanRecord rec = result.getScanRecord();
            that.callBack.Debug("Search", "Device name:" + rec.getDeviceName() + ", address: " + dev.getAddress());// +  ", scan record : " + rec.toString());
            that.callBack.Debug("Search", "advertFlags: " + rec.getAdvertiseFlags());

            List<ParcelUuid> serv = rec.getServiceUuids();
            Map<ParcelUuid, byte[]> mapp = rec.getServiceData();

            for(int i = 0; i < serv.size(); i++){
                String datttaa = "";
                if(serv.get(i) != null) {
                    if(mapp.get(serv.get(i)) != null) {
                        datttaa = new String(mapp.get(serv.get(i)));
                    }
                }
                that.callBack.Debug("Search", "UUID : " + serv.get(i).toString() + ", data: " + datttaa);
            }
        }

        public void onBatchScanResults(java.util.List<android.bluetooth.le.ScanResult> results) {
            that.callBack.Debug("Search", "onBatchScanResults : " + results.size());
        }

        public void onScanFailed(int errorCode) {
            that.callBack.Debug("Search", "onScanFailed : " + errorCode);
        }
    };
}
