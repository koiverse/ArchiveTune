package moe.koiverse.archivetune.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import moe.koiverse.archivetune.di.DownloadCache
import moe.koiverse.archivetune.di.PlayerCache
import moe.koiverse.archivetune.extensions.system.ExtensionManager
import androidx.media3.datasource.cache.SimpleCache
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.playback.DownloadUtil
import moe.koiverse.archivetune.utils.SyncUtils

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExtensionManagerEntryPoint {
    fun extensionManager(): ExtensionManager

    @PlayerCache
    fun playerCache(): SimpleCache

    @DownloadCache
    fun downloadCache(): SimpleCache

    fun musicDatabase(): MusicDatabase

    fun downloadUtil(): DownloadUtil

    fun syncUtils(): SyncUtils
}

