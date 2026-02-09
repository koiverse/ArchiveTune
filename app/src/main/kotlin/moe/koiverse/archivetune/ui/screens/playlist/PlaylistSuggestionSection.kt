/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */


package moe.koiverse.archivetune.ui.screens.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.util.Log
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.models.PlaylistSuggestion
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.NavigationTitle
import moe.koiverse.archivetune.ui.component.YouTubeListItem
import moe.koiverse.archivetune.viewmodels.LocalPlaylistViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun PlaylistSuggestionsSection(
    modifier: Modifier = Modifier,
    viewModel: LocalPlaylistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playlistSuggestions by viewModel.playlistSuggestions.collectAsState()
    val isLoading by viewModel.isLoadingSuggestions.collectAsState()
    val listState = rememberLazyListState()
    
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
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Suggestions LazyRow
                Box {
                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = suggestions.items,
                            key = { item -> item.id }
                        ) { song ->
                            YouTubeListItem(
                                item = song,
                                isActive = false,
                                isPlaying = false,
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
                                    .width(280.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        }
                        
                        // Loading indicator for pagination
                        item {
                            if (isLoading) {
                                Box(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .height(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    // Infinite scroll detection
                    InfiniteScrollEffect(
                        listState = listState,
                        isLoading = isLoading,
                        hasMore = suggestions.hasMore,
                        onLoadMore = { viewModel.loadMoreSuggestions() }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfiniteScrollEffect(
    listState: androidx.compose.foundation.lazy.LazyListState,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(listState, isLoading, hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val totalItems = listState.layoutInfo.totalItemsCount
                if (hasMore && lastVisibleIndex != null && lastVisibleIndex >= totalItems - 3 && !isLoading) {
                    onLoadMore()
                }
            }
    }
}