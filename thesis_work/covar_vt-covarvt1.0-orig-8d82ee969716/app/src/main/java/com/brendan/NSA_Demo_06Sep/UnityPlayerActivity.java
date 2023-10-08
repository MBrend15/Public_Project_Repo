package com.brendan.NSA_Demo_06Sep;

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

import android.app.Activity;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
	//variables for debugging and also to extract the name of the current trackable
	static final String TAG_2 = "And_debug";
	public String curr_trackable = " ";


	private static DateFormat df;
	public static String finalResponse;

	//public static final String buildingName;
	public static String buildingName = "";
	public static RequestQueue mRequestQueue;
	public static JSONArray array = null;

	private Calendar now = null;

	private HashMap<String, ArrayList<String>> inf_ip_list;

	Integer totHits;

	JsonObjectRequest jsObjectRequest;


	// Setup activity layout
	@Override protected void onCreate (Bundle savedInstanceState)
	{
		//all imported from Unity, leave intact
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

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

        RefreshQuery(findViewById(R.id.butRefresh));

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

	@Override
	public void Vuforia_onUpdate(State state) {

		//code to process number of trackable results currently registered to the player
		Log.i(TAG_2, "Number of trackables: "+Integer.toString(state.getNumTrackableResults()));
		final String testTrack;

		//if number of trackable results is greater than 1, aka something is being tracked, return
		//the name of the current trackable.
		if (state.getNumTrackableResults()>1) {
			testTrack = state.getTrackableResult(0).getTrackable().getName();
		}

		else{
			//else clear any alterations you may have made to the layout.
			testTrack = "";
		}

		//log current trackable as well as the most recent trackable (debugging)
		Log.i(TAG_2, "TestTrack: "+testTrack);
		Log.i(TAG_2, "CurrTrack: "+curr_trackable);
		Log.i(TAG_2,"returned: "+testTrack);

		//if the trackables don't match, then build list etc. Had to use the .equals operator because
		//of the object nature of Strings
		if (!curr_trackable.trim().equals(testTrack.trim())){
			Log.i(TAG_2,"run script");

			//had to direct this code to run on the UI (main) thread because you can only alter UI
			//on the main thread.
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					build2List(testTrack);
                    curr_trackable = testTrack;
					final TextView bv = (TextView) findViewById(R.id.build_text);
					bv.setText("Building: "+curr_trackable+"    ");
				}});
		}
		else{
			Log.i(TAG_2, "trackable same");
		}

		//this is query implementation
		//elasticQuery query_eng = new elasticQuery();
		//String query_results = query_eng.getBuilding("building name");

	}
	//create on click event to register the activty to the vuforia callback event. this passes
	//state update events to the event listener.
	public void Start_register(View view) {

		Vuforia.registerCallback(UnityPlayerActivity.this);
		Log.i(TAG_2,"button click");

	}

	public void onVolleyUpdate() {

		Log.d("Volley Success", array.toString());
	}

	//function that fills out a list view based on dummy IP address. Called when a new trackable
	//is discovered.
	public void build2List(final String track_name) {

        final List<String> ipAddr_Dur = new ArrayList<>();
        final List<String> ipAddr_Tor = new ArrayList<>();
        final View overlay = findViewById(R.id.fl_overlay);

        now = Calendar.getInstance();
        String endDate = df.format(now.getTime());
        //Set time range, how many minutes ago
        now.add(Calendar.MINUTE, -60);
        String startDate = df.format(now.getTime());
        String urlToRead;

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


                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        TextView tv_toHits = (TextView) findViewById(R.id.txt_totHits);
                        ListView lv = (ListView) findViewById(R.id.listView);

                        if (totHits1 != null) {

                            tv_toHits.setText("Total Attacks: " + Integer.toString(totHits1));
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

                runOnUiThread(runnable);

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

	private void ipListtoAdapter(ListView lv, HashMap<String, HashMap<String, Integer>> ip_and_sig_cnt){

		//create an intermediary list to combine ip address and signature count
		final ArrayList<String> address_and_signature_count = new ArrayList<>();
		final ArrayList<AdapterHelper> address_and_signature_count2 = new ArrayList<>();


		for(String key: ip_and_sig_cnt.keySet()){

			//integer to count attacks
			int totalAttacks = 0;

			for(String sig: ip_and_sig_cnt.get(key).keySet()) {

				//add total attacks from information in signatures.
				totalAttacks = totalAttacks + ip_and_sig_cnt.get(key).get(sig);

			}
			address_and_signature_count.add(key+","+totalAttacks);
            //so need to generate a list, believe it should be delimited and not a hashMap

            AdapterHelper adapterHelper = new AdapterHelper();
            adapterHelper.setSignature(key);
            adapterHelper.setCount(totalAttacks);

            address_and_signature_count2.add(adapterHelper);

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
                text2.setText("Attacks: "+splitStr[1]);//Integer.toString(address_and_signature_count2.get(position).getCount()));

                return view;
            }


        };


		lv.setAdapter(adapter);

        TextView tv = (TextView) findViewById(R.id.txtQuery);
        tv.setText("");

        Toast.makeText(this, "Query Returned!", Toast.LENGTH_LONG).show();

        Button butRef = (Button) findViewById(R.id.butRefresh);
        butRef.setEnabled(true);

	}

    public void RefreshQuery(View view) {

        View over_view = findViewById(R.id.fl_overlay);
        over_view.setBackgroundColor(Color.TRANSPARENT);

        TextView tv = (TextView) findViewById(R.id.txt_totHits);
        tv.setText("Total Hits: ");

        //by setting the list adapter to null you clear the list of all entries
        ListView lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(null);

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
}
