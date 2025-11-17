package moe.koiverse.archivetune.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.palette.graphics.Palette
import androidx.core.graphics.drawable.toBitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.LocalDownloadUtil
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.AlbumThumbnailSize
import moe.koiverse.archivetune.constants.HideExplicitKey
import moe.koiverse.archivetune.constants.ThumbnailCornerRadius
import moe.koiverse.archivetune.constants.DisableBlurKey
import moe.koiverse.archivetune.db.entities.Album
import moe.koiverse.archivetune.ui.theme.PlayerColorExtractor
import moe.koiverse.archivetune.extensions.togglePlayPause
import moe.koiverse.archivetune.playback.ExoDownloadService
import moe.koiverse.archivetune.playback.queues.LocalAlbumRadio
import moe.koiverse.archivetune.ui.component.AutoResizeText
import moe.koiverse.archivetune.ui.component.FontSizeRange
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.component.NavigationTitle
import moe.koiverse.archivetune.ui.component.SongListItem
import moe.koiverse.archivetune.ui.component.YouTubeGridItem
import moe.koiverse.archivetune.ui.component.shimmer.ButtonPlaceholder
import moe.koiverse.archivetune.ui.component.shimmer.ListItemPlaceHolder
import moe.koiverse.archivetune.ui.component.shimmer.ShimmerHost
import moe.koiverse.archivetune.ui.component.shimmer.TextPlaceholder
import moe.koiverse.archivetune.ui.menu.AlbumMenu
import moe.koiverse.archivetune.ui.menu.SelectionSongMenu
import moe.koiverse.archivetune.ui.menu.SongMenu
import moe.koiverse.archivetune.ui.menu.YouTubeAlbumMenu
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.ui.utils.ItemWrapper
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.AlbumViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    
    // Gradient colors state for album cover
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    
    // Extract gradient colors from album cover
    LaunchedEffect(albumWithSongs?.album?.thumbnailUrl) {
        val thumbnailUrl = albumWithSongs?.album?.thumbnailUrl
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                .allowHardware(false)
                .build()

            val result = runCatching { 
                context.imageLoader.execute(request)
            }.getOrNull()
            
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
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

    val wrappedSongs = remember(albumWithSongs, hideExplicit) {
        val filteredSongs = if (hideExplicit) {
            albumWithSongs?.songs?.filter { !it.song.explicit } ?: emptyList()
        } else {
            albumWithSongs?.songs ?: emptyList()
        }
        filteredSongs.map { item -> ItemWrapper(item) }.toMutableStateList()
    }
    var selection by remember {
        mutableStateOf(false)
    }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }
    
    // State for LazyColumn to track scroll
    val lazyListState = rememberLazyListState()
    
    // Calculate gradient opacity based on scroll position
    val gradientAlpha by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                val offset = lazyListState.firstVisibleItemScrollOffset
                // Fade out over 900dp of scrolling
                (1f - (offset / 900f)).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }
    
    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Mesh gradient background layer
        if (!disableBlur && gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.7f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawBehind {
                        val width = size.width
                        val height = size.height
                        
                        if (gradientColors.size >= 3) {
                            // First color blob - top left
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        gradientColors[0].copy(alpha = gradientAlpha * 0.7f),
                                        gradientColors[0].copy(alpha = gradientAlpha * 0.4f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.2f, height * 0.15f),
                                    radius = width * 0.6f
                                )
                            )
                            
                            // Second color blob - top right
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        gradientColors[1].copy(alpha = gradientAlpha * 0.6f),
                                        gradientColors[1].copy(alpha = gradientAlpha * 0.35f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.8f, height * 0.25f),
                                    radius = width * 0.7f
                                )
                            )
                            
                            // Third color blob - middle
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        gradientColors[2].copy(alpha = gradientAlpha * 0.5f),
                                        gradientColors[2].copy(alpha = gradientAlpha * 0.25f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.5f, height * 0.5f),
                                    radius = width * 0.8f
                                )
                            )
                        } else {
                            // Fallback: single radial gradient
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        gradientColors[0].copy(alpha = gradientAlpha * 0.7f),
                                        gradientColors[0].copy(alpha = gradientAlpha * 0.4f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.5f, height * 0.3f),
                                    radius = width * 0.8f
                                )
                            )
                        }
                    }
            ) {}
        }

    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        val albumWithSongs = albumWithSongs
        if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = albumWithSongs.album.thumbnailUrl,
                            contentDescription = null,
                            modifier =
                            Modifier
                                .size(AlbumThumbnailSize)
                                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                        )

                        Spacer(Modifier.width(16.dp))

                        Column(
                            verticalArrangement = Arrangement.Center,
                        ) {
                            AutoResizeText(
                                text = albumWithSongs.album.title,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSizeRange = FontSizeRange(16.sp, 22.sp),
                            )

                            Text(buildAnnotatedString {
                                withStyle(
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onBackground
                                    ).toSpanStyle()
                                ) {
                                    albumWithSongs.artists.fastForEachIndexed { index, artist ->
                                        val link = LinkAnnotation.Clickable(artist.id) {
                                            navController.navigate("artist/${artist.id}")
                                        }
                                        withLink(link) {
                                            append(artist.name)
                                        }
                                        if (index != albumWithSongs.artists.lastIndex) {
                                            append(", ")
                                        }
                                    }
                                }
                            })

                            if (albumWithSongs.album.year != null) {
                                Text(
                                    text = albumWithSongs.album.year.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        database.query {
                                            update(albumWithSongs.album.toggleLike())
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter =
                                        painterResource(
                                            if (albumWithSongs.album.bookmarkedAt !=
                                                null
                                            ) {
                                                R.drawable.favorite
                                            } else {
                                                R.drawable.favorite_border
                                            },
                                        ),
                                        contentDescription = null,
                                        tint =
                                        if (albumWithSongs.album.bookmarkedAt !=
                                            null
                                        ) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            LocalContentColor.current
                                        },
                                    )
                                }

                                when (downloadState) {
                                    Download.STATE_COMPLETED -> {
                                        IconButton(
                                            onClick = {
                                                albumWithSongs.songs.forEach { song ->
                                                    DownloadService.sendRemoveDownload(
                                                        context,
                                                        ExoDownloadService::class.java,
                                                        song.id,
                                                        false,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.offline),
                                                contentDescription = null,
                                            )
                                        }
                                    }

                                    Download.STATE_DOWNLOADING -> {
                                        IconButton(
                                            onClick = {
                                                albumWithSongs.songs.forEach { song ->
                                                    DownloadService.sendRemoveDownload(
                                                        context,
                                                        ExoDownloadService::class.java,
                                                        song.id,
                                                        false,
                                                    )
                                                }
                                            },
                                        ) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }

                                    else -> {
                                        IconButton(
                                            onClick = {
                                                albumWithSongs.songs.forEach { song ->
                                                    val downloadRequest =
                                                        DownloadRequest
                                                            .Builder(song.id, song.id.toUri())
                                                            .setCustomCacheKey(song.id)
                                                            .setData(song.song.title.toByteArray())
                                                            .build()
                                                    DownloadService.sendAddDownload(
                                                        context,
                                                        ExoDownloadService::class.java,
                                                        downloadRequest,
                                                        false,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.download),
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            AlbumMenu(
                                                originalAlbum = Album(
                                                    albumWithSongs.album,
                                                    albumWithSongs.artists
                                                ),
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                playerConnection.service.getAutomix(playlistId)
                                playerConnection.playQueue(
                                    LocalAlbumRadio(albumWithSongs),
                                )
                            },
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.play),
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(
                                text = stringResource(R.string.play),
                            )
                        }

                    Button(
                        onClick = {
                            playerConnection.service.getAutomix(playlistId)
                            playerConnection.playQueue(
                                LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled())),
                            )
                        },
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = Modifier.weight(1f),
                    ) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.shuffle))
                        }
                    }
                }
            }

            if (!wrappedSongs.isNullOrEmpty()) {
                itemsIndexed(
                    items = wrappedSongs,
                    key = { _, song -> song.item.id },
                ) { index, songWrapper ->
                    SongListItem(
                        song = songWrapper.item,
                        albumIndex = index + 1,
                        isActive = songWrapper.item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        showInLibraryIcon = true,

                        trailingContent = {
                            IconButton(
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
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                )
                            }
                        },
                        isSelected = songWrapper.isSelected && selection,
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!selection) {
                                        if (songWrapper.item.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.service.getAutomix(playlistId)
                                            playerConnection.playQueue(
                                                LocalAlbumRadio(albumWithSongs, startIndex = index),
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
                                    wrappedSongs.forEach {
                                        it.isSelected = false
                                    } // Clear previous selections
                                    songWrapper.isSelected = true // Select the current item
                                },
                            ),
                    )
                }
            }

            if (otherVersions.isNotEmpty()) {
                item {
                    NavigationTitle(
                        title = stringResource(R.string.other_versions),
                    )
                }
                item {
                    LazyRow {
                        items(
                            items = otherVersions.distinctBy { it.id },
                            key = { it.id },
                        ) { item ->
                            YouTubeGridItem(
                                item = item,
                                isActive = mediaMetadata?.album?.id == item.id,
                                isPlaying = isPlaying,
                                coroutineScope = scope,
                                modifier =
                                Modifier
                                    .combinedClickable(
                                        onClick = { navController.navigate("album/${item.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = item,
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
                }
            }
        } else {
            item {
                ShimmerHost {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(
                                modifier =
                                Modifier
                                    .size(AlbumThumbnailSize)
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                    .background(MaterialTheme.colorScheme.onSurface),
                            )

                            Spacer(Modifier.width(16.dp))

                            Column(
                                verticalArrangement = Arrangement.Center,
                            ) {
                                TextPlaceholder()
                                TextPlaceholder()
                                TextPlaceholder()
                            }
                        }

                        Spacer(Modifier.padding(8.dp))

                        Row {
                            ButtonPlaceholder(Modifier.weight(1f))

                            Spacer(Modifier.width(12.dp))

                            ButtonPlaceholder(Modifier.weight(1f))
                        }
                    }

                    repeat(6) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (showTopBarTitle) MaterialTheme.colorScheme.surface else Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
            if (selection) {
                val count = wrappedSongs?.count { it.isSelected } ?: 0
                Text(
                    text = pluralStringResource(R.plurals.n_song, count, count),
                    style = MaterialTheme.typography.titleLarge
                )
            } else {
                Text(
                    text = albumWithSongs?.album?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (selection) {
                        selection = false
                    } else {
                        navController.navigateUp()
                    }
                },
                onLongClick = {
                    if (!selection) {
                        navController.backToMain()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (selection) R.drawable.close else R.drawable.arrow_back
                    ),
                    contentDescription = null
                )
            }
        },
        actions = {
            if (selection) {
                val count = wrappedSongs?.count { it.isSelected } ?: 0
                IconButton(
                    onClick = {
                        if (count == wrappedSongs?.size) {
                            wrappedSongs.forEach { it.isSelected = false }
                        } else {
                            wrappedSongs?.forEach { it.isSelected = true }
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(
                            if (count == wrappedSongs?.size) R.drawable.deselect else R.drawable.select_all
                        ),
                        contentDescription = null
                    )
                }

                IconButton(
                    onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = wrappedSongs?.filter { it.isSelected }!!
                                    .map { it.item },
                                onDismiss = menuState::dismiss,
                                clearAction = { selection = false }
                            )
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null
                    )
                }
            }
        }
    )
    }
}
