package com.cookiegames.smartcookie.settings.fragment

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.PasswordChoice
import com.cookiegames.smartcookie.browser.SiteBlockChoice
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.dialog.BrowserDialog.setDialogSize
import com.cookiegames.smartcookie.extensions.withSingleChoiceItems
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.settings.activity.SettingsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import javax.inject.Inject

/**
 * The general settings of the app.
 */
class ParentalControlSettingsFragment : AbstractSettingsFragment() {

    @Inject lateinit var userPreferences: UserPreferences

    private lateinit var proxyChoices: Array<String>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_parents)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        proxyChoices = resources.getStringArray(R.array.blocked_sites)
        var fullName: String
        if(userPreferences.siteBlockChoice == SiteBlockChoice.BLACKLIST){
            fullName = getText(R.string.only_allow_sites).toString()
        }
        else if(userPreferences.siteBlockChoice == SiteBlockChoice.NONE){
            fullName = getText(R.string.none).toString()
        }
        else{
            fullName = getText(R.string.block_all_sites).toString()
        }

        clickableDynamicPreference(
                preference = SETTINGS_SITE_BLOCK,
                summary = fullName,
                onClick = ::showSiteBlockPicker
        )
        clickableDynamicPreference(
                preference = SETTINGS_PASSWORD,
                summary = userPreferences.passwordChoice.toSummary(),
                onClick = ::showPasswordPicker
        )
        val prefs: SharedPreferences? = activity?.getSharedPreferences("com.cookiegames.smartcookie", MODE_PRIVATE)

        if (prefs?.getBoolean("noPassword", true)!!) {
        }
        else {
            passwordDialog()
        }
    }

    private fun passwordDialog() {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.dialog_edit_text)

        editText.setHint(R.string.enter_password)

        val editorDialog = MaterialAlertDialogBuilder(requireContext())
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
                ) { _, _ ->
                    //listener.onClick(editText.getText().toString());
                    if (editText.text.toString() != userPreferences.passwordText){
                        val duration = Toast.LENGTH_SHORT
                        val toast = Toast.makeText(activity, resources.getString(R.string.wrong_password), duration)
                        toast.show()
                        passwordDialog()
                    }
                }

        val dialog = editorDialog.show()
        setDialogSize(requireContext(), dialog)
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
                updateSiteBlockChoice(it, activity as Activity, summaryUpdater)
            }
            setPositiveButton(R.string.action_ok, null)
        }
    }

    private fun updateSiteBlockChoice(choice: SiteBlockChoice, activity: Activity, summaryUpdater: SummaryUpdater) {
        if (choice == SiteBlockChoice.WHITELIST || choice == SiteBlockChoice.BLACKLIST) {
            showManualSiteBlockPicker(activity, summaryUpdater, choice)
        }

        userPreferences.siteBlockChoice = choice
        summaryUpdater.updateSummary(choice.toSummary())
    }

    private fun showManualSiteBlockPicker(activity: Activity, summaryUpdater: SummaryUpdater, choice: SiteBlockChoice) {
        val v = activity.layoutInflater.inflate(R.layout.site_block, null)
        val blockedSites = v.findViewById<TextView>(R.id.siteBlock)
        // Limit the number of characters since the port needs to be of type int
        // Use input filters to limit the EditText length and determine the max
        // length by using length of integer MAX_VALUE
        val maxCharacters = Integer.MAX_VALUE.toString().length

        blockedSites.text = userPreferences.siteBlockNames

        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.block_sites)
            setView(v)
            setPositiveButton(R.string.action_ok) { _, _ ->
                val proxyHost = blockedSites.text.toString()
                userPreferences.siteBlockNames = proxyHost
                if(choice.toString() == "BLACKLIST"){
                    summaryUpdater.updateSummary(getText(R.string.only_allow_sites).toString())
                }
                else{
                    summaryUpdater.updateSummary(getText(R.string.block_all_sites).toString())
                }

            }
        }
    }

    private fun PasswordChoice.toSummary(): String {
        val stringArray = resources.getStringArray(R.array.password_set_array)
        return when (this) {
            PasswordChoice.NONE -> stringArray[0]
            PasswordChoice.CUSTOM -> stringArray[1]
        }
    }

    private fun showPasswordPicker(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.enter_password)
            val stringArray = resources.getStringArray(R.array.password_set_array)
            val values = PasswordChoice.values().map {
                Pair(it, when (it) {
                    PasswordChoice.NONE -> stringArray[0]
                    PasswordChoice.CUSTOM -> resources.getString(R.string.enter_password)
                })
            }
            withSingleChoiceItems(values, userPreferences.passwordChoice) {
                updatePasswordChoice(it, activity as Activity, summaryUpdater)
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
