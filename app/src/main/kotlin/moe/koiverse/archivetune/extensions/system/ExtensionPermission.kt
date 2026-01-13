package moe.koiverse.archivetune.extensions.system

import kotlinx.serialization.Serializable

@Serializable
enum class ExtensionPermission {
    PlaybackEvents,
    PlaybackControl,
    QueueObserve,
    QueueModify,
    QueueReorder,
    SettingsRead,
    SettingsWrite,
    UIOverride,
    UIInject,
    UITopBar,
    UIBottomBar,
    UIFloatingAction,
    UIContextMenu,
    UINavigation,
    UIPlayer,
    UILyrics,
    UIQueue,
    UIHome,
    UISearch,
    UISettings,
    UIAlbum,
    UIArtist,
    UIPlaylist,
    ThemeOverride,
    ThemeColors,
    ThemeTypography,
    ThemeShapes,
    NetworkAccess,
    NetworkInternet,
    NetworkLocal,
    StorageRead,
    StorageWrite,
    StorageCache,
    StorageDownloads,
    MediaMetadata,
    MediaArtwork,
    MediaLyrics,
    MediaDownload,
    LibraryRead,
    LibraryWrite,
    LibraryPlaylists,
    LibraryHistory,
    LibraryFavorites,
    AccountInfo,
    AccountSync,
    NotificationShow,
    NotificationMedia,
    BackgroundService,
    BackgroundPlayback,
    WakeLock,
    Vibration,
    Clipboard,
    SystemInfo,
    DeviceInfo,
    Analytics,
    ExternalApps,
    DeepLinks,
    Shortcuts,
    Widgets,
    AutoStart,
    FullAccess
}

object PermissionGroups {
    val UI_ALL = listOf(
        ExtensionPermission.UIOverride,
        ExtensionPermission.UIInject,
        ExtensionPermission.UITopBar,
        ExtensionPermission.UIBottomBar,
        ExtensionPermission.UIFloatingAction,
        ExtensionPermission.UIContextMenu,
        ExtensionPermission.UINavigation,
        ExtensionPermission.UIPlayer,
        ExtensionPermission.UILyrics,
        ExtensionPermission.UIQueue,
        ExtensionPermission.UIHome,
        ExtensionPermission.UISearch,
        ExtensionPermission.UISettings,
        ExtensionPermission.UIAlbum,
        ExtensionPermission.UIArtist,
        ExtensionPermission.UIPlaylist
    )

    val PLAYBACK_ALL = listOf(
        ExtensionPermission.PlaybackEvents,
        ExtensionPermission.PlaybackControl,
        ExtensionPermission.QueueObserve,
        ExtensionPermission.QueueModify,
        ExtensionPermission.QueueReorder
    )

    val THEME_ALL = listOf(
        ExtensionPermission.ThemeOverride,
        ExtensionPermission.ThemeColors,
        ExtensionPermission.ThemeTypography,
        ExtensionPermission.ThemeShapes
    )

    val STORAGE_ALL = listOf(
        ExtensionPermission.StorageRead,
        ExtensionPermission.StorageWrite,
        ExtensionPermission.StorageCache,
        ExtensionPermission.StorageDownloads
    )

    val LIBRARY_ALL = listOf(
        ExtensionPermission.LibraryRead,
        ExtensionPermission.LibraryWrite,
        ExtensionPermission.LibraryPlaylists,
        ExtensionPermission.LibraryHistory,
        ExtensionPermission.LibraryFavorites
    )

    val MEDIA_ALL = listOf(
        ExtensionPermission.MediaMetadata,
        ExtensionPermission.MediaArtwork,
        ExtensionPermission.MediaLyrics,
        ExtensionPermission.MediaDownload
    )

    val NETWORK_ALL = listOf(
        ExtensionPermission.NetworkAccess,
        ExtensionPermission.NetworkInternet,
        ExtensionPermission.NetworkLocal
    )

    val SETTINGS_ALL = listOf(
        ExtensionPermission.SettingsRead,
        ExtensionPermission.SettingsWrite
    )

    val DANGEROUS = listOf(
        ExtensionPermission.FullAccess,
        ExtensionPermission.StorageWrite,
        ExtensionPermission.LibraryWrite,
        ExtensionPermission.AccountSync,
        ExtensionPermission.NetworkInternet,
        ExtensionPermission.BackgroundService,
        ExtensionPermission.ExternalApps
    )
}

data class PermissionInfo(
    val permission: ExtensionPermission,
    val name: String,
    val description: String,
    val group: String,
    val dangerous: Boolean = false
)

object PermissionRegistry {
    private val permissions = mapOf(
        ExtensionPermission.PlaybackEvents to PermissionInfo(
            ExtensionPermission.PlaybackEvents,
            "Playback Events",
            "Listen to playback events like play, pause, skip",
            "Playback"
        ),
        ExtensionPermission.PlaybackControl to PermissionInfo(
            ExtensionPermission.PlaybackControl,
            "Playback Control",
            "Control playback: play, pause, skip, seek",
            "Playback"
        ),
        ExtensionPermission.QueueObserve to PermissionInfo(
            ExtensionPermission.QueueObserve,
            "Queue Observe",
            "View the current playback queue",
            "Playback"
        ),
        ExtensionPermission.QueueModify to PermissionInfo(
            ExtensionPermission.QueueModify,
            "Queue Modify",
            "Add or remove items from the queue",
            "Playback"
        ),
        ExtensionPermission.UIOverride to PermissionInfo(
            ExtensionPermission.UIOverride,
            "UI Override",
            "Replace or modify app UI screens",
            "UI",
            dangerous = true
        ),
        ExtensionPermission.UIInject to PermissionInfo(
            ExtensionPermission.UIInject,
            "UI Inject",
            "Inject custom UI elements into screens",
            "UI"
        ),
        ExtensionPermission.ThemeOverride to PermissionInfo(
            ExtensionPermission.ThemeOverride,
            "Theme Override",
            "Modify app colors and appearance",
            "Theme"
        ),
        ExtensionPermission.NetworkAccess to PermissionInfo(
            ExtensionPermission.NetworkAccess,
            "Network Access",
            "Access network for API calls",
            "Network",
            dangerous = true
        ),
        ExtensionPermission.StorageRead to PermissionInfo(
            ExtensionPermission.StorageRead,
            "Storage Read",
            "Read files from extension storage",
            "Storage"
        ),
        ExtensionPermission.StorageWrite to PermissionInfo(
            ExtensionPermission.StorageWrite,
            "Storage Write",
            "Write files to extension storage",
            "Storage",
            dangerous = true
        ),
        ExtensionPermission.LibraryRead to PermissionInfo(
            ExtensionPermission.LibraryRead,
            "Library Read",
            "Access music library data",
            "Library"
        ),
        ExtensionPermission.LibraryWrite to PermissionInfo(
            ExtensionPermission.LibraryWrite,
            "Library Write",
            "Modify music library data",
            "Library",
            dangerous = true
        ),
        ExtensionPermission.FullAccess to PermissionInfo(
            ExtensionPermission.FullAccess,
            "Full Access",
            "Complete access to all app features",
            "System",
            dangerous = true
        )
    )

    fun getInfo(permission: ExtensionPermission): PermissionInfo? = permissions[permission]

    fun isDangerous(permission: ExtensionPermission): Boolean =
        permissions[permission]?.dangerous ?: PermissionGroups.DANGEROUS.contains(permission)

    fun getGroup(permission: ExtensionPermission): String =
        permissions[permission]?.group ?: "Other"
}
