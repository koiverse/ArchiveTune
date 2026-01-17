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
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score
import moe.koiverse.archivetune.extensions.system.ExtensionThemePatch

val DefaultThemeColor = Color(0xFFED5564)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArchiveTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    themePatches: List<ExtensionThemePatch> = emptyList(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    val baseColorScheme = if (useSystemDynamicColor) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(
            seedColor = themeColor,
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot
        )
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme, themePatches) {
        val patched = baseColorScheme.applyExtensionColorPatches(themePatches, darkTheme)
        if (darkTheme && pureBlack) patched.pureBlack(true) else patched
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
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

private fun ColorScheme.applyExtensionColorPatches(
    patches: List<ExtensionThemePatch>,
    darkTheme: Boolean
): ColorScheme {
    if (patches.isEmpty()) return this
    var scheme: ColorScheme = this
    patches.forEach { patch ->
        if (patch.target != "colorScheme") return@forEach
        val mode = patch.mode.lowercase()
        val applies =
            when (mode) {
                "both" -> true
                "dark" -> darkTheme
                "light" -> !darkTheme
                else -> true
            }
        if (!applies) return@forEach
        val color = parseThemePatchColor(patch.value) ?: return@forEach
        scheme =
            when (patch.property) {
                "primary" -> scheme.copy(primary = color)
                "secondary" -> scheme.copy(secondary = color)
                "tertiary" -> scheme.copy(tertiary = color)
                "background" -> scheme.copy(background = color)
                "surface" -> scheme.copy(surface = color)
                "error" -> scheme.copy(error = color)
                "onPrimary" -> scheme.copy(onPrimary = color)
                "onSecondary" -> scheme.copy(onSecondary = color)
                "onTertiary" -> scheme.copy(onTertiary = color)
                "onBackground" -> scheme.copy(onBackground = color)
                "onSurface" -> scheme.copy(onSurface = color)
                "primaryContainer" -> scheme.copy(primaryContainer = color)
                "secondaryContainer" -> scheme.copy(secondaryContainer = color)
                "tertiaryContainer" -> scheme.copy(tertiaryContainer = color)
                "errorContainer" -> scheme.copy(errorContainer = color)
                "onPrimaryContainer" -> scheme.copy(onPrimaryContainer = color)
                "onSecondaryContainer" -> scheme.copy(onSecondaryContainer = color)
                "onTertiaryContainer" -> scheme.copy(onTertiaryContainer = color)
                "onError" -> scheme.copy(onError = color)
                "onErrorContainer" -> scheme.copy(onErrorContainer = color)
                "inversePrimary" -> scheme.copy(inversePrimary = color)
                "surfaceTint" -> scheme.copy(surfaceTint = color)
                "outline" -> scheme.copy(outline = color)
                "outlineVariant" -> scheme.copy(outlineVariant = color)
                "scrim" -> scheme.copy(scrim = color)
                "surfaceVariant" -> scheme.copy(surfaceVariant = color)
                "onSurfaceVariant" -> scheme.copy(onSurfaceVariant = color)
                "inverseSurface" -> scheme.copy(inverseSurface = color)
                "inverseOnSurface" -> scheme.copy(inverseOnSurface = color)
                else -> scheme
            }
    }
    return scheme
}

private fun parseThemePatchColor(value: String): Color? {
    val text = value.trim()
    if (!text.startsWith("#")) return null
    return runCatching { Color(android.graphics.Color.parseColor(text)) }.getOrNull()
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
