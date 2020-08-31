/*
 * Copyright 2014 A.C.R. Development
 */
package com.cookiegames.smartcookie.settings.activity

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.settings.fragment.SettingsFragment


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        try {
            setSupportActionBar(toolbar)
            val actionBar: ActionBar? = getSupportActionBar()
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(false)
                actionBar.setDisplayHomeAsUpEnabled(true)
                actionBar.setDisplayShowHomeEnabled(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        toolbar.title = getString(R.string.settings)
        getSupportFragmentManager().beginTransaction().replace(R.id.container, SettingsFragment()).commit()
    }
}
