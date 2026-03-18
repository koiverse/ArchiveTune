/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean,
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
    val toolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(
        toolbarContainerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
    )

    if (onShuffleClick != null && shuffleIconRes != null) {
        var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                Box {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = { fabMenuExpanded = !fabMenuExpanded },
                        containerColor = if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_horiz),
                            contentDescription =
                                shuffleContentDescription.ifEmpty {
                                    stringResource(R.string.more)
                                },
                        )
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
                                    color =
                                        if (pureBlack) {
                                            Color.White.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        },
                                    contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
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
                                    color =
                                        if (pureBlack) {
                                            Color.White.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        },
                                    contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
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
            },
            modifier = modifier.widthIn(max = 480.dp),
            colors = toolbarColors,
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
    } else if (onFabClick != null && fabIconRes != null) {
        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = onFabClick,
                    containerColor = if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Icon(
                        painter = painterResource(fabIconRes),
                        contentDescription =
                            fabContentDescription.ifEmpty {
                                stringResource(R.string.create_playlist)
                            },
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
                    slim = slim,
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
    val showLabel = selected && !slim && screen.route != Screens.Search.route

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
