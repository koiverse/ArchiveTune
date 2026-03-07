/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.models

import java.io.Serializable

data class PersistQueue(
    val title: String?,
    val items: List<MediaMetadata>,
    val mediaItemIndex: Int,
    val position: Long,
    val queueType: QueueType = QueueType.LIST,
    val queueData: QueueData? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

sealed class QueueType : Serializable {
    object LIST : QueueType() {
        private const val serialVersionUID = 1L
    }
    object YOUTUBE : QueueType() {
        private const val serialVersionUID = 1L
    }
    object YOUTUBE_ALBUM_RADIO : QueueType() {
        private const val serialVersionUID = 1L
    }
    object LOCAL_ALBUM_RADIO : QueueType() {
        private const val serialVersionUID = 1L
    }
}

sealed class QueueData : Serializable {
    data class YouTubeData(
        val endpoint: String,
        val continuation: String? = null
    ) : QueueData() {
        companion object {
            private const val serialVersionUID = 1L
        }
    }
    
    data class YouTubeAlbumRadioData(
        val playlistId: String,
        val albumSongCount: Int = 0,
        val continuation: String? = null,
        val firstTimeLoaded: Boolean = false
    ) : QueueData() {
        companion object {
            private const val serialVersionUID = 1L
        }
    }
    
    data class LocalAlbumRadioData(
        val albumId: String,
        val startIndex: Int = 0,
        val playlistId: String? = null,
        val continuation: String? = null,
        val firstTimeLoaded: Boolean = false
    ) : QueueData() {
        companion object {
            private const val serialVersionUID = 1L
        }
    }
}
