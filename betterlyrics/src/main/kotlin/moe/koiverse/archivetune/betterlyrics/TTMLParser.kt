/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.betterlyrics

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
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
        val isBackground: Boolean = false
    )

    private data class TimingContext(
        val tickRate: Double,
        val frameRate: Double,
    )

    private fun isCjk(text: String): Boolean {
        return text.any { c ->
            Character.UnicodeBlock.of(c) in setOf(
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
                Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
                Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT,
                Character.UnicodeBlock.HIRAGANA,
                Character.UnicodeBlock.KATAKANA,
                Character.UnicodeBlock.HANGUL_SYLLABLES,
                Character.UnicodeBlock.HANGUL_JAMO,
                Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
            )
        }
    }
    
    fun parseTTML(ttml: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()
        
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ttml.byteInputStream())
            val timingContext = readTimingContext(doc.documentElement)
            
            // Find all elements ending with "div" which contain elements ending with "p"
            val divElements = doc.getElementsByTagName("*") // Get all elements first
            
            for (divIdx in 0 until divElements.length) {
                val divElement = divElements.item(divIdx) as? Element ?: continue
                if (!divElement.tagName.endsWith("div", ignoreCase = true)) continue

                // Get all child elements and check for "p" tag suffix
                val pElements = divElement.getElementsByTagName("*")
                
                for (pIdx in 0 until pElements.length) {
                    val pElement = pElements.item(pIdx) as? Element ?: continue
                    if (!pElement.tagName.endsWith("p", ignoreCase = true)) continue
                    
                    val begin = pElement.getAttribute("begin")
                    val end = pElement.getAttribute("end")
                    val dur = pElement.getAttribute("dur")
                    if (begin.isNullOrEmpty()) continue
                    
                    val startTime = parseTime(begin, timingContext)
                    val endTime = when {
                        end.isNotEmpty() -> parseTime(end, timingContext)
                        dur.isNotEmpty() -> startTime + parseTime(dur, timingContext)
                        else -> startTime + 5.0
                    }
                    
                    // Parse agent attribute (ttm:agent or similar)
                    val agent = pElement.getAttribute("ttm:agent").takeIf { it.isNotEmpty() }
                        ?: pElement.attributes?.let { attrs ->
                            (0 until attrs.length)
                                .map { attrs.item(it) }
                                .firstOrNull { it.nodeName.endsWith("agent", ignoreCase = true) }
                                ?.nodeValue?.takeIf { it.isNotEmpty() }
                        }
                    
                    val words = mutableListOf<ParsedWord>()
                    val lineText = StringBuilder()
                    
                    // Recursively parse all span elements including nested ones (for background vocals)
                    parseSpanElements(pElement, words, lineText, startTime, endTime, false, timingContext)
                    
                    // If words list is empty but we have text, we need to generate fallback word timings
                    if (words.isEmpty() && lineText.isNotEmpty()) {
                        val directText = lineText.toString()
                        val isCjkText = isCjk(directText)
                        val splitWords = if (isCjkText) {
                            val chars = mutableListOf<String>()
                            var currentWord = StringBuilder()
                            directText.forEach { char ->
                                if (char.isWhitespace()) {
                                    if (currentWord.isNotEmpty()) {
                                        chars.add(currentWord.toString())
                                        currentWord.clear()
                                    }
                                    chars.add(char.toString())
                                } else if (isCjk(char.toString())) {
                                    if (currentWord.isNotEmpty()) {
                                        chars.add(currentWord.toString())
                                        currentWord.clear()
                                    }
                                    chars.add(char.toString())
                                } else {
                                    currentWord.append(char)
                                }
                            }
                            if (currentWord.isNotEmpty()) {
                                chars.add(currentWord.toString())
                            }

                            val groupedTokens = mutableListOf<String>()
                            chars.forEach { c ->
                                if (c.isBlank()) {
                                    if (groupedTokens.isNotEmpty()) {
                                        groupedTokens[groupedTokens.lastIndex] = groupedTokens.last() + c
                                    }
                                } else {
                                    groupedTokens.add(c)
                                }
                            }
                            groupedTokens
                        } else {
                            directText.split(Regex("\\s+"))
                        }
                        
                        val totalDuration = endTime - startTime
                        val totalLength = splitWords.sumOf { it.length }.toDouble()
                        
                        var currentWordStart = startTime
                        
                        splitWords.forEachIndexed { index, word ->
                            val wordLen = word.length.toDouble()
                            val wordDuration = if (totalLength > 0) {
                                (wordLen / totalLength) * totalDuration
                            } else {
                                totalDuration / splitWords.size
                            }
                            
                            val wordEnd = currentWordStart + wordDuration
                            val wordText = if (index < splitWords.size - 1 && !isCjkText) "$word " else word
                            
                            words.add(
                                ParsedWord(
                                    text = wordText,
                                    startTime = currentWordStart,
                                    endTime = wordEnd,
                                    isBackground = false
                                )
                            )
                            currentWordStart = wordEnd
                        }
                    } else if (lineText.isEmpty()) {
                        // Original fallback for when parseSpanElements failed to find even text nodes (e.g. CDATA or just completely empty?)
                        val directText = getDirectTextContent(pElement).trim()
                        if (directText.isNotEmpty()) {
                            lineText.append(directText)
                            
                            val isCjkText = isCjk(directText)
                            val splitWords = if (isCjkText) {
                                val chars = mutableListOf<String>()
                                var currentWord = StringBuilder()
                                directText.forEach { char ->
                                    if (char.isWhitespace()) {
                                        if (currentWord.isNotEmpty()) {
                                            chars.add(currentWord.toString())
                                            currentWord.clear()
                                        }
                                        chars.add(char.toString())
                                    } else if (isCjk(char.toString())) {
                                        if (currentWord.isNotEmpty()) {
                                            chars.add(currentWord.toString())
                                            currentWord.clear()
                                        }
                                        chars.add(char.toString())
                                    } else {
                                        currentWord.append(char)
                                    }
                                }
                                if (currentWord.isNotEmpty()) {
                                    chars.add(currentWord.toString())
                                }

                                val groupedTokens = mutableListOf<String>()
                                chars.forEach { c ->
                                    if (c.isBlank()) {
                                        if (groupedTokens.isNotEmpty()) {
                                            groupedTokens[groupedTokens.lastIndex] = groupedTokens.last() + c
                                        }
                                    } else {
                                        groupedTokens.add(c)
                                    }
                                }
                                groupedTokens
                            } else {
                                directText.split(Regex("\\s+"))
                            }
                            
                            val totalDuration = endTime - startTime
                            val totalLength = splitWords.sumOf { it.length }.toDouble()
                            
                            var currentWordStart = startTime
                            
                            splitWords.forEachIndexed { index, word ->
                                val wordLen = word.length.toDouble()
                                val wordDuration = if (totalLength > 0) {
                                    (wordLen / totalLength) * totalDuration
                                } else {
                                    totalDuration / splitWords.size
                                }
                                
                                val wordEnd = currentWordStart + wordDuration
                                val wordText = if (index < splitWords.size - 1 && !isCjkText) "$word " else word
                                
                                words.add(
                                    ParsedWord(
                                        text = wordText,
                                        startTime = currentWordStart,
                                        endTime = wordEnd,
                                        isBackground = false
                                    )
                                )
                                currentWordStart = wordEnd
                            }
                        }
                    }
                    
                    if (lineText.isNotEmpty()) {
                        lines.add(
                            ParsedLine(
                                text = lineText.toString().trim(),
                                startTime = startTime,
                                endTime = endTime,
                                words = words,
                                isBackground = false,
                                agent = agent,
                            )
                        )
                    }
                }
            }
            
            // Fallback: If no div elements found, try parsing p elements directly
            if (lines.isEmpty()) {
                val pElements = doc.getElementsByTagName("*")
                
                for (i in 0 until pElements.length) {
                    val pElement = pElements.item(i) as? Element ?: continue
                    if (!pElement.tagName.endsWith("p", ignoreCase = true)) continue
                    
                    val begin = pElement.getAttribute("begin")
                    val end = pElement.getAttribute("end")
                    val dur = pElement.getAttribute("dur")
                    if (begin.isNullOrEmpty()) continue
                    
                    val startTime = parseTime(begin, timingContext)
                    val endTime = when {
                        end.isNotEmpty() -> parseTime(end, timingContext)
                        dur.isNotEmpty() -> startTime + parseTime(dur, timingContext)
                        else -> startTime + 5.0
                    }
                    
                    // Parse agent attribute
                    val agent = pElement.getAttribute("ttm:agent").takeIf { it.isNotEmpty() }
                        ?: pElement.attributes?.let { attrs ->
                            (0 until attrs.length)
                                .map { attrs.item(it) }
                                .firstOrNull { it.nodeName.endsWith("agent", ignoreCase = true) }
                                ?.nodeValue?.takeIf { it.isNotEmpty() }
                        }
                    
                    val words = mutableListOf<ParsedWord>()
                    val lineText = StringBuilder()
                    
                    parseSpanElements(pElement, words, lineText, startTime, endTime, false, timingContext)
                    
                    if (words.isEmpty() && lineText.isNotEmpty()) {
                        val directText = lineText.toString()
                        
                         val isCjkText = isCjk(directText)
                            val splitWords = if (isCjkText) {
                                val chars = mutableListOf<String>()
                                var currentWord = StringBuilder()
                                directText.forEach { char ->
                                    if (char.isWhitespace()) {
                                        if (currentWord.isNotEmpty()) {
                                            chars.add(currentWord.toString())
                                            currentWord.clear()
                                        }
                                        chars.add(char.toString())
                                    } else if (isCjk(char.toString())) {
                                        if (currentWord.isNotEmpty()) {
                                            chars.add(currentWord.toString())
                                            currentWord.clear()
                                        }
                                        chars.add(char.toString())
                                    } else {
                                        currentWord.append(char)
                                    }
                                }
                                if (currentWord.isNotEmpty()) {
                                    chars.add(currentWord.toString())
                                }

                                val groupedTokens = mutableListOf<String>()
                                chars.forEach { c ->
                                    if (c.isBlank()) {
                                        if (groupedTokens.isNotEmpty()) {
                                            groupedTokens[groupedTokens.lastIndex] = groupedTokens.last() + c
                                        }
                                    } else {
                                        groupedTokens.add(c)
                                    }
                                }
                                groupedTokens
                            } else {
                                directText.split(Regex("\\s+"))
                            }
                            
                            val totalDuration = endTime - startTime
                            val totalLength = splitWords.sumOf { it.length }.toDouble()
                            
                            var currentWordStart = startTime
                            
                            splitWords.forEachIndexed { index, word ->
                                val wordLen = word.length.toDouble()
                                val wordDuration = if (totalLength > 0) {
                                    (wordLen / totalLength) * totalDuration
                                } else {
                                    totalDuration / splitWords.size
                                }
                                
                                val wordEnd = currentWordStart + wordDuration
                                
                                val wordText = if (index < splitWords.size - 1 && !isCjkText) "$word " else word
                                
                                words.add(
                                    ParsedWord(
                                        text = wordText,
                                        startTime = currentWordStart,
                                        endTime = wordEnd,
                                        isBackground = false
                                    )
                                )
                                currentWordStart = wordEnd
                            }
                    } else if (lineText.isEmpty()) {
                        val directText = getDirectTextContent(pElement).trim()
                        if (directText.isNotEmpty()) {
                            lineText.append(directText)
                            
                            // Split line into words and interpolate timing
                            val isCjkText = isCjk(directText)
                            val splitWords = if (isCjkText) {
                                val chars = mutableListOf<String>()
                                var currentWord = StringBuilder()
                                directText.forEach { char ->
                                    if (char.isWhitespace()) {
                                        if (currentWord.isNotEmpty()) {
                                            chars.add(currentWord.toString())
                                            currentWord.clear()
                                        }
                                        chars.add(char.toString())
                                    } else if (isCjk(char.toString())) {
                                        if (currentWord.isNotEmpty()) {
                                            chars.add(currentWord.toString())
                                            currentWord.clear()
                                        }
                                        chars.add(char.toString())
                                    } else {
                                        currentWord.append(char)
                                    }
                                }
                                if (currentWord.isNotEmpty()) {
                                    chars.add(currentWord.toString())
                                }

                                val groupedTokens = mutableListOf<String>()
                                chars.forEach { c ->
                                    if (c.isBlank()) {
                                        if (groupedTokens.isNotEmpty()) {
                                            groupedTokens[groupedTokens.lastIndex] = groupedTokens.last() + c
                                        }
                                    } else {
                                        groupedTokens.add(c)
                                    }
                                }
                                groupedTokens
                            } else {
                                directText.split(Regex("\\s+"))
                            }
                            
                            val totalDuration = endTime - startTime
                            val totalLength = splitWords.sumOf { it.length }.toDouble()
                            
                            var currentWordStart = startTime
                            
                            splitWords.forEachIndexed { index, word ->
                                val wordLen = word.length.toDouble()
                                val wordDuration = if (totalLength > 0) {
                                    (wordLen / totalLength) * totalDuration
                                } else {
                                    totalDuration / splitWords.size
                                }
                                
                                val wordEnd = currentWordStart + wordDuration
                                
                                val wordText = if (index < splitWords.size - 1 && !isCjkText) "$word " else word
                                
                                words.add(
                                    ParsedWord(
                                        text = wordText,
                                        startTime = currentWordStart,
                                        endTime = wordEnd,
                                        isBackground = false
                                    )
                                )
                                currentWordStart = wordEnd
                            }
                        }
                    }
                    
                    if (lineText.isNotEmpty()) {
                        lines.add(
                            ParsedLine(
                                text = lineText.toString().trim(),
                                startTime = startTime,
                                endTime = endTime,
                                words = words,
                                isBackground = false,
                                agent = agent,
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
        
        return lines.sortedBy { it.startTime }
    }
    
    /**
     * Recursively parse span elements to extract word-level timing
     * Handles nested spans for background vocals (role="x-bg")
     */
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
                    val childElement = node as Element
                    if (childElement.tagName.endsWith("span", ignoreCase = true)) {
                        // Check if this is a background vocal span (handles role="" and ttm:role="")
                        val role = childElement.getAttribute("role").takeIf { it.isNotEmpty() }
                            ?: childElement.getAttribute("ttm:role")
                        val isBgSpan = role == "x-bg" || isBackground
                        
                        val wordBegin = childElement.getAttribute("begin")
                        val wordEnd = childElement.getAttribute("end")
                        val wordDur = childElement.getAttribute("dur")
                        
                        // Check if this span has nested spans (for word-level timing within bg)
                        val nestedSpans = childElement.getElementsByTagName("*")
                        if (nestedSpans.length > 0 && hasDirectSpanChildren(childElement)) {
                            // Parse nested spans recursively
                            parseSpanElements(childElement, words, lineText, lineStartTime, lineEndTime, isBgSpan, timingContext)
                        } else {
                            // This is a leaf span with text
                            val wordText = getDirectTextContent(childElement)
                            if (wordText.isNotEmpty()) {
                                // Check if this span visually continues the previous word without a space
                                val isSyllableContinuation = words.isNotEmpty() && !words.last().text.endsWith(" ")

                                lineText.append(wordText)

                                val rawWordStart = wordBegin.takeIf { it.isNotEmpty() }?.let { parseTime(it, timingContext) }
                                val rawWordEnd = when {
                                    wordEnd.isNotEmpty() -> parseTime(wordEnd, timingContext)
                                    wordDur.isNotEmpty() && rawWordStart != null -> rawWordStart + parseTime(wordDur, timingContext)
                                    else -> null
                                }

                                val wordStartTime = normalizeChildTime(
                                    raw = rawWordStart,
                                    lineStartTime = lineStartTime,
                                    lineEndTime = lineEndTime,
                                    fallback = lineStartTime,
                                )
                                val wordEndTime = normalizeChildTime(
                                    raw = rawWordEnd,
                                    lineStartTime = lineStartTime,
                                    lineEndTime = lineEndTime,
                                    fallback = lineEndTime,
                                ).coerceAtLeast(wordStartTime)
                                
                                val trimmedText = wordText.trim()
                                val newWord = ParsedWord(
                                    text = trimmedText,
                                    startTime = wordStartTime,
                                    endTime = wordEndTime,
                                    isBackground = isBgSpan
                                )
                                
                                val lastWord = words.lastOrNull()
                                if (isSyllableContinuation && lastWord != null && 
                                    !lastWord.text.endsWith(" ") && 
                                    lastWord.isBackground == isBgSpan && 
                                    !isCjk(lastWord.text.trim()) && !isCjk(trimmedText) &&
                                    trimmedText.isNotEmpty()
                                ) {
                                    // Merge non-CJK syllables into a single animated word to prevent visual tearing
                                    words[words.lastIndex] = lastWord.copy(
                                        text = lastWord.text + trimmedText,
                                        endTime = wordEndTime
                                    )
                                } else if (trimmedText.isNotEmpty()) {
                                    words.add(newWord)
                                }
                            }
                        }
                    }
                }
                Node.TEXT_NODE -> {
                    val text = node.textContent
                    // If text node is purely whitespace, treat as a single space indicator
                    // If it has content, append as is
                    if (text.isNotBlank()) {
                         lineText.append(text)
                    } else if (text.isNotEmpty() && !text.contains('\n')) {
                         // Collapse multiple whitespace chars to single space
                         if (words.isNotEmpty() && !words.last().text.endsWith(" ")) {
                             lineText.append(" ")
                             val lastWord = words.last()
                             words[words.lastIndex] = lastWord.copy(text = lastWord.text + " ")
                         }
                    }
                }
            }
        }
    }
    
    /**
     * Check if element has direct span children (not nested further)
     */
    private fun hasDirectSpanChildren(element: Element): Boolean {
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val childElement = node as Element
                    if (childElement.tagName.endsWith("span", ignoreCase = true)) {
                        return true
                    }
            }
        }
        return false
    }
    
    /**
     * Get only direct text content of an element (not from nested elements)
     */
    private fun getDirectTextContent(element: Element): String {
        val textBuilder = StringBuilder()
        val childNodes = element.childNodes
        
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType == Node.TEXT_NODE) {
                textBuilder.append(node.textContent)
            }
        }
        
        return textBuilder.toString()
    }

    private fun normalizeChildTime(
        raw: Double?,
        lineStartTime: Double,
        lineEndTime: Double,
        fallback: Double,
    ): Double {
        if (raw == null || raw.isNaN() || raw.isInfinite()) return fallback
        val lineDuration = (lineEndTime - lineStartTime).coerceAtLeast(0.0)
        val isProbablyRelative =
            raw < (lineStartTime - 0.25) && raw <= (lineDuration + 1.0)
        val adjusted = if (isProbablyRelative) lineStartTime + raw else raw
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

        val baseFrameRate = getAttrBySuffix("frameRate")?.toDoubleOrNull() ?: 30.0
        val frameRateMultiplierRaw = getAttrBySuffix("frameRateMultiplier")
        val frameRateMultiplier = frameRateMultiplierRaw
            ?.split(Regex("\\s+"))
            ?.mapNotNull { it.toDoubleOrNull() }
            ?.takeIf { it.size == 2 && it[1] != 0.0 }
            ?.let { it[0] / it[1] }
            ?: 1.0
        val frameRate = (baseFrameRate * frameRateMultiplier).coerceAtLeast(1.0)

        val tickRate = getAttrBySuffix("tickRate")?.toDoubleOrNull()
            ?: (frameRate * 1.0).coerceAtLeast(1.0)

        return TimingContext(
            tickRate = tickRate,
            frameRate = frameRate,
        )
    }
    
    /**
     * Parse TTML time format
     * Supports: "9.731", "1:23.456", "1:23:45.678", "00:01:23.456"
     */
    private fun parseTime(timeStr: String, timingContext: TimingContext): Double {
        return try {
            val raw = timeStr.trim()
            if (raw.isEmpty()) return 0.0

            val offsetRegex = Regex("""^([0-9]+(?:\.[0-9]+)?)(h|ms|m|s|f|t)$""", RegexOption.IGNORE_CASE)
            offsetRegex.matchEntire(raw)?.let { m ->
                val value = m.groupValues[1].toDoubleOrNull() ?: return 0.0
                return when (m.groupValues[2].lowercase()) {
                    "h" -> value * 3600.0
                    "m" -> value * 60.0
                    "s" -> value
                    "ms" -> value / 1000.0
                    "f" -> value / timingContext.frameRate
                    "t" -> value / timingContext.tickRate
                    else -> value
                }
            }

            val cleanClock = raw
                .replace(';', ':')
                .trimEnd { it.isLetter() }

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
        } catch (e: Exception) {
            0.0
        }
    }
}
