package org.thaliproject.p2p.bleggone;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

    MainActivity that = this;
    MainBCReceiver mBRReceiver;
    private IntentFilter filter;
    public static boolean isService = false;

    SearchService myService;
    boolean isBound = false;
    private ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            ((TextView)findViewById(R.id.debugdataBox)).append("onServiceConnected.\n");
            SearchService.MyLocalBinder binder = (SearchService.MyLocalBinder) service;
            myService = binder.getService();
            isBound = true;
            isService = myService.isRunnuing();
            ((TextView)findViewById(R.id.debugdataBox)).append("Service running " + isService + ".\n");
        }

        public void onServiceDisconnected(ComponentName arg0) {
            ((TextView)findViewById(R.id.debugdataBox)).append("onServiceDisconnected.\n");
            isBound = false;
            myService = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBRReceiver = new MainBCReceiver();
        filter = new IntentFilter();
        filter.addAction(SearchService.DSS_WIFIDIRECT_VALUES);

        this.registerReceiver((mBRReceiver), filter);

        Button clButton = (Button) findViewById(R.id.button2);
        clButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((TextView)findViewById(R.id.debugdataBox)).setText("cleared.\n");
            }
        });

        Button cuntButton = (Button) findViewById(R.id.cuntToggle);
        cuntButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myService == null) {
                    ((TextView) findViewById(R.id.debugdataBox)).append("myService is null.\n");
                }else{
                    ((TextView) findViewById(R.id.debugdataBox)).append("roundsCount : " + myService.roundsCount() + "\n");
                    ((TextView) findViewById(R.id.debugdataBox)).append("Last msg : " + myService.getLastDbgString() + "\n");
                }
            }
        });

        Button toggleButton = (Button) findViewById(R.id.buttonToggle);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               if(myService != null){
                    if(isService){
                        myService.Stop();
                        isService = false;
                        stopService(new Intent(that, SearchService.class));
                        ((TextView)findViewById(R.id.debugdataBox)).append("Service stopped\n");
                    }else {
                         myService.Start();
                        isService = true;
                        Intent i = new Intent(that, SearchService.class);
                        startService(i);
                        ((TextView) findViewById(R.id.debugdataBox)).append("Service running\n");
                    }
                }else {
                    ((TextView) findViewById(R.id.debugdataBox)).append("myService is null\n");
                }
            }
        });
    }

    @Override
    protected void onResume() {
        Log.i("MyMainActivity", "onResume -- bindService");
        super.onResume();
        Intent i = new Intent(that, SearchService.class);
        bindService(i, myConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {

        super.onPause();
        if (isBound) {
            Log.i("MyMainActivity", "onPause -- unbindService");
            unbindService(myConnection);
            isBound = false;
        }
    }

    @Override
    public void onDestroy() {
        Log.i("MyMainActivity", "onDestroy");
        super.onDestroy();
        this.unregisterReceiver(mBRReceiver);

        if (isBound) {
            Log.i("MyMainActivity", "onDestroy -- unbindService");
            unbindService(myConnection);
            isBound = false;
        }
    }

    private class MainBCReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SearchService.DSS_WIFIDIRECT_VALUES.equals(action)) {
                String s = intent.getStringExtra(SearchService.DSS_WIFIDIRECT_MESSAGE);
                ((TextView) findViewById(R.id.debugdataBox)).append(s + "\n");
            }
        }
    }
}
