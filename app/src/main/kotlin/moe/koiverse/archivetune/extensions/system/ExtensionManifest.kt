package moe.koiverse.archivetune.extensions.system

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionUIRoute(
    val route: String,
    val mode: String = "replace",
    val position: String = "content",
    val priority: Int = 0
)

@Serializable
data class ExtensionFeaturePatch(
    val feature: String,
    val action: String,
    val target: String? = null,
    val value: String? = null,
    val priority: Int = 0,
    val condition: String? = null
)

@Serializable
data class ExtensionHook(
    val event: String,
    val handler: String,
    val priority: Int = 0,
    val async: Boolean = false
)

@Serializable
data class ExtensionThemePatch(
    val target: String,
    val property: String,
    val value: String,
    val mode: String = "light"
)

@Serializable
data class ExtensionMenuEntry(
    val id: String,
    val label: String,
    val icon: String? = null,
    val route: String? = null,
    val action: String? = null,
    val position: String = "bottom",
    val order: Int = 0,
    val showWhen: String? = null
)

@Serializable
data class ExtensionContextAction(
    val id: String,
    val label: String,
    val icon: String? = null,
    val action: String,
    val context: List<String> = emptyList(),
    val showWhen: String? = null
)

@Serializable
data class ExtensionSettingsPage(
    val id: String,
    val title: String,
    val icon: String? = null,
    val description: String? = null,
    val settings: List<SettingDefinition> = emptyList(),
    val order: Int = 0,
    val showInMainSettings: Boolean = false
)

@Serializable
data class ExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val entry: String,
    val description: String? = null,
    val website: String? = null,
    val repository: String? = null,
    val license: String? = null,
    val minAppVersion: String? = null,
    val maxAppVersion: String? = null,
    val icon: String? = null,
    val banner: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val allowSettings: Boolean = false,
    val permissions: List<String> = emptyList(),
    val settings: List<SettingDefinition> = emptyList(),
    val settingsPages: List<ExtensionSettingsPage> = emptyList(),
    val uiRoutes: List<ExtensionUIRoute> = emptyList(),
    val featurePatches: List<ExtensionFeaturePatch> = emptyList(),
    val hooks: List<ExtensionHook> = emptyList(),
    val themePatches: List<ExtensionThemePatch> = emptyList(),
    val menuEntries: List<ExtensionMenuEntry> = emptyList(),
    val contextActions: List<ExtensionContextAction> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val conflicts: List<String> = emptyList(),
    val provides: List<String> = emptyList(),
    val autoEnable: Boolean = false,
    val hidden: Boolean = false,
    val beta: Boolean = false,
    val experimental: Boolean = false
)

