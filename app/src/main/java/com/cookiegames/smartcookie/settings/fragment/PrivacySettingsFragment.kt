package com.cookiegames.smartcookie.settings.fragment

import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.database.history.HistoryRepository
import com.cookiegames.smartcookie.di.DatabaseScheduler
import com.cookiegames.smartcookie.di.MainScheduler
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.dialog.DialogItem
import com.cookiegames.smartcookie.extensions.snackbar
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.utils.WebUtils
import com.cookiegames.smartcookie.view.SmartCookieView
import android.os.Bundle
import android.webkit.WebView
import com.cookiegames.smartcookie.DeviceCapabilities
import com.cookiegames.smartcookie.isSupported
import io.reactivex.Completable
import io.reactivex.Scheduler
import javax.inject.Inject

class PrivacySettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var historyRepository: HistoryRepository
    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler

    override fun providePreferencesXmlResource() = R.xml.preference_privacy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)

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
                preference = SETTINGS_BLOCKMALWARE,
                isChecked = userPreferences.blockMalwareEnabled,
                onCheckChange = { userPreferences.blockMalwareEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_IDENTIFYINGHEADERS,
            isChecked = userPreferences.removeIdentifyingHeadersEnabled,
            summary = "${SmartCookieView.HEADER_REQUESTED_WITH}, ${SmartCookieView.HEADER_WAP_PROFILE}",
            onCheckChange = { userPreferences.removeIdentifyingHeadersEnabled = it }
        )

        switchPreference(
                preference = SETTINGS_INCOGNITO,
                isChecked = userPreferences.incognito,
                onCheckChange = { userPreferences.incognito = it }
        )

    }

    private fun clearHistoryDialog() {
        BrowserDialog.showPositiveNegativeDialog(
            activity = activity,
            title = R.string.title_clear_history,
            message = R.string.dialog_history,
            positiveButton = DialogItem(title = R.string.action_yes) {
                clearHistory()
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe {
                        activity.snackbar(R.string.message_clear_history)
                    }
            },
            negativeButton = DialogItem(title = R.string.action_no) {},
            onCancel = {}
        )
    }

    private fun clearCookiesDialog() {
        BrowserDialog.showPositiveNegativeDialog(
            activity = activity,
            title = R.string.title_clear_cookies,
            message = R.string.dialog_cookies,
            positiveButton = DialogItem(title = R.string.action_yes) {
                clearCookies()
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe {
                        activity.snackbar(R.string.message_cookies_cleared)
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
        activity.snackbar(R.string.message_cache_cleared)
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
        WebUtils.clearWebStorage()
        activity.snackbar(R.string.message_web_storage_cleared)
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
        private const val SETTINGS_BLOCKMALWARE = "block_malicious_sites"
        private const val SETTINGS_INCOGNITO = "start_incognito"

    }

}
