package org.thaliproject.p2p.mybletest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by juksilve on 9.7.2015.
 */
public class ConnectedActivity extends Activity implements BLEConnector.BLEConnectCallback{

    ConnectedActivity that = this;
    BLEConnector mBLEConnector = null;
    List<BluetoothGattService> mServices = null;

    ListView mlistView = null;
    ArrayAdapter<String> mAdapter = null;
    ArrayList<String> listItems=new ArrayList<String>();

    enum myState {
        ShowingServices,
        SowingCharacters,
        ShowingDescriptions
    }
    private myState curreState  = myState.ShowingServices;
    String mLastServiceSelected = "";
    String mLastCharSelected = "";
    boolean mConnected = false;

    ProgressDialog progress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);

        final Button clearButton = (Button) findViewById(R.id.ConnectBut);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mConnected) {
                    debug_Text("BUT", "Connecting");
                    doconnectNow();
                } else {

                    debug_Text("BUT", "Disconnecting");
                    if(mBLEConnector != null){
                        mBLEConnector.disConnect();
                        mBLEConnector = null;
                    }

                    if(mServices != null){
                        mServices.clear();
                        mServices = null;
                    }

                    setConnectButtonText();
                    mConnected = false;
                }
            }
        });

        final Button mtuButton = (Button) findViewById(R.id.mtuBut);
        mtuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnected) {
                    changeMtuValue();
                }
            }
        });

        mlistView = (ListView) findViewById(R.id.servList);
        mAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, android.R.id.text1, listItems);
        // Assign adapter to ListView
        mlistView.setAdapter(mAdapter);
        mlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // ListView Clicked item index
                int itemPosition = position;

                if(itemPosition >= 0 && itemPosition < mAdapter.getCount()) {
                    String itemValue = (String) mlistView.getItemAtPosition(position);
                    debug_Text("LISTBOX", "pos :" + itemPosition + " values: " + itemValue);

                    if (curreState == myState.ShowingServices) {
                        gotoState(myState.SowingCharacters, itemValue);
                    } else if (curreState == myState.SowingCharacters) {
                        gotoState(myState.ShowingDescriptions, itemValue);
                    } else {
                        // its descriptor view
                        readItemValue(false, itemValue);
                    }
                }
        }});

        mlistView.setOnItemLongClickListener (new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView parent, View view, int position, long id) {
                int itemPosition = position;

                if (itemPosition >= 0 && itemPosition < mAdapter.getCount()) {
                    String itemValue = (String) mlistView.getItemAtPosition(position);
                    debug_Text("LISTBOX", "pos :" + itemPosition + " values: " + itemValue);

                    //if (curreState == myState.ShowingServices)
                    if (curreState == myState.SowingCharacters) {
                        readItemValue(true, itemValue);
                        return true;
                    } else if (curreState == myState.ShowingDescriptions) {
                        readItemValue(false, itemValue);
                        return true;
                    }
                }
                return false;
            }
        });

        doconnectNow();
    }

    private void setStateBox(String text){
        ((TextView) findViewById(R.id.curState)).setText(text);
    }

    private void setConnectButtonText() {
        final Button clearButton = (Button) findViewById(R.id.ConnectBut);
        if (mConnected) {
            clearButton.setText("Disconnect");
        } else {
            clearButton.setText("Connect");
        }
    }

    private void readItemValue(boolean isCharacter, String uuid){

        if(progress != null){
            progress.dismiss();
            progress = null;
        }

        if(mServices != null && mBLEConnector != null ){
            for (BluetoothGattService item : mServices) {
                outerloop:
                if (item != null && item.getUuid().toString().equalsIgnoreCase(mLastServiceSelected)) {
                    List<BluetoothGattCharacteristic> charList = item.getCharacteristics();
                    if (charList != null) {
                        for (BluetoothGattCharacteristic charItem : charList) {
                            if (charItem != null){
                                if(isCharacter){
                                    if(charItem.getUuid().toString().equalsIgnoreCase(uuid)){
                                        progress = ProgressDialog.show(this, "Reading Character", uuid, true);
                                        mBLEConnector.readCharacter(charItem);
                                        break outerloop;
                                    }
                                }else {

                                    List<BluetoothGattDescriptor> descr = charItem.getDescriptors();
                                    if (descr != null) {
                                        for (BluetoothGattDescriptor descriptor : descr) {
                                            if (descriptor != null && descriptor.getUuid().toString().equalsIgnoreCase(uuid)) {
                                                progress = ProgressDialog.show(this, "Reading Descriptor", uuid, true);
                                                mBLEConnector.readDescriptor(descriptor);
                                                break outerloop;
                                            }
                                        }
                                     }
                                 }
                             }
                         }
                    }
                }
            }
        }
    }

    private void changeMtuValue() {
        if(mBLEConnector != null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Request new MTU size");

            // Set an EditText view to get user input
            final EditText input = new EditText(this);
            input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(3),DigitsKeyListener.getInstance(), });
            input.setKeyListener(DigitsKeyListener.getInstance());
            builder.setView(input);
            input.setText("20");


            builder.setPositiveButton("Change",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String tmppp = input.getText().toString();
                            Log.i("Dialog","new value: "  + tmppp);

                            mBLEConnector.requestMtu(Integer.parseInt(tmppp));
                        }
                    });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });
            builder.show();
        }
    }

    private void showItemValue(String title, String value,BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor){

        //just to be certain its not up there.
        if(progress != null){
            progress.dismiss();
            progress = null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(value);

        final BluetoothGattCharacteristic characteristicTmp = characteristic;
        final BluetoothGattDescriptor descriptorTmp = descriptor;

        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String changedText = input.getText().toString();
                Log.i("Dialog", "Text : " + changedText);
                if(mBLEConnector != null) {
                    if (characteristicTmp != null) {

                        progress = ProgressDialog.show(that, "Writing Character", changedText, true);
                        characteristicTmp.setValue(changedText.getBytes());
                        if(mBLEConnector.writeCharacteristic(characteristicTmp)){
                            Log.i("Dialog", "writeCharacteristic");
                        }else{
                            Log.i("Dialog", "NOT-writeCharacteristic");
                        }
                    } else if (descriptorTmp != null) {
                        progress = ProgressDialog.show(that, "Writing Descriptor", changedText, true);
                        descriptorTmp.setValue(changedText.getBytes());
                        mBLEConnector.writeDescriptor(descriptorTmp);
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void doconnectNow(){

        if(mBLEConnector == null) {
            mBLEConnector = new BLEConnector(this, this);
        }

        BLEScannerKitKat.DeviceListItem item = ((MyBLEHandlerApp)getApplicationContext()).getSelectedItem();
        if(item != null && item.getDevice() != null){
            setStateBox("Device: " + item.getDevice().getAddress());
            mBLEConnector.connect(item.getDevice());
        }else{
            ((TextView) findViewById(R.id.connStatus)).setText("Device is NULL: disconnected");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(progress != null){
            progress.dismiss();
            progress = null;
        }

        if(mBLEConnector != null){
            mBLEConnector.disConnect();
            mBLEConnector = null;
        }
    }

    @Override
    public void onBackPressed() {
        Log.i("DEBUG", "back pressed");

        if(progress != null){
            progress.dismiss();
            progress = null;
        }

        if (curreState == myState.ShowingDescriptions) {
            gotoState(myState.SowingCharacters,mLastServiceSelected);
        } else if (curreState == myState.SowingCharacters) {
            gotoState(myState.ShowingServices,"Connected");
        } else {
            super.onBackPressed();
        }
    }

    private void gotoState(myState nextState, String selItem){
        curreState  = nextState;

        if(mAdapter != null && mlistView != null){
            listItems.clear();
            if(mServices != null){
                if(curreState == myState.ShowingServices) {
                   setStateBox("Services");
                   for (BluetoothGattService item : mServices) {
                       if (item != null) {
                           listItems.add(item.getUuid().toString());
                       }
                   }
                }else if(curreState == myState.SowingCharacters) {
                    mLastServiceSelected  = selItem;
                    setStateBox("Characters for " + mLastServiceSelected);
                    for (BluetoothGattService item : mServices) {
                        if (item != null && item.getUuid().toString().equalsIgnoreCase(selItem)) {
                            List<BluetoothGattCharacteristic> charList = item.getCharacteristics();
                            if (charList != null) {
                                for (BluetoothGattCharacteristic charItem : charList) {
                                    if (charItem != null) {
                                        listItems.add(charItem.getUuid().toString());
                                    }
                                }
                            }

                            break;
                        }
                    }
                }else if(curreState == myState.ShowingDescriptions) {
                    mLastCharSelected = selItem;
                    setStateBox("Descriptions for " + mLastCharSelected);
                    for (BluetoothGattService item : mServices) {
                        if (item != null && item.getUuid().toString().equalsIgnoreCase(mLastServiceSelected)) {
                            List<BluetoothGattCharacteristic> charList = item.getCharacteristics();
                            if (charList != null) {
                                for (BluetoothGattCharacteristic charItem : charList) {
                                    if (charItem != null && charItem.getUuid().toString().equalsIgnoreCase(selItem)) {
                                        List<BluetoothGattDescriptor> descr = charItem.getDescriptors();
                                        if(descr != null){
                                            for(BluetoothGattDescriptor descriptor :descr){
                                                if(descriptor != null){
                                                    listItems.add(descriptor.getUuid().toString());
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            break;
                        }
                    }
                }
            }

            mAdapter.notifyDataSetChanged();
            mlistView.invalidateViews();
            mlistView.refreshDrawableState();
        }
    }

    @Override
    public void onDeviceConnected(String deviceAddress, int status) {
        debug_Text("Conn", "onDeviceConnected: " + deviceAddress + ", status: " + BLEBase.getGATTStatus(status));
        ((TextView) findViewById(R.id.connStatus)).setText("Connected : " + BLEBase.getGATTStatus(status));

        mConnected = true;
        setConnectButtonText();
        if(mBLEConnector != null) {
            mBLEConnector.discoverServices();
        }
    }

    @Override
    public void onDeviceDisconnected(String deviceAddress, int status) {
        debug_Text("Conn", "onDeviceDisconnected: " + deviceAddress + ", status: " + BLEBase.getGATTStatus(status));
        mConnected = false;
        setConnectButtonText();
        if(mServices != null){
            mServices.clear();
            mServices = null;
            gotoState(myState.ShowingServices,"Disconnected : " + BLEBase.getGATTStatus(status));
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {

        if (characteristic != null) {
            debug_Text("Conn", "onCharacteristicChanged: " + characteristic.getUuid());
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {

                String tempString = new String(data);
                debug_Text("Conn", "Characteristic Value(" + data.length + "): " + tempString);
            } else {
                if (data == null) {
                    debug_Text("Conn", "Characteristic Value : Is NULL");
                } else if (data.length <= 0) {
                    debug_Text("Conn", "Characteristic Value : Lenght is ZERO");
                }
            }
        }
    }

    @Override
    public void onServicesDiscovered(List<BluetoothGattService> services, int status) {
        mServices = services;
        gotoState(myState.ShowingServices,"Connected");
    }

    @Override
    public void onCharacterRead(BluetoothGattCharacteristic characteristic, int status) {

        if(progress != null){
            progress.dismiss();
            progress = null;
        }

        if (characteristic != null) {
            debug_Text("Conn", "onCharacterRead: " + characteristic.getUuid());
            byte[] data = characteristic.getValue();
            if (data != null) {
                String tempString = new String(data);
                debug_Text("Conn", "Characteristic Value(" + data.length + "): " + tempString);
                showItemValue("Character: " + characteristic.getUuid(),tempString,characteristic,null);
            } else {
                if (data == null) {
                    debug_Text("Conn", "Characteristic Value : Is NULL");
                } else if (data.length <= 0) {
                    debug_Text("Conn", "Characteristic Value : Lenght is ZERO");
                }
            }
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGattDescriptor descriptor, int status) {

        if(progress != null){
            progress.dismiss();
            progress = null;
        }

        if (descriptor != null) {
            debug_Text("Conn", "onDescriptorRead: " + descriptor.getUuid());
            byte[] data = descriptor.getValue();
            if (data != null && data.length > 0) {
                String tempString = new String(data);
                debug_Text("Conn", "Descriptor Value(" + data.length + "): " + tempString);
                showItemValue("Descriptor: " + descriptor.getUuid(),tempString,null,descriptor);
            } else {
                if (data == null) {
                    debug_Text("Conn", "Descriptor Value : Is NULL");
                } else if (data.length <= 0) {
                    debug_Text("Conn", "Descriptor Value : Lenght is ZERO");
                }
            }
        }
    }

    @Override
    public void onCharacterWrite(BluetoothGattCharacteristic characteristic, int status) {

        if(progress != null){
            progress.dismiss();
            progress = null;
        }
        if (characteristic != null) {
            debug_Text("Conn", "onCharacterWrite: " + characteristic.getUuid());
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {

                String tempString = new String(data);
                debug_Text("Conn", "Characteristic Value(" + data.length + "): " + tempString);
            } else {
                if (data == null) {
                    debug_Text("Conn", "Characteristic Value : Is NULL");
                } else if (data.length <= 0) {
                    debug_Text("Conn", "Characteristic Value : Lenght is ZERO");
                }

            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGattDescriptor descriptor, int status) {
        if(progress != null){
            progress.dismiss();
            progress = null;
        }
        if (descriptor != null) {
            debug_Text("Conn", "onDescriptorWrite: " + descriptor.getUuid());
            byte[] data = descriptor.getValue();
            if (data != null && data.length > 0) {

                String tempString = new String(data);
                debug_Text("Conn", "Descriptor Value(" + data.length + "): " + tempString);
            } else {
                if (data == null) {
                    debug_Text("Conn", "Descriptor Value : Is NULL");
                } else if (data.length <= 0) {
                    debug_Text("Conn", "Descriptor Value : Lenght is ZERO");
                }
            }
        }
    }

    @Override
    public void onReliableWriteCompleted(String deviceAddress, int status) {
        debug_Text("Conn", "onReliableWriteCompleted: " + deviceAddress + ", status: " + BLEBase.getGATTStatus(status));
    }

    @Override
    public void onReadRemoteRssi(String deviceAddress, int rssi, int status) {
        debug_Text("Conn", "onReadRemoteRssi: " + deviceAddress + ", rssi : " + rssi + ", status: " + BLEBase.getGATTStatus(status));
    }

    @Override
    public void onMtuChanged(String deviceAddress, int mtu, int status) {
        debug_Text("Conn", "onReadRemoteRssi: " + deviceAddress + ", mtu : " + mtu + ", status: " + BLEBase.getGATTStatus(status));
        new AlertDialog.Builder(this)
                .setTitle("onMtuChanged")
                .setMessage("Mtu Changed to value : " + mtu + ", status : " + BLEBase.getGATTStatus(status))
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void debug_Text(String Who, String text) {
        Log.d(Who, text);

/*        final String WhoTmp = Who;
        final String textTmp = text;

        runOnUiThread(new Thread(new Runnable() {
            public void run() {
                     ((TextView) findViewById(R.id.debugdataBox)).append(WhoTmp + " : " + textTmp + "\n");
            }
        }));*/
    }
}
