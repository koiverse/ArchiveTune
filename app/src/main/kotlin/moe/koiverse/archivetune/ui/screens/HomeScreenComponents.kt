/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.GridThumbnailHeight
import moe.koiverse.archivetune.constants.ListItemHeight
import moe.koiverse.archivetune.constants.ListThumbnailSize
import moe.koiverse.archivetune.constants.QuickPicksDisplayMode
import moe.koiverse.archivetune.constants.ThumbnailCornerRadius
import moe.koiverse.archivetune.db.entities.Album
import moe.koiverse.archivetune.db.entities.Artist
import moe.koiverse.archivetune.db.entities.LocalItem
import moe.koiverse.archivetune.db.entities.Playlist
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.extensions.toMediaItem
import moe.koiverse.archivetune.extensions.togglePlayPause
import moe.koiverse.archivetune.innertube.models.AlbumItem
import moe.koiverse.archivetune.innertube.models.ArtistItem
import moe.koiverse.archivetune.innertube.models.PlaylistItem
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.innertube.models.YTItem
import moe.koiverse.archivetune.innertube.pages.HomePage
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.models.toMediaMetadata
import moe.koiverse.archivetune.playback.PlayerConnection
import moe.koiverse.archivetune.playback.queues.ListQueue
import moe.koiverse.archivetune.playback.queues.YouTubeQueue
import moe.koiverse.archivetune.ui.component.AlbumGridItem
import moe.koiverse.archivetune.ui.component.ArtistGridItem
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.component.MenuState
import moe.koiverse.archivetune.ui.component.NavigationTitle
import moe.koiverse.archivetune.ui.component.RandomizeGridItem
import moe.koiverse.archivetune.ui.component.SongGridItem
import moe.koiverse.archivetune.ui.component.SongListItem
import moe.koiverse.archivetune.ui.component.SpeedDialGridItem
import moe.koiverse.archivetune.ui.component.YouTubeGridItem
import moe.koiverse.archivetune.ui.component.shimmer.GridItemPlaceHolder
import moe.koiverse.archivetune.ui.component.shimmer.ShimmerHost
import moe.koiverse.archivetune.ui.component.shimmer.TextPlaceholder
import moe.koiverse.archivetune.ui.menu.AlbumMenu
import moe.koiverse.archivetune.ui.menu.ArtistMenu
import moe.koiverse.archivetune.ui.menu.PlaylistMenu
import moe.koiverse.archivetune.ui.menu.SongMenu
import moe.koiverse.archivetune.ui.menu.YouTubeAlbumMenu
import moe.koiverse.archivetune.ui.menu.YouTubeArtistMenu
import moe.koiverse.archivetune.ui.menu.YouTubePlaylistMenu
import moe.koiverse.archivetune.ui.menu.YouTubeSongMenu
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import moe.koiverse.archivetune.ui.utils.SnapLayoutInfoProvider as buildSnapLayoutInfoProvider
import moe.koiverse.archivetune.models.SimilarRecommendation
import kotlin.math.ceil
import kotlin.random.Random
import kotlin.math.min

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import moe.koiverse.archivetune.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuickPicksSection(
    quickPicks: List<Song>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    displayMode: QuickPicksDisplayMode,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    val distinctQuickPicks = remember(quickPicks) { quickPicks.distinctBy { it.id } }

    when (displayMode) {
        QuickPicksDisplayMode.CARD -> HorizontalCenteredHeroCarousel(
            state = rememberCarouselState { distinctQuickPicks.size },
            maxItemWidth = 250.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(290.dp)
        ) { index ->
            val song = distinctQuickPicks[index]
            val isActive = song.id == mediaMetadata?.id

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .maskBorder(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        MaterialTheme.shapes.extraLarge
                    )
                    .focusable()
                    .combinedClickable(
                        onClick = {
                            if (isActive) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                SongMenu(
                                    originalSong = song,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.song.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )

                if (isActive && isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.volume_up),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = song.song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        QuickPicksDisplayMode.LIST -> BoxWithConstraints(
            modifier = modifier.fillMaxWidth()
        ) {
            val widthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val itemWidth = maxWidth * widthFactor
            val lazyGridState = rememberLazyGridState()
            val snapLayoutInfoProvider = remember(lazyGridState, widthFactor) {
                buildSnapLayoutInfoProvider(
                    lazyGridState = lazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        layoutSize * widthFactor / 2f - itemSize / 2f
                    }
                )
            }
            LazyHorizontalGrid(
                state = lazyGridState,
                rows = GridCells.Fixed(4),
                flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                contentPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Horizontal)
                    .asPaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ListItemHeight * 4)
            ) {
                items(
                    items = distinctQuickPicks,
                    key = { it.id }
                ) { song ->
                    SongListItem(
                        song = song,
                        showInLibraryIcon = true,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        isSwipeable = false,
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier
                            .width(itemWidth)
                            .combinedClickable(
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeedDialSection(
    speedDialItems: List<LocalItem>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    data class SpeedDialTile(
        val key: String,
        val localItem: LocalItem?,
        val ytItem: YTItem?,
    )

    val distinctSpeedDial = remember(speedDialItems) {
        speedDialItems.distinctBy {
            when (it) {
                is Song -> "song_${it.id}"
                is Album -> "album_${it.id}"
                is Artist -> "artist_${it.id}"
                is Playlist -> "playlist_${it.id}"
            }
        }.take(24)
    }
    val speedDialSongs = remember(distinctSpeedDial) { distinctSpeedDial.filterIsInstance<Song>() }
    val speedDialSongIndexById = remember(speedDialSongs) {
        speedDialSongs.mapIndexed { index, song -> song.id to index }.toMap()
    }
    val tileSize = 130.dp
    val spacing = 10.dp
    val state = rememberLazyGridState()
    val rowCount = min(3, distinctSpeedDial.size + 1)
    val gridHeight = (tileSize * rowCount) + (spacing * (rowCount - 1))

    val tiles = remember(distinctSpeedDial) {
        buildList {
            distinctSpeedDial.forEach { localItem ->
                val key = when (localItem) {
                    is Song -> "song_${localItem.id}"
                    is Album -> "album_${localItem.id}"
                    is Artist -> "artist_${localItem.id}"
                    is Playlist -> "playlist_${localItem.id}"
                }
                val ytItem = when (localItem) {
                    is Song -> SongItem(
                        id = localItem.id,
                        title = localItem.title,
                        artists = localItem.artists.map {
                            moe.koiverse.archivetune.innertube.models.Artist(name = it.name, id = it.id)
                        },
                        thumbnail = localItem.song.thumbnailUrl.orEmpty(),
                        explicit = localItem.song.explicit,
                    )
                    is Album -> AlbumItem(
                        browseId = localItem.id,
                        playlistId = localItem.album.playlistId.orEmpty(),
                        title = localItem.title,
                        artists = localItem.artists.map {
                            moe.koiverse.archivetune.innertube.models.Artist(name = it.name, id = it.id)
                        },
                        year = localItem.album.year,
                        thumbnail = localItem.album.thumbnailUrl.orEmpty(),
                    )
                    is Artist -> ArtistItem(
                        id = localItem.id,
                        title = localItem.title,
                        thumbnail = localItem.artist.thumbnailUrl,
                        channelId = localItem.artist.channelId,
                        playEndpoint = null,
                        shuffleEndpoint = null,
                        radioEndpoint = null,
                    )
                    is Playlist -> PlaylistItem(
                        id = localItem.id,
                        title = localItem.title,
                        author = null,
                        songCountText = localItem.songCount.toString(),
                        thumbnail = localItem.thumbnails.firstOrNull(),
                        playEndpoint = null,
                        shuffleEndpoint = null,
                        radioEndpoint = null,
                        isEditable = localItem.playlist.isEditable,
                    )
                }
                add(SpeedDialTile(key = key, localItem = localItem, ytItem = ytItem))
            }
            add(SpeedDialTile(key = "random", localItem = null, ytItem = null))
        }
    }

    val columnCount = remember(tiles.size, rowCount) {
        ceil(tiles.size / rowCount.toFloat()).toInt().coerceAtLeast(1)
    }

    val orderedTiles = remember(tiles, rowCount, columnCount) {
        buildList {
            for (column in 0 until columnCount) {
                for (row in 0 until rowCount) {
                    val index = row * columnCount + column
                    if (index < tiles.size) add(tiles[index])
                }
            }
        }
    }

    fun playSpeedDialQueue(startIndex: Int) {
        if (speedDialSongs.isEmpty()) return
        playerConnection.playQueue(
            ListQueue(
                title = context.getString(R.string.speed_dial),
                items = speedDialSongs.map { it.toMediaItem() },
                startIndex = startIndex,
            )
        )
    }

    val dotState by
        remember(state, distinctSpeedDial.size, rowCount) {
            derivedStateOf {
                val totalItems = distinctSpeedDial.size
                if (totalItems <= 0) {
                    Triple(0, 0, 0)
                } else {
                    val songsPerDot = 8
                    val columnsPerDot =
                        ceil(songsPerDot / rowCount.toFloat()).toInt().coerceAtLeast(1)
                    val totalSongColumns =
                        ceil(totalItems / rowCount.toFloat()).toInt().coerceAtLeast(1)
                    val pages =
                        ceil(totalSongColumns / columnsPerDot.toFloat()).toInt().coerceAtLeast(1)
                    val currentColumn =
                        (state.firstVisibleItemIndex / rowCount).coerceIn(0, totalSongColumns - 1)
                    val currentPage = (currentColumn / columnsPerDot).coerceIn(0, pages - 1)
                    val dots = min(3, pages)
                    val selectedDot =
                        if (pages <= 3) currentPage
                        else ((currentPage.toFloat() / (pages - 1).coerceAtLeast(1)) * (dots - 1))
                            .toInt()
                            .coerceIn(0, dots - 1)
                    Triple(dots, selectedDot, pages)
                }
            }
        }

    val (dotsCount, selectedDotIndex) = dotState.let { (dots, selected, _) -> dots to selected }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyHorizontalGrid(
            state = state,
            rows = GridCells.Fixed(rowCount),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
            modifier =
                Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .height(gridHeight),
        ) {
            items(
                items = orderedTiles,
                key = { it.key },
                contentType = { tile -> if (tile.localItem == null) "speed_dial_random" else "speed_dial_item" },
            ) { tile ->
                val localItem = tile.localItem
                val ytItem = tile.ytItem
                if (localItem == null || ytItem == null) {
                    RandomizeGridItem(
                        isLoading = false,
                        onClick = {
                            if (speedDialSongs.isNotEmpty()) {
                                playSpeedDialQueue(Random.nextInt(speedDialSongs.size))
                            }
                        },
                        modifier = Modifier.width(tileSize),
                    )
                } else {
                    val isActive = when (localItem) {
                        is Song -> localItem.id == mediaMetadata?.id
                        is Album -> localItem.id == mediaMetadata?.album?.id
                        is Artist -> false
                        is Playlist -> false
                    }
                    val songIndex = if (localItem is Song) speedDialSongIndexById[localItem.id] ?: 0 else 0

                    Box(
                        modifier = Modifier
                            .width(tileSize)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .focusable()
                            .combinedClickable(
                                onClick = {
                                    when (localItem) {
                                        is Song -> {
                                            if (isActive) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playSpeedDialQueue(songIndex)
                                            }
                                        }
                                        is Album -> navController.navigate("album/${localItem.id}")
                                        is Artist -> navController.navigate("artist/${localItem.id}")
                                        is Playlist -> navController.navigate("local_playlist/${localItem.id}")
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        when (localItem) {
                                            is Song -> SongMenu(
                                                originalSong = localItem,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                            is Album -> AlbumMenu(
                                                originalAlbum = localItem,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                            is Artist -> ArtistMenu(
                                                originalArtist = localItem,
                                                coroutineScope = scope,
                                                onDismiss = menuState::dismiss
                                            )
                                            is Playlist -> PlaylistMenu(
                                                playlist = localItem,
                                                coroutineScope = scope,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                }
                            )
                    ) {
                        SpeedDialGridItem(
                            item = ytItem,
                            isPinned = true,
                            isActive = isActive,
                            isPlaying = isPlaying,
                        )
                    }
                }
            }
        }

        if (dotsCount > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                repeat(dotsCount) { index ->
                    val isSelected = index == selectedDotIndex
                    Surface(
                        color =
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        shape = CircleShape,
                        modifier =
                            Modifier.size(
                                if (isSelected) 8.dp else 6.dp
                            ),
                    ) {}
                }
            }
        }
    }
}

/**
 * Keep Listening section - horizontal grid of local items
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeepListeningSection(
    keepListening: List<LocalItem>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val rows = if (keepListening.size > 6) 2 else 1
    val gridHeight = (GridThumbnailHeight + with(LocalDensity.current) {
        MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
    }) * rows

    LazyHorizontalGrid(
        state = rememberLazyGridState(),
        rows = GridCells.Fixed(rows),
        modifier = modifier
            .fillMaxWidth()
            .height(gridHeight)
    ) {
        items(
            items = keepListening,
            key = { item -> 
                when (item) {
                    is Song -> "song_${item.id}"
                    is Album -> "album_${item.id}"
                    is Artist -> "artist_${item.id}"
                    is Playlist -> "playlist_${item.id}"
                }
            }
        ) { item ->
            LocalGridItem(
                item = item,
                mediaMetadata = mediaMetadata,
                isPlaying = isPlaying,
                navController = navController,
                playerConnection = playerConnection,
                menuState = menuState,
                haptic = haptic,
                scope = scope
            )
        }
    }
}

/**
 * Forgotten Favorites section - horizontal grid of songs
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ForgottenFavoritesSection(
    forgottenFavorites: List<Song>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    horizontalLazyGridItemWidth: Dp,
    lazyGridState: LazyGridState,
    snapLayoutInfoProvider: SnapLayoutInfoProvider,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    val rows = min(4, forgottenFavorites.size)
    val distinctForgottenFavorites = remember(forgottenFavorites) { forgottenFavorites.distinctBy { it.id } }
    
    LazyHorizontalGrid(
        state = lazyGridState,
        rows = GridCells.Fixed(rows),
        flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
        contentPadding = WindowInsets.systemBars
            .only(WindowInsetsSides.Horizontal)
            .asPaddingValues(),
        modifier = modifier
            .fillMaxWidth()
            .height(ListItemHeight * rows)
    ) {
        items(
            items = distinctForgottenFavorites,
            key = { it.id }
        ) { song ->
            SongListItem(
                song = song,
                showInLibraryIcon = true,
                isActive = song.id == mediaMetadata?.id,
                isPlaying = isPlaying,
                isSwipeable = false,
                trailingContent = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                SongMenu(
                                    originalSong = song,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier
                    .width(horizontalLazyGridItemWidth)
                    .focusable()
                    .combinedClickable(
                        onClick = {
                            if (song.id == mediaMetadata?.id) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                SongMenu(
                                    originalSong = song,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )
        }
    }
}

/**
 * Account Playlists section - horizontal row of YouTube playlists
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountPlaylistsSection(
    accountPlaylists: List<PlaylistItem>,
    accountName: String,
    accountImageUrl: String?,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val distinctPlaylists = remember(accountPlaylists) { accountPlaylists.distinctBy { it.id } }
    
    LazyRow(
        contentPadding = WindowInsets.systemBars
            .only(WindowInsetsSides.Horizontal)
            .asPaddingValues(),
        modifier = modifier
    ) {
        items(
            items = distinctPlaylists,
            key = { it.id }
        ) { item ->
            YouTubeGridItemWrapper(
                item = item,
                mediaMetadata = mediaMetadata,
                isPlaying = isPlaying,
                navController = navController,
                playerConnection = playerConnection,
                menuState = menuState,
                haptic = haptic,
                scope = scope
            )
        }
    }
}

/**
 * Similar Recommendations section
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimilarRecommendationsSection(
    recommendation: SimilarRecommendation,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = WindowInsets.systemBars
            .only(WindowInsetsSides.Horizontal)
            .asPaddingValues(),
        modifier = modifier
    ) {
        items(
            items = recommendation.items,
            key = { it.id }
        ) { item ->
            YouTubeGridItemWrapper(
                item = item,
                mediaMetadata = mediaMetadata,
                isPlaying = isPlaying,
                navController = navController,
                playerConnection = playerConnection,
                menuState = menuState,
                haptic = haptic,
                scope = scope
            )
        }
    }
}

/**
 * HomePage Section - a single section from YouTube home page
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePageSectionContent(
    section: HomePage.Section,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = WindowInsets.systemBars
            .only(WindowInsetsSides.Horizontal)
            .asPaddingValues(),
        modifier = modifier
    ) {
        items(
            items = section.items,
            key = { it.id }
        ) { item ->
            YouTubeGridItemWrapper(
                item = item,
                mediaMetadata = mediaMetadata,
                isPlaying = isPlaying,
                navController = navController,
                playerConnection = playerConnection,
                menuState = menuState,
                haptic = haptic,
                scope = scope
            )
        }
    }
}

/**
 * Loading shimmer for home page sections
 */
@Composable
fun HomeLoadingShimmer(modifier: Modifier = Modifier) {
    ShimmerHost(modifier = modifier) {
        TextPlaceholder(
            height = 36.dp,
            modifier = Modifier
                .padding(12.dp)
                .width(250.dp),
        )
        LazyRow {
            items(4) {
                GridItemPlaceHolder()
            }
        }
    }
}

// ============== Helper Composables ==============

/**
 * Wrapper for YouTube grid items with click handling
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun YouTubeGridItemWrapper(
    item: YTItem,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    YouTubeGridItem(
        item = item,
        isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
        isPlaying = isPlaying,
        coroutineScope = scope,
        thumbnailRatio = 1f,
        modifier = modifier
            .focusable()
            .combinedClickable(
            onClick = {
                when (item) {
                    is SongItem -> playerConnection.playQueue(
                        YouTubeQueue(
                            item.endpoint ?: WatchEndpoint(videoId = item.id),
                            item.toMediaMetadata()
                        )
                    )
                    is AlbumItem -> navController.navigate("album/${item.id}")
                    is ArtistItem -> navController.navigate("artist/${item.id}")
                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                }
            },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                menuState.show {
                    when (item) {
                        is SongItem -> YouTubeSongMenu(
                            song = item,
                            navController = navController,
                            onDismiss = menuState::dismiss
                        )
                        is AlbumItem -> YouTubeAlbumMenu(
                            albumItem = item,
                            navController = navController,
                            onDismiss = menuState::dismiss
                        )
                        is ArtistItem -> YouTubeArtistMenu(
                            artist = item,
                            onDismiss = menuState::dismiss
                        )
                        is PlaylistItem -> YouTubePlaylistMenu(
                            playlist = item,
                            coroutineScope = scope,
                            onDismiss = menuState::dismiss
                        )
                    }
                }
            }
        )
    )
}

/**
 * Local item grid item for songs, albums, artists
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalGridItem(
    item: LocalItem,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    when (item) {
        is Song -> SongGridItem(
            song = item,
            modifier = modifier
                .fillMaxWidth()
                .focusable()
                .combinedClickable(
                    onClick = {
                        if (item.id == mediaMetadata?.id) {
                            playerConnection.player.togglePlayPause()
                        } else {
                            playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            SongMenu(
                                originalSong = item,
                                navController = navController,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                ),
            isActive = item.id == mediaMetadata?.id,
            isPlaying = isPlaying
        )

        is Album -> AlbumGridItem(
            album = item,
            isActive = item.id == mediaMetadata?.album?.id,
            isPlaying = isPlaying,
            coroutineScope = scope,
            modifier = modifier
                .fillMaxWidth()
                .focusable()
                .combinedClickable(
                    onClick = { navController.navigate("album/${item.id}") },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            AlbumMenu(
                                originalAlbum = item,
                                navController = navController,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                )
        )

        is Artist -> ArtistGridItem(
            artist = item,
            modifier = modifier
                .fillMaxWidth()
                .focusable()
                .combinedClickable(
                    onClick = { navController.navigate("artist/${item.id}") },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            ArtistMenu(
                                originalArtist = item,
                                coroutineScope = scope,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                )
        )

        is Playlist -> { /* Not displayed */ }
    }
}

/**
 * Account playlist navigation title with image
 */
@Composable
fun AccountPlaylistsTitle(
    accountName: String,
    accountImageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationTitle(
        label = stringResource(R.string.your_youtube_playlists),
        title = accountName.ifBlank { stringResource(R.string.account) },
        thumbnail = {
            if (accountImageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(accountImageUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(accountImageUrl)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(id = R.drawable.person),
                    error = painterResource(id = R.drawable.person),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(ListThumbnailSize)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(ListThumbnailSize)
                )
            }
        },
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Similar recommendations navigation title
 */
@Composable
fun SimilarRecommendationsTitle(
    recommendation: SimilarRecommendation,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    NavigationTitle(
        label = stringResource(R.string.similar_to),
        title = recommendation.title.title,
        thumbnail = recommendation.title.thumbnailUrl?.let { thumbnailUrl ->
            {
                val shape = if (recommendation.title is Artist) CircleShape 
                    else RoundedCornerShape(ThumbnailCornerRadius)
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(ListThumbnailSize)
                        .clip(shape)
                )
            }
        },
        onClick = {
            when (recommendation.title) {
                is Song -> navController.navigate("album/${recommendation.title.album!!.id}")
                is Album -> navController.navigate("album/${recommendation.title.id}")
                is Artist -> navController.navigate("artist/${recommendation.title.id}")
                is Playlist -> {}
            }
        },
        modifier = modifier
    )
}

/**
 * HomePage section navigation title
 */
@Composable
fun HomePageSectionTitle(
    section: HomePage.Section,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    NavigationTitle(
        title = section.title,
        label = section.label,
        thumbnail = section.thumbnail?.let { thumbnailUrl ->
            {
                val shape = if (section.endpoint?.isArtistEndpoint == true) CircleShape 
                    else RoundedCornerShape(ThumbnailCornerRadius)
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(ListThumbnailSize)
                        .clip(shape)
                )
            }
        },
        onClick = section.endpoint?.browseId?.let { browseId ->
            {
                if (browseId == "FEmusic_moods_and_genres")
                    navController.navigate(Screens.MoodAndGenres.route)
                else
                    navController.navigate("browse/$browseId")
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.AccountPlaylistsContainer(
    viewModel: HomeViewModel,
    accountName: String?,
    accountImageUrl: String?,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope
) {
    item {
        val accountPlaylists by viewModel.accountPlaylists.collectAsState()
        
        // Check if list is not null and not empty
        val currentPlaylists = accountPlaylists
        if (!currentPlaylists.isNullOrEmpty()) {
            Column {
                 AccountPlaylistsTitle(
                    accountName = accountName ?: "",
                    accountImageUrl = accountImageUrl,
                    onClick = { navController.navigate("account") },
                    modifier = Modifier
                )
                AccountPlaylistsSection(
                    accountPlaylists = currentPlaylists,
                    accountName = accountName ?: "",
                    accountImageUrl = accountImageUrl,
                    mediaMetadata = mediaMetadata,
                    isPlaying = isPlaying,
                    navController = navController,
                    playerConnection = playerConnection,
                    menuState = menuState,
                    haptic = haptic,
                    scope = scope
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.SimilarRecommendationsContainer(
    viewModel: HomeViewModel,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope
) {
     item {
        val similarRecommendations by viewModel.similarRecommendations.collectAsState()
        
        Column {
            similarRecommendations?.forEach { recommendation ->
                SimilarRecommendationsTitle(
                    recommendation = recommendation,
                    navController = navController,
                    modifier = Modifier
                )
                SimilarRecommendationsSection(
                    recommendation = recommendation,
                    mediaMetadata = mediaMetadata,
                    isPlaying = isPlaying,
                    navController = navController,
                    playerConnection = playerConnection,
                    menuState = menuState,
                    haptic = haptic,
                    scope = scope
                )
            }
        }
    }
}
