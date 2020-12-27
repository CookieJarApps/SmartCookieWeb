package com.cookiegames.smartcookie.browser.tabs

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.extensions.desaturate
import com.cookiegames.smartcookie.extensions.inflater
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.view.BackgroundDrawable
import java.util.*

/**
 * The adapter for vertical mobile style browser tabs.
 */
class TabsDrawerAdapter(
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
        val view = viewGroup.context.inflater.inflate(R.layout.tab_list_item, viewGroup, false)
        view.background = BackgroundDrawable(view.context)
        return TabViewHolder(view, uiController, userPreferences = userPreferences)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.exitButton.tag = position

        val web = tabList[position]

        holder.txtTitle.text = web.title
        updateViewHolderAppearance(holder, web.favicon, web.isForegroundTab)
        updateViewHolderFavicon(holder, web.favicon, web.isForegroundTab)
        updateViewHolderBackground(holder, web.isForegroundTab)
    }

    fun moveItem(from: Int, to: Int){
        val oldList = tabList
        if (from < to) {
            for (i in from until to) {
                Collections.swap(oldList, i, i + 1)
            }
        } else {
            for (i in from downTo to + 1) {
                Collections.swap(oldList, i, i - 1)
            }
        }
        uiController.getTabModel().moveTab(from, to)
        showTabs(oldList)
        notifyItemMoved(from, to)
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

    private fun updateViewHolderBackground(viewHolder: TabViewHolder, isForeground: Boolean) {
        val verticalBackground = viewHolder.layout.background as BackgroundDrawable
        verticalBackground.isCrossFadeEnabled = false
        if (isForeground) {
            verticalBackground.startTransition(200)
        } else {
            verticalBackground.reverseTransition(200)
        }
    }

    private fun updateViewHolderAppearance(viewHolder: TabViewHolder, favicon: Bitmap?, isForeground: Boolean) {
        if (isForeground) {
            TextViewCompat.setTextAppearance(viewHolder.txtTitle, R.style.boldText)
            uiController.changeToolbarBackground(favicon, null)
            uiController.changeToolbarColor(null)
        } else {
            TextViewCompat.setTextAppearance(viewHolder.txtTitle, R.style.normalText)
        }
    }

    override fun getItemCount() = tabList.size

}
