package com.cookiegames.smartcookie;

import android.content.Context;
import android.net.ConnectivityManager;


import java.net.InetAddress;

public class CheckForInternet {
    public static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

}
