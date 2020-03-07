package com.cookiegames.smartcookie.search.engine

import com.cookiegames.smartcookie.R

/**
 * A custom search engine.
 */
class CustomSearch(queryUrl: String) : BaseSearchEngine(
    "file:///android_asset/smartcookieweb.webp",
    queryUrl,
    R.string.search_engine_custom
)
