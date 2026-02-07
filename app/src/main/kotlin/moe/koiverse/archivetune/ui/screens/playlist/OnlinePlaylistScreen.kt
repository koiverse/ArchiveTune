/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.AppBarHeight
import moe.koiverse.archivetune.constants.DisableBlurKey
import moe.koiverse.archivetune.constants.HideExplicitKey
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import moe.koiverse.archivetune.db.entities.PlaylistSongMap
import moe.koiverse.archivetune.extensions.metadata
import moe.koiverse.archivetune.extensions.toMediaItem
import moe.koiverse.archivetune.extensions.togglePlayPause
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.models.toMediaMetadata
import moe.koiverse.archivetune.playback.queues.YouTubeQueue
import moe.koiverse.archivetune.ui.component.DraggableScrollbar
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.component.YouTubeListItem
import moe.koiverse.archivetune.ui.component.shimmer.ButtonPlaceholder
import moe.koiverse.archivetune.ui.component.shimmer.ListItemPlaceHolder
import moe.koiverse.archivetune.ui.component.shimmer.ShimmerHost
import moe.koiverse.archivetune.ui.component.shimmer.TextPlaceholder
import moe.koiverse.archivetune.ui.menu.SelectionMediaMetadataMenu
import moe.koiverse.archivetune.ui.menu.YouTubePlaylistMenu
import moe.koiverse.archivetune.ui.menu.YouTubeSongMenu
import moe.koiverse.archivetune.ui.theme.PlayerColorExtractor
import moe.koiverse.archivetune.ui.utils.ItemWrapper
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.OnlinePlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()

    var selection by remember { mutableStateOf(false) }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    // System bars padding
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by
        rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs =
        remember(songs, query) {
            if (query.text.isEmpty()) {
                songs.mapIndexed { index, song -> index to song }
            } else {
                songs
                    .mapIndexed { index, song -> index to song }
                    .filter { (_, song) ->
                        song.title.contains(query.text, ignoreCase = true) ||
                            song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
                    }
            }
        }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler { selection = false }
    }

    val wrappedSongs =
        remember(filteredSongs) { filteredSongs.map { item -> ItemWrapper(item) } }
            .toMutableStateList()

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }

    // Gradient colors state for playlist cover
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Extract gradient colors from playlist cover
    LaunchedEffect(playlist?.thumbnail) {
        val thumbnailUrl = playlist?.thumbnail
        if (thumbnailUrl != null) {
            val request =
                ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(
                        PlayerColorExtractor.Config.IMAGE_SIZE,
                        PlayerColorExtractor.Config.IMAGE_SIZE
                    )
                    .allowHardware(false)
                    .build()

            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()

            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette =
                        withContext(Dispatchers.Default) {
                            Palette.from(bitmap)
                                .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                                .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                                .generate()
                        }

                    val extractedColors =
                        PlayerColorExtractor.extractGradientColors(
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

    // Calculate gradient opacity based on scroll position
    val gradientAlpha by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                val offset = lazyListState.firstVisibleItemScrollOffset
                (1f - (offset / 600f)).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val transparentAppBar by remember {
        derivedStateOf { !disableBlur && !selection && !showTopBarTitle }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (
                    songs.size >= 5 &&
                        lastVisibleIndex != null &&
                        lastVisibleIndex >= songs.size - 5
                ) {
                    viewModel.loadMoreSongs()
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor),
    ) {
        // Mesh gradient background layer
        if (!disableBlur && gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .fillMaxSize(0.55f)
                        .align(Alignment.TopCenter)
                        .zIndex(-1f)
                        .drawBehind {
                            val width = size.width
                            val height = size.height

                            if (gradientColors.size >= 3) {
                                val c0 = gradientColors[0]
                                val c1 = gradientColors[1]
                                val c2 = gradientColors[2]
                                val c3 = gradientColors.getOrElse(3) { c0 }
                                val c4 = gradientColors.getOrElse(4) { c1 }
                                // Primary color blob - top center
                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    c0.copy(
                                                        alpha = gradientAlpha * 0.75f
                                                    ),
                                                    c0.copy(
                                                        alpha = gradientAlpha * 0.4f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center = Offset(width * 0.5f, height * 0.15f),
                                            radius = width * 0.8f
                                        )
                                )

                                // Secondary color blob - left side
                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    c1.copy(
                                                        alpha = gradientAlpha * 0.55f
                                                    ),
                                                    c1.copy(
                                                        alpha = gradientAlpha * 0.3f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center = Offset(width * 0.1f, height * 0.4f),
                                            radius = width * 0.6f
                                        )
                                )

                                // Third color blob - right side
                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    c2.copy(
                                                        alpha = gradientAlpha * 0.5f
                                                    ),
                                                    c2.copy(
                                                        alpha = gradientAlpha * 0.25f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center = Offset(width * 0.9f, height * 0.35f),
                                            radius = width * 0.55f
                                        )
                                )

                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    c3.copy(
                                                        alpha = gradientAlpha * 0.35f
                                                    ),
                                                    c3.copy(
                                                        alpha = gradientAlpha * 0.18f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center = Offset(width * 0.25f, height * 0.65f),
                                            radius = width * 0.75f
                                        )
                                )

                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    c4.copy(
                                                        alpha = gradientAlpha * 0.3f
                                                    ),
                                                    c4.copy(
                                                        alpha = gradientAlpha * 0.15f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center = Offset(width * 0.55f, height * 0.85f),
                                            radius = width * 0.9f
                                        )
                                )
                            } else if (gradientColors.isNotEmpty()) {
                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    gradientColors[0].copy(
                                                        alpha = gradientAlpha * 0.7f
                                                    ),
                                                    gradientColors[0].copy(
                                                        alpha = gradientAlpha * 0.35f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center = Offset(width * 0.5f, height * 0.25f),
                                            radius = width * 0.85f
                                        )
                                )
                            }

                            drawRect(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                surfaceColor.copy(alpha = gradientAlpha * 0.22f),
                                                surfaceColor.copy(alpha = gradientAlpha * 0.55f),
                                                surfaceColor
                                            ),
                                        startY = height * 0.4f,
                                        endY = height
                                    )
                            )
                        }
            )
        }

        LazyColumn(
            state = lazyListState,
            contentPadding =
                LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
        ) {
            playlist.let { playlist ->
                if (isLoading) {
                    // Shimmer Loading State
                    item(key = "shimmer") {
                        ShimmerHost {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(top = systemBarsTopPadding + AppBarHeight),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Playlist art placeholder
                                Box(
                                    modifier =
                                        Modifier.padding(top = 8.dp, bottom = 20.dp)
                                            .size(240.dp)
                                            .shimmer()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                )

                                // Title placeholder
                                TextPlaceholder(
                                    height = 28.dp,
                                    modifier =
                                        Modifier.fillMaxWidth(0.6f).padding(horizontal = 32.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Author placeholder
                                TextPlaceholder(
                                    height = 20.dp,
                                    modifier = Modifier.fillMaxWidth(0.4f)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Metadata placeholder
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    repeat(2) {
                                        TextPlaceholder(
                                            height = 32.dp,
                                            modifier = Modifier.width(80.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Buttons placeholder
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(48.dp)
                                                .shimmer()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                    ButtonPlaceholder(modifier = Modifier.weight(1f).height(48.dp))
                                    ButtonPlaceholder(modifier = Modifier.weight(1f).height(48.dp))
                                    Box(
                                        modifier =
                                            Modifier.size(48.dp)
                                                .shimmer()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            repeat(6) { ListItemPlaceHolder() }
                        }
                    }
                } else if (playlist != null) {
                    if (!isSearching) {
                        // Hero Header
                        item(key = "header") {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(top = systemBarsTopPadding + AppBarHeight)
                                        .animateItem(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Playlist Thumbnail - Large centered with shadow
                                Box(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
                                    Surface(
                                        modifier =
                                            Modifier.size(240.dp)
                                                .shadow(
                                                    elevation = 24.dp,
                                                    shape = RoundedCornerShape(16.dp),
                                                    spotColor =
                                                        gradientColors
                                                            .getOrNull(0)
                                                            ?.copy(alpha = 0.5f)
                                                            ?: MaterialTheme.colorScheme.primary
                                                                .copy(alpha = 0.3f)
                                                ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        AsyncImage(
                                            model = playlist.thumbnail,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // Playlist Title
                                Text(
                                    text = playlist.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )

                                // Author (Clickable)
                                playlist.author?.let { artist ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text =
                                            buildAnnotatedString {
                                                withStyle(
                                                    style =
                                                        MaterialTheme.typography.titleMedium
                                                            .copy(
                                                                fontWeight = FontWeight.Normal,
                                                                color =
                                                                    MaterialTheme.colorScheme
                                                                        .primary
                                                            )
                                                            .toSpanStyle()
                                                ) {
                                                    if (artist.id != null) {
                                                        val link =
                                                            LinkAnnotation.Clickable(artist.id!!) {
                                                                navController.navigate(
                                                                    "artist/${artist.id}"
                                                                )
                                                            }
                                                        withLink(link) { append(artist.name) }
                                                    } else {
                                                        append(artist.name)
                                                    }
                                                }
                                            },
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Metadata Row - Song Count
                                playlist.songCountText?.let { songCountText ->
                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MetadataChip(
                                            icon = R.drawable.music_note,
                                            text = songCountText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Action Buttons Row
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Like/Save Button
                                    if (playlist.id != "LM") {
                                        Surface(
                                            onClick = {
                                                if (dbPlaylist?.playlist == null) {
                                                    database.transaction {
                                                        val playlistEntity =
                                                            PlaylistEntity(
                                                                    name = playlist.title,
                                                                    browseId = playlist.id,
                                                                    thumbnailUrl =
                                                                        playlist.thumbnail,
                                                                    isEditable =
                                                                        playlist.isEditable,
                                                                    playEndpointParams =
                                                                        playlist.playEndpoint
                                                                            ?.params,
                                                                    shuffleEndpointParams =
                                                                        playlist.shuffleEndpoint
                                                                            ?.params,
                                                                    radioEndpointParams =
                                                                        playlist.radioEndpoint
                                                                            ?.params
                                                                )
                                                                .toggleLike()
                                                        insert(playlistEntity)
                                                        songs
                                                            .map(SongItem::toMediaMetadata)
                                                            .onEach(::insert)
                                                            .mapIndexed { index, song ->
                                                                PlaylistSongMap(
                                                                    songId = song.id,
                                                                    playlistId = playlistEntity.id,
                                                                    position = index
                                                                )
                                                            }
                                                            .forEach(::insert)
                                                    }
                                                } else {
                                                    database.transaction {
                                                        val currentPlaylist = dbPlaylist!!.playlist
                                                        update(currentPlaylist, playlist)
                                                        update(currentPlaylist.toggleLike())
                                                    }
                                                }
                                            },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter =
                                                        painterResource(
                                                            if (
                                                                dbPlaylist
                                                                    ?.playlist
                                                                    ?.bookmarkedAt != null
                                                            )
                                                                R.drawable.favorite
                                                            else R.drawable.favorite_border
                                                        ),
                                                    contentDescription = null,
                                                    tint =
                                                        if (
                                                            dbPlaylist?.playlist?.bookmarkedAt !=
                                                                null
                                                        )
                                                            MaterialTheme.colorScheme.error
                                                        else
                                                            MaterialTheme.colorScheme
                                                                .onSurfaceVariant,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Shuffle Button
                                    playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                                        Button(
                                            onClick = {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(shuffleEndpoint)
                                                )
                                            },
                                            shape = RoundedCornerShape(24.dp),
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.shuffle),
                                                contentDescription =
                                                    stringResource(R.string.shuffle),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // Radio Button
                                    playlist.radioEndpoint?.let { radioEndpoint ->
                                        Button(
                                            onClick = {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(radioEndpoint)
                                                )
                                            },
                                            shape = RoundedCornerShape(24.dp),
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.radio),
                                                contentDescription = stringResource(R.string.radio),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // More Options Button
                                    Surface(
                                        onClick = {
                                            menuState.show {
                                                YouTubePlaylistMenu(
                                                    playlist = playlist,
                                                    songs = songs,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                    selectAction = { selection = true },
                                                    canSelect = true,
                                                    snackbarHostState = snackbarHostState,
                                                )
                                            }
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 20.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                                                playerConnection.playQueue(
                                                    YouTubeQueue(shuffleEndpoint)
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.mix),
                                            contentDescription = "Start Mix",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    if (songs.isEmpty() && !isLoading && error == null) {
                        item(key = "empty") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_playlist),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.empty_playlist_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Songs List
                    items(items = wrappedSongs, key = { it.item.second.id }) { song ->
                        YouTubeListItem(
                            item = song.item.second,
                            isActive = mediaMetadata?.id == song.item.second.id,
                            isPlaying = isPlaying,
                            isSelected = song.isSelected && selection,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song.item.second,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                    onLongClick = {}
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier =
                                Modifier.combinedClickable(
                                        enabled = !hideExplicit || !song.item.second.explicit,
                                        onClick = {
                                            if (!selection) {
                                                if (song.item.second.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.service.getAutomix(
                                                        playlistId = playlist.id
                                                    )
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            song.item.second.endpoint
                                                                ?: WatchEndpoint(
                                                                    videoId = song.item.second.id
                                                                ),
                                                            song.item.second.toMediaMetadata(),
                                                        ),
                                                    )
                                                }
                                            } else {
                                                song.isSelected = !song.isSelected
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                            if (!selection) {
                                                selection = true
                                            }
                                            wrappedSongs.forEach { it.isSelected = false }
                                            song.isSelected = true
                                        },
                                    )
                                    .animateItem(),
                        )
                    }

                    if (viewModel.continuation != null && songs.isNotEmpty() && isLoadingMore) {
                        item(key = "loading_more") {
                            ShimmerHost { repeat(2) { ListItemPlaceHolder() } }
                        }
                    }
                } else {
                    // Error State
                    item(key = "error") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text =
                                    if (error != null) {
                                        stringResource(R.string.error_unknown)
                                    } else {
                                        stringResource(R.string.playlist_not_found)
                                    },
                                style = MaterialTheme.typography.titleLarge,
                                color =
                                    if (error != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text =
                                    if (error != null) {
                                        error!!
                                    } else {
                                        stringResource(R.string.playlist_not_found_desc)
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (error != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.retry() }) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                }
            }
        }

        DraggableScrollbar(
            modifier =
                Modifier.padding(
                        LocalPlayerAwareWindowInsets.current
                            .union(WindowInsets.ime)
                            .asPaddingValues()
                    )
                    .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = 1
        )

        // Top App Bar
        val topAppBarColors =
            if (transparentAppBar) {
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            } else {
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            }

        TopAppBar(
            colors = topAppBarColors,
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else if (isSearching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                } else if (showTopBarTitle) {
                    Text(playlist?.title.orEmpty())
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            query = TextFieldValue()
                        } else if (selection) {
                            selection = false
                        } else {
                            navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!isSearching && !selection) {
                            navController.backToMain()
                        }
                    }
                ) {
                    Icon(
                        painter =
                            painterResource(
                                if (selection) R.drawable.close else R.drawable.arrow_back
                            ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(
                        onClick = {
                            if (count == wrappedSongs.size) {
                                wrappedSongs.forEach { it.isSelected = false }
                            } else {
                                wrappedSongs.forEach { it.isSelected = true }
                            }
                        },
                        onLongClick = {}
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    if (count == wrappedSongs.size) R.drawable.deselect
                                    else R.drawable.select_all
                                ),
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection =
                                        wrappedSongs
                                            .filter { it.isSelected }
                                            .map { it.item.second.toMediaItem().metadata!! },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false },
                                    currentItems = emptyList()
                                )
                            }
                        },
                        onLongClick = {}
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }, onLongClick = {}) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier.windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                    )
                    .align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun MetadataChip(icon: Int, text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
