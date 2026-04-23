package moe.koiverse.archivetune.playback

import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.BandwidthMeter

internal class SafeTrackSelectionFactory(
    private val delegate: ExoTrackSelection.Factory = AdaptiveTrackSelection.Factory()
) : ExoTrackSelection.Factory {
    override fun createTrackSelections(
        definitions: Array<out ExoTrackSelection.Definition?>,
        bandwidthMeter: BandwidthMeter,
        mediaPeriodId: MediaSource.MediaPeriodId,
        timeline: Timeline
    ): Array<ExoTrackSelection?> {
        val selections = delegate.createTrackSelections(definitions, bandwidthMeter, mediaPeriodId, timeline)
        return selections.map { selection ->
            selection?.let { SafeExoTrackSelection(it) }
        }.toTypedArray()
    }
}

private class SafeExoTrackSelection(
    private val delegate: ExoTrackSelection
) : ExoTrackSelection by delegate {
    override fun isTrackExcluded(index: Int, nowMs: Long): Boolean {
        if (index < 0 || index >= length()) {
            return false
        }
        return delegate.isTrackExcluded(index, nowMs)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherDelegate = if (other is SafeExoTrackSelection) other.delegate else other
        return delegate == otherDelegate
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }
}
