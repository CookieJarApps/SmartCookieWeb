package com.cookiegames.smartcookie.di

import com.cookiegames.smartcookie.adblock.allowlist.AllowListModel
import com.cookiegames.smartcookie.adblock.allowlist.SessionAllowListModel
import com.cookiegames.smartcookie.adblock.source.AssetsHostsDataSource
import com.cookiegames.smartcookie.adblock.source.HostsDataSource
import com.cookiegames.smartcookie.adblock.source.HostsDataSourceProvider
import com.cookiegames.smartcookie.adblock.source.PreferencesHostsDataSourceProvider
import com.cookiegames.smartcookie.database.adblock.HostsDatabase
import com.cookiegames.smartcookie.database.adblock.HostsRepository
import com.cookiegames.smartcookie.database.allowlist.AdBlockAllowListDatabase
import com.cookiegames.smartcookie.database.allowlist.AdBlockAllowListRepository
import com.cookiegames.smartcookie.database.bookmark.BookmarkDatabase
import com.cookiegames.smartcookie.database.bookmark.BookmarkRepository
import com.cookiegames.smartcookie.database.downloads.DownloadsDatabase
import com.cookiegames.smartcookie.database.downloads.DownloadsRepository
import com.cookiegames.smartcookie.database.history.HistoryDatabase
import com.cookiegames.smartcookie.database.history.HistoryRepository
import com.cookiegames.smartcookie.database.javascript.JavaScriptDatabase
import com.cookiegames.smartcookie.database.javascript.JavaScriptRepository
import com.cookiegames.smartcookie.ssl.SessionSslWarningPreferences
import com.cookiegames.smartcookie.ssl.SslWarningPreferences
import dagger.Binds
import dagger.Module

/**
 * Dependency injection module used to bind implementations to interfaces.
 */
@Module
abstract class AppBindsModule {

    @Binds
    abstract fun provideBookmarkModel(bookmarkDatabase: BookmarkDatabase): BookmarkRepository

    @Binds
    abstract fun provideDownloadsModel(downloadsDatabase: DownloadsDatabase): DownloadsRepository

    @Binds
    abstract fun providesHistoryModel(historyDatabase: HistoryDatabase): HistoryRepository

    @Binds
    abstract fun providesJavaScriptModel(javaScriptDatabase: JavaScriptDatabase): JavaScriptRepository

    @Binds
    abstract fun providesAdBlockAllowListModel(adBlockAllowListDatabase: AdBlockAllowListDatabase): AdBlockAllowListRepository

    @Binds
    abstract fun providesAllowListModel(sessionAllowListModel: SessionAllowListModel): AllowListModel

    @Binds
    abstract fun providesSslWarningPreferences(sessionSslWarningPreferences: SessionSslWarningPreferences): SslWarningPreferences

    @Binds
    abstract fun providesHostsDataSource(assetsHostsDataSource: AssetsHostsDataSource): HostsDataSource

    @Binds
    abstract fun providesHostsRepository(hostsDatabase: HostsDatabase): HostsRepository

    @Binds
    abstract fun providesHostsDataSourceProvider(preferencesHostsDataSourceProvider: PreferencesHostsDataSourceProvider): HostsDataSourceProvider
}
