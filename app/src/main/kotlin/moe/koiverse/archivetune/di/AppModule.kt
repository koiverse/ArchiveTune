package moe.koiverse.archivetune.di

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import moe.koiverse.archivetune.constants.MaxSongCacheSizeKey
import moe.koiverse.archivetune.db.InternalDatabase
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.get
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCache

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MusicDatabase = InternalDatabase.newInstance(context)

    @Singleton
    @Provides
    fun provideDatabaseProvider(
        @ApplicationContext context: Context,
    ): DatabaseProvider = StandaloneDatabaseProvider(context)

    @Singleton
    @Provides
    @PlayerCache
    fun providePlayerCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): SimpleCache {
        val cacheSize = context.dataStore[MaxSongCacheSizeKey] ?: 1024
        val evictor = when (cacheSize) {
            -1 -> NoOpCacheEvictor()
            else -> LeastRecentlyUsedCacheEvictor(cacheSize * 1024 * 1024L)
        }
        val cache = SimpleCache(
            context.filesDir.resolve("exoplayer"),
            evictor,
            databaseProvider,
        )
        if (cache.cacheSpace > 500 * 1024 * 1024L) {
            Thread {
                try {
                    val keys = cache.keys.sortedBy { cache.getCacheSpaceForKey(it) }
                    var freed = 0L
                    for (key in keys) {
                        if (cache.cacheSpace - freed <= 500 * 1024 * 1024L) break
                        val size = cache.getCacheSpaceForKey(key)
                        cache.removeResource(key)
                        freed += size
                    }
                } catch (_: Exception) {}
            }.start()
        }
        return cache
    }

    @Singleton
    @Provides
    @DownloadCache
    fun provideDownloadCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): SimpleCache {
        val constructor = {
            SimpleCache(context.filesDir.resolve("download"), NoOpCacheEvictor(), databaseProvider)
        }
        constructor().release()
        return constructor()
    }
}
