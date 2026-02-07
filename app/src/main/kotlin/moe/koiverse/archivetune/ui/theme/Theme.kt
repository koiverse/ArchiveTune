/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.theme

import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
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
import com.kyant.m3color.scheme.SchemeMonochrome
import com.kyant.m3color.scheme.SchemeNeutral
import com.kyant.m3color.scheme.SchemeTonalSpot
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    val appColorScheme =
        remember(seedPalette, themeColor, darkTheme) {
            if (seedPalette != null) {
                exactPaletteColorScheme(
                    palette = seedPalette,
                    isDark = darkTheme,
                )
            } else {
                m3DynamicColorScheme(
                    seedPalette = null,
                    keyColor = themeColor,
                    isDark = darkTheme,
                )
            }
        }

    val baseColorScheme =
        if (useSystemDynamicColor) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            appColorScheme
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

private fun exactPaletteColorScheme(
    palette: ThemeSeedPalette,
    isDark: Boolean,
): ColorScheme {
    val primaryScheme = m3Scheme(palette.primary, isDark, 0.0)
    val secondaryScheme = m3Scheme(palette.secondary, isDark, 0.0)
    val tertiaryScheme = m3Scheme(palette.tertiary, isDark, 0.0)
    val neutralScheme = m3Scheme(palette.neutral, isDark, 0.0)

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

    val primaryScheme = m3Scheme(primarySeed, isDark, contrastLevel)
    val secondaryScheme = m3Scheme(secondarySeed, isDark, contrastLevel)
    val tertiaryScheme = m3Scheme(tertiarySeed, isDark, contrastLevel)
    val neutralScheme = m3Scheme(neutralSeed, isDark, contrastLevel)

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

private fun m3Scheme(seedColor: Color, isDark: Boolean, contrastLevel: Double) =
    Hct.fromInt(seedColor.toArgb()).let { hct ->
        when {
            hct.chroma < 4.0 -> SchemeMonochrome(hct, isDark, contrastLevel)
            hct.chroma < 12.0 -> SchemeNeutral(hct, isDark, contrastLevel)
            else -> SchemeTonalSpot(hct, isDark, contrastLevel)
        }
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

@Serializable
data class ThemeExportV1(
    val version: Int = 1,
    val name: String? = null,
    val primary: String,
    val secondary: String,
    val tertiary: String,
    val neutral: String,
)

object ThemeSeedPaletteCodec {
    private const val PreferencePrefix = "seedPalette:"
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun encodeForPreference(palette: ThemeSeedPalette, name: String? = null): String {
        val payload = json.encodeToString(
            ThemeExportV1(
                name = name,
                primary = palette.primary.toHexArgbString(),
                secondary = palette.secondary.toHexArgbString(),
                tertiary = palette.tertiary.toHexArgbString(),
                neutral = palette.neutral.toHexArgbString(),
            )
        )
        val b64 = Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        return PreferencePrefix + b64
    }

    fun decodeFromPreference(value: String): ThemeSeedPalette? {
        if (!value.startsWith(PreferencePrefix)) return null
        val b64 = value.removePrefix(PreferencePrefix)
        val decoded =
            runCatching {
                val bytes = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP)
                bytes.toString(Charsets.UTF_8)
            }.getOrNull()
                ?: return null
        return decodeFromJson(decoded)
    }

    fun encodeAsJson(palette: ThemeSeedPalette, name: String? = null): String =
        json.encodeToString(
            ThemeExportV1(
                name = name,
                primary = palette.primary.toHexArgbString(),
                secondary = palette.secondary.toHexArgbString(),
                tertiary = palette.tertiary.toHexArgbString(),
                neutral = palette.neutral.toHexArgbString(),
            )
        )

    fun decodeFromJson(text: String): ThemeSeedPalette? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            val element = json.parseToJsonElement(trimmed)
            val obj = element.jsonObject

            val version = obj["version"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
            if (version != 1) return@runCatching null

            fun getColor(key: String): Color? =
                obj[key]
                    ?.jsonPrimitive
                    ?.content
                    ?.toColorOrNull()

            val primary = getColor("primary") ?: return@runCatching null
            val secondary = getColor("secondary") ?: primary
            val tertiary = getColor("tertiary") ?: primary
            val neutral = getColor("neutral") ?: primary

            ThemeSeedPalette(
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                neutral = neutral,
            )
        }.getOrNull()
            ?: decodeFromLegacyObject(trimmed)
    }

    fun extractNameFromJsonOrNull(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            val element: JsonElement = json.parseToJsonElement(trimmed)
            element.jsonObject["name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun extractNameFromPreference(value: String): String? {
        if (!value.startsWith(PreferencePrefix)) return null
        val b64 = value.removePrefix(PreferencePrefix)
        val decoded =
            runCatching {
                val bytes = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP)
                bytes.toString(Charsets.UTF_8)
            }.getOrNull()
                ?: return null
        return extractNameFromJsonOrNull(decoded)
    }

    private fun decodeFromLegacyObject(text: String): ThemeSeedPalette? {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{")) return null
        return runCatching {
            val element = json.parseToJsonElement(trimmed)
            val obj = element.jsonObject

            fun getHex(key: String): String? =
                obj[key]
                    ?.jsonPrimitive
                    ?.content
                    ?.takeIf { it.isNotBlank() }

            val primary = getHex("primary")?.toColorOrNull() ?: return@runCatching null
            val secondary = getHex("secondary")?.toColorOrNull() ?: primary
            val tertiary = getHex("tertiary")?.toColorOrNull() ?: primary
            val neutral = getHex("neutral")?.toColorOrNull() ?: primary

            ThemeSeedPalette(primary, secondary, tertiary, neutral)
        }.getOrNull()
    }

    private fun Color.toHexArgbString(): String = String.format("#%08X", this.toArgb())

    private fun String.toColorOrNull(): Color? {
        val normalized = trim()
        if (normalized.isEmpty()) return null
        return runCatching {
            val withHash = if (normalized.startsWith("#")) normalized else "#$normalized"
            Color(android.graphics.Color.parseColor(withHash))
        }.getOrNull()
    }
}
