/*
 * Copyright 2014 A.C.R. Development
 */
package com.cookiegames.smartcookie.download

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.os.Build
import android.text.format.Formatter
import android.view.View
import android.webkit.DownloadListener
import android.widget.CheckBox
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.cookiegames.smartcookie.permissions.PermissionsManager
import com.cookiegames.smartcookie.permissions.PermissionsResultAction
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.database.downloads.DownloadsRepository
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog.setDialogSize
import com.cookiegames.smartcookie.log.Logger
import com.cookiegames.smartcookie.preference.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import javax.inject.Inject

class LightningDownloadListener(context: Activity) : DownloadListener {
    private val mActivity: Activity

    @JvmField
    @Inject
    var userPreferences: UserPreferences? = null

    @JvmField
    @Inject
    var downloadHandler: DownloadHandler? = null

    @JvmField
    @Inject
    var downloadsRepository: DownloadsRepository? = null

    @JvmField
    @Inject
    var logger: Logger? = null

    override fun onDownloadStart(url: String, userAgent: String,
                                 contentDisposition: String, mimetype: String, contentLength: Long) {
        val fileName =
            DownloadHandler.getFileNameFromURL(url, contentDisposition, mimetype)
        val downloadSize: String = if (contentLength > 0) {
            Formatter.formatFileSize(mActivity, contentLength)
        } else {
            mActivity.getString(R.string.unknown_size)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            triggerDownload(url, userAgent, contentDisposition, mimetype, downloadSize, fileName)
        } else {
            PermissionsManager.instance.requestPermissionsIfNecessaryForResult(mActivity,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                object : PermissionsResultAction() {
                    override fun onGranted() {
                        triggerDownload(url, userAgent, contentDisposition, mimetype, downloadSize, fileName)
                    }

                    override fun onDenied(permission: String) {
                        //TODO show message
                        logger!!.log(TAG, "Permission denied: $permission")
                    }
                })
        }
    }

    private fun triggerDownload(url: String, userAgent: String, contentDisposition: String, mimetype: String, downloadSize: String, fileName: String) {
        val checkBoxView = View.inflate(mActivity, R.layout.download_dialog, null)
        val checkBox = checkBoxView.findViewById<View>(R.id.checkbox) as CheckBox
        checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            userPreferences!!.showDownloadConfirmation = !isChecked
        }
        checkBox.text = mActivity.resources.getString(R.string.dont_ask_again)

        val dialogClickListener =
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> downloadHandler!!.onDownloadStart(
                        mActivity,
                        userPreferences!!,
                        url,
                        userAgent,
                        contentDisposition,
                        mimetype,
                        downloadSize
                    )

                    DialogInterface.BUTTON_NEUTRAL -> {
                        val clipboard = getSystemService(
                            mActivity,
                            ClipboardManager::class.java
                        )
                        clipboard?.setPrimaryClip(ClipData.newPlainText("", url))
                        Toast.makeText(
                            mActivity,
                            mActivity.resources.getString(R.string.message_text_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
            }

        if (userPreferences!!.showDownloadConfirmation) {
            val builder = MaterialAlertDialogBuilder(mActivity) // dialog
            val message =
                mActivity.getString(R.string.dialog_download, downloadSize)
            val dialog: Dialog = builder.setTitle(fileName)
                .setMessage(message)
                .setView(checkBoxView)
                .setPositiveButton(
                    mActivity.resources.getString(R.string.action_download),
                    dialogClickListener
                )
                .setNeutralButton(
                    R.string.action_copy,
                    dialogClickListener
                )
                .setNegativeButton(
                    mActivity.resources.getString(R.string.action_cancel),
                    dialogClickListener
                ).show()
            setDialogSize(mActivity, dialog)
            logger!!.log(TAG, "Downloading: $fileName")
        } else {
            Toast.makeText(
                mActivity,
                mActivity.resources.getString(R.string.download_pending),
                Toast.LENGTH_LONG
            ).show()
            downloadHandler!!.onDownloadStart(
                mActivity,
                userPreferences!!,
                url,
                userAgent,
                contentDisposition,
                mimetype,
                downloadSize
            )
        }
    }

    companion object {
        private const val TAG = "LightningDownloader"
    }

    init {
        context.injector.inject(this)
        mActivity = context
    }
}