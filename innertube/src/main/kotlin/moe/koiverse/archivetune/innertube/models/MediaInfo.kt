/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.innertube.models

data class MediaInfo(
    val videoId: String,
    val title: String? = null,
    val author: String? = null,
    val authorId: String? = null,
    val authorThumbnail: String? = null,
    val description: String? = null,
    val uploadDate: String? = null,
    val subscribers: String? = null,
    val viewCount: Int? = null,
    val like: Int? = null,
    val dislike: Int? = null,
)