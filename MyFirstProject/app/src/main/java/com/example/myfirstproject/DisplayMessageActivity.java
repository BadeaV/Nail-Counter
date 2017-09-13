package com.example.myfirstproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DisplayMessageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);


        // Face apel la Intentul care a initializat activitatea si isi trage stringul

        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        // Apeleaza textView-ul din layout si ii seteaza textul sa fie stingul obtinut

        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(message);
    }
}
