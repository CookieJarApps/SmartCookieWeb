package com.cookiegames.smartcookie.view

import com.cookiegames.smartcookie.BuildConfig
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.adblock.AdBlocker
import com.cookiegames.smartcookie.adblock.allowlist.AllowListModel
import com.cookiegames.smartcookie.constant.FILE
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.extensions.resizeAndShow
import com.cookiegames.smartcookie.extensions.snackbar
import com.cookiegames.smartcookie.js.InvertPage
import com.cookiegames.smartcookie.js.TextReflow
import com.cookiegames.smartcookie.log.Logger
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.ssl.SslState
import com.cookiegames.smartcookie.ssl.SslWarningPreferences
import com.cookiegames.smartcookie.utils.*
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Bitmap
import android.net.MailTo
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.webkit.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.BrowserApp
import com.cookiegames.smartcookie.browser.SiteBlockChoice
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.*
import java.net.HttpURLConnection
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Files.exists
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.log

class LightningWebClient(
    private val activity: Activity,
    private val lightningView: LightningView
) : WebViewClient() {

    private val uiController: UIController
    private val intentUtils = IntentUtils(activity)
    private val emptyResponseByteArray: ByteArray = byteArrayOf()

    private var color = "<style>body{background-color:#424242 !important;} h1{color:#ffffff !important;} .error-code{color:#e6e6e6 !important;}</style>"

    @Inject internal lateinit var proxyUtils: ProxyUtils
    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject internal lateinit var sslWarningPreferences: SslWarningPreferences
    @Inject internal lateinit var whitelistModel: AllowListModel
    @Inject internal lateinit var logger: Logger
    @Inject internal lateinit var textReflowJs: TextReflow
    @Inject internal lateinit var invertPageJs: InvertPage

    private var adBlock: AdBlocker

    private var urlWithSslError: String? = null

    @Volatile private var isRunning = false
    private var zoomScale = 0.0f

    private var currentUrl: String = ""

    var sslState: SslState = SslState.None
        private set(value) {
            sslStateSubject.onNext(value)
            field = value
        }

    private val sslStateSubject: PublishSubject<SslState> = PublishSubject.create()

    init {
        activity.injector.inject(this)
        uiController = activity as UIController
        adBlock = chooseAdBlocker()
    }

    fun sslStateObservable(): Observable<SslState> = sslStateSubject.hide()

    fun updatePreferences() {
        adBlock = chooseAdBlocker()
    }

    private fun chooseAdBlocker(): AdBlocker = if (userPreferences.adBlockEnabled) {
        activity.injector.provideBloomFilterAdBlocker()
    } else {
        activity.injector.provideNoOpAdBlocker()
    }

    private fun shouldRequestBeBlocked(pageUrl: String, requestUrl: String) =
        !whitelistModel.isUrlAllowedAds(pageUrl) && adBlock.isAd(requestUrl)

    fun exists(URLName: String): Boolean {

        try {
            HttpURLConnection.setFollowRedirects(false)
            val con = URL(URLName)
                    .openConnection() as HttpURLConnection
            con.requestMethod = "HEAD"
            var response = con.responseCode
            if(response == HttpURLConnection.HTTP_OK || response == HttpURLConnection.HTTP_NOT_FOUND || response == HttpURLConnection.HTTP_MOVED_PERM) {
                return true
            }
            Log.d("TAGG", response.toString())
            return false
        }
        catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (shouldRequestBeBlocked(currentUrl, request.url.toString())) {
            val empty = ByteArrayInputStream(emptyResponseByteArray)
            return WebResourceResponse("text/plain", "utf-8", empty)
        }
        return super.shouldInterceptRequest(view, request)
    }

    @Suppress("OverridingDeprecatedMember")
    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        if (shouldRequestBeBlocked(currentUrl, url)) {
            val empty = ByteArrayInputStream(emptyResponseByteArray)
            return WebResourceResponse("text/plain", "utf-8", empty)
        }
        return null
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (view.isShown) {
            uiController.updateUrl(url, false)
            uiController.setBackButtonEnabled(view.canGoBack())
            uiController.setForwardButtonEnabled(view.canGoForward())
            view.postInvalidate()
        }
        if(url.contains("//extensions.cookiejarapps.com") && Locale.getDefault().getDisplayLanguage() != "English"){
            view.evaluateJavascript("$('#myModal').modal('show')", null)
            view.evaluateJavascript("document.getElementById(\"notSupported\").innerHTML = \""+ activity.resources.getString(R.string.language_not_supported) +"\"; document.getElementById(\"notSupportedText\").innerHTML = \"" + activity.resources.getString(R.string.language_not_supported_text) + "\";", null)

        }

        if(url.contains("?install_extension=true") && url.contains("//extensions.cookiejarapps.com/")){
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Install Extension")
            builder.setMessage("This extension is verified. Do you want to install this extension?")
            builder.setPositiveButton("Yes"){dialog, which ->
                //Toast.makeText(activity,"Extension installed.",Toast.LENGTH_SHORT).show()

                view.evaluateJavascript("""(function() {
                return document.body.innerText;
                })()""".trimMargin()) {
                    val extensionSource = it.substring(1, it.length-1)
                    Log.d("PageSource", extensionSource)
                    installExtension(extensionSource)
                }

            }
            builder.setNegativeButton("No"){dialog,which ->
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
        else if(url.contains("?install_extension=true")){
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Install Extension")
            builder.setMessage("This extension is not verified! Do you still want to install this extension?")
            builder.setPositiveButton("Yes"){dialog, which ->
                view.settings.javaScriptEnabled = true
                view.evaluateJavascript("""(function() {
                return document.body.innerText;
                })()""".trimMargin()) {
                    val extensionSource = it.substring(1, it.length-1)
                    Log.d("PageSource", extensionSource)
                    installExtension(extensionSource)
                }
                view.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
            }
            builder.setNegativeButton("No"){dialog,which ->
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
        else if(url.contains("?install_extension=false")){
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Unnstall Extension")
            builder.setMessage("Would you like to uninstall this extension?")
            builder.setPositiveButton("Yes"){dialog, which ->
                view.settings.javaScriptEnabled = true
                view.evaluateJavascript("""(function() {
                return document.getElementsByTagName('pre')[0].innerHTML;
                })()""".trimMargin()) {
                    val extensionSource = it
                    uninstallExtension(extensionSource)
                    view.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
                }
                Toast.makeText(activity,"Extension uninstalled.",Toast.LENGTH_SHORT).show()
            }
            builder.setNegativeButton("No"){dialog,which ->
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()

        }

        if (view.title == null || view.title.isEmpty()) {
            lightningView.titleInfo.setTitle(activity.getString(R.string.untitled))
        } else {
            lightningView.titleInfo.setTitle(view.title)
        }
        if (lightningView.invertPage) {
            view.evaluateJavascript(invertPageJs.provideJs(), null)
        }
        if(userPreferences.cookieBlockEnabled){
            view.settings.javaScriptEnabled = true
            view.loadUrl(
                    "javascript:(function() { " +
                            "var elems = document.querySelectorAll(\".cc-banner, .qc-cmp-ui-content, .bbccookies-banner, .cc_banner-wrapper, .hnf-banner, .m-privacy-consent, .evidon-consent-button, .privacyPolicyBanner, .c-cookie-disclaimer, .important-banner--cookies, .cookie-policy, .cookie-banner-optout, .cookie-banner__wrapper\");\n" +
                            "\telems.forEach(function(element) {\n" +
                            "  \telement.parentNode.removeChild(element);\n" +
                            "\t});\n" +
                            "\n" +
                            "var elem1 = document.getElementById(\"cookiescript_injected\");\n" +
                            "if(elem1 != null){\n" +
                            "elem1.parentNode.removeChild(elem1);\n" +
                            "}\n" +
                            "\n" +
                            "var elem2 = document.getElementById(\"CybotCookiebotDialog\");\n" +
                            "\n" +
                            "if(elem2 != null){\n" +
                            "elem2.parentNode.removeChild(elem2);\n" +
                            "}\n" +
                            "\n" +
                            "var elem3 = document.getElementById(\"cookie-banner\");\n" +
                            "\n" +
                            "if(elem3 != null){\n" +
                            "elem3.parentNode.removeChild(elem3)\n" +
                            "}\n" +
                            "\n" +
                            "var elem4 = document.getElementById(\"cookieNotificationBannerWrapper\");\n" +
                            "\n" +
                            "if(elem4 != null){\n" +
                            "elem4.parentNode.removeChild(elem4)\n" +
                            "}" + "if(document.getElementById(\"cmp-container-id\"0 != null){ document.getElementById(\"cmp-container-id\").parentNode.removeChild(document.getElementById(\"cmp-container-id\"));}" +
                            "})()")
            view.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
        }
        if (userPreferences.blockMalwareEnabled) {
            val inputStream: InputStream = activity.assets.open("malware.txt")
            val inputString = inputStream.bufferedReader().use { it.readText() }
            val lines =  inputString.split(",").toTypedArray()
            if (stringContainsItemFromList(url, lines)) {
                view.settings.javaScriptEnabled = true
                if(userPreferences.useTheme == AppTheme.LIGHT){
                    color = ""
                }
                val start = "<html><head><script language=\"javascript\"> function reload(){setTimeout(function(){window.history.back();}, 500);"
                val start1 = "};</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"file:///android_asset/warning.png\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
                val title = activity.getString(R.string.malware_title)
                val start2 = "</h1><p></p><div class=\"error-code\">"
                val start3 = "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\"><!-- <button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">"
                val reload = activity.getString(R.string.error_reload)
                val error = activity.getString(R.string.malware_message)
                val end = "</button> --> </div></div></div></body></center></html>"
                view.loadDataWithBaseURL(null, color+ start + start1 + title + start2 + error + start3 + reload + end, "text/html; charset=utf-8", "UTF-8", null)
                view.invalidate()
                view.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
            }
            if (userPreferences.forceHTTPSenabled || userPreferences.preferHTTPSenabled) {
                if (url.contains("http://")) {
                    if (url.contains("https://")) {
                        //Secure!
                        return
                    } else {
                        var newUrl = url.replace("http://", "https://")
                        if (exists(newUrl)) {
                            //Supports HTTPS, but SSL isn't used, so redirect to HTTPS
                            try {
                                TimeUnit.MILLISECONDS.sleep(500)
                            } catch (ex: InterruptedException) {
                                Thread.currentThread().interrupt()
                            }

                            view.loadUrl(newUrl)
                        } else {
                            //Dosen't support HTTPS :(
                            if (userPreferences.forceHTTPSenabled) {
                                view.settings.javaScriptEnabled = true
                                if(userPreferences.useTheme == AppTheme.LIGHT){
                                    color = ""
                                }
                                val start = "<html><head><script language=\"javascript\"> function reload(){setTimeout(function(){window.history.back();}, 500);"
                                val start1 = "};</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"file:///android_asset/warning.png\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
                                val title = activity.getString(R.string.https_title)
                                val start2 = "</h1><p></p><div class=\"error-code\">"
                                val start3 = "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\"><!-- <button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">"
                                val reload = activity.getString(R.string.error_reload)
                                val end = "</button> --> </div></div></div></body></center></html>"
                                view.loadDataWithBaseURL(null, color + start + start1 + title + start2 + "NET::ERR_HTTPS_NOT_SUPPORTED" + start3 + reload + end, "text/html; charset=utf-8", "UTF-8", null)
                                view.invalidate()
                                view.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
                            }
                        }
                    }
                }
            }
            if (userPreferences.siteBlockChoice === SiteBlockChoice.WHITELIST) run {
                if (userPreferences.siteBlockNames !== "" && userPreferences.siteBlockNames != null) {
                    val arrayOfURLs = userPreferences.siteBlockNames
                    val strgs: Array<String>
                    if (arrayOfURLs.contains(", ")) {
                        strgs = arrayOfURLs.split(", ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    } else {
                        strgs = arrayOfURLs.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    }
                    if (stringContainsItemFromList(url, strgs)) {
                        if (url.contains("file:///android_asset") or url.contains("about:blank")) {
                            return
                        } else {
                            if(userPreferences.useTheme == AppTheme.LIGHT){
                                color = ""
                            }
                            view.settings.javaScriptEnabled = true
                            val start = "<html><head><script language=\"javascript\"> function reload(){setTimeout(function(){window.history.back();}, 500);"
                            val start1 = "};</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"file:///android_asset/warning.png\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
                            val title = activity.getString(R.string.blocked_title)
                            val start2 = "</h1><p></p><div class=\"error-code\">"
                            val start3 = "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\"><!-- <button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">"
                            val reload = activity.getString(R.string.error_reload)
                            val end = "</button> --> </div></div></div></body></center></html>"
                            view.loadDataWithBaseURL(null, color + start + start1 + title + start2 + "NET::SITE_BLOCKED_BY_LIST" + start3 + reload + end, "text/html; charset=utf-8", "UTF-8", null)
                            view.invalidate()
                            view.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
                        }
                    }
                }
            }
            else if (userPreferences.siteBlockChoice === SiteBlockChoice.BLACKLIST) {
                if (userPreferences.siteBlockNames !== "" && userPreferences.siteBlockNames != null) {
                    val arrayOfURLs = userPreferences.siteBlockNames
                    val strgs: Array<String>
                    if (arrayOfURLs.contains(", ")) {
                        strgs = arrayOfURLs.split(", ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    } else {
                        strgs = arrayOfURLs.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    }
                    if (!stringContainsItemFromList(url, strgs)) {
                        if (url.contains("file:///android_asset") or url.contains("about:blank")) {
                            return
                        } else {
                            if(userPreferences.useTheme == AppTheme.LIGHT){
                                color = ""
                            }
                            view.settings.javaScriptEnabled = true
                            val start = "<html><head><script language=\"javascript\"> function reload(){setTimeout(function(){window.history.back();}, 500);"
                            val start1 = "};</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"file:///android_asset/warning.png\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
                            val title = activity.getString(R.string.blocked_title)
                            val start2 = "</h1><p></p><div class=\"error-code\">"
                            val start3 = "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\"><!-- <button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">"
                            val reload = activity.getString(R.string.error_reload)
                            val end = "</button> --> </div></div></div></body></center></html>"
                            view.loadDataWithBaseURL(null, color + start + start1 + title + start2 + "NET::SITE_BLOCKED_BY_LIST" + start3 + reload + end, "text/html; charset=utf-8", "UTF-8", null)
                            view.invalidate()
                            view.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
                        }
                    }
                }
                uiController.tabChanged(lightningView)
            }
        }

        val letDirectory = File(activity.getFilesDir(), "extensions")
        letDirectory.mkdirs()
        val file = File(letDirectory, "extension_file.txt")
        if(file.exists()){
            val contents = file.readText() // Read file

            Log.d("extensions", contents)

            //view.loadUrl("javascript:(function() {" + contents + "})()")
            view.settings.javaScriptEnabled = true
            view.evaluateJavascript(contents, null)
            view.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
        }
    }

        fun stringContainsItemFromList(inputStr: String, items: Array<String>): Boolean {
            for (i in items.indices) {
                if (inputStr.contains(items[i])) {
                    return true
                }
            }
            return false
        }
    private fun uninstallExtension(text: String) {
        val path = activity.getFilesDir()
        val letDirectory = File(path, "extensions")
        letDirectory.mkdirs()
        val file = File(letDirectory, "extension_file.txt")
        if(!file.exists()){
            file.appendText("/* begin extensions file */")
        }
        var inputAsString = FileInputStream(file).bufferedReader().use { it.readText() }
        var result: String
        if(inputAsString.contains("/*") && inputAsString.contains("*/")){
            result = text.substring(text.indexOf("/*") + 2, text.indexOf("*/"))
        }
        else{
            val toast = Toast.makeText(activity, "Extension not installed", Toast.LENGTH_LONG)
            toast.show()
            lightningView.loadUrl("https://extensions.cookiejarapps.com/error.html")
            Handler().postDelayed({
            lightningView.webView!!.settings.javaScriptEnabled = true
            lightningView.webView!!.evaluateJavascript("document.getElementById('description').innerHTML = 'The extension could not be uninstalled because it isn\'t installed.';", null)
            lightningView.webView!!.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
            }, 600)
            return
        }

        var string1 = inputAsString.substring(inputAsString.indexOf("/*" + result + "*/") + 4 + result.length, inputAsString.indexOf("/*End " + result + "*/"))
        inputAsString = inputAsString.replace(string1, "")
        inputAsString = inputAsString.replace("/*" + result + "*/", "")
        inputAsString = inputAsString.replace("/*End " + result + "*/", "")
        PrintWriter(file).close()
        file.appendText(inputAsString)
    }
    private fun installExtension(text: String){
        var result = ""
        val path = activity.getFilesDir()
        val letDirectory = File(path, "extensions")
        letDirectory.mkdirs()
        val file = File(letDirectory, "extension_file.txt")
        if(!file.exists()){
            file.appendText("/* begin extensions file */")
        }
        val inputAsString = FileInputStream(file).bufferedReader().use { it.readText() }
        result = text.substring(text.indexOf("/*") + 2, text.indexOf("*/"))
        if(inputAsString.contains("/*" + result + "*/")){
            val toast = Toast.makeText(activity, "Extension already installed", Toast.LENGTH_LONG)
            toast.show()
            lightningView.loadUrl("https://extensions.cookiejarapps.com/error.html")
            Handler().postDelayed({
                lightningView.webView!!.settings.javaScriptEnabled = true
                lightningView.webView!!.evaluateJavascript("document.getElementById('description').innerHTML = 'The extension could not be installed because it is already installed.';", null)
                lightningView.webView!!.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
            }, 600)
        }
        else{
            if(text.contains("/*" + result + "*/") && text.contains("/*End " + result + "*/")){
                val toast = Toast.makeText(activity, "Extension installed", Toast.LENGTH_LONG)
                toast.show()
                file.appendText(text)
                file.appendText(System.getProperty("line.separator")!!)
                lightningView.loadUrl("https://extensions.cookiejarapps.com/success.html")
            }
            else{
                val toast = Toast.makeText(activity, "Extension invalid", Toast.LENGTH_LONG)
                toast.show()
                lightningView.loadUrl("https://extensions.cookiejarapps.com/error.html")
                Handler().postDelayed({
                lightningView.webView!!.settings.javaScriptEnabled = true
                lightningView.webView!!.evaluateJavascript("document.getElementById('description').innerHTML = 'The extension could not be installed because it isn\'t valid.';", null)
                lightningView.webView!!.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
            }, 600)
            }
        }
    }
    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        currentUrl = url
        // Only set the SSL state if there isn't an error for the current URL.
        if (urlWithSslError != url) {
            sslState = if (URLUtil.isHttpsUrl(url)) {
                SslState.Valid
            } else {
                SslState.None
            }
        }

        lightningView.titleInfo.setFavicon(null)
        if (lightningView.isShown) {
            uiController.updateUrl(url, true)
            uiController.showActionBar()
        }
        uiController.tabChanged(lightningView)
    }


    override fun onReceivedHttpAuthRequest(
        view: WebView,
        handler: HttpAuthHandler,
        host: String,
        realm: String
    ) {
        AlertDialog.Builder(activity).apply {
            val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_auth_request, null)

            val realmLabel = dialogView.findViewById<TextView>(R.id.auth_request_realm_textview)
            val name = dialogView.findViewById<EditText>(R.id.auth_request_username_edittext)
            val password = dialogView.findViewById<EditText>(R.id.auth_request_password_edittext)

            realmLabel.text = activity.getString(R.string.label_realm, realm)

            setView(dialogView)
            setTitle(R.string.title_sign_in)
            setCancelable(true)
            setPositiveButton(R.string.title_sign_in) { _, _ ->
                val user = name.text.toString()
                val pass = password.text.toString()
                handler.proceed(user.trim(), pass.trim())
                logger.log(TAG, "Attempting HTTP Authentication")
            }
            setNegativeButton(R.string.action_cancel) { _, _ ->
                handler.cancel()
            }
        }.resizeAndShow()
    }

    override fun onReceivedError(webview: WebView, errorCode: Int, error: String, failingUrl: String) {
        Thread.sleep(500)
        webview.settings.javaScriptEnabled = true
        if(userPreferences.useTheme == AppTheme.LIGHT){
            color = ""
        }
        val start = "<html><head><script> function reload(){setTimeout(function(){window.history.back();}, 500);}"
        val start1 = "</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"data:image/png;base64,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\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
        val title = activity.getString(R.string.error_title)
        val start2 = "</h1><p></p><div class=\"error-code\">"
        val start3 = "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\"><button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">"
        val reload = activity.getString(R.string.error_reload)
        val end = "</button> </div></div></div></body></center></html>"
        webview.loadUrl("about:blank")
        webview.loadDataWithBaseURL(null, color + start + start1 + title + start2 + error + start3 + reload + end, "text/html", "UTF-8", null);
        webview.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
    }


    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
        if (view.isShown && lightningView.userPreferences.textReflowEnabled) {
            if (isRunning)
                return
            val changeInPercent = abs(100 - 100 / zoomScale * newScale)
            if (changeInPercent > 2.5f && !isRunning) {
                isRunning = view.postDelayed({
                    zoomScale = newScale
                    view.evaluateJavascript(textReflowJs.provideJs()) { isRunning = false }
                }, 100)
            }

        }
    }

    override fun onReceivedSslError(webView: WebView, handler: SslErrorHandler, error: SslError) {
        urlWithSslError = webView.url
        sslState = SslState.Invalid(error)

        when (sslWarningPreferences.recallBehaviorForDomain(webView.url)) {
            SslWarningPreferences.Behavior.PROCEED -> return handler.proceed()
            SslWarningPreferences.Behavior.CANCEL -> return handler.cancel()
            null -> Unit
        }

        val errorCodeMessageCodes = getAllSslErrorMessageCodes(error)

        val stringBuilder = StringBuilder()
        for (messageCode in errorCodeMessageCodes) {
            stringBuilder.append(" - ").append(activity.getString(messageCode)).append('\n')
        }
        val alertMessage = activity.getString(R.string.message_insecure_connection, stringBuilder.toString())

        AlertDialog.Builder(activity).apply {
            val view = LayoutInflater.from(activity).inflate(R.layout.dialog_ssl_warning, null)
            val dontAskAgain = view.findViewById<CheckBox>(R.id.checkBoxDontAskAgain)
            setTitle(activity.getString(R.string.title_warning))
            setMessage(alertMessage)
            setCancelable(true)
            setView(view)
            setOnCancelListener { handler.cancel() }
            setPositiveButton(activity.getString(R.string.action_yes)) { _, _ ->
                if (dontAskAgain.isChecked) {
                    sslWarningPreferences.rememberBehaviorForDomain(webView.url, SslWarningPreferences.Behavior.PROCEED)
                }
                handler.proceed()
            }
            setNegativeButton(activity.getString(R.string.action_no)) { _, _ ->
                if (dontAskAgain.isChecked) {
                    sslWarningPreferences.rememberBehaviorForDomain(webView.url, SslWarningPreferences.Behavior.CANCEL)
                }
                handler.cancel()
            }
        }.resizeAndShow()
    }

    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
        AlertDialog.Builder(activity).apply {
            setTitle(activity.getString(R.string.title_form_resubmission))
            setMessage(activity.getString(R.string.message_form_resubmission))
            setCancelable(true)
            setPositiveButton(activity.getString(R.string.action_yes)) { _, _ ->
                resend.sendToTarget()
            }
            setNegativeButton(activity.getString(R.string.action_no)) { _, _ ->
                dontResend.sendToTarget()
            }
        }.resizeAndShow()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        shouldOverrideLoading(view, request.url.toString()) || super.shouldOverrideUrlLoading(view, request)

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
        shouldOverrideLoading(view, url) || super.shouldOverrideUrlLoading(view, url)

    private fun shouldOverrideLoading(view: WebView, url: String): Boolean {
        // Check if configured proxy is available
        if (!proxyUtils.isProxyReady(activity)) {
            // User has been notified
            return true
        }

        val headers = lightningView.requestHeaders

        if (lightningView.isIncognito) {
            // If we are in incognito, immediately load, we don't want the url to leave the app
            return continueLoadingUrl(view, url, headers)
        }
        if (URLUtil.isAboutUrl(url)) {
            // If this is an about page, immediately load, we don't need to leave the app
            return continueLoadingUrl(view, url, headers)
        }

        return if (isMailOrIntent(url, view) || intentUtils.startActivityForUrl(view, url)) {
            // If it was a mailto: link, or an intent, or could be launched elsewhere, do that
            true
        } else {
            // If none of the special conditions was met, continue with loading the url
            continueLoadingUrl(view, url, headers)
        }
    }

    private fun continueLoadingUrl(webView: WebView, url: String, headers: Map<String, String>): Boolean {
        if (!URLUtil.isNetworkUrl(url)
            && !URLUtil.isFileUrl(url)
            && !URLUtil.isAboutUrl(url)
            && !URLUtil.isDataUrl(url)
            && !URLUtil.isJavaScriptUrl(url)) {
            webView.stopLoading()
            return true
        }
        return when {
            headers.isEmpty() -> false
            ApiUtils.doesSupportWebViewHeaders() -> {
                webView.loadUrl(url, headers)
                true
            }
            else -> false
        }
    }

    private fun isMailOrIntent(url: String, view: WebView): Boolean {
        if (url.startsWith("mailto:")) {
            val mailTo = MailTo.parse(url)
            val i = Utils.newEmailIntent(mailTo.to, mailTo.subject, mailTo.body, mailTo.cc)
            activity.startActivity(i)
            view.reload()
            return true
        } else if (url.startsWith("intent://")) {
            val intent = try {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } catch (ignored: URISyntaxException) {
                null
            }

            if (intent != null) {
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.component = null
                intent.selector = null
                try {
                    activity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    logger.log(TAG, "ActivityNotFoundException")
                }

                return true
            }
        } else if (URLUtil.isFileUrl(url) && !url.isSpecialUrl()) {
            val file = File(url.replace(FILE, ""))

            if (file.exists()) {
                val newMimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(Utils.guessFileExtension(file.toString()))

                val intent = Intent(Intent.ACTION_VIEW)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val contentUri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".fileprovider", file)
                intent.setDataAndType(contentUri, newMimeType)

                try {
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    println("LightningWebClient: cannot open downloaded file")
                }

            } else {
                activity.snackbar(R.string.message_open_download_fail)
            }
            return true
        }
        return false
    }

    private fun getAllSslErrorMessageCodes(error: SslError): List<Int> {
        val errorCodeMessageCodes = ArrayList<Int>(1)

        if (error.hasError(SslError.SSL_DATE_INVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_date_invalid)
        }
        if (error.hasError(SslError.SSL_EXPIRED)) {
            errorCodeMessageCodes.add(R.string.message_certificate_expired)
        }
        if (error.hasError(SslError.SSL_IDMISMATCH)) {
            errorCodeMessageCodes.add(R.string.message_certificate_domain_mismatch)
        }
        if (error.hasError(SslError.SSL_NOTYETVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_not_yet_valid)
        }
        if (error.hasError(SslError.SSL_UNTRUSTED)) {
            errorCodeMessageCodes.add(R.string.message_certificate_untrusted)
        }
        if (error.hasError(SslError.SSL_INVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_invalid)
        }

        return errorCodeMessageCodes
    }

    companion object {

        private const val TAG = "LightningWebClient"

    }
}
