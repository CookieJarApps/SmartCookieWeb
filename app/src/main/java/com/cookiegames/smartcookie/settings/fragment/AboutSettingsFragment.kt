/*
 * Copyright 2014 A.C.R. Development
 */
package com.cookiegames.smartcookie.settings.fragment

import com.cookiegames.smartcookie.BuildConfig
import com.cookiegames.smartcookie.R
import android.os.Bundle
import android.preference.Preference
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AboutSettingsFragment : AbstractSettingsFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clickablePreference(
            preference = SETTINGS_VERSION,
            summary = BuildConfig.VERSION_NAME,
            onClick = { }
        )

        var pref: androidx.preference.Preference? = findPreference(SETTINGS_VERSION)
        pref!!.setOnPreferenceClickListener {
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setTitle("SCW v" + BuildConfig.VERSION_NAME)
            builder.setMessage("What's new:\n- New settings page")


            builder.setPositiveButton(resources.getString(R.string.action_ok)){dialogInterface , which ->

            }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.setCancelable(false)
            alertDialog.show()
            true
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_about)
    }

    companion object {
        private const val SETTINGS_VERSION = "pref_version"
    }
}
