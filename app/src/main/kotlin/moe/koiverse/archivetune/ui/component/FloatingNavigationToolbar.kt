/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package moe.koiverse.archivetune.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.screens.Screens

private enum class FloatingToolbarActionState {
    None,
    Overflow,
    Fab,
}

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
    val actionState =
        when {
            hasOverflowAction -> FloatingToolbarActionState.Overflow
            hasFabAction -> FloatingToolbarActionState.Fab
            else -> FloatingToolbarActionState.None
        }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val showSelectedLabels = maxWidth >= 360.dp

        AnimatedContent(
            targetState = actionState,
            transitionSpec = {
                (
                    fadeIn(animationSpec = floatingToolbarEffectsFloatSpec()) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = floatingToolbarSpatialFloatSpec(),
                        ) +
                        slideInHorizontally(
                            animationSpec = floatingToolbarSpatialOffsetSpec(),
                            initialOffsetX = { width -> width / 6 },
                        )
                    ) togetherWith
                    (
                        fadeOut(animationSpec = floatingToolbarEffectsFloatSpec()) +
                            scaleOut(
                                targetScale = 0.92f,
                                animationSpec = floatingToolbarSpatialFloatSpec(),
                            ) +
                            slideOutHorizontally(
                                animationSpec = floatingToolbarSpatialOffsetSpec(),
                                targetOffsetX = { width -> width / 8 },
                            )
                        ) using SizeTransform(clip = false)
            },
            label = "FloatingToolbarActionState",
        ) { currentActionState ->
            when (currentActionState) {
                FloatingToolbarActionState.Overflow -> {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        floatingActionButton = {
                            FloatingToolbarOverflowAction(
                                pureBlack = pureBlack,
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
                }

                FloatingToolbarActionState.Fab -> {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        floatingActionButton = {
                            FloatingToolbarFabAction(
                                pureBlack = pureBlack,
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
                }

                FloatingToolbarActionState.None -> {
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
    }
}

@Composable
private fun FloatingToolbarOverflowAction(
    pureBlack: Boolean,
    onShuffleClick: (() -> Unit)?,
    shuffleIconRes: Int?,
    shuffleContentDescription: String,
    onMusicRecognitionClick: (() -> Unit)?,
    musicRecognitionContentDescription: String,
) {
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Box {
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
    onClick: (() -> Unit)?,
    iconRes: Int?,
    contentDescription: String,
) {
    if (onClick == null || iconRes == null) return

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
        animationSpec = floatingToolbarEffectsColorSpec(),
        label = "",
    )
    val contentColor by animateColorAsState(
        targetValue =
            when {
                selected -> floatingToolbarSelectedItemContentColor(pureBlack = pureBlack)
                else -> floatingToolbarItemContentColor(pureBlack = pureBlack)
            },
        animationSpec = floatingToolbarEffectsColorSpec(),
        label = "",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = floatingToolbarSpatialFloatSpec(),
        label = "",
    )
    val showLabel = selected && showSelectedLabel && screen.route != Screens.Search.route

    Row(
        modifier =
            Modifier
                .scale(itemScale)
                .animateContentSize(animationSpec = floatingToolbarSpatialSizeSpec())
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
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                val activating = targetState

                (
                    fadeIn(animationSpec = floatingToolbarEffectsFloatSpec()) +
                        scaleIn(
                            initialScale = if (activating) 0.82f else 1.12f,
                            animationSpec = floatingToolbarSpatialFloatSpec(),
                        ) +
                        slideInHorizontally(
                            animationSpec = floatingToolbarSpatialOffsetSpec(),
                            initialOffsetX = { width -> if (activating) width / 3 else -width / 3 },
                        )
                    ) togetherWith
                    (
                        fadeOut(animationSpec = floatingToolbarEffectsFloatSpec()) +
                            scaleOut(
                                targetScale = if (activating) 1.12f else 0.82f,
                                animationSpec = floatingToolbarSpatialFloatSpec(),
                            ) +
                            slideOutHorizontally(
                                animationSpec = floatingToolbarSpatialOffsetSpec(),
                                targetOffsetX = { width -> if (activating) -width / 3 else width / 3 },
                            )
                        ) using SizeTransform(clip = false)
            },
            label = "FloatingToolbarItemIcon",
        ) { isActive ->
            Icon(
                modifier = Modifier.scale(iconScale),
                painter = painterResource(if (isActive) screen.iconIdActive else screen.iconIdInactive),
                contentDescription = stringResource(screen.titleId),
                tint = contentColor,
            )
        }

        AnimatedVisibility(
            visible = showLabel,
            enter =
                fadeIn(animationSpec = floatingToolbarEffectsFloatSpec()) +
                    expandHorizontally(
                        animationSpec = floatingToolbarSpatialSizeSpec(),
                        expandFrom = Alignment.Start,
                    ),
            exit =
                fadeOut(animationSpec = floatingToolbarEffectsFloatSpec()) +
                    shrinkHorizontally(
                        animationSpec = floatingToolbarSpatialSizeSpec(),
                        shrinkTowards = Alignment.Start,
                    ),
            label = "FloatingToolbarItemLabel",
        ) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
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

private fun floatingToolbarEffectsColorSpec(): FiniteAnimationSpec<Color> =
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

private fun floatingToolbarEffectsFloatSpec(): FiniteAnimationSpec<Float> =
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

private fun floatingToolbarSpatialFloatSpec(): FiniteAnimationSpec<Float> =
    spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium,
    )

private fun floatingToolbarSpatialOffsetSpec(): FiniteAnimationSpec<IntOffset> =
    spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium,
    )

private fun floatingToolbarSpatialSizeSpec(): FiniteAnimationSpec<IntSize> =
    spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium,
    )
