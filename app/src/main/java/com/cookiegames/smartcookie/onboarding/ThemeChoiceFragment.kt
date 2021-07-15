/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 10/01/2020 */

package com.cookiegames.smartcookie.onboarding

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AbsListView.CHOICE_MODE_SINGLE
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.Fragment
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.search.SearchEngineProvider
import javax.inject.Inject


class ThemeChoiceFragment : Fragment() {
    @Inject
    lateinit var searchEngineProvider: SearchEngineProvider

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_onboarding_theme_choice, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var col: Int
        var textCol: Int

        when (userPreferences.useTheme) {
            AppTheme.LIGHT ->{
                col = Color.WHITE
                textCol = Color.BLACK
            }
            AppTheme.DARK ->{
                textCol = Color.WHITE
                col = Color.BLACK
            }
            AppTheme.BLACK ->{
                textCol = Color.WHITE
                col = Color.BLACK
            }
        }

        requireView().setBackgroundColor(col)
        requireView().findViewById<TextView>(R.id.themeTitle).setTextColor(textCol)

        val listView = activity?.findViewById<ListView>(R.id.themes)
        val values = AppTheme.values().map { it }
        val names = AppTheme.values().map { it.toDisplayString() }
        val arrayAdapter = ArrayAdapter(activity as Context, android.R.layout.simple_list_item_single_choice, names)
        listView?.adapter = arrayAdapter
        listView?.choiceMode = CHOICE_MODE_SINGLE
        listView?.setItemChecked(0, true)

        listView!!.onItemClickListener = OnItemClickListener { _, _, i, _ ->
            userPreferences.useTheme = values[i]
            activity?.recreate()
        }
    }

    private fun AppTheme.toDisplayString(): String = getString(when (this) {
        AppTheme.LIGHT -> R.string.light_theme
        AppTheme.DARK -> R.string.dark_theme
        AppTheme.BLACK -> R.string.black_theme
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)
    }

    companion object {
        fun newInstance() : ThemeChoiceFragment {
            return ThemeChoiceFragment()
        }
    }
}
