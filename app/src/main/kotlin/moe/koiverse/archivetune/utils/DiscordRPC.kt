package moe.koiverse.archivetune.utils

import android.content.Context
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.constants.*
import moe.koiverse.archivetune.utils.dataStore
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class DiscordRPC(
    val context: Context,
    token: String,
) : KizzyRPC(token) {

    companion object {
        private const val APPLICATION_ID = "1165706613961789445"
        // Pause image URL (external). Replace with Discord asset key if available.
        private const val PAUSE_IMAGE_URL = "https://raw.githubusercontent.com/koiverse/ArchiveTune/main/fastlane/metadata/android/en-US/images/RPC/pause_icon.png"
        private const val logTag = "DiscordRPC"
    }

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

    val button1Label = context.dataStore[DiscordActivityButton1LabelKey] ?: "Listen on YouTube Music"
    val button1Enabled = context.dataStore[DiscordActivityButton1EnabledKey] ?: true
    val button2Label = context.dataStore[DiscordActivityButton2LabelKey] ?: "View Album"
    val button2Enabled = context.dataStore[DiscordActivityButton2EnabledKey] ?: true

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

    val activityTypePref = context.dataStore[DiscordActivityTypeKey] ?: "LISTENING"
    val resolvedType = when (activityTypePref.uppercase()) {
        "PLAYING" -> Type.PLAYING
        "STREAMING" -> Type.STREAMING
        "LISTENING" -> Type.LISTENING
        "WATCHING" -> Type.WATCHING
        "COMPETING" -> Type.COMPETING
        else -> Type.LISTENING
    }

    val largeImageTypePref = context.dataStore[DiscordLargeImageTypeKey] ?: "thumbnail"
    val largeImageCustomPref = context.dataStore[DiscordLargeImageCustomUrlKey] ?: ""
    val smallImageTypePref = context.dataStore[DiscordSmallImageTypeKey] ?: "artist"
    val smallImageCustomPref = context.dataStore[DiscordSmallImageCustomUrlKey] ?: ""

    fun pickImage(type: String, custom: String?, song: Song?, preferArtist: Boolean = false): RpcImage? {
        return when (type) {
            "thumbnail" -> song?.song?.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
            "artist" -> song?.artists?.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
            "appicon" -> RpcImage.DiscordImage("appicon")
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

    val largeImageRpc = pickImage(largeImageTypePref, largeImageCustomPref, song, false)
    val smallImageRpc = if (isPaused) RpcImage.ExternalImage(PAUSE_IMAGE_URL)
    else when (smallImageTypePref.lowercase()) {
        "none", "dontshow" -> null
        else -> pickImage(smallImageTypePref, smallImageCustomPref, song, true)
    }

    // ✅ Preload images and skip sending if not resolved
    val resolvedLargeImage = if (largeImageRpc != null) withTimeoutOrNull(2000L) { preloadImage(largeImageRpc) } else null
    val resolvedSmallImage = if (smallImageRpc != null) withTimeoutOrNull(2000L) { preloadImage(smallImageRpc) } else null
    if ((largeImageRpc != null && resolvedLargeImage == null) || (smallImageRpc != null && resolvedSmallImage == null)) {
        Timber.w("DiscordRPC: Skipping presence update because images could not be resolved")
        return@runCatching
    }

    val resolvedLargeText = when ((context.dataStore[DiscordLargeTextSourceKey] ?: "album").lowercase()) {
        "song" -> song.song.title
        "artist" -> song.artists.firstOrNull()?.name
        "album" -> song.song.albumName ?: song.album?.title
        "app" -> context.getString(R.string.app_name)
        "custom" -> (context.dataStore[DiscordLargeTextCustomKey] ?: "").ifBlank { null }
        "dontshow" -> null
        else -> song.song.albumName ?: song.album?.title
    }

    // ✅ Only send app ID when using DiscordImage + buttons exist
    val applicationIdToSend = if (buttons.isNotEmpty() &&
        (largeImageRpc is RpcImage.DiscordImage || smallImageRpc is RpcImage.DiscordImage)
    ) APPLICATION_ID else null

    val platformPref = context.dataStore[DiscordActivityPlatformKey] ?: "desktop"
    this.setPlatform(platformPref)

    val sendStartTime: Long? = if (isPaused) null else calculatedStartTime
    val sendEndTime: Long? = if (isPaused) null else currentTime + (song.song.duration * 1000L - currentPlaybackTimeMillis)
    val sendSince: Long? = if (isPaused) null else currentTime
    val sendSmallText: String? = if (isPaused) context.getString(R.string.discord_paused) else song.artists.firstOrNull()?.name

    val safeStatus = when (statusPref.lowercase()) {
        "online", "idle", "dnd", "invisible" -> statusPref
        else -> "online"
    }

    Timber.d("DiscordRPC: sending presence name=%s details=%s state=%s appId=%s buttons=%s",
        activityName, activityDetails, activityState, applicationIdToSend, buttons)

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
        status = safeStatus
    )
}


    suspend fun refreshActivity(song: Song, currentPlaybackTimeMillis: Long, isPaused: Boolean = false) = runCatching {
        try {
            updateSong(song, currentPlaybackTimeMillis, isPaused).getOrThrow()
        } catch (ex: Exception) {
            try {
                val msg = ex.message ?: ex.toString()
                Timber.tag(logTag).e("refreshActivity updateSong failed: %s", msg)
                moe.koiverse.archivetune.utils.GlobalLog.append(android.util.Log.ERROR, "DiscordRPC", "refreshActivity updateSong failed: $msg\n${ex.stackTraceToString()}")
            } catch (_: Exception) {}
            throw ex
        }
    }
}