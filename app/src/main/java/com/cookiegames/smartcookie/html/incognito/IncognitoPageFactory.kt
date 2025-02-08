package com.cookiegames.smartcookie.html.incognito

import android.app.Application
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import android.webkit.URLUtil
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.constant.FILE
import com.cookiegames.smartcookie.constant.UTF8
import com.cookiegames.smartcookie.database.history.HistoryRepository
import com.cookiegames.smartcookie.html.HtmlPageFactory
import com.cookiegames.smartcookie.html.ListPageReader
import com.cookiegames.smartcookie.html.jsoup.*
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.search.SearchEngineProvider
import com.cookiegames.smartcookie.utils.DrawableUtils
import dagger.Reusable
import io.reactivex.Single
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter
import java.net.URL
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
                parse(incognitoPageReader.provideHtml(application)) andBuild {
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
                            var shortcuts = arrayListOf(userPreferences.link1, userPreferences.link2, userPreferences.link3, userPreferences.link4)

                            id("edit_shortcuts"){ text(resources.getString(R.string.edit_shortcuts)) }
                            id("apply"){ text(resources.getString(R.string.apply)) }
                            id("link1click"){ attr("href", shortcuts[0])}
                            id("link2click"){ attr("href", shortcuts[1])}
                            id("link3click"){ attr("href", shortcuts[2])}
                            id("link4click"){ attr("href", shortcuts[3])}

                            shortcuts.forEachIndexed { index, element ->
                                if(!URLUtil.isValidUrl(element)){
                                    val icon = createIconByName('?')
                                    val encoded = bitmapToBase64(icon)

                                    id("link" + (index + 1)){ attr("src", "data:image/png;base64," + encoded)}

                                    return@forEachIndexed
                                }

                                val url = URL(element.replaceFirst("www.", ""))
                                val icon = createIconByName(url.getHost().first().toUpperCase())
                                val encoded = bitmapToBase64(icon)
                                id("link" + (index + 1)){ attr("src", element + "/favicon.ico")}
                                id("link" + (index + 1)){ attr("onerror", "this.src = 'data:image/png;base64,$encoded';")}

                            }
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
     * Create the incognito page file.
     */
    fun createIncognitoPage() = File(application.filesDir, FILENAME)

    fun createIconByName(name: Char): Bitmap {
        val icon = DrawableUtils.createRoundedLetterImage(
                name,
                64,
                64,
                Color.GRAY
        )
        return icon
    }

    fun bitmapToBase64(bitmap: Bitmap): String{
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
        val encoded: String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        return encoded
    }

    companion object {

        const val FILENAME = "private.html"

    }

}
