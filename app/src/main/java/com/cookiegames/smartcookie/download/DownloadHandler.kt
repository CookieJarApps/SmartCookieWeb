/*
 * Copyright 2014 A.C.R. Development
 */

// Copyright (C) 2020 CookieJarApps
// MPL-2.0
package com.cookiegames.smartcookie.download

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.cookiegames.smartcookie.BuildConfig
import com.cookiegames.smartcookie.MainActivity
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.constant.FILE
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.database.downloads.DownloadEntry
import com.cookiegames.smartcookie.database.downloads.DownloadsRepository
import com.cookiegames.smartcookie.di.DatabaseScheduler
import com.cookiegames.smartcookie.di.MainScheduler
import com.cookiegames.smartcookie.di.NetworkScheduler
import com.cookiegames.smartcookie.dialog.BrowserDialog.setDialogSize
import com.cookiegames.smartcookie.extensions.snackbar
import com.cookiegames.smartcookie.log.Logger
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.utils.FileUtils
import com.cookiegames.smartcookie.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.huxq17.download.Pump
import com.huxq17.download.core.DownloadListener
import com.huxq17.download.utils.LogUtil
import io.reactivex.Scheduler
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Handle download requests
 */
@Singleton
class DownloadHandler @Inject constructor(private val downloadsRepository: DownloadsRepository,
                                          private val downloadManager: DownloadManager,
                                          @param:DatabaseScheduler private val databaseScheduler: Scheduler,
                                          @param:NetworkScheduler private val networkScheduler: Scheduler,
                                          @param:MainScheduler private val mainScheduler: Scheduler,
                                          private val logger: Logger) {

    fun onDownloadStart(context: Activity, manager: UserPreferences, url: String, userAgent: String,
                            contentDisposition: String?, mimeType: String, contentSize: String) {
        logger.log(TAG, "DOWNLOAD: Trying to download from URL: $url")
        logger.log(TAG, "DOWNLOAD: Content disposition: $contentDisposition")
        logger.log(TAG, "DOWNLOAD: MimeType: $mimeType")
        logger.log(TAG, "DOWNLOAD: User agent: $userAgent")

        // if we're dealing wih A/V content that's not explicitly marked
        // for download, check if it's streamable.
        if (contentDisposition == null
                || !contentDisposition.regionMatches(0, "attachment", 0, 10, ignoreCase = true)) {
            // query the package manager to see if there's a registered handler
            // that matches.
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(url), mimeType)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.component = null
            intent.selector = null
            val info = context.packageManager.resolveActivity(intent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            if (info != null) {
                // If we resolved to ourselves, we don't want to attempt to
                // load the url only to try and download it again.
                if (BuildConfig.APPLICATION_ID == info.activityInfo.packageName || MainActivity::class.java.name == info.activityInfo.name) {
                    // someone (other than us) knows how to handle this mime
                    // type with this scheme, don't download.
                    try {
                        context.startActivity(intent)
                        return
                    } catch (ex: ActivityNotFoundException) {
                        // Best behavior is to fall back to a download in this
                        // case
                    }
                }
            }
        }

        if(manager.useNewDownloader){
            onDownloadStartNoStream(context, manager, url, userAgent, contentDisposition, mimeType, contentSize)
        }
        else{
            legacyOnDownloadStartNoStream(context, manager, url, userAgent, contentDisposition, mimeType, contentSize)
        }
    }
    fun onDownloadStartNoStream(context: Activity, manager: UserPreferences, url: String, userAgent: String,
                        contentDisposition: String?, mimeType: String, contentSize: String) {
        logger.log(TAG, "DOWNLOAD: Trying to download from URL: $url")
        logger.log(TAG, "DOWNLOAD: Content disposition: $contentDisposition")
        logger.log(TAG, "DOWNLOAD: MimeType: $mimeType")
        logger.log(TAG, "DOWNLOAD: User agent: $userAgent")

        var location
                = manager.downloadDirectory
        location = FileUtils.addNecessarySlashes(location)
        val downloadFolder = Uri.parse(location)

        if(url.toUri().scheme == "data"){
            Toast.makeText(context, R.string.data_scheme, Toast.LENGTH_LONG).show()
            return
        }

        val now = Date()
        val uniqid = SimpleDateFormat("ddHHmmss", Locale.US).format(now).toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = context.getString(R.string.download_channel)
            val description = context.getString(R.string.download_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("com.cookiegames.smartcookieweb.downloads", name, importance)
            channel.description = description
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val fileName = getFileNameFromURL(url, contentDisposition, mimeType)
        val notificationManager = NotificationManagerCompat.from(context)
        val builder = NotificationCompat.Builder(context, "com.cookiegames.smartcookieweb.downloads")
        Log.d(TAG, fileName)

        builder.setContentTitle(context.getString(R.string.action_download))
                .setContentText(fileName)
                .setSmallIcon(R.drawable.ic_file_download_black)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)

        // Issue the initial notification with zero progress
        val PROGRESS_MAX = 100
        val PROGRESS_CURRENT = 0
        builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false)
        notificationManager.notify(uniqid, builder.build())

        // Open DownloadActivity
        val intent = Intent(context, DownloadActivity::class.java)

        val rpIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        Pump.newRequest(url, "$downloadFolder/$fileName") //Set id,optionally
                .listener(object : DownloadListener() {
                    override fun onSuccess() {
                        notificationManager.cancel(uniqid)
                        builder.setContentTitle(context.getString(R.string.download_successful))
                                .setContentText(URLUtil.guessFileName(url, contentDisposition, mimeType))
                                .setSmallIcon(R.drawable.ic_file_download_black)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setContentIntent(rpIntent)
                                .setOnlyAlertOnce(true)
                        builder.setProgress(0, 0, false);
                        notificationManager.notify(uniqid + 1, builder.build())

                        val file = downloadInfo.filePath

                        MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null
                        ) { path, uri ->
                            Log.i("ExternalStorage", "Scanned $path:")
                            Log.i("ExternalStorage", "-> uri=$uri")
                        }

                    }
                    override fun onFailed() {
                        notificationManager.cancel(uniqid)
                        LogUtil.e("onFailed code=" + downloadInfo.errorCode)
                    }
                    override fun onProgress(progress: Int) {
                        if (progress.toString().contains("0")){
                            builder.setProgress(100, progress, false)
                            notificationManager.notify(uniqid, builder.build())
                        }

                    }
                })
            .threadNum(1)
            .submit()

    }

    class DownloadCancelReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("downloader", "null")
        }
    }

    /**
     * Notify the host application a download should be done, even if there is a
     * streaming viewer available for this type.
     *
     * @param context            The context in which the download is requested.
     * @param url                The full url to the content that should be downloaded
     * @param userAgent          User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype           The mimetype of the content reported by the server
     * @param contentSize        The size of the content
     */
    /* package */
    private fun legacyOnDownloadStartNoStream(context: Activity, preferences: UserPreferences,
                                        url: String, userAgent: String,
                                        contentDisposition: String?, mimetype: String?, contentSize: String) {
        val filename = getFileNameFromURL(url, contentDisposition, mimetype)

        // Check to see if we have an SDCard
        val status = Environment.getExternalStorageState()
        if (status != Environment.MEDIA_MOUNTED) {
            val title: Int
            val msg: String

            // Check to see if the SDCard is busy, same as the music app
            if (status == Environment.MEDIA_SHARED) {
                msg = context.getString(R.string.download_sdcard_busy_dlg_msg)
                title = R.string.download_sdcard_busy_dlg_title
            } else {
                msg = context.getString(R.string.download_no_sdcard_dlg_msg)
                title = R.string.download_no_sdcard_dlg_title
            }
            val dialog: Dialog = MaterialAlertDialogBuilder(context).setTitle(title)
                    .setIcon(android.R.drawable.ic_dialog_alert).setMessage(msg)
                    .setPositiveButton(R.string.action_ok, null).show()
            setDialogSize(context, dialog)
            return
        }

        // java.net.URI is a lot stricter than KURL so we have to encode some
        // extra characters. Fix for b 2538060 and b 1634719
        val webAddress: WebAddress
        try {
            webAddress = WebAddress(url)
            webAddress.path = encodePath(webAddress.path)
        } catch (e: Exception) {
            // This only happens for very bad urls, we want to catch the
            // exception here
            logger.log(TAG, "Exception while trying to parse url '$url'", e)
            context.snackbar(R.string.problem_download)
            return
        }
        val addressString = webAddress.toString()
        val uri = Uri.parse(addressString)
        val request: DownloadManager.Request = try {
            DownloadManager.Request(uri)
        } catch (e: IllegalArgumentException) {
            context.snackbar(R.string.cannot_download)
            return
        }

        // set downloaded file destination to /sdcard/Download.
        // or, should it be set to one of several Environment.DIRECTORY* dirs
        // depending on mimetype?
        var location = preferences.downloadDirectory
        location = FileUtils.addNecessarySlashes(location)
        val downloadFolder = Uri.parse(location)
        if (!isWriteAccessAvailable(downloadFolder)) {
            context.snackbar(R.string.problem_location_download)
            return
        }
        val newMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(filename)
        logger.log(TAG, "New mimetype: $newMimeType")
        request.setMimeType(newMimeType)
        request.setDestinationUri(Uri.parse(FILE + location + filename))
        // let this downloaded file be scanned by MediaScanner - so that it can
        // show up in Gallery app, for example.
        request.setVisibleInDownloadsUi(true)
        request.allowScanningByMediaScanner()
        request.setDescription(webAddress.host)
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
        val cookies = CookieManager.getInstance().getCookie(url)
        request.addRequestHeader(COOKIE_REQUEST_HEADER, cookies)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        if (mimetype == null) {
            logger.log(TAG, "Mimetype is null")
            if (TextUtils.isEmpty(addressString)) {
                return
            }
            // We must have long pressed on a link or image to download it. We
            // are not sure of the mimetype in this case, so do a head request
            val disposable = FetchUrlMimeType(downloadManager, request, addressString, cookies, userAgent)
                    .create()
                    .subscribeOn(networkScheduler)
                    .observeOn(mainScheduler)
                    .subscribe { result: FetchUrlMimeType.Result? ->
                        when (result) {
                            FetchUrlMimeType.Result.FAILURE_ENQUEUE -> context.snackbar(R.string.cannot_download)
                            FetchUrlMimeType.Result.FAILURE_LOCATION -> context.snackbar(R.string.problem_location_download)
                            FetchUrlMimeType.Result.SUCCESS -> context.snackbar(R.string.download_pending)
                            else -> context.snackbar(R.string.cannot_download)
                        }
                    }
        } else {
            logger.log(TAG, "Valid mimetype, attempting to download")
            try {
                downloadManager.enqueue(request)
            } catch (e: IllegalArgumentException) {
                // Probably got a bad URL or something
                logger.log(TAG, "Unable to enqueue request", e)
                context.snackbar(R.string.cannot_download)
            } catch (e: SecurityException) {
                // TODO write a download utility that downloads files rather than rely on the system
                // because the system can only handle Environment.getExternal... as a path
                context.snackbar(R.string.problem_location_download)
            }
            context.snackbar(context.getString(R.string.download_pending) + ' ' + filename)
        }

        // save download in database
        val browserActivity = context as UIController
        val view = browserActivity.getTabModel().currentTab
        if (view != null && !view.isIncognito) {
            downloadsRepository.addDownloadIfNotExists(DownloadEntry(url, filename, contentSize))
                    .subscribeOn(databaseScheduler)
                    .subscribe { aBoolean: Boolean? ->
                        if (!aBoolean!!) {
                            logger.log(TAG, "error saving download to database")
                        }
                    }
        }
    }

    companion object {
        private const val TAG = "DownloadHandler"
        private const val COOKIE_REQUEST_HEADER = "Cookie"

        fun getFileNameFromURL(url: String?, contentDisposition: String?, mimeType: String?): String {
            return if(contentDisposition?.contains("filename=") == true) {
                var fileName: String? = ContentDispositionFileNameParser.parse(contentDisposition)
                fileName = URLDecoder.decode(fileName, StandardCharsets.ISO_8859_1.toString())
                fileName
            } else {
                var substring: String? = url!!.substring(url.lastIndexOf('/') + 1).split("\\?")[0].split("#")[0]
                if(substring?.contains(".") == true) {
                    substring
                } else {
                    URLUtil.guessFileName(url, contentDisposition, mimeType)
                }
            }
        }

        private fun isWriteAccessAvailable(fileUri: Uri): Boolean {
            if (fileUri.path == null) {
                return false
            }
            val file = File(fileUri.path)
            return if (!file.isDirectory && !file.mkdirs()) {
                false
            } else try {
                if (file.createNewFile()) {
                    file.delete()
                }
                true
            } catch (ignored: IOException) {
                false
            }
        }

        // This is to work around the fact that java.net.URI throws Exceptions
        // instead of just encoding URL's properly
        // Helper method for onDownloadStartNoStream
        private fun encodePath(path: String): String {
            val chars = path.toCharArray()
            var needed = false
            for (c in chars) {
                if (c == '[' || c == ']' || c == '|') {
                    needed = true
                    break
                }
            }
            if (!needed) {
                return path
            }
            val sb = StringBuilder()
            for (c in chars) {
                if (c == '[' || c == ']' || c == '|') {
                    sb.append('%')
                    sb.append(Integer.toHexString(c.code))
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }
    }

}