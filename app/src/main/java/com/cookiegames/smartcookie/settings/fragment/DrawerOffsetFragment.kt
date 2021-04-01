package com.cookiegames.smartcookie.settings.fragment

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Space
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.preference.UserPreferences
import com.google.android.material.slider.Slider
import javax.inject.Inject


class DrawerOffsetFragment : Fragment(R.layout.fragment_drawer_offset) {
    @Inject
    internal lateinit var userPreferences: UserPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        injector.inject(this)

        super.onViewCreated(view, savedInstanceState)

        val top = if(userPreferences.stackFromBottom) 0 else userPreferences.drawerOffset * 10
        val bottom = if(userPreferences.stackFromBottom) userPreferences.drawerOffset * 10 else 0

        if(!userPreferences.stackFromBottom){
            view.findViewById<Space>(R.id.spacer).visibility = View.GONE
        }

        val rv = view.findViewById<RelativeLayout>(R.id.drawer_row)
        val params = rv.layoutParams as LinearLayout.LayoutParams
        params.setMargins(0, top, 0, bottom)

        rv.layoutParams = params

        view.findViewById<Slider>(R.id.seekBar).addOnChangeListener { slider, value, fromUser ->
            if(userPreferences.stackFromBottom){
                params.setMargins(0, 0, 0, value.toInt() * 10)
            }
            else{
                params.setMargins(0, value.toInt() * 10, 0, 0)
            }

            userPreferences.drawerOffset = value.toInt()

            rv.layoutParams = params
        }
    }

    override fun onDestroy() {
        Toast.makeText(context, R.string.please_restart, Toast.LENGTH_LONG).show()
        super.onDestroy()
    }
}