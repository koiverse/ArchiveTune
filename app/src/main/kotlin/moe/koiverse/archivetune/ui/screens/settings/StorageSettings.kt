package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.StrokeCap
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
import moe.koiverse.archivetune.constants.MaxImageCacheSizeKey
import moe.koiverse.archivetune.constants.MaxSongCacheSizeKey
import moe.koiverse.archivetune.constants.SmartTrimmerKey
import moe.koiverse.archivetune.extensions.directorySizeBytes
import moe.koiverse.archivetune.extensions.tryOrNull
import moe.koiverse.archivetune.ui.component.ActionPromptDialog
import moe.koiverse.archivetune.ui.component.DefaultDialog
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.ListPreference
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.SwitchPreference
import moe.koiverse.archivetune.ui.component.PreferenceGroupTitle
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
        defaultValue = false
    )
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = 1024
    )
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember {
        mutableStateOf(imageDiskCache.size)
    }
    var playerCacheSize by remember {
        mutableStateOf(0L)
    }
    var downloadCacheSize by remember {
        mutableStateOf(0L)
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

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.smart_trimmer)) },
            description = stringResource(R.string.smart_trimmer_description),
            checked = smartTrimmer && isSmartTrimmerAvailable,
            onCheckedChange = onSmartTrimmerChange,
            isEnabled = isSmartTrimmerAvailable,
        )

        // --- Section: Downloads ---
        CacheCard(
            icon = R.drawable.ic_download,
            title = stringResource(R.string.downloaded_songs),
            description = stringResource(R.string.size_used, formatFileSize(downloadCacheSize)),
            progress = null,
            actions = {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_all_downloads)) },
                    onClick = { clearDownloads = true },
                )
            }
        )

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
                }
            )
        }

        // --- Section: Song cache ---
        CacheCard(
            icon = R.drawable.ic_music,
            title = stringResource(R.string.song_cache),
            description = if (maxSongCacheSize == -1) {
                stringResource(R.string.size_used, formatFileSize(playerCacheSize))
            } else {
                "${formatFileSize(playerCacheSize)} / ${formatFileSize(maxSongCacheSize * 1024 * 1024L)}"
            },
            progress = if (maxSongCacheSize > 0) playerCacheProgress else null,
            actions = {
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
        )

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
                }
            )
        }

        // --- Section: Image cache ---
        CacheCard(
            icon = R.drawable.image,
            title = stringResource(R.string.image_cache),
            description = if (maxImageCacheSize > 0) {
                "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}"
            } else {
                stringResource(R.string.disable)
            },
            progress = if (maxImageCacheSize > 0) imageCacheProgress else null,
            actions = {
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
        )

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
                }
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.storage)) },
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

@Composable
fun CacheCard(
    icon: Int,
    title: String,
    description: String,
    progress: Float?,
    actions: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    modifier = Modifier.padding(end = 12.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                ) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (progress != null) {
                Spacer(Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                    // percent label
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.padding(4.dp))
            actions()
        }
    }
}
