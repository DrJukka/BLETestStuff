package com.drjukka.heartbeatsimulator;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements HeartBeatAdvertiser.HeartBeatAdvertiserCallback {

    private final String TAG = "HEARTBEAT_SIM";
    private final MainActivity self = this;

    private HeartBeatEngine mHeartBeatModel = null;

    private IntentFilter mfilter;
    private PowerConnectionReceiver mReceiver;

    private int mInterval = 1000; // 1 second by default, can be changed later
    private Handler timeHandler;
    private int valueHB = 50;
    private boolean directionUp = true;
    private boolean timerEnabled = false;
    private int minTimerValue = 50;
    private int maxTimerValue = 150;
    private boolean timerRunning = false;

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {

            if(timerEnabled) {
                if (directionUp) {
                    valueHB++;
                } else {
                    valueHB--;
                }

                if (valueHB < minTimerValue) {
                    directionUp = true;
                    valueHB = minTimerValue;
                }

                if (valueHB > maxTimerValue) {
                    directionUp = false;
                    valueHB = maxTimerValue;
                }

                if (self.mHeartBeatModel != null) {
                    self.mHeartBeatModel.setHeartbeatValue(valueHB);
                    Log.i(TAG, "Set value : " + valueHB);
                }

                ((SeekBar) findViewById(R.id.sliderControl)).setProgress(valueHB);
            }

            if(timerRunning) {
                timeHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button buttonStart = (Button) findViewById(R.id.StartSim);
        final Button buttonStop = (Button) findViewById(R.id.StopSim);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Start();
                timerRunning = true;
                mStatusChecker.run();
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);
            }
        });


        buttonStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Stop();
                timerRunning = false;
                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);
            }
        });

        final SeekBar sliderVal = (SeekBar) findViewById(R.id.sliderControl);
        sliderVal.setMax(200);
        sliderVal.setProgress(100);

        final TextView seekBarValue = (TextView)findViewById(R.id.sliderValue);
        seekBarValue.setGravity(Gravity.CENTER);
        seekBarValue.setText("100");

        sliderVal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarValue.setText(String.valueOf(progress));
                if(mHeartBeatModel != null) {
                    mHeartBeatModel.setHeartbeatValue(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        final SeekBar sliderMax = (SeekBar) findViewById(R.id.maxValueSlider);
        sliderMax.setMax(200);
        sliderMax.setProgress(maxTimerValue);

        final TextView maxValue = (TextView)findViewById(R.id.maxValueText);
        maxValue.setGravity(Gravity.CENTER);
        maxValue.setText("" + maxTimerValue);

        sliderMax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxTimerValue = progress;
                maxValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        final SeekBar sliderMin = (SeekBar) findViewById(R.id.minValueSlider);
        sliderMin.setMax(200);
        sliderMin.setProgress(minTimerValue);

        final TextView minValue = (TextView)findViewById(R.id.minValueText);
        minValue.setGravity(Gravity.CENTER);
        minValue.setText("" + minTimerValue);

        sliderMin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minTimerValue = progress;
                minValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        final CheckBox checkTimer = (CheckBox)findViewById(R.id.timerCheckBox);
        checkTimer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                self.timerEnabled = ((CheckBox) v).isChecked();
            }
        });


        final Spinner spinner1 = (Spinner) findViewById(R.id.spinner1);
        List<String> list = new ArrayList<String>();
        list.add("Other");
        list.add("Chest");
        list.add("Wrist");
        list.add("Finger");
        list.add("Hand");
        list.add("Ear Lobe");
        list.add("Foot");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(dataAdapter);
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(mHeartBeatModel != null) {
                    mHeartBeatModel.setbodySensorLocation(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        timeHandler = new Handler();
        mfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    }

    public void Start() {
        Log.i(TAG, "Start-");

        String manData = "052B006B";
        if(mHeartBeatModel == null) {
            mHeartBeatModel = new HeartBeatEngine(this,this);
            mHeartBeatModel.setbodySensorLocation(0);
            mHeartBeatModel.setBatteryLevel(50);
        }
        mHeartBeatModel.start();

        mReceiver = new PowerConnectionReceiver();
        registerReceiver(mReceiver, mfilter);
    }

    public void Stop() {
        Log.i(TAG, "Stop-");

        HeartBeatEngine tmpModel = mHeartBeatModel;
        mHeartBeatModel = null;
        if(tmpModel != null)
        {
            tmpModel.stop();
        }

        if(mReceiver != null){
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
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

    @Override
    public void onAdvertisingStarted(String error) {
        Log.i(TAG, "onAdvertisingStarted : " + error);
    }

    @Override
    public void onAdvertisingStopped(String error) {
        Log.i(TAG, "onAdvertisingStarted : " + error);
    }

    public class PowerConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            mHeartBeatModel.setBatteryLevel(((level*100)/scale));
        }
    }
}
