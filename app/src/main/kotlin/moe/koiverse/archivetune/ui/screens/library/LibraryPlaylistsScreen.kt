/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import moe.koiverse.archivetune.ui.theme.PlayerColorExtractor
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import moe.koiverse.archivetune.innertube.utils.parseCookieString
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.CONTENT_TYPE_HEADER
import moe.koiverse.archivetune.constants.CONTENT_TYPE_PLAYLIST
import moe.koiverse.archivetune.constants.GridItemSize
import moe.koiverse.archivetune.constants.GridItemsSizeKey
import moe.koiverse.archivetune.constants.GridThumbnailHeight
import moe.koiverse.archivetune.constants.InnerTubeCookieKey
import moe.koiverse.archivetune.constants.LibraryViewType
import moe.koiverse.archivetune.constants.PlaylistSortDescendingKey
import moe.koiverse.archivetune.constants.PlaylistSortType
import moe.koiverse.archivetune.constants.PlaylistSortTypeKey
import moe.koiverse.archivetune.constants.PlaylistViewTypeKey
import moe.koiverse.archivetune.constants.ShowLikedPlaylistKey
import moe.koiverse.archivetune.constants.ShowDownloadedPlaylistKey
import moe.koiverse.archivetune.constants.ShowTopPlaylistKey
import moe.koiverse.archivetune.constants.ShowCachedPlaylistKey
import moe.koiverse.archivetune.constants.UseNewLibraryDesignKey
import moe.koiverse.archivetune.constants.YtmSyncKey
import moe.koiverse.archivetune.constants.DisableBlurKey
import moe.koiverse.archivetune.constants.PlaylistTagsFilterKey
import moe.koiverse.archivetune.constants.PureBlackKey
import moe.koiverse.archivetune.db.entities.Playlist
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import moe.koiverse.archivetune.ui.component.CreatePlaylistDialog
import moe.koiverse.archivetune.ui.component.LibraryFloatingToolbar
import moe.koiverse.archivetune.ui.component.LibraryMeshGradient
import moe.koiverse.archivetune.ui.component.LibraryPlaylistGridItem
import moe.koiverse.archivetune.ui.component.LibraryPlaylistListItem
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.component.PlaylistGridItem
import moe.koiverse.archivetune.ui.component.PlaylistListItem
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.LibraryPlaylistsViewModel
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.extensions.move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSortTypeKey,
        PlaylistSortType.CUSTOM
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSortDescendingKey,
        true
    )
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val useNewLibraryDesign by rememberPreference(UseNewLibraryDesignKey, false)
    val (pureBlack) = rememberPreference(PureBlackKey, false)
    val scrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior()


    val (selectedTagsFilter, onSelectedTagsFilterChange) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }
    val database = LocalDatabase.current
    val filteredPlaylistIds by database.playlistIdsByTags(
        if (selectedTagIds.isEmpty()) emptyList() else selectedTagIds.toList()
    ).collectAsState(initial = emptyList())

    val playlists by viewModel.allPlaylists.collectAsState()

    val visiblePlaylists = playlists.filter { playlist ->
        val name = playlist.playlist.name ?: ""
        val matchesName = !name.contains("episode", ignoreCase = true)
        val matchesTags = selectedTagIds.isEmpty() || playlist.id in filteredPlaylistIds
        matchesName && matchesTags
    }

    val topSize by viewModel.topValue.collectAsState(initial = 50)

    val likedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.liked)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val downloadPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.offline)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val topPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.my_top) + " $topSize"
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val cachePlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.cached_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val canEnterReorderMode = sortType == PlaylistSortType.CUSTOM && selectedTagIds.isEmpty()
    var reorderEnabled by rememberSaveable { mutableStateOf(false) }
    val canReorderPlaylists = canEnterReorderMode && reorderEnabled
    val listHeaderItems =
        1 +
            (if (showLiked) 1 else 0) +
            (if (showDownloaded) 1 else 0) +
            (if (showTop) 1 else 0) +
            (if (showCached) 1 else 0)
    val mutableVisiblePlaylists = remember { mutableStateListOf<Playlist>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) { from, to ->
        if (!canReorderPlaylists) return@rememberReorderableLazyListState
        if (from.index < listHeaderItems || to.index < listHeaderItems) return@rememberReorderableLazyListState

        val fromIndex = from.index - listHeaderItems
        val toIndex = to.index - listHeaderItems

        if (fromIndex !in mutableVisiblePlaylists.indices || toIndex !in mutableVisiblePlaylists.indices) {
            return@rememberReorderableLazyListState
        }

        val currentDragInfo = dragInfo
        dragInfo =
            if (currentDragInfo == null) {
                fromIndex to toIndex
            } else {
                currentDragInfo.first to toIndex
            }

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
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.sync()
            }
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    // Gradient colors state for playlists page background
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    
    // Extract gradient colors from the first playlist with thumbnails
    LaunchedEffect(playlists) {
        val firstPlaylistWithThumbs = playlists.firstOrNull { it.songThumbnails.isNotEmpty() }
        val thumbnailUrl = firstPlaylistWithThumbs?.songThumbnails?.firstOrNull()
        
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE)
                .allowHardware(false)
                .build()

            val result = runCatching {
                withContext(Dispatchers.IO) { context.imageLoader.execute(request) }
            }.getOrNull()
            
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        Palette.from(bitmap)
                            .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                            .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                            .generate()
                    }
                
                    val extractedColors = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColor
                    )
                    gradientColors = extractedColors
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }
    
    // Calculate gradient opacity based on scroll position for both list and grid
    val gradientAlpha by remember {
        derivedStateOf {
            val firstVisibleIndex = when (viewType) {
                LibraryViewType.LIST -> lazyListState.firstVisibleItemIndex
                LibraryViewType.GRID -> lazyGridState.firstVisibleItemIndex
            }
            val scrollOffset = when (viewType) {
                LibraryViewType.LIST -> lazyListState.firstVisibleItemScrollOffset
                LibraryViewType.GRID -> lazyGridState.firstVisibleItemScrollOffset
            }
            
            if (firstVisibleIndex == 0) {
                // Fade out over 900dp of scrolling
                (1f - (scrollOffset / 900f)).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    val itemCountText = pluralStringResource(
        R.plurals.n_playlist,
        playlists.size,
        playlists.size,
    )

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = { if (ytmSync) viewModel.sync() }
            ),
    ) {
        if (!disableBlur && gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            LibraryMeshGradient(colors = gradientColors, alpha = gradientAlpha)
        }
        
        when (viewType) {
            LibraryViewType.LIST -> {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = likedPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("auto_playlist/liked")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = downloadPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("auto_playlist/downloaded")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = topPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("top_playlist/$topSize")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = cachePlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("cache_playlist/cached")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (visiblePlaylists.isEmpty()) {
                        item {
                        }
                    }

                    if (canReorderPlaylists) {
                        itemsIndexed(
                            items = mutableVisiblePlaylists,
                            key = { _, item -> item.id },
                            contentType = { _, _ -> CONTENT_TYPE_PLAYLIST },
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
                                    useNewDesign = useNewLibraryDesign,
                                    showDragHandle = true,
                                    dragHandleModifier = Modifier.draggableHandle(),
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    } else {
                        items(
                            items = visiblePlaylists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { playlist ->
                            LibraryPlaylistListItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                playlist = playlist,
                                useNewDesign = useNewLibraryDesign,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }

            LibraryViewType.GRID -> {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                    GridCells.Adaptive(
                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                    ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = likedPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/liked")
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = downloadPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/downloaded")
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = topPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("top_playlist/$topSize")
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = cachePlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("cache_playlist/cached")
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (visiblePlaylists.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                        }
                    }

                    items(
                        items = visiblePlaylists,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { playlist ->
                        LibraryPlaylistGridItem(
                            navController = navController,
                            menuState = menuState,
                            coroutineScope = coroutineScope,
                            playlist = playlist,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        LibraryFloatingToolbar(
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
            viewType = viewType,
            onViewTypeToggle = { viewType = viewType.toggle() },
            scrollBehavior = scrollBehavior,
            pureBlack = pureBlack,
            itemCountText = itemCountText,
            canReorder = canEnterReorderMode,
            reorderEnabled = reorderEnabled,
            onReorderToggle = { reorderEnabled = !reorderEnabled },
            fabIcon = R.drawable.add,
            onFabClick = { showCreatePlaylistDialog = true },
        )

        PullToRefreshDefaults.Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}

