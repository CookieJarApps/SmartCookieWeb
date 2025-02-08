package com.cookiegames.smartcookie.settings.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import com.cookiegames.smartcookie.DeviceCapabilities
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.SearchBoxDisplayChoice
import com.cookiegames.smartcookie.constant.TEXT_ENCODINGS
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.extensions.resizeAndShow
import com.cookiegames.smartcookie.extensions.withSingleChoiceItems
import com.cookiegames.smartcookie.isSupported
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.view.RenderingMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import javax.inject.Inject

/**
 * The advanced settings of the app.
 */
class AdvancedSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        clickableDynamicPreference(
                preference = SETTINGS_NEWS_ENDPOINT,
                summary = userPreferences.newsEndpoint,
                onClick = this::showNewsEndpointPicker
        )

        clickableDynamicPreference(
                preference = SETTINGS_TRANSLATION_ENDPOINT,
                summary = userPreferences.translationEndpoint,
                onClick = this::showTranslationEndpointPicker
        )

        clickableDynamicPreference(
            preference = SETTINGS_RENDERING_MODE,
            summary = userPreferences.renderingMode.toDisplayString(),
            onClick = this::showRenderingDialogPicker
        )

        clickableDynamicPreference(
            preference = SETTINGS_TEXT_ENCODING,
            summary = userPreferences.textEncoding,
            onClick = this::showTextEncodingDialogPicker
        )

        clickableDynamicPreference(
            preference = SETTINGS_URL_CONTENT,
            summary = userPreferences.urlBoxContentChoice.toDisplayString(),
            onClick = this::showUrlBoxDialogPicker
        )

        switchPreference(
            preference = SETTINGS_NEW_WINDOW,
            isChecked = userPreferences.popupsEnabled,
            onCheckChange = { userPreferences.popupsEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_ENABLE_COOKIES,
            isChecked = userPreferences.cookiesEnabled,
            onCheckChange = { userPreferences.cookiesEnabled = it }
        )
        switchPreference(
                preference = SETTINGS_BLOCK_INTENT,
                isChecked = userPreferences.blockIntent,
                onCheckChange = { userPreferences.blockIntent = it }
        )

        switchPreference(
            preference = SETTINGS_COOKIES_INCOGNITO,
            isChecked = userPreferences.incognitoCookiesEnabled,
            onCheckChange = { userPreferences.incognitoCookiesEnabled = it }
        )

        switchPreference(
                preference = SETTINGS_SHOW_SSL,
                isChecked = userPreferences.ssl,
                onCheckChange = { userPreferences.ssl = it }
        )

        switchPreference(
            preference = SETTINGS_RESTORE_TABS,
            isChecked = userPreferences.restoreLostTabsEnabled,
            onCheckChange = { userPreferences.restoreLostTabsEnabled = it }
        )

        val incognitoCheckboxPreference = switchPreference(
                preference = SETTINGS_COOKIES_INCOGNITO,
                isEnabled = !DeviceCapabilities.FULL_INCOGNITO.isSupported,
                isChecked = if (DeviceCapabilities.FULL_INCOGNITO.isSupported) {
                    userPreferences.cookiesEnabled
                } else {
                    userPreferences.incognitoCookiesEnabled
                },
                summary = if (DeviceCapabilities.FULL_INCOGNITO.isSupported) {
                    getString(R.string.incognito_cookies_new)
                } else {
                    null
                },
                onCheckChange = { userPreferences.incognitoCookiesEnabled = it }
        )

        switchPreference(
                preference = SETTINGS_ENABLE_COOKIES,
                isChecked = userPreferences.cookiesEnabled,
                onCheckChange = {
                    userPreferences.cookiesEnabled = it
                    if (DeviceCapabilities.FULL_INCOGNITO.isSupported) {
                        incognitoCheckboxPreference?.isChecked = it
                    }
                }
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_advanced)
    }

    /**
     * Shows the dialog which allows the user to choose the browser's rendering method.
     *
     * @param summaryUpdater the command which allows the summary to be updated.
     */
    private fun showRenderingDialogPicker(summaryUpdater: SummaryUpdater) {
        activity?.let { MaterialAlertDialogBuilder(it) }?.apply {
            setTitle(resources.getString(R.string.rendering_mode))

            val values = RenderingMode.values().map { Pair(it, it.toDisplayString()) }
            withSingleChoiceItems(values, userPreferences.renderingMode) {
                userPreferences.renderingMode = it
                summaryUpdater.updateSummary(it.toDisplayString())

            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }?.resizeAndShow()

    }

    /**
     * Shows the dialog which allows the user to choose the browser's text encoding.
     *
     * @param summaryUpdater the command which allows the summary to be updated.
     */
    private fun showTextEncodingDialogPicker(summaryUpdater: SummaryUpdater) {
        activity?.let {
            MaterialAlertDialogBuilder(it).apply {
                setTitle(resources.getString(R.string.text_encoding))

                val currentChoice = TEXT_ENCODINGS.indexOf(userPreferences.textEncoding)

                setSingleChoiceItems(TEXT_ENCODINGS, currentChoice) { _, which ->
                    userPreferences.textEncoding = TEXT_ENCODINGS[which]
                    summaryUpdater.updateSummary(TEXT_ENCODINGS[which])
                }
                setPositiveButton(resources.getString(R.string.action_ok), null)
            }.resizeAndShow()
        }
    }

    /**
     * Shows the dialog which allows the user to choose the browser's URL box display options.
     *
     * @param summaryUpdater the command which allows the summary to be updated.
     */
    private fun showUrlBoxDialogPicker(summaryUpdater: SummaryUpdater) {
        activity?.let { MaterialAlertDialogBuilder(it) }?.apply {
            setTitle(resources.getString(R.string.url_contents))

            val items = SearchBoxDisplayChoice.values().map { Pair(it, it.toDisplayString()) }

            withSingleChoiceItems(items, userPreferences.urlBoxContentChoice) {
                userPreferences.urlBoxContentChoice = it
                summaryUpdater.updateSummary(it.toDisplayString())
            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }?.resizeAndShow()
    }

    private fun SearchBoxDisplayChoice.toDisplayString(): String {
        val stringArray = resources.getStringArray(R.array.url_content_array)
        return when (this) {
            SearchBoxDisplayChoice.URL -> stringArray[0]
            SearchBoxDisplayChoice.DOMAIN -> stringArray[1]
            SearchBoxDisplayChoice.TITLE -> stringArray[2]
        }
    }

    private fun RenderingMode.toDisplayString(): String = getString(when (this) {
        RenderingMode.NORMAL -> R.string.name_normal
        RenderingMode.INVERTED -> R.string.name_inverted
        RenderingMode.GRAYSCALE -> R.string.name_grayscale
        RenderingMode.INVERTED_GRAYSCALE -> R.string.name_inverted_grayscale
        RenderingMode.INCREASE_CONTRAST -> R.string.name_increase_contrast
    })

    private fun showNewsEndpointPicker(summaryUpdater: SummaryUpdater) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.dialog_edit_text)

        editText.setText(userPreferences.newsEndpoint)

        val editorDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.news_endpoint)
                .setView(dialogView)
                .setCancelable(false)
                .setNegativeButton(R.string.action_back) { dialog, which ->
                }
                .setPositiveButton(R.string.action_ok
                ) { _, _ ->
                    userPreferences.newsEndpoint = editText.text.toString()
                }

        val dialog = editorDialog.show()
        BrowserDialog.setDialogSize(requireContext(), dialog)

        summaryUpdater.updateSummary(editText.text.toString())
    }

    private fun showTranslationEndpointPicker(summaryUpdater: SummaryUpdater) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.dialog_edit_text)

        editText.setText(userPreferences.translationEndpoint)

        val editorDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.news_endpoint)
                .setView(dialogView)
                .setCancelable(false)
                .setNegativeButton(R.string.action_back) { dialog, which ->
                }
                .setPositiveButton(R.string.action_ok
                ) { _, _ ->
                    userPreferences.translationEndpoint = editText.text.toString()
                }

        val dialog = editorDialog.show()
        BrowserDialog.setDialogSize(requireContext(), dialog)

        summaryUpdater.updateSummary(editText.text.toString())
    }

    companion object {
        private const val SETTINGS_NEW_WINDOW = "allow_new_window"
        private const val SETTINGS_ENABLE_COOKIES = "allow_cookies"
        private const val SETTINGS_COOKIES_INCOGNITO = "incognito_cookies"
        private const val SETTINGS_RESTORE_TABS = "restore_tabs"
        private const val SETTINGS_RENDERING_MODE = "rendering_mode"
        private const val SETTINGS_URL_CONTENT = "url_contents"
        private const val SETTINGS_TEXT_ENCODING = "text_encoding"
        private const val SETTINGS_BLOCK_INTENT = "block_intent"
        private const val SETTINGS_SHOW_SSL = "show_ssl"
        private const val SETTINGS_LEGACY_DOWNLOADER = "downloader"
        private const val SETTINGS_TRANSLATION_ENDPOINT = "translation_endpoint"
        private const val SETTINGS_NEWS_ENDPOINT = "news_endpoint"
    }

}
