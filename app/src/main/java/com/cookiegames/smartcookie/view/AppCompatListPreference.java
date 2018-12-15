package com.cookiegames.smartcookie.view;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.util.AttributeSet;

import java.lang.reflect.Method;

public class AppCompatListPreference extends ListPreference {

    private Context mContext;
    private AppCompatDialog mDialog;

    public AppCompatListPreference(Context context) {
        super(context);
        this.mContext = context;
    }

    public AppCompatListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    @Override
    public AppCompatDialog getDialog() {
        return mDialog;
    }

    @Override
    protected void showDialog(Bundle state) {
        if (getEntries() == null || getEntryValues() == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }

        int preselect = findIndexOfValue(getValue());
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(getDialogTitle())
                .setIcon(getDialogIcon())
                .setSingleChoiceItems(getEntries(), preselect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which >= 0 && getEntryValues() != null) {
                            String value = getEntryValues()[which].toString();
                            if (callChangeListener(value) && isPersistent())
                                setValue(value);
                        }
                        dialog.dismiss();
                    }
                });

        PreferenceManager pm = getPreferenceManager();
        try {
            Method method = pm.getClass().getDeclaredMethod(
                    "registerOnActivityDestroyListener",
                    PreferenceManager.OnActivityDestroyListener.class);
            method.setAccessible(true);
            method.invoke(pm, this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mDialog = builder.create();
        if (state != null)
            mDialog.onRestoreInstanceState(state);
        mDialog.show();
    }

    @Override
    public void onActivityDestroy() {
        super.onActivityDestroy();
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
    }
}