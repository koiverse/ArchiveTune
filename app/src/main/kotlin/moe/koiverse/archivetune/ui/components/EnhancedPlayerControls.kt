/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * 
 * Enhanced Player Controls by @cenzer0
 */

package moe.koiverse.archivetune.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import moe.koiverse.archivetune.R

/**
 * Enhanced Player Controls with Gesture Support
 * 
 * Features:
 * - Swipe gestures for next/previous
 * - Long press for fast forward/rewind
 * - Haptic feedback
 * - Smooth animations
 * - Visual feedback for gestures
 * 
 * @author @cenzer0
 */

@Composable
fun EnhancedPlayerControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    modifier: Modifier = Modifier,
    enableGestures: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    var swipeOffset by remember { mutableStateOf(0f) }
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressDirection by remember { mutableStateOf<LongPressDirection?>(null) }
    
    val swipeThreshold = 100f
    val scale by animateFloatAsState(
        targetValue = if (isLongPressing) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )
    
    // Auto-repeat for long press
    LaunchedEffect(isLongPressing, longPressDirection) {
        if (isLongPressing && longPressDirection != null) {
            while (isLongPressing) {
                when (longPressDirection) {
                    LongPressDirection.FORWARD -> onSeekForward()
                    LongPressDirection.BACKWARD -> onSeekBackward()
                    null -> {}
                }
                delay(300)
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (enableGestures) {
                    Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (swipeOffset > swipeThreshold) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNext()
                                } else if (swipeOffset < -swipeThreshold) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPrevious()
                                }
                                swipeOffset = 0f
                            },
                            onDragCancel = { swipeOffset = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                swipeOffset += dragAmount
                                if (kotlin.math.abs(swipeOffset) > swipeThreshold / 2) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Swipe indicator
        AnimatedVisibility(
            visible = kotlin.math.abs(swipeOffset) > swipeThreshold / 2,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            SwipeIndicator(
                direction = if (swipeOffset > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT,
                progress = (kotlin.math.abs(swipeOffset) / swipeThreshold).coerceIn(0f, 1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button with long press
            ControlButton(
                icon = R.drawable.skip_previous,
                onClick = onPrevious,
                onLongPress = {
                    isLongPressing = true
                    longPressDirection = LongPressDirection.BACKWARD
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onRelease = {
                    isLongPressing = false
                    longPressDirection = null
                },
                size = 56.dp,
                isHighlighted = longPressDirection == LongPressDirection.BACKWARD
            )
            
            // Play/Pause button
            PlayPauseButton(
                isPlaying = isPlaying,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayPause()
                },
                size = 72.dp
            )
            
            // Next button with long press
            ControlButton(
                icon = R.drawable.skip_next,
                onClick = onNext,
                onLongPress = {
                    isLongPressing = true
                    longPressDirection = LongPressDirection.FORWARD
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onRelease = {
                    isLongPressing = false
                    longPressDirection = null
                },
                size = 56.dp,
                isHighlighted = longPressDirection == LongPressDirection.FORWARD
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: Int,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onRelease: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isHighlighted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() },
                    onPress = {
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(size * 0.6f),
            tint = if (isHighlighted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isPlaying) 0f else 180f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                if (isPlaying) R.drawable.pause else R.drawable.play
            ),
            contentDescription = null,
            modifier = Modifier.size(size * 0.5f),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun SwipeIndicator(
    direction: SwipeDirection,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = if (direction == SwipeDirection.RIGHT) {
            Arrangement.End
        } else {
            Arrangement.Start
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (direction == SwipeDirection.LEFT) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Text(
                    text = if (direction == SwipeDirection.RIGHT) "Next" else "Previous",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                
                if (direction == SwipeDirection.RIGHT) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

private enum class SwipeDirection {
    LEFT, RIGHT
}

private enum class LongPressDirection {
    FORWARD, BACKWARD
}
