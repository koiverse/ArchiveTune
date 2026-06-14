/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.library

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.AppBarHeight
import moe.rukamori.archivetune.constants.ChipSortTypeKey
import moe.rukamori.archivetune.constants.LibraryFilter
import moe.rukamori.archivetune.utils.rememberEnumPreference
import moe.rukamori.archivetune.utils.rememberPreference

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    val database = LocalDatabase.current
    val (selectedTagIds) = rememberPlaylistTagFilterState(database)

    val pagerState = rememberPagerState(
        initialPage = remember {
            when (filterType) {
                LibraryFilter.LIBRARY -> 0
                LibraryFilter.PLAYLISTS -> 1
                LibraryFilter.SONGS -> 2
                LibraryFilter.ARTISTS -> 3
                LibraryFilter.ALBUMS -> 4
                else -> 0
            }
        }
    ) { 5 }

    val currentFilter = when (pagerState.currentPage) {
        0 -> LibraryFilter.LIBRARY
        1 -> LibraryFilter.PLAYLISTS
        2 -> LibraryFilter.SONGS
        3 -> LibraryFilter.ARTISTS
        4 -> LibraryFilter.ALBUMS
        else -> LibraryFilter.LIBRARY
    }

    // Dynamic header content based on selection
    val headerTitle = when (currentFilter) {
        LibraryFilter.LIBRARY -> stringResource(R.string.library_title)
        LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
        LibraryFilter.SONGS -> stringResource(R.string.songs)
        LibraryFilter.ARTISTS -> stringResource(R.string.artists)
        LibraryFilter.ALBUMS -> stringResource(R.string.albums)
        else -> stringResource(R.string.library_title)
    }

    val headerSubtitle = when (currentFilter) {
        LibraryFilter.LIBRARY -> stringResource(R.string.library_subtitle)
        LibraryFilter.PLAYLISTS -> stringResource(R.string.library_playlists_subtitle)
        LibraryFilter.SONGS -> stringResource(R.string.library_songs_subtitle)
        LibraryFilter.ARTISTS -> stringResource(R.string.library_artists_subtitle)
        LibraryFilter.ALBUMS -> stringResource(R.string.library_albums_subtitle)
        else -> stringResource(R.string.library_subtitle)
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val maxHeaderHeight = 90.dp
    val maxHeaderOffsetPx = with(density) { maxHeaderHeight.toPx() }
    var headerOffsetPx by rememberSaveable { mutableStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // Scrolling down the page (dragging finger up, delta < 0): collapse header first
                if (delta < 0) {
                    val newOffset = headerOffsetPx + delta
                    val oldOffset = headerOffsetPx
                    headerOffsetPx = newOffset.coerceIn(-maxHeaderOffsetPx, 0f)
                    val consumedY = headerOffsetPx - oldOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                // Scrolling up the page (dragging finger down, delta > 0): expand header ONLY if list is at top
                if (delta > 0) {
                    val newOffset = headerOffsetPx + delta
                    val oldOffset = headerOffsetPx
                    headerOffsetPx = newOffset.coerceIn(-maxHeaderOffsetPx, 0f)
                    val consumedY = headerOffsetPx - oldOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }
        }
    }

    // Only collapse the header after the first few items have scrolled past
    // We use a larger header height so the collapse feels more gradual

    val headerHeight = maxHeaderHeight + with(density) { headerOffsetPx.toDp() }
    val progress = 1f + (headerOffsetPx / maxHeaderOffsetPx)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = AppBarHeight)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Main Top Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .clipToBounds()
                    .graphicsLayer { alpha = progress }
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = headerSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            val tabListState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            // Sync Pager -> Preference & lazy list centering
            LaunchedEffect(pagerState.currentPage) {
                headerOffsetPx = 0f
                val targetFilter = when (pagerState.currentPage) {
                    0 -> LibraryFilter.LIBRARY
                    1 -> LibraryFilter.PLAYLISTS
                    2 -> LibraryFilter.SONGS
                    3 -> LibraryFilter.ARTISTS
                    4 -> LibraryFilter.ALBUMS
                    else -> LibraryFilter.LIBRARY
                }
                if (filterType != targetFilter) {
                    filterType = targetFilter
                }

                // Centering the tab chip scroll alignment
                val tabWidth = when (targetFilter) {
                    LibraryFilter.LIBRARY -> 116.dp
                    LibraryFilter.PLAYLISTS -> 132.dp
                    LibraryFilter.SONGS -> 102.dp
                    LibraryFilter.ARTISTS -> 116.dp
                    LibraryFilter.ALBUMS -> 110.dp
                    else -> 116.dp
                }
                val screenWidth = configuration.screenWidthDp.dp
                val targetOffsetDp = (screenWidth - tabWidth) / 2
                val targetOffsetPx = with(density) { targetOffsetDp.roundToPx() }
                
                tabListState.animateScrollToItem(pagerState.currentPage, scrollOffset = -targetOffsetPx)
            }

            // Expressive Tab Chips Row
            LazyRow(
                state = tabListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    ExpressiveTabChip(
                        label = stringResource(R.string.filter_library),
                        iconRes = R.drawable.graphic_eq,
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        }
                    )
                }
                item {
                    ExpressiveTabChip(
                        label = stringResource(R.string.playlists),
                        iconRes = R.drawable.queue_music,
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                }
                item {
                    ExpressiveTabChip(
                        label = stringResource(R.string.songs),
                        iconRes = R.drawable.music_note,
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        }
                    )
                }
                item {
                    ExpressiveTabChip(
                        label = stringResource(R.string.artists),
                        iconRes = R.drawable.person,
                        selected = pagerState.currentPage == 3,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        }
                    )
                }
                item {
                    ExpressiveTabChip(
                        label = stringResource(R.string.albums),
                        iconRes = R.drawable.album,
                        selected = pagerState.currentPage == 4,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(4)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> {
                        LibraryMixScreen(
                            navController = navController,
                            filterContent = {},
                            selectedTagIds = selectedTagIds,
                            onTabSelected = { targetFilter ->
                                coroutineScope.launch {
                                    val targetPage = when (targetFilter) {
                                        LibraryFilter.LIBRARY -> 0
                                        LibraryFilter.PLAYLISTS -> 1
                                        LibraryFilter.SONGS -> 2
                                        LibraryFilter.ARTISTS -> 3
                                        LibraryFilter.ALBUMS -> 4
                                        else -> 0
                                    }
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            }
                        )
                    }
                    1 -> {
                        LibraryPlaylistsScreen(
                            navController = navController,
                            filterContent = {},
                            selectedTagIds = selectedTagIds
                        )
                    }
                    2 -> {
                        LibrarySongsScreen(
                            navController = navController,
                            onDeselect = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                    }
                    3 -> {
                        LibraryArtistsScreen(
                            navController = navController,
                            onDeselect = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                    }
                    4 -> {
                        LibraryAlbumsScreen(
                            navController = navController,
                            onDeselect = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpressiveTabChip(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else if (selected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "TabChipScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "TabChipBgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "TabChipContentColor"
    )

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = contentColor
        )
    }
}

