/*
 Copyright 2016 Vlad Todosin
*/

package com.cookiegames.smartcookie.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.ArrayList;

public class HistoryDatabase extends SQLiteOpenHelper{
    @Nullable SQLiteDatabase liteDatabase;
    public HistoryDatabase(Context context){
        super(context,Names.DATABASE_NAME,null,1);
        liteDatabase = openIfNeeded();
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Names.SQL_CREATE_ENTRIES);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Names.DATABASE_NAME);
        onCreate(db);
    }
    public void clearAllItems(){
        liteDatabase = openIfNeeded();
        liteDatabase.delete(Names.TABLE_NAME,null, null);
        liteDatabase.close();
        liteDatabase = this.getWritableDatabase();
    }
    public SQLiteDatabase openIfNeeded(){
        if(liteDatabase == null || !liteDatabase.isOpen()){
          liteDatabase = this.getWritableDatabase();
        }
        return liteDatabase;
    }
    public void addItem(@NonNull DbItem db){
        liteDatabase = openIfNeeded();
        ContentValues values = new ContentValues();
        values.put(Names.KEY_TITLE,db.getTitle());
        values.put(Names.KEY_URL,db.getUrl());
        values.put(Names.KEY_TIME, System.currentTimeMillis());
        liteDatabase.insert(Names.TABLE_NAME, null, values);
        liteDatabase.close();
    }
    public ArrayList<DbItem> getHistory(){
        liteDatabase = openIfNeeded();
        ArrayList<DbItem> list = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + Names.TABLE_NAME + " ORDER BY " + Names.KEY_TIME
                + " DESC";
        Cursor mCursor = liteDatabase.rawQuery(selectQuery,null);
        if(mCursor.moveToFirst()){
            do{
              DbItem dbItem = new DbItem();
              dbItem.setTitle(mCursor.getString(1));
              dbItem.setUrl(mCursor.getString(2));
              list.add(dbItem);
            } while (mCursor.moveToNext());

        }
        mCursor.close();
        return list;
    }
    public static abstract class Names{
    public Names(){}
    public static String TABLE_NAME = "history";
    public static String  KEY_ID =  "id";
    public static String  KEY_URL =  "url";
    public static String  KEY_TIME =  "time";
    public static String KEY_TITLE =  "title";
    private static final String DATABASE_NAME = "historyDB";
    public static final String SQL_CREATE_ENTRIES =
                       "CREATE TABLE " + Names.TABLE_NAME + " (" +
                        Names.KEY_ID + " INTEGER PRIMARY KEY," +
                        Names.KEY_TITLE + " TEXT,"  +
                        Names.KEY_URL + " TEXT," + Names.KEY_TIME + " INTEGER" + ")";
}
}
