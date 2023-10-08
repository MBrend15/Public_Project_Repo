package com.vt.itsl.twod_connect;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class StartScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);


    }

    public void start_Scen1(View view) {

        Intent intent = new Intent(this, Scenario1.class);
        startActivity(intent);

    }

    public void start_Scen2(View view) {

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }
}
