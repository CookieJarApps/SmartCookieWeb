package com.cookiegames.smartcookie.settings.fragment

import android.R.attr
import android.R.attr.fragment
import android.os.Bundle
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.replace
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.cookiegames.smartcookie.R


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_headers)
    }
}