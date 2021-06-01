/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 7/11/2020 */

package com.cookiegames.smartcookie.settings.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.MainActivity
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.*
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.extensions.resizeAndShow
import com.cookiegames.smartcookie.extensions.withSingleChoiceItems
import com.cookiegames.smartcookie.preference.UserPreferences
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import javax.inject.Inject


class ThemeSettingsFragment : AbstractSettingsFragment() {

    private lateinit var themeOptions: Array<String>

    @Inject internal lateinit var userPreferences: UserPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_theme)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        clickableDynamicPreference(
                preference = SETTINGS_THEME,
                summary = userPreferences.useTheme.toDisplayString(),
                onClick = ::showThemePicker
        )

        clickablePreference(
                preference = SETTINGS_NAVBAR_COL,
                onClick = ::showColorPicker
        )

        switchPreference(
            preference = SETTINGS_DARK_MODE,
            isChecked = userPreferences.darkModeExtension,
            onCheckChange = { userPreferences.darkModeExtension = it; Toast.makeText(activity, R.string.please_restart, Toast.LENGTH_LONG).show()}
        )

        switchPreference(
                preference = SETTINGS_BLACK_STATUS,
                isChecked = userPreferences.useBlackStatusBar,
                onCheckChange = { userPreferences.useBlackStatusBar = it }
        )
        switchPreference(
                preference = SETTINGS_STARTPAGE,
                isChecked = userPreferences.startPageThemeEnabled,
                onCheckChange = { userPreferences.startPageThemeEnabled = it }
        )

    }


    private fun AppTheme.toDisplayString(): String = getString(when (this) {
        AppTheme.LIGHT -> R.string.light_theme
        AppTheme.DARK -> R.string.dark_theme
        AppTheme.BLACK -> R.string.black_theme
    })


    private fun showThemePicker(summaryUpdater: SummaryUpdater) {
        val currentTheme = userPreferences.useTheme
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(resources.getString(R.string.theme))
            val values = AppTheme.values().map { Pair(it, it.toDisplayString()) }
            withSingleChoiceItems(values, userPreferences.useTheme) {
                userPreferences.useTheme = it
                summaryUpdater.updateSummary(it.toDisplayString())
            }
            setPositiveButton(resources.getString(R.string.action_ok)) { _, _ ->
                if (currentTheme != userPreferences.useTheme) {
                    //activity.onBackPressed()
                    val intent = Intent(activity, MainActivity::class.java)
                    startActivity(intent)
                }
            }
            setOnCancelListener {
                if (currentTheme != userPreferences.useTheme) {
                    activity?.onBackPressed()
                }
            }
        }.resizeAndShow()

    }

    private fun showColorPicker() {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.navbar_col)
            val stringArray = resources.getStringArray(R.array.navbar_col)
            val values = ChooseNavbarCol.values().map {
                Pair(it, when (it) {
                    ChooseNavbarCol.NONE -> stringArray[0]
                    ChooseNavbarCol.COLOR -> stringArray[1]
                })
            }
            withSingleChoiceItems(values, userPreferences.navbarColChoice) {
                userPreferences.navbarColChoice = it
            }
            setPositiveButton(R.string.action_ok) { _, _ ->
                updateNavbarCol(userPreferences.navbarColChoice)
            }
        }

    }

    private fun updateNavbarCol(choice: ChooseNavbarCol) {
        if (choice == ChooseNavbarCol.COLOR) {
            showNavbarColPicker()
        }

        userPreferences.navbarColChoice = choice
    }

    private fun showNavbarColPicker(){
        var initColor = userPreferences.colorNavbar
        if(userPreferences.navbarColChoice == ChooseNavbarCol.NONE){
            initColor = Color.WHITE
        }
        ColorPickerDialogBuilder
                .with(activity)
                .setTitle("Choose color")
                .initialColor(initColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener { /*selectedColor -> activity.toast("onColorSelected: 0x" + Integer.toHexString(selectedColor))*/ }
                .setPositiveButton("ok") { dialog, selectedColor, allColors -> userPreferences.colorNavbar = selectedColor }
                .setNegativeButton("cancel") { dialog, which -> }
                .build()
                .show()
    }

    companion object {
        private const val SETTINGS_THEME = "app_theme"
        private const val SETTINGS_NAVBAR_COL = "navbar_col"
        private const val SETTINGS_DARK_MODE = "dark_mode"
        private const val SETTINGS_BLACK_STATUS = "black_status_bar"
        private const val SETTINGS_STARTPAGE = "startpage_theme"
    }
}
