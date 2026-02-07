/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BigSeekBar(
    progressProvider: () -> Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.13f),
    color: Color = MaterialTheme.colorScheme.primary,
    steps: Int = 19,
) {
    Slider(
        value = progressProvider(),
        onValueChange = onProgressChange,
        valueRange = 0f..1f,
        steps = steps,
        colors = SliderDefaults.colors(
            activeTrackColor = color,
            activeTickColor = color,
            thumbColor = color,
            inactiveTrackColor = background
        ),
        thumb = { 
            Spacer(modifier = Modifier.size(0.dp)) 
        },
        track = { sliderState ->
            PlayerSliderTrack(
                sliderState = sliderState,
                colors = SliderDefaults.colors(
                    activeTrackColor = color,
                    activeTickColor = color,
                    inactiveTrackColor = background
                ),
                trackHeight = 10.dp
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    )
}
