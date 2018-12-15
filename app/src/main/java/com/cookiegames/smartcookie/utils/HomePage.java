package com.cookiegames.smartcookie.utils;

/**
 * Created by vlad on 22.11.2016.
 */
import android.app.Application;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.WebView;
import com.cookiegames.smartcookie.R;
import com.cookiegames.smartcookie.controllers.TabManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class HomePage extends AsyncTask<Void, Void, Void> {

    public static final String FILENAME = "homepage.html";

    private static final String HEAD_1 = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\">"
            + "<head>"
            + "<meta content=\"en-us\" http-equiv=\"Content-Language\" />"
            + "<meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">"
            + "<title>";

    private static final String HEAD_2 = "</title>"
            + "</head>"
            + "<style>body{background:#f4f5f7;text-align:center;margin:0px;}#search_input{height:35px; "
            + "width:100%;outline:none;border:none;font-size: 16px;background-color:transparent;}"
            + "span { display: block; overflow: hidden; padding-left:5px;vertical-align:middle;}"
            + ".search_bar{display:table;vertical-align:middle;width:90%;height:35px;max-width:500px;margin:0 auto;background-color:#993333;box-shadow: 0px 2px 3px rgba( 0, 0, 0, 0.25 );"
            + "font-family: Arial;color: #444;-moz-border-radius: 2px;-webkit-border-radius: 2px;border-radius: 2px;}"
            + "#search_submit{outline:none;height:37px;float:right;color:#404040;font-size:16px;font-weight:bold;border:none;"
            + "background-color:transparent;}.outer { display: table; position: absolute; height: 100%; width: 100%;}"
            + ".middle { display: table-cell; vertical-align: middle;}.inner { margin-left: auto; margin-right: auto; "
            + "margin-bottom:10%; width: 100%;}img.smaller{width:50%;max-width:300px;}"
            + ".box { vertical-align:middle;position:relative; display: block; margin: 10px;padding-left:10px;padding-right:10px;padding-top:5px;padding-bottom:5px;"
            + " background-color:#bcc5db;box-shadow: 0px 3px rgba( 0, 0, 0, 0.1 );font-family: Arial;color: #444;"
            + "font-size: 12px;-moz-border-radius: 2px;-webkit-border-radius: 2px;"
            + "border-radius: 2px;}</style><body> <div class=\"outer\"><div class=\"middle\"><div class=\"inner\"><img class=\"smaller\" src=\"";

    private static final String MIDDLE = "\" ></br></br><form onsubmit=\"return search()\" class=\"search_bar\" autocomplete=\"off\">"
            + "<input type=\"submit\" id=\"search_submit\" value=\"";
    private static  String MIDDLE_TWO = "\" ><span><input class=\"search\" type=\"text\" value=\"\" id=\"search_input\" >"
            + "</span></form></br></br></div></div></div><script type=\"text/javascript\">function search(){if(document.getElementById(\"search_input\").value != \"\"){window.location.href = \"";

    private static final String END = "\" + document.getElementById(\"search_input\").value;document.getElementById(\"search_input\").value = \"\";}return false;}</script></body></html>";

    @NonNull
    private final String mTitle;
    @NonNull
    private final Application mApp;
    private String mStartpageUrl;
    private WebView mBeHeView;

    public HomePage(WebView tab, @NonNull Application app) {
        mTitle = app.getString(R.string.home);
        mApp = app;
        mBeHeView = tab;
    }

    @Nullable
    @Override
    protected Void doInBackground(Void... params) {
        mStartpageUrl = getHomepage();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (mBeHeView != null) {
            mBeHeView.loadUrl(mStartpageUrl);
        }
    }

    /**
     * This method builds the homepage and returns the local URL to be loaded
     * when it finishes building.
     *
     * @return the URL to load
     */
    @NonNull
    private String getHomepage() {
        File homepage = new File(mApp.getApplicationContext().getFilesDir(), FILENAME);
            StringBuilder homepageBuilder = new StringBuilder(HEAD_1 + mTitle + HEAD_2);
            String icon = "file:///android_asset/google.png";
            String searchUrl = TabManager.getSearchEngine(mApp.getApplicationContext());
            String searchString = mApp.getString(R.string.go);
            homepageBuilder.append(icon);
            homepageBuilder.append(MIDDLE);
            homepageBuilder.append(searchString);
            homepageBuilder.append(MIDDLE_TWO);
            homepageBuilder.append(searchUrl);
            homepageBuilder.append(END);
            FileWriter hWriter = null;
            try {
                hWriter = new FileWriter(homepage, false);
                hWriter.write(homepageBuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    hWriter.close();
                } catch (Exception e) {

                }
            }
            return "file://" + homepage;
        }
    }