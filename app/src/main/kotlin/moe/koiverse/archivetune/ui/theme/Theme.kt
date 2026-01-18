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
        val baseColorScheme =
            remember(seedPalette, themeColor, darkTheme) {
                m3DynamicColorScheme(
                    seedPalette = seedPalette,
                    keyColor = themeColor,
                    isDark = darkTheme,
                )
            }
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
    seedPalette: ThemeSeedPalette?,
    keyColor: Color,
    isDark: Boolean,
    contrastLevel: Double = 0.0,
): ColorScheme {
    val primarySeed = seedPalette?.primary ?: keyColor
    val secondarySeed = seedPalette?.secondary ?: primarySeed
    val tertiarySeed = seedPalette?.tertiary ?: primarySeed
    val neutralSeed = seedPalette?.neutral ?: primarySeed

    val primaryScheme = SchemeTonalSpot(Hct.fromInt(primarySeed.toArgb()), isDark, contrastLevel)
    val secondaryScheme = SchemeTonalSpot(Hct.fromInt(secondarySeed.toArgb()), isDark, contrastLevel)
    val tertiaryScheme = SchemeTonalSpot(Hct.fromInt(tertiarySeed.toArgb()), isDark, contrastLevel)
    val neutralScheme = SchemeTonalSpot(Hct.fromInt(neutralSeed.toArgb()), isDark, contrastLevel)

    return ColorScheme(
        primary = primaryScheme.primary.toComposeColor(),
        onPrimary = primaryScheme.onPrimary.toComposeColor(),
        primaryContainer = primaryScheme.primaryContainer.toComposeColor(),
        onPrimaryContainer = primaryScheme.onPrimaryContainer.toComposeColor(),
        inversePrimary = primaryScheme.inversePrimary.toComposeColor(),

        secondary = secondaryScheme.primary.toComposeColor(),
        onSecondary = secondaryScheme.onPrimary.toComposeColor(),
        secondaryContainer = secondaryScheme.primaryContainer.toComposeColor(),
        onSecondaryContainer = secondaryScheme.onPrimaryContainer.toComposeColor(),

        tertiary = tertiaryScheme.primary.toComposeColor(),
        onTertiary = tertiaryScheme.onPrimary.toComposeColor(),
        tertiaryContainer = tertiaryScheme.primaryContainer.toComposeColor(),
        onTertiaryContainer = tertiaryScheme.onPrimaryContainer.toComposeColor(),

        background = neutralScheme.background.toComposeColor(),
        onBackground = neutralScheme.onBackground.toComposeColor(),
        surface = neutralScheme.surface.toComposeColor(),
        onSurface = neutralScheme.onSurface.toComposeColor(),
        surfaceVariant = neutralScheme.surfaceVariant.toComposeColor(),
        onSurfaceVariant = neutralScheme.onSurfaceVariant.toComposeColor(),
        inverseSurface = neutralScheme.inverseSurface.toComposeColor(),
        inverseOnSurface = neutralScheme.inverseOnSurface.toComposeColor(),

        surfaceBright = neutralScheme.surfaceBright.toComposeColor(),
        surfaceDim = neutralScheme.surfaceDim.toComposeColor(),
        surfaceContainer = neutralScheme.surfaceContainer.toComposeColor(),
        surfaceContainerLow = neutralScheme.surfaceContainerLow.toComposeColor(),
        surfaceContainerLowest = neutralScheme.surfaceContainerLowest.toComposeColor(),
        surfaceContainerHigh = neutralScheme.surfaceContainerHigh.toComposeColor(),
        surfaceContainerHighest = neutralScheme.surfaceContainerHighest.toComposeColor(),

        outline = neutralScheme.outline.toComposeColor(),
        outlineVariant = neutralScheme.outlineVariant.toComposeColor(),

        error = primaryScheme.error.toComposeColor(),
        onError = primaryScheme.onError.toComposeColor(),
        errorContainer = primaryScheme.errorContainer.toComposeColor(),
        onErrorContainer = primaryScheme.onErrorContainer.toComposeColor(),

        scrim = neutralScheme.scrim.toComposeColor(),
        surfaceTint = primaryScheme.surfaceTint.toComposeColor(),
    )
}

private fun Int.toComposeColor(): Color = Color(this.toLong() and 0xFFFFFFFFL)

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

    return swatch?.rgb?.toComposeColor() ?: DefaultThemeColor
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

    return listOf(first.rgb.toComposeColor(), second.rgb.toComposeColor())
        .sortedByDescending { it.luminance() }
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = value.toComposeColor()
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
