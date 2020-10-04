package com.cookiegames.smartcookie.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;

import com.cookiegames.smartcookie.database.history.HistoryRepository;

import java.io.File;
import java.util.Map;

import io.reactivex.Scheduler;

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
            //noinspection deprecation
            CookieSyncManager.createInstance(context);
            //noinspection deprecation
            c.removeAllCookie();
        }
    }

    public static void clearWebStorage() {
        WebStorage.getInstance().deleteAllData();
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
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
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
