package moe.koiverse.archivetune.utils

import android.content.Context
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.constants.*
import moe.koiverse.archivetune.utils.dataStore
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage
import com.my.kizzy.rpc.Type
import com.my.kizzy.rpc.StatusDisplayType

class DiscordRPC(
    val context: Context,
    token: String,
) : KizzyRPC(token) {

    suspend fun updateSong(
        song: Song,
        currentPlaybackTimeMillis: Long,
        isPaused: Boolean = false
    ) = runCatching {
        val currentTime = System.currentTimeMillis()
        val calculatedStartTime = currentTime - currentPlaybackTimeMillis

        val namePref = context.dataStore[DiscordActivityNameKey] ?: "APP"
        val detailsPref = context.dataStore[DiscordActivityDetailsKey] ?: "SONG"
        val statePref = context.dataStore[DiscordActivityStateKey] ?: "ARTIST"
        val buttonUrlPref = context.dataStore[DiscordActivityButtonUrlKey] ?: "SONG"
        val showWhenPaused = context.dataStore[DiscordShowWhenPausedKey] ?: true

        fun pickSourceValue(pref: String, song: Song?, default: String): String {
            return when (pref) {
                "ARTIST" -> song?.artists?.firstOrNull()?.name ?: default
                "ALBUM" -> song?.song?.albumName ?: song.album?.title ?: default
                "SONG" -> song?.song?.title ?: default
                "APP" -> default
                else -> default
            }
        }

        val activityName = pickSourceValue(namePref, song, context.getString(R.string.app_name))
        val activityDetails = pickSourceValue(detailsPref, song, song?.song?.title ?: "Unknown")
        val activityState = pickSourceValue(statePref, song, song?.artists?.joinToString { it.name } ?: "Unknown")

        val baseSongUrl = "https://music.youtube.com/watch?v=${song?.song?.id}"
        val defaultButton1Label = "Listen on YouTube Music"
        val defaultButton1Url = baseSongUrl
        val defaultButton2Label = "View Album"
        val defaultButton2Url = song?.album?.playlistId?.let { "https://music.youtube.com/playlist?list=$it" } ?: baseSongUrl

        val button1Label = context.dataStore[DiscordActivityButton1LabelKey] ?: defaultButton1Label
        val button1UrlPref = context.dataStore[DiscordActivityButton1UrlKey] ?: ""
        val button1Enabled = context.dataStore[DiscordActivityButton1EnabledKey] ?: true
        val resolvedButton1Url = if (button1UrlPref.isNotEmpty()) button1UrlPref else defaultButton1Url

        val button2Label = context.dataStore[DiscordActivityButton2LabelKey] ?: defaultButton2Label
        val button2UrlPref = context.dataStore[DiscordActivityButton2UrlKey] ?: ""
        val button2Enabled = context.dataStore[DiscordActivityButton2EnabledKey] ?: true
        val resolvedButton2Url = if (button2UrlPref.isNotEmpty()) button2UrlPref else defaultButton2Url

        val activityTypePref = context.dataStore[DiscordActivityTypeKey] ?: "LISTENING"
        val resolvedType = when (activityTypePref.uppercase()) {
            "PLAYING" -> Type.PLAYING
            "STREAMING" -> Type.STREAMING
            "LISTENING" -> Type.LISTENING
            "WATCHING" -> Type.WATCHING
            "COMPETING" -> Type.COMPETING
            else -> Type.LISTENING
        }

        val buttons = mutableListOf<Pair<String, String>>()
        if (button1Enabled && button1Label.isNotBlank() && resolvedButton1Url.isNotBlank()) {
            buttons.add(button1Label to resolvedButton1Url)
        }
        if (button2Enabled && button2Label.isNotBlank() && resolvedButton2Url.isNotBlank()) {
            buttons.add(button2Label to resolvedButton2Url)
        }

        val largeImageTypePref = context.dataStore[DiscordLargeImageTypeKey] ?: "thumbnail"
        val largeImageCustomPref = context.dataStore[DiscordLargeImageCustomUrlKey] ?: ""
        val smallImageTypePref = context.dataStore[DiscordSmallImageTypeKey] ?: "artist"
        val smallImageCustomPref = context.dataStore[DiscordSmallImageCustomUrlKey] ?: ""
        val smallImageEnabled = context.dataStore[DiscordSmallImageEnabledKey] ?: true

        fun pickImage(type: String, custom: String?, song: Song?, preferArtist: Boolean = false): RpcImage? {
            return when (type) {
                "thumbnail" -> song?.song?.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
                "artist" -> song?.artists?.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
                "appicon" -> null
                "custom" -> (custom?.takeIf { it.isNotBlank() } ?: song?.song?.thumbnailUrl)?.let { RpcImage.ExternalImage(it) }
                else -> if (preferArtist) {
                    song?.artists?.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
                } else {
                    song?.song?.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
                }
            }
        }

        val largeImageRpc = pickImage(largeImageTypePref, largeImageCustomPref, song, false)

        val smallImageRpc = when {
            isPaused && showWhenPaused -> RpcImage.DiscordAsset("ic_pause_white")
            else -> {
                val resolved = if (smallImageEnabled) pickImage(smallImageTypePref, smallImageCustomPref, song, true) else null
                resolved ?: RpcImage.DiscordAsset("ic_play_white")
            }
        }

        setActivity(
            name = activityName.removeSuffix(" Debug"),
            details = activityDetails,
            state = activityState,
            detailsUrl = baseSongUrl,
            largeImage = largeImageRpc,
            smallImage = smallImageRpc,
            largeText = song.song.albumName ?: song.album?.title,
            smallText = song.artists.firstOrNull()?.name,
            buttons = buttons,
            type = resolvedType,
            statusDisplayType = StatusDisplayType.STATE,
            since = currentTime,
            startTime = if (isPaused) null else calculatedStartTime,
            endTime = if (isPaused) null else currentTime + (song.song.duration * 1000L - currentPlaybackTimeMillis),
            applicationId = APPLICATION_ID
        )
    }

    companion object {
        private const val APPLICATION_ID = "1165706613961789445"
    }
}
