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

    private static final String SETTINGS_PROXY = "proxy";
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
    private CharSequence[] mProxyChoices;
    private Preference password, blockedsites, downloadloc, home, searchengine, searchsSuggestions;
    private String mDownloadLocation;
    private int mAgentChoice;
    private int mBlockChoice;
    private String mHomepage;

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
        password = findPreference(SETTINGS_PROXY);
        // useragent = findPreference(SETTINGS_USERAGENT);
        blockedsites = findPreference(SETTINGS_SITES);

        password.setOnPreferenceClickListener(this);
        //useragent.setOnPreferenceClickListener(this);
        blockedsites.setOnPreferenceClickListener(this);

        // mAgentChoice = mPreferenceManager.getUserAgentChoice();
        mBlockChoice = mPreferenceManager.getSiteBlockChoice();
        mHomepage = mPreferenceManager.getHomepage();
        mDownloadLocation = mPreferenceManager.getDownloadDirectory();
        mProxyChoices = getResources().getStringArray(R.array.proxy_choices_array);

        if (API >= Build.VERSION_CODES.KITKAT) {
            mPreferenceManager.setFlashSupport(0);
        }

        //BaseSearchEngine currentSearchEngine = mSearchEngineProvider.getCurrentSearchEngine();
        //setSearchEngineSummary(currentSearchEngine);

        /*switch (mAgentChoice) {
            case 1:
                useragent.setSummary(getResources().getString(R.string.agent_default));
                break;
            case 2:
                useragent.setSummary(getResources().getString(R.string.agent_desktop));
                break;
            case 3:
                useragent.setSummary(getResources().getString(R.string.agent_mobile));
                break;
            case 4:
                useragent.setSummary(getResources().getString(R.string.agent_custom));
        }*/

        switch (mBlockChoice) {
            case 1:
                blockedsites.setSummary(getResources().getString(R.string.none));
                break;
            case 2:
                blockedsites.setSummary(getResources().getString(R.string.agent_custom));
            case 3:
                blockedsites.setSummary("Only Allow Listed Sites");
        }

        int flashNum = mPreferenceManager.getFlashSupport();
        boolean imagesBool = mPreferenceManager.getBlockImagesEnabled();
        boolean enableJSBool = mPreferenceManager.getJavaScriptEnabled();
    }

    private void showUrlPicker(@NonNull final CustomSearch customSearch) {

        BrowserDialog.showEditText(mActivity,
                R.string.search_engine_custom,
                R.string.search_engine_custom,
                mPreferenceManager.getSearchUrl(),
                R.string.action_ok,
                new BrowserDialog.EditorListener() {
                    @Override
                    public void onClick(String text) {
                        mPreferenceManager.setSearchUrl(text);
                        setSearchEngineSummary(customSearch);
                    }
                });

    }

    private void getFlashChoice() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getResources().getString(R.string.title_flash));
        builder.setMessage(getResources().getString(R.string.flash))
                .setCancelable(true)
                .setPositiveButton(getResources().getString(R.string.action_manual),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                mPreferenceManager.setFlashSupport(1);
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.action_auto),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPreferenceManager.setFlashSupport(2);
                            }
                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mPreferenceManager.setFlashSupport(0);
            }

        });
        AlertDialog alert = builder.create();
        alert.show();
        BrowserDialog.setDialogSize(mActivity, alert);
    }

    private void proxyChoicePicker() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(R.string.http_proxy);
        picker.setSingleChoiceItems(mProxyChoices, mPreferenceManager.getProxyChoice(),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setProxyChoice(which);
                    }
                });
        picker.setPositiveButton(R.string.action_ok, null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void setProxyChoice(@Constants.Proxy int choice) {
        switch (choice) {
            case Constants.PROXY_MANUAL:
                manualProxyPicker();
                break;
            case Constants.NO_PROXY:
                break;
        }
    }

    private void manualProxyPicker() {
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
                    }
                });
    }

    @NonNull
    private CharSequence[] convertSearchEngineToString(@NonNull List<BaseSearchEngine> searchEngines) {
        CharSequence[] titles = new CharSequence[searchEngines.size()];

        for (int n = 0; n < searchEngines.size(); n++) {
            titles[n] = getString(searchEngines.get(n).getTitleRes());
        }

        return titles;
    }

    private void searchDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.title_search_engine));

        final List<BaseSearchEngine> searchEngineList = mSearchEngineProvider.getAllSearchEngines();

        CharSequence[] chars = convertSearchEngineToString(searchEngineList);

        int n = mPreferenceManager.getSearchChoice();

        picker.setSingleChoiceItems(chars, n, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                BaseSearchEngine searchEngine = searchEngineList.get(which);

                // Store the search engine preference
                int preferencesIndex = mSearchEngineProvider.mapSearchEngineToPreferenceIndex(searchEngine);
                mPreferenceManager.setSearchChoice(preferencesIndex);

                if (searchEngine instanceof CustomSearch) {
                    // Show the URL picker
                    showUrlPicker((CustomSearch) searchEngine);
                } else {
                    // Set the new search engine summary
                    setSearchEngineSummary(searchEngine);
                }
            }
        });
        picker.setPositiveButton(R.string.action_ok, null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void homepageDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(R.string.home);
        mHomepage = mPreferenceManager.getHomepage();
        int n;
        switch (mHomepage) {
            case Constants.SCHEME_HOMEPAGE:
                n = 0;
                break;
            case Constants.SCHEME_BLANK:
                n = 1;
                break;
            case Constants.SCHEME_BOOKMARKS:
                n = 2;
                break;
            default:
                n = 3;
                break;
        }

        picker.setSingleChoiceItems(R.array.homepage, n,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                mPreferenceManager.setHomepage(Constants.SCHEME_HOMEPAGE);
                                home.setSummary(getResources().getString(R.string.action_homepage));
                                break;
                            case 1:
                                mPreferenceManager.setHomepage(Constants.SCHEME_BLANK);
                                home.setSummary(getResources().getString(R.string.action_blank));
                                break;
                            case 2:
                                mPreferenceManager.setHomepage(Constants.SCHEME_BOOKMARKS);
                                home.setSummary(getResources().getString(R.string.action_bookmarks));
                                break;
                            case 3:
                                homePicker();
                                break;
                        }
                    }
                });
        picker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void suggestionsDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.search_suggestions));

        int currentChoice = 3;

        switch (mPreferenceManager.getSearchSuggestionChoice()) {
            case SUGGESTION_GOOGLE:
                currentChoice = 0;
                break;
            case SUGGESTION_DUCK:
                currentChoice = 1;
                break;
            case SUGGESTION_BAIDU:
                currentChoice = 2;
                break;
            case SUGGESTION_NONE:
                currentChoice = 3;
                break;
        }

        picker.setSingleChoiceItems(R.array.suggestions, currentChoice,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                mPreferenceManager.setSearchSuggestionChoice(Suggestion.SUGGESTION_GOOGLE);
                                searchsSuggestions.setSummary(R.string.powered_by_google);
                                break;
                            case 1:
                                mPreferenceManager.setSearchSuggestionChoice(Suggestion.SUGGESTION_DUCK);
                                searchsSuggestions.setSummary(R.string.powered_by_duck);
                                break;
                            case 2:
                                mPreferenceManager.setSearchSuggestionChoice(Suggestion.SUGGESTION_BAIDU);
                                searchsSuggestions.setSummary(R.string.powered_by_baidu);
                                break;
                            case 3:
                                mPreferenceManager.setSearchSuggestionChoice(Suggestion.SUGGESTION_NONE);
                                searchsSuggestions.setSummary(R.string.search_suggestions_off);
                                break;
                        }
                    }
                });
        picker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void homePicker() {
        String currentHomepage;
        mHomepage = mPreferenceManager.getHomepage();
        if (!URLUtil.isAboutUrl(mHomepage)) {
            currentHomepage = mHomepage;
        } else {
            currentHomepage = "https://www.google.com";
        }

        BrowserDialog.showEditText(mActivity,
                R.string.title_custom_homepage,
                R.string.title_custom_homepage,
                currentHomepage,
                R.string.action_ok,
                new BrowserDialog.EditorListener() {
                    @Override
                    public void onClick(String text) {
                        mPreferenceManager.setHomepage(text);
                        home.setSummary(text);
                    }
                });
    }

    private void downloadLocDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.title_download_location));
        mDownloadLocation = mPreferenceManager.getDownloadDirectory();
        int n;
        if (mDownloadLocation.contains(Environment.DIRECTORY_DOWNLOADS)) {
            n = 0;
        } else {
            n = 1;
        }

        picker.setSingleChoiceItems(R.array.download_folder, n,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                mPreferenceManager.setDownloadDirectory(FileUtils.DEFAULT_DOWNLOAD_PATH);
                                downloadloc.setSummary(FileUtils.DEFAULT_DOWNLOAD_PATH);
                                break;
                            case 1:
                                downPicker();
                                break;
                        }
                    }
                });
        picker.setPositiveButton(getResources().getString(R.string.action_ok), null);
        Dialog dialog = picker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void blockDialog() {
        AlertDialog.Builder blockPicker = new AlertDialog.Builder(mActivity);
        blockPicker.setTitle(getResources().getString(R.string.block_sites));
        mAgentChoice = mPreferenceManager.getSiteBlockChoice();
        blockPicker.setSingleChoiceItems(R.array.blocked_sites, mBlockChoice - 1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPreferenceManager.setSiteBlockChoice(which + 1);
                        switch (which) {
                            case 0:
                                blockedsites.setSummary(getResources().getString(R.string.none));
                                break;
                            case 1:
                                blockedsites.setSummary(getResources().getString(R.string.agent_custom));
                                blockPicker();
                                break;
                            case 2:
                                blockedsites.setSummary("Only Allow Listed Sites");
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
                        blockedsites.setSummary(mActivity.getString(R.string.agent_custom));
                    }
                });
    }


    private void downPicker() {

        View dialogView = LayoutInflater.from(mActivity).inflate(R.layout.dialog_edit_text, null);
        final EditText getDownload = dialogView.findViewById(R.id.dialog_edit_text);

        final int errorColor = ContextCompat.getColor(mActivity, R.color.error_red);
        final int regularColor = ThemeUtils.getTextColor(mActivity);
        getDownload.setTextColor(regularColor);
        getDownload.addTextChangedListener(new DownloadLocationTextWatcher(getDownload, errorColor, regularColor));
        getDownload.setText(mPreferenceManager.getDownloadDirectory());

        AlertDialog.Builder downLocationPicker = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.title_download_location)
                .setView(dialogView)
                .setPositiveButton(R.string.action_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String text = getDownload.getText().toString();
                                text = FileUtils.addNecessarySlashes(text);
                                mPreferenceManager.setDownloadDirectory(text);
                                downloadloc.setSummary(text);
                            }
                        });
        Dialog dialog = downLocationPicker.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private void setSearchEngineSummary(BaseSearchEngine baseSearchEngine) {
        if (baseSearchEngine instanceof CustomSearch) {
            searchengine.setSummary(mPreferenceManager.getSearchUrl());
        } else {
            searchengine.setSummary(getString(baseSearchEngine.getTitleRes()));
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            case SETTINGS_PROXY:
                proxyChoicePicker();
                return true;
            case SETTINGS_SITES:
                blockDialog();
                return true;
            case SETTINGS_DOWNLOAD:
                downloadLocDialog();
                return true;
            case SETTINGS_HOME:
                homepageDialog();
                return true;
            case SETTINGS_SEARCHENGINE:
                searchDialog();
                return true;
            case SETTINGS_SUGGESTIONS:
                suggestionsDialog();
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
            case SETTINGS_FLASH:
                if (!Utils.isFlashInstalled(mActivity) && checked) {
                    Utils.createInformativeDialog(mActivity, R.string.title_warning, R.string.dialog_adobe_not_installed);
                    mPreferenceManager.setFlashSupport(0);
                    return false;
                } else {
                    if (checked) {
                        getFlashChoice();
                    } else {
                        mPreferenceManager.setFlashSupport(0);
                    }
                }
                return true;
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

    private static class DownloadLocationTextWatcher implements TextWatcher {
        @NonNull
        private final EditText getDownload;
        private final int errorColor;
        private final int regularColor;

        public DownloadLocationTextWatcher(@NonNull EditText getDownload, int errorColor, int regularColor) {
            this.getDownload = getDownload;
            this.errorColor = errorColor;
            this.regularColor = regularColor;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(@NonNull Editable s) {
            if (!FileUtils.isWriteAccessAvailable(s.toString())) {
                this.getDownload.setTextColor(this.errorColor);
            } else {
                this.getDownload.setTextColor(this.regularColor);
            }
        }
    }
}
