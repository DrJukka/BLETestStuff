package org.thaliproject.p2p.mybletest;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;


public class MainActivity extends ActionBarActivity implements BLEBase.CallBack, AdvertiserLollipop.BLEAdvertiserCallback {

    MainActivity that = this;

    static public String SERVICE_UUID_1      = "0105facb-00b0-1000-8000-00805f9b34fb";

    static public String CharacteristicsUID1  = "46651222-96e0-4aca-a710-8f35f7e702b9";
    static public String CharacteristicsNAME1 = "1st-Char";

    static public String DescriptorUID        = "f360ef7a-52c2-442a-95c5-3048b161ed1d";
    static public String DescriptorNAME1      = "1st-Desc";

    static public String unknown              = "UNKNOWN";


    final int REQUEST_ENABLE_BT = 1;

    SearchKitKat mSearchKitKat = null;
    AdvertiserLollipop mAdvertiserLollipop = null;

    boolean leScanActive= false;

    private int mInterval = 1000; // 1 second by default, can be changed later
    private Handler timeHandler;
    private int timeCounter = 0;
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            // call function to update timer
            timeCounter = timeCounter + 1;
            ((TextView) findViewById(R.id.TimeBox)).setText("T: " + timeCounter);

            timeHandler.postDelayed(mStatusChecker, mInterval);
        }
    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button clearButton = (Button) findViewById(R.id.button2);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((TextView) findViewById(R.id.debugdataBox)).setText("");
            }
        });

        Button showIPButton = (Button) findViewById(R.id.button3);
        showIPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(leScanActive){
                    leScanActive = false;
                    debug_Text("BUT","Stopping advertising");
                    if(mAdvertiserLollipop != null){
                        mAdvertiserLollipop.Stop();
                        mAdvertiserLollipop = null;
                    }

                    debug_Text("BUT","Stopping LE scan");
                    if(mSearchKitKat != null){
                        mSearchKitKat.Stop();
                        mSearchKitKat = null;
                    }


                }else{
                    leScanActive = true;

                    debug_Text("BUT","Starting LE scan");

                    BluetoothGattService firstService = new BluetoothGattService(UUID.fromString(SERVICE_UUID_1),BluetoothGattService.SERVICE_TYPE_PRIMARY);
                    // alert level char.
                    BluetoothGattCharacteristic firstServiceChar = new BluetoothGattCharacteristic(
                            UUID.fromString(CharacteristicsUID1)
                            ,BluetoothGattCharacteristic.PROPERTY_READ |BluetoothGattCharacteristic.PROPERTY_WRITE
                            ,BluetoothGattCharacteristic.PERMISSION_READ |BluetoothGattCharacteristic.PERMISSION_WRITE);

                    // max 600 gets delivered in 22 byte chunks,
                    // if 609 is defined then 'android.os.DeadObjectException' will be throun in BluetoothGattServer::sendResponse
                    String data1 = "humppaan itsekseni";
                    firstServiceChar.setValue(data1.getBytes());

                    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString(DescriptorUID)
                             , BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);


                   // max 600 gets delivered in 22 byte chunks,
                   // if 609 is defined then 'android.os.DeadObjectException' will be throun in BluetoothGattServer::sendResponse
                    String data = "000000000011111111112222222222333333333344444444445555555555666666666677777777778888888888999999999900000000001111111111222222222233333333334444444444555555555566666666667777777777888888888899999999990000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999000000000011111111112222222222333333333344444444445555555555666666666677777777778888888888999999999900000000001111111111222222222233333333334444444444555555555566666666667777777777888888888899999999990000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999";
                    descriptor.setValue(data.getBytes());
                    firstServiceChar.addDescriptor(descriptor);

                    firstService.addCharacteristic(firstServiceChar);

                    mAdvertiserLollipop = new AdvertiserLollipop(that,that);
                    mAdvertiserLollipop.addService(firstService);
                    mAdvertiserLollipop.Start();

                    mSearchKitKat = new SearchKitKat(that,that);
                    mSearchKitKat.Start();
                }
            }
        });

        BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            debug_Text("ADAPTER","Starting to enable Bluetooth");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }
        timeHandler = new Handler();
        mStatusChecker.run();
        debug_Text("","Running we are.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeHandler.removeCallbacks(mStatusChecker);

        if(mSearchKitKat != null){
            mSearchKitKat.Stop();
            mSearchKitKat = null;
        }

        if(mAdvertiserLollipop != null){
            mAdvertiserLollipop.Stop();
            mAdvertiserLollipop = null;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            debug_Text("ADAPTER","onActivityResult REQUEST_ENABLE_BT : " + resultCode);
        }
    }//

    @Override
    public void debug(String who, String what) {
        debug_Text(who, what);
    }

    @Override
    public void Started(AdvertiseSettings settingsInEffec, String error) {
        if(error != null){
            debug_Text("Advert", "Start error " + error);
        }else{
            debug_Text("Advert", "Advertising started");
        }
    }

    @Override
    public void Stopped(String error) {
        if(error != null){
            debug_Text("Advert", "Stop error " + error);
        }else{
            debug_Text("Advert", "Advertising STOPPED");
        }
    }

    @Override
    public void onDeviceConnected(BluetoothDevice device, int status) {
        debug_Text("CONN", BLEBase.getDeviceNameOrAddress(device,this) + " Connected with status: " + BLEBase.getGATTStatus(status));
    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice device, int status) {
        debug_Text("CONN", BLEBase.getDeviceNameOrAddress(device,this) + " Disconnected with status: " + BLEBase.getGATTStatus(status));
    }

    @Override
    public void onCharacterRead(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
        debug_Text("Character", BLEBase.getDeviceNameOrAddress(device,this) + " Read-Start for " + getCharacterName(characteristic.getUuid().toString()));
    }

    @Override
    public void onDescriptorRead(BluetoothDevice device, BluetoothGattDescriptor characteristic) {
        debug_Text("Descriptor", BLEBase.getDeviceNameOrAddress(device,this) + " Read-Start for " + getDescriptorName(characteristic.getUuid().toString()));
    }


    String getCharacterName(String UUIID){

        String ret = "";

        if(UUIID.equalsIgnoreCase(CharacteristicsUID1)){
            ret = CharacteristicsNAME1;
        }else{
            ret = unknown;
        }

        return ret;
    }

    String getDescriptorName(String UUIID){

        String ret = "";

        if(UUIID.equalsIgnoreCase(DescriptorUID)){
            ret = DescriptorNAME1;
        }else{
            ret = unknown;
        }

        return ret;
    }

    public void debug_Text(String Who, String text) {

        final String WhoTmp = Who;
        final String textTmp = text;

        runOnUiThread(new Thread(new Runnable() {
            public void run() {
                timeCounter = 0;
                ((TextView) findViewById(R.id.TimeBox)).setText("T: " + timeCounter);
                Log.d(WhoTmp, textTmp);
                ((TextView) findViewById(R.id.debugdataBox)).append(WhoTmp + " : " + textTmp + "\n");
            }
        }));
    }
}
