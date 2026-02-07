package moe.koiverse.archivetune.ui.screens.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.ListItemHeight
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.extensions.togglePlayPause
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.models.PlaylistSuggestionsState
import moe.koiverse.archivetune.models.toMediaMetadata
import moe.koiverse.archivetune.playback.PlayerConnection
import moe.koiverse.archivetune.playback.queues.YouTubeQueue
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.component.SongListItem
import moe.koiverse.archivetune.ui.component.shimmer.ListItemPlaceHolder
import moe.koiverse.archivetune.ui.component.shimmer.ShimmerHost
import moe.koiverse.archivetune.ui.menu.YouTubeSongMenu

/**
 * Section displaying song suggestions based on playlist name.
 * Shows a vertical list of songs with quick add functionality.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistSuggestionSection(
    state: PlaylistSuggestionsState,
    playlistName: String,
    existingSongIds: Set<String>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    scope: CoroutineScope,
    onAddToPlaylist: (SongItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Section title - smaller font like SortHeader
        Text(
            text = stringResource(R.string.you_might_like),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        when (state) {
            is PlaylistSuggestionsState.Idle -> {
                // Show nothing or initial state
            }

            is PlaylistSuggestionsState.Loading -> {
                // Show shimmer loading state
                ShimmerHost {
                    repeat(3) {
                        ListItemPlaceHolder()
                    }
                }
            }

            is PlaylistSuggestionsState.Error -> {
                // Show error with retry button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ListItemHeight * 2)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(onClick = onRetry) {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = stringResource(R.string.retry)
                            )
                        }
                    }
                }
            }

            is PlaylistSuggestionsState.Success -> {
                val suggestions = state.suggestion.songs
                    .filter { it.id !in existingSongIds }
                    .take(10) // Limit to 10 suggestions

                if (suggestions.isEmpty()) {
                    // No suggestions available
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = stringResource(R.string.no_suggestions_available),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Vertical list of suggested songs
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggestions.forEach { songItem ->
                            SuggestedSongItem(
                                songItem = songItem,
                                mediaMetadata = mediaMetadata,
                                isPlaying = isPlaying,
                                navController = navController,
                                playerConnection = playerConnection,
                                scope = scope,
                                onAddToPlaylist = { onAddToPlaylist(songItem) },
                                onShowMenu = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        YouTubeSongMenu(
                                            song = songItem,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                        }

                        // Refresh button at the bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = stringResource(R.string.refresh),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * A single suggested song item with add button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestedSongItem(
    songItem: SongItem,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    scope: CoroutineScope,
    onAddToPlaylist: () -> Unit,
    onShowMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Convert SongItem to local Song entity for SongListItem
    val song = rememberSongFromItem(songItem)

    SongListItem(
        song = song,
        isActive = songItem.id == mediaMetadata?.id,
        isPlaying = isPlaying,
        showInLibraryIcon = false,
        trailingContent = {
            // Add to playlist button
            IconButton(
                onClick = onAddToPlaylist,
                onLongClick = {}
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = stringResource(R.string.add_to_playlist),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // More options button
            IconButton(
                onClick = onShowMenu,
                onLongClick = {}
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = stringResource(R.string.more)
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (songItem.id == mediaMetadata?.id) {
                        playerConnection.player.togglePlayPause()
                    } else {
                        songItem.endpoint?.let { endpoint ->
                            playerConnection.playQueue(
                                YouTubeQueue(
                                    endpoint,
                                    songItem.toMediaMetadata()
                                )
                            )
                        }
                    }
                },
                onLongClick = onShowMenu
            )
    )
}

/**
 * Creates a local Song entity from a YouTube SongItem for display purposes.
 */
@Composable
private fun rememberSongFromItem(songItem: SongItem): Song {
    return Song(
        moe.koiverse.archivetune.db.entities.SongEntity(
            id = songItem.id,
            title = songItem.title,
            duration = -1, // Duration not always available from search
            thumbnailUrl = songItem.thumbnail,
            liked = false,
            inLibrary = null
        ),
        artists = songItem.artists.map { artist ->
            moe.koiverse.archivetune.db.entities.ArtistEntity(
                id = artist.id ?: "",
                name = artist.name
            )
        },
        album = null
    )
}
