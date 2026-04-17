/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */


package moe.koiverse.archivetune.ui.screens.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.CONTENT_TYPE_HEADER
import moe.koiverse.archivetune.constants.CONTENT_TYPE_SONG
import moe.koiverse.archivetune.extensions.toMediaItem
import moe.koiverse.archivetune.extensions.togglePlayPause
import moe.koiverse.archivetune.playback.queues.ListQueue
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.component.SongListItem
import moe.koiverse.archivetune.ui.component.SortHeader
import moe.koiverse.archivetune.ui.menu.SongMenu
import moe.koiverse.archivetune.viewmodels.LocalSongsScanState
import moe.koiverse.archivetune.viewmodels.LocalSongsViewModel
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun LocalSongScreen(
    navController: NavController,
    viewModel: LocalSongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val listState = rememberLazyListState()
    val scanSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showScanSheet by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortDescending by rememberSaveable { mutableStateOf(true) }
    var sortTypeName by rememberSaveable { mutableStateOf(LocalSongSortType.MODIFIED.name) }
    val sortType = remember(sortTypeName) { LocalSongSortType.valueOf(sortTypeName) }

    val storagePermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    var hasStoragePermission by remember(storagePermission) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasStoragePermission = granted
    }

    val collator = remember {
        Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
    }
    val bottomContentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 20.dp
    val statusText = when {
        scanState.isScanning -> stringResource(R.string.scanning_device)
        scanState.errorMessage != null -> stringResource(R.string.local_songs_scan_failed)
        !hasStoragePermission -> stringResource(R.string.local_songs_permission_body)
        scanState.lastSummary != null -> stringResource(
            R.string.local_songs_scan_summary,
            scanState.lastSummary.scannedSongs,
            scanState.lastSummary.removedSongs,
        )

        else -> stringResource(R.string.local_songs_ready_desc)
    }

    val visibleSongs by remember(songs, query, sortType, sortDescending, collator) {
        derivedStateOf {
            val normalizedQuery = query.trim()
            val filteredSongs = if (normalizedQuery.isBlank()) {
                songs
            } else {
                songs.filter { song ->
                    song.song.title.contains(normalizedQuery, ignoreCase = true) ||
                        song.song.albumName.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
                        song.artists.any { artist -> artist.name.contains(normalizedQuery, ignoreCase = true) }
                }
            }

            val sortedSongs = when (sortType) {
                LocalSongSortType.MODIFIED -> filteredSongs.sortedBy { song ->
                    song.song.dateModified ?: LocalDateTime.MIN
                }

                LocalSongSortType.NAME -> filteredSongs.sortedWith(compareBy(collator) { song -> song.song.title })
                LocalSongSortType.ARTIST -> filteredSongs.sortedWith(
                    compareBy(collator) { song ->
                        song.artists.joinToString(separator = "") { artist -> artist.name }
                    },
                )

                LocalSongSortType.ALBUM -> filteredSongs.sortedWith(
                    compareBy(collator) { song -> song.song.albumName.orEmpty() },
                )
            }

            if (sortDescending) sortedSongs.asReversed() else sortedSongs
        }
    }

    val queueItems = remember(visibleSongs) { visibleSongs.map { it.toMediaItem() } }

    if (showScanSheet) {
        LocalSongScanSheet(
            hasStoragePermission = hasStoragePermission,
            scanState = scanState,
            sheetState = scanSheetState,
            onDismiss = { showScanSheet = false },
            onPrimaryAction = {
                if (hasStoragePermission) {
                    viewModel.scanDevice()
                } else {
                    permissionLauncher.launch(storagePermission)
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.local_history),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showScanSheet = true }) {
                        Icon(
                            painter = painterResource(R.drawable.settings),
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = bottomContentPadding,
            ),
        ) {
            item(
                key = "overview",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                LocalSongOverviewCard(
                    songCount = songs.size,
                    statusText = statusText,
                    hasStoragePermission = hasStoragePermission,
                    isScanning = scanState.isScanning,
                )
            }

            item(
                key = "controls",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                LocalSongControlsCard(
                    query = query,
                    onQueryChange = { query = it },
                    sortType = sortType,
                    sortDescending = sortDescending,
                    visibleSongCount = visibleSongs.size,
                    onSortTypeChange = { sortTypeName = it.name },
                    onSortDescendingChange = { sortDescending = it },
                )
            }

            if (visibleSongs.isEmpty()) {
                item(
                    key = "empty",
                    contentType = CONTENT_TYPE_HEADER,
                ) {
                    LocalSongEmptyState(query = query)
                }
            } else {
                item(
                    key = "divider",
                    contentType = CONTENT_TYPE_HEADER,
                ) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                itemsIndexed(
                    items = visibleSongs,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> CONTENT_TYPE_SONG },
                ) { index, song ->
                    SongListItem(
                        song = song,
                        showInLibraryIcon = false,
                        showDownloadIcon = false,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = if (query.isBlank()) {
                                                    context.getString(R.string.local_history)
                                                } else {
                                                    context.getString(R.string.queue_searched_songs)
                                                },
                                                items = queueItems,
                                                startIndex = index,
                                            ),
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            )
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalSongOverviewCard(
    songCount: Int,
    statusText: String,
    hasStoragePermission: Boolean,
    isScanning: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.library_music),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.local_history),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LocalSongBadge(
                    iconRes = R.drawable.music_note,
                    text = pluralStringResource(R.plurals.n_song, songCount, songCount),
                )
                LocalSongBadge(
                    iconRes = if (isScanning) R.drawable.sync else R.drawable.settings,
                    text = if (isScanning) {
                        stringResource(R.string.scanning_device)
                    } else if (hasStoragePermission) {
                        stringResource(R.string.permission_status_allowed)
                    } else {
                        stringResource(R.string.not_allowed)
                    },
                )
            }
        }
    }
}

@Composable
private fun LocalSongBadge(
    iconRes: Int,
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LocalSongControlsCard(
    query: String,
    onQueryChange: (String) -> Unit,
    sortType: LocalSongSortType,
    sortDescending: Boolean,
    visibleSongCount: Int,
    onSortTypeChange: (LocalSongSortType) -> Unit,
    onSortDescendingChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_library),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(22.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SortHeader(
                    sortType = sortType,
                    sortDescending = sortDescending,
                    onSortTypeChange = onSortTypeChange,
                    onSortDescendingChange = onSortDescendingChange,
                    sortTypeText = { selectedSort ->
                        when (selectedSort) {
                            LocalSongSortType.MODIFIED -> R.string.sort_by_last_updated
                            LocalSongSortType.NAME -> R.string.sort_by_name
                            LocalSongSortType.ARTIST -> R.string.sort_by_artist
                            LocalSongSortType.ALBUM -> R.string.sort_by_album
                        }
                    },
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = pluralStringResource(R.plurals.n_song, visibleSongCount, visibleSongCount),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun LocalSongEmptyState(
    query: String,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Icon(
                painter = painterResource(if (query.isBlank()) R.drawable.music_note else R.drawable.search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = if (query.isBlank()) {
                    stringResource(R.string.local_songs_empty_title)
                } else {
                    stringResource(R.string.local_songs_no_matches_title)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (query.isBlank()) {
                    stringResource(R.string.local_songs_empty_desc)
                } else {
                    stringResource(R.string.local_songs_no_matches_desc)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalSongScanSheet(
    hasStoragePermission: Boolean,
    scanState: LocalSongsScanState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    val primaryText = if (hasStoragePermission) {
        stringResource(R.string.scan_device)
    } else {
        stringResource(R.string.allow)
    }
    val bodyText = when {
        scanState.isScanning -> stringResource(R.string.scanning_device)
        scanState.errorMessage != null -> stringResource(R.string.local_songs_scan_failed)
        !hasStoragePermission -> stringResource(R.string.local_songs_permission_body)
        scanState.lastSummary != null -> stringResource(
            R.string.local_songs_scan_summary,
            scanState.lastSummary.scannedSongs,
            scanState.lastSummary.removedSongs,
        )

        else -> stringResource(R.string.local_songs_ready_desc)
    }
    val permissionStatusText = if (hasStoragePermission) {
        stringResource(R.string.permission_status_allowed)
    } else {
        stringResource(R.string.not_allowed)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.local_songs_scan_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.local_songs_scan_subtitle),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (scanState.isScanning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.scanning_device),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.library_music),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.permission_storage_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.permission_storage_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = permissionStatusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.local_songs_latest_scan),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = bodyText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onPrimaryAction,
                enabled = !scanState.isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (scanState.isScanning) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        painter = painterResource(if (hasStoragePermission) R.drawable.sync else R.drawable.library_music),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = stringResource(R.string.close))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    )
}

private enum class LocalSongSortType {
    MODIFIED,
    NAME,
    ARTIST,
    ALBUM,
}