package com.cookiegames.smartcookie.browser

import android.app.Activity
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.webkit.URLUtil
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.constant.*
import com.cookiegames.smartcookie.di.MainScheduler
import com.cookiegames.smartcookie.html.bookmark.BookmarkPageFactory
import com.cookiegames.smartcookie.html.homepage.HomePageFactory
import com.cookiegames.smartcookie.html.incognito.IncognitoPageFactory
import com.cookiegames.smartcookie.log.Logger
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.ssl.SslState
import com.cookiegames.smartcookie.view.BundleInitializer
import com.cookiegames.smartcookie.view.SmartCookieView
import com.cookiegames.smartcookie.view.TabInitializer
import com.cookiegames.smartcookie.view.UrlInitializer
import com.cookiegames.smartcookie.view.find.FindResults
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

/**
 * Presenter in charge of keeping track of the current tab and setting the current tab of the
 * browser.
 */
class BrowserPresenter(
        private val view: BrowserView,
        private val isIncognito: Boolean,
        private val userPreferences: UserPreferences,
        private val tabsModel: TabsManager,
        @MainScheduler private val mainScheduler: Scheduler,
        private val homePageFactory: HomePageFactory,
        private val incognitoPageFactory: IncognitoPageFactory,
        private val bookmarkPageFactory: BookmarkPageFactory,
        private val recentTabModel: RecentTabModel,
        private val logger: Logger
) {

    private var currentTab: SmartCookieView? = null
    private var shouldClose: Boolean = false
    private var intented: SmartCookieView? = null
    private var sslStateSubscription: Disposable? = null

    init {
        tabsModel.addTabNumberChangedListener(view::updateTabNumber)
    }

    /**
     * Initializes the tab manager with the new intent that is handed in by the BrowserActivity.
     *
     * @param intent the intent to handle, may be null.
     */
    fun setupTabs(intent: Intent?) {
        tabsModel.initializeTabs(view as Activity, intent, isIncognito)
            .subscribeBy(
                onSuccess = {
                    // At this point we always have at least a tab in the tab manager
                    view.notifyTabViewInitialized()
                    view.updateTabNumber(tabsModel.size())
                    tabChanged(tabsModel.positionOf(it))
                }
            )

    }

    /**
     * Notify the presenter that a change occurred to the current tab. Currently doesn't do anything
     * other than tell the view to notify the adapter about the change.
     *
     * @param tab the tab that changed, may be null.
     */
    fun tabChangeOccurred(tab: SmartCookieView?) = tab?.let {
        view.notifyTabViewChanged(tabsModel.indexOfTab(it))
    }

    private fun onTabChanged(newTab: SmartCookieView?) {
        logger.log(TAG, "On tab changed")
        view.updateSslState(newTab?.currentSslState() ?: SslState.None)

        sslStateSubscription?.dispose()
        sslStateSubscription = newTab
            ?.sslStateObservable()
            ?.observeOn(mainScheduler)
            ?.subscribe(view::updateSslState)

        val webView = newTab?.webView

        if (newTab == null) {
            view.removeTabView()
            currentTab?.let {
                it.pauseTimers()
                it.onDestroy()
            }
        } else {
            if (webView == null) {
                view.removeTabView()
                currentTab?.let {
                    it.pauseTimers()
                    it.onDestroy()
                }
            } else {
                currentTab.let {
                    // TODO: Restore this when Google fixes the bug where the WebView is
                    // blank after calling onPause followed by onResume.
                    // currentTab.onPause();
                    it?.isForegroundTab = false
                }

                newTab.resumeTimers()
                newTab.onResume()
                newTab.isForegroundTab = true

                view.updateProgress(newTab.progress)
                view.setBackButtonEnabled(newTab.canGoBack())
                view.setForwardButtonEnabled(newTab.canGoForward())
                view.updateUrl(newTab.url, false)
                view.setTabView(webView)
                val index = tabsModel.indexOfTab(newTab)
                if (index >= 0) {
                    view.notifyTabViewChanged(tabsModel.indexOfTab(newTab))
                }
            }
        }

        currentTab = newTab
    }

    /**
     * Closes all tabs but the current tab.
     */
    fun closeAllOtherTabs() {

        while (tabsModel.last() != tabsModel.indexOfCurrentTab()) {
            deleteTab(tabsModel.last())
        }

        while (0 != tabsModel.indexOfCurrentTab()) {
            deleteTab(0)
        }

    }

    private fun mapHomepageToCurrentUrl(): String = when (val homepage = userPreferences.homepage) {
        SCHEME_HOMEPAGE -> "$FILE${homePageFactory.createHomePage()}"
        SCHEME_INCOGNITO -> "$FILE${incognitoPageFactory.createIncognitoPage()}"
        SCHEME_BOOKMARKS -> "$FILE${bookmarkPageFactory.createBookmarkPage(null)}"
        else -> homepage
    }

    /**
     * Deletes the tab at the specified position.
     *
     * @param position the position at which to delete the tab.
     */
    fun deleteTab(position: Int, back: Boolean = false) {
        logger.log(TAG, "deleting tab...")
        val tabToDelete = tabsModel.getTabAtPosition(position) ?: return

        recentTabModel.addClosedTab(tabToDelete.saveState())

        if(!back && tabToDelete.isNewTab) tabToDelete.isNewTab = false

        val isShown = tabToDelete.isShown
        val shouldClose = shouldClose && isShown && tabToDelete.isNewTab || intented == currentTab && back && isShown
        val currentTab = tabsModel.currentTab

        if (tabsModel.size() == 1
            && currentTab != null
            && URLUtil.isFileUrl(currentTab.url)
            && currentTab.url == mapHomepageToCurrentUrl()) {
            if(userPreferences.closeOnLastTab) {
                view.closeActivity()
            }
            else if(!userPreferences.closeOnLastTab && tabsModel.currentTab == null && !isIncognito) {
                newTab(UrlInitializer(mapHomepageToCurrentUrl()), true)
            }
            return
        } else {
            if (isShown) {
                view.removeTabView()
            }
            val currentDeleted = tabsModel.deleteTab(position)
            if (currentDeleted) {
                tabChanged(tabsModel.indexOfCurrentTab())
            }
        }

        val afterTab = tabsModel.currentTab
        view.notifyTabViewRemoved(position)

        if (afterTab == null) {
            if(userPreferences.closeOnLastTab){
                view.closeBrowser()
            }
            else{
                newTab(UrlInitializer(mapHomepageToCurrentUrl()), true)
            }
            return
        } else if (afterTab !== currentTab) {
            view.notifyTabViewChanged(tabsModel.indexOfCurrentTab())
        }

        if (shouldClose && !isIncognito) {
            this.shouldClose = false
            view.closeActivity()
        }

        view.updateTabNumber(tabsModel.size())

        logger.log(TAG, "...deleted tab")
    }

    /**
     * Handle a new intent from the the main BrowserActivity.
     *
     * @param intent the intent to handle, may be null.
     */
    fun onNewIntent(intent: Intent?) = tabsModel.doAfterInitialization {
        if(intent?.getBooleanExtra("EXPORT_TABS", false) == true){

            try {
                var bookmarksExport = File(
                    Environment.getExternalStorageDirectory(),
                    "TabsExport.txt")
                var counter = 0
                while (bookmarksExport.exists()) {
                    counter++
                    bookmarksExport = File(
                        Environment.getExternalStorageDirectory(),
                        "TabsExport-$counter.txt")
                }
                val exportFile = bookmarksExport

                val fOut = FileOutputStream(exportFile)
                val myOutWriter = OutputStreamWriter(fOut)
                for(i in tabsModel.allTabs){
                    myOutWriter.append("${i.title}\n${i.url}\n=================\n")
                }
                myOutWriter.close()
                fOut.close()
            } catch (e: IOException) {
                Log.e("Exception", "File write failed: " + e.toString())
            }

            return@doAfterInitialization
        }
     val url = if (intent?.action == Intent.ACTION_WEB_SEARCH) {
        tabsModel.extractSearchFromIntent(intent)
    } else {
        intent?.dataString
    }
        val tabHashCode = intent?.extras?.getInt(INTENT_ORIGIN, 0) ?: 0
        if (tabHashCode != 0 && url != null) {
            tabsModel.getTabForHashCode(tabHashCode)?.loadUrl(url)
        } else if (url != null) {
            if (URLUtil.isFileUrl(url)) {
                intented = currentTab
                view.showBlockedLocalFileDialog {
                    newTab(UrlInitializer(url), true)
                    shouldClose = true
                    tabsModel.lastTab()?.isNewTab = true
                }
            } else {
                intented = currentTab
                newTab(UrlInitializer(url), true)
                shouldClose = true
                tabsModel.lastTab()?.isNewTab = true
            }
        }
    }

    /**
     * Call when the user long presses the new tab button.
     */
    fun onNewTabLongClicked() {
        recentTabModel.lastClosed()?.let {
            newTab(BundleInitializer(it), true)
            view.showSnackbar(R.string.reopening_recent_tab)
        }
    }

    /**
     * Loads a URL in the current tab.
     *
     * @param url the URL to load, must not be null.
     */
    fun loadUrlInCurrentView(url: String) {
        tabsModel.currentTab?.loadUrl(url)
    }

    /**
     * Notifies the presenter that it should shut down. This should be called when the
     * BrowserActivity is destroyed so that we don't leak any memory.
     */
    fun shutdown() {
        //TODO: Fix a crash when this is called!
        //onTabChanged(null)
        tabsModel.cancelPendingWork()
        sslStateSubscription?.dispose()
    }

    /**
     * Notifies the presenter that we wish to switch to a different tab at the specified position.
     * If the position is not in the model, this method will do nothing.
     *
     * @param position the position of the tab to switch to.
     */
    fun tabChanged(position: Int) {
        if (position < 0 || position >= tabsModel.size()) {
            logger.log(TAG, "tabChanged invalid position: $position")
            return
        }

        logger.log(TAG, "tabChanged: $position")
        onTabChanged(tabsModel.switchToTab(position))
    }

    /**
+     * Open a new tab with the specified URL. You can choose to show the tab or load it in the
     * background.
     *
     * @param tabInitializer the tab initializer to run after the tab as been created.
     * @param show whether or not to switch to this tab after opening it.
     * @return true if we successfully created the tab.
     */
    fun newTab(tabInitializer: TabInitializer, show: Boolean): Boolean {


        logger.log(TAG, "New tab, show: $show")

        val startingTab = tabsModel.newTab(view as Activity, tabInitializer, isIncognito)
        if (tabsModel.size() == 1) {
            startingTab.resumeTimers()
        }

        view.notifyTabViewAdded()
        view.updateTabNumber(tabsModel.size())

        if (show) {
            onTabChanged(tabsModel.switchToTab(tabsModel.last()))
        }

        return true
    }

    /**
    +     * Open a new tab with the specified URL at a specific index. You can choose to show the tab or load it in the
     * background.
     *
     * @param tabInitializer the tab initializer to run after the tab as been created.
     * @param show whether or not to switch to this tab after opening it.
     * @return true if we successfully created the tab.
     */
    fun newTabAtPosition(tabInitializer: TabInitializer, show: Boolean, index: Int): Boolean {


        logger.log(TAG, "New tab, show: $show")

        val startingTab = tabsModel.newTabAtPosition(view as Activity, tabInitializer, isIncognito, index)
        if (tabsModel.size() == 1) {
            startingTab.resumeTimers()
        }

        view.notifyTabViewAdded()
        view.updateTabNumber(tabsModel.size())

        if (show) {
            onTabChanged(tabsModel.switchToTab(index))
        }

        return true
    }

    fun onAutoCompleteItemPressed() {
        tabsModel.currentTab?.requestFocus()
    }

    fun findInPage(query: String): FindResults? {
        return tabsModel.currentTab?.find(query)
    }

    companion object {
        private const val TAG = "BrowserPresenter"
    }

}
