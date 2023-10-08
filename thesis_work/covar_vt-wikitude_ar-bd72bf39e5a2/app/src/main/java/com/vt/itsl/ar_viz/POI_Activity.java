package com.vt.itsl.ar_viz;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.net.wifi.WifiInfo;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.wikitude.architect.ArchitectView;
import com.wikitude.architect.StartupConfiguration;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

//import custom classes
import com.vt.itsl.ar_viz.WifiHandler;

public class POI_Activity extends Activity implements IARegion.Listener, WifiBroadcastReceiver.OnWifiChngListener {

    //grab intent from which activity was called to extract the extras included there-in
    Intent master_intent;

    //log tag to track events/api calls
    static final String EVENTLOG = "ATLAS";

    //arch view for AR
    ArchitectView architectView = null;
    ArchitectView.ArchitectUrlListener urlListener;

    protected ArchitectView.SensorAccuracyChangeListener sensorAccuracyListener;

    //indoor location parameters
    static final String FASTEST_INTERVAL = "fastestInterval";
    static final String SHORTEST_DISPLACEMENT = "shortestDisplacement";
    static final String LOGTAG = "ATLAS";
    IALocationManager locationManager;
    IALocationListener locationListener;
    IALocation master_location;
    long mFastestInterval = -1L;
    float mShortestDisplacement = -1f;
    long mRequestStartTime;
    int accuracyCheck = 0;

    //culling distance to display nearby POIs
    public static final int CULLING_DISTANCE_DEFAULT_METERS = 50 * 1000;

    //non IA listener
    LocationListener geo_locationListener;

    //wifi fields
    WifiBroadcastReceiver wifiBroadcastRecevier;
    IntentFilter filter;
    WifiManager wifiManager;
    WifiInfo wifiInfo;

    //database variables
    DataAdapter dataAdapter;
    Cursor dbData;
    Cursor regionAPs;

    ArrayList<String> apList;

    //create region dictionary to associate regions with buildings/floors etc
    HashMap<String, String> region2building;

    //for determining masterLocation for onresume
    String[] mast_loc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //wifi manager to extract user wifi connection information
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiInfo = wifiManager.getConnectionInfo();
        wifiBroadcastRecevier = new WifiBroadcastReceiver();
        filter = new IntentFilter();
        filter.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(wifiBroadcastRecevier,filter);

        //create region to building dictionary
        region2building = new HashMap<>();
        region2building.put("0724ce54-449b-4eb4-b0a8-08802020b381","ITSL-1");
        region2building.put("ccf82511-562c-490f-82aa-311d2585b405","LIB-1");
        region2building.put("31fc2b21-bd0d-49b6-bf40-93b4f5275923","LIB-2");

        //load fastest interval if saved to the bundle upon destruction
        if (savedInstanceState != null) {
            mFastestInterval = savedInstanceState.getLong(FASTEST_INTERVAL);
            mShortestDisplacement = savedInstanceState.getFloat(SHORTEST_DISPLACEMENT);
        }

        //create location manager
        locationManager = IALocationManager.create(this);

        //pull intents
        master_intent = getIntent();
        mast_loc = master_intent.getStringArrayExtra("currentLocation");
        apList = master_intent.getStringArrayListExtra("apList");

        //set archview sensor accuray listener. may want to explore calibration, may make your projections
        //more accurate.
        sensorAccuracyListener = new ArchitectView.SensorAccuracyChangeListener() {
            @Override
            public void onCompassAccuracyChanged(int i) {
            }
        };

        //sample camera layout, applicable to all AR activities
        setContentView(R.layout.sample_camera);

        //pull arch view from layout
        architectView = (ArchitectView) findViewById(R.id.architectView1);

        //wikitude key
        String key = "qUknPI0CZz2I8qNVd5jcHnjLppGWVMKMwgwv+B5qt1TKsmyCuRlm2OGCQXxsnHIfS8z2AaTMOeP+kgIqMBghOlHbHfGH366oVr//zL55wurGcvVeCxa8F5PxqCQvrWLhAJiq5vUuK+RZXGVyF7STjI4IUQlvPDpwXgtjLMmg7HVTYWx0ZWRfX2hwvU3ndQNL4ImAV+nPLhbLUqdADtfWK7p9ppZgqg9eZ9Pl7OV8wG2YQ6dX4CFOgwfPD3gxuA3C7yuFogMUnsGHyftqCoDCZmEu3FDsu8YYvYnjDroqHyQ0C8OlqcNuaePMS3k3x4elukJMksPO2wI8Pfp+lgCq9pAfSfeWIDJ74sHA8HVbZIuvtjm4s8HfIS7m3iR2o8J2fORppOIOG6NugUHzZr4ht7ixXXrdUwwW1F31J7MhmTzNUVm/92FGuzUlN3h+cllo8wU2aiOlAgxfhpTS9HbTdOQSESjD+D+9wD+/Jm++5XwBtI9GcPkKbdFfu1CdoSFWqmvBakJ8NZM0oxrXMXg7e2n5pIoY7xw73dGMT0uKRSo3wv4xYCjIU4feEcrZOpk6X53e9QKKI50+0hR2UfCCzezhja2AcPYZXs4D/2d5sba9KtPc/wjQyZRGJt4N0F5m+VNAXdrvfeUSJCg3IlQiwmbYLaUersVxddS66PGn326UM3o8SJxR0k7uIllOtusR";

        //create configuration hopefully with a camera feed and a feature seat for geo events NOT IR tracking. unsure how to combine them both.
        final StartupConfiguration config = new StartupConfiguration(key, StartupConfiguration.Features.Geo, StartupConfiguration.CameraPosition.DEFAULT);

        //attempt to create archview, if not throw exception message.
        try{
            architectView.onCreate(config);
        }
        catch(RuntimeException e)
        {
            this.architectView = null;
            Toast.makeText(getApplicationContext(), "can't create architect view", Toast.LENGTH_LONG).show();
            Log.e("architectView","error with ArchView");
        }

        //need to create a register listener with the activity so it knows to communicate
        //with the associated javascripts. need to register before content is loaded.
        //note for some reason the sample called the get url listener twice, but I believe
        //you just need to call it once then register on your new url.
        urlListener = getUrlListener();
        if (this.urlListener != null && this.architectView != null){
            this.architectView.registerUrlListener(urlListener);
        }

        //create a IndoorAtlas location listener to catch user location and relay to the ar world.
        locationListener = new IALocationListener() {
            @Override
            public void onLocationChanged(IALocation iaLocation) {

                //if location has a value,
                if (iaLocation != null){

                    //establish a master location if necessary
                    master_location = iaLocation;

                    mast_loc[0] = Double.toString(master_location.getLatitude());
                    mast_loc[1] = Double.toString(master_location.getLongitude());
                    mast_loc[2] = Double.toString(master_location.getAltitude());
                    mast_loc[3]= Float.toString(master_location.getAccuracy());

                    //pass set location to the javascript. this is imperative as it informs the AR world where you are at all times
                    architectView.setLocation(iaLocation.getLatitude(),iaLocation.getLongitude(),iaLocation.getAltitude(),
                        iaLocation.getAccuracy());

                    Log.i(LOGTAG,"setLocation");

                    //log message for debugging
                    Log.i(LOGTAG, String.format(Locale.US, "%f,%f, altitude: %f, accuracy: %.2f", iaLocation.getLatitude(),
                            iaLocation.getLongitude(), iaLocation.getAltitude(), iaLocation.getAccuracy()));


                    //pass all information via the specific java function as set functionality not working
                    String java_call = "World.locationChanged()";

                    //architectView.callJavascript(java_call);
                    Log.i(LOGTAG,"java call");
                }

                if(accuracyCheck%16==0){
                    Toast.makeText(POI_Activity.this, "accuracy: "+Float.toString(iaLocation.getAccuracy()), Toast.LENGTH_SHORT).show();
                }

                accuracyCheck++;

                //may want to include some thing about accuracy later on like filtering updates if accuracy is wack.
                //not sure what the looks like but whatever.
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }
        };

        //this is the standard location listener.
        // TODO: implement standard location listener upon exiting a specific region.

        geo_locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                /*
                will have to set location here

                if(location != null) {

                    //pass all information via the specific java function as set functionality not working
                    String java_call = "World.locationChanged()";
                    architectView.callJavascript(java_call);

                }

                    //architectView.setLocation(iaLocation.getLatitude(),iaLocation.getLongitude(),iaLocation.getAltitude(),
                //      iaLocation.getAccuracy());

                */

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        Log.i(EVENTLOG,"onCreate");
    }

    //end onCreate

    //in the sample, the architectView is surrounded by if not null blocks. for time
    //i'm not going to do that, but if there are issues in the future consider adding


//--------------------lifecycle---------------------------------------------------------------------
    //begin lifecycle events, need to tie indoorAtlas, goe_locationListener, and ArchView to
    //all lifecycle events
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        //load ArchView from HTML index which invokes javaScript
        if(architectView != null){
            architectView.onPostCreate();
            try
            {
                architectView.load("4_Point$Of$Interest_1_Poi$At$Location/index.html");

                //set culling distance which defines the visibile radius within which you are seeing POIs
                architectView.setCullingDistance((float)CULLING_DISTANCE_DEFAULT_METERS);

            }catch(IOException e){
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(getApplicationContext(),"something's wrong loading architect",Toast.LENGTH_LONG).show();
        }

        Log.i(EVENTLOG,"post Create");
    }

    @Override
    protected void onPause() {
        super.onPause();
        architectView.onPause();
        locationManager.unregisterRegionListener(this);

        // unregister accuracy listener in architectView, if set
        if ( this.sensorAccuracyListener != null ) {
            this.architectView.unregisterSensorAccuracyChangeListener( this.sensorAccuracyListener );
            Log.i(LOGTAG,"Sensor unreg");
        }
        Log.i(EVENTLOG,"on Pause");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume() {
        super.onResume();
        architectView.onResume();
        locationManager.registerRegionListener(this);




        //pass javascript master location from main activity, both as set location and as a function call
        //pass all information via the specific java function as set functionality not working

        architectView.setLocation(Double.parseDouble(mast_loc[0]),Double.parseDouble(mast_loc[1]), Double.parseDouble(mast_loc[2]),
                Float.parseFloat(mast_loc[3]));

        //need to run this method to update radar position
        String java_call = "World.locationChanged()";
        architectView.callJavascript(java_call);

        //also set location request to associate locations with this activity
        setLocationRequest();

        if (this.sensorAccuracyListener!=null) {
            this.architectView.registerSensorAccuracyChangeListener( this.sensorAccuracyListener );
            Log.i(LOGTAG, "sensor register");
        }

        Log.i(EVENTLOG,"on resume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        architectView.onDestroy();
        locationManager.destroy();
        unregisterReceiver(wifiBroadcastRecevier);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        architectView.onLowMemory();
    }

    public ArchitectView.ArchitectUrlListener getUrlListener() {
        return new ArchitectView.ArchitectUrlListener() {
            @Override
            //additional clause for interacting with options presented via url.
            public boolean urlWasInvoked(String s) {
                return false;
            }
        };
    }

    //Location specific ---------------------------------------------------------------------------

    //region event handlers with log messages for debugging
    @Override
    public void onEnterRegion(IARegion iaRegion) {

        //clear apList to recreate
        apList.clear();

        //pull query criteria, specifically looking for fold and floor
        String[] criteria = region2building.get(iaRegion.getId()).split("[-]");
        String floorHeight = Integer.toString(Integer.parseInt(criteria[1])*10 - 10);
        Toast.makeText(this,  "Entered Region: " + criteria[0]+" Floor: "+floorHeight, Toast.LENGTH_SHORT).show();

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
        Log.i(LOGTAG, "Exited Region: " + iaRegion.getType() + ", " + iaRegion.getId());
    }

    public void setLocationRequest() {

        //instantiate the request, the class we use to request location
        //not sure why we need the start time but covering my bases regardless.
        mRequestStartTime = SystemClock.elapsedRealtime();
        IALocationRequest request = IALocationRequest.create();

        //if fastest interval and shortest displacement are saved simply make the request, otherwise
        //set to 100ms and make the request. Do likewise for shortest displacement (units:meters).
        if (mFastestInterval == -1L) {
            mFastestInterval = 100;
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
        locationManager.removeLocationUpdates(locationListener);
        locationManager.requestLocationUpdates(request, locationListener);
        Log.i(LOGTAG, "requesting location updates");
        //log("requesting location updates");

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putLong(FASTEST_INTERVAL, mFastestInterval);
        outState.putFloat(SHORTEST_DISPLACEMENT, mShortestDisplacement);

        super.onSaveInstanceState(outState);

    }


    @Override
    public void onConnected() {

        String apName = null;
        String lat = null;
        String longi = null;

        wifiManager = (WifiManager) POI_Activity.this.getSystemService(Context.WIFI_SERVICE);
        wifiInfo = wifiManager.getConnectionInfo();

        String query = wifiInfo.getBSSID().trim().substring(0,wifiInfo.getBSSID().length()-1)+"%";

        //Instantiate dbVars
        dataAdapter = new DataAdapter(POI_Activity.this);
        try {
            dataAdapter.createDatabase();
        } catch (IOException e) {
            Log.i("database",e.toString());
        }
        dataAdapter.open();

        String sql = "select _id,lat,long from ap where radio_mac1 like '"+query+"' or radio_mac2 like '"+query+"';";

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

        //generate an AP
        WifiHandler wifiHandler = new WifiHandler();
        //wifiHandler.javaCall(architectView,wifiHandler.apIdentifier(apName,Double.parseDouble(lat),Double.parseDouble(longi),-32768f));
        //String java_call = "World.locationChanged()";
        //architectView.callJavascript(java_call);
        //wifiHandler.javaCall(architectView,wifiHandler.apIdentifier("backDoor", 37.22917532,-80.41911288,-32768f));

        Toast.makeText(this, "connected to: "+apName, Toast.LENGTH_SHORT).show();
        Log.i("ap","ap lat: "+lat+" ap long: "+longi);

        dataAdapter.close();


    }

    @Override
    public void onDisconnected() {



    }
}
