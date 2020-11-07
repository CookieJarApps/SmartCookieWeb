// Copyright 2020 CookieJarApps MPL
package com.cookiegames.smartcookie.popup

import android.app.Activity
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.cookiegames.smartcookie.IncognitoActivity
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.TabsManager
import com.cookiegames.smartcookie.browser.activity.BrowserActivity
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.settings.activity.SettingsActivity
import kotlinx.android.synthetic.main.search_interface.*
import java.util.*
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
        val popupView = inflater.inflate(R.layout.toolbar_menu, null)
        val r = view.context.resources
        val px = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 228f, r.displayMetrics))

        //Specify the length and width through constants
        val height = LinearLayout.LayoutParams.MATCH_PARENT

        //Make Inactive Items Outside Of PopupWindow
        val focusable = true
        uiController = view.context as UIController


        //Create a window with our parameters
        val popupWindow = PopupWindow(popupView, px, height, focusable)
        val relView = popupView.findViewById<RelativeLayout>(R.id.toolbar_menu)

        //Set the location of the window on the screen
        if (userPreferences!!.bottomBar) {
            popupWindow.animationStyle = R.style.ToolbarAnimReverse
            popupWindow.showAtLocation(view, Gravity.BOTTOM or Gravity.END, 0, 0)
            relView.gravity = Gravity.BOTTOM
        } else {
            popupWindow.animationStyle = R.style.ToolbarAnim
            popupWindow.showAtLocation(view, Gravity.TOP or Gravity.END, 0, 0)
        }
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        val resources = view.context.resources
        val textString = arrayOf(resources.getString(R.string.action_new_tab), resources.getString(R.string.action_incognito), resources.getString(R.string.action_share), resources.getString(R.string.action_print), resources.getString(R.string.action_history), resources.getString(R.string.action_downloads), resources.getString(R.string.action_find), resources.getString(R.string.action_copy), resources.getString(R.string.action_add_to_homescreen), resources.getString(R.string.action_bookmarks), resources.getString(R.string.reading_mode), resources.getString(R.string.settings))
        val drawableIds = intArrayOf(R.drawable.ic_round_add, R.drawable.incognito_mode, R.drawable.ic_share_black_24dp, R.drawable.ic_round_print_24, R.drawable.ic_history, R.drawable.ic_file_download_black_24dp, R.drawable.ic_search, R.drawable.ic_content_copy_black_24dp, R.drawable.ic_round_smartphone, R.drawable.state_ic_bookmark, R.drawable.ic_action_reading, R.drawable.ic_round_settings)
        if (userPreferences!!.bottomBar) {
            Collections.reverse(Arrays.asList(*textString))
            for (i in 0 until drawableIds.size / 2) {
                val temp = drawableIds[i]
                drawableIds[i] = drawableIds[drawableIds.size - i - 1]
                drawableIds[drawableIds.size - i - 1] = temp
            }
        }
        val adapter = CustomAdapter(view.context, textString, drawableIds)
        list = popupView.findViewById<ListView>(R.id.menuList)
        list?.setAdapter(adapter)
        list?.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
            var positionList = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
            var currentView = uiController!!.getTabModel().currentTab
            var currentUrl = uiController!!.getTabModel().currentTab?.url
            if (userPreferences!!.bottomBar) {
                positionList = intArrayOf(11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0)
            }
            if (position == positionList[0]) {
                uiController!!.newTabButtonClicked()
            } else if (position == positionList[1]) {
                val incognito = Intent(view.context, IncognitoActivity::class.java)
                view.context.startActivity(incognito)
            } else if (position == positionList[2]) {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                if (currentView?.title != null) {
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentView.title)
                }
                shareIntent.putExtra(Intent.EXTRA_TEXT, currentUrl)
                view.context.startActivity(Intent.createChooser(shareIntent, view.context.getString(R.string.dialog_title_share)))
            } else if (position == positionList[3]) {
                currentView!!.webView?.let { currentView.createWebPagePrint(it) }
            } else if (position == positionList[4]) {
                currentView?.loadHistoryPage()
            } else if (position == positionList[5]) {
                currentView?.loadDownloadsPage()
            } else if (position == positionList[6]) {
                activity.findInPage()
            } else if (position == positionList[7]) {
                val incognito = Intent(view.context, IncognitoActivity::class.java)
                view.context.startActivity(incognito)
            } else if (position == positionList[8]) {
                val incognito = Intent(view.context, IncognitoActivity::class.java)
                view.context.startActivity(incognito)
            } else if (position == positionList[9]) {
                val incognito = Intent(view.context, IncognitoActivity::class.java)
                view.context.startActivity(incognito)
            } else if (position == positionList[10]) {
                val incognito = Intent(view.context, IncognitoActivity::class.java)
                view.context.startActivity(incognito)
            } else if (position == positionList[11]) {
                val incognito = Intent(view.context, SettingsActivity::class.java)
                view.context.startActivity(incognito)
            }
            popupWindow.dismiss()
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 20f
        }


        //Handler for clicking on the inactive zone of the window
        popupView.setOnTouchListener { v, event -> //Close the window when clicked
            popupWindow.dismiss()
            true
        }
    }
}