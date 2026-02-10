package moe.koiverse.archivetune.together

import androidx.compose.runtime.Immutable
import kotlin.math.absoluteValue

@Immutable
data class TogetherClockSnapshot(
    val estimatedOffsetMs: Long = 0L,
    val estimatedRttMs: Long = 0L,
)

class TogetherClock {
    private var offsetMs: Double = 0.0
    private var rttMs: Double = 0.0

    fun onPong(
        sentAtElapsedMs: Long,
        receivedAtElapsedMs: Long,
        serverElapsedMs: Long,
    ): TogetherClockSnapshot {
        val rtt = (receivedAtElapsedMs - sentAtElapsedMs).coerceAtLeast(0L)
        val mid = sentAtElapsedMs + rtt / 2L
        val newOffset = (serverElapsedMs - mid).toDouble()

        val rttAlpha = 0.15
        val offsetAlpha = if (newOffset.absoluteValue > 1500.0) 0.6 else 0.2

        rttMs = if (rttMs == 0.0) rtt.toDouble() else rttMs + (rtt - rttMs) * rttAlpha
        offsetMs = if (offsetMs == 0.0) newOffset else offsetMs + (newOffset - offsetMs) * offsetAlpha

        return snapshot()
    }

    fun snapshot(): TogetherClockSnapshot =
        TogetherClockSnapshot(
            estimatedOffsetMs = offsetMs.toLong(),
            estimatedRttMs = rttMs.toLong(),
        )
}
