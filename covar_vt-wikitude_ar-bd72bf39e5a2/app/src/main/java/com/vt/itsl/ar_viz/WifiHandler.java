package com.vt.itsl.ar_viz;

import android.util.Log;

import com.wikitude.architect.ArchitectView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by bcmattina on 03-Nov-16.
 */

class WifiHandler {

    //use this function to accept an apList and generate a json Array from an ArrayList of data
    public JSONArray apIdentifier(ArrayList<String> apList){

        JSONArray apArray = new JSONArray();

        for(String ap : apList){

            HashMap<String, String> accPt = new HashMap<>();
            String[] apInfo = ap.split("[,]");

            //input parameters into hashmap
            accPt.put("id", apInfo[0]);
            accPt.put("latitude", apInfo[1]);
            accPt.put("longitude", apInfo[2]);
            accPt.put("altitude", apInfo[3]);

            JSONObject apJSON = new JSONObject(accPt);

            apArray.put(apJSON);

        }

        return apArray;

    }

    public void javaCall(ArchitectView architectView, JSONArray jsonArray){

        //final String arguments = apInfo.toString();

        /*

        final StringBuilder argumentsString = new StringBuilder("");
        for (int i= 0; i<arguments.length; i++) {
            argumentsString.append(arguments[i]);
            if (i<arguments.length-1) {
                argumentsString.append(", ");
            }

        }
        */
        //pass all information via the specific java function as set functionality not working
        String java_call = "World.loadPoisFromJsonData( "+ jsonArray.toString() + " );";
        architectView.callJavascript(java_call);

    }

}
