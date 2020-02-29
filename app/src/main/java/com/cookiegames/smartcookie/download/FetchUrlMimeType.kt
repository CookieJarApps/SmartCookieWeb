/*
 * Copyright 2014 A.C.R. Development
 */
package com.cookiegames.smartcookie.download

import android.app.DownloadManager
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.URLUtil

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

import com.cookiegames.smartcookie.utils.Utils
import io.reactivex.Single

/**
 * This class is used to pull down the http headers of a given URL so that we
 * can analyse the mimetype and make any correction needed before we give the
 * URL to the download manager. This operation is needed when the user
 * long-clicks on a link or image and we don't know the mimetype. If the user
 * just clicks on the link, we will do the same steps of correcting the mimetype
 * down in android.os.webkit.LoadListener rather than handling it here.
 */
internal class FetchUrlMimeType(private val mDownloadManager: DownloadManager,
                                private val mRequest: DownloadManager.Request,
                                private val mUri: String,
                                private val mCookies: String?,
                                private val mUserAgent: String) {

    fun create(): Single<Result> {
        return Single.create { emitter ->
            // User agent is likely to be null, though the AndroidHttpClient
            // seems ok with that.
            var mimeType: String? = null
            var contentDisposition: String? = null
            var connection: HttpURLConnection? = null
            try {
                val url = URL(mUri)
                connection = url.openConnection() as HttpURLConnection
                if (mCookies != null && !mCookies.isEmpty()) {
                    connection.addRequestProperty("Cookie", mCookies)
                    connection.setRequestProperty("User-Agent", mUserAgent)
                }
                connection.connect()
                // We could get a redirect here, but if we do lets let
                // the download manager take care of it, and thus trust that
                // the server sends the right mimetype
                if (connection.responseCode == 200) {
                    val header = connection.getHeaderField("Content-Type")
                    if (header != null) {
                        mimeType = header
                        val semicolonIndex = mimeType.indexOf(';')
                        if (semicolonIndex != -1) {
                            mimeType = mimeType.substring(0, semicolonIndex)
                        }
                    }
                    val contentDispositionHeader = connection.getHeaderField("Content-Disposition")
                    if (contentDispositionHeader != null) {
                        contentDisposition = contentDispositionHeader
                    }
                }
            } catch (ex: IllegalArgumentException) {
                connection?.disconnect()
            } catch (ex: IOException) {
                connection?.disconnect()
            } finally {
                connection?.disconnect()
            }

            if (mimeType != null) {
                if (mimeType.equals("text/plain", ignoreCase = true) || mimeType.equals("application/octet-stream", ignoreCase = true)) {
                    val newMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                            Utils.guessFileExtension(mUri))
                    if (newMimeType != null) {
                        mRequest.setMimeType(newMimeType)
                    }
                }
                val filename = URLUtil.guessFileName(mUri, contentDisposition, mimeType)
                mRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            }

            // Start the download
            try {
                mDownloadManager.enqueue(mRequest)
                emitter.onSuccess(Result.SUCCESS)
            } catch (e: IllegalArgumentException) {
                // Probably got a bad URL or something
                Log.e(TAG, "Unable to enqueue request", e)
                emitter.onSuccess(Result.FAILURE_ENQUEUE)
            } catch (e: SecurityException) {
                // TODO write a download utility that downloads files rather than rely on the system
                // because the system can only handle Environment.getExternal... as a path
                emitter.onSuccess(Result.FAILURE_LOCATION)
            }
        }
    }

    internal enum class Result {
        FAILURE_ENQUEUE,
        FAILURE_LOCATION,
        SUCCESS
    }

    companion object {

        private val TAG = "FetchUrlMimeType"
    }
}
