/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 7/11/2020 */

package com.cookiegames.smartcookie.settings.fragment

import android.os.Bundle
import android.webkit.URLUtil
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.HomepageTypeChoice
import com.cookiegames.smartcookie.constant.SCHEME_BLANK
import com.cookiegames.smartcookie.constant.SCHEME_BOOKMARKS
import com.cookiegames.smartcookie.constant.SCHEME_HOMEPAGE
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.extensions.withSingleChoiceItems
import com.cookiegames.smartcookie.preference.UserPreferences
import javax.inject.Inject


class HomepageSettingsFragment : AbstractSettingsFragment() {

    private lateinit var themeOptions: Array<String>

    @Inject internal lateinit var userPreferences: UserPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_homepage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        clickablePreference(
                preference = SETTINGS_IMAGE_URL,
                onClick = ::showImageUrlPicker
        )
        clickableDynamicPreference(
                preference = SETTINGS_HOME,
                summary = homePageUrlToDisplayTitle(userPreferences.homepage),
                onClick = ::showHomePageDialog
        )
        clickableDynamicPreference(
                preference = SETTINGS_HOMEPAGE_TYPE,
                isEnabled = userPreferences.homepage == SCHEME_HOMEPAGE,
                summary = homePageTypeToDisplayTitle(userPreferences.homepageType),
                onClick = ::showHomepageTypePicker
        )
        switchPreference(
                preference = SETTINGS_SHORTCUTS,
                isChecked = userPreferences.showShortcuts,
                onCheckChange = { userPreferences.showShortcuts = it }
        )
    }


    private fun showHomepageTypePicker(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.homepage_type)
            val stringArray = resources.getStringArray(R.array.homepage_type)
            val values = HomepageTypeChoice.values().map {
                Pair(it, when (it) {
                    HomepageTypeChoice.DEFAULT -> stringArray[0]
                    HomepageTypeChoice.FOCUSED -> stringArray[1]
                    HomepageTypeChoice.INFORMATIVE -> stringArray[2]
                })
            }
            withSingleChoiceItems(values, userPreferences.homepageType) {
                userPreferences.homepageType = it
                summaryUpdater.updateSummary(homePageTypeToDisplayTitle(it))
            }
            setPositiveButton(R.string.action_ok, null)
        }
    }

    private fun homePageTypeToDisplayTitle(choice: HomepageTypeChoice): String = when (choice) {
        HomepageTypeChoice.DEFAULT -> resources.getString(R.string.agent_default)
        HomepageTypeChoice.FOCUSED -> resources.getString(R.string.focused)
        HomepageTypeChoice.INFORMATIVE -> resources.getString(R.string.informational)
        else -> choice.toString()
    }

    private fun homePageUrlToDisplayTitle(url: String): String = when (url) {
        SCHEME_HOMEPAGE -> resources.getString(R.string.action_homepage)
        SCHEME_BLANK -> resources.getString(R.string.action_blank)
        SCHEME_BOOKMARKS -> resources.getString(R.string.action_bookmarks)
        else -> url
    }

    private fun showHomePageDialog(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.home)
            val n = when (userPreferences.homepage) {
                SCHEME_HOMEPAGE -> 0
                SCHEME_BLANK -> 1
                SCHEME_BOOKMARKS -> 2
                else -> 3
            }

            setSingleChoiceItems(R.array.homepage, n) { _, which ->
                when (which) {
                    0 -> {
                        userPreferences.homepage = SCHEME_HOMEPAGE
                        summaryUpdater.updateSummary(resources.getString(R.string.action_homepage))
                    }
                    1 -> {
                        userPreferences.homepage = SCHEME_BLANK
                        summaryUpdater.updateSummary(resources.getString(R.string.action_blank))
                    }
                    2 -> {
                        userPreferences.homepage = SCHEME_BOOKMARKS
                        summaryUpdater.updateSummary(resources.getString(R.string.action_bookmarks))
                    }
                    3 -> {
                        showCustomHomePagePicker(summaryUpdater)
                    }
                }
            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }
    }

    private fun showCustomHomePagePicker(summaryUpdater: SummaryUpdater) {
        val currentHomepage: String = if (!URLUtil.isAboutUrl(userPreferences.homepage)) {
            userPreferences.homepage
        } else {
            "https://www.google.com"
        }

        activity?.let {
            BrowserDialog.showEditText(it,
                    R.string.title_custom_homepage,
                    R.string.title_custom_homepage,
                    currentHomepage,
                    R.string.action_ok) { url ->
                if(url.startsWith("http") || url.startsWith("file")){
                    userPreferences.homepage = url
                    summaryUpdater.updateSummary(url)
                }
                else{
                    userPreferences.homepage = "https://" + url
                    summaryUpdater.updateSummary("https://" + url)
                }

            }
        }
    }


    private fun showImageUrlPicker() {
        activity?.let {
            BrowserDialog.showEditText(it,
                    R.string.image_url,
                    R.string.hint_url,
                    userPreferences.imageUrlString,
                    R.string.action_ok) { s ->
                userPreferences.imageUrlString = s
            }
        }
    }


    companion object {
        private const val SETTINGS_HOME = "home"
        private const val SETTINGS_HOMEPAGE_TYPE = "homepage_type"
        private const val SETTINGS_IMAGE_URL = "image_url"
        private const val SETTINGS_SHORTCUTS = "show_shortcuts"
    }
}
