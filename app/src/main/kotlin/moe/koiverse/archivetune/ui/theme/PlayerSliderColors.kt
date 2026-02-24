/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.theme

import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Player slider color configuration for consistent styling across all slider types
 * 
 * This object provides standardized color schemes for Default, Squiggly, and Slim sliders
 * used in the music player interface, ensuring visual consistency and proper contrast.
 */
object PlayerSliderColors {

    /**
     * Standard slider colors for all slider types
     * 
     * @param activeColor Color for active track, ticks, and thumb
     * @param inactiveAlpha Alpha transparency for inactive track (default: 0.15f for subtle appearance)
     * @return SliderColors configuration
     */
    @Composable
    fun getSliderColors(
        activeColor: Color,
        inactiveAlpha: Float = 0.15f
    ): SliderColors {
        return SliderDefaults.colors(
            activeTrackColor = activeColor,
            activeTickColor = activeColor,
            thumbColor = activeColor,
            inactiveTrackColor = Color.White.copy(alpha = inactiveAlpha)
        )
    }

    /**
     * Default slider colors using button color scheme
     * 
     * @param buttonColor The active button color from player theme
     * @return SliderColors configuration for default slider
     */
    @Composable
    fun standardSliderColors(buttonColor: Color): SliderColors {
        return getSliderColors(
            activeColor = buttonColor,
            inactiveAlpha = Config.INACTIVE_TRACK_ALPHA
        )
    }

    /**
     * Squiggly slider colors using button color scheme
     * 
     * @param buttonColor The active button color from player theme
     * @return SliderColors configuration for squiggly slider
     */
    @Composable
    fun wavySliderColors(buttonColor: Color): SliderColors {
        return SliderDefaults.colors(
            activeTrackColor = buttonColor,
            activeTickColor = buttonColor,
            thumbColor = Color.Transparent,
            inactiveTrackColor = Color.White.copy(alpha = Config.INACTIVE_TRACK_ALPHA),
            inactiveTickColor = Color.White.copy(alpha = Config.INACTIVE_TICK_ALPHA)
        )
    }

    @Composable
    fun thickSliderColors(buttonColor: Color): SliderColors {
        return getSliderColors(
            activeColor = buttonColor,
            inactiveAlpha = Config.THICK_INACTIVE_TRACK_ALPHA
        )
    }

    @Composable
    fun circularSliderColors(buttonColor: Color): SliderColors {
        return SliderDefaults.colors(
            activeTrackColor = buttonColor,
            activeTickColor = buttonColor,
            thumbColor = buttonColor,
            inactiveTrackColor = Color.White.copy(alpha = Config.INACTIVE_TRACK_ALPHA)
        )
    }

    @Composable
    fun simpleSliderColors(buttonColor: Color): SliderColors {
        return SliderDefaults.colors(
            activeTrackColor = buttonColor.copy(alpha = Config.SIMPLE_ACTIVE_TRACK_ALPHA),
            activeTickColor = buttonColor.copy(alpha = Config.SIMPLE_ACTIVE_TRACK_ALPHA),
            thumbColor = Color.Transparent,
            inactiveTrackColor = Color.White.copy(alpha = Config.SIMPLE_INACTIVE_TRACK_ALPHA),
            inactiveTickColor = Color.White.copy(alpha = Config.SIMPLE_INACTIVE_TRACK_ALPHA)
        )
    }

    /**
     * Configuration constants for slider colors
     */
    object Config {
        /** Alpha transparency for inactive track - subtle white appearance */
        const val INACTIVE_TRACK_ALPHA = 0.15f

        const val THICK_INACTIVE_TRACK_ALPHA = 0.2f

        const val SIMPLE_ACTIVE_TRACK_ALPHA = 0.8f

        const val SIMPLE_INACTIVE_TRACK_ALPHA = 0.1f
        
        /** Alpha transparency for inactive ticks */
        const val INACTIVE_TICK_ALPHA = 0.2f
        
        /** Default active color when no theme color is available */
        val DEFAULT_ACTIVE_COLOR = Color(0xFF1976D2)
        
        /** Default inactive color when no theme color is available */
        val DEFAULT_INACTIVE_COLOR = Color.White.copy(alpha = INACTIVE_TRACK_ALPHA)
    }
}