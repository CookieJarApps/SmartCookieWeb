/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 10/01/2020 */

package com.cookiegames.smartcookie.download

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.Filter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import butterknife.ButterKnife
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.BuildConfig
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.utils.ThemeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.huxq17.download.Pump
import com.huxq17.download.core.DownloadInfo
import com.huxq17.download.core.DownloadListener
import com.huxq17.download.utils.LogUtil
import kotlinx.android.synthetic.main.download_item.view.*
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap


class DownloadActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    @JvmField
    @Inject
    var mUserPreferences: UserPreferences? = null

    private var downloadObserver: DownloadListener = object : DownloadListener() {
        override fun onProgress(progress: Int) {
            val downloadInfo = downloadInfo
            val viewHolder = downloadInfo.extraData as DownloadViewHolder?
            val tag = map[viewHolder]
            if (tag != null && tag.id == downloadInfo.id) {
                viewHolder?.bindData(downloadInfo, status)
            }
        }

        override fun onFailed() {
            super.onFailed()
            LogUtil.e("onFailed code=" + downloadInfo.errorCode)
        }
    }

    private val map = HashMap<DownloadViewHolder, DownloadInfo>()
    private var downloadAdapter: DownloadAdapter? = null
    private lateinit var downloadInfoList: MutableList<DownloadInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        this.injector.inject(this)

        val color: Int
        if (mUserPreferences!!.useTheme === AppTheme.LIGHT) {
            setTheme(R.style.Theme_SettingsTheme)
            color = ThemeUtils.getColorBackground(this)
            window.setBackgroundDrawable(ColorDrawable(color))
        } else if (mUserPreferences!!.useTheme === AppTheme.DARK) {
            setTheme(R.style.Theme_SettingsTheme_Dark)
            color = ThemeUtils.getColorBackground(this)
            window.setBackgroundDrawable(ColorDrawable(color))
        } else {
            setTheme(R.style.Theme_SettingsTheme_Black)
            color = ThemeUtils.getColorBackground(this)
            window.setBackgroundDrawable(ColorDrawable(color))
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        ButterKnife.bind(this)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val downloadInfoList  = Pump.getAllDownloadList()
        val list = findViewById<RecyclerView>(R.id.downloads)
        val linearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        //Sort download list if need.
        Collections.sort(downloadInfoList) { o1, o2 -> (o1.createTime - o2.createTime).toInt() }
        list.layoutManager = linearLayoutManager
        downloadAdapter = DownloadAdapter(map, downloadInfoList)
        list.adapter = downloadAdapter
        downloadObserver.enable()

        //downloadAdapter!!.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            else -> finish()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onDestroy() {
        super.onDestroy()
        downloadObserver.disable()
        /*for (downloadInfo in downloadInfoList) {
            Pump.stop(downloadInfo.id)
        }
        Pump.shutdown()*/
    }

    class DownloadAdapter(var map: HashMap<DownloadViewHolder, DownloadInfo>, var downloadInfoList: MutableList<DownloadInfo>) : androidx.recyclerview.widget.RecyclerView.Adapter<DownloadViewHolder>() {

        lateinit var filtered: MutableList<DownloadInfo>
        lateinit var oldList: MutableList<DownloadInfo>

        fun getFilter(): Filter? {
            return object : Filter() {
                override fun performFiltering(charSequence: CharSequence): FilterResults? {
                    val charString = charSequence.toString()
                    if (charString.isEmpty()) {
                        filtered = oldList
                    } else {
                        val filteredList: MutableList<DownloadInfo> = ArrayList()
                        for (row in oldList) {
                            if (row.getName().toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(row)
                            }
                        }
                        filtered = filteredList
                    }
                    val filterResults = FilterResults()
                    filterResults.values = filtered
                    return filterResults
                }

                override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults) {
                    if(filterResults.values != null){
                        downloadInfoList = filterResults.values as MutableList<DownloadInfo>

                        notifyDataSetChanged()
                    }
                }
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): DownloadViewHolder {
            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.download_item, viewGroup, false)
            oldList = downloadInfoList
            return DownloadViewHolder(v, this)
        }

        override fun onBindViewHolder(viewHolder: DownloadViewHolder, i: Int) {
            val downloadInfo = downloadInfoList[i]
            viewHolder.bindData(downloadInfo, downloadInfo.status)

            downloadInfo.extraData = viewHolder
            map[viewHolder] = downloadInfo
        }

        fun delete(viewHolder: DownloadViewHolder) {
            val position = viewHolder.adapterPosition
            downloadInfoList.removeAt(position)
            notifyItemRemoved(position)
            map.remove(viewHolder)
        }

        override fun getItemCount(): Int {
            return downloadInfoList.size
        }
    }

    class DownloadViewHolder(itemView: View, adapter: DownloadAdapter) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        lateinit var downloadInfo: DownloadInfo
        lateinit var status: DownloadInfo.Status
        private var totalSizeString: String? = null
        var totalSize: Long = 0
        var dialog: AlertDialog

        init {
            itemView.dl_status.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            dialog = MaterialAlertDialogBuilder(itemView.context)
                    .setTitle(itemView.context.resources.getString(R.string.confirm_delete))
                    .setPositiveButton(itemView.context.resources.getString(R.string.yes)) { _, _ ->
                        adapter.delete(this@DownloadViewHolder)
                        Pump.deleteById(downloadInfo.id)
                    }
                    .setNegativeButton(itemView.context.resources.getString(R.string.no)) { _, _ -> }
                    .create()
        }

        fun bindData(downloadInfo: DownloadInfo, status: DownloadInfo.Status) {
            this.downloadInfo = downloadInfo
            this.status = status
            itemView.dl_name.text = downloadInfo.name
            itemView.dl_name.isSelected = true
            var speed = ""
            val progress = downloadInfo.progress
            itemView.dl_progress.progress = progress
            when (status) {
                DownloadInfo.Status.STOPPED -> itemView.dl_status.text = itemView.context.getString(R.string.start_download)
                DownloadInfo.Status.PAUSING -> itemView.dl_status.text = itemView.context.getString(R.string.pausing_download)
                DownloadInfo.Status.PAUSED -> itemView.dl_status.text = itemView.context.getString(R.string.continue_download)
                DownloadInfo.Status.WAIT -> itemView.dl_status.text = itemView.context.getString(R.string.waiting_download)
                DownloadInfo.Status.RUNNING -> {
                    itemView.dl_status.text = itemView.context.getString(R.string.pause_download)
                    speed = downloadInfo.speed
                }
                DownloadInfo.Status.FINISHED -> itemView.dl_status.text = itemView.context.getString(R.string.action_open)
                else -> itemView.dl_status.text = itemView.context.getString(R.string.title_error)
            }
            itemView.dl_speed.text = speed
            val completedSize = downloadInfo.completedSize
            if (totalSize == 0L) {
                val totalSize = downloadInfo.contentLength
                totalSizeString = "/" + DownloadUtil.getDataSize(totalSize)
            }
            itemView.dl_download.text = DownloadUtil.getDataSize(completedSize) + totalSizeString!!
            when(File(downloadInfo.filePath).extension){
                "pdf" -> itemView.dl_icon.setImageResource(R.drawable.icon_pdf)
                "zip" -> itemView.dl_icon.setImageResource(R.drawable.icon_zip)
                "apk" -> itemView.dl_icon.setImageResource(R.drawable.icon_apk)
                "txt", "doc", "docx" -> itemView.dl_icon.setImageResource(R.drawable.icon_txt)
                "jpg", "jpeg", "gif", "png" -> itemView.dl_icon.setImageResource(R.drawable.icon_img)
                "bin" -> itemView.dl_icon.setImageResource(R.drawable.icon_bin)
            }

        }

        fun openFile(filePath: String, v: View){
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            val fileURI = filePath.toUri()

            val finalUri = FileProvider.getUriForFile(v.context, BuildConfig.APPLICATION_ID + ".fileprovider", File(filePath))
            intent.setDataAndType(finalUri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExtension(filePath)))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (intent.resolveActivity(v.context.packageManager) != null) {
                v.context.startActivity(intent)
            } else {
                Toast.makeText(v.context, v.context.resources.getString(R.string.title_error), Toast.LENGTH_LONG).show()
            }
            v.context.startActivity(intent)
        }

        fun getFileExtension(filename: String?): String? {
            if (filename == null) {
                return null
            }
            val lastUnixPos = filename.lastIndexOf('/')
            val lastWindowsPos = filename.lastIndexOf('\\')
            val indexOfLastSeparator = Math.max(lastUnixPos, lastWindowsPos)
            val extensionPos = filename.lastIndexOf('.')
            val indexOfExtension = if (indexOfLastSeparator > extensionPos) -1 else extensionPos
            return if (indexOfExtension == -1) {
                null
            } else {
                filename.substring(indexOfExtension + 1).toLowerCase()
            }
        }

        override fun onClick(v: View) {
            if (v === itemView.dl_status) {
                when (status) {
                    DownloadInfo.Status.STOPPED -> Pump.newRequest(downloadInfo.url, downloadInfo.filePath)
                            .setId(downloadInfo.id)
                            .submit()
                    DownloadInfo.Status.PAUSED -> Pump.resume(downloadInfo.id)
                    DownloadInfo.Status.WAIT -> {
                    }
                    DownloadInfo.Status.RUNNING -> Pump.pause(downloadInfo.id)
                    DownloadInfo.Status.FINISHED -> openFile(downloadInfo.filePath, v)
                    else -> Pump.resume(downloadInfo.id)
                }
            }

        }

        override fun onLongClick(v: View): Boolean {
            dialog.show()
            return true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.download, menu)

        val searchItem: MenuItem = menu.findItem(R.id.action_search)
        val searchView: SearchView = searchItem.getActionView() as SearchView
        searchView.setOnQueryTextListener(this)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onQueryTextChange(query: String?): Boolean {
        downloadAdapter?.getFilter()?.filter(query)
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }
}