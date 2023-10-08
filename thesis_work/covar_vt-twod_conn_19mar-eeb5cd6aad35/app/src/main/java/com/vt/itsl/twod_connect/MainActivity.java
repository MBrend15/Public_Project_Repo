package com.vt.itsl.twod_connect;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

//map imports
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.R.id.input;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, IALocationListener, IARegion.Listener, WifiBroadcastReceiver.OnWifiChngListener {


    //define the following classes so they can be used throughout the activity to manipulate location.
    //ultimately if location is transferred to and from teh activity consider using constants etc.
    Location location;
    LocationListener ll;
    LocationManager lm;
    Criteria locationCriteria;
    String provider;
    GoogleMap mMap;
    Location apLocation;
    Location curr_loc;

    //establish lat and longs to be used throughout the the main activity
    LatLng bburg = new LatLng(37.2239, -80.4073);

    //use this constant to request certain dialogue box form google play service library.
    private static final int ERROR_DIALOGUE_REQ = 1001;

    //instantiate permission constants to eventually determine if the app has permissions to use location etc
    private static final String[] INIT_PERMS = {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.INTERNET};
    //have to establish a int request number, potentially to determine if permissions requested before etc
    private static final int INIT_REQ = 0;

    //indoorAtlas
    //static values to request and accept permissions for indoor location engines
    public static final String[] INDOOR_LOCA_PERM = {Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static final int INDOOR_LOCA_CHK = 1;
    static final String LOGTAG = "ATLAS";
    IALocationManager locationManager;
    IALocation master_location;
    IARegion currentRegion;
    long mFastestInterval = -1L;
    float mShortestDisplacement = -1f;
    int accuracy_ctr = 0;
    long mRequestStartTime;
    Marker mMarker;
    Polyline polyline;
    String currentBuilding;

    //create a dictionary of Markers to remove the markers based on floor info
    HashMap<String, Marker> markerList;

    HashMap<String, String> id2build;

    //Database and connection variables
    //wifi fields
    WifiBroadcastReceiver wifiBroadcastRecevier;
    IntentFilter filter;
    WifiManager wifiManager;
    WifiInfo wifiInfo;

    //database variables
    DataAdapter dataAdapter;
    Cursor dbData;
    Cursor regionAPs;

    //create region dictionary to associate regions with buildings/floors etc
    HashMap<String, String> region2building;

    ArrayList<String> apList;

    //for determining masterLocation for onresume
    String[] mast_loc;

    //string to maintian master connection
    String apName = null;
    Boolean isConnected = false;
    Boolean differentFloor  = false;
    Boolean testing = false;

    //metrics variables
    long startTime;
    long totalTime;
    Calendar now = null;
    public static RequestQueue mRequestQueue;
    public static JSONArray array = null;
    JsonObjectRequest jsonObjectRequest;
    JsonObjectRequest jsonObjectRequest2;
    JsonObjectRequest jsonObjectRequest3;
    JsonObjectRequest jsonObjectRequest4;
    public static DateFormat df;
    int totalHitsDeauth = 0;
    int totalHitsSuccess = 0;
    int hourDeauths = 0;
    int hourSuccesses = 0;
    RetryPolicy policy = null;
    Float markerColor = null;

    //query trackers
    Boolean query1 = false;
    Boolean query2 = false;
    Boolean query3 = false;
    Boolean query4 = false;
    int totalQueries = 0;
    double totalTraffic = 0;
    double trafficMean = 0;
    double trafficSD = 0;
    double traffic3Q = 0;
    String trafficLevel = null;
    float dist2AP;
    float dist2APwrt;
    String file = null;
    ArrayList<String> queriedResults;
    ArrayList<String> failedQueries;
    HashMap<String,String> trafficRecord;

    //variables to manipulate views
    //Button but_findAP;
    //EditText partID;

    //variables for modifications
    //String
    String apTrk = null;
    static final String SCEN2TESTING = "scen2";
    private static final String SAVETAG = "fileSave";

    //int
    int connectCtr = 0;
    int taskCtr = 0;
    int testNum = 0;
    int testConnectCtr = 0;

    //Longs
    Long recallStart;
    Long recallTotal;
    Long findStart;
    Long findTotal;

    //Float
    //Default marker hue
    float defColor;

    //Marker
    Marker rogueMarker = null;
    Marker polyLineMark = null;

    //views
    //EditText
    EditText txt_scne2PID = null;
    //button
    Button but_scen2Start = null;
    Button but_safe = null;
    Button but_rogue = null;
    Button but_finish = null;

    //Boolean
    Boolean study2 = true;
    Boolean started = false;

    //HashMaps
    HashMap<String, Location> test2Locations = null;
    HashMap<String, Location> test1Locations = null;

    //ArrayList
    ArrayList<String> testSafeAPs = null;


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scen2_layout);

        //instantiate marker list
        markerList = new HashMap<>();
        apList = new ArrayList<>();
        queriedResults = new ArrayList<>();
        trafficRecord = new HashMap<>();

        //to register the broadcast receiver
        wifiBroadcastRecevier = new WifiBroadcastReceiver();
        filter = new IntentFilter();
        filter.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(wifiBroadcastRecevier,filter);

        //adjust the date time format for queries
        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024);
        Network network = new BasicNetwork(new HurlStack());

        mRequestQueue = new RequestQueue(cache, network);
        mRequestQueue.start();

        //create this variable up here because it's weird down there
        policy = new DefaultRetryPolicy(50000,DefaultRetryPolicy.DEFAULT_MAX_RETRIES,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        //instantiate views for the entire activity
       //but_findAP = (Button) findViewById(R.id.but_FindAP);
       // partID = (EditText) findViewById(R.id.part_ID);

        //EditText
        txt_scne2PID = (EditText) findViewById(R.id.txt_scen2PID);

        //getIntent and load into txtView for partID
        Intent intent = getIntent();
        txt_scne2PID.setText(intent.getStringExtra("partID"));

        //button
        but_scen2Start = (Button) findViewById(R.id.but_scen2Start);
        but_safe = (Button) findViewById(R.id.but_safe);
        but_rogue = (Button) findViewById(R.id.but_rogue);
        but_finish = (Button) findViewById(R.id.but_scen2End);

        but_rogue.setEnabled(false);
        but_safe.setEnabled(false);
        but_scen2Start.setEnabled(true);
        but_finish.setEnabled(false);

        //wifiManger
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);


        //before you generate any mapping code, run servicesOK method to determine if user device is compatible
        //with google services.
        if (servicesOk()) {
            Toast.makeText(this, "Compatible!", Toast.LENGTH_LONG).show();
            //Changed to a mapFragment object as I was having issues with the supporMapFragment class.
            //should continue to work with all as this example is ripped off the android website.
            //so you associate the mapfragment with the fragment id, then you get the map and associate
            //it with a certain activity. the activity has to implement the OnReadyMapCallback interface.
            MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map2);
            mapFragment.getMapAsync(MainActivity.this);
        } else {
            Toast.makeText(this, "Incompatible with Google Play services", Toast.LENGTH_LONG).show();
        }

        //on create determine if this app has given the app the permissions necessary to utilize the location and google services api
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
            requestPermissions(INIT_PERMS, INIT_REQ);
        }
        //instantiate the location manager class within the location service service context
        //believe this starts pull information from built in location features.
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        //allow the user to enable location settings if they are off.
        if (isLocationEnabled() == false) {
            showAlert();
        }

        //instantiate criteria to develop faithful positioning
        locationCriteria = new Criteria();

        //use criteria to set the provider to provide you with the necessary accuracy.
        final String provider = lm.getBestProvider(locationCriteria, true);
        apLocation = new Location("markerList");

        //pass location update to the last known location. last known location is known to return null
        //if another location hasn't used gps recently
        location = lm.getLastKnownLocation(provider);


        //instantiate location listener. create responses to the various methods of changing location
        ll = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if (location == null) {
            //request an instant update on location information if location item is null. won't get data without this
            lm.requestLocationUpdates(provider, 1, 0, ll);
            location = lm.getLastKnownLocation(provider);
        }

        //request location updates every 5 seconds when the app is open
        lm.requestLocationUpdates(provider, 5000, 0, ll);

        //build region hashMap
        id2build = new HashMap<String, String>();
        id2build.put("b58154f7-4a7a-422d-92ea-9d0f0e52cd40","ITSL1");
        id2build.put("0724ce54-449b-4eb4-b0a8-08802020b381","ITSL1");
        id2build.put("31fc2b21-bd0d-49b6-bf40-93b4f5275923","Library2");
        id2build.put("ccf82511-562c-490f-82aa-311d2585b405","Library1");

        //create region to building dictionary
        region2building = new HashMap<>();
        region2building.put("0724ce54-449b-4eb4-b0a8-08802020b381","ITSL-1");
        region2building.put("ccf82511-562c-490f-82aa-311d2585b405","LIB-1");
        region2building.put("31fc2b21-bd0d-49b6-bf40-93b4f5275923","LIB-2");

        //IA fields etc
        //check permissions for indoor location
        ActivityCompat.requestPermissions(this, INDOOR_LOCA_PERM, INDOOR_LOCA_CHK);
        locationManager = IALocationManager.create(this);

        //study 2 mods-------------------------------------------------------------

        //initialize variables
        test1Locations = new HashMap<>();
        test2Locations = new HashMap<>();
        testSafeAPs = new ArrayList<>();

        study2 = true;

        //compile locations for test 1 and test 2
        //create test locations for the various scenarios
        Location test1 = new Location("tests");
        test1.setLatitude(37.228943);
        test1.setLongitude(-80.419158);
        test1Locations.put("t1_1",test1);
        Location test2 = new Location("tests");
        test2.setLatitude(37.228738);
        test2.setLongitude(-80.419340);
        test1Locations.put("t1_3",test2);
        Location test3 = new Location("tests");
        test3.setLatitude(37.228615);
        test3.setLongitude(-80.419187);
        test2Locations.put("t2_2",test3);
        Location test = new Location("tests");
        test.setLatitude(37.228715);
        test.setLongitude(-80.419413);
        test2Locations.put("t2_3",test);

        testSafeAPs.add("LIB-234CA1052B");
        testSafeAPs.add("LIB-234CA1124Q");

    }

    //------lifecycle methods----------------------------------------

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.destroy();
        unregisterReceiver(wifiBroadcastRecevier);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //register the region listener to calculate location within the region
        locationManager.registerRegionListener(this);
        setLocationRequest();




    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.unregisterRegionListener(this);
    }

    //---------------------------------------------------------------

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }



        //googleMap.setMyLocationEnabled(true);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.2290196,-80.4219621),15));

        mMap = googleMap;

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                Toast.makeText(MainActivity.this, marker.getTitle(), Toast.LENGTH_SHORT).show();

                return true;
            }
        });

        //opend Dialog box to explain what the user will be doing in Scenario 2
        final android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(MainActivity.this);
        dialog.setIcon(android.R.drawable.alert_light_frame);
        dialog.setTitle("Scenario 2");
        dialog.setMessage("Please hand your phone to your administrator so they may calibrate for Scenario 2.");
        dialog.setPositiveButton("Calibrating...", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                but_scen2Start.setEnabled(true);

            }
        });

        dialog.show();

    }



    //-----------------------------IA Methods------------------------------------------------------

    @Override
    public void onLocationChanged(IALocation iaLocation) {

        /*

        PolylineOptions polylineOptions = new PolylineOptions()
                .add(new LatLng(iaLocation.getLatitude(),iaLocation.getLongitude()))
                .add(new LatLng(37.22926291,-80.41919840));

        Polyline polyline = mMap.addPolyline(polylineOptions);
        */
        curr_loc = new Location("IA");
        curr_loc.setLatitude(iaLocation.getLatitude());
        curr_loc.setLongitude(iaLocation.getLongitude());

        LatLng latLng = new LatLng(iaLocation.getLatitude(),iaLocation.getLongitude());

        Drawable mapIcon = getResources().getDrawable(R.drawable.user_loc);
        Bitmap mapMap = ((BitmapDrawable) mapIcon).getBitmap();
        mapMap = Bitmap.createScaledBitmap(mapMap,100,100,true);
        //Drawable mapDraw = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(mapMap,1,1,true));

        MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(curr_loc.getLatitude(),curr_loc.getLongitude())).icon(BitmapDescriptorFactory.fromBitmap(mapMap));// .fromResource()) .icon(mapDraw); // .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));


        if(mMarker != null){
            mMarker.remove();
        }

        if(polyline!=null && testing){

            polyline.remove();

            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(new LatLng(curr_loc.getLatitude(), curr_loc.getLongitude()))
                    .add(new LatLng(polyLineMark.getPosition().latitude, polyLineMark.getPosition().longitude))
                    .width(20);


            polyline = mMap.addPolyline(polylineOptions);

        }

        mMarker = mMap.addMarker(markerOptions);

        Log.i(LOGTAG, String.format(Locale.US, "%f,%f, accuracy: %.2f", iaLocation.getLatitude(),
                iaLocation.getLongitude(), iaLocation.getAccuracy()));


        if(isConnected!=null && isConnected == true){

            connectViz();

        }

        if(accuracy_ctr%32 == 0){

            Toast.makeText(this, "accuracy: "+Float.toString(curr_loc.getAccuracy()), Toast.LENGTH_SHORT).show();

        }

        accuracy_ctr++;



        if(apName!=null && !markerList.isEmpty()){

            //could also use isConnected boolean

            //apLocation.setLatitude(markerList.get(apName).getPosition().latitude);
            //apLocation.setLongitude(markerList.get(apName).getPosition().longitude);

            dist2AP = curr_loc.distanceTo(apLocation);

            if (accuracy_ctr%16 == 0){
                if(testing){
                    Toast.makeText(MainActivity.this, "dist to ap: "+dist2AP, Toast.LENGTH_SHORT).show();
                }
                Log.i("coords","user: lat-"+curr_loc.getLatitude()+"longi-"+curr_loc.getLongitude());
                Log.i("coords","ap: lat-"+apLocation.getLatitude()+"longi-"+apLocation.getLongitude());

                Log.i("dataDump pre-dump","dist2AP: "+dist2APwrt);
                Log.i("dataDump pre-dump","user: lat-"+curr_loc.getLatitude()+"longi-"+curr_loc.getLongitude());
                Log.i("dataDump pre-dump","ap: lat-"+apLocation.getLatitude()+"longi-"+apLocation.getLongitude());

            }



            if (dist2AP<8.0  && testing){

                //but_findAP.setEnabled(true);
               // but_findAP.setBackgroundColor(0xFFADFF2F);

                testing = false;

                findTotal = System.currentTimeMillis()-findStart;

                if(testConnectCtr ==3) {

                    //TODO: Stop timer here.

                    //opend Dialog box to explain what the user will be doing in Scenario 2
                    final android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(MainActivity.this);
                    dialog.setIcon(android.R.drawable.btn_star);
                    dialog.setTitle("Congratulations!");
                    dialog.setMessage("You found the rogue AP! Report it to the security center by clicking \"Report!\"!");
                    dialog.setPositiveButton("Report!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {



                            onDisconnected();

                            Toast.makeText(MainActivity.this, "Scenario complete!", Toast.LENGTH_LONG).show();

                            final android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(MainActivity.this);
                            dialog.setIcon(android.R.drawable.alert_light_frame);
                            dialog.setTitle("Scenario Complete!");
                            dialog.setMessage("You finished the scenario! Please hand the phone to your administrator!");// please press \"Finish\" to exit the application!");
                            dialog.setPositiveButton("Ok!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    onBackPressed();

                                }
                            });

                            ArrayList<String> save = new ArrayList<>();
                            save.add(String.valueOf(recallTotal));
                            save.add(String.valueOf(findTotal));
                            try {
                                fileSave(testNum,testConnectCtr,save);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            testing = false;
                            dialog.show();


                        }
                    });

                    dialog.show();

                }else{
                    //TODO: Stop timer here.

                    //opend Dialog box to explain what the user will be doing in Scenario 2
                    final android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(MainActivity.this);
                    dialog.setIcon(android.R.drawable.btn_star);
                    dialog.setTitle("Congratulations!");
                    dialog.setMessage("You found the rogue AP! Report it to the operations center by clicking \"Report!\"!");
                    dialog.setPositiveButton("Report!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            onDisconnected();

                            Toast.makeText(MainActivity.this, "Task complete! Press start to connect to a new AP", Toast.LENGTH_LONG).show();
                            but_scen2Start.setEnabled(true);

                            but_scen2Start.setEnabled(true);


                            ArrayList<String> save = new ArrayList<>();
                            save.add(String.valueOf(recallTotal));
                            save.add(String.valueOf(findTotal));
                            try {
                                fileSave(testNum,testConnectCtr,save);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            testing = false;

                        }
                    });

                    dialog.show();
                }



            }

        }

    }

    private void fileSave(int test, int ctr, ArrayList<String> input) throws IOException {

        if(ctr == 3){

            //Strings
            String folder = Environment.getExternalStorageDirectory().getAbsolutePath();

            //create date to append to fileName
            now = Calendar.getInstance();
            Date time_now = now.getTime();
            String saveDate = df.format(time_now);

            //create file name
            String fileName = "s2_t"+String.valueOf(test)+"_2d_"+saveDate.substring(1,10)+".csv";
            file = folder+ File.separator+fileName;
            File file2 = new File(file);
            FileWriter fw;

            //if file exists append, otherwise create new file and write to the file

            fw = new FileWriter(file, true);

            for (int i = 0; i < input.size(); i++) {

                fw.append(input.get(i)+",");
                Log.i(SAVETAG,input.get(i));

            }

            fw.flush();
            fw.close();

        }
        else{

            //Strings
            String folder = Environment.getExternalStorageDirectory().getAbsolutePath();

            //create date to append to fileName
            now = Calendar.getInstance();
            Date time_now = now.getTime();
            String saveDate = df.format(time_now);

            //create file name
            String fileName = "s2_t"+String.valueOf(test)+"_2d_"+saveDate.substring(1,10)+".csv";
            file = folder+ File.separator+fileName;
            File file2 = new File(file);
            FileWriter fw;

            //if file exists append, otherwise create new file and write to the file
            if(file2.exists()){

                fw = new FileWriter(file, true);

                if(ctr==1){
                    String user_id = txt_scne2PID.getText().toString();
                    fw.append("\n"+user_id+",");
                    Log.i(SAVETAG,user_id);
                }

                for (int i = 0; i < input.size(); i++) {

                    fw.append(input.get(i)+",");
                    Log.i(SAVETAG,input.get(i));

                }

                fw.flush();
                fw.close();

            }else{

                fw = new FileWriter(file);

                String inputStr = "";

                String user_id = txt_scne2PID.getText().toString();
                inputStr = user_id+",";
                Log.i(SAVETAG,user_id);

                for (int i = 0; i < input.size(); i++) {

                    inputStr = inputStr+input.get(i)+",";

                }
                Log.i(SAVETAG,inputStr);
                fw.write(inputStr);
                fw.flush();
                fw.close();
            }

        }


    }

    @Override
    public void onStatusChanged(String status, int i, Bundle extras) {

    }

    @Override
    public void onEnterRegion(IARegion iaRegion) {



        if(polyline!=null){

            polyline.remove();

        }

        if(markerList!=null) {
            mMap.clear();
            for (String key : markerList.keySet()) {

                markerList.get(key).remove();

            }
            markerList.clear();
        }

        if(apList != null) {
            //clear apList to recreate
            apList.clear();
        }

        if(queriedResults!=null){

            queriedResults.clear();
            trafficRecord.clear();

        }

        //create a master region for reference
        currentRegion = iaRegion;

        //pull query criteria, specifically looking for fold and floor
        String[] criteria = region2building.get(iaRegion.getId()).split("[-]");
        String floorHeight = Integer.toString(Integer.parseInt(criteria[1])*10 - 10);
        Toast.makeText(this,  "Entered Region: " + criteria[0]+" Floor: "+floorHeight, Toast.LENGTH_SHORT).show();
        currentBuilding = criteria[0];

//        TextView userLoc = (TextView) findViewById(R.id.txt_UserLoc);
 //       userLoc.setText("Building: "+criteria[0]+"  Floor: "+criteria[1]);

        DataAdapter da2 = new DataAdapter(this);

        try {
            da2.createDatabase();
        } catch (IOException e) {
            Log.i("database",e.toString());
        }
        da2.open();
        if(criteria[0].equals("ITSL")){

            apList.add("TOR-A25AP03B,37.22926291,-80.41919840,0");
            //apList.add("sample,37.22916366,-80.41913083,0");

        }else {

            String sql = "select _id,lat,long,floor from ap where fold like '" + criteria[0].trim() + "%' and floor ='" + floorHeight.trim() + "';";
            regionAPs = da2.getData(sql);

            while (regionAPs.moveToNext()){

                apList.add(regionAPs.getString(0).trim()+","+regionAPs.getString(1).trim()+","+regionAPs.getString(2).trim()+","+regionAPs.getString(3).trim());

            }

        }

        //counter to ensure that map zooms off of the first AP
        int ctr = 0;

        for(String ap: apList){

            //from apList pull string, parse, then load information into a marker position which can then
            //be populated on the map.
            String[] apInfo = ap.split("[,]");
            LatLng markerPos = new LatLng(Double.parseDouble(apInfo[1]),Double.parseDouble(apInfo[2]));
            totalTraffic = 0;
            totalQueries = 0;

            Drawable mapIcon = getResources().getDrawable(R.drawable.google_map_white_black);
            Bitmap mapMap = ((BitmapDrawable) mapIcon).getBitmap();
            mapMap = Bitmap.createScaledBitmap(mapMap,80,134,true);

            markerList.put(apInfo[0], mMap.addMarker(new MarkerOptions().position(markerPos).title(apInfo[0]).icon( BitmapDescriptorFactory.fromBitmap(mapMap))));
            APTraffic(apInfo[0]);


            if (ctr == 0) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerPos, 17));
            }

            ctr++;


        }


        if(isConnected){

            connectViz();

        }



        /*
        WifiHandler wifiHandler = new WifiHandler();
        wifiHandler.javaCall(architectView,wifiHandler.apIdentifier(apList));
        String java_call = "World.locationChanged()";
        architectView.callJavascript(java_call);

        if(isConnected){
            architectView.callJavascript("World.apConnection(\""+apName+"\")");
        }*/

    }

    @Override
    public void onExitRegion(IARegion iaRegion) {

        //enter a region and pass this value to the logs
        Log.i(LOGTAG, "Exited Region: " + iaRegion.getType() + ", " + iaRegion.getId());
        //clear map of markers and lines and empty hashMap of saved markers
        if(polyline!=null){

            polyline.remove();

        }

        mMap.clear();

        for(String key:markerList.keySet()){

            markerList.get(key).remove();

        }

        markerList.clear();

        if(queriedResults!=null){

            queriedResults.clear();
            trafficRecord.clear();

        }


    }

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
            mShortestDisplacement = (float) .25;
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

    //------------------aux google Maps methods----------------------------------------------------------------

    //create method to determine if device has requisite services to run google maps.
    public boolean servicesOk(){

        //pull int which reveals whether services were reached
        int isAvail = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        //if success, return true
        if(isAvail == ConnectionResult.SUCCESS){
            return true;
        }
        //if error recoverable, display dialogue box to guide user through recovery
        else if (GooglePlayServicesUtil.isUserRecoverableError(isAvail)){
            Dialog err_di = GooglePlayServicesUtil.getErrorDialog(isAvail, this, ERROR_DIALOGUE_REQ);
            err_di.show();
        }
        //if unrecoverable inform user that mapping services are not available.
        else{
            Toast.makeText(this, "Can't connect to mapping service", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    //create a method to determine if location providers are enabled. can potentially run this in a
    //conditional loop.
    public boolean isLocationEnabled(){
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)||lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    //this is a method to show alert in the event that the position source isn't enabled. shoudl be used in combination
    //with the above is position enabled to prevent a run time crash.
    public void showAlert(){

        final AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("Enable Location")
                .setMessage("Your location or wireless settings are set to 'Off'.\nPlease enable location and wireless settings to use this app.")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent myInt = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myInt);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        dialog.show();

    }

    //---------------------Wifi Listener Methods----------------------------

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Toast.makeText(MainActivity.this, "broadcasttReceived", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onConnected() {

        if(started){

            if(study2){

                testConnectCtr++;
                Log.i(SCEN2TESTING,"TestNum: "+String.valueOf(testNum)+" ConnectCtr: "+String.valueOf(testConnectCtr));

                //if test number == 1
                if(testNum == 1){

                    //check how many times you've connected
                    if(testConnectCtr ==1){

                        Double lat = test1Locations.get("t1_1").getLatitude();
                        Double lon = test1Locations.get("t1_1").getLongitude();

                        apLocation = test1Locations.get("t1_1");

                        //distance based on provided random distance
                        dist2APwrt = curr_loc.distanceTo(apLocation);

                        apName = "rogue";
                        //pass to new js function
                        connectViz();

                    }else if(testConnectCtr == 2){

                        apName = testSafeAPs.get(0);
                        Log.i("javaCall","World.apConnection(\""+apName+"\",0,0)");
                        connectViz();


                    }else{

                        Double lat = test1Locations.get("t1_3").getLatitude();
                        Double lon = test1Locations.get("t1_3").getLongitude();


                        apLocation = test1Locations.get("t1_3");

                        //distance based on provided random distance
                        dist2APwrt = curr_loc.distanceTo(apLocation);
                        apName = "rogue";
                        connectViz();
                    }

                }else{

                    if(testConnectCtr ==1){

                        apName = testSafeAPs.get(1);
                        Log.i("javaCall","World.apConnection(\""+apName+"\",0,0)");
                        connectViz();

                    }else if(testConnectCtr == 2){

                        Double lat = test2Locations.get("t2_2").getLatitude();
                        Double lon = test2Locations.get("t2_2").getLongitude();

                        apLocation = test2Locations.get("t2_2");

                        //distance based on provided random distance
                        dist2APwrt = curr_loc.distanceTo(apLocation);

                        //pass to new js function
                        apName = "rogue";
                        connectViz();

                    }else{

                        Double lat = test2Locations.get("t2_3").getLatitude();
                        Double lon = test2Locations.get("t2_3").getLongitude();

                        apLocation = test2Locations.get("t2_3");

                        //distance based on provided random distance
                        dist2APwrt = curr_loc.distanceTo(apLocation);

                        //pass to new js function
                        apName = "rogue";
                        connectViz();

                    }

                }


            }else{
                if(connectCtr ==1) {

                    isConnected = true;

                    //create txtView to manipulate once connected
                    //
                    // TextView apLoc = (TextView) findViewById(R.id.txt_APLoc);

        /*
        if(polyline!=null){

            polyline.remove();

        }
        */

                    String lat = null;
                    String longi = null;
                    String floor = null;
                    String fold = null;

                    //wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
                    wifiInfo = wifiManager.getConnectionInfo();

                    String query = wifiInfo.getBSSID().trim().substring(0, wifiInfo.getBSSID().length() - 1) + "%";

                    //Instantiate dbVars
                    dataAdapter = new DataAdapter(this);
                    try {
                        dataAdapter.createDatabase();
                    } catch (IOException e) {
                        Log.i("database", e.toString());
                    }
                    dataAdapter.open();

                    String sql = "select _id,lat,long,floor,fold from ap where radio_mac1 like '" + query + "' or radio_mac2 like '" + query + "';";

                    try {
                        dbData = dataAdapter.getData(sql);

                        for (int i = 0; i < dbData.getColumnCount(); i++) {
                            switch (i) {

                                case 0:
                                    apName = dbData.getString(0).trim();
                                    break;
                                case 1:
                                    lat = dbData.getString(1);
                                    break;
                                case 2:
                                    longi = dbData.getString(2);
                                    break;
                                case 3:
                                    floor = dbData.getString(3);
                                    break;
                                case 4:
                                    fold = dbData.getString(4);
                                    break;

                                default:
                                    apName = apName;

                            }

                        }

                        apLocation.setLatitude(Double.parseDouble(lat));
                        apLocation.setLongitude(Double.parseDouble(longi));
                        Toast.makeText(this, "Connected to " + apName, Toast.LENGTH_SHORT).show();

                        Log.i("dataDump onConnect", "user: lat-" + curr_loc.getLatitude() + "longi-" + curr_loc.getLongitude());
                        Log.i("dataDump onConnect", "user: lat-" + apLocation.getLatitude() + "longi-" + apLocation.getLongitude());


                        dist2APwrt = curr_loc.distanceTo(apLocation);

                        //TODO troubleshoot to ensure that something is displayed when you connect without floor
                        //info


                        if (floor == null) {
                            floor = "1";
                            //apLoc.setText("Building: " + fold + "  Floor: " + floor);
                        } else {
                            int floorHeight = Integer.parseInt(dbData.getString(3)) / 10 + 1;
                            // apLoc.setText("Building: " + fold + "  Floor: " + floorHeight);
                        }


                    } catch (RuntimeException e) {
                        // apLoc.setText("AP not recognized");
                        Log.i("database", "no ap name returned");
                        Log.i("database", e.getMessage());

                    }
                    connectViz();

                    dataAdapter.close();
                }else{}

                connectCtr++;
            }

        }else{}



    }


    @Override
    public void onDisconnected() {

//        if(connectCtr > 0) {
//
//            connectCtr = 0;
//
//
//        }

        if (polyline != null) {

            polyline.remove();


        }
        if (apName != null && !apTrk.equals("rogue")) {

            Drawable mapIcon = getResources().getDrawable(R.drawable.google_map_white_black);
            Bitmap mapMap = ((BitmapDrawable) mapIcon).getBitmap();
            mapMap = Bitmap.createScaledBitmap(mapMap,80,134,true);

            markerList.get(apName).setIcon(BitmapDescriptorFactory.fromBitmap(mapMap));
        }
        isConnected = false;
        //TextView apLoc = (TextView) findViewById(R.id.txt_APLoc);
        //apLoc.setText("Building:          Floor:");

        if (rogueMarker != null) {

            rogueMarker.remove();

        }


    }

    //method invoked on location changed to draw a line from location to connected AP (if connected)
    //solves the issue of not having the location upon start up
    public void connectViz(){
        //local variables

        if(polyline!=null){

            polyline.remove();

        }

        if(apName.equals("rogue")){

            apTrk = "rogue";

            LatLng markerPos = new LatLng(apLocation.getLatitude(),apLocation.getLongitude());

            Drawable mapIcon = getResources().getDrawable(R.drawable.ap_alert);
            Bitmap mapMap = ((BitmapDrawable) mapIcon).getBitmap();
            mapMap = Bitmap.createScaledBitmap(mapMap,110,110,true);

            polyLineMark = rogueMarker = mMap.addMarker(new MarkerOptions().position(markerPos).icon(BitmapDescriptorFactory.fromBitmap(mapMap)));

            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(new LatLng(curr_loc.getLatitude(), curr_loc.getLongitude()))
                    .add(new LatLng(rogueMarker.getPosition().latitude,
                            rogueMarker.getPosition().longitude))
                    .width(20);

            polyline = mMap.addPolyline(polylineOptions);

            recallStart = System.currentTimeMillis();

        }
        else {
            apTrk = "safe";
            //if ap is contained within the
            if (markerList.get(apName) != null) {
                PolylineOptions polylineOptions = new PolylineOptions()
                        .add(new LatLng(curr_loc.getLatitude(), curr_loc.getLongitude()))
                        .add(new LatLng(markerList.get(apName).getPosition().latitude,
                                markerList.get(apName).getPosition().longitude))
                        .width(20);

                polyLineMark = markerList.get(apName);

                markerList.get(apName).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));


                polyline = mMap.addPolyline(polylineOptions);

                recallStart = System.currentTimeMillis();
            }
        }

    }

    //--------------------click listeners------------------------------------------------

    public void StartTask(View view) {

        testing = true;
        startTime = System.currentTimeMillis(); //SystemClock.currentThreadTimeMillis();
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        Log.i("time","start time: "+startTime);


       // partID.setEnabled(false);

        Intent openVPN = new Intent("android.intent.action.VIEW");
        openVPN.setPackage("net.openvpn.openvpn");
        openVPN.setClassName("net.openvpn.openvpn", "net.openvpn.openvpn.OpenVPNDisconnect");
        startActivityForResult(openVPN, 0);


    }


    public void FoundAP(View view) {

        //local Variables
        String apTraffic = trafficRecord.get(apName);

        testing = false;

        trafficMean = 0;
        trafficSD = 0;
        traffic3Q = 0;

        totalQueries = queriedResults.size();

        totalTime = System.currentTimeMillis()-startTime;

        Log.i("time","start time: "+startTime);
        Log.i("time","current time: "+System.currentTimeMillis());
        Log.i("time","total time: "+totalTime);

        wifiManager.setWifiEnabled(false);

        trafficMean = totalQueries*17.18;
        trafficSD = totalQueries*37.558;
        traffic3Q = trafficMean+trafficSD;

        if(totalTraffic < trafficMean){
            trafficLevel = "low";

        }else if(totalTraffic>= trafficMean && totalTraffic<traffic3Q){

            trafficLevel = "moderate";
        }
        else{

            trafficLevel = "heavy";
        }

        Toast.makeText(this, "traffic level: "+trafficLevel, Toast.LENGTH_SHORT).show();

        //write to a file, will try an duse fileWriter exclusively to open a file if it doesn't
        //exist and write to it if it does

        String folder = Environment.getExternalStorageDirectory().getAbsolutePath();

        //create date to append to fileName
        Date time_now = now.getTime();
        String saveDate = df.format(time_now);

        //create file name
        String fileName = "twoDConnect_"+saveDate.substring(1,10)+".csv";
        file = folder+ File.separator+fileName;
        File file2 = new File(file);
        FileWriter fw;

        //if file exists append, otherwise create new file and write to the file
        if(file2.exists()){

            try {
                fw = new FileWriter(file, true);
                //fw.append(partID.getText().toString());
                fw.append(",");
                fw.append(Float.toString(dist2APwrt));
                fw.append(",");
                fw.append(Long.toString(totalTime));
                fw.append(",");
                fw.append(apTraffic);
                fw.append(",");
                fw.append(trafficLevel);
                fw.append("\n");
                fw.flush();
                fw.close();

                Log.i("dataDump dump","dist2AP: "+dist2APwrt);
                Log.i("dataDump dump","user: lat-"+curr_loc.getLatitude()+"longi-"+curr_loc.getLongitude());
                Log.i("dataDump dump","user: lat-"+apLocation.getLatitude()+"longi-"+apLocation.getLongitude());

            } catch (IOException e) {

                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

        }
        else{

            try {
                fw = new FileWriter(file);
                //fw.write(partID.getText().toString()+","+Float.toString(dist2APwrt)+","+
                //        Long.toString(totalTime)+","+apTraffic+","+trafficLevel+"\n");
                fw.flush();
                fw.close();
                Log.i("dataDump dump","dist2AP: "+dist2APwrt);
                Log.i("dataDump dump","user: lat-"+curr_loc.getLatitude()+"longi-"+curr_loc.getLongitude());
                Log.i("dataDump dump","user: lat-"+apLocation.getLatitude()+"longi-"+apLocation.getLongitude());

            } catch (IOException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }

       // partID.setEnabled(true);
       // but_findAP.setEnabled(false);
      //  but_findAP.setBackgroundColor(0xFFFF4081);
    }

    public void DifferentFloor(View view) {

        if(differentFloor){

            trafficMean = 0;
            trafficSD = 0;
            traffic3Q = 0;
            totalQueries = queriedResults.size();

            totalTime = System.currentTimeMillis()-startTime;
            wifiManager.setWifiEnabled(false);

            trafficMean = totalQueries*17.18;
            trafficSD = totalQueries*37.558;
            traffic3Q = trafficMean+trafficSD;

            if(totalTraffic < trafficMean){
                trafficLevel = "low";

            }else if(totalTraffic>= trafficMean && totalTraffic<traffic3Q){

                trafficLevel = "moderate";
            }
            else{

                trafficLevel = "heavy";
            }

            Toast.makeText(this, "traffic level: "+trafficLevel, Toast.LENGTH_SHORT).show();

            //write to a file, will try an duse fileWriter exclusively to open a file if it doesn't
            //exist and write to it if it does

            String folder = Environment.getExternalStorageDirectory().getAbsolutePath();

            //create date to append to fileName
            Date time_now = now.getTime();
            String saveDate = df.format(time_now);

            //create file name
            String fileName = "twoDConnect_"+saveDate.substring(1,10)+".csv";
            file = folder+ File.separator+fileName;
            File file2 = new File(file);
            FileWriter fw;

            //if file exists append, otherwise create new file and write to the file
            if(file2.exists()){

                try {
                    fw = new FileWriter(file, true);
                   // fw.append(partID.getText().toString());
                    fw.append(",");
                    fw.append("DiffFloor");
                    fw.append(",");
                    fw.append(Long.toString(totalTime));
                    fw.append(",");
                    fw.append(trafficLevel);
                    fw.append("\n");
                    fw.flush();
                    fw.close();

                } catch (IOException e) {

                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

            }
            else{

                try {
                    fw = new FileWriter(file);
                  //  fw.write(partID.getText().toString()+",DiffFloor,"+
                   //         Long.toString(totalTime)+","+trafficLevel+"\n");
                    fw.flush();
                    fw.close();

                } catch (IOException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }

          //  partID.setEnabled(true);
          //  but_findAP.setEnabled(false);
          //  but_findAP.setBackgroundColor(0xFFFF4081);


        }
    }

    public void Re_query(View view) {

//        if(markerList!=null) {
//
//            for (String key : markerList.keySet()) {
//
//                markerList.get(key).remove();
//
//            }
//            markerList.clear();
//        }

//        for(String ap: apList){
//
//            //from apList pull string, parse, then load information into a marker position which can then
//            //be populated on the map.
//            String[] apInfo = ap.split("[,]");
//            LatLng markerPos = new LatLng(Double.parseDouble(apInfo[1]),Double.parseDouble(apInfo[2]));
//            totalTraffic = 0;
//            totalQueries = 0;
//            markerList.put(apInfo[0], mMap.addMarker(new MarkerOptions().position(markerPos).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))));
//            APTraffic(apInfo[0]);
//
//        }

        //queriedResults.clear();

//        Intent openVPN = new Intent("android.intent.action.VIEW");
//        openVPN.setPackage("net.openvpn.openvpn");
//        openVPN.setClassName("net.openvpn.openvpn", "net.openvpn.openvpn.OpenVPNClient");
//        openVPN.putExtra("net.openvpn.openvpn.AUTOSTART_PROFILE_NAME", "admin");
//        startActivityForResult(openVPN, 0);

        failedQueries = new ArrayList<>();

        for(String key : markerList.keySet()){

            if(!queriedResults.contains(key)){

                failedQueries.add(key);

            }
        }

        for(String ap:failedQueries){

            APTraffic(ap);
        }
    }

    //-------metrics development and reporting-------------------------------------------

    public void APTraffic(final String accessPt){



        now = Calendar.getInstance();
        Date time_now = now.getTime();
        String endDate = df.format(time_now);

        Date startTimedt = time_now;

        startTimedt.setHours(0);
        startTimedt.setMinutes(0);
        startTimedt.setSeconds(0);

        String startTime = df.format(startTimedt);

        String urlToReadDeauth = "http://10.10.200.151:9200/logstash-*/_search?q=%2B@timestamp%3A["+startTime+"+TO+"+endDate+"]+%2Bsyslog_program%3Astm+%2Bap_name%3A%22"+accessPt+"%22+%2Bwlan_event%3A%22Deauth%20to%20sta%22&sort=@timestamp:desc&size=1";
        String urlToReadSuccess = "http://10.10.200.151:9200/logstash-*/_search?q=%2B@timestamp%3A["+startTime+"+TO+"+endDate+"]+%2Bsyslog_program%3Astm+%2Bap_name%3A%22"+accessPt+"%22+%2Bwlan_event%3A%22Assoc%20success%22&sort=@timestamp:desc&size=1";

        Log.i("HTML Query","Query: "+urlToReadDeauth);
        Log.i("HTML Query","Query: "+urlToReadSuccess);

        JSONObject test = null;

        jsonObjectRequest = new JsonObjectRequest(urlToReadDeauth, test, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                Log.i("HTML Query", "response1: " + response);

                try {
                    JSONObject sourceObject = response.getJSONObject("hits");
                    totalHitsDeauth = (Integer) sourceObject.get("total");
                    query1 = true;
                    JSONArray sourceArray = sourceObject.getJSONArray("hits");
                    JSONObject source2 = sourceArray.getJSONObject(0);
                    source2 = source2.getJSONObject("_source");
                    String APName = source2.getString("ap_name");
                    MarkerPlot(query1,query2,query3,query4,APName);

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

                Log.d("HTML Query", "error: " + volleyError.toString());

            }
        }
        );
        jsonObjectRequest2 = new JsonObjectRequest(urlToReadSuccess, test, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                Log.i("HTML Query", "response2: " + response);

                try {
                    JSONObject sourceObject = response.getJSONObject("hits");
                    totalHitsSuccess = (Integer) sourceObject.get("total");
                    query2 = true;
                    JSONArray sourceArray = sourceObject.getJSONArray("hits");
                    JSONObject source2 = sourceArray.getJSONObject(0);
                    source2 = source2.getJSONObject("_source");
                    String APName = source2.getString("ap_name");
                    MarkerPlot(query1,query2,query3,query4,APName);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

                Log.d("HTML Query", "error: " + volleyError.toString());

            }
        }
        );

        Date topOftheHour = time_now;
        topOftheHour.setMinutes(0);
        topOftheHour.setSeconds(0);

        String topOfHour = df.format(topOftheHour);

        String urlToReadDeauth2 = "http://10.10.200.151:9200/logstash-*/_search?q=%2B@timestamp%3A["+startTime+"+TO+"+topOfHour+"]+%2Bsyslog_program%3Astm+%2Bap_name%3A%22"+accessPt+"%22+%2Bwlan_event%3A%22Deauth%20to%20sta%22&sort=@timestamp:desc&size=1";
        String urlToReadSuccess2 = "http://10.10.200.151:9200/logstash-*/_search?q=%2B@timestamp%3A["+startTime+"+TO+"+topOfHour+"]+%2Bsyslog_program%3Astm+%2Bap_name%3A%22"+accessPt+"%22+%2Bwlan_event%3A%22Assoc%20success%22&sort=@timestamp:desc&size=1";

        jsonObjectRequest3 = new JsonObjectRequest(urlToReadSuccess2, test, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                Log.i("HTML Query", "response3: " + response);

                try {
                    JSONObject sourceObject = response.getJSONObject("hits");
                    hourSuccesses = (Integer) sourceObject.get("total");
                    query3 = true;
                    JSONArray sourceArray = sourceObject.getJSONArray("hits");
                    JSONObject source2 = sourceArray.getJSONObject(0);
                    source2 = source2.getJSONObject("_source");
                    String APName = source2.getString("ap_name");
                    MarkerPlot(query1,query2,query3,query4,APName);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

                Log.d("HTML Query", "error: " + volleyError.toString());

            }
        }
        );

        jsonObjectRequest4 = new JsonObjectRequest(urlToReadDeauth2, test, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                Log.i("HTML Query", "response4: " + response);

                try {
                    JSONObject sourceObject = response.getJSONObject("hits");
                    hourDeauths = (Integer) sourceObject.get("total");
                    query4 = true;
                    JSONArray sourceArray = sourceObject.getJSONArray("hits");
                    JSONObject source2 = sourceArray.getJSONObject(0);
                    source2 = source2.getJSONObject("_source");
                    String APName = source2.getString("ap_name");
                    MarkerPlot(query1,query2,query3,query4,APName);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

                Log.d("HTML Query", "error: " + volleyError.toString());

            }
        }
        );

        jsonObjectRequest.setRetryPolicy(policy);
        mRequestQueue.add(jsonObjectRequest);
        jsonObjectRequest2.setRetryPolicy(policy);
        mRequestQueue.add(jsonObjectRequest2);
        jsonObjectRequest3.setRetryPolicy(policy);
        mRequestQueue.add(jsonObjectRequest3);
        jsonObjectRequest4.setRetryPolicy(policy);
        mRequestQueue.add(jsonObjectRequest4);

    }

    public void MarkerPlot(Boolean q1, Boolean q2, Boolean q3, Boolean q4, String APName){

        //local variables
        String tfcLevel = null;

        if(q1 && q2 && q3 && q4) {

            query1 = false;
            query2 = false;
            query3 = false;
            query4 = false;

            //now perform simple arithmetic to determin, new successes and new deauths from the hour on,
            //and then subtract from each other to determine a generalized idea of current traffic
            //on the AP
            int tmpSuccess = totalHitsSuccess - hourSuccesses;
            int tmpDeauth = totalHitsDeauth - hourDeauths;
            int asso_diff = tmpSuccess - tmpDeauth;



            if (asso_diff < 17) {
                markerColor = BitmapDescriptorFactory.HUE_GREEN;
                tfcLevel = "light";

            } else if (asso_diff >= 17 && asso_diff < 54) {
                markerColor = BitmapDescriptorFactory.HUE_YELLOW;
                tfcLevel = "moderate";


            } else {
                markerColor = BitmapDescriptorFactory.HUE_RED;
                tfcLevel = "heavy";

            }

            if(markerList!=null) {

                try {
                    markerList.get(APName).setIcon(BitmapDescriptorFactory.defaultMarker(markerColor));
                    //Toast.makeText(this, "amount of hits: " + asso_diff, Toast.LENGTH_SHORT).show();
                }
                catch (Exception e){

                    //Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();

                }
            }

            //add result into queried results list
            if(!queriedResults.contains(APName)){

                //only add to total traffic when you add a name to the queried results
                queriedResults.add(APName);
                totalTraffic = totalTraffic + asso_diff;
                trafficRecord.put(APName,tfcLevel);

            }

        }
    }


    public void scen2Start_click(View view) {

        String user_id;
        user_id = String.valueOf(txt_scne2PID.getText().toString());

        if(user_id==""){

            Toast.makeText(this, "You must enter a valid userID!", Toast.LENGTH_SHORT).show();


        }else{

            //set started to true
            started = true;

            //wifiManager.setWifiEnabled(true);
            but_safe.setEnabled(true);
            but_rogue.setEnabled(true);

            //call onConnect/ConnectViz

            but_scen2Start.setEnabled(false);
            but_finish.setEnabled(false);

            //use this formula to shift the testNumber from that of AR users
            testNum = Math.abs(Integer.parseInt(user_id)%2-1);

            onConnected();

            started = false;

        }
    }

    public void safe_click(View view) throws IOException {



        if(apTrk == "safe"){
            //  wifiManager.setWifiEnabled(false);

            recallTotal = System.currentTimeMillis()-recallStart;

            but_scen2Start.setEnabled(true);

            //opend Dialog box to explain what the user will be doing in Scenario 2
            final android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(MainActivity.this);
            dialog.setIcon(android.R.drawable.btn_star);
            dialog.setTitle("Correct choice!");
            dialog.setMessage("Congratualtions, you've chosen correctly! Press start to connect to another access point!");

            dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    onDisconnected();

                    ArrayList<String> save = new ArrayList<>();
                    save.add(String.valueOf(recallTotal));
                    try {
                        fileSave(testNum,testConnectCtr,save);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
            });

            dialog.show();

            but_safe.setEnabled(false);
            but_rogue.setEnabled(false);

            but_scen2Start.setEnabled(true);

        }else{

            Toast.makeText(this, "Incorrect! Please select again.", Toast.LENGTH_SHORT).show();

        }


    }

    public void rogue_click(View view) throws IOException {



        if(apTrk.equals("rogue")){

            recallTotal = System.currentTimeMillis()-recallStart;

            //opend Dialog box to explain what the user will be doing in Scenario 2
            final android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(MainActivity.this);
            dialog.setIcon(android.R.drawable.btn_star);
            dialog.setTitle("Find Rogue AP!");
            dialog.setMessage("Congratualtions, you've chosen correctly! You are connected to a rogue AP. Use your application to find and report" +
                    "this rogue AP. You will complete this task once you have moved close enough to the rogue AP. You will see another pop-up message when that happens.");
            dialog.setPositiveButton("Find AP!", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    //TODO: Start timer here.
                    findStart = System.currentTimeMillis();
                    testing = true;

                }
            });

            dialog.show();

            but_rogue.setEnabled(false);
            but_safe.setEnabled(false);

            but_scen2Start.setEnabled(false);


        }
        else{

            Toast.makeText(this, "Incorrect! Please select again.", Toast.LENGTH_SHORT).show();

        }
    }


    public void finish_click(View view) {

        this.onBackPressed();
    }

    public void start2_click(View view) {
    }
}
