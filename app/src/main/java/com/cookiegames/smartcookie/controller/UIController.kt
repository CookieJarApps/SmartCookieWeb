package com.cookiegames.smartcookie.controller

import com.cookiegames.smartcookie.browser.TabsManager
import com.cookiegames.smartcookie.database.HistoryItem
import com.cookiegames.smartcookie.dialog.LightningDialogBuilder
import com.cookiegames.smartcookie.view.LightningView
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Message
import androidx.annotation.ColorInt
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback

interface UIController {

    @ColorInt
    fun getUiColor(): Int

    fun getUseDarkTheme(): Boolean

    fun getTabModel(): TabsManager

    fun changeToolbarBackground(favicon: Bitmap, drawable: Drawable?)

    fun updateUrl(title: String?, isLoading: Boolean)

    fun updateProgress(n: Int)

    fun updateHistory(title: String?, url: String)

    fun openFileChooser(uploadMsg: ValueCallback<Uri>)

    fun onShowCustomView(view: View, callback: CustomViewCallback)

    fun onShowCustomView(view: View, callback: CustomViewCallback, requestedOrienation: Int)

    fun onHideCustomView()

    fun onCreateWindow(resultMsg: Message)

    fun onCloseWindow(view: LightningView)

    fun hideActionBar()

    fun showActionBar()

    fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>)

    fun closeEmptyTab()

    fun showCloseDialog(position: Int)

    fun newTabButtonClicked()

    fun tabCloseClicked(position: Int)

    fun tabClicked(position: Int)

    fun newTabButtonLongClicked()

    fun bookmarkButtonClicked()

    fun bookmarkItemClicked(item: HistoryItem)

    fun closeBookmarksDrawer()

    fun setForwardButtonEnabled(enabled: Boolean)

    fun setBackButtonEnabled(enabled: Boolean)

    fun tabChanged(tab: LightningView)

    fun onBackButtonPressed()

    fun onForwardButtonPressed()

    fun onHomeButtonPressed()

    fun handleBookmarksChange()

    fun handleDownloadDeleted()

    fun handleBookmarkDeleted(item: HistoryItem)

    fun handleNewTab(newTabType: LightningDialogBuilder.NewTab, url: String)

    fun handleHistoryChange()

}