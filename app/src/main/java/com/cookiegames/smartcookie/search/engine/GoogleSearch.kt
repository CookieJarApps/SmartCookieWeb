package com.cookiegames.smartcookie.search.engine

import com.cookiegames.smartcookie.CheckForInternet
import com.cookiegames.smartcookie.MainActivity
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.constant.Constants

/**
 * The Google search engine.
 *
 * See https://www.google.com/images/srpr/logo11w.png for the icon.
 */
class GoogleSearch : BaseSearchEngine(
        "file:///android_asset/google.png",
        Constants.GOOGLE_SEARCH,
        R.string.search_engine_google
)
