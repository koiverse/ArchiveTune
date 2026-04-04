@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package moe.koiverse.archivetune.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.CONTENT_TYPE_HEADER
import moe.koiverse.archivetune.constants.CONTENT_TYPE_SONG
import moe.koiverse.archivetune.constants.LocalSongSortDescendingKey
import moe.koiverse.archivetune.constants.LocalSongSortTypeKey
import moe.koiverse.archivetune.constants.SongSortType
import moe.koiverse.archivetune.extensions.toMediaItem
import moe.koiverse.archivetune.extensions.togglePlayPause
import moe.koiverse.archivetune.playback.queues.ListQueue
import moe.koiverse.archivetune.ui.component.HideOnScrollFAB
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.component.SongListItem
import moe.koiverse.archivetune.ui.component.SortHeader
import moe.koiverse.archivetune.ui.menu.SelectionSongMenu
import moe.koiverse.archivetune.ui.menu.SongMenu
import moe.koiverse.archivetune.ui.utils.ItemWrapper
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.LocalSongViewModel

@Composable
fun LocalSong(
    navController: NavController,
    viewModel: LocalSongViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val songs by viewModel.allLocalSongs.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        LocalSongSortTypeKey,
        SongSortType.CREATE_DATE,
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(LocalSongSortDescendingKey, true)

    val wrappedSongs = songs.map { item -> ItemWrapper(item) }.toMutableList()
    var selection by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = { Text(stringResource(R.string.local_songs)) },
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
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("scan") },
                        onLongClick = {},
                    ) {
                        Icon(
                            painterResource(R.drawable.settings),
                            contentDescription = stringResource(R.string.scan_library),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )

            if (songs.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painterResource(R.drawable.music_note),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                        Text(
                            text = stringResource(R.string.no_local_songs),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.scan_your_device),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        androidx.compose.material3.FilledTonalButton(
                            onClick = { navController.navigate("scan") },
                        ) {
                            Text(stringResource(R.string.scan_device))
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        .asPaddingValues(),
                ) {
                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selection) {
                                val count = wrappedSongs.count { it.isSelected }
                                androidx.compose.material3.IconButton(
                                    onClick = { selection = false },
                                ) {
                                    Icon(
                                        painterResource(R.drawable.close),
                                        contentDescription = null,
                                    )
                                }
                                Text(
                                    text = pluralStringResource(R.plurals.n_song, count, count),
                                    modifier = Modifier.weight(1f),
                                )
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        if (count == wrappedSongs.size) {
                                            wrappedSongs.forEach { it.isSelected = false }
                                        } else {
                                            wrappedSongs.forEach { it.isSelected = true }
                                        }
                                    },
                                ) {
                                    Icon(
                                        painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all),
                                        contentDescription = null,
                                    )
                                }
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        menuState.show {
                                            SelectionSongMenu(
                                                songSelection = wrappedSongs.filter { it.isSelected }.map { it.item },
                                                onDismiss = menuState::dismiss,
                                                clearAction = { selection = false },
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                ) {
                                    SortHeader(
                                        sortType = sortType,
                                        sortDescending = sortDescending,
                                        onSortTypeChange = onSortTypeChange,
                                        onSortDescendingChange = onSortDescendingChange,
                                        sortTypeText = { sortType ->
                                            when (sortType) {
                                                SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                                SongSortType.NAME -> R.string.sort_by_name
                                                SongSortType.ARTIST -> R.string.sort_by_artist
                                                SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                            }
                                        },
                                    )

                                    Spacer(Modifier.weight(1f))

                                    Text(
                                        text = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(
                        items = wrappedSongs,
                        key = { _, item -> item.item.song.id },
                        contentType = { _, _ -> CONTENT_TYPE_SONG },
                    ) { index, songWrapper ->
                        SongListItem(
                            song = songWrapper.item,
                            isActive = songWrapper.item.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = songWrapper.item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            },
                            isSelected = songWrapper.isSelected && selection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (!selection) {
                                            if (songWrapper.item.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = context.getString(R.string.local_songs),
                                                        items = songs.map { it.toMediaItem() },
                                                        startIndex = index,
                                                    ),
                                                )
                                            }
                                        } else {
                                            songWrapper.isSelected = !songWrapper.isSelected
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                        }
                                        wrappedSongs.forEach { it.isSelected = false }
                                        songWrapper.isSelected = true
                                    },
                                )
                                .animateItem(),
                        )
                    }
                }
            }
        }

        HideOnScrollFAB(
            visible = songs.isNotEmpty(),
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = context.getString(R.string.local_songs),
                        items = songs.shuffled().map { it.toMediaItem() },
                    ),
                )
            },
        )
    }
}
