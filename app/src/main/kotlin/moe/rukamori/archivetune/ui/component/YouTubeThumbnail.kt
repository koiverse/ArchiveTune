package moe.rukamori.archivetune.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import moe.rukamori.archivetune.R

private val ytVideoIdRegex = Regex("/vi/([^/]+)/")

@Composable
fun YTFallbackImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    widthPx: Int? = null,
    heightPx: Int? = null,
) {
    val videoId = url?.let { ytVideoIdRegex.find(it)?.groupValues?.getOrNull(1) }
    if (videoId != null) {
        val urls = remember(videoId) {
            listOf(
                "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg",
                "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            )
        }
        var urlIndex by remember { mutableStateOf(0) }

        if (urlIndex < urls.size) {
            val request = remember(urls[urlIndex], widthPx, heightPx) {
                ImageRequest.Builder(LocalContext.current)
                    .data(urls[urlIndex])
                    .allowHardware(true)
                    .apply {
                        if (widthPx != null && heightPx != null) {
                            size(widthPx, heightPx)
                        }
                    }
                    .listener(onError = { _, _ -> urlIndex++ })
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier,
            )
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.about_splash),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    } else {
        val request = remember(url, widthPx, heightPx) {
            ImageRequest.Builder(LocalContext.current)
                .data(url)
                .allowHardware(true)
                .apply {
                    if (widthPx != null && heightPx != null) {
                        size(widthPx, heightPx)
                    }
                }
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}
