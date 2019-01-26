/*
 Copyright 2016 Vlad Todosin
*/
package com.cookiegames.smartcookie.activity;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;

import com.cookiegames.smartcookie.adapters.BookAdapter;
import com.cookiegames.smartcookie.controllers.TabManager;
import com.cookiegames.smartcookie.database.HistoryDatabase;
import com.cookiegames.smartcookie.utils.PreferenceUtils;
import com.cookiegames.smartcookie.utils.ThemeUtils;
import com.cookiegames.smartcookie.view.AnimatedProgressBar;
import com.cookiegames.smartcookie.view.CustomView;
import com.cookiegames.smartcookie.R;


@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {
	private boolean _doubleBackToExitPressedOnce = false;
	public Context mContext = this;
	AppCompatActivity activity;
	SwitchCompat desktop;
	SwitchCompat privat;
	public DrawerLayout mDrawerLayout;
	ActionBarDrawerToggle mDrawerToggle;
	EditText txt;
	Toolbar bar;
	Button btn;
	NavigationView navView;
	NavigationView tabView;
	AnimatedProgressBar pBar;
	MenuItem m1;
	MenuItem m2;
	CustomView web;
	RelativeLayout swipe;
	TextView txe;
	RelativeLayout root;
	Uri data;
	ArrayList<String> m = new ArrayList<>();
	ArrayList<String> u = new ArrayList<>();
	GridView mGrid;
	PreferenceUtils preferenceUtils;
	private static final float[] NEGATIVE = {
			-1.0f, 0, 0, 0, 255, // red
			0, -1.0f, 0, 0, 255, // green
			0, 0, -1.0f, 0, 255, // blue
			0, 0, 0, 1.0f, 0  // alpha
	};


	@Override
	public void onCreate(Bundle savedInstanceState) {
		ThemeUtils ut = new ThemeUtils(this);
		ut.setTheme();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Context context = getApplicationContext();
            CharSequence text = "This permission is required for downloads.";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                1);
                    }
                });

            }
        }

		initialize();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		setTitle("");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		m1 = menu.findItem(R.id.action_home);
		m2 = menu.findItem(R.id.action_book);
		m1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				CustomView view = TabManager.getCurrentTab();
				view.loadHomepage();
				return false;
			}
		});
        if(ThemeUtils.isBlack()){
			m2.getIcon().setColorFilter(new PorterDuffColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN));
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_book:
				final Context context = this;
				LayoutInflater li = LayoutInflater.from(context);
				View promptsView = li.inflate(R.layout.promt, null);
				AlertDialog.Builder alertDialogBuilder;
				if(ThemeUtils.isBlack()) {
					 alertDialogBuilder = new AlertDialog.Builder(context, R.style.blackDialogTheme);
				}
				else{
					 alertDialogBuilder = new AlertDialog.Builder(context);
				}
				alertDialogBuilder.setView(promptsView);
				final EditText userInput = (EditText) promptsView
						.findViewById(R.id.editTextDialogUserInput);
				try {
					userInput.setText(web.getTitle());
				} catch (Exception e) {
					userInput.setText("Web Page");
				}
				alertDialogBuilder
						.setCancelable(false)
						.setPositiveButton(R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										HashMap<String, String> map = new HashMap<>();
										CustomView view = TabManager.getCurrentTab();
										try {
											String result = userInput.getText().toString();
											File toWrite = new File(getApplicationContext().getFilesDir(), "bookmarks.oi");
											if (toWrite.exists()) {
												ObjectInputStream ois = new ObjectInputStream(new FileInputStream(toWrite));
												Object obj = ois.readObject();
												ois.close();
												HashMap<String, String> mHash = (HashMap<String, String>) obj;
												map.putAll(mHash);
												map.put(result, view.getUrl());

											} else {
												map.put(result, view.getUrl());
											}
											ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(toWrite));
											oos.writeObject(map);
											oos.flush();
											oos.close();

											Snackbar.make(root, getResources().getString(R.string.action_added), Snackbar.LENGTH_LONG)
													.setAction(getResources().getString(R.string.action_see), new View.OnClickListener() {
														@Override
														public void onClick(View view) {
															showBookMarks();
														}
													})
													.show();
										} catch (Exception ee) {

										}
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


				break;
		}

		return super.onOptionsItemSelected(item);
	}

	public void initialize() {
		activity = this;
		bar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(bar);
		txt = (EditText) findViewById(R.id.edit);
		swipe = new RelativeLayout(this);
		pBar = (AnimatedProgressBar) findViewById(R.id.progressBar);
		btn = (Button) findViewById(R.id.voice);
		txe = new TextView(this);
		root = (RelativeLayout) findViewById(R.id.root);
		navView = (NavigationView) findViewById(R.id.left_navigation);
		desktop = (SwitchCompat) navView.getMenu().getItem(8).getActionView();
		privat = (SwitchCompat) navView.getMenu().getItem(9).getActionView();
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerElevation(20);
		mGrid = (GridView) findViewById(R.id.gridview);
		preferenceUtils = new PreferenceUtils(this);
		mDrawerToggle = new ActionBarDrawerToggle(this,
				mDrawerLayout, bar,
				R.string.drawer_open,
				R.string.drawer_close) {
			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
			}

			@Override
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				if (view == findViewById(R.id.right_navigation) && preferenceUtils.getLockDrawer()) {
					mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
				} else {
					if (view == findViewById(R.id.right_navigation)) {
						mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED, GravityCompat.END);
					}
				}
			}


		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		PreferenceUtils utils = new PreferenceUtils(getApplicationContext());
		if (preferenceUtils.getLockDrawer()) {
			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
		} else {
			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED, GravityCompat.END);
		}
		if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("cookie",true)){
                 TabManager.setCookie(true);
        }
		else{
			TabManager.setCookie(false);
		}
        ActionBar mBar = getSupportActionBar();
		mBar.setDisplayHomeAsUpEnabled(true);
		initializeBeHeView();
        data = getIntent().getData();
		mDrawerToggle.syncState();
		tabView = (NavigationView) findViewById(R.id.right_navigation);
		TabManager.setNavigationView(tabView);
		tabView.setItemIconTintList(null);
		FloatingActionButton addTab = (FloatingActionButton) findViewById(R.id.add_ta);
		addTab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				CustomView behe = new CustomView(getApplicationContext(), activity, pBar, false, txt);
				behe.loadHomepage();
				TabManager.addTab(behe);
				TabManager.setCurrentTab(behe);
				TabManager.updateTabView();
				refreshTab();
			}
		});
		FloatingActionButton delTab = (FloatingActionButton) findViewById(R.id.remove_tab);
		delTab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int size = TabManager.getList().size();
				if (size > 1) {
					CustomView tab = TabManager.getCurrentTab();
					CustomView main = TabManager.getList().get(0);
					TabManager.setCurrentTab(main);
					TabManager.removeTab(tab);
					TabManager.updateTabView();
					refreshTab();
				}
			}
		});
		tabView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(MenuItem item) {
				List<MenuItem> items = new ArrayList<>();
				Menu menu = tabView.getMenu();
				for (int i = 0; i < menu.size(); i++) {
					items.add(menu.getItem(i));
				}
				for (MenuItem itm : items) {
					itm.setChecked(false);
				}
				item.setChecked(true);
				CustomView view = TabManager.getTabAtPosition(item);
				TabManager.setCurrentTab(view);
				refreshTab();
				if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("tab_close",true)){
					mDrawerLayout.closeDrawer(GravityCompat.END);
				}
				return false;
			}
		});
		File image = new File(getFilesDir(), "drawer_image.png");
		ImageView img1 = new ImageView(this);
		ImageView img2 = new ImageView(this);
		img1.setScaleType(ImageView.ScaleType.CENTER_CROP);
		img2.setScaleType(ImageView.ScaleType.CENTER_CROP);
		if (!image.exists()) {
			img1.setImageResource(R.drawable.hed);
			img2.setImageResource(R.drawable.hed);
		} else {
			Bitmap bit = BitmapFactory.decodeFile(image.getPath());
			img1.setImageBitmap(bit);
			img2.setImageBitmap(bit);
		}
		img1.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getWindowManager().getDefaultDisplay().getHeight() / 4));
		img2.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getWindowManager().getDefaultDisplay().getHeight() / 4));
		for (int i = 0; i < navView.getHeaderCount(); i++) {
			navView.removeHeaderView(navView.getHeaderView(i));
		}
		for (int i = 0; i < tabView.getHeaderCount(); i++) {
			tabView.removeHeaderView(tabView.getHeaderView(i));
		}
		tabView.addHeaderView(img1);
		navView.addHeaderView(img2);
		navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

			// This method will trigger on item Click of nav_text menu
			@Override
			public boolean onNavigationItemSelected(MenuItem menuItem) {

				mDrawerLayout.closeDrawers();
				switch (menuItem.getItemId()) {
					case R.id.inbox:
						showBookMarks();
						return true;
					case R.id.search:
						hideBookMarks();
						btn = (Button) findViewById(R.id.voice);
						btn.setVisibility(View.VISIBLE);
						return true;
					case R.id.sett:
						menuItem.setChecked(false);
						Intent ine = new Intent(getApplicationContext(), SettingsActivity.class);
						ine.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(ine);
						return true;
					case R.id.history:
						TabManager.getCurrentTab().loadHistory();
						return true;
					case R.id.desktop:
						return true;
					case R.id.privat:
						return true;
					case R.id.tabs:
						mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED, GravityCompat.END);
						mDrawerLayout.openDrawer(GravityCompat.END);
						return true;
					case R.id.clear_history:
						menuItem.setChecked(false);
						HistoryDatabase db = new HistoryDatabase(getApplicationContext());
						db.clearAllItems();
						TabManager.deleteAllHistory();
						WebStorage storage = WebStorage.getInstance();
						storage.deleteAllData();
						CookieManager.getInstance().removeAllCookie();
						Snackbar.make(root, getResources().getString(R.string.historytast), Snackbar.LENGTH_LONG)
								.setAction(getResources().getString(R.string.action_see), new View.OnClickListener() {
									@Override
									public void onClick(View view) {
										TabManager.getCurrentTab().loadHistory();
									}
								})
								.show();
						break;
					case R.id.credit:
						LayoutInflater li = LayoutInflater.from(mContext);
						View promptsView = li.inflate(R.layout.promt, null);
						TextView v1 = (TextView) promptsView.findViewById(R.id.textView1);
						v1.setText(getString(R.string.find));
						AlertDialog.Builder alertDialogBuilder;
						if(ThemeUtils.isBlack()) {
							alertDialogBuilder = new AlertDialog.Builder(mContext, R.style.blackDialogTheme);
						}
						else{
							 alertDialogBuilder = new AlertDialog.Builder(mContext);
						}
						alertDialogBuilder.setView(promptsView);
						final EditText userInput = (EditText) promptsView
								.findViewById(R.id.editTextDialogUserInput);
						alertDialogBuilder
								.setCancelable(false)
								.setPositiveButton(R.string.ok,
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
												TabManager.getCurrentTab().findInPage(userInput.getText().toString());
												m1.setIcon(R.drawable.ic_cancel_black_24dp);
												m1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
													@Override
													public boolean onMenuItemClick(MenuItem menuItem) {
														TabManager.getCurrentTab().findInPage("");
														m1.setIcon(R.drawable.ic_home_black_24dp);
														m1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
															@Override
															public boolean onMenuItemClick(MenuItem menuItem) {
																TabManager.getCurrentTab().loadHomepage();
																return false;
															}
														});
														return true;
													}
												});
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
						break;
					case R.id.read:
						web.startReaderMode();
					break;
				}


				return true;
			}
		});

		txt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean b) {
				if (view.isFocused()) {
					txt.setCursorVisible(true);
					m1.setIcon(R.drawable.ic_cancel_black_24dp);
					m1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem menuItem) {
							txt.setText("");
							return false;
						}
					});
					m2.setVisible(false);
				} else {
					txt.setCursorVisible(true);
					m1.setIcon(R.drawable.ic_home_black_24dp);
					txt.setSelection(0);
					m1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem menuItem) {
							TabManager.getCurrentTab().loadHomepage();
							return false;
						}
					});
					m2.setVisible(true);
				    txt.setText(web.getUrl());
				}
			}
		});
		txt.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				txt.setCursorVisible(true);
				if (actionId == EditorInfo.IME_ACTION_GO) {
					String toSearch;
					toSearch = txt.getText().toString();
					if (toSearch.contains("http://") || toSearch.contains("https://")) {
						web.loadUrl(toSearch);
					} else {
						if (toSearch.contains("www")) {
							web.loadUrl("http://" + toSearch);
						} else {
							if (toSearch.contains(".")) {
								web.loadUrl("http://" + toSearch);
							} else {
								web.searchWeb(toSearch);
							}
						}
					}

					View view = getCurrentFocus();
					if (view != null) {
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
					}
					txt.setCursorVisible(false);
					return true;
				} else {
					txt.setCursorVisible(false);
					View view = getCurrentFocus();
					if (view != null) {
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
					}
					return true;
				}

			}
		});

		desktop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				CustomView behe = TabManager.getCurrentTab();
				if (b) {
					behe.setDesktop();
				} else {
					behe.setMobile();
				}
			}
		});
		privat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				CustomView behe = TabManager.getCurrentTab();
				behe.setPrivate(b);
				if(!b) {
					TypedValue typedValue = new TypedValue();
					getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
					getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(typedValue.coerceToString().toString())));
				}
			}
		});
		getSupportActionBar().setHomeAsUpIndicator(getResources().getDrawable(R.drawable.ic_reorder_black_24dp));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		}
		if (ThemeUtils.isBlack()) {
			Field[] drawablesFields = com.cookiegames.smartcookie.R.drawable.class.getFields();
			ArrayList<Drawable> drawables = new ArrayList<>();

			for (Field field : drawablesFields) {
				try {
					drawables.add(getResources().getDrawable(field.getInt(null)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			for (Drawable dr : drawables) {
				dr.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN));
			}

		}
		else{
			Field[] drawablesFields = com.cookiegames.smartcookie.R.drawable.class.getFields();
			ArrayList<Drawable> drawables = new ArrayList<>();

			for (Field field : drawablesFields) {
				try {
					drawables.add(getResources().getDrawable(field.getInt(null)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			for (Drawable dr : drawables) {
				dr.setColorFilter(null);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mDrawerLayout.invalidate();
		TabManager.stopPlayback();
	}

	@Override
	public void onResume() {
		super.onResume();
		initialize();
		TabManager.resetAll(this, pBar, privat.isChecked(), txt);
		if (data != null) {
			web.loadUrl(data.toString());
		}
		else{
			txt.setText(web.getUrl());
		}
		if (data == null && web.getUrl() == null) {
			web.loadHomepage();
		}
		TabManager.resume();
		TabManager.updateTabView();
		try {
			if (web.getUrl().contains("file")) {
				txt.setText(Html.fromHtml("<font color='#228B22'>" + getResources().getString(R.string.home) + "</font>"), TextView.BufferType.SPANNABLE);
			}
			pBar.setVisibility(View.GONE);
		} catch (Exception e) {
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	public void voice(View v) {
		TabManager.getCurrentTab().reload();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				if (data != null) {
					List<String> results = data.getStringArrayListExtra(
							RecognizerIntent.EXTRA_RESULTS);
					String spokenText = results.get(0);
					txt.setText(spokenText);
				}
			}
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
		super.onCreateContextMenu(menu, v, info);
		final WebView.HitTestResult result = web.getHitTestResult();

		MenuItem.OnMenuItemClickListener handler = new MenuItem.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				final String url = result.getExtra();
				switch (item.getItemId()) {
					case 1:
						String name = URLUtil.guessFileName(url, "", "");
						DownloadManager.Request request = new DownloadManager.Request(
								Uri.parse(url));
						request.allowScanningByMediaScanner();
						request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
						request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
						DownloadManager dm = (DownloadManager) getSystemService(Activity.DOWNLOAD_SERVICE);
						dm.enqueue(request);
						break;
					case 2:
						web.loadUrl(url);
						break;
					case 3:
						ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText("", url);
						clipboard.setPrimaryClip(clip);
						break;

				}
				return true;
			}
		};

		if (result.getType() == WebView.HitTestResult.IMAGE_TYPE ||
				result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

			menu.setHeaderTitle(result.getExtra());
			menu.add(0, 1, 0, getString(R.string.download_picture)).setOnMenuItemClickListener(handler);
			menu.add(0, 2, 0, getString(R.string.see_picture)).setOnMenuItemClickListener(handler);
		} else if (result.getType() == WebView.HitTestResult.ANCHOR_TYPE ||
				result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {

			menu.setHeaderTitle(result.getExtra());
			menu.add(0, 3, 0, getString(R.string.save_link)).setOnMenuItemClickListener(handler);

		}
	}

	@Override
	public void onBackPressed() {
    if(!web.isFull()) {

	if (mGrid.getVisibility() == View.VISIBLE) {
		hideBookMarks();
	} else {

		if (!TabManager.getCurrentTab().canGoBack()) {
			if (_doubleBackToExitPressedOnce) {
				super.onBackPressed();
				this.finish();
			} else {
				this._doubleBackToExitPressedOnce = true;
				Toast.makeText(this, getResources().getString(R.string.press_to_quit), Toast.LENGTH_SHORT).show();
			}
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					_doubleBackToExitPressedOnce = false;
				}
			}, 2000);
		} else {
			TabManager.getCurrentTab().goBack();
		}
	}
    }
}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}
	public void initializeBeHeView() {
		List<CustomView> list = TabManager.getList();
		if (list != null && list.isEmpty()) {
			web = new CustomView(getApplicationContext(), this, pBar, false, txt);
			TabManager.addTab(web);
			TabManager.setCurrentTab(web);
		} else {
			web = TabManager.getCurrentTab();
			ViewGroup parent = (ViewGroup) web.getParent();
			if (parent != null) {
				parent.removeAllViews();
			}
		}
		ViewGroup group = (ViewGroup) web.getParent();
		if (group != null) {
			group.removeAllViews();
		}
		web.setLayoutParams(new SwipeRefreshLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		web.setIsCurrentTab(true);
		TabManager.setCurrentTab(web);
		web = TabManager.getCurrentTab();
		swipe.addView(web);
		swipe.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
		root.addView(swipe);
	}

	public void refreshTab() {
		web = TabManager.getCurrentTab();
		web.setLayoutParams(new SwipeRefreshLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		swipe = new RelativeLayout(this);
		ViewGroup group = (ViewGroup) web.getParent();
		if (group != null) {
			group.removeAllViews();
		}
		swipe.addView(web);
		for (int i = 0; i < root.getChildCount(); i++) {
			if (root.getChildAt(i) instanceof GridView) {
			} else {
				View view = root.getChildAt(i);
				root.removeView(view);
			}
		}
		root.addView(swipe);
		if (web.getUrl() == null) {
			txt.setText(Html.fromHtml("<font color='#228B22'>" + getResources().getString(R.string.home) + "</font>"), TextView.BufferType.SPANNABLE);
		} else {
			txt.setText(web.getUrl());
		}

	}

	public void readBookmarks() {
		try {

			File toRead = new File(getApplicationContext().getFilesDir(), "bookmarks.oi");
			if (toRead.exists()) {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(toRead));
				Object obj = ois.readObject();
				ois.close();
				ois.close();
				HashMap<String, String> mHash = (HashMap<String, String>) obj;
				for (String title : mHash.keySet()) {
					if (!m.contains(title)) {
						m.add(title);
					}
				}
				for (String url : mHash.values()) {
					if (!u.contains(url)) {
						u.add(url);
					}
				}
			}
		} catch (Exception ee) {

		}
	}

	public void showBookMarks() {
		readBookmarks();
		final BookAdapter adt = new BookAdapter(mContext, m, u);
		mGrid.setAdapter(adt);
		adt.notifyDataSetChanged();
		mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				try {
					CustomView behe = TabManager.getCurrentTab();
					behe.loadUrl(u.get(i));
					hideBookMarks();
				} catch (Exception e) {

				}
			}
		});
		mGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {

				Log.d("INT", String.valueOf(i));
				new android.app.AlertDialog.Builder(mContext)
						.setTitle(getResources().getString(R.string.delete_dialog_title))
						.setMessage(getResources().getString(R.string.delete_dialog_content))
						.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								File toRead = new File(mContext.getFilesDir(), "bookmarks.oi");
								HashMap<String, String> mHash = new HashMap<>();
								try {
									if (toRead.exists()) {
										ObjectInputStream ois = new ObjectInputStream(new FileInputStream(toRead));
										Object obj = ois.readObject();
										ois.close();
										ois.close();
										mHash = (HashMap<String, String>) obj;
										String toRemove = m.get(i);
										mHash.remove(toRemove);
										m.clear();
										u.clear();
										adt.notifyDataSetChanged();
									}
								} catch (Exception ee) {
								}
								try {
									if (toRead.exists()) {
										ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(toRead));
										oos.writeObject(mHash);
										oos.flush();
										oos.close();
									} else {

									}
								} catch (Exception e) {

								}

								showBookMarks();
							}
						})
						.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						})
						.show();
				return true;
			}
		});
		txt.setText(getResources().getText(R.string.boomarks));
		CustomView view = TabManager.getCurrentTab();
		view.setIsCurrentTab(false);
		root.removeAllViews();
		mGrid.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
		mGrid.setVisibility(View.VISIBLE);
		root.addView(mGrid);
	}

	public void hideBookMarks() {
		if (mGrid != null) {
			CustomView view = TabManager.getCurrentTab();
			view.setIsCurrentTab(true);
			txt.setText(view.getUrl());
			mGrid.setVisibility(View.GONE);
			root.removeAllViews();
			swipe.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
			root.addView(swipe);
			if (web.getUrl().contains("file")) {
				txt.setText(Html.fromHtml("<font color='#228B22'>" + getResources().getString(R.string.home) + "</font>"), TextView.BufferType.SPANNABLE);
			}
			swipe.setVisibility(View.VISIBLE);
		}
	}

}

