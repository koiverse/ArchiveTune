package moe.koiverse.archivetune.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import moe.koiverse.archivetune.extensions.system.ExtensionManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExtensionManagerEntryPoint {
    fun extensionManager(): ExtensionManager
}

