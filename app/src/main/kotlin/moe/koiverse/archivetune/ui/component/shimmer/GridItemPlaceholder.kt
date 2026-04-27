/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.ui.component.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import moe.koiverse.archivetune.constants.GridThumbnailCornerRadius
import moe.koiverse.archivetune.constants.GridThumbnailHeight

@Composable
fun GridItemPlaceHolder(
    modifier: Modifier = Modifier,
    thumbnailShape: Shape = RoundedCornerShape(GridThumbnailCornerRadius),
    fillMaxWidth: Boolean = false,
) {
    Column(
        modifier =
        if (fillMaxWidth) {
            modifier
                .padding(12.dp)
                .fillMaxWidth()
        } else {
            modifier
                .padding(12.dp)
                .width(GridThumbnailHeight)
        },
    ) {
        Spacer(
            modifier =
            if (fillMaxWidth) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.height(GridThumbnailHeight)
            }.aspectRatio(1f)
                .clip(thumbnailShape)
                .background(MaterialTheme.colorScheme.onSurface),
        )

        Spacer(modifier = Modifier.height(6.dp))

        TextPlaceholder()

        TextPlaceholder()
    }
}
