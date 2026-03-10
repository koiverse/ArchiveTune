package moe.koiverse.archivetune.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class GlassSurfaceStyle(
    val surfaceTint: Color,
    val surfaceAlpha: Float,
    val overlayColor: Color,
    val overlayAlpha: Float,
    val blurRadius: Dp,
    val useVibrancy: Boolean,
    val useLens: Boolean,
    val lensHeight: Dp,
    val lensAmount: Dp,
    val borderColor: Color,
    val borderAlpha: Float,
    val backgroundDimAlpha: Float,
    val backgroundDimColor: Color,
    val shadowElevation: Dp,
    val shadowColor: Color,
    val topHighlightAlpha: Float,
)

object GlassEffectDefaults {

    val NavigationBarDark = GlassSurfaceStyle(
        surfaceTint = Color(0xFF0D0D1A),
        surfaceAlpha = 0.22f,
        overlayColor = Color(0xFF1A1A2E),
        overlayAlpha = 0.12f,
        blurRadius = 60.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 24.dp,
        lensAmount = 48.dp,
        borderColor = Color.White,
        borderAlpha = 0.22f,
        backgroundDimAlpha = 0.08f,
        backgroundDimColor = Color.Black,
        shadowElevation = 12.dp,
        shadowColor = Color.Black.copy(alpha = 0.35f),
        topHighlightAlpha = 0.10f,
    )

    val NavigationBarLight = GlassSurfaceStyle(
        surfaceTint = Color(0xFFF5F5FA),
        surfaceAlpha = 0.38f,
        overlayColor = Color.White,
        overlayAlpha = 0.22f,
        blurRadius = 64.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 24.dp,
        lensAmount = 48.dp,
        borderColor = Color.White,
        borderAlpha = 0.45f,
        backgroundDimAlpha = 0.04f,
        backgroundDimColor = Color.Black,
        shadowElevation = 8.dp,
        shadowColor = Color.Black.copy(alpha = 0.12f),
        topHighlightAlpha = 0.18f,
    )

    val NavigationBarPureBlack = GlassSurfaceStyle(
        surfaceTint = Color(0xFF050508),
        surfaceAlpha = 0.48f,
        overlayColor = Color(0xFF0A0A14),
        overlayAlpha = 0.20f,
        blurRadius = 52.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 18.dp,
        lensAmount = 36.dp,
        borderColor = Color.White,
        borderAlpha = 0.08f,
        backgroundDimAlpha = 0.25f,
        backgroundDimColor = Color.Black,
        shadowElevation = 10.dp,
        shadowColor = Color.Black.copy(alpha = 0.50f),
        topHighlightAlpha = 0.05f,
    )

    val MiniPlayerDark = GlassSurfaceStyle(
        surfaceTint = Color(0xFF0D0D1A),
        surfaceAlpha = 0.20f,
        overlayColor = Color(0xFF1A1A2E),
        overlayAlpha = 0.10f,
        blurRadius = 56.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 24.dp,
        lensAmount = 48.dp,
        borderColor = Color.White,
        borderAlpha = 0.24f,
        backgroundDimAlpha = 0.06f,
        backgroundDimColor = Color.Black,
        shadowElevation = 14.dp,
        shadowColor = Color.Black.copy(alpha = 0.40f),
        topHighlightAlpha = 0.12f,
    )

    val MiniPlayerLight = GlassSurfaceStyle(
        surfaceTint = Color(0xFFF5F5FA),
        surfaceAlpha = 0.35f,
        overlayColor = Color.White,
        overlayAlpha = 0.20f,
        blurRadius = 60.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 24.dp,
        lensAmount = 48.dp,
        borderColor = Color.White,
        borderAlpha = 0.40f,
        backgroundDimAlpha = 0.03f,
        backgroundDimColor = Color.Black,
        shadowElevation = 10.dp,
        shadowColor = Color.Black.copy(alpha = 0.10f),
        topHighlightAlpha = 0.20f,
    )

    val MiniPlayerPureBlack = GlassSurfaceStyle(
        surfaceTint = Color(0xFF050508),
        surfaceAlpha = 0.45f,
        overlayColor = Color(0xFF0A0A14),
        overlayAlpha = 0.18f,
        blurRadius = 48.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 18.dp,
        lensAmount = 36.dp,
        borderColor = Color.White,
        borderAlpha = 0.08f,
        backgroundDimAlpha = 0.20f,
        backgroundDimColor = Color.Black,
        shadowElevation = 12.dp,
        shadowColor = Color.Black.copy(alpha = 0.55f),
        topHighlightAlpha = 0.04f,
    )

    fun navigationBarStyle(isDark: Boolean, isPureBlack: Boolean): GlassSurfaceStyle {
        return when {
            isPureBlack -> NavigationBarPureBlack
            isDark -> NavigationBarDark
            else -> NavigationBarLight
        }
    }

    fun miniPlayerStyle(isDark: Boolean, isPureBlack: Boolean): GlassSurfaceStyle {
        return when {
            isPureBlack -> MiniPlayerPureBlack
            isDark -> MiniPlayerDark
            else -> MiniPlayerLight
        }
    }
}
