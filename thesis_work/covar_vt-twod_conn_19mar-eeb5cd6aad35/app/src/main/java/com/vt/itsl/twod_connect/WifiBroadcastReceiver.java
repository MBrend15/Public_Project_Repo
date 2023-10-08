package com.vt.itsl.twod_connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Created by bcmattina on 17-Nov-16.
 */

public class WifiBroadcastReceiver extends BroadcastReceiver {

    int state_mem;

    public interface OnWifiChngListener{

        public void onConnected();
        public void onDisconnected();

    }

    private OnWifiChngListener onWifiChngListener;

    public WifiBroadcastReceiver(){

    }

    @Override
    public void onReceive(Context context, Intent intent) {

        this.onWifiChngListener = (OnWifiChngListener) context;


        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();


        int int_state = wifiManager.getWifiState();

        if (int_state == WifiManager.WIFI_STATE_ENABLED){
            Log.i("WIFI", "enabled!");

            if(wifiInfo.getBSSID()!=null) {
                onWifiChngListener.onConnected();
                Log.i("WIFI",wifiInfo.getBSSID());
            }
        }
        else{

            Log.i("WIFI","not enabled");
            onWifiChngListener.onDisconnected();

        }





    }
}
