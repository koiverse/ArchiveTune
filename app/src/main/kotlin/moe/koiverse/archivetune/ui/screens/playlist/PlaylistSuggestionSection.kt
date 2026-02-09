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
import androidx.compose.runtime.mutableStateOf
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
    val isPlaying by playerConnection?.isPlaying?.collectAsState() ?: mutableStateOf(false)
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: mutableStateOf(null)
    
    val playlistSuggestions by viewModel.playlistSuggestions.collectAsState()
    val isLoading by viewModel.isLoadingSuggestions.collectAsState()
    
    playlistSuggestions?.let { suggestions ->
        if (suggestions.items.isNotEmpty()) {
            Column(
                modifier = modifier.fillMaxWidth()
            ) {
                // Header with refresh button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavigationTitle(
                        title = stringResource(R.string.you_might_like),
                        subtitle = if (suggestions.totalQueries > 1) {
                            "${suggestions.currentQueryIndex + 1}/${suggestions.totalQueries}"
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = { viewModel.resetAndLoadPlaylistSuggestions() },
                        onLongClick = {}
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = stringResource(R.string.refresh_suggestions)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Suggestions List (Vertical)
                suggestions.items.forEach { song ->
                    YouTubeListItem(
                        item = song,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying == true,
                        trailingContent = {
                            IconButton(
                                onClick = { 
                                    // Handle add to playlist
                                    coroutineScope.launch {
                                        try {
                                            val songItem = song as? SongItem
                                            val browseId = viewModel.playlist.value?.playlist?.browseId
                                            
                                            if (songItem == null) {
                                                Toast.makeText(context, R.string.error_unknown, Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }
                                            
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
                                if (song.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    val songItems = suggestions.items.filterIsInstance<SongItem>()
                                    val startIndex = songItems.indexOfFirst { it.id == song.id }
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
                    )
                }
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (suggestions.hasMore) {
                    TextButton(
                        onClick = { viewModel.loadMoreSuggestions() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(text = stringResource(R.string.more))
                    }
                }
            }
        }
    }
}
