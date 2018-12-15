/* Copyright (c) 2016 Vlad Todosin */
package com.cookiegames.smartcookie.adapters;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.amulyakhare.textdrawable.*;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.cookiegames.smartcookie.R;

import java.util.ArrayList;

public class BookAdapter extends BaseAdapter {
    private Context mContext;
    ArrayList<String>    names;
    ArrayList<String>    urls;
    public BookAdapter(Context c, ArrayList<String> etc,ArrayList<String> url) {
        mContext = c;
        names = etc;
        urls = url;
    }


    public int getCount() {
        return names.size();
    }


    public Object getItem(int position) {
        return names.get(position);
    }


    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        View myView = convertView;
        if (convertView == null) {
          try {
              LayoutInflater li = ((Activity) mContext).getLayoutInflater();
              myView = li.inflate(R.layout.bookmarkitem, null);
              ImageView img = (ImageView) myView.findViewById(R.id.img2);
              TextView txt = (TextView) myView.findViewById(R.id.txt2);
              TextView url = (TextView) myView.findViewById(R.id.txt3);
              ColorGenerator gen = ColorGenerator.MATERIAL;
              int col = gen.getColor(names.get(position));
              TextDrawable drawable = TextDrawable.builder()
                      .buildRound(String.valueOf(names.get(position).charAt(0)), col);
              img.setImageDrawable(drawable);
              txt.setText(names.get(position));
              String n = urls.get(position).substring(0, Math.min(urls.get(position).length(), 10));
              url.setText(n + "...");
          }
          catch(Exception e){}
          } else {
            myView = convertView;
        }
        return  myView;
    }
}
