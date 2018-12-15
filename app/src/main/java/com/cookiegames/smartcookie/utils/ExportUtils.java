package com.cookiegames.smartcookie.utils;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.cookiegames.smartcookie.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

/**
 * Created by todo on 06.02.2017.
 */
public class ExportUtils {
  private static String[] mFileList;
  private static File mPath = new File(Environment.getExternalStorageDirectory() + "/");
  private static String mChosenFile;
  private static final String FTYPE = ".txt";
  private static final int DIALOG_LOAD_FILE = 1000;
  private static String THEME_KEY   = "theme";
  private static String LOCK_KEY = "lock";
  private static String TEXT_KEY = "text";
  private static String SEARCH_KEY = "search";
  private static String JAVA_KEY = "java";
  private static String PLUGIN_KEY = "plugins";
  private static String CACHE_KEY = "cache";
  private static String START_KEY = "start";
  private static String BOOKMARK_KEY = "book";
  public static void writeToFile(File file, Context context){
      PreferenceUtils utils = new PreferenceUtils(context);
      HashMap<String,Object> map = new HashMap<String,Object>();
      map.put(THEME_KEY,utils.getTheme());
      map.put(LOCK_KEY,utils.getLockDrawer());
      map.put(TEXT_KEY,utils.getTextSize());
      map.put(SEARCH_KEY,utils.getSearchEngine());
      map.put(JAVA_KEY,utils.getJavaEnabled());
      map.put(PLUGIN_KEY,utils.getPluginsEnabled());
      map.put(CACHE_KEY,utils.getCacheEnabled());
      map.put(START_KEY,utils.getHomePage());
      map.put(BOOKMARK_KEY,readBookmarks(context));
      try {
          ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
          outputStream.writeObject(map);
          outputStream.flush();
          outputStream.close();
          Toast.makeText(context,context.getString(R.string.succes_save),Toast.LENGTH_LONG).show();
      }
      catch(Exception e){
          Toast.makeText(context,context.getString(R.string.error),Toast.LENGTH_LONG).show();
      }

  }
  public static void readFromFile(File file,Context context){
       try {
          ObjectInputStream inp = new ObjectInputStream(new FileInputStream(file));
          Object obj = inp.readObject();
          inp.close();
          HashMap<String,Object> map = (HashMap<String,Object>) obj;
          PreferenceUtils utils = new PreferenceUtils(context);
          utils.setTheme(map.get(THEME_KEY).toString());
          utils.setTextSize(map.get(TEXT_KEY).toString());
          utils.setSearchEngine(map.get(SEARCH_KEY).toString());
          utils.setPluginsEnabled((boolean)map.get(PLUGIN_KEY));
          utils.setJavaEnabled((boolean)map.get(JAVA_KEY));
          utils.setCacheEnabled((boolean)map.get(CACHE_KEY));
          utils.setHomePage(map.get(START_KEY).toString());
          utils.setLockDrawer((boolean)map.get(LOCK_KEY));
          writeBookmarks(context,map);
          Toast.makeText(context,context.getString(R.string.succes_import),Toast.LENGTH_LONG).show();
       }
       catch (Exception e){
           Toast.makeText(context,context.getString(R.string.error),Toast.LENGTH_LONG).show();
       }
  }
  private static void writeBookmarks(Context context,HashMap<String,Object> value){
      File toWrite = new File(context.getFilesDir(), "bookmarks.oi");
    try {
        HashMap<String,String> map = (HashMap<String,String>) value.get (BOOKMARK_KEY);
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(toWrite));
        oos.writeObject(map);
        oos.flush();
        oos.close();
    }
    catch(Exception e){

    }
  }
    private static HashMap readBookmarks(Context context) {
       HashMap<String,String > map = new HashMap<>();
        try {
            File toRead = new File(context.getFilesDir(), "bookmarks.oi");
            if (toRead.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(toRead));
                Object obj = ois.readObject();
                ois.close();
                ois.close();
                HashMap<String, String> mHash = (HashMap<String, String>) obj;
                map = mHash;
            }
        } catch (Exception ee) {
        }
      return map;
    }

}
