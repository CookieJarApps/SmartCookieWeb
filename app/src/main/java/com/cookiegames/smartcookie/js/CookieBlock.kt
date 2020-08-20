package com.cookiegames.smartcookie.js

import com.anthonycr.mezzanine.FileStream


@FileStream("app/src/main/js/CookieBlock.js")
interface CookieBlock {

    fun provideJs(): String

}