/*
 Copyright 2016 Vlad Todosin
*/


package com.cookiegames.smartcookie.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.cookiegames.smartcookie.R;
import com.cookiegames.smartcookie.activity.ReadingActivity;
import com.cookiegames.smartcookie.utils.HomePage;
import com.cookiegames.smartcookie.utils.HystoryTask;
import com.cookiegames.smartcookie.utils.PreferenceUtils;
import com.cookiegames.smartcookie.utils.ThemeUtils;
import java.util.Map;


@SuppressWarnings("unused")
 /*
 * This view acts as the engine of BeHe ExploreR
 */


public class BeHeView extends WebView{
	ThemeUtils theme;
	private boolean isPrivate;
	private String TEXT = "1";
	private int searchEngine = 1;
	private ProgressBar P_BAR;
	private boolean ico ;
	private boolean FOCUS;
	AppCompatActivity WEB_ACTIVITY;
	EditText text;
	private String PAGE_TITLE;
	public String found = "";
	public static int GOOGLE_SEARCH = 1;
	public static int BING_SEARCH = 2;
	public static int YAHOO_SEARCH = 3;
	public static int DUCKDUCKGO_SEARCH = 4;
	public static int ASK_SEARCH = 5;
	private Paint mPaint = new Paint();
	public static int WOW_SEARCH = 6;
	private static final int API = android.os.Build.VERSION.SDK_INT;
	private static final float[] sNegativeColorArray = {
			-1.0f, 0, 0, 0, 255, // red
			0, -1.0f, 0, 0, 255, // green
			0, 0, -1.0f, 0, 255, // blue
			0, 0, 0, 1.0f, 0 // alpha
	};

	public BeHeChromeClient chromeClient;
	/*
	* Public constructors of BeHeView
	 */
	public BeHeView(Context c,Activity activity){
		super(c);
		WEB_ACTIVITY = (AppCompatActivity) activity;
	}
	public BeHeView(Context context, AppCompatActivity activity, ProgressBar pBar, boolean Private, final EditText txt)  {
		super(activity);
		theme = new ThemeUtils(activity);
		isPrivate = Private;
		P_BAR = pBar;
		WEB_ACTIVITY = activity;
		text = txt;
		chromeClient = new BeHeChromeClient(P_BAR,this,WEB_ACTIVITY);
		setWebChromeClient(chromeClient);
	    setWebViewClient(new BeHeWebClient(text,WEB_ACTIVITY,false,this));
	    setDownloadListener(new CiobanDownloadListener(WEB_ACTIVITY, this));
        WEB_ACTIVITY.registerForContextMenu(this);
	    initializeSettings();

	}
	public void initializeSettings(){
		setDrawingCacheBackgroundColor(Color.WHITE);
		setFocusableInTouchMode(true);
		setFocusable(true);
		setDrawingCacheEnabled(false);
		setWillNotCacheDrawing(true);
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {

			setAlwaysDrawnWithCacheEnabled(false);
		}
		setBackgroundColor(Color.WHITE);
		setScrollbarFadingEnabled(true);
		setSaveEnabled(true);
		setNetworkAvailable(true);

		PreferenceUtils utils = new PreferenceUtils(WEB_ACTIVITY);
		WebSettings settings = getSettings();
		settings.setDisplayZoomControls(false);
		settings.setBuiltInZoomControls(true);
		settings.setSupportMultipleWindows(false);
		settings.setEnableSmoothTransition(true);
		if (API < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			//noinspection deprecation
			settings.setAppCacheMaxSize(Long.MAX_VALUE);
		}
		if (API < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			//noinspection deprecation
			settings.setEnableSmoothTransition(true);
		}
		if (API > Build.VERSION_CODES.JELLY_BEAN) {
			settings.setMediaPlaybackRequiresUserGesture(true);
		}
		if (API >= Build.VERSION_CODES.LOLLIPOP && !isPrivate) {
			settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
		} else if (API >= Build.VERSION_CODES.LOLLIPOP) {
			settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
		}
		if (isPrivate){
			settings.setDomStorageEnabled(false);
			settings.setAppCacheEnabled(false);
			settings.setDatabaseEnabled(false);
			settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		    settings.setGeolocationEnabled(false);
		    CookieManager.getInstance().setAcceptCookie(false);
		}
		else {
			settings.setDomStorageEnabled(true);
			settings.setAppCacheEnabled(true);
			settings.setCacheMode(WebSettings.LOAD_DEFAULT);
			settings.setDatabaseEnabled(true);
			theme.setTheme();
			if (utils.getPluginsEnabled()) {
			    settings.setPluginState(WebSettings.PluginState.ON);
			}
			else {
				settings.setPluginState(WebSettings.PluginState.OFF);
			}
			settings.setGeolocationEnabled(utils.getEnableLocation());
		}
		if(utils.getNightModeEnabled()) {
			ColorMatrixColorFilter filterInvert = new ColorMatrixColorFilter(
					sNegativeColorArray);
			mPaint.setColorFilter(filterInvert);
		}
		else{
			mPaint.setColorFilter(null);
		}
		setLayerType(View.LAYER_TYPE_HARDWARE, mPaint);
        searchEngine = utils.getSearchEngine();
		settings.setSupportZoom(true);
		settings.setBuiltInZoomControls(true);
		settings.setDisplayZoomControls(false);
		settings.setAllowContentAccess(true);
		settings.setAllowFileAccess(true);
		if (API >= Build.VERSION_CODES.JELLY_BEAN) {
			settings.setAllowFileAccessFromFileURLs(false);
			settings.setAllowUniversalAccessFromFileURLs(false);
		}
		settings.setJavaScriptEnabled(utils.getJavaEnabled());
		settings.setAppCacheEnabled(utils.getCacheEnabled());
	    settings.setLoadsImagesAutomatically(!utils.getBlockImages());
	}

	/*
	* This method sets the current instance of the BeHeView to go private or not
	 */
	public void setPrivate(boolean Private) {
		isPrivate = Private;
		if (isPrivate){
		CookieManager.getInstance().setAcceptCookie(false);
		getSettings().setCacheMode(getSettings().LOAD_NO_CACHE);
		getSettings().setAppCacheEnabled(false);
		clearHistory();
		clearCache(true);
		clearFormData();
		getSettings().setSavePassword(false);
		getSettings().setSaveFormData(false);
		theme.setIncognitoTheme();
	}
	else{
		theme.setTheme();
	}
	}

	public void searchWeb(String query){
		switch (searchEngine){
			case 1:
				String google = "https://www.google.com/search?q=" + query.replace(" ","+");
				loadUrl(google);
			break;
			case 2:
				String bing = "http://www.bing.com/search?q=" + query.replace(" ","+");
				loadUrl(bing);
			break;
			case 3:
				String yahoo = "https://search.yahoo.com/search?p=" + query.replace(" ","+");
				loadUrl(yahoo);
			break;
			case 4:
				String duck = "https://duckduckgo.com/?q=" + query.replace(" ","+");
				loadUrl(duck);
			break;
			case 5:
				String ask = "http://www.ask.com/web?q=" + query.replace(" ","+");
			    loadUrl(ask);
			break;
			case 6:
			String wow = "http://www.wow.com/search?s_it=search-thp&v_t=na&q=" + query.replace(" ","+");
			loadUrl(wow);
			break;
			case 7:
			String aol = "https://search.aol.com/aol/search?s_chn=prt_ticker-test-g&q=" + query.replace(" ","+");
			loadUrl(aol);
			break;
			case 8:
			String crawler = "https://www.webcrawler.com/serp?q=" + query.replace(" ","+");
			loadUrl(crawler);
			break;
			case 9:
			String myweb = "http://int.search.mywebsearch.com/mywebsearch/GGmain.jhtml?searchfor=" +  query.replace(" ","+");
			loadUrl(myweb);
			break;
			case 10:
            String info = "http://search.infospace.com/search/web?q=" + query.replace(" ","+");
			loadUrl(info);
			break;
			case 11:
			String yandex = "https://www.yandex.com/search/?text=" + query.replace(" ","+");
			loadUrl(yandex);
			break;
			case 12:
			String startpage = "https://www.startpage.com/do/search?q="  + query.replace(" ","+");
			loadUrl(startpage);
			break;
			case 13:
			String searx = "https://searx.me/?q="  + query.replace(" ","+");
			loadUrl(searx);
			break;
			}
	}
	public boolean isPrivate(){
		return isPrivate;
	}
	public void findInPage(String searchText){
		found = searchText;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
			findAllAsync(searchText);
		}
	    else{
			findAll(searchText);
		}
	}
    public void startReaderMode(){
		reload();
		addJavascriptInterface(new IJavascriptHandler(WEB_ACTIVITY), "INTERFACE");
		loadUrl("javascript:window.INTERFACE.processContent(document.getElementsByTagName('body')[0].innerText,document.title);");
	}
	public void destroy(){
	  ViewGroup parent = (ViewGroup) this.getParent();
	  if( parent != null){
		  parent.removeView(this);
	  }
	  this.stopLoading();
	  this.onPause();
	  this.clearHistory();
	  this.clearCache(true);
	  this.setVisibility(View.GONE);
	  this.removeAllViews();
	  this.destroyDrawingCache();
	  super.destroy();
	}
	public void loadHomepage(){
		PreferenceUtils utils = new PreferenceUtils(WEB_ACTIVITY);
		if(utils.getHomePage().equals("default")) {
			HomePage page = new HomePage(this, WEB_ACTIVITY.getApplication());
			Void[] b = null;
			page.execute(b);
			text.setText(Html.fromHtml("<font color='#228B22'>" + getResources().getString(R.string.home) + "</font>"), TextView.BufferType.SPANNABLE);
		}
	    else{
			loadUrl(utils.getHomePage());
		}
	}
	public void setDesktop(){
		getSettings().setDisplayZoomControls(true);
		getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 Safari/537.36");
		setInitialScale(-10);
		getSettings().setBuiltInZoomControls(true);
		reload();
	}
	public void setMobile(){
		getSettings().setDisplayZoomControls(false);
		getSettings().setBuiltInZoomControls(false);
		if (API >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			getSettings().setUserAgentString(WebSettings.getDefaultUserAgent(WEB_ACTIVITY));
		} else {
			getSettings().setUserAgentString("Mozilla/5.0 (Linux; <Android Version>; <Build Tag etc.>) AppleWebKit/<WebKit Rev> (KHTML, like Gecko) Chrome/<Chrome Rev> Mobile Safari/<WebKit Rev> ");
		}
		setInitialScale(0);
		reload();
	}
	public void loadHistory(){
		HystoryTask task = new HystoryTask(WEB_ACTIVITY,this);
		Void[] va = null;
		task.execute(va);
	}

	class IJavascriptHandler {
		Context mContext;
		IJavascriptHandler(Context c) {
			mContext = c;
		}
		@android.webkit.JavascriptInterface
		public void processContent(String aContent,String title) {
			final String content = aContent;
			Intent mReading = new Intent(WEB_ACTIVITY, ReadingActivity.class);
		    mReading.putExtra("text",content);
			mReading.putExtra("title",title);
			WEB_ACTIVITY.startActivity(mReading);
		}
	}
    public void loadHistoty(){
		HystoryTask task = new HystoryTask(this.getContext(),this);
		Void[] d = null;
		task.execute(d);
	}
    public void setTitle(){
		if(getUrl().contains("file")){
			text.setText(Html.fromHtml("<font color='#228B22'>" + getResources().getString(R.string.home) + "</font>"), TextView.BufferType.SPANNABLE);
		}
		else{
			if(getUrl() != null){
				text.setText(getUrl());
			}
		}

	}
    public Drawable getScreenshot(){
		Picture pic = capturePicture();
		PictureDrawable draw = new PictureDrawable(pic);
		return draw;
	}
    public boolean isCurrentTab(){
		return FOCUS;
	}
    public void setIsCurrentTab(boolean focus){
		FOCUS = focus;
	}
    public void setSearchEngine(int engine){
		searchEngine = engine;
	}
    public Activity getActivity(){
		return WEB_ACTIVITY;
	}
    public void setNewParams(EditText txt, ProgressBar pBar, AppCompatActivity activity, boolean pvt){
		text = txt;
		P_BAR = pBar;
		WEB_ACTIVITY = activity;
		isPrivate = pvt;
		chromeClient = new BeHeChromeClient(P_BAR,this,WEB_ACTIVITY);
		setWebChromeClient(chromeClient);
		setWebViewClient(new BeHeWebClient(text,WEB_ACTIVITY,false,this));
		setDownloadListener(new CiobanDownloadListener(WEB_ACTIVITY, this));
		initializeSettings();
	}
    public void setMAtch(String t){
		found = t;
	}
    public String hasAnyMatches(){
		return found;
	}
	public class JavascriptInterface
	{
		@android.webkit.JavascriptInterface
		public void notifyVideoEnd() // Must match Javascript interface method of VideoEnabledWebChromeClient
		{
			// This code is not executed in the UI thread, so we must force that to happen
			new Handler(Looper.getMainLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					if (videoEnabledWebChromeClient != null)
					{
						videoEnabledWebChromeClient.onHideCustomView();
					}
				}
			});
		}
	}

	private BeHeChromeClient videoEnabledWebChromeClient;
	private boolean addedJavascriptInterface;

	private void addJavascriptInterface()
	{
		if (!addedJavascriptInterface)
		{
			// Add javascript interface to be called when the video ends (must be done before page load)
			addJavascriptInterface(new JavascriptInterface(), "_VideoEnabledWebView"); // Must match Javascript interface name of VideoEnabledWebChromeClient

			addedJavascriptInterface = true;
		}
	}
	@Override
	public void loadData(String data, String mimeType, String encoding)
	{
		addJavascriptInterface();
		super.loadData(data, mimeType, encoding);
	}

	@Override
	public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl)
	{
		addJavascriptInterface();
		super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
	}

	@Override
	public void loadUrl(String url)
	{
		addJavascriptInterface();
		super.loadUrl(url);
	}

	@Override
	public void loadUrl(String url, Map<String, String> additionalHttpHeaders)
	{
		addJavascriptInterface();
		super.loadUrl(url, additionalHttpHeaders);
	}
    public BeHeChromeClient getBeHeChromeClient(){
		return chromeClient;
	}
    public boolean isFull(){
		 return chromeClient.onBackPressed();
	}
}





