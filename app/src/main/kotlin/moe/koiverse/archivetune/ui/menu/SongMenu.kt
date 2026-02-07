/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.menu

import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.LocalDownloadUtil
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.LocalSyncUtils
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.ArtistSeparatorsKey
import moe.koiverse.archivetune.constants.ListItemHeight
import moe.koiverse.archivetune.constants.ListThumbnailSize
import moe.koiverse.archivetune.db.entities.ArtistEntity
import moe.koiverse.archivetune.db.entities.Event
import moe.koiverse.archivetune.db.entities.PlaylistSong
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.db.entities.SongArtistMap
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.extensions.toMediaItem
import moe.koiverse.archivetune.models.toMediaMetadata
import moe.koiverse.archivetune.playback.ExoDownloadService
import moe.koiverse.archivetune.playback.queues.YouTubeQueue
import moe.koiverse.archivetune.ui.component.ListDialog
import moe.koiverse.archivetune.ui.component.LocalBottomSheetPageState
import moe.koiverse.archivetune.ui.component.MenuSurfaceSection
import moe.koiverse.archivetune.ui.component.NewAction
import moe.koiverse.archivetune.ui.component.NewActionGrid
import moe.koiverse.archivetune.ui.component.SongListItem
import moe.koiverse.archivetune.ui.component.TextFieldDialog
import moe.koiverse.archivetune.ui.utils.ShowMediaInfo
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.CachePlaylistViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SongMenu(
    originalSong: Song,
    event: Event? = null,
    navController: NavController,
    playlistSong: PlaylistSong? = null,
    playlistBrowseId: String? = null,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val songState = database.song(originalSong.id).collectAsState(initial = originalSong)
    val song = songState.value ?: originalSong
    val download by LocalDownloadUtil.current.getDownload(originalSong.id)
        .collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val cacheViewModel = hiltViewModel<CachePlaylistViewModel>()

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    // Artist separators for splitting artist names
    val (artistSeparators) = rememberPreference(ArtistSeparatorsKey, defaultValue = ",;/&")

    val orderedArtists by produceState(initialValue = emptyList<ArtistEntity>(), song) {
        withContext(Dispatchers.IO) {
            val artistMaps = database.songArtistMap(song.id).sortedBy { it.position }
            val sorted = artistMaps.mapNotNull { map ->
                song.artists.firstOrNull { it.id == map.artistId }
            }
            value = sorted
        }
    }

    // Split artists by configured separators
    data class SplitArtist(
        val name: String,
        val originalArtist: ArtistEntity?
    )

    val splitArtists = remember(orderedArtists, artistSeparators) {
        if (artistSeparators.isEmpty()) {
            orderedArtists.map { SplitArtist(it.name, it) }
        } else {
            val separatorRegex = "[${Regex.escape(artistSeparators)}]".toRegex()
            orderedArtists.flatMap { artist ->
                val parts = artist.name.split(separatorRegex).map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size > 1) {
                    // If the name contains separators, create split artists
                    // The first part keeps the original artist reference for navigation
                    parts.mapIndexed { index, name ->
                        SplitArtist(name, if (index == 0) artist else null)
                    }
                } else {
                    listOf(SplitArtist(artist.name, artist))
                }
            }
        }
    }

    var showEditDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val TextFieldValueSaver: Saver<TextFieldValue, *> = Saver(
        save = { it.text },
        restore = { text -> TextFieldValue(text, TextRange(text.length)) }
    )

    var titleField by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(TextFieldValue(song.song.title))
    }

    var artistField by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(TextFieldValue(song.artists.firstOrNull()?.name.orEmpty()))
    }

    if (showEditDialog) {
        TextFieldDialog(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null
                )
            },
            title = {
                Text(text = stringResource(R.string.edit_song))
            },
            textFields = listOf(
                stringResource(R.string.song_title) to titleField,
                stringResource(R.string.artist_name) to artistField
            ),
            onTextFieldsChange = { index, newValue ->
                if (index == 0) titleField = newValue
                else artistField = newValue
            },
            onDoneMultiple = { values ->
                val newTitle = values[0]
                val newArtist = values[1]

                coroutineScope.launch {
                    database.query {
                        update(song.song.copy(title = newTitle))
                        val artist = song.artists.firstOrNull()
                        if (artist != null) {
                            update(artist.copy(name = newArtist))
                        }
                    }

                    showEditDialog = false
                    onDismiss()
                }
            },
            onDismiss = { showEditDialog = false }
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { browseId ->
                    YouTube.addToPlaylist(browseId, song.id)
                }
            }
            listOf(song.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
        onAddComplete = { songCount, playlistNames ->
            val message = when {
                playlistNames.size == 1 -> context.getString(R.string.added_to_playlist, playlistNames.first())
                else -> context.getString(R.string.added_to_n_playlists, playlistNames.size)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.already_in_playlist)) },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(listOf(song)) { song ->
                SongListItem(song = song)
            }
        }
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(
                items = splitArtists.distinctBy { it.name },
                key = { it.name },
            ) { splitArtist ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = splitArtist.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        AsyncImage(
                            model = splitArtist.originalArtist?.thumbnailUrl,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                splitArtist.originalArtist?.let { artist ->
                                    navController.navigate("artist/${artist.id}")
                                    showSelectArtistDialog = false
                                    onDismiss()
                                }
                            },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SongListItem(
            song = song,
            badges = {},
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            trailingContent = {
                IconButton(
                    onClick = {
                        val s = song.song.toggleLike()
                        database.query {
                            update(s)
                        }
                        syncUtils.likeSong(s)
                    },
                ) {
                    Icon(
                        painter = painterResource(if (song.song.liked) R.drawable.favorite else R.drawable.favorite_border),
                        tint = if (song.song.liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        contentDescription = null,
                    )
                }
            },
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    val bottomSheetPageState = LocalBottomSheetPageState.current

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val startRadioText = stringResource(R.string.start_radio)
    val playNextText = stringResource(R.string.play_next)
    val addToQueueText = stringResource(R.string.add_to_queue)
    val addToPlaylistText = stringResource(R.string.add_to_playlist)
    val shareText = stringResource(R.string.share)
    val editText = stringResource(R.string.edit)

    val primaryActions = remember(
        song,
        startRadioText,
        playNextText,
        addToQueueText,
        addToPlaylistText,
        shareText,
        editText,
        onDismiss,
        playerConnection,
    ) {
        listOf(
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                text = startRadioText,
                onClick = {
                    onDismiss()
                    playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                },
            ),
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.playlist_play),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                text = playNextText,
                onClick = {
                    onDismiss()
                    playerConnection.playNext(song.toMediaItem())
                },
            ),
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                text = addToQueueText,
                onClick = {
                    onDismiss()
                    playerConnection.addToQueue(song.toMediaItem())
                },
            ),
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.playlist_add),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                text = addToPlaylistText,
                onClick = { showChoosePlaylistDialog = true },
            ),
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                text = shareText,
                onClick = {
                    onDismiss()
                    val intent =
                        Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}")
                        }
                    context.startActivity(Intent.createChooser(intent, null))
                },
            ),
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                text = editText,
                onClick = { showEditDialog = true },
            ),
        )
    }

    LazyColumn(
        userScrollEnabled = !isPortrait,
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                NewActionGrid(
                    actions = primaryActions,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                ListItem(
                    headlineContent = {
                        Text(
                            text =
                                stringResource(
                                    if (song.song.inLibrary == null) R.string.add_to_library
                                    else R.string.remove_from_library,
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter =
                                painterResource(
                                    if (song.song.inLibrary == null) R.drawable.library_add
                                    else R.drawable.library_add_check,
                                ),
                            contentDescription = null,
                        )
                    },
                    modifier =
                        Modifier.clickable {
                            database.query {
                                update(song.song.toggleLibrary())
                            }
                        },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                val dividerModifier = Modifier.padding(start = 56.dp)
                Column {
                    if (event != null) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.remove_from_history),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    contentDescription = null,
                                )
                            },
                            modifier =
                                Modifier.clickable {
                                    onDismiss()
                                    database.query {
                                        delete(event)
                                    }
                                },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }

                    if (event != null) {
                        HorizontalDivider(
                            modifier = dividerModifier,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }

                    if (playlistSong != null) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.remove_from_playlist),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    contentDescription = null,
                                )
                            },
                            modifier =
                                Modifier.clickable {
                                    val map = playlistSong.map
                                    coroutineScope.launch(Dispatchers.IO) {
                                        database.withTransaction {
                                            move(map.playlistId, map.position, Int.MAX_VALUE)
                                            delete(map.copy(position = Int.MAX_VALUE))
                                        }
                                        val browseId = playlistBrowseId
                                        val setVideoId = map.setVideoId
                                        if (browseId != null && setVideoId != null) {
                                            YouTube.removeFromPlaylist(browseId, map.songId, setVideoId)
                                        }
                                    }
                                    onDismiss()
                                },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )

                        HorizontalDivider(
                            modifier = dividerModifier,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }

                    if (isFromCache) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.remove_from_cache),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    contentDescription = null,
                                )
                            },
                            modifier =
                                Modifier.clickable {
                                    onDismiss()
                                    cacheViewModel.removeSongFromCache(song.id)
                                },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )

                        HorizontalDivider(
                            modifier = dividerModifier,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }

                    when (download?.state) {
                        Download.STATE_COMPLETED -> {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = stringResource(R.string.remove_download),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.offline),
                                        tint = MaterialTheme.colorScheme.error,
                                        contentDescription = null,
                                    )
                                },
                                modifier =
                                    Modifier.clickable {
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            song.id,
                                            false,
                                        )
                                    },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                        }
                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                            ListItem(
                                headlineContent = { Text(text = stringResource(R.string.downloading)) },
                                leadingContent = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                },
                                modifier =
                                    Modifier.clickable {
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            song.id,
                                            false,
                                        )
                                    },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                        }
                        else -> {
                            ListItem(
                                headlineContent = { Text(text = stringResource(R.string.action_download)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.download),
                                        contentDescription = null,
                                    )
                                },
                                modifier =
                                    Modifier.clickable {
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
                                    },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                Column {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.view_artist)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.artist),
                                contentDescription = null,
                            )
                        },
                        modifier =
                            Modifier.clickable {
                                if (splitArtists.size == 1 && splitArtists[0].originalArtist != null) {
                                    navController.navigate("artist/${splitArtists[0].originalArtist!!.id}")
                                    onDismiss()
                                } else {
                                    showSelectArtistDialog = true
                                }
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )

                    if (song.song.albumId != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )

                        ListItem(
                            headlineContent = { Text(text = stringResource(R.string.view_album)) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.album),
                                    contentDescription = null,
                                )
                            },
                            modifier =
                                Modifier.clickable {
                                    onDismiss()
                                    navController.navigate("album/${song.song.albumId}")
                                },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                Column {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.refetch)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                            )
                        },
                        modifier =
                            Modifier.clickable {
                                refetchIconDegree -= 360
                                coroutineScope.launch(Dispatchers.IO) {
                                    YouTube.queue(listOf(song.id)).onSuccess {
                                        val newSong = it.firstOrNull()
                                        if (newSong != null) {
                                            database.transaction {
                                                update(song, newSong.toMediaMetadata())
                                            }
                                        }
                                    }
                                }
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.details)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                            )
                        },
                        modifier =
                            Modifier.clickable {
                                onDismiss()
                                bottomSheetPageState.show {
                                    ShowMediaInfo(song.id)
                                }
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }
}
