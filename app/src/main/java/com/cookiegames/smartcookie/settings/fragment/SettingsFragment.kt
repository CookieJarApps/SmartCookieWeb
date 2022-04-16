package com.cookiegames.smartcookie.settings.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.cookiegames.smartcookie.R


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_headers)
    }
}