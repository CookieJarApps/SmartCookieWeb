package com.cookiegames.smartcookie.browser

import com.cookiegames.smartcookie.database.HistoryItem

interface BookmarksView {

    fun navigateBack()

    fun handleUpdatedUrl(url: String)

    fun handleBookmarkDeleted(item: HistoryItem)

}
