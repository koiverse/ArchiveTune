/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.screens.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.ChipSortTypeKey
import moe.koiverse.archivetune.constants.DisableBlurKey
import moe.koiverse.archivetune.constants.LibraryFilter
import moe.koiverse.archivetune.constants.PlaylistTagsFilterKey
import moe.koiverse.archivetune.constants.ShowTagsInLibraryKey
import moe.koiverse.archivetune.ui.component.ChipsRow
import moe.koiverse.archivetune.ui.component.LibraryMeshGradient
import moe.koiverse.archivetune.ui.component.TagsFilterChips
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    val database = LocalDatabase.current
    val (showTagsInLibrary) = rememberPreference(ShowTagsInLibraryKey, true)
    val (selectedTagsFilter, onSelectedTagsFilterChange) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }

    val filterContent = @Composable {
        Column {
            Row {
                ChipsRow(
                    chips =
                    listOf(
                        LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                        LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                        LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                        LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                    ),
                    currentValue = filterType,
                    onValueUpdate = {
                        filterType =
                            if (filterType == it) {
                                LibraryFilter.LIBRARY
                            } else {
                                it
                            }
                    },
                    icons = mapOf(
                        LibraryFilter.PLAYLISTS to R.drawable.queue_music,
                        LibraryFilter.SONGS to R.drawable.music_note,
                        LibraryFilter.ALBUMS to R.drawable.album,
                        LibraryFilter.ARTISTS to R.drawable.person,
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                )
            }

            if (showTagsInLibrary) {
                TagsFilterChips(
                    database = database,
                    selectedTags = selectedTagIds,
                    onTagToggle = { tag ->
                        val newTags = if (tag.id in selectedTagIds) {
                            selectedTagIds - tag.id
                        } else {
                            selectedTagIds + tag.id
                        }
                        onSelectedTagsFilterChange(newTags.joinToString(","))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!disableBlur) {
            LibraryMeshGradient(colors = gradientColors)
        }

        AnimatedContent(
            targetState = filterType,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 16 })
                    .togetherWith(fadeOut() + slideOutVertically { -it / 16 })
            },
            label = "libraryFilter",
        ) { target ->
            when (target) {
                LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
                LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
                LibraryFilter.SONGS -> LibrarySongsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY })

                LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY })

                LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY })
            }
        }
    }
}
