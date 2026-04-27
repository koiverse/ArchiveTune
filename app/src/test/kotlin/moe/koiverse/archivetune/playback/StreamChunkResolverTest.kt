package moe.koiverse.archivetune.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamChunkResolverTest {
    @Test
    fun clampsUnknownLengthToRemainingBytesNearEndOfStream() {
        val resolvedLength =
            resolveStreamChunkLength(
                requestedLength = -1L,
                position = 998L,
                knownContentLength = 1_000L,
                chunkLength = 512L,
            )

        assertEquals(2L, resolvedLength)
    }

    @Test
    fun keepsSmallerExplicitPlayerRequest() {
        val resolvedLength =
            resolveStreamChunkLength(
                requestedLength = 128L,
                position = 100L,
                knownContentLength = 1_000L,
                chunkLength = 512L,
            )

        assertEquals(128L, resolvedLength)
    }

    @Test
    fun returnsNullWhenChunkingCannotProducePositiveLength() {
        val resolvedLength =
            resolveStreamChunkLength(
                requestedLength = -1L,
                position = 1_000L,
                knownContentLength = 1_000L,
                chunkLength = 512L,
            )

        assertNull(resolvedLength)
    }
}
