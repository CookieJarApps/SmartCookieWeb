package com.cookiegames.smartcookie.js

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class ThemeColor @Inject constructor() {

    fun provideJs(context: Context): String {
        val inputStream = context.assets.open("ThemeColor.js")
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        return bufferedReader.use { it.readText() }
    }

}