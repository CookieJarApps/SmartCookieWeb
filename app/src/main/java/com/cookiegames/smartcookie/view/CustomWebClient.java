/*
 Copyright 2016 Vlad Todosin
*/
package com.cookiegames.smartcookie.view;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.webkit.*;
import android.widget.EditText;
import android.widget.TextView;

import com.cookiegames.smartcookie.R;
import com.cookiegames.smartcookie.database.DbItem;
import com.cookiegames.smartcookie.database.HistoryDatabase;
import com.cookiegames.smartcookie.utils.AdBlocker;
import com.cookiegames.smartcookie.utils.PreferenceUtils;

import java.util.HashMap;
import java.util.Map;

public class CustomWebClient extends WebViewClient {
    private EditText TEXT;
    private Map<String, Boolean> loadedUrls = new HashMap<>();
    Activity act;
    CustomView mainView;
    boolean priv;
    public CustomWebClient(EditText textView, Activity ac, boolean privat, CustomView view){
      super();
      TEXT = textView;
      act = ac;
      priv = privat;
      mainView = view;
    }


    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
       if(mainView.isCurrentTab()) {
           String str;
        try{
           if (view.getUrl().contains("https://")) {
               str = view.getUrl().toString().replace("https://", "<font color='#228B22'>https://</font>");
               TEXT.setText(Html.fromHtml(str), TextView.BufferType.SPANNABLE);
               TEXT.clearFocus();
           } else {
               if (!view.getUrl().contains("file")) {
                   TEXT.setText(view.getUrl());
               }
           }
       }
       catch(Exception e){}
     }
    }

    @Override
    public void onPageFinished(WebView view,String url) {
        if (mainView.isCurrentTab()) {
            String str;
            if (mainView.isPrivate()) {
                mainView.clearCache(true);
                WebStorage storage = WebStorage.getInstance();
                storage.deleteAllData();
            } else {
                DbItem dbItem = new DbItem(url, view.getTitle());
                HistoryDatabase db = new HistoryDatabase(act);
                db.addItem(dbItem);
                mainView.clearCache(true);
            }
            try {
             if(!TEXT.isFocused()) {
                 if (view.getUrl().contains("https://")) {
                     str = view.getUrl().toString().replace("https://", "<font color='#228B22'>https://</font>");
                     TEXT.setText(Html.fromHtml(str), TextView.BufferType.SPANNABLE);
                     TEXT.clearFocus();
                 } else {
                     if (!view.getUrl().contains("file")) {
                         TEXT.setText(view.getUrl());
                     }
                 }
             }
            } catch (Exception e) {
            }
            view.clearCache(true);
        }
    }
    @Override
    public void onFormResubmission(WebView view, @NonNull final Message dontResend, @NonNull final Message resend) {
        Activity mActivity = mainView.getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Form resubmission?");
        builder.setMessage("Do you want to resubmit?")
                .setCancelable(true)
                .setPositiveButton(mActivity.getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                resend.sendToTarget();
                            }
                        })
                .setNegativeButton(mActivity.getString(android.R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dontResend.sendToTarget();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      if(url.startsWith("http") || url.startsWith("file")){
          return false;
      }
      else{
          Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
          mainView.getActivity().startActivity(Intent.createChooser(intent,mainView.getActivity().getResources().getString(R.string.share)));
          return true;
      }
    }
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        AdBlocker.init(act);
        PreferenceUtils utils = new PreferenceUtils(act);
        if(utils.getAdBlock()) {
            boolean ad;
            if (!loadedUrls.containsKey(url)) {
                ad = AdBlocker.isAd(url);
                loadedUrls.put(url, ad);
            } else {
                ad = loadedUrls.get(url);
            }
            return ad ? AdBlocker.createEmptyResource() :
                    super.shouldInterceptRequest(view, url);
        }
        else {
            return super.shouldInterceptRequest(view,url);
        }
    }
}


