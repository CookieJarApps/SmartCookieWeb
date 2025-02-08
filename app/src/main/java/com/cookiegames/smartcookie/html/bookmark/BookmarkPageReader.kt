package com.cookiegames.smartcookie.html.bookmark

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * The store for the bookmarks HTML.
 */
class BookmarkPageReader @Inject constructor() {

    fun provideHtml(context: Context): String {
        val inputStream = context.assets.open("bookmarks.html")
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        return bufferedReader.use { it.readText() }
    }

}