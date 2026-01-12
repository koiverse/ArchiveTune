package moe.koiverse.archivetune.extensions.system

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val entry: String,
    val allowSettings: Boolean = false,
    val permissions: List<String> = emptyList(),
    val settings: List<SettingDefinition> = emptyList()
)

