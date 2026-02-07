/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.MusicDatabase

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignTagsDialog(
    database: MusicDatabase,
    playlistId: String,
    onDismiss: () -> Unit
) {
    val allTags by database.allTags().collectAsState(initial = emptyList())
    val currentTags by database.playlistTags(playlistId).collectAsState(initial = emptyList())
    
    val currentTagIds = remember(currentTags) { currentTags.map { it.id }.toSet() }
    var selectedTagIds by remember(currentTagIds) { mutableStateOf(currentTagIds) }
    var showManageTagsDialog by remember { mutableStateOf(false) }
    var showAssignToPlaylistsDialog by remember { mutableStateOf(false) }

    if (showManageTagsDialog) {
        TagsManagementDialog(
            database = database,
            onDismiss = { showManageTagsDialog = false }
        )
    }

    if (showAssignToPlaylistsDialog) {
        AssignTagsToPlaylistsDialog(
            database = database,
            initialSelectedTagIds = selectedTagIds,
            initialSelectedPlaylistIds = setOf(playlistId),
            onDismiss = { showAssignToPlaylistsDialog = false },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.assign_tags),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (allTags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_tags_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTags.forEach { tag ->
                            TagChip(
                                tag = tag,
                                selected = tag.id in selectedTagIds,
                                onClick = {
                                    selectedTagIds = if (tag.id in selectedTagIds) {
                                        selectedTagIds - tag.id
                                    } else {
                                        selectedTagIds + tag.id
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = { showManageTagsDialog = true }) {
                            Text(stringResource(R.string.manage_tags))
                        }

                        TextButton(
                            enabled = allTags.isNotEmpty(),
                            onClick = { showAssignToPlaylistsDialog = true },
                        ) {
                            Text(stringResource(R.string.assign_tags_to_playlists))
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(android.R.string.cancel))
                        }

                        Button(
                            onClick = {
                                database.transaction {
                                    removeAllPlaylistTags(playlistId)
                                    selectedTagIds.forEach { tagId ->
                                        addTagToPlaylist(playlistId, tagId)
                                    }
                                }
                                onDismiss()
                            }
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssignTagsToPlaylistsDialog(
    database: MusicDatabase,
    initialSelectedTagIds: Set<String>,
    initialSelectedPlaylistIds: Set<String>,
    onDismiss: () -> Unit,
) {
    val allTags by database.allTags().collectAsState(initial = emptyList())
    val playlists by database.editablePlaylistsByCreateDateAsc().collectAsState(initial = emptyList())

    var selectedTagIds by remember(initialSelectedTagIds) { mutableStateOf(initialSelectedTagIds) }
    var selectedPlaylistIds by remember(initialSelectedPlaylistIds) { mutableStateOf(initialSelectedPlaylistIds) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.assign_tags_to_playlists),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (allTags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_tags_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTags.forEach { tag ->
                            TagChip(
                                tag = tag,
                                selected = tag.id in selectedTagIds,
                                onClick = {
                                    selectedTagIds = if (tag.id in selectedTagIds) {
                                        selectedTagIds - tag.id
                                    } else {
                                        selectedTagIds + tag.id
                                    }
                                }
                            )
                        }
                    }
                }

                if (playlists.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                    ) {
                        items(playlists.asReversed(), key = { it.id }) { playlist ->
                            val isSelected = playlist.id in selectedPlaylistIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPlaylistIds =
                                            if (isSelected) {
                                                selectedPlaylistIds - playlist.id
                                            } else {
                                                selectedPlaylistIds + playlist.id
                                            }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlaylistListItem(
                                    playlist = playlist,
                                    modifier = Modifier.weight(1f)
                                )

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedPlaylistIds =
                                            if (checked) {
                                                selectedPlaylistIds + playlist.id
                                            } else {
                                                selectedPlaylistIds - playlist.id
                                            }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }

                    Button(
                        enabled = selectedPlaylistIds.isNotEmpty() && selectedTagIds.isNotEmpty(),
                        onClick = {
                            database.transaction {
                                addTagsToPlaylists(
                                    playlistIds = selectedPlaylistIds.toList(),
                                    tagIds = selectedTagIds.toList(),
                                )
                            }
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}
