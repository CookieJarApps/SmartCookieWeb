package com.cookiegames.smartcookie.html.incognito

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.constant.FILE
import com.cookiegames.smartcookie.constant.UTF8
import com.cookiegames.smartcookie.database.history.HistoryDatabase
import com.cookiegames.smartcookie.database.history.HistoryRepository
import com.cookiegames.smartcookie.html.HtmlPageFactory
import com.cookiegames.smartcookie.html.ListPageReader
import com.cookiegames.smartcookie.html.homepage.HomePageReader
import com.cookiegames.smartcookie.html.jsoup.*
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.search.SearchEngineProvider
import dagger.Reusable
import io.reactivex.Single
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

/**
 * A factory for the home page.
 */
@Reusable
class IncognitoPageFactory @Inject constructor(
        private val application: Application,
        private val searchEngineProvider: SearchEngineProvider,
        private val incognitoPageReader: IncognitoPageReader,
        private var userPreferences: UserPreferences,
        private var resources: Resources,
        private val historyRepository: HistoryRepository,
        private val listPageReader: ListPageReader
) : HtmlPageFactory {

    private val title = application.getString(R.string.home)

    override fun buildPage(): Single<String> = Single
            .just(searchEngineProvider.provideSearchEngine())
            .map { (iconUrl, queryUrl, _) ->
                parse(incognitoPageReader.provideHtml()) andBuild {
                    title { title }
                    charset { UTF8 }
                    body {
                        id("search_input") { attr("style", "background: url('" + iconUrl + "') no-repeat scroll 7px 7px;background-size: 22px 22px;") }
                        tag("script") {
                            html(
                                    html()
                                                .replace("\${BASE_URL}", queryUrl)
                                                .replace("&", "\\u0026")


                            )
                        }

                        id("title-pm"){text(resources.getString(R.string.private_title))}
                        id("desc-pm"){text(resources.getString(R.string.private_desc))}
                        if(userPreferences.showShortcuts){
                            id("edit_shortcuts"){ text(resources.getString(R.string.edit_shortcuts)) }
                            id("apply"){ text(resources.getString(R.string.apply)) }
                            id("link1click"){ attr("href", userPreferences.link1)}
                            id("link2click"){ attr("href", userPreferences.link2)}
                            id("link3click"){ attr("href", userPreferences.link3)}
                            id("link4click"){ attr("href", userPreferences.link4)}
                            id("link1"){ attr("src", userPreferences.link1 + "/favicon.ico")}
                            id("link2"){ attr("src", userPreferences.link2 + "/favicon.ico")}
                            id("link3"){ attr("src", userPreferences.link3 + "/favicon.ico")}
                            id("link4"){ attr("src", userPreferences.link4 + "/favicon.ico")}
                            id("search_input"){ attr("placeholder", resources.getString(R.string.search_homepage))}
                        }
                        else{
                            id("shortcuts"){ attr("style", "display: none;")}
                        }

                    }
                }
            }
            .map { content -> Pair(createIncognitoPage(), content) }
            .doOnSuccess { (page, content) ->
                FileWriter(page, false).use {
                    if(userPreferences.startPageThemeEnabled && userPreferences.useTheme == AppTheme.LIGHT){
                        it.write(content)
                    }
                    else if(userPreferences.startPageThemeEnabled && userPreferences.useTheme == AppTheme.BLACK){
                        it.write(content + "<style>body {\n" +
                                "    background-color: #000000;\n" +
                                "} .text, .edit{" +
                                "color: #ffffff;" +
                                "fill: #ffffff;" +
                                "}</style>")
                    }
                    else if(userPreferences.startPageThemeEnabled && userPreferences.useTheme == AppTheme.DARK){
                        it.write(content + "<style>body {\n" +
                                "    background-color: #2a2a2a;\n" +
                                "} .text, .edit{" +
                                "color: #ffffff;" +
                                "fill: #ffffff;" +
                                "}</style>")
                    }
                    else{
                        it.write(content)
                    }
                }
            }
            .map { (page, _) -> "$FILE$page" }

    /**
     * Create the home page file.
     */
    fun createIncognitoPage() = File(application.filesDir, FILENAME)

    companion object {

        const val FILENAME = "private.html"

    }

}
