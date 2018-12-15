/*
*
 */
package com.cookiegames.smartcookie.utils;


import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.webkit.WebView;
import com.cookiegames.smartcookie.database.DbItem;
import com.cookiegames.smartcookie.database.HistoryDatabase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class HystoryTask extends AsyncTask<Void,Void,Void> {
    private Context cont;
    private HistoryDatabase mDataBase;
    private WebView mWebView;
    private String mFilePath;
    private static final String HEADING_1 = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta content=\"en-us\" http-equiv=\"Content-Language\" /><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\"><title>";
    private static final String HEADING_2 = "</title></head><style>body { background: #E5E5E5; padding-top: 5px;}" +
                       ".box { vertical-align:middle;position:relative; display: block; margin: 6px;padding-left:14px;padding-right:14px;padding-top:9px;padding-bottom:9px; background-color:#fff;border: 1px solid #d2d2d2;border-top-width: 0;border-bottom-width: 2px;font-family: Arial;color: #444;font-size: 12px;-moz-border-radius: 2px;-webkit-border-radius: 2px;border-radius: 2px;}" +
                        ".box a { width: 100%; height: 100%; position: absolute; left: 0; top: 0;}" +
                        ".black {color: black;font-size: 15px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}" +
                        ".font {color: gray;font-size: 10px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}" +
                        "</style><body><div id=\"content\">";
    private static final String PART1 = "<div class=\"box\"><a href=\"";

    private static final String PART2 = "\"></a><p class=\"black\">";

    private static final String PART3 = "</p><p class=\"font\">";

    private static final String PART4 = "</p></div></div>";

    private static final String END = "</div></body></html>";
    public HystoryTask(Context context,WebView view){
        cont = context;
        mDataBase = new HistoryDatabase(cont);
        mWebView = view;
    }
    @Override
    protected Void doInBackground(Void... params) {
        mFilePath = getHistoryPage();
        return null;
    }
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (mWebView != null && mFilePath != null) {
            mWebView.loadUrl(mFilePath);
        }
    }
    @NonNull
    private String getHistoryPage() {
        StringBuilder historyBuilder = new StringBuilder(HEADING_1 + "History" + HEADING_2);
        List<DbItem> historyList = mDataBase.getHistory();
        Iterator<DbItem> it = historyList.iterator();
        DbItem helper;
        while (it.hasNext()) {
            helper = it.next();
            historyBuilder.append(PART1);
            historyBuilder.append(helper.getUrl());
            historyBuilder.append(PART2);
            historyBuilder.append(helper.getTitle());
            historyBuilder.append(PART3);
            historyBuilder.append(helper.getUrl());
            historyBuilder.append(PART4);
        }

        historyBuilder.append(END);
        File historyWebPage = new File(cont.getFilesDir(), "behe.html");
        FileWriter historyWriter = null;
        try {
            historyWriter = new FileWriter(historyWebPage, false);
            historyWriter.write(historyBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
          try {
              historyWriter.close();
          }
          catch(Exception e){

          }
        }
        return "file://" + historyWebPage;
    }

}
