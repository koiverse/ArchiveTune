/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import moe.koiverse.archivetune.innertube.utils.parseCookieString
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.InnerTubeCookieKey
import moe.koiverse.archivetune.db.entities.Playlist
import moe.koiverse.archivetune.ui.component.CreatePlaylistDialog
import moe.koiverse.archivetune.ui.component.DefaultDialog
import moe.koiverse.archivetune.ui.component.PlaylistListItem
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.Locale

internal fun playlistsForAddToPlaylist(playlists: List<Playlist>): List<Playlist> =
    playlists.filter { it.playlist.isEditable || it.playlist.browseId != null }

internal enum class AddToPlaylistSortOption {
    RECENTLY_MODIFIED,
    RECENTLY_CREATED,
    MOST_PLAYED,
}

internal fun visiblePlaylistsForAddToPlaylist(
    playlists: List<Playlist>,
    sortOption: AddToPlaylistSortOption,
    query: String,
    playlistPlayCounts: Map<String, Long> = emptyMap(),
): List<Playlist> {
    val normalizedQuery = query.trim()
    val filteredPlaylists =
        playlistsForAddToPlaylist(playlists).filter { playlist ->
            normalizedQuery.isBlank() ||
                playlist.playlist.name.contains(normalizedQuery, ignoreCase = true)
        }

    return filteredPlaylists.sortedWith { first, second ->
        when (sortOption) {
            AddToPlaylistSortOption.RECENTLY_MODIFIED ->
                compareNullableDates(
                    second.playlist.lastUpdateTime ?: second.playlist.createdAt,
                    first.playlist.lastUpdateTime ?: first.playlist.createdAt,
                )

            AddToPlaylistSortOption.RECENTLY_CREATED ->
                compareNullableDates(second.playlist.createdAt, first.playlist.createdAt)

            AddToPlaylistSortOption.MOST_PLAYED -> {
                compareValues(
                    playlistPlayCounts[second.id] ?: 0L,
                    playlistPlayCounts[first.id] ?: 0L,
                ).takeIf { it != 0 }
                    ?: compareNullableDates(
                        second.playlist.lastUpdateTime ?: second.playlist.createdAt,
                        first.playlist.lastUpdateTime ?: first.playlist.createdAt,
                    )
            }
        }.takeIf { it != 0 }
            ?: compareValues(
                first.playlist.name.lowercase(Locale.getDefault()),
                second.playlist.name.lowercase(Locale.getDefault()),
            )
    }
}

private fun compareNullableDates(
    first: LocalDateTime?,
    second: LocalDateTime?,
): Int {
    if (first == second) return 0
    if (first == null) return -1
    if (second == null) return 1
    return first.compareTo(second)
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val allPlaylists by database.playlistsByCreateDateAsc().collectAsState(initial = emptyList())
    val playlistPlayCounts by database.playlistPlayCounts().collectAsState(initial = emptyList())
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    var sortOption by rememberSaveable { mutableStateOf(AddToPlaylistSortOption.RECENTLY_CREATED) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchField by rememberSaveable { mutableStateOf(false) }
    val playlists = remember(allPlaylists, sortOption, searchQuery, playlistPlayCounts) {
        visiblePlaylistsForAddToPlaylist(
            playlists = allPlaylists,
            sortOption = sortOption,
            query = searchQuery,
            playlistPlayCounts = playlistPlayCounts.associate { it.playlistId to it.playCount },
        )
    }
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

    LaunchedEffect(isVisible) {
        if (isVisible) {
            songIds = null
            setSelectedPlaylistIds(emptySet())
            isAddingToPlaylist = false
            showDuplicateDialog = false
            playlistsWithDuplicates = emptyList()
            duplicateSongsMap = emptyMap()
            searchQuery = ""
            showSearchField = false
            sortOption = AddToPlaylistSortOption.RECENTLY_CREATED
        }
    }

    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(28.dp),
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column {
                    Column(
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.playlist_add),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Add to playlist",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (selectedPlaylistIds.isNotEmpty()) {
                                    Text(
                                        text = "${selectedPlaylistIds.size} selected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (showSearchField) {
                                        showSearchField = false
                                        searchQuery = ""
                                    } else {
                                        showSearchField = true
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (showSearchField) R.drawable.close else R.drawable.search
                                    ),
                                    contentDescription = stringResource(R.string.search),
                                )
                            }
                        }

                        AnimatedVisibility(visible = showSearchField) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                placeholder = { Text(stringResource(R.string.search)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.search),
                                        contentDescription = null,
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                painter = painterResource(R.drawable.close),
                                                contentDescription = stringResource(R.string.close),
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search,
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AddToPlaylistSortChip(
                            label = stringResource(R.string.sort_by_last_updated),
                            selected = sortOption == AddToPlaylistSortOption.RECENTLY_MODIFIED,
                            onClick = { sortOption = AddToPlaylistSortOption.RECENTLY_MODIFIED },
                        )
                        AddToPlaylistSortChip(
                            label = stringResource(R.string.sort_by_create_date),
                            selected = sortOption == AddToPlaylistSortOption.RECENTLY_CREATED,
                            onClick = { sortOption = AddToPlaylistSortOption.RECENTLY_CREATED },
                        )
                        AddToPlaylistSortChip(
                            label = stringResource(R.string.sort_by_most_played),
                            selected = sortOption == AddToPlaylistSortOption.MOST_PLAYED,
                            onClick = { sortOption = AddToPlaylistSortOption.MOST_PLAYED },
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreatePlaylistDialog = true }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = stringResource(R.string.create_playlist),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    if (playlists.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                        ) {
                            items(playlists, key = { it.id }) { playlist ->
                                val isSelected = selectedPlaylistIds.contains(playlist.id)
                                val rowBackground by animateColorAsState(
                                    targetValue = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else
                                        androidx.compose.ui.graphics.Color.Transparent,
                                    animationSpec = tween(durationMillis = 180),
                                    label = "rowBackground",
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBackground)
                                        .clickable {
                                            val currentIds = selectedPlaylistIds.toMutableSet()
                                            if (isSelected) currentIds.remove(playlist.id)
                                            else currentIds.add(playlist.id)
                                            setSelectedPlaylistIds(currentIds)
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    PlaylistListItem(
                                        playlist = playlist,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .padding(end = 16.dp)
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                painter = painterResource(R.drawable.done),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "No playlists yet"
                                } else {
                                    stringResource(R.string.no_matching_playlists)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(android.R.string.cancel))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
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
                            },
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        ) {
                            if (isAddingToPlaylist) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                                Text(
                                    text = if (selectedPlaylistIds.size > 1)
                                        "Add to ${selectedPlaylistIds.size}"
                                    else
                                        "Add"
                                )
                            }
                        }
                    }
                }
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

@Composable
private fun AddToPlaylistSortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 40.dp,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.done),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        },
        modifier = modifier.heightIn(min = minHeight),
        shape = RoundedCornerShape(16.dp),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}
