package com.cookiegames.smartcookie.settings.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.ChooseNavbarCol
import com.cookiegames.smartcookie.browser.DrawerLineChoice
import com.cookiegames.smartcookie.browser.DrawerSizeChoice
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.extensions.resizeAndShow
import com.cookiegames.smartcookie.extensions.withSingleChoiceItems
import com.cookiegames.smartcookie.preference.UserPreferences
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import javax.inject.Inject


class DisplaySettingsFragment : AbstractSettingsFragment() {

    private lateinit var themeOptions: Array<String>

    @Inject internal lateinit var userPreferences: UserPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_display)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        // preferences storage
        clickablePreference(
            preference = SETTINGS_TEXTSIZE,
            onClick = ::showTextSizePicker
        )

        switchPreference(
                preference = SETTINGS_NAVBAR,
                isChecked = userPreferences.navbar,
                isEnabled = !userPreferences.bottomBar || userPreferences.navbar,
                onCheckChange = { userPreferences.navbar = it }
        )

        switchPreference(
            preference = SETTINGS_HIDESTATUSBAR,
            isChecked = userPreferences.hideStatusBarEnabled,
            onCheckChange = { userPreferences.hideStatusBarEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_FULLSCREEN,
            isChecked = userPreferences.fullScreenEnabled,
            onCheckChange = {userPreferences.fullScreenEnabled = it }
        )

        switchPreference(
                preference = SETTINGS_EXTRA,
                isChecked = userPreferences.showExtraOptions,
                onCheckChange = { userPreferences.showExtraOptions = it}
        )

        switchPreference(
            preference = SETTINGS_VIEWPORT,
            isChecked = userPreferences.useWideViewPortEnabled,
            onCheckChange = { userPreferences.useWideViewPortEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_OVERVIEWMODE,
            isChecked = userPreferences.overviewModeEnabled,
            onCheckChange = { userPreferences.overviewModeEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_REFLOW,
            isChecked = userPreferences.textReflowEnabled,
            onCheckChange = { userPreferences.textReflowEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_DRAWERTABS,
            isChecked = userPreferences.showTabsInDrawer,
            onCheckChange = { userPreferences.showTabsInDrawer = it }
        )

        switchPreference(
            preference = SETTINGS_SWAPTABS,
            isChecked = userPreferences.bookmarksAndTabsSwapped,
            onCheckChange = { userPreferences.bookmarksAndTabsSwapped = it }
        )

        switchPreference(
                preference = SETTINGS_FOREGROUND,
                isChecked = userPreferences.tabsToForegroundEnabled,
                onCheckChange = { userPreferences.tabsToForegroundEnabled = it }
        )
        switchPreference(
                preference = SETTINGS_BOTTOM_BAR,
                isChecked = userPreferences.bottomBar,
                isEnabled = !userPreferences.navbar || userPreferences.bottomBar,
                onCheckChange = {userPreferences.bottomBar = it; Toast.makeText(activity, R.string.please_restart, Toast.LENGTH_LONG).show()}
        )
        clickablePreference(
                preference = SETTINGS_LINES,
                onClick = ::showDrawerLines
        )

        clickablePreference(
                preference = SETTINGS_SIZE,
                onClick = ::showDrawerSize
        )
    }

    private fun showDrawerSize() {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.drawer_size)
            val stringArray = resources.getStringArray(R.array.drawer_size)
            val values = DrawerSizeChoice.values().map {
                Pair(it, when (it) {
                    DrawerSizeChoice.AUTO -> stringArray[0]
                    DrawerSizeChoice.ONE -> stringArray[1]
                    DrawerSizeChoice.TWO -> stringArray[2]
                    DrawerSizeChoice.THREE -> stringArray[3]
                })
            }
            withSingleChoiceItems(values, userPreferences.drawerSize) {
                userPreferences.drawerSize = it
            }
            setPositiveButton(R.string.action_ok){_, _ ->
                Toast.makeText(activity, R.string.please_restart, Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun showDrawerLines() {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.drawer_lines)
            val stringArray = resources.getStringArray(R.array.drawer_lines)
            val values = DrawerLineChoice.values().map {
                Pair(it, when (it) {
                    DrawerLineChoice.ONE -> stringArray[0]
                    DrawerLineChoice.TWO -> stringArray[1]
                    DrawerLineChoice.THREE -> stringArray[2]
                })
            }
            withSingleChoiceItems(values, userPreferences.drawerLines) {
                userPreferences.drawerLines = it
            }
            setPositiveButton(R.string.action_ok){_, _ ->
                Toast.makeText(activity, R.string.please_restart, Toast.LENGTH_LONG).show()
            }
        }

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

    private fun updateNavbarCol(choice: ChooseNavbarCol) {
        if (choice == ChooseNavbarCol.COLOR) {
            showNavbarColPicker()

        }

        userPreferences.navbarColChoice = choice
    }


    private fun showTextSizePicker() {
        val maxValue = 7
        MaterialAlertDialogBuilder(requireContext()).apply {
            val layoutInflater = activity?.layoutInflater
            val customView = (layoutInflater?.inflate(R.layout.dialog_seek_bar, null) as LinearLayout).apply {
                val text = TextView(activity).apply {
                    setText(R.string.untitled)
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                addView(text)
                val size = TextView(activity).apply {
                    setText(R.string.untitled)
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                addView(size)

                findViewById<SeekBar>(R.id.text_size_seekbar).apply {
                    setOnSeekBarChangeListener(TextSeekBarListener(text, size))
                    max = maxValue
                    progress = maxValue - userPreferences.textSize
                }

            }
            setView(customView)
            setTitle(R.string.title_text_size)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val seekBar = customView.findViewById<SeekBar>(R.id.text_size_seekbar)
                userPreferences.textSize = maxValue - seekBar.progress
            }
        }.resizeAndShow()
    }

    private class TextSeekBarListener(
        private val sampleText: TextView,
        private val sizeText: TextView
    ) : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(view: SeekBar, size: Int, user: Boolean) {
            this.sampleText.textSize = getTextSize(size)
            this.sizeText.text = (size * 15 + 40).toString() + "%"
        }

        override fun onStartTrackingTouch(arg0: SeekBar) {}

        override fun onStopTrackingTouch(arg0: SeekBar) {}

    }

    companion object {

        private const val SETTINGS_HIDESTATUSBAR = "fullScreenOption"
        private const val SETTINGS_FULLSCREEN = "fullscreen"
        private const val SETTINGS_VIEWPORT = "wideViewPort"
        private const val SETTINGS_OVERVIEWMODE = "overViewMode"
        private const val SETTINGS_REFLOW = "text_reflow"
        private const val SETTINGS_TEXTSIZE = "text_size"
        private const val SETTINGS_DRAWERTABS = "cb_drawertabs"
        private const val SETTINGS_SWAPTABS = "cb_swapdrawers"
        private const val SETTINGS_FOREGROUND = "new_tabs_foreground"
        private const val SETTINGS_EXTRA = "show_extra"
        private const val SETTINGS_BOTTOM_BAR = "bottom_bar"
        private const val SETTINGS_LINES = "drawer_lines"
        private const val SETTINGS_SIZE = "drawer_size"
        private const val SETTINGS_NAVBAR = "second_bar"

        private const val XXXX_LARGE = 38.0f
        private const val XXX_LARGE = 34.0f
        private const val XX_LARGE = 30.0f
        private const val X_LARGE = 26.0f
        private const val LARGE = 22.0f
        private const val MEDIUM = 20.0f
        private const val SMALL = 18.0f
        private const val X_SMALL = 16.0f

        private fun getTextSize(size: Int): Float = when (size) {
            0 -> X_SMALL
            1 -> SMALL
            2 -> MEDIUM
            3 -> LARGE
            4 -> X_LARGE
            5 -> XX_LARGE
            6 -> XXX_LARGE
            7 -> XXXX_LARGE
            else -> MEDIUM
        }
    }
}
