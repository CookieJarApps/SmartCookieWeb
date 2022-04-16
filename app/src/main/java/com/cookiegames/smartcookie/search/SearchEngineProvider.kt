package com.cookiegames.smartcookie.search

import android.app.Application
import com.cookiegames.smartcookie.di.SuggestionsClient
import com.cookiegames.smartcookie.log.Logger
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.search.engine.*
import com.cookiegames.smartcookie.search.suggestions.*
import dagger.Reusable
import io.reactivex.Single
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * The model that provides the search engine based
 * on the user's preference.
 */
@Reusable
class  SearchEngineProvider @Inject constructor(
    private val userPreferences: UserPreferences,
    @SuggestionsClient private val okHttpClient: Single<OkHttpClient>,
    private val requestFactory: RequestFactory,
    private val application: Application,
    private val logger: Logger
) {

    /**
     * Provide the [SuggestionsRepository] that maps to the user's current preference.
     */
    fun provideSearchSuggestions(): SuggestionsRepository =
        when (userPreferences.searchSuggestionChoice) {
            0 -> GoogleSuggestionsModel(okHttpClient, requestFactory, application, logger)
            1 -> DuckSuggestionsModel(okHttpClient, requestFactory, application, logger)
            2 -> BaiduSuggestionsModel(okHttpClient, requestFactory, application, logger)
            3 -> NaverSuggestionsModel(okHttpClient, requestFactory, application, logger)
            4 -> SmartCookieWebSuggestionsModel(okHttpClient, requestFactory, application, logger)
            5 -> NoOpSuggestionsRepository()
            else -> GoogleSuggestionsModel(okHttpClient, requestFactory, application, logger)
        }

    /**
     * Provide the [BaseSearchEngine] that maps to the user's current preference.
     */
    fun provideSearchEngine(): BaseSearchEngine =
        when (userPreferences.searchChoice) {
            0 -> CustomSearch(userPreferences.searchUrl)
            1 -> GoogleSearch()
            2 -> AskSearch()
            3 -> BingSearch()
            4 -> YahooSearch()
            5 -> StartPageSearch()
            6 -> StartPageMobileSearch()
            7 -> DuckSearch()
            8 -> DuckLiteSearch()
            9 -> BaiduSearch()
            10 -> YandexSearch()
            11 -> NaverSearch()
            12 -> EcosiaSearch()
            13 -> EkoruSearch()
            14 -> CookieJarAppsSearch()
            15 -> SearxSearch()
            else -> GoogleSearch()
        }

    /**
     * Return the serializable index of of the provided [BaseSearchEngine].
     */
    fun mapSearchEngineToPreferenceIndex(searchEngine: BaseSearchEngine): Int =
        when (searchEngine) {
            is CustomSearch -> 0
            is GoogleSearch -> 1
            is AskSearch -> 2
            is BingSearch -> 3
            is YahooSearch -> 4
            is StartPageSearch -> 5
            is StartPageMobileSearch -> 6
            is DuckSearch -> 7
            is DuckLiteSearch -> 8
            is BaiduSearch -> 9
            is YandexSearch -> 10
            is NaverSearch -> 11
            is EcosiaSearch -> 12
            is EkoruSearch -> 13
            is CookieJarAppsSearch -> 14
            is SearxSearch -> 15
            else -> throw UnsupportedOperationException("Unknown search engine provided: " + searchEngine.javaClass)
        }

    /**
     * Provide a list of all supported search engines.
     */
    fun provideAllSearchEngines(): List<BaseSearchEngine> = listOf(
        CustomSearch(userPreferences.searchUrl),
        GoogleSearch(),
        AskSearch(),
        BingSearch(),
        YahooSearch(),
        StartPageSearch(),
        StartPageMobileSearch(),
        DuckSearch(),
        DuckLiteSearch(),
        BaiduSearch(),
        YandexSearch(),
        NaverSearch(),
        EcosiaSearch(),
        EkoruSearch(),
        CookieJarAppsSearch(),
        SearxSearch()
    )

}
