/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

object BackdropDefaults {
    val MaxBlurRadius: Dp = 25.dp
    const val MaxAmount = 100

    fun blurRadius(amount: Int): Dp {
        if (amount <= 0) return 0.dp
        return MaxBlurRadius * (amount.toFloat() / MaxAmount)
    }
}

@Composable
fun AlbumBackdrop(
    imageUrl: String?,
    blurAmount: Int,
    enabled: Boolean,
    surfaceColor: Color,
    gradientAlpha: Float,
    modifier: Modifier = Modifier,
) {
    if (!enabled || imageUrl.isNullOrBlank() || gradientAlpha <= 0f) return

    val radius = BackdropDefaults.blurRadius(blurAmount)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxSize(0.55f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (radius > 0.dp) Modifier.blur(radius = radius) else Modifier),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                surfaceColor.copy(alpha = gradientAlpha * 0.22f),
                                surfaceColor.copy(alpha = gradientAlpha * 0.55f),
                                surfaceColor,
                            ),
                            startY = size.height * 0.4f,
                            endY = size.height,
                        ),
                    )
                }
        )
    }
}
