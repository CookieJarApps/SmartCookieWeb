package com.cookiegames.smartcookie.browser.tabs

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.TabsView
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.extensions.inflater
import com.cookiegames.smartcookie.list.VerticalItemAnimator
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.view.SmartCookieView


/**
 * A view which displays tabs in a vertical [RecyclerView].
 */
class TabsDrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    userPreferences: UserPreferences
) : LinearLayout(context, attrs, defStyleAttr), TabsView {

    private val uiController = context as UIController
    private val tabsAdapter = TabsDrawerAdapter(uiController, userPreferences = userPreferences)
    private val tabList: RecyclerView
    private val actionBack: View
    private val actionForward: View

    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback =
                object : ItemTouchHelper.SimpleCallback(UP or
                        DOWN or
                        START or
                        END, 0) {

                    override fun onMove(recyclerView: RecyclerView,
                                        viewHolder: RecyclerView.ViewHolder,
                                        target: RecyclerView.ViewHolder): Boolean {

                        val adapter = recyclerView.adapter as TabsDrawerAdapter
                        val from = viewHolder.adapterPosition
                        val to = target.adapterPosition
                        adapter.moveItem(from, to)

                        return true
                    }    override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                               direction: Int) {

                    }
                }

        ItemTouchHelper(simpleItemTouchCallback)
    }

    init {
        orientation = VERTICAL
        context.inflater.inflate(R.layout.tab_drawer, this, true)
        actionBack = findViewById(R.id.action_back)
        actionForward = findViewById(R.id.action_forward)

        val animator = VerticalItemAnimator().apply {
            supportsChangeAnimations = false
            addDuration = 200
            changeDuration = 0
            removeDuration = 200
            moveDuration = 200
        }

        val layoutManagerItem = LinearLayoutManager(context)
        layoutManagerItem.reverseLayout = userPreferences.stackFromBottom

        tabList = findViewById<RecyclerView>(R.id.tabs_list).apply {
            setLayerType(View.LAYER_TYPE_NONE, null)
            itemAnimator = animator
            layoutManager = layoutManagerItem
            adapter = tabsAdapter
            setHasFixedSize(true)
        }

        if(userPreferences.stackFromBottom){
            tabList.setPadding(tabList.paddingLeft, 0, tabList.paddingRight, userPreferences.drawerOffset * 10)
        }
        else{
            tabList.setPadding(tabList.paddingLeft, userPreferences.drawerOffset * 10, tabList.paddingRight, tabList.paddingBottom)
        }

        itemTouchHelper.attachToRecyclerView(tabList)

        findViewById<View>(R.id.tab_header_button).setOnClickListener {
            uiController.showCloseDialog(uiController.getTabModel().indexOfCurrentTab())
        }
        findViewById<View>(R.id.new_tab_button).apply {
            setOnClickListener {
                uiController.newTabButtonClicked()
            }
            setOnLongClickListener {
                uiController.newTabButtonLongClicked()
                true
            }
        }
        findViewById<View>(R.id.action_back).setOnClickListener {
            uiController.onBackButtonPressed()
        }
        findViewById<View>(R.id.action_forward).setOnClickListener {
            uiController.onForwardButtonPressed()
        }
        findViewById<View>(R.id.action_home).setOnClickListener {
            uiController.onHomeButtonPressed()
        }

        if(userPreferences.navbar){
            findViewById<View>(R.id.action_home).visibility = View.GONE
            findViewById<View>(R.id.action_forward).visibility = View.GONE
            findViewById<View>(R.id.action_back).visibility = View.GONE
            findViewById<View>(R.id.new_tab_button).visibility = View.GONE
            findViewById<View>(R.id.new_tab).visibility = View.VISIBLE
            findViewById<View>(R.id.new_tab).setOnClickListener {
                uiController.newTabButtonClicked()
            }
        }
    }


    override fun tabAdded() {
        displayTabs()
        tabList.postDelayed({ tabList.smoothScrollToPosition(tabsAdapter.itemCount - 1) }, 500)
    }

    override fun tabRemoved(position: Int) {
        displayTabs()
    }

    override fun tabChanged(position: Int) {
        displayTabs()
    }

    private fun displayTabs() {
        tabsAdapter.showTabs(uiController.getTabModel().allTabs.map(SmartCookieView::asTabViewState))
    }

    override fun tabsInitialized() {
        tabsAdapter.notifyDataSetChanged()
    }

    override fun setGoBackEnabled(isEnabled: Boolean) {
        actionBack.isEnabled = isEnabled
    }

    override fun setGoForwardEnabled(isEnabled: Boolean) {
        actionForward.isEnabled = isEnabled
    }

}
