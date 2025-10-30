package moe.koiverse.archivetune.ui.menu

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import moe.koiverse.archivetune.ui.component.DefaultDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import moe.koiverse.archivetune.ui.component.TextFieldDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImportPlaylistDialog(
    isVisible: Boolean,
    onGetSong: suspend () -> List<String>, // list of song ids. Songs should be inserted to database in this function.
    playlistTitle: String,
    browseId: String? = null,
    snackbarHostState: SnackbarHostState? = null,
    onDismiss: () -> Unit,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val textFieldValue by remember { mutableStateOf(TextFieldValue(text = playlistTitle)) }
    var songIds by remember {
        mutableStateOf<List<String>?>(null) // list is not saveable
    }
    var isImporting by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var existingPlaylistId by remember { mutableStateOf<String?>(null) }

    if (isVisible) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
            title = { Text(text = stringResource(R.string.import_playlist)) },
            initialTextFieldValue = textFieldValue,
            autoFocus = false,
            onDismiss = onDismiss,
            extraContent = {
                if (isImporting) {
                    CircularProgressIndicator()
                }
            },
            onDone = { finalName ->
                // Start import
                isImporting = true

                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val ids = onGetSong()
                        songIds = ids

                        if (browseId != null) {
                            // check for existing playlist with same browseId
                            val existing = database.playlistByBrowseId(browseId).firstOrNull()
                            if (existing != null) {
                                // show duplicate dialog on main thread
                                existingPlaylistId = existing.playlist.id
                                isImporting = false
                                showDuplicateDialog = true
                                return@launch
                            }
                        }

                        // create new playlist and insert
                        val newPlaylist = PlaylistEntity(
                            name = finalName,
                            browseId = browseId,
                        )
                        database.query { insert(newPlaylist) }

                        val playlist = database.playlist(newPlaylist.id).firstOrNull()
                        if (playlist != null) {
                            database.addSongToPlaylist(playlist, songIds!!)
                        }

                        // success snackbar
                        withContext(Dispatchers.Main) {
                            snackbarHostState?.showSnackbar(context.getString(R.string.playlist_synced))
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            snackbarHostState?.showSnackbar("Import failed: ${e.message}")
                        }
                    } finally {
                        isImporting = false
                        withContext(Dispatchers.Main) { onDismiss() }
                    }
                }
            }
        )
    }

    if (showDuplicateDialog && existingPlaylistId != null) {
        DefaultDialog(
            onDismiss = { showDuplicateDialog = false },
            title = { Text(text = stringResource(R.string.import_playlist)) },
            content = {
                Text(text = stringResource(R.string.already_in_playlist))
            },
            buttons = {
                TextButton(onClick = {
                    // Skip
                    showDuplicateDialog = false
                    onDismiss()
                }) { Text(text = stringResource(android.R.string.cancel)) }

                TextButton(onClick = {
                    // Update existing: add songs
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val ids = songIds ?: onGetSong()
                            val playlist = database.playlist(existingPlaylistId!!).firstOrNull()
                            if (playlist != null) {
                                database.addSongToPlaylist(playlist, ids)
                            }
                            withContext(Dispatchers.Main) {
                                snackbarHostState?.showSnackbar(context.getString(R.string.playlist_synced))
                                showDuplicateDialog = false
                                onDismiss()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                snackbarHostState?.showSnackbar("Update failed: ${e.message}")
                            }
                        }
                    }
                }) { Text(text = stringResource(R.string.update)) }

                TextButton(onClick = {
                    // Import as new playlist (create copy)
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val ids = songIds ?: onGetSong()
                            val newPlaylist = PlaylistEntity(
                                name = textFieldValue.text,
                                browseId = null
                            )
                            database.query { insert(newPlaylist) }
                            val playlist = database.playlist(newPlaylist.id).firstOrNull()
                            if (playlist != null) database.addSongToPlaylist(playlist, ids)
                            withContext(Dispatchers.Main) {
                                snackbarHostState?.showSnackbar(context.getString(R.string.playlist_synced))
                                showDuplicateDialog = false
                                onDismiss()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                snackbarHostState?.let { it.showSnackbar("Import failed: ${e.message}") }
                            }
                        }
                    }
                }) { Text(text = stringResource(R.string.import_playlist)) }
            }
        )
    }
}
