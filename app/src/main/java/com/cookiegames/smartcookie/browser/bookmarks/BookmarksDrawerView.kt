package com.cookiegames.smartcookie.browser.bookmarks

import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.adblock.allowlist.AllowListModel
import com.cookiegames.smartcookie.animation.AnimationUtils
import com.cookiegames.smartcookie.browser.BookmarksView
import com.cookiegames.smartcookie.browser.TabsManager
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.database.Bookmark
import com.cookiegames.smartcookie.database.bookmark.BookmarkRepository
import com.cookiegames.smartcookie.di.DatabaseScheduler
import com.cookiegames.smartcookie.di.MainScheduler
import com.cookiegames.smartcookie.di.NetworkScheduler
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.dialog.DialogItem
import com.cookiegames.smartcookie.dialog.LightningDialogBuilder
import com.cookiegames.smartcookie.extensions.color
import com.cookiegames.smartcookie.extensions.drawable
import com.cookiegames.smartcookie.extensions.inflater
import com.cookiegames.smartcookie.favicon.FaviconModel
import com.cookiegames.smartcookie.reading.activity.ReadingActivity
import com.cookiegames.smartcookie.utils.isSpecialUrl
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookiegames.smartcookie.extensions.resizeAndShow
import com.cookiegames.smartcookie.settings.fragment.DisplaySettingsFragment
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * The view that displays bookmarks in a list and some controls.
 */
class BookmarksDrawerView @JvmOverloads constructor(
    context: Context,
    private val activity: Activity,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), BookmarksView {

    @Inject internal lateinit var bookmarkModel: BookmarkRepository
    @Inject internal lateinit var allowListModel: AllowListModel
    @Inject internal lateinit var bookmarksDialogBuilder: LightningDialogBuilder
    @Inject internal lateinit var faviconModel: FaviconModel
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject @field:NetworkScheduler internal lateinit var networkScheduler: Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler

    private val uiController: UIController

    // Adapter
    private var bookmarkAdapter: BookmarkListAdapter? = null

    // Colors
    private var scrollIndex: Int = 0

    private var bookmarksSubscription: Disposable? = null
    private var bookmarkUpdateSubscription: Disposable? = null

    private val uiModel = BookmarkUiModel()

    private var bookmarkRecyclerView: RecyclerView? = null
    private var backNavigationView: ImageView? = null
    private var addBookmarkView: ImageView? = null

    init {
        context.inflater.inflate(R.layout.bookmark_drawer, this, true)
        context.injector.inject(this)

        uiController = context as UIController

        bookmarkRecyclerView = findViewById(R.id.bookmark_list_view)
        backNavigationView = findViewById(R.id.bookmark_back_button)
        addBookmarkView = findViewById(R.id.action_add_bookmark)
        backNavigationView?.setOnClickListener {
            if (!uiModel.isCurrentFolderRoot()) {
                setBookmarksShown(null, true)
                bookmarkRecyclerView?.layoutManager?.scrollToPosition(scrollIndex)
            }
        }
        addBookmarkView?.setOnClickListener { uiController.bookmarkButtonClicked() }
        findViewById<View>(R.id.action_reading).setOnClickListener {
            getTabsManager().currentTab?.url?.let {
                ReadingActivity.launch(context, it)
            }
        }
        findViewById<View>(R.id.action_page_tools).setOnClickListener { showPageToolsDialog(context) }

        bookmarkAdapter = BookmarkListAdapter(
            context,
            faviconModel,
            networkScheduler,
            mainScheduler,
            ::handleItemLongPress,
            ::handleItemClick
        )

        bookmarkRecyclerView?.let {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = bookmarkAdapter
        }

        setBookmarksShown(null, true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        bookmarksSubscription?.dispose()
        bookmarkUpdateSubscription?.dispose()

        bookmarkAdapter?.cleanupSubscriptions()
    }

    private fun getTabsManager(): TabsManager = uiController.getTabModel()

    private fun updateBookmarkIndicator(url: String) {
        bookmarkUpdateSubscription?.dispose()
        bookmarkUpdateSubscription = bookmarkModel.isBookmark(url)
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { isBookmark ->
                bookmarkUpdateSubscription = null
                addBookmarkView?.isSelected = isBookmark
                addBookmarkView?.isEnabled = !url.isSpecialUrl()
            }
    }

    override fun handleBookmarkDeleted(bookmark: Bookmark) = when (bookmark) {
        is Bookmark.Folder -> setBookmarksShown(null, false)
        is Bookmark.Entry -> bookmarkAdapter?.deleteItem(BookmarksViewModel(bookmark)) ?: Unit
    }

    private fun setBookmarksShown(folder: String?, animate: Boolean) {
        bookmarksSubscription?.dispose()
        bookmarksSubscription = bookmarkModel.getBookmarksFromFolderSorted(folder)
            .concatWith(Single.defer {
                if (folder == null) {
                    bookmarkModel.getFoldersSorted()
                } else {
                    Single.just(emptyList())
                }
            })
            .toList()
            .map { it.flatten() }
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { bookmarksAndFolders ->
                uiModel.currentFolder = folder
                setBookmarkDataSet(bookmarksAndFolders, animate)
            }
    }

    private fun setBookmarkDataSet(items: List<Bookmark>, animate: Boolean) {
        bookmarkAdapter?.updateItems(items.map { BookmarksViewModel(it) })
        val resource = if (uiModel.isCurrentFolderRoot()) {
            R.drawable.ic_action_star
        } else {
            R.drawable.ic_action_back
        }

        if (animate) {
            backNavigationView?.let {
                val transition = AnimationUtils.createRotationTransitionAnimation(it, resource)
                it.startAnimation(transition)
            }
        } else {
            backNavigationView?.setImageResource(resource)
        }
    }

    private fun handleItemLongPress(bookmark: Bookmark): Boolean {
        (context as Activity?)?.let {
            when (bookmark) {
                is Bookmark.Folder -> bookmarksDialogBuilder.showBookmarkFolderLongPressedDialog(it, uiController, bookmark)
                is Bookmark.Entry -> bookmarksDialogBuilder.showLongPressedDialogForBookmarkUrl(it, uiController, bookmark)
            }
        }
        return true
    }

    private fun handleItemClick(bookmark: Bookmark) = when (bookmark) {
        is Bookmark.Folder -> {
            scrollIndex = (bookmarkRecyclerView?.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            setBookmarksShown(bookmark.title, true)
        }
        is Bookmark.Entry -> uiController.bookmarkItemClicked(bookmark)
    }


    /**
     * Show the page tools dialog.
     */
    private fun showPageToolsDialog(context: Context) {
        val currentTab = getTabsManager().currentTab ?: return
        val isAllowedAds = allowListModel.isUrlAllowedAds(currentTab.url)
        val whitelistString = if (isAllowedAds) {
            R.string.dialog_adblock_enable_for_site
        } else {
            R.string.dialog_adblock_disable_for_site
        }

        BrowserDialog.showWithIcons(context, context.getString(R.string.dialog_tools_title),
            DialogItem(
                icon = context.drawable(R.drawable.ic_action_desktop),
                title = R.string.dialog_toggle_desktop
            ) {
                getTabsManager().currentTab?.apply {
                    toggleDesktopUA()
                    reload()
                    // TODO add back drawer closing
                }
            },
                DialogItem(
                        icon= context.drawable(R.drawable.ic_page_tools),
                        title = R.string.inspect

                ){
                    val builder = AlertDialog.Builder(context)
                    val inflater = activity.layoutInflater
                    builder.setTitle(R.string.inspect)
                    val dialogLayout = inflater.inflate(R.layout.dialog_edit_text, null)
                    val editText  = dialogLayout.findViewById<EditText>(R.id.dialog_edit_text)
                    builder.setView(dialogLayout)
                    builder.setPositiveButton("OK") { dialogInterface, i -> currentTab.loadUrl("javascript:(function() {" + editText.text.toString() + "})()") }
                    builder.show()

                },
            DialogItem(
                icon = context.drawable(R.drawable.ic_block),
                colorTint = context.color(R.color.error_red).takeIf { isAllowedAds },
                title = whitelistString,
                isConditionMet = !currentTab.url.isSpecialUrl()
            ) {
                if (isAllowedAds) {
                    allowListModel.removeUrlFromAllowList(currentTab.url)
                } else {
                    allowListModel.addUrlToAllowList(currentTab.url)
                }
                getTabsManager().currentTab?.reload()
            }
        )
    }

    override fun navigateBack() {
        if (uiModel.isCurrentFolderRoot()) {
            uiController.onBackButtonPressed()
        } else {
            setBookmarksShown(null, true)
            bookmarkRecyclerView?.layoutManager?.scrollToPosition(scrollIndex)
        }
    }

    override fun handleUpdatedUrl(url: String) {
        updateBookmarkIndicator(url)
        val folder = uiModel.currentFolder
        setBookmarksShown(folder, false)
    }

    private class BookmarkViewHolder(
        itemView: View,
        private val adapter: BookmarkListAdapter,
        private val onItemLongClickListener: (Bookmark) -> Boolean,
        private val onItemClickListener: (Bookmark) -> Unit
    ) : RecyclerView.ViewHolder(itemView), OnClickListener, OnLongClickListener {

        val txtTitle: TextView = itemView.findViewById(R.id.textBookmark)
        val favicon: ImageView = itemView.findViewById(R.id.faviconBookmark)

        init {
            itemView.setOnLongClickListener(this)
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val index = adapterPosition
            if (index.toLong() != RecyclerView.NO_ID) {
                onItemClickListener(adapter.itemAt(index).bookmark)
            }
        }

        override fun onLongClick(v: View): Boolean {
            val index = adapterPosition
            return index != RecyclerView.NO_POSITION && onItemLongClickListener(adapter.itemAt(index).bookmark)
        }
    }

    private class BookmarkListAdapter(
        context: Context,
        private val faviconModel: FaviconModel,
        private val networkScheduler: Scheduler,
        private val mainScheduler: Scheduler,
        private val onItemLongClickListener: (Bookmark) -> Boolean,
        private val onItemClickListener: (Bookmark) -> Unit
    ) : RecyclerView.Adapter<BookmarkViewHolder>() {

        private var bookmarks: List<BookmarksViewModel> = listOf()
        private val faviconFetchSubscriptions = ConcurrentHashMap<String, Disposable>()
        private val folderIcon = context.drawable(R.drawable.ic_folder)
        private val webpageIcon = context.drawable(R.drawable.ic_webpage)

        fun itemAt(position: Int): BookmarksViewModel = bookmarks[position]

        fun deleteItem(item: BookmarksViewModel) {
            val newList = bookmarks - item
            updateItems(newList)
        }

        fun updateItems(newList: List<BookmarksViewModel>) {
            val oldList = bookmarks
            bookmarks = newList

            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldList.size

                override fun getNewListSize() = bookmarks.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    oldList[oldItemPosition].bookmark.url == bookmarks[newItemPosition].bookmark.url

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    oldList[oldItemPosition] == bookmarks[newItemPosition]
            })

            diffResult.dispatchUpdatesTo(this)
        }

        fun cleanupSubscriptions() {
            for (subscription in faviconFetchSubscriptions.values) {
                subscription.dispose()
            }
            faviconFetchSubscriptions.clear()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val itemView = inflater.inflate(R.layout.bookmark_list_item, parent, false)

            return BookmarkViewHolder(itemView, this, onItemLongClickListener, onItemClickListener)
        }

        override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
            holder.itemView.jumpDrawablesToCurrentState()

            val viewModel = bookmarks[position]
            holder.txtTitle.text = viewModel.bookmark.title

            val url = viewModel.bookmark.url
            holder.favicon.tag = url

            viewModel.icon?.let {
                holder.favicon.setImageBitmap(it)
                return
            }

            val imageDrawable = when (viewModel.bookmark) {
                is Bookmark.Folder -> folderIcon
                is Bookmark.Entry -> webpageIcon.also {
                    faviconFetchSubscriptions[url]?.dispose()
                    faviconFetchSubscriptions[url] = faviconModel
                        .faviconForUrl(url, viewModel.bookmark.title)
                        .subscribeOn(networkScheduler)
                        .observeOn(mainScheduler)
                        .subscribeBy(
                            onSuccess = { bitmap ->
                                viewModel.icon = bitmap
                                if (holder.favicon.tag == url) {
                                    holder.favicon.setImageBitmap(bitmap)
                                }
                            }
                        )
                }
            }

            holder.favicon.setImageDrawable(imageDrawable)
        }

        override fun getItemCount() = bookmarks.size
    }

}
