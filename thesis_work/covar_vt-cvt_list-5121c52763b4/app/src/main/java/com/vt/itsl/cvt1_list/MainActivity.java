package com.vt.itsl.cvt1_list;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    //views
    ListView list_Buildings;
    ListView list_IP;
    Button but_Start;
    EditText txt_PartID;
    TextView txt_queryProg;

    //lists
    ArrayList<String> buildingList;

    //reader
    BufferedReader listReader = null;

    //longs
    long startTime=0;
    long totalTime=0;

    //ints
    int taskCtr=0;
    public static final int PERM_CHK = 1;

    //strings
    String targetBuilding = null;
    String startButState = null;

    //string[]
    //permissions
    public static final String[] PERM = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //Calendar
    Calendar now = null;
    //date format
    DateFormat df;
    //RequestQueue
    public static RequestQueue mRequestQueue;
    //JsonObjectRequest
    JsonObjectRequest jsonObjectRequest;
    //JsonObjectArray
    JSONArray array = null;
    //policy
    RetryPolicy policy = null;

    //HashMap
    HashMap<String, HashMap<String,ArrayList<String>>> totalQuery = null;

    //Booleans
    boolean torgRet = false;
    boolean mcBRet = false;
    boolean holdRet = false;
    boolean pattRet = false;

    //---------life cycle methods-----------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //TODO create a section in onCreate where you disable all elements which should be disabled - do this for all prototype apps
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //local variables
        //adapters
        ArrayAdapter<String> buildingListAdapter;
        Cache cache;
        Network network;

        //instantiate views
        list_Buildings = (ListView) findViewById(R.id.list_Building);
        list_IP = (ListView) findViewById(R.id.list_IP);
        but_Start = (Button) findViewById(R.id.but_Start);
        txt_PartID = (EditText) findViewById(R.id.txt_PartID);
        txt_queryProg = (TextView) findViewById(R.id.txt_queryProg);

        //instantiate collections
        totalQuery = new HashMap<>();
        totalQuery.put("torgersen",null);
        totalQuery.put("mcbryde",null);
        totalQuery.put("holden",null);
        totalQuery.put("patton",null);

        //set certain elements as false
        list_Buildings.setEnabled(false);
        list_IP.setEnabled(false);

        //instantiate lists
        buildingList = new ArrayList<>();

        //date format
        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        //cache
        cache = new DiskBasedCache(getCacheDir(), 1024 * 1024);
        //Network
        network = new BasicNetwork(new HurlStack());
        //RequestQueue
        mRequestQueue = new RequestQueue(cache, network);
        mRequestQueue.start();
        //policy
        policy = new DefaultRetryPolicy(50000,DefaultRetryPolicy.DEFAULT_MAX_RETRIES,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        //check permissions for indoor location
        ActivityCompat.requestPermissions(this, PERM, PERM_CHK);

        //read in building names from list in assets folder
        try {

            //first create an reader and identify the assets file
            listReader = new BufferedReader(new InputStreamReader(
                    getAssets().open("buildingList.csv")));
            String line;

            //while you are still able to read lines place lines in the building list
            while((line = listReader.readLine())!=null){

                buildingList.add(line);
            }

        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "cannot open file!", Toast.LENGTH_SHORT).show();
        }
        //once the unable to read, close the reader for future use
        finally{

            if (listReader != null) {

                try {
                    listReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "list reader doesn't exist!", Toast.LENGTH_SHORT).show();
                }

            }

        }

        //Add admin reset row to list view
        buildingList.add(0,"Admin");

        //only reason why i am instantiating down here is because I essentially have to make the entire
        //adapter right here
        buildingListAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, buildingList);

        //now associate adapter with lv
        list_Buildings.setAdapter(buildingListAdapter);

        //set up onclicklistener
        list_Buildings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                //local varaibles

                //strings
                String selectedBuilding = null;

                //basically you want to compare the value of the item you selected to the target building
                //which is set when the user presses start

                selectedBuilding = buildingList.get(position);

                if(!selectedBuilding.equals(targetBuilding)&& targetBuilding == null){

                    but_Start.setEnabled(true);

                }
                else if(selectedBuilding.equals(targetBuilding)){

                    //increment taskCtr
                    //taskCtr++;
                    Log.i("taskCtr: ",Integer.toString(taskCtr));

                    //build2List(targetBuilding);

                    switch(targetBuilding){
                        case "Torgersen Hall":
                            ipListtoAdapter(list_IP,ListCompare(totalQuery.get(targetBuilding)));
                            torgRet = true;
                            break;
                        case "McBryde Hall":
                            ipListtoAdapter(list_IP,ListCompare(totalQuery.get(targetBuilding)));
                            mcBRet = true;
                            break;
                        case "Holden Hall":
                            ipListtoAdapter(list_IP,ListCompare(totalQuery.get(targetBuilding)));
                            holdRet = true;
                            break;
                        case "Patton Hall":
                            ipListtoAdapter(list_IP,ListCompare(totalQuery.get(targetBuilding)));
                            pattRet = true;
                            break;
                        default:
                            break;

                    }



                    //list_Buildings.setEnabled(false);
                    but_Start.setText("Querying!");
                    //Toast.makeText(MainActivity.this, "Querying!", Toast.LENGTH_SHORT).show();

                }else if(selectedBuilding.equals("Admin")){
                    //if user selects admin reset, roll the counter back, then reset start button and
                    //selected building
                    Log.i("task ctr before: ",Integer.toString(taskCtr));
                    taskCtr--;
                    Log.i("task ctr after: ",Integer.toString(taskCtr));

                    if(taskCtr%5==0){

                        but_Start.setText("Start-Pre");
                        targetBuilding = "Carol M. Newman Library";
                        but_Start.setEnabled(true);
                        list_Buildings.setEnabled(false);

                    }
                    else if(taskCtr%5==1){

                        but_Start.setText("Start-1");
                        targetBuilding = "Torgersen Hall";
                        but_Start.setEnabled(true);
                        list_Buildings.setEnabled(false);

                    }
                    else if(taskCtr%5==2){

                        but_Start.setText("Start-2");
                        targetBuilding = "McBryde Hall";
                        but_Start.setEnabled(true);
                        list_Buildings.setEnabled(false);

                    }
                    else if(taskCtr%5==3){

                        but_Start.setText("Start-3");
                        targetBuilding = "Holden Hall";
                        but_Start.setEnabled(true);
                        list_Buildings.setEnabled(false);

                    }
                    else{

                        but_Start.setText("Start-4");
                        targetBuilding = "Patton Hall";
                        but_Start.setEnabled(true);
                        list_Buildings.setEnabled(false);

                    }

                    DialogMethod("Admin reset!","Admin reset! Counter: "+taskCtr,"Close");

                }else{

                    //Local Variables
                    //ArrayList
                    //wrong IP message
                    ArrayList<String> wrongIp = new ArrayList<>();
                    //Dialogue builder
                    AlertDialog.Builder signatureDialoge = new AlertDialog.Builder(MainActivity.this);
                    //arrayadapter
                    ArrayAdapter adapter2 = new ArrayAdapter(MainActivity.this, android.R.layout.select_dialog_item,wrongIp);

                    //set warning message
                    wrongIp.add("You've selected the wrong building!");

                    signatureDialoge.setIcon(android.R.drawable.ic_dialog_alert);
                    signatureDialoge.setTitle("Wrong Building!");
                    signatureDialoge.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();

                        }
                    });

                    signatureDialoge.setAdapter(adapter2, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });

                    signatureDialoge.show();

                }

            }
        });
    }

    //-------------------view methods-----------------------------------------------

    public void StartTask(View view) {

        //disable the start button, preventing it from being pressed again and screwing everything up
        //also disable user ID, not to be re-enabled until the taskCTR is divided by 0
        txt_PartID.setEnabled(false);
        but_Start.setEnabled(false);
        list_Buildings.setEnabled(true);
        //start the timer
        startTime = System.currentTimeMillis();
        //set text which ensures that the users click the right list item, based on a counter which
        //TODO establish a method by which task counter goes back one upon reset

        //use modular arithmatic to ensure you know exactly where taskCtr is at
        if(taskCtr%5==0){

            //local variable
            ArrayList<String> buildings = new ArrayList<>();
            buildings.add("Torgersen Hall");
            buildings.add("McBryde Hall");
            buildings.add("Holden Hall");
            buildings.add("Patton Hall");

            torgRet = false;
            mcBRet = false;
            holdRet = false;
            pattRet = false;

            for(String build : buildings) {
                newBuild2List(build);
            }

            but_Start.setText("Start-1");
            startButState = "Start-1";
            targetBuilding = null;

        }
        else if(taskCtr%5==1){

            but_Start.setText("Start-2");
            startButState = "Start-2";
            targetBuilding = "Torgersen Hall";

        }
        else if(taskCtr%5==2){

            but_Start.setText("Start-3");
            startButState = "Start-3";
            targetBuilding = "McBryde Hall";

        }
        else if(taskCtr%5==3){

            but_Start.setText("Start-4");
            startButState = "Start-4";
            targetBuilding = "Holden Hall";

        }
        else{

            but_Start.setText("Start-Pre");
            startButState = "Start-Pre";
            targetBuilding = "Patton Hall";

        }

        //increment taskCtr
        taskCtr++;


    }

    //-------------------query methods--------------------------------------------------

    public void onVolleyUpdate() {

        Log.d("Volley Success", array.toString());
    }

    public void newBuild2List(final String buildName){

        //local variables
        String endDate;
        String startDate;
        String urlToRead = null;
        JSONObject test = null;

        //instantiate
        //calendar
        now = Calendar.getInstance();
        //Strings
        endDate = df.format(now.getTime());
        now.add(Calendar.MINUTE,-60);
        startDate = df.format(now.getTime());

        txt_queryProg.setText("Querying!");



        switch(buildName){

            case "Carol M. Newman Library":
                urlToRead = "http://10.10.200.151:9200/logstash-*/_" +
                        "search?q=%2B@timestamp%3A[" + startDate + "+TO+" + endDate + "]+%2Bbuilding_in%3ALibrary+%2Btags%3Asnort&sort=@timestamp:desc&size=6000";
                break;
            case "Torgersen Hall":
                urlToRead = "http://10.10.200.151:9200/logstash-*/_" +
                        "search?q=%2B@timestamp%3A[" + startDate + "+TO+" + endDate + "]+%2Bbuilding_in%3Atorgersen+%2Btags%3Asnort&sort=@timestamp:desc&size=6000";
                break;
            case "McBryde Hall":
                urlToRead = "http://10.10.200.151:9200/logstash-*/_" +
                        "search?q=%2B@timestamp%3A[" + startDate + "+TO+" + endDate + "]+%2Bbuilding_in%3AMcBryde+%2Btags%3Asnort&sort=@timestamp:desc&size=6000";
                break;
            case "Holden Hall":
                urlToRead = "http://10.10.200.151:9200/logstash-*/_" +
                        "search?q=%2B@timestamp%3A[" + startDate + "+TO+" + endDate + "]+%2Bbuilding_in%3AHolden+%2Btags%3Asnort&sort=@timestamp:desc&size=6000";
                break;
            case "Patton Hall":
                urlToRead = "http://10.10.200.151:9200/logstash-*/_" +
                        "search?q=%2B@timestamp%3A[" + startDate + "+TO+" + endDate + "]+%2Bbuilding_in%3APatton+%2Btags%3Asnort&sort=@timestamp:desc&size=6000";
                break;
            default:
                Toast.makeText(this, "Invalid search criteria.", Toast.LENGTH_SHORT).show();
                break;

        }

        Log.d("Query", "Query: " + urlToRead);

        jsonObjectRequest = new JsonObjectRequest(urlToRead, test, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //local variables
                //lists
                ArrayList<String> ip = new ArrayList();
                //hashMaps
                final HashMap<String, ArrayList<String>> inf_ip_list1 = new HashMap<String, ArrayList<String>>();
                //Integers
                final Integer totHits1;
                //JsonObject
                JSONObject sourceObject = null;
                JSONObject sourceObject2 = null;
                //JsonArray
                JSONArray sourceArray = null;
                //Strings
                String attackInfo = null;
                String ipDest = null;

                try {

                    sourceObject = response.getJSONObject("hits");
                    totHits1 = (Integer) sourceObject.get("total");
                    sourceArray = sourceObject.getJSONArray("hits");
                    array = sourceArray;
                    onVolleyUpdate();

                    for (int i = 0; i < sourceArray.length(); i++) {

                        sourceObject2 = sourceArray.getJSONObject(i);
                        sourceObject2 = sourceObject2.getJSONObject("_source");

                        //create a HashMap of ip addresses that contain collections
                        attackInfo = sourceObject2.getString("sig_name");
                        ipDest = sourceObject2.getString("inet_ntoa(ip_dst)");

                        if (inf_ip_list1.containsKey(ipDest)) {
                            inf_ip_list1.get(ipDest).add(attackInfo);
                        } else {

                            inf_ip_list1.put(ipDest, new ArrayList<String>());
                            inf_ip_list1.get(ipDest).add(attackInfo);

                        }
                    }

                    totalQuery.put(buildName,inf_ip_list1);

                    switch(buildName){
                        case "Torgersen Hall":
                            torgRet = true;
                            break;
                        case "McBryde Hall":
                            mcBRet = true;
                            break;
                        case "Holden Hall":
                            holdRet = true;
                            break;
                        case "Patton Hall":
                            pattRet = true;
                            break;
                        default:
                            break;

                    }


                } catch (JSONException e) {
                    throw new RuntimeException(e);

                }

                if(!torgRet){
                    newBuild2List("Torgersen Hall");
                }else if(!mcBRet){

                    newBuild2List("McBryde Hall");

                }else if(!holdRet){

                    newBuild2List("Holden Hall");

                }else if(!pattRet){

                    newBuild2List("Patton Hall");

                }

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                        if(torgRet && mcBRet && holdRet && pattRet){

                            txt_queryProg.setText("Query returned!");
                        }

                    }
                };

                runOnUiThread(runnable);


                /*

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                        //run combination of methods to populate the IPList with the top ten alert-ridden
                        //ip Addresses
                        //TODO create a clause if totHits = 0
                        if(totHits1 == 0){

                        }else {
                            ipListtoAdapter(list_IP, ListCompare(inf_ip_list1));
                        }
                    }
                };

                runOnUiThread(runnable);
                */

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.d("Response", "error: " + error.toString());

            }
        });

        jsonObjectRequest.setRetryPolicy(policy);
        mRequestQueue.add(jsonObjectRequest);

    }

    void build2List(String buildName){

        //local variables
        String endDate;
        String startDate;
        String urlToRead = null;
        JSONObject test = null;

        //instantiate
        //calendar
        now = Calendar.getInstance();
        //Strings
        endDate = df.format(now.getTime());
        now.add(Calendar.MINUTE,-60);
        startDate = df.format(now.getTime());

        switch(buildName){

            case "Carol M. Newman Library":
                urlToRead = "http://10.10.200.151:9200/logstash-*/_" +
                        "search?q=%2B@timestamp%3A[" + startDate + "+TO+" + endDate + "]+%2Bbuilding_in%3ALibrary+%2Btags%3Asnort&sort=@timestamp:desc&size=6000";
                break;
            case "Torgersen Hall":
                urlToRead = "http://10.10.200.151:9200/logstash-*/_" +
                        "search?q=%2B@timestamp%3A[" + startDate + "+TO+" + endDate + "]+%2Bbuilding_in%3Atorgersen+%2Btags%3Asnort&sort=@timestamp:desc&size=6000";
                break;
            case "McBryde Hall":
                urlToRead = "http://10.10.200.151:9200/logstash-*/_" +
                        "search?q=%2B@timestamp%3A[" + startDate + "+TO+" + endDate + "]+%2Bbuilding_in%3AMcBryde+%2Btags%3Asnort&sort=@timestamp:desc&size=6000";
                break;
            case "Holden Hall":
                urlToRead = "http://10.10.200.151:9200/logstash-*/_" +
                        "search?q=%2B@timestamp%3A[" + startDate + "+TO+" + endDate + "]+%2Bbuilding_in%3AHolden+%2Btags%3Asnort&sort=@timestamp:desc&size=6000";
                break;
            case "Patton Hall":
                urlToRead = "http://10.10.200.151:9200/logstash-*/_" +
                        "search?q=%2B@timestamp%3A[" + startDate + "+TO+" + endDate + "]+%2Bbuilding_in%3APatton+%2Btags%3Asnort&sort=@timestamp:desc&size=6000";
                break;
            default:
                Toast.makeText(this, "Invalid search criteria.", Toast.LENGTH_SHORT).show();
                break;

        }

        Log.d("Query", "Query: " + urlToRead);

        jsonObjectRequest = new JsonObjectRequest(urlToRead, test, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //local variables
                //lists
                ArrayList<String> ip = new ArrayList();
                //hashMaps
                final HashMap<String, ArrayList<String>> inf_ip_list1 = new HashMap<String, ArrayList<String>>();
                //Integers
                final Integer totHits1;
                //JsonObject
                JSONObject sourceObject = null;
                JSONObject sourceObject2 = null;
                //JsonArray
                JSONArray sourceArray = null;
                //Strings
                String attackInfo = null;
                String ipDest = null;

                try {

                    sourceObject = response.getJSONObject("hits");
                    totHits1 = (Integer) sourceObject.get("total");
                    sourceArray = sourceObject.getJSONArray("hits");
                    array = sourceArray;
                    onVolleyUpdate();

                    for (int i = 0; i < sourceArray.length(); i++) {

                        sourceObject2 = sourceArray.getJSONObject(i);
                        sourceObject2 = sourceObject2.getJSONObject("_source");

                        //create a HashMap of ip addresses that contain collections
                        attackInfo = sourceObject2.getString("sig_name");
                        ipDest = sourceObject2.getString("inet_ntoa(ip_dst)");

                        if (inf_ip_list1.containsKey(ipDest)) {
                            inf_ip_list1.get(ipDest).add(attackInfo);
                        } else {

                            inf_ip_list1.put(ipDest, new ArrayList<String>());
                            inf_ip_list1.get(ipDest).add(attackInfo);

                        }
                    }


                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                        //run combination of methods to populate the IPList with the top ten alert-ridden
                        //ip Addresses
                        //TODO create a clause if totHits = 0
                        if(totHits1 == 0){

                        }else {
                            ipListtoAdapter(list_IP, ListCompare(inf_ip_list1));
                        }
                    }
                };

                runOnUiThread(runnable);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.d("Response", "error: " + error.toString());

            }
        });

        jsonObjectRequest.setRetryPolicy(policy);
        mRequestQueue.add(jsonObjectRequest);
    }
    //---------------------list population methods and classes-----------------------------------------
    private HashMap<String, HashMap<String, Integer>> ListCompare(HashMap<String,ArrayList<String>> ipList){

        //local variables
        //hashmaps
        HashMap<String, HashMap<String, Integer>> ip_and_sig_cnt = new HashMap<>();
        //intermediary hashMap
        HashMap<String, Integer> ipaddr_and_sz = new HashMap<>();
        //create a new hashmap to store the top ten attacked ip address
        HashMap<String, Integer> sizeList10 = new HashMap<>();

        //integers
        int sizecomp = 0;
        for(String ipAddr : ipList.keySet()) {
            //create initial hashMap to compare number of attacks on IP addresses
            ipaddr_and_sz.put(ipAddr, ipList.get(ipAddr).size());
        }
        //for all ip addresses in the trace
        for(String ip_Addr : ipaddr_and_sz.keySet()){

            //for ip addresses in the trace, compare to the mimum attack count required to get into the
            //top ten
            if(ipaddr_and_sz.get(ip_Addr)>sizecomp){
                //if storage hashmap is less then ten, simply store the counts
                if(sizeList10.size()<10){
                    sizeList10.put(ip_Addr,ipaddr_and_sz.get(ip_Addr));
                    sizecomp = 0;
                }
                //if the list is ten, sort by count, pop the minimum and add the new minimum
                else{
                    //local variables
                    //integer
                    //find the current minimum
                    int minComp = Collections.min(sizeList10.values());
                    //arrayList
                    ArrayList<String> mult_entry = new ArrayList<>();
                    //find the minimum in the set, and remove. use above list to prevent
                    //removing multiple entries
                    for(Map.Entry<String, Integer> entry:sizeList10.entrySet()){
                        if(entry.getValue()==minComp){
                            mult_entry.add(entry.getKey());
                        }
                    }
                    //remove item from hashMap based on IP that returned minimum count
                    sizeList10.remove(mult_entry.get(0));
                    //add new ip address and event count
                    sizeList10.put(ip_Addr,ipaddr_and_sz.get(ip_Addr));
                    //create new sizecomp from new minimum on the list
                    sizecomp = Collections.min(sizeList10.values());

                }
            }
        }
        for(String key:sizeList10.keySet()){
            //local variables
            //hashmap
            //HashMap to count signatures
            HashMap<String,Integer> sig_count = new HashMap<>();
            //arraylist
            //create a list of the signatures attached to an ipAddress
            ArrayList<String> stringList = ipList.get(key);
            //for each signature in the list
            for (int i = 0; i < stringList.size(); i++) {
                //local variables
                //Strings
                //get the signature
                String sig = stringList.get(i);
                //check to see if the signautre is in the signature counting hashMap
                if (sig_count.containsKey(sig)) {
                    //local variables
                    //integers
                    //grab the count and increment by one
                    int sigCount = sig_count.get(sig);
                    int SC_updt = sigCount + 1;

                    sig_count.put(sig, SC_updt);

                } else {
                    //just add a count of one for a new signature
                    sig_count.put(sig, 1);
                }
            }
            //add the signature hash map for the top ten ipAddresses
            ip_and_sig_cnt.put(key, sig_count);
        }
        return ip_and_sig_cnt;
    }
    private void ipListtoAdapter(ListView lv, final HashMap<String, HashMap<String, Integer>> ip_and_sig_cnt){

        //local variables
        //arraylists
        //create an intermediary list to combine ip address and signature count
        final ArrayList<String> address_and_signature_count = new ArrayList<>();
        final ArrayList<String> sample = new ArrayList<>();
        final ArrayList<AdapterHelper> address_and_signature_count2 = new ArrayList<>();
        //hashmap
        //create an intermediary hashMap to order signatures and also to compare scrambled and unscrambled IPs
        HashMap<String, Integer> reorderIPAddr = new HashMap<>();
        HashMap<String, Integer> orderIPAddr;
        final HashMap<String,String> hashKey = new HashMap<>();


        //strings
        String correct_IPAddr = null;
        //new address
        String newIp = null;

        //counter to grab the "first" IP address as the correct IP address
        int ctr=0;

        for(String key: ip_and_sig_cnt.keySet()){

            //local variables
            //int
            //integer to count attacks
            int totalAttacks = 0;

            for(String sig: ip_and_sig_cnt.get(key).keySet()) {

                //add total attacks from information in signatures.
                totalAttacks = totalAttacks + ip_and_sig_cnt.get(key).get(sig);

            }

            reorderIPAddr.put(key,totalAttacks);
        }

        orderIPAddr = sortByComparator(reorderIPAddr,"des");

        for(String key: orderIPAddr.keySet()){

            //local variables
            //integer
            Integer totalAttacks = orderIPAddr.get(key);
            //adapterHelper
            AdapterHelper adapterHelper = new AdapterHelper();

            if (ctr == 0) {
                correct_IPAddr = key;
            }


            //use IP Scrambler to scramble the addresses and hashMap to save a key comparing old and new IP addr
            newIp = IPScrambler(key);
            address_and_signature_count.add(newIp+","+totalAttacks);
            hashKey.put(newIp,key);
            //so need to generate a list, believe it should be delimited and not a hashMap
            adapterHelper.setSignature(key);
            adapterHelper.setCount(totalAttacks);

            address_and_signature_count2.add(adapterHelper);

            Log.i("ListSorting: ",Integer.toString(orderIPAddr.get(key)));

            ctr++;

        }


        final ArrayAdapter adapter = new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_2, android.R.id.text2, address_and_signature_count2){

            @Override
            public View getView(int position, View convertView, ViewGroup parent){

                //local varaibles
                //view
                View view = super.getView(position, convertView, parent);
                //create text views for the simpleListItem2 rows stacked vertical layout
                TextView text1 = (TextView) view.findViewById(android.R.id.text1); //new TextView(UnityPlayerActivity.this);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2); // new TextView(UnityPlayerActivity.this);
                //strings
                String delim = "[,]";
                String[] splitStr = address_and_signature_count.get(position).split(delim);

                text1.setText("Address: "+splitStr[0]);
                text2.setText("Alerts: "+splitStr[1]);//Integer.toString(address_and_signature_count2.get(position).getCount()));

                return view;
            }
        };
        list_IP.setAdapter(adapter);
        list_IP.setEnabled(true);
        //TODO setEnabled false upon task completion
        //Toast.makeText(MainActivity.this, "Query Returned!", Toast.LENGTH_LONG).show();
        but_Start.setText("Query Returned!");

        //set correctIPAddr to final to ensure tranfer to listener (i guess)
        final String finalCorrect_IPAddr = correct_IPAddr;
        list_IP.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                //address to compare to user selection and get count total, use hashMap to pull old address from new address
                //String
                String selected_IPAddr = hashKey.get(address_and_signature_count.get(position).split("[,]")[0]);

                //if user selects the first IPAddress
                if(selected_IPAddr.equals(finalCorrect_IPAddr)){

                    //local variables
                    //hashmap
                    //get hashmap from address at index of item clicked
                    HashMap<String,Integer> sigCnt = ip_and_sig_cnt.get(selected_IPAddr);
                    //ArrayList
                    ArrayList<String> signature = new ArrayList<String>();
                    //arrayadapter
                    ArrayAdapter adapter2 = new ArrayAdapter(MainActivity.this, android.R.layout.select_dialog_item,signature);
                    //Dialogue builder
                    AlertDialog.Builder signatureDialoge = new AlertDialog.Builder(MainActivity.this);

                    for(String sig : sigCnt.keySet()){

                        //for all sigs in the count populate an array list, comma delim'ed, with signature and count
                        signature.add("Signature: "+sig+" Count:"+Integer.toString(sigCnt.get(sig)));

                    }
                    signatureDialoge.setIcon(android.R.drawable.ic_dialog_alert);
                    signatureDialoge.setTitle("Alert List");
                    signatureDialoge.setNegativeButton("Report to Operations Center!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            //Variables
                            //String
                            String time_nowStr = df.format(now.getTime());
                            String fileName = null;

                            dialogInterface.dismiss();
                            list_IP.setAdapter(null);
                            but_Start.setEnabled(true);
                            but_Start.setText(startButState);

                            //open up participantID if taskCTR % = 4
                            if(taskCtr%5==0){
                                txt_PartID.setEnabled(true);
                            }

                            //list_Buildings.setEnabled(true);
                            Log.i("StartTime", String.valueOf(startTime));
                            Log.i("EndTime", String.valueOf(System.currentTimeMillis()));


                            totalTime = System.currentTimeMillis() - startTime;
                            Log.i("TotalTime",String.valueOf(totalTime));

                            fileName = "CVT1_list_"+time_nowStr.substring(1,10)+"_"+targetBuilding+".csv";

                            FileWriterLocal(fileName,txt_PartID.getText()+","+Long.toString(totalTime));

                        }
                    });

                    signatureDialoge.setAdapter(adapter2, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });

                    signatureDialoge.show();

                }else{

                    //Local Variables
                    //ArrayList
                    //wrong IP message
                    ArrayList<String> wrongIp = new ArrayList<>();
                    //Dialogue builder
                    AlertDialog.Builder signatureDialoge = new AlertDialog.Builder(MainActivity.this);
                    //arrayadapter
                    ArrayAdapter adapter2 = new ArrayAdapter(MainActivity.this, android.R.layout.select_dialog_item,wrongIp);

                    //set warning message
                    wrongIp.add("You've selected the wrong IP address!");

                    signatureDialoge.setIcon(android.R.drawable.ic_dialog_alert);
                    signatureDialoge.setTitle("Wrong IP!");
                    signatureDialoge.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();

                        }
                    });

                    signatureDialoge.setAdapter(adapter2, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });

                    signatureDialoge.show();

                    }


            }
        });

    }
    private class AdapterHelper{

        //fields
        String signature;
        Integer count;

        //methods
        public String getSignature(){

            return signature;

        }
        public void setSignature(String signature){

            this.signature = signature;

        }
        public Integer getCount(){

            return count;

        }
        public void setCount(Integer count){

            this.count = count;

        }

    }

    //method to sort HashMap
    public static HashMap<String,Integer> sortByComparator(HashMap<String, Integer> unsortMap, String ascORdesc){
        //local Variables
        //boolean
        Boolean order = true;
        //hashMap
        HashMap<String,Integer> sortedMap = new LinkedHashMap<>();

        if(ascORdesc == "asc") {
            order = true;
        }
        else {

            order = false;
        }

        List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());

        //sorting list based on values
        final Boolean finalOrder = order;
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {

                if (finalOrder) {

                    return o1.getValue().compareTo(o2.getValue());

                } else {

                    return o2.getValue().compareTo(o1.getValue());
                }

            }

        });

        //maintain order with help of linkedList
        for(Map.Entry<String,Integer> entry:list){

            sortedMap.put(entry.getKey(),entry.getValue());

        }

        return sortedMap;

    }

    public String IPScrambler(String ipAddr){

        //local variables
        //Random - to generate a random number to hash all elements of the IP address
        Random randomGen = new Random();
        //ints to add before modular division
        int randInt = randomGen.nextInt(255);
        //quartet - will have to divide the IP address before using it obvy
        int quartet = 0;
        //list - to store quartets and store new IPaddr
        ArrayList<String> ip = new ArrayList<>();
        ArrayList<String> new_ip = new ArrayList<>();
        //string
        String returnAddr = null;

        //String selected_IPAddr = address_and_signature_count.get(position).split("[,]")[0];

        ip.add(ipAddr.split("[.]")[0]);
        ip.add(ipAddr.split("[.]")[1]);
        ip.add(ipAddr.split("[.]")[2]);
        ip.add(ipAddr.split("[.]")[3]);

        for(String quart : ip){

            //extract quartet then "hash" it
            quartet = Integer.parseInt(quart);
            quartet = (quartet+randInt)%255;

            new_ip.add(Integer.toString(quartet));

        }

        return new_ip.get(0)+"."+new_ip.get(1)+"."+new_ip.get(2)+"."+new_ip.get(3);

    }

    public void DialogMethod(String title, String message, String negButton){

        //Local Variables
        //ArrayList
        //wrong IP message
        ArrayList<String> wrongIp = new ArrayList<>();
        //Dialogue builder
        AlertDialog.Builder signatureDialoge = new AlertDialog.Builder(MainActivity.this);
        //arrayadapter
        ArrayAdapter adapter2 = new ArrayAdapter(MainActivity.this, android.R.layout.select_dialog_item,wrongIp);

        //set warning message
        wrongIp.add(message);

        signatureDialoge.setIcon(android.R.drawable.ic_dialog_alert);
        signatureDialoge.setTitle(title);
        signatureDialoge.setNegativeButton(negButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

            }
        });

        signatureDialoge.setAdapter(adapter2, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        signatureDialoge.show();

    }

    public void FileWriterLocal(String fileStr, String writeLine){

        //local variables
        //get external storage path
        String folder = Environment.getExternalStorageDirectory().getAbsolutePath();
        //file
        File file = null;
        //String
        String fullFileName = null;
        //FileWriter
        FileWriter fw = null;

        //instantiate
        fullFileName = folder+File.separator+fileStr;
        file = new File(fullFileName);

        if(file.exists()){

            try {
                fw = new FileWriter(fullFileName,true);
                fw.append(writeLine+"\n");
                fw.flush();
                fw.close();
            } catch (IOException e) {
                Log.i("FileError",e.getMessage());
            }

        }else{

            try {
                fw = new FileWriter(fullFileName);
                fw.write(writeLine+"\n");
                fw.flush();
                fw.close();
            } catch (IOException e) {
                Log.i("FileError",e.getMessage());
            }

        }

    }



}
