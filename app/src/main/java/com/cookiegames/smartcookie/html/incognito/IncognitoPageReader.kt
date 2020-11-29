package com.cookiegames.smartcookie.html.incognito

import com.anthonycr.mezzanine.FileStream

/**
 * The store for the homepage HTML.
 */
@FileStream("app/src/main/html/private.html")
interface IncognitoPageReader {

    fun provideHtml(): String

}