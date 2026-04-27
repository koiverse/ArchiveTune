/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */


package moe.koiverse.archivetune.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import java.util.Locale

fun String.isLocalMediaId(): Boolean {
    return runCatching {
        when (toUri().scheme?.lowercase(Locale.US)) {
            "content", "file", "android.resource" -> true
            else -> false
        }
    }.getOrDefault(false)
}

fun shareLocalAudio(
    context: Context,
    mediaId: String,
    mimeType: String? = null,
): Boolean {
    val uri = mediaId.toUri()
    val scheme = uri.scheme?.lowercase(Locale.US)
    if (scheme != "content" && scheme != "android.resource") return false

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType?.takeIf(String::isNotBlank) ?: "audio/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, null))
    return true
}