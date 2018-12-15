/*
 Copyright 2016 Vlad Todosin
*/
package com.cookiegames.smartcookie.utils;

import android.content.Context;
import android.preference.PreferenceManager;
public class PreferenceUtils {
private static PreferenceManager MANAGER;
private static Context mContext;
   public PreferenceUtils(Context context){
   mContext = context;
    }
   public  int getSearchEngine(){
   String searchEngine;
   searchEngine = MANAGER.getDefaultSharedPreferences(mContext).getString("search","1");
   int e = Integer.parseInt(searchEngine);
   return e;
    }
    public  int getTextSize(){
      return Integer.parseInt(MANAGER.getDefaultSharedPreferences(mContext).getString("size","18"));
    }
    public  int getTheme(){
       try {
           String color = MANAGER.getDefaultSharedPreferences(mContext).getString("color", "1");
           return Integer.parseInt(color);
       }
       catch (Exception e){
           return 1;
       }
    }
    public  String getHomePage(){
       return MANAGER.getDefaultSharedPreferences(mContext).getString("home_page","default");
    }
    public  boolean getNightModeEnabled(){
        return MANAGER.getDefaultSharedPreferences(mContext).getBoolean("night",false);
    }
    public  boolean getJavaEnabled(){
    return MANAGER.getDefaultSharedPreferences(mContext).getBoolean("java", true);
    }

    public  boolean getPluginsEnabled(){
    return MANAGER.getDefaultSharedPreferences(mContext).getBoolean("plugins", true);
    }
    public  boolean getCacheEnabled(){
    return MANAGER.getDefaultSharedPreferences(mContext).getBoolean("cache", false);
    }
    public  boolean getLockDrawer(){
    return MANAGER.getDefaultSharedPreferences(mContext).getBoolean("lock",true);
    }
    public boolean getAdBlock(){
        return MANAGER.getDefaultSharedPreferences(mContext).getBoolean("ad",false);
    }
    public boolean getBlockImages(){
        return MANAGER.getDefaultSharedPreferences(mContext).getBoolean("img",false);
    }
    public boolean getEnableLocation(){
        return MANAGER.getDefaultSharedPreferences(mContext).getBoolean("loc",false);
    }
    public void setTextSize(String size){
        MANAGER.getDefaultSharedPreferences(mContext).edit().putString("size",size).commit();
    }
    public void setTheme(String theme){
        MANAGER.getDefaultSharedPreferences(mContext).edit().putString("color",theme).commit();
    }
    public void setSearchEngine(String searchEngine){
        MANAGER.getDefaultSharedPreferences(mContext).edit().putString("search",searchEngine).commit();
    }
    public void setHomePage(String homePage){
        MANAGER.getDefaultSharedPreferences(mContext).edit().putString("home_page",homePage).commit();
    }
    public void setJavaEnabled(boolean enabled){
        MANAGER.getDefaultSharedPreferences(mContext).edit().putBoolean("java",enabled).commit();
    }
    public void setPluginsEnabled(boolean enabled){
        MANAGER.getDefaultSharedPreferences(mContext).edit().putBoolean("plugins",enabled).commit();
    }
    public void setCacheEnabled(boolean enabled){
        MANAGER.getDefaultSharedPreferences(mContext).edit().putBoolean("cache",enabled).commit();
    }
    public void setLockDrawer(boolean lock){
        MANAGER.getDefaultSharedPreferences(mContext).edit().putBoolean("lock",lock).commit();
    }
}
