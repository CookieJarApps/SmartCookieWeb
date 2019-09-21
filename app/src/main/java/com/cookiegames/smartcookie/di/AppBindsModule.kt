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
import com.cookiegames.smartcookie.ssl.SessionSslWarningPreferences
import com.cookiegames.smartcookie.ssl.SslWarningPreferences
import dagger.Binds
import dagger.Module

/**
 * Dependency injection module used to bind implementations to interfaces.
 */
@Module
interface AppBindsModule {

    @Binds
    fun provideBookmarkModel(bookmarkDatabase: BookmarkDatabase): BookmarkRepository

    @Binds
    fun provideDownloadsModel(downloadsDatabase: DownloadsDatabase): DownloadsRepository

    @Binds
    fun providesHistoryModel(historyDatabase: HistoryDatabase): HistoryRepository

    @Binds
    fun providesAdBlockAllowListModel(adBlockAllowListDatabase: AdBlockAllowListDatabase): AdBlockAllowListRepository

    @Binds
    fun providesAllowListModel(sessionAllowListModel: SessionAllowListModel): AllowListModel

    @Binds
    fun providesSslWarningPreferences(sessionSslWarningPreferences: SessionSslWarningPreferences): SslWarningPreferences

    @Binds
    fun providesHostsDataSource(assetsHostsDataSource: AssetsHostsDataSource): HostsDataSource

    @Binds
    fun providesHostsRepository(hostsDatabase: HostsDatabase): HostsRepository

    @Binds
    fun providesHostsDataSourceProvider(preferencesHostsDataSourceProvider: PreferencesHostsDataSourceProvider): HostsDataSourceProvider
}
