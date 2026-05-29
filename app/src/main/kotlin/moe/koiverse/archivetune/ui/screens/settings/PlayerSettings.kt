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





package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.ArtistSeparatorsKey
import moe.koiverse.archivetune.constants.ExternalDownloaderEnabledKey
import moe.koiverse.archivetune.constants.ExternalDownloaderPackageKey
import moe.koiverse.archivetune.constants.AudioNormalizationKey
import moe.koiverse.archivetune.constants.AudioOffload
import moe.koiverse.archivetune.constants.AudioQuality
import moe.koiverse.archivetune.constants.AudioQualityKey
import moe.koiverse.archivetune.constants.LowDataModeKey
import moe.koiverse.archivetune.constants.AutoDownloadOnLikeKey
import moe.koiverse.archivetune.constants.AutoStartOnBluetoothKey
import moe.koiverse.archivetune.constants.AutoSkipNextOnErrorKey
import moe.koiverse.archivetune.constants.PauseOnDeviceMuteKey
import moe.koiverse.archivetune.constants.PermanentShuffleKey
import moe.koiverse.archivetune.constants.PersistentQueueKey

import moe.koiverse.archivetune.constants.SkipSilenceKey
import moe.koiverse.archivetune.constants.StopMusicOnTaskClearKey
import moe.koiverse.archivetune.constants.WakelockKey
import moe.koiverse.archivetune.constants.HistoryDuration
import moe.koiverse.archivetune.constants.CrossfadeDurationKey
import moe.koiverse.archivetune.constants.CrossfadeEnabledKey
import moe.koiverse.archivetune.constants.CrossfadeGaplessKey
import moe.koiverse.archivetune.constants.PlayerStreamClient
import moe.koiverse.archivetune.constants.PlayerStreamClientKey
import moe.koiverse.archivetune.constants.SeekExtraSeconds
import moe.koiverse.archivetune.ui.component.ArtistSeparatorsDialog
import moe.koiverse.archivetune.ui.component.TagsManagementDialog
import moe.koiverse.archivetune.ui.component.TextFieldDialog
import moe.koiverse.archivetune.ui.component.EnumListPreference
import moe.koiverse.archivetune.ui.component.ListPreference
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.PreferenceGroup
import moe.koiverse.archivetune.ui.component.SliderPreference
import moe.koiverse.archivetune.ui.component.CrossfadeSliderPreference
import moe.koiverse.archivetune.ui.component.SwitchPreference
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.LocalDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (playerStreamClient, onPlayerStreamClientChange) = rememberEnumPreference(
        PlayerStreamClientKey,
        defaultValue = PlayerStreamClient.ANDROID_VR
    )
    val (lowDataMode, onLowDataModeChange) = rememberPreference(
        LowDataModeKey,
        defaultValue = true
    )
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (permanentShuffle, onPermanentShuffleChange) = rememberPreference(
        PermanentShuffleKey,
        defaultValue = false
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = true
    )
    val (audioOffload, onAudioOffloadChange) = rememberPreference(
        AudioOffload,
        defaultValue = false
    )

    val (seekExtraSeconds, onSeekExtraSeconds) = rememberPreference(
        SeekExtraSeconds,
        defaultValue = false
    )

    val (autoDownloadOnLike, onAutoDownloadOnLikeChange) = rememberPreference(
        AutoDownloadOnLikeKey,
        defaultValue = false
    )
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(
        AutoSkipNextOnErrorKey,
        defaultValue = false
    )
    val (pauseOnDeviceMute, onPauseOnDeviceMuteChange) = rememberPreference(
        PauseOnDeviceMuteKey,
        defaultValue = false
    )
    val (autoStartOnBluetooth, onAutoStartOnBluetoothChange) = rememberPreference(
        AutoStartOnBluetoothKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = false
    )
    val (historyDuration, onHistoryDurationChange) = rememberPreference(
        HistoryDuration,
        defaultValue = 30f
    )

    val (crossfadeEnabled, onCrossfadeEnabledChange) = rememberPreference(
        CrossfadeEnabledKey,
        defaultValue = false
    )
    val (crossfadeDurationSeconds, onCrossfadeDurationSecondsChange) = rememberPreference(
        CrossfadeDurationKey,
        defaultValue = 5f
    )
    val (crossfadeGapless, onCrossfadeGaplessChange) = rememberPreference(
        CrossfadeGaplessKey,
        defaultValue = true
    )

    val (artistSeparators, onArtistSeparatorsChange) = rememberPreference(
        ArtistSeparatorsKey,
        defaultValue = ",;/&"
    )
    val (externalDownloaderEnabled, onExternalDownloaderEnabledChange) = rememberPreference(
        ExternalDownloaderEnabledKey,
        defaultValue = false
    )
    val (externalDownloaderPackage, onExternalDownloaderPackageChange) = rememberPreference(
        ExternalDownloaderPackageKey,
        defaultValue = ""
    )

    val (wakelockEnabled, onWakelockChange) = rememberPreference(
        WakelockKey,
        defaultValue = false
    )

    var showArtistSeparatorsDialog by remember { mutableStateOf(false) }
    var showTagsManagementDialog by remember { mutableStateOf(false) }
    var showExternalDownloaderPackageDialog by remember { mutableStateOf(false) }
    val database = LocalDatabase.current

    if (showArtistSeparatorsDialog) {
        ArtistSeparatorsDialog(
            currentSeparators = artistSeparators,
            onDismiss = { showArtistSeparatorsDialog = false },
            onSave = { newSeparators ->
                onArtistSeparatorsChange(newSeparators)
                showArtistSeparatorsDialog = false
            }
        )
    }

    if (showTagsManagementDialog) {
        TagsManagementDialog(
            database = database,
            onDismiss = { showTagsManagementDialog = false }
        )
    }

    if (showExternalDownloaderPackageDialog) {
        TextFieldDialog(
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(externalDownloaderPackage),
            onDone = { pkg ->
                onExternalDownloaderPackageChange(pkg)
                showExternalDownloaderPackageDialog = false
            },
            onDismiss = { showExternalDownloaderPackageDialog = false },
            singleLine = true,
            maxLines = 1,
        )
    }


    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        PreferenceGroup(title = stringResource(R.string.player)) {
            item {
                EnumListPreference(
                    title = { Text(stringResource(R.string.audio_quality)) },
                    icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                    selectedValue = audioQuality,
                    onValueSelected = onAudioQualityChange,
                    valueText = {
                        when (it) {
                            AudioQuality.HIGHEST -> stringResource(R.string.audio_quality_max)
                            AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                            AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                            AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                        }
                    }
                )
            }

            item {
                ListPreference(
                    title = { Text(stringResource(R.string.player_stream_client)) },
                    description = stringResource(R.string.player_stream_client_desc),
                    icon = { Icon(painterResource(R.drawable.integration), null) },
                    selectedValue = playerStreamClient,
                    values = remember { listOf(PlayerStreamClient.ANDROID_VR, PlayerStreamClient.WEB_REMIX) },
                    onValueSelected = onPlayerStreamClientChange,
                    valueText = {
                        when (it) {
                            PlayerStreamClient.ANDROID_VR -> stringResource(R.string.player_stream_client_android_vr)
                            else -> stringResource(R.string.player_stream_client_web_remix)
                        }
                    },
                    valueDescription = {
                        when (it) {
                            PlayerStreamClient.ANDROID_VR -> stringResource(R.string.player_stream_client_android_vr_desc)
                            else -> stringResource(R.string.player_stream_client_web_remix_desc)
                        }
                    },
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.low_data_mode_title)) },
                    description = stringResource(R.string.low_data_mode_description),
                    icon = { Icon(painterResource(R.drawable.android_cell), null) },
                    checked = lowDataMode,
                    onCheckedChange = onLowDataModeChange
                )
            }

            item {
                SliderPreference(
                    title = { Text(stringResource(R.string.history_duration)) },
                    icon = { Icon(painterResource(R.drawable.history), null) },
                    value = historyDuration,
                    onValueChange = onHistoryDurationChange,
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.audio_crossfade_title)) },
                    description = stringResource(R.string.audio_crossfade_description),
                    icon = { Icon(painterResource(R.drawable.animation), null) },
                    checked = crossfadeEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            onAudioOffloadChange(false)
                        }
                        onCrossfadeEnabledChange(enabled)
                    },
                )
            }

            item {
                CrossfadeSliderPreference(
                    valueSeconds = crossfadeDurationSeconds,
                    onValueChange = onCrossfadeDurationSecondsChange,
                    isEnabled = crossfadeEnabled,
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.crossfade_gapless_title)) },
                    description = stringResource(R.string.crossfade_gapless_description),
                    icon = { Icon(painterResource(R.drawable.fast_forward), null) },
                    checked = crossfadeGapless,
                    onCheckedChange = onCrossfadeGaplessChange,
                    isEnabled = crossfadeEnabled,
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.skip_silence)) },
                    icon = { Icon(painterResource(R.drawable.fast_forward), null) },
                    checked = skipSilence,
                    onCheckedChange = onSkipSilenceChange,
                    isEnabled = !audioOffload,
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.audio_normalization)) },
                    icon = { Icon(painterResource(R.drawable.volume_up), null) },
                    checked = audioNormalization,
                    onCheckedChange = onAudioNormalizationChange
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.audio_offload)) },
                    description = stringResource(R.string.audio_offload_desc),
                    icon = { Icon(painterResource(R.drawable.speed), null) },
                    checked = audioOffload,
                    onCheckedChange = { enabled ->
                        onAudioOffloadChange(enabled)
                        if (enabled) {
                            onSkipSilenceChange(false)
                            onCrossfadeEnabledChange(false)
                        }
                    }
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.seek_seconds_addup)) },
                    description = stringResource(R.string.seek_seconds_addup_description),
                    icon = { Icon(painterResource(R.drawable.arrow_forward), null) },
                    checked = seekExtraSeconds,
                    onCheckedChange = onSeekExtraSeconds
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.pause_on_device_mute)) },
                    description = stringResource(R.string.pause_on_device_mute_desc),
                    icon = { Icon(painterResource(R.drawable.volume_off), null) },
                    checked = pauseOnDeviceMute,
                    onCheckedChange = onPauseOnDeviceMuteChange
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.auto_start_on_bluetooth)) },
                    description = stringResource(R.string.auto_start_on_bluetooth_desc),
                    icon = { Icon(painterResource(R.drawable.bluetooth), null) },
                    checked = autoStartOnBluetooth,
                    onCheckedChange = onAutoStartOnBluetoothChange
                )
            }
        }

        PreferenceGroup(title = stringResource(R.string.queue)) {
            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.persistent_queue)) },
                    description = stringResource(R.string.persistent_queue_desc),
                    icon = { Icon(painterResource(R.drawable.queue_music), null) },
                    checked = persistentQueue,
                    onCheckedChange = onPersistentQueueChange
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.permanent_shuffle)) },
                    description = stringResource(R.string.permanent_shuffle_desc),
                    icon = { Icon(painterResource(R.drawable.shuffle), null) },
                    checked = permanentShuffle,
                    onCheckedChange = onPermanentShuffleChange
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.auto_download_on_like)) },
                    description = stringResource(R.string.auto_download_on_like_desc),
                    icon = { Icon(painterResource(R.drawable.download), null) },
                    checked = autoDownloadOnLike,
                    onCheckedChange = onAutoDownloadOnLikeChange
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
                    description = stringResource(R.string.auto_skip_next_on_error_desc),
                    icon = { Icon(painterResource(R.drawable.skip_next), null) },
                    checked = autoSkipNextOnError,
                    onCheckedChange = onAutoSkipNextOnErrorChange
                )
            }
        }

        PreferenceGroup(title = stringResource(R.string.misc)) {
            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                    icon = { Icon(painterResource(R.drawable.clear_all), null) },
                    checked = stopMusicOnTaskClear,
                    onCheckedChange = onStopMusicOnTaskClearChange
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.wakelock)) },
                    description = stringResource(R.string.wakelock_desc),
                    icon = { Icon(painterResource(R.drawable.bolt), null) },
                    checked = wakelockEnabled,
                    onCheckedChange = onWakelockChange
                )
            }

            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.artist_separators)) },
                    description = artistSeparators.map { "\"$it\"" }.joinToString("  "),
                    icon = { Icon(painterResource(R.drawable.artist), null) },
                    onClick = { showArtistSeparatorsDialog = true }
                )
            }

            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.manage_playlist_tags)) },
                    description = stringResource(R.string.manage_playlist_tags_desc),
                    icon = { Icon(painterResource(R.drawable.style), null) },
                    onClick = { showTagsManagementDialog = true }
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.external_downloader)) },
                    description = stringResource(R.string.external_downloader_desc),
                    icon = { Icon(painterResource(R.drawable.download), null) },
                    checked = externalDownloaderEnabled,
                    onCheckedChange = onExternalDownloaderEnabledChange
                )
            }

            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.external_downloader_package)) },
                    description = externalDownloaderPackage.ifEmpty { stringResource(R.string.external_downloader_package_desc) },
                    icon = { Icon(painterResource(R.drawable.integration), null) },
                    onClick = { showExternalDownloaderPackageDialog = true },
                    isEnabled = externalDownloaderEnabled
                )
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.player_and_audio)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )
}
