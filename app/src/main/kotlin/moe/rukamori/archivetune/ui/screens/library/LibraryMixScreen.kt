/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.library

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.LocalPlayerAwareWindowInsets
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.MixSortDescendingKey
import moe.rukamori.archivetune.constants.MixSortType
import moe.rukamori.archivetune.constants.MixSortTypeKey
import moe.rukamori.archivetune.constants.PlaylistSortType
import moe.rukamori.archivetune.constants.PlaylistSortTypeKey
import moe.rukamori.archivetune.constants.ShowCachedPlaylistKey
import moe.rukamori.archivetune.constants.ShowDownloadedPlaylistKey
import moe.rukamori.archivetune.constants.ShowLikedPlaylistKey
import moe.rukamori.archivetune.constants.ShowSpotifyPlaylistsKey
import moe.rukamori.archivetune.constants.ShowTopPlaylistKey
import moe.rukamori.archivetune.constants.YtmSyncKey
import moe.rukamori.archivetune.db.entities.Playlist
import moe.rukamori.archivetune.db.entities.PlaylistEntity
import moe.rukamori.archivetune.extensions.move
import moe.rukamori.archivetune.playback.queues.LocalAlbumRadio
import moe.rukamori.archivetune.ui.component.DefaultDialog
import moe.rukamori.archivetune.ui.component.ExpressivePullToRefreshBox
import moe.rukamori.archivetune.ui.component.LibraryAlbumSpotlightCard
import moe.rukamori.archivetune.ui.component.LibraryArtistSpotlightCard
import moe.rukamori.archivetune.ui.component.LibraryPlaylistListItem
import moe.rukamori.archivetune.ui.component.LocalMenuState
import moe.rukamori.archivetune.ui.component.LibraryPinnedCollectionTile
import moe.rukamori.archivetune.ui.component.SpotifyLibraryPlaylistListItem
import moe.rukamori.archivetune.ui.menu.AlbumMenu
import moe.rukamori.archivetune.ui.menu.ArtistMenu
import moe.rukamori.archivetune.utils.rememberEnumPreference
import moe.rukamori.archivetune.utils.rememberPreference
import moe.rukamori.archivetune.spotify.SpotifyLibraryViewModel
import moe.rukamori.archivetune.viewmodels.BuildYourMixBasis
import moe.rukamori.archivetune.viewmodels.BuildYourMixUiState
import moe.rukamori.archivetune.viewmodels.LibraryMixViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.Collator
import java.util.Locale
import kotlin.math.roundToInt

private val LibraryGroupLargeCorner: Dp = 30.dp
private val LibraryGroupSmallCorner: Dp = 9.dp
private val LibraryGroupItemSpacing: Dp = 3.dp

private fun librarySegmentedShape(index: Int, count: Int): Shape {
    val large = LibraryGroupLargeCorner
    val small = LibraryGroupSmallCorner
    return when {
        count <= 1 -> RoundedCornerShape(large)
        index == 0 -> RoundedCornerShape(
            topStart = large,
            topEnd = large,
            bottomStart = small,
            bottomEnd = small,
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = small,
            topEnd = small,
            bottomStart = large,
            bottomEnd = large,
        )
        else -> RoundedCornerShape(small)
    }
}

private data class LibraryShortcutEntry(
    val title: String,
    @DrawableRes val iconRes: Int,
    val route: String? = null,
    val action: LibraryShortcutAction = LibraryShortcutAction.Navigate,
    val accentColor: Color,
)

private enum class LibraryShortcutAction {
    Navigate,
    BuildYourMix,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    selectedTagIds: Set<String>,
    viewModel: LibraryMixViewModel = hiltViewModel(),
    spotifyLibraryViewModel: SpotifyLibraryViewModel = hiltViewModel(),
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
    val filteredPlaylistIds by database.playlistIdsByTags(
        if (selectedTagIds.isEmpty()) emptyList() else selectedTagIds.toList(),
    ).collectAsState(initial = emptyList())

    val topSize by viewModel.topValue.collectAsState(initial = "50")
    val likedTitle = stringResource(R.string.liked)
    val downloadedTitle = stringResource(R.string.offline)
    val cachedTitle = stringResource(R.string.cached_playlist)
    val localTitle = stringResource(R.string.local_history)
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
    val (showSpotifyPlaylists) = rememberPreference(ShowSpotifyPlaylistsKey, false)

    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val buildYourMixState by viewModel.buildYourMixState.collectAsState()
    val isBuildYourMixAvailable by viewModel.isBuildYourMixAvailable.collectAsState()
    val spotifyPlaylists by spotifyLibraryViewModel.playlists.collectAsState()
    val spotifyIsRefreshing by spotifyLibraryViewModel.isRefreshing.collectAsState()
    val spotifyErrorMessage by spotifyLibraryViewModel.errorMessage.collectAsState()
    var showBuildYourMixDialog by rememberSaveable { mutableStateOf(false) }
    var buildYourMixSongCount by rememberSaveable { mutableStateOf(30) }
    var buildYourMixManualBasis by rememberSaveable { mutableStateOf("") }
    var buildYourMixBasis by rememberSaveable { mutableStateOf(BuildYourMixBasis.LISTENING_HISTORY) }

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

    val buildYourMixTitle = stringResource(R.string.build_your_mix_title)
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
        add(
            LibraryShortcutEntry(
                title = localTitle,
                iconRes = R.drawable.snippet_folder,
                route = "local_songs",
                accentColor = MaterialTheme.colorScheme.primary,
            ),
        )
        if (isBuildYourMixAvailable) {
            add(
                LibraryShortcutEntry(
                    title = buildYourMixTitle,
                    iconRes = R.drawable.auto_awesome,
                    action = LibraryShortcutAction.BuildYourMix,
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
    val spotifySectionItemCount = if (showSpotifyPlaylists) {
        1 +
            if (spotifyIsRefreshing) 1 else 0 +
            if (spotifyErrorMessage != null) 1 else 0 +
            if (spotifyPlaylists.isNotEmpty()) 1 else 0
    } else {
        0
    }
    val playlistSectionLeadingItems = 3 + if (shortcuts.isNotEmpty()) 1 else 0 + spotifySectionItemCount
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

    val buildYourMixSuccess = buildYourMixState as? BuildYourMixUiState.Success
    LaunchedEffect(buildYourMixSuccess?.playlistId) {
        buildYourMixSuccess?.let { success ->
            showBuildYourMixDialog = false
            navController.navigate("local_playlist/${success.playlistId}")
            viewModel.resetBuildYourMixState()
        }
    }

    if (showBuildYourMixDialog) {
        BuildYourMixDialog(
            state = buildYourMixState,
            selectedBasis = buildYourMixBasis,
            songCount = buildYourMixSongCount,
            manualBasis = buildYourMixManualBasis,
            onBasisChange = { buildYourMixBasis = it },
            onSongCountChange = { buildYourMixSongCount = it },
            onManualBasisChange = { buildYourMixManualBasis = it },
            onBuild = {
                viewModel.buildYourMix(
                    basis = buildYourMixBasis,
                    songCount = buildYourMixSongCount,
                    manualBasis = buildYourMixManualBasis,
                )
            },
            onDismiss = {
                showBuildYourMixDialog = false
                viewModel.resetBuildYourMixState()
            },
        )
    }

    ExpressivePullToRefreshBox(
        isRefreshing = isRefreshing || spotifyIsRefreshing,
        onRefresh = {
            if (ytmSync) viewModel.syncAllLibrary()
            if (showSpotifyPlaylists) spotifyLibraryViewModel.refreshPlaylists()
        },
        modifier = Modifier.fillMaxSize(),
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
                        onClick = { entry ->
                            when (entry.action) {
                                LibraryShortcutAction.BuildYourMix -> showBuildYourMixDialog = true
                                LibraryShortcutAction.Navigate -> entry.route?.let(navController::navigate)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (showSpotifyPlaylists) {
                item(key = "spotify_playlist_section_header") {
                    LibrarySectionHeaderText(
                        title = stringResource(R.string.spotify_playlists),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                if (spotifyIsRefreshing) {
                    item(key = "spotify_playlist_loading") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularWavyProgressIndicator(modifier = Modifier.size(28.dp))
                            Text(
                                text = stringResource(R.string.spotify_loading_library),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                spotifyErrorMessage?.let { error ->
                    item(key = "spotify_playlist_error") {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                }

                if (spotifyPlaylists.isNotEmpty()) {
                    item(key = "spotify_playlists_group") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(LibraryGroupItemSpacing),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            spotifyPlaylists.forEachIndexed { index, playlist ->
                                SpotifyLibraryPlaylistListItem(
                                    playlist = playlist,
                                    navController = navController,
                                    shape = librarySegmentedShape(index, spotifyPlaylists.size),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            item(key = "playlist_section_header") {
                LibrarySectionHeaderText(
                    title = stringResource(R.string.playlists),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
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
                item(key = "playlists_group") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(LibraryGroupItemSpacing),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        visiblePlaylists.forEachIndexed { index, item ->
                            LibraryPlaylistListItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                playlist = item,
                                shape = librarySegmentedShape(index, visiblePlaylists.size),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            if (sortedAlbums.isNotEmpty()) {

                item(key = "album_section_header") {
                    LibrarySectionHeaderText(
                        title = stringResource(R.string.albums),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                item(key = "albums_group") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(LibraryGroupItemSpacing),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        sortedAlbums.forEachIndexed { index, album ->
                            LibraryAlbumSpotlightCard(
                                album = album,
                                shape = librarySegmentedShape(index, sortedAlbums.size),
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
                                    .fillMaxWidth()
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
                                    ),
                            )
                        }
                    }
                }
            }

            if (sortedArtists.isNotEmpty()) {

                item(key = "artist_section_header") {
                    LibrarySectionHeaderText(
                        title = stringResource(R.string.artists),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                item(key = "artists_group") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(LibraryGroupItemSpacing),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        sortedArtists.forEachIndexed { index, artist ->
                            LibraryArtistSpotlightCard(
                                artist = artist,
                                shape = librarySegmentedShape(index, sortedArtists.size),
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
                                    .fillMaxWidth()
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
                                    ),
                            )
                        }
                    }
                }
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
    var menuExpanded by remember { mutableStateOf(false) }
    val sortDirectionRotation by animateFloatAsState(
        targetValue = if (sortDescending) 0f else 180f,
        label = "LibraryMixSortDirection",
    )

    Box(modifier = modifier) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.TonalLeadingButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.heightIn(min = SplitButtonDefaults.MediumContainerHeight),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BuildYourMixDialog(
    state: BuildYourMixUiState,
    selectedBasis: BuildYourMixBasis,
    songCount: Int,
    manualBasis: String,
    onBasisChange: (BuildYourMixBasis) -> Unit,
    onSongCountChange: (Int) -> Unit,
    onManualBasisChange: (String) -> Unit,
    onBuild: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isLoading = state == BuildYourMixUiState.Loading

    DefaultDialog(
        onDismiss = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                painter = painterResource(R.drawable.auto_awesome),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = stringResource(R.string.build_your_mix_title),
                textAlign = TextAlign.Center,
            )
        },
        horizontalAlignment = Alignment.Start,
        contentScrollable = true,
    ) {
        Crossfade(
            targetState = state,
            label = "BuildYourMixDialogState",
        ) { targetState ->
            when (targetState) {
                BuildYourMixUiState.Loading -> BuildYourMixLoadingContent()
                else -> BuildYourMixConfigurationContent(
                    state = targetState,
                    selectedBasis = selectedBasis,
                    songCount = songCount,
                    manualBasis = manualBasis,
                    onBasisChange = onBasisChange,
                    onSongCountChange = onSongCountChange,
                    onManualBasisChange = onManualBasisChange,
                    onBuild = onBuild,
                )
            }
        }
    }
}

@Composable
private fun BuildYourMixConfigurationContent(
    state: BuildYourMixUiState,
    selectedBasis: BuildYourMixBasis,
    songCount: Int,
    manualBasis: String,
    onBasisChange: (BuildYourMixBasis) -> Unit,
    onSongCountChange: (Int) -> Unit,
    onManualBasisChange: (String) -> Unit,
    onBuild: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.build_your_mix_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BuildYourMixBasisOption(
                selected = selectedBasis == BuildYourMixBasis.LISTENING_HISTORY,
                text = stringResource(R.string.build_your_mix_listening_history),
                onClick = { onBasisChange(BuildYourMixBasis.LISTENING_HISTORY) },
            )
            BuildYourMixBasisOption(
                selected = selectedBasis == BuildYourMixBasis.AVERAGE_LISTENED,
                text = stringResource(R.string.build_your_mix_average_listened),
                onClick = { onBasisChange(BuildYourMixBasis.AVERAGE_LISTENED) },
            )
            BuildYourMixBasisOption(
                selected = selectedBasis == BuildYourMixBasis.INPUT_MANUALLY,
                text = stringResource(R.string.build_your_mix_input_manually),
                onClick = { onBasisChange(BuildYourMixBasis.INPUT_MANUALLY) },
            )
        }
        AnimatedVisibility(visible = selectedBasis == BuildYourMixBasis.INPUT_MANUALLY) {
            TextField(
                value = manualBasis,
                onValueChange = onManualBasisChange,
                label = { Text(stringResource(R.string.build_your_mix_manual_basis)) },
                placeholder = { Text(stringResource(R.string.build_your_mix_manual_placeholder)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.build_your_mix_song_count),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = songCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = songCount.toFloat(),
                onValueChange = { onSongCountChange(it.roundToInt().coerceIn(1, 100)) },
                valueRange = 1f..100f,
                steps = 98,
            )
        }
        if (state is BuildYourMixUiState.Error) {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        FilledTonalButton(
            onClick = onBuild,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.build_your_mix_create))
        }
    }
}

@Composable
private fun BuildYourMixBasisOption(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BuildYourMixLoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        LoadingIndicator(modifier = Modifier.size(48.dp))
        Text(
            text = "\"${stringResource(R.string.build_your_mix_loading_quote)}\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryShortcutGrid(
    entries: List<LibraryShortcutEntry>,
    onClick: (LibraryShortcutEntry) -> Unit,
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
                            .combinedClickable(onClick = { onClick(entry) }),
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
