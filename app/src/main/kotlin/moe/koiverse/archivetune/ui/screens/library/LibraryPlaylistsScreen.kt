/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */


package moe.koiverse.archivetune.ui.screens.library

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.PlaylistSortDescendingKey
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
import moe.koiverse.archivetune.ui.component.LibraryPinnedCollectionTile
import moe.koiverse.archivetune.ui.component.LibraryPlaylistListItem
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.LibraryPlaylistsViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private data class PlaylistShortcutEntry(
    val title: String,
    @DrawableRes val iconRes: Int,
    val route: String,
    val accentColor: Color,
)

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()
    val database = LocalDatabase.current

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSortTypeKey,
        PlaylistSortType.CUSTOM,
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSortDescendingKey,
        true,
    )
    val (selectedTagsFilter) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }
    val filteredPlaylistIds by database.playlistIdsByTags(
        if (selectedTagIds.isEmpty()) emptyList() else selectedTagIds.toList(),
    ).collectAsState(initial = emptyList())

    val playlists by viewModel.allPlaylists.collectAsState()
    val visiblePlaylists = remember(playlists, selectedTagIds, filteredPlaylistIds) {
        playlists.filter { playlist ->
            val name = playlist.playlist.name
            val matchesName = !name.contains("episode", ignoreCase = true)
            val matchesTags = selectedTagIds.isEmpty() || playlist.id in filteredPlaylistIds
            matchesName && matchesTags
        }
    }

    val topSize by viewModel.topValue.collectAsState(initial = "50")
    val likedTitle = stringResource(R.string.liked)
    val downloadedTitle = stringResource(R.string.offline)
    val cachedTitle = stringResource(R.string.cached_playlist)
    val topTitle = stringResource(R.string.my_top) + " $topSize"

    val likedPlaylist = remember(likedTitle) {
        Playlist(
            playlist = PlaylistEntity(id = "AUTO_LIKED_PLAYLISTS", name = likedTitle, isEditable = false),
            songCount = 0,
            songThumbnails = emptyList(),
        )
    }
    val downloadPlaylist = remember(downloadedTitle) {
        Playlist(
            playlist = PlaylistEntity(id = "AUTO_DOWNLOADED_PLAYLISTS", name = downloadedTitle, isEditable = false),
            songCount = 0,
            songThumbnails = emptyList(),
        )
    }
    val topPlaylist = remember(topTitle) {
        Playlist(
            playlist = PlaylistEntity(id = "AUTO_TOP_PLAYLISTS", name = topTitle, isEditable = false),
            songCount = 0,
            songThumbnails = emptyList(),
        )
    }
    val cachePlaylist = remember(cachedTitle) {
        Playlist(
            playlist = PlaylistEntity(id = "AUTO_CACHED_PLAYLISTS", name = cachedTitle, isEditable = false),
            songCount = 0,
            songThumbnails = emptyList(),
        )
    }

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val shortcuts = buildList {
        if (showLiked) {
            add(
                PlaylistShortcutEntry(
                    title = likedPlaylist.playlist.name,
                    iconRes = R.drawable.favorite,
                    route = "auto_playlist/liked",
                    accentColor = MaterialTheme.colorScheme.error,
                ),
            )
        }
        if (showDownloaded) {
            add(
                PlaylistShortcutEntry(
                    title = downloadPlaylist.playlist.name,
                    iconRes = R.drawable.offline,
                    route = "auto_playlist/downloaded",
                    accentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        if (showCached) {
            add(
                PlaylistShortcutEntry(
                    title = cachePlaylist.playlist.name,
                    iconRes = R.drawable.cached,
                    route = "cache_playlist/cached",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                ),
            )
        }
        if (showTop) {
            add(
                PlaylistShortcutEntry(
                    title = topPlaylist.playlist.name,
                    iconRes = R.drawable.trending_up,
                    route = "top_playlist/$topSize",
                    accentColor = MaterialTheme.colorScheme.secondary,
                ),
            )
        }
    }

    val lazyListState = rememberLazyListState()
    val canEnterReorderMode = sortType == PlaylistSortType.CUSTOM && selectedTagIds.isEmpty()
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

    LaunchedEffect(ytmSync, allowSyncing) {
        if (ytmSync && allowSyncing) {
            viewModel.sync()
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val summary = pluralStringResource(R.plurals.n_playlist, visiblePlaylists.size, visiblePlaylists.size)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = {
                    if (ytmSync && allowSyncing) {
                        viewModel.sync()
                    }
                },
            ),
    ) {
        LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(key = "filter") {
                filterContent()
            }

            item(key = "controls") {
                    PlaylistSortSplitButton(
                        sortType = sortType,
                        sortDescending = sortDescending,
                        onSortTypeChange = onSortTypeChange,
                        onSortDescendingChange = onSortDescendingChange,
                        sortTypeText = { type ->
                            when (type) {
                                PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                                PlaylistSortType.NAME -> R.string.sort_by_name
                                PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                                PlaylistSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                                PlaylistSortType.CUSTOM -> R.string.sort_by_custom
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
            }

            if (shortcuts.isNotEmpty()) {
                item(key = "shortcuts") {
                    PlaylistShortcutGrid(
                        entries = shortcuts,
                        onClick = navController::navigate,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            item(key = "playlist_section_header") {
                LibrarySectionHeaderText(
                        title = stringResource(R.string.playlists),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

            if (canReorderPlaylists) {
                itemsIndexed(
                    items = mutableVisiblePlaylists,
                    key = { _, item -> item.id },
                ) { _, playlist ->
                    ReorderableItem(
                        state = reorderableState,
                        key = playlist.id,
                    ) {
                        LibraryPlaylistListItem(
                            navController = navController,
                            menuState = menuState,
                            coroutineScope = coroutineScope,
                            playlist = playlist,
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
                ) { playlist ->
                    LibraryPlaylistListItem(
                        navController = navController,
                        menuState = menuState,
                        coroutineScope = coroutineScope,
                        playlist = playlist,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                }
            }
        }

        PullToRefreshDefaults.Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaylistSortSplitButton(
    sortType: PlaylistSortType,
    sortDescending: Boolean,
    onSortTypeChange: (PlaylistSortType) -> Unit,
    onSortDescendingChange: (Boolean) -> Unit,
    sortTypeText: (PlaylistSortType) -> Int,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val sortDirectionRotation by animateFloatAsState(
        targetValue = if (sortDescending) 0f else 180f,
        label = "PlaylistSortDirection",
    )

    Box(modifier = modifier) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.TonalLeadingButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .weight(1f)
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
                    checked = sortDescending,
                    onCheckedChange = onSortDescendingChange,
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
                            .size(SplitButtonDefaults.TrailingIconSize)
                            .rotate(sortDirectionRotation),
                    )
                }
            },
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            PlaylistSortType.entries.forEach { type ->
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistShortcutGrid(
    entries: List<PlaylistShortcutEntry>,
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

@Composable
private fun LibrarySectionHeaderText(
    title: String,
    modifier: Modifier = Modifier,
) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
      }
}
