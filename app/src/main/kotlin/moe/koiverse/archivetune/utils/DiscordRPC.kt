/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.utils

import android.content.Context
import me.bush.translator.Language
import me.bush.translator.Translator
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.DiscordActivityButton1CustomUrlKey
import moe.koiverse.archivetune.constants.DiscordActivityButton1EnabledKey
import moe.koiverse.archivetune.constants.DiscordActivityButton1LabelKey
import moe.koiverse.archivetune.constants.DiscordActivityButton1UrlSourceKey
import moe.koiverse.archivetune.constants.DiscordActivityButton2CustomUrlKey
import moe.koiverse.archivetune.constants.DiscordActivityButton2EnabledKey
import moe.koiverse.archivetune.constants.DiscordActivityButton2LabelKey
import moe.koiverse.archivetune.constants.DiscordActivityButton2UrlSourceKey
import moe.koiverse.archivetune.constants.DiscordActivityDetailsKey
import moe.koiverse.archivetune.constants.DiscordActivityNameKey
import moe.koiverse.archivetune.constants.DiscordActivityPlatformKey
import moe.koiverse.archivetune.constants.DiscordActivityStateKey
import moe.koiverse.archivetune.constants.DiscordActivityTypeKey
import moe.koiverse.archivetune.constants.DiscordLargeImageCustomUrlKey
import moe.koiverse.archivetune.constants.DiscordLargeImageTypeKey
import moe.koiverse.archivetune.constants.DiscordLargeTextCustomKey
import moe.koiverse.archivetune.constants.DiscordLargeTextSourceKey
import moe.koiverse.archivetune.constants.DiscordPresenceStatusKey
import moe.koiverse.archivetune.constants.DiscordShowWhenPausedKey
import moe.koiverse.archivetune.constants.DiscordSmallImageCustomUrlKey
import moe.koiverse.archivetune.constants.DiscordSmallImageTypeKey
import moe.koiverse.archivetune.constants.EnableTranslatorKey
import moe.koiverse.archivetune.constants.TranslatorContextsKey
import moe.koiverse.archivetune.constants.TranslatorTargetLangKey
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.discord.DiscordActivityPlatform
import moe.koiverse.archivetune.discord.DiscordActivityType
import moe.koiverse.archivetune.discord.DiscordOnlineStatus
import moe.koiverse.archivetune.discord.DiscordPresenceActivity
import moe.koiverse.archivetune.discord.DiscordPresenceAssets
import moe.koiverse.archivetune.discord.DiscordPresenceButton
import moe.koiverse.archivetune.discord.DiscordPresenceTimestamps
import moe.koiverse.archivetune.discord.DiscordSocialPresenceClient
import moe.koiverse.archivetune.discord.DiscordStatusDisplayType
import timber.log.Timber

class DiscordRPC(
    private val context: Context,
    private val accessToken: String,
) {
    companion object {
        private const val PAUSE_IMAGE_URL =
            "https://raw.githubusercontent.com/koiverse/ArchiveTune/main/fastlane/metadata/android/en-US/images/RPC/pause_icon.png"
        private const val APP_ICON_URL =
            "https://raw.githubusercontent.com/koiverse/ArchiveTune/main/fastlane/metadata/android/en-US/images/icon.png"
        private const val TAG = "DiscordRPC"
    }

    private val translationCache: MutableMap<String, String> = mutableMapOf()
    private var lastSongId: String? = null

    fun isRpcRunning(): Boolean = DiscordSocialPresenceClient.isStarted

    suspend fun stopActivity() {
        DiscordSocialPresenceClient.clearPresence().getOrElse {
            Timber.tag(TAG).v(it, "clearPresence failed")
        }
    }

    suspend fun closeRPC() {
        DiscordSocialPresenceClient.close().getOrElse {
            Timber.tag(TAG).v(it, "close failed")
        }
    }

    suspend fun updateSong(
        song: Song,
        currentPlaybackTimeMillis: Long,
        isPaused: Boolean = false,
    ) = runCatching {
        if (lastSongId != song.song.id) {
            translationCache.clear()
            DiscordImageResolver.clearCache()
            lastSongId = song.song.id
        }

        val showWhenPaused = context.dataStore[DiscordShowWhenPausedKey] ?: false
        if (isPaused && !showWhenPaused) {
            Timber.tag(TAG).v("Paused and show-when-paused is disabled; clearing Discord presence")
            stopActivity()
            return@runCatching
        }

        val translatedMap = translateSongFields(song)
        val appName = context.getString(R.string.app_name)
        val namePref = context.dataStore[DiscordActivityNameKey] ?: "APP"
        val detailsPref = context.dataStore[DiscordActivityDetailsKey] ?: "SONG"
        val statePref = context.dataStore[DiscordActivityStateKey] ?: "ARTIST"

        val activityName = sourceValue(
            pref = namePref,
            song = song,
            translatedMap = translatedMap,
            default = appName,
        )

        val activityDetails = sourceValue(
            pref = detailsPref,
            song = song,
            translatedMap = translatedMap,
            default = song.song.title.ifBlank { appName },
        ).toDiscordText(maxLength = 128, fallback = song.song.title.ifBlank { appName })

        val activityState = sourceValue(
            pref = statePref,
            song = song,
            translatedMap = translatedMap,
            default = song.artists.joinToString { it.name }.ifBlank { appName },
        ).toDiscordText(maxLength = 128, fallback = appName)

        val baseSongUrl = "https://music.youtube.com/watch?v=${song.song.id}"
        val resolvedImages = DiscordImageResolver.resolveImagesForSong(context, song)
        val largeImageType = context.dataStore[DiscordLargeImageTypeKey] ?: "thumbnail"
        val largeImageCustomUrl = context.dataStore[DiscordLargeImageCustomUrlKey] ?: ""
        val smallImageType = context.dataStore[DiscordSmallImageTypeKey] ?: "artist"
        val smallImageCustomUrl = context.dataStore[DiscordSmallImageCustomUrlKey] ?: ""

        val largeImage = when (largeImageType.lowercase()) {
            "appicon" -> APP_ICON_URL
            else -> DiscordImageResolver.buildImageUrl(
                imageType = largeImageType,
                customUrl = largeImageCustomUrl,
                resolvedImages = resolvedImages,
                song = song,
            )
        }

        val smallImage = when {
            isPaused -> PAUSE_IMAGE_URL
            smallImageType.equals("appicon", ignoreCase = true) -> APP_ICON_URL
            else -> DiscordImageResolver.buildImageUrl(
                imageType = smallImageType,
                customUrl = smallImageCustomUrl,
                resolvedImages = resolvedImages,
                song = song,
            )
        }

        val largeText = resolveLargeText(song, translatedMap)
        val smallText = if (isPaused) {
            context.getString(R.string.discord_paused)
        } else {
            resolveSmallText(song, translatedMap, smallImageType, appName)
        }

        val buttons = resolveButtons(song)
        val activityType = DiscordActivityType.fromPreference(
            context.dataStore[DiscordActivityTypeKey] ?: "LISTENING",
        )
        val platform = DiscordActivityPlatform.fromPreference(
            context.dataStore[DiscordActivityPlatformKey] ?: "android",
        )
        val status = DiscordOnlineStatus.fromPreference(
            context.dataStore[DiscordPresenceStatusKey] ?: "online",
        )

        val timestamps = buildTimestamps(
            song = song,
            currentPlaybackTimeMillis = currentPlaybackTimeMillis,
            isPaused = isPaused,
            showWhenPaused = showWhenPaused,
        )

        val activity = DiscordPresenceActivity(
            applicationId = BuildConfig.DISCORD_APPLICATION_ID_LONG,
            name = activityName.toDiscordText(maxLength = 128, fallback = appName),
            type = activityType,
            details = activityDetails,
            state = activityState,
            detailsUrl = baseSongUrl.toDiscordUrl(),
            assets = DiscordPresenceAssets(
                largeImage = largeImage.toDiscordUrl(),
                largeText = largeText.toDiscordText(maxLength = 128),
                largeUrl = largeImage.toDiscordUrl(),
                smallImage = smallImage.toDiscordUrl(),
                smallText = smallText.toDiscordText(maxLength = 128),
                smallUrl = baseSongUrl.toDiscordUrl(),
            ),
            buttons = buttons,
            timestamps = timestamps,
            statusDisplayType = DiscordStatusDisplayType.State,
            supportedPlatforms = platform,
            onlineStatus = status,
        )

        DiscordSocialPresenceClient.updatePresence(
            context = context,
            accessToken = accessToken,
            activity = activity,
        ).getOrThrow()

        Timber.tag(TAG).i(
            "Updated Discord presence via Social SDK name=%s details=%s state=%s",
            activityName,
            activityDetails,
            activityState,
        )
    }

    suspend fun refreshActivity(
        song: Song,
        currentPlaybackTimeMillis: Long,
        isPaused: Boolean = false,
    ) = runCatching {
        updateSong(song, currentPlaybackTimeMillis, isPaused).getOrThrow()
    }

    private suspend fun translateSongFields(song: Song): Map<String, String> {
        val translatorEnabled = context.dataStore[EnableTranslatorKey] ?: false
        if (!translatorEnabled) return emptyMap()

        val contextList = (context.dataStore[TranslatorContextsKey] ?: "{song}")
            .split(",")
            .map { it.trim() }
        val targetLang = context.dataStore[TranslatorTargetLangKey] ?: "ENGLISH"
        val rawMap = mapOf(
            "{song}" to song.song.title,
            "{artist}" to song.artists.joinToString { it.name },
            "{album}" to (song.song.albumName ?: song.album?.title ?: ""),
        )
        val translatedMap = mutableMapOf<String, String>()

        runCatching {
            val translator = Translator()
            contextList.forEach { key ->
                val value = rawMap[key]?.takeIf { it.isNotBlank() } ?: return@forEach
                val cacheKey = "${song.song.id}:$key:$targetLang"
                val cached = translationCache[cacheKey]
                if (cached != null) {
                    translatedMap[key] = cached
                } else {
                    val translated = runCatching {
                        translator.translateBlocking(
                            value,
                            Language.valueOf(targetLang.uppercase()),
                        ).translatedText
                    }.getOrElse {
                        Timber.tag(TAG).e(it, "Translation failed for %s", key)
                        value
                    }
                    translationCache[cacheKey] = translated
                    translatedMap[key] = translated
                }
            }
        }.onFailure {
            Timber.tag(TAG).e(it, "Translator init failed")
        }

        return translatedMap
    }

    private fun sourceValue(
        pref: String,
        song: Song,
        translatedMap: Map<String, String>,
        default: String,
    ): String =
        when (pref.uppercase()) {
            "ARTIST" -> translatedMap["{artist}"] ?: song.artists.firstOrNull()?.name ?: default
            "ALBUM" -> translatedMap["{album}"] ?: song.song.albumName ?: song.album?.title ?: default
            "SONG" -> translatedMap["{song}"] ?: song.song.title.ifBlank { default }
            "APP" -> context.getString(R.string.app_name)
            else -> default
        }

    private suspend fun resolveLargeText(
        song: Song,
        translatedMap: Map<String, String>,
    ): String? =
        when ((context.dataStore[DiscordLargeTextSourceKey] ?: "album").lowercase()) {
            "song" -> translatedMap["{song}"] ?: song.song.title
            "artist" -> translatedMap["{artist}"] ?: song.artists.firstOrNull()?.name
            "album" -> translatedMap["{album}"] ?: song.song.albumName ?: song.album?.title ?: song.song.title
            "app" -> context.getString(R.string.app_name)
            "custom" -> (context.dataStore[DiscordLargeTextCustomKey] ?: "").ifBlank { null }
            "dontshow" -> null
            else -> translatedMap["{album}"] ?: song.song.albumName ?: song.album?.title
        }

    private fun resolveSmallText(
        song: Song,
        translatedMap: Map<String, String>,
        smallImageType: String,
        appName: String,
    ): String? {
        val base = when (smallImageType.lowercase()) {
            "song" -> translatedMap["{song}"] ?: song.song.title
            "artist" -> translatedMap["{artist}"] ?: song.artists.firstOrNull()?.name
            "thumbnail", "album" -> translatedMap["{album}"] ?: song.song.albumName ?: song.album?.title
            "appicon", "app" -> appName
            "dontshow", "none" -> null
            else -> translatedMap["{artist}"] ?: song.artists.firstOrNull()?.name
        }

        return base?.let { "$it on $appName" }
    }

    private suspend fun resolveButtons(song: Song): List<DiscordPresenceButton> {
        val button1Label = context.dataStore[DiscordActivityButton1LabelKey] ?: "Listen on YouTube Music"
        val button1Enabled = context.dataStore[DiscordActivityButton1EnabledKey] ?: true
        val button2Label = context.dataStore[DiscordActivityButton2LabelKey] ?: "Go to ArchiveTune"
        val button2Enabled = context.dataStore[DiscordActivityButton2EnabledKey] ?: true
        val button1UrlSource = context.dataStore[DiscordActivityButton1UrlSourceKey] ?: "songurl"
        val button1CustomUrl = context.dataStore[DiscordActivityButton1CustomUrlKey] ?: ""
        val button2UrlSource = context.dataStore[DiscordActivityButton2UrlSourceKey] ?: "custom"
        val button2CustomUrl = context.dataStore[DiscordActivityButton2CustomUrlKey]
            ?: "https://github.com/koiverse/ArchiveTune"

        return buildList {
            if (button1Enabled) {
                val url = resolveUrl(button1UrlSource, song, button1CustomUrl)
                if (button1Label.isNotBlank() && !url.isNullOrBlank()) {
                    add(DiscordPresenceButton(button1Label.toButtonLabel(), url))
                }
            }

            if (button2Enabled) {
                val url = resolveUrl(button2UrlSource, song, button2CustomUrl)
                if (button2Label.isNotBlank() && !url.isNullOrBlank()) {
                    add(DiscordPresenceButton(button2Label.toButtonLabel(), url))
                }
            }
        }.take(2)
    }

    private fun resolveUrl(source: String, song: Song, custom: String): String? =
        when (source.lowercase()) {
            "songurl" -> "https://music.youtube.com/watch?v=${song.song.id}"
            "artisturl" -> song.artists.firstOrNull()?.id?.let { "https://music.youtube.com/channel/$it" }
            "albumurl" -> song.album?.playlistId?.let { "https://music.youtube.com/playlist?list=$it" }
            "custom" -> custom.normalizeUrl()
            else -> null
        }?.toDiscordUrl()

    private fun buildTimestamps(
        song: Song,
        currentPlaybackTimeMillis: Long,
        isPaused: Boolean,
        showWhenPaused: Boolean,
    ): DiscordPresenceTimestamps {
        if (isPaused && showWhenPaused) {
            return DiscordPresenceTimestamps(
                startEpochSeconds = System.currentTimeMillis() / 1000L,
            )
        }

        val durationSeconds = song.song.duration.toLong()
        if (isPaused || durationSeconds <= 0L) {
            return DiscordPresenceTimestamps()
        }

        val startMs = System.currentTimeMillis() - currentPlaybackTimeMillis.coerceAtLeast(0L)
        return DiscordPresenceTimestamps(
            startEpochSeconds = startMs / 1000L,
            endEpochSeconds = (startMs + durationSeconds * 1000L) / 1000L,
        )
    }

    private fun String.normalizeUrl(): String? {
        val trimmed = trim()
        if (trimmed.isBlank()) return null
        return if (
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun String?.toDiscordText(
        maxLength: Int,
        fallback: String? = null,
    ): String? {
        val value = this?.trim().orEmpty().ifBlank { fallback?.trim().orEmpty() }
        return value
            .takeIf { it.length >= 2 }
            ?.take(maxLength)
    }

    private fun String?.toDiscordUrl(): String? =
        this?.normalizeUrl()?.take(256)

    private fun String.toButtonLabel(): String =
        trim().take(32).ifBlank { context.getString(R.string.app_name) }
}
