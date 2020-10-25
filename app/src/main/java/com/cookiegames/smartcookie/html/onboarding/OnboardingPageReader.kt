package com.cookiegames.smartcookie.html.onboarding

import com.anthonycr.mezzanine.FileStream

/**
 * The store for the homepage HTML.
 */
@FileStream("app/src/main/html/onboarding.html")
interface OnboardingPageReader {

    fun provideHtml(): String

}