/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * 
 * Simple Audio Visualizer by @cenzer0
 */

package moe.koiverse.archivetune.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Simple Audio Visualizer
 * 
 * Lightweight visualizer with wave animation
 * 
 * @author @cenzer0
 */

@Composable
fun SimpleAudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    
    Canvas(modifier = modifier.fillMaxWidth().height(60.dp)) {
        val amplitude = if (isPlaying) size.height * 0.3f else size.height * 0.05f
        val frequency = 0.02f
        val centerY = size.height / 2
        
        // Draw 3 waves with different phases
        for (waveIndex in 0..2) {
            val wavePhase = phase + (waveIndex * 120f)
            val alpha = 1f - (waveIndex * 0.3f)
            
            for (x in 0..size.width.toInt() step 10) {
                val y1 = centerY + amplitude * sin((x * frequency + wavePhase) * Math.PI / 180).toFloat()
                val x2 = (x + 10).coerceAtMost(size.width.toInt()).toFloat()
                val y2 = centerY + amplitude * sin((x2 * frequency + wavePhase) * Math.PI / 180).toFloat()
                
                drawLine(
                    color = color.copy(alpha = alpha),
                    start = Offset(x.toFloat(), y1),
                    end = Offset(x2, y2),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
