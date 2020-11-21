/*
 * Copyright 2014 A.C.R. Development
 */
package com.cookiegames.smartcookie.settings.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context.ACTIVITY_SERVICE
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.anthonycr.grant.PermissionsManager
import com.anthonycr.grant.PermissionsResultAction
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.bookmark.LegacyBookmarkImporter
import com.cookiegames.smartcookie.bookmark.NetscapeBookmarkFormatImporter
import com.cookiegames.smartcookie.database.bookmark.BookmarkExporter
import com.cookiegames.smartcookie.database.bookmark.BookmarkRepository
import com.cookiegames.smartcookie.di.DatabaseScheduler
import com.cookiegames.smartcookie.di.MainScheduler
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.dialog.DialogItem
import com.cookiegames.smartcookie.extensions.resizeAndShow
import com.cookiegames.smartcookie.extensions.snackbar
import com.cookiegames.smartcookie.extensions.toast
import com.cookiegames.smartcookie.log.Logger
import com.cookiegames.smartcookie.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.*
import javax.inject.Inject


class BookmarkSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var bookmarkRepository: BookmarkRepository
    @Inject internal lateinit var application: Application
    @Inject internal lateinit var netscapeBookmarkFormatImporter: NetscapeBookmarkFormatImporter
    @Inject internal lateinit var legacyBookmarkImporter: LegacyBookmarkImporter
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler
    @Inject internal lateinit var logger: Logger

    private var importSubscription: Disposable? = null
    private var exportSubscription: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)

        PermissionsManager
                .getInstance()
                .requestPermissionsIfNecessaryForResult(activity, REQUIRED_PERMISSIONS, null)

        clickablePreference(preference = SETTINGS_EXPORT, onClick = this::exportBookmarks)
        clickablePreference(preference = SETTINGS_IMPORT, onClick = this::importBookmarks)
        clickablePreference(preference = SETTINGS_DELETE_BOOKMARKS, onClick = this::deleteAllBookmarks)

        clickablePreference(preference = SETTINGS_SETTINGS_EXPORT, onClick = this::exportSettings)
        clickablePreference(preference = SETTINGS_SETTINGS_IMPORT, onClick = this::importBookmarks)
        clickablePreference(preference = SETTINGS_DELETE_SETTINGS, onClick = this::clearSettings)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_bookmarks)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        exportSubscription?.dispose()
        importSubscription?.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()

        exportSubscription?.dispose()
        importSubscription?.dispose()
    }

    private fun clearSettings() {
        val builder = MaterialAlertDialogBuilder(activity as Activity)
        builder.setTitle(getString(R.string.confirm))
        builder.setMessage(getString(R.string.clear))


        builder.setPositiveButton(resources.getString(R.string.action_ok)){dialogInterface , which ->
            Toast.makeText(getActivity(),
                    R.string.reset_settings, Toast.LENGTH_LONG).show()

            val handler = Handler()
            handler.postDelayed(Runnable {
                (activity?.getSystemService(ACTIVITY_SERVICE) as ActivityManager)
                        .clearApplicationUserData()
            }, 500)
        }
        builder.setNegativeButton(resources.getString(R.string.action_cancel)){dialogInterface , which ->

        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(true)
        alertDialog.show()
    }

    private fun exportSettings() {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(activity, REQUIRED_PERMISSIONS,
                object : PermissionsResultAction() {
                    override fun onGranted() {
                        var bookmarksExport = File(
                                Environment.getExternalStorageDirectory(),
                                "SettingsExport.txt")
                        var counter = 0
                        while (bookmarksExport.exists()) {
                            counter++
                            bookmarksExport = File(
                                    Environment.getExternalStorageDirectory(),
                                    "SettingsExport-$counter.txt")
                        }
                        val exportFile = bookmarksExport

                        val userPref = application.getSharedPreferences("settings", 0)
                        val allEntries: Map<String, *> = userPref!!.getAll()
                        for (entry in allEntries.entries) {
                        }

                        try {
                            val datfile = exportFile
                            Toast.makeText(context, "File is:  $datfile", Toast.LENGTH_LONG).show() //##4

                            val fOut = FileOutputStream(datfile)
                            val myOutWriter = OutputStreamWriter(fOut)
                            myOutWriter.append(allEntries.toString())
                            myOutWriter.close()
                            fOut.close()
                        } catch (e: IOException) {
                            Log.e("Exception", "File write failed: " + e.toString())
                        }
                    }

                    override fun onDenied(permission: String) {
                        val activity = activity
                        if (activity != null && !activity.isFinishing && isAdded) {
                            Utils.createInformativeDialog(activity, R.string.title_error, R.string.bookmark_export_failure)
                        } else {
                            application.toast(R.string.bookmark_export_failure)
                        }
                    }
                })
    }

    private fun exportBookmarks() {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(activity, REQUIRED_PERMISSIONS,
                object : PermissionsResultAction() {
                    @SuppressLint("CheckResult")
                    override fun onGranted() {
                        bookmarkRepository.getAllBookmarksSorted()
                                .subscribeOn(databaseScheduler)
                                .subscribe { list ->
                                    if (!isAdded) {
                                        return@subscribe
                                    }

                                    val exportFile = BookmarkExporter.createNewExportFile()
                                    exportSubscription?.dispose()
                                    exportSubscription = BookmarkExporter.exportBookmarksToFile(list, exportFile)
                                            .subscribeOn(databaseScheduler)
                                            .observeOn(mainScheduler)
                                            .subscribeBy(
                                                    onComplete = {
                                                        activity?.apply {
                                                            snackbar("${getString(R.string.bookmark_export_path)} ${exportFile.path}")
                                                        }
                                                    },
                                                    onError = { throwable ->
                                                        logger.log(TAG, "onError: exporting bookmarks", throwable)
                                                        val activity = activity
                                                        if (activity != null && !activity.isFinishing && isAdded) {
                                                            Utils.createInformativeDialog(activity, R.string.title_error, R.string.bookmark_export_failure)
                                                        } else {
                                                            application.toast(R.string.bookmark_export_failure)
                                                        }
                                                    }
                                            )
                                }
                    }

                    override fun onDenied(permission: String) {
                        val activity = activity
                        if (activity != null && !activity.isFinishing && isAdded) {
                            Utils.createInformativeDialog(activity, R.string.title_error, R.string.bookmark_export_failure)
                        } else {
                            application.toast(R.string.bookmark_export_failure)
                        }
                    }
                })
    }

    private fun importBookmarks() {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(activity, REQUIRED_PERMISSIONS,
                object : PermissionsResultAction() {
                    override fun onGranted() {
                        showImportBookmarkDialog(null)
                    }

                    override fun onDenied(permission: String) {
                        //TODO Show message
                    }
                })
    }

    private fun deleteAllBookmarks() {
        showDeleteBookmarksDialog()
    }

    private fun showDeleteBookmarksDialog() {
        BrowserDialog.showPositiveNegativeDialog(
                activity = activity as Activity,
                title = R.string.action_delete,
                message = R.string.action_delete_all_bookmarks,
                positiveButton = DialogItem(title = R.string.yes) {
                    bookmarkRepository
                            .deleteAllBookmarks()
                            .subscribeOn(databaseScheduler)
                            .subscribe()
                },
                negativeButton = DialogItem(title = R.string.no) {},
                onCancel = {}
        )
    }

    private fun loadFileList(path: File?): Array<File> {
        val file: File = path ?: File(Environment.getExternalStorageDirectory().toString())

        try {
            file.mkdirs()
        } catch (e: SecurityException) {
            logger.log(TAG, "Unable to make directory", e)
        }

        return (if (file.exists()) {
            file.listFiles()
        } else {
            arrayOf()
        }).apply {
            sortWith(SortName())
        }
    }

    private class SortName : Comparator<File> {

        override fun compare(a: File, b: File): Int {
            return if (a.isDirectory && b.isDirectory) {
                a.name.compareTo(b.name)
            } else if (a.isDirectory) {
                -1
            } else if (b.isDirectory) {
                1
            } else if (a.isFile && b.isFile) {
                a.name.compareTo(b.name)
            } else {
                1
            }
        }
    }

    private fun showImportBookmarkDialog(path: File?) {
        val builder = MaterialAlertDialogBuilder(activity as Activity)

        val title = getString(R.string.title_chooser)
        builder.setTitle(title + ": " + Environment.getExternalStorageDirectory())

        val fileList = loadFileList(path)
        val fileNames = fileList.map(File::getName).toTypedArray()

        builder.setItems(fileNames) { _, which ->
            if (fileList[which].isDirectory) {
                showImportBookmarkDialog(fileList[which])
            } else {
                Single.fromCallable(fileList[which]::inputStream)
                        .map {
                            if (fileList[which].extension == EXTENSION_HTML) {
                                netscapeBookmarkFormatImporter.importBookmarks(it)
                            } else {
                                legacyBookmarkImporter.importBookmarks(it)
                            }
                        }
                        .flatMap {
                            bookmarkRepository.addBookmarkList(it).andThen(Single.just(it.size))
                        }
                        .subscribeOn(databaseScheduler)
                        .observeOn(mainScheduler)
                        .subscribeBy(
                                onSuccess = { count ->
                                    activity?.apply {
                                        snackbar("$count ${getString(R.string.message_import)}")
                                    }
                                },
                                onError = {
                                    logger.log(TAG, "onError: importing bookmarks", it)
                                    val activity = activity
                                    if (activity != null && !activity.isFinishing && isAdded) {
                                        Utils.createInformativeDialog(activity, R.string.title_error, R.string.import_bookmark_error)
                                    } else {
                                        application.toast(R.string.import_bookmark_error)
                                    }
                                }
                        )
            }
        }
        builder.resizeAndShow()
    }

    companion object {

        private const val TAG = "BookmarkSettingsFrag"

        private const val EXTENSION_HTML = "html"

        private const val SETTINGS_EXPORT = "export_bookmark"
        private const val SETTINGS_IMPORT = "import_bookmark"
        private const val SETTINGS_DELETE_BOOKMARKS = "delete_bookmarks"
        private const val SETTINGS_SETTINGS_EXPORT = "export_settings"
        private const val SETTINGS_SETTINGS_IMPORT = "import_settings"
        private const val SETTINGS_DELETE_SETTINGS = "clear_settings"

        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
