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
        val isBackground: Boolean = false
    )
    
    data class ParsedWord(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val isBackground: Boolean = false
    )
    
    fun parseTTML(ttml: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()
        
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ttml.byteInputStream())
            
            // Find all <div> elements which contain <p> elements (sections)
            val divElements = doc.getElementsByTagName("div")
            
            for (divIdx in 0 until divElements.length) {
                val divElement = divElements.item(divIdx) as? Element ?: continue
                
                // Get all <p> elements within this div
                val pElements = divElement.getElementsByTagName("p")
                
                for (pIdx in 0 until pElements.length) {
                    val pElement = pElements.item(pIdx) as? Element ?: continue
                    
                    val begin = pElement.getAttribute("begin")
                    val end = pElement.getAttribute("end")
                    if (begin.isNullOrEmpty()) continue
                    
                    val startTime = parseTime(begin)
                    val endTime = if (end.isNotEmpty()) parseTime(end) else startTime + 5.0
                    val words = mutableListOf<ParsedWord>()
                    val lineText = StringBuilder()
                    
                    // Recursively parse all span elements including nested ones (for background vocals)
                    parseSpanElements(pElement, words, lineText, startTime, endTime, false)
                    
                    // If no spans found, check for direct text content
                    if (lineText.isEmpty()) {
                        val directText = getDirectTextContent(pElement).trim()
                        if (directText.isNotEmpty()) {
                            lineText.append(directText)
                            // Create a single word entry for the entire line if no word timing
                            words.add(
                                ParsedWord(
                                    text = directText,
                                    startTime = startTime,
                                    endTime = endTime,
                                    isBackground = false
                                )
                            )
                        }
                    }
                    
                    if (lineText.isNotEmpty()) {
                        lines.add(
                            ParsedLine(
                                text = lineText.toString().trim(),
                                startTime = startTime,
                                endTime = endTime,
                                words = words,
                                isBackground = false
                            )
                        )
                    }
                }
            }
            
            // Fallback: If no div elements found, try parsing p elements directly
            if (lines.isEmpty()) {
                val pElements = doc.getElementsByTagName("p")
                
                for (i in 0 until pElements.length) {
                    val pElement = pElements.item(i) as? Element ?: continue
                    
                    val begin = pElement.getAttribute("begin")
                    val end = pElement.getAttribute("end")
                    if (begin.isNullOrEmpty()) continue
                    
                    val startTime = parseTime(begin)
                    val endTime = if (end.isNotEmpty()) parseTime(end) else startTime + 5.0
                    val words = mutableListOf<ParsedWord>()
                    val lineText = StringBuilder()
                    
                    parseSpanElements(pElement, words, lineText, startTime, endTime, false)
                    
                    if (lineText.isEmpty()) {
                        val directText = getDirectTextContent(pElement).trim()
                        if (directText.isNotEmpty()) {
                            lineText.append(directText)
                            words.add(
                                ParsedWord(
                                    text = directText,
                                    startTime = startTime,
                                    endTime = endTime,
                                    isBackground = false
                                )
                            )
                        }
                    }
                    
                    if (lineText.isNotEmpty()) {
                        lines.add(
                            ParsedLine(
                                text = lineText.toString().trim(),
                                startTime = startTime,
                                endTime = endTime,
                                words = words,
                                isBackground = false
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
        isBackground: Boolean
    ) {
        val childNodes = element.childNodes
        
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            
            when (node.nodeType) {
                Node.ELEMENT_NODE -> {
                    val childElement = node as Element
                    if (childElement.tagName.equals("span", ignoreCase = true)) {
                        // Check if this is a background vocal span
                        val role = childElement.getAttribute("role")
                        val isBgSpan = role == "x-bg" || isBackground
                        
                        val wordBegin = childElement.getAttribute("begin")
                        val wordEnd = childElement.getAttribute("end")
                        
                        // Check if this span has nested spans (for word-level timing within bg)
                        val nestedSpans = childElement.getElementsByTagName("span")
                        if (nestedSpans.length > 0 && hasDirectSpanChildren(childElement)) {
                            // Parse nested spans recursively
                            parseSpanElements(childElement, words, lineText, lineStartTime, lineEndTime, isBgSpan)
                        } else {
                            // This is a leaf span with text
                            val wordText = getDirectTextContent(childElement)
                            
                            if (wordText.isNotEmpty()) {
                                // Add space between words if needed
                                if (lineText.isNotEmpty() && !wordText.startsWith(" ") && 
                                    !lineText.endsWith(" ") && !lineText.endsWith("\n")) {
                                    lineText.append(" ")
                                }
                                lineText.append(wordText)
                                
                                val wordStartTime = if (wordBegin.isNotEmpty()) parseTime(wordBegin) else lineStartTime
                                val wordEndTime = if (wordEnd.isNotEmpty()) parseTime(wordEnd) else lineEndTime
                                
                                words.add(
                                    ParsedWord(
                                        text = wordText.trim(),
                                        startTime = wordStartTime,
                                        endTime = wordEndTime,
                                        isBackground = isBgSpan
                                    )
                                )
                            }
                        }
                    }
                }
                Node.TEXT_NODE -> {
                    val text = node.textContent
                    // Only add non-whitespace text that's not just spacing
                    if (text.isNotBlank()) {
                        if (lineText.isNotEmpty() && !text.startsWith(" ") && 
                            !lineText.endsWith(" ") && !lineText.endsWith("\n")) {
                            lineText.append(" ")
                        }
                        lineText.append(text.trim())
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
                if (childElement.tagName.equals("span", ignoreCase = true)) {
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
    
    /**
     * Convert parsed lines to enhanced LRC format with word-level timing
     * Format: [mm:ss.cc]text
     *         <word1:start1:end1|word2:start2:end2|...>
     */
    fun toLRC(lines: List<ParsedLine>): String {
        return buildString {
            lines.forEach { line ->
                val timeMs = (line.startTime * 1000).toLong()
                val minutes = timeMs / 60000
                val seconds = (timeMs % 60000) / 1000
                val centiseconds = (timeMs % 1000) / 10
                
                appendLine(String.format("[%02d:%02d.%02d]%s", minutes, seconds, centiseconds, line.text))
                
                // Add word-level timestamps as special format if available
                // Format: <word:startTime:endTime|word2:startTime2:endTime2|...>
                if (line.words.isNotEmpty() && line.words.any { it.startTime != it.endTime }) {
                    val wordsData = line.words
                        .filter { it.text.isNotBlank() }
                        .joinToString("|") { word ->
                            // Escape special characters in word text
                            val escapedText = word.text
                                .replace("|", "\\|")
                                .replace(":", "\\:")
                            "${escapedText}:${String.format("%.3f", word.startTime)}:${String.format("%.3f", word.endTime)}"
                        }
                    if (wordsData.isNotEmpty()) {
                        appendLine("<$wordsData>")
                    }
                }
            }
        }
    }
    
    /**
     * Parse TTML time format
     * Supports: "9.731", "1:23.456", "1:23:45.678", "00:01:23.456"
     */
    private fun parseTime(timeStr: String): Double {
        return try {
            val cleanTime = timeStr.trim()
            when {
                cleanTime.contains(":") -> {
                    val parts = cleanTime.split(":")
                    when (parts.size) {
                        2 -> {
                            // MM:SS.mmm format
                            val minutes = parts[0].toDoubleOrNull() ?: 0.0
                            val seconds = parts[1].toDoubleOrNull() ?: 0.0
                            minutes * 60 + seconds
                        }
                        3 -> {
                            // HH:MM:SS.mmm format
                            val hours = parts[0].toDoubleOrNull() ?: 0.0
                            val minutes = parts[1].toDoubleOrNull() ?: 0.0
                            val seconds = parts[2].toDoubleOrNull() ?: 0.0
                            hours * 3600 + minutes * 60 + seconds
                        }
                        else -> cleanTime.toDoubleOrNull() ?: 0.0
                    }
                }
                else -> cleanTime.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
}
