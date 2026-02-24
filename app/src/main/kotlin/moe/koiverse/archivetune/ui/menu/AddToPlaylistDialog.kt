/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import moe.koiverse.archivetune.innertube.utils.parseCookieString
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.InnerTubeCookieKey
import moe.koiverse.archivetune.constants.ListThumbnailSize
import moe.koiverse.archivetune.db.entities.Playlist
import moe.koiverse.archivetune.ui.component.CreatePlaylistDialog
import moe.koiverse.archivetune.ui.component.DefaultDialog
import moe.koiverse.archivetune.ui.component.ListItem
import moe.koiverse.archivetune.ui.component.PlaylistListItem
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.withContext

internal fun playlistsForAddToPlaylist(playlists: List<Playlist>): List<Playlist> =
    playlists.filter { it.playlist.isEditable || it.playlist.browseId != null }

@Composable
fun AddToPlaylistDialog(
    isVisible: Boolean,
    allowSyncing: Boolean = true,
    initialTextFieldValue: String? = null,
    onGetSong: suspend (Playlist) -> List<String>,
    onDismiss: () -> Unit,
    onAddComplete: ((songCount: Int, playlistNames: List<String>) -> Unit)? = null,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var allPlaylists by remember { mutableStateOf(emptyList<Playlist>()) }
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val playlists = remember(allPlaylists) { playlistsForAddToPlaylist(allPlaylists).asReversed() }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var playlistsWithDuplicates by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var duplicateSongsMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var songIds by remember { mutableStateOf<List<String>?>(null) }
    val (selectedPlaylistIds, setSelectedPlaylistIds) = remember { mutableStateOf(emptySet<String>()) }
    var isAddingToPlaylist by remember { mutableStateOf(false) }

    suspend fun addSongsToPlaylistSafely(
        playlist: Playlist,
        requestedSongIds: List<String>,
    ): Int {
        if (requestedSongIds.isEmpty()) return 0

        val browseId = playlist.playlist.browseId
        if (isLoggedIn && browseId != null) {
            val acceptedSongIds = mutableListOf<String>()
            requestedSongIds.forEach { songId ->
                var wasAdded = false
                for (attempt in 0 until 3) {
                    if (YouTube.addToPlaylist(browseId, songId).isSuccess) {
                        wasAdded = true
                        break
                    }
                    if (attempt < 2) delay(250)
                }
                if (wasAdded) {
                    acceptedSongIds += songId
                }
            }
            if (acceptedSongIds.isNotEmpty()) {
                database.addSongToPlaylist(playlist, acceptedSongIds)
            }
            return acceptedSongIds.size
        }

        database.addSongToPlaylist(playlist, requestedSongIds)
        return requestedSongIds.size
    }


    LaunchedEffect(Unit) {
        database.playlistsByCreateDateAsc().collect {
            allPlaylists = it
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            songIds = null
            setSelectedPlaylistIds(emptySet())
            isAddingToPlaylist = false
            showDuplicateDialog = false
            playlistsWithDuplicates = emptyList()
            duplicateSongsMap = emptyMap()
        }
    }

    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add to playlist") },
            text = {
                LazyColumn {
                    item {
                        ListItem(
                            title = stringResource(R.string.create_playlist),
                            thumbnailContent = {
                                Image(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                                    modifier = Modifier.size(ListThumbnailSize)
                                )
                            },
                            modifier = Modifier.clickable { showCreatePlaylistDialog = true }
                        )
                    }
                    items(playlists) { playlist ->
                        val isSelected = selectedPlaylistIds.contains(playlist.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val currentIds = selectedPlaylistIds.toMutableSet()
                                    if (isSelected) currentIds.remove(playlist.id)
                                    else currentIds.add(playlist.id)
                                    setSelectedPlaylistIds(currentIds)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlaylistListItem(
                                playlist = playlist,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { isChecked ->
                                    val currentIds = selectedPlaylistIds.toMutableSet()
                                    if (isChecked) currentIds.add(playlist.id)
                                    else currentIds.remove(playlist.id)
                                    setSelectedPlaylistIds(currentIds)
                                }
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedPlaylistIds.isNotEmpty() && !isAddingToPlaylist,
                    onClick = {
                        isAddingToPlaylist = true
                        coroutineScope.launch {
                            val currentSongIds = withContext(Dispatchers.IO) {
                                songIds ?: if (playlists.isNotEmpty()) onGetSong(playlists.first()) else null
                            }

                            if (currentSongIds.isNullOrEmpty()) {
                                isAddingToPlaylist = false
                                onDismiss()
                                return@launch
                            }
                            songIds = currentSongIds

                            val (withDuplicates, duplicatesMap, successfullyAddedPlaylistIds) = withContext(Dispatchers.IO) {
                                val selectedPlaylists = playlists.filter { selectedPlaylistIds.contains(it.id) }
                                val tempDuplicatesMap = mutableMapOf<String, List<String>>()
                                val addedPlaylistIds = mutableSetOf<String>()

                                val (playlistsWithDups, playlistsWithoutDups) = selectedPlaylists.partition { playlist ->
                                    val dups = database.playlistDuplicates(playlist.id, currentSongIds)
                                    if (dups.isNotEmpty()) {
                                        tempDuplicatesMap[playlist.id] = dups
                                        true
                                    } else {
                                        false
                                    }
                                }

                                playlistsWithoutDups.forEach { playlist ->
                                    val addedCount = addSongsToPlaylistSafely(playlist, currentSongIds)
                                    if (addedCount > 0) {
                                        addedPlaylistIds += playlist.id
                                    }
                                }
                                Triple(playlistsWithDups, tempDuplicatesMap, addedPlaylistIds)
                            }

                            isAddingToPlaylist = false

                            val selectedPlaylists = playlists.filter { selectedPlaylistIds.contains(it.id) }
                            val addedPlaylistNames = selectedPlaylists
                                .filter { successfullyAddedPlaylistIds.contains(it.id) }
                                .map { it.playlist.name }
                            if (addedPlaylistNames.isNotEmpty()) {
                                onAddComplete?.invoke(currentSongIds.size, addedPlaylistNames)
                            }
                            
                            if (withDuplicates.isNotEmpty()) {
                                playlistsWithDuplicates = withDuplicates
                                duplicateSongsMap = duplicatesMap
                                showDuplicateDialog = true
                            }
                            onDismiss()
                        }
                    }
                ) {
                    if (isAddingToPlaylist) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Done")
                    }
                }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    //  Duplicate songs
    if (showDuplicateDialog) {
        val totalDuplicates = duplicateSongsMap.values.flatten().distinct().size
        DefaultDialog(
            title = { Text(stringResource(R.string.duplicates)) },
            buttons = {
                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            var totalAdded = 0
                            val names = mutableListOf<String>()
                            playlistsWithDuplicates.forEach { playlist ->
                                val duplicatesForThisPlaylist = duplicateSongsMap[playlist.id] ?: emptyList()
                                val songsToAdd = songIds!!.filter { it !in duplicatesForThisPlaylist }
                                val addedCount = addSongsToPlaylistSafely(playlist, songsToAdd)
                                if (addedCount > 0) {
                                    totalAdded += addedCount
                                    names += playlist.playlist.name
                                }
                            }
                            if (totalAdded > 0) {
                                withContext(Dispatchers.Main) {
                                    onAddComplete?.invoke(totalAdded, names)
                                }
                            }
                        }
                        showDuplicateDialog = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.skip_duplicates))
                }

                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            var totalAdded = 0
                            val names = mutableListOf<String>()
                            playlistsWithDuplicates.forEach { playlist ->
                                val addedCount = addSongsToPlaylistSafely(playlist, songIds!!)
                                if (addedCount > 0) {
                                    totalAdded += addedCount
                                    names += playlist.playlist.name
                                }
                            }
                            if (totalAdded > 0) {
                                withContext(Dispatchers.Main) {
                                    onAddComplete?.invoke(totalAdded, names)
                                }
                            }
                        }
                        showDuplicateDialog = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.add_anyway))
                }

                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            onDismiss = { showDuplicateDialog = false }
        ) {
            Text(
                text = if (totalDuplicates == 1) {
                    stringResource(R.string.duplicates_description_single)
                } else {
                    stringResource(R.string.duplicates_description_multiple, totalDuplicates)
                },
                textAlign = TextAlign.Start,
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}
