/*
 * Copyright 2014 A.C.R. Development
 */
package com.cookiegames.smartcookie.settings.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.cookiegames.smartcookie.BrowserApp;
import com.cookiegames.smartcookie.BuildConfig;
import com.cookiegames.smartcookie.R;
import com.cookiegames.smartcookie.constant.Constants;
import com.cookiegames.smartcookie.dialog.BrowserDialog;
import com.cookiegames.smartcookie.search.SearchEngineProvider;
import com.cookiegames.smartcookie.search.engine.BaseSearchEngine;
import com.cookiegames.smartcookie.search.engine.CustomSearch;
import com.cookiegames.smartcookie.utils.FileUtils;
import com.cookiegames.smartcookie.utils.ProxyUtils;
import com.cookiegames.smartcookie.utils.ThemeUtils;
import com.cookiegames.smartcookie.utils.Utils;

import java.util.List;

import javax.inject.Inject;

import static android.content.Context.MODE_PRIVATE;
import static com.cookiegames.smartcookie.preference.PreferenceManager.Suggestion;

public class ParentalControlSettingsFragment extends LightningPreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String SETTINGS_PASSWORD = "password";
    private static final String SETTINGS_FLASH = "cb_flash";
    private static final String SETTINGS_ADS = "cb_ads";
    private static final String SETTINGS_IMAGES = "cb_images";
    private static final String SETTINGS_SITES = "siteblock";
    private static final String SETTINGS_JAVASCRIPT = "cb_javascript";
    private static final String SETTINGS_COLORMODE = "cb_colormode";
    private static final String SETTINGS_DOWNLOAD = "download";
    private static final String SETTINGS_HOME = "home";
    private static final String SETTINGS_SEARCHENGINE = "search";
    private static final String SETTINGS_SUGGESTIONS = "suggestions_choice";


    private Activity mActivity;
    private static final int API = Build.VERSION.SDK_INT;
    private CharSequence[] mPasswordChoices;
    private Preference password, blockedsites;
    private int mSiteBlockChoice, mPasswordChoice;
    private int mBlockChoice;

    SharedPreferences prefs = null;

    @Inject
    SearchEngineProvider mSearchEngineProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_parents);

        BrowserApp.getAppComponent().inject(this);

        mActivity = getActivity();

        initPrefs();

        prefs = mActivity.getSharedPreferences("com.cookiegames.smartcookie", MODE_PRIVATE);

        if (prefs.getBoolean("noPassword", true)) {
        } else {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("password", prefs.getString("password", ""));
            editor.commit();
            String text = prefs.getString("password", "");
            StringBuilder builder = new StringBuilder(text);
            builder.deleteCharAt(0);
            builder.deleteCharAt(text.length() - 2);
            String removed = builder.toString();
            String str = removed.replaceAll("[a-zA-Z1-9]", "?");
            password.setSummary(text.charAt(0) + str + text.charAt(text.length() - 1));

            passwordDialog();
        }
    }
//FIX AFTER CLICKING ON BLOCK SITES, DIALOG REENTERS NONE
    private void passwordDialog() {
        BrowserDialog.showHiddenEditText(mActivity,
                R.string.enter_password,
                R.string.enter_password,
                "",
                R.string.action_ok,
                new BrowserDialog.EditorListener() {
                    @Override
                    public void onClick(String text) {
                        if (text.equals(prefs.getString("password", ""))) {
                        } else {
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(mActivity, getResources().getString(R.string.wrong_password), duration);
                            toast.show();
                            passwordDialog();
                        }

                    }
                });
    }

    private void initPrefs() {
        password = findPreference(SETTINGS_PASSWORD);
        // useragent = findPreference(SETTINGS_USERAGENT);
        blockedsites = findPreference(SETTINGS_SITES);

        password.setOnPreferenceClickListener(this);
        //useragent.setOnPreferenceClickListener(this);
        blockedsites.setOnPreferenceClickListener(this);

        // mAgentChoice = mPreferenceManager.getUserAgentChoice();
        mBlockChoice = mPreferenceManager.getSiteBlockChoice();
        mPasswordChoice = mPreferenceManager.getPasswordChoice();
        mPasswordChoices = getResources().getStringArray(R.array.password_set_array);

        //BaseSearchEngine currentSearchEngine = mSearchEngineProvider.getCurrentSearchEngine();
        //setSearchEngineSummary(currentSearchEngine);

        switch (mPasswordChoice) {
            case 1:
                password.setSummary(getResources().getString(R.string.none));
                break;
            case 2:
                password.setSummary(getResources().getString(R.string.enter_password));
                break;
        }

        switch (mBlockChoice) {
            case 1:
                blockedsites.setSummary(getResources().getString(R.string.none));
                break;
            case 2:
                blockedsites.setSummary(getResources().getString(R.string.block_all_sites));
                break;
            case 3:
                blockedsites.setSummary(getResources().getString(R.string.only_allow_sites));
                break;
        }
    }

    private void passwordChoicePicker() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(R.string.enter_password);
        picker.setSingleChoiceItems(mPasswordChoices, mPreferenceManager.getPasswordChoice(),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setPasswordChoice(which);
                    }
                });
        picker.setPositiveButton(R.string.action_ok, null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void setPasswordChoice(@Constants.Password int choice) {
        switch (choice) {
            case Constants.CUSTOM_PASSWORD:
                manualPasswordPicker();
                mPreferenceManager.setPasswordChoice(1);
                break;
            case Constants.NO_PASSWORD:
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("noPassword", true);
                editor.commit();
                password.setSummary("");
                mPreferenceManager.setPasswordChoice(2);
                break;
        }
    }

    private void manualPasswordPicker() {
        BrowserDialog.showHiddenEditText(mActivity,
                R.string.enter_password,
                R.string.enter_password,
                mPreferenceManager.getSiteBlockString(""),
                R.string.action_ok,
                new BrowserDialog.EditorListener() {
                    @Override
                    public void onClick(String text) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("password", text);
                        editor.commit();
                        StringBuilder builder = new StringBuilder(text);
                        builder.deleteCharAt(0);
                        builder.deleteCharAt(text.length() - 2);
                        String removed = builder.toString();
                        String str = removed.replaceAll("[a-zA-Z1-9]", "?");
                        password.setSummary(text.charAt(0) + str + text.charAt(text.length() - 1));
                        editor.putBoolean("noPassword", false);
                        editor.commit();
                    }
                });
    }
    
    private void blockDialog() {
        AlertDialog.Builder blockPicker = new AlertDialog.Builder(mActivity);
        blockPicker.setTitle(getResources().getString(R.string.block_sites));
        mSiteBlockChoice = mPreferenceManager.getSiteBlockChoice();
        blockPicker.setSingleChoiceItems(R.array.blocked_sites, mPreferenceManager.getSiteBlockChoice() - 1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPreferenceManager.setSiteBlockChoice(which + 1);
                        switch (which) {
                            case 0:
                                blockedsites.setSummary(getResources().getString(R.string.none));
                                break;
                            case 1:
                                blockedsites.setSummary(getResources().getString(R.string.block_all_sites));
                                blockPicker();
                                break;
                            case 2:
                                blockedsites.setSummary(getResources().getString(R.string.only_allow_sites));
                                blockPicker();
                                break;
                        }
                    }
                });
        blockPicker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = blockPicker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void blockPicker() {

        BrowserDialog.showEditText(mActivity,
                R.string.block_sites_title,
                R.string.block_info,
                mPreferenceManager.getSiteBlockString(""),
                R.string.action_ok,
                new BrowserDialog.EditorListener() {
                    @Override
                    public void onClick(String text) {
                        mPreferenceManager.setSiteBlockString(text);
                    }
                });
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            case SETTINGS_PASSWORD:
                passwordChoicePicker();
                return true;
            case SETTINGS_SITES:
                blockDialog();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        boolean checked = false;
        if (newValue instanceof Boolean) {
            checked = Boolean.TRUE.equals(newValue);
        }
        switch (preference.getKey()) {
            case SETTINGS_ADS:
                mPreferenceManager.setAdBlockEnabled(checked);
                return true;
            case SETTINGS_IMAGES:
                mPreferenceManager.setBlockImagesEnabled(checked);
                return true;
            case SETTINGS_JAVASCRIPT:
                mPreferenceManager.setJavaScriptEnabled(checked);
                return true;
            case SETTINGS_COLORMODE:
                mPreferenceManager.setColorModeEnabled(checked);
                return true;
            default:
                return false;
        }
    }
}
