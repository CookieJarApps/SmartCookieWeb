package com.cookiegames.smartcookie;

import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import android.util.Log;
import android.webkit.WebView;

import com.anthonycr.bonsai.Schedulers;

import java.util.List;

import javax.inject.Inject;

import com.cookiegames.smartcookie.database.HistoryItem;
import com.cookiegames.smartcookie.database.bookmark.BookmarkExporter;
import com.cookiegames.smartcookie.database.bookmark.BookmarkModel;
import com.cookiegames.smartcookie.database.bookmark.legacy.LegacyBookmarkManager;
import com.cookiegames.smartcookie.di.AppComponent;
import com.cookiegames.smartcookie.di.AppModule;
import com.cookiegames.smartcookie.di.DaggerAppComponent;
import com.cookiegames.smartcookie.preference.PreferenceManager;
import com.cookiegames.smartcookie.utils.FileUtils;
import com.cookiegames.smartcookie.utils.MemoryLeakUtils;
import com.cookiegames.smartcookie.utils.Preconditions;

public class BrowserApp extends Application {

    private static final String TAG = "BrowserApp";

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT);
    }

    @Nullable private static AppComponent sAppComponent;

    @Inject PreferenceManager mPreferenceManager;
    @Inject BookmarkModel mBookmarkModel;

    public static BrowserApp instance;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        }

        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, @NonNull Throwable ex) {

                if (BuildConfig.DEBUG) {
                    FileUtils.writeCrashToStorage(ex);
                }

                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, ex);
                } else {
                    System.exit(2);
                }
            }
        });

        sAppComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
        sAppComponent.inject(this);

        Schedulers.worker().execute(new Runnable() {
            @Override
            public void run() {
                List<HistoryItem> oldBookmarks = LegacyBookmarkManager.destructiveGetBookmarks(BrowserApp.this);

                if (!oldBookmarks.isEmpty()) {
                    // If there are old bookmarks, import them
                    mBookmarkModel.addBookmarkList(oldBookmarks).subscribeOn(Schedulers.io()).subscribe();
                } else if (mBookmarkModel.count() == 0) {
                    // If the database is empty, fill it from the assets list
                    List<HistoryItem> assetsBookmarks = BookmarkExporter.importBookmarksFromAssets(BrowserApp.this);
                    mBookmarkModel.addBookmarkList(assetsBookmarks).subscribeOn(Schedulers.io()).subscribe();
                }
            }
        });

        if (!isRelease() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        registerActivityLifecycleCallbacks(new MemoryLeakUtils.LifecycleAdapter() {
            @Override
            public void onActivityDestroyed(Activity activity) {
                Log.d(TAG, "Cleaning up after the Android framework");
                MemoryLeakUtils.clearNextServedView(activity, BrowserApp.this);
            }
        });
    }

    @NonNull
    public static AppComponent getAppComponent() {
        Preconditions.checkNonNull(sAppComponent);
        return sAppComponent;
    }

    /**
     * Determines whether this is a release build.
     *
     * @return true if this is a release build, false otherwise.
     */
    public static boolean isRelease() {
        return !BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.toLowerCase().equals("release");
    }

    public static void copyToClipboard(@NonNull Context context, @NonNull String string) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("URL", string);
        clipboard.setPrimaryClip(clip);
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }
    public static BrowserApp getInstance() {
        return instance;
    }
}
