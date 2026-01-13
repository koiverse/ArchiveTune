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
    val pair = manager.uiConfig(route)

    if (pair == null) {
        content()
        return
    }

    val cfg = pair.second.config
    val base = pair.second.base
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

    when (cfg.mode) {
        UIMode.replace -> {
            RenderUI(cfg, base, onAction, onValueChange, values)
        }
        UIMode.overlay -> {
            Box(modifier = modifier) {
                content()
                RenderUI(cfg, base, onAction, onValueChange, values)
            }
        }
        UIMode.prepend -> {
            Column(modifier = modifier) {
                RenderUI(cfg, base, onAction, onValueChange, values)
                content()
            }
        }
        UIMode.append -> {
            Column(modifier = modifier) {
                content()
                RenderUI(cfg, base, onAction, onValueChange, values)
            }
        }
        UIMode.wrap -> {
            Box(modifier = modifier) {
                RenderUI(cfg, base, onAction, onValueChange, values)
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
        UIMode.inject -> {
            Box(modifier = modifier) {
                content()
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

@Composable
fun ExtensionSlot(
    slotId: String,
    modifier: Modifier = Modifier,
    defaultContent: @Composable () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val pair = manager.uiConfig("slot_$slotId")

    if (pair == null) {
        defaultContent()
        return
    }

    val cfg = pair.second.config
    val base = pair.second.base
    val values = remember { mutableStateMapOf<String, Any>() }

    RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
}

@Composable
fun ExtensionTopBarActions(
    route: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val pair = manager.uiConfig("topbar_actions_$route")

    if (pair == null) return

    val cfg = pair.second.config
    val base = pair.second.base
    val values = remember { mutableStateMapOf<String, Any>() }

    RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
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
    val pair = manager.uiConfig("bottombar_$route")

    if (pair == null) {
        defaultContent()
        return
    }

    val cfg = pair.second.config
    val base = pair.second.base
    val values = remember { mutableStateMapOf<String, Any>() }

    when (cfg.mode) {
        UIMode.replace -> RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
        UIMode.overlay -> {
            Box(modifier = modifier) {
                defaultContent()
                RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
            }
        }
        UIMode.prepend -> {
            Column(modifier = modifier) {
                RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
                defaultContent()
            }
        }
        UIMode.append -> {
            Column(modifier = modifier) {
                defaultContent()
                RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
            }
        }
        else -> {
            defaultContent()
            RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
        }
    }
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
    val pair = manager.uiConfig("fab_$route")

    if (pair == null) {
        defaultContent()
        return
    }

    val cfg = pair.second.config
    val base = pair.second.base
    val values = remember { mutableStateMapOf<String, Any>() }

    when (cfg.mode) {
        UIMode.replace -> RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
        else -> {
            Box(modifier = modifier) {
                defaultContent()
                RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
            }
        }
    }
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
    val pair = manager.uiConfig("context_${contextId}_${itemType}")

    if (pair == null) {
        defaultContent()
        return
    }

    val cfg = pair.second.config
    val base = pair.second.base
    val values = remember { mutableStateMapOf<String, Any>() }

    values["itemId"] = itemId
    values["itemType"] = itemType

    when (cfg.mode) {
        UIMode.replace -> RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
        UIMode.append -> {
            Column(modifier = modifier) {
                defaultContent()
                RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
            }
        }
        UIMode.prepend -> {
            Column(modifier = modifier) {
                RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
                defaultContent()
            }
        }
        else -> {
            defaultContent()
        }
    }
}

@Composable
fun ExtensionNavigationItem(
    position: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val pair = manager.uiConfig("nav_item_$position")

    if (pair == null) return

    val cfg = pair.second.config
    val base = pair.second.base
    val values = remember { mutableStateMapOf<String, Any>() }

    RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
}

@Composable
fun ExtensionPlayerOverlay(
    modifier: Modifier = Modifier,
    defaultContent: @Composable () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val pair = manager.uiConfig("player_overlay")

    if (pair == null) {
        defaultContent()
        return
    }

    val cfg = pair.second.config
    val base = pair.second.base
    val values = remember { mutableStateMapOf<String, Any>() }

    Box(modifier = modifier) {
        defaultContent()
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
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
    val pair = manager.uiConfig("lyrics_overlay")

    if (pair == null) {
        defaultContent()
        return
    }

    val cfg = pair.second.config
    val base = pair.second.base
    val values = remember { mutableStateMapOf<String, Any>() }

    Box(modifier = modifier) {
        defaultContent()
        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
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
    val pair = manager.uiConfig("queue_overlay")

    if (pair == null) {
        defaultContent()
        return
    }

    val cfg = pair.second.config
    val base = pair.second.base
    val values = remember { mutableStateMapOf<String, Any>() }

    Box(modifier = modifier) {
        defaultContent()
        RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
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
    val pair = manager.uiConfig("home_widget_$widgetId")

    if (pair == null) return

    val cfg = pair.second.config
    val base = pair.second.base
    val values = remember { mutableStateMapOf<String, Any>() }

    RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
}

@Composable
fun ExtensionSearchFilter(
    modifier: Modifier = Modifier,
    defaultContent: @Composable () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val pair = manager.uiConfig("search_filter")

    if (pair == null) {
        defaultContent()
        return
    }

    val cfg = pair.second.config
    val base = pair.second.base
    val values = remember { mutableStateMapOf<String, Any>() }

    when (cfg.mode) {
        UIMode.replace -> RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
        UIMode.append -> {
            Column(modifier = modifier) {
                defaultContent()
                RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
            }
        }
        else -> {
            Box(modifier = modifier) {
                defaultContent()
                RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
            }
        }
    }
}

@Composable
fun ExtensionSettingsEntry(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager = entryPoint.extensionManager()
    val pair = manager.uiConfig("settings_entry")

    if (pair == null) return

    val cfg = pair.second.config
    val base = pair.second.base
    val values = remember { mutableStateMapOf<String, Any>() }

    RenderUI(cfg, base, {}, { k, v -> values[k] = v }, values)
}
