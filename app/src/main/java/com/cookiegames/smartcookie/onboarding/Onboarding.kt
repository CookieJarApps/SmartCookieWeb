/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 10/01/2020 */

package com.cookiegames.smartcookie.onboarding

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.cookiegames.smartcookie.AppTheme
import com.cookiegames.smartcookie.MainActivity
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.preference.UserPreferences
import com.github.appintro.AppIntro2
import com.github.appintro.AppIntroFragment
import javax.inject.Inject


class Onboarding : AppIntro2(){

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)

        setTheme(when (userPreferences.useTheme) {
            AppTheme.LIGHT -> R.style.Theme_LightTheme
            AppTheme.DARK -> R.style.Theme_DarkTheme
            AppTheme.BLACK -> R.style.Theme_BlackTheme
        })

        super.onCreate(savedInstanceState)
        showSlide()
    }

    private fun showSlide(){
        isWizardMode = true
        
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

        val a = TypedValue()
        theme.resolveAttribute(android.R.attr.windowBackground, a, true)

        addSlide(AppIntroFragment.newInstance(
                title = resources.getString(R.string.app_name),
                backgroundColor = col,
                titleColor = textCol,
                descriptionColor = textCol,
                imageDrawable = R.drawable.slide1,
                description = resources.getString(R.string.app_desc)
        ))

        addSlide(PermsFragment.newInstance())

        addSlide(SearchEngineFragment.newInstance())

        addSlide(ThemeChoiceFragment.newInstance())

        addSlide(NavbarChoiceFragment.newInstance())

        askForPermissions(
                permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                slideNumber = 2,
                required = false)


        setIndicatorColor(
                selectedIndicatorColor = (Color.BLACK),
                unselectedIndicatorColor = (Color.GRAY)
        )
    }

    private fun main()
    {
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        main()
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"
        main()
        finish()
    }

    override fun onSlideChanged(oldFragment: Fragment?, newFragment: Fragment?) {
        super.onSlideChanged(oldFragment, newFragment)
        Log.d("Hello", "Changed")
    }

    override fun onIntroFinished() {
        super.onIntroFinished()
    }

    companion object {
        private const val PRIVATE_MODE = 0
        private const val PREFERENCE_CONFIGURATION_NAME = "configuration"
        private const val FIRST_TIME = "isFirstRun"
    }
}
