package moe.koiverse.archivetune.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.Score

val DefaultThemeColor = Color(0xFFED5564)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArchiveTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    useSystemFont: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Determine if system dynamic colors should be used (Android S+ and default theme color)
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (useSystemDynamicColor) {
        // Use standard Material 3 dynamic color functions for system wallpaper colors
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        remember(themeColor, darkTheme) {
            materialColorUtilitiesScheme(seedColor = themeColor, isDark = darkTheme)
        }
    }

    // Apply pureBlack modification if needed, similar to original logic
    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    val typography = remember(useSystemFont) {
        if (useSystemFont) SystemTypography else AppTypography
    }

    // Use the defined M3 Expressive Typography
    // TODO: Define M3 Expressive Shapes instance if needed
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = typography,
        // shapes = MaterialTheme.shapes, // Placeholder - Needs update (Shapes not used in original)
        content = content
    )
}

private fun materialColorUtilitiesScheme(seedColor: Color, isDark: Boolean): ColorScheme {
    val scheme = SchemeTonalSpot(Hct.fromInt(seedColor.toArgb()), isDark, 0.0)
    val color = { argb: Int -> Color(argb) }

    return if (isDark) {
        darkColorScheme(
            primary = color(scheme.primary),
            onPrimary = color(scheme.onPrimary),
            primaryContainer = color(scheme.primaryContainer),
            onPrimaryContainer = color(scheme.onPrimaryContainer),
            inversePrimary = color(scheme.inversePrimary),
            secondary = color(scheme.secondary),
            onSecondary = color(scheme.onSecondary),
            secondaryContainer = color(scheme.secondaryContainer),
            onSecondaryContainer = color(scheme.onSecondaryContainer),
            tertiary = color(scheme.tertiary),
            onTertiary = color(scheme.onTertiary),
            tertiaryContainer = color(scheme.tertiaryContainer),
            onTertiaryContainer = color(scheme.onTertiaryContainer),
            background = color(scheme.background),
            onBackground = color(scheme.onBackground),
            surface = color(scheme.surface),
            onSurface = color(scheme.onSurface),
            surfaceVariant = color(scheme.surfaceVariant),
            onSurfaceVariant = color(scheme.onSurfaceVariant),
            surfaceTint = color(scheme.surfaceTint),
            inverseSurface = color(scheme.inverseSurface),
            inverseOnSurface = color(scheme.inverseOnSurface),
            error = color(scheme.error),
            onError = color(scheme.onError),
            errorContainer = color(scheme.errorContainer),
            onErrorContainer = color(scheme.onErrorContainer),
            outline = color(scheme.outline),
            outlineVariant = color(scheme.outlineVariant),
            scrim = color(scheme.scrim),
            surfaceBright = color(scheme.surfaceBright),
            surfaceDim = color(scheme.surfaceDim),
            surfaceContainer = color(scheme.surfaceContainer),
            surfaceContainerHigh = color(scheme.surfaceContainerHigh),
            surfaceContainerHighest = color(scheme.surfaceContainerHighest),
            surfaceContainerLow = color(scheme.surfaceContainerLow),
            surfaceContainerLowest = color(scheme.surfaceContainerLowest),
        )
    } else {
        lightColorScheme(
            primary = color(scheme.primary),
            onPrimary = color(scheme.onPrimary),
            primaryContainer = color(scheme.primaryContainer),
            onPrimaryContainer = color(scheme.onPrimaryContainer),
            inversePrimary = color(scheme.inversePrimary),
            secondary = color(scheme.secondary),
            onSecondary = color(scheme.onSecondary),
            secondaryContainer = color(scheme.secondaryContainer),
            onSecondaryContainer = color(scheme.onSecondaryContainer),
            tertiary = color(scheme.tertiary),
            onTertiary = color(scheme.onTertiary),
            tertiaryContainer = color(scheme.tertiaryContainer),
            onTertiaryContainer = color(scheme.onTertiaryContainer),
            background = color(scheme.background),
            onBackground = color(scheme.onBackground),
            surface = color(scheme.surface),
            onSurface = color(scheme.onSurface),
            surfaceVariant = color(scheme.surfaceVariant),
            onSurfaceVariant = color(scheme.onSurfaceVariant),
            surfaceTint = color(scheme.surfaceTint),
            inverseSurface = color(scheme.inverseSurface),
            inverseOnSurface = color(scheme.inverseOnSurface),
            error = color(scheme.error),
            onError = color(scheme.onError),
            errorContainer = color(scheme.errorContainer),
            onErrorContainer = color(scheme.onErrorContainer),
            outline = color(scheme.outline),
            outlineVariant = color(scheme.outlineVariant),
            scrim = color(scheme.scrim),
            surfaceBright = color(scheme.surfaceBright),
            surfaceDim = color(scheme.surfaceDim),
            surfaceContainer = color(scheme.surfaceContainer),
            surfaceContainerHigh = color(scheme.surfaceContainerHigh),
            surfaceContainerHighest = color(scheme.surfaceContainerHighest),
            surfaceContainerLow = color(scheme.surfaceContainerLow),
            surfaceContainerLowest = color(scheme.surfaceContainerLowest),
        )
    }
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
