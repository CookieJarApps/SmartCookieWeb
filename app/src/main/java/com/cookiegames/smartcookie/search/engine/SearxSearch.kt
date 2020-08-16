package com.cookiegames.smartcookie.search.engine

import com.cookiegames.smartcookie.R

/**
 * The Searx search engine.
 *
 */
class SearxSearch : BaseSearchEngine(
    "file:///android_asset/searx.webp",
    "https://www.searx.be/?q=",
    R.string.search_engine_searx
)
