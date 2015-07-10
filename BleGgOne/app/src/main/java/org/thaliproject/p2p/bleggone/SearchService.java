package org.thaliproject.p2p.bleggone;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by juksilve on 8.7.2015.
 */
public class SearchService extends Service implements BLEAdvertiserLollipop.BLEAdvertiserCallback, BLEScannerKitKat.BLEScannerCallback, BLEConnector.BLEConnectCallback {

    SearchService that = this;

    static final public String DSS_WIFIDIRECT_VALUES = "test.microsoft.com.wifidirecttest.DSS_WIFIDIRECT_VALUES";
    static final public String DSS_WIFIDIRECT_MESSAGE = "test.microsoft.com.wifidirecttest.DSS_WIFIDIRECT_MESSAGE";

    static public String SERVICE_UUID_1      = "010500a1-00b0-1000-8000-00805f9b34fb";
    static public String CharacteristicsUID1  = "46651222-96e0-4aca-a710-8f35f7e702b9";
    static public String DescriptorUID        = "f360ef7a-52c2-442a-95c5-3048b161ed1d";

    BLEConnector mBLEConnector = null;
    BLEScannerKitKat mSearchKitKat = null;

    BLEAdvertiserLollipop mBLEAdvertiserLollipop = null;
    BluetoothGattService mFirstService = null;
    BluetoothGattCharacteristic mSelectedCharacter = null;

    long lastSaveTimeStamp= 0;
    // 20 minute timer
    CountDownTimer SaveDataTimeOutTimer = new CountDownTimer(60000, 1000) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {

            if (mTestDataFile != null) {
                long nowtimme = System.currentTimeMillis();
                if ((nowtimme - lastSaveTimeStamp) > 1200000) {
                    lastSaveTimeStamp = System.currentTimeMillis();
                    //long Started , long got , long Noservices ,long Peererr ,long ServiceErr , long AddreqErr ,long  resetcounter) {
                    mTestDataFile.WriteDebugline(lastChargePercent, fullRoundCount,deviceConnectedCounter,deviceDisConnectedCounter,characterReadCounter,characterWriteCounter,descriptorReadCounter,descriptorWriteCounter,scanRoundCount,startConnectCount, ConnectedCount, DisConnectedCount,servicesDiscoveredCount, charactersReadCounter, charactersWriteCounter);
                }
                SaveDataTimeOutTimer.start();
            }
        }
    };

    CountDownTimer readWriteOutTimer = new CountDownTimer(60000, 1000) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            if(mBLEConnector != null && mSelectedCharacter != null) {
                print_line("SCAN", "readinf char");
                mBLEConnector.readCharacter(mSelectedCharacter);
            }
            readWriteOutTimer.start();
        }
    };


    long deviceConnectedCounter = 0;
    long deviceDisConnectedCounter = 0;
    long characterReadCounter = 0;
    long characterWriteCounter = 0;
    long descriptorReadCounter = 0;
    long descriptorWriteCounter = 0;
    long scanRoundCount = 0;
    long startConnectCount = 0;
    long ConnectedCount = 0;
    long DisConnectedCount = 0;
    long servicesDiscoveredCount = 0;
    long charactersReadCounter = 0;
    long charactersWriteCounter = 0;

    long fullRoundCount = 0;
    String latsDbgString = "";

    IntentFilter mfilter = null;
    BroadcastReceiver mReceiver = null;
    TestDataFile mTestDataFile = null;
    int lastChargePercent = -1;

    private final IBinder mBinder = new MyLocalBinder();


    public class MyLocalBinder extends Binder {
        SearchService getService() {
            return SearchService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        print_line("SearchService","onStartCommand rounds so far :" + fullRoundCount);
        Start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        print_line("SearchService","onDestroy");
        super.onDestroy();
        SaveDataTimeOutTimer.cancel();
        Stop();
    }

    public void Start() {
        Stop();
        mTestDataFile = new TestDataFile(this);
        SaveDataTimeOutTimer.start();

        mfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        mReceiver = new PowerConnectionReceiver();
        registerReceiver(mReceiver, mfilter);

        StartScanner();
        StartAdvertiser();
    }

    public void Stop() {

        if(mSearchKitKat != null){
            mSearchKitKat.Stop();
            mSearchKitKat = null;
        }

        readWriteOutTimer.cancel();
        if(mBLEConnector != null){
            mBLEConnector.disConnect();
            mBLEConnector = null;
        }

        if(mBLEAdvertiserLollipop != null){
            mBLEAdvertiserLollipop.Stop();
            mBLEAdvertiserLollipop = null;
        }

        if(mReceiver != null){
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        if(mTestDataFile != null) {
            print_line("SearchService","Closing File");
            mTestDataFile.CloseFile();
            mTestDataFile = null;
        }

        SaveDataTimeOutTimer.cancel();
    }

    private void StartAdvertiser(){
        fullRoundCount = fullRoundCount + 1;
        mFirstService = new BluetoothGattService(UUID.fromString(SERVICE_UUID_1),BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic firstServiceChar = new BluetoothGattCharacteristic(UUID.fromString(CharacteristicsUID1),BluetoothGattCharacteristic.PROPERTY_READ |BluetoothGattCharacteristic.PROPERTY_WRITE,BluetoothGattCharacteristic.PERMISSION_READ |BluetoothGattCharacteristic.PERMISSION_WRITE);

        String data1 = "first advert char";
        firstServiceChar.setValue(data1.getBytes());

        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString(DescriptorUID), BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);

        String data = "First advert desc";
        descriptor.setValue(data.getBytes());
        firstServiceChar.addDescriptor(descriptor);

        mFirstService.addCharacteristic(firstServiceChar);

        mBLEAdvertiserLollipop = new BLEAdvertiserLollipop(that,that);
        mBLEAdvertiserLollipop.addService(mFirstService);
        mBLEAdvertiserLollipop.Start();
    }
    /*
   SCANNER related coding start *****************************************************************
   */

    private void StartScanner() {
        print_line("SCAN", "starting");
        mSearchKitKat = new BLEScannerKitKat(this, this);
        mSearchKitKat.Start();
    }

    @Override
    public void onScanListChanged(List<BLEScannerKitKat.DeviceListItem> scanList) {
        scanRoundCount = scanRoundCount + 1;
        if(scanList != null && mBLEConnector == null){// if mBLEConnector is null we dont have connection, so we can get one
            for(BLEScannerKitKat.DeviceListItem item :scanList){
                if(item != null && item.getDevice() != null ) {
                    if(item.getUUID().equalsIgnoreCase(SERVICE_UUID_1)){
                        startConnectCount = startConnectCount + 1;
                        print_line("SCAN", "Connecting");
                        mBLEConnector = new BLEConnector(that,that);
                        mBLEConnector.connect(item.getDevice());
                        break;
                    }
                }
            }
        }
    }
    /*
      SCANNER related coding end *****************************************************************
    */

    long roundsCount(){
        return fullRoundCount;
    }

    String getLastDbgString() {

        String ret = "Battery : " + lastChargePercent + ", fullrounds: " + fullRoundCount + "\n";
        ret =  ret + "last: " + latsDbgString + "\n";

        ret =  ret + "Acon: " + deviceConnectedCounter + ", Adis : " + deviceDisConnectedCounter + "\n";
        ret =  ret + "aRead: " + characterReadCounter + ", aWriet : " + characterWriteCounter + "\n";
        ret =  ret + "scan: " + scanRoundCount + ", try con : " + startConnectCount + "\n";
        ret =  ret + "con: " + ConnectedCount + ", dis : " + DisConnectedCount + ", ser : " + servicesDiscoveredCount +  "\n";
        ret =  ret + "read: " + charactersReadCounter + ", write : " + charactersWriteCounter + "\n";

        return ret;
    }

    boolean isRunnuing(){
        boolean ret = false;
        if(mBLEAdvertiserLollipop != null){
            ret = true;
        }
        return ret;
    }
     /*
    START of advertiser *****************************************************************
     */

    @Override
    public void onAdvertisingStarted(AdvertiseSettings settingsInEffec, String error) {
        print_line("advertising","Started : " + error);
    }

    @Override
    public void onAdvertisingStopped(String error) {
        print_line("advertising","Stopped : " + error);
    }

    @Override
    public void onRemoteDeviceConnected(String deviceAddress, int status) {
        deviceConnectedCounter = deviceConnectedCounter + 1;
        print_line("SCAN", "onRemoteDeviceConnected.");
    }

    @Override
    public void onRemoteDeviceDisconnected(String deviceAddress, int status) {
        deviceDisConnectedCounter = deviceDisConnectedCounter + 1;
        print_line("SCAN", "onRemoteDeviceDisconnected.");
    }

    @Override
    public void onRemoteCharacterRead(String deviceAddress, String uuid) {
        characterReadCounter = characterReadCounter + 1;
        print_line("SCAN", "onRemoteCharacterRead.");
    }

    @Override
    public void onRemoteDescriptorRead(String deviceAddress, String uuid) {
        descriptorReadCounter = descriptorReadCounter + 1;
    }

    @Override
    public void onRemoteCharacterWrite(String deviceAddress, String uuid, byte[] value) {
        characterWriteCounter = characterWriteCounter + 1;
        print_line("SCAN", "onRemoteCharacterWrite.");

        if (mBLEAdvertiserLollipop != null && value != null && value.length > 0) {
            mBLEAdvertiserLollipop.setCharacterValue(UUID.fromString(uuid),value);
        }
    }

    @Override
    public void onRemoteDescriptorWrite(String deviceAddress, String uuid, byte[] value) {
        descriptorWriteCounter = descriptorWriteCounter + 1;

        if (mBLEAdvertiserLollipop != null && value != null && value.length > 0) {
            mBLEAdvertiserLollipop.setDescriptorValue(UUID.fromString(uuid), value);
        }
    }

    /*
    START of connector *****************************************************************
     */

    @Override
    public void onDeviceConnected(String deviceAddress, int status) {
        ConnectedCount = ConnectedCount + 1;
        print_line("SCAN", "Connected");
        if(mBLEConnector != null){
            mBLEConnector.discoverServices();
        }
    }

    @Override
    public void onDeviceDisconnected(String deviceAddress, int status) {
        DisConnectedCount = DisConnectedCount + 1;
        print_line("SCAN", "DISCONNECTED");

        readWriteOutTimer.cancel();
        //lets clear this out, next scan will reconnect again
        if(mBLEConnector != null){
            mBLEConnector.disConnect();
            mBLEConnector = null;
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void onServicesDiscovered(List<BluetoothGattService> services, int status) {
        servicesDiscoveredCount = servicesDiscoveredCount + 1;
        if (services != null && mBLEConnector != null) {
            for (BluetoothGattService item : services) {
                outterloop:
                if (item != null && item.getUuid().toString().equalsIgnoreCase(SERVICE_UUID_1)) {
                    List<BluetoothGattCharacteristic> charList = item.getCharacteristics();
                    if (charList != null) {
                        for (BluetoothGattCharacteristic charItem : charList) {
                            if (charItem != null && charItem.getUuid().toString().equalsIgnoreCase(CharacteristicsUID1)) {
                                mSelectedCharacter =charItem;
                                print_line("SCAN", "readinf char");
                                mBLEConnector.readCharacter(mSelectedCharacter);
                                break outterloop;
                            }
                        }
                    }
                }
            }
        }
        readWriteOutTimer.start();
    }
    @Override
    public void onCharacterRead(BluetoothGattCharacteristic characteristic, int status) {
        charactersReadCounter = charactersReadCounter + 1;
        if (mBLEConnector != null && characteristic != null) {

            String stuff = "12345678901234567890123456789012345678901234567890123456789012345678901234567890aaaa" + UUID.randomUUID().toString();

            characteristic.setValue(stuff.getBytes());
            print_line("SCAN", "writing char");
            mBLEConnector.writeCharacteristic(characteristic);
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGattDescriptor descriptor, int status) {

    }

    @Override
    public void onCharacterWrite(BluetoothGattCharacteristic characteristic, int status) {
        charactersWriteCounter = charactersWriteCounter + 1;
        print_line("SCAN", "character wrote.");
    }

    @Override
    public void onDescriptorWrite(BluetoothGattDescriptor descriptor, int status) {

    }

    @Override
    public void onReliableWriteCompleted(String deviceAddress, int status) {

    }

    @Override
    public void onReadRemoteRssi(String deviceAddress, int rssi, int status) {

    }

    @Override
    public void onMtuChanged(String deviceAddress, int mtu, int status) {

    }

    public void print_line(String who, String line) {
        latsDbgString = who + " : " + line;
        Log.i("BleBgOne" + who, line);
    }

    public class PowerConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);

            lastChargePercent = (level*100)/scale;
            //String message = "Battery charge: " + lastChargePercent + " %";
        }
    }
}



