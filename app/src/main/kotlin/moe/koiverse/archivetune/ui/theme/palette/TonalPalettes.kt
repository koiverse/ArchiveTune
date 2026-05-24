/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */



package moe.koiverse.archivetune.ui.theme.palette

import androidx.compose.ui.graphics.Color
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import com.materialkolor.ktx.toHct

private val tonalTokens = listOf(0, 10, 20, 25, 30, 40, 50, 60, 70, 80, 90, 95, 98, 99, 100)

data class TonalPalettes(
    val primary: Map<Int, Color>,
    val secondary: Map<Int, Color>,
    val tertiary: Map<Int, Color>,
    val neutral: Map<Int, Color>,
    val neutralVariant: Map<Int, Color>,
    val error: Map<Int, Color>,
) {
    companion object {
        fun fromSeedColors(
            primarySeed: Color,
            secondarySeed: Color,
            tertiarySeed: Color,
            neutralSeed: Color,
        ): TonalPalettes {
            val neutralHct = neutralSeed.toHct()
            return TonalPalettes(
                primary = primarySeed.buildTonalMap(),
                secondary = secondarySeed.buildTonalMap(),
                tertiary = tertiarySeed.buildTonalMap(),
                neutral = neutralHct.buildReducedChromaMap(4.0),
                neutralVariant = neutralHct.buildReducedChromaMap(8.0),
                error = Color(0xFFB3261E).buildTonalMap(),
            )
        }

        private fun Color.buildTonalMap(): Map<Int, Color> {
            val hct = toHct()
            return tonalTokens.associateWith { tone ->
                hct.withTone(tone.toDouble()).toColor()
            }
        }

        private fun Hct.buildReducedChromaMap(maxChroma: Double): Map<Int, Color> =
            tonalTokens.associateWith { tone ->
                Hct.from(hue, minOf(chroma, maxChroma), tone.toDouble()).toColor()
            }
    }
}
