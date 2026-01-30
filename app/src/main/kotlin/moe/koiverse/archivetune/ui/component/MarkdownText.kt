package moe.koiverse.archivetune.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val lines = text.lines()
    
    Column(modifier = modifier.padding(end = 8.dp)) {
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# ").trim(),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## ").trim(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### ").trim(),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                line.startsWith("- ") -> {
                    Row {
                        Text("• ", style = MaterialTheme.typography.bodyLarge)
                        val bulletText = line.removePrefix("- ").trim()
                        val annotatedText = parseInlineMarkdown(bulletText)
                        if (annotatedText.getStringAnnotations("URL", 0, annotatedText.length).isNotEmpty()) {
                            ClickableText(
                                text = annotatedText,
                                style = MaterialTheme.typography.bodyLarge,
                                onClick = { offset ->
                                    annotatedText.getStringAnnotations("URL", offset, offset)
                                        .firstOrNull()?.let { annotation ->
                                            uriHandler.openUri(annotation.item)
                                        }
                                }
                            )
                        } else {
                            Text(
                                text = annotatedText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                line.isBlank() -> {
                    // Skip blank lines, spacing is handled by Spacer below
                }
                else -> {
                    val annotatedText = parseInlineMarkdown(line)
                    if (annotatedText.getStringAnnotations("URL", 0, annotatedText.length).isNotEmpty()) {
                        ClickableText(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyMedium,
                            onClick = { offset ->
                                annotatedText.getStringAnnotations("URL", offset, offset)
                                    .firstOrNull()?.let { annotation ->
                                        uriHandler.openUri(annotation.item)
                                    }
                            }
                        )
                    } else {
                        Text(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentText = text
        var currentIndex = 0
        
        // Process HTML tags first
        currentText = processHtmlTags(currentText, this)
        
        // Reset for markdown processing
        currentIndex = 0
        val processedText = currentText
        
        while (currentIndex < processedText.length) {
            when {
                // Bold with **text**
                processedText.substring(currentIndex).startsWith("**") -> {
                    val endIndex = processedText.indexOf("**", currentIndex + 2)
                    if (endIndex != -1) {
                        val boldText = processedText.substring(currentIndex + 2, endIndex)
                        val start = length
                        append(boldText)
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                        currentIndex = endIndex + 2
                    } else {
                        append(processedText[currentIndex])
                        currentIndex++
                    }
                }
                // Bold with __text__
                processedText.substring(currentIndex).startsWith("__") -> {
                    val endIndex = processedText.indexOf("__", currentIndex + 2)
                    if (endIndex != -1) {
                        val boldText = processedText.substring(currentIndex + 2, endIndex)
                        val start = length
                        append(boldText)
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                        currentIndex = endIndex + 2
                    } else {
                        append(processedText[currentIndex])
                        currentIndex++
                    }
                }
                // Italic with *text* (but not **text**)
                processedText.substring(currentIndex).startsWith("*") && 
                !processedText.substring(currentIndex).startsWith("**") -> {
                    val endIndex = processedText.indexOf("*", currentIndex + 1)
                    if (endIndex != -1 && endIndex != currentIndex + 1) {
                        val italicText = processedText.substring(currentIndex + 1, endIndex)
                        val start = length
                        append(italicText)
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                        currentIndex = endIndex + 1
                    } else {
                        append(processedText[currentIndex])
                        currentIndex++
                    }
                }
                // Italic with _text_ (but not __text__)
                processedText.substring(currentIndex).startsWith("_") && 
                !processedText.substring(currentIndex).startsWith("__") -> {
                    val endIndex = processedText.indexOf("_", currentIndex + 1)
                    if (endIndex != -1 && endIndex != currentIndex + 1) {
                        val italicText = processedText.substring(currentIndex + 1, endIndex)
                        val start = length
                        append(italicText)
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                        currentIndex = endIndex + 1
                    } else {
                        append(processedText[currentIndex])
                        currentIndex++
                    }
                }
                // Inline code with `code`
                processedText.substring(currentIndex).startsWith("`") -> {
                    val endIndex = processedText.indexOf("`", currentIndex + 1)
                    if (endIndex != -1) {
                        val codeText = processedText.substring(currentIndex + 1, endIndex)
                        val start = length
                        append(codeText)
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color.Gray.copy(alpha = 0.2f)
                            ), 
                            start, 
                            length
                        )
                        currentIndex = endIndex + 1
                    } else {
                        append(processedText[currentIndex])
                        currentIndex++
                    }
                }
                // Links with [text](url)
                processedText.substring(currentIndex).startsWith("[") -> {
                    val closeBracket = processedText.indexOf("]", currentIndex + 1)
                    val openParen = if (closeBracket != -1) processedText.indexOf("(", closeBracket + 1) else -1
                    val closeParen = if (openParen != -1) processedText.indexOf(")", openParen + 1) else -1
                    
                    if (closeBracket != -1 && openParen == closeBracket + 1 && closeParen != -1) {
                        val linkText = processedText.substring(currentIndex + 1, closeBracket)
                        val linkUrl = processedText.substring(openParen + 1, closeParen)
                        val start = length
                        append(linkText)
                        addStyle(
                            SpanStyle(
                                color = Color.Blue,
                                textDecoration = TextDecoration.Underline
                            ), 
                            start, 
                            length
                        )
                        addStringAnnotation("URL", linkUrl, start, length)
                        currentIndex = closeParen + 1
                    } else {
                        append(processedText[currentIndex])
                        currentIndex++
                    }
                }
                else -> {
                    append(processedText[currentIndex])
                    currentIndex++
                }
            }
        }
    }
}

private fun processHtmlTags(text: String, builder: AnnotatedString.Builder): String {
    // For now, we'll do basic HTML tag removal and let markdown handle the formatting
    // This is a simplified approach - a full HTML parser would be more robust
    return text
        .replace("<b>", "**").replace("</b>", "**")
        .replace("<strong>", "**").replace("</strong>", "**")
        .replace("<i>", "*").replace("</i>", "*")
        .replace("<em>", "*").replace("</em>", "*")
        .replace("<code>", "`").replace("</code>", "`")
}
