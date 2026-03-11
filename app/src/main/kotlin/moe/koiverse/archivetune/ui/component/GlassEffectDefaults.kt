package moe.koiverse.archivetune.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class GlassSurfaceStyle(
    val surfaceTint: Color,
    val surfaceAlpha: Float,
    val overlayColor: Color,
    val overlayAlpha: Float,
    val cloudyRadius: Int,
    val refraction: Float,
    val curve: Float,
    val dispersion: Float,
    val glassSaturation: Float,
    val glassContrast: Float,
    val glassEdge: Float,
    val glassCornerRadius: Float,
    val glassTint: Color,
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
        surfaceAlpha = 0.32f,
        overlayColor = Color(0xFF1A1A2E),
        overlayAlpha = 0.18f,
        cloudyRadius = 20,
        refraction = 0.25f,
        curve = 0.25f,
        dispersion = 0.0f,
        glassSaturation = 1.15f,
        glassContrast = 1.05f,
        glassEdge = 0.2f,
        glassCornerRadius = 50f,
        glassTint = Color(0xFF0D0D1A).copy(alpha = 0.22f),
        borderColor = Color.White,
        borderAlpha = 0.22f,
        backgroundDimAlpha = 0.22f,
        backgroundDimColor = Color.Black,
        shadowElevation = 12.dp,
        shadowColor = Color.Black.copy(alpha = 0.35f),
        topHighlightAlpha = 0.10f,
    )

    val NavigationBarLight = GlassSurfaceStyle(
        surfaceTint = Color(0xFFF5F5FA),
        surfaceAlpha = 0.42f,
        overlayColor = Color.White,
        overlayAlpha = 0.28f,
        cloudyRadius = 22,
        refraction = 0.20f,
        curve = 0.20f,
        dispersion = 0.0f,
        glassSaturation = 1.10f,
        glassContrast = 1.02f,
        glassEdge = 0.25f,
        glassCornerRadius = 50f,
        glassTint = Color(0xFFF5F5FA).copy(alpha = 0.10f),
        borderColor = Color.White,
        borderAlpha = 0.45f,
        backgroundDimAlpha = 0.10f,
        backgroundDimColor = Color.Black,
        shadowElevation = 8.dp,
        shadowColor = Color.Black.copy(alpha = 0.12f),
        topHighlightAlpha = 0.18f,
    )

    val NavigationBarPureBlack = GlassSurfaceStyle(
        surfaceTint = Color(0xFF050508),
        surfaceAlpha = 0.55f,
        overlayColor = Color(0xFF0A0A14),
        overlayAlpha = 0.30f,
        cloudyRadius = 18,
        refraction = 0.20f,
        curve = 0.20f,
        dispersion = 0.0f,
        glassSaturation = 1.05f,
        glassContrast = 1.0f,
        glassEdge = 0.15f,
        glassCornerRadius = 50f,
        glassTint = Color(0xFF050508).copy(alpha = 0.38f),
        borderColor = Color.White,
        borderAlpha = 0.08f,
        backgroundDimAlpha = 0.38f,
        backgroundDimColor = Color.Black,
        shadowElevation = 10.dp,
        shadowColor = Color.Black.copy(alpha = 0.50f),
        topHighlightAlpha = 0.05f,
    )

    val MiniPlayerDark = GlassSurfaceStyle(
        surfaceTint = Color(0xFF0D0D1A),
        surfaceAlpha = 0.30f,
        overlayColor = Color(0xFF1A1A2E),
        overlayAlpha = 0.16f,
        cloudyRadius = 19,
        refraction = 0.25f,
        curve = 0.25f,
        dispersion = 0.0f,
        glassSaturation = 1.15f,
        glassContrast = 1.05f,
        glassEdge = 0.2f,
        glassCornerRadius = 56f,
        glassTint = Color(0xFF0D0D1A).copy(alpha = 0.20f),
        borderColor = Color.White,
        borderAlpha = 0.24f,
        backgroundDimAlpha = 0.20f,
        backgroundDimColor = Color.Black,
        shadowElevation = 14.dp,
        shadowColor = Color.Black.copy(alpha = 0.40f),
        topHighlightAlpha = 0.12f,
    )

    val MiniPlayerLight = GlassSurfaceStyle(
        surfaceTint = Color(0xFFF5F5FA),
        surfaceAlpha = 0.40f,
        overlayColor = Color.White,
        overlayAlpha = 0.26f,
        cloudyRadius = 20,
        refraction = 0.20f,
        curve = 0.20f,
        dispersion = 0.0f,
        glassSaturation = 1.10f,
        glassContrast = 1.02f,
        glassEdge = 0.25f,
        glassCornerRadius = 56f,
        glassTint = Color(0xFFF5F5FA).copy(alpha = 0.08f),
        borderColor = Color.White,
        borderAlpha = 0.40f,
        backgroundDimAlpha = 0.08f,
        backgroundDimColor = Color.Black,
        shadowElevation = 10.dp,
        shadowColor = Color.Black.copy(alpha = 0.10f),
        topHighlightAlpha = 0.20f,
    )

    val MiniPlayerPureBlack = GlassSurfaceStyle(
        surfaceTint = Color(0xFF050508),
        surfaceAlpha = 0.52f,
        overlayColor = Color(0xFF0A0A14),
        overlayAlpha = 0.28f,
        cloudyRadius = 16,
        refraction = 0.20f,
        curve = 0.20f,
        dispersion = 0.0f,
        glassSaturation = 1.05f,
        glassContrast = 1.0f,
        glassEdge = 0.15f,
        glassCornerRadius = 56f,
        glassTint = Color(0xFF050508).copy(alpha = 0.35f),
        borderColor = Color.White,
        borderAlpha = 0.08f,
        backgroundDimAlpha = 0.35f,
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
