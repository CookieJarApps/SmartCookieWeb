/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 10/01/2020 */

package com.cookiegames.smartcookie.onboarding

import android.Manifest
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
import com.github.appintro.SlidePolicy
import javax.inject.Inject


class PermsFragment : Fragment(), SlidePolicy {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.perms, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        requireView().findViewById<TextView>(R.id.textView2).setTextColor(textCol)
    }

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
        fun newInstance() : PermsFragment {
            return PermsFragment()
        }
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
