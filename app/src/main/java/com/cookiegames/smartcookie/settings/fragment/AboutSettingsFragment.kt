/*
 * Copyright 2014 A.C.R. Development
 */
package com.cookiegames.smartcookie.settings.fragment

import com.cookiegames.smartcookie.BuildConfig
import com.cookiegames.smartcookie.R
import android.os.Bundle

class AboutSettingsFragment : AbstractSettingsFragment() {

    override fun providePreferencesXmlResource() = R.xml.preference_about

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clickablePreference(
            preference = SETTINGS_VERSION,
            summary = BuildConfig.VERSION_NAME,
            onClick = { }
        )
    }

    companion object {
        private const val SETTINGS_VERSION = "pref_version"
    }
}
