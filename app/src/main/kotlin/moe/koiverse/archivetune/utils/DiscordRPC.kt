/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.utils

import android.content.Context
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.constants.*
import moe.koiverse.archivetune.utils.dataStore
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage
import timber.log.Timber
import me.bush.translator.Translator
import me.bush.translator.Language


class DiscordRPC(
    val context: Context,
    token: String,
) : KizzyRPC(token) {

    companion object {
        private const val APPLICATION_ID = "1165706613961789445"
        private const val PAUSE_IMAGE_URL =
            "https://raw.githubusercontent.com/koiverse/ArchiveTune/main/fastlane/metadata/android/en-US/images/RPC/pause_icon.png"
        private const val APP_ICON_URL = 
            "https://raw.githubusercontent.com/koiverse/ArchiveTune/main/fastlane/metadata/android/en-US/images/icon.png"
        private const val logtag = "DiscordRPC"
    }

    private val translationCache: MutableMap<String, String> = mutableMapOf()
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
        "custom" -> custom.takeIf { it.isNotBlank() }?.let { normalizeUrl(it) }
        else -> null
    }

    private fun normalizeUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        return "https://$trimmed"
    }

    private fun createRpcImage(imageId: String?): RpcImage? {
        if (imageId.isNullOrBlank()) return null
        return when {
            imageId.startsWith("mp:") -> RpcImage.DiscordImage(imageId.removePrefix("mp:"))
            imageId.startsWith("external/") -> RpcImage.DiscordImage(imageId)
            imageId.startsWith("attachments/") -> RpcImage.DiscordImage(imageId)
            imageId.startsWith("http://", ignoreCase = true) || imageId.startsWith("https://", ignoreCase = true) -> RpcImage.ExternalImage(imageId)
            else -> RpcImage.DiscordImage(imageId)
        }
    }

    suspend fun updateSong(
        song: Song,
        currentPlaybackTimeMillis: Long,
        isPaused: Boolean = false,
    ) = runCatching {
        val currentTime = System.currentTimeMillis()
        val calculatedStartTime = currentTime - currentPlaybackTimeMillis

        if (lastSongId != song.song.id) {
            translationCache.clear()
            DiscordImageResolver.clearCache()
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
                val translator = Translator()
                for (ctx in contextList) {
                    val value = rawMap[ctx]
                    if (!value.isNullOrBlank()) {
                        val cacheKey = "${song.song.id}:$ctx:$targetLang"
                        val translated = translationCache[cacheKey]

                        if (translated != null) {
                            translatedMap[ctx] = translated
                        } else {
                            try {
                                val result = translator.translateBlocking(value, Language.valueOf(targetLang.uppercase()))
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

        val activityName = when (namePref.uppercase()) {
            "ARTIST" -> translatedMap["{artist}"] ?: pickSourceValue(namePref, song, context.getString(R.string.app_name))
            "ALBUM" -> translatedMap["{album}"] ?: pickSourceValue(namePref, song, context.getString(R.string.app_name))
            "SONG" -> translatedMap["{song}"] ?: pickSourceValue(namePref, song, context.getString(R.string.app_name))
            "APP" -> context.getString(R.string.app_name)
            else -> pickSourceValue(namePref, song, context.getString(R.string.app_name))
        }

        var activityDetails = when (detailsPref.uppercase()) {
            "ARTIST" -> translatedMap["{artist}"] ?: pickSourceValue(detailsPref, song, song.song.title)
            "ALBUM" -> translatedMap["{album}"] ?: pickSourceValue(detailsPref, song, song.song.title)
            "SONG" -> translatedMap["{song}"] ?: pickSourceValue(detailsPref, song, song.song.title)
            "APP" -> context.getString(R.string.app_name)
            else -> pickSourceValue(detailsPref, song, song.song.title)
        }

        // Ensure details are not empty — fallback to song title or app name so Discord shows something.
        if (activityDetails.isNullOrBlank()) {
            activityDetails = song.song.title.ifBlank { context.getString(R.string.app_name) }
        }

        var activityState = when (statePref.uppercase()) {
            "ARTIST" -> translatedMap["{artist}"] ?: pickSourceValue(statePref, song, song.artists.joinToString { it.name })
            "ALBUM" -> translatedMap["{album}"] ?: pickSourceValue(statePref, song, song.artists.joinToString { it.name })
            "SONG" -> translatedMap["{song}"] ?: pickSourceValue(statePref, song, song.artists.joinToString { it.name })
            "APP" -> context.getString(R.string.app_name)
            else -> pickSourceValue(statePref, song, song.artists.joinToString { it.name })
        }

        if (activityState.isNullOrBlank()) {
            activityState = song.artists.firstOrNull()?.name ?: context.getString(R.string.app_name)
        }

        val baseSongUrl = "https://music.youtube.com/watch?v=${song.song.id}"

        val button1Label = context.dataStore[DiscordActivityButton1LabelKey] ?: "Listen on YouTube Music"
        val button1Enabled = context.dataStore[DiscordActivityButton1EnabledKey] ?: true
        val button2Label = context.dataStore[DiscordActivityButton2LabelKey] ?: "Go to ArchiveTune"
        val button2Enabled = context.dataStore[DiscordActivityButton2EnabledKey] ?: true

        val button1UrlSource = context.dataStore[DiscordActivityButton1UrlSourceKey] ?: "songurl"
        val button1CustomUrl = context.dataStore[DiscordActivityButton1CustomUrlKey] ?: ""
        val button2UrlSource = context.dataStore[DiscordActivityButton2UrlSourceKey] ?: "custom"
        val button2CustomUrl = context.dataStore[DiscordActivityButton2CustomUrlKey] ?: "https://github.com/koiverse/ArchiveTune"

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

        val resolvedImages = DiscordImageResolver.resolveImagesForSong(context, song)
        
        Timber.tag(logtag).d(
            "Resolved images - thumbnail: %s, artist: %s",
            resolvedImages.thumbnailResolvedId?.take(40) ?: "null",
            resolvedImages.artistResolvedId?.take(40) ?: "null"
        )

        val finalLargeImage: RpcImage? = when (largeImageTypePref.lowercase()) {
            "thumbnail", "song", "album" -> {
                createRpcImage(resolvedImages.thumbnailResolvedId)
                    ?: createRpcImage(resolvedImages.thumbnailOriginalUrl)
                    ?: song.song.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { RpcImage.ExternalImage(it) }
            }
            "artist" -> {
                createRpcImage(resolvedImages.artistResolvedId)
                    ?: createRpcImage(resolvedImages.artistOriginalUrl)
                    ?: song.artists.firstOrNull()?.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { RpcImage.ExternalImage(it) }
            }
            "appicon" -> RpcImage.ExternalImage(APP_ICON_URL)
            "custom" -> {
                val customUrl = largeImageCustomPref.takeIf { it.isNotBlank() }
                if (customUrl != null) {
                    RpcImage.ExternalImage(customUrl)
                } else {
                    createRpcImage(resolvedImages.thumbnailResolvedId)
                        ?: createRpcImage(resolvedImages.thumbnailOriginalUrl)
                        ?: song.song.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { RpcImage.ExternalImage(it) }
                }
            }
            "none", "dontshow" -> null
            else -> {
                createRpcImage(resolvedImages.thumbnailResolvedId)
                    ?: createRpcImage(resolvedImages.thumbnailOriginalUrl)
            }
        }

        val finalSmallImage: RpcImage? = when {
            isPaused -> RpcImage.ExternalImage(PAUSE_IMAGE_URL)
            smallImageTypePref.lowercase() in listOf("none", "dontshow") -> null
            smallImageTypePref.lowercase() == "appicon" -> RpcImage.ExternalImage(APP_ICON_URL)
            smallImageTypePref.lowercase() == "artist" -> {
                createRpcImage(resolvedImages.artistResolvedId)
                    ?: createRpcImage(resolvedImages.artistOriginalUrl)
                    ?: song.artists.firstOrNull()?.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { RpcImage.ExternalImage(it) }
            }
            smallImageTypePref.lowercase() in listOf("thumbnail", "song", "album") -> {
                createRpcImage(resolvedImages.thumbnailResolvedId)
                    ?: createRpcImage(resolvedImages.thumbnailOriginalUrl)
                    ?: song.song.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { RpcImage.ExternalImage(it) }
            }
            smallImageTypePref.lowercase() == "custom" -> {
                val customUrl = smallImageCustomPref.takeIf { it.isNotBlank() }
                if (customUrl != null) {
                    RpcImage.ExternalImage(customUrl)
                } else {
                    createRpcImage(resolvedImages.artistResolvedId)
                        ?: createRpcImage(resolvedImages.artistOriginalUrl)
                        ?: song.artists.firstOrNull()?.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { RpcImage.ExternalImage(it) }
                }
            }
            else -> {
                createRpcImage(resolvedImages.artistResolvedId)
                    ?: createRpcImage(resolvedImages.artistOriginalUrl)
            }
        }
        
        val largeImageStr = when (finalLargeImage) {
            is RpcImage.DiscordImage -> "discord:${finalLargeImage.image.take(30)}"
            is RpcImage.ExternalImage -> "external:${finalLargeImage.image.take(30)}"
            null -> "null"
        }
        val smallImageStr = when (finalSmallImage) {
            is RpcImage.DiscordImage -> "discord:${finalSmallImage.image.take(30)}"
            is RpcImage.ExternalImage -> "external:${finalSmallImage.image.take(30)}"
            null -> "null"
        }
        Timber.tag(logtag).d("Final images - large: %s, small: %s", largeImageStr, smallImageStr)

        val largeTextSource = (context.dataStore[DiscordLargeTextSourceKey] ?: "album").lowercase()
        val resolvedLargeText = when (largeTextSource) {
            "song" -> translatedMap["{song}"] ?: song.song.title
            "artist" -> translatedMap["{artist}"] ?: song.artists.firstOrNull()?.name
            "album" -> translatedMap["{album}"] ?: song.song.albumName ?: song.album?.title ?: song.song.title
            "app" -> context.getString(R.string.app_name)
            "custom" -> (context.dataStore[DiscordLargeTextCustomKey] ?: "").ifBlank { null }
            "dontshow" -> null
            else -> translatedMap["{album}"] ?: song.song.albumName ?: song.album?.title
        }

        // Derive small text from small image type, prefer translated values when available
        val sendSmallText = if (isPaused) {
            context.getString(R.string.discord_paused)
        } else {
            val baseSmallText = when (smallImageTypePref.lowercase()) {
                "song" -> translatedMap["{song}"] ?: song.song.title
                "artist" -> translatedMap["{artist}"] ?: song.artists.firstOrNull()?.name
                "thumbnail", "album" -> translatedMap["{album}"] ?: song.song.albumName ?: song.album?.title
                "appicon", "app" -> context.getString(R.string.app_name)
                "custom" -> song.artists.firstOrNull()?.name
                else -> translatedMap["{artist}"] ?: song.artists.firstOrNull()?.name
            }
            "$baseSmallText on ArchiveTune"
        }

        val applicationIdToSend = APPLICATION_ID

        val platformPref = context.dataStore[DiscordActivityPlatformKey] ?: "desktop"
        this.setPlatform(platformPref)

        val hasValidDuration = (song.song.duration ?: -1) > 0
        val durationMs = if (hasValidDuration) song.song.duration * 1000L else 0L
        
        val sendStartTime: Long?
        val sendEndTime: Long?
        val sendSince: Long?
        
        when {
            isPaused && showWhenPaused -> {
                sendStartTime = null
                sendEndTime = null
                sendSince = currentTime
            }
            isPaused && !showWhenPaused -> {
                sendStartTime = null
                sendEndTime = null
                sendSince = null
            }
            !isPaused && hasValidDuration -> {
                sendStartTime = calculatedStartTime
                sendEndTime = calculatedStartTime + durationMs
                sendSince = null
            }
            else -> {
                sendStartTime = null
                sendEndTime = null
                sendSince = null
            }
        }
        
        Timber.tag(logtag).d(
            "Timestamps - isPaused=%s, showWhenPaused=%s, hasValidDuration=%s, startTime=%s, endTime=%s, since=%s",
            isPaused, showWhenPaused, hasValidDuration, sendStartTime, sendEndTime, sendSince
        )

        val safeStatus = when (statusPref.lowercase()) {
            "online", "idle", "dnd", "invisible" -> statusPref
            else -> "online"
        }

        try {
            // Only include the buttons argument when we actually have buttons to send.
            if (finalButtons.isNotEmpty()) {
                this.refreshRPC(
                    name = activityName.removeSuffix(" Debug"),
                    details = activityDetails,
                    state = activityState,
                    detailsUrl = baseSongUrl,
                    largeImage = finalLargeImage,
                    smallImage = finalSmallImage,
                    largeText = resolvedLargeText ?: "",
                    smallText = sendSmallText ?: "",
                    buttons = finalButtons,
                    type = resolvedType,
                    statusDisplayType = StatusDisplayType.STATE,
                    since = sendSince,
                    startTime = sendStartTime,
                    endTime = sendEndTime,
                    applicationId = applicationIdToSend,
                    status = safeStatus
                )
            } else {
                this.refreshRPC(
                    name = activityName.removeSuffix(" Debug"),
                    details = activityDetails,
                    state = activityState,
                    detailsUrl = baseSongUrl,
                    largeImage = finalLargeImage,
                    smallImage = finalSmallImage,
                    largeText = resolvedLargeText ?: "",
                    smallText = sendSmallText ?: "",
                    type = resolvedType,
                    statusDisplayType = StatusDisplayType.STATE,
                    since = sendSince,
                    startTime = sendStartTime,
                    endTime = sendEndTime,
                    applicationId = applicationIdToSend,
                    status = safeStatus
                )
            }
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
