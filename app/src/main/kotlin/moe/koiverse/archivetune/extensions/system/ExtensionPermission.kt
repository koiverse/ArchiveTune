package moe.koiverse.archivetune.extensions.system

import kotlinx.serialization.Serializable

@Serializable
enum class ExtensionPermission {
    PlaybackEvents,
    QueueObserve,
    QueueModify,
    SettingsRead
}

