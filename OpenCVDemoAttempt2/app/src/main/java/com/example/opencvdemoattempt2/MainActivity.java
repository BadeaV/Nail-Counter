package com.example.opencvdemoattempt2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    static {
        if(!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Cacat spurcat");
        } else {
            Log.d(TAG, "cacat facut haur");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
