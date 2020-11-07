// Copyright 2020 CookieJarApps
package com.cookiegames.smartcookie.popup;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.cookiegames.smartcookie.AppTheme;
import com.cookiegames.smartcookie.R;
import com.cookiegames.smartcookie.preference.UserPreferences;

import javax.inject.Inject;

public class CustomAdapter extends BaseAdapter {

    private Context mContext;
    private String[]  Title;
    private int[] imge;

    public CustomAdapter(Context context, String[] text1,int[] imageIds) {
        mContext = context;
        Title = text1;
        imge = imageIds;

    }

    public int getCount() {
        // TODO Auto-generated method stub
        return Title.length;
    }

    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View row;
        row = inflater.inflate(R.layout.menu_row, parent, false);
        TextView title;
        ImageView i1;
        i1 = (ImageView) row.findViewById(R.id.imgIcon);
        title = (TextView) row.findViewById(R.id.txtTitle);
        title.setTextSize(14);
        title.setText(Title[position]);
        i1.setImageResource(imge[position]);
        int tint = ContextCompat.getColor(mContext, R.color.black);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = mContext.getTheme();
        theme.resolveAttribute(R.attr.iconColor, typedValue, true);
        @ColorInt int color = typedValue.data;

        if(color == -16777216){
            tint = ContextCompat.getColor(mContext, R.color.black);
        }
        else{
            tint = ContextCompat.getColor(mContext, R.color.white);
        }

        ImageViewCompat.setImageTintList(i1, ColorStateList.valueOf(tint));

        return (row);
    }
}