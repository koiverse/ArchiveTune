/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.unit.ColorProvider
import moe.koiverse.archivetune.R

class AlbumArtWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            AlbumArtWidgetContent()
        }
    }
}

@Composable
private fun AlbumArtWidgetContent() {
    val prefs = currentState<Preferences>()
    val isPlaying = prefs[MusicWidgetKeys.IS_PLAYING] ?: false
    val artPath = prefs[MusicWidgetKeys.ART_PATH]
    val available = prefs[MusicWidgetKeys.IS_AVAILABLE] ?: false
    val dominantColor = prefs[MusicWidgetKeys.DOMINANT_COLOR]

    // Background color for album art container
    val bgColor = if (dominantColor != null) {
        ColorProvider(Color(dominantColor).copy(alpha = 0.3f))
    } else {
        ColorProvider(Color(0xFF1A1A1A))
    }

    GlanceTheme {
        // Root transparent container
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.Transparent)),
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: Album art (larger than widget - creates overflow effect)
            Box(
                modifier = GlanceModifier
                    .size(140.dp)  // Larger than 2×2 widget (120dp) - extends beyond boundaries!
                    .background(bgColor)
                    .cornerRadius(24.dp)
                    .clickable(
                        actionStartActivity(
                            Intent(Intent.ACTION_MAIN).apply {
                                component = ComponentName(
                                    "moe.koiverse.archivetune",
                                    "moe.koiverse.archivetune.MainActivity"
                                )
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (artPath != null) {
                    val bitmap = BitmapFactory.decodeFile(artPath)
                    if (bitmap != null) {
                        Image(
                            provider = ImageProvider(bitmap),
                            contentDescription = "Album art",
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .cornerRadius(20.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            provider = ImageProvider(R.drawable.music_note),
                            contentDescription = "Album art",
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .cornerRadius(20.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.music_note),
                        contentDescription = "Album art",
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(20.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Layer 2: Floating buttons (positioned at widget edges, NOT inside album art)
            if (available) {
                // Prev/Next buttons together at top-right edge
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = GlanceModifier.padding(top = 4.dp, end = 4.dp)
                    ) {
                        androidx.glance.layout.Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous button (left)
                            Box(
                                modifier = GlanceModifier
                                    .size(40.dp)
                                    .background(ColorProvider(Color.White.copy(alpha = 0.95f)))
                                    .cornerRadius(8.dp)
                                    .clickable(actionRunCallback<SkipPrevAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.widget_triangle_prev),
                                    contentDescription = "Previous",
                                    modifier = GlanceModifier.size(20.dp),
                                    colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(Color.Black))
                                )
                            }

                            androidx.glance.layout.Spacer(GlanceModifier.width(6.dp))

                            // Next button (right)
                            Box(
                                modifier = GlanceModifier
                                    .size(40.dp)
                                    .background(ColorProvider(Color.White.copy(alpha = 0.95f)))
                                    .cornerRadius(8.dp)
                                    .clickable(actionRunCallback<SkipNextAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.widget_triangle_next),
                                    contentDescription = "Next",
                                    modifier = GlanceModifier.size(20.dp),
                                    colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(Color.Black))
                                )
                            }
                        }
                    }
                }

                // Play/Pause button at bottom-left edge
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Box(
                        modifier = GlanceModifier.padding(bottom = 4.dp, start = 4.dp)
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(48.dp)
                                .background(ColorProvider(Color.White.copy(alpha = 0.95f)))
                                .cornerRadius(16.dp)
                                .clickable(actionRunCallback<PlayPauseAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(
                                    if (isPlaying) R.drawable.pause else R.drawable.play
                                ),
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = GlanceModifier.size(24.dp),
                                colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(Color.Black))
                            )
                        }
                    }
                }
            }
        }
    }
}

// Made with Bob