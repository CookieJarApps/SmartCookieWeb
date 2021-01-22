package com.cookiegames.smartcookie.view

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.MailTo
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.webkit.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.webkit.WebViewFeature
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.BuildConfig
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.adblock.AdBlocker
import com.cookiegames.smartcookie.adblock.allowlist.AllowListModel
import com.cookiegames.smartcookie.browser.JavaScriptChoice
import com.cookiegames.smartcookie.browser.SiteBlockChoice
import com.cookiegames.smartcookie.constant.FILE
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.extensions.resizeAndShow
import com.cookiegames.smartcookie.extensions.snackbar
import com.cookiegames.smartcookie.js.*
import com.cookiegames.smartcookie.log.Logger
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.ssl.SslState
import com.cookiegames.smartcookie.ssl.SslWarningPreferences
import com.cookiegames.smartcookie.utils.IntentUtils
import com.cookiegames.smartcookie.utils.ProxyUtils
import com.cookiegames.smartcookie.utils.Utils
import com.cookiegames.smartcookie.utils.isSpecialUrl
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.*
import java.net.HttpURLConnection
import java.net.URISyntaxException
import java.net.URL
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

class SmartCookieWebClient(
    private val activity: Activity,
    private val smartCookieView: SmartCookieView
) : WebViewClient() {

    private val uiController: UIController
    private val intentUtils = IntentUtils(activity)
    private val emptyResponseByteArray: ByteArray = byteArrayOf()
    private var urlLoaded = ""

    private var badsslList: MutableList<String> = ArrayList<String>()
    private var badsslErrors: MutableList<SslError> = ArrayList<SslError>()

    private val startBlocked = "<html><head><script language=\"javascript\"> function reload(){setTimeout(function(){window.history.back();}, 500);"
    private val start1Blocked = "};</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"file:///android_asset/warning.webp\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
    private val titleBlocked = activity.getString(R.string.blocked_title)
    private val start2Blocked = "</h1><p></p><div class=\"error-code\">"
    private val start3Blocked = "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\"><!-- <button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">"
    private val reloadBlocked = activity.getString(R.string.error_reload)
    private val endBlocked = "</button> --> </div></div></div></body></center></html>"

    private var zoomed = false
    private var first = true
    private var initScale = 0f

    private var color = "<style>body{background-color:#424242 !important;} h1{color:#ffffff !important;} .error-code{color:#e6e6e6 !important;}</style>"

    @Inject internal lateinit var proxyUtils: ProxyUtils
    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject internal lateinit var sslWarningPreferences: SslWarningPreferences
    @Inject internal lateinit var whitelistModel: AllowListModel
    @Inject internal lateinit var logger: Logger
    @Inject internal lateinit var textReflowJs: TextReflow
    @Inject internal lateinit var invertPageJs: InvertPage
    @Inject internal lateinit var darkMode: DarkMode
    @Inject internal lateinit var translate: Translate
    @Inject internal lateinit var noAMP: BlockAMP
    @Inject internal lateinit var cookieBlock: CookieBlock
    @Inject internal lateinit var blockAds: BlockAds
    @Inject internal lateinit var setWidenView: SetWidenViewport
    private var adBlock: AdBlocker

    @Volatile private var isRunning = false
    private var zoomScale = 0.0f

    private var currentUrl: String = ""

    var sslState: SslState = SslState.None
        private set(value) {
            sslStateSubject.onNext(value)
            field = value
        }

    private var whitelistIntent = false

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

    fun setWhitelist(a: Boolean){
        whitelistIntent = a
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
            val string = ByteArrayInputStream(activity.resources.getString(R.string.site_ad_blocked).toByteArray())
            if(request.isForMainFrame){
                return WebResourceResponse("text/plain", "utf-8", string)
            }

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
        //workarounds for sites that are broken in WebView.
        if(url.contains("detectPopBlock.js")){
            val empty = ByteArrayInputStream(emptyResponseByteArray)
            return WebResourceResponse("text/plain", "utf-8", empty)
        }
        return null
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
            smartCookieView.loadHomePage()
            /*smartCookieView.loadUrl("https://extensions.cookiejarapps.com/error.html")
            Handler().postDelayed({
            smartCookieView.webView!!.settings.javaScriptEnabled = true
            smartCookieView.webView!!.evaluateJavascript("document.getElementById('description').innerHTML = 'The extension could not be uninstalled because it isn\'t installed.';", null)
            smartCookieView.webView!!.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
            }, 600)*/
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
            smartCookieView.loadHomePage()
            /* smartCookieView.loadUrl("https://extensions.cookiejarapps.com/error.html")
             Handler().postDelayed({
                 smartCookieView.webView!!.settings.javaScriptEnabled = true
                 smartCookieView.webView!!.evaluateJavascript("document.getElementById('description').innerHTML = 'The extension could not be installed because it is already installed.';", null)
                 smartCookieView.webView!!.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
             }, 600)*/
        }
        else{
            if(text.contains("/*" + result + "*/") && text.contains("/*End " + result + "*/")){
                val toast = Toast.makeText(activity, "Extension installed", Toast.LENGTH_LONG)
                toast.show()
                file.appendText(text)
                file.appendText(System.getProperty("line.separator")!!)
                smartCookieView.loadHomePage()
            }
            else{
                val toast = Toast.makeText(activity, "Extension invalid", Toast.LENGTH_LONG)
                toast.show()
                smartCookieView.loadHomePage()
                /* smartCookieView.loadUrl("https://extensions.cookiejarapps.com/error.html")
                 Handler().postDelayed({
                 smartCookieView.webView!!.settings.javaScriptEnabled = true
                 smartCookieView.webView!!.evaluateJavascript("document.getElementById('description').innerHTML = 'The extension could not be installed because it isn\'t valid.';", null)
                 smartCookieView.webView!!.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
             }, 600)*/
            }
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        if(url.contains(BuildConfig.APPLICATION_ID + "/files/homepage.html")) {
            view.evaluateJavascript("javascript:(function() {"
                    + "link1var = '" + userPreferences.link1  + "';"
                    + "})();", null)
            view.evaluateJavascript("javascript:(function() {"
                    + "link2var = '" + userPreferences.link2 + "';"
                    + "})();", null)
            view.evaluateJavascript("javascript:(function() {"
                    + "link3var = '" + userPreferences.link3 + "';"
                    + "})();", null)
            view.evaluateJavascript("javascript:(function() {"
                    + "link4var = '" + userPreferences.link4  + "';"
                    + "})();", null)
        }

        if(url.contains("?install_extension=true") && url.contains("//cookiejarapps.com/extensions")){
            val builder = MaterialAlertDialogBuilder(activity)
            builder.setTitle("Install Extension")
            builder.setMessage("This extension is verified. Do you want to install this extension?")
            builder.setPositiveButton(R.string.yes){dialog, which ->
                //Toast.makeText(activity,"Extension installed.",Toast.LENGTH_SHORT).show()

                view.evaluateJavascript("""(function() {
                return document.body.innerText;
                })()""".trimMargin()) {
                    val extensionSource = it.substring(1, it.length-1)
                    Log.d("PageSource", extensionSource)
                    installExtension(extensionSource)
                }

            }
            builder.setNegativeButton("No"){_,_ ->
            }
            val dialog: androidx.appcompat.app.AlertDialog = builder.create()
            dialog.show()
        }

        if (view.isShown) {
            uiController.updateUrl(url, false)

            uiController.setBackButtonEnabled(view.canGoBack())
            uiController.setForwardButtonEnabled(view.canGoForward())
            view.postInvalidate()
        }
        if(url.contains("android_asset/onboarding.html")){
            uiController.updateUrl("", false)
        }

        if(userPreferences.forceZoom){
            view.loadUrl(
                    "javascript:(function() { document.querySelector('meta[name=\"viewport\"]').setAttribute(\"content\",\"width=device-width\"); })();"
            )
        }

        if(userPreferences.translateExtension && url.contains("translatetheweb.com/")){
            val lang: String
            if(Build.VERSION.SDK_INT > 20){
                lang = Locale.getDefault().toLanguageTag()
            }
            else{
                lang = Locale.getDefault().displayLanguage
            }
            when(lang){
                "pt-BR" -> view.evaluateJavascript("lang = 'pt'; " + translate.provideJs(), null)
                "pt", "português" -> view.evaluateJavascript("lang = 'pt-PT'; " + translate.provideJs(), null)
                else -> view.evaluateJavascript("lang = '" + lang + "'; " + translate.provideJs(), null)
            }
        }

        view.evaluateJavascript("""(function() {
        return "<html>" + document.getElementsByTagName('html')[0].innerHTML + "</html>";
        })()""".trimMargin()) {
            val editor: SharedPreferences.Editor = activity.getSharedPreferences("com.cookiegames.smartcookie", Context.MODE_PRIVATE).edit()
            editor.putString("source", it)
            editor.apply()
        }

        if(url.contains("//cookiejarapps.com/extensions") && Locale.getDefault().getDisplayLanguage() != "English"){
            view.evaluateJavascript("\$('#modal1').modal();\n" +
                    "    \$('#modal1').modal('open'); ", null)
            view.evaluateJavascript("document.getElementById(\"notSupported\").innerHTML = \""+ activity.resources.getString(R.string.language_not_supported) +"\"; document.getElementById(\"notSupportedText\").innerHTML = \"" + activity.resources.getString(R.string.language_not_supported_text) + "\";", null)

        }

        if (view.title == null || view.title.isNullOrEmpty()) {
            smartCookieView.titleInfo.setTitle(activity.getString(R.string.untitled))
        } else {
            smartCookieView.titleInfo.setTitle(view.title)
        }
        if (smartCookieView.invertPage) {
            view.evaluateJavascript(invertPageJs.provideJs(), null)
        }
        if (userPreferences.darkModeExtension && !WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            view.evaluateJavascript(darkMode.provideJs(), null)
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
                val start1 = "};</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"file:///android_asset/warning.webp\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
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


            if (userPreferences.siteBlockChoice === SiteBlockChoice.WHITELIST) run {
                if (userPreferences.siteBlockNames !== "") {
                    val arrayOfURLs = userPreferences.siteBlockNames
                    val strgs: Array<String>
                    if (arrayOfURLs.contains(", ")) {
                        strgs = arrayOfURLs.split(", ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    } else if (arrayOfURLs.contains("," + System.getProperty("line.separator").toString())){
                        strgs = arrayOfURLs.split("," + System.getProperty("line.separator").toString().toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    }  else{
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
                            view.loadDataWithBaseURL(null, color + startBlocked + start1Blocked + titleBlocked + start2Blocked + "NET::SITE_BLOCKED_BY_LIST" + start3Blocked + reloadBlocked + endBlocked, "text/html; charset=utf-8", "UTF-8", null)
                            view.invalidate()
                            view.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
                        }
                    }
                }
            }
            else if (userPreferences.siteBlockChoice === SiteBlockChoice.BLACKLIST) {
                if (userPreferences.siteBlockNames !== "") {
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
                            view.loadDataWithBaseURL(null, color + startBlocked + start1Blocked + titleBlocked + start2Blocked + "NET::SITE_BLOCKED_BY_LIST" + start3Blocked + reloadBlocked + endBlocked, "text/html; charset=utf-8", "UTF-8", null)
                            view.invalidate()
                            view.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
                        }
                    }
                }
                uiController.tabChanged(smartCookieView)
            }
        }
        if(userPreferences.cookieBlockEnabled){
            view.evaluateJavascript(cookieBlock.provideJs(), null)
        }

        if(userPreferences.noAmp){
            view.evaluateJavascript(noAMP.provideJs(), null)
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

    override fun onLoadResource(view: WebView?, url: String?) {
        //TODO: explore whether this fixes patchy js load on cached reload
        if(smartCookieView.toggleDesktop){
            view?.evaluateJavascript(setWidenView.provideJs(), null)
        }
        super.onLoadResource(view, url)
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        if(view.settings?.userAgentString!!.contains("wv")){
            view.settings?.userAgentString = view.settings?.userAgentString?.replace("wv", "")
        }
        zoomed = false
        first = true
        currentUrl = url
        view.settings.javaScriptEnabled = userPreferences.javaScriptEnabled

        if(userPreferences.noAmp){
            view.evaluateJavascript(noAMP.provideJs(), null)
            if(url.contains("&ampcf=1")){
                view.evaluateJavascript("window.location.replace(\"" + url.replace("&ampcf=1", "") + "\");", null)
            }
        }

        if (userPreferences.forceHTTPSenabled || userPreferences.preferHTTPSenabled) {
            if (url.contains("http://")) {
                if (url.contains("https://")) {
                    //Secure!
                    return
                } else {
                    view.pauseTimers()
                    val newUrl = url.replace("http://", "https://")
                    if (exists(newUrl)) {
                        //Supports HTTPS, but SSL isn't used, so redirect to HTTPS
                        view.resumeTimers()

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

        if(url.contains("//finished")) {
            view.evaluateJavascript("""(function() {
            if(localStorage.getItem("adblock")){ return "checked"; } else{ return "not checked"; }
            })()""".trimMargin()) {
                Log.d("itxxa2qw", it)
            }

           // view.loadUrl(BuildConfig.APPLICATION_ID + "/files/homepage.html")
        }


            if(url.contains(BuildConfig.APPLICATION_ID + "/files/homepage.html")){
                view.evaluateJavascript("""(function() {
                return localStorage.getItem("shouldUpdate");
                })()""".trimMargin()) {
                    Log.d("itxxa2qw", it)
                    if(it.substring(1, it.length - 1) == "yes"){
                        view.evaluateJavascript("""(function() {
                                return localStorage.getItem("link1");
                                })()""".trimMargin()) {
                            userPreferences.link1 = it.substring(1, it.length - 1)
                            view.evaluateJavascript("""(function() {
                                                    localStorage.setItem("shouldUpdate", "no");
                                                    })()"""){}
                        }
                        view.evaluateJavascript("""(function() {
                            return localStorage.getItem("link2");
                            })()""".trimMargin()) {
                            userPreferences.link2 = it.substring(1, it.length - 1)
                            view.evaluateJavascript("""(function() {
                                            localStorage.setItem("shouldUpdate", "no");
                                            })()"""){}
                        }
                        view.evaluateJavascript("""(function() {
                            return localStorage.getItem("link3");
                            })()""".trimMargin()) {
                            userPreferences.link3 = it.substring(1, it.length - 1)
                            view.evaluateJavascript("""(function() {
                                            localStorage.setItem("shouldUpdate", "no");
                                            })()"""){}
                        }
                        view.evaluateJavascript("""(function() {
                            return localStorage.getItem("link4");
                            })()""".trimMargin()) {
                            userPreferences.link4 = it.substring(1, it.length - 1)
                            view.evaluateJavascript("""(function() {
                                            localStorage.setItem("shouldUpdate", "no");
                                            })()"""){}
                        }
                        Handler(Looper.getMainLooper()).postDelayed({
                            uiController.newTabButtonClicked()
                        }, 100)
                    }
                }
        }
        if(userPreferences.cookieBlockEnabled){
            view.evaluateJavascript(cookieBlock.provideJs(), null)
        }
        if (userPreferences.javaScriptChoice === JavaScriptChoice.BLACKLIST) {
            if (userPreferences.javaScriptBlocked !== "" && userPreferences.javaScriptBlocked !== " ") {
                val arrayOfURLs = userPreferences.javaScriptBlocked
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
                        view.settings.javaScriptEnabled = false
                    }
                }
                else{
                    return
                }
            }
        }
        else  if (userPreferences.javaScriptChoice === JavaScriptChoice.WHITELIST) run {
            if (userPreferences.javaScriptBlocked !== "" && userPreferences.javaScriptBlocked !== " ") {
                val arrayOfURLs = userPreferences.javaScriptBlocked
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
                        view.settings.javaScriptEnabled = false
                    }
                }
                else{
                    return
                }
            }
        }

        if(url.contains("https://homepage")){
            uiController.newTabButtonClicked()
            uiController.tabCloseClicked(0)
        }

        // Only set the SSL state if there isn't an error for the current URL.
        if (!badsslList.contains(url)) {
            sslState = if (URLUtil.isHttpsUrl(url)) {
                SslState.Valid
            } else {
                SslState.None
            }
        } else{
            sslState = SslState.Invalid(badsslErrors[badsslList.indexOf(url)])
        }

        smartCookieView.titleInfo.setFavicon(null)
        if (smartCookieView.isShown) {
            uiController.updateUrl(url, true)


            uiController.showActionBar()
            uiController.showActionBar()
        }
        uiController.tabChanged(smartCookieView)
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView,
        handler: HttpAuthHandler,
        host: String,
        realm: String
    ) {
        MaterialAlertDialogBuilder(activity).apply {
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

        if(errorCode != -1) {
            Thread.sleep(500)
            webview.settings.javaScriptEnabled = true
            if (userPreferences.useTheme == AppTheme.LIGHT) {
                color = ""
            }
            val start = "<html><head><script> function reload(){setTimeout(function(){window.location.href = '" + failingUrl + "';}, 500);}"
            val start1 = "</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"file:///android_asset/warning.webp\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
            val title = activity.getString(R.string.error_title)
            val start2 = "</h1><p></p><div class=\"error-code\">"
            val start3 = "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\"><button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">"
            val reload = activity.getString(R.string.error_reload)
            val end = "</button> </div></div></div></body></center></html>"
            webview.loadUrl("about:blank")
            webview.loadDataWithBaseURL(failingUrl, color + start + start1 + title + start2 + error + start3 + reload + end, "text/html", "UTF-8", null)
            uiController.updateUrl(failingUrl, false)
            currentUrl = failingUrl
            urlLoaded = failingUrl
            webview.settings.javaScriptEnabled = userPreferences.javaScriptEnabled
        }

    }

    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
        if (first) {
            initScale = newScale
            first=false
            zoomed = false
        }
        else {
            if (newScale>oldScale) {
                zoomed = true
            }
            else {
                if (newScale<initScale) {
                    zoomed = false
                }
            }
        }

      if (view.isShown && smartCookieView.userPreferences.textReflowEnabled) {
            if (isRunning)
                return
            val changeInPercent = abs(100 - 100 / zoomScale * newScale)
            if (changeInPercent > 2.5f && !isRunning) {
                isRunning = view.postDelayed({
                    zoomScale = newScale

                    val textScale = (newScale / initScale)
                    view.evaluateJavascript(textReflowJs.provideJs() + textScale.toString() + " + 'px'; }());") { isRunning = false }
                }, 100)
            }
            else{
                view.evaluateJavascript(textReflowJs.provideJs() + "window.document.documentElement.clientWidth + 'px'; }());") { isRunning = false }
            }
        }
    }

    override fun onReceivedSslError(webView: WebView, handler: SslErrorHandler, error: SslError) {

        badsslList.add(error.url)
        badsslErrors.add(error)

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

        if(!userPreferences.ssl){
            handler.proceed()
            Toast.makeText(activity, errorCodeMessageCodes[0], Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(activity).apply {
            val view = LayoutInflater.from(activity).inflate(R.layout.dialog_ssl_warning, null)
            val dontAskAgain = view.findViewById<CheckBox>(R.id.checkBoxDontAskAgain)
            setTitle(activity.getString(R.string.title_warning))
            setMessage(alertMessage)
            setCancelable(true)
            setView(view)
            setOnCancelListener { handler.cancel() }
            setPositiveButton(activity.getString(R.string.action_yes)) { _, _ ->
                if (dontAskAgain.isChecked) {
                    sslWarningPreferences.rememberBehaviorForDomain(webView.url.orEmpty(), SslWarningPreferences.Behavior.PROCEED)
                }
                handler.proceed()
                webView.clearSslPreferences()
            }
            setNegativeButton(activity.getString(R.string.action_no)) { _, _ ->
                if (dontAskAgain.isChecked) {
                    sslWarningPreferences.rememberBehaviorForDomain(webView.url.orEmpty(), SslWarningPreferences.Behavior.CANCEL)
                }
                handler.cancel()
            }
        }.resizeAndShow()
    }

    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
        MaterialAlertDialogBuilder(activity).apply {
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

        val headers = smartCookieView.requestHeaders

        if (smartCookieView.isIncognito || userPreferences.blockIntent && !url.contains("auth")) {

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
            else -> {
                webView.loadUrl(url, headers)
                true
            }
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
                    println("SmartCookieWebClient: cannot open downloaded file")
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

        private const val TAG = "SmartCookieWebClient"

    }
}
