package moe.koiverse.archivetune.playback

import androidx.media3.common.TrackGroup
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
        return delegate
            .createTrackSelections(definitions, bandwidthMeter, mediaPeriodId, timeline)
            .map { trackSelection -> trackSelection?.let(::SafeTrackSelection) }
            .toTypedArray()
    }
}

private class SafeTrackSelection(
    private val delegate: ExoTrackSelection
) : ExoTrackSelection by delegate {
    override fun isTrackExcluded(index: Int, nowMs: Long): Boolean {
        if (index < 0 || index >= delegate.length()) {
            return false
        }
        return delegate.isTrackExcluded(index, nowMs)
    }

    override fun excludeTrack(index: Int, exclusionDurationMs: Long): Boolean {
        if (index < 0 || index >= delegate.length()) {
            return false
        }
        return delegate.excludeTrack(index, exclusionDurationMs)
    }
}
