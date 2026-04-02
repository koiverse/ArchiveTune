@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * ArchiveTune Project Original (2026)
 * Koi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.screens.Screens

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    onFabClick: (() -> Unit)? = null,
    fabIconRes: Int? = null,
    fabContentDescription: String = "",
    onShuffleClick: (() -> Unit)? = null,
    shuffleIconRes: Int? = null,
    shuffleContentDescription: String = "",
    onMusicRecognitionClick: (() -> Unit)? = null,
    musicRecognitionContentDescription: String = "",
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    val toolbarContainerColor = floatingToolbarContainerColor(pureBlack = pureBlack)
    val toolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(
        toolbarContainerColor = toolbarContainerColor,
    )
    val hasOverflowAction = onShuffleClick != null && shuffleIconRes != null
    val hasFabAction = onFabClick != null && fabIconRes != null

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val useCompactFallback = maxWidth < if (hasOverflowAction || hasFabAction) 336.dp else 280.dp
        val toolbarWidth = if (hasOverflowAction || hasFabAction) 480.dp else 420.dp
        val showSelectedLabels = maxWidth >= 360.dp
        val compactAction: (@Composable () -> Unit)? =
            when {
                hasOverflowAction -> {
                    {
                        FloatingToolbarOverflowAction(
                            pureBlack = pureBlack,
                            compact = true,
                            onShuffleClick = onShuffleClick,
                            shuffleIconRes = shuffleIconRes,
                            shuffleContentDescription = shuffleContentDescription,
                            onMusicRecognitionClick = onMusicRecognitionClick,
                            musicRecognitionContentDescription = musicRecognitionContentDescription,
                        )
                    }
                }

                hasFabAction -> {
                    {
                        FloatingToolbarFabAction(
                            pureBlack = pureBlack,
                            compact = true,
                            onClick = onFabClick,
                            iconRes = fabIconRes,
                            contentDescription = fabContentDescription,
                        )
                    }
                }

                else -> null
            }

        if (useCompactFallback) {
            CompactFloatingNavigationToolbar(
                items = items,
                pureBlack = pureBlack,
                containerColor = toolbarContainerColor,
                showSelectedLabels = false,
                action = compactAction,
                isSelected = isSelected,
                onItemClick = onItemClick,
                modifier = Modifier.fillMaxWidth().widthIn(max = toolbarWidth),
            )
            return@BoxWithConstraints
        }

        if (hasOverflowAction) {
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingToolbarOverflowAction(
                        pureBlack = pureBlack,
                        compact = false,
                        onShuffleClick = onShuffleClick,
                        shuffleIconRes = shuffleIconRes,
                        shuffleContentDescription = shuffleContentDescription,
                        onMusicRecognitionClick = onMusicRecognitionClick,
                        musicRecognitionContentDescription = musicRecognitionContentDescription,
                    )
                },
                modifier = Modifier.widthIn(max = 480.dp),
                colors = toolbarColors,
            ) {
                items.forEach { screen ->
                    val selected = isSelected(screen)

                    FloatingNavigationToolbarItem(
                        screen = screen,
                        selected = selected,
                        showSelectedLabel = showSelectedLabels,
                        pureBlack = pureBlack,
                        onClick = { onItemClick(screen, selected) },
                    )
                }
            }
        } else if (hasFabAction) {
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingToolbarFabAction(
                        pureBlack = pureBlack,
                        compact = false,
                        onClick = onFabClick,
                        iconRes = fabIconRes,
                        contentDescription = fabContentDescription,
                    )
                },
                modifier = Modifier.widthIn(max = 480.dp),
                colors = toolbarColors,
            ) {
                items.forEach { screen ->
                    val selected = isSelected(screen)

                    FloatingNavigationToolbarItem(
                        screen = screen,
                        selected = selected,
                        showSelectedLabel = showSelectedLabels,
                        pureBlack = pureBlack,
                        onClick = { onItemClick(screen, selected) },
                    )
                }
            }
        } else {
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = Modifier.widthIn(max = 420.dp),
                colors = toolbarColors,
            ) {
                items.forEach { screen ->
                    val selected = isSelected(screen)

                    FloatingNavigationToolbarItem(
                        screen = screen,
                        selected = selected,
                        showSelectedLabel = showSelectedLabels,
                        pureBlack = pureBlack,
                        onClick = { onItemClick(screen, selected) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactFloatingNavigationToolbar(
    items: List<Screens>,
    pureBlack: Boolean,
    containerColor: Color,
    showSelectedLabels: Boolean,
    action: (@Composable () -> Unit)?,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { screen ->
                val selected = isSelected(screen)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    FloatingNavigationToolbarItem(
                        screen = screen,
                        selected = selected,
                        showSelectedLabel = showSelectedLabels,
                        pureBlack = pureBlack,
                        onClick = { onItemClick(screen, selected) },
                    )
                }
            }

            if (action != null) {
                Box(contentAlignment = Alignment.Center) {
                    action()
                }
            }
        }
    }
}

@Composable
private fun FloatingToolbarOverflowAction(
    pureBlack: Boolean,
    compact: Boolean,
    onShuffleClick: (() -> Unit)?,
    shuffleIconRes: Int?,
    shuffleContentDescription: String,
    onMusicRecognitionClick: (() -> Unit)?,
    musicRecognitionContentDescription: String,
) {
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Box {
        if (compact) {
            FloatingToolbarFallbackActionButton(
                pureBlack = pureBlack,
                onClick = { fabMenuExpanded = !fabMenuExpanded },
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_horiz),
                    contentDescription =
                        shuffleContentDescription.ifEmpty {
                            stringResource(R.string.more)
                        },
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = { fabMenuExpanded = !fabMenuExpanded },
                containerColor = floatingToolbarFabContainerColor(pureBlack = pureBlack),
                contentColor = floatingToolbarFabContentColor(pureBlack = pureBlack),
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_horiz),
                    contentDescription =
                        shuffleContentDescription.ifEmpty {
                            stringResource(R.string.more)
                        },
                )
            }
        }

        DropdownMenu(
            expanded = fabMenuExpanded,
            onDismissRequest = { fabMenuExpanded = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.music_recognition)) },
                onClick = {
                    fabMenuExpanded = false
                    onMusicRecognitionClick?.invoke()
                },
                leadingIcon = {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = floatingToolbarMenuIconContainerColor(pureBlack = pureBlack),
                        contentColor = floatingToolbarMenuIconContentColor(pureBlack = pureBlack),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.mic),
                                contentDescription =
                                    musicRecognitionContentDescription.ifEmpty {
                                        stringResource(R.string.music_recognition)
                                    },
                            )
                        }
                    }
                },
                enabled = onMusicRecognitionClick != null,
                colors =
                    MenuDefaults.itemColors(
                        textColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = if (pureBlack) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        disabledLeadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    ),
            )

            if (onShuffleClick != null && shuffleIconRes != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.shuffle)) },
                    onClick = {
                        fabMenuExpanded = false
                        onShuffleClick()
                    },
                    leadingIcon = {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = floatingToolbarMenuIconContainerColor(pureBlack = pureBlack),
                            contentColor = floatingToolbarMenuIconContentColor(pureBlack = pureBlack),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(shuffleIconRes),
                                    contentDescription =
                                        shuffleContentDescription.ifEmpty {
                                            stringResource(R.string.shuffle)
                                        },
                                )
                            }
                        }
                    },
                    colors =
                        MenuDefaults.itemColors(
                            textColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                            leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )
            }
        }
    }
}

@Composable
private fun FloatingToolbarFabAction(
    pureBlack: Boolean,
    compact: Boolean,
    onClick: (() -> Unit)?,
    iconRes: Int?,
    contentDescription: String,
) {
    if (onClick == null || iconRes == null) return

    if (compact) {
        FloatingToolbarFallbackActionButton(
            pureBlack = pureBlack,
            onClick = onClick,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription =
                    contentDescription.ifEmpty {
                        stringResource(R.string.create_playlist)
                    },
                modifier = Modifier.size(20.dp),
            )
        }
    } else {
        FloatingToolbarDefaults.VibrantFloatingActionButton(
            onClick = onClick,
            containerColor = floatingToolbarFabContainerColor(pureBlack = pureBlack),
            contentColor = floatingToolbarFabContentColor(pureBlack = pureBlack),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription =
                    contentDescription.ifEmpty {
                        stringResource(R.string.create_playlist)
                    },
            )
        }
    }
}

@Composable
private fun FloatingToolbarFallbackActionButton(
    pureBlack: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = CircleShape
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "",
    )

    Surface(
        modifier = modifier.size(40.dp).scale(scale),
        color = floatingToolbarFabContainerColor(pureBlack = pureBlack),
        contentColor = floatingToolbarFabContentColor(pureBlack = pureBlack),
        shape = shape,
    ) {
        Box(
            modifier =
                Modifier
                    .clip(shape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onClick,
                    )
                    .size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun FloatingNavigationToolbarItem(
    screen: Screens,
    selected: Boolean,
    showSelectedLabel: Boolean,
    pureBlack: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val containerColor by animateColorAsState(
        targetValue =
            when {
                selected -> floatingToolbarSelectedItemContainerColor(pureBlack = pureBlack)
                else -> Color.Transparent
            },
        label = "",
    )
    val contentColor by animateColorAsState(
        targetValue =
            when {
                selected -> floatingToolbarSelectedItemContentColor(pureBlack = pureBlack)
                else -> floatingToolbarItemContentColor(pureBlack = pureBlack)
            },
        label = "",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "",
    )
    val showLabel = selected && showSelectedLabel && screen.route != Screens.Search.route

    Row(
        modifier =
            Modifier
                .scale(scale)
                .animateContentSize()
                .clip(shape)
                .background(color = containerColor, shape = shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .widthIn(min = 48.dp)
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

@Composable
private fun floatingToolbarContainerColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
}

@Composable
private fun floatingToolbarFabContainerColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiaryContainer
}

@Composable
private fun floatingToolbarFabContentColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White else MaterialTheme.colorScheme.onTertiaryContainer
}

@Composable
private fun floatingToolbarSelectedItemContainerColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun floatingToolbarSelectedItemContentColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
}

@Composable
private fun floatingToolbarItemContentColor(pureBlack: Boolean): Color {
    return if (pureBlack) {
        Color.White.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun floatingToolbarMenuIconContainerColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun floatingToolbarMenuIconContentColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
}
