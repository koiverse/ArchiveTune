package moe.koiverse.archivetune.utils

import android.content.Context
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.constants.*
import moe.koiverse.archivetune.utils.dataStore
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage

class DiscordRPC(
    val context: Context,
    token: String,
) : KizzyRPC(token) {

    suspend fun updateSong(song: Song, currentPlaybackTimeMillis: Long, isPaused: Boolean = false) = runCatching {
        val currentTime = System.currentTimeMillis()
        val calculatedStartTime = currentTime - currentPlaybackTimeMillis

        // Activity text sources
        val namePref = context.dataStore[DiscordActivityNameKey] ?: "APP"
        val detailsPref = context.dataStore[DiscordActivityDetailsKey] ?: "SONG"
        val statePref = context.dataStore[DiscordActivityStateKey] ?: "ARTIST"
        val statusPref = context.dataStore[DiscordPresenceStatusKey] ?: "online"

        fun pickSourceValue(pref: String, song: Song?, default: String): String {
            return when (pref) {
                "ARTIST" -> song?.artists?.firstOrNull()?.name ?: default
                "ALBUM" -> song?.song?.albumName ?: song?.album?.title ?: default
                "SONG" -> song?.song?.title ?: default
                "APP" -> default
                else -> default
            }
        }

        val activityName = pickSourceValue(namePref, song, context.getString(R.string.app_name))
        val activityDetails = pickSourceValue(detailsPref, song, song.song.title)
        val activityState = pickSourceValue(statePref, song, song.artists.joinToString { it.name })

        val baseSongUrl = "https://music.youtube.com/watch?v=${song.song.id}"
        val defaultButton1Label = "Listen on YouTube Music"
        val defaultButton2Label = "View Album"

        val button1Label = context.dataStore[DiscordActivityButton1LabelKey] ?: defaultButton1Label
        val button1Enabled = context.dataStore[DiscordActivityButton1EnabledKey] ?: true

        val button2Label = context.dataStore[DiscordActivityButton2LabelKey] ?: defaultButton2Label
        val button2Enabled = context.dataStore[DiscordActivityButton2EnabledKey] ?: true

        // Per-button URL source
        val button1UrlSource = context.dataStore[DiscordActivityButton1UrlSourceKey] ?: "songurl"
        val button1CustomUrl = context.dataStore[DiscordActivityButton1CustomUrlKey] ?: ""

        val button2UrlSource = context.dataStore[DiscordActivityButton2UrlSourceKey] ?: "albumurl"
        val button2CustomUrl = context.dataStore[DiscordActivityButton2CustomUrlKey] ?: ""

        fun resolveUrl(source: String, song: Song, custom: String): String? {
            return when (source.lowercase()) {
                "songurl" -> "https://music.youtube.com/watch?v=${song.song.id}"
                "artisturl" -> song.artists.firstOrNull()?.id?.let { "https://music.youtube.com/channel/$it" }
                "albumurl" -> song.album?.playlistId?.let { "https://music.youtube.com/playlist?list=$it" }
                "custom" -> if (custom.isNotBlank()) custom else null
                else -> null
            }
        }

        val resolvedButton1Url = resolveUrl(button1UrlSource, song, button1CustomUrl)
        val resolvedButton2Url = resolveUrl(button2UrlSource, song, button2CustomUrl)

        val buttons = mutableListOf<Pair<String, String>>()
        if (button1Enabled && button1Label.isNotBlank() && !resolvedButton1Url.isNullOrBlank()) {
            buttons.add(button1Label to resolvedButton1Url)
        }
        if (button2Enabled && button2Label.isNotBlank() && !resolvedButton2Url.isNullOrBlank()) {
            buttons.add(button2Label to resolvedButton2Url)
        }

        // Activity type
        val activityTypePref = context.dataStore[DiscordActivityTypeKey] ?: "LISTENING"
        val resolvedType = when (activityTypePref.uppercase()) {
            "PLAYING" -> Type.PLAYING
            "STREAMING" -> Type.STREAMING
            "LISTENING" -> Type.LISTENING
            "WATCHING" -> Type.WATCHING
            "COMPETING" -> Type.COMPETING
            else -> Type.LISTENING
        }

        // Images
        val largeImageTypePref = context.dataStore[DiscordLargeImageTypeKey] ?: "thumbnail"
        val largeImageCustomPref = context.dataStore[DiscordLargeImageCustomUrlKey] ?: ""
        val smallImageTypePref = context.dataStore[DiscordSmallImageTypeKey] ?: "artist"
        val smallImageCustomPref = context.dataStore[DiscordSmallImageCustomUrlKey] ?: ""

        fun pickImage(type: String, custom: String?, song: Song?, preferArtist: Boolean = false): RpcImage? {
            return when (type) {
                "thumbnail" -> song?.song?.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
                "artist" -> song?.artists?.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
                "appicon" -> RpcImage.ExternalImage(
                    "https://raw.githubusercontent.com/koiverse/ArchiveTune/main/fastlane/metadata/android/en-US/images/icon.png"
                )
                "custom" -> (custom?.takeIf { it.isNotBlank() } ?: song?.song?.thumbnailUrl)?.let {
                    RpcImage.ExternalImage(it)
                }
                else -> if (preferArtist) {
                    song?.artists?.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
                } else {
                    song?.song?.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
                }
            }
        }
        // Preload/resolve images before sending presence to avoid showing broken images.
        // Attempt resolution with a short timeout to keep UI responsive.
        val resolvedLargeImage = if (largeImageRpc != null) {
            kotlinx.coroutines.withTimeoutOrNull(2000L) {
                this@DiscordRPC.preloadImage(largeImageRpc)
            }
        } else null
        val smallImageRpc = if (isPaused) {
            // external hosted pause icon (neutral licensed emoji asset)
            RpcImage.ExternalImage("https://raw.githubusercontent.com/koiverse/ArchiveTune/refs/heads/main/fastlane/metadata/android/en-US/images/RPC/pause_icon.png")

        val largeImageRpc = pickImage(largeImageTypePref, largeImageCustomPref, song, false)
        // If paused we prefer to show a pause small image and avoid timestamps (no progress bar)
        val smallImageRpc = if (isPaused) {
            // external hosted pause icon (neutral licensed emoji asset)
            RpcImage.ExternalImage("https://raw.githubusercontent.com/koiverse/ArchiveTune/refs/heads/main/fastlane/metadata/android/en-US/images/RPC/pause_icon.png")
        val resolvedSmallImage = if (smallImageRpc != null) {
            kotlinx.coroutines.withTimeoutOrNull(2000L) {
                this@DiscordRPC.preloadImage(smallImageRpc)
            }
        } else null
        } else {
        // If either image was requested but couldn't be resolved within timeout, skip sending RPC
        if ((largeImageRpc != null && resolvedLargeImage == null) || (smallImageRpc != null && resolvedSmallImage == null)) {
            // abort presence update to avoid showing broken images
            return@runCatching
        }
            when (smallImageTypePref.lowercase()) {
                "none", "dontshow" -> null
                else -> pickImage(smallImageTypePref, smallImageCustomPref, song, true)
            }
        }

        // Large text customization
        val largeTextSourcePref = context.dataStore[DiscordLargeTextSourceKey] ?: "album"
        val largeTextCustomPref = context.dataStore[DiscordLargeTextCustomKey] ?: ""

    val resolvedLargeText = when (largeTextSourcePref.lowercase()) {
            "song" -> song.song.title
            "artist" -> song.artists.firstOrNull()?.name
            "album" -> song.song.albumName ?: song.album?.title
            "app" -> context.getString(R.string.app_name)
            "custom" -> largeTextCustomPref.ifBlank { null }
            "dontshow" -> null
            else -> song.song.albumName ?: song.album?.title
        }

        // Only send applicationId when we're using Discord-hosted asset keys for images
        // This prevents Discord from falling back to the application's icon when using
        // externally uploaded images (attachments). If buttons are present and the
        // provided images are Discord asset keys, include the application id so buttons
        // continue to work as expected.
        val applicationIdToSend = if (buttons.isNotEmpty() &&
            (largeImageRpc is RpcImage.DiscordImage || smallImageRpc is RpcImage.DiscordImage)
        ) APPLICATION_ID else null
        val platformPref = context.dataStore[DiscordActivityPlatformKey] ?: "desktop"
        this.setPlatform(platformPref)

        // When paused we don't send start/end timestamps to avoid the progress bar; instead show a pause small image
        val sendStartTime: Long? = if (isPaused) null else calculatedStartTime
        val sendEndTime: Long? = if (isPaused) null else currentTime + (song.song.duration * 1000L - currentPlaybackTimeMillis)
        val sendSince: Long? = if (isPaused) null else currentTime
        val sendSmallText: String? = if (isPaused) context.getString(R.string.discord_paused) else song.artists.firstOrNull()?.name

        refreshRPC(
            name = activityName.removeSuffix(" Debug"),
            details = activityDetails,
            state = activityState,
            detailsUrl = baseSongUrl,
            largeImage = largeImageRpc,
            smallImage = smallImageRpc,
            largeText = resolvedLargeText,
            smallText = sendSmallText,
            buttons = buttons,
            type = resolvedType,
            statusDisplayType = StatusDisplayType.STATE,
            since = sendSince,
            startTime = sendStartTime,
            endTime = sendEndTime,
            applicationId = applicationIdToSend,
            status = statusPref
        )
    }

    suspend fun refreshActivity(song: Song, currentPlaybackTimeMillis: Long, isPaused: Boolean = false) = runCatching {
        updateSong(song, currentPlaybackTimeMillis, isPaused).getOrThrow()
    }


    companion object {
        private const val APPLICATION_ID = "1165706613961789445"
    }
}
