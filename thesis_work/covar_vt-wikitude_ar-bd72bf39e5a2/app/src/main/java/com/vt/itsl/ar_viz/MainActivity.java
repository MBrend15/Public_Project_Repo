package com.vt.itsl.ar_viz;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.wikitude.architect.ArchitectView;
import com.wikitude.architect.StartupConfiguration;
import android.opengl.GLES20;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.wikitude.architect.ArchitectView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements IALocationListener, IARegion.Listener, WifiBroadcastReceiver.OnWifiChngListener {

    //permission check constants
    public static final String CAM_PERM = Manifest.permission.CAMERA;
    public static final String GPS_PERM = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String[] INDOOR_LOCA_PERM = {Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION};
    public static final int CAM_CHECK = 1;
    public static final int GPS_CHECK = 2;
    public static final int INDOOR_LOCA_CHK = 3;

    //variables to support indoor ATLAS location, per the example
    static final String FASTEST_INTERVAL = "fastestInterval";
    static final String SHORTEST_DISPLACEMENT = "shortestDisplacement";
    static final String LOGTAG = "ATLAS";
    TextView log_out;
    ScrollView scrollView2;
    IALocationManager locationManager;
    long mFastestInterval = -1L;
    float mShortestDisplacement = -1f;
    IALocation master_location;
    long mRequestStartTime;

    WifiBroadcastReceiver wifiBroadcastReceiver;
    IntentFilter filter;

    //databse variables
    DataAdapter dataAdapter;
    Cursor dbData;
    Cursor regionAPs;

    WifiManager wifiManager;
    WifiInfo networkInfo;

    int accuracy_check = 1;

    Button but_poi_act;

    HashMap<String,String> region2building;

    ArrayList<String> apList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //believe these next lines retrieve saved parameters upon resume? or if the state is saved for some reason
        //yeah so if there is data in the savedInstanceState, then restore interval and displacement variables.
        if (savedInstanceState != null) {
            mFastestInterval = savedInstanceState.getLong(FASTEST_INTERVAL);
            mShortestDisplacement = savedInstanceState.getFloat(SHORTEST_DISPLACEMENT);
        }

        setContentView(R.layout.activity_main);

        //runa permissions chekc for elements necessary to run architect view
        permission_check(CAM_PERM, CAM_CHECK);

        //check permissions for indoor location
        ActivityCompat.requestPermissions(this, INDOOR_LOCA_PERM, INDOOR_LOCA_CHK);

        //create IA location manager and associate with current context
        locationManager = IALocationManager.create(this);


        wifiBroadcastReceiver = new WifiBroadcastReceiver();
        filter = new IntentFilter();
        filter.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(wifiBroadcastReceiver,filter);

        but_poi_act = (Button) findViewById(R.id.but_POI);
        but_poi_act.setEnabled(false);

        //create region to building dictionary
        region2building = new HashMap<>();
        region2building.put("0724ce54-449b-4eb4-b0a8-08802020b381","ITSL-1");
        region2building.put("ccf82511-562c-490f-82aa-311d2585b405","LIB-1");
        region2building.put("31fc2b21-bd0d-49b6-bf40-93b4f5275923","LIB-2");


    }

    //call methods associated with the activity to establish lifecycle of indoor atlas\
    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.destroy();
        unregisterReceiver(wifiBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //register the region listener to calculate location within the region
        locationManager.registerRegionListener(this);
        setLocationRequest();
        //but_poi_act.setEnabled(false);

    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.unregisterRegionListener(this);


    }

    //intent launchers------------------------------------------------------------------------------

    public void AR_Click(View view) {

        Intent intent = new Intent(this, SampCam.class);
        startActivity(intent);

    }

    public void POI_Click(View view) {

        Intent intent = new Intent(this, POI_Activity.class);
        String[] curr_loc = {Double.toString(master_location.getLatitude()),Double.toString(master_location.getLongitude()),
        Double.toString(master_location.getAltitude()),Float.toString(master_location.getAccuracy())};
        intent.putExtra("currentLocation",curr_loc);
        intent.putStringArrayListExtra("apList",apList);
        startActivity(intent);

    }


    //permission checks for the entire application-------------------------------------------------

    public void permission_check(String permissions, int perm_code) {

        if (ContextCompat.checkSelfPermission(this, permissions) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permissions}, perm_code);
        }
    }

    //request base permissions for wikitude SDK
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case CAM_CHECK: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permission_check(GPS_PERM, GPS_CHECK);
                } else {
                    Toast.makeText(MainActivity.this, "permission must be granted to use this app", Toast.LENGTH_SHORT).show();
                }
            }
            case GPS_CHECK: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(MainActivity.this, "permission must be granted to use this app", Toast.LENGTH_SHORT).show();
                }
            }
            default: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(MainActivity.this, "nice try, I need ALL the permissions", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //methods to support indoor locaiton----------------------------------------------------------
    public void setLocationRequest() {

        //instantiate the request, the class we use to request location
        //not sure why we need the start time but covering my bases regardless.
        mRequestStartTime = SystemClock.elapsedRealtime();
        IALocationRequest request = IALocationRequest.create();

        //if fastest interval and shortest displacement are saved simply make the request, otherwise
        //set to 1000ms and make the request. Do likewise for shortest displacement (units:meters).
        if (mFastestInterval == -1L) {
            mFastestInterval = 250;
            request.setFastestInterval(mFastestInterval);
        } else {
            request.setFastestInterval(mFastestInterval);
        }

        if (mShortestDisplacement == -1f) {
            mShortestDisplacement = (float) 1.0;
            request.setSmallestDisplacement(mShortestDisplacement);
        } else {
            request.setSmallestDisplacement(mShortestDisplacement);
        }

        //remove any existing updates so we can start our own. log requesting update
        locationManager.removeLocationUpdates(MainActivity.this);
        locationManager.requestLocationUpdates(request, MainActivity.this);
        Log.i(LOGTAG, "requesting location updates");
        //log("requesting location updates");

    }

    public void askLocation() {

        try {
            //set region via floor plan id
            IARegion region = IARegion.floorPlan("b58154f7-4a7a-422d-92ea-9d0f0e52cd40");
            locationManager.setLocation(IALocation.from(region));
            Log.i(LOGTAG, "set location: " + region.toString());
            //log("set location: "+ region.getId());
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "could not set location with given floor plan id!", Toast.LENGTH_LONG).show();
        }

    }

    //--------------------------------------------------------------------------------------------

    //function to request location updates
    public void requesetUpdates(View view) {

        setLocationRequest();

    }

    public void setLocation(View view) {
        
        askLocation();


    }

    //function to remove updates
    public void removeUpdates(View view) {
        Log.i(LOGTAG, "removing location updates");
        //log("removing location updates");
        locationManager.removeLocationUpdates(MainActivity.this);
    }


    //listener events
    @Override
    public void onLocationChanged(IALocation iaLocation) {

        Log.i(LOGTAG, String.format(Locale.US, "%f,%f, accuracy: %.2f", iaLocation.getLatitude(),
                iaLocation.getLongitude(), iaLocation.getAccuracy()));
        
        master_location = iaLocation;
        if (accuracy_check%12==0){
            Toast.makeText(this, "accuracy: "+Float.toString(iaLocation.getAccuracy()), Toast.LENGTH_SHORT).show();
        }

        accuracy_check++;
        //Toast.makeText(MainActivity.this, Float.toString(master_location.getAccuracy()), Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

        //got a couple switch statements going on here, the nested switch statement reviews the
        //quality of the calibration of the signal, the actual switch statement just reviews the
        //different situations in which a status is changed.
        switch (status) {

            case IALocationManager.STATUS_CALIBRATION_CHANGED:
                String quality = "unk";
                switch (extras.getInt("quality")) {

                    case IALocationManager.CALIBRATION_POOR:
                        quality = "Poor";
                        break;
                    case IALocationManager.CALIBRATION_GOOD:
                        quality = "Good";
                        break;
                    case IALocationManager.CALIBRATION_EXCELLENT:
                        quality = "Excellent";
                        break;
                }
                Log.i(LOGTAG, "Calib changed. quality: " + quality);
                //log("Calib changed. quality: "+quality);
                break;
            case IALocationManager.STATUS_AVAILABLE:
                Log.i(LOGTAG, "status changed: Available");
                //log("status changed: Available");
                break;
            case IALocationManager.STATUS_LIMITED:
                Log.i(LOGTAG, "status changed: Limited");
                //log("status changed: Limited");
                break;
            case IALocationManager.STATUS_OUT_OF_SERVICE:
                Log.i(LOGTAG, "status changed: Out of service");
                //log("status changed: Out of service");
                break;
            case IALocationManager.STATUS_TEMPORARILY_UNAVAILABLE:
                Log.i(LOGTAG, "status changed: temporarily unavailable");
                //log("status changed: temporarily unavailable");
                break;
        }
    }

    @Override
    public void onEnterRegion(IARegion iaRegion) {

        apList = new ArrayList<>();

        //pull query criteria, specifically looking for fold and floor
        String[] criteria = region2building.get(iaRegion.getId()).split("[-]");
        String floorHeight = Integer.toString(Integer.parseInt(criteria[1])*10 - 10);
        Toast.makeText(this,  "Entered Region: " + criteria[0]+" Floor: "+floorHeight, Toast.LENGTH_SHORT).show();
        but_poi_act.setEnabled(true);

        DataAdapter da2 = new DataAdapter(this);

        try {
            da2.createDatabase();
        } catch (IOException e) {
            Log.i("database",e.toString());
        }
        da2.open();
        if(criteria[0].equals("ITSL")){

            apList.add("TOR-A25AP03B,37.22926291,-80.41919840,0");
            apList.add("sample,37.229.16366,-80.41913083,0");

        }else {

            String sql = "select _id,lat,long,floor from ap where fold like '" + criteria[0].trim() + "%' and floor ='" + floorHeight.trim() + "';";
            regionAPs = da2.getData(sql);

            while (regionAPs.moveToNext()){

                apList.add(regionAPs.getString(0).trim()+","+regionAPs.getString(1).trim()+","+regionAPs.getString(2).trim()+","+regionAPs.getString(3).trim());

            }

        }
    }

    @Override
    public void onExitRegion(IARegion iaRegion) {

        //enter a region and pass this value to the logs
        Log.i(LOGTAG, "Exited Region: " + iaRegion.getType() + ", " + iaRegion.getId());
        but_poi_act.setEnabled(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putLong(FASTEST_INTERVAL, mFastestInterval);
        outState.putFloat(SHORTEST_DISPLACEMENT, mShortestDisplacement);
        super.onSaveInstanceState(outState);

    }

    //-----methods to support the broadcast receiver--------------------------------------

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Toast.makeText(MainActivity.this, "broadcast received", Toast.LENGTH_SHORT).show();

            ConnectivityManager connectivityManager = (ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                Log.i(LOGTAG, "connected");
            }
            else{

                Log.i(LOGTAG,"not connected");

            }

        }
    };

    //method from the listener interface we created fires everytime we achieve a new association
    @Override
    public void onConnected() {

        String apName = null;
        String lat = null;
        String longi = null;

        wifiManager = (WifiManager) MainActivity.this.getSystemService(Context.WIFI_SERVICE);
        networkInfo = wifiManager.getConnectionInfo();

        String query = networkInfo.getBSSID().trim().substring(0,networkInfo.getBSSID().length()-1);

        //Instantiate dbVars
        dataAdapter = new DataAdapter(MainActivity.this);
        try {
            dataAdapter.createDatabase();
        } catch (IOException e) {
            Log.i("database",e.toString());
        }
        dataAdapter.open();

        String sql = "select _id,lat,long from ap where radio_mac1 like '"+query+"%' or radio_mac2 like '"+query+"%';";

        try {
            dbData = dataAdapter.getData(sql);

            for (int i = 0; i < dbData.getColumnCount(); i++) {
                switch (i){

                    case 0: apName = dbData.getString(0);
                        break;
                    case 1: lat = dbData.getString(1);
                        break;
                    case 2: longi = dbData.getString(2);

                    default: apName = apName;

                }

            }

        }
        catch (RuntimeException e)
        {
            apName = null;
            Log.i("database","no ap name returned");
        }

        Toast.makeText(this, "connected to: "+apName, Toast.LENGTH_SHORT).show();
        Log.i("ap","ap lat: "+lat+" ap long: "+longi);

        dataAdapter.close();

    }

    @Override
    public void onDisconnected() {

    }
}
