package com.cookiegames.smartcookie.settings.fragment

import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.ProxyChoice
import com.cookiegames.smartcookie.constant.SCHEME_BLANK
import com.cookiegames.smartcookie.constant.SCHEME_BOOKMARKS
import com.cookiegames.smartcookie.constant.SCHEME_HOMEPAGE
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.extensions.withSingleChoiceItems
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.search.SearchEngineProvider
import com.cookiegames.smartcookie.search.Suggestions
import com.cookiegames.smartcookie.search.engine.BaseSearchEngine
import com.cookiegames.smartcookie.search.engine.CustomSearch
import com.cookiegames.smartcookie.utils.FileUtils
import com.cookiegames.smartcookie.utils.ProxyUtils
import com.cookiegames.smartcookie.utils.ThemeUtils
import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.preference.Preference
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.cookiegames.smartcookie.browser.PasswordChoice
import com.cookiegames.smartcookie.browser.SiteBlockChoice
import com.cookiegames.smartcookie.dialog.BrowserDialog.setDialogSize
import com.cookiegames.smartcookie.extensions.toast
import com.cookiegames.smartcookie.settings.activity.SettingsActivity
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * The general settings of the app.
 */
class ExtensionsSettingsFragment : AbstractSettingsFragment() {

    @Inject lateinit var userPreferences: UserPreferences

    private lateinit var proxyChoices: Array<String>

    override fun providePreferencesXmlResource() = R.xml.preference_extensions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        proxyChoices = resources.getStringArray(R.array.blocked_sites)

        val prefs: SharedPreferences = activity.getSharedPreferences("com.cookiegames.smartcookie", MODE_PRIVATE)

        var pref: Preference = findPreference(EXTENSIONS_LIST)
        pref.setOnPreferenceClickListener {
            val path = activity.getFilesDir()
            val letDirectory = File(path, "extensions")
            letDirectory.mkdirs()
            val file = File(letDirectory, "extension_file.txt")
            if(!file.exists()){
                file.appendText("/* begin extensions file */")
            }
            var allMatches = ArrayList<String>()
            var inputAsString = FileInputStream(file).bufferedReader().use { it.readText() }
            var match: Matcher = Pattern.compile("(?! begin extensions file)(?!End)(?<=/\\*)(.*?)(?=\\*/)").matcher(inputAsString)
            while (match.find()) {
                allMatches.add(match.group())
            }

            var formattedString:String = allMatches.toString()
            .replace("[", "")  //remove the right bracket
            .replace("]", "")  //remove the left bracket
            .trim()

            if(formattedString == ""){
                formattedString = resources.getString(R.string.none)
            }

            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Extensions List")
            builder.setMessage(formattedString)


            builder.setPositiveButton(resources.getString(R.string.action_ok)){dialogInterface , which ->

            }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.setCancelable(false)
            alertDialog.show()
            true
        }

        var pref2: Preference = findPreference(UNINSTALL)
        pref2.setOnPreferenceClickListener {
            val builder = AlertDialog.Builder(activity)
            val edittext = EditText(activity)
            builder.setMessage(resources.getString(R.string.uninstall_more))
            builder.setTitle(resources.getString(R.string.uninstall_extension))

            builder.setView(edittext)

            builder.setPositiveButton(resources.getString(R.string.action_ok)){dialogInterface , which ->
                val text = edittext.text.toString()
                val path = activity.getFilesDir()
                var result = ""
                val letDirectory = File(path, "extensions")
                letDirectory.mkdirs()
                val file = File(letDirectory, "extension_file.txt")
                if(!file.exists()){
                    file.appendText("/* begin extensions file */")
                }
                var inputAsString = FileInputStream(file).bufferedReader().use { it.readText() }
                if(inputAsString.contains("/*") && inputAsString.contains("*/") && inputAsString.contains("/*" + edittext.text.toString()) && edittext.text.toString() != ""){
                    var string1 = inputAsString.substring(inputAsString.indexOf("/*" + edittext.text.toString() + "*/") + 4 + edittext.text.toString().length, inputAsString.indexOf("/*End " + edittext.text.toString() + "*/"))
                    inputAsString = inputAsString.replace(string1, "")
                    inputAsString = inputAsString.replace("/*" + edittext.text.toString() + "*/", "")
                    inputAsString = inputAsString.replace("/*End " + edittext.text.toString() + "*/", "")
                    PrintWriter(file).close()
                    file.appendText(inputAsString)
                    Toast.makeText(activity,"Extension uninstalled.",Toast.LENGTH_SHORT).show()
                    true
                }
                else{
                    val toast = Toast.makeText(activity, "Extension not installed", Toast.LENGTH_LONG)
                    toast.show()
                    false
                }
            }
            builder.setNeutralButton("Delete All"){dialogInterface, which ->
                val path = activity.getFilesDir()
                val letDirectory = File(path, "extensions")
                letDirectory.mkdirs()
                val file = File(letDirectory, "extension_file.txt")
                if(!file.exists()){
                    file.appendText("/* begin extensions file */")
                }
                PrintWriter(file).close()
                Toast.makeText(activity, "Uninstalled all extensions", Toast.LENGTH_LONG).show()
            }
            builder.setNegativeButton(resources.getString(R.string.action_cancel)){dialogInterface , which ->

            }

            builder.show()
            true
        }

    }




    companion object {
        private const val EXTENSIONS_LIST = "extensions_list"
        private const val UNINSTALL = "uninstall_extension"
    }
}
