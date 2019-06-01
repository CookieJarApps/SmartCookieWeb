package com.cookiegames.smartcookie.di;

import javax.inject.Singleton;

import com.cookiegames.smartcookie.adblock.AssetsAdBlocker;
import com.cookiegames.smartcookie.adblock.NoOpAdBlocker;
import com.cookiegames.smartcookie.browser.activity.BrowserActivity;
import com.cookiegames.smartcookie.malwareblock.AssetsMalwareBlock;
import com.cookiegames.smartcookie.malwareblock.NoOpMalwareBlock;
import com.cookiegames.smartcookie.reading.activity.ReadingActivity;
import com.cookiegames.smartcookie.browser.TabsManager;
import com.cookiegames.smartcookie.browser.activity.ThemableBrowserActivity;
import com.cookiegames.smartcookie.settings.activity.ThemableSettingsActivity;
import com.cookiegames.smartcookie.BrowserApp;
import com.cookiegames.smartcookie.browser.BrowserPresenter;
import com.cookiegames.smartcookie.browser.SearchBoxModel;
import com.cookiegames.smartcookie.constant.BookmarkPage;
import com.cookiegames.smartcookie.constant.DownloadsPage;
import com.cookiegames.smartcookie.constant.HistoryPage;
import com.cookiegames.smartcookie.constant.StartPage;
import com.cookiegames.smartcookie.dialog.LightningDialogBuilder;
import com.cookiegames.smartcookie.download.DownloadHandler;
import com.cookiegames.smartcookie.download.LightningDownloadListener;
import com.cookiegames.smartcookie.settings.fragment.BookmarkSettingsFragment;
import com.cookiegames.smartcookie.browser.fragment.BookmarksFragment;
import com.cookiegames.smartcookie.settings.fragment.DebugSettingsFragment;
import com.cookiegames.smartcookie.settings.fragment.GeneralSettingsFragment;
import com.cookiegames.smartcookie.settings.fragment.LightningPreferenceFragment;
import com.cookiegames.smartcookie.settings.fragment.PrivacySettingsFragment;
import com.cookiegames.smartcookie.browser.fragment.TabsFragment;
import com.cookiegames.smartcookie.search.SearchEngineProvider;
import com.cookiegames.smartcookie.search.SuggestionsAdapter;
import com.cookiegames.smartcookie.utils.ProxyUtils;
import com.cookiegames.smartcookie.view.LightningChromeClient;
import com.cookiegames.smartcookie.view.LightningView;
import com.cookiegames.smartcookie.view.LightningWebClient;
import dagger.Component;

@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {

    void inject(BrowserActivity activity);

    void inject(BookmarksFragment fragment);

    void inject(BookmarkSettingsFragment fragment);

    void inject(LightningDialogBuilder builder);

    void inject(TabsFragment fragment);

    void inject(LightningView lightningView);

    void inject(ThemableBrowserActivity activity);

    void inject(LightningPreferenceFragment fragment);

    void inject(BrowserApp app);

    void inject(ProxyUtils proxyUtils);

    void inject(ReadingActivity activity);

    void inject(LightningWebClient webClient);

    void inject(ThemableSettingsActivity activity);

    void inject(LightningDownloadListener listener);

    void inject(PrivacySettingsFragment fragment);

    void inject(StartPage startPage);

    void inject(HistoryPage historyPage);

    void inject(BookmarkPage bookmarkPage);

    void inject(DownloadsPage downloadsPage);

    void inject(BrowserPresenter presenter);

    void inject(TabsManager manager);

    void inject(DebugSettingsFragment fragment);

    void inject(SuggestionsAdapter suggestionsAdapter);

    void inject(LightningChromeClient chromeClient);

    void inject(DownloadHandler downloadHandler);

    void inject(SearchBoxModel searchBoxModel);

    void inject(SearchEngineProvider searchEngineProvider);

    void inject(GeneralSettingsFragment generalSettingsFragment);

    //void inject(NetworkObservable networkObservable);

    AssetsAdBlocker provideAssetsAdBlocker();

    NoOpAdBlocker provideNoOpAdBlocker();

    AssetsMalwareBlock provideAssetsMalwareBlock();

    NoOpMalwareBlock provideNoOpMalwareBlock();

}