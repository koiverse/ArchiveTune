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
import androidx.compose.ui.unit.sp
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
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import moe.koiverse.archivetune.R

class MusicWidget : GlanceAppWidget() {

    // Tell Glance to use DataStore Preferences as the state store.
    // This persists across process death — critical for closed-app reliability.
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            MusicWidgetContent()
        }
    }
}

// Helper function to determine if a color is dark (needs white text)
private fun isColorDark(color: Color): Boolean {
    // Calculate relative luminance using WCAG formula
    val red = color.red
    val green = color.green
    val blue = color.blue
    
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return luminance < 0.5  // Dark if luminance is less than 50%
}

@Composable
private fun MusicWidgetContent() {
    val prefs = currentState<Preferences>()
    val title = prefs[MusicWidgetKeys.TRACK_TITLE] ?: "Nothing playing"
    val artist = prefs[MusicWidgetKeys.TRACK_ARTIST] ?: ""
    val isPlaying = prefs[MusicWidgetKeys.IS_PLAYING] ?: false
    val artPath = prefs[MusicWidgetKeys.ART_PATH]
    val available = prefs[MusicWidgetKeys.IS_AVAILABLE] ?: false
    val dominantColor = prefs[MusicWidgetKeys.DOMINANT_COLOR]
    val playbackPosition = prefs[MusicWidgetKeys.PLAYBACK_POSITION] ?: 0f
    
    // Calculate text color based on background luminance
    val textColor = if (dominantColor != null) {
        val color = Color(dominantColor)
        if (isColorDark(color)) Color.White else Color.Black
    } else {
        Color.White  // Default to white for fallback background
    }
    
    val subtextColor = textColor.copy(alpha = 0.7f)

    // Determine background color with glassmorphism effect
    val bgColor = if (dominantColor != null) {
        ColorProvider(Color(dominantColor).copy(alpha = 0.85f))
    } else {
        GlanceTheme.colors.widgetBackground
    }

    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .cornerRadius(24.dp)
                .padding(12.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album art - clickable to open app
                    val artModifier = GlanceModifier
                        .size(60.dp)
                        .cornerRadius(8.dp)
                        .clickable(
                            actionStartActivity(
                                Intent(
                                    Intent.ACTION_MAIN
                                ).apply {
                                    component = ComponentName(
                                        "moe.koiverse.archivetune",
                                        "moe.koiverse.archivetune.MainActivity"
                                    )
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                            )
                        )
                    
                    if (artPath != null) {
                        val bitmap = BitmapFactory.decodeFile(artPath)
                        if (bitmap != null) {
                            Image(
                                provider = ImageProvider(bitmap),
                                contentDescription = "Album art - Tap to open app",
                                modifier = artModifier,
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                provider = ImageProvider(R.drawable.music_note),
                                contentDescription = "Album art - Tap to open app",
                                modifier = artModifier
                            )
                        }
                    } else {
                        Image(
                            provider = ImageProvider(R.drawable.music_note),
                            contentDescription = "Album art - Tap to open app",
                            modifier = artModifier
                        )
                    }

                    Spacer(GlanceModifier.width(12.dp))

                    // Track info
                    Column(
                        modifier = GlanceModifier.defaultWeight().wrapContentHeight()
                    ) {
                        Text(
                            text = title,
                            style = TextStyle(
                                color = ColorProvider(textColor),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                        if (artist.isNotEmpty()) {
                            Text(
                                text = artist,
                                style = TextStyle(
                                    color = ColorProvider(subtextColor),
                                    fontSize = 12.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    // Controls — only shown when something is/was loaded
                    if (available) {
                        Spacer(GlanceModifier.width(4.dp))
                        // Previous button - rounded triangle shape
                        TriangleButton(
                            imageProvider = ImageProvider(R.drawable.widget_triangle_prev),
                            contentDescription = "Previous",
                            onClick = actionRunCallback<SkipPrevAction>(),
                            backgroundColor = textColor.copy(alpha = 0.15f),
                            contentColor = textColor
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        // Play/Pause button - rounded square shape
                        SquareButton(
                            imageProvider = ImageProvider(
                                if (isPlaying) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            onClick = actionRunCallback<PlayPauseAction>(),
                            backgroundColor = textColor.copy(alpha = 0.15f),
                            contentColor = textColor
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        // Next button - rounded triangle shape
                        TriangleButton(
                            imageProvider = ImageProvider(R.drawable.widget_triangle_next),
                            contentDescription = "Next",
                            onClick = actionRunCallback<SkipNextAction>(),
                            backgroundColor = textColor.copy(alpha = 0.15f),
                            contentColor = textColor
                        )
                    }
                }

                // Progress bar
                if (available && playbackPosition > 0f) {
                    Spacer(GlanceModifier.height(8.dp))
                    androidx.glance.appwidget.LinearProgressIndicator(
                        progress = playbackPosition,
                        modifier = GlanceModifier.fillMaxWidth().height(3.dp),
                        color = if (dominantColor != null) {
                            ColorProvider(Color(dominantColor).copy(alpha = 1f))
                        } else {
                            GlanceTheme.colors.primary
                        },
                        backgroundColor = ColorProvider(Color.White.copy(alpha = 0.2f))
                    )
                }
            }
        }
    }
}

// Custom button composables for different shapes

@Composable
private fun TriangleButton(
    imageProvider: ImageProvider,
    contentDescription: String,
    onClick: androidx.glance.action.Action,
    backgroundColor: Color,
    contentColor: Color
) {
    Box(
        modifier = GlanceModifier
            .size(40.dp)
            .background(ColorProvider(backgroundColor))
            .cornerRadius(8.dp)  // Rounded triangle effect
            .clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = imageProvider,
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(contentColor))
        )
    }
}

@Composable
private fun SquareButton(
    imageProvider: ImageProvider,
    contentDescription: String,
    onClick: androidx.glance.action.Action,
    backgroundColor: Color,
    contentColor: Color
) {
    Box(
        modifier = GlanceModifier
            .size(40.dp)
            .background(ColorProvider(backgroundColor))
            .cornerRadius(12.dp)  // More rounded for square
            .clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = imageProvider,
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(contentColor))
        )
    }
}

// Made with Bob
