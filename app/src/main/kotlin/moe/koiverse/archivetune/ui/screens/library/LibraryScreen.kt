package moe.koiverse.archivetune.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.ChipSortTypeKey
import moe.koiverse.archivetune.constants.LibraryFilter
import moe.koiverse.archivetune.ui.component.ChipsRow
import moe.koiverse.archivetune.utils.rememberEnumPreference

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

    val filterContent = @Composable {
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
                modifier = Modifier.weight(1f),
            )
        }
    }

    // Capture M3 Expressive colors from theme outside drawBehind
    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val color4 = MaterialTheme.colorScheme.primaryContainer
    val color5 = MaterialTheme.colorScheme.secondaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // M3E Mesh gradient background layer at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.7f) // Cover top 70% of screen
                .align(Alignment.TopCenter)
                .zIndex(-1f) // Place behind all content
                .drawBehind {
                    val width = size.width
                    val height = size.height
                    
                    // Create mesh gradient with 5 color blobs for more variation
                    // First color blob - top left
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color1.copy(alpha = 0.25f),
                                color1.copy(alpha = 0.15f),
                                color1.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.15f, height * 0.1f),
                            radius = width * 0.55f
                        )
                    )
                    
                    // Second color blob - top right
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color2.copy(alpha = 0.22f),
                                color2.copy(alpha = 0.12f),
                                color2.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.85f, height * 0.2f),
                            radius = width * 0.65f
                        )
                    )
                    
                    // Third color blob - middle left
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color3.copy(alpha = 0.2f),
                                color3.copy(alpha = 0.1f),
                                color3.copy(alpha = 0.03f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.3f, height * 0.45f),
                            radius = width * 0.6f
                        )
                    )
                    
                    // Fourth color blob - middle right
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color4.copy(alpha = 0.18f),
                                color4.copy(alpha = 0.09f),
                                color4.copy(alpha = 0.02f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.7f, height * 0.5f),
                            radius = width * 0.7f
                        )
                    )
                    
                    // Fifth color blob - bottom center (helps with smooth fade)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color5.copy(alpha = 0.15f),
                                color5.copy(alpha = 0.07f),
                                color5.copy(alpha = 0.02f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.5f, height * 0.75f),
                            radius = width * 0.8f
                        )
                    )
                    
                    // Add a final vertical gradient overlay to ensure smooth bottom fade
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                surfaceColor.copy(alpha = 0.3f),
                                surfaceColor.copy(alpha = 0.7f),
                                surfaceColor
                            ),
                            startY = height * 0.4f,
                            endY = height
                        )
                    )
                }
        ) {}

        when (filterType) {
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
