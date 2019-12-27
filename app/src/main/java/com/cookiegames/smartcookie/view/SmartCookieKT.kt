/*
package com.cookiegames.smartcookie.view

import android.annotation.TargetApi
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.MailTo
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import androidx.core.content.FileProvider
import androidx.appcompat.app.AlertDialog

import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.HttpAuthHandler
import android.webkit.MimeTypeMap
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URISyntaxException
import java.net.URL
import java.util.ArrayList
import java.util.concurrent.TimeUnit

import javax.inject.Inject

import com.cookiegames.smartcookie.BrowserApp
import com.cookiegames.smartcookie.BuildConfig
import com.cookiegames.smartcookie.MainActivity
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.adblock.AdBlocker
import com.cookiegames.smartcookie.constant.*
import com.cookiegames.smartcookie.controller.UIController
import com.cookiegames.smartcookie.database.history.HistoryModel
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.malwareblock.MalwareBlock
import com.cookiegames.smartcookie.preference.PreferenceManager
import com.cookiegames.smartcookie.utils.IntentUtils
import com.cookiegames.smartcookie.utils.Preconditions
import com.cookiegames.smartcookie.utils.ProxyUtils
import com.cookiegames.smartcookie.utils.*
import com.cookiegames.smartcookie.utils.Utils
import com.cookiegames.smartcookie.database.history.HistoryDatabase

class LightningWebClient internal constructor(private val mActivity: Activity, private val mLightningView: LightningView) : WebViewClient() {
    private val mUIController: UIController
    private val mIntentUtils: IntentUtils
    private val urlName: String? = null
    private val adsBlocked: Int = 0
    private val newHTTPS: String? = null
    private val reload: Button? = null
    //SharedPreferences sharedPref;


    @Inject
    internal var mProxyUtils: ProxyUtils? = null
    @Inject
    internal var mPreferences: PreferenceManager? = null

    private var mAdBlock: AdBlocker
    private var mMalwareBlock: MalwareBlock

    @Volatile
    private var mIsRunning = false
    private var mZoomScale = 0.0f

    init {
        BrowserApp.appComponent.inject(this)
        Preconditions.checkNonNull(mActivity)
        Preconditions.checkNonNull(mLightningView)
        mUIController = mActivity as UIController
        mAdBlock = chooseAdBlocker()
        mMalwareBlock = chooseMalwareBlocker()
        mIntentUtils = IntentUtils(mActivity)
    }


    fun updatePreferences() {
        mAdBlock = chooseAdBlocker()
        mMalwareBlock = chooseMalwareBlocker()
    }

    private fun chooseAdBlocker(): AdBlocker {
        return if (mPreferences!!.getAdBlockEnabled()) {
            BrowserApp.appComponent.provideAssetsAdBlocker()
        } else {
            BrowserApp.appComponent.provideNoOpAdBlocker()
        }
    }

    private fun chooseMalwareBlocker(): MalwareBlock {
        return if (mPreferences!!.getBlockMalwareEnabled()) {
            BrowserApp.appComponent.provideAssetsMalwareBlock()
        } else {
            BrowserApp.appComponent.provideNoOpMalwareBlock()
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest/*, Context ctx*/): WebResourceResponse? {
        if (mAdBlock.isAd(request.url.toString())) {
            /*adsBlocked = Integer.parseInt(sharedPref.getString("user_id", "0"));
            adsBlocked = adsBlocked + 1;
            sharedPref = ctx.getSharedPreferences("myPref", MODE_PRIVATE);
            sharedPref.edit().putString("user_id", Integer.toString(adsBlocked)).commit();*/

            val EMPTY = ByteArrayInputStream("".toByteArray())
            return WebResourceResponse("text/plain", "utf-8", EMPTY)
        }
        return super.shouldInterceptRequest(view, request)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        var url = url

        if (mAdBlock.isAd(url)) {
            val EMPTY = ByteArrayInputStream("".toByteArray())
            return WebResourceResponse("text/plain", "utf-8", EMPTY)
        }

        if (mMalwareBlock.isMalware(url)) {
            val EMPTY = ByteArrayInputStream("Malware site blocked.".toByteArray())
            return WebResourceResponse("text/plain", "utf-8", EMPTY)
        }

        if (mPreferences!!.getSiteBlockChoice() === 2) {
            if (mPreferences!!.getSiteBlockString("") !== "" && mPreferences!!.getSiteBlockString("") != null) {
                if (mPreferences!!.getSiteBlockString("").contains(url)) {
                    if (url.contains("file:///android_asset")) {

                    } else {
                        val EMPTY = ByteArrayInputStream("Site blocked in settings.".toByteArray())
                        return WebResourceResponse("text/plain", "utf-8", EMPTY)
                    }
                }
            }
        }


        if (mPreferences!!.getForceHTTPSenabled() || mPreferences!!.getPreferHTTPSenabled()) {
            if (!url.contains("file:///") && url !== "") {
                if (url.contains("https://")) {
                    //Secure!
                } else {
                    url = url.replace("http://", "https://")
                    if (exists(url)) {
                        //Supports HTTPS, but SSL isn't used, so redirect to HTTPS
                    } else {
                        //Dosen't support HTTPS :(
                        if (mPreferences!!.getForceHTTPSenabled()) {
                            //Stop site loading while error page loads
                            val EMPTY = ByteArrayInputStream("".toByteArray())
                            return WebResourceResponse("text/plain", "utf-8", EMPTY)
                        }
                    }
                }
            }
        }

        return null

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    override fun onPageFinished(view: WebView, url: String) {
        var url = url

        if (view.isShown) {
            mUIController.updateUrl(url, false)
            mUIController.setBackButtonEnabled(view.canGoBack())
            mUIController.setForwardButtonEnabled(view.canGoForward())
            view.postInvalidate()
        }
        if (view.title == null || view.title.isEmpty()) {
            mLightningView.titleInfo.setTitle(mActivity.getString(R.string.untitled))
        } else {
            mLightningView.titleInfo.setTitle(view.title)
        }
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && mLightningView.getInvertePage()) {
            view.evaluateJavascript(Constants.JAVASCRIPT_INVERT_PAGE, null)
        }

        if (mPreferences!!.getForceHTTPSenabled() || mPreferences!!.getPreferHTTPSenabled()) {
            if (url.contains("http") && url.contains("://")) {
                if (url.contains("https://")) {
                    //Secure!
                } else {
                    url = url.replace("http://", "https://")
                    if (exists(url)) {
                        //Supports HTTPS, but SSL isn't used, so redirect to HTTPS
                        try {
                            TimeUnit.MILLISECONDS.sleep(500)
                        } catch (ex: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }

                        view.loadUrl(url)
                    } else {
                        //Dosen't support HTTPS :(
                        if (mPreferences!!.getForceHTTPSenabled()) {
                            view.settings.javaScriptEnabled = true
                            val start = "<html><head><script language=\"javascript\"> function reload(){setTimeout(function(){window.history.back();}, 500);"
                            val start1 = "};</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"file:///android_asset/warning.png\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
                            val title = mActivity.getString(R.string.https_title)
                            val start2 = "</h1><p></p><div class=\"error-code\">"
                            val start3 = "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\"><!-- <button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">"
                            val reload = mActivity.getString(R.string.error_reload)
                            val end = "</button> --> </div></div></div></body></center></html>"
                            view.loadDataWithBaseURL(null, start + start1 + title + start2 + "NET::ERR_HTTPS_NOT_SUPPORTED" + start3 + reload + end, "text/html; charset=utf-8", "UTF-8", null)
                            view.invalidate()
                            view.settings.javaScriptEnabled = mPreferences!!.getJavaScriptEnabled()
                        }
                    }
                }
            }
        }
        view.settings.javaScriptEnabled = true
        //TODO: better solution to WebView crash
        view.loadUrl(
                "javascript:(function() { " +
                        "var links = document.links; \n" +
                        "if (links != null) { \n" +
                        "for (var i = 0; i < links.length; i++) {\n" +
                        "links[i].rel = '';\n" +
                        "}\n" +
                        "}" +
                        "})()")
        view.settings.javaScriptEnabled = mPreferences!!.getJavaScriptEnabled()

        mUIController.tabChanged(mLightningView)
    }

    override fun onReceivedError(webview: WebView, errorCode: Int, error: String, failingUrl: String) {
        webview.settings.javaScriptEnabled = true
        // TODO: fix reload button - causes white screen
        val start = "<html><head><script language=\"javascript\"> function reload(){setTimeout(function(){window.history.back();}, 500);"
        val start1 = "};</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"file:///android_asset/warning.png\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
        val title = mActivity.getString(R.string.error_title)
        val start2 = "</h1><p></p><div class=\"error-code\">"
        val start3 = "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\"><!-- <button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">"
        val reload = mActivity.getString(R.string.error_reload)
        val end = "</button> --> </div></div></div></body></center></html>"
        webview.loadDataWithBaseURL(null, start + start1 + title + start2 + error + start3 + reload + end, "text/html; charset=utf-8", "UTF-8", null)
        webview.invalidate()
        webview.settings.javaScriptEnabled = mPreferences!!.getJavaScriptEnabled()
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
        if (mPreferences!!.getBlockMalwareEnabled()) {
            if (mMalwareBlock.isMalware(url)) {
                view.settings.javaScriptEnabled = true
                val start = "<html><head><script language=\"javascript\"> function reload(){setTimeout(function(){window.history.back();}, 500);"
                val start1 = "};</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"file:///android_asset/warning.png\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
                val title = mActivity.getString(R.string.malware_title)
                val start2 = "</h1><p></p><div class=\"error-code\">"
                val start3 = "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\"><!-- <button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">"
                val reload = mActivity.getString(R.string.error_reload)
                val error = mActivity.getString(R.string.malware_message)
                val end = "</button> --> </div></div></div></body></center></html>"
                view.loadDataWithBaseURL(null, start + start1 + title + start2 + error + start3 + reload + end, "text/html; charset=utf-8", "UTF-8", null)
                view.invalidate()
                view.settings.javaScriptEnabled = mPreferences!!.getJavaScriptEnabled()
            }
        }

        if (mPreferences!!.getSiteBlockChoice() === 2) {
            if (mPreferences!!.getSiteBlockString("") !== "" && mPreferences!!.getSiteBlockString("") != null) {
                val arrayOfURLs = mPreferences!!.getSiteBlockString("")
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
                        view.settings.javaScriptEnabled = true
                        val start = "<html><head><script language=\"javascript\"> function reload(){setTimeout(function(){window.history.back();}, 500);"
                        val start1 = "};</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"file:///android_asset/warning.png\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
                        val title = mActivity.getString(R.string.blocked_title)
                        val start2 = "</h1><p></p><div class=\"error-code\">"
                        val start3 = "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\"><!-- <button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">"
                        val reload = mActivity.getString(R.string.error_reload)
                        val end = "</button> --> </div></div></div></body></center></html>"
                        view.loadDataWithBaseURL(null, start + start1 + title + start2 + "NET::SITE_BLOCKED_BY_LIST" + start3 + reload + end, "text/html; charset=utf-8", "UTF-8", null)
                        view.invalidate()
                        view.settings.javaScriptEnabled = mPreferences!!.getJavaScriptEnabled()
                    }
                }
            }
        } else if (mPreferences!!.getSiteBlockChoice() === 3) {
            if (mPreferences!!.getSiteBlockString("") !== "" && mPreferences!!.getSiteBlockString("") != null) {
                val arrayOfURLs = mPreferences!!.getSiteBlockString("")
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
                        view.settings.javaScriptEnabled = true
                        val start = "<html><head><script language=\"javascript\"> function reload(){setTimeout(function(){window.history.back();}, 500);"
                        val start1 = "};</script><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color:#f7f7f7; color: #646464; font-family: 'Segoe UI', Tahoma, sans-serif; font-size: 75%;}div{display:block;}h1{margin-top: 0; color: #333; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{-webkit-user-select: none; background: rgb(76, 142, 250); border: 0; border-radius: 2px; box-sizing: border-box; color: #fff; cursor: pointer; font-size: .875em; margin: 0; padding: 10px 24px; transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: #777; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}.reload{border: none; padding: 12px 16px; font-size: 16px; cursor: pointer;}.reload:before{content: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='none' d='M0 0h24v24H0V0z'/%3E%3Cpath d='M17.65 6.35c-1.63-1.63-3.94-2.57-6.48-2.31-3.67.37-6.69 3.35-7.1 7.02C3.52 15.91 7.27 20 12 20c3.19 0 5.93-1.87 7.21-4.56.32-.67-.16-1.44-.9-1.44-.37 0-.72.2-.88.53-1.13 2.43-3.84 3.97-6.8 3.31-2.22-.49-4.01-2.3-4.48-4.52C5.31 9.44 8.26 6 12 6c1.66 0 3.14.69 4.22 1.78l-1.51 1.51c-.63.63-.19 1.71.7 1.71H19c.55 0 1-.45 1-1V6.41c0-.89-1.08-1.34-1.71-.71l-.64.65z'/%3E%3C/svg%3E\"); filter: invert(1); width: 20px; float: left; margin-right: 5px; margin-top: -2px;}</style></head><center><body class=\"offline\">\n" + "<div class=\"interstitial-wrapper\"><div id=\"main-content\"><img src=\"file:///android_asset/warning.png\" height=\"52\" width=\"52\"><br><br><div class=\"icon icon-offline\"></div><div id=\"main-message\"><h1>"
                        val title = mActivity.getString(R.string.blocked_title)
                        val start2 = "</h1><p></p><div class=\"error-code\">"
                        val start3 = "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\"><!-- <button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">"
                        val reload = mActivity.getString(R.string.error_reload)
                        val end = "</button> --> </div></div></div></body></center></html>"
                        view.loadDataWithBaseURL(null, start + start1 + title + start2 + "NET::SITE_BLOCKED_BY_LIST" + start3 + reload + end, "text/html; charset=utf-8", "UTF-8", null)
                        view.invalidate()
                        view.settings.javaScriptEnabled = mPreferences!!.getJavaScriptEnabled()
                    }
                }
            }
        }


        mLightningView.titleInfo.setFavicon(null)
        if (mLightningView.isShown) {
            mUIController.updateUrl(url, true)
            mUIController.showActionBar()
        }
        mUIController.tabChanged(mLightningView)
    }

    override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler,
                                           host: String, realm: String) {

        val builder = AlertDialog.Builder(mActivity)

        val dialogView = LayoutInflater.from(mActivity).inflate(R.layout.dialog_auth_request, null)

        val realmLabel = dialogView.findViewById<TextView>(R.id.auth_request_realm_textview)
        val name = dialogView.findViewById<EditText>(R.id.auth_request_username_edittext)
        val password = dialogView.findViewById<EditText>(R.id.auth_request_password_edittext)

        realmLabel.text = mActivity.getString(R.string.label_realm, realm)

        builder.setView(dialogView)
                .setTitle(R.string.title_sign_in)
                .setCancelable(true)
                .setPositiveButton(R.string.title_sign_in
                ) { dialog, id ->
                    val user = name.text.toString()
                    val pass = password.text.toString()
                    handler.proceed(user.trim { it <= ' ' }, pass.trim { it <= ' ' })
                    Log.d(TAG, "Attempting HTTP Authentication")
                }
                .setNegativeButton(R.string.action_cancel
                ) { dialog, id -> handler.cancel() }
        val dialog = builder.create()
        dialog.show()
        BrowserDialog.setDialogSize(mActivity, dialog)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
        if (view.isShown && mLightningView.mPreferences.getTextReflowEnabled() &&
                Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (mIsRunning)
                return
            val changeInPercent = Math.abs(100 - 100 / mZoomScale * newScale)
            if (changeInPercent > 2.5f && !mIsRunning) {
                mIsRunning = view.postDelayed({
                    mZoomScale = newScale
                    view.evaluateJavascript(Constants.JAVASCRIPT_TEXT_REFLOW) { mIsRunning = false }
                }, 100)
            }

        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        val errorCodeMessageCodes = getAllSslErrorMessageCodes(error)

        val stringBuilder = StringBuilder()
        for (messageCode in errorCodeMessageCodes) {
            stringBuilder.append(" - ").append(mActivity.getString(messageCode)).append('\n')
        }
        val alertMessage = mActivity.getString(R.string.message_insecure_connection, stringBuilder.toString())

        val builder = AlertDialog.Builder(mActivity)
        builder.setTitle(mActivity.getString(R.string.title_warning))
        builder.setMessage(alertMessage)
                .setCancelable(true)
                .setPositiveButton(mActivity.getString(R.string.action_yes)
                ) { dialog, id -> handler.proceed() }
                .setNegativeButton(mActivity.getString(R.string.action_no)
                ) { dialog, id -> handler.cancel() }
        val dialog = builder.show()
        BrowserDialog.setDialogSize(mActivity, dialog)
    }

    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
        val builder = AlertDialog.Builder(mActivity)
        builder.setTitle(mActivity.getString(R.string.title_form_resubmission))
        builder.setMessage(mActivity.getString(R.string.message_form_resubmission))
                .setCancelable(true)
                .setPositiveButton(mActivity.getString(R.string.action_yes)
                ) { dialog, id -> resend.sendToTarget() }
                .setNegativeButton(mActivity.getString(R.string.action_no)
                ) { dialog, id -> dontResend.sendToTarget() }
        val alert = builder.create()
        alert.show()
        BrowserDialog.setDialogSize(mActivity, alert)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return shouldOverrideLoading(view, request.url.toString()) || super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return shouldOverrideLoading(view, url) || super.shouldOverrideUrlLoading(view, url)
    }

    private fun shouldOverrideLoading(view: WebView, url: String): Boolean {

        // Check if configured proxy is available
        if (!mProxyUtils!!.isProxyReady(mActivity)) {
            // User has been notified
            return true
        }

        val headers = mLightningView.getRequestHeaders()

        if (mLightningView.isIncognito) {
            // If we are in incognito, immediately load, we don't want the url to leave the app
            return continueLoadingUrl(view, url, headers)
        }
        if (URLUtil.isAboutUrl(url)) {
            // If this is an about page, immediately load, we don't need to leave the app
            return continueLoadingUrl(view, url, headers)
        }

        return if (isMailOrIntent(url, view) || mIntentUtils.startActivityForUrl(view, url)) {
            // If it was a mailto: link, or an intent, or could be launched elsewhere, do that
            true
        } else continueLoadingUrl(view, url, headers)

        // If none of the special conditions was met, continue with loading the url
    }

    private fun continueLoadingUrl(webView: WebView,
                                   url: String,
                                   headers: Map<String, String>): Boolean {
        if (headers.isEmpty()) {
            return false
        } else if (Utils.doesSupportHeaders()) {
            webView.loadUrl(url, headers)
            return true
        } else {
            return false
        }
    }


    private fun isMailOrIntent(url: String, view: WebView): Boolean {
        if (url.startsWith("mailto:")) {
            val mailTo = MailTo.parse(url)
            val i = Utils.newEmailIntent(mailTo.to, mailTo.subject,
                    mailTo.body, mailTo.cc)
            mActivity.startActivity(i)
            view.reload()
            return true
        } else if (url.startsWith("intent://")) {
            var intent: Intent?
            try {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } catch (ignored: URISyntaxException) {
                intent = null
            }

            if (intent != null) {
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.component = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    intent.selector = null
                }
                try {
                    mActivity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, "ActivityNotFoundException")
                }

                return true
            }
        } else if (URLUtil.isFileUrl(url) && !url.isSpecialUrl()) {
            val file = File(url.replace(Constants.FILE, ""))

            if (file.exists()) {
                val newMimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(Utils.guessFileExtension(file.toString()))

                val intent = Intent(Intent.ACTION_VIEW)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val contentUri = FileProvider.getUriForFile(mActivity, BuildConfig.APPLICATION_ID + ".fileprovider", file)
                intent.setDataAndType(contentUri, newMimeType)

                try {
                    mActivity.startActivity(intent)
                } catch (e: Exception) {
                    println("SmartCookieWeb: cannot open downloaded file")
                }

            } else {
                Utils.showSnackbar(mActivity, R.string.message_open_download_fail)
            }
            return true
        }
        return false
    }

    companion object {

        private val TAG = "LightningWebClient"

        fun exists(URLName: String): Boolean {

            try {
                HttpURLConnection.setFollowRedirects(false)
                // note : you may also need
                // HttpURLConnection.setInstanceFollowRedirects(false)
                val con = URL(URLName)
                        .openConnection() as HttpURLConnection
                con.requestMethod = "HEAD"
                return con.responseCode == HttpURLConnection.HTTP_OK
            } catch (e: Exception) {
                e.printStackTrace()
                return false
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

        private fun getAllSslErrorMessageCodes(error: SslError): List<Int> {
            val errorCodeMessageCodes = ArrayList<Int>(1)

            if (error.hasError(SslError.SSL_DATE_INVALID)) {
                errorCodeMessageCodes.add(R.string.message_certificate_date_invalid)
            }
            if (error.hasError(SslError.SSL_EXPIRED)) {
                errorCodeMessageCodes.add(R.string.message_certificate_expired)
            }
            //causes issues
            //if (error.hasError(SslError.SSL_IDMISMATCH)) {
            //   errorCodeMessageCodes.add(R.string.message_certificate_domain_mismatch);
            //}
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
    }
}
*/