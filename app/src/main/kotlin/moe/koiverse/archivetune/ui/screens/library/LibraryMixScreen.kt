/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */


package moe.koiverse.archivetune.ui.screens.library

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.MixSortDescendingKey
import moe.koiverse.archivetune.constants.MixSortType
import moe.koiverse.archivetune.constants.MixSortTypeKey
import moe.koiverse.archivetune.constants.PlaylistSortType
import moe.koiverse.archivetune.constants.PlaylistSortTypeKey
import moe.koiverse.archivetune.constants.PlaylistTagsFilterKey
import moe.koiverse.archivetune.constants.ShowCachedPlaylistKey
import moe.koiverse.archivetune.constants.ShowDownloadedPlaylistKey
import moe.koiverse.archivetune.constants.ShowLikedPlaylistKey
import moe.koiverse.archivetune.constants.ShowTopPlaylistKey
import moe.koiverse.archivetune.constants.YtmSyncKey
import moe.koiverse.archivetune.db.entities.Playlist
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import moe.koiverse.archivetune.extensions.move
import moe.koiverse.archivetune.playback.queues.LocalAlbumRadio
import moe.koiverse.archivetune.ui.component.LibraryAlbumSpotlightCard
import moe.koiverse.archivetune.ui.component.LibraryArtistSpotlightCard
import moe.koiverse.archivetune.ui.component.LibraryPlaylistListItem
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.component.LibraryPinnedCollectionTile
import moe.koiverse.archivetune.ui.menu.AlbumMenu
import moe.koiverse.archivetune.ui.menu.ArtistMenu
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.LibraryMixViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.Collator
import java.util.Locale

private data class LibraryShortcutEntry(
    val title: String,
    @DrawableRes val iconRes: Int,
    val route: String,
    val accentColor: Color,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val database = LocalDatabase.current

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        MixSortTypeKey,
        MixSortType.CREATE_DATE,
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val (playlistSortType) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CUSTOM)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val (selectedTagsFilter) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }
    val filteredPlaylistIds by database.playlistIdsByTags(
        if (selectedTagIds.isEmpty()) emptyList() else selectedTagIds.toList(),
    ).collectAsState(initial = emptyList())

    val topSize by viewModel.topValue.collectAsState(initial = "50")
    val likedTitle = stringResource(R.string.liked)
    val downloadedTitle = stringResource(R.string.offline)
    val cachedTitle = stringResource(R.string.cached_playlist)
    val topTitle = stringResource(R.string.my_top) + " $topSize"

    val likedPlaylist = remember(likedTitle) {
        Playlist(
            playlist = PlaylistEntity(id = "AUTO_LIKED_LIBRARY", name = likedTitle, isEditable = false),
            songCount = 0,
            songThumbnails = emptyList(),
        )
    }
    val downloadPlaylist = remember(downloadedTitle) {
        Playlist(
            playlist = PlaylistEntity(id = "AUTO_DOWNLOADED_LIBRARY", name = downloadedTitle, isEditable = false),
            songCount = 0,
            songThumbnails = emptyList(),
        )
    }
    val topPlaylist = remember(topTitle) {
        Playlist(
            playlist = PlaylistEntity(id = "AUTO_TOP_LIBRARY", name = topTitle, isEditable = false),
            songCount = 0,
            songThumbnails = emptyList(),
        )
    }
    val cachePlaylist = remember(cachedTitle) {
        Playlist(
            playlist = PlaylistEntity(id = "AUTO_CACHED_LIBRARY", name = cachedTitle, isEditable = false),
            songCount = 0,
            songThumbnails = emptyList(),
        )
    }

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)

    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val collator = remember {
        Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
    }

    val visiblePlaylists = remember(playlists, selectedTagIds, filteredPlaylistIds) {
        if (selectedTagIds.isEmpty()) {
            playlists
        } else {
            playlists.filter { it.id in filteredPlaylistIds }
        }
    }
    val sortedAlbums = remember(albums, sortType, sortDescending, collator) {
        val sorted = when (sortType) {
            MixSortType.CREATE_DATE -> albums.sortedBy { it.album.bookmarkedAt }
            MixSortType.NAME -> albums.sortedWith(compareBy(collator) { it.album.title })
            MixSortType.LAST_UPDATED -> albums.sortedBy { it.album.lastUpdateTime }
        }
        if (sortDescending) sorted.asReversed() else sorted
    }
    val sortedArtists = remember(artists, sortType, sortDescending, collator) {
        val sorted = when (sortType) {
            MixSortType.CREATE_DATE -> artists.sortedBy { it.artist.bookmarkedAt }
            MixSortType.NAME -> artists.sortedWith(compareBy(collator) { it.artist.name })
            MixSortType.LAST_UPDATED -> artists.sortedBy { it.artist.lastUpdateTime }
        }
        if (sortDescending) sorted.asReversed() else sorted
    }

    val shortcuts = buildList {
        if (showLiked) {
            add(
                LibraryShortcutEntry(
                    title = likedPlaylist.playlist.name,
                    iconRes = R.drawable.favorite,
                    route = "auto_playlist/liked",
                    accentColor = MaterialTheme.colorScheme.error,
                ),
            )
        }
        if (showDownloaded) {
            add(
                LibraryShortcutEntry(
                    title = downloadPlaylist.playlist.name,
                    iconRes = R.drawable.offline,
                    route = "auto_playlist/downloaded",
                    accentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        if (showCached) {
            add(
                LibraryShortcutEntry(
                    title = cachePlaylist.playlist.name,
                    iconRes = R.drawable.cached,
                    route = "cache_playlist/cached",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                ),
            )
        }
        if (showTop) {
            add(
                LibraryShortcutEntry(
                    title = topPlaylist.playlist.name,
                    iconRes = R.drawable.trending_up,
                    route = "top_playlist/$topSize",
                    accentColor = MaterialTheme.colorScheme.secondary,
                ),
            )
        }
    }

    val lazyListState = rememberLazyListState()
    val customPlaylistMode = playlistSortType == PlaylistSortType.CUSTOM
    val canEnterReorderMode = customPlaylistMode && selectedTagIds.isEmpty()
    var reorderEnabled by rememberSaveable { mutableStateOf(false) }
    val canReorderPlaylists = canEnterReorderMode && reorderEnabled
    val playlistSectionLeadingItems = 3 + if (shortcuts.isNotEmpty()) 1 else 0
    val mutableVisiblePlaylists = remember { mutableStateListOf<Playlist>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) { from, to ->
        if (!canReorderPlaylists) return@rememberReorderableLazyListState
        if (from.index < playlistSectionLeadingItems || to.index < playlistSectionLeadingItems) {
            return@rememberReorderableLazyListState
        }

        val fromIndex = from.index - playlistSectionLeadingItems
        val toIndex = to.index - playlistSectionLeadingItems
        if (fromIndex !in mutableVisiblePlaylists.indices || toIndex !in mutableVisiblePlaylists.indices) {
            return@rememberReorderableLazyListState
        }

        val currentDragInfo = dragInfo
        dragInfo = if (currentDragInfo == null) fromIndex to toIndex else currentDragInfo.first to toIndex
        mutableVisiblePlaylists.move(fromIndex, toIndex)
    }

    LaunchedEffect(visiblePlaylists, canReorderPlaylists, reorderableState.isAnyItemDragging, dragInfo) {
        if (!canReorderPlaylists) {
            mutableVisiblePlaylists.clear()
            mutableVisiblePlaylists.addAll(visiblePlaylists)
            return@LaunchedEffect
        }

        if (!reorderableState.isAnyItemDragging && dragInfo == null) {
            mutableVisiblePlaylists.clear()
            mutableVisiblePlaylists.addAll(visiblePlaylists)
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging, canReorderPlaylists) {
        if (!canReorderPlaylists || reorderableState.isAnyItemDragging) return@LaunchedEffect

        dragInfo ?: return@LaunchedEffect
        val playlistsToReorder = mutableVisiblePlaylists.toList()
        database.transaction {
            playlistsToReorder.forEachIndexed { index, playlist ->
                setPlaylistCustomOrder(playlist.id, index)
            }
        }
        dragInfo = null
    }

    LaunchedEffect(canEnterReorderMode) {
        if (!canEnterReorderMode) reorderEnabled = false
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(ytmSync) {
        if (ytmSync) {
            viewModel.syncAllLibrary()
        }
    }

    LazyColumn(
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        item(key = "filter") {
            filterContent()
        }

        item(key = "controls") {
            LibraryControlCard(
                canEnterReorderMode = canEnterReorderMode,
                reorderEnabled = reorderEnabled,
                onToggleReorder = { reorderEnabled = !reorderEnabled },
            ) {
                LibraryMixSortSplitButton(
                    sortType = sortType,
                    sortDescending = sortDescending,
                    onSortTypeChange = onSortTypeChange,
                    onSortDescendingChange = onSortDescendingChange,
                    sortTypeText = { type ->
                        when (type) {
                            MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                            MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                            MixSortType.NAME -> R.string.sort_by_name
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (shortcuts.isNotEmpty()) {
            item(key = "shortcuts") {
                LibraryShortcutGrid(
                    entries = shortcuts,
                    onClick = navController::navigate,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (customPlaylistMode && canReorderPlaylists) {
            itemsIndexed(
                items = mutableVisiblePlaylists,
                key = { _, item -> item.id },
            ) { _, item ->
                ReorderableItem(
                    state = reorderableState,
                    key = item.id,
                ) {
                    LibraryPlaylistListItem(
                        navController = navController,
                        menuState = menuState,
                        coroutineScope = coroutineScope,
                        playlist = item,
                        showDragHandle = true,
                        dragHandleModifier = Modifier.draggableHandle(),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                }
            }
        } else {
            items(
                items = visiblePlaylists,
                key = { it.id },
            ) { item ->
                LibraryPlaylistListItem(
                    navController = navController,
                    menuState = menuState,
                    coroutineScope = coroutineScope,
                    playlist = item,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
            }
        }

        if (sortedAlbums.isNotEmpty()) {
            items(
                items = sortedAlbums,
                key = { it.id },
            ) { album ->
                LibraryAlbumSpotlightCard(
                    album = album,
                    isActive = album.id == mediaMetadata?.album?.id,
                    isPlaying = isPlaying,
                    onPlay = {
                        coroutineScope.launch {
                            database.albumWithSongs(album.id).firstOrNull()?.let { albumWithSongs ->
                                playerConnection.playQueue(LocalAlbumRadio(albumWithSongs))
                            }
                        }
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    AlbumMenu(
                                        originalAlbum = album,
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
                        .padding(horizontal = 16.dp)
                        .combinedClickable(
                            onClick = { navController.navigate("album/${album.id}") },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    AlbumMenu(
                                        originalAlbum = album,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        )
                        .animateItem(),
                )
            }
        }

        if (sortedArtists.isNotEmpty()) {
            items(
                items = sortedArtists,
                key = { it.id },
            ) { artist ->
                LibraryArtistSpotlightCard(
                    artist = artist,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    ArtistMenu(
                                        originalArtist = artist,
                                        coroutineScope = coroutineScope,
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
                        .padding(horizontal = 16.dp)
                        .combinedClickable(
                            onClick = { navController.navigate("artist/${artist.id}") },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    ArtistMenu(
                                        originalArtist = artist,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        )
                        .animateItem(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryMixSortSplitButton(
    sortType: MixSortType,
    sortDescending: Boolean,
    onSortTypeChange: (MixSortType) -> Unit,
    onSortDescendingChange: (Boolean) -> Unit,
    sortTypeText: (MixSortType) -> Int,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        LibraryMixSortTypeSplitButton(
            sortType = sortType,
            onSortTypeChange = onSortTypeChange,
            sortTypeText = sortTypeText,
            modifier = Modifier.weight(1f),
        )
        LibraryMixSortDirectionSplitButton(
            sortDescending = sortDescending,
            onSortDescendingChange = onSortDescendingChange,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryMixSortTypeSplitButton(
    sortType: MixSortType,
    onSortTypeChange: (MixSortType) -> Unit,
    sortTypeText: (MixSortType) -> Int,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val menuRotation by animateFloatAsState(
        targetValue = if (menuExpanded) 180f else 0f,
        label = "LibraryMixSortTypeMenu",
    )

    Box(modifier = modifier) {
        SplitButtonLayout(
            modifier = Modifier.fillMaxWidth(),
            leadingButton = {
                SplitButtonDefaults.TonalLeadingButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SplitButtonDefaults.MediumContainerHeight),
                ) {
                    Text(
                        text = stringResource(sortTypeText(sortType)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            trailingButton = {
                SplitButtonDefaults.TonalTrailingButton(
                    checked = menuExpanded,
                    onCheckedChange = { menuExpanded = it },
                    modifier = Modifier.heightIn(min = SplitButtonDefaults.MediumContainerHeight),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = null,
                        modifier = Modifier
                            .size(SplitButtonDefaults.TrailingIconSize)
                            .rotate(menuRotation),
                    )
                }
            },
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            MixSortType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(sortTypeText(type)),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(
                                if (sortType == type) {
                                    R.drawable.radio_button_checked
                                } else {
                                    R.drawable.radio_button_unchecked
                                }
                            ),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onSortTypeChange(type)
                        menuExpanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryMixSortDirectionSplitButton(
    sortDescending: Boolean,
    onSortDescendingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val sortDirectionRotation by animateFloatAsState(
        targetValue = if (sortDescending) 0f else 180f,
        label = "LibraryMixSortDirection",
    )
    val menuRotation by animateFloatAsState(
        targetValue = if (menuExpanded) 180f else 0f,
        label = "LibraryMixSortDirectionMenu",
    )

    Box(modifier = modifier) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.TonalLeadingButton(
                    onClick = { onSortDescendingChange(!sortDescending) },
                    modifier = Modifier
                        .heightIn(min = SplitButtonDefaults.MediumContainerHeight)
                        .widthIn(min = SplitButtonDefaults.MediumContainerHeight),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_downward),
                        contentDescription = stringResource(
                            if (sortDescending) {
                                R.string.sort_order_descending
                            } else {
                                R.string.sort_order_ascending
                            }
                        ),
                        modifier = Modifier
                            .size(SplitButtonDefaults.LeadingIconSize)
                            .rotate(sortDirectionRotation),
                    )
                }
            },
            trailingButton = {
                SplitButtonDefaults.TonalTrailingButton(
                    checked = menuExpanded,
                    onCheckedChange = { menuExpanded = it },
                    modifier = Modifier.heightIn(min = SplitButtonDefaults.MediumContainerHeight),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = null,
                        modifier = Modifier
                            .size(SplitButtonDefaults.TrailingIconSize)
                            .rotate(menuRotation),
                    )
                }
            },
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            listOf(false, true).forEach { descending ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(
                                if (descending) {
                                    R.string.sort_order_descending
                                } else {
                                    R.string.sort_order_ascending
                                }
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(
                                if (sortDescending == descending) {
                                    R.drawable.radio_button_checked
                                } else {
                                    R.drawable.radio_button_unchecked
                                }
                            ),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onSortDescendingChange(descending)
                        menuExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun LibraryControlCard(
    canEnterReorderMode: Boolean,
    reorderEnabled: Boolean,
    onToggleReorder: () -> Unit,
    controls: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        controls()
        if (canEnterReorderMode) {
            FilledTonalIconButton(
                onClick = onToggleReorder,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(if (reorderEnabled) R.drawable.lock_open else R.drawable.lock),
                    contentDescription = null,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryShortcutGrid(
    entries: List<LibraryShortcutEntry>,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        entries.chunked(2).forEach { rowEntries ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowEntries.forEach { entry ->
                    LibraryPinnedCollectionTile(
                        title = entry.title,
                        iconRes = entry.iconRes,
                        accentColor = entry.accentColor,
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(onClick = { onClick(entry.route) }),
                    )
                }
                if (rowEntries.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
