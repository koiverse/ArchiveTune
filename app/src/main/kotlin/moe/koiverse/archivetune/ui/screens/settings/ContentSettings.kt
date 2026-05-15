/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.*
import moe.koiverse.archivetune.paxsenix.models.ProviderStats
import moe.koiverse.archivetune.paxsenix.models.PaxsenixStats
import moe.koiverse.archivetune.ui.component.*
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.utils.setAppLocale
import moe.koiverse.archivetune.viewmodels.ContentSettingsViewModel
import moe.koiverse.archivetune.viewmodels.PaxsenixStatsState
import java.net.Proxy
import java.util.Locale
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

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
        val statsState by viewModel.paxsenixStatsState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.fetchPaxsenixStats()
        }

        PaxsenixStatsDialog(
            state = statsState,
            onDismiss = { showPaxsenixStatsDialog = false },
            onRetry = { viewModel.fetchPaxsenixStats() },
        )
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
    val (enableUnisonLyrics, onEnableUnisonLyricsChange) = rememberPreference(key = EnableUnisonLyricsKey, defaultValue = true)
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
    val (lyricsLineBlur, onLyricsLineBlurChange) = rememberPreference(LyricsLineBlurKey, defaultValue = true)
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
            title = { Text(stringResource(R.string.enable_unison_lyrics)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableUnisonLyrics,
            onCheckedChange = onEnableUnisonLyricsChange,
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
                PreferredLyricsProvider.UNISON,
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
                    PreferredLyricsProvider.UNISON -> "Unison"
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
            title = { Text(stringResource(R.string.lyrics_line_blur)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsLineBlur,
            onCheckedChange = onLyricsLineBlurChange,
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

private enum class PaxsenixServerStatus { Operational, Degraded, Down }

private fun successRateToStatus(rate: Float): PaxsenixServerStatus = when {
    rate >= 90f -> PaxsenixServerStatus.Operational
    rate >= 70f -> PaxsenixServerStatus.Degraded
    else -> PaxsenixServerStatus.Down
}

private fun formatUptimeSeconds(seconds: Double): String {
    val total = seconds.toLong()
    val days = total / 86400L
    val hours = (total % 86400L) / 3600L
    val minutes = (total % 3600L) / 60L
    return when {
        days > 0L -> "${days}d ${hours}h ${minutes}m"
        hours > 0L -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

@Composable
private fun PaxsenixStatsDialog(
    state: PaxsenixStatsState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.paxsenix_stats)) },
        icon = { Icon(painterResource(R.drawable.stats), contentDescription = null) },
        buttons = {
            if (state is PaxsenixStatsState.Error) {
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.retry))
                }
            } else {
                TextButton(onClick = { uriHandler.openUri("https://lyrics.paxsenix.org/") }) {
                    Text(stringResource(R.string.visit_website))
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    ) {
        when (state) {
            PaxsenixStatsState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            }

            PaxsenixStatsState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.error),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = stringResource(R.string.paxsenix_stats_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is PaxsenixStatsState.Success -> {
                PaxsenixStatsContent(stats = state.stats)
            }
        }
    }
}

@Composable
private fun PaxsenixStatsContent(stats: PaxsenixStats) {
    val overallRate = remember(stats.overallSuccessRate) {
        stats.overallSuccessRate.trimEnd('%').toFloatOrNull() ?: 0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PaxsenixStatusBar(successRate = overallRate)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.uptime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatUptimeSeconds(stats.uptimeSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.total_requests),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stats.totalRequests.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.success_rate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stats.overallSuccessRate,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        if (stats.providers.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = stringResource(R.string.providers),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                stats.providers.forEach { (name, providerStats) ->
                    key(name) {
                        PaxsenixProviderRow(name = name, providerStats = providerStats)
                    }
                }
            }
        }

        if (stats.requestLog.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = stringResource(R.string.recent_requests),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                stats.requestLog.take(5).forEach { entry ->
                    key(entry.timestamp + entry.endpoint) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (entry.success)
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                else
                                    MaterialTheme.colorScheme.errorContainer,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.endpoint,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = entry.provider,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (entry.success)
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else
                                            MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                                Text(
                                    text = "${entry.responseTimeMs.toInt()}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entry.success)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaxsenixStatusBar(successRate: Float) {
    val status = remember(successRate) { successRateToStatus(successRate) }
    val statusColor = when (status) {
        PaxsenixServerStatus.Operational -> Color(0xFF4CAF50)
        PaxsenixServerStatus.Degraded -> Color(0xFFFF9800)
        PaxsenixServerStatus.Down -> MaterialTheme.colorScheme.error
    }
    val statusLabel = when (status) {
        PaxsenixServerStatus.Operational -> stringResource(R.string.paxsenix_status_operational)
        PaxsenixServerStatus.Degraded -> stringResource(R.string.paxsenix_status_degraded)
        PaxsenixServerStatus.Down -> stringResource(R.string.paxsenix_status_down)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                text = "${successRate.toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                color = statusColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PaxsenixProviderRow(name: String, providerStats: ProviderStats) {
    val rate = remember(providerStats.successRate) {
        providerStats.successRate.trimEnd('%').toFloatOrNull() ?: 0f
    }
    val status = remember(rate) { successRateToStatus(rate) }
    val dotColor = when (status) {
        PaxsenixServerStatus.Operational -> Color(0xFF4CAF50)
        PaxsenixServerStatus.Degraded -> Color(0xFFFF9800)
        PaxsenixServerStatus.Down -> MaterialTheme.colorScheme.error
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${providerStats.hits} hits",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = providerStats.successRate,
                style = MaterialTheme.typography.labelSmall,
                color = dotColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
