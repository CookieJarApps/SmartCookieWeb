package com.cookiegames.smartcookie.browser.bookmark

import com.cookiegames.smartcookie.browser.BookmarksView

/**
 * The UI model representing the current folder shown
 * by the [BookmarksView].
 *
 *
 * Created by anthonycr on 5/7/17.
 */
class BookmarkUiModel {

    /**
     * Gets the current folder that is being shown.

     * @return the current folder, null for root.
     */
    /**
     * Sets the current folder that is being shown.
     * Use null as the root folder.

     * @param folder the current folder, null for root.
     */
    var currentFolder: String? = null

    /**
     * Determines if the current folder is
     * the root folder.

     * @return true if the current folder is
     * * the root, false otherwise.
     */
    val isRootFolder: Boolean
        get() = currentFolder == null

}
