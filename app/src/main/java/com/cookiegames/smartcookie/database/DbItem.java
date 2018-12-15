
/**
 * Copyright 2016 Vlad Todosin
 */
package com.cookiegames.smartcookie.database;

public class DbItem {
    private String URL;
    private String TITLE;
    private int TIME;
    public  DbItem(){
        TIME = (int) System.currentTimeMillis();
    }
    public DbItem(String url,String title){
        URL = url;
        TITLE = title;
        TIME = (int)System.currentTimeMillis();
    }
   public String getUrl(){
      if(URL != null) {
          return URL;
      }
      else{
          return "about:blank";
      }
   }
   public String getTitle(){
       if(TITLE != null){
           return TITLE;
       }
       else{
           return "Web Page";
       }
   }
   public int getTime(){
       return TIME;
   }
   public void setUrl(String url){
       URL = url;
   }
   public void setTitle(String title){
       TITLE = title;
   }

}
