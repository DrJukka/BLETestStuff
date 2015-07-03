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


public class MainActivity extends ActionBarActivity implements BLEBase.CallBack {

    static public String SERVICE_UUID_1      = "0105facb-00b0-1000-8000-00805f9b34fb";
    static public String CharacteristicsUID1 = "46651222-96e0-4aca-a710-8f35f7e702b9";
    static public String DescriptorUID       = "f360ef7a-52c2-442a-95c5-3048b161ed1d";

    MainActivity that = this;

    final int REQUEST_ENABLE_BT = 1;

    SearchKitKat mSearchKitKat = null;
    SearchLollipop mSearchLollipop = null;
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
                    debug_Text("BUT","Stopping LE scan");
                    if(mSearchKitKat != null){
                        mSearchKitKat.Stop();
                        mSearchKitKat = null;
                    }

                    if(mSearchLollipop != null){
                        mSearchLollipop.Stop();
                        mSearchLollipop = null;
                    }
                    if(mAdvertiserLollipop != null){
                        mAdvertiserLollipop.Stop();
                        mAdvertiserLollipop = null;
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

                    String data1 = "Hi";
                    firstServiceChar.setValue(data1.getBytes());

                    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString(DescriptorUID)
                             , BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
                    String data = "Hello there !";
                    descriptor.setValue(data.getBytes());
                    firstServiceChar.addDescriptor(descriptor);

                    firstService.addCharacteristic(firstServiceChar);

                    mAdvertiserLollipop = new AdvertiserLollipop(that,that);
                    mAdvertiserLollipop.addService(firstService);
                    mAdvertiserLollipop.Start();

                    mSearchKitKat = new SearchKitKat(that,that);
                    mSearchKitKat.Start();

                //    mSearchLollipop = new SearchLollipop(that,that);
                //    mSearchLollipop.Start();
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

        if(requestCode == REQUEST_ENABLE_BT)
        {
            debug_Text("ADAPTER","onActivityResult REQUEST_ENABLE_BT : " + resultCode);
        }
    }//


    @Override
    public void Debug(String who, String text) {
        final String WhoTmp = who;
        final String textTmp = text;

        runOnUiThread(new Thread(new Runnable() {
            public void run() {
                debug_Text(WhoTmp, textTmp);
            }

        }));
    }

    public void debug_Text(String Who, String text) {
        timeCounter = 0;
        ((TextView) findViewById(R.id.TimeBox)).setText("T: " + timeCounter);
        Log.d(Who, text);
        ((TextView) findViewById(R.id.debugdataBox)).append(Who + " : " + text + "\n");
    }
}
