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
        
        // Extract multiple distinct colors from the palette
        val vibrantColor = palette.vibrantSwatch?.rgb?.let { Color(it) }
        val darkVibrantColor = palette.darkVibrantSwatch?.rgb?.let { Color(it) }
        val lightVibrantColor = palette.lightVibrantSwatch?.rgb?.let { Color(it) }
        val dominantColor = palette.dominantSwatch?.rgb?.let { Color(it) }
        val mutedColor = palette.mutedSwatch?.rgb?.let { Color(it) }
        val darkMutedColor = palette.darkMutedSwatch?.rgb?.let { Color(it) }
        
        // Build list of available distinct colors
        val availableColors = mutableListOf<Color>()
        
        // Add vibrant colors first (more colorful)
        vibrantColor?.let { availableColors.add(enhanceColorVividness(it, 1.3f)) }
        lightVibrantColor?.let { 
            if (!isSimilarColor(it, vibrantColor)) {
                availableColors.add(enhanceColorVividness(it, 1.2f))
            }
        }
        darkVibrantColor?.let { 
            if (!isSimilarColor(it, vibrantColor) && !isSimilarColor(it, lightVibrantColor)) {
                availableColors.add(enhanceColorVividness(it, 1.2f))
            }
        }
        
        // Add muted/dominant colors if we need more variety
        dominantColor?.let { 
            if (availableColors.size < 3 && !isSimilarToAny(it, availableColors)) {
                availableColors.add(enhanceColorVividness(it, 1.1f))
            }
        }
        mutedColor?.let { 
            if (availableColors.size < 3 && !isSimilarToAny(it, availableColors)) {
                availableColors.add(enhanceColorVividness(it, 1.0f))
            }
        }
        darkMutedColor?.let { 
            if (availableColors.size < 3 && !isSimilarToAny(it, availableColors)) {
                availableColors.add(enhanceColorVividness(it, 0.9f))
            }
        }
        
        // Return 3 distinct colors, or create darkened versions if not enough
        when {
            availableColors.size >= 3 -> {
                listOf(availableColors[0], availableColors[1], availableColors[2])
            }
            availableColors.size == 2 -> {
                listOf(
                    availableColors[0],
                    availableColors[1],
                    darkenColor(availableColors[1], 0.5f)
                )
            }
            availableColors.size == 1 -> {
                val base = availableColors[0]
                listOf(
                    base,
                    darkenColor(base, 0.7f),
                    darkenColor(base, 0.4f)
                )
            }
            else -> {
                // Fallback: use fallback color
                val base = Color(fallbackColor)
                listOf(
                    base,
                    darkenColor(base, 0.7f),
                    darkenColor(base, 0.4f)
                )
            }
        }
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
        
        // Color is vibrant if it has sufficient saturation and appropriate brightness
        // Avoid colors that are too dark or too bright
        return saturation > 0.25f && brightness > 0.2f && brightness < 0.9f
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
        // Adjust brightness for better visibility
        hsv[2] = (hsv[2] * 0.9f).coerceIn(0.4f, 0.85f)
        
        return Color(android.graphics.Color.HSVToColor(hsv))
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
     * Checks if two colors are similar (to avoid using nearly identical colors)
     */
    private fun isSimilarColor(color1: Color?, color2: Color?): Boolean {
        if (color1 == null || color2 == null) return false
        val threshold = 40 // RGB difference threshold
        val r1 = (color1.red * 255).toInt()
        val g1 = (color1.green * 255).toInt()
        val b1 = (color1.blue * 255).toInt()
        val r2 = (color2.red * 255).toInt()
        val g2 = (color2.green * 255).toInt()
        val b2 = (color2.blue * 255).toInt()
        
        return kotlin.math.abs(r1 - r2) < threshold && 
               kotlin.math.abs(g1 - g2) < threshold && 
               kotlin.math.abs(b1 - b2) < threshold
    }
    
    /**
     * Checks if a color is similar to any in a list
     */
    private fun isSimilarToAny(color: Color, colors: List<Color>): Boolean {
        return colors.any { isSimilarColor(color, it) }
    }
    
    /**
     * Darkens a color by a factor
     */
    private fun darkenColor(color: Color, factor: Float): Color {
        return color.copy(
            red = (color.red * factor).coerceAtLeast(0f),
            green = (color.green * factor).coerceAtLeast(0f),
            blue = (color.blue * factor).coerceAtLeast(0f)
        )
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
        
        const val BRIGHTNESS_MULTIPLIER = 0.9f
        const val BRIGHTNESS_MIN = 0.4f
        const val BRIGHTNESS_MAX = 0.85f
        
        const val DARKER_VARIANT_FACTOR = 0.6f
    }
}