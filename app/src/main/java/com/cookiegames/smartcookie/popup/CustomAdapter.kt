/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 7/11/2020 */

package com.cookiegames.smartcookie.popup

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.preference.UserPreferences
import javax.inject.Inject

class CustomAdapter(private val mContext: Context, private val Title: Array<String>, private val imge: IntArray) : BaseAdapter() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun getCount(): Int {
        // TODO Auto-generated method stub
        return Title.size
    }

    override fun getItemId(position: Int): Long {
        // TODO Auto-generated method stub
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val row: View
        row = inflater.inflate(R.layout.menu_row, parent, false)
        val title: TextView
        val i1: ImageView
        i1 = row.findViewById<View>(R.id.imgIcon) as ImageView
        title = row.findViewById<View>(R.id.txtTitle) as TextView
        title.textSize = 14f
        title.text = Title[position]
        i1.setImageResource(imge[position])
        var tint = ContextCompat.getColor(mContext, R.color.black)
        val typedValue = TypedValue()
        val theme = mContext.theme
        theme.resolveAttribute(R.attr.iconColor, typedValue, true)
        @ColorInt val color = typedValue.data
        //TODO: find another way to get the theme here, this'll break if I add new themes
        if (color == -16777216) {
            tint = ContextCompat.getColor(mContext, R.color.black)
            title.setTextColor(ContextCompat.getColor(mContext, R.color.black))
        } else {
            tint = ContextCompat.getColor(mContext, R.color.white)
            title.setTextColor(ContextCompat.getColor(mContext, R.color.white))
        }
        ImageViewCompat.setImageTintList(i1, ColorStateList.valueOf(tint))
        return row
    }

    override fun getItem(position: Int): Any {
        return Title.get(position)
    }

}