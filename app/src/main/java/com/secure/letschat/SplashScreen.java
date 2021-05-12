package com.secure.letschat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreen extends AppCompatActivity {
    public static boolean flag = false;

    @Override
    protected void onStart() {
        if(flag){
            SplashScreen.this.finish();
            System.exit(0);
            super.onStart();
        }
        else{
            super.onStart();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        getSupportActionBar().hide();
        final Handler handler = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                startActivity(intent);
            }
        };
        handler.postDelayed(r, 2500);
    }
}