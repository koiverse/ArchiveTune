/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.ListThumbnailSize
import moe.koiverse.archivetune.db.entities.Playlist
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.models.toMediaMetadata
import moe.koiverse.archivetune.ui.component.CreatePlaylistDialog
import moe.koiverse.archivetune.ui.component.DefaultDialog
import moe.koiverse.archivetune.ui.component.ListDialog
import moe.koiverse.archivetune.ui.component.ListItem
import moe.koiverse.archivetune.ui.component.PlaylistListItem
import moe.koiverse.archivetune.utils.reportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AddToPlaylistDialogOnline(
    isVisible: Boolean,
    allowSyncing: Boolean = true,
    initialTextFieldValue: String? = null,
    songs: SnapshotStateList<Song>, // list of song ids. Songs should be inserted to database in this function.
    onDismiss: () -> Unit,
    onProgressStart: (Boolean) -> Unit,
    onPercentageChange: (Int) -> Unit
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var playlists by remember {
        mutableStateOf(emptyList<Playlist>())
    }


    var showCreatePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showDuplicateDialog by remember {
        mutableStateOf(false)
    }
    var selectedPlaylist by remember {
        mutableStateOf<Playlist?>(null)
    }
    val songIds by remember {
        mutableStateOf<List<String>?>(null) // list is not saveable
    }
    val duplicates by remember {
        mutableStateOf(emptyList<String>())
    }


    LaunchedEffect(Unit) {
        database.editablePlaylistsByCreateDateAsc().collect {
            playlists = it.asReversed()
        }
    }

    if (isVisible) {
        ListDialog(
            onDismiss = onDismiss
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.create_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(id = R.drawable.playlist_add),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize)
                        )
                    },
                    modifier = Modifier.clickable {
                        showCreatePlaylistDialog = true
                    }
                )
            }

            items(playlists) { playlist ->
                PlaylistListItem(
                    playlist = playlist,
                    modifier = Modifier.clickable {
                        selectedPlaylist = playlist
                        coroutineScope.launch(Dispatchers.IO) {
                            onDismiss()
                            val total = songs.size
                            if (total == 0) {
                                withContext(Dispatchers.Main) {
                                    onProgressStart(false)
                                    onPercentageChange(0)
                                }
                                return@launch
                            }

                            withContext(Dispatchers.Main) {
                                onProgressStart(true)
                                onPercentageChange(0)
                            }

                            var processed = 0

                            songs.asReversed().forEach { song ->
                                val allArtists =
                                    song.artists.joinToString(" ") { artist ->
                                        URLDecoder.decode(artist.name, StandardCharsets.UTF_8.toString())
                                    }.trim()
                                val query = if (allArtists.isEmpty()) {
                                    song.title
                                } else {
                                    "${song.title} - $allArtists"
                                }

                                try {
                                    val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                                    result
                                        .onSuccess { search ->
                                            val firstSong = search.items.distinctBy { it.id }.firstOrNull() as? SongItem
                                            if (firstSong != null) {
                                                val media = firstSong.toMediaMetadata()
                                                val ids = listOf(firstSong.id)
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        try {
                                                            database.insert(media)
                                                        } catch (e: Exception) {
                                                            Timber.tag("Exception inserting song in database:")
                                                                .e(e.toString())
                                                        }
                                                        database.addSongToPlaylist(playlist, ids)
                                                    }
                                                } catch (e: Exception) {
                                                    Timber.tag("ERROR").v(e.toString())
                                                }
                                            }
                                        }
                                        .onFailure {
                                            reportException(it)
                                        }
                                } catch (e: Exception) {
                                    Timber.tag("ERROR").v(e.toString())
                                }

                                processed += 1
                                val percent = ((processed.toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 100)
                                withContext(Dispatchers.Main) {
                                    onPercentageChange(percent)
                                    if (processed == total) {
                                        onProgressStart(false)
                                    }
                                }
                            }

                        }
                    }
                )
            }

            item {
                ListItem(
                    modifier = Modifier.clickable {
                        coroutineScope.launch(Dispatchers.IO) {
                            onDismiss()
                            val total = songs.size
                            if (total == 0) {
                                withContext(Dispatchers.Main) {
                                    onProgressStart(false)
                                    onPercentageChange(0)
                                }
                                return@launch
                            }

                            withContext(Dispatchers.Main) {
                                onProgressStart(true)
                                onPercentageChange(0)
                            }

                            var processed = 0

                            songs.asReversed().forEach { song ->
                                val allArtists =
                                    song.artists.joinToString(" ") { artist ->
                                        URLDecoder.decode(artist.name, StandardCharsets.UTF_8.toString())
                                    }.trim()
                                val query = if (allArtists.isEmpty()) {
                                    song.title
                                } else {
                                    "${song.title} - $allArtists"
                                }

                                try {
                                    val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                                    result
                                        .onSuccess { search ->
                                            val firstSong = search.items.distinctBy { it.id }.firstOrNull() as? SongItem
                                            if (firstSong != null) {
                                                val media = firstSong.toMediaMetadata()
                                                val entity = media.toSongEntity()
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        try {
                                                            database.insert(media)
                                                            database.query {
                                                                update(entity.toggleLike())
                                                            }
                                                        } catch (e: Exception) {
                                                            Timber.tag("Exception inserting song in database:")
                                                                .e(e.toString())
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Timber.tag("ERROR").v(e.toString())
                                                }
                                            }
                                        }
                                        .onFailure {
                                            reportException(it)
                                        }
                                } catch (e: Exception) {
                                    Timber.tag("ERROR").v(e.toString())
                                }

                                processed += 1
                                val percent = ((processed.toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 100)
                                withContext(Dispatchers.Main) {
                                    onPercentageChange(percent)
                                    if (processed == total) {
                                        onProgressStart(false)
                                    }
                                }
                            }

                        }
                    },
                    title = stringResource(R.string.liked_songs),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(id = R.drawable.favorite), // The XML image
                            contentDescription = null,
                            modifier = Modifier.size(40.dp), // Adjust size as needed
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground) // Optional tinting
                        )
                    },
                    trailingContent = {}
                )
            }

            item {
                Text(
                    text = stringResource(R.string.playlist_add_local_to_synced_note),
                    fontSize = TextUnit(12F, TextUnitType.Sp),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    // duplicate songs warning
    if (showDuplicateDialog) {
        DefaultDialog(
            title = { Text(stringResource(R.string.duplicates)) },
            buttons = {
                TextButton(
                    onClick = {
                        showDuplicateDialog = false
                        onDismiss()
                        database.transaction {
                            addSongToPlaylist(
                                selectedPlaylist!!,
                                songIds!!.filter {
                                    !duplicates.contains(it)
                                }
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.skip_duplicates))
                }

                TextButton(
                    onClick = {
                        showDuplicateDialog = false
                        onDismiss()
                        database.transaction {
                            addSongToPlaylist(selectedPlaylist!!, songIds!!)
                        }
                    }
                ) {
                    Text(stringResource(R.string.add_anyway))
                }

                TextButton(
                    onClick = {
                        showDuplicateDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showDuplicateDialog = false
            }
        ) {
            Text(
                text = if (duplicates.size == 1) {
                    stringResource(R.string.duplicates_description_single)
                } else {
                    stringResource(R.string.duplicates_description_multiple, duplicates.size)
                },
                textAlign = TextAlign.Start,
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}
