/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 10/01/2020 */

package com.cookiegames.smartcookie.onboarding

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.search.SearchEngineProvider
import com.github.appintro.SlidePolicy
import javax.inject.Inject


class NavbarChoiceFragment : Fragment() {
    @Inject
    lateinit var searchEngineProvider: SearchEngineProvider

    @Inject
    lateinit var userPreferences: UserPreferences

    private lateinit var checkBox: CheckBox

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.navbar_choice, container, false)

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

        val rGroup = getView()?.findViewById(R.id.radioGroup) as RadioGroup
        rGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.defaultNavbar -> userPreferences.bottomBar = false
                R.id.defaultNavbar2nd -> {
                    userPreferences.bottomBar = false
                    userPreferences.navbar = true
                }
                R.id.bottomNavbar -> userPreferences.bottomBar = true
            }
        }
        val rGroup2 = getView()?.findViewById(R.id.radioGroup2) as RadioGroup
        rGroup2.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.defaultTabs -> userPreferences.showTabsInDrawer = true
                R.id.fullTabs -> userPreferences.showTabsInDrawer = false
            }
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
    }

    companion object {
        fun newInstance() : NavbarChoiceFragment {
            return NavbarChoiceFragment()
        }
    }
}
