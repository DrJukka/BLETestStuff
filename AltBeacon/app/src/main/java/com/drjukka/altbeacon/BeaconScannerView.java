package com.drjukka.altbeacon;

import android.bluetooth.BluetoothManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;

/**
 * Created by juksilve on 20.11.2015.
 */
public class BeaconScannerView  extends Fragment implements BeaconScannerCallback {

    private final String TAG = "BeaconScannerView";

    private ArrayList<AltBeacon> listItems = null;
    private MainListAdapter adapter = null;
    private ListView listView = null;

    private BluetoothManager mBluetoothManager = null;
    private BeaconScanner mBeaconScanner = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBluetoothManager = (BluetoothManager) getActivity().getApplicationContext().getSystemService(getActivity().getApplicationContext().BLUETOOTH_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.scanner_tab, container, false);

        Button toggleButton = (Button) v.findViewById(R.id.scannerToggle);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBeaconScanner == null) {
                    debugData("Starting Scanner");
                    StartScanner();
                } else {
                    debugData("Stopping Scanner");
                    StopScanner();
                }
            }
        });

        listView = (ListView) v.findViewById(R.id.list);
        listItems = new ArrayList<AltBeacon>();
        adapter = new MainListAdapter(getActivity(),listItems);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // ListView Clicked item value
                AltBeacon itemValue = (AltBeacon) listView.getItemAtPosition(position);

                String showText = "Id1  :" + itemValue.getId1() + "  id2 : " + itemValue.getId2() + "  id3 : " + itemValue.getId3();
                Toast.makeText(getActivity().getApplicationContext(),showText, Toast.LENGTH_LONG).show();
            }

        });

        return v;
    }

    private void StartScanner() {
        StopScanner();
        BeaconScanner tmpBeaconScanner = new BeaconScanner(getActivity().getApplicationContext(), this, this.mBluetoothManager);
        tmpBeaconScanner.Start();
        mBeaconScanner = tmpBeaconScanner;
    }

    private void StopScanner() {
        BeaconScanner tmpScanner = mBeaconScanner;
        mBeaconScanner = null;
        if(tmpScanner != null){
            tmpScanner.Stop();
        }

        listItems.clear();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void BeaconDiscovered(AltBeacon beacon) {

        if(beacon == null && beacon.getDevice() != null){
            return;
        }

        debugData("BeaconDiscovered : " + beacon.getDevice().getAddress());
        debugData("Id1:" + beacon.getId1());
        debugData("Id2:" + beacon.getId2() + ", Id2:" + beacon.getId2());
        debugData("getBeaconCode:" + beacon.getBeaconCode());

        if(!beacon.getBeaconCode().equalsIgnoreCase(MainActivity.BEACON_CODE)){
            //not what we look for in this app
            return;
        }

        for(int i = 0; i < listItems.size();i++){
            if(listItems.get(i).getId1().equalsIgnoreCase(beacon.getId1())){
                listItems.remove(i);
                listItems.add(i, beacon);
                adapter.notifyDataSetChanged();
                return;
            }
        }
        listItems.add(beacon);
        adapter.notifyDataSetChanged();

        debugData("listItems count:" + listItems.size());
    }

    @Override
    public void debugData(String data) {
        Log.i(TAG, data);
    }
}
