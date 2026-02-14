/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */


package moe.koiverse.archivetune.ui.screens.playlist

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.extensions.toMediaItem
import moe.koiverse.archivetune.extensions.togglePlayPause
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.playback.queues.ListQueue
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.NavigationTitle
import moe.koiverse.archivetune.ui.component.YouTubeListItem
import moe.koiverse.archivetune.viewmodels.LocalPlaylistViewModel

@Composable
fun PlaylistSuggestionsSection(
    modifier: Modifier = Modifier,
    viewModel: LocalPlaylistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current
    val isPlaying by playerConnection?.isPlaying?.collectAsState() ?: androidx.compose.runtime.mutableStateOf(false)
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: androidx.compose.runtime.mutableStateOf(null)
    
    val playlistSuggestions by viewModel.playlistSuggestions.collectAsState()
    val isLoading by viewModel.isLoadingSuggestions.collectAsState()
    
    val currentSuggestions = playlistSuggestions
    if (currentSuggestions == null && !isLoading) return
    if (currentSuggestions != null && currentSuggestions.items.isEmpty() && !isLoading) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTitle(
                title = stringResource(R.string.you_might_like),
                subtitle = currentSuggestions?.let { s ->
                    if (s.totalQueries > 1) {
                        "${s.currentQueryIndex + 1} / ${s.totalQueries}"
                    } else null
                },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        currentSuggestions?.let { suggestions ->
            // Suggestions List (Vertical)
            suggestions.items.forEach { item ->
                YouTubeListItem(
                    item = item,
                    isActive = item.id == mediaMetadata?.id,
                    isPlaying = isPlaying == true,
                    trailingContent = {
                        IconButton(
                            onClick = { 
                                coroutineScope.launch {
                                    try {
                                        val songItem = item as? SongItem
                                        if (songItem == null) {
                                            Toast.makeText(context, R.string.error_unknown, Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }
                                        
                                        val browseId = viewModel.playlist.value?.playlist?.browseId
                                        val success = viewModel.addSongToPlaylist(
                                            song = songItem,
                                            browseId = browseId
                                        )
                                        
                                        if (success) {
                                            val playlistName = viewModel.playlist.value?.playlist?.name
                                            val message = if (playlistName != null) {
                                                context.getString(R.string.added_to_playlist, playlistName)
                                            } else {
                                                context.getString(R.string.add_to_playlist)
                                            }
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, R.string.error_unknown, Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("PlaylistSuggestions", "Error adding song to playlist", e)
                                        Toast.makeText(context, R.string.error_unknown, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onLongClick = {}
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = stringResource(R.string.add_to_playlist)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            if (playerConnection == null) return@clickable
                            if (item.id == mediaMetadata?.id) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                if (item is SongItem) {
                                    val songItems = suggestions.items.filterIsInstance<SongItem>()
                                    val startIndex = songItems.indexOfFirst { it.id == item.id }
                                    if (startIndex != -1) {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = context.getString(R.string.you_might_like),
                                                items = songItems.map { it.toMediaItem() },
                                                startIndex = startIndex
                                            )
                                        )
                                    }
                                }
                            }
                        }
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    TextButton(
                        onClick = { viewModel.resetAndLoadPlaylistSuggestions() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = stringResource(R.string.refresh_suggestions))
                        }
                    }
                }
            }
        }
    }
}
