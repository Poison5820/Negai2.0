package com.example.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.min

/**
 * Real-time PCM AudioProcessor for adjusting audio synchronization/delay in range [-5000, +5000] ms.
 * - Positive delay (> 0 ms): Delays audio relative to video by inserting leading PCM silence frames.
 * - Negative delay (< 0 ms): Advances audio relative to video by skipping leading PCM frames on flush/change.
 */
@OptIn(UnstableApi::class)
class AudioDelayProcessor : BaseAudioProcessor() {

    companion object {
        const val MIN_DELAY_MS = -5000L
        const val MAX_DELAY_MS = 5000L

        fun clampDelayMs(delayMs: Long): Long = delayMs.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)

        fun formatDelayLabel(delayMs: Long): String {
            return when {
                delayMs > 0L -> "+${delayMs} мс"
                delayMs < 0L -> "${delayMs} мс"
                else -> "0 мс"
            }
        }
    }

    @Volatile
    var delayMs: Long = 0L
        private set

    private var appliedDelayMs: Long = 0L
    private var pendingSilenceBytes: Long = 0L
    private var pendingSkipBytes: Long = 0L

    @Synchronized
    fun setDelayMs(newDelayMs: Long) {
        val clamped = clampDelayMs(newDelayMs)
        val deltaMs = clamped - appliedDelayMs
        delayMs = clamped
        appliedDelayMs = clamped
        if (inputAudioFormat != AudioFormat.NOT_SET && deltaMs != 0L) {
            applyDeltaBytes(deltaMs)
        }
    }

    @Synchronized
    fun forceResyncFlush() {
        recalculateFromScratch()
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        return when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT,
            C.ENCODING_PCM_8BIT,
            C.ENCODING_PCM_24BIT,
            C.ENCODING_PCM_32BIT,
            C.ENCODING_PCM_FLOAT -> inputAudioFormat
            else -> AudioFormat.NOT_SET
        }
    }

    @Synchronized
    override fun queueInput(inputBuffer: ByteBuffer) {
        var remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val frameSize = inputAudioFormat.bytesPerFrame.coerceAtLeast(1)

        // 1. If we have pending skip bytes (negative delay adjustment), drop frames cleanly aligned to frameSize
        if (pendingSkipBytes > 0L) {
            val bytesToSkip = min(remaining.toLong(), pendingSkipBytes).toInt()
            val alignedSkip = (bytesToSkip / frameSize) * frameSize
            if (alignedSkip > 0) {
                inputBuffer.position(inputBuffer.position() + alignedSkip)
                pendingSkipBytes = (pendingSkipBytes - alignedSkip).coerceAtLeast(0L)
                remaining = inputBuffer.remaining()
                if (remaining == 0) return
            }
        }

        // 2. If we have pending silence bytes (positive delay adjustment), prepend zeroed PCM frames
        val silenceChunk = if (pendingSilenceBytes > 0L) {
            val maxSilenceThisTurn = min(pendingSilenceBytes, 32768L).toInt()
            ((maxSilenceThisTurn / frameSize) * frameSize).coerceAtLeast(0)
        } else {
            0
        }

        val outBuffer = replaceOutputBuffer(silenceChunk + remaining)
        if (silenceChunk > 0) {
            for (i in 0 until silenceChunk) {
                outBuffer.put(0.toByte())
            }
            pendingSilenceBytes = (pendingSilenceBytes - silenceChunk).coerceAtLeast(0L)
        }
        outBuffer.put(inputBuffer)
        outBuffer.flip()
    }

    @Synchronized
    override fun onFlush() {
        recalculateFromScratch()
    }

    @Synchronized
    override fun onReset() {
        pendingSilenceBytes = 0L
        pendingSkipBytes = 0L
        appliedDelayMs = delayMs
    }

    private fun recalculateFromScratch() {
        appliedDelayMs = delayMs
        if (inputAudioFormat == AudioFormat.NOT_SET || inputAudioFormat.sampleRate <= 0) {
            pendingSilenceBytes = 0L
            pendingSkipBytes = 0L
            return
        }
        val bytesForDelay = msToAlignedBytes(abs(delayMs))
        if (delayMs > 0L) {
            pendingSilenceBytes = bytesForDelay
            pendingSkipBytes = 0L
        } else if (delayMs < 0L) {
            pendingSilenceBytes = 0L
            pendingSkipBytes = bytesForDelay
        } else {
            pendingSilenceBytes = 0L
            pendingSkipBytes = 0L
        }
    }

    private fun applyDeltaBytes(deltaMs: Long) {
        val deltaBytes = msToAlignedBytes(abs(deltaMs))
        if (deltaMs > 0L) {
            if (pendingSkipBytes >= deltaBytes) {
                pendingSkipBytes -= deltaBytes
            } else {
                val leftover = deltaBytes - pendingSkipBytes
                pendingSkipBytes = 0L
                pendingSilenceBytes += leftover
            }
        } else if (deltaMs < 0L) {
            if (pendingSilenceBytes >= deltaBytes) {
                pendingSilenceBytes -= deltaBytes
            } else {
                val leftover = deltaBytes - pendingSilenceBytes
                pendingSilenceBytes = 0L
                pendingSkipBytes += leftover
            }
        }
    }

    private fun msToAlignedBytes(ms: Long): Long {
        if (inputAudioFormat == AudioFormat.NOT_SET || inputAudioFormat.sampleRate <= 0) return 0L
        val frameSize = inputAudioFormat.bytesPerFrame.coerceAtLeast(1).toLong()
        val frames = (ms * inputAudioFormat.sampleRate.toLong()) / 1000L
        return frames * frameSize
    }
}
