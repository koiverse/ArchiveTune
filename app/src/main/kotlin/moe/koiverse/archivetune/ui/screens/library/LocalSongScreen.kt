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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import moe.koiverse.archivetune.viewmodels.LocalSongsViewModel
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
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

    if (showSettingsDialog) {
        LocalSongSettingsDialog(
            hasStoragePermission = hasStoragePermission,
            scanState = scanState,
            onDismiss = { showSettingsDialog = false },
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
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                title = {
                    Text(
                        text = stringResource(R.string.local_history),
                        style = MaterialTheme.typography.titleLarge,
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
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.settings),
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(
                key = "top_spacer",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item(
                key = "controls",
                contentType = CONTENT_TYPE_HEADER,
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
                            onValueChange = { query = it },
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
                                    IconButton(onClick = { query = "" }) {
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
                                onSortTypeChange = { sortTypeName = it.name },
                                onSortDescendingChange = { sortDescending = it },
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
                                text = pluralStringResource(R.plurals.n_song, visibleSongs.size, visibleSongs.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }

            if (visibleSongs.isEmpty()) {
                item(
                    key = "empty",
                    contentType = CONTENT_TYPE_HEADER,
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
                                text = if (query.isBlank()) stringResource(R.string.local_songs_empty_title) else stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (query.isBlank()) {
                                    stringResource(R.string.local_songs_empty_desc)
                                } else {
                                    stringResource(R.string.search_try_different)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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
private fun LocalSongSettingsDialog(
    hasStoragePermission: Boolean,
    scanState: moe.koiverse.archivetune.viewmodels.LocalSongsScanState,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.settings),
                contentDescription = null,
            )
        },
        title = {
            Text(text = stringResource(R.string.settings))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (scanState.isScanning) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onPrimaryAction,
                enabled = !scanState.isScanning,
            ) {
                Text(text = primaryText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        },
    )
}

private enum class LocalSongSortType {
    MODIFIED,
    NAME,
    ARTIST,
    ALBUM,
}