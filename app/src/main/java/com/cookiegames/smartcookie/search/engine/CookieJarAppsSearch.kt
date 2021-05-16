package com.cookiegames.smartcookie.search.engine

import com.cookiegames.smartcookie.R

/**
 * The CookieJarApps searx instance search engine.
 *
 */
class CookieJarAppsSearch : BaseSearchEngine(
    "file:///android_asset/smartcookieweb.webp",
    "https://searx.cookiejarapps.com/?q=",
    R.string.search_engine_searx_cookiejarapps
)
