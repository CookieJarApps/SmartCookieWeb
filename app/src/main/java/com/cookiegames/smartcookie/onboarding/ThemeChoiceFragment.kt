/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 10/01/2020 */

package com.cookiegames.smartcookie.onboarding

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
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
import com.cookiegames.smartcookie.search.engine.BaseSearchEngine
import com.github.appintro.SlidePolicy
import org.w3c.dom.Text
import javax.inject.Inject


class ThemeChoiceFragment : Fragment(), SlidePolicy {
    @Inject
    lateinit var searchEngineProvider: SearchEngineProvider

    @Inject
    lateinit var userPreferences: UserPreferences

    private lateinit var checkBox: CheckBox

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.theme_choice, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //checkBox = view.findViewById(R.id.check_box)
        var col: Int
        when (userPreferences.useTheme) {
            AppTheme.LIGHT -> col = Color.WHITE
            AppTheme.DARK -> col = Color.BLACK
            AppTheme.BLACK -> col = Color.BLACK
        }
        var textCol: Int
        when (userPreferences.useTheme) {
            AppTheme.LIGHT -> textCol = Color.BLACK
            AppTheme.DARK -> textCol = Color.WHITE
            AppTheme.BLACK -> textCol = Color.WHITE
        }

        requireView().setBackgroundColor(col)
        requireView().findViewById<TextView>(R.id.textView4).setTextColor(textCol)

        val listView = activity?.findViewById<ListView>(R.id.themes)
        val values = AppTheme.values().map { it }
        val names = AppTheme.values().map { it.toDisplayString() }
        val arrayAdapter = ArrayAdapter(activity as Context, android.R.layout.simple_list_item_single_choice, names)
        listView?.adapter = arrayAdapter
        listView?.choiceMode = CHOICE_MODE_SINGLE
        listView?.setItemChecked(0, true)

        listView!!.onItemClickListener = OnItemClickListener { adapterView, view, i, l ->
            userPreferences.useTheme = values[i]
            activity?.recreate()
        }
    }

    private fun AppTheme.toDisplayString(): String = getString(when (this) {
        AppTheme.LIGHT -> R.string.light_theme
        AppTheme.DARK -> R.string.dark_theme
        AppTheme.BLACK -> R.string.black_theme
        /*AppTheme.BLUE -> R.string.blue_theme
        AppTheme.GREEN -> R.string.green_theme
        AppTheme.YELLOW -> R.string.yellow_theme*/
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)
    }

    override val isPolicyRespected: Boolean
        get() = true //checkBox.isChecked

    override fun onUserIllegallyRequestedNextPage() {
        Toast.makeText(
                requireContext(),
                R.string.tabs,
                Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        fun newInstance() : ThemeChoiceFragment {
            return ThemeChoiceFragment()
        }
    }
}
