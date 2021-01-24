package com.cookiegames.smartcookie.settings.fragment

import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.Preference
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.anthonycr.bonsai.Scheduler
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.database.javascript.JavaScriptDatabase
import com.cookiegames.smartcookie.database.javascript.JavaScriptRepository
import com.cookiegames.smartcookie.di.DatabaseScheduler
import com.cookiegames.smartcookie.di.MainScheduler
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.preference.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    @Inject internal lateinit var javascriptRepository: JavaScriptRepository

    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: io.reactivex.Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: io.reactivex.Scheduler


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

        clickablePreference(
                preference = SCRIPT_UNINSTALL,
                onClick = ::uninstallUserScript
        )

    }

    fun uninstallUserScript(){
        val builderSingle = MaterialAlertDialogBuilder(requireContext())
        builderSingle.setTitle(resources.getString(R.string.action_delete) + ":")
        val arrayAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.select_dialog_singlechoice)

        var jsList = emptyList<JavaScriptDatabase.JavaScriptEntry>()
        javascriptRepository.lastHundredVisitedJavaScriptEntries()
                .subscribe { list ->
                    jsList = list
                }

        for(i in jsList){
            arrayAdapter.add(i.name.replace("\\s".toRegex(), "").replace("\\n", ""))
        }


        builderSingle.setAdapter(arrayAdapter) { dialog: DialogInterface?, which: Int ->
            javascriptRepository.deleteJavaScriptEntry(jsList[which].name)
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe()
        }

        builderSingle.setPositiveButton(resources.getString(R.string.action_cancel)) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        builderSingle.show()
    }




    companion object {
        private const val DARK_MODE = "dark_mode"
        private const val TRANSLATE = "translate"
        private const val SCRIPT_UNINSTALL = "remove_userscript"
        private const val AMP = "amp"
    }
}
