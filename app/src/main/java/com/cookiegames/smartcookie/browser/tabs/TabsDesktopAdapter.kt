package com.cookiegames.smartcookie.browser.tabs

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.view.ViewGroup
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.extensions.desaturate
import com.cookiegames.smartcookie.extensions.inflater
import com.cookiegames.smartcookie.extensions.tint
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.utils.ThemeUtils
import com.cookiegames.smartcookie.utils.Utils

/**
 * The adapter for horizontal desktop style browser tabs.
 */
class TabsDesktopAdapter(
    context: Context,
    private val resources: Resources,
    private val uiController: UIController,
    private val userPreferences: UserPreferences
) : RecyclerView.Adapter<TabViewHolder>() {

    private var tabList: List<TabViewState> = emptyList()

    fun showTabs(tabs: List<TabViewState>) {
        val oldList = tabList
        tabList = tabs

        DiffUtil.calculateDiff(TabViewStateDiffCallback(oldList, tabList)).dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): TabViewHolder {
        val view = viewGroup.context.inflater.inflate(R.layout.tab_list_item_horizontal, viewGroup, false)
        return TabViewHolder(view, uiController, userPreferences)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.exitButton.tag = position

        val web = tabList[position]

        holder.txtTitle.text = web.title
        updateViewHolderAppearance(holder, web.favicon, web.isForegroundTab)
        updateViewHolderFavicon(holder, web.favicon, web.isForegroundTab)
    }

    private fun updateViewHolderFavicon(viewHolder: TabViewHolder, favicon: Bitmap?, isForeground: Boolean) {
        favicon?.let {
            if (isForeground) {
                viewHolder.favicon.setImageBitmap(it)
            } else {
                viewHolder.favicon.setImageBitmap(it.desaturate())
            }
        } ?: viewHolder.favicon.setImageResource(R.drawable.ic_webpage)
    }

    private fun updateViewHolderAppearance(viewHolder: TabViewHolder, favicon: Bitmap?, isForeground: Boolean) {
        if (isForeground) {
            val foregroundDrawable = resources.getDrawable(R.drawable.desktop_tab_selected)
            foregroundDrawable.tint(ThemeUtils.getColorBackground(uiController as Context))
            if (uiController.isColorMode()) {
                foregroundDrawable.tint(uiController.getUiColor())
            }
            TextViewCompat.setTextAppearance(viewHolder.txtTitle, R.style.boldText)
            viewHolder.layout.background = foregroundDrawable
            uiController.changeToolbarBackground(favicon, foregroundDrawable)
        } else {
            val backgroundDrawable = resources.getDrawable(R.drawable.desktop_tab)
            backgroundDrawable.tint(Utils.mixTwoColors(ThemeUtils.getColorBackground(uiController as Context), Color.BLACK, 0.85f))
            TextViewCompat.setTextAppearance(viewHolder.txtTitle, R.style.normalText)
            viewHolder.layout.background = backgroundDrawable
        }
    }

    override fun getItemCount() = tabList.size

}
