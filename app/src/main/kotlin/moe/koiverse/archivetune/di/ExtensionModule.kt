package moe.koiverse.archivetune.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import moe.koiverse.archivetune.extensions.system.ExtensionManager

@Module
@InstallIn(SingletonComponent::class)
object ExtensionModule {
    @Provides
    @Singleton
    fun provideExtensionManager(@ApplicationContext context: Context): ExtensionManager {
        val manager = ExtensionManager(context)
        manager.discover()
        return manager
    }
}

