/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YTItem
import moe.rukamori.archivetune.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SpeedDialGridItem(
    item: YTItem,
    isPinned: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    val motionScheme = MaterialTheme.motionScheme
    val containerColor by animateColorAsState(
        targetValue =
            if (isActive) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "speedDialContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "speedDialContentColor",
    )
    val labelColor by animateColorAsState(
        targetValue =
            if (isActive) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "speedDialLabelColor",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isActive) 2.dp else 0.dp,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "speedDialBorderWidth",
    )
    val playingScale by animateFloatAsState(
        targetValue = if (isPlaying && isActive) 0.96f else 1f,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "speedDialPlayingScale",
    )
    val shape = if (isActive) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large
    val thumbnailShape = if (item is ArtistItem) CircleShape else MaterialTheme.shapes.medium

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = shape,
        tonalElevation = if (isActive) 6.dp else 2.dp,
        border = BorderStroke(borderWidth, MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(playingScale),
    ) {
        ItemThumbnail(
            thumbnailUrl = item.thumbnail,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = thumbnailShape,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = labelColor,
                contentColor = contentColor,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(6.dp)
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (item !is SongItem) {
                        Icon(
                            painter = painterResource(R.drawable.navigate_next),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            if (isPinned) {
                Icon(
                    painter = painterResource(R.drawable.bookmark_filled),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(18.dp)
                )
            }
        }
    }
}
