/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Custom audio processor that applies crossfade between tracks
 */
@UnstableApi
class CrossfadeAudioProcessor : AudioProcessor {
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var isEnding = false
    
    @Volatile
    var crossfadeDurationMs: Int = 0
        set(value) {
            field = value
            updateCrossfadeSamples()
        }
    
    private var crossfadeSamples = 0
    private var currentSample = 0
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    
    private fun updateCrossfadeSamples() {
        crossfadeSamples =
            if (inputAudioFormat != AudioFormat.NOT_SET && crossfadeDurationMs > 0) {
                (inputAudioFormat.sampleRate * crossfadeDurationMs) / 1000
            } else {
                0
            }
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioFormat.NOT_SET
        }
        
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        updateCrossfadeSamples()
        
        return outputAudioFormat
    }

    override fun isActive(): Boolean = crossfadeDurationMs > 0 && inputAudioFormat != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (inputAudioFormat == AudioFormat.NOT_SET) {
            return
        }

        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        
        buffer = replaceOutputBuffer(remaining)

        if (isEnding && crossfadeDurationMs > 0 && crossfadeSamples > 0 && currentSample < crossfadeSamples) {
            val samplesThisPass = min(crossfadeSamples - currentSample, remaining / 2)
            for (i in 0 until samplesThisPass) {
                val sample = inputBuffer.short
                val factor = 1.0f - (currentSample + i).toFloat() / crossfadeSamples
                val fadedSample = (sample * factor).toInt().toShort()
                buffer.putShort(fadedSample)
                currentSample++
            }
            if (inputBuffer.hasRemaining()) {
                buffer.put(inputBuffer)
            }
        } else {
            buffer.put(inputBuffer)
        }
        
        buffer.flip()
    }

    override fun getOutput(): ByteBuffer {
        val output = buffer
        buffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean {
        return isEnding && buffer === AudioProcessor.EMPTY_BUFFER
    }

    override fun flush() {
        buffer = AudioProcessor.EMPTY_BUFFER
        currentSample = 0
        isEnding = false
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }

    override fun queueEndOfStream() {
        isEnding = true
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (outputBuffer.capacity() < size) {
            outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }
        return outputBuffer
    }
}
