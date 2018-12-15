package com.cookiegames.smartcookie.utils;

/**
 * Created by todo on 24.01.2017.
 */
public class PreferenceHelper {
    private static boolean isLook = false;
    private static boolean isBrowser = false;
    public static void setIsLookScreen(boolean b){
        isLook = b;
        isBrowser = false;
    }
    public static void setIsBrowserScreen(boolean b){
        isBrowser = b;
        isLook = false;
    }
    public static boolean getIsLook(){
        return isLook;
    }
    public static boolean getIsBrowser(){
        return isBrowser;
    }
}
