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

    @Volatile
    var crossfadeDurationMs: Int = 0
        set(value) {
            field = value
            pendingCrossfadeDurationMs = value
        }

    @Volatile
    private var pendingCrossfadeDurationMs: Int = 0

    private var appliedCrossfadeDurationMs: Int = 0
    private var crossfadeFrames: Int = 0
    private var bytesPerFrame: Int = 0

    private var isEnding: Boolean = false
    private var shouldFadeInThisStream: Boolean = false
    private var shouldFadeInNextStream: Boolean = false
    private var framesOutputInStream: Long = 0L

    private var tailBufferBytes: ByteArray = ByteArray(0)
    private var tailCapacityBytes: Int = 0
    private var tailStartIndex: Int = 0
    private var tailSizeBytes: Int = 0

    private var outBufferBytes: ByteArray = ByteArray(0)
    private var outCapacityBytes: Int = 0
    private var outStartIndex: Int = 0
    private var outSizeBytes: Int = 0

    private var scratch: ByteArray = ByteArray(0)
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        bytesPerFrame = inputAudioFormat.channelCount * 2
        applyCrossfadeDurationIfNeeded(force = true)
        return outputAudioFormat
    }

    override fun isActive(): Boolean = pendingCrossfadeDurationMs > 0 && inputAudioFormat != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (inputAudioFormat == AudioFormat.NOT_SET) {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        applyCrossfadeDurationIfNeeded(force = false)

        val incomingBytes = inputBuffer.remaining()
        if (incomingBytes <= 0) return

        if (crossfadeFrames <= 0 || bytesPerFrame <= 0 || tailCapacityBytes <= 0) {
            ensureScratchCapacity(incomingBytes)
            inputBuffer.get(scratch, 0, incomingBytes)
            enqueueOutput(scratch, 0, incomingBytes)
            return
        }

        if (shouldFadeInNextStream) {
            shouldFadeInThisStream = true
            shouldFadeInNextStream = false
        }

        val bytesToOutput = computeBytesToOutput(incomingBytes)
        val alignedBytesToOutput = bytesToOutput - (bytesToOutput % bytesPerFrame)
        if (alignedBytesToOutput > 0) {
            ensureScratchCapacity(alignedBytesToOutput)
            drainTailToArray(scratch, 0, alignedBytesToOutput)
            if (shouldFadeInThisStream) {
                applyFadeInPcm16(
                    data = scratch,
                    offset = 0,
                    length = alignedBytesToOutput,
                    startFrameIndex = framesOutputInStream,
                )
            }
            framesOutputInStream += (alignedBytesToOutput / bytesPerFrame).toLong()
            if (shouldFadeInThisStream && framesOutputInStream >= crossfadeFrames.toLong()) {
                shouldFadeInThisStream = false
            }
            enqueueOutput(scratch, 0, alignedBytesToOutput)
        }

        appendInputToTail(inputBuffer)
    }

    override fun getOutput(): ByteBuffer {
        applyCrossfadeDurationIfNeeded(force = false)

        if (outSizeBytes > 0) {
            return dequeueOutputToByteBuffer(outSizeBytes)
        }

        if (!isEnding) {
            return AudioProcessor.EMPTY_BUFFER
        }

        if (crossfadeFrames <= 0 || bytesPerFrame <= 0 || tailSizeBytes <= 0) {
            tailStartIndex = 0
            tailSizeBytes = 0
            return AudioProcessor.EMPTY_BUFFER
        }

        val alignedTailBytes = tailSizeBytes - (tailSizeBytes % bytesPerFrame)
        if (alignedTailBytes <= 0) {
            tailStartIndex = 0
            tailSizeBytes = 0
            return AudioProcessor.EMPTY_BUFFER
        }

        ensureScratchCapacity(alignedTailBytes)
        drainTailToArray(scratch, 0, alignedTailBytes)

        val tailFrames = alignedTailBytes / bytesPerFrame
        applyFadeOutPcm16(
            data = scratch,
            offset = 0,
            frameCount = tailFrames,
            startFrameIndex = framesOutputInStream,
        )
        framesOutputInStream += tailFrames.toLong()

        shouldFadeInNextStream = true
        enqueueOutput(scratch, 0, alignedTailBytes)

        return dequeueOutputToByteBuffer(outSizeBytes)
    }

    override fun isEnded(): Boolean {
        return isEnding && outSizeBytes == 0 && tailSizeBytes == 0
    }

    override fun flush() {
        framesOutputInStream = 0L
        isEnding = false
        shouldFadeInThisStream = false
        tailStartIndex = 0
        tailSizeBytes = 0
        outStartIndex = 0
        outSizeBytes = 0
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
        pendingCrossfadeDurationMs = 0
        appliedCrossfadeDurationMs = 0
        crossfadeDurationMs = 0
        crossfadeFrames = 0
        bytesPerFrame = 0
        shouldFadeInNextStream = false
        tailBufferBytes = ByteArray(0)
        tailCapacityBytes = 0
        tailStartIndex = 0
        tailSizeBytes = 0
        outBufferBytes = ByteArray(0)
        outCapacityBytes = 0
        outStartIndex = 0
        outSizeBytes = 0
        scratch = ByteArray(0)
        outputBuffer = AudioProcessor.EMPTY_BUFFER
    }

    override fun queueEndOfStream() {
        isEnding = true
    }

    private fun applyCrossfadeDurationIfNeeded(force: Boolean) {
        val targetMs = pendingCrossfadeDurationMs
        if (!force && targetMs == appliedCrossfadeDurationMs) return

        appliedCrossfadeDurationMs = targetMs

        val newCrossfadeFrames =
            if (inputAudioFormat != AudioFormat.NOT_SET && bytesPerFrame > 0 && targetMs > 0) {
                (inputAudioFormat.sampleRate * targetMs) / 1000
            } else {
                0
            }

        crossfadeFrames = newCrossfadeFrames

        val newCapacityBytes =
            if (newCrossfadeFrames > 0 && bytesPerFrame > 0) {
                newCrossfadeFrames * bytesPerFrame
            } else {
                0
            }

        if (newCapacityBytes == tailCapacityBytes) return

        if (newCapacityBytes <= 0) {
            tailBufferBytes = ByteArray(0)
            tailCapacityBytes = 0
            tailStartIndex = 0
            tailSizeBytes = 0
            shouldFadeInThisStream = false
            shouldFadeInNextStream = false
            return
        }

        val newBuffer = ByteArray(newCapacityBytes)
        val bytesToCopy = min(tailSizeBytes, newCapacityBytes)

        if (bytesToCopy > 0 && tailCapacityBytes > 0) {
            val oldCapacity = tailCapacityBytes
            val tailEndExclusive = (tailStartIndex + tailSizeBytes) % oldCapacity
            val copyStartIndexInOld =
                ((tailEndExclusive - bytesToCopy) % oldCapacity + oldCapacity) % oldCapacity

            val firstChunk = min(bytesToCopy, oldCapacity - copyStartIndexInOld)
            System.arraycopy(tailBufferBytes, copyStartIndexInOld, newBuffer, 0, firstChunk)
            val remaining = bytesToCopy - firstChunk
            if (remaining > 0) {
                System.arraycopy(tailBufferBytes, 0, newBuffer, firstChunk, remaining)
            }
        }

        tailBufferBytes = newBuffer
        tailCapacityBytes = newCapacityBytes
        tailStartIndex = 0
        tailSizeBytes = bytesToCopy
    }

    private fun computeBytesToOutput(incomingBytes: Int): Int {
        val total = tailSizeBytes + incomingBytes
        return (total - tailCapacityBytes).coerceAtLeast(0)
    }

    private fun drainTailToArray(target: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        val capacity = tailCapacityBytes
        val firstChunk = min(length, capacity - tailStartIndex)
        System.arraycopy(tailBufferBytes, tailStartIndex, target, offset, firstChunk)
        val remaining = length - firstChunk
        if (remaining > 0) {
            System.arraycopy(tailBufferBytes, 0, target, offset + firstChunk, remaining)
        }
        tailStartIndex = (tailStartIndex + length) % capacity
        tailSizeBytes -= length
    }

    private fun appendInputToTail(inputBuffer: ByteBuffer) {
        val capacity = tailCapacityBytes
        if (capacity <= 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        val bytesToAppend = inputBuffer.remaining()
        if (bytesToAppend <= 0) return

        var endIndex = (tailStartIndex + tailSizeBytes) % capacity
        var remaining = bytesToAppend
        while (remaining > 0) {
            val chunk = min(remaining, capacity - endIndex)
            inputBuffer.get(tailBufferBytes, endIndex, chunk)
            tailSizeBytes += chunk
            remaining -= chunk
            endIndex = (endIndex + chunk) % capacity
        }
    }

    private fun enqueueOutput(source: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        ensureOutputCapacity(length)
        val capacity = outCapacityBytes
        var endIndex = (outStartIndex + outSizeBytes) % capacity
        var remaining = length
        var srcOffset = offset
        while (remaining > 0) {
            val chunk = min(remaining, capacity - endIndex)
            System.arraycopy(source, srcOffset, outBufferBytes, endIndex, chunk)
            srcOffset += chunk
            remaining -= chunk
            endIndex = (endIndex + chunk) % capacity
        }
        outSizeBytes += length
    }

    private fun dequeueOutputToByteBuffer(maxBytes: Int): ByteBuffer {
        val bytesToRead = min(outSizeBytes, maxBytes).coerceAtLeast(0)
        if (bytesToRead <= 0) return AudioProcessor.EMPTY_BUFFER

        val out = replaceOutputBuffer(bytesToRead)
        val capacity = outCapacityBytes
        val firstChunk = min(bytesToRead, capacity - outStartIndex)
        out.put(outBufferBytes, outStartIndex, firstChunk)
        val remaining = bytesToRead - firstChunk
        if (remaining > 0) {
            out.put(outBufferBytes, 0, remaining)
        }
        out.flip()

        outStartIndex = (outStartIndex + bytesToRead) % capacity
        outSizeBytes -= bytesToRead
        if (outSizeBytes == 0) {
            outStartIndex = 0
        }

        return out
    }

    private fun ensureOutputCapacity(additionalBytes: Int) {
        val required = outSizeBytes + additionalBytes
        if (outCapacityBytes >= required) return

        val newCapacity =
            when {
                outCapacityBytes <= 0 -> maxOf(16_384, required)
                else -> maxOf(outCapacityBytes * 2, required)
            }

        val newBuffer = ByteArray(newCapacity)
        if (outSizeBytes > 0) {
            val firstChunk = min(outSizeBytes, outCapacityBytes - outStartIndex)
            System.arraycopy(outBufferBytes, outStartIndex, newBuffer, 0, firstChunk)
            val remaining = outSizeBytes - firstChunk
            if (remaining > 0) {
                System.arraycopy(outBufferBytes, 0, newBuffer, firstChunk, remaining)
            }
        }

        outBufferBytes = newBuffer
        outCapacityBytes = newCapacity
        outStartIndex = 0
    }

    private fun applyFadeInPcm16(
        data: ByteArray,
        offset: Int,
        length: Int,
        startFrameIndex: Long,
    ) {
        if (crossfadeFrames <= 0 || bytesPerFrame <= 0) return
        val frames = length / bytesPerFrame
        var i = 0
        while (i < frames) {
            val globalFrame = startFrameIndex + i
            val gain =
                if (globalFrame < crossfadeFrames.toLong()) {
                    globalFrame.toFloat() / crossfadeFrames.toFloat()
                } else {
                    1f
                }
            scaleFramePcm16(data, offset + i * bytesPerFrame, gain)
            i++
        }
    }

    private fun applyFadeOutPcm16(
        data: ByteArray,
        offset: Int,
        frameCount: Int,
        startFrameIndex: Long,
    ) {
        if (frameCount <= 0 || bytesPerFrame <= 0) return

        val denom = (frameCount - 1).coerceAtLeast(1).toFloat()
        var i = 0
        while (i < frameCount) {
            val fadeOutGain =
                if (frameCount <= 1) {
                    0f
                } else {
                    1f - (i.toFloat() / denom)
                }

            val globalFrame = startFrameIndex + i
            val fadeInGain =
                if (shouldFadeInThisStream && crossfadeFrames > 0 && globalFrame < crossfadeFrames.toLong()) {
                    globalFrame.toFloat() / crossfadeFrames.toFloat()
                } else {
                    1f
                }

            scaleFramePcm16(data, offset + i * bytesPerFrame, fadeInGain * fadeOutGain)
            i++
        }
    }

    private fun scaleFramePcm16(data: ByteArray, frameOffset: Int, gain: Float) {
        if (gain == 1f) return
        var byteIndex = frameOffset
        val frameEnd = frameOffset + bytesPerFrame
        while (byteIndex < frameEnd) {
            val lo = data[byteIndex].toInt() and 0xFF
            val hi = data[byteIndex + 1].toInt()
            val sample = ((hi shl 8) or lo).toShort().toInt()
            val scaled = (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            data[byteIndex] = (scaled and 0xFF).toByte()
            data[byteIndex + 1] = ((scaled shr 8) and 0xFF).toByte()
            byteIndex += 2
        }
    }

    private fun ensureScratchCapacity(size: Int) {
        if (scratch.size < size) {
            scratch = ByteArray(size)
        }
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
