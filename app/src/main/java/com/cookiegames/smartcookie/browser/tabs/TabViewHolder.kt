package com.cookiegames.smartcookie.browser.tabs

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.DrawerSizeChoice
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.preference.UserPreferences

/**
 * The [RecyclerView.ViewHolder] for both vertical and horizontal tabs.
 */
class TabViewHolder(
    view: View,
    private val uiController: UIController,
    private val userPreferences: UserPreferences
) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnLongClickListener {

    val txtTitle: TextView = view.findViewById(R.id.textTab)
    val favicon: ImageView = view.findViewById(R.id.faviconTab)
    val exitButton: View = view.findViewById(R.id.deleteAction)
    val layout: LinearLayout = view.findViewById(R.id.tab_item_background)
    val faviconButton: FrameLayout = view.findViewById(R.id.favicon_button)

    init {
        exitButton.setOnClickListener(this)
        layout.setOnClickListener(this)
        layout.setOnLongClickListener(this)
        txtTitle.maxLines = userPreferences.drawerLines.value + 1
        if(userPreferences.drawerSize != DrawerSizeChoice.AUTO){
            TextViewCompat.setAutoSizeTextTypeWithDefaults(txtTitle, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
            txtTitle.setTextSize(userPreferences.drawerSize.value.toFloat() * 7)
        }
        faviconButton.setOnClickListener(){
            uiController.showCloseDialog(adapterPosition)
            true
        }

    }


    override fun onClick(v: View) {
        if (v === exitButton) {
            uiController.tabCloseClicked(adapterPosition)
        } else if (v === layout) {
            uiController.tabClicked(adapterPosition)
        }
    }

    override fun onLongClick(v: View): Boolean {
        return true
    }
}
