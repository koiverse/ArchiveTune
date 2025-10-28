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
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.AlbumThumbnailSize
import moe.koiverse.archivetune.constants.HideExplicitKey
import moe.koiverse.archivetune.constants.ThumbnailCornerRadius
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import moe.koiverse.archivetune.db.entities.PlaylistSongMap
import moe.koiverse.archivetune.extensions.metadata
import moe.koiverse.archivetune.extensions.toMediaItem
import moe.koiverse.archivetune.extensions.togglePlayPause
import moe.koiverse.archivetune.models.toMediaMetadata
import moe.koiverse.archivetune.playback.queues.YouTubeQueue
import moe.koiverse.archivetune.ui.component.AutoResizeText
import moe.koiverse.archivetune.ui.component.DraggableScrollbar
import moe.koiverse.archivetune.ui.component.FontSizeRange
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
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.ui.utils.ItemWrapper
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.OnlinePlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
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

    var selection by remember {
        mutableStateOf(false)
    }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val importLabel = stringResource(R.string.import_playlist)

    var isSearching by rememberSaveable { mutableStateOf(false) }

    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val filteredSongs =
        remember(songs, query) {
            if (query.text.isEmpty()) {
                songs.mapIndexed { index, song -> index to song }
            } else {
                songs
                    .mapIndexed { index, song -> index to song }
                    .filter { (_, song) ->
                        song.title.contains(query.text, ignoreCase = true) ||
                                song.artists.fastAny {
                                    it.name.contains(
                                        query.text,
                                        ignoreCase = true
                                    )
                                }
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
        BackHandler {
            selection = false
        }
    }

    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { item -> ItemWrapper(item) }
    }.toMutableStateList()

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (songs.size >= 5 && lastVisibleIndex != null && lastVisibleIndex >= songs.size - 5) {
                    viewModel.loadMoreSongs()
                }
            }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                .asPaddingValues(),
        ) {
            playlist.let { playlist ->
                if (isLoading) {
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
                } else if (playlist != null) {
                    if (!isSearching) {
                        item {
                            Column(
                                modifier =
                                Modifier
                                    .padding(12.dp)
                                    .animateItem(),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(AlbumThumbnailSize)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                        .fillMaxWidth(),
                                ) {
                                    AsyncImage(
                                        model = playlist.thumbnail,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                                        )
                                    }

                                    Spacer(Modifier.width(16.dp))

                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        AutoResizeText(
                                            text = playlist.title,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSizeRange = FontSizeRange(16.sp, 22.sp),
                                        )

                                        playlist.author?.let { artist ->
                                            Text(
                                                buildAnnotatedString {
                                                    withStyle(
                                                        style =
                                                        MaterialTheme.typography.titleMedium
                                                            .copy(
                                                                fontWeight = FontWeight.Normal,
                                                                color = MaterialTheme.colorScheme.onBackground,
                                                            ).toSpanStyle(),
                                                    ) {
                                                        if (artist.id != null) {
                                                            val link =
                                                                LinkAnnotation.Clickable(artist.id!!) {
                                                                    navController.navigate("artist/${artist.id!!}")
                                                                }
                                                            withLink(link) {
                                                                append(artist.name)
                                                            }
                                                        } else {
                                                            append(artist.name)
                                                        }
                                                    }
                                                },
                                            )
                                        }

                                        playlist.songCountText?.let { songCountText ->
                                            Text(
                                                text = songCountText,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Normal,
                                            )
                                        }

                                        Row {
                                            if (playlist.id != "LM") {
                                                IconButton(
                                                    onClick = {
                                                        if (dbPlaylist?.playlist == null) {
                                                            database.transaction {
                                                                val playlistEntity = PlaylistEntity(
                                                                    name = playlist.title,
                                                                    browseId = playlist.id,
                                                                    thumbnailUrl = playlist.thumbnail,
                                                                    isEditable = playlist.isEditable,
                                                                    playEndpointParams = playlist.playEndpoint?.params,
                                                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                                                    radioEndpointParams = playlist.radioEndpoint?.params
                                                                ).toggleLike()
                                                                insert(playlistEntity)
                                                                songs.map(SongItem::toMediaMetadata)
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
                                                                // Update playlist information including thumbnail before toggling like
                                                                val currentPlaylist = dbPlaylist!!.playlist
                                                                update(currentPlaylist, playlist)
                                                                update(currentPlaylist.toggleLike())
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(
                                                            if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border
                                                        ),
                                                        contentDescription = null,
                                                        tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        YouTubePlaylistMenu(
                                                            playlist = playlist,
                                                            songs = songs,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                            selectAction = { selection = true },
                                                            canSelect = true,
                                                        )
                                                    }
                                                },
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
                                    playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                                        Button(
                                            onClick = {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        shuffleEndpoint
                                                    )
                                                )
                                            },
                                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.shuffle),
                                                contentDescription = null,
                                                modifier = Modifier.size(ButtonDefaults.IconSize)
                                            )
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Text(stringResource(R.string.shuffle))
                                        }
                                    }

                                    playlist.radioEndpoint?.let { radioEndpoint ->
                                        OutlinedButton(
                                            onClick = {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        radioEndpoint
                                                    )
                                                )
                                            },
                                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.radio),
                                                contentDescription = null,
                                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                            )
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Text(stringResource(R.string.radio))
                                        }
                                    }
                                    if (dbPlaylist?.playlist == null || dbPlaylist?.playlist?.isLocal == false) {
                                        OutlinedButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    database.transaction {
                                                        val imported = PlaylistEntity(
                                                            name = playlist.title,
                                                            thumbnailUrl = playlist.thumbnail,
                                                            isEditable = playlist.isEditable,
                                                            isLocal = true
                                                        )
                                                        insert(imported)
                                                        songs.map(SongItem::toMediaMetadata)
                                                            .onEach(::insert)
                                                            .mapIndexed { index, song ->
                                                                PlaylistSongMap(
                                                                    songId = song.id,
                                                                    playlistId = imported.id,
                                                                    position = index
                                                                )
                                                            }
                                                            .forEach(::insert)
                                                    }
                                                    snackbarHostState.showSnackbar("${playlist.title} $importLabel")
                                                }
                                            },
                                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.download),
                                                contentDescription = null,
                                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                            )
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Text(stringResource(R.string.import_playlist))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (songs.isEmpty() && !isLoading && error == null) {
                        // Show empty playlist message when playlist is loaded but has no songs
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
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

                    items(
                        items = wrappedSongs,
                    ) { song ->
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
                                    enabled = !hideExplicit || !song.item.second.explicit,
                                    onClick = {
                                        if (!selection) {
                                            if (song.item.second.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.service.getAutomix(playlistId = playlist.id)
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        song.item.second.endpoint
                                                            ?: WatchEndpoint(
                                                                videoId =
                                                                song.item.second
                                                                    .id,
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
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        item {
                            ShimmerHost {
                                repeat(2) {
                                    ListItemPlaceHolder()
                                }
                            }
                        }
                    }

                } else {
                    // Show error state when playlist is null and there's an error
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (error != null) {
                                    stringResource(R.string.error_unknown)
                                } else {
                                    stringResource(R.string.playlist_not_found)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (error != null) {
                                    error!!
                                } else {
                                    stringResource(R.string.playlist_not_found_desc)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (error != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { 
                                        viewModel.retry()
                                    }
                                ) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                }
            }
        }

        DraggableScrollbar(
            modifier = Modifier
                .padding(
                    LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                        .asPaddingValues()
                )
                .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = 1
        )

        TopAppBar(
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
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
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
                        painter = painterResource(
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
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = wrappedSongs.filter { it.isSelected }
                                        .map { it.item.second.toMediaItem().metadata!! },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false },
                                    currentItems = emptyList()
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else {
                    if (!isSearching) {
                        IconButton(
                            onClick = { isSearching = true }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
            Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                .align(Alignment.BottomCenter),
        )
    }
}
