package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.LyricsV2BounceFactorKey
import moe.koiverse.archivetune.constants.LyricsV2FillTransitionWidthKey
import moe.koiverse.archivetune.constants.LyricsV2GlowFactorKey
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.PreferenceGroupTitle
import moe.koiverse.archivetune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsAnimationSettings(
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (bounceFactor, onBounceFactorChange) = rememberPreference(LyricsV2BounceFactorKey, defaultValue = 1f)
    val (glowFactor, onGlowFactorChange) = rememberPreference(LyricsV2GlowFactorKey, defaultValue = 1f)
    val (fillTransitionWidth, onFillTransitionWidthChange) = rememberPreference(LyricsV2FillTransitionWidthKey, defaultValue = 8f)

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        PreferenceGroupTitle(
            title = "Animation Tuning",
        )

        PreferenceEntry(
            title = { Text("Bounce Amplitude") },
            description = "Adjust the bounce effect when a word is sung (${(bounceFactor * 100).toInt()}%)",
            icon = { Icon(painterResource(R.drawable.animation), null) },
            content = {
                Slider(
                    value = bounceFactor,
                    onValueChange = onBounceFactorChange,
                    valueRange = 0f..2f,
                )
            }
        )

        PreferenceEntry(
            title = { Text("Glow Intensity") },
            description = "Adjust the glow brightness of the sung word (${(glowFactor * 100).toInt()}%)",
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            content = {
                Slider(
                    value = glowFactor,
                    onValueChange = onGlowFactorChange,
                    valueRange = 0f..2f,
                )
            }
        )

        PreferenceEntry(
            title = { Text("Fill Transition Smoothness") },
            description = "Adjust the gradient edge width of the liquid fill effect (${fillTransitionWidth.toInt()} dp)",
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            content = {
                Slider(
                    value = fillTransitionWidth,
                    onValueChange = onFillTransitionWidthChange,
                    valueRange = 2f..24f,
                )
            }
        )
    }
}
