package com.cookiegames.smartcookie.settings.fragment

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.Preference
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.preference.UserPreferences
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * The general settings of the app.
 */
class ExtensionsSettingsFragment : AbstractSettingsFragment() {

    @Inject lateinit var userPreferences: UserPreferences

    private lateinit var proxyChoices: Array<String>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_extensions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        switchPreference(
                preference = DARK_MODE,
                isChecked = userPreferences.darkModeExtension,
                onCheckChange = { userPreferences.darkModeExtension = it; Toast.makeText(activity, R.string.please_restart, Toast.LENGTH_LONG).show()}
        )

        switchPreference(
                preference = "block_cookies",
                isChecked = userPreferences.cookieBlockEnabled,
                onCheckChange = { userPreferences.cookieBlockEnabled = it }
        )
         switchPreference(
                preference = TRANSLATE,
                isChecked = userPreferences.translateExtension,
                onCheckChange = { userPreferences.translateExtension = it }
        )
        switchPreference(
                preference = AMP,
                isChecked = userPreferences.noAmp,
                onCheckChange = { userPreferences.noAmp = it }
        )

    }




    companion object {
        private const val DARK_MODE = "dark_mode"
        private const val TRANSLATE = "translate"
        private const val AMP = "amp"
    }
}
