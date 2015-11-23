package com.drjukka.altbeacon;

import android.bluetooth.BluetoothManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by juksilve on 20.11.2015.
 */
public class BeaconAdvertiseView extends Fragment implements BeaconAdvertiserCallback {

    private final String TAG = "BeaconAdvertiseView";

    private BluetoothManager mBluetoothManager = null;
    private BeaconAdvertiser mBeaconAdvertiser = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBluetoothManager = (BluetoothManager) getActivity().getApplicationContext().getSystemService(getActivity().getApplicationContext().BLUETOOTH_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.advertise_tab, container, false);

        final EditText id1Text = (EditText) v.findViewById(R.id.id1box);
        final EditText id2Text = (EditText) v.findViewById(R.id.id2box);
        final EditText id3Text = (EditText) v.findViewById(R.id.id3box);

        Button toggleButton = (Button) v.findViewById(R.id.advertToggle);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBeaconAdvertiser == null) {
                    debugData(" Starting Advertising");
                    StartAdvertiser(id1Text.getText().toString(), id2Text.getText().toString(), id3Text.getText().toString());
                    id1Text.setEnabled(false);
                    id2Text.setEnabled(false);
                    id3Text.setEnabled(false);

                } else {
                    debugData(" Stopping Advertising");
                    StopAdvertiser();
                    id1Text.setEnabled(true);
                    id2Text.setEnabled(true);
                    id3Text.setEnabled(true);
                }
            }
        });

        return v;
    }

    private void StartAdvertiser(String id1,String id2,String id3){
        StopAdvertiser();

        //API is not available before Lollipop
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            BeaconAdvertiser tmpAdvertiserLollipop = new BeaconAdvertiser(getActivity().getApplicationContext(), this, mBluetoothManager,MainActivity.BEACON_CODE,MainActivity.REFERENCE_RRSI,MainActivity.BEACON_MANUFACTURER);
            // tmpAdvertiserLollipop.addService(mFirstService);
            tmpAdvertiserLollipop.Start(id1,Integer.decode(id2),Integer.decode(id3));
            mBeaconAdvertiser = tmpAdvertiserLollipop;
        }else{
            onAdvertisingStarted("Advertisement Not supported by platform version : " + Build.VERSION.SDK_INT);
        }
    }

    private void StopAdvertiser(){
        BeaconAdvertiser tmpAdvertiser = mBeaconAdvertiser;
        mBeaconAdvertiser = null;
        if(tmpAdvertiser != null){
            tmpAdvertiser.Stop();
        }
    }

    @Override
    public void onAdvertisingStarted(String error) {
        if(error != null) {
            Toast.makeText(getActivity().getApplicationContext(), "Can not start advertising : " + error, Toast.LENGTH_LONG).show();
        }
        debugData("onAdvertisingStarted : " + error);
    }

    @Override
    public void onAdvertisingStopped(String error) {
        debugData("onAdvertisingStopped : " + error);
    }

    @Override
    public void debugData(String data) {
        Log.i(TAG,data);
    }
}


