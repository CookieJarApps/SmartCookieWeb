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
import android.widget.AbsListView.CHOICE_MODE_SINGLE
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.search.SearchEngineProvider
import com.cookiegames.smartcookie.search.engine.BaseSearchEngine
import javax.inject.Inject


class SearchEngineFragment : Fragment() {
    @Inject
    lateinit var searchEngineProvider: SearchEngineProvider

    @Inject
    lateinit var userPreferences: UserPreferences

    private lateinit var checkBox: CheckBox

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.search_choice, container, false)

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
        requireView().findViewById<TextView>(R.id.permissionsTitle).setTextColor(textCol)

        val listView = activity?.findViewById<ListView>(R.id.engines)
        val values = convertSearchEngineToString(searchEngineProvider.provideAllSearchEngines())
        val arrayAdapter = ArrayAdapter(activity as Context, android.R.layout.simple_list_item_single_choice, values.drop(1))
        listView?.adapter = arrayAdapter
        listView?.choiceMode = CHOICE_MODE_SINGLE
        listView?.setItemChecked(0, true)

        listView!!.onItemClickListener = OnItemClickListener { adapterView, view, i, l ->
            userPreferences.searchChoice = searchEngineProvider.mapSearchEngineToPreferenceIndex(searchEngineProvider.provideAllSearchEngines()[i+1])
        }
    }

    private fun convertSearchEngineToString(searchEngines: List<BaseSearchEngine>): Array<String> =
            searchEngines.map { getString(it.titleRes) }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)
    }

    companion object {
        fun newInstance() : SearchEngineFragment {
            return SearchEngineFragment()
        }
    }
}
