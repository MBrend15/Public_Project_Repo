package com.covar.building_snort;
import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.unity3d.player.*;
import com.vuforia.State;
import com.vuforia.Vuforia;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static android.R.attr.key;
import static android.R.attr.track;

public class UnityPlayerActivity extends Activity implements Vuforia.UpdateCallbackInterface
{
	protected UnityPlayer mUnityPlayer; // don't change the name of this variable; referenced from native code

	//strings
	//variables for debugging and also to extract the name of the current trackable
	static final String TAG_2 = "And_debug";
	public String curr_trackable = " ";
	public static String finalResponse;
	public static String buildingName = "";
	String startButState = null;
	String buildNm = null;

	//string[]
	//permissions
	public static final String[] PERM = {Manifest.permission.WRITE_EXTERNAL_STORAGE};


	//date format
	private static DateFormat df;


	//public static final String buildingName;
	//response queue
	public static RequestQueue mRequestQueue;
	//jasonArray
	public static JSONArray array = null;
	//json Object request
	JsonObjectRequest jsObjectRequest;
	JsonObjectRequest jsonObjectRequest;

	RetryPolicy policy = null;

	//calendar
	private Calendar now = null;

	//hashMap
	private HashMap<String, ArrayList<String>> inf_ip_list;
	private HashMap<String, Integer> totalHits;

	//ints
	int totHits;
	public static final int PERM_CHK = 1;
	int taskCtr = 0;

	//Longs
	long startTime=0;
	long totalTime=0;

	//Views
	//textView
	EditText txt_PartID = null;
	Button but_Start = null;
	TextView txt_queryProg;
	ListView list_IP;

	//boolean
	Boolean paused = false;
	boolean torgRet = false;
	boolean mcBRet = false;
	boolean holdRet = false;
	boolean pattRet = false;

	//HashMap
	HashMap<String, HashMap<String,ArrayList<String>>> totalQuery = null;



	// Setup activity layout
	@Override protected void onCreate (Bundle savedInstanceState)
	{
		//all imported from Unity, leave intact
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		//instantiate shit
		df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

		Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024);
		Network network = new BasicNetwork(new HurlStack());

		mRequestQueue = new RequestQueue(cache, network);
		mRequestQueue.start();

		getWindow().setFormat(PixelFormat.RGBX_8888); // <--- This makes xperia play happy
		mUnityPlayer = new UnityPlayer(this);



		//I created a new layout to use. Able to condense unity player to a fram layout.
		setContentView(R.layout.main_layout);

		//instantiate the frame layout, then add unity player to it as a view.
		//set parameters to that of the view layout.
		FrameLayout fl = (FrameLayout) findViewById(R.id.frame_layout);
		ViewGroup.LayoutParams params = fl.getLayoutParams();
		fl.addView(mUnityPlayer.getView(),0,params);

		//line imported from Unity. leave it.
		mUnityPlayer.requestFocus();

		//txtView
		txt_queryProg = (TextView) findViewById(R.id.txt_queryProg);
		//listView
		list_IP = (ListView) findViewById(R.id.listView);

		//check permissions for indoor location may not need this...
		//ActivityCompat.requestPermissions(this, PERM, PERM_CHK);

		//policy
		policy = new DefaultRetryPolicy(50000,DefaultRetryPolicy.DEFAULT_MAX_RETRIES,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

		//hashMap
		totalHits = new HashMap<>();
		totalQuery = new HashMap<>();


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
					totalHits.put(buildName,totHits1);
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
							but_Start.setEnabled(true);
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



	// Quit Unity
	@Override protected void onDestroy ()
	{
		mUnityPlayer.quit();
		super.onDestroy();
	}

	// Pause Unity
	@Override protected void onPause()
	{
		super.onPause();
		mUnityPlayer.pause();

		View over_view = findViewById(R.id.fl_overlay);
		over_view.setBackgroundColor(Color.TRANSPARENT);

		//RefreshQuery(findViewById(R.id.butRefresh));

	}

	// Resume Unity
	@Override protected void onResume()
	{
		super.onResume();
		mUnityPlayer.resume();
	}

	// This ensures the layout will be correct.
	@Override public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		mUnityPlayer.configurationChanged(newConfig);
	}

	// Notify Unity of the focus change.
	@Override public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		mUnityPlayer.windowFocusChanged(hasFocus);
	}

	// For some reason the multiple keyevent type is not supported by the ndk.
	// Force event injection by overriding dispatchKeyEvent().
	@Override public boolean dispatchKeyEvent(KeyEvent event)
	{
		if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
			return mUnityPlayer.injectEvent(event);
		return super.dispatchKeyEvent(event);
	}

	// Pass any events not handled by (unfocused) views straight to UnityPlayer
	@Override public boolean onKeyUp(int keyCode, KeyEvent event)     { return mUnityPlayer.injectEvent(event); }
	@Override public boolean onKeyDown(int keyCode, KeyEvent event)   { return mUnityPlayer.injectEvent(event); }
	@Override public boolean onTouchEvent(MotionEvent event)          { return mUnityPlayer.injectEvent(event); }
	/*API12*/ public boolean onGenericMotionEvent(MotionEvent event)  { return mUnityPlayer.injectEvent(event); }

	//create on click event to register the activty to the vuforia callback event. this passes
	//state update events to the event listener. This will also start the testing process.
	public void Start_register(View view) {

		//instntiate views
		txt_PartID = (EditText) findViewById(R.id.txt_PartID);
		but_Start = (Button) findViewById(R.id.start_button);

		if(paused){
			onResume();
			paused = false;
		}
		else{

			Vuforia.registerCallback(UnityPlayerActivity.this);
			Log.i(TAG_2,"button click");

		}

		//set startTime
		startTime = System.currentTimeMillis();
		//what elements do you want to enable and disable?
		//TODO enable these controls upon "reporting" IPs

		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				txt_PartID.setEnabled(true);
				but_Start.setEnabled(false);

			}
		});


		//use modular arithmatic to ensure you know exactly where taskCtr is at
		if(taskCtr%5==0){

			but_Start.setText("Start-1");
			startButState = "Start-1";

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



		}
		else if(taskCtr%5==1){

			but_Start.setText("Start-2");
			startButState = "Start-2";

		}
		else if(taskCtr%5==2){

			but_Start.setText("Start-3");
			startButState = "Start-3";

		}
		else if(taskCtr%5==3){

			but_Start.setText("Start-4");
			startButState = "Start-4";

		}
		else{

			but_Start.setText("Start-Pre");
			startButState = "Start-Pre";

		}

		//increment taskCtr
		taskCtr++;

	}

	@Override
	public void Vuforia_onUpdate(State state) {

		final View overlay = findViewById(R.id.fl_overlay);



		//code to process number of trackable results currently registered to the player
		Log.i(TAG_2, "Number of trackables: "+Integer.toString(state.getNumTrackableResults()));
		final String testTrack;

		//if number of trackable results is greater than 1, aka something is being tracked, return
		//the name of the current trackable.
		if (state.getNumTrackableResults()>1) {
			String[] testTrack1 = state.getTrackableResult(0).getTrackable().getName().split("[_]");
			if (testTrack1[0].trim().equals("newman")){

				testTrack = "library";
				buildNm = testTrack;
				Log.i("Building Name","VuforiaUpdate "+buildNm);

			}
			else{
				testTrack = testTrack1[0].split("[-]")[0];
				buildNm = testTrack;
				Log.i("Building Name","VuforiaUpdate "+buildNm);
			}
		}

		else{
			//else clear any alterations you may have made to the layout.
			testTrack = "";
		}

		if(!testTrack.equals("")){

			runOnUiThread(new Runnable() {
				@Override
				public void run() {

					TextView tv = (TextView) findViewById(R.id.txtQuery);
					tv.setText("Querying!");
					Button butRef = (Button) findViewById(R.id.butRefresh);
					butRef.setEnabled(false);

				}
			});

		}

		//log current trackable as well as the most recent trackable (debugging)
		Log.i(TAG_2, "TestTrack: "+testTrack);
		Log.i(TAG_2, "CurrTrack: "+curr_trackable);
		Log.i(TAG_2,"returned: "+testTrack);
		Log.i("Building Name","VuforiaUpdate"+buildNm);

		//if the trackables don't match, then build list etc. Had to use the .equals operator because
		//of the object nature of Strings
		if (!curr_trackable.trim().equals(testTrack.trim())) {
            Log.i(TAG_2, "run script");

            //had to direct this code to run on the UI (main) thread because you can only alter UI
            //on the main thread.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //local Varaible
                    int totHits2 = 0;

                    switch (buildNm) {
                        case "torgersen":
                            ipListtoAdapter(list_IP, ListCompare(totalQuery.get("Torgersen Hall")));
                            totHits2 = totalHits.get("Torgersen Hall");
                            //torgRet = true;
                            break;
                        case "mcbryde":
                            ipListtoAdapter(list_IP, ListCompare(totalQuery.get("McBryde Hall")));
                            totHits2 = totalHits.get("McBryde Hall");
                            //mcBRet = true;
                            break;
                        case "holden":
                            ipListtoAdapter(list_IP, ListCompare(totalQuery.get("Holden Hall")));
                            totHits2 = totalHits.get("Holden Hall");
                            //holdRet = true;
                            break;
                        case "patton":
                            ipListtoAdapter(list_IP, ListCompare(totalQuery.get("Patton Hall")));
                            totHits2 = totalHits.get("Patton Hall");
                            //pattRet = true;
                            break;
                        default:
                            break;

                    }

                    TextView tv_toHits = (TextView) findViewById(R.id.txt_totHits);
                    //ListView lv = (ListView) findViewById(R.id.listView);

                    if (totHits2 == 0) {

                        tv_toHits.setText("Total Alerts: " + Integer.toString(totHits2));

                        if (totHits2 < 500) {

                            //was able to set a transparent ish background color by messing with the alpha
                            //channel, the first two characters of the hex number
                            overlay.setBackgroundColor(0x8060DC33);

                        } else if (totHits2 >= 500 && totHits2 < 1333) {

                            overlay.setBackgroundColor(0x80D6E95F);

                        } else {

                            overlay.setBackgroundColor(0x80FE5A5A);

                        }
                    }
                    curr_trackable=testTrack;
                    final TextView bv = (TextView) findViewById(R.id.build_text);
                    bv.setText("Building: "+curr_trackable.toUpperCase());
                }
            });

        }
		else{
			Log.i(TAG_2, "trackable same");
		}

		//this is query implementation
		//elasticQuery query_eng = new elasticQuery();
		//String query_results = query_eng.getBuilding("building name");

	}


	public void onVolleyUpdate() {

		Log.d("Volley Success", array.toString());
	}

	//function that fills out a list view based on dummy IP address. Called when a new trackable
	//is discovered.
	public void build2List(final String track_name) {

		Log.i("Building Name","build2List: "+buildNm);

		final List<String> ipAddr_Dur = new ArrayList<>();
		final List<String> ipAddr_Tor = new ArrayList<>();


		now = Calendar.getInstance();
		String endDate = df.format(now.getTime());
		//Set time range, how many minutes ago
		now.add(Calendar.MINUTE, -60);
		String startDate = df.format(now.getTime());
		String urlToRead;

		final View overlay = findViewById(R.id.fl_overlay);

		if(!track_name.equals("")){

			runOnUiThread(new Runnable() {
				@Override
				public void run() {

					TextView tv = (TextView) findViewById(R.id.txtQuery);
					tv.setText("Querying!");
					Button butRef = (Button) findViewById(R.id.butRefresh);
					butRef.setEnabled(false);

				}
			});

		}

		//this clause will have to be deleted, only for the test case
		if(track_name.equals("Torg")){

			//change name for building for hard code
			urlToRead = "http://10.10.200.151:9200/logstash-*/_" +
					"search?q=%2B@timestamp%3A[" + startDate + "+TO+" + endDate + "]+%2Bbuilding_in%3Atorgersen+%2Btags%3Asnort&sort=@timestamp:desc&size=6000";

		}else{

			//change name for building for hard code
			urlToRead = "http://10.10.200.151:9200/logstash-*/_" +
					"search?q=%2B@timestamp%3A[" + startDate + "+TO+" + endDate + "]+%2Bbuilding_in%3A" + track_name + "+%2Btags%3Asnort&sort=@timestamp:desc&size=6000";
		}

		Log.d("Query", "Query: " + urlToRead);

		JSONObject test = null;


		jsObjectRequest = new JsonObjectRequest(urlToRead, test, new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {

				Log.i(TAG_2, "response: " + response);
				ArrayList<String> ip = new ArrayList<String>();
				final HashMap<String, ArrayList<String>> inf_ip_list1 = new HashMap<String, ArrayList<String>>();
				final Integer totHits1;

				Log.i("Building Name","onRespons"+buildNm);

				try {
					JSONObject sourceObject = response.getJSONObject("hits");
					totHits1 = (Integer) sourceObject.get("total");

					JSONArray sourceArray = sourceObject.getJSONArray("hits");
					array = sourceArray;
					onVolleyUpdate();

					for (int i = 0; i < sourceArray.length(); i++) {

						JSONObject sourceObject2 = sourceArray.getJSONObject(i);
						sourceObject2 = sourceObject2.getJSONObject("_source");

						//create a HashMap of ip addresses that contain collections
						String attackInfo = sourceObject2.getString("sig_name");
						String ipDest = sourceObject2.getString("inet_ntoa(ip_dst)");

						if (inf_ip_list1.containsKey(ipDest)) {
							inf_ip_list1.get(ipDest).add(attackInfo);
						} else {

							inf_ip_list1.put(ipDest, new ArrayList<String>());
							inf_ip_list1.get(ipDest).add(attackInfo);

						}
					}


				} catch (JSONException ex) {
					throw new RuntimeException(ex);
				}


				Runnable runnable1 = new Runnable() {
					@Override
					public void run() {
						TextView tv_toHits = (TextView) findViewById(R.id.txt_totHits);
						ListView lv = (ListView) findViewById(R.id.listView);

						if (totHits1 != null) {

							tv_toHits.setText("Total Alerts: " + Integer.toString(totHits1));
							ipListtoAdapter(lv, ListCompare(inf_ip_list1));

							if(totHits1<500){

								//was able to set a transparent ish background color by messing with the alpha
								//channel, the first two characters of the hex number
								overlay.setBackgroundColor(0x8060DC33);

							}else if(totHits1>=500 && totHits1<1333){

								overlay.setBackgroundColor(0x80D6E95F);

							}else{

								overlay.setBackgroundColor(0x80FE5A5A);

							}
						}
					}
				};

				runOnUiThread(runnable1);

			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {

				Log.d("Response", "error: " + error.toString());

			}
		});

		int socketTimout = 50000;
		RetryPolicy policy = new DefaultRetryPolicy(socketTimout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
		jsObjectRequest.setRetryPolicy(policy);
		mRequestQueue.add(jsObjectRequest);
	}


	private HashMap<String, HashMap<String, Integer>> ListCompare(HashMap<String,ArrayList<String>> ipList){

		HashMap<String, HashMap<String, Integer>> ip_and_sig_cnt = new HashMap<>();
		//intermediary hashMap
		HashMap<String, Integer> ipaddr_and_sz = new HashMap<>();
		//create a new hashmap to store the top ten attacked ip address
		HashMap<String, Integer> sizeList10 = new HashMap<>();

		int sizecomp = 0;

		Log.i("Building Name","ListCompare: "+buildNm);

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
					//find the current minimum
					int minComp = Collections.min(sizeList10.values());
					//list to prevent multiple minimums
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

			//HashMap to count signatures
			HashMap<String,Integer> sig_count = new HashMap<>();

			//create a list of the signatures attached to an ipAddress
			ArrayList<String> stringList = ipList.get(key);

			//for each signature in the list
			for (int i = 0; i < stringList.size(); i++) {

				//get the signature
				String sig = stringList.get(i);

				//check to see if the signautre is in the signature counting hashMap
				if(sig_count.containsKey(sig)){

					//grab the count and increment by one
					int sigCount = sig_count.get(sig);
					int SC_updt = sigCount+1;
					sig_count.put(sig,SC_updt);

				}else{
					//just add a count of one for a new signature
					sig_count.put(sig,1);
				}
			}
			//add the signature hash map for the top ten ipAddresses
			ip_and_sig_cnt.put(key, sig_count);
		}
		return ip_and_sig_cnt;
	}

	private void ipListtoAdapter(final ListView lv, final HashMap<String, HashMap<String, Integer>> ip_and_sig_cnt){

		//create an intermediary list to combine ip address and signature count
		final ArrayList<String> address_and_signature_count = new ArrayList<>();
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

		Log.i("Building Name","ipList2Adap"+buildNm);


		for(String key: ip_and_sig_cnt.keySet()){

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

		final ArrayAdapter adapter = new ArrayAdapter(UnityPlayerActivity.this,android.R.layout.simple_list_item_2, android.R.id.text2, address_and_signature_count2){

			@Override
			public View getView(int position, View convertView, ViewGroup parent){

				View view = super.getView(position, convertView, parent);

				//create text views for the simpleListItem2 rows stacked vertical layout
				TextView text1 = (TextView) view.findViewById(android.R.id.text1); //new TextView(UnityPlayerActivity.this);
				TextView text2 = (TextView) view.findViewById(android.R.id.text2); // new TextView(UnityPlayerActivity.this);

				String delim = "[,]";
				String[] splitStr = address_and_signature_count.get(position).split(delim);

				text1.setText("Address: "+splitStr[0]);
				text2.setText("Alerts: "+splitStr[1]);//Integer.toString(address_and_signature_count2.get(position).getCount()));

				return view;
			}


		};


		lv.setAdapter(adapter);

		TextView tv = (TextView) findViewById(R.id.txtQuery);
		tv.setText("");

		Toast.makeText(this, "Query Returned!", Toast.LENGTH_LONG).show();

		Button butRef = (Button) findViewById(R.id.butRefresh);
		butRef.setEnabled(true);

		//set correctIPAddr to final to ensure tranfer to listener (i guess)
		final String finalCorrect_IPAddr = correct_IPAddr;
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

				Toast.makeText(UnityPlayerActivity.this, "Item Clicked! Position: "+Integer.toString(position), Toast.LENGTH_SHORT).show();
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
					ArrayAdapter adapter2 = new ArrayAdapter(UnityPlayerActivity.this, android.R.layout.select_dialog_item,signature);
					//Dialogue builder
					AlertDialog.Builder signatureDialoge = new AlertDialog.Builder(UnityPlayerActivity.this);

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
							//everything in Refresh besides admin stuff
							onPause();
							onPause();
							paused = true;

							Log.i("Building Name","onDialog"+buildNm);

							View over_view = findViewById(R.id.fl_overlay);
							over_view.setBackgroundColor(Color.TRANSPARENT);

							TextView tv = (TextView) findViewById(R.id.txt_totHits);
							tv.setText("Total Hits: ");
							TextView bv = (TextView) findViewById(R.id.build_text);
							bv.setText("Building: ");

							//by setting the list adapter to null you clear the list of all entries
							ListView lv = (ListView) findViewById(R.id.listView);
							lv.setAdapter(null);

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

							fileName = "CVT1_AR_"+time_nowStr.substring(1,10)+"_"+buildNm+".csv";

							FileWriterLocal(fileName,txt_PartID.getText()+","+Long.toString(totalTime));

						}
					});

					signatureDialoge.setAdapter(adapter2, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {

						}
					});

					signatureDialoge.show();

				}
				else{

					//Local Variables
					//ArrayList
					//wrong IP message
					ArrayList<String> wrongIp = new ArrayList<>();
					//Dialogue builder
					AlertDialog.Builder signatureDialoge = new AlertDialog.Builder(UnityPlayerActivity.this);
					//arrayadapter
					ArrayAdapter adapter2 = new ArrayAdapter(UnityPlayerActivity.this, android.R.layout.select_dialog_item,wrongIp);

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

	public void RefreshQuery(View view) {

		onPause();
		paused = true;

		View over_view = findViewById(R.id.fl_overlay);
		over_view.setBackgroundColor(Color.TRANSPARENT);

		TextView tv = (TextView) findViewById(R.id.txt_totHits);
		tv.setText("Total Hits: ");

		//by setting the list adapter to null you clear the list of all entries
		ListView lv = (ListView) findViewById(R.id.listView);
		lv.setAdapter(null);

		//if user selects admin reset, roll the counter back, then reset start button and
		//selected building
		Log.i("task ctr before: ",Integer.toString(taskCtr));
		taskCtr--;
		Log.i("task ctr after: ",Integer.toString(taskCtr));

		if(taskCtr%5==0){

			but_Start.setText("Start-Pre");
			but_Start.setEnabled(true);


		}
		else if(taskCtr%5==1){

			but_Start.setText("Start-1");
			but_Start.setEnabled(true);


		}
		else if(taskCtr%5==2){

			but_Start.setText("Start-2");
			but_Start.setEnabled(true);


		}
		else if(taskCtr%5==3){

			but_Start.setText("Start-3");
			but_Start.setEnabled(true);


		}
		else{

			but_Start.setText("Start-4");
			but_Start.setEnabled(true);


		}

		DialogMethod("Admin reset!","Admin reset! Counter: "+taskCtr,"Close");

	}

	private class AdapterHelper{

		String signature;
		Integer count;

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

	public void DialogMethod(String title, String message, String negButton){

		//Local Variables
		//ArrayList
		//wrong IP message
		ArrayList<String> wrongIp = new ArrayList<>();
		//Dialogue builder
		AlertDialog.Builder signatureDialoge = new AlertDialog.Builder(UnityPlayerActivity.this);
		//arrayadapter
		ArrayAdapter adapter2 = new ArrayAdapter(UnityPlayerActivity.this, android.R.layout.select_dialog_item,wrongIp);

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
}
