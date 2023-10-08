package com.vt.itsl.twod_connect;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

public class Scenario1 extends AppCompatActivity implements OnMapReadyCallback {

    //global variables
    //location api
    //locaiton
    Location curr_loc = null;
    LocationListener and_ll = null;
    LocationManager and_lm = null;
    Criteria locationCriteria = null;
    GoogleMap mMap = null;

    //latlng - for initial location when map loads
    LatLng bburg = new LatLng(37.2239, -80.4073);

    //use this constant to request certain dialogue box form google play service library.
    private static final int ERROR_DIALOGUE_REQ = 1001;

    //instantiate permission constants to eventually determine if the app has permissions to use location etc
    private static final String[] INIT_PERMS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET};
    //have to establish a int request number, potentially to determine if permissions requested before etc
    private static final int INIT_REQ = 0;

    //indoorAtlas
    //static values to request and accept permissions for indoor location engines
    public static final String[] INDOOR_LOCA_PERM = {Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static final int INDOOR_LOCA_CHK = 1;

    //HashMaps
    HashMap<String, Marker> markerList;
    HashMap<String, HashMap> buildingLocs;

    //ArrayList
    ArrayList<String> buildingList = new ArrayList<>();

    //Longs
    long task1_start;
    long task1_total;
    long t1_recall_st;
    long t1_recall_tot;
    long task2_start;
    long task2_total;

    //int
    int hiLvl_ctr = 0;

    //views
    //text
    EditText txt_scen1PID;
    TextView txt_scen1Hi;
    TextView txt_closeLw;
    //Buttons
    Button but_scen1Start;
    Button but_scen2Start;

    //calendar
    Calendar now = null;
    //dateFormat
    public static DateFormat df;

    //Boolean
    Boolean startClicked = false;
    Boolean task2 = false;
    Boolean taskComp = false;

    //String
    String closest_build = null;


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scenario1);

        //permission checks for required services
        //before you generate any mapping code, run servicesOK method to determine if user device is compatible
        //with google services.
        if (servicesOk()) {
            Toast.makeText(this, "Compatible!", Toast.LENGTH_LONG).show();
            //Changed to a mapFragment object as I was having issues with the supporMapFragment class.
            //should continue to work with all as this example is ripped off the android website.
            //so you associate the mapfragment with the fragment id, then you get the map and associate
            //it with a certain activity. the activity has to implement the OnReadyMapCallback interface.
            MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map3);
            mapFragment.getMapAsync(Scenario1.this);
        } else {
            Toast.makeText(this, "Incompatible with Google Play services", Toast.LENGTH_LONG).show();
        }

        //on create determine if this app has given the app the permissions necessary to utilize the location and google services api
        if (ActivityCompat.checkSelfPermission(Scenario1.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(Scenario1.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
            requestPermissions(INIT_PERMS, INIT_REQ);
        }

        //instantiate variables

        //views

        //text
        txt_scen1PID = (EditText) findViewById(R.id.txt_scen1PID);
        txt_scen1Hi = (TextView) findViewById(R.id.txt_scen1HA);
        txt_closeLw = (TextView) findViewById(R.id.txt_closeLow);
        //Buttons
        but_scen1Start = (Button) findViewById(R.id.but_scen1Start);
        but_scen2Start = (Button) findViewById(R.id.but_scen1Start2);

        but_scen2Start.setEnabled(false);

        //df formating
        //adjust the date time format for queries
        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        //ArrayLists
        buildingList = new ArrayList<>();

        //hashMaps
        markerList = new HashMap<>();
        buildingLocs = new HashMap<>();

        //instantiate the location manager class within the location service service context
        //believe this starts pull information from built in location features.
        and_lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //allow the user to enable location settings if they are off.
        if (isLocationEnabled() == false) {
            showAlert();
        }
        //instantiate criteria to develop faithful positioning
        locationCriteria = new Criteria();

        //use criteria to set the provider to provide you with the necessary accuracy.
        final String provider = and_lm.getBestProvider(locationCriteria, true);
        //pass location update to the last known location. last known location is known to return null
        //if another location hasn't used gps recently
        curr_loc = and_lm.getLastKnownLocation(provider);

        //location listener
        and_ll = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

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

        if (curr_loc == null) {
            //request an instant update on location information if location item is null. won't get data without this
            and_lm.requestLocationUpdates(provider, 1, 0, and_ll);
            curr_loc = and_lm.getLastKnownLocation(provider);
        }

        //request location updates every 5 seconds when the app is open
        and_lm.requestLocationUpdates(provider, 5000, 0, and_ll);

        //build buildingList

        buildingList.add("williams hall");
        buildingList.add("Torgerson Hall");
        buildingList.add("Squires");
        buildingList.add("Patton");
        buildingList.add("Newman Library");
        buildingList.add("Hutcheson");
        buildingList.add("Holden");
        buildingList.add("War Memorial Hall");
        buildingList.add("Davidson");
        buildingList.add("Campbell Main");
        buildingList.add("Burruss");
        buildingList.add("Mcbryde");
        buildingList.add("pamplin hall");
        buildingList.add("Hahn Hall South");
        buildingList.add("eggleston main");
        buildingList.add("sandy hall");
        buildingList.add("price hall");



    }

    //aux googleMaps methods -------------------------------------------------------

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
        return and_lm.isProviderEnabled(LocationManager.GPS_PROVIDER)||and_lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    //this is a method to show alert in the event that the position source isn't enabled. shoudl be used in combination
    //with the above is position enabled to prevent a run time crash.
    public void showAlert(){

        final AlertDialog.Builder dialog = new AlertDialog.Builder(Scenario1.this);
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

    @Override
    public void onMapReady(GoogleMap googleMap) {

        //double triple permissions check
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //googleMap.setMyLocationEnabled(true);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.2290196,-80.4219621),15));

        mMap = googleMap;

        mMap.setMyLocationEnabled(true);

        buildGen(buildingList);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                  @Override
                  public boolean onMarkerClick(Marker marker) {


                      if(startClicked && !taskComp) {

                          //local variables
                          //string
                          String title;
                          //bitmap
                          BitmapDescriptor bm = null;

                          title = marker.getTitle();

                          if (!task2) {

                              Drawable mapIcon = getResources().getDrawable(R.drawable.select);
                              Bitmap mapMap = ((BitmapDrawable) mapIcon).getBitmap();
                              mapMap = Bitmap.createScaledBitmap(mapMap, 110, 110, true);
                              bm = BitmapDescriptorFactory.fromBitmap(mapMap);

                              if (buildingLocs.get(title).get("alert").equals("2")  && buildingLocs.get(title).get("clicked").equals("no")) {

                                  buildingLocs.get(title).put("clicked","yes");
                                  marker.setIcon(bm);
                                  txt_scen1Hi.setText(txt_scen1Hi.getText() + title + "\n");
                                  hiLvl_ctr++;

                                  task1complete(hiLvl_ctr);

                              } else if(buildingLocs.get(title).get("clicked").equals("yes")){

                                  Toast.makeText(Scenario1.this, "You already clicked this one! Keep looking! Remember, red triangles.", Toast.LENGTH_SHORT).show();

                              }
                              else {
                                  Toast.makeText(Scenario1.this, "Not a high threat level! Keep looking! Remember, red triangles.", Toast.LENGTH_SHORT).show();
                              }
                          } else {

                              if (title.equals(closest_build)) {
                                  task2_total = System.currentTimeMillis() - task2_start;
                                  fileSave(2, String.valueOf(task2_total));
                                  //Toast.makeText(Scenario1.this, "Success! You found the closest, \"safest\" building!", Toast.LENGTH_SHORT).show();

                                  //Dialog
                                  android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(Scenario1.this);
                                  dialog.setIcon(android.R.drawable.btn_star);
                                  dialog.setTitle("Task 2 Complete!");
                                  dialog.setMessage("Congratulations, you've found the closest low threat building! Time to move onto Scenario 2!");
                                  dialog.setPositiveButton("Scenario 2", new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialogInterface, int i) {

                                          onBackPressed();

                                      }
                                  });
                                  dialog.show();

                                  taskComp = true;
                                  but_scen2Start.setEnabled(true);


                              } else {
                                  Toast.makeText(Scenario1.this, "This is not the right building! Remember, green circles!", Toast.LENGTH_SHORT).show();
                              }
                          }
                      }
                      else{}

                      return true;

                  }
              }
            );

        }
    //lifecycle methods-----------------------------------------


    //activity methods------------------------------------------------

    //method to generate enough high threat levels (7)
    //a function that generates a pseudo random sequence for high alerts among presented buildings
    ArrayList<Integer> sequenceGen(int listLength){

        //local variables
        //DOubles
        Double lam=0.0;
        Double lam_new=0.0;
        Double lam_tmp = 0.0;
        Double testVal = 0.0;
        Double propCalc = 0.0;
        //int
        int ctr = 0;
        int no_ctr = 1;
        int listDiff = 0;
        int alertDiff = 0;
        int currRnd = 0;

        //Random
        Random randomGen = new Random();

        //ArrayList
        ArrayList<Integer> seqList = new ArrayList<>();

        //proportion of presented non-hi alerts

        lam = 1- ((double) 5/listLength);


        //for length of building list
        for (int i = 1; i < listLength+1; i++) {

//            if (i == 13) {
//                //seqList.add(0);
//            }else if(i==5){
//                //seqList.add(1);
//            } else{
//
//
//
//            }
            //compare the difference in buildList length to sequence size
            listDiff = listLength - seqList.size();
            //compare difference in generated high alerts to target number, 7
            alertDiff = 5 - ctr;

            //if these differences are the same, you have to fill ou the list with high alerts
            if (listDiff == alertDiff) {

                for (int j = 0; j < alertDiff; j++) {

                    seqList.add(2);

                }
                //completely break the original for loop if you completed the clause
                break;
            } else if (ctr == 5) {
                //add 0's or 1's because we already have 7 high alerts
                int randInt = randomGen.nextInt(9);
                if (randInt > 4) {
                    seqList.add(1);
                } else {
                    seqList.add(0);
                }
            } else {

                //generate new proportion of non-high alerts
                //subtract the current round by how many high lvl elements you've presented


                //equate new lam to old lam to facilitate exponizing the
                lam_new = lam;


                //exponizing the proprotion via for loop because i don't have the internet
                for (int j = 1; j < no_ctr; j++) {

                    //if j is greater than 0, multiply lam by itself
                    if (no_ctr > 1) {
                        lam_new = lam * lam_new;
                    } else {
                        //else lam_new just equals lam because you just had a success or the series started
                        lam_new = lam;
                    }

                }

                //generate test val and compare to lamNew
                testVal = randomGen.nextDouble();
                //if testVal is greater than random gen value, then add a high alert to the
                //sequence and add counter
                if (testVal > lam_new) {

                    seqList.add(2);
                    no_ctr = 1;
                    ctr++;

                }
                //else add a one or a zero based on a random number
                else {
                    //increment counter that keeps tracking of no events
                    no_ctr++;
                    //local varaibles
                    int randInt = randomGen.nextInt(9);
                    if (randInt > 4) {
                        seqList.add(1);
                    } else {
                        seqList.add(0);
                    }

                }

            }
        }

        return seqList;

    }

    //adjust method to return a json array to produce a list of markers
    void buildGen(ArrayList<String> buildNm){

        //localVariables
        //seqList for trafficLevel
        ArrayList<Integer> seqList = sequenceGen(buildNm.size());
        //JsonArray
        JSONArray buildingArray = new JSONArray();
        //dataAdapter to query database
        DataAdapter da = new DataAdapter(this);



        //for all buildings in provided building list
        for(String name : buildNm){


            //local variables

            //open database to get building info
            try{
                da.createDatabase();

            }catch(IOException e){
                Log.i("database",e.toString());
            }
            da.open();


            //Strings
            String sql = "select lat,long from building where name like \""+name+"%\";";
            String lat = null;
            String lon = null;
            String alert = null;
            //HashMap to store data before JSONObject
            HashMap<String,String> storeData = new HashMap<>();
            //cursor
            Cursor result = da.getData(sql);
            //latLong to create marker position
            LatLng markerPos = null;

            //get lat and lon from local database

            //have to play around witht he nature of data returned as well as the amount. If multiple entries
            //are returned then only accept data when lat/lon are null, not when a value is present already.
            if(result.getCount()>1) {
                result.moveToFirst();
                lat = result.getString(0);
                lon = result.getString(1);
                /*
                while (result.moveToLast()) {

                    if(lat == null || lat.equals("")) {
                        lat = result.getString(0);
                        lon = result.getString(1);
                    }

                }
                */
            }
            else if(result.getCount() == 0){
                lat = null;
                lon = null;
            }
            else{
                lat = result.getString(0);
                lon = result.getString(1);
            }

            alert = String.valueOf(seqList.get(buildNm.indexOf(name)));

            markerPos = new LatLng(Double.parseDouble(lat),Double.parseDouble(lon));

            markerList.put(name, mMap.addMarker(new MarkerOptions().position(markerPos).icon(markerCreate(alert)).title(name)));

            //store data in a hashMap before storing in a JSON object to put in a json Array
            storeData.put("id",name.trim());
            storeData.put("lat",lat);
            storeData.put("lon",lon);
            Log.i("Lat/Long",lat+" "+lon);
            storeData.put("alert",alert);
            storeData.put("clicked","no");

            //store tmp hash map into buildingLoc hashmap for later
            buildingLocs.put(name,storeData);

            da.close();

        }
    }

    //method to assign the correct icon to the marker list
    BitmapDescriptor markerCreate(String alert){

        //local variables
        BitmapDescriptor bm = null;

        switch(alert){

            case "0":

                //local variables
                Drawable mapIcon = getResources().getDrawable(R.drawable.green_circle);
                Bitmap mapMap = ((BitmapDrawable) mapIcon).getBitmap();
                mapMap = Bitmap.createScaledBitmap(mapMap,110,110,true);
                bm = BitmapDescriptorFactory.fromBitmap(mapMap);

                break;
            case "1":
                //local variables
                Drawable mapIcon1 = getResources().getDrawable(R.drawable.yellow_square);
                Bitmap mapMap1 = ((BitmapDrawable) mapIcon1).getBitmap();
                mapMap1 = Bitmap.createScaledBitmap(mapMap1,110,110,true);
                bm = BitmapDescriptorFactory.fromBitmap(mapMap1);


                break;
            case "2":
                //local variables
                Drawable mapIcon2 = getResources().getDrawable(R.drawable.red_triangle);
                Bitmap mapMap2 = ((BitmapDrawable) mapIcon2).getBitmap();
                mapMap2 = Bitmap.createScaledBitmap(mapMap2,110,110,true);
                bm = BitmapDescriptorFactory.fromBitmap(mapMap2);

                break;
            default:
                break;

        }

        return bm;

    }


    public void start_click(View view) {

        //Dialog
        android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(Scenario1.this);
        dialog.setIcon(android.R.drawable.alert_light_frame);
        dialog.setTitle("Task 1");
        dialog.setMessage("Use your application to find and select (click) all buildings with a \"high\" threat level. Once you're done, we'll ask you a question about how many high levels you found. Remember, you're looking for red triangles!");
        dialog.setPositiveButton("Begin task!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                task1_start = System.currentTimeMillis();


            }
        });
        dialog.show();

        startClicked = true;
        but_scen1Start.setEnabled(false);
    }

    private void task1complete(int hiLvl_ctr) {

        //Dialog

        final android.app.AlertDialog.Builder task2_dialog = new android.app.AlertDialog.Builder(Scenario1.this);
        task2_dialog.setIcon(android.R.drawable.alert_light_frame);
        task2_dialog.setTitle("Scenario 1: Task 2");
        task2_dialog.setMessage("Now, you want to find a \"safe\" in which to do work. Use your application to find and select (click) the building closest to you with a low threat level. Think a bigger green circle! Click \"Find\" to begin your task.");
        task2_dialog.setPositiveButton("Ok!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                but_scen2Start.setEnabled(true);


            }
        });

        final android.app.AlertDialog.Builder finalTask2_dialog = task2_dialog;

        if(hiLvl_ctr==5){

            task1_total = System.currentTimeMillis()-task1_start;

            //Dialog
            final Dialog dialog = new Dialog(Scenario1.this);

            dialog.setContentView(R.layout.task1_recall_dialog);
            dialog.setTitle("How do you feel?");

            //radio group
            final RadioGroup rg = (RadioGroup) dialog.findViewById(R.id.recallQuestions);

            Button but_dialog = (Button) dialog.findViewById(R.id.but_task1Submit);

            but_dialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {


                    t1_recall_tot = System.currentTimeMillis() - t1_recall_st;
                    //local variable
                    //Strings
                    String args = null;

                    //radio button
                    RadioButton rb = (RadioButton) dialog.findViewById(rg.getCheckedRadioButtonId());
                    //String
                    String rb_id = rb.getResources().getResourceName(rb.getId());
                    rb_id = rb_id.substring(rb_id.indexOf("r"));

                    dialog.hide();

                    args = txt_scen1PID.getText()+","+String.valueOf(task1_total)+","+rb_id+","+String.valueOf(t1_recall_tot)+",";

                    fileSave(1,"\n"+args);
                    task2_dialog.show();


                }
            });

            final android.app.AlertDialog.Builder end1_dialog = new android.app.AlertDialog.Builder(Scenario1.this);
            end1_dialog.setIcon(android.R.drawable.btn_star);
            end1_dialog.setTitle("Scenario 1: Complete!");
            end1_dialog.setMessage("Congratulations, you've finished scenario 1! Now for a question...");
            end1_dialog.setPositiveButton("Ask me my question!",new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i) {


                      t1_recall_st = System.currentTimeMillis();
                      dialog.show();

                  }
              });



            end1_dialog.show();

        }else{

        }

    }

    private void fileSave(int task, String args){

        //write to a file, will try an duse fileWriter exclusively to open a file if it doesn't
        //exist and write to it if it does

        String folder = Environment.getExternalStorageDirectory().getAbsolutePath();
        String file = null;

        //create date to append to fileName
        now = Calendar.getInstance();
        Date time_now = now.getTime();
        String saveDate = df.format(time_now);

        //create file name
        String fileName = "scen1_twod_"+saveDate.substring(1,10)+".csv";
        file = folder+ File.separator+fileName;
        File file2 = new File(file);
        FileWriter fw;

        if(task == 1) {

            //if file exists append, otherwise create new file and write to the file
            if (file2.exists()) {

                try {
                    fw = new FileWriter(file, true);
                    fw.append(args);
                    fw.flush();
                    fw.close();

                } catch (IOException e) {

                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

            } else {

                try {
                    fw = new FileWriter(file);
                    fw.write(args);
                    fw.flush();
                    fw.close();


                } catch (IOException e) {

                    Log.i("error",e.toString());
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        }else{

            try {
                fw = new FileWriter(file, true);
                //fw.append(partID.getText().toString());
                fw.append(args);
                fw.flush();
                fw.close();

            } catch (IOException e) {

                Log.i("error",e.toString());
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

        }

    }

    public void start2_click(View view) {

        //local variables

        //float
        float tmpDist = 0;

        //set boolean to let onclikc listener to know to listen for events pertaining to the
        //second task
        task2 = true;

        for(String name:buildingList){
            //local Variabls
            Location buildLoc = new Location("gps");
            //doouble
            float dist = 0;

            //set location based on parameters
            buildLoc.setLatitude((Double.parseDouble(buildingLocs.get(name).get("lat").toString())));
            buildLoc.setLatitude((Double.parseDouble(buildingLocs.get(name).get("lon").toString())));

            //calculate distance from current user location to the building
            dist = curr_loc.distanceTo(buildLoc);

            //if building has a low level
            if(buildingLocs.get(name).get("alert") == "0") {
                if (tmpDist > dist) {
                    tmpDist = dist;
                    closest_build = name;
                } else if (tmpDist == 0) {
                    tmpDist = dist;
                    closest_build = name;
                } else {
                    closest_build = closest_build;
                }
            }else{
                //if not just keep closet build as is
                closest_build = closest_build;
            }
        }
        //TODO: Start timer immediately after java call
        //invoke java script to transform the closest building and add
        //indicator arrow

        BitmapDescriptor bm = null;
        Drawable mapIcon = getResources().getDrawable(R.drawable.green_circle);
        Bitmap mapMap = ((BitmapDrawable) mapIcon).getBitmap();
        mapMap = Bitmap.createScaledBitmap(mapMap,200,200,true);
        bm = BitmapDescriptorFactory.fromBitmap(mapMap);

        markerList.get(closest_build).setIcon(bm);

        txt_closeLw.setText(closest_build);

        task2_start = System.currentTimeMillis();


    }
}
