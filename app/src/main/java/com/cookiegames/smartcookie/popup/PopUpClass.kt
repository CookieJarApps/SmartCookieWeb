/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 7/11/2020 */

package com.cookiegames.smartcookie.popup

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.webkit.CookieManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.IncognitoActivity
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.adblock.allowlist.AllowListModel
import com.cookiegames.smartcookie.browser.JavaScriptChoice
import com.cookiegames.smartcookie.browser.MenuDividerClass
import com.cookiegames.smartcookie.browser.MenuItemClass
import com.cookiegames.smartcookie.browser.TabsManager
import com.cookiegames.smartcookie.browser.activity.BrowserActivity
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.database.Bookmark
import com.cookiegames.smartcookie.database.HistoryEntry
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.dialog.DialogItem
import com.cookiegames.smartcookie.download.DownloadActivity
import com.cookiegames.smartcookie.extensions.color
import com.cookiegames.smartcookie.extensions.copyToClipboard
import com.cookiegames.smartcookie.extensions.drawable
import com.cookiegames.smartcookie.extensions.snackbar
import com.cookiegames.smartcookie.history.HistoryActivity
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.reading.activity.ReadingActivity
import com.cookiegames.smartcookie.settings.activity.SettingsActivity
import com.cookiegames.smartcookie.utils.IntentUtils
import com.cookiegames.smartcookie.utils.Utils
import com.cookiegames.smartcookie.utils.isSpecialUrl
import com.cookiegames.smartcookie.utils.stringContainsItemFromList
import com.github.ahmadaghazadeh.editor.widget.CodeEditor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URL
import javax.inject.Inject
import kotlin.math.roundToInt


class PopUpClass {
    private var list: ListView? = null
    private var uiController: UIController? = null

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var tabsManager: TabsManager

    @Inject
    lateinit var allowListModel: AllowListModel

    //PopupWindow display method
    fun showPopupWindow(view: View, activity: BrowserActivity) {
        view.context.injector.inject(this)

        val inflater = view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View?

        if(userPreferences.bottomBar) popupView = inflater.inflate(R.layout.toolbar_menu_btm, null)
        else popupView = inflater.inflate(R.layout.toolbar_menu, null)
        
        val r = view.context.resources

        val px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 228f, r.displayMetrics).roundToInt()

        val focusable = true
        uiController = view.context as UIController

        val popupWindow = PopupWindow(popupView, px, LinearLayout.LayoutParams.WRAP_CONTENT, focusable)
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        var relView = popupView.findViewById<RelativeLayout>(R.id.toolbar_menu)

        if(activity.isIncognito()){
            popupView.findViewById<ConstraintLayout>(R.id.transparent_container).setBackgroundResource(R.drawable.toolbar_dark)
        }
        else{
            when(userPreferences.useTheme){
                AppTheme.DARK -> popupView.findViewById<ConstraintLayout>(R.id.transparent_container).setBackgroundResource(R.drawable.toolbar_dark)
                AppTheme.BLACK -> popupView.findViewById<ConstraintLayout>(R.id.transparent_container).setBackgroundResource(R.drawable.toolbar_black)
                else -> {}
            }
        }

        val currentView = activity.tabsManager.currentTab
        val currentUrl = uiController!!.getTabModel().currentTab?.url

        popupView.findViewById<ImageButton>(R.id.back_option).setOnClickListener {
            currentView?.goBack()
        }
        popupView.findViewById<ImageButton>(R.id.forward_option).setOnClickListener {
            currentView?.goForward()
        }
        popupView.findViewById<ImageButton>(R.id.close_option).setOnClickListener {
            if(Build.VERSION.SDK_INT >= 21){
                activity.finishAndRemoveTask()
            }
            else{
                activity.finish()
            }
        }

        val uri = Uri.parse(currentUrl)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        val packageManager: PackageManager = activity.packageManager
        if (intent.resolveActivity(packageManager) == null || intent.resolveActivity(packageManager).packageName == activity.applicationContext.packageName || currentUrl.isSpecialUrl()) {
            popupView.findViewById<ImageButton>(R.id.open_in_app).visibility = View.GONE
        }

        popupView.findViewById<ImageButton>(R.id.open_in_app).setOnClickListener {
            val components = arrayOf(ComponentName(activity, BrowserActivity::class.java))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                activity.startActivity(Intent.createChooser(intent, null).putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS,components))
            else activity.startActivity(intent)
            popupWindow.dismiss()
        }

        popupView.findViewById<ImageButton>(R.id.bookmark_option).setOnClickListener {
            val bookmark = Bookmark.Entry(currentUrl!!, currentView!!.title, 0, Bookmark.Folder.Root)
            activity.bookmarksDialogBuilder.showAddBookmarkDialog(activity, uiController!!, bookmark)
        }

        val container =  popupView.findViewById<ConstraintLayout>(R.id.transparent_container)

        if(userPreferences.navbar){
            popupView.findViewById<LinearLayout>(R.id.topBar).visibility = View.GONE
            val noTopMenu = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 56f, r.displayMetrics).roundToInt()
           container.setPadding(container.paddingLeft, container.paddingTop - noTopMenu, container.paddingRight, container.paddingBottom)
        }

        val menu = mutableListOf(
                MenuDividerClass(),
                MenuItemClass("new_tab", R.string.action_new_tab, R.drawable.ic_round_add, true),
                MenuItemClass("new_private_tab", R.string.action_incognito, R.drawable.incognito_mode, true),
                MenuDividerClass(),
                MenuItemClass("share", R.string.action_share, R.drawable.ic_share_black, true),
                MenuItemClass("open_in_app", R.string.open_in_app, R.drawable.ic_round_open_in_new, userPreferences.navbar && !activity.isIncognito() && intent.resolveActivity(packageManager) != null && intent.resolveActivity(packageManager).packageName != activity.applicationContext.packageName && !currentUrl.isSpecialUrl()),
                MenuItemClass("translate", R.string.translator, R.drawable.translate, userPreferences.translateExtension),
                MenuItemClass("print", R.string.action_print, R.drawable.ic_round_print, true),
                MenuDividerClass(),
                MenuItemClass("history", R.string.action_history, R.drawable.ic_history, true),
                MenuItemClass("bookmarks", R.string.action_bookmarks, R.drawable.ic_action_star, true),
                MenuItemClass("downloads", R.string.action_downloads, R.drawable.ic_file_download_black, true),
                MenuDividerClass(),
                MenuItemClass("find_in_page", R.string.action_find, R.drawable.ic_search, true),
                MenuItemClass("copy_link", R.string.dialog_copy_link, R.drawable.ic_content_copy_black, true),
                MenuItemClass("reading_mode", R.string.reading_mode, R.drawable.ic_action_reading, true),
                MenuDividerClass(),
                MenuItemClass("page_tools", R.string.dialog_tools_title, R.drawable.ic_page_tools, true),
                MenuItemClass("add_to_homepage", R.string.action_add_to_homescreen, R.drawable.ic_round_smartphone, true),
                MenuItemClass("settings", R.string.settings, R.drawable.ic_round_settings, true)
        )

        val incognitoMenu = mutableListOf(
                MenuItemClass("new_tab", R.string.action_new_tab, R.drawable.ic_round_add, true),
                MenuDividerClass(),
                MenuItemClass("print", R.string.action_print, R.drawable.ic_round_print, true),
                MenuItemClass("find_in_page", R.string.action_find, R.drawable.ic_search, true),
                MenuItemClass("copy_link", R.string.dialog_copy_link, R.drawable.ic_content_copy_black, true),
                MenuItemClass("add_to_homepage", R.string.action_add_to_homescreen, R.drawable.ic_round_smartphone, true),
                MenuDividerClass(),
                MenuItemClass("bookmarks", R.string.action_bookmarks, R.drawable.ic_action_star, true),
                MenuItemClass("reading_mode", R.string.reading_mode, R.drawable.ic_action_reading, true),
                MenuDividerClass(),
                MenuItemClass("settings", R.string.settings, R.drawable.ic_round_settings, true),
                MenuItemClass("exit_private", R.string.quit_private, R.drawable.incognito_mode, true)
        )

        val finalMenu = if(activity.isIncognito()) incognitoMenu else menu
        finalMenu.removeAll { it is MenuItemClass && !it.enabled }
        if (userPreferences.bottomBar) finalMenu.reverse()

        //Set the location of the window on the screen
        if (userPreferences.bottomBar) {
            popupWindow.animationStyle = R.style.ToolbarAnimReverse
            popupWindow.showAtLocation(view, Gravity.BOTTOM or Gravity.END, 0, 0)
            relView.gravity = Gravity.BOTTOM
        } else {
            popupWindow.animationStyle = R.style.ToolbarAnim
            popupWindow.showAtLocation(view, Gravity.TOP or Gravity.END, 0, 0)
        }

        popupWindow.setBackgroundDrawable(ColorDrawable(Color.WHITE))

        val adapter = MenuItemAdapter(view.context, finalMenu)
        list = popupView.findViewById(R.id.menuList)
        list?.adapter = adapter
        list?.setOnItemClickListener { parent, listView, position, id ->
            if(finalMenu[position] !is MenuItemClass){
                return@setOnItemClickListener
            }

            when((finalMenu[position] as MenuItemClass).id){
                "new_tab" -> uiController!!.newTabButtonClicked() // 0 - New tab
                "new_private_tab" -> view.context.startActivity(Intent(view.context, IncognitoActivity::class.java)) // 1 - New incognito tab
                "share" -> IntentUtils(activity).shareUrl(currentUrl, currentView?.title) // 2 - Share
                "print" -> currentView!!.webView?.let { currentView.createWebPagePrint(it) } // 3 - Print
                "history" -> view.context.startActivity(Intent(view.context, HistoryActivity::class.java)) // 4 - History
                "downloads" -> currentView?.loadDownloadsPage() // 5 - Download
                "find_in_page" -> activity.findInPage() // 6 - Find in Page
                "copy_link" -> {
                    if (currentUrl != null && !currentUrl.isSpecialUrl()) { // 7 - Copy link
                        activity.clipboardManager.copyToClipboard(currentUrl)
                        activity.snackbar(R.string.message_link_copied)
                    }
                }
                "add_to_homepage" -> {
                    if (currentView != null
                            && currentView.url.isNotBlank()
                            && !currentView.url.isSpecialUrl()) { // 8 - Add to Homepage
                        HistoryEntry(currentView.url, currentView.title).also {
                            Utils.createShortcut(activity, it, currentView.favicon ?: activity.webPageBitmap!!)
                        }
                    }
                }
                "bookmarks" -> activity.openBookmarksDrawer() // 9 - Bookmarks
                "reading_mode" -> {
                    if (currentUrl != null) { // 10 - Reading mode
                        ReadingActivity.launch(view.context, currentUrl, false)
                    }
                }
                "page_tools" -> {
                    val currentTab = activity.tabsManager.currentTab ?: return@setOnItemClickListener
                    val isAllowedAds = allowListModel.isUrlAllowedAds(currentTab.url)
                    val whitelistString = if (isAllowedAds) {
                        R.string.dialog_adblock_enable_for_site
                    } else {
                        R.string.dialog_adblock_disable_for_site
                    }
                    val arrayOfURLs = userPreferences.javaScriptBlocked
                    val strgs: Array<String>
                    if (arrayOfURLs.contains(", ")) {
                        strgs = arrayOfURLs.split(", ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    } else {
                        strgs = arrayOfURLs.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    }
                    var jsEnabledString = if(userPreferences.javaScriptChoice == JavaScriptChoice.BLACKLIST && !stringContainsItemFromList(currentTab.url, strgs) || userPreferences.javaScriptChoice == JavaScriptChoice.WHITELIST && stringContainsItemFromList(currentTab.url, strgs)) {
                        R.string.allow_javascript
                    } else{
                        R.string.block_javascript
                    }


                    BrowserDialog.showWithIcons(activity, activity.getString(R.string.dialog_tools_title),
                        DialogItem(
                            icon = activity.drawable(R.drawable.ic_action_desktop),
                            title = R.string.dialog_toggle_desktop
                        ) {
                            activity.tabsManager.currentTab?.apply {
                                toggleDesktopUA()
                                reload()
                                // TODO add back drawer closing
                            }
                        },
                        DialogItem(
                            icon = activity.drawable(R.drawable.ic_page_tools),
                            title = R.string.inspect

                        ) {
                            val builder = AlertDialog.Builder(activity)
                            val inflater = activity.layoutInflater
                            builder.setTitle(R.string.inspect)
                            val dialogLayout = inflater.inflate(R.layout.dialog_edit_text, null)
                            val editText = dialogLayout.findViewById<EditText>(R.id.dialog_edit_text)
                            builder.setView(dialogLayout)
                            builder.setPositiveButton("OK") { dialogInterface, i -> currentTab.loadUrl("javascript:(function() {" + editText.text.toString() + "})()") }
                            builder.show()

                        }, DialogItem(
                            icon = activity.drawable(R.drawable.ic_round_storage),
                            title = R.string.edit_cookies
                        ) {

                            val cookieManager = CookieManager.getInstance()
                            if (cookieManager.getCookie(currentTab.url) != null) {
                                val builder = MaterialAlertDialogBuilder(activity)
                                val inflater = activity.layoutInflater
                                builder.setTitle(R.string.edit_cookies)
                                val dialogLayout = inflater.inflate(R.layout.dialog_multi_line, null)
                                val editText = dialogLayout.findViewById<EditText>(R.id.dialog_multi_line)
                                editText.setText(cookieManager.getCookie(currentTab.url))
                                builder.setView(dialogLayout)
                                builder.setPositiveButton("OK") { dialogInterface, i ->
                                    val cookiesList = editText.text.toString().split(";")
                                    cookiesList.forEach { item ->
                                        CookieManager.getInstance().setCookie(currentTab.url, item)
                                    }
                                }
                                builder.show()
                            }

                        },
                        DialogItem(
                            icon = activity.drawable(R.drawable.ic_baseline_code),
                            title = R.string.page_source

                        ) {
                            currentTab.webView?.evaluateJavascript("""(function() {
                        return "<html>" + document.getElementsByTagName('html')[0].innerHTML + "</html>";
                     })()""".trimMargin()) {
                                // Hacky workaround for weird WebView encoding bug
                                var name = it?.replace("\\u003C", "<")
                                name = name?.replace("\\n", System.getProperty("line.separator").toString())
                                name = name?.replace("\\t", "")
                                name = name?.replace("\\\"", "\"")
                                name = name?.substring(1, name.length - 1);

                                val builder = MaterialAlertDialogBuilder(activity)
                                val inflater = activity.layoutInflater
                                builder.setTitle(R.string.page_source)
                                val dialogLayout = inflater.inflate(R.layout.dialog_view_source, null)
                                val editText = dialogLayout.findViewById<CodeEditor>(R.id.dialog_multi_line)
                                editText.setText(name, 1)
                                builder.setView(dialogLayout)
                                builder.setPositiveButton("OK") { _, _ ->
                                    editText.setText(editText.text?.toString()?.replace("\'", "\\\'"), 1);
                                    currentTab.loadUrl("javascript:(function() { document.documentElement.innerHTML = '" + editText.text.toString() + "'; })()")
                                }
                                builder.show()
                            }
                        },
                        DialogItem(
                            icon = activity.drawable(R.drawable.ic_block),
                            colorTint = activity.color(R.color.error_red).takeIf { isAllowedAds },
                            title = whitelistString,
                            isConditionMet = !currentTab.url.isSpecialUrl()
                        ) {
                            if (isAllowedAds) {
                                allowListModel.removeUrlFromAllowList(currentTab.url)
                            } else {
                                allowListModel.addUrlToAllowList(currentTab.url)
                            }
                            activity.tabsManager.currentTab?.reload()
                        }, DialogItem(
                            icon = activity.drawable(R.drawable.ic_action_delete),
                            title = jsEnabledString,
                            isConditionMet = !currentTab.url.isSpecialUrl()
                        ) {
                            val url = URL(currentTab.url)
                            if (userPreferences.javaScriptChoice != JavaScriptChoice.NONE) {
                                if (!stringContainsItemFromList(currentTab.url, strgs)) {
                                    if (userPreferences.javaScriptBlocked.equals("")) {
                                        userPreferences.javaScriptBlocked = url.host
                                    } else {
                                        userPreferences.javaScriptBlocked = userPreferences.javaScriptBlocked + ", " + url.host
                                    }
                                } else {
                                    if (!userPreferences.javaScriptBlocked.contains(", " + url.host)) {
                                        userPreferences.javaScriptBlocked = userPreferences.javaScriptBlocked.replace(url.host, "")
                                    } else {
                                        userPreferences.javaScriptBlocked = userPreferences.javaScriptBlocked.replace(", " + url.host, "")
                                    }
                                }
                            } else {
                                userPreferences.javaScriptChoice = JavaScriptChoice.WHITELIST
                            }
                            activity.tabsManager.currentTab?.reload()
                            Handler().postDelayed({
                                activity.tabsManager.currentTab?.reload()
                            }, 250)

                        }
                    ) // 14 - Page Tools
                }
                "settings" -> {
                    val settings = Intent(view.context, SettingsActivity::class.java) // 11 - Settings
                    view.context.startActivity(settings)
                }
                "exit_private" -> {
                    view.context.startActivity(Intent(view.context, BrowserActivity::class.java))
                    activity.finish()
                }
                "translate" -> {
                    val locale = Resources.getSystem().configuration.locale
                    currentView?.loadUrl("https://www.translatetheweb.com/?from=&to=$locale&dl=$locale&a=$currentUrl")
                }
                "open_in_app" -> {
                    val components = arrayOf(ComponentName(activity, BrowserActivity::class.java))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        activity.startActivity(Intent.createChooser(intent, null).putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS,components))
                    else activity.startActivity(intent)
                    popupWindow.dismiss() // 13 - Open in App
                }
            }
            popupWindow.dismiss()
        }
    }
}