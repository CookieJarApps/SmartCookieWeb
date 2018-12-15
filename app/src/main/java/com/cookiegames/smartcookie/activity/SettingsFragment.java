package com.cookiegames.smartcookie.activity;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cookiegames.smartcookie.R;
import com.cookiegames.smartcookie.utils.FileChooser;
import com.cookiegames.smartcookie.utils.PreferenceHelper;
import com.cookiegames.smartcookie.utils.ThemeUtils;

import java.io.File;
import java.io.FileOutputStream;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        final Preference pr1 = findPreference("look");
        pr1.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getPreferenceScreen().removeAll();
                addPreferencesFromResource(R.xml.settings2);
                getActivity().setTitle(pr1.getTitle());
                Preference pref = findPreference("change");
                pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK,
                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        photoPickerIntent.setType("image/*");
                        photoPickerIntent.putExtra("crop", "true");
                        photoPickerIntent.putExtra("return-data", true);
                        photoPickerIntent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
                        startActivityForResult(photoPickerIntent, 1);
                        return true;
                    }
                });
                PreferenceHelper.setIsLookScreen(true);
                return true;
            }
        });
        final Preference pr2 = findPreference("second");
        pr2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getPreferenceScreen().removeAll();
                addPreferencesFromResource(R.xml.settings3);
                getActivity().setTitle(pr2.getTitle());
                final Preference prf = findPreference("home_page");
                prf.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o.toString().equals("custom")) {
                            final Context context = getActivity();
                            LayoutInflater li = LayoutInflater.from(context);
                            View promptsView = li.inflate(R.layout.promt, null);
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                                    context);
                            alertDialogBuilder.setView(promptsView);
                            final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
                            alertDialogBuilder
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.ok,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString("home_page", userInput.getText().toString()).commit();
                                                }
                                            });
                            alertDialogBuilder.setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();
                        } else {
                            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString("home_page", "default").commit();
                            prf.setDefaultValue("default");
                        }
                        return true;
                    }
                });
                Preference pr = findPreference("export");
                pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        FileChooser chos = new FileChooser();
                        chos.showDirectoryDialog(getActivity());
                        return true;
                    }
                });
                Preference pf = findPreference("import");
                pf.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        FileChooser chos = new FileChooser();
                        chos.showFileDialog(getActivity());
                        return true;
                    }
                });
                PreferenceHelper.setIsBrowserScreen(true);
                return true;
            }
        });
        Preference credits = findPreference("credits");
        credits.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog mCreditsDialog;
                final TextView mCreditsTitle = new TextView(getActivity());
                final TextView mCreditsText = new TextView(getActivity());
                final ScrollView mScrollView = new ScrollView(getActivity());
                mCreditsText.setText(R.string.about_credits_content);
                mCreditsText.setTextSize(18);
                mCreditsText.setTypeface(Typeface.DEFAULT_BOLD);
                mCreditsTitle.setText(getResources().getString(R.string.credits));
                mCreditsTitle.setTypeface(Typeface.DEFAULT_BOLD);
                mCreditsTitle.setTextSize(20);
                mCreditsTitle.setGravity(Gravity.CENTER_HORIZONTAL);
                mScrollView.addView(mCreditsText);
                mCreditsText.setGravity(Gravity.CENTER_HORIZONTAL);
               if(ThemeUtils.isBlack()){
                   mCreditsDialog = new AlertDialog.Builder(getActivity(),R.style.blackDialogTheme)
                           .setCustomTitle(mCreditsTitle)
                           .setPositiveButton(android.R.string.ok, null)
                           .setView(mScrollView)
                           .show();
               }
               else {
                   mCreditsDialog = new AlertDialog.Builder(getActivity())
                           .setCustomTitle(mCreditsTitle)
                           .setPositiveButton(android.R.string.ok, null)
                           .setView(mScrollView)
                           .show();
               }
                return true;
            }
        });
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
                if (data != null) {
                 try {
                     Bundle extras = data.getExtras();
                     final Bitmap selectedBitmap = extras.getParcelable("data");
                     new Thread(new Runnable() {
                         @Override
                         public void run() {
                             FileOutputStream writer = null;
                             File image = new File(getActivity().getFilesDir(), "drawer_image.png");
                             try {
                                 writer = new FileOutputStream(image, false);
                                 selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, writer);
                                 writer.flush();
                                 writer.close();
                             } catch (Exception e) {
                             }
                         }
                     }).start();
                 }
                 catch(Exception e){
                     Log.d("ERROR",e.toString());
                 }
                }
            }
    }

    }

