package com.cookiegames.smartcookie.di

import com.cookiegames.smartcookie.BrowserApp
import com.cookiegames.smartcookie.adblock.BloomFilterAdBlocker
import com.cookiegames.smartcookie.adblock.NoOpAdBlocker
import com.cookiegames.smartcookie.browser.SearchBoxModel
import com.cookiegames.smartcookie.browser.activity.BrowserActivity
import com.cookiegames.smartcookie.browser.activity.ThemableBrowserActivity
import com.cookiegames.smartcookie.browser.bookmarks.BookmarksDrawerView
import com.cookiegames.smartcookie.device.BuildInfo
import com.cookiegames.smartcookie.dialog.LightningDialogBuilder
import com.cookiegames.smartcookie.download.DownloadHandler
import com.cookiegames.smartcookie.download.LightningDownloadListener
import com.cookiegames.smartcookie.reading.activity.ReadingActivity
import com.cookiegames.smartcookie.search.SuggestionsAdapter
import com.cookiegames.smartcookie.settings.activity.SettingsActivity
import com.cookiegames.smartcookie.settings.activity.ThemableSettingsActivity
import com.cookiegames.smartcookie.settings.fragment.*
import com.cookiegames.smartcookie.utils.ProxyUtils
import com.cookiegames.smartcookie.view.LightningChromeClient
import com.cookiegames.smartcookie.view.LightningView
import com.cookiegames.smartcookie.view.LightningWebClient
import android.app.Application
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [(AppModule::class), (AppBindsModule::class)])
interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun buildInfo(buildInfo: BuildInfo): Builder

        fun build(): AppComponent
    }

    fun inject(activity: BrowserActivity)

    fun inject(fragment: BookmarkSettingsFragment)

    fun inject(builder: LightningDialogBuilder)

    fun inject(lightningView: LightningView)

    fun inject(activity: ThemableBrowserActivity)

    fun inject(advancedSettingsFragment: AdvancedSettingsFragment)

    fun inject(app: BrowserApp)

    fun inject(proxyUtils: ProxyUtils)

    fun inject(activity: ReadingActivity)

    fun inject(webClient: LightningWebClient)

    fun inject(activity: SettingsActivity)

    fun inject(activity: ThemableSettingsActivity)

    fun inject(listener: LightningDownloadListener)

    fun inject(fragment: PrivacySettingsFragment)

    fun inject(fragment: DebugSettingsFragment)

    fun inject(fragment: ExtensionsSettingsFragment)

    fun inject(suggestionsAdapter: SuggestionsAdapter)

    fun inject(chromeClient: LightningChromeClient)

    fun inject(downloadHandler: DownloadHandler)

    fun inject(searchBoxModel: SearchBoxModel)

    fun inject(generalSettingsFragment: GeneralSettingsFragment)

    fun inject(displaySettingsFragment: DisplaySettingsFragment)

    fun inject(adBlockSettingsFragment: AdBlockSettingsFragment)

    fun inject(parentalSettingsFragment: ParentalControlSettingsFragment)

    fun inject(bookmarksView: BookmarksDrawerView)

    fun provideBloomFilterAdBlocker(): BloomFilterAdBlocker

    fun provideNoOpAdBlocker(): NoOpAdBlocker

}
