package com.cookiegames.smartcookie.html.onboarding

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
class OnboardingPageFactory @Inject constructor(
        private val application: Application,
        private val searchEngineProvider: SearchEngineProvider,
        private val onboardingPageReader: OnboardingPageReader,
        private var userPreferences: UserPreferences,
        private var resources: Resources,
        private val historyRepository: HistoryRepository,
        private val listPageReader: ListPageReader
) : HtmlPageFactory {

    private val title = application.getString(R.string.app_name)

    override fun buildPage(): Single<String> = Single
        .just(searchEngineProvider.provideSearchEngine())
        .map { (iconUrl, queryUrl, _) ->
            parse(onboardingPageReader.provideHtml()) andBuild {
                title { title }
                charset { UTF8 }
                body{
                    id("name"){ text(resources.getString(R.string.app_name)) }
                    id("1"){ text(resources.getString(R.string.onboarding_one)) }
                    id("2"){ text(resources.getString(R.string.onboarding_two)) }
                    id("3"){ text(resources.getString(R.string.onboarding_three)) }
                    id("getstarted"){ text(resources.getString(R.string.start))}
                    //id("adblock"){ text(resources.getString(R.string.adblock_category)) }
                    //id("cookieblock"){ text(resources.getString(R.string.cookie_category)) }
                    //id("load"){ text(resources.getString(R.string.load_tabs)) }
                    //id("dark"){ text(resources.getString(R.string.dark_theme)) }
                }

            }
        }
        .map { content -> Pair(createOnboarding(), content) }
        .doOnSuccess { (page, content) ->
            FileWriter(page, false).use {
                if(userPreferences.startPageThemeEnabled && userPreferences.useTheme == AppTheme.LIGHT){
                    it.write(content)
                }
                else if(userPreferences.startPageThemeEnabled && userPreferences.useTheme == AppTheme.BLACK){
                    it.write(content + "<style>body {\n" +
                            "    background-color: #000000;\n" +
                            "}</style>")
                }
                else if(userPreferences.startPageThemeEnabled && userPreferences.useTheme == AppTheme.DARK){
                    it.write(content + "<style>body {\n" +
                            "    background-color: #2a2a2a;\n" +
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
    fun createOnboarding() = File(application.filesDir, FILENAME)

    companion object {

        const val FILENAME = "onboarding.html"

    }

}
