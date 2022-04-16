package com.cookiegames.smartcookie.settings.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.widget.ArrayAdapter
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.database.javascript.JavaScriptDatabase
import com.cookiegames.smartcookie.database.javascript.JavaScriptRepository
import com.cookiegames.smartcookie.di.DatabaseScheduler
import com.cookiegames.smartcookie.di.MainScheduler
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.preference.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        private const val SCRIPT_UNINSTALL = "remove_userscript"
    }
}
