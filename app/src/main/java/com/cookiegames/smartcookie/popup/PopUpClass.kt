/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 7/11/2020 */

package com.cookiegames.smartcookie.popup

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.IncognitoActivity
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.TabsManager
import com.cookiegames.smartcookie.browser.activity.BrowserActivity
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.database.Bookmark
import com.cookiegames.smartcookie.database.HistoryEntry
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.download.DownloadActivity
import com.cookiegames.smartcookie.extensions.copyToClipboard
import com.cookiegames.smartcookie.extensions.snackbar
import com.cookiegames.smartcookie.history.HistoryActivity
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.reading.activity.ReadingActivity
import com.cookiegames.smartcookie.settings.activity.SettingsActivity
import com.cookiegames.smartcookie.utils.IntentUtils
import com.cookiegames.smartcookie.utils.Utils
import com.cookiegames.smartcookie.utils.isSpecialUrl
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject


class PopUpClass {
    private var list: ListView? = null
    private var uiController: UIController? = null

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var tabsManager: TabsManager

    //PopupWindow display method
    fun showPopupWindow(view: View, activity: BrowserActivity) {
        view.context.injector.inject(this)
        //Create a View object yourself through inflater
        val inflater = view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var popupView: View?

        if(userPreferences.bottomBar) popupView = inflater.inflate(R.layout.toolbar_menu_btm, null)
        else popupView = inflater.inflate(R.layout.toolbar_menu, null)
        
        val r = view.context.resources

        val px = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 228f, r.displayMetrics))

        val height = LinearLayout.LayoutParams.WRAP_CONTENT

        val focusable = true
        uiController = view.context as UIController

        val popupWindow = PopupWindow(popupView, px, height, focusable)
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
            }
        }

        var currentView = activity.tabsManager.currentTab
        var currentUrl = uiController!!.getTabModel().currentTab?.url

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
        val packageManager: PackageManager = activity.getPackageManager()
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
        var container =  popupView.findViewById<ConstraintLayout>(R.id.transparent_container)
        if(userPreferences.navbar){
            popupView.findViewById<LinearLayout>(R.id.topBar).visibility = View.GONE
            val noTopMenu = Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 56f, r.displayMetrics))
           // container.maxHeight = container.maxHeight - noTopMenu
           container.setPadding(container.paddingLeft, container.paddingTop - noTopMenu, container.paddingRight, container.paddingBottom)
        }


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
        val resources = view.context.resources
        var textString: MutableList<String> = mutableListOf(resources.getString(R.string.action_new_tab), resources.getString(R.string.action_incognito), resources.getString(R.string.action_share), resources.getString(R.string.action_print), resources.getString(R.string.action_history), resources.getString(R.string.action_downloads), resources.getString(R.string.action_find), resources.getString(R.string.action_copy), resources.getString(R.string.action_add_to_homescreen), resources.getString(R.string.action_bookmarks), resources.getString(R.string.reading_mode), resources.getString(R.string.settings))
        var drawableIds: MutableList<Int> = mutableListOf(R.drawable.ic_round_add, R.drawable.incognito_mode, R.drawable.ic_share_black, R.drawable.ic_round_print, R.drawable.ic_history, R.drawable.ic_file_download_black, R.drawable.ic_search, R.drawable.ic_content_copy_black, R.drawable.ic_round_smartphone, R.drawable.state_ic_bookmark, R.drawable.ic_action_reading, R.drawable.ic_round_settings)

        if(userPreferences.translateExtension){
            textString = mutableListOf(resources.getString(R.string.action_new_tab), resources.getString(R.string.action_incognito), resources.getString(R.string.action_share), resources.getString(R.string.translator), resources.getString(R.string.action_print), resources.getString(R.string.action_history), resources.getString(R.string.action_downloads), resources.getString(R.string.action_find), resources.getString(R.string.action_copy), resources.getString(R.string.action_add_to_homescreen), resources.getString(R.string.action_bookmarks), resources.getString(R.string.reading_mode), resources.getString(R.string.settings))
            drawableIds = mutableListOf(R.drawable.ic_round_add, R.drawable.incognito_mode, R.drawable.ic_share_black, R.drawable.translate, R.drawable.ic_round_print, R.drawable.ic_history, R.drawable.ic_file_download_black, R.drawable.ic_search, R.drawable.ic_content_copy_black, R.drawable.ic_round_smartphone, R.drawable.state_ic_bookmark, R.drawable.ic_action_reading, R.drawable.ic_round_settings)
        }
        if(activity.isIncognito()){
            textString = mutableListOf(resources.getString(R.string.action_new_tab), resources.getString(R.string.action_print), resources.getString(R.string.action_find), resources.getString(R.string.action_copy), resources.getString(R.string.action_add_to_homescreen), resources.getString(R.string.action_bookmarks), resources.getString(R.string.reading_mode), resources.getString(R.string.settings), resources.getString(R.string.quit_private))
            drawableIds = mutableListOf(R.drawable.ic_round_add, R.drawable.ic_round_print, R.drawable.ic_search, R.drawable.ic_content_copy_black, R.drawable.ic_round_smartphone, R.drawable.state_ic_bookmark, R.drawable.ic_action_reading, R.drawable.ic_round_settings, R.drawable.ic_action_back)
        }
        if (userPreferences.navbar && !activity.isIncognito() && intent.resolveActivity(packageManager) != null && intent.resolveActivity(packageManager).packageName != activity.applicationContext.packageName && !currentUrl.isSpecialUrl()) {
            var index = if(userPreferences.translateExtension && userPreferences.bottomBar){5}else{4}
            textString.add(index, resources.getString(R.string.open_in_app))
            drawableIds.add(index, R.drawable.ic_round_open_in_new)
        }
        if (userPreferences!!.bottomBar) {
            textString.reverse()
            for (i in 0 until drawableIds.size / 2) {
                val temp = drawableIds[i]
                drawableIds[i] = drawableIds[drawableIds.size - i - 1]
                drawableIds[drawableIds.size - i - 1] = temp
            }
        }

        val adapter = CustomAdapter(view.context, textString.toTypedArray(), drawableIds.toIntArray())
        list = popupView.findViewById(R.id.menuList)
        list?.setAdapter(adapter)
        list?.setOnItemClickListener { parent, view, position, id ->

            val currentView = activity.tabsManager.currentTab
            val currentUrl = uiController!!.getTabModel().currentTab?.url

            when(textString[position]){
                resources.getString(R.string.action_new_tab) -> uiController!!.newTabButtonClicked() // 0 - New tab
                resources.getString(R.string.action_incognito) -> view.context.startActivity(Intent(view.context, IncognitoActivity::class.java)) // 1 - New incognito tab
                resources.getString(R.string.action_share) -> IntentUtils(activity).shareUrl(currentUrl, currentView?.title) // 2 - Share
                resources.getString(R.string.action_print) -> currentView!!.webView?.let { currentView.createWebPagePrint(it) } // 3 - Print
                resources.getString(R.string.action_history) -> view.context.startActivity(Intent(view.context, HistoryActivity::class.java)) // 4 - History
                resources.getString(R.string.action_downloads) -> {
                    when(userPreferences.useNewDownloader){
                        true -> view.context.startActivity(Intent(view.context, DownloadActivity::class.java))
                        false -> currentView?.loadDownloadsPage()
                    }

                } // 5 - Download
                resources.getString(R.string.action_find) -> activity.findInPage() // 6 - Find in Page
                resources.getString(R.string.action_copy) -> {
                    if (currentUrl != null && !currentUrl.isSpecialUrl()) { // 7 - Copy link
                        activity.clipboardManager.copyToClipboard(currentUrl)
                        activity.snackbar(R.string.message_link_copied)
                    }
                }
                resources.getString(R.string.action_add_to_homescreen) -> {
                    if (currentView != null
                            && currentView.url.isNotBlank()
                            && !currentView.url.isSpecialUrl()) { // 8 - Add to Homepage
                        HistoryEntry(currentView.url, currentView.title).also {
                            Utils.createShortcut(activity, it, currentView.favicon ?: activity.webPageBitmap!!)
                        }
                    }
                }
                resources.getString(R.string.action_bookmarks) -> activity.drawer_layout.openDrawer(activity.getBookmarkDrawer()) // 9 - Bookmarks
                resources.getString(R.string.reading_mode) -> {
                    if (currentUrl != null) { // 10 - Reading mode
                        ReadingActivity.launch(view.context, currentUrl, false)
                    }
                }
                resources.getString(R.string.settings) -> {
                    val settings = Intent(view.context, SettingsActivity::class.java) // 11 - Settings
                    view.context.startActivity(settings)
                }
                resources.getString(R.string.close) -> {
                    activity.onBackPressed() // 12 - Quit
                    activity.finish()
                }
                resources.getString(R.string.quit_private) -> {
                    view.context.startActivity(Intent(view.context, BrowserActivity::class.java))
                    activity.finish()
                }
                resources.getString(R.string.translator) -> {
                    currentView?.loadUrl("https://translatetheweb.com/?scw=yes&a=" + currentUrl!!) // 13 - Translate
                }
                resources.getString(R.string.open_in_app) -> {
                    val components = arrayOf(ComponentName(activity, BrowserActivity::class.java))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        activity.startActivity(Intent.createChooser(intent, null).putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS,components))
                    else activity.startActivity(intent)
                    popupWindow.dismiss() // 14 - Open in App
                }
            }
            popupWindow.dismiss()
        }
    }
}