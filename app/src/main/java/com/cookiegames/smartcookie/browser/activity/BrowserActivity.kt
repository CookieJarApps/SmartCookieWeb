package com.cookiegames.smartcookie.browser.activity

import android.app.Activity
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.AssetManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.*
import android.view.ViewGroup.LayoutParams
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.drawerlayout.widget.DrawerLayout
import androidx.palette.graphics.Palette
import butterknife.ButterKnife
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.IncognitoActivity
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.*
import com.cookiegames.smartcookie.browser.bookmarks.BookmarksDrawerView
import com.cookiegames.smartcookie.browser.tabs.TabsDesktopView
import com.cookiegames.smartcookie.browser.tabs.TabsDrawerView
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.database.Bookmark
import com.cookiegames.smartcookie.database.HistoryEntry
import com.cookiegames.smartcookie.database.SearchSuggestion
import com.cookiegames.smartcookie.database.WebPage
import com.cookiegames.smartcookie.database.bookmark.BookmarkRepository
import com.cookiegames.smartcookie.database.history.HistoryRepository
import com.cookiegames.smartcookie.databinding.ActivityMainBinding
import com.cookiegames.smartcookie.databinding.DialogAppLockBinding
import com.cookiegames.smartcookie.databinding.ToolbarContentBinding
import com.cookiegames.smartcookie.di.*
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.dialog.DialogItem
import com.cookiegames.smartcookie.dialog.LightningDialogBuilder
import com.cookiegames.smartcookie.extensions.*
import com.cookiegames.smartcookie.html.bookmark.BookmarkPageFactory
import com.cookiegames.smartcookie.html.history.HistoryPageFactory
import com.cookiegames.smartcookie.html.homepage.HomePageFactory
import com.cookiegames.smartcookie.html.incognito.IncognitoPageFactory
import com.cookiegames.smartcookie.icon.TabCountView
import com.cookiegames.smartcookie.interpolator.BezierDecelerateInterpolator
import com.cookiegames.smartcookie.log.Logger
import com.cookiegames.smartcookie.notifications.IncognitoNotification
import com.cookiegames.smartcookie.permissions.PermissionsManager
import com.cookiegames.smartcookie.popup.PopUpClass
import com.cookiegames.smartcookie.search.SearchEngineProvider
import com.cookiegames.smartcookie.search.SuggestionsAdapter
import com.cookiegames.smartcookie.ssl.SslState
import com.cookiegames.smartcookie.ssl.createSslDrawableForState
import com.cookiegames.smartcookie.ssl.showSslDialog
import com.cookiegames.smartcookie.utils.*
import com.cookiegames.smartcookie.view.*
import com.cookiegames.smartcookie.view.SearchView
import com.cookiegames.smartcookie.view.find.FindResults
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.subscribeBy
import java.io.IOException
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.system.exitProcess


abstract class BrowserActivity : ThemableBrowserActivity(), BrowserView, UIController, OnClickListener, OnKeyboardVisibilityListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbarBinding: ToolbarContentBinding

    // Toolbar ViewscreateSslDrawableForState
    private var searchBackground: View? = null
    private var searchView: SearchView? = null
    private var homeImageView: ImageView? = null
    private var tabCountView: TabCountView? = null

    // Current tab view being displayed
    private var currentTabView: View? = null

    // Full Screen Video Views
    private var fullscreenContainerView: FrameLayout? = null
    private var videoView: VideoView? = null
    private var customView: View? = null

    // Adapter
    private var suggestionsAdapter: SuggestionsAdapter? = null

    // Callback
    private var customViewCallback: CustomViewCallback? = null
    private var uploadMessageCallback: ValueCallback<Uri>? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Primitives
    private var isFullScreen: Boolean = false
    private var hideStatusBar: Boolean = false
    private var isDarkTheme: Boolean = false
    private var isImmersiveMode = false
    private var shouldShowTabsInDrawer: Boolean = false
    private var swapBookmarksAndTabs: Boolean = false

    private var originalOrientation: Int = 0
    private var currentUiColor = Color.BLACK
    private var keyDownStartTime: Long = 0
    private var searchText: String? = null
    private var cameraPhotoPath: String? = null

    private var findResult: FindResults? = null

    private var prefs: SharedPreferences? = null

    private var lastTouchTime = 0
    private var currentTouchTime = 0

    // The singleton BookmarkManager
    @Inject
    lateinit var bookmarkManager: BookmarkRepository

    @Inject
    lateinit var historyModel: HistoryRepository

    @Inject
    lateinit var searchBoxModel: SearchBoxModel

    @Inject
    lateinit var searchEngineProvider: SearchEngineProvider

    @Inject
    lateinit var inputMethodManager: InputMethodManager

    @Inject
    lateinit var clipboardManager: ClipboardManager

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    @field:DiskScheduler
    lateinit var diskScheduler: Scheduler

    @Inject
    @field:DatabaseScheduler
    lateinit var databaseScheduler: Scheduler

    @Inject
    @field:MainScheduler
    lateinit var mainScheduler: Scheduler

    @Inject
    lateinit var tabsManager: TabsManager

    @Inject
    lateinit var homePageFactory: HomePageFactory

    @Inject
    lateinit var incognitoPageFactory: IncognitoPageFactory

    @Inject
    lateinit var bookmarkPageFactory: BookmarkPageFactory

    @Inject
    lateinit var historyPageFactory: HistoryPageFactory

    @Inject
    lateinit var historyPageInitializer: HistoryPageInitializer

    @Inject
    lateinit var downloadPageInitializer: DownloadPageInitializer

    @Inject
    lateinit var homePageInitializer: HomePageInitializer

    @Inject
    lateinit var incognitoPageInitializer: IncognitoPageInitializer

    @Inject
    @field:MainHandler
    lateinit var mainHandler: Handler

    @Inject
    lateinit var proxyUtils: ProxyUtils

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var bookmarksDialogBuilder: LightningDialogBuilder

    // Image
    var webPageBitmap: Bitmap? = null
    private val backgroundDrawable = ColorDrawable()
    private var incognitoNotification: IncognitoNotification? = null

    private var presenter: BrowserPresenter? = null
    private var tabsView: TabsView? = null
    private var bookmarksView: BookmarksView? = null

    // Menu
    private var backMenuItem: MenuItem? = null
    private var forwardMenuItem: MenuItem? = null

    private val longPressBackRunnable = Runnable {
        showCloseDialog(tabsManager.positionOf(tabsManager.currentTab))
    }

    /**
     * Determines if the current browser instance is in incognito mode or not.
     */
    abstract fun isIncognito(): Boolean

    /**
     * Choose the behavior when the controller closes the view.
     */
    abstract override fun closeActivity()

    /**
     * Choose what to do when the browser visits a website.
     *
     * @param title the title of the site visited.
     * @param url the url of the site visited.
     */
    abstract override fun updateHistory(title: String?, url: String)

    /**
     * An observable which asynchronously updates the user's cookie preferences.
     */
    protected abstract fun updateCookiePreference(): Completable



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)

        // TODO: add back bottom bar support
        /*if (userPreferences.bottomBar) {
            setContentView(R.layout.activity_main_btm)
        } else {
            setContentView(R.layout.activity_main)
        }*/

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        DynamicColors.applyIfAvailable(this)

        if (isIncognito()) {
            incognitoNotification = IncognitoNotification(this, notificationManager)
        }
        tabsManager.addTabNumberChangedListener {
            if (isIncognito()) {
                if (it == 0) {
                    incognitoNotification?.hide()
                } else {
                    incognitoNotification?.show(it)
                }
            }
        }

        presenter = BrowserPresenter(
                this,
                isIncognito(),
                userPreferences,
                tabsManager,
                mainScheduler,
                homePageFactory,
                incognitoPageFactory,
                bookmarkPageFactory,
                RecentTabModel(),
                logger
        )
        prefs = getSharedPreferences("com.cookiegames.smartcookie", MODE_PRIVATE)


        if(userPreferences.navbar) {
            setKeyboardVisibilityListener(this)
        }

        initialize(savedInstanceState)
    }

    private fun setKeyboardVisibilityListener(onKeyboardVisibilityListener: OnKeyboardVisibilityListener) {
        val parentView = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)
        parentView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private var alreadyOpen = false
            private val defaultKeyboardHeightDP = 100
            private val EstimatedKeyboardDP = defaultKeyboardHeightDP + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) 48 else 0
            private val rect: Rect = Rect()
            override fun onGlobalLayout() {
                val estimatedKeyboardHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, EstimatedKeyboardDP.toFloat(), parentView.resources.displayMetrics).toInt()
                parentView.getWindowVisibleDisplayFrame(rect)
                val heightDiff: Int = parentView.rootView.height - (rect.bottom - rect.top)
                val isShown = heightDiff >= estimatedKeyboardHeight
                if (isShown == alreadyOpen) {
                    return
                }
                alreadyOpen = isShown
                onKeyboardVisibilityListener.onVisibilityChanged(isShown)
            }
        })
    }

    override fun onVisibilityChanged(visible: Boolean) {
            val extraBar = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            if(visible){
                extraBar?.visibility = GONE
            }
            else{
                extraBar?.visibility = View.VISIBLE
            }
    }

    private fun initialize(savedInstanceState: Bundle?) {
        initializeToolbarHeight(resources.configuration)
        setSupportActionBar(binding.toolbar)
        val actionBar = requireNotNull(supportActionBar)

        if(userPreferences.translationEndpoint == "https://cookiejarapps.com/translate/"){
            userPreferences.translationEndpoint = "https://smartcookieweb.com/translate/"
        }

        //TODO make sure dark theme flag gets set correctly
        isDarkTheme = userPreferences.useTheme == AppTheme.DARK || userPreferences.useTheme == AppTheme.BLACK || isIncognito()
        shouldShowTabsInDrawer = userPreferences.showTabsInDrawer
        swapBookmarksAndTabs = userPreferences.bookmarksAndTabsSwapped

        if(!userPreferences.bottomBar){
            isFullScreen = userPreferences.fullScreenEnabled
        }

        // initialize background ColorDrawable
        val primaryColor = ThemeUtils.getPrimaryColor(this)
        backgroundDrawable.color = primaryColor

        currentTabView?.setBackgroundColor(primaryColor)
        currentTabView?.invalidate()

        // Drawer stutters otherwise
        binding.leftDrawer.setLayerType(LAYER_TYPE_NONE, null)
        binding.rightDrawer.setLayerType(LAYER_TYPE_NONE, null)

        if(userPreferences.bottomBar && userPreferences.navbar){
            val sizeInDP = 56f

            val marginInDp = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, sizeInDP, resources
                    .displayMetrics)

            val param = binding.toolbar.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(binding.toolbar.marginLeft, binding.toolbar.marginTop, binding.toolbar.marginRight, marginInDp.roundToInt())
            binding.toolbar.layoutParams = param
        }

        setNavigationDrawerWidth()
        binding.drawerLayout.addDrawerListener(DrawerLocker())

        webPageBitmap = drawable(R.drawable.ic_webpage).toBitmap()

        tabsView = if (shouldShowTabsInDrawer) {
            TabsDrawerView(this, userPreferences = userPreferences).also(findViewById<FrameLayout>(getTabsContainerId())::addView)
        } else {
            TabsDesktopView(this, userPreferences = userPreferences).also(findViewById<FrameLayout>(getTabsContainerId())::addView)
        }
        var mPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        var shouldRestoreTabs = mPrefs.getBoolean("shouldRestoreTabs", false)

        if (userPreferences.incognito) {

            WebUtils.clearHistory(this, historyModel, databaseScheduler)
            WebUtils.clearCookies(this)

            if (userPreferences.restoreLostTabsEnabled) {
                var editor = mPrefs.edit()
                editor.putBoolean("shouldRestoreTabs", true)
                editor.commit()
                userPreferences.restoreLostTabsEnabled = false
            }
        } else if (shouldRestoreTabs && userPreferences.restoreLostTabsEnabled == false && !userPreferences.incognito) {
            var editor = mPrefs.edit()
            editor.putBoolean("shouldRestoreTabs", false)
            editor.commit()
            userPreferences.restoreLostTabsEnabled = true
        }

        if(userPreferences.firstLaunch111 && userPreferences.searchSuggestionChoice == 4){
            userPreferences.searchSuggestionChoice = 5
            userPreferences.firstLaunch111 = false
        }

        bookmarksView = BookmarksDrawerView(this, this, userPreferences = userPreferences).also(findViewById<FrameLayout>(getBookmarksContainerId())::addView)

        if (shouldShowTabsInDrawer) {
            binding.tabsToolbarContainer.visibility = GONE
        }

        // set display options of the ActionBar
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.setDisplayShowHomeEnabled(false)
        actionBar.setDisplayShowCustomEnabled(true)
        toolbarBinding = ToolbarContentBinding.inflate(layoutInflater)
        actionBar.customView = toolbarBinding.root

        val customView = actionBar.customView
        customView.layoutParams = customView.layoutParams.apply {
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.MATCH_PARENT
        }

        tabCountView = customView.findViewById(R.id.tab_count_view)
        homeImageView = customView.findViewById(R.id.home_image_view)
        if (shouldShowTabsInDrawer && !isIncognito()) {
            tabCountView?.visibility = VISIBLE
            homeImageView?.visibility = GONE
        } else if (shouldShowTabsInDrawer) {
            tabCountView?.visibility = GONE
            homeImageView?.visibility = VISIBLE
            homeImageView?.setImageResource(R.drawable.incognito_mode)
            // Post drawer locking in case the activity is being recreated
            mainHandler.post { binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, getTabDrawer()) }
        } else {
            tabCountView?.visibility = GONE
            homeImageView?.visibility = VISIBLE
            homeImageView?.setImageResource(R.drawable.ic_action_home)
            // Post drawer locking in case the activity is being recreated
            mainHandler.post { binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, getTabDrawer()) }
        }

        // Post drawer locking in case the activity is being recreated
        mainHandler.post { binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, getBookmarkDrawer()) }

        customView.findViewById<FrameLayout>(R.id.home_button).setOnClickListener(this)
        customView.findViewById<FrameLayout>(R.id.home_button).setOnLongClickListener {
            newTabButtonClicked()
            true
        }

        customView.findViewById<FrameLayout>(R.id.more_button).setOnClickListener(this)
        customView.findViewById<FrameLayout>(R.id.download_button).setOnClickListener(this)

        // create the search EditText in the ToolBar
        searchView = customView.findViewById<SearchView>(R.id.search).apply {
            toolbarBinding.searchSslStatus.setOnClickListener {
                if (tabsManager.currentTab?.sslCertificate == null) {
                    val builder = MaterialAlertDialogBuilder(context)
                    builder.setTitle(R.string.site_not_secure)
                    builder.setIcon(R.drawable.ic_alert)
                    builder.setPositiveButton(R.string.action_ok) { _, _ ->
                    }
                    builder.show()
                } else {
                    tabsManager.currentTab?.let { tab ->
                        tab.sslCertificate?.let { showSslDialog(it, tab.currentSslState()) }
                    }
                }

            }
            toolbarBinding.searchSslStatus.updateVisibilityForContent()

            if (isDarkTheme) {
                toolbarBinding.searchRefresh.setImageResource(R.drawable.ic_action_refresh_light)
            } else {
                toolbarBinding.searchRefresh.setImageResource(R.drawable.ic_action_refresh)
            }

            val searchListener = SearchListenerClass()
            setOnKeyListener(searchListener)
            onFocusChangeListener = searchListener
            setOnEditorActionListener(searchListener)
            onPreFocusListener = searchListener
            addTextChangedListener(StyleRemovingTextWatcher())

            initializeSearchSuggestions(this)
        }

        toolbarBinding.searchRefresh.setOnClickListener {
            if (searchView?.hasFocus() == true) {
                searchView?.setText("")
            } else {
                refreshOrStop()
            }
        }


        searchBackground = customView.findViewById<View>(R.id.search_container).apply {
            // initialize search background color
            background.tint(getSearchBarColor(primaryColor, primaryColor))
        }

        binding.drawerLayout.setDrawerShadow(R.drawable.drawer_right_shadow, GravityCompat.END)
        binding.drawerLayout.setDrawerShadow(R.drawable.drawer_left_shadow, GravityCompat.START)

        var intent: Intent? = if (savedInstanceState == null) {
            intent
        } else {
            null
        }

        val launchedFromHistory = intent != null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0

        if (intent?.action == INTENT_PANIC_TRIGGER) {
            setIntent(null)
            panicClean()
        } else {
            if (launchedFromHistory) {
                intent = null
            }
            presenter?.setupTabs(intent)
            setIntent(null)
            proxyUtils.checkForProxy(this)

        }

        if(userPreferences.passwordChoiceLock == PasswordChoice.CUSTOM){
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)

            val customLayoutBinding = DialogAppLockBinding.inflate(layoutInflater)
            builder.setView(customLayoutBinding.root)
            builder.setCancelable(false)

            builder.setPositiveButton(resources.getString(R.string.action_ok)) { dialog, _ ->
                if (customLayoutBinding.textFieldText.text.toString() == userPreferences.passwordTextLock) {
                    dialog.cancel()
                }
                else{
                    this.finishAffinity()
                }
            }
            builder.setNegativeButton(resources.getString(R.string.action_cancel)) { _, _ ->
                this.finishAffinity()
            }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    private fun getBookmarksContainerId(): Int = if (swapBookmarksAndTabs) {
        R.id.left_drawer
    } else {
        R.id.right_drawer
    }

    private fun getTabsContainerId(): Int = if (shouldShowTabsInDrawer) {
        if (swapBookmarksAndTabs) {
            R.id.right_drawer
        } else {
            R.id.left_drawer
        }
    } else {
        R.id.tabs_toolbar_container
    }

    private fun getBookmarkDrawer(): View = if (swapBookmarksAndTabs) {
        binding.leftDrawer
    } else {
        binding.rightDrawer
    }

    private fun getTabDrawer(): View = if (swapBookmarksAndTabs) {
        binding.rightDrawer
    } else {
        binding.leftDrawer
    }

    fun openBookmarksDrawer() {
        binding.drawerLayout.openDrawer(getBookmarkDrawer())
    }

    fun openTabsDrawer() {
        binding.drawerLayout.openDrawer(getTabDrawer())
    }

    protected fun panicClean() {
        logger.log(TAG, "Closing browser")
        tabsManager.newTab(this, NoOpInitializer(), false)
        tabsManager.switchToTab(0)
        tabsManager.clearSavedState()

        historyPageFactory.deleteHistoryPage().subscribe()
        closeBrowser()
        // System exit needed in the case of receiving
        // the panic intent since finish() isn't completely
        // closing the browser
        exitProcess(1)
    }

    private inner class SearchListenerClass : OnKeyListener,
            OnEditorActionListener,
            OnFocusChangeListener,
            SearchView.PreFocusListener {

        override fun onKey(view: View, keyCode: Int, keyEvent: KeyEvent): Boolean {
            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    searchView?.let {
                        inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
                        searchTheWeb(it.text.toString())
                    }

                    tabsManager.currentTab?.requestFocus()
                    return true
                }
                else -> {
                }
            }
            return false
        }

        override fun onEditorAction(arg0: TextView, actionId: Int, arg2: KeyEvent?): Boolean {
            // hide the keyboard and search the web when the enter key
            // button is pressed
            if (actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT
                    || actionId == EditorInfo.IME_ACTION_SEND
                    || actionId == EditorInfo.IME_ACTION_SEARCH
                    || arg2?.action == KeyEvent.KEYCODE_ENTER) {
                searchView?.let {
                    inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
                    searchTheWeb(it.text.toString())
                }

                tabsManager.currentTab?.requestFocus()
                return true
            }
            return false
        }

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            val currentView = tabsManager.currentTab
            if (currentView?.url!!.contains("data:text/html;charset=utf-8")) {

            } else if (!hasFocus) {
                setIsLoading(currentView.progress < 100)
                updateUrl(currentView.url, false)
            } else if (hasFocus) {

                // Hack to make sure the text gets selected
                (v as SearchView).selectAll()
                toolbarBinding.searchSslStatus.visibility = GONE
                toolbarBinding.searchRefresh.setImageResource(R.drawable.ic_action_delete)
            }

            if (!hasFocus) {
                toolbarBinding.searchSslStatus.updateVisibilityForContent()
                searchView?.let {
                    inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
                }
            }
        }

        override fun onPreFocus() {
            val currentView = tabsManager.currentTab ?: return
            val url = currentView.url
            if (!url.isSpecialUrl()) {
                if (searchView?.hasFocus() == false) {
                    if (url.contains("data:text/html") || url == "about:blank") {

                    } else {
                        searchView?.setText(url)
                    }

                }
            }
        }
    }

    private inner class DrawerLocker : DrawerLayout.DrawerListener {

        override fun onDrawerClosed(v: View) {
            val tabsDrawer = getTabDrawer()
            val bookmarksDrawer = getBookmarkDrawer()

            if (v === tabsDrawer) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, bookmarksDrawer)
            } else if (shouldShowTabsInDrawer) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, tabsDrawer)
            }
        }

        override fun onDrawerOpened(v: View) {
            val tabsDrawer = getTabDrawer()
            val bookmarksDrawer = getBookmarkDrawer()

            if (v === tabsDrawer) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, bookmarksDrawer)
            } else {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, tabsDrawer)
            }
        }

        override fun onDrawerSlide(v: View, arg: Float) = Unit

        override fun onDrawerStateChanged(arg: Int) = Unit

    }

    private fun setNavigationDrawerWidth() {
        val width = resources.displayMetrics.widthPixels - dimen(R.dimen.navigation_drawer_minimum_space)
        val maxWidth = resources.getDimensionPixelSize(R.dimen.navigation_drawer_max_width)
        if (width < maxWidth) {
            val params = binding.leftDrawer.layoutParams as DrawerLayout.LayoutParams
            params.width = width
            binding.leftDrawer.layoutParams = params
            binding.leftDrawer.requestLayout()
            val paramsRight = binding.rightDrawer.layoutParams as DrawerLayout.LayoutParams
            paramsRight.width = width
            binding.rightDrawer.layoutParams = paramsRight
            binding.rightDrawer.requestLayout()
        }
    }

    private fun initializePreferences() {
        val currentView = tabsManager.currentTab
        val extraBar = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if(isDarkTheme && userPreferences.navbar){
            extraBar.setBackgroundColor(resources.getColor(R.color.black))
        }

        webPageBitmap?.let { webBitmap ->
            if (!isIncognito() && !isColorMode() && !isDarkTheme) {
                changeToolbarBackground(webBitmap, null)
            } else if (!isIncognito() && currentView != null && !isDarkTheme) {
                changeToolbarBackground(currentView.favicon ?: webBitmap, null)
            } else if (!isIncognito() && !isDarkTheme) {
                changeToolbarBackground(webBitmap, null)
            } else if (userPreferences.navbarColChoice == ChooseNavbarCol.COLOR && !isIncognito()) {
                changeToolbarColor(null)
            }
        }

        // TODO layout transition causing memory leak
        //        content_frame.setLayoutTransition(new LayoutTransition());

        setFullscreen(userPreferences.hideStatusBarEnabled, false)

        val currentSearchEngine = searchEngineProvider.provideSearchEngine()
        searchText = currentSearchEngine.queryUrl

        updateCookiePreference().subscribeOn(diskScheduler).subscribe()
        proxyUtils.updateProxySettings(this)
        if(!userPreferences.navbar){
            extraBar.visibility = GONE
        }
        else{
            extraBar.visibility = VISIBLE
            if(!userPreferences.showTabsInDrawer){
                extraBar.menu.removeItem(R.id.tabs)
            }
            extraBar.setOnNavigationItemSelectedListener { item ->
                when(item.itemId) {
                    R.id.tabs -> {
                        binding.drawerLayout.closeDrawer(getBookmarkDrawer())
                        toggleDrawer(binding.drawerLayout, getTabDrawer())
                        true
                    }
                    R.id.bookmarks -> {
                        binding.drawerLayout.closeDrawer(getTabDrawer())
                        toggleDrawer(binding.drawerLayout, getBookmarkDrawer())
                        true
                    }
                    R.id.forward -> {
                        tabsManager.currentTab?.goForward()
                        true
                    }
                    R.id.back -> {
                        tabsManager.currentTab?.goBack()
                        true
                    }
                    R.id.home -> {
                        tabsManager.currentTab?.loadHomePage()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun toggleDrawer(layout: DrawerLayout, drawer: View) {
        if(layout.isDrawerOpen(drawer)){
            layout.closeDrawer(drawer)
        }
        else{
            layout.openDrawer(drawer)
        }
    }

    public override fun onWindowVisibleToUserAfterResume() {
        super.onWindowVisibleToUserAfterResume()
        binding.toolbarLayout.translationY = 0f
        setWebViewTranslation(binding.toolbarLayout.height.toFloat())
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (searchView?.hasFocus() == true) {
                searchView?.let { searchTheWeb(it.text.toString()) }
            }
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            keyDownStartTime = System.currentTimeMillis()
            mainHandler.postDelayed(longPressBackRunnable, ViewConfiguration.getLongPressTimeout().toLong())
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mainHandler.removeCallbacks(longPressBackRunnable)
            if (System.currentTimeMillis() - keyDownStartTime > ViewConfiguration.getLongPressTimeout()) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Keyboard shortcuts
        if (event.action == KeyEvent.ACTION_DOWN) {
            when {
                event.isCtrlPressed -> when (event.keyCode) {
                    KeyEvent.KEYCODE_P -> {
                        // Print
                        tabsManager.currentTab?.webView?.let { tabsManager.currentTab?.createWebPagePrint(it) }
                        return true
                    }
                    KeyEvent.KEYCODE_F -> {
                        // Search in page
                        findInPage()
                        return true
                    }
                    KeyEvent.KEYCODE_T -> {
                        // Open new tab
                        presenter?.newTab(
                                homePageInitializer,
                                true
                        )
                        return true
                    }
                    KeyEvent.KEYCODE_W -> {
                        // Close current tab
                        tabsManager.let { presenter?.deleteTab(it.indexOfCurrentTab()) }
                        return true
                    }
                    KeyEvent.KEYCODE_Q -> {
                        // Close browser
                        closeBrowser()
                        return true
                    }
                    KeyEvent.KEYCODE_H -> {
                        // History
                        tabsManager.currentTab?.loadHistoryPage()
                        return true
                    }
                    KeyEvent.KEYCODE_B -> {
                        // History
                        addBookmark(tabsManager.currentTab!!.title, tabsManager.currentTab!!.url)
                        return true
                    }
                    KeyEvent.KEYCODE_S -> {
                        IntentUtils(this).shareUrl(tabsManager.currentTab?.url, tabsManager.currentTab?.title)
                        return true
                    }
                    KeyEvent.KEYCODE_R -> {
                        // Refresh current tab
                        tabsManager.currentTab?.reload()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (binding.drawerLayout.isDrawerOpen(getBookmarkDrawer())) {
                            binding.drawerLayout.closeDrawers()
                        }
                        binding.drawerLayout.openDrawer(getTabDrawer())
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (binding.drawerLayout.isDrawerOpen(getTabDrawer())) {
                            binding.drawerLayout.closeDrawers()
                        }
                        binding.drawerLayout.openDrawer(getBookmarkDrawer())
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        tabsManager.currentTab?.goBack()
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        tabsManager.currentTab?.goForward()
                    }
                    KeyEvent.KEYCODE_TAB -> {
                        tabsManager.let {
                            val nextIndex = if (event.isShiftPressed) {
                                // Go back one tab
                                if (it.indexOfCurrentTab() > 0) {
                                    it.indexOfCurrentTab() - 1
                                } else {
                                    it.last()
                                }
                            } else {
                                // Go forward one tab
                                if (it.indexOfCurrentTab() < it.last()) {
                                    it.indexOfCurrentTab() + 1
                                } else {
                                    0
                                }
                            }

                            presenter?.tabChanged(nextIndex)
                        }

                        return true
                    }
                }
                event.isAltPressed -> // Alt + tab number
                    tabsManager.let {
                        if (KeyEvent.KEYCODE_0 <= event.keyCode && event.keyCode <= KeyEvent.KEYCODE_9) {
                            val nextIndex = if (event.keyCode > it.last() + KeyEvent.KEYCODE_1 || event.keyCode == KeyEvent.KEYCODE_0) {
                                it.last()
                            } else {
                                event.keyCode - KeyEvent.KEYCODE_1
                            }
                            presenter?.tabChanged(nextIndex)
                            return true
                        }
                    }
                else -> when (event.keyCode) {
                    KeyEvent.KEYCODE_SEARCH -> {
                        // Highlight search field
                        searchView?.requestFocus()
                        searchView?.selectAll()
                        return true
                    }
                    KeyEvent.KEYCODE_F5 -> {
                        tabsManager.currentTab?.reload()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // By using a manager, adds a bookmark and notifies third parties about that
    private fun addBookmark(title: String, url: String) {
        val bookmark = Bookmark.Entry(url, title, 0, Bookmark.Folder.Root)
        bookmarksDialogBuilder.showAddBookmarkDialog(this, this, bookmark)
    }

    private fun deleteBookmark(title: String, url: String) {
        bookmarkManager.deleteBookmark(Bookmark.Entry(url, title, 0, Bookmark.Folder.Root))
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribe { boolean ->
                    if (boolean) {
                        handleBookmarksChange()
                    }
                }
    }

    private fun putToolbarInRoot() {
        if (binding.toolbarLayout.parent != binding.uiLayout) {
            (binding.toolbarLayout.parent as ViewGroup?)?.removeView(binding.toolbarLayout)

            binding.uiLayout.addView(binding.toolbarLayout, 0)
            binding.uiLayout.requestLayout()
        }

        setWebViewTranslation(0f)
    }

    private fun overlayToolbarOnWebView() {
        if (binding.toolbarLayout.parent != binding.contentFrame) {
            (binding.toolbarLayout.parent as ViewGroup?)?.removeView(binding.toolbarLayout)

            binding.contentFrame.addView(binding.toolbarLayout)
            binding.contentFrame.requestLayout()
        }
        setWebViewTranslation(binding.toolbarLayout.height.toFloat())


    }

    private fun setWebViewTranslation(translation: Float) =
            if (isFullScreen) {
                currentTabView?.translationY = translation
            } else {
                currentTabView?.translationY = 0f
            }


    /**
     * method that shows a dialog asking what string the user wishes to search
     * for. It highlights the text entered.
     */
    public fun findInPage() = BrowserDialog.showEditText(
            this,
            R.string.action_find,
            R.string.action_find,
            R.string.action_find
    ) { text ->
        if (text.isNotEmpty()) {
            findResult = presenter?.findInPage(text)
            showFindInPageControls(text)
        }
    }

    private fun showFindInPageControls(text: String) {
        binding.searchBar.visibility = VISIBLE

        findViewById<TextView>(R.id.search_query).text = text
        findViewById<ImageButton>(R.id.button_next).setOnClickListener(this)
        findViewById<ImageButton>(R.id.button_back).setOnClickListener(this)
        findViewById<ImageButton>(R.id.button_quit).setOnClickListener(this)
        findViewById<ImageButton>(R.id.button_search).setOnClickListener(this)
    }

    override fun isColorMode(): Boolean = userPreferences.colorModeEnabled && !isDarkTheme

    override fun getTabModel(): TabsManager = tabsManager

    override fun showCloseDialog(position: Int) {
        if (position < 0) {
            return
        }
        BrowserDialog.showWithIcons(this, getString(R.string.dialog_title_close_browser),
                DialogItem(title = R.string.close_tab, icon = drawable(R.drawable.ic_delete_this)) {
                    presenter?.deleteTab(position)
                },
                DialogItem(title = R.string.close_other_tabs, icon = drawable(R.drawable.ic_delete_other)) {
                    presenter?.closeAllOtherTabs()
                },
                DialogItem(title = R.string.close_all_tabs, icon = drawable(R.drawable.ic_delete_all), onClick = this::closeBrowser),
                DialogItem(title = R.string.close_app, icon = drawable(R.drawable.ic_action_delete), onClick = this::closeApp))


    }

    override fun notifyTabViewRemoved(position: Int) {
        logger.log(TAG, "Notify Tab Removed: $position")
        tabsView?.tabRemoved(position)
    }

    override fun notifyTabViewAdded() {
        logger.log(TAG, "Notify Tab Added")
        tabsView?.tabAdded()
    }

    override fun notifyTabViewChanged(position: Int) {
        logger.log(TAG, "Notify Tab Changed: $position")
        tabsView?.tabChanged(position)
    }

    override fun notifyTabViewInitialized() {
        logger.log(TAG, "Notify Tabs Initialized")
        tabsView?.tabsInitialized()

    }

    override fun updateSslState(sslState: SslState) {
        val currentTab = tabsManager.currentTab
        val url = currentTab?.url
        if(url!!.contains("http://") || url.contains("https://")){
            toolbarBinding.searchSslStatus.setImageDrawable(createSslDrawableForState(sslState))
        }
        else{
            toolbarBinding.searchSslStatus.setImageDrawable(null)
        }


        if (searchView?.hasFocus() == false) {
            toolbarBinding.searchSslStatus.updateVisibilityForContent()
        }
    }

    private fun ImageView.updateVisibilityForContent() {
        drawable?.let { visibility = VISIBLE } ?: run { visibility = GONE }
    }

    override fun tabChanged(tab: SmartCookieView) {
        presenter?.tabChangeOccurred(tab)
    }

    override fun removeTabView() {

        logger.log(TAG, "Remove the tab view")

        currentTabView.removeFromParent()

        currentTabView = null

        // Use a delayed handler to make the transition smooth
        // otherwise it will get caught up with the showTab code
        // and cause a janky motion
        mainHandler.postDelayed(binding.drawerLayout::closeDrawers, 200)

    }
    override fun setTabView(view: View) {
        if (currentTabView == view) {
            return
        }

        logger.log(TAG, "Setting the tab view")

        view.removeFromParent()
        currentTabView.removeFromParent()

        binding.contentFrame.addView(view, 0, MATCH_PARENT)

         if(isFullScreen) {
            view.translationY = binding.toolbarLayout.height + binding.toolbarLayout.translationY
        }
        else {
         view.translationY = 0f
       }

        val displayMetrics =  DisplayMetrics()
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels

        currentTabView = view

        view.requestFocus()

        showActionBar()

        // Use a delayed handler to make the transition smooth
        // otherwise it will get caught up with the showTab code
        // and cause a janky motion
        mainHandler.postDelayed(binding.drawerLayout::closeDrawers, 200)
    }

    override fun showBlockedLocalFileDialog(onPositiveClick: Function0<Unit>) {
        MaterialAlertDialogBuilder(this)
                .setCancelable(true)
                .setTitle(R.string.title_warning)
                .setMessage(R.string.message_blocked_local)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_open) { _, _ -> onPositiveClick.invoke() }
                .resizeAndShow()
    }

    override fun showSnackbar(@StringRes resource: Int) = snackbar(resource)

    override fun tabCloseClicked(position: Int) {
        presenter?.deleteTab(position)
    }

    override fun tabClicked(position: Int) {
        presenter?.tabChanged(position)
    }

    override fun newTabButtonClicked() {
        if(userPreferences.tabsToForegroundEnabled){
            if(isIncognito()){
                presenter?.newTab(
                        incognitoPageInitializer,
                        true
                )
            }
            else{
                presenter?.newTab(
                        homePageInitializer,
                        true
                )
            }
        }
        else{
            if(isIncognito()){
                presenter?.newTab(
                        incognitoPageInitializer,
                        false
                )
            }
            else{
                presenter?.newTab(
                        homePageInitializer,
                        false
                )
            }
        }
    }

    override fun newTabButtonLongClicked() {
        presenter?.onNewTabLongClicked()
    }

    override fun bookmarkButtonClicked() {
        val currentTab = tabsManager.currentTab
        val url = currentTab?.url
        val title = currentTab?.title
        if (url == null || title == null) {
            return
        }

        if (!url.isSpecialUrl()) {
            bookmarkManager.isBookmark(url)
                    .subscribeOn(databaseScheduler)
                    .observeOn(mainScheduler)
                    .subscribe { boolean ->
                        if (boolean) {
                            MaterialAlertDialogBuilder(this)
                                    .setCancelable(true)
                                    .setTitle(R.string.bookmark_delete)
                                    .setNegativeButton(android.R.string.no, null)
                                    .setPositiveButton(R.string.yes) { _, _ ->
                                        deleteBookmark(title, url)
                                    }
                                    .resizeAndShow()
                        } else {
                            addBookmark(title, url)
                        }
                    }
        }
    }

    override fun bookmarkItemClicked(entry: Bookmark.Entry) {
        presenter?.loadUrlInCurrentView(entry.url)
        // keep any jank from happening when the drawer is closed after the URL starts to load
        mainHandler.postDelayed({ closeDrawers(null) }, 150)
    }

    override fun handleHistoryChange() {
        historyPageFactory
                .buildPage()
                .subscribeOn(databaseScheduler)
                .observeOn(mainScheduler)
                .subscribeBy(onSuccess = { tabsManager.currentTab?.reload() })
    }

    protected fun handleNewIntent(intent: Intent) {
        presenter?.onNewIntent(intent)
    }

    protected fun performExitCleanUp() {
        val currentTab = tabsManager.currentTab
        if (userPreferences.clearCacheExit && currentTab != null && !isIncognito()) {
            WebUtils.clearCache(currentTab.webView, applicationContext)
            logger.log(TAG, "Cache Cleared")
        }
        if (userPreferences.clearHistoryExitEnabled && !isIncognito()) {
            WebUtils.clearHistory(this, historyModel, databaseScheduler)
            logger.log(TAG, "History Cleared")
        }
        if (userPreferences.clearCookiesExitEnabled && !isIncognito()) {
            WebUtils.clearCookies(this)
            logger.log(TAG, "Cookies Cleared")
        }
        if (userPreferences.clearWebStorageExitEnabled && !isIncognito()) {
            WebUtils.eraseWebStorage(applicationContext)
            logger.log(TAG, "WebStorage Cleared")
        } else if (isIncognito()) {
            WebUtils.clearWebStorage()     // We want to make sure incognito mode is secure
        }
    }

    fun getScreenSize(): Int{
        var display: Display = getWindowManager().getDefaultDisplay()
        var size: Point = Point()
        var extra = 0
        var height = 0
        var barHeight = 0
        display.getSize(size)
        Log.d("displayMetrics", size.y.toString())
        var resources = getResources()
        var resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            height= resources.getDimensionPixelSize(resourceId)
        }

        if(userPreferences.showTabsInDrawer){
            barHeight = 48
        }
        else{
            barHeight = 72
        }

        return size.y - barHeight - height
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        logger.log(TAG, "onConfigurationChanged")

        if (isFullScreen) {
            showActionBar()

            setWebViewTranslation(binding.toolbarLayout.height.toFloat())

        }

        invalidateOptionsMenu()
        initializeToolbarHeight(newConfig)
    }

    private fun initializeToolbarHeight(configuration: Configuration) =
            binding.uiLayout.doOnLayout {
                // TODO externalize the dimensions
                val toolbarSize = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    R.dimen.toolbar_height_portrait
                } else {
                    R.dimen.toolbar_height_landscape
                }

                binding.toolbar.layoutParams = (binding.toolbar.layoutParams as LinearLayout.LayoutParams).apply {
                    height = dimen(toolbarSize)
                }
                binding.toolbar.minimumHeight = toolbarSize
                binding.toolbar.doOnLayout {
                    setWebViewTranslation(binding.toolbarLayout.height.toFloat())

                }
                binding.toolbar.requestLayout()
            }

    override fun closeBrowser() {
        currentTabView.removeFromParent()
        performExitCleanUp()
        val size = tabsManager.size()
        tabsManager.shutdown()
        currentTabView = null
        for (n in 0 until size) {
            tabsView?.tabRemoved(0)
        }
        finish()
    }

    fun closeApp(){
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(a)
    }
    override fun onBackPressed() {
        val currentTab = tabsManager.currentTab
        if (binding.drawerLayout.isDrawerOpen(getTabDrawer())) {
            binding.drawerLayout.closeDrawer(getTabDrawer())
        } else if (binding.drawerLayout.isDrawerOpen(getBookmarkDrawer())) {
            bookmarksView?.navigateBack()
        } else {
            if (currentTab != null) {
                logger.log(TAG, "onBackPressed")
                if (searchView?.hasFocus() == true) {
                    currentTab.requestFocus()
                } else if (currentTab.canGoBack()) {
                    if (!currentTab.isShown) {
                        onHideCustomView()
                    } else {
                        currentTab.goBack()
                    }
                } else {
                    if (customView != null || customViewCallback != null) {
                        onHideCustomView()
                    } else {
                        presenter?.deleteTab(tabsManager.positionOf(currentTab), true)
                    }
                }
            } else {
                logger.log(TAG, "This shouldn't happen ever")
                super.onBackPressed()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        logger.log(TAG, "onPause")
        tabsManager.pauseAll()

        if (isIncognito() && isFinishing) {
            overridePendingTransition(R.anim.fade_in_scale, R.anim.slide_down_out)
        }
    }

    protected fun saveOpenTabs() {
        if (userPreferences.restoreLostTabsEnabled) {
            tabsManager.saveState()
        }
    }

    override fun onStop() {
        super.onStop()
        proxyUtils.onStop()
    }

    override fun onDestroy() {
        logger.log(TAG, "onDestroy")

        incognitoNotification?.hide()

        mainHandler.removeCallbacksAndMessages(null)

        presenter?.shutdown()

        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        proxyUtils.onStart(this)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        tabsManager.shutdown()
    }

    override fun onResume() {
        super.onResume()
        logger.log(TAG, "onResume")
        if (swapBookmarksAndTabs != userPreferences.bookmarksAndTabsSwapped) {
            restart()
        }

        suggestionsAdapter?.let {
            it.refreshPreferences()
            it.refreshBookmarks()
        }
        tabsManager.resumeAll()
        initializePreferences()


        if(!userPreferences.bottomBar){
            isFullScreen = userPreferences.fullScreenEnabled
        }

        if (isFullScreen) {
            overlayToolbarOnWebView()
        } else {
            putToolbarInRoot()
        }

        if(userPreferences.incognito){
            WebUtils.clearHistory(this, historyModel, databaseScheduler)
            WebUtils.clearCookies(this)
        }

        changeToolbarColor(null)

        if(!isIncognito() && !userPreferences.onlyForceClose){
            performExitCleanUp()
        }

        handleBookmarksChange()
    }

    /**
     * searches the web for the query fixing any and all problems with the input
     * checks if it is a search, url, etc.
     */
    private fun searchTheWeb(query: String) {
        val currentTab = tabsManager.currentTab
        if (query.isEmpty()) {
            return
        }
        val searchUrl = "$searchText$QUERY_PLACE_HOLDER"
        if (currentTab != null) {
            currentTab.stopLoading()
            presenter?.loadUrlInCurrentView(smartUrlFilter(query.trim(), true, searchUrl))
        }
    }

    private fun loadState(above: Int) = tabsManager.currentTab?.let { it.progress >= above } ?: false

    override fun changeToolbarColor(tabBackground: Drawable?){
        val primaryColor = ThemeUtils.getColorBackground(this)

        if(userPreferences.darkModeExtension) currentTabView?.setBackgroundColor(if (loadState(50)) Color.WHITE else primaryColor); currentTabView?.invalidate()

        if(userPreferences.navbarColChoice == ChooseNavbarCol.COLOR && !isIncognito()){
            tabBackground?.tint(userPreferences.colorNavbar)
            binding.toolbarLayout.setBackgroundColor(userPreferences.colorNavbar)
            currentUiColor = userPreferences.colorNavbar

            searchBackground?.background?.tint(
                    Utils.mixTwoColors(Color.WHITE, userPreferences.colorNavbar, 0.25f)
            )

            backgroundDrawable.color = userPreferences.colorNavbar

            window.setBackgroundDrawable(backgroundDrawable)

        } //Reset theme without needing a restart
        else if(userPreferences.navbarColChoice == ChooseNavbarCol.NONE){
            var currentColor = Color.WHITE
            if(userPreferences.useTheme == AppTheme.LIGHT && !isIncognito()){
                currentColor = ContextCompat.getColor(this, R.color.primary_color)
            }
            else if(userPreferences.useTheme == AppTheme.DARK || isIncognito()){
                currentColor = ContextCompat.getColor(this, R.color.primary_color_dark)
            }
            else{
                currentColor = ContextCompat.getColor(this, R.color.black)
            }
            changeToolbarBackground(null, null)
            tabBackground?.tint(currentColor)
            binding.toolbarLayout.setBackgroundColor(currentColor)
            searchBackground?.background?.tint(
                    Utils.mixTwoColors(Color.WHITE, currentColor, 0.25f)
            )
        }
    }

    /**
     * Animates the color of the toolbar from one color to another. Optionally animates
     * the color of the tab background, for use when the tabs are displayed on the top
     * of the screen.
     *
     * @param favicon the Bitmap to extract the color from
     * @param tabBackground the optional LinearLayout to color
     */
    override fun changeToolbarBackground(favicon: Bitmap?, tabBackground: Drawable?) {
        if (!isColorMode()) {
            return
        }
        val defaultColor = ContextCompat.getColor(this, R.color.primary_color)
        if (currentUiColor == Color.BLACK) {
            currentUiColor = defaultColor
        }
        Palette.from(favicon ?: webPageBitmap!!).generate { palette ->
            // OR with opaque black to remove transparency glitches
            val color = Color.BLACK or (palette?.getVibrantColor(defaultColor) ?: defaultColor)

            // Lighten up the dark color if it is too dark
            val finalColor = if (!shouldShowTabsInDrawer || Utils.isColorTooDark(color)) {
                Utils.mixTwoColors(defaultColor, color, 0.25f)
            } else {
                color
            }

            val window = window
            if (!shouldShowTabsInDrawer) {
                window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
            }

            val startSearchColor = getSearchBarColor(currentUiColor, defaultColor)
            val finalSearchColor = getSearchBarColor(finalColor, defaultColor)

            val animation = object : Animation() {
                override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    val animatedColor = DrawableUtils.mixColor(interpolatedTime, currentUiColor, finalColor)
                    if (shouldShowTabsInDrawer) {
                        backgroundDrawable.color = animatedColor
                        mainHandler.post { window.setBackgroundDrawable(backgroundDrawable) }
                    } else {
                        tabBackground?.tint(animatedColor)
                    }
                    currentUiColor = animatedColor
                    binding.toolbarLayout.setBackgroundColor(animatedColor)
                    searchBackground?.background?.tint(
                            DrawableUtils.mixColor(interpolatedTime, startSearchColor, finalSearchColor)
                    )
                }
            }
            animation.duration = 300
            binding.toolbarLayout.startAnimation(animation)
        }
    }

    private fun getSearchBarColor(requestedColor: Int, defaultColor: Int): Int =
            if (requestedColor == defaultColor) {
                if (isDarkTheme) DrawableUtils.mixColor(0.25f, defaultColor, Color.WHITE) else Color.WHITE
            } else {
                DrawableUtils.mixColor(0.25f, requestedColor, Color.WHITE)
            }

    @ColorInt
    override fun getUiColor(): Int = currentUiColor

    override fun updateUrl(url: String?, isLoading: Boolean) {
        if(!isIncognito()){
            saveOpenTabs()
        }
        if (url == null || searchView?.hasFocus() != false) {
            return
        }
        val currentTab = tabsManager.currentTab
        bookmarksView?.handleUpdatedUrl(url)

        val currentTitle = currentTab?.title

        searchView?.setText(searchBoxModel.getDisplayContent(url, currentTitle, isLoading))
    }

    override fun updateTabNumber(number: Int) {
        if (shouldShowTabsInDrawer && !isIncognito()) {
            tabCountView?.updateCount(number)
        }
    }

    override fun updateProgress(progress: Int) {
        setIsLoading(progress < 100)
        binding.progressView.progress = progress
    }

    protected fun addItemToHistory(title: String?, url: String) {
        if (url.isSpecialUrl()) {
            return
        }

        historyModel.visitHistoryEntry(url, title)
                .subscribeOn(databaseScheduler)
                .subscribe()
    }

    /**
     * method to generate search suggestions for the AutoCompleteTextView from
     * previously searched URLs
     */
    private fun initializeSearchSuggestions(getUrl: AutoCompleteTextView) {
        suggestionsAdapter = SuggestionsAdapter(this, isIncognito())
        suggestionsAdapter?.onInsertClicked = {
            when(it){
                is SearchSuggestion -> {
                    getUrl.setText(it.title)
                    getUrl.setSelection(it.title.length)
                }
                else -> {
                    getUrl.setText(it.url)
                    getUrl.setSelection(it.url.length)
                }
            }

        }
        getUrl.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            val url = when (val selection = suggestionsAdapter?.getItem(position) as WebPage) {
                is HistoryEntry,
                is Bookmark.Entry -> selection.url
                is SearchSuggestion -> selection.title
                else -> null
            } ?: return@OnItemClickListener
            getUrl.setText(url)
            searchTheWeb(url)
            inputMethodManager.hideSoftInputFromWindow(getUrl.windowToken, 0)
            presenter?.onAutoCompleteItemPressed()
        }

        getUrl.setAdapter(suggestionsAdapter)
    }

    /**
     * function that opens the HTML history page in the browser
     */
    fun openHistory() {
        presenter?.newTab(
                historyPageInitializer,
                true
        )
    }

    fun openDownloads() {
        presenter?.newTab(
                downloadPageInitializer,
                true
        )
    }

    /**
     * helper function that opens the bookmark drawer
     */
    private fun openBookmarks() {
        if (binding.drawerLayout.isDrawerOpen(getTabDrawer())) {
            binding.drawerLayout.closeDrawers()
        }
        binding.drawerLayout.openDrawer(getBookmarkDrawer())
    }

    /**
     * This method closes any open drawer and executes the runnable after the drawers are closed.
     *
     * @param runnable an optional runnable to run after the drawers are closed.
     */
    protected fun closeDrawers(runnable: (() -> Unit)?) {
        if (!binding.drawerLayout.isDrawerOpen(binding.leftDrawer) && !binding.drawerLayout.isDrawerOpen(binding.rightDrawer)) {
            if (runnable != null) {
                runnable()
                return
            }
        }
        binding.drawerLayout.closeDrawers()

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) {
                runnable?.invoke()
                binding.drawerLayout.removeDrawerListener(this)
            }

            override fun onDrawerStateChanged(newState: Int) = Unit
        })
    }

    override fun setForwardButtonEnabled(enabled: Boolean) {
        forwardMenuItem?.isEnabled = enabled
        tabsView?.setGoForwardEnabled(enabled)
    }

    override fun setBackButtonEnabled(enabled: Boolean) {
        backMenuItem?.isEnabled = enabled
        tabsView?.setGoBackEnabled(enabled)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        backMenuItem = menu.findItem(R.id.action_back)
        forwardMenuItem = menu.findItem(R.id.action_forward)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * opens a file chooser
     * param ValueCallback is the message from the WebView indicating a file chooser
     * should be opened
     */
    override fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
        uploadMessageCallback = uploadMsg
        startActivityForResult(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }, getString(R.string.title_file_chooser)), FILE_CHOOSER_REQUEST_CODE)
    }

    /**
     * used to allow uploading into the browser
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                val result = if (intent == null || resultCode != Activity.RESULT_OK) {
                    null
                } else {
                    intent.data
                }

                uploadMessageCallback?.onReceiveValue(result)
                uploadMessageCallback = null
            } else {
                val results: Array<Uri>? = if (resultCode == Activity.RESULT_OK) {
                    if (intent == null) {
                        // If there is not data, then we may have taken a photo
                        cameraPhotoPath?.let { arrayOf(it.toUri()) }
                    } else {
                        intent.dataString?.let { arrayOf(it.toUri()) }
                    }
                } else {
                    null
                }

                filePathCallback?.onReceiveValue(results)
                filePathCallback = null
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent)
        }
    }

    override fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>) {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        // Create the File where the photo should go
        val intentArray: Array<Intent> = try {
            arrayOf(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra("PhotoPath", cameraPhotoPath)
                putExtra(
                        MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(Utils.createImageFile().also { file ->
                            cameraPhotoPath = "file:${file.absolutePath}"
                        })
                )
            })
        } catch (ex: IOException) {
            // Error occurred while creating the File
            logger.log(TAG, "Unable to create Image File", ex)
            emptyArray()
        }

        startActivityForResult(Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            })
            putExtra(Intent.EXTRA_TITLE, "Image Chooser")
            putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
        }, FILE_CHOOSER_REQUEST_CODE)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback, requestedOrientation: Int) {
        val currentTab = tabsManager.currentTab
        if (customView != null) {
            try {
                callback.onCustomViewHidden()
            } catch (e: Exception) {
                logger.log(TAG, "Error hiding custom view", e)
            }

            return
        }

        try {
            view.keepScreenOn = true
        } catch (e: SecurityException) {
            logger.log(TAG, "WebView is not allowed to keep the screen on")
        }

        originalOrientation = getRequestedOrientation()
        customViewCallback = callback
        customView = view

        //setRequestedOrientation(requestedOrientation)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR)
        val decorView = window.decorView as FrameLayout

        fullscreenContainerView = FrameLayout(this)
        fullscreenContainerView?.setBackgroundColor(ContextCompat.getColor(this, R.color.black))
        if (view is FrameLayout) {
            val child = view.focusedChild
            if (child is VideoView) {
                videoView = child
                child.setOnErrorListener(VideoCompletionListener())
                child.setOnCompletionListener(VideoCompletionListener())
            }
        } else if (view is VideoView) {
            videoView = view
            view.setOnErrorListener(VideoCompletionListener())
            view.setOnCompletionListener(VideoCompletionListener())
        }
        decorView.addView(fullscreenContainerView, COVER_SCREEN_PARAMS)
        fullscreenContainerView?.addView(customView, COVER_SCREEN_PARAMS)
        decorView.requestLayout()
        setFullscreen(enabled = true, immersive = true)
        currentTab?.setVisibility(INVISIBLE)
    }

    override fun onHideCustomView() {
        val currentTab = tabsManager.currentTab
        if (customView == null || customViewCallback == null || currentTab == null) {
            if (customViewCallback != null) {
                try {
                    customViewCallback?.onCustomViewHidden()
                } catch (e: Exception) {
                    logger.log(TAG, "Error hiding custom view", e)
                }

                customViewCallback = null
            }
            return
        }
        logger.log(TAG, "onHideCustomView")
        currentTab.setVisibility(VISIBLE)
        try {
            customView?.keepScreenOn = false
        } catch (e: SecurityException) {
            logger.log(TAG, "WebView is not allowed to keep the screen on")
        }

        setFullscreen(userPreferences.hideStatusBarEnabled, false)
        if (fullscreenContainerView != null) {
            val parent = fullscreenContainerView?.parent as ViewGroup
            parent.removeView(fullscreenContainerView)
            fullscreenContainerView?.removeAllViews()
        }

        fullscreenContainerView = null
        customView = null

        logger.log(TAG, "VideoView is being stopped")
        videoView?.stopPlayback()
        videoView?.setOnErrorListener(null)
        videoView?.setOnCompletionListener(null)
        videoView = null

        try {
            customViewCallback?.onCustomViewHidden()
        } catch (e: Exception) {
            logger.log(TAG, "Error hiding custom view", e)
        }

        customViewCallback = null
        requestedOrientation = originalOrientation
    }

    private inner class VideoCompletionListener : MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean = false

        override fun onCompletion(mp: MediaPlayer) = onHideCustomView()

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        logger.log(TAG, "onWindowFocusChanged")
        if (hasFocus) {
            setFullscreen(hideStatusBar, isImmersiveMode)
        }
    }

    override fun onBackButtonPressed() {
        if (binding.drawerLayout.closeDrawerIfOpen(getTabDrawer())) {
            val currentTab = tabsManager.currentTab
            if (currentTab?.canGoBack() == true) {
                currentTab.goBack()
            } else if (currentTab != null) {
                tabsManager.let { presenter?.deleteTab(it.positionOf(currentTab), true) }
            }
        } else if (binding.drawerLayout.closeDrawerIfOpen(getBookmarkDrawer())) {
            // Don't do anything other than close the bookmarks drawer when the activity is being
            // delegated to.
            return
        }
    }

    override fun onForwardButtonPressed() {
        val currentTab = tabsManager.currentTab
        if (currentTab?.canGoForward() == true) {
            currentTab.goForward()
            closeDrawers(null)
        }
    }

    override fun onHomeButtonPressed() {
        tabsManager.currentTab?.loadHomePage()
        closeDrawers(null)
    }

    /**
     * This method sets whether or not the activity will display
     * in full-screen mode (i.e. the ActionBar will be hidden) and
     * whether or not immersive mode should be set. This is used to
     * set both parameters correctly as during a full-screen video,
     * both need to be set, but other-wise we leave it up to user
     * preference.
     *
     * @param enabled   true to enable full-screen, false otherwise
     * @param immersive true to enable immersive mode, false otherwise
     */
    private fun setFullscreen(enabled: Boolean, immersive: Boolean) {
        hideStatusBar = enabled
        isImmersiveMode = immersive
        val window = window
        val decor = window.decorView
        if (enabled) {
            if (immersive) {
                decor.systemUiVisibility = (SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or SYSTEM_UI_FLAG_FULLSCREEN
                        or SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            } else {
                decor.systemUiVisibility = SYSTEM_UI_FLAG_VISIBLE
            }
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decor.systemUiVisibility = SYSTEM_UI_FLAG_VISIBLE
        }
    }

    /**
     * This method handles the JavaScript callback to create a new tab.
     * Basically this handles the event that JavaScript needs to create
     * a popup.
     *
     * @param resultMsg the transport message used to send the URL to
     * the newly created WebView.
     */
    override fun onCreateWindow(resultMsg: Message) {
        presenter?.newTab(ResultMessageInitializer(resultMsg), true)
    }

    /**
     * Closes the specified [SmartCookieView]. This implements
     * the JavaScript callback that asks the tab to close itself and
     * is especially helpful when a page creates a redirect and does
     * not need the tab to stay open any longer.
     *
     * @param tab the LightningView to close, delete it.
     */
    override fun onCloseWindow(tab: SmartCookieView) {
        presenter?.deleteTab(tabsManager.positionOf(tab))
    }

    /**
     * Hide the ActionBar using an animation if we are in full-screen
     * mode. This method also re-parents the ActionBar if its parent is
     * incorrect so that the animation can happen correctly.
     */
    override fun hideActionBar() {
        if (isFullScreen) {
            if (binding.toolbarLayout == null || binding.contentFrame == null)
                return

            val height = binding.toolbarLayout.height

            if (binding.toolbarLayout.translationY > -0.01f) {
                val hideAnimation = object : Animation() {
                    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {

                        val trans = interpolatedTime * height
                        binding.toolbarLayout.translationY = -trans
                        setWebViewTranslation(height - trans)

                    }
                }
                hideAnimation.duration = 250
                hideAnimation.interpolator = BezierDecelerateInterpolator()
                binding.contentFrame.startAnimation(hideAnimation)
            }
        }
    }

    /**
     * Display the ActionBar using an animation if we are in full-screen
     * mode. This method also re-parents the ActionBar if its parent is
     * incorrect so that the animation can happen correctly.
     */
    override fun showActionBar() {
        if (isFullScreen) {
            logger.log(TAG, "showActionBar")
            if (binding.toolbarLayout == null)
                return

            var height = binding.toolbarLayout.height
            if (height == 0) {
                binding.toolbarLayout.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
                height = binding.toolbarLayout.measuredHeight
            }

            val totalHeight = height
            if (binding.toolbarLayout.translationY < -(height - 0.01f)) {
                val show = object : Animation() {
                    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {

                        val trans = interpolatedTime * totalHeight
                        binding.toolbarLayout.translationY = trans - totalHeight
                        setWebViewTranslation(trans)

                    }
                }
                show.duration = 250
                show.interpolator = BezierDecelerateInterpolator()
                binding.contentFrame.startAnimation(show)
            }
        }
    }

    override fun handleBookmarksChange() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null && currentTab.url.isBookmarkUrl()) {
            currentTab.loadBookmarkPage()
        }
        if (currentTab != null) {
            bookmarksView?.handleUpdatedUrl(currentTab.url)
        }
        suggestionsAdapter?.refreshBookmarks()
    }

    override fun handleDownloadDeleted() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null && currentTab.url.isDownloadsUrl()) {
            currentTab.loadDownloadsPage()
        }
        if (currentTab != null) {
            bookmarksView?.handleUpdatedUrl(currentTab.url)
        }
    }

    override fun handleBookmarkDeleted(bookmark: Bookmark) {
        bookmarksView?.handleBookmarkDeleted(bookmark)
        handleBookmarksChange()
    }

    override fun handleNewTab(newTabType: LightningDialogBuilder.NewTab, url: String, addToIndex: Boolean) {
        val urlInitializer = UrlInitializer(url)
        when (newTabType) {
            LightningDialogBuilder.NewTab.FOREGROUND -> if (addToIndex) {
                presenter?.newTabAtPosition(urlInitializer, true, tabsManager.indexOfCurrentTab() + 1)
            } else {
                presenter?.newTab(urlInitializer, true)
            }
            LightningDialogBuilder.NewTab.BACKGROUND -> {
                if (addToIndex) {
                    presenter?.newTabAtPosition(urlInitializer, false, tabsManager.indexOfCurrentTab() + 1)
                } else {
                    presenter?.newTab(urlInitializer, false)
                }
                val snackbar = Snackbar
                        .make(findViewById(android.R.id.content), resources.getString(R.string.new_tab_opened), Snackbar.LENGTH_SHORT)
                        .setAction(resources.getString(R.string.switch_button)) { tabClicked(tabsManager.indexOfCurrentTab() + 1) }
                snackbar.show()
            }
            LightningDialogBuilder.NewTab.INCOGNITO -> {
                binding.drawerLayout.closeDrawers()
                val intent = IncognitoActivity.createIntent(this, url.toUri())
                startActivity(intent)
                overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out_scale)
            }
        }
    }

    /**
     * This method lets the search bar know that the page is currently loading
     * and that it should display the stop icon to indicate to the user that
     * pressing it stops the page from loading
     */
    private fun setIsLoading(isLoading: Boolean) {
        if (searchView?.hasFocus() == false) {
            toolbarBinding.searchSslStatus.updateVisibilityForContent()
            toolbarBinding.searchRefresh.setImageResource(if (isLoading) R.drawable.ic_action_delete else
                if (isDarkTheme) {
                    R.drawable.ic_action_refresh_light
                } else {
                    R.drawable.ic_action_refresh
                })
        }
    }

    /**
     * handle presses on the refresh icon in the search bar, if the page is
     * loading, stop the page, if it is done loading refresh the page.
     * See setIsFinishedLoading and setIsLoading for displaying the correct icon
     */
    private fun refreshOrStop() {
        val currentTab = tabsManager.currentTab
        if (currentTab != null) {
            if (currentTab.progress < 100) {
                currentTab.stopLoading()
            } else {
                currentTab.reload()
            }
        }
    }

    /**
     * Handle the click event for the views that are using
     * this class as a click listener. This method should
     * distinguish between the various views using their IDs.
     *
     * @param v the view that the user has clickedf
     */
    override fun onClick(v: View) {
        val currentTab = tabsManager.currentTab ?: return
        val popUpClass = PopUpClass()
        when (v.id) {
            R.id.home_button -> when {
                searchView?.hasFocus() == true -> currentTab.requestFocus()
                shouldShowTabsInDrawer -> binding.drawerLayout.openDrawer(getTabDrawer())
                else -> currentTab.loadHomePage()
            }
            R.id.more_button -> {
                popUpClass.showPopupWindow(v, this)
            }
            R.id.download_button -> {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, currentTab.url)
                    type = "text/plain"
                }
                sendIntent.setClassName("com.cookiejarapps.smartcookieweb_ytdl",
                        "com.cookiejarapps.smartcookieweb_ytdl.MainActivity")

                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }
            R.id.button_next -> findResult?.nextResult()
            R.id.button_back -> findResult?.previousResult()
            R.id.button_quit -> {
                findResult?.clearResults()
                findResult = null
                binding.searchBar.visibility = GONE
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            }
            R.id.button_search -> {
                showFindInPageControls(findViewById<EditText>(R.id.search_query).text.toString())
                findResult = presenter?.findInPage(findViewById<EditText>(R.id.search_query).text.toString())
            }
        }
    }


    /**
     * Handle the callback that permissions requested have been granted or not.
     * This method should act upon the results of the permissions request.
     *
     * @param requestCode  the request code sent when initially making the request
     * @param permissions  the array of the permissions that was requested
     * @param grantResults the results of the permissions requests that provides
     * information on whether the request was granted or not
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        PermissionsManager.instance.notifyPermissionsChange(permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

   override fun getAssets(): AssetManager {
        return resources.assets
    }

    /**
     * If the [drawer] is open, close it and return true. Return false otherwise.
     */
    private fun DrawerLayout.closeDrawerIfOpen(drawer: View): Boolean =
            if (isDrawerOpen(drawer)) {
                closeDrawer(drawer)
                true
            } else {
                false
            }

    companion object {

        private const val TAG = "BrowserActivity"

        const val INTENT_PANIC_TRIGGER = "info.guardianproject.panic.action.TRIGGER"

        private const val FILE_CHOOSER_REQUEST_CODE = 1111

        // Constant
        private val MATCH_PARENT = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        private val COVER_SCREEN_PARAMS = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    }

}