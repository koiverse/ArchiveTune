/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Markdown(
        content = markdown,
        modifier = modifier,
        colors = markdownColor(
            text = color,
            codeBackground = MaterialTheme.colorScheme.surfaceVariant,
        ),
        typography = markdownTypography(
            text = style
        )
    )
}
