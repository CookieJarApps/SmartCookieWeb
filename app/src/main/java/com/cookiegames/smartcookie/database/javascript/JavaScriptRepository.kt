package com.cookiegames.smartcookie.database.javascript

import io.reactivex.Completable
import io.reactivex.Single

/**
 * An interface that should be used to communicate with the javascript database.
 *
 */
interface JavaScriptRepository {

    /**
     * An observable that deletes browser history.
     *
     * @return a valid observable.
     */
    fun deleteJavaScript(): Completable

    /**
     * An observable that deletes the history entry with the specific URL.
     *
     * @param url the URL of the item to delete.
     * @return a valid observable.
     */
    fun deleteJavaScriptEntry(name: String): Completable

    /**
     * An observable that finds all history items containing the given query. If the query is
     * contained anywhere within the title or the URL of the history item, it will be returned. For
     * the sake of performance, only the first five items will be emitted.
     *
     * @param query the query to search for.
     * @return a valid observable that emits
     * a list of history items.
     */
    fun findJavaScriptEntriesContaining(query: String): Single<List<JavaScriptDatabase.JavaScriptEntry>>

    /**
     * An observable that emits a list of the last 100 visited history items.
     *
     * @return a valid observable that emits a list of history items.
     */
    fun lastHundredVisitedJavaScriptEntries(): Single<List<JavaScriptDatabase.JavaScriptEntry>>


    /**
     * Adds a script if one does not already exist with the same URL.
     *
     * @param entry the script to add.
     * @return an observable that emits true if the download was added, false otherwise.
     */
    fun addJavaScriptIfNotExists(entry: JavaScriptDatabase.JavaScriptEntry): Single<Boolean>
}
