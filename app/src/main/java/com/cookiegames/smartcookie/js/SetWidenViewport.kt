package com.cookiegames.smartcookie.js

import com.anthonycr.mezzanine.FileStream

@FileStream("app/src/main/js/WidenViewport.js")
interface SetWidenViewport{
    fun provideJs(): String
}