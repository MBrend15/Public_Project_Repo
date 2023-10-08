package com.vt.itsl.ar_viz;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.wikitude.architect.ArchitectView;
import com.wikitude.architect.StartupConfiguration;

import java.io.IOException;

/**
 * Created by bcmattina on 12-Oct-16.
 */
public class SampCam extends Activity {

    ArchitectView architectView = null;
    ArchitectView.ArchitectUrlListener urlListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.samp2);

        //grab intent from home screen in case you are going to pass amplifying information
        Intent intent = getIntent();

        architectView = (ArchitectView) findViewById(R.id.architectView2);

        String key = "qUknPI0CZz2I8qNVd5jcHnjLppGWVMKMwgwv+B5qt1TKsmyCuRlm2OGCQXxsnHIfS8z2AaTMOeP+kgIqMBghOlHbHfGH366oVr//zL55wurGcvVeCxa8F5PxqCQvrWLhAJiq5vUuK+RZXGVyF7STjI4IUQlvPDpwXgtjLMmg7HVTYWx0ZWRfX2hwvU3ndQNL4ImAV+nPLhbLUqdADtfWK7p9ppZgqg9eZ9Pl7OV8wG2YQ6dX4CFOgwfPD3gxuA3C7yuFogMUnsGHyftqCoDCZmEu3FDsu8YYvYnjDroqHyQ0C8OlqcNuaePMS3k3x4elukJMksPO2wI8Pfp+lgCq9pAfSfeWIDJ74sHA8HVbZIuvtjm4s8HfIS7m3iR2o8J2fORppOIOG6NugUHzZr4ht7ixXXrdUwwW1F31J7MhmTzNUVm/92FGuzUlN3h+cllo8wU2aiOlAgxfhpTS9HbTdOQSESjD+D+9wD+/Jm++5XwBtI9GcPkKbdFfu1CdoSFWqmvBakJ8NZM0oxrXMXg7e2n5pIoY7xw73dGMT0uKRSo3wv4xYCjIU4feEcrZOpk6X53e9QKKI50+0hR2UfCCzezhja2AcPYZXs4D/2d5sba9KtPc/wjQyZRGJt4N0F5m+VNAXdrvfeUSJCg3IlQiwmbYLaUersVxddS66PGn326UM3o8SJxR0k7uIllOtusR";

        //create configuration hopefully with a camera feed and a feature seat for two d tracking
        final StartupConfiguration config = new StartupConfiguration(key, StartupConfiguration.Features.Tracking2D, StartupConfiguration.CameraPosition.DEFAULT);

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

        //if it had geo functionality, you would then throw a section in here with location
        //functionality
    }

    //in the sample, the architectView is urrounded by if not null blocks. for time
    //i'm not going to do that, but if there are issues in the future consider adding

    @Override


    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if(architectView != null){
            architectView.onPostCreate();
            try
            {
                architectView.load("1_Client$Recognition_1_Image$On$Target/index.html");
            }catch(IOException e){
                e.printStackTrace();
            }

        }
        else{
            Toast.makeText(getApplicationContext(),"something's wrong loading architect",Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        architectView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        architectView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        architectView.onDestroy();
    }

    public ArchitectView.ArchitectUrlListener getUrlListener() {
        return new ArchitectView.ArchitectUrlListener() {
            @Override
            //additional clause for interacting with options presented via url.
            public boolean urlWasInvoked(String s) {

                Uri invoked = Uri.parse(s);

                Toast.makeText(SampCam.this, invoked.getQueryParameter("id"), Toast.LENGTH_SHORT).show();
                return true;
            }
        };
    }

    public void changeImg(View view) {

        architectView.callJavascript("World.changeImage()");
        //Toast.makeText(SampCam.this, "test", Toast.LENGTH_LONG).show();

    }
}
