package moe.koiverse.archivetune.extensions.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.EntryPointAccessors
import moe.koiverse.archivetune.di.ExtensionManagerEntryPoint
import moe.koiverse.archivetune.extensions.system.ui.RenderNode
import moe.koiverse.archivetune.extensions.system.ui.RenderUI
import moe.koiverse.archivetune.extensions.system.ui.UIConfig
import moe.koiverse.archivetune.extensions.system.ui.UIMode
import moe.koiverse.archivetune.extensions.system.ui.UiSlots
import java.io.File

@Composable
fun ExtensionsUiContainer(
    route: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(route)

    if (entries.isEmpty()) {
        content()
        return
    }

    val values = remember { mutableStateMapOf<String, Any>() }

    val onAction: (String) -> Unit = { action ->
        when {
            action.startsWith("navigate:") -> { }
            action.startsWith("url:") -> { }
            action.startsWith("toast:") -> { }
            action.startsWith("reload") -> { }
            action.startsWith("setValue:") -> {
                val parts = action.removePrefix("setValue:").split("=", limit = 2)
                if (parts.size == 2) values[parts[0]] = parts[1]
            }
            action.startsWith("toggle:") -> {
                val key = action.removePrefix("toggle:")
                val current = values[key] as? Boolean ?: false
                values[key] = !current
            }
        }
    }

    val onValueChange: (String, Any) -> Unit = { key, value ->
        values[key] = value
    }

    val sortedEntries = entries.sortedByDescending { it.second.config.priority }

    var composed: @Composable () -> Unit = content

    sortedEntries.forEach { entry ->
        val cfg = entry.second.config
        val base = entry.second.base
        composed = when (cfg.mode) {
            UIMode.replace -> {
                { RenderUI(cfg, base, onAction, onValueChange, values) }
            }
            UIMode.overlay -> {
                {
                    Box(modifier = modifier) {
                        composed()
                        RenderUI(cfg, base, onAction, onValueChange, values)
                    }
                }
            }
            UIMode.prepend -> {
                {
                    Column(modifier = modifier) {
                        RenderUI(cfg, base, onAction, onValueChange, values)
                        composed()
                    }
                }
            }
            UIMode.append -> {
                {
                    Column(modifier = modifier) {
                        composed()
                        RenderUI(cfg, base, onAction, onValueChange, values)
                    }
                }
            }
            UIMode.wrap -> {
                {
                    Box(modifier = modifier) {
                        RenderUI(cfg, base, onAction, onValueChange, values)
                        Box(modifier = Modifier.fillMaxSize()) {
                            composed()
                        }
                    }
                }
            }
            UIMode.inject -> {
                {
                    Box(modifier = modifier) {
                        composed()
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            RenderUI(cfg, base, onAction, onValueChange, values)
                        }
                    }
                }
            }
        }
    }

    composed()
}

@Composable
fun ExtensionSlot(
    slotId: String,
    modifier: Modifier = Modifier,
    defaultContent: @Composable () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(UiSlots.slot(slotId))

    if (entries.isEmpty()) {
        defaultContent()
        return
    }

    val values = remember { mutableStateMapOf<String, Any>() }

    entries.sortedByDescending { it.second.config.priority }.forEach { entry ->
        val cfg = entry.second.config
        val base = entry.second.base
        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
    }
}

@Composable
fun ExtensionTopBarActions(
    route: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(UiSlots.topBarActions(route))

    if (entries.isEmpty()) return

    val values = remember { mutableStateMapOf<String, Any>() }

    entries.sortedByDescending { it.second.config.priority }.forEach { entry ->
        val cfg = entry.second.config
        val base = entry.second.base
        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
    }
}

@Composable
fun ExtensionBottomBar(
    route: String,
    modifier: Modifier = Modifier,
    defaultContent: @Composable () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(UiSlots.bottomBar(route))

    if (entries.isEmpty()) {
        defaultContent()
        return
    }

    val values = remember { mutableStateMapOf<String, Any>() }

    val sortedEntries = entries.sortedByDescending { it.second.config.priority }

    var composed: @Composable () -> Unit = defaultContent

    sortedEntries.forEach { entry ->
        val cfg = entry.second.config
        val base = entry.second.base
        composed = when (cfg.mode) {
            UIMode.replace -> {
                { RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values) }
            }
            UIMode.overlay -> {
                {
                    Box(modifier = modifier) {
                        composed()
                        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
                    }
                }
            }
            UIMode.prepend -> {
                {
                    Column(modifier = modifier) {
                        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
                        composed()
                    }
                }
            }
            UIMode.append -> {
                {
                    Column(modifier = modifier) {
                        composed()
                        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
                    }
                }
            }
            else -> {
                {
                    Column(modifier = modifier) {
                        composed()
                        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
                    }
                }
            }
        }
    }

    composed()
}

@Composable
fun ExtensionFloatingAction(
    route: String,
    modifier: Modifier = Modifier,
    defaultContent: @Composable () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(UiSlots.fab(route))

    if (entries.isEmpty()) {
        defaultContent()
        return
    }

    val values = remember { mutableStateMapOf<String, Any>() }

    val sortedEntries = entries.sortedByDescending { it.second.config.priority }

    var composed: @Composable () -> Unit = defaultContent

    sortedEntries.forEach { entry ->
        val cfg = entry.second.config
        val base = entry.second.base
        composed = when (cfg.mode) {
            UIMode.replace -> {
                { RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values) }
            }
            else -> {
                {
                    Box(modifier = modifier) {
                        composed()
                        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
                    }
                }
            }
        }
    }

    composed()
}

@Composable
fun ExtensionContextMenu(
    contextId: String,
    itemType: String,
    itemId: String,
    modifier: Modifier = Modifier,
    defaultContent: @Composable () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(UiSlots.contextMenu(contextId, itemType))

    if (entries.isEmpty()) {
        defaultContent()
        return
    }

    val values = remember { mutableStateMapOf<String, Any>() }

    values["itemId"] = itemId
    values["itemType"] = itemType

    val sortedEntries = entries.sortedByDescending { it.second.config.priority }

    var composed: @Composable () -> Unit = defaultContent

    sortedEntries.forEach { entry ->
        val cfg = entry.second.config
        val base = entry.second.base
        composed = when (cfg.mode) {
            UIMode.replace -> {
                { RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values) }
            }
            UIMode.append -> {
                {
                    Column(modifier = modifier) {
                        composed()
                        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
                    }
                }
            }
            UIMode.prepend -> {
                {
                    Column(modifier = modifier) {
                        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
                        composed()
                    }
                }
            }
            else -> {
                composed
            }
        }
    }

    composed()
}

@Composable
fun ExtensionNavigationItem(
    position: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(UiSlots.navItem(position))

    if (entries.isEmpty()) return

    val values = remember { mutableStateMapOf<String, Any>() }

    entries.sortedByDescending { it.second.config.priority }.forEach { entry ->
        val cfg = entry.second.config
        val base = entry.second.base
        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
    }
}

@Composable
fun ExtensionPlayerOverlay(
    modifier: Modifier = Modifier,
    defaultContent: @Composable () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(UiSlots.PLAYER_OVERLAY)

    if (entries.isEmpty()) {
        defaultContent()
        return
    }

    val values = remember { mutableStateMapOf<String, Any>() }

    Box(modifier = modifier) {
        defaultContent()
        entries.sortedByDescending { it.second.config.priority }.forEach { entry ->
            val cfg = entry.second.config
            val base = entry.second.base
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
            }
        }
    }
}

@Composable
fun ExtensionLyricsOverlay(
    modifier: Modifier = Modifier,
    defaultContent: @Composable () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(UiSlots.LYRICS_OVERLAY)

    if (entries.isEmpty()) {
        defaultContent()
        return
    }

    val values = remember { mutableStateMapOf<String, Any>() }

    Box(modifier = modifier) {
        defaultContent()
        entries.sortedByDescending { it.second.config.priority }.forEach { entry ->
            val cfg = entry.second.config
            val base = entry.second.base
            RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
        }
    }
}

@Composable
fun ExtensionQueueOverlay(
    modifier: Modifier = Modifier,
    defaultContent: @Composable () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(UiSlots.QUEUE_OVERLAY)

    if (entries.isEmpty()) {
        defaultContent()
        return
    }

    val values = remember { mutableStateMapOf<String, Any>() }

    Box(modifier = modifier) {
        defaultContent()
        entries.sortedByDescending { it.second.config.priority }.forEach { entry ->
            val cfg = entry.second.config
            val base = entry.second.base
            RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
        }
    }
}

@Composable
fun ExtensionHomeWidget(
    widgetId: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(UiSlots.homeWidget(widgetId))

    if (entries.isEmpty()) return

    val values = remember { mutableStateMapOf<String, Any>() }

    entries.sortedByDescending { it.second.config.priority }.forEach { entry ->
        val cfg = entry.second.config
        val base = entry.second.base
        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
    }
}

@Composable
fun ExtensionSearchFilter(
    modifier: Modifier = Modifier,
    defaultContent: @Composable () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(UiSlots.SEARCH_FILTER)

    if (entries.isEmpty()) {
        defaultContent()
        return
    }

    val values = remember { mutableStateMapOf<String, Any>() }

    val sortedEntries = entries.sortedByDescending { it.second.config.priority }

    var composed: @Composable () -> Unit = defaultContent

    sortedEntries.forEach { entry ->
        val cfg = entry.second.config
        val base = entry.second.base
        composed = when (cfg.mode) {
            UIMode.replace -> {
                { RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values) }
            }
            UIMode.append -> {
                {
                    Column(modifier = modifier) {
                        composed()
                        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
                    }
                }
            }
            else -> {
                {
                    Box(modifier = modifier) {
                        composed()
                        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
                    }
                }
            }
        }
    }

    composed()
}

@Composable
fun ExtensionSettingsEntry(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val entries = manager.uiConfigs(UiSlots.SETTINGS_ENTRY)

    if (entries.isEmpty()) return

    val values = remember { mutableStateMapOf<String, Any>() }

    entries.sortedByDescending { it.second.config.priority }.forEach { entry ->
        val cfg = entry.second.config
        val base = entry.second.base
        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
    }
}
