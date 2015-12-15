package org.thaliproject.p2p.mybletest;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends ActionBarActivity{

    MainActivity that = this;

    static public String SERVICE_UUID_1 = "0105facb-00b0-1000-8000-00805f9b34fb";
    static public String CharacteristicsUID1 = "46651222-96e0-4aca-a710-8f35f7e702b9";
    static public String DescriptorUID = "f360ef7a-52c2-442a-95c5-3048b161ed1d";

    final int REQUEST_ENABLE_BT = 1;

    ListView mlistView = null;
    ArrayAdapter<String> mAdapter = null;
    ArrayList<String> listItems=new ArrayList<String>();

    MainBCReceiver mBRReceiver;
    private IntentFilter filter;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button clearButton = (Button) findViewById(R.id.button2);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((MyBLEHandlerApp)getApplicationContext()).isBLEScanActive()) {
                    debug_Text("BUT", "Stopping Scanning");
                    ((MyBLEHandlerApp)getApplicationContext()).stopBLEScan();
                    clearButton.setText("Start Scan");

                    //clear the discovered list
                    listItems.clear();
                    mAdapter.notifyDataSetChanged();
                    mlistView.invalidateViews();
                    mlistView.refreshDrawableState();

                } else {
                    debug_Text("BUT", "Starting Scanning");
                    ((MyBLEHandlerApp)getApplicationContext()).startBLEScan();
                    clearButton.setText("Stop Scan");
                }
            }
        });

        final Button showIPButton = (Button) findViewById(R.id.button3);
        showIPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((MyBLEHandlerApp)getApplicationContext()).isBLEAdvertisingActive()) {
                    debug_Text("BUT", "Stopping advertising");
                    ((MyBLEHandlerApp)getApplicationContext()).stopBLEAdvertising();
                    showIPButton.setText("Start advert");
                } else {
                    BluetoothGattService firstService = new BluetoothGattService(UUID.fromString(SERVICE_UUID_1), BluetoothGattService.SERVICE_TYPE_PRIMARY);
                    // alert level char.
                    BluetoothGattCharacteristic firstServiceChar = new BluetoothGattCharacteristic(
                            UUID.fromString(CharacteristicsUID1)
                            , BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE
                            , BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

                    // max 600 gets delivered in 22 byte chunks,
                    // if 609 is defined then 'android.os.DeadObjectException' will be throun in BluetoothGattServer::sendResponse
                    String data1 = "humppaan itsekseni";
                    firstServiceChar.setValue(data1.getBytes());

                    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString(DescriptorUID)
                            , BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);


                    // max 600 gets delivered in 22 byte chunks,
                    // if 609 is defined then 'android.os.DeadObjectException' will be throun in BluetoothGattServer::sendResponse
                    String data = "this is descriptor that for the characteristic number one";
                    descriptor.setValue(data.getBytes());
                    firstServiceChar.addDescriptor(descriptor);

                    firstService.addCharacteristic(firstServiceChar);

                    debug_Text("BUT", "Starting Advertising");
                    ((MyBLEHandlerApp)getApplicationContext()).startBLEAdvertising(firstService);
                    showIPButton.setText("Stop advert");
                }
            }
        });

        mlistView = (ListView) findViewById(R.id.devList);
        mAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, android.R.id.text1, listItems);
        // Assign adapter to ListView
        mlistView.setAdapter(mAdapter);
        mlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // ListView Clicked item index
                int itemPosition = position;
                // ListView Clicked item value
                String itemValue = (String) mlistView.getItemAtPosition(position);
                debug_Text("LISTBOX", "pos :" + itemPosition + " values: " + itemValue);

                List<BLEScannerKitKat.DeviceListItem> list = ((MyBLEHandlerApp) getApplicationContext()).getScanList();

                debug_Text("LISTBOX", "Size :" + list.size());


                if (list != null && list.size() > itemPosition) {

                    debug_Text("LISTBOX", "Size :" + list.get(itemPosition).getDevice().toString());

                    if (list.get(itemPosition).getDevice() != null){// && list.get(itemPosition).getDevice().getAddress().equalsIgnoreCase(itemValue)) {

                        ((MyBLEHandlerApp) getApplicationContext()).setSelectedItem(list.get(itemPosition));
                        Intent intent = new Intent(getBaseContext(), ConnectedActivity.class);
                        startActivity(intent);
                    }
                }
            }
        });

        mBRReceiver = new MainBCReceiver();
        filter = new IntentFilter();
        filter.addAction(MyBLEHandlerApp.DSS_BLESCAN_RESULT);
        filter.addAction(MyBLEHandlerApp.DSS_ADVERT_STATUS);

        this.registerReceiver((mBRReceiver), filter);

        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            debug_Text("ADAPTER", "Starting to enable Bluetooth");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        debug_Text("", "Running we are.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mBRReceiver);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            debug_Text("ADAPTER", "onActivityResult REQUEST_ENABLE_BT : " + resultCode);
        }
    }

    private class MainBCReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyBLEHandlerApp.DSS_BLESCAN_RESULT.equals(action)) {
               // String s = intent.getStringExtra(SearchService.DSS_WIFIDIRECT_MESSAGE);
               // ((TextView) findViewById(R.id.debugdataBox)).append(s + "\n");
                if(mAdapter != null && mlistView != null){
                    listItems.clear();
                    List<BLEScannerKitKat.DeviceListItem> list = ((MyBLEHandlerApp)getApplicationContext()).getScanList();
                    if(list != null){
                        for (BLEScannerKitKat.DeviceListItem item : list){
                            if(item != null && item.getDevice() != null){
                                listItems.add(BLEBase.getDeviceNameOrAddress(item.getDevice().getAddress(),that));
                            }
                        }
                    }

                    mAdapter.notifyDataSetChanged();
                    mlistView.invalidateViews();
                    mlistView.refreshDrawableState();
                }

            }else if (MyBLEHandlerApp.DSS_ADVERT_STATUS.equals(action)) {
                String stringStatus = intent.getStringExtra(MyBLEHandlerApp.DSS_ADVERT_STATVAL);
                ((TextView) findViewById(R.id.advertStatus)).setText(stringStatus);
            }
        }
    }

    public void debug_Text(String Who, String text) {
        Log.d(Who, text);
    }
}



