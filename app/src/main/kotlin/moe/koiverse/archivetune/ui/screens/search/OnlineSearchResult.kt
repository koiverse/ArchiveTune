/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import moe.koiverse.archivetune.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import moe.koiverse.archivetune.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import moe.koiverse.archivetune.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import moe.koiverse.archivetune.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import moe.koiverse.archivetune.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import moe.koiverse.archivetune.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import moe.koiverse.archivetune.innertube.models.AlbumItem
import moe.koiverse.archivetune.innertube.models.ArtistItem
import moe.koiverse.archivetune.innertube.models.PlaylistItem
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.innertube.models.YTItem
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.AppBarHeight
import moe.koiverse.archivetune.constants.SearchFilterHeight
import moe.koiverse.archivetune.extensions.togglePlayPause
import moe.koiverse.archivetune.models.toMediaMetadata
import moe.koiverse.archivetune.playback.queues.YouTubeQueue
import moe.koiverse.archivetune.ui.component.ChipsRow
import moe.koiverse.archivetune.ui.component.EmptyPlaceholder
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.component.YouTubeListItem
import moe.koiverse.archivetune.ui.component.shimmer.ListItemPlaceHolder
import moe.koiverse.archivetune.ui.component.shimmer.ShimmerHost
import moe.koiverse.archivetune.ui.menu.YouTubeAlbumMenu
import moe.koiverse.archivetune.ui.menu.YouTubeArtistMenu
import moe.koiverse.archivetune.ui.menu.YouTubePlaylistMenu
import moe.koiverse.archivetune.ui.menu.YouTubeSongMenu
import moe.koiverse.archivetune.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let {
                viewModel.viewStateMap[it]
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    val ytItemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item: YTItem ->
        val longClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuState.show {
                when (item) {
                    is SongItem ->
                        YouTubeSongMenu(
                            song = item,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )

                    is AlbumItem ->
                        YouTubeAlbumMenu(
                            albumItem = item,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )

                    is ArtistItem ->
                        YouTubeArtistMenu(
                            artist = item,
                            onDismiss = menuState::dismiss,
                        )

                    is PlaylistItem ->
                        YouTubePlaylistMenu(
                            playlist = item,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss,
                        )
                }
            }
        }
        YouTubeListItem(
            item = item,
            isActive =
            when (item) {
                is SongItem -> mediaMetadata?.id == item.id
                is AlbumItem -> mediaMetadata?.album?.id == item.id
                else -> false
            },
            isPlaying = isPlaying,
            trailingContent = {
                IconButton(
                    onClick = longClick,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                    )
                }
            },
            modifier =
            Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> {
                                if (item.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = item.id),
                                            item.toMediaMetadata()
                                        )
                                    )
                                }
                            }

                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = longClick,
                )
                .animateItem(),
        )
    }

    LazyColumn(
        state = lazyListState,
        contentPadding =
        LocalPlayerAwareWindowInsets.current
            .add(WindowInsets(top = SearchFilterHeight + 8.dp))
            .asPaddingValues(),
    ) {
        if (searchFilter == null) {
            searchSummary?.summaries?.forEachIndexed { index, summary ->
                if (index > 0) {
                    item(key = "divider_$index") {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = summary.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                items(
                    items = summary.items,
                    key = { "${summary.title}/${it.id}/${summary.items.indexOf(it)}" },
                    itemContent = ytItemContent,
                )

                item {
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (searchSummary?.summaries?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        } else {
            items(
                items = itemsPage?.items.orEmpty().distinctBy { it.id },
                key = { "filtered_${it.id}" },
                itemContent = ytItemContent,
            )

            if (itemsPage?.continuation != null) {
                item(key = "loading") {
                    ShimmerHost {
                        repeat(3) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }

            if (itemsPage?.items?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        }

        if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
            item {
                ShimmerHost {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top).add(WindowInsets(top = AppBarHeight)))
            .fillMaxWidth()
    ) {
        ChipsRow(
            chips =
            listOf(
                null to stringResource(R.string.filter_all),
                FILTER_SONG to stringResource(R.string.filter_songs),
                FILTER_VIDEO to stringResource(R.string.filter_videos),
                FILTER_ALBUM to stringResource(R.string.filter_albums),
                FILTER_ARTIST to stringResource(R.string.filter_artists),
                FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
                FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists),
            ),
            currentValue = searchFilter,
            onValueUpdate = {
                if (viewModel.filter.value != it) {
                    viewModel.filter.value = it
                }
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            },
        )
    }
}
