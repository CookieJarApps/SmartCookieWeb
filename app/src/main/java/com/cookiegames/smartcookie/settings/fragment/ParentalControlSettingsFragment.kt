package com.cookiegames.smartcookie.settings.fragment

import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.ProxyChoice
import com.cookiegames.smartcookie.constant.SCHEME_BLANK
import com.cookiegames.smartcookie.constant.SCHEME_BOOKMARKS
import com.cookiegames.smartcookie.constant.SCHEME_HOMEPAGE
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.extensions.withSingleChoiceItems
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.search.SearchEngineProvider
import com.cookiegames.smartcookie.search.Suggestions
import com.cookiegames.smartcookie.search.engine.BaseSearchEngine
import com.cookiegames.smartcookie.search.engine.CustomSearch
import com.cookiegames.smartcookie.utils.FileUtils
import com.cookiegames.smartcookie.utils.ProxyUtils
import com.cookiegames.smartcookie.utils.ThemeUtils
import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.cookiegames.smartcookie.browser.PasswordChoice
import com.cookiegames.smartcookie.browser.SiteBlockChoice
import com.cookiegames.smartcookie.dialog.BrowserDialog.setDialogSize
import com.cookiegames.smartcookie.settings.activity.SettingsActivity
import javax.inject.Inject

/**
 * The general settings of the app.
 */
class ParentalControlSettingsFragment : AbstractSettingsFragment() {

    @Inject lateinit var userPreferences: UserPreferences

    private lateinit var proxyChoices: Array<String>

    override fun providePreferencesXmlResource() = R.xml.preference_parents

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        proxyChoices = resources.getStringArray(R.array.blocked_sites)

        clickableDynamicPreference(
                preference = SETTINGS_SITE_BLOCK,
                summary = userPreferences.siteBlockChoice.toSummary(),
                onClick = ::showSiteBlockPicker
        )
        clickableDynamicPreference(
                preference = SETTINGS_PASSWORD,
                summary = userPreferences.passwordChoice.toSummary(),
                onClick = ::showPasswordPicker
        )
        val prefs: SharedPreferences = activity.getSharedPreferences("com.cookiegames.smartcookie", MODE_PRIVATE)

        if (prefs.getBoolean("noPassword", true)) {
            Log.d("TAGGG", "nopassword")
        }
        else {
            Log.d("TAGGG", userPreferences.passwordText)
            passwordDialog()
        }
    }

    private fun passwordDialog() {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.dialog_edit_text)

        editText.setHint(R.string.enter_password)

        val editorDialog = AlertDialog.Builder(activity)
                .setTitle(R.string.enter_password)
                .setView(dialogView)
                .setCancelable(false)
                .setNegativeButton(R.string.action_back) { dialog, which ->
                    //listener.onClick(editText.getText().toString());
                    val settings = Intent(activity, SettingsActivity::class.java)
                    settings.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(settings)
                }
                .setPositiveButton(R.string.action_ok
                ) { dialog, which ->
                    //listener.onClick(editText.getText().toString());
                    if (editText.text.toString() != userPreferences.passwordText){
                        val duration = Toast.LENGTH_SHORT
                        val toast = Toast.makeText(activity, resources.getString(R.string.wrong_password), duration)
                        toast.show()
                        passwordDialog()
                    }
                }

        val dialog = editorDialog.show()
        setDialogSize(activity, dialog)
    }

    private fun SiteBlockChoice.toSummary(): String {
        val stringArray = resources.getStringArray(R.array.blocked_sites)
        return when (this) {
            SiteBlockChoice.NONE -> stringArray[0]
            SiteBlockChoice.WHITELIST -> userPreferences.siteBlockNames
            SiteBlockChoice.BLACKLIST -> userPreferences.siteBlockNames
        }
    }

    private fun showSiteBlockPicker(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.block_sites_title)
            val stringArray = resources.getStringArray(R.array.blocked_sites)
            val values = SiteBlockChoice.values().map {
                Pair(it, when (it) {
                    SiteBlockChoice.NONE -> stringArray[0]
                    SiteBlockChoice.WHITELIST -> stringArray[1]
                    SiteBlockChoice.BLACKLIST -> stringArray[2]
                })
            }
            withSingleChoiceItems(values, userPreferences.siteBlockChoice) {
                updateSiteBlockChoice(it, activity, summaryUpdater)
            }
            setPositiveButton(R.string.action_ok, null)
        }
    }

    private fun updateSiteBlockChoice(choice: SiteBlockChoice, activity: Activity, summaryUpdater: SummaryUpdater) {
        if (choice == SiteBlockChoice.WHITELIST || choice == SiteBlockChoice.BLACKLIST) {
            showManualSiteBlockPicker(activity, summaryUpdater)
        }

        userPreferences.siteBlockChoice = choice
        summaryUpdater.updateSummary(choice.toSummary())
    }

    private fun showManualSiteBlockPicker(activity: Activity, summaryUpdater: SummaryUpdater) {
        val v = activity.layoutInflater.inflate(R.layout.site_block, null)
        val eProxyHost = v.findViewById<TextView>(R.id.siteBlock)

        // Limit the number of characters since the port needs to be of type int
        // Use input filters to limit the EditText length and determine the max
        // length by using length of integer MAX_VALUE
        val maxCharacters = Integer.MAX_VALUE.toString().length

        eProxyHost.text = userPreferences.siteBlockNames

        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.block_sites_title)
            setView(v)
            setPositiveButton(R.string.action_ok) { _, _ ->
                val proxyHost = eProxyHost.text.toString()
                userPreferences.siteBlockNames = proxyHost
                summaryUpdater.updateSummary("$proxyHost")
            }
        }
    }

    private fun PasswordChoice.toSummary(): String {
        val stringArray = resources.getStringArray(R.array.password)
        return when (this) {
            PasswordChoice.NONE -> resources.getString(R.string.none)
            PasswordChoice.CUSTOM -> resources.getString(R.string.agent_custom)
        }
    }

    private fun showPasswordPicker(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.enter_password)
            val stringArray = resources.getStringArray(R.array.password)
            val values = PasswordChoice.values().map {
                Pair(it, when (it) {
                    PasswordChoice.NONE -> stringArray[0]
                    PasswordChoice.CUSTOM -> stringArray[1]
                })
            }
            withSingleChoiceItems(values, userPreferences.passwordChoice) {
                updatePasswordChoice(it, activity, summaryUpdater)
            }
            setPositiveButton(R.string.action_ok, null)
        }
    }

    private fun updatePasswordChoice(choice: PasswordChoice, activity: Activity, summaryUpdater: SummaryUpdater) {
        if (choice == PasswordChoice.CUSTOM) {
            showPasswordTextPicker(activity, summaryUpdater)

            val prefs: SharedPreferences = activity.getSharedPreferences("com.cookiegames.smartcookie", MODE_PRIVATE)

            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean("noPassword", false)
            editor.apply()
        }
        else{
            val prefs: SharedPreferences = activity.getSharedPreferences("com.cookiegames.smartcookie", MODE_PRIVATE)

            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean("noPassword", true)
            editor.apply()

            summaryUpdater.updateSummary(resources.getString(R.string.none))
        }

        userPreferences.passwordChoice = choice
        summaryUpdater.updateSummary(choice.toSummary())
    }

    private fun showPasswordTextPicker(activity: Activity, summaryUpdater: SummaryUpdater) {
        val v = activity.layoutInflater.inflate(R.layout.password, null)
        val passwordText = v.findViewById<TextView>(R.id.password)

        // Limit the number of characters since the port needs to be of type int
        // Use input filters to limit the EditText length and determine the max
        // length by using length of integer MAX_VALUE
        val maxCharacters = Integer.MAX_VALUE.toString().length

        passwordText.text = userPreferences.passwordText

        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.enter_password)
            setView(v)
            setPositiveButton(R.string.action_ok) { _, _ ->
            val passwordCode = passwordText.text.toString()
            userPreferences.passwordText = passwordCode
        }
    }
    }


    companion object {
        private const val SETTINGS_SITE_BLOCK = "siteblock"
        private const val SETTINGS_PASSWORD = "password"
    }
}
