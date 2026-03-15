/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import moe.koiverse.archivetune.ui.screens.Screens

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    onShuffleClick: (() -> Unit)? = null,
    shuffleIconRes: Int? = null,
    shuffleContentDescription: String = "",
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    val toolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(
        toolbarContainerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
    )

    if (onShuffleClick != null && shuffleIconRes != null) {
        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = onShuffleClick,
                    containerColor = if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Icon(
                        painter = painterResource(shuffleIconRes),
                        contentDescription = shuffleContentDescription,
                    )
                }
            },
            modifier = modifier.widthIn(max = 480.dp),
            colors = toolbarColors,
        ) {
            items.forEach { screen ->
                val selected = isSelected(screen)

                FloatingNavigationToolbarItem(
                    screen = screen,
                    selected = selected,
                    pureBlack = pureBlack,
                    onClick = { onItemClick(screen, selected) },
                )
            }
        }
    } else {
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = modifier.widthIn(max = 420.dp),
            colors = toolbarColors,
        ) {
            items.forEach { screen ->
                val selected = isSelected(screen)

                FloatingNavigationToolbarItem(
                    screen = screen,
                    selected = selected,
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
    pureBlack: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "",
    )

    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 56.dp else 40.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "",
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "",
    )

    val baseIndicatorColor = if (pureBlack) Color.White.copy(alpha = 0.12f)
                             else MaterialTheme.colorScheme.secondaryContainer

    val contentColor by animateColorAsState(
        targetValue = when {
            selected && pureBlack -> Color.White
            selected -> MaterialTheme.colorScheme.onSecondaryContainer
            pureBlack -> Color.White.copy(alpha = 0.82f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "",
    )

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(32.dp)
                .background(
                    color = baseIndicatorColor.copy(alpha = baseIndicatorColor.alpha * indicatorAlpha),
                    shape = RoundedCornerShape(16.dp),
                )
        )
        Icon(
            painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = stringResource(screen.titleId),
            tint = contentColor,
        )
    }
}