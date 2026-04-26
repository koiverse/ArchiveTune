/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.os.LocaleList
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.toLowerCase
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import androidx.compose.material3.LoadingIndicator
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.*
import moe.koiverse.archivetune.ui.component.*
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.utils.setAppLocale
import moe.koiverse.archivetune.viewmodels.ContentSettingsViewModel
import java.net.Proxy
import java.util.Locale
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ContentSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var showClearLyricsDialog by remember { mutableStateOf(false) }
    var showPaxsenixStatsDialog by remember { mutableStateOf(false) }

    if (showClearLyricsDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_lyrics_cache),
            onDismiss = { showClearLyricsDialog = false },
            onConfirm = {
                viewModel.clearLyricsCache()
                showClearLyricsDialog = false
            },
            onCancel = { showClearLyricsDialog = false }
        ) {
            Text(stringResource(R.string.clear_lyrics_cache_confirm))
        }
    }

    if (showPaxsenixStatsDialog) {
        val stats by viewModel.paxsenixStats.collectAsState()
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        
        LaunchedEffect(Unit) {
            viewModel.fetchPaxsenixStats()
        }

        DefaultDialog(
            onDismiss = { showPaxsenixStatsDialog = false },
            title = { Text(stringResource(R.string.paxsenix_stats)) },
            icon = { Icon(painterResource(R.drawable.stats), null) },
            buttons = {
                TextButton(onClick = { uriHandler.openUri("https://lyrics.paxsenix.org/") }) {
                    Text("Visit Website")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showPaxsenixStatsDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
            if (stats == null) {
                LoadingIndicator()
            } else {
                val currentStats = stats!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${stringResource(R.string.uptime)}: ${currentStats.uptimeSeconds.toInt()}s")
                    Text("${stringResource(R.string.total_requests)}: ${currentStats.totalRequests}")
                    Text("${stringResource(R.string.success_rate)}: ${currentStats.overallSuccessRate}")
                    
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.providers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    currentStats.providers.forEach { (name, pStats) ->
                        Text("$name: ${pStats.hits} hits (${pStats.successRate} success)")
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Recent Requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    currentStats.requestLog.take(10).forEach { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (entry.success) 
                                    MaterialTheme.colorScheme.surfaceVariant 
                                else 
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("${entry.endpoint} [${entry.provider}]", style = MaterialTheme.typography.bodySmall)
                                Text("Time: ${entry.responseTimeMs}ms | Status: ${if (entry.success) "OK" else "Failed"}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // Used only before Android 13
    val (appLanguage, onAppLanguageChange) = rememberPreference(key = AppLanguageKey, defaultValue = SYSTEM_DEFAULT)

    val (contentLanguage, onContentLanguageChange) = rememberPreference(key = ContentLanguageKey, defaultValue = "system")
    val (contentCountry, onContentCountryChange) = rememberPreference(key = ContentCountryKey, defaultValue = "system")
    val (playlistSuggestionSource, onPlaylistSuggestionSourceChange) =
        rememberEnumPreference(
            key = PlaylistSuggestionSourceKey,
            defaultValue = PlaylistSuggestionSource.BOTH,
        )
    val (hideExplicit, onHideExplicitChange) = rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (hideVideo, onHideVideoChange) = rememberPreference(key = HideVideoKey, defaultValue = false)
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (enableBetterLyrics, onEnableBetterLyricsChange) = rememberPreference(key = EnableBetterLyricsKey, defaultValue = true)
    val (enableSimpMusicLyrics, onEnableSimpMusicLyricsChange) =
        rememberPreference(key = EnableSimpMusicLyricsKey, defaultValue = true)
    val (enablePaxsenixLyrics, onEnablePaxsenixLyricsChange) =
        rememberPreference(key = EnablePaxsenixLyricsKey, defaultValue = true)
    val (enablePaxsenixAppleMusicLyrics, onEnablePaxsenixAppleMusicLyricsChange) =
        rememberPreference(key = EnablePaxsenixAppleMusicLyricsKey, defaultValue = true)
    val (enablePaxsenixNeteaseLyrics, onEnablePaxsenixNeteaseLyricsChange) =
        rememberPreference(key = EnablePaxsenixNeteaseLyricsKey, defaultValue = true)
    val (enablePaxsenixSpotifyLyrics, onEnablePaxsenixSpotifyLyricsChange) =
        rememberPreference(key = EnablePaxsenixSpotifyLyricsKey, defaultValue = true)
    val (enablePaxsenixMusixmatchLyrics, onEnablePaxsenixMusixmatchLyricsChange) =
        rememberPreference(key = EnablePaxsenixMusixmatchLyricsKey, defaultValue = true)
    val (enablePaxsenixKuGouLyrics, onEnablePaxsenixKuGouLyricsChange) =
        rememberPreference(key = EnablePaxsenixKuGouLyricsKey, defaultValue = true)
    val (preferredProvider, onPreferredProviderChange) =
        rememberEnumPreference(
            key = PreferredLyricsProviderKey,
            defaultValue = PreferredLyricsProvider.LRCLIB,
        )
    val (lyricsRomanizeJapanese, onLyricsRomanizeJapaneseChange) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (lyricsRomanizeKorean, onLyricsRomanizeKoreanChange) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val (lyricsRomanizeChinese, onLyricsRomanizeChineseChange) = rememberPreference(LyricsRomanizeChineseKey, defaultValue = true)
    val (lyricsRomanizeHindi, onLyricsRomanizeHindiChange) = rememberPreference(LyricsRomanizeHindiKey, defaultValue = true)
    val (lyricsRomanizeOtherLanguages, onLyricsRomanizeOtherLanguagesChange) = rememberPreference(LyricsRomanizeOtherLanguagesKey, defaultValue = true)
    val (preloadQueueLyricsEnabled, onPreloadQueueLyricsEnabledChange) = rememberPreference(PreloadQueueLyricsEnabledKey, defaultValue = true)
    val (queueLyricsPreloadCount, onQueueLyricsPreloadCountChange) = rememberPreference(QueueLyricsPreloadCountKey, defaultValue = 1)
    val (lengthTop, onLengthTopChange) = rememberPreference(key = TopSize, defaultValue = "50")
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(key = QuickPicksKey, defaultValue = QuickPicks.QUICK_PICKS)

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroupTitle(title = stringResource(R.string.general))
        ListPreference(
            title = { Text(stringResource(R.string.content_language)) },
            icon = { Icon(painterResource(R.drawable.language), null) },
            selectedValue = contentLanguage,
            values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
            valueText = {
                LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            },
            onValueSelected = { newValue ->
                val locale = Locale.getDefault()
                val languageTag = locale.toLanguageTag().replace("-Hant", "")
 
                YouTube.locale = YouTube.locale.copy(
                    hl = newValue.takeIf { it != SYSTEM_DEFAULT }
                        ?: locale.language.takeIf { it in LanguageCodeToName }
                        ?: languageTag.takeIf { it in LanguageCodeToName }
                        ?: "en"
                )
 
                onContentLanguageChange(newValue)
            }
        )
        ListPreference(
            title = { Text(stringResource(R.string.content_country)) },
            icon = { Icon(painterResource(R.drawable.location_on), null) },
            selectedValue = contentCountry,
            values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
            valueText = {
                CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            },
            onValueSelected = { newValue ->
                val locale = Locale.getDefault()
 
                YouTube.locale = YouTube.locale.copy(
                    gl = newValue.takeIf { it != SYSTEM_DEFAULT }
                        ?: locale.country.takeIf { it in CountryCodeToName }
                        ?: "US"
                )
 
                onContentCountryChange(newValue)
           }
        )
        ListPreference(
            title = { Text(stringResource(R.string.you_might_like_source)) },
            icon = { Icon(painterResource(R.drawable.playlist_play), null) },
            selectedValue = playlistSuggestionSource,
            values = listOf(
                PlaylistSuggestionSource.PLAYLIST_TITLE,
                PlaylistSuggestionSource.PLAYLIST_CONTENT,
                PlaylistSuggestionSource.BOTH,
            ),
            valueText = {
                when (it) {
                    PlaylistSuggestionSource.PLAYLIST_TITLE -> stringResource(R.string.playlist_suggestion_source_title)
                    PlaylistSuggestionSource.PLAYLIST_CONTENT -> stringResource(R.string.playlist_suggestion_source_content)
                    PlaylistSuggestionSource.BOTH -> stringResource(R.string.playlist_suggestion_source_both)
                }
            },
            onValueSelected = onPlaylistSuggestionSourceChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.hide_explicit)) },
            icon = { Icon(painterResource(R.drawable.explicit), null) },
            checked = hideExplicit,
            onCheckedChange = onHideExplicitChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.hide_video)) },
            icon = { Icon(painterResource(R.drawable.slow_motion_video), null) },
            checked = hideVideo,
            onCheckedChange = onHideVideoChange,
        )

        PreferenceGroupTitle(title = stringResource(R.string.app_language))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.app_language)) },
                icon = { Icon(painterResource(R.drawable.language), null) },
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APP_LOCALE_SETTINGS,
                            "package:${context.packageName}".toUri()
                        )
                    )
                }
            )
        }
        // Support for Android versions before Android 13
        else {
            ListPreference(
                title = { Text(stringResource(R.string.app_language)) },
                icon = { Icon(painterResource(R.drawable.language), null) },
                selectedValue = appLanguage,
                values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
                valueText = {
                    LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
                },
                onValueSelected = { langTag ->
                    val newLocale = langTag
                        .takeUnless { it == SYSTEM_DEFAULT }
                        ?.let { Locale.forLanguageTag(it) }
                        ?: Locale.getDefault()

                    onAppLanguageChange(langTag)
                    setAppLocale(context, newLocale)

                }
            )
        }

        PreferenceGroupTitle(title = stringResource(R.string.lyrics))
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_lrclib)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableLrclib,
            onCheckedChange = onEnableLrclibChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_kugou)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableKugou,
            onCheckedChange = onEnableKugouChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_betterlyrics)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableBetterLyrics,
            onCheckedChange = onEnableBetterLyricsChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_simpmusic_lyrics)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableSimpMusicLyrics,
            onCheckedChange = onEnableSimpMusicLyricsChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_paxsenix_lyrics)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enablePaxsenixLyrics,
            onCheckedChange = onEnablePaxsenixLyricsChange,
        )
        if (enablePaxsenixLyrics) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.paxsenix_stats)) },
                icon = { Icon(painterResource(R.drawable.stats), null) },
                onClick = { showPaxsenixStatsDialog = true },
            )
            SwitchPreference(
                title = { Text("Paxsenix: Apple Music") },
                icon = { Icon(painterResource(R.drawable.lyrics), null) },
                checked = enablePaxsenixAppleMusicLyrics,
                onCheckedChange = onEnablePaxsenixAppleMusicLyricsChange,
            )
            SwitchPreference(
                title = { Text("Paxsenix: NetEase") },
                icon = { Icon(painterResource(R.drawable.lyrics), null) },
                checked = enablePaxsenixNeteaseLyrics,
                onCheckedChange = onEnablePaxsenixNeteaseLyricsChange,
            )
            SwitchPreference(
                title = { Text("Paxsenix: Spotify") },
                icon = { Icon(painterResource(R.drawable.lyrics), null) },
                checked = enablePaxsenixSpotifyLyrics,
                onCheckedChange = onEnablePaxsenixSpotifyLyricsChange,
            )
            SwitchPreference(
                title = { Text("Paxsenix: Musixmatch") },
                icon = { Icon(painterResource(R.drawable.lyrics), null) },
                checked = enablePaxsenixMusixmatchLyrics,
                onCheckedChange = onEnablePaxsenixMusixmatchLyricsChange,
            )
            SwitchPreference(
                title = { Text("Paxsenix: KuGou") },
                icon = { Icon(painterResource(R.drawable.lyrics), null) },
                checked = enablePaxsenixKuGouLyrics,
                onCheckedChange = onEnablePaxsenixKuGouLyricsChange,
            )
        }
        ListPreference(
            title = { Text(stringResource(R.string.set_first_lyrics_provider)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            selectedValue = preferredProvider,
            values = listOf(
                PreferredLyricsProvider.LRCLIB,
                PreferredLyricsProvider.KUGOU,
                PreferredLyricsProvider.BETTER_LYRICS,
                PreferredLyricsProvider.SIMPMUSIC,
                PreferredLyricsProvider.PAXSENIX_APPLE_MUSIC,
                PreferredLyricsProvider.PAXSENIX_NETEASE,
                PreferredLyricsProvider.PAXSENIX_SPOTIFY,
                PreferredLyricsProvider.PAXSENIX_MUSIXMATCH,
                PreferredLyricsProvider.PAXSENIX_KUGOU,
            ),
            valueText = {
                when (it) {
                    PreferredLyricsProvider.LRCLIB -> "LrcLib"
                    PreferredLyricsProvider.KUGOU -> "KuGou"
                    PreferredLyricsProvider.BETTER_LYRICS -> "BetterLyrics"
                    PreferredLyricsProvider.SIMPMUSIC -> "SimpMusic"
                    PreferredLyricsProvider.PAXSENIX_APPLE_MUSIC -> "Paxsenix: Apple Music"
                    PreferredLyricsProvider.PAXSENIX_NETEASE -> "Paxsenix: NetEase"
                    PreferredLyricsProvider.PAXSENIX_SPOTIFY -> "Paxsenix: Spotify"
                    PreferredLyricsProvider.PAXSENIX_MUSIXMATCH -> "Paxsenix: Musixmatch"
                    PreferredLyricsProvider.PAXSENIX_KUGOU -> "Paxsenix: KuGou"
                }
            },
            onValueSelected = onPreferredProviderChange,
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.clear_lyrics_cache)) },
            icon = { Icon(painterResource(R.drawable.delete), null) },
            onClick = { showClearLyricsDialog = true },
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_japanese)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsRomanizeJapanese,
            onCheckedChange = onLyricsRomanizeJapaneseChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_korean)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsRomanizeKorean,
            onCheckedChange = onLyricsRomanizeKoreanChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_chinese)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsRomanizeChinese,
            onCheckedChange = onLyricsRomanizeChineseChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_hindi)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsRomanizeHindi,
            onCheckedChange = onLyricsRomanizeHindiChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_other_languages)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsRomanizeOtherLanguages,
            onCheckedChange = onLyricsRomanizeOtherLanguagesChange,
        )
        // Queue lyrics pre-load settings
        SwitchPreference(
            title = { Text(stringResource(R.string.preload_queue_lyrics)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = preloadQueueLyricsEnabled,
            onCheckedChange = onPreloadQueueLyricsEnabledChange,
        )
        if (preloadQueueLyricsEnabled) {
            NumberPickerPreference(
                title = { Text(stringResource(R.string.queue_lyrics_preload_count)) },
                icon = { Icon(painterResource(R.drawable.lyrics), null) },
                value = queueLyricsPreloadCount,
                onValueChange = onQueueLyricsPreloadCountChange,
                minValue = 0,
                maxValue = 10,
                valueText = { if (it == 0) "Off" else it.toString() },
            )
        }

        PreferenceGroupTitle(title = stringResource(R.string.misc))
        EditTextPreference(
            title = { Text(stringResource(R.string.top_length)) },
            icon = { Icon(painterResource(R.drawable.trending_up), null) },
            value = lengthTop,
            isInputValid = { it.toIntOrNull()?.let { num -> num > 0 } == true },
            onValueChange = onLengthTopChange,
        )
        ListPreference(
            title = { Text(stringResource(R.string.set_quick_picks)) },
            icon = { Icon(painterResource(R.drawable.home_outlined), null) },
            selectedValue = quickPicks,
            values = listOf(QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN, QuickPicks.DONT_SHOW),
            valueText = {
                when (it) {
                    QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                    QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                    QuickPicks.DONT_SHOW -> stringResource(R.string.dont_show)
                }
            },
            onValueSelected = onQuickPicksChange,
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.content)) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}
