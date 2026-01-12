package moe.koiverse.archivetune.extensions.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import dagger.hilt.android.EntryPointAccessors
import moe.koiverse.archivetune.di.ExtensionManagerEntryPoint
import moe.koiverse.archivetune.extensions.system.ui.RenderUI
import moe.koiverse.archivetune.extensions.system.ui.UIMode

@Composable
fun ExtensionsUiContainer(route: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val pair = manager.uiConfig(route)
    if (pair == null) {
        content()
        return
    }
    val cfg = pair.second.config
    val base = pair.second.base
    if (cfg.mode == UIMode.replace) {
        RenderUI(cfg, base)
    } else {
        Box(modifier = modifier) {
            content()
            RenderUI(cfg, base)
        }
    }
}

