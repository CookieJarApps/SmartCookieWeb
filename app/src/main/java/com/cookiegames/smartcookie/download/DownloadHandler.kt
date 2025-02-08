/*
 * Copyright 2014 A.C.R. Development
 */

package com.cookiegames.smartcookie.download

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import com.cookiegames.smartcookie.BuildConfig
import com.cookiegames.smartcookie.MainActivity
import com.cookiegames.smartcookie.database.downloads.DownloadsRepository
import com.cookiegames.smartcookie.di.DatabaseScheduler
import com.cookiegames.smartcookie.di.MainScheduler
import com.cookiegames.smartcookie.di.NetworkScheduler
import com.cookiegames.smartcookie.download.ContentDispositionFileNameParser
import com.cookiegames.smartcookie.log.Logger
import com.cookiegames.smartcookie.preference.UserPreferences
import io.reactivex.Scheduler
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadHandler @Inject constructor(
    private val downloadsRepository: DownloadsRepository,
    private val downloadManager: DownloadManager,
    @param:DatabaseScheduler private val databaseScheduler: Scheduler,
    @param:NetworkScheduler private val networkScheduler: Scheduler,
    @param:MainScheduler private val mainScheduler: Scheduler,
    private val logger: Logger
) {

    fun onDownloadStart(
        context: Activity, manager: UserPreferences, url: String, userAgent: String,
        contentDisposition: String?, mimeType: String, contentSize: String
    ) {
        logger.log(TAG, "DOWNLOAD: Trying to download from URL: $url")
        logger.log(TAG, "DOWNLOAD: Content disposition: $contentDisposition")
        logger.log(TAG, "DOWNLOAD: MimeType: $mimeType")
        logger.log(TAG, "DOWNLOAD: User agent: $userAgent")

        if (contentDisposition == null || !contentDisposition.regionMatches(0, "attachment", 0, 10, ignoreCase = true)) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(url), mimeType)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.component = null
            intent.selector = null
            val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (info != null) {
                if (BuildConfig.APPLICATION_ID == info.activityInfo.packageName || MainActivity::class.java.name == info.activityInfo.name) {
                    try {
                        context.startActivity(intent)
                        return
                    } catch (ex: ActivityNotFoundException) {
                    }
                }
            }
        }

        onDownloadStartNoStream(context, manager, url, userAgent, contentDisposition, mimeType, contentSize)
    }

    fun onDownloadStartNoStream(
        context: Activity, manager: UserPreferences, url: String, userAgent: String,
        contentDisposition: String?, mimeType: String, contentSize: String
    ) {
        logger.log(TAG, "DOWNLOAD: Trying to download from URL: $url")
        logger.log(TAG, "DOWNLOAD: Content disposition: $contentDisposition")
        logger.log(TAG, "DOWNLOAD: MimeType: $mimeType")
        logger.log(TAG, "DOWNLOAD: User agent: $userAgent")

        if (manager.useThirdPartyDownloaderApps) {
            // Show dialog listing third party downloaders
        } else {
            val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val outputStream: OutputStream?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
                }
                val uri: Uri? = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename)
                outputStream = FileOutputStream(file)
            }

            outputStream?.use {
                downloadManager.enqueue(
                    DownloadManager.Request(Uri.parse(url))
                        .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                        .setAllowedOverRoaming(false)
                        .setTitle(filename)
                        .setDescription(contentSize)
                        .setMimeType(mimeType)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationUri(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) null else Uri.fromFile(File(it.toString())))
                        .addRequestHeader(COOKIE_REQUEST_HEADER, CookieManager.getInstance().getCookie(url))
                )
            }
        }
    }

    companion object {
        private const val TAG = "DownloadHandler"
        private const val COOKIE_REQUEST_HEADER = "Cookie"

        fun getFileNameFromURL(url: String?, contentDisposition: String?, mimeType: String?): String {
            return if (contentDisposition?.contains("filename=") == true) {
                var fileName: String? = ContentDispositionFileNameParser.parse(contentDisposition)
                fileName = URLDecoder.decode(fileName, StandardCharsets.ISO_8859_1.toString())
                fileName
            } else {
                var substring: String? = url!!.substring(url.lastIndexOf('/') + 1).split("\\?")[0].split("#")[0]
                if (substring?.contains(".") == true) {
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