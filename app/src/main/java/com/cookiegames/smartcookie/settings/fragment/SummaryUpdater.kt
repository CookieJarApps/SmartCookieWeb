package com.cookiegames.smartcookie.settings.fragment

/**
 * A command that updates the summary of a preference.
 */
class SummaryUpdater(private val preference: androidx.preference.Preference) {

    /**
     * Updates the summary of the preference.
     *
     * @param text the text to display in the summary.
     */
    fun updateSummary(text: String) {
        preference.summary = text
    }

}