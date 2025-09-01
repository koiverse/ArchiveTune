package moe.koiverse.archivetune.utils

import android.content.Context
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.constants.DiscordActivityNameKey
import moe.koiverse.archivetune.constants.DiscordActivityDetailsKey
import moe.koiverse.archivetune.constants.DiscordActivityStateKey
import moe.koiverse.archivetune.constants.DiscordActivityButtonUrlKey
import moe.koiverse.archivetune.constants.DiscordActivityButton1LabelKey
import moe.koiverse.archivetune.constants.DiscordActivityButton1UrlKey
import moe.koiverse.archivetune.constants.DiscordActivityButton2LabelKey
import moe.koiverse.archivetune.constants.DiscordActivityButton2UrlKey
import moe.koiverse.archivetune.utils.dataStore
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage

class DiscordRPC(
    val context: Context,
    token: String,
) : KizzyRPC(token) {
    suspend fun updateSong(song: Song, currentPlaybackTimeMillis: Long) = runCatching {
        val currentTime = System.currentTimeMillis()
        val calculatedStartTime = currentTime - currentPlaybackTimeMillis
        // Read user preferences (stored as strings matching enum names)
        val namePref = context.dataStore[DiscordActivityNameKey] ?: "APP"
        val detailsPref = context.dataStore[DiscordActivityDetailsKey] ?: "SONG"
        val statePref = context.dataStore[DiscordActivityStateKey] ?: "ARTIST"
        val buttonUrlPref = context.dataStore[DiscordActivityButtonUrlKey] ?: "SONG"

        fun pickSourceValue(pref: String, song: Song?, default: String): String {
            return when (pref) {
                "ARTIST" -> song?.artists?.firstOrNull()?.name ?: default
                "ALBUM" -> song?.album?.title ?: default
                "SONG" -> song?.song?.title ?: default
                "APP" -> default
                else -> default
            }
        }

        val activityName = pickSourceValue(namePref, song, context.getString(R.string.app_name))
        val activityDetails = pickSourceValue(detailsPref, song, song.song.title)
        val activityState = pickSourceValue(statePref, song, song.artists.joinToString { it.name })

        val buttonUrl = when (buttonUrlPref) {
            "ALBUM" -> song.album?.playlistId?.let { pid -> "https://music.youtube.com/playlist?list=$pid" }
            "SONG" -> "https://music.youtube.com/watch?v=${song.song.id}"
            "ARTIST" -> "https://music.youtube.com/watch?v=${song.song.id}"
            "APP" -> "https://music.youtube.com/watch?v=${song.song.id}"
            else -> "https://music.youtube.com/watch?v=${song.song.id}"
        } ?: "https://music.youtube.com/watch?v=${song.song.id}"

        // custom button labels / urls (optional)
        val defaultButton1Label = "Listen on YouTube Music"
        val defaultButton1Url = "https://music.youtube.com/watch?v=${song.song.id}"
        val defaultButton2Label = "View Album"
        val defaultButton2Url = song.album?.playlistId?.let { pid -> "https://music.youtube.com/playlist?list=$pid" } ?: defaultButton1Url

        val button1Label = context.dataStore[DiscordActivityButton1LabelKey] ?: defaultButton1Label
        val button1UrlPref = context.dataStore[DiscordActivityButton1UrlKey] ?: ""
        val button1Url = if (button1UrlPref.isNotEmpty()) button1UrlPref else defaultButton1Url

        val button2Label = context.dataStore[DiscordActivityButton2LabelKey] ?: defaultButton2Label
        val button2UrlPref = context.dataStore[DiscordActivityButton2UrlKey] ?: ""
        val button2Url = if (button2UrlPref.isNotEmpty()) button2UrlPref else defaultButton2Url

        setActivity(
            name = activityName.removeSuffix(" Debug"),
            details = activityDetails,
            state = activityState,
            detailsUrl = "https://music.youtube.com/watch?v=${song.song.id}",
            largeImage = song.song.thumbnailUrl?.let { RpcImage.ExternalImage(it) },
            smallImage = song.artists.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) },
            largeText = song.album?.title,
            smallText = song.artists.firstOrNull()?.name,
            buttons = listOf(
                button1Label to button1Url,
                button2Label to button2Url
            ),
            type = Type.LISTENING,
            statusDisplayType = StatusDisplayType.STATE,
            since = currentTime,
            startTime = calculatedStartTime,
            endTime = currentTime + (song.song.duration * 1000L - currentPlaybackTimeMillis),
            applicationId = APPLICATION_ID
        )
    }

    companion object {
        private const val APPLICATION_ID = "1165706613961789445"
    }
}
