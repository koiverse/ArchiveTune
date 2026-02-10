package moe.koiverse.archivetune.together

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TogetherCoreTest {
    @Test
    fun deepLink_roundTrip() {
        val join =
            TogetherJoinInfo(
                host = "192.168.1.20",
                port = 42117,
                sessionId = "sid123",
                sessionKey = "key456",
            )
        val encoded = TogetherLink.encode(join)
        val decoded = TogetherLink.decode(encoded)
        assertEquals(join, decoded)
    }

    @Test
    fun wsUrl_decode() {
        val decoded =
            TogetherLink.decode("ws://10.0.0.5:42117/together?sid=sid123&key=key456")
        assertNotNull(decoded)
        assertEquals("10.0.0.5", decoded!!.host)
        assertEquals(42117, decoded.port)
        assertEquals("sid123", decoded.sessionId)
        assertEquals("key456", decoded.sessionKey)
    }

    @Test
    fun protocol_serialization_roundTrip() {
        val hello =
            ClientHello(
                protocolVersion = TogetherProtocolVersion,
                sessionId = "sid",
                sessionKey = "key",
                clientId = "cid",
                displayName = "Alice",
            )
        val json = TogetherJson.json.encodeToString(TogetherMessage.serializer(), hello)
        val decoded = TogetherJson.json.decodeFromString(TogetherMessage.serializer(), json)
        assertTrue(decoded is ClientHello)
        assertEquals(hello, decoded)
    }

    @Test
    fun clock_estimates_offset_and_rtt() {
        val clock = TogetherClock()
        val snapshot =
            clock.onPong(
                sentAtElapsedMs = 1000L,
                receivedAtElapsedMs = 1200L,
                serverElapsedMs = 1150L,
            )
        assertTrue(snapshot.estimatedRttMs >= 0L)
    }
}
