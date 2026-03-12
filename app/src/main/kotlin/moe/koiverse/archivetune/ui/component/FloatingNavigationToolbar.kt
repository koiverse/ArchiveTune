/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.koiverse.archivetune.ui.screens.Screens

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    val containerColor =
        if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val borderColor =
        if (pureBlack) {
            Color.White.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        }

    Surface(
        modifier = modifier.widthIn(max = 420.dp),
        shape = RoundedCornerShape(32.dp),
        color = containerColor,
        contentColor =
            if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = if (pureBlack) 0.dp else 8.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(32.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { screen ->
                val selected = isSelected(screen)

                FloatingNavigationToolbarItem(
                    screen = screen,
                    selected = selected,
                    slim = slim,
                    pureBlack = pureBlack,
                    onClick = { onItemClick(screen, selected) },
                )
            }
        }
    }
}

@Composable
private fun FloatingNavigationToolbarItem(
    screen: Screens,
    selected: Boolean,
    slim: Boolean,
    pureBlack: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val containerColor by animateColorAsState(
        targetValue =
            when {
                selected && pureBlack -> Color.White.copy(alpha = 0.12f)
                selected -> MaterialTheme.colorScheme.secondaryContainer
                else -> Color.Transparent
            },
        label = "",
    )
    val contentColor by animateColorAsState(
        targetValue =
            when {
                selected && pureBlack -> Color.White
                selected -> MaterialTheme.colorScheme.onSecondaryContainer
                pureBlack -> Color.White.copy(alpha = 0.82f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        label = "",
    )
    val showLabel = selected && !slim && screen.route != Screens.Search.route

    Row(
        modifier =
            Modifier
                .animateContentSize()
                .clip(shape)
                .background(color = containerColor, shape = shape)
                .clickable(role = Role.Tab, onClick = onClick)
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .padding(
                    horizontal = if (showLabel) 16.dp else 12.dp,
                    vertical = 12.dp,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = stringResource(screen.titleId),
            tint = contentColor,
        )

        if (showLabel) {
            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = stringResource(screen.titleId),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}