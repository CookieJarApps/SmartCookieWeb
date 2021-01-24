package com.cookiegames.smartcookie.database.javascript

import com.cookiegames.smartcookie.database.databaseDelegate
import com.cookiegames.smartcookie.extensions.firstOrNullMap
import com.cookiegames.smartcookie.extensions.useMap
import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.WorkerThread
import com.cookiegames.smartcookie.database.WebPage
import com.cookiegames.smartcookie.database.downloads.DownloadsDatabase
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton


/**
 * The disk backed download database. See [JavaScriptRepository] for function documentation.
 */
@Singleton
@WorkerThread
class JavaScriptDatabase @Inject constructor(
        application: Application
) : SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION), JavaScriptRepository {

    private val database: SQLiteDatabase by databaseDelegate()

    data class JavaScriptEntry(
            val name: String,
            val urlList: String,
            val code: String
    )


    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val createJavaScriptTable = "CREATE TABLE $TABLE_JAVASCRIPT(" +
                " $KEY_ID INTEGER PRIMARY KEY," +
                " $KEY_NAME TEXT," +
                " $KEY_URL_LIST TEXT," +
                " $KEY_CODE TEXT" +
                ")"
        db.execSQL(createJavaScriptTable)
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS $TABLE_JAVASCRIPT")
        // Create tables again
        onCreate(db)
    }

    override fun deleteJavaScript(): Completable = Completable.fromAction {
        database.run {
            delete(TABLE_JAVASCRIPT, null, null)
            close()
        }
    }

    override fun deleteJavaScriptEntry(url: String): Completable = Completable.fromAction {
        database.delete(TABLE_JAVASCRIPT, "$KEY_NAME = ?", arrayOf(url))
    }

    override fun findJavaScriptEntriesContaining(query: String): Single<List<JavaScriptEntry>> =
            Single.fromCallable {
                val search = "%$query%"

                return@fromCallable database.query(
                        TABLE_JAVASCRIPT,
                        null,
                        "$KEY_NAME LIKE ? OR $KEY_URL_LIST LIKE ?",
                        arrayOf(search, search),
                        null,
                        null,
                        "$KEY_ID DESC",
                        "5"
                ).useMap { it.bindToJavaScriptEntry() }
            }

    override fun lastHundredVisitedJavaScriptEntries(): Single<List<JavaScriptEntry>> =
            Single.fromCallable {
                database.query(
                        TABLE_JAVASCRIPT,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "$KEY_ID DESC",
                        "100"
                ).useMap { it.bindToJavaScriptEntry() }
            }

    override fun addJavaScriptIfNotExists(entry: JavaScriptEntry): Single<Boolean> = Single.fromCallable {
        database.query(
                TABLE_JAVASCRIPT,
                null,
                "${KEY_NAME}=?",
                arrayOf(entry.name),
                null,
                null,
                "1"
        ).use {
            if (it.moveToFirst()) {
                return@fromCallable false
            }
        }

        val id = database.insert(TABLE_JAVASCRIPT, null, entry.toContentValues())

        return@fromCallable id != -1L
    }

    @WorkerThread
    private fun addJavaScriptEntry(item: JavaScriptEntry) {
        database.insert(TABLE_JAVASCRIPT, null, item.toContentValues())
    }

    @WorkerThread
    fun getJavaScriptEntry(url: String): String? =
            database.query(
                    TABLE_JAVASCRIPT,
                    arrayOf(KEY_ID, KEY_URL_LIST, KEY_CODE, KEY_NAME),
                    "$KEY_URL_LIST = ?",
                    arrayOf(url),
                    null,
                    null,
                    null,
                    "1"
            ).firstOrNullMap { it.getString(0) }


    fun getAllJavaScriptEntries(): List<JavaScriptEntry> {
        return database.query(
                TABLE_JAVASCRIPT,
                null,
                null,
                null,
                null,
                null,
                "$KEY_ID DESC"
        ).useMap { it.bindToJavaScriptEntry() }
    }

    fun getJavaScriptEntriesCount(): Long = DatabaseUtils.queryNumEntries(database, TABLE_JAVASCRIPT)

    private fun JavaScriptEntry.toContentValues() = ContentValues().apply {
        put(KEY_URL_LIST, urlList)
        put(KEY_NAME, name)
        put(KEY_CODE, code)
    }

    private fun Cursor.bindToJavaScriptEntry() = JavaScriptEntry(
            name = getString(1),
            urlList = getString(2),
            code = getString(3)
    )

    companion object {

        // Database version
        private const val DATABASE_VERSION = 1

        // Database name
        private const val DATABASE_NAME = "javascriptManager"

        // JavaScriptEntry table name
        private const val TABLE_JAVASCRIPT = "javascript"

        // JavaScriptEntry table columns names
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_URL_LIST = "urls"
        private const val KEY_CODE = "code"

    }
}
