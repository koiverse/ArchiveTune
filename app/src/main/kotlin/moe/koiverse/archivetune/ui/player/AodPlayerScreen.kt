/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package moe.koiverse.archivetune.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.utils.makeTimeString

@Composable
fun AodPlayerScreen(
    mediaMetadata: MediaMetadata,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    sliderPosition: Long?,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    thumbnailCornerRadius: Float,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayPosition = sliderPosition ?: position
    val seekEnabled = duration > 0L && duration != C.TIME_UNSET
    val sliderValue = remember(displayPosition, duration) {
        if (seekEnabled) displayPosition.toFloat() else 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        IconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = stringResource(R.string.aod_mode_exit),
                tint = Color.White.copy(alpha = 0.7f),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 40.dp),
        ) {
            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(thumbnailCornerRadius.dp)),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = mediaMetadata.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = mediaMetadata.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Slider(
                    value = sliderValue,
                    onValueChange = { if (seekEnabled) onSeek(it.toLong()) },
                    onValueChangeFinished = onSeekFinished,
                    valueRange = 0f..(if (seekEnabled) duration.toFloat() else 1f),
                    enabled = seekEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                        disabledThumbColor = Color.White.copy(alpha = 0.3f),
                        disabledActiveTrackColor = Color.White.copy(alpha = 0.3f),
                        disabledInactiveTrackColor = Color.White.copy(alpha = 0.15f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = makeTimeString(displayPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                    Text(
                        text = if (seekEnabled) makeTimeString(duration) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = onSkipPrevious,
                    enabled = canSkipPrevious,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = null,
                        tint = if (canSkipPrevious) Color.White else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(32.dp),
                    )
                }

                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    enabled = canSkipNext,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        tint = if (canSkipNext) Color.White else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
