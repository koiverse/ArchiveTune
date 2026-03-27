package moe.koiverse.archivetune.innertube.models

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeClientRequestConfigTest {
    @Test
    fun tvClientsUseYouTubeOriginAndReferer() {
        assertEquals(YouTubeClient.ORIGIN_YOUTUBE, YouTubeClient.TVHTML5.requestOrigin())
        assertEquals(YouTubeClient.REFERER_YOUTUBE_TV, YouTubeClient.TVHTML5.requestReferer())
    }

    @Test
    fun webMusicClientsUseYouTubeMusicOriginAndReferer() {
        assertEquals(YouTubeClient.ORIGIN_YOUTUBE_MUSIC, YouTubeClient.WEB_REMIX.requestOrigin())
        assertEquals(YouTubeClient.REFERER_YOUTUBE_MUSIC, YouTubeClient.WEB_REMIX.requestReferer())
    }
}
