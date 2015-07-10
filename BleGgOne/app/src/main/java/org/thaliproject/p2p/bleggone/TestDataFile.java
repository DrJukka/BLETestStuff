// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.bleggone;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by juksilve on 6.3.2015.
 */
public class TestDataFile {

    private final String fileNameStart = "BleAdvert1";
    private final String firstLine= "Os ,time ,battery ,Started ,Connected ,DisConnected ,characterRead ,characterWrite ,descriptorRead ,descriptorWrite ,scanrounds ,startConnectCount ,ConnectedCount ,DisConnectedCount ,servicesDiscoveredCount ,charactersReadCounter ,charactersWriteCounter\n";

    private File dbgFile;
    private OutputStream dbgFileOs;
    private Context context;

    public TestDataFile(Context Context){
        this.context = Context;
        Time t= new Time();
        t.setToNow();

        File path = this.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        String sFileName =  "/" + fileNameStart + t.yearDay + t.hour+ t.minute + t.second + ".txt";

        try {
            dbgFile = new File(path, sFileName);
            dbgFileOs = new FileOutputStream(dbgFile);
            dbgFileOs.write(firstLine.getBytes());
            dbgFileOs.flush();

            Log.d(fileNameStart, "File created:" + path + " ,filename : " + sFileName);
        }catch(Exception e){
            Log.d("FILE", "FileWriter, create file error, :" + e.toString());
        }
    }

    public void CloseFile(){
        try {
            if (dbgFile != null) {
                dbgFileOs.close();
                dbgFile.delete();
            }
        }catch (Exception e){
            Log.d(fileNameStart, "dbgFile close error :" + e.toString());
        }
    }

    public void WriteDebugline(int battery, long Started,long deviceConnectedCounter,long deviceDisConnectedCounter,long characterReadCounter,long characterWriteCounter,long descriptorReadCounter,long descriptorWriteCounter,long scanRoundCount,long startConnectCount, long ConnectedCount, long DisConnectedCount,long servicesDiscoveredCount, long charactersReadCounter, long charactersWriteCounter) {

        //"Os ,time ,battery ,Started ,got ,No services ,Peer err ,Service Err ,Add req Err ,reset counter \n";

        try {
            String dbgData = Build.VERSION.SDK_INT + " ," ;
            dbgData = dbgData  + System.currentTimeMillis() + " ,";
            dbgData = dbgData + battery + " ,";
            dbgData = dbgData + Started +  " ,";
            dbgData = dbgData + deviceConnectedCounter +  " ,";
            dbgData = dbgData + deviceDisConnectedCounter +  " ,";
            dbgData = dbgData + characterReadCounter +  " ,";
            dbgData = dbgData + characterWriteCounter +  " ,";
            dbgData = dbgData + descriptorReadCounter +  " ,";
            dbgData = dbgData + descriptorWriteCounter +  " ,";
            dbgData = dbgData + scanRoundCount +  " ,";
            dbgData = dbgData + startConnectCount +  " ,";
            dbgData = dbgData + ConnectedCount +  " ,";
            dbgData = dbgData + DisConnectedCount +  " ,";
            dbgData = dbgData + servicesDiscoveredCount +  " ,";
            dbgData = dbgData + charactersReadCounter +  " ,";
            dbgData = dbgData + charactersWriteCounter +  " \n";

            Log.d("FILE", "write: " + dbgData);
            dbgFileOs.write(dbgData.getBytes());
            dbgFileOs.flush();

        }catch(Exception e){
            Log.d("FILE", "dbgFile write error :" + e.toString());
        }
    }
}
