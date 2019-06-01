package com.cookiegames.smartcookie.search.engine

import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.constant.Constants

/**
 * The Ask search engine.
 */
class AskSearch : BaseSearchEngine(
        "file:///android_asset/ask.png",
        Constants.ASK_SEARCH,
        R.string.search_engine_ask
)
