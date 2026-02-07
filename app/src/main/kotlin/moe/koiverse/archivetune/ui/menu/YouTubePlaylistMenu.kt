/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.PlaylistItem
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.innertube.utils.completed
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.LocalDownloadUtil
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.LocalSyncUtils
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.ListThumbnailSize
import moe.koiverse.archivetune.constants.ThumbnailCornerRadius
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import moe.koiverse.archivetune.db.entities.PlaylistSongMap
import moe.koiverse.archivetune.extensions.toMediaItem
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.models.toMediaMetadata
import moe.koiverse.archivetune.playback.ExoDownloadService
import moe.koiverse.archivetune.playback.queues.YouTubeQueue
import moe.koiverse.archivetune.ui.component.DefaultDialog
import moe.koiverse.archivetune.ui.component.ListDialog
import moe.koiverse.archivetune.ui.component.NewAction
import moe.koiverse.archivetune.ui.component.NewActionGrid
import moe.koiverse.archivetune.ui.component.YouTubeListItem
import moe.koiverse.archivetune.utils.joinByBullet
import moe.koiverse.archivetune.utils.makeTimeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubePlaylistMenu(
    playlist: PlaylistItem,
    songs: List<SongItem> = emptyList(),
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    selectAction: () -> Unit = {},
    canSelect: Boolean = false,
    snackbarHostState: androidx.compose.material3.SnackbarHostState? = null,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val syncUtils = LocalSyncUtils.current
    val dbPlaylist by database.playlistByBrowseId(playlist.id).collectAsState(initial = null)

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showImportPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<MediaMetadata>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { targetPlaylist ->
            val allSongs = songs
                .ifEmpty {
                    YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                }.map {
                    it.toMediaMetadata()
                }
            database.transaction {
                allSongs.forEach(::insert)
            }
            allSongs.map { it.id }
        },
        onDismiss = { showChoosePlaylistDialog = false },
        onAddComplete = { songCount, playlistNames ->
            val message = when {
                songCount == 1 && playlistNames.size == 1 -> context.getString(R.string.added_to_playlist, playlistNames.first())
                songCount > 1 && playlistNames.size == 1 -> context.getString(R.string.added_n_songs_to_playlist, songCount, playlistNames.first())
                songCount == 1 -> context.getString(R.string.added_to_n_playlists, playlistNames.size)
                else -> context.getString(R.string.added_n_songs_to_n_playlists, songCount, playlistNames.size)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )

    YouTubeListItem(
        item = playlist,
        trailingContent = {
            if (playlist.id != "LM" && !playlist.isEditable) {
                IconButton(
                    onClick = {
                        if (dbPlaylist?.playlist == null) {
                            database.transaction {
                                val playlistEntity = PlaylistEntity(
                                    name = playlist.title,
                                    browseId = playlist.id,
                                    thumbnailUrl = playlist.thumbnail,
                                    isEditable = false,
                                    remoteSongCount = playlist.songCountText?.let {
                                        Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                    },
                                    playEndpointParams = playlist.playEndpoint?.params,
                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = playlist.radioEndpoint?.params
                                ).toggleLike()
                                insert(playlistEntity)
                                coroutineScope.launch(Dispatchers.IO) {
                                    songs.ifEmpty {
                                        YouTube.playlist(playlist.id).completed()
                                            .getOrNull()?.songs.orEmpty()
                                    }.map { it.toMediaMetadata() }
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
                        painter = painterResource(if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                        tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        contentDescription = null
                    )
                }
            }
        }
    )
    HorizontalDivider()

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }
    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED })
                    Download.STATE_COMPLETED
                else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED
                                || downloads[it.id]?.state == Download.STATE_DOWNLOADING
                                || downloads[it.id]?.state == Download.STATE_COMPLETED
                    })
                    Download.STATE_DOWNLOADING
                else
                    Download.STATE_STOPPED
        }
    }
    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist.title
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    ImportPlaylistDialog(
        isVisible = showImportPlaylistDialog,
        onGetSong = {
            val allSongs = songs
                .ifEmpty {
                    YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                }.map {
                    it.toMediaMetadata()
                }
            database.transaction {
                allSongs.forEach(::insert)
            }
            allSongs.map { it.id }
        },
        playlistTitle = playlist.title,
        browseId = playlist.id,
        snackbarHostState = snackbarHostState,
        onDismiss = { showImportPlaylistDialog = false }
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

            items(notAddedList) { song ->
                ListItem(
                    headlineContent = { Text(text = song.title) },
                    leadingContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    supportingContent = {
                        Text(
                            text = joinByBullet(
                                song.artists.joinToString { it.name },
                                makeTimeString(song.duration * 1000L),
                            )
                        )
                    },
                )
            }
        }
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
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            // Enhanced Action Grid using NewMenuComponents
            NewActionGrid(
                actions = buildList {
                    playlist.playEndpoint?.let { playEndpoint ->
                        add(
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                text = stringResource(R.string.play),
                                onClick = {
                                    playerConnection.playQueue(YouTubeQueue(playEndpoint))
                                    onDismiss()
                                }
                            )
                        )
                    }
                    playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                        add(
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                text = stringResource(R.string.shuffle),
                                onClick = {
                                    playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                                    onDismiss()
                                }
                            )
                        )
                    }
                    playlist.radioEndpoint?.let { radioEndpoint ->
                        add(
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.radio),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                text = stringResource(R.string.start_radio),
                                onClick = {
                                    playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                    onDismiss()
                                }
                            )
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }

        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.play_next)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.playlist_play),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        songs
                            .ifEmpty {
                                withContext(Dispatchers.IO) {
                                    YouTube
                                        .playlist(playlist.id)
                                        .completed()
                                        .getOrNull()
                                        ?.songs
                                        .orEmpty()
                                }
                            }.let { songs ->
                                playerConnection.playNext(songs.map { it.toMediaItem() })
                            }
                    }
                    onDismiss()
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.add_to_queue)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        songs
                            .ifEmpty {
                                withContext(Dispatchers.IO) {
                                    YouTube
                                        .playlist(playlist.id)
                                        .completed()
                                        .getOrNull()
                                        ?.songs
                                        .orEmpty()
                                }
                            }.let { songs ->
                                playerConnection.addToQueue(songs.map { it.toMediaItem() })
                            }
                    }
                    onDismiss()
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.add_to_playlist)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.playlist_add),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    showChoosePlaylistDialog = true
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.import_playlist)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    showImportPlaylistDialog = true
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.yt_sync)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.sync),
                        contentDescription = null,
                    )
                },
                trailingContent = {
                    val checked = dbPlaylist?.playlist?.isAutoSync ?: false
                    Switch(
                        checked = checked,
                        onCheckedChange = { newValue ->
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val currentDbPlaylist = dbPlaylist
                                    if (currentDbPlaylist?.playlist == null) {
                                        val playlistPage = YouTube.playlist(playlist.id).completed().getOrNull()
                                        val fetchedSongs = playlistPage?.songs.orEmpty().map { it.toMediaMetadata() }

                                        if (fetchedSongs.isEmpty() && newValue) {
                                            withContext(Dispatchers.Main) {
                                                if (snackbarHostState != null) {
                                                    snackbarHostState.showSnackbar(context.getString(R.string.import_failed))
                                                } else {
                                                    android.widget.Toast.makeText(context, context.getString(R.string.import_failed), android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            return@launch
                                        }

                                        database.transaction {
                                            val playlistEntity = PlaylistEntity(
                                                name = playlist.title,
                                                browseId = playlist.id,
                                                thumbnailUrl = playlist.thumbnail,
                                                isEditable = false,
                                                isAutoSync = newValue,
                                                remoteSongCount = playlist.songCountText?.let {
                                                    Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                                },
                                                playEndpointParams = playlist.playEndpoint?.params,
                                                shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                                radioEndpointParams = playlist.radioEndpoint?.params
                                            )
                                            insert(playlistEntity)
                                            fetchedSongs.forEach(::insert)
                                            fetchedSongs.mapIndexed { index, song ->
                                                PlaylistSongMap(
                                                    songId = song.id,
                                                    playlistId = playlistEntity.id,
                                                    position = index
                                                )
                                            }.forEach(::insert)
                                        }

                                        if (newValue) {
                                            withContext(Dispatchers.Main) {
                                                if (snackbarHostState != null) {
                                                    snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced))
                                                } else {
                                                    android.widget.Toast.makeText(context, context.getString(R.string.playlist_synced), android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } else {
                                        val existing = currentDbPlaylist.playlist
                                        database.query {
                                            update(existing.copy(isAutoSync = newValue))
                                        }
                                        
                                        if (newValue) {
                                            syncUtils.syncAutoSyncPlaylists()
                                            withContext(Dispatchers.Main) {
                                                if (snackbarHostState != null) {
                                                    snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced))
                                                } else {
                                                    android.widget.Toast.makeText(context, context.getString(R.string.playlist_synced), android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        val errorMsg = context.getString(R.string.import_failed) + ": ${e.message ?: "Unknown error"}"
                                        if (snackbarHostState != null) {
                                            snackbarHostState.showSnackbar(errorMsg)
                                        } else {
                                            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            )
        }
        item {
            when (downloadState) {
                Download.STATE_COMPLETED -> {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.remove_download),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.offline),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
                            showRemoveDownloadDialog = true
                        }
                    )
                }
                Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.downloading)) },
                        leadingContent = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        },
                        modifier = Modifier.clickable {
                            showRemoveDownloadDialog = true
                        }
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
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                songs
                                    .ifEmpty {
                                        withContext(Dispatchers.IO) {
                                            YouTube
                                                .playlist(playlist.id)
                                                .completed()
                                                .getOrNull()
                                                ?.songs
                                                .orEmpty()
                                        }
                                    }.forEach { song ->
                                        val downloadRequest = DownloadRequest.Builder(song.id, song.id.toUri())
                                            .setCustomCacheKey(song.id)
                                            .setData(song.title.toByteArray())
                                            .build()
                                        DownloadService.sendAddDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            downloadRequest,
                                            false
                                        )
                                    }
                            }
                        }
                    )
                }
            }
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.share)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, playlist.shareLink)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                    onDismiss()
                }
            )
        }
        if (canSelect) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.select)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.select_all),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        selectAction()
                    }
                )
            }
        }
    }
}
