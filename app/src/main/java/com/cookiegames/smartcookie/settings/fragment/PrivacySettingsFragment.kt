// Copyright 2020 CookieJarApps MPL
package com.cookiegames.smartcookie.settings.fragment

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.cookiegames.smartcookie.DeviceCapabilities
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.PasswordChoice
import com.cookiegames.smartcookie.database.history.HistoryRepository
import com.cookiegames.smartcookie.di.DatabaseScheduler
import com.cookiegames.smartcookie.di.MainScheduler
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.dialog.DialogItem
import com.cookiegames.smartcookie.extensions.withSingleChoiceItems
import com.cookiegames.smartcookie.isSupported
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.utils.WebUtils
import com.cookiegames.smartcookie.view.SmartCookieView
import io.reactivex.Completable
import io.reactivex.Scheduler
import java.io.File
import javax.inject.Inject

class PrivacySettingsFragment : AbstractSettingsFragment() {
    @Inject internal lateinit var historyRepository: HistoryRepository
    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler

    var toastMessage: Toast? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_privacy)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)

        val stringArrayPassword = resources.getStringArray(R.array.password_set_array)
        clickableDynamicPreference(
                preference = SETTINGS_APP_LOCK,
                summary = stringArrayPassword[userPreferences.passwordChoiceLock.value],
                onClick = ::showPasswordPicker
        )

        clickablePreference(preference = SETTINGS_CLEARCACHE, onClick = this::clearCache)
        clickablePreference(preference = SETTINGS_CLEARHISTORY, onClick = this::clearHistoryDialog)
        clickablePreference(preference = SETTINGS_CLEARCOOKIES, onClick = this::clearCookiesDialog)
        clickablePreference(preference = SETTINGS_CLEARWEBSTORAGE, onClick = this::clearWebStorage)

        switchPreference(
            preference = SETTINGS_LOCATION,
            isChecked = userPreferences.locationEnabled,
            onCheckChange = { userPreferences.locationEnabled = it }
        )

        switchPreference(
                preference = SETTINGS_ONLY_CLOSE,
                isChecked = userPreferences.onlyForceClose,
                onCheckChange = { userPreferences.onlyForceClose = it }
        )

        switchPreference(
            preference = SETTINGS_THIRDPCOOKIES,
            isChecked = userPreferences.blockThirdPartyCookiesEnabled,
            isEnabled = DeviceCapabilities.THIRD_PARTY_COOKIE_BLOCKING.isSupported,
            onCheckChange = { userPreferences.blockThirdPartyCookiesEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_SAVEPASSWORD,
            isChecked = userPreferences.savePasswordsEnabled,
            onCheckChange = { userPreferences.savePasswordsEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_CACHEEXIT,
            isChecked = userPreferences.clearCacheExit,
            onCheckChange = { userPreferences.clearCacheExit = it }
        )

        switchPreference(
            preference = SETTINGS_HISTORYEXIT,
            isChecked = userPreferences.clearHistoryExitEnabled,
            onCheckChange = { userPreferences.clearHistoryExitEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_COOKIEEXIT,
            isChecked = userPreferences.clearCookiesExitEnabled,
            onCheckChange = { userPreferences.clearCookiesExitEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_WEBSTORAGEEXIT,
            isChecked = userPreferences.clearWebStorageExitEnabled,
            onCheckChange = { userPreferences.clearWebStorageExitEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_DONOTTRACK,
            isChecked = userPreferences.doNotTrackEnabled,
            summary = (if(userPreferences.popupsEnabled) {
                resources.getString(R.string.crash_warning)
            }
             else{
                ""
            }).toString(),
            onCheckChange = { userPreferences.doNotTrackEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_WEBRTC,
            isChecked = userPreferences.webRtcEnabled && DeviceCapabilities.WEB_RTC.isSupported,
            isEnabled = DeviceCapabilities.WEB_RTC.isSupported,
            onCheckChange = { userPreferences.webRtcEnabled = it }
        )

        switchPreference(
                preference = SETTINGS_FORCEHTTPS,
                isChecked = userPreferences.forceHTTPSenabled,
                onCheckChange = { userPreferences.forceHTTPSenabled = it }
        )

        switchPreference(
                preference = SETTINGS_PREFERHTTPS,
                isChecked = userPreferences.preferHTTPSenabled,
                onCheckChange = { userPreferences.preferHTTPSenabled = it }
        )

        switchPreference(
            preference = SETTINGS_IDENTIFYINGHEADERS,
            isChecked = userPreferences.removeIdentifyingHeadersEnabled,
            summary = (if(userPreferences.popupsEnabled) {
                resources.getString(R.string.crash_warning)
            }
            else{
                "${SmartCookieView.HEADER_REQUESTED_WITH}, ${SmartCookieView.HEADER_WAP_PROFILE}"
            }).toString(),
            onCheckChange = { userPreferences.removeIdentifyingHeadersEnabled = it }
        )

        switchPreference(
                preference = SETTINGS_INCOGNITO,
                isChecked = userPreferences.incognito,
                onCheckChange = { userPreferences.incognito = it }
        )

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
            withSingleChoiceItems(values, userPreferences.passwordChoiceLock) {
                updatePasswordChoice(it, activity as Activity, summaryUpdater)
            }
            setPositiveButton(R.string.action_ok, null)
        }
    }

    private fun updatePasswordChoice(choice: PasswordChoice, activity: Activity, summaryUpdater: SummaryUpdater) {
        if (choice == PasswordChoice.CUSTOM) {
            showPasswordTextPicker(activity, summaryUpdater)

            val prefs: SharedPreferences = activity.getSharedPreferences("com.cookiegames.smartcookie", Context.MODE_PRIVATE)

            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean("noPassword", false)
            editor.apply()
        }
        else{
            val prefs: SharedPreferences = activity.getSharedPreferences("com.cookiegames.smartcookie", Context.MODE_PRIVATE)

            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean("noPassword", true)
            editor.apply()

            summaryUpdater.updateSummary(resources.getString(R.string.none))
        }

        userPreferences.passwordChoiceLock = choice
        summaryUpdater.updateSummary(choice.toSummary())
    }

    private fun showPasswordTextPicker(activity: Activity, summaryUpdater: SummaryUpdater) {
        val v = activity.layoutInflater.inflate(R.layout.password, null)
        val passwordText = v.findViewById<TextView>(R.id.password)

        // Limit the number of characters since the port needs to be of type int
        // Use input filters to limit the EditText length and determine the max
        // length by using length of integer MAX_VALUE
        val maxCharacters = Integer.MAX_VALUE.toString().length

        passwordText.text = userPreferences.passwordTextLock

        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.enter_password)
            setView(v)
            setPositiveButton(R.string.action_ok) { _, _ ->
                val passwordCode = passwordText.text.toString()
                userPreferences.passwordTextLock = passwordCode
            }
        }
    }

    private fun clearHistoryDialog() {
        BrowserDialog.showPositiveNegativeDialog(
            activity = activity as Activity,
            title = R.string.title_clear_history,
            message = R.string.dialog_history,
            positiveButton = DialogItem(title = R.string.action_yes) {
                clearHistory()
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe {toastMessage?.cancel()
                        toastMessage = Toast.makeText(activity, R.string.message_clear_history, Toast.LENGTH_LONG)
                        toastMessage!!.show()
                    }
            },
            negativeButton = DialogItem(title = R.string.action_no) {},
            onCancel = {}
        )
    }

    private fun clearCookiesDialog() {
        BrowserDialog.showPositiveNegativeDialog(
            activity = activity as Activity,
            title = R.string.title_clear_cookies,
            message = R.string.dialog_cookies,
            positiveButton = DialogItem(title = R.string.action_yes) {
                clearCookies()
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe {
                        toastMessage?.cancel()
                        toastMessage = Toast.makeText(activity, R.string.message_cookies_cleared, Toast.LENGTH_LONG)
                        toastMessage!!.show()
                    }
            },
            negativeButton = DialogItem(title = R.string.action_no) {},
            onCancel = {}
        )
    }

    private fun clearCache() {
        WebView(requireNotNull(activity)).apply {
            clearCache(true)
            destroy()
        }
        deleteCache(requireContext())
        toastMessage?.cancel()
        toastMessage = Toast.makeText(activity, R.string.message_cache_cleared, Toast.LENGTH_LONG)
        toastMessage!!.show()
    }

    fun deleteCache(context: Context) {
        try {
            val dir = context.cacheDir
            deleteDir(dir)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun deleteData(context: Context) {
        try {
            val dir = context.dataDir
            deleteDir(dir)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
            dir.delete()
        } else if (dir != null && dir.isFile) {
            dir.delete()
        } else {
            false
        }
    }

    private fun clearHistory(): Completable = Completable.fromAction {
        val activity = activity
        if (activity != null) {
            // TODO: 6/9/17 clearHistory is not synchronous
            WebUtils.clearHistory(activity, historyRepository, databaseScheduler)
        } else {
            throw RuntimeException("Activity was null in clearHistory")
        }
    }

    private fun clearCookies(): Completable = Completable.fromAction {
        val activity = activity
        if (activity != null) {
            WebUtils.clearCookies(activity)
        } else {
            throw RuntimeException("Activity was null in clearCookies")
        }
    }

    private fun clearWebStorage() {
        WebView(requireNotNull(activity)).apply {
            clearFormData()
            clearSslPreferences()
            destroy()
        }
        context?.let { WebUtils.eraseWebStorage(it) }

        toastMessage?.cancel()
        toastMessage = Toast.makeText(activity, R.string.message_web_storage_cleared, Toast.LENGTH_LONG)
        toastMessage!!.show()
    }

    companion object {
        private const val SETTINGS_LOCATION = "location"
        private const val SETTINGS_THIRDPCOOKIES = "third_party"
        private const val SETTINGS_SAVEPASSWORD = "password"
        private const val SETTINGS_CACHEEXIT = "clear_cache_exit"
        private const val SETTINGS_HISTORYEXIT = "clear_history_exit"
        private const val SETTINGS_COOKIEEXIT = "clear_cookies_exit"
        private const val SETTINGS_CLEARCACHE = "clear_cache"
        private const val SETTINGS_CLEARHISTORY = "clear_history"
        private const val SETTINGS_CLEARCOOKIES = "clear_cookies"
        private const val SETTINGS_CLEARWEBSTORAGE = "clear_webstorage"
        private const val SETTINGS_WEBSTORAGEEXIT = "clear_webstorage_exit"
        private const val SETTINGS_DONOTTRACK = "do_not_track"
        private const val SETTINGS_WEBRTC = "webrtc_support"
        private const val SETTINGS_IDENTIFYINGHEADERS = "remove_identifying_headers"
        private const val SETTINGS_FORCEHTTPS = "force_https"
        private const val SETTINGS_PREFERHTTPS = "prefer_https"
        private const val SETTINGS_INCOGNITO = "start_incognito"
        private const val SETTINGS_ONLY_CLOSE = "only_clear"
        private const val SETTINGS_APP_LOCK = "app_lock"
        private const val SETTINGS_CLEAR_ALL = "clear_all"
    }

}
