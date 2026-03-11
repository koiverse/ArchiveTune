package moe.koiverse.archivetune.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.liquidGlass
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.constants.MiniPlayerHeight
import moe.koiverse.archivetune.constants.SwipeSensitivityKey
import moe.koiverse.archivetune.ui.component.GlassEffectDefaults
import moe.koiverse.archivetune.utils.rememberPreference
import kotlin.math.roundToInt

@Composable
fun GlassMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(moe.koiverse.archivetune.constants.SwipeThumbnailKey, true)

    val isDark = pureBlack || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glassStyle = GlassEffectDefaults.miniPlayerStyle(isDark, pureBlack)
    val pillShape = RoundedCornerShape(32.dp)

    var componentSize by remember { mutableStateOf(Size.Zero) }
    val lensCenter = remember(componentSize) {
        Offset(componentSize.width / 2f, componentSize.height / 2f)
    }
    val lensSize = remember(componentSize) {
        Size(componentSize.width, componentSize.height)
    }

    SwipeableMiniPlayerBox(
        modifier = modifier,
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false
    ) { offsetX ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .onSizeChanged { size ->
                    componentSize = Size(size.width.toFloat(), size.height.toFloat())
                }
                .graphicsLayer {
                    shadowElevation = glassStyle.shadowElevation.toPx()
                    shape = pillShape
                    clip = false
                    ambientShadowColor = glassStyle.shadowColor
                    spotShadowColor = glassStyle.shadowColor
                }
                .clip(pillShape)
                .cloudy(radius = glassStyle.cloudyRadius)
                .liquidGlass(
                    lensCenter = lensCenter,
                    lensSize = lensSize,
                    cornerRadius = glassStyle.glassCornerRadius,
                    refraction = glassStyle.refraction,
                    curve = glassStyle.curve,
                    dispersion = glassStyle.dispersion,
                    saturation = glassStyle.glassSaturation,
                    contrast = glassStyle.glassContrast,
                    tint = glassStyle.glassTint,
                    edge = glassStyle.glassEdge,
                )
                .drawWithContent {
                    drawContent()
                    drawRect(glassStyle.backgroundDimColor.copy(alpha = glassStyle.backgroundDimAlpha))
                    drawRect(glassStyle.surfaceTint.copy(alpha = glassStyle.surfaceAlpha))
                    drawRect(glassStyle.overlayColor.copy(alpha = glassStyle.overlayAlpha))
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = glassStyle.topHighlightAlpha),
                                Color.Transparent,
                            ),
                            startY = 0f,
                            endY = size.height * 0.45f,
                        ),
                        size = size,
                    )
                }
                .border(
                    width = 0.75.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            glassStyle.borderColor.copy(alpha = glassStyle.borderAlpha),
                            glassStyle.borderColor.copy(alpha = glassStyle.borderAlpha * 0.15f),
                        )
                    ),
                    shape = pillShape
                )
        ) {
            NewMiniPlayerContent(
                pureBlack = pureBlack,
                position = position,
                duration = duration,
                playerConnection = playerConnection
            )
        }
    }
}
