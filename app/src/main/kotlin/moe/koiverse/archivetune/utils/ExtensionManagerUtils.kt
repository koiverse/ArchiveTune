package moe.koiverse.archivetune.utils

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import moe.koiverse.archivetune.di.ExtensionManagerEntryPoint

fun getExtensionManager(context: Context): moe.koiverse.archivetune.extensions.system.ExtensionManager {
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        ExtensionManagerEntryPoint::class.java
    )
    return entryPoint.extensionManager()
}