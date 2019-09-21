package com.cookiegames.smartcookie.browser

import com.cookiegames.smartcookie.database.Bookmark

interface BookmarksView {

    fun navigateBack()

    fun handleUpdatedUrl(url: String)

    fun handleBookmarkDeleted(bookmark: Bookmark)

}
