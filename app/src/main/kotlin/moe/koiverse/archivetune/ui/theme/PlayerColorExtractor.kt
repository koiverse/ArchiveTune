package moe.koiverse.archivetune.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Player color extraction system for generating gradients from album artwork
 * 
 * This system analyzes album artwork and extracts vibrant, dominant colors
 * to create visually appealing gradients for the music player interface.
 */
object PlayerColorExtractor {

    /**
     * Extracts colors from a palette and creates a gradient
     * 
     * @param palette The color palette extracted from album artwork
     * @param fallbackColor Fallback color to use if extraction fails
     * @return List of colors for gradient (primary, darker variant, black)
     */
    suspend fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int
    ): List<Color> = withContext(Dispatchers.Default) {
        
        // Extract all available colors with priority for dominant colors
        val colorCandidates = listOfNotNull(
            palette.dominantSwatch, // High priority for dominant color
            palette.vibrantSwatch,
            palette.darkVibrantSwatch,
            palette.lightVibrantSwatch,
            palette.mutedSwatch,
            palette.darkMutedSwatch,
            palette.lightMutedSwatch
        )

        // Select best color based on weight (dominance + vibrancy)
        val bestSwatch = colorCandidates.maxByOrNull { calculateColorWeight(it) }
        val fallbackDominant = palette.dominantSwatch?.rgb?.let { Color(it) }
            ?: Color(palette.getDominantColor(fallbackColor))

        val primaryColor = if (bestSwatch != null) {
            val bestColor = Color(bestSwatch.rgb)
                var chosen = if (isColorVibrant(bestColor)) {
                    enhanceColorVividness(bestColor, 1.3f)
                } else {
                    enhanceColorVividness(fallbackDominant, 1.1f)
                }
                chosen = darkenIfTooBright(chosen)
                chosen
        } else {
            enhanceColorVividness(fallbackDominant, 1.1f)
        }
        val darkerFactor = Config.DARKER_VARIANT_FACTOR
        listOf(
            primaryColor,
            primaryColor.copy(
                red = (primaryColor.red * darkerFactor).coerceAtLeast(0f),
                green = (primaryColor.green * darkerFactor).coerceAtLeast(0f),
                blue = (primaryColor.blue * darkerFactor).coerceAtLeast(0f)
            ),
            Color.Black
        )
    }

    /**
     * Determines if a color is vibrant enough for use in player UI
     * 
     * @param color The color to analyze
     * @return true if the color has sufficient saturation and brightness
     */
    private fun isColorVibrant(color: Color): Boolean {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        val saturation = hsv[1] // HSV[1] is saturation
        val brightness = hsv[2] // HSV[2] is brightness
        return saturation > 0.25f && brightness > 0.2f && brightness < 0.82f
    }
    
    /**
     * Enhances color vividness by adjusting saturation and brightness
     * 
     * @param color The color to enhance
     * @param saturationFactor Factor to multiply saturation by (default 1.4)
     * @return Enhanced color with improved vividness
     */
    private fun enhanceColorVividness(color: Color, saturationFactor: Float = 1.4f): Color {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        
        // Increase saturation for more vivid colors
        hsv[1] = (hsv[1] * saturationFactor).coerceAtMost(1.0f)
        hsv[2] = (hsv[2] * Config.BRIGHTNESS_MULTIPLIER).coerceIn(0.35f, 0.75f)

        return Color(android.graphics.Color.HSVToColor(hsv))
    }
    private fun darkenIfTooBright(color: Color, maxAllowedBrightness: Float = 0.78f): Color {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        if (hsv[2] > maxAllowedBrightness) {
            hsv[2] = maxAllowedBrightness
            return Color(android.graphics.Color.HSVToColor(hsv))
        }
        return color
    }

    /**
     * Calculates weight for color selection based on dominance and vibrancy
     * 
     * @param swatch The palette swatch to analyze
     * @return Weight value for color selection priority
     */
    private fun calculateColorWeight(swatch: Palette.Swatch?): Float {
        if (swatch == null) return 0f
        val population = swatch.population.toFloat()
        val color = Color(swatch.rgb)
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        val saturation = hsv[1]
        val brightness = hsv[2]
        
        // Give higher priority to dominance (population) while considering vibrancy
        val populationWeight = population * 2f // Double dominance weight
        val vibrancyBonus = if (saturation > 0.3f && brightness > 0.3f) 1.5f else 1f
        
        return populationWeight * vibrancyBonus * (saturation + brightness) / 2f
    }

    /**
     * Configuration constants for color extraction
     */
    object Config {
        const val MAX_COLOR_COUNT = 32
        const val BITMAP_AREA = 8000
        const val IMAGE_SIZE = 200
        
        // Color enhancement factors
        const val VIBRANT_SATURATION_THRESHOLD = 0.25f
        const val VIBRANT_BRIGHTNESS_MIN = 0.2f
        const val VIBRANT_BRIGHTNESS_MAX = 0.9f
        
        const val POPULATION_WEIGHT_MULTIPLIER = 2f
        const val VIBRANCY_THRESHOLD_SATURATION = 0.3f
        const val VIBRANCY_THRESHOLD_BRIGHTNESS = 0.3f
        const val VIBRANCY_BONUS = 1.5f
        
        const val DEFAULT_SATURATION_FACTOR = 1.4f
        const val VIBRANT_SATURATION_FACTOR = 1.3f
        const val FALLBACK_SATURATION_FACTOR = 1.1f
        
        const val BRIGHTNESS_MULTIPLIER = 0.85f
        const val BRIGHTNESS_MIN = 0.35f
        const val BRIGHTNESS_MAX = 0.75f
        
        const val DARKER_VARIANT_FACTOR = 0.5f
    }
}