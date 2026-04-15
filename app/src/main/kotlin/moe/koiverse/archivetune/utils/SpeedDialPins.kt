package moe.koiverse.archivetune.utils

enum class SpeedDialPinType(val value: String) {
    SONG("song"),
    ALBUM("album"),
    ARTIST("artist"),
    PLAYLIST("playlist");

    companion object {
        fun from(raw: String): SpeedDialPinType? {
            return entries.firstOrNull { it.value == raw.lowercase() }
        }
    }
}

data class SpeedDialPin(
    val type: SpeedDialPinType,
    val id: String,
) {
    fun encode(): String = "${type.value}:$id"

    companion object {
        fun decode(raw: String): SpeedDialPin? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val separatorIndex = trimmed.indexOf(':')
            if (separatorIndex <= 0) {
                return SpeedDialPin(type = SpeedDialPinType.SONG, id = trimmed)
            }
            val type = SpeedDialPinType.from(trimmed.substring(0, separatorIndex)) ?: return null
            val id = trimmed.substring(separatorIndex + 1).trim()
            if (id.isEmpty()) return null
            return SpeedDialPin(type = type, id = id)
        }
    }
}

fun parseSpeedDialPins(raw: String, maxItems: Int = 24): List<SpeedDialPin> {
    return raw
        .split(",")
        .mapNotNull(SpeedDialPin::decode)
        .distinctBy { "${it.type.value}:${it.id}" }
        .take(maxItems)
}

fun serializeSpeedDialPins(pins: List<SpeedDialPin>): String {
    return pins.joinToString(",") { it.encode() }
}

fun toggleSpeedDialPin(
    pins: List<SpeedDialPin>,
    pin: SpeedDialPin,
    maxItems: Int = 24,
): List<SpeedDialPin> {
    val exists = pins.any { it.type == pin.type && it.id == pin.id }
    return if (exists) {
        pins.filterNot { it.type == pin.type && it.id == pin.id }
    } else {
        (pins + pin).distinctBy { "${it.type.value}:${it.id}" }.take(maxItems)
    }
}
