/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */


package moe.koiverse.archivetune.betterlyrics

import java.io.StringReader
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import javax.xml.parsers.DocumentBuilderFactory

object TTMLParser {

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
        val isBackground: Boolean = false,
    )

    private data class TimingContext(
        val tickRate: Double,
        val frameRate: Double,
    )

    private val CJK_BLOCKS: Set<Character.UnicodeBlock> = setOf(
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS,
        Character.UnicodeBlock.HANGUL_SYLLABLES,
        Character.UnicodeBlock.HANGUL_JAMO,
        Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO,
        Character.UnicodeBlock.HANGUL_JAMO_EXTENDED_A,
        Character.UnicodeBlock.HANGUL_JAMO_EXTENDED_B,
    )

    private fun isCjk(text: String): Boolean =
        text.any { Character.UnicodeBlock.of(it) in CJK_BLOCKS }

    fun parseTTML(ttml: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
                runCatching { isExpandEntityReferences = false }
            }
            val builder = factory.newDocumentBuilder().apply {
                setEntityResolver { _, _ -> InputSource(StringReader("")) }
            }
            val doc = builder.parse(InputSource(StringReader(ttml)))
            val timingContext = readTimingContext(doc.documentElement)
            val allElements = doc.getElementsByTagName("*")

            for (i in 0 until allElements.length) {
                val divElement = allElements.item(i) as? Element ?: continue
                if (!divElement.localName.equals("div", ignoreCase = true)) continue
                val children = divElement.childNodes
                for (j in 0 until children.length) {
                    val pElement = children.item(j) as? Element ?: continue
                    if (pElement.localName.equals("p", ignoreCase = true)) {
                        parsePElement(pElement, timingContext, lines)
                    }
                }
            }

            if (lines.isEmpty()) {
                for (i in 0 until allElements.length) {
                    val pElement = allElements.item(i) as? Element ?: continue
                    if (pElement.localName.equals("p", ignoreCase = true)) {
                        parsePElement(pElement, timingContext, lines)
                    }
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return lines.sortedBy { it.startTime }
    }

    private fun parsePElement(
        pElement: Element,
        timingContext: TimingContext,
        lines: MutableList<ParsedLine>,
    ) {
        val begin = pElement.getAttribute("begin")
        if (begin.isEmpty()) return

        val end = pElement.getAttribute("end")
        val dur = pElement.getAttribute("dur")
        val startTime = parseTime(begin, timingContext)
        val endTime = when {
            end.isNotEmpty() -> parseTime(end, timingContext)
            dur.isNotEmpty() -> startTime + parseTime(dur, timingContext)
            else -> startTime + 5.0
        }

        val agent = resolveAgent(pElement)
        val words = mutableListOf<ParsedWord>()
        val lineText = StringBuilder()

        parseSpanElements(pElement, words, lineText, startTime, endTime, false, timingContext)

        when {
            words.isEmpty() && lineText.isNotEmpty() ->
                interpolateWords(lineText.toString(), startTime, endTime, false, words)
            lineText.isEmpty() -> {
                val directText = collectAllText(pElement).trim()
                if (directText.isNotEmpty()) {
                    lineText.append(directText)
                    interpolateWords(directText, startTime, endTime, false, words)
                }
            }
        }

        val text = lineText.toString().trim()
        if (text.isNotEmpty()) {
            lines.add(
                ParsedLine(
                    text = text,
                    startTime = startTime,
                    endTime = endTime,
                    words = words,
                    isBackground = false,
                    agent = agent,
                )
            )
        }
    }

    private fun resolveAgent(element: Element): String? {
        val direct = element.getAttribute("ttm:agent").takeIf { it.isNotEmpty() }
        if (direct != null) return direct
        val attrs = element.attributes ?: return null
        return (0 until attrs.length)
            .map { attrs.item(it) }
            .firstOrNull { it.nodeName.endsWith("agent", ignoreCase = true) }
            ?.nodeValue?.takeIf { it.isNotEmpty() }
    }

    private fun interpolateWords(
        text: String,
        startTime: Double,
        endTime: Double,
        isBackground: Boolean,
        words: MutableList<ParsedWord>,
    ) {
        val isCjkText = isCjk(text)
        val tokens = if (isCjkText) tokenizeCjk(text) else splitLatin(text)
        if (tokens.isEmpty()) return
        val totalDuration = endTime - startTime
        val totalLength = tokens.sumOf { it.length }.toDouble()
        var cursor = startTime
        tokens.forEachIndexed { index, token ->
            val duration = if (totalLength > 0.0) {
                (token.length.toDouble() / totalLength) * totalDuration
            } else {
                totalDuration / tokens.size
            }
            val tokenEnd = cursor + duration
            val tokenText = if (!isCjkText && index < tokens.size - 1) "$token " else token
            words.add(ParsedWord(text = tokenText, startTime = cursor, endTime = tokenEnd, isBackground = isBackground))
            cursor = tokenEnd
        }
    }

    private fun splitLatin(text: String): List<String> =
        text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

    private fun tokenizeCjk(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val buffer = StringBuilder()
        for (char in text) {
            when {
                char.isWhitespace() -> {
                    if (buffer.isNotEmpty()) {
                        tokens.add(buffer.toString())
                        buffer.clear()
                    }
                    if (tokens.isNotEmpty()) {
                        tokens[tokens.lastIndex] = tokens.last() + char
                    }
                }
                isCjk(char.toString()) -> {
                    if (buffer.isNotEmpty()) {
                        tokens.add(buffer.toString())
                        buffer.clear()
                    }
                    tokens.add(char.toString())
                }
                else -> buffer.append(char)
            }
        }
        if (buffer.isNotEmpty()) tokens.add(buffer.toString())
        return tokens.filter { it.isNotBlank() }
    }

    private fun collectAllText(element: Element): String {
        val sb = StringBuilder()
        val nodes = element.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            when (node.nodeType) {
                Node.TEXT_NODE -> sb.append(node.textContent)
                Node.ELEMENT_NODE -> sb.append(collectAllText(node as Element))
            }
        }
        return sb.toString().replace(Regex("[ \\t\\r\\n]+"), " ")
    }

    private fun parseSpanElements(
        element: Element,
        words: MutableList<ParsedWord>,
        lineText: StringBuilder,
        lineStartTime: Double,
        lineEndTime: Double,
        isBackground: Boolean,
        timingContext: TimingContext,
    ) {
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            when (node.nodeType) {
                Node.ELEMENT_NODE -> {
                    val child = node as Element
                    if (!child.localName.equals("span", ignoreCase = true)) continue

                    val role = child.getAttribute("role").takeIf { it.isNotEmpty() }
                        ?: child.getAttribute("ttm:role")
                    val isBgSpan = role == "x-bg" || isBackground

                    if (hasDirectSpanChildren(child)) {
                        parseSpanElements(child, words, lineText, lineStartTime, lineEndTime, isBgSpan, timingContext)
                        continue
                    }

                    val wordText = getDirectTextContent(child)
                    if (wordText.isEmpty()) continue

                    lineText.append(wordText)

                    val wordBegin = child.getAttribute("begin")
                    val wordEnd = child.getAttribute("end")
                    val wordDur = child.getAttribute("dur")

                    val rawWordStart = wordBegin.takeIf { it.isNotEmpty() }?.let { parseTime(it, timingContext) }
                    val rawWordEnd = when {
                        wordEnd.isNotEmpty() -> parseTime(wordEnd, timingContext)
                        wordDur.isNotEmpty() && rawWordStart != null -> rawWordStart + parseTime(wordDur, timingContext)
                        else -> null
                    }

                    val wordStartTime = normalizeChildTime(rawWordStart, lineStartTime, lineEndTime, lineStartTime)
                    val wordEndTime = normalizeChildTime(rawWordEnd, lineStartTime, lineEndTime, lineEndTime)
                        .coerceAtLeast(wordStartTime)

                    val cleanText = wordText.trimStart()
                    if (cleanText.isEmpty()) continue

                    val isSyllableContinuation = !wordText.startsWith(" ")
                        && words.isNotEmpty()
                        && !words.last().text.endsWith(" ")

                    val lastWord = words.lastOrNull()
                    if (isSyllableContinuation && lastWord != null
                        && lastWord.isBackground == isBgSpan
                        && !isCjk(lastWord.text.trimEnd())
                        && !isCjk(cleanText.trimEnd())
                    ) {
                        words[words.lastIndex] = lastWord.copy(
                            text = lastWord.text + cleanText,
                            endTime = wordEndTime,
                        )
                    } else {
                        words.add(ParsedWord(text = cleanText, startTime = wordStartTime, endTime = wordEndTime, isBackground = isBgSpan))
                    }
                }
                Node.TEXT_NODE -> {
                    val text = node.textContent ?: continue
                    if (text.isNotBlank()) {
                        lineText.append(text)
                    } else if (text.isNotEmpty() && !text.contains('\n') && words.isNotEmpty() && !words.last().text.endsWith(" ")) {
                        lineText.append(" ")
                        words[words.lastIndex] = words.last().let { it.copy(text = it.text + " ") }
                    }
                }
            }
        }
    }

    private fun hasDirectSpanChildren(element: Element): Boolean {
        val children = element.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType == Node.ELEMENT_NODE && (node as Element).localName.equals("span", ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun getDirectTextContent(element: Element): String {
        val sb = StringBuilder()
        val children = element.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType == Node.TEXT_NODE) sb.append(node.textContent)
        }
        return sb.toString()
    }

    private fun normalizeChildTime(
        raw: Double?,
        lineStartTime: Double,
        lineEndTime: Double,
        fallback: Double,
    ): Double {
        if (raw == null || raw.isNaN() || raw.isInfinite()) return fallback
        val lineDuration = (lineEndTime - lineStartTime).coerceAtLeast(0.0)
        val adjusted = if (raw < lineStartTime - 0.001 && raw <= lineDuration + 0.5) {
            lineStartTime + raw
        } else {
            raw
        }
        return adjusted.coerceIn(lineStartTime.coerceAtLeast(0.0), lineEndTime.coerceAtLeast(lineStartTime))
    }

    private fun readTimingContext(root: Element): TimingContext {
        fun getAttrBySuffix(suffix: String): String? {
            val attrs = root.attributes ?: return null
            for (i in 0 until attrs.length) {
                val node = attrs.item(i) ?: continue
                if (node.nodeName.endsWith(suffix, ignoreCase = true)) {
                    val v = node.nodeValue?.trim()
                    if (!v.isNullOrEmpty()) return v
                }
            }
            return null
        }

        val explicitFrameRate = getAttrBySuffix("frameRate")
        val baseFrameRate = explicitFrameRate?.toDoubleOrNull() ?: 30.0
        val frameRateMultiplier = getAttrBySuffix("frameRateMultiplier")
            ?.split(Regex("\\s+"))
            ?.mapNotNull { it.toDoubleOrNull() }
            ?.takeIf { it.size == 2 && it[1] != 0.0 }
            ?.let { it[0] / it[1] }
            ?: 1.0
        val frameRate = (baseFrameRate * frameRateMultiplier).coerceAtLeast(1.0)
        val subFrameRate = getAttrBySuffix("subFrameRate")?.toDoubleOrNull() ?: 1.0
        val tickRate = getAttrBySuffix("tickRate")?.toDoubleOrNull()
            ?: if (explicitFrameRate != null) (frameRate * subFrameRate).coerceAtLeast(1.0) else 1.0

        return TimingContext(tickRate = tickRate, frameRate = frameRate)
    }

    private fun parseTime(timeStr: String, timingContext: TimingContext): Double {
        return try {
            val raw = timeStr.trim()
            if (raw.isEmpty()) return 0.0

            val offsetMatch = Regex("""^([0-9]+(?:\.[0-9]+)?)(h|ms|m|s|f|t)$""", RegexOption.IGNORE_CASE).matchEntire(raw)
            if (offsetMatch != null) {
                val value = offsetMatch.groupValues[1].toDoubleOrNull() ?: return 0.0
                return when (offsetMatch.groupValues[2].lowercase()) {
                    "h" -> value * 3600.0
                    "m" -> value * 60.0
                    "s" -> value
                    "ms" -> value / 1000.0
                    "f" -> value / timingContext.frameRate
                    "t" -> value / timingContext.tickRate
                    else -> value
                }
            }

            val cleanClock = raw.replace(';', ':').trimEnd { it.isLetter() }
            if (cleanClock.contains(":")) {
                val parts = cleanClock.split(":")
                return when (parts.size) {
                    2 -> {
                        val minutes = parts[0].toDoubleOrNull() ?: 0.0
                        val seconds = parts[1].toDoubleOrNull() ?: 0.0
                        minutes * 60.0 + seconds
                    }
                    3 -> {
                        val hours = parts[0].toDoubleOrNull() ?: 0.0
                        val minutes = parts[1].toDoubleOrNull() ?: 0.0
                        val seconds = parts[2].toDoubleOrNull() ?: 0.0
                        hours * 3600.0 + minutes * 60.0 + seconds
                    }
                    4 -> {
                        val hours = parts[0].toDoubleOrNull() ?: 0.0
                        val minutes = parts[1].toDoubleOrNull() ?: 0.0
                        val seconds = parts[2].toDoubleOrNull() ?: 0.0
                        val frames = parts[3].toDoubleOrNull() ?: 0.0
                        hours * 3600.0 + minutes * 60.0 + seconds + (frames / timingContext.frameRate)
                    }
                    else -> cleanClock.toDoubleOrNull() ?: 0.0
                }
            }

            raw.toDoubleOrNull() ?: 0.0
        } catch (_: Exception) {
            0.0
        }
    }
}
