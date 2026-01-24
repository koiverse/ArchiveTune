package moe.koiverse.archivetune.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.DisableBlurKey
import moe.koiverse.archivetune.db.entities.Album
import moe.koiverse.archivetune.db.entities.Artist
import moe.koiverse.archivetune.db.entities.SongWithStats
import moe.koiverse.archivetune.extensions.togglePlayPause
import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.models.toMediaMetadata
import moe.koiverse.archivetune.playback.queues.YouTubeQueue
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.menu.ArtistMenu
import moe.koiverse.archivetune.ui.menu.SongMenu
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.ComposeToImage
import moe.koiverse.archivetune.utils.joinByBullet
import moe.koiverse.archivetune.utils.makeTimeString
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.YearInMusicViewModel
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YearInMusicScreen(
    navController: NavController,
    viewModel: YearInMusicViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val availableYears by viewModel.availableYears.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val topSongsStats by viewModel.topSongsStats.collectAsState()
    val topSongs by viewModel.topSongs.collectAsState()
    val topArtists by viewModel.topArtists.collectAsState()
    val topAlbums by viewModel.topAlbums.collectAsState()
    val totalListeningTime by viewModel.totalListeningTime.collectAsState()
    val totalSongsPlayed by viewModel.totalSongsPlayed.collectAsState()

    var isGeneratingImage by remember { mutableStateOf(false) }
    var isShareCaptureMode by remember { mutableStateOf(false) }
    var shareBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var isYearPickerOpen by remember { mutableStateOf(false) }
    var recapCurrentPage by remember { mutableStateOf(0) }
    var recapLastPage by remember { mutableStateOf(0) }

    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val color4 = MaterialTheme.colorScheme.primaryContainer
    val color5 = MaterialTheme.colorScheme.secondaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface
    val shareBackgroundArgb = surfaceColor.toArgb()
    val view = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                shareBounds = coordinates.boundsInRoot()
            }
    ) {
        // Gradient background
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.7f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawWithCache {
                        val width = this.size.width
                        val height = this.size.height

                        val brush1 = Brush.radialGradient(
                            colors = listOf(
                                color1.copy(alpha = 0.45f),
                                color1.copy(alpha = 0.28f),
                                color1.copy(alpha = 0.16f),
                                color1.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.2f, height * 0.1f),
                            radius = width * 0.6f
                        )

                        val brush2 = Brush.radialGradient(
                            colors = listOf(
                                color2.copy(alpha = 0.4f),
                                color2.copy(alpha = 0.24f),
                                color2.copy(alpha = 0.14f),
                                color2.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.8f, height * 0.25f),
                            radius = width * 0.7f
                        )

                        val brush3 = Brush.radialGradient(
                            colors = listOf(
                                color3.copy(alpha = 0.35f),
                                color3.copy(alpha = 0.2f),
                                color3.copy(alpha = 0.1f),
                                color3.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.4f, height * 0.5f),
                            radius = width * 0.65f
                        )

                        val overlayBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                surfaceColor.copy(alpha = 0.3f),
                                surfaceColor.copy(alpha = 0.7f),
                                surfaceColor
                            ),
                            startY = height * 0.4f,
                            endY = height
                        )

                        onDrawBehind {
                            drawRect(brush = brush1)
                            drawRect(brush = brush2)
                            drawRect(brush = brush3)
                            drawRect(brush = overlayBrush)
                        }
                    }
            )
        }

        YearInMusicStoryPager(
            year = selectedYear,
            totalListeningTime = totalListeningTime,
            totalSongsPlayed = totalSongsPlayed,
            topSongsStats = topSongsStats,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbums = topAlbums,
            isPlaying = isPlaying,
            mediaMetadataId = mediaMetadata?.id,
            navController = navController,
            menuState = menuState,
            haptic = haptic,
            playerConnection = playerConnection,
            coroutineScope = coroutineScope,
            isShareCaptureMode = isShareCaptureMode,
            onPagerStateChanged = { current, last ->
                recapCurrentPage = current
                recapLastPage = last
            },
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
                )
        )

        if (topSongsStats.isNotEmpty() || topArtists.isNotEmpty() || topAlbums.isNotEmpty()) {
            if (!isShareCaptureMode) {
                if (recapCurrentPage == recapLastPage) FloatingActionButton(
                    onClick = {
                        if (!isGeneratingImage) {
                            isGeneratingImage = true
                            coroutineScope.launch {
                                try {
                                    isShareCaptureMode = true
                                    awaitNextPreDraw(view)
                                    awaitNextPreDraw(view)

                                    val raw = ComposeToImage.captureViewBitmap(
                                        view = view,
                                        backgroundColor = shareBackgroundArgb,
                                    )
                                    val bounds = shareBounds
                                    val cropped =
                                        if (bounds != null) {
                                            ComposeToImage.cropBitmap(
                                                source = raw,
                                                left = bounds.left.toInt(),
                                                top = bounds.top.toInt(),
                                                width = bounds.width.toInt(),
                                                height = bounds.height.toInt(),
                                            )
                                        } else {
                                            raw
                                        }
                                    val fitted = ComposeToImage.fitBitmap(
                                        source = cropped,
                                        targetWidth = 1080,
                                        targetHeight = 1920,
                                        backgroundColor = shareBackgroundArgb,
                                    )

                                    val uri = ComposeToImage.saveBitmapAsFile(
                                        context,
                                        fitted,
                                        "ArchiveTune_YearInMusic_$selectedYear"
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            context.getString(R.string.share_summary)
                                        )
                                    )
                                } finally {
                                    isShareCaptureMode = false
                                    isGeneratingImage = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .windowInsetsPadding(
                            LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
                        ),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    if (isGeneratingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = stringResource(R.string.share_summary)
                        )
                    }
                }
            }
        }

        if (!isShareCaptureMode) {
            TopAppBar(
                title = { Text(stringResource(R.string.year_in_music)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isYearPickerOpen = true },
                        onLongClick = {},
                    ) {
                        Icon(
                            painterResource(R.drawable.calendar_today),
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }

        if (!isShareCaptureMode && isYearPickerOpen) {
            YearInMusicYearPickerDialog(
                availableYears = availableYears,
                selectedYear = selectedYear,
                onSelectYear = { year ->
                    viewModel.selectedYear.value = year
                    isYearPickerOpen = false
                },
                onDismiss = { isYearPickerOpen = false }
            )
        }
    }
}

private suspend fun awaitNextPreDraw(view: View) {
    suspendCancellableCoroutine { cont ->
        val vto = view.viewTreeObserver
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (vto.isAlive) vto.removeOnPreDrawListener(this)
                cont.resume(Unit)
                return true
            }
        }
        vto.addOnPreDrawListener(listener)
        cont.invokeOnCancellation {
            if (vto.isAlive) vto.removeOnPreDrawListener(listener)
        }
        view.invalidate()
    }
}

@Composable
private fun YearInMusicStoryPager(
    year: Int,
    totalListeningTime: Long,
    totalSongsPlayed: Long,
    topSongsStats: List<SongWithStats>,
    topSongs: List<moe.koiverse.archivetune.db.entities.Song>,
    topArtists: List<Artist>,
    topAlbums: List<Album>,
    isPlaying: Boolean,
    mediaMetadataId: String?,
    navController: NavController,
    menuState: moe.koiverse.archivetune.ui.component.MenuState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    playerConnection: moe.koiverse.archivetune.playback.PlayerConnection,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    isShareCaptureMode: Boolean,
    onPagerStateChanged: (currentPage: Int, lastPage: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = remember(topSongsStats, topArtists, topAlbums) {
        buildList {
            add(YearInMusicStoryPage.Hero)
            if (topSongsStats.isNotEmpty()) add(YearInMusicStoryPage.TopSong)
            if (topArtists.isNotEmpty()) add(YearInMusicStoryPage.TopArtist)
            if (topAlbums.isNotEmpty()) add(YearInMusicStoryPage.TopAlbum)
            add(YearInMusicStoryPage.Summary)
        }
    }

    var currentPage by remember { mutableStateOf(0) }
    val lastPage = pages.lastIndex.coerceAtLeast(0)

    LaunchedEffect(lastPage) {
        currentPage = currentPage.coerceIn(0, lastPage)
    }

    LaunchedEffect(isShareCaptureMode, lastPage) {
        if (isShareCaptureMode) currentPage = lastPage
    }

    LaunchedEffect(currentPage, lastPage) {
        onPagerStateChanged(currentPage, lastPage)
    }

    fun navigateTo(page: Int) {
        currentPage = page.coerceIn(0, lastPage)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                slideInHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    initialOffsetX = { it * direction }
                ) + fadeIn(animationSpec = tween(180)) togetherWith
                    slideOutHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        targetOffsetX = { -it * direction }
                    ) + fadeOut(animationSpec = tween(140))
            },
            label = "yearInMusicPage"
        ) { pageIndex ->
            when (pages.getOrNull(pageIndex)) {
                YearInMusicStoryPage.Hero -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = !isShareCaptureMode && currentPage < lastPage) {
                                navigateTo(currentPage + 1)
                            }
                    ) {
                        YearInMusicHeroStoryCard(
                            year = year,
                            totalListeningTime = totalListeningTime,
                            totalSongsPlayed = totalSongsPlayed
                        )
                    }
                }

                YearInMusicStoryPage.TopSong -> {
                    val topSong = topSongsStats.firstOrNull()
                    val topSongEntity = topSongs.firstOrNull()
                    if (topSong != null) {
                        YearInMusicTopSongStoryCard(
                            song = topSong,
                            onClick = {
                                if (topSong.id == mediaMetadataId) {
                                    playerConnection.player.togglePlayPause()
                                } else if (topSongEntity != null) {
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            endpoint = WatchEndpoint(topSong.id),
                                            preloadItem = topSongEntity.toMediaMetadata()
                                        )
                                    )
                                }
                            },
                            onLongClick = {
                                if (topSongEntity != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = topSongEntity,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                YearInMusicStoryPage.TopArtist -> {
                    val topArtist = topArtists.firstOrNull()
                    if (topArtist != null) {
                        YearInMusicTopArtistStoryCard(
                            artist = topArtist,
                            onClick = { navController.navigate("artist/${topArtist.id}") },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    ArtistMenu(
                                        originalArtist = topArtist,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                    }
                }

                YearInMusicStoryPage.TopAlbum -> {
                    val topAlbum = topAlbums.firstOrNull()
                    if (topAlbum != null) {
                        YearInMusicTopAlbumStoryCard(
                            album = topAlbum,
                            onClick = { navController.navigate("album/${topAlbum.id}") }
                        )
                    }
                }

                YearInMusicStoryPage.Summary -> {
                    YearInMusicSummaryStoryCard(
                        year = year,
                        totalListeningTime = totalListeningTime,
                        totalSongsPlayed = totalSongsPlayed,
                        topSong = topSongsStats.firstOrNull(),
                        topArtist = topArtists.firstOrNull(),
                        topAlbum = topAlbums.firstOrNull()
                    )
                }

                null -> Unit
            }
        }

        if (!isShareCaptureMode) {
            YearInMusicStoryProgressIndicator(
                totalPages = pages.size,
                currentPage = currentPage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    )
                    .padding(top = 12.dp)
            )

            YearInMusicStoryNavBar(
                canGoBack = currentPage > 0,
                canGoNext = currentPage < lastPage,
                pageLabel = when (pages.getOrNull(currentPage)) {
                    YearInMusicStoryPage.Hero -> stringResource(R.string.year_in_music)
                    YearInMusicStoryPage.TopSong -> stringResource(R.string.top_songs)
                    YearInMusicStoryPage.TopArtist -> stringResource(R.string.top_artists)
                    YearInMusicStoryPage.TopAlbum -> stringResource(R.string.albums)
                    YearInMusicStoryPage.Summary -> stringResource(R.string.share_summary)
                    null -> ""
                },
                onBack = { navigateTo(currentPage - 1) },
                onNext = { navigateTo(currentPage + 1) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun YearInMusicYearPickerDialog(
    availableYears: List<Int>,
    selectedYear: Int,
    onSelectYear: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.year_in_music)) },
        text = {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableYears) { year ->
                    FilterChip(
                        selected = year == selectedYear,
                        onClick = { onSelectYear(year) },
                        label = {
                            Text(
                                text = year.toString(),
                                fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss))
            }
        }
    )
}

@Composable
private fun YearInMusicStoryProgressIndicator(
    totalPages: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            val activeAlpha by animateFloatAsState(
                targetValue = when {
                    index < currentPage -> 0.7f
                    index == currentPage -> 1f
                    else -> 0.12f
                },
                animationSpec = tween(durationMillis = 220),
                label = "progressAlpha"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = activeAlpha))
                )
            }
        }
    }
}

@Composable
private fun YearInMusicStoryNavBar(
    canGoBack: Boolean,
    canGoNext: Boolean,
    pageLabel: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                onLongClick = {},
                enabled = canGoBack,
                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null
                )
            }

            Text(
                text = pageLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                    .clickable(enabled = canGoNext, onClick = onNext)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.next),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
@Composable
private fun YearInMusicHeroStoryCard(
    year: Int,
    totalListeningTime: Long,
    totalSongsPlayed: Long
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
        )
    )

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        val transition = rememberInfiniteTransition(label = "introGlow")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "introGlowPhase"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val c1 = Offset(w * (0.15f + 0.7f * phase), h * (0.15f + 0.25f * phase))
                    val c2 = Offset(w * (0.85f - 0.7f * phase), h * (0.45f + 0.35f * phase))
                    val aura1 = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = c1,
                        radius = w * 0.75f
                    )
                    val aura2 = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = c2,
                        radius = w * 0.9f
                    )

                    onDrawBehind {
                        drawRect(brush = aura1)
                        drawRect(brush = aura2)
                    }
                }
                .padding(28.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.your_year_in_music, year),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = makeTimeString(totalListeningTime),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = joinByBullet(
                        stringResource(R.string.total_listening_time),
                        pluralStringResource(
                            R.plurals.n_song,
                            totalSongsPlayed.toInt(),
                            totalSongsPlayed.toInt()
                        ) + " " + stringResource(R.string.played)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
                Text(
                    text = stringResource(R.string.next),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun YearInMusicTopSongStoryCard(
    song: SongWithStats,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = song.thumbnailUrl,
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
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.top_songs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = joinByBullet(
                        pluralStringResource(
                            R.plurals.n_time,
                            song.songCountListened,
                            song.songCountListened
                        ),
                        makeTimeString(song.timeListened)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun YearInMusicTopArtistStoryCard(
    artist: Artist,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = artist.artist.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.top_artists),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
                AsyncImage(
                    model = artist.artist.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(148.dp)
                        .clip(CircleShape)
                )
                Text(
                    text = artist.artist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = joinByBullet(
                        pluralStringResource(
                            R.plurals.n_time,
                            artist.songCount,
                            artist.songCount
                        ),
                        makeTimeString(artist.timeListened?.toLong())
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun YearInMusicTopAlbumStoryCard(
    album: Album,
    onClick: () -> Unit
) {
    val artistNames = album.artists.take(2).joinToString(" • ") { it.name }
    val artistDisplay = artistNames.takeIf { it.isNotBlank() }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = album.thumbnailUrl,
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
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.albums),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = album.album.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = joinByBullet(
                        artistDisplay,
                        album.songCountListened?.let {
                            pluralStringResource(R.plurals.n_time, it, it)
                        },
                        makeTimeString(album.timeListened)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun YearInMusicSummaryStoryCard(
    year: Int,
    totalListeningTime: Long,
    totalSongsPlayed: Long,
    topSong: SongWithStats?,
    topArtist: Artist?,
    topAlbum: Album?
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.year_in_music),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = makeTimeString(totalListeningTime),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.n_song,
                        totalSongsPlayed.toInt(),
                        totalSongsPlayed.toInt()
                    ) + " " + stringResource(R.string.played),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = joinByBullet(
                        topSong?.title,
                        topArtist?.artist?.name,
                        topAlbum?.album?.title
                    ).ifEmpty { stringResource(R.string.no_listening_data) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.share_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private enum class YearInMusicStoryPage {
    Hero,
    TopSong,
    TopArtist,
    TopAlbum,
    Summary
}
