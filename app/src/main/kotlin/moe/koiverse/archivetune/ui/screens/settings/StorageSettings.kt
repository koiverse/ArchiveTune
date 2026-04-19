/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.MaxCanvasCacheSizeKey
import moe.koiverse.archivetune.constants.MaxImageCacheSizeKey
import moe.koiverse.archivetune.constants.MaxSongCacheSizeKey
import moe.koiverse.archivetune.constants.SmartTrimmerKey
import moe.koiverse.archivetune.extensions.directorySizeBytes
import moe.koiverse.archivetune.extensions.tryOrNull
import moe.koiverse.archivetune.ui.component.ActionPromptDialog
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.ListPreference
import moe.koiverse.archivetune.ui.component.LocalPreferenceInGroup
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.SwitchPreference
import moe.koiverse.archivetune.ui.player.CanvasArtworkPlaybackCache
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.ui.utils.formatFileSize
import moe.koiverse.archivetune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return

    val downloadCacheDir = remember { context.filesDir.resolve("download") }
    val playerCacheDir = remember { context.filesDir.resolve("exoplayer") }

    val coroutineScope = rememberCoroutineScope()
    val (smartTrimmer, onSmartTrimmerChange) = rememberPreference(
        key = SmartTrimmerKey,
        defaultValue = false,
    )
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512,
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = 1024,
    )
    val (maxCanvasCacheSize, onMaxCanvasCacheSizeChange) = rememberPreference(
        key = MaxCanvasCacheSizeKey,
        defaultValue = 256,
    )
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }
    var clearCanvasCacheDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember { mutableStateOf(imageDiskCache.size) }
    var playerCacheSize by remember { mutableStateOf(0L) }
    var downloadCacheSize by remember { mutableStateOf(0L) }
    var canvasCacheSize by remember { mutableStateOf(CanvasArtworkPlaybackCache.size()) }
    val totalTrackedStorage by remember(imageCacheSize, playerCacheSize, downloadCacheSize) {
        derivedStateOf { imageCacheSize + playerCacheSize + downloadCacheSize }
    }
    val imageCacheProgress by animateFloatAsState(
        targetValue = if (imageDiskCache.maxSize > 0) {
            (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f)
        } else 0f,
        label = "imageCacheProgress",
    )
    val maxSongCacheSizeBytes = if (maxSongCacheSize > 0) maxSongCacheSize * 1024 * 1024L else 0L
    val playerCacheProgress by animateFloatAsState(
        targetValue = if (maxSongCacheSizeBytes > 0) {
            (playerCacheSize.toFloat() / maxSongCacheSizeBytes).coerceIn(0f, 1f)
        } else 0f,
        label = "playerCacheProgress",
    )
    val canvasCacheProgress by animateFloatAsState(
        targetValue = if (maxCanvasCacheSize > 0) {
            (canvasCacheSize.toFloat() / maxCanvasCacheSize).coerceIn(0f, 1f)
        } else 0f,
        label = "canvasCacheProgress",
    )

    val songLimitText = when (maxSongCacheSize) {
        0 -> stringResource(R.string.storage_cache_disabled_detail)
        -1 -> stringResource(R.string.storage_cache_unlimited_detail)
        else -> stringResource(R.string.storage_limit_format, formatFileSize(maxSongCacheSizeBytes))
    }
    val imageLimitText = if (maxImageCacheSize == 0) {
        stringResource(R.string.storage_cache_disabled_detail)
    } else {
        stringResource(R.string.storage_limit_format, formatFileSize(imageDiskCache.maxSize))
    }
    val canvasLimitText = if (maxCanvasCacheSize == 0) {
        stringResource(R.string.storage_cache_disabled_detail)
    } else {
        stringResource(
            R.string.storage_limit_format,
            stringResource(R.string.canvas_cache_items, maxCanvasCacheSize),
        )
    }

    val isSmartTrimmerAvailable = maxImageCacheSize != 0 || maxSongCacheSize != 0
    LaunchedEffect(isSmartTrimmerAvailable) {
        if (!isSmartTrimmerAvailable && smartTrimmer) onSmartTrimmerChange(false)
    }

    LaunchedEffect(maxImageCacheSize) {
        if (maxImageCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                imageDiskCache.clear()
                moe.koiverse.archivetune.utils.ArtworkStorage.clear(context)
            }
        }
    }
    LaunchedEffect(maxSongCacheSize) {
        if (maxSongCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                playerCache.keys.forEach { key ->
                    playerCache.removeResource(key)
                }
            }
        }
    }
    LaunchedEffect(maxCanvasCacheSize) {
        CanvasArtworkPlaybackCache.setMaxSize(maxCanvasCacheSize)
        if (maxCanvasCacheSize == 0) {
            CanvasArtworkPlaybackCache.clear()
        }
    }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(500)
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache, playerCacheDir) {
        while (isActive) {
            delay(500)
            playerCacheSize =
                withContext(Dispatchers.IO) {
                    val cacheSpace = tryOrNull { playerCache.cacheSpace } ?: 0L
                    if (cacheSpace == 0L) playerCacheDir.directorySizeBytes() else cacheSpace
                }
        }
    }
    LaunchedEffect(downloadCache, downloadCacheDir) {
        while (isActive) {
            delay(500)
            downloadCacheSize =
                withContext(Dispatchers.IO) {
                    val cacheSpace = tryOrNull { downloadCache.cacheSpace } ?: 0L
                    if (cacheSpace == 0L) downloadCacheDir.directorySizeBytes() else cacheSpace
                }
        }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(500)
            canvasCacheSize = CanvasArtworkPlaybackCache.size()
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.storage)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SettingsDimensions.ScreenHorizontalPadding)
                .padding(top = innerPadding.calculateTopPadding() + 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SectionSpacing),
        ) {
            StorageSummaryCard(
                trackedStorageText = stringResource(
                    R.string.storage_tracked_storage,
                    formatFileSize(totalTrackedStorage),
                ),
                downloadValue = formatFileSize(downloadCacheSize),
                songValue = formatFileSize(playerCacheSize),
                imageValue = formatFileSize(imageCacheSize),
                canvasValue = stringResource(R.string.canvas_cache_items, canvasCacheSize),
            )

            StorageSectionCard(
                icon = R.drawable.style,
                title = stringResource(R.string.storage_automation_title),
                value = if (smartTrimmer && isSmartTrimmerAvailable) {
                    stringResource(R.string.enabled)
                } else {
                    stringResource(R.string.disabled)
                },
                supportingText = if (isSmartTrimmerAvailable) {
                    stringResource(R.string.smart_trimmer_description)
                } else {
                    stringResource(R.string.storage_trimmer_unavailable)
                },
            ) {
                SwitchPreference(
                    title = { Text(stringResource(R.string.smart_trimmer)) },
                    description = if (isSmartTrimmerAvailable) {
                        stringResource(R.string.smart_trimmer_description)
                    } else {
                        stringResource(R.string.storage_trimmer_unavailable)
                    },
                    checked = smartTrimmer && isSmartTrimmerAvailable,
                    onCheckedChange = onSmartTrimmerChange,
                    isEnabled = isSmartTrimmerAvailable,
                )
            }

            StorageSectionCard(
                icon = R.drawable.ic_download,
                title = stringResource(R.string.downloaded_songs),
                value = formatFileSize(downloadCacheSize),
                supportingText = stringResource(R.string.storage_downloads_detail),
            ) {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_all_downloads)) },
                    onClick = { clearDownloads = true },
                )
            }

            StorageSectionCard(
                icon = R.drawable.ic_music,
                title = stringResource(R.string.song_cache),
                value = formatFileSize(playerCacheSize),
                supportingText = songLimitText,
                progress = if (maxSongCacheSize > 0) playerCacheProgress else null,
            ) {
                ListPreference(
                    title = { Text(stringResource(R.string.max_cache_size)) },
                    selectedValue = maxSongCacheSize,
                    values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1),
                    valueText = {
                        when (it) {
                            0 -> stringResource(R.string.disable)
                            -1 -> stringResource(R.string.unlimited)
                            else -> formatFileSize(it * 1024 * 1024L)
                        }
                    },
                    onValueSelected = onMaxSongCacheSizeChange,
                )
                PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_song_cache)) },
                    onClick = { clearCacheDialog = true },
                )
            }

            StorageSectionCard(
                icon = R.drawable.image,
                title = stringResource(R.string.image_cache),
                value = formatFileSize(imageCacheSize),
                supportingText = imageLimitText,
                progress = if (maxImageCacheSize > 0) imageCacheProgress else null,
            ) {
                ListPreference(
                    title = { Text(stringResource(R.string.max_cache_size)) },
                    selectedValue = maxImageCacheSize,
                    values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192),
                    valueText = {
                        when (it) {
                            0 -> stringResource(R.string.disable)
                            else -> formatFileSize(it * 1024 * 1024L)
                        }
                    },
                    onValueSelected = onMaxImageCacheSizeChange,
                )
                PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_image_cache)) },
                    onClick = { clearImageCacheDialog = true },
                )
            }

            StorageSectionCard(
                icon = R.drawable.motion_photos_on,
                title = stringResource(R.string.canvas_cache),
                value = stringResource(R.string.canvas_cache_items, canvasCacheSize),
                supportingText = canvasLimitText,
                note = stringResource(R.string.storage_canvas_detail),
                progress = if (maxCanvasCacheSize > 0) canvasCacheProgress else null,
            ) {
                ListPreference(
                    title = { Text(stringResource(R.string.max_cache_size)) },
                    selectedValue = maxCanvasCacheSize,
                    values = listOf(0, 64, 128, 256, 512, 1024),
                    valueText = {
                        when (it) {
                            0 -> stringResource(R.string.disable)
                            else -> stringResource(R.string.canvas_cache_items, it)
                        }
                    },
                    onValueSelected = onMaxCanvasCacheSizeChange,
                )
                PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_canvas_cache)) },
                    onClick = { clearCanvasCacheDialog = true },
                )
            }
        }
    }

    if (clearDownloads) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_all_downloads),
            onDismiss = { clearDownloads = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    downloadCache.keys.forEach { key ->
                        downloadCache.removeResource(key)
                    }
                }
                clearDownloads = false
            },
            onCancel = { clearDownloads = false },
            content = {
                Text(text = stringResource(R.string.clear_downloads_dialog))
            },
        )
    }

    if (clearCacheDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_song_cache),
            onDismiss = { clearCacheDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    playerCache.keys.forEach { key ->
                        playerCache.removeResource(key)
                    }
                }
                clearCacheDialog = false
            },
            onCancel = { clearCacheDialog = false },
            content = {
                Text(text = stringResource(R.string.clear_song_cache_dialog))
            },
        )
    }

    if (clearImageCacheDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_image_cache),
            onDismiss = { clearImageCacheDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    imageDiskCache.clear()
                    moe.koiverse.archivetune.utils.ArtworkStorage.clear(context)
                }
                clearImageCacheDialog = false
            },
            onCancel = { clearImageCacheDialog = false },
            content = {
                Text(text = stringResource(R.string.clear_image_cache_dialog))
            },
        )
    }

    if (clearCanvasCacheDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_canvas_cache),
            onDismiss = { clearCanvasCacheDialog = false },
            onConfirm = {
                CanvasArtworkPlaybackCache.clear()
                clearCanvasCacheDialog = false
            },
            onCancel = { clearCanvasCacheDialog = false },
            content = {
                Text(text = stringResource(R.string.clear_canvas_cache_dialog))
            },
        )
    }
}

@Composable
private fun StorageSummaryCard(
    trackedStorageText: String,
    downloadValue: String,
    songValue: String,
    imageValue: String,
    canvasValue: String,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
        ),
        start = Offset.Zero,
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(SettingsDimensions.HeroCardCornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = trackedStorageText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.storage_summary_supporting),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StorageMetricChip(
                        icon = R.drawable.ic_download,
                        value = downloadValue,
                        modifier = Modifier.weight(1f),
                    )
                    StorageMetricChip(
                        icon = R.drawable.ic_music,
                        value = songValue,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StorageMetricChip(
                        icon = R.drawable.image,
                        value = imageValue,
                        modifier = Modifier.weight(1f),
                    )
                    StorageMetricChip(
                        icon = R.drawable.motion_photos_on,
                        value = canvasValue,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageMetricChip(
    icon: Int,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StorageSectionCard(
    icon: Int,
    title: String,
    value: String,
    supportingText: String,
    progress: Float? = null,
    note: String? = null,
    actions: @Composable ColumnScope.() -> Unit,
) {
    val progressLabel = progress?.let { "${(it * 100).toInt()}%" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius),
    ) {
        Column(
            modifier = Modifier.padding(SettingsDimensions.CardInternalPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(SettingsDimensions.RowIconCornerRadius),
                    modifier = Modifier.padding(end = 14.dp),
                ) {
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                progressLabel?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape,
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            if (progress != null) {
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            note?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            CompositionLocalProvider(LocalPreferenceInGroup provides true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions()
                }
            }
        }
    }
}
