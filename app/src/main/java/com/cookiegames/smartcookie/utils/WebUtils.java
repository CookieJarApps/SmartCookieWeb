package com.cookiegames.smartcookie.utils;

import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;

import com.cookiegames.smartcookie.database.history.HistoryRepository;

import java.io.File;

import io.reactivex.Scheduler;

import static android.os.Build.VERSION_CODES.N;

/**
 * Copyright 8/4/2015 Anthony Restaino
 */
public final class WebUtils {

    private WebUtils() {}

    public static void clearCookies(@NonNull Context context) {
        CookieManager c = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            c.removeAllCookies(null);
        } else {
            CookieSyncManager.createInstance(context);
            c.removeAllCookie();
        }
    }

    public static void clearWebStorage() {
        WebStorage.getInstance().deleteAllData();
    }
    public static void eraseWebStorage(@NonNull Context context) {
        WebStorage.getInstance().deleteAllData();

        if(Build.VERSION.SDK_INT > N){
           deleteData(context);
        }
    }

    @RequiresApi(N)
    public static void deleteData(@NonNull Context context) {
        try {
            File dir = new File(context.getDataDir()  + "/app_webview");
            deleteDir(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearHistory(@NonNull Context context,
                                    @NonNull HistoryRepository historyRepository,
                                    @NonNull Scheduler databaseScheduler) {
        historyRepository.deleteHistory()
            .subscribeOn(databaseScheduler)
            .subscribe();
        WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(context);
        webViewDatabase.clearFormData();
        webViewDatabase.clearHttpAuthUsernamePassword();
        Utils.trimCache(context);
    }

    public static void clearCache(@Nullable WebView view, @NonNull Context context) {
        if (view == null) return;
        view.clearCache(true);
        deleteCache(context);
    }

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) { e.printStackTrace();}
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }



}
