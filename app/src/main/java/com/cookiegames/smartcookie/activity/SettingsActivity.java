package com.cookiegames.smartcookie.activity;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.cookiegames.smartcookie.R;
import com.cookiegames.smartcookie.utils.PreferenceHelper;
import com.cookiegames.smartcookie.utils.ThemeUtils;

/**
 * Copyright (c) 2016 Vlad Todosin
 */
public class SettingsActivity extends AppCompatActivity{
    private MenuItem itm;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils theme = new ThemeUtils(this);
        theme.setTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference);
        Toolbar bar = (Toolbar)findViewById(R.id.settingsbar);
        setSupportActionBar(bar);
        setTitle(getResources().getString(R.string.action_settings));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(R.id.layout, new SettingsFragment()).commit();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return false;
    }
    @Override
    public void onBackPressed() {
        if(PreferenceHelper.getIsBrowser() || PreferenceHelper.getIsLook()){
            setTitle(getResources().getString(R.string.action_settings));
            getFragmentManager().beginTransaction().replace(R.id.layout, new SettingsFragment()).commit();
            PreferenceHelper.setIsBrowserScreen(false);
            PreferenceHelper.setIsLookScreen(false);
        }
        else{
            Intent in = new Intent(this,MainActivity.class);
            in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(in);
        }
    }
}
