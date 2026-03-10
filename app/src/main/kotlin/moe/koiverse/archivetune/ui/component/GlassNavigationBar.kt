package moe.koiverse.archivetune.ui.component

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import moe.koiverse.archivetune.constants.NavigationBarHeight
import moe.koiverse.archivetune.constants.SlimNavBarHeight
import moe.koiverse.archivetune.ui.screens.Screens

@Composable
fun GlassNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    isRouteSelected: (Screens) -> Boolean,
    onItemClick: (Screens) -> Unit,
    slimNav: Boolean,
    pureBlack: Boolean,
    bottomInset: Dp,
    bottomPadding: Dp,
    slideOffset: Dp,
    modifier: Modifier = Modifier,
) {
    val isDark = pureBlack || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glassStyle = GlassEffectDefaults.navigationBarStyle(isDark, pureBlack)
    val navBarHeight = if (slimNav) SlimNavBarHeight else NavigationBarHeight
    val totalHeight = bottomInset + bottomPadding + navBarHeight
    val glassShape = RoundedCornerShape(28.dp)

    val supportsBackdrop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val supportsLens = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val backdrop = rememberCanvasBackdrop {
        drawRect(
            color = glassStyle.backgroundDimColor.copy(alpha = glassStyle.backgroundDimAlpha),
            size = size
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .offset { IntOffset(0, slideOffset.roundToPx()) }
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = bottomInset + bottomPadding)
                    .fillMaxWidth()
                    .height(navBarHeight)
                    .graphicsLayer {
                        shadowElevation = glassStyle.shadowElevation.toPx()
                        shape = glassShape
                        clip = false
                        ambientShadowColor = glassStyle.shadowColor
                        spotShadowColor = glassStyle.shadowColor
                    }
                    .clip(glassShape)
                    .then(
                        if (supportsBackdrop) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { glassShape },
                                effects = {
                                    if (glassStyle.useVibrancy) vibrancy()
                                    blur(with(density) { glassStyle.blurRadius.toPx() })
                                    if (supportsLens && glassStyle.useLens) {
                                        lens(
                                            with(density) { glassStyle.lensHeight.toPx() },
                                            with(density) { glassStyle.lensAmount.toPx() }
                                        )
                                    }
                                },
                                onDrawSurface = {
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
                            )
                        } else {
                            Modifier.background(
                                if (pureBlack) Color.Black
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                        }
                    )
                    .border(
                        width = 0.75.dp,
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        glassStyle.borderColor.copy(alpha = glassStyle.borderAlpha),
                                        glassStyle.borderColor.copy(alpha = glassStyle.borderAlpha * 0.15f),
                                    )
                            ),
                        shape = glassShape
                    )
        ) {
            GlassNavigationBarContent(
                navigationItems = navigationItems,
                isRouteSelected = isRouteSelected,
                onItemClick = onItemClick,
                slimNav = slimNav,
                pureBlack = pureBlack,
                isDark = isDark,
                modifier = Modifier.fillMaxWidth().height(navBarHeight),
            )
        }
    }
}

@Composable
private fun GlassNavigationBarContent(
    navigationItems: List<Screens>,
    isRouteSelected: (Screens) -> Boolean,
    onItemClick: (Screens) -> Unit,
    slimNav: Boolean,
    pureBlack: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationItems.fastForEach { screen ->
            val isSelected = isRouteSelected(screen)

            GlassNavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(screen) },
                isDark = isDark,
                icon = {
                    Icon(
                        painter = painterResource(
                            id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) {
                            if (pureBlack) Color.White
                            else MaterialTheme.colorScheme.primary
                        } else {
                            if (pureBlack) Color.White.copy(alpha = 0.5f)
                            else if (isDark) Color.White.copy(alpha = 0.6f)
                            else Color.Black.copy(alpha = 0.5f)
                        }
                    )
                },
                label = if (!slimNav) {
                    {
                        Text(
                            text = stringResource(screen.titleId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp,
                            color = if (isSelected) {
                                if (pureBlack) Color.White
                                else MaterialTheme.colorScheme.primary
                            } else {
                                if (pureBlack) Color.White.copy(alpha = 0.5f)
                                else if (isDark) Color.White.copy(alpha = 0.6f)
                                else Color.Black.copy(alpha = 0.5f)
                            }
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
private fun RowScope.GlassNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    icon: @Composable () -> Unit,
    label: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectedIndicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 0.12f else 0f,
        label = "indicatorAlpha"
    )

    Column(
        modifier = modifier
            .weight(1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val indicatorColor = if (isDark) Color.White else Color.Black

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .drawBehind {
                    if (selectedIndicatorAlpha > 0f) {
                        drawRoundRect(
                            color = indicatorColor.copy(alpha = selectedIndicatorAlpha),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        )
                    }
                }
                .padding(horizontal = 20.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }

        if (label != null) {
            Spacer(modifier = Modifier.height(2.dp))
            label()
        }
    }
}
