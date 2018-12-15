package com.cookiegames.smartcookie.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.cookiegames.smartcookie.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

/**
 * Created by todo on 06.02.2017.
 */
public class FileChooser   {
    ListView list;
    AlertDialog.Builder builder;
    ArrayList<String> mArrayList = new ArrayList<>();
    ArrayAdapter<String> adt;
    String path = Environment.getExternalStorageDirectory().getAbsolutePath();
    String dirPath;
    FilenameFilter filter;
    Dialog dialog;
    private static String[] mFileList;
    private  File mPath = new File(path);

    private static final int DIALOG_LOAD_FILE = 1000;

    private  void loadDirList() {
        try {
            mPath.mkdirs();
        } catch (SecurityException e) {

        }
        if (mPath.exists()) {
            filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return sel.isDirectory();
                }

            };
            mFileList = mPath.list(filter);
        } else {
            mFileList = new String[0];
        }
    }
    private  void loadFileList() {
        try {
            mPath.mkdirs();
        } catch (SecurityException e) {

        }
        if (mPath.exists()) {
            filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return true;
                }

            };
            mFileList = mPath.list(filter);
        } else {
            mFileList = new String[0];
        }
    }
    public void showDirectoryDialog(final Context context) {
        loadDirList();
        for (String s : mFileList) {
            mArrayList.add(s);
        }
        adt = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, mArrayList);
        list = new ListView(context);
        list.setAdapter(adt);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                   File file = new File(path);
                  if(mArrayList.get(i) == "..."){
                      path = file.getParent();
                      if(path == null){
                      }
                      else {
                          mPath = new File(path);
                          loadDirList();
                          mArrayList.clear();
                          for (String s : mFileList) {
                              mArrayList.add(s);
                          }
                          mArrayList.add(0, "...");
                          adt.notifyDataSetChanged();
                          dirPath = path;
                      }
                  }
                  else {
                      path += "/" + mArrayList.get(i);
                      file = new File(path);
                      if (file.isDirectory()) {
                          mFileList = file.list(filter);
                          mArrayList.clear();
                          mArrayList.add(0, "...");
                          for (String s : mFileList) {
                              mArrayList.add(s);
                          }
                          adt.notifyDataSetChanged();
                      }
                  }

                }
        });
        Dialog dialog = null;
        builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.choose));
        if (mFileList == null) {
            dialog = builder.create();
        }
        builder.setView(list);
        builder.setPositiveButton(context.getString(R.string.select), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dirPath = path;
                File fil = new File(dirPath,"behe-explorer.backup");
                ExportUtils.writeToFile(fil,builder.getContext());
            }
        });
        builder.show();
        adt.notifyDataSetChanged();
    }
    public void showFileDialog(final Context context) {
        loadFileList();
        for (String s : mFileList) {
            mArrayList.add(s);
        }

        builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.choose));
        adt = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, mArrayList);
        list = new ListView(context);
        list.setAdapter(adt);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                File file = new File(path);
                if(mArrayList.get(i) == "..."){
                    path = file.getParent();
                    if(path == null){
                    }
                    else {
                        mPath = new File(path);
                        loadFileList();
                        mArrayList.clear();
                        for (String s : mFileList) {
                            mArrayList.add(s);
                        }
                        mArrayList.add(0, "...");
                        adt.notifyDataSetChanged();
                        dirPath = path;
                    }
                }
                else {
                    path += "/" + mArrayList.get(i);
                    file = new File(path);
                    if (file.isDirectory()) {
                        mFileList = file.list();
                        mArrayList.clear();
                        mArrayList.add(0, "...");
                        for (String s : mFileList) {
                            mArrayList.add(s);
                        }
                        adt.notifyDataSetChanged();
                    }
                    else{
                        File fil = new File(path);
                        ExportUtils.readFromFile(fil,builder.getContext());
                        dialog.dismiss();
                    }
                }

            }
        });

        builder.setView(list);
        dialog = builder.create();
        dialog.show();
        adt.notifyDataSetChanged();
    }

}
