@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex

private val colorAnimSpec = spring<Color>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow,
)

@Composable
fun BoxScope.LibraryMeshGradient(
    colors: List<Color>,
    alpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    if (colors.isEmpty() || alpha <= 0f) return

    val surfaceColor = MaterialTheme.colorScheme.surface

    val c0 by animateColorAsState(colors[0], colorAnimSpec, label = "mesh0")
    val c1 by animateColorAsState(colors.getOrElse(1) { colors[0] }, colorAnimSpec, label = "mesh1")
    val c2 by animateColorAsState(colors.getOrElse(2) { colors[0] }, colorAnimSpec, label = "mesh2")
    val c3 by animateColorAsState(colors.getOrElse(3) { colors[0] }, colorAnimSpec, label = "mesh3")
    val c4 by animateColorAsState(colors.getOrElse(4) { colors.getOrElse(1) { colors[0] } }, colorAnimSpec, label = "mesh4")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxSize(0.7f)
            .align(Alignment.TopCenter)
            .zIndex(-1f)
            .drawBehind {
                val w = size.width
                val h = size.height

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c0.copy(alpha = alpha * 0.36f),
                            c0.copy(alpha = alpha * 0.22f),
                            c0.copy(alpha = alpha * 0.12f),
                            c0.copy(alpha = alpha * 0.05f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.15f, h * 0.1f),
                        radius = w * 0.55f,
                    ),
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c1.copy(alpha = alpha * 0.32f),
                            c1.copy(alpha = alpha * 0.19f),
                            c1.copy(alpha = alpha * 0.1f),
                            c1.copy(alpha = alpha * 0.045f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.85f, h * 0.2f),
                        radius = w * 0.65f,
                    ),
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c2.copy(alpha = alpha * 0.28f),
                            c2.copy(alpha = alpha * 0.16f),
                            c2.copy(alpha = alpha * 0.085f),
                            c2.copy(alpha = alpha * 0.038f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.3f, h * 0.45f),
                        radius = w * 0.6f,
                    ),
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c3.copy(alpha = alpha * 0.24f),
                            c3.copy(alpha = alpha * 0.13f),
                            c3.copy(alpha = alpha * 0.075f),
                            c3.copy(alpha = alpha * 0.03f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.7f, h * 0.5f),
                        radius = w * 0.7f,
                    ),
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c4.copy(alpha = alpha * 0.2f),
                            c4.copy(alpha = alpha * 0.11f),
                            c4.copy(alpha = alpha * 0.06f),
                            c4.copy(alpha = alpha * 0.022f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.5f, h * 0.75f),
                        radius = w * 0.8f,
                    ),
                )

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            surfaceColor.copy(alpha = alpha * 0.22f),
                            surfaceColor.copy(alpha = alpha * 0.55f),
                            surfaceColor,
                        ),
                        startY = h * 0.4f,
                        endY = h,
                    ),
                )
            },
    )
}
