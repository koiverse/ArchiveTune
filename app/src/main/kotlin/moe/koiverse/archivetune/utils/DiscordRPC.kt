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
        private const val PAUSE_IMAGE_URL =
            "https://raw.githubusercontent.com/koiverse/ArchiveTune/main/fastlane/metadata/android/en-US/images/RPC/pause_icon.png"
        private const val logtag = "DiscordRPC"
    }

    private val repo = com.my.kizzy.repository.KizzyRepository()
    private val translator = com.github.therealbush:translator.Translator()
    private val translationCache: MutableMap<String, String> = mutableMapOf()
    private val preloadResults: MutableMap<String, String?> = mutableMapOf()
    private var lastSongId: String? = null

    private fun pickSourceValue(pref: String, song: Song?, default: String): String = when (pref) {
        "ARTIST" -> song?.artists?.firstOrNull()?.name ?: default
        "ALBUM" -> song?.song?.albumName ?: song?.album?.title ?: default
        "SONG" -> song?.song?.title ?: default
        "APP" -> default
        else -> default
    }

    private fun resolveUrl(source: String, song: Song, custom: String): String? = when (source.lowercase()) {
        "songurl" -> "https://music.youtube.com/watch?v=${song.song.id}"
        "artisturl" -> song.artists.firstOrNull()?.id?.let { "https://music.youtube.com/channel/$it" }
        "albumurl" -> song.album?.playlistId?.let { "https://music.youtube.com/playlist?list=$it" }
        "custom" -> if (custom.isNotBlank()) custom else null
        else -> null
    }

    private fun String?.toExternal(): RpcImage? {
        if (this == null) return null
        return if (startsWith("http://") || startsWith("https://")) {
            RpcImage.ExternalImage(this)
        } else {
            Timber.tag(logtag).v("Skipping non-http image for RPC: %s", this)
            null
        }
    }

    private fun pickImage(type: String, custom: String?, song: Song?, preferArtist: Boolean = false): RpcImage? {
        return when (type) {
            "thumbnail" -> song?.song?.thumbnailUrl.toExternal()
            "artist" -> song?.artists?.firstOrNull()?.thumbnailUrl.toExternal()
            "appicon" -> RpcImage.DiscordImage("appicon")
            "custom" -> (custom?.takeIf { it.isNotBlank() } ?: song?.song?.thumbnailUrl).toExternal()
            else -> if (preferArtist) song?.artists?.firstOrNull()?.thumbnailUrl.toExternal()
            else song?.song?.thumbnailUrl.toExternal()
        }
    }

    private fun rpcKey(image: RpcImage): String = when (image) {
        is RpcImage.DiscordImage -> "discord:${image.image}"
        is RpcImage.ExternalImage -> "external:${image.image}"
    }

    private suspend fun resolveOnce(image: RpcImage?): String? {
        if (image == null) return null
        val key = rpcKey(image)
        if (preloadResults.containsKey(key)) return preloadResults[key]

        val resolved = withTimeoutOrNull(4000L) {
            when (image) {
                is RpcImage.ExternalImage -> repo.getImage(image.image) ?: image.image
                is RpcImage.DiscordImage -> image.image
            }
        }

        preloadResults[key] = resolved
        Timber.tag(logtag).v("Invocation preload result for %s -> %s", key, resolved)
        return resolved
    }

    private fun wrapResolved(original: RpcImage?, resolved: String?): RpcImage? {
        if (resolved.isNullOrBlank()) return original
        return when {
            resolved.startsWith("http://") || resolved.startsWith("https://") -> RpcImage.ExternalImage(resolved)
            resolved.startsWith("mp:") || resolved.startsWith("b7.") -> original
            else -> original
        }
    }

    suspend fun updateSong(song: Song, currentPlaybackTimeMillis: Long, isPaused: Boolean = false) = runCatching {
        val currentTime = System.currentTimeMillis()
        val calculatedStartTime = currentTime - currentPlaybackTimeMillis

        // Reset cache if song changes
        if (lastSongId != song.song.id) {
            translationCache.clear()
            lastSongId = song.song.id
        }

        val namePref = context.dataStore[DiscordActivityNameKey] ?: "APP"
        val detailsPref = context.dataStore[DiscordActivityDetailsKey] ?: "SONG"
        val statePref = context.dataStore[DiscordActivityStateKey] ?: "ARTIST"
        val statusPref = context.dataStore[DiscordPresenceStatusKey] ?: "online"
        val showWhenPaused = context.dataStore[DiscordShowWhenPausedKey] ?: false

        if (isPaused && !showWhenPaused) {
            Timber.tag(logtag).v("paused and 'showWhenPaused' disabled → stopping activity")
            stopActivity()
            return@runCatching
        }


        // --- Translator Integration with Cache ---
        val translatorEnabled = context.dataStore[EnableTranslatorKey] ?: false
        val translatedMap = mutableMapOf<String, String>()

        if (translatorEnabled) {
            val contextList = (context.dataStore[TranslatorContextsKey] ?: "{song}")
                .split(",")
                .map { it.trim() }
            val targetLang = context.dataStore[TranslatorTargetLangKey] ?: "ENGLISH"

            val rawMap = mapOf(
                "{song}" to song.song.title,
                "{artist}" to song.artists.joinToString { it.name },
                "{album}" to (song.song.albumName ?: song.album?.title ?: "")
            )

            try {
                for (ctx in contextList) {
                    val value = rawMap[ctx]
                    if (!value.isNullOrBlank()) {
                        val cacheKey = "${song.song.id}:$ctx:$targetLang"
                        val translated = translationCache[cacheKey]

                        if (translated != null) {
                            translatedMap[ctx] = translated
                        } else {
                            try {
                                val result = translator.translateBlocking(value, targetLang)
                                translatedMap[ctx] = result.translatedText
                                translationCache[cacheKey] = result.translatedText
                            } catch (e: Exception) {
                                Timber.tag(logtag).e(e, "Translation failed for $ctx")
                                translatedMap[ctx] = value // fallback original
                                translationCache[cacheKey] = value
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag(logtag).e(e, "Translator init failed")
            }
        }
        // --- End Translator ---


        val activityName = translatedMap["{song}"] ?: pickSourceValue(namePref, song, context.getString(R.string.app_name))
        val activityDetails = translatedMap["{artist}"] ?: pickSourceValue(detailsPref, song, song.song.title)
        val activityState = translatedMap["{album}"] ?: pickSourceValue(statePref, song, song.artists.joinToString { it.name })

        val baseSongUrl = "https://music.youtube.com/watch?v=${song.song.id}"

        val button1Label = context.dataStore[DiscordActivityButton1LabelKey] ?: "Listen on YouTube Music"
        val button1Enabled = context.dataStore[DiscordActivityButton1EnabledKey] ?: true
        val button2Label = context.dataStore[DiscordActivityButton2LabelKey] ?: "View Album"
        val button2Enabled = context.dataStore[DiscordActivityButton2EnabledKey] ?: true

        val button1UrlSource = context.dataStore[DiscordActivityButton1UrlSourceKey] ?: "songurl"
        val button1CustomUrl = context.dataStore[DiscordActivityButton1CustomUrlKey] ?: ""
        val button2UrlSource = context.dataStore[DiscordActivityButton2UrlSourceKey] ?: "albumurl"
        val button2CustomUrl = context.dataStore[DiscordActivityButton2CustomUrlKey] ?: ""

        val resolvedButton1Url = resolveUrl(button1UrlSource, song, button1CustomUrl)
        val resolvedButton2Url = resolveUrl(button2UrlSource, song, button2CustomUrl)

        val buttons = mutableListOf<Pair<String, String>>().apply {
            if (button1Enabled && button1Label.isNotBlank() && !resolvedButton1Url.isNullOrBlank())
                add(button1Label to resolvedButton1Url)
            if (button2Enabled && button2Label.isNotBlank() && !resolvedButton2Url.isNullOrBlank())
                add(button2Label to resolvedButton2Url)
        }
        val finalButtons = buttons.take(2)

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

        val largeImageRpc = pickImage(largeImageTypePref, largeImageCustomPref, song, false)
        val smallImageRpc =
            if (isPaused) RpcImage.ExternalImage(PAUSE_IMAGE_URL)
            else if (smallImageTypePref.lowercase() in listOf("none", "dontshow")) null
            else pickImage(smallImageTypePref, smallImageCustomPref, song, true)

        val resolvedLargeImage = resolveOnce(largeImageRpc)
        val resolvedSmallImage = resolveOnce(smallImageRpc)

        val largeRequestedButFailed = largeImageRpc != null && resolvedLargeImage == null
        val smallRequestedButFailed = smallImageRpc != null && resolvedSmallImage == null
        if (largeRequestedButFailed && smallRequestedButFailed) {
            Timber.tag(logtag).w("Skipping presence update because both images failed")
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

        val finalLargeImage: RpcImage? = wrapResolved(largeImageRpc, resolvedLargeImage)
        val finalSmallImage: RpcImage? = wrapResolved(smallImageRpc, resolvedSmallImage)

        val applicationIdToSend =
            if (largeImageRpc is RpcImage.DiscordImage || smallImageRpc is RpcImage.DiscordImage) APPLICATION_ID else null

        val platformPref = context.dataStore[DiscordActivityPlatformKey] ?: "desktop"
        this.setPlatform(platformPref)

        val sendStartTime = if (isPaused) null else calculatedStartTime
        val sendEndTime = if (isPaused) null else currentTime + (song.song.duration * 1000L - currentPlaybackTimeMillis)
        val sendSince = if (isPaused) null else currentTime
        val sendSmallText = if (isPaused) context.getString(R.string.discord_paused) else song.artists.firstOrNull()?.name

        val safeStatus = when (statusPref.lowercase()) {
            "online", "idle", "dnd", "invisible" -> statusPref
            else -> "online"
        }

        try {
            this.refreshRPC(
                name = activityName.removeSuffix(" Debug"),
                details = activityDetails,
                state = activityState,
                detailsUrl = baseSongUrl,
                largeImage = finalLargeImage,
                smallImage = finalSmallImage,
                largeText = resolvedLargeText,
                smallText = sendSmallText,
                buttons = finalButtons,
                type = resolvedType,
                statusDisplayType = StatusDisplayType.STATE,
                since = sendSince,
                startTime = sendStartTime,
                endTime = sendEndTime,
                applicationId = applicationIdToSend,
                status = safeStatus
            )
            Timber.tag(logtag).i("sending presence name=%s details=%s state=%s", activityName, activityDetails, activityState)
        } catch (ex: Exception) {
            Timber.tag(logtag).e(ex, "updatePresence failed")
            throw ex
        }
    }

    suspend fun refreshActivity(song: Song, currentPlaybackTimeMillis: Long, isPaused: Boolean = false) = runCatching {
        try {
            updateSong(song, currentPlaybackTimeMillis, isPaused).getOrThrow()
        } catch (ex: Exception) {
            val msg = ex.message ?: ex.toString()
            Timber.tag("DiscordRPC").e("refreshActivity updateSong failed: %s", msg)
            moe.koiverse.archivetune.utils.GlobalLog.append(
                android.util.Log.ERROR,
                "DiscordRPC",
                "refreshActivity updateSong failed: $msg\n${ex.stackTraceToString()}"
            )
            throw ex
        }
    }
}
