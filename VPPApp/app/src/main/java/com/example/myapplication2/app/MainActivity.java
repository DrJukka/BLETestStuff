package com.example.myapplication2.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class MainActivity extends Activity implements DiscoveryCallback {

    protected PowerManager.WakeLock mWakeLock;


    private final MainActivity that = this;
    private String mInstanceString = "";
    static final String JSON_ID_PEERID   = "pi";
    static final String JSON_ID_PEERNAME = "pn";
    static final String JSON_ID_BTADRRES = "ra";

    private boolean isStarted = false;
    private BTConnector_Discovery mBTConnector_Discovery = null;
    private final BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();


    private int mInterval = 1000; // 1 second by default, can be changed later
    private Handler timeHandler;
    private int timeCounter1 = 0;
    private int timeCounter2 = 0;
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            // call function to update timer
            timeCounter1++;
            timeCounter2++;

            ((TextView) findViewById(R.id.TimeBox1)).setText("L: " + timeCounter1);
            ((TextView) findViewById(R.id.TimeBox2)).setText(", T: " + timeCounter2);
            timeHandler.postDelayed(mStatusChecker, mInterval);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        JSONObject jsonobj = new JSONObject();
        try {
            jsonobj.put(JSON_ID_PEERID, bluetooth.getAddress());
            jsonobj.put(JSON_ID_PEERNAME, bluetooth.getName());
            jsonobj.put(JSON_ID_BTADRRES, bluetooth.getAddress());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mInstanceString = jsonobj.toString();

        final Button button = (Button) findViewById(R.id.StartNode);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (that.mBTConnector_Discovery == null) {
                    logScreen("Starting");
                    that.mBTConnector_Discovery = new BTConnector_Discovery(that, that, "not used with BLE", that.mInstanceString);
                    that.mBTConnector_Discovery.Start();
                    isStarted = true;
                } else {
                    logScreen("Stopping");
                    that.mBTConnector_Discovery.Stop();
                    that.mBTConnector_Discovery = null;
                    isStarted = false;
                    logListScreen("");
                }
            }
        });
        /* This code together with the one in onDestroy()
        * will make the screen be always on until this Activity gets destroyed. */
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();

        timeHandler = new Handler();
        mStatusChecker.run();
    }

    private void logScreen(final String line) {
        timeCounter2 = 0;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditText contentData = ((EditText) findViewById(R.id.serviceReplyBox));
                contentData.setText(line + "\n" + contentData.getText());
            }
        });
    }

    private void logListScreen(final String line) {
        timeCounter1 = 0;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditText contentData = ((EditText) findViewById(R.id.ServiceListBox));
                contentData.setText(line);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private final CopyOnWriteArrayList<ServiceItem> mLastSeenList = new CopyOnWriteArrayList<ServiceItem>();
    @Override
    public void gotServicesList(List<ServiceItem> list) {

        String startText = "New list : " + list.size();
        mLastSeenList.clear();

        for(ServiceItem item : list){
            mLastSeenList.add(item);
        }

        printLastSeendList(startText);
    }

    private void printLastSeendList(String startText){
        String textToSet = startText;
        for(ServiceItem item : mLastSeenList){
            textToSet =  textToSet + "\n" + item.peerName + ", " + item.peerAddress + ", " + item.deviceAddress;
        }
        logListScreen(textToSet);
    }

    @Override
    public void foundService(ServiceItem item) {
        logScreen("foundService " + item.peerName);

        boolean alreadyInList = false;
        for(ServiceItem listItem : mLastSeenList){
            if(item.peerAddress.equalsIgnoreCase(listItem.peerAddress)){
                alreadyInList = true;
            }
        }

        if(!alreadyInList) {
            mLastSeenList.add(item);
            printLastSeendList("Available:");
        }
    }

    @Override
    public void StateChanged(State newState) {

    }

    @Override
    public void debugData(String data) {
        logScreen(data);
    }
}
