/*
 Copyright 2016 Vlad Todosin
*/

package com.cookiegames.smartcookie.view;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebView;

import com.cookiegames.smartcookie.R;
import com.cookiegames.smartcookie.activity.MainActivity;


public class CustomDownloadListener implements android.webkit.DownloadListener {
   private WebView view;
    private AppCompatActivity mActivity;
    public CustomDownloadListener(AppCompatActivity activity, WebView web) {
        super();
        mActivity = activity;
        view = web;
    }
    public void onDownloadStart(final String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        final String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                            String downloadSize = null;
                                if (contentLength > 0) {
                                     downloadSize = Formatter.formatFileSize(mActivity, contentLength);
                                   } else {
                                       downloadSize = "";
                                    }
                        String message = mActivity.getResources().getString(R.string.download);
                        builder.setTitle(fileName)
                       .setMessage(message + downloadSize + " ?")
                       .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int which) {
                               DownloadManager.Request request = new DownloadManager.Request(
                                       Uri.parse(url));
                               request.allowScanningByMediaScanner();
                               request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                               request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                               DownloadManager dm = (DownloadManager) mActivity.getSystemService(Activity.DOWNLOAD_SERVICE);
                               dm.enqueue(request);
                           }
                       })

                      .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialog, int which) {

                          }
                      });
                builder.create();
                builder.show();

    }
}
