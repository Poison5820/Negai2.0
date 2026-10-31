package com.example.player

import android.media.audiofx.LoudnessEnhancer
import androidx.media3.common.C

/**
 * Hardware/DSP Audio Volume Boost controller (100%..200% -> 0..+1200 mB = +12 dB)
 * backed by Android's `LoudnessEnhancer` tied to ExoPlayer's `audioSessionId`.
 */
class AudioBoostController {

    private var enhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    var boostPercent: Int = 100
        private set

    fun onAudioSessionIdChanged(audioSessionId: Int) {
        if (audioSessionId == currentSessionId) return
        releaseEnhancer()
        currentSessionId = audioSessionId
        syncEnhancer()
    }

    fun setBoostPercent(percent: Int): Int {
        val clamped = percent.coerceIn(100, 200)
        boostPercent = clamped
        syncEnhancer()
        return clamped
    }

    private fun syncEnhancer() {
        val sessionId = currentSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) return

        val gainMillibels = percentToGainMillibels(boostPercent)
        if (gainMillibels <= 0) {
            runCatching { enhancer?.enabled = false }
            return
        }

        runCatching {
            val active = enhancer ?: LoudnessEnhancer(sessionId).also { enhancer = it }
            active.setTargetGain(gainMillibels)
            active.enabled = true
        }
    }

    fun releaseEnhancer() {
        runCatching {
            enhancer?.enabled = false
            enhancer?.release()
        }
        enhancer = null
    }

    companion object {
        /**
         * Maps 100%..200% linearly to 0..1200 millibels (0 dB .. +12 dB).
         */
        fun percentToGainMillibels(percent: Int): Int {
            val clamped = percent.coerceIn(100, 200)
            return (clamped - 100) * 12
        }
    }
}
