/*
 Copyright 2016 Vlad Todosin
*/
package com.cookiegames.smartcookie.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cookiegames.smartcookie.R;
import com.cookiegames.smartcookie.utils.ThemeUtils;

public class ReadingActivity extends AppCompatActivity {
    boolean inverted = true;
    TextView textTitle;
    TextView textBody;
    @Override
    public void onCreate(Bundle saved){
    ThemeUtils ut = new ThemeUtils(this);
    ut.setTheme();
        super.onCreate(saved);
    setContentView(R.layout.reading);
    Toolbar bar = (Toolbar)findViewById(R.id.toolbar);
    setSupportActionBar(bar);
    textTitle = (TextView) findViewById(R.id.textViewTitle);
    textTitle.setText(getIntent().getExtras().getString("title"));
    textBody = (TextView) findViewById(R.id.textViewBody);
    textBody.setText(getIntent().getExtras().getString("text"));
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        setTitle("");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reading, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
            return true;
        }
        switch(id){
            case R.id.action_invert:
            inverted = !inverted;
                ScrollView view = (ScrollView) findViewById(R.id.scroll) ;
                if(!inverted) {
                    view.setBackgroundColor(Color.BLACK);
                    textTitle.setTextColor(Color.WHITE);
                    textBody.setTextColor(Color.WHITE);
                }
                else{
                    view.setBackgroundColor(Color.WHITE);
                    textTitle.setTextColor(Color.BLACK);
                    textBody.setTextColor(Color.BLACK);
                }

             break;
        }

        return false;
    }}
