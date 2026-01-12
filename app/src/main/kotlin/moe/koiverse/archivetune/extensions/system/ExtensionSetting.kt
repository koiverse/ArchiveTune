package moe.koiverse.archivetune.extensions.system

import kotlinx.serialization.Serializable

@Serializable
enum class SettingType {
    toggle,
    slider
}

@Serializable
data class SettingDefinition(
    val key: String,
    val type: SettingType,
    val label: String,
    val defaultBoolean: Boolean? = null,
    val defaultNumber: Int? = null,
    val min: Int? = null,
    val max: Int? = null,
    val step: Int? = null
)

