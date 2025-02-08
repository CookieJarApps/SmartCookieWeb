package com.cookiegames.smartcookie.html

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class ListPageReader @Inject constructor() {

    fun provideHtml(context: Context): String {
        val inputStream = context.assets.open("list.html")
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        return bufferedReader.use { it.readText() }
    }

}
