package com.cookiegames.smartcookie.settings.fragment

import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.extensions.snackbar
import com.cookiegames.smartcookie.preference.DeveloperPreferences
import android.os.Bundle
import javax.inject.Inject

class DebugSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var developerPreferences: DeveloperPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_debug)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)

        togglePreference(
            preference = LEAK_CANARY,
            isChecked = developerPreferences.useLeakCanary,
            onCheckChange = { change ->
                activity?.snackbar(R.string.app_restart)
                developerPreferences.useLeakCanary = change
            }
        )
    }

    companion object {
        private const val LEAK_CANARY = "leak_canary_enabled"
    }
}
