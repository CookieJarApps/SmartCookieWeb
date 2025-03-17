package com.cookiegames.smartcookie.utils;

import android.app.Activity;
import android.util.Log;

import net.i2p.android.ui.I2PAndroidHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.cookiegames.smartcookie.BrowserApp;
import com.cookiegames.smartcookie.R;
import com.cookiegames.smartcookie.browser.ProxyChoice;
import com.cookiegames.smartcookie.extensions.ActivityExtensions;
import com.cookiegames.smartcookie.preference.DeveloperPreferences;
import com.cookiegames.smartcookie.preference.UserPreferences;
import androidx.annotation.NonNull;

import info.guardianproject.netcipher.webkit.WebkitProxy;

@Singleton
public final class ProxyUtils {

    private static final String TAG = "ProxyUtils";

    // Helper
    private static boolean sI2PHelperBound;
    private static boolean sI2PProxyInitialized;

    private final UserPreferences userPreferences;
    private final DeveloperPreferences developerPreferences;
    private final I2PAndroidHelper i2PAndroidHelper;

    @Inject
    public ProxyUtils(UserPreferences userPreferences,
                      DeveloperPreferences developerPreferences,
                      I2PAndroidHelper i2PAndroidHelper) {
        this.userPreferences = userPreferences;
        this.developerPreferences = developerPreferences;
        this.i2PAndroidHelper = i2PAndroidHelper;
    }

    /*
     * If Orbot/Tor or I2P is installed, prompt the user if they want to enable
     * proxying for this session
     */
    public void checkForProxy(@NonNull final Activity activity) {

    }

    /*
     * Initialize WebKit Proxying
     */
    private void initializeProxy(@NonNull Activity activity) {
        String host;
        int port;

        switch (userPreferences.getProxyChoice()) {
            case NONE:
                // We shouldn't be here
                return;
            case ORBOT:
                host = "localhost";
                port = 8118;
                break;
            case I2P:
                sI2PProxyInitialized = true;
                if (sI2PHelperBound && !i2PAndroidHelper.isI2PAndroidRunning()) {
                    i2PAndroidHelper.requestI2PAndroidStart(activity);
                }
                host = "localhost";
                port = 4444;
                break;
            default:
            case MANUAL:
                host = userPreferences.getProxyHost();
                port = userPreferences.getProxyPort();
                break;
        }

        try {
        } catch (Exception e) {
            Log.d(TAG, "error enabling web proxying", e);
        }

        try {
            WebkitProxy.setProxy(BrowserApp.class.getName(), activity.getApplicationContext(), null, host, port);
        } catch (Exception e) {
            Log.d(TAG, "error enabling web proxying", e);
        }
    }

    public boolean isProxyReady(@NonNull Activity activity) {
        if (userPreferences.getProxyChoice() == ProxyChoice.I2P) {
            if (!i2PAndroidHelper.isI2PAndroidRunning()) {
                ActivityExtensions.snackbar(activity, R.string.i2p_not_running);
                return false;
            } else if (!i2PAndroidHelper.areTunnelsActive()) {
                ActivityExtensions.snackbar(activity, R.string.i2p_tunnels_not_ready);
                return false;
            }
        }

        return true;
    }

    public void updateProxySettings(@NonNull Activity activity) {
        if (userPreferences.getProxyChoice() != ProxyChoice.NONE) {
            initializeProxy(activity);
        } else {
            try {
                WebkitProxy.resetProxy(BrowserApp.class.getName(), activity.getApplicationContext());
            } catch (Exception e) {
                Log.e(TAG, "Unable to reset proxy", e);
            }

            sI2PProxyInitialized = false;
        }
    }

    public void onStop() {
        i2PAndroidHelper.unbind();
        sI2PHelperBound = false;
    }

    public void onStart(final Activity activity) {
        if (userPreferences.getProxyChoice() == ProxyChoice.I2P) {
            // Try to bind to I2P Android
            i2PAndroidHelper.bind(() -> {
                sI2PHelperBound = true;
                if (sI2PProxyInitialized && !i2PAndroidHelper.isI2PAndroidRunning())
                    i2PAndroidHelper.requestI2PAndroidStart(activity);
            });
        }
    }

    public static ProxyChoice sanitizeProxyChoice(ProxyChoice choice, @NonNull Activity activity) {
        switch (choice) {
            case ORBOT:
                break;
            case I2P:
                I2PAndroidHelper ih = new I2PAndroidHelper(activity.getApplication());
                if (!ih.isI2PAndroidInstalled()) {
                    choice = ProxyChoice.NONE;
                    ih.promptToInstall(activity);
                }
                break;
            case MANUAL:
                break;
        }
        return choice;
    }
}