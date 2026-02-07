/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.utils

import java.text.DecimalFormat
import kotlin.math.absoluteValue

fun formatFileSize(sizeBytes: Long): String {
    val prefix = if (sizeBytes < 0) "-" else ""
    val absBytes = sizeBytes.absoluteValue.toDouble()
    
    return when {
        absBytes < 1024 -> "$prefix${absBytes.toLong()} B"
        absBytes < 1024 * 1024 -> {
            val kb = absBytes / 1024
            "$prefix${DecimalFormat("#.#").format(kb)} KB"
        }
        absBytes < 1024 * 1024 * 1024 -> {
            val mb = absBytes / (1024 * 1024)
            "$prefix${DecimalFormat("#.#").format(mb)} MB"
        }
        absBytes < 1024L * 1024 * 1024 * 1024 -> {
            val gb = absBytes / (1024 * 1024 * 1024)
            "$prefix${DecimalFormat("#.##").format(gb)} GB"
        }
        else -> {
            val tb = absBytes / (1024L * 1024 * 1024 * 1024)
            "$prefix${DecimalFormat("#.##").format(tb)} TB"
        }
    }
}

fun numberFormatter(n: Int) =
    DecimalFormat("#,###")
        .format(n)
        .replace(",", ".")