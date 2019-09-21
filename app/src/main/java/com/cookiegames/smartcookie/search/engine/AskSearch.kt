package com.cookiegames.smartcookie.search.engine

import com.cookiegames.smartcookie.R

/**
 * The Ask search engine.
 */
class AskSearch : BaseSearchEngine(
    "file:///android_asset/ask.png",
    "http://www.ask.com/web?qsrc=0&o=0&l=dir&qo=LightningBrowser&q=",
    R.string.search_engine_ask
)
