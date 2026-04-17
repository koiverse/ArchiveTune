/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.betterlyrics

data class ParsedLine(
    val text: String,
    val startTime: Double,
    val endTime: Double,
    val words: List<ParsedWord>,
    val isBackground: Boolean = false,
    val agent: String? = null,
)

data class ParsedWord(
    val text: String,
    val startTime: Double,
    val endTime: Double,
    val isBackground: Boolean = false
)

expect object TTMLParser {
    fun parseTTML(ttml: String): List<ParsedLine>
}