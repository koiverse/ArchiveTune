package moe.koiverse.archivetune.ui.component

import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import moe.koiverse.archivetune.R

data class LyricsGlassStyle(
    val nameRes: Int,
    val surfaceTint: Color,
    val surfaceAlpha: Float,
    val cloudyRadius: Int,
    val refraction: Float,
    val curve: Float,
    val dispersion: Float,
    val glassSaturation: Float,
    val glassContrast: Float,
    val glassEdge: Float,
    val glassCornerRadius: Float,
    val glassTint: Color,
    val textColor: Color,
    val secondaryTextColor: Color,
    val overlayColor: Color,
    val overlayAlpha: Float,
    val isDark: Boolean,
    val backgroundDimAlpha: Float = 0.3f,
) {
    companion object {
        val FrostedDark = LyricsGlassStyle(
            nameRes = R.string.glass_style_frosted_dark,
            surfaceTint = Color.Black,
            surfaceAlpha = 0.35f,
            cloudyRadius = 15,
            refraction = 0.20f,
            curve = 0.20f,
            dispersion = 0.0f,
            glassSaturation = 1.10f,
            glassContrast = 1.0f,
            glassEdge = 0.2f,
            glassCornerRadius = 40f,
            glassTint = Color.Black.copy(alpha = 0.35f),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.7f),
            overlayColor = Color.Black,
            overlayAlpha = 0.25f,
            isDark = true,
            backgroundDimAlpha = 0.35f,
        )

        val FrostedLight = LyricsGlassStyle(
            nameRes = R.string.glass_style_frosted_light,
            surfaceTint = Color.White,
            surfaceAlpha = 0.45f,
            cloudyRadius = 15,
            refraction = 0.18f,
            curve = 0.18f,
            dispersion = 0.0f,
            glassSaturation = 1.05f,
            glassContrast = 1.0f,
            glassEdge = 0.25f,
            glassCornerRadius = 40f,
            glassTint = Color.White.copy(alpha = 0.15f),
            textColor = Color(0xFF1A1A1A),
            secondaryTextColor = Color(0xFF1A1A1A).copy(alpha = 0.65f),
            overlayColor = Color.White,
            overlayAlpha = 0.35f,
            isDark = false,
            backgroundDimAlpha = 0.15f,
        )

        val ClearGlass = LyricsGlassStyle(
            nameRes = R.string.glass_style_clear_glass,
            surfaceTint = Color.White,
            surfaceAlpha = 0.15f,
            cloudyRadius = 12,
            refraction = 0.25f,
            curve = 0.25f,
            dispersion = 0.0f,
            glassSaturation = 1.15f,
            glassContrast = 1.05f,
            glassEdge = 0.2f,
            glassCornerRadius = 40f,
            glassTint = Color.White.copy(alpha = 0.08f),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.75f),
            overlayColor = Color.White,
            overlayAlpha = 0.08f,
            isDark = true,
            backgroundDimAlpha = 0.2f,
        )

        val DeepBlur = LyricsGlassStyle(
            nameRes = R.string.glass_style_deep_blur,
            surfaceTint = Color(0xFF0A0A14),
            surfaceAlpha = 0.55f,
            cloudyRadius = 25,
            refraction = 0.15f,
            curve = 0.15f,
            dispersion = 0.0f,
            glassSaturation = 0.95f,
            glassContrast = 1.0f,
            glassEdge = 0.15f,
            glassCornerRadius = 40f,
            glassTint = Color(0xFF0A0A14).copy(alpha = 0.50f),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.6f),
            overlayColor = Color(0xFF0A0A14),
            overlayAlpha = 0.4f,
            isDark = true,
            backgroundDimAlpha = 0.5f,
        )

        val VividGlow = LyricsGlassStyle(
            nameRes = R.string.glass_style_vivid_glow,
            surfaceTint = Color(0xFFFF6B9D),
            surfaceAlpha = 0.2f,
            cloudyRadius = 18,
            refraction = 0.22f,
            curve = 0.22f,
            dispersion = 0.02f,
            glassSaturation = 1.20f,
            glassContrast = 1.05f,
            glassEdge = 0.2f,
            glassCornerRadius = 40f,
            glassTint = Color(0xFFFF6B9D).copy(alpha = 0.12f),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.8f),
            overlayColor = Color(0xFFFF6B9D),
            overlayAlpha = 0.12f,
            isDark = true,
            backgroundDimAlpha = 0.25f,
        )

        val allPresets = listOf(FrostedDark, FrostedLight, ClearGlass, DeepBlur, VividGlow)

        fun fromPalette(palette: Palette): LyricsGlassStyle {
            val vibrantSwatch = palette.vibrantSwatch
                ?: palette.lightVibrantSwatch
                ?: palette.darkVibrantSwatch
                ?: palette.mutedSwatch

            val dominantSwatch = palette.dominantSwatch

            val tintColor = vibrantSwatch?.let { Color(it.rgb) } ?: Color(0xFF6366F1)
            val bgDominant = dominantSwatch?.let { Color(it.rgb) } ?: Color.Black

            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(bgDominant.hashCode(), hsv)
            val isDarkBackground = hsv[2] < 0.5f

            return LyricsGlassStyle(
                nameRes = R.string.glass_style_album_tint,
                surfaceTint = tintColor.copy(alpha = 0.6f),
                surfaceAlpha = if (isDarkBackground) 0.25f else 0.3f,
                cloudyRadius = 15,
                refraction = 0.20f,
                curve = 0.20f,
                dispersion = 0.0f,
                glassSaturation = 1.10f,
                glassContrast = 1.0f,
                glassEdge = 0.2f,
                glassCornerRadius = 40f,
                glassTint = tintColor.copy(alpha = 0.15f),
                textColor = if (isDarkBackground) Color.White else Color(0xFF1A1A1A),
                secondaryTextColor = if (isDarkBackground) Color.White.copy(alpha = 0.7f) else Color(0xFF1A1A1A).copy(alpha = 0.65f),
                overlayColor = tintColor.copy(alpha = 0.3f),
                overlayAlpha = 0.15f,
                isDark = isDarkBackground,
                backgroundDimAlpha = if (isDarkBackground) 0.3f else 0.15f,
            )
        }
    }
}
