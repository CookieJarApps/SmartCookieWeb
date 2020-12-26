// Copyright 2020 CookieJarApps MPL
package com.cookiegames.smartcookie.reading.activity

import android.animation.ObjectAnimator
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.di.MainScheduler
import com.cookiegames.smartcookie.di.NetworkScheduler
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog.setDialogSize
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.reading.activity.ReadingActivity
import com.cookiegames.smartcookie.utils.ThemeUtils
import com.cookiegames.smartcookie.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import net.dankito.readability4j.Readability4J
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.text.BreakIterator
import java.util.*
import javax.inject.Inject

class ReadingActivity : AppCompatActivity() {
    @JvmField
    @BindView(R.id.textViewTitle)
    var mTitle: TextView? = null

    @JvmField
    @BindView(R.id.textViewBody)
    var mBody: TextView? = null

    @JvmField
    @Inject
    var mUserPreferences: UserPreferences? = null

    @JvmField
    @Inject
    @NetworkScheduler
    var mNetworkScheduler: Scheduler? = null

    @JvmField
    @Inject
    @MainScheduler
    var mMainScheduler: Scheduler? = null
    private var mInvert = false
    private var mUrl: String? = null
    private var mTextSize = 0
    private var mProgressDialog: ProgressDialog? = null
    private val mPageLoaderSubscription: Disposable? = null
    private val originalHtml: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        this.injector.inject(this)
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.fade_out_scale)
        mInvert = mUserPreferences!!.invertColors
        val color: Int
        if (mInvert) {
            if (mUserPreferences!!.useTheme === AppTheme.LIGHT) {
                setTheme(R.style.Theme_SettingsTheme_Black)
                color = ThemeUtils.getPrimaryColor(this)
                window.setBackgroundDrawable(ColorDrawable(color))
            } else {
                setTheme(R.style.Theme_SettingsTheme)
                color = ThemeUtils.getPrimaryColor(this)
                window.setBackgroundDrawable(ColorDrawable(color))
            }
        } else {
            if (mUserPreferences!!.useTheme === AppTheme.LIGHT) {
                setTheme(R.style.Theme_SettingsTheme)
                color = ThemeUtils.getPrimaryColor(this)
                window.setBackgroundDrawable(ColorDrawable(color))
            } else if (mUserPreferences!!.useTheme === AppTheme.DARK) {
                setTheme(R.style.Theme_SettingsTheme_Dark)
                color = ThemeUtils.getPrimaryColor(this)
                window.setBackgroundDrawable(ColorDrawable(color))
            } else {
                setTheme(R.style.Theme_SettingsTheme_Black)
                color = ThemeUtils.getPrimaryColor(this)
                window.setBackgroundDrawable(ColorDrawable(color))
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reading_view)
        ButterKnife.bind(this)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        mTextSize = mUserPreferences!!.readingTextSize
        mBody!!.textSize = getTextSize(mTextSize)
        mTitle!!.text = getString(R.string.untitled)
        mBody!!.text = getString(R.string.loading)
        mTitle!!.visibility = View.INVISIBLE
        mBody!!.visibility = View.INVISIBLE
        val intent = intent
        try {
            if (!loadPage(intent)) {
                //setText(getString(R.string.untitled), getString(R.string.loading_failed));
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.reading, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private inner class loadData : AsyncTask<Void?, Void?, Void?>() {
        var extractedContentHtml: String? = null
        var extractedContentHtmlWithUtf8Encoding: String? = null
        var extractedContentPlainText: String? = null
        var title: String? = null
        var byline: String? = null
        var excerpt: String? = null


        override fun onPostExecute(aVoid: Void?) {
            val html: String? = extractedContentHtmlWithUtf8Encoding?.replace("image copyright".toRegex(), resources.getString(R.string.reading_mode_image_copyright) + " ")?.replace("image caption".toRegex(), resources.getString(R.string.reading_mode_image_caption) + " ")?.replace("￼".toRegex(), "")?.replace("<a".toRegex(), "<span")?.replace("</a>".toRegex(), "</span>")
            val doc = Jsoup.parse(html)
            for (element in doc.select("img")) {
                element.remove()
            }
            setText(title, Html.fromHtml(doc.outerHtml()))
            dismissProgressDialog()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            var url: URL
            try {
                val google = URL(mUrl)
                val line = BufferedReader(InputStreamReader(google.openStream()))
                var input: String?
                val stringBuffer = StringBuffer()
                while (line.readLine().also { input = it } != null) {
                    stringBuffer.append(input)
                }
                line.close()
                val htmlData = stringBuffer.toString()
                val readability4J = Readability4J(mUrl!!, htmlData) // url is just needed to resolve relative urls
                val article = readability4J.parse()
                extractedContentHtml = article.content
                extractedContentHtmlWithUtf8Encoding = article.contentWithUtf8Encoding
                extractedContentPlainText = article.textContent
                title = article.title
                byline = article.byline
                excerpt = article.excerpt
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }
    }

    @Throws(IOException::class)
    private fun loadPage(intent: Intent?): Boolean {
        if (intent == null) {
            return false
        }
        mUrl = intent.getStringExtra(LOAD_READING_URL)
        if (mUrl == null) {
            return false
        }
        if (supportActionBar != null) {
            supportActionBar!!.title = Utils.getDomainName(mUrl)
        }
        mProgressDialog = ProgressDialog(this@ReadingActivity)
        mProgressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.isIndeterminate = true
        mProgressDialog!!.setMessage(getString(R.string.loading))
        mProgressDialog!!.show()
        setDialogSize(this@ReadingActivity, mProgressDialog!!)
        loadData().execute()
        return true
    }

    private fun dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog!!.dismiss()
            mProgressDialog = null
        }
    }

    private class ReaderInfo internal constructor(val title: String, val body: String)

    private fun setText(title: String?, body: Spanned) {
        if (mTitle == null || mBody == null) return
        if (mTitle!!.visibility == View.INVISIBLE) {
            mTitle!!.alpha = 1.0f
            mTitle!!.visibility = View.VISIBLE
            mTitle!!.text = title
        } else {
            mTitle!!.text = title
        }
        if (mBody!!.visibility == View.INVISIBLE) {
            mBody!!.alpha = 1.0f
            mBody!!.visibility = View.VISIBLE
            mBody!!.text = body
        } else {
            mBody!!.text = body
        }
    }

    override fun onDestroy() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog!!.dismiss()
            mProgressDialog = null
        }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            overridePendingTransition(R.anim.fade_in_scale, R.anim.slide_out_to_right)
        }
    }

    fun translate(lang: String?) {
        val client = OkHttpClient()
        val iterator = BreakIterator.getSentenceInstance(Locale.US)
        val source = mBody!!.text.toString()
        iterator.setText(source)
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            start = end
            end = iterator.next()
        }
        val mySearchUrl = HttpUrl.Builder()
                .scheme("https")
                .host("cookiejarapps.com")
                .addPathSegment("translate")
                .addQueryParameter("text", Html.toHtml(mBody!!.text as Spanned).replace("\"", "\\\""))
                .addQueryParameter("lang", lang)
                .build()
        val request = Request.Builder()
                .url(mySearchUrl)
                .addHeader("Accept", "application/json")
                .method("GET", null)
                .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            // TODO: This is... not a good way of doing this...
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                } else {
                    val values = response.body!!.string()
                    runOnUiThread {
                        try {
                            val Jobject = JSONObject(values)
                            mBody!!.text = Html.fromHtml(Jobject.getString("text"))
                        } catch (ignored: JSONException) {
                        }
                    }
                }
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.invert_item -> {
                mUserPreferences!!.invertColors = !mInvert
                if (mUrl != null) {
                    launch(this, mUrl!!)
                    finish()
                }
            }
            R.id.translate_item -> {
                val lang = "fr"
                val builderSingle = AlertDialog.Builder(this@ReadingActivity)
                builderSingle.setTitle(resources.getString(R.string.translate_to))
                val arrayAdapter = ArrayAdapter<String>(this@ReadingActivity, android.R.layout.select_dialog_singlechoice)
                arrayAdapter.add("English")
                arrayAdapter.add("Français")
                arrayAdapter.add("Português")
                arrayAdapter.add("Português do Brasil")
                arrayAdapter.add("Italiano")
                val languages = arrayOf("en", "fr", "pt-pt", "pt-br", "it")
                builderSingle.setNegativeButton("cancel") { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                builderSingle.setAdapter(arrayAdapter) { dialog: DialogInterface?, which: Int -> translate(languages[which]) }
                builderSingle.show()
            }
            R.id.text_size_item -> {
                val view = LayoutInflater.from(this).inflate(R.layout.dialog_seek_bar, null)
                val bar = view.findViewById<SeekBar>(R.id.text_size_seekbar)
                bar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(view: SeekBar, size: Int, user: Boolean) {
                        mBody!!.textSize = getTextSize(size)
                    }

                    override fun onStartTrackingTouch(arg0: SeekBar) {}
                    override fun onStopTrackingTouch(arg0: SeekBar) {}
                })
                bar.max = 5
                bar.progress = mTextSize
                val builder = MaterialAlertDialogBuilder(this)
                        .setView(view)
                        .setTitle(R.string.size)
                        .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, arg1: Int ->
                            mTextSize = bar.progress
                            mBody!!.textSize = getTextSize(mTextSize)
                            mUserPreferences!!.readingTextSize = bar.progress
                        }
                val dialog: Dialog = builder.show()
                setDialogSize(this, dialog)
            }
            else -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val LOAD_READING_URL = "ReadingUrl"

        /**
         * Launches this activity with the necessary URL argument.
         *
         * @param context The context needed to launch the activity.
         * @param url     The URL that will be loaded into reading mode.
         */
        fun launch(context: Context, url: String) {
            val intent = Intent(context, ReadingActivity::class.java)
            intent.putExtra(LOAD_READING_URL, url)
            context.startActivity(intent)
        }

        private const val TAG = "ReadingActivity"
        private const val XXLARGE = 30.0f
        private const val XLARGE = 26.0f
        private const val LARGE = 22.0f
        private const val MEDIUM = 18.0f
        private const val SMALL = 14.0f
        private const val XSMALL = 10.0f
        private fun getTextSize(size: Int): Float {
            return when (size) {
                0 -> XSMALL
                1 -> SMALL
                2 -> MEDIUM
                3 -> LARGE
                4 -> XLARGE
                5 -> XXLARGE
                else -> MEDIUM
            }
        }
    }
}