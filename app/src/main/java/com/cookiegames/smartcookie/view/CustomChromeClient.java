/*
 Copyright 2016 Vlad Todosin
*/
package com.cookiegames.smartcookie.view;


import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import android.widget.ProgressBar;

import com.cookiegames.smartcookie.controllers.TabManager;


public class CustomChromeClient extends WebChromeClient {
    private ProgressBar PBar;
    private AppCompatActivity ACTIVITY;
    private CustomView mWeb;
    private View v;
    private  CustomViewCallback callback;
    private boolean isVideoFullscreen; // Indicates if the video is being displayed using a custom view (typically full-screen)

    public CustomChromeClient(ProgressBar pBar, CustomView web, AppCompatActivity act) {
        super();
        mWeb = web;
        PBar = pBar;
        ACTIVITY = act;
        this.isVideoFullscreen = false;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        if (mWeb.isCurrentTab()) {
            if (newProgress < 100) {
                PBar.setVisibility(View.VISIBLE);
                PBar.setProgress(view.getProgress());
            } else {
                PBar.setProgress(0);
                PBar.setVisibility(View.GONE);
            }
        }
    }
    @Override
    public void onReceivedTitle(WebView view,String title){
        super.onReceivedTitle(view,title);
        TabManager.updateTabView();
    }
    @Override
    public void onShowCustomView(View view, CustomViewCallback callback)
    {
             v = view;
             view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
             ViewGroup gr = (ViewGroup) mWeb.getParent().getParent().getParent();
             if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN){
                 ACTIVITY.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
             }
             else{
                 ACTIVITY.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                         WindowManager.LayoutParams.FLAG_FULLSCREEN);
             }
            view.setFitsSystemWindows(true);
            gr.setBackgroundColor(Color.BLACK);
            for(int i = 0;i < gr.getChildCount();i++){
                gr.getChildAt(i).setVisibility(View.GONE);
            }
           this.callback = callback;
           isVideoFullscreen = true;
            gr.addView(view);
    }
    @Override @SuppressWarnings("deprecation")
    public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) // Available in API level 14+, deprecated in API level 18+
    {
        onShowCustomView(view, callback);
    }

    @Override
    public void onHideCustomView()
    {
        callback.onCustomViewHidden();
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN){
            ACTIVITY.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ACTIVITY.getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        ViewGroup gr = (ViewGroup) mWeb.getParent().getParent().getParent();
        gr.setBackgroundColor(Color.WHITE);
        for(int i = 0;i < gr.getChildCount();i++){
            gr.getChildAt(i).setVisibility(View.VISIBLE);
        }
        gr.removeView(v);
        isVideoFullscreen = false;
        PBar.setVisibility(View.GONE);

    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          Intent intent = fileChooserParams.createIntent();
          ACTIVITY.startActivity(intent);
          return true;
      }
      else{
          return  false;
      }
    }

    public boolean onBackPressed()
    {
        if (isVideoFullscreen)
        {
            onHideCustomView();
            return true;
        }
        else
        {
            return false;
        }
    }

}