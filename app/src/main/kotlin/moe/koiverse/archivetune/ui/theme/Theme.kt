package moe.koiverse.archivetune.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeTonalSpot
import kotlin.math.abs
import kotlin.math.min

val DefaultThemeColor = Color(0xFFED5564)

data class ThemeSeedPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArchiveTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    seedPalette: ThemeSeedPalette? = null,
    useSystemFont: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useSystemDynamicColor =
        (seedPalette == null && themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    val typography = remember(useSystemFont) {
        if (useSystemFont) SystemTypography else AppTypography
    }

    if (useSystemDynamicColor) {
        val baseColorScheme =
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
            if (darkTheme && pureBlack) baseColorScheme.pureBlack(true) else baseColorScheme
        }

        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    } else {
        val keyColor = remember(seedPalette, themeColor) { seedPalette?.primary ?: themeColor }
        val baseColorScheme = remember(keyColor, darkTheme) { m3DynamicColorScheme(keyColor, darkTheme) }
        val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
            if (darkTheme && pureBlack) baseColorScheme.pureBlack(true) else baseColorScheme
        }

        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

private fun m3DynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    contrastLevel: Double = 0.0,
): ColorScheme {
    val hct = Hct.fromInt(keyColor.toArgb())
    val scheme = SchemeTonalSpot(hct, isDark, contrastLevel)

    return ColorScheme(
        background = Color(scheme.background),
        error = Color(scheme.error),
        errorContainer = Color(scheme.errorContainer),
        inverseOnSurface = Color(scheme.inverseOnSurface),
        inversePrimary = Color(scheme.inversePrimary),
        inverseSurface = Color(scheme.inverseSurface),
        onBackground = Color(scheme.onBackground),
        onError = Color(scheme.onError),
        onErrorContainer = Color(scheme.onErrorContainer),
        onPrimary = Color(scheme.onPrimary),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        onSecondary = Color(scheme.onSecondary),
        onSecondaryContainer = Color(scheme.onSecondaryContainer),
        onSurface = Color(scheme.onSurface),
        onSurfaceVariant = Color(scheme.onSurfaceVariant),
        onTertiary = Color(scheme.onTertiary),
        onTertiaryContainer = Color(scheme.onTertiaryContainer),
        outline = Color(scheme.outline),
        outlineVariant = Color(scheme.outlineVariant),
        primary = Color(scheme.primary),
        primaryContainer = Color(scheme.primaryContainer),
        scrim = Color(scheme.scrim),
        secondary = Color(scheme.secondary),
        secondaryContainer = Color(scheme.secondaryContainer),
        surface = Color(scheme.surface),
        surfaceBright = Color(scheme.surfaceBright),
        surfaceContainer = Color(scheme.surfaceContainer),
        surfaceContainerLow = Color(scheme.surfaceContainerLow),
        surfaceContainerLowest = Color(scheme.surfaceContainerLowest),
        surfaceContainerHigh = Color(scheme.surfaceContainerHigh),
        surfaceContainerHighest = Color(scheme.surfaceContainerHighest),
        surfaceDim = Color(scheme.surfaceDim),
        surfaceTint = Color(scheme.surfaceTint),
        surfaceVariant = Color(scheme.surfaceVariant),
        tertiary = Color(scheme.tertiary),
        tertiaryContainer = Color(scheme.tertiaryContainer),
    )
}

fun Bitmap.extractThemeColor(): Color {
    val palette = Palette.from(this)
        .maximumColorCount(16)
        .generate()

    val swatch =
        palette.vibrantSwatch
            ?: palette.dominantSwatch
            ?: palette.mutedSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.lightMutedSwatch
            ?: palette.darkMutedSwatch

    return swatch?.rgb?.let { Color(it) } ?: DefaultThemeColor
}

fun Bitmap.extractGradientColors(): List<Color> {
    val palette = Palette.from(this)
        .maximumColorCount(48)
        .generate()

    val swatches = palette.swatches
        .filter { it.population > 0 }
        .sortedByDescending { it.population }

    if (swatches.isEmpty()) {
        return listOf(Color(0xFF595959), Color(0xFF0D0D0D))
    }

    val first = swatches.first()
    val firstHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(first.rgb, firstHsv)

    val second =
        swatches
            .drop(1)
            .maxByOrNull { candidate ->
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(candidate.rgb, hsv)

                val hueDiffRaw = abs(hsv[0] - firstHsv[0])
                val hueDiff = min(hueDiffRaw, 360f - hueDiffRaw) / 180f
                val satDiff = abs(hsv[1] - firstHsv[1])
                val valueDiff = abs(hsv[2] - firstHsv[2])

                hueDiff * 0.65f + satDiff * 0.2f + valueDiff * 0.15f
            }
            ?: first

    return listOf(Color(first.rgb), Color(second.rgb))
        .sortedByDescending { it.luminance() }
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
