package moe.koiverse.archivetune.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun WavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors,
    isPlaying: Boolean = false,
) {
    var isDragging by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "waveAnimation")
    
    // Animate wave phase for smooth wave motion
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    
    // Animate wave amplitude based on playing state
    val waveAmplitude by animateFloatAsState(
        targetValue = if (isPlaying && !isDragging) 8f else 0f,
        animationSpec = tween(300),
        label = "waveAmplitude"
    )
    
    val coercedValue = value.coerceIn(valueRange)
    val fraction = (coercedValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(enabled, valueRange) {
                detectTapGestures(
                    onPress = { offset ->
                        if (enabled) {
                            val newValue = valueRange.start + (offset.x / size.width) * (valueRange.endInclusive - valueRange.start)
                            onValueChange(newValue.coerceIn(valueRange))
                        }
                    }
                )
            }
            .pointerInput(enabled, valueRange) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (enabled) {
                            val delta = dragAmount / size.width * (valueRange.endInclusive - valueRange.start)
                            val newValue = (coercedValue + delta).coerceIn(valueRange)
                            onValueChange(newValue)
                        }
                    }
                )
            }
    ) {
        val trackHeight = 4.dp.toPx()
        val centerY = size.height / 2f
        val trackWidth = size.width
        val progressWidth = trackWidth * fraction
        
        // Draw inactive track (with subtle wave)
        drawWavyLine(
            startX = progressWidth,
            endX = trackWidth,
            centerY = centerY,
            amplitude = if (isPlaying && !isDragging) 2f else 0f,
            frequency = 0.02f,
            phase = wavePhase,
            color = colors.inactiveTrackColor,
            strokeWidth = trackHeight
        )
        
        // Draw active track (with animated wave)
        if (progressWidth > 0) {
            drawWavyLine(
                startX = 0f,
                endX = progressWidth,
                centerY = centerY,
                amplitude = waveAmplitude,
                frequency = 0.015f,
                phase = wavePhase,
                color = colors.activeTrackColor,
                strokeWidth = trackHeight
            )
        }
        
        // Draw thumb
        drawCircle(
            color = colors.thumbColor,
            radius = 10.dp.toPx(),
            center = Offset(progressWidth, centerY)
        )
        
        // Draw thumb glow when playing
        if (isPlaying && !isDragging) {
            drawCircle(
                color = colors.thumbColor.copy(alpha = 0.3f),
                radius = 14.dp.toPx(),
                center = Offset(progressWidth, centerY)
            )
        }
    }
}

private fun DrawScope.drawWavyLine(
    startX: Float,
    endX: Float,
    centerY: Float,
    amplitude: Float,
    frequency: Float,
    phase: Float,
    color: Color,
    strokeWidth: Float
) {
    if (startX >= endX) return
    
    val path = Path().apply {
        val segments = 100
        val segmentWidth = (endX - startX) / segments
        
        for (i in 0..segments) {
            val x = startX + i * segmentWidth
            val waveY = centerY + amplitude * sin(x * frequency + phase)
            
            if (i == 0) {
                moveTo(x, waveY)
            } else {
                lineTo(x, waveY)
            }
        }
    }
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}
