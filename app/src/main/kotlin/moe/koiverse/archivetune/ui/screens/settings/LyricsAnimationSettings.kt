/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.LyricsV2BounceFactorKey
import moe.koiverse.archivetune.constants.LyricsV2FillTransitionWidthKey
import moe.koiverse.archivetune.constants.LyricsV2GlowFactorKey
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.PreferenceGroupTitle
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsAnimationSettings(
    navController: NavController,
) {
    val (bounceFactor, onBounceFactorChange) = rememberPreference(LyricsV2BounceFactorKey, defaultValue = 1f)
    val (glowFactor, onGlowFactorChange) = rememberPreference(LyricsV2GlowFactorKey, defaultValue = 1f)
    val (fillTransitionWidth, onFillTransitionWidthChange) = rememberPreference(LyricsV2FillTransitionWidthKey, defaultValue = 8f)

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(text = stringResource(R.string.lyrics_animation_style)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    )
                )
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
}
