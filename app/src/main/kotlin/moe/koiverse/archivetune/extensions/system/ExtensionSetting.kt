package moe.koiverse.archivetune.extensions.system

import kotlinx.serialization.Serializable

@Serializable
enum class SettingType {
    toggle,
    slider,
    text,
    select
}

@Serializable
data class SettingDefinition(
    val key: String,
    val type: SettingType,
    val label: String,
    val defaultBoolean: Boolean? = null,
    val defaultNumber: Int? = null,
    val defaultString: String? = null,
    val options: List<String>? = null,
    val min: Int? = null,
    val max: Int? = null,
    val step: Int? = null
)
