package com.example.subtitles

import androidx.compose.runtime.Immutable

enum class SubtitleAlignment {
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    MIDDLE_LEFT,
    MIDDLE_CENTER,
    MIDDLE_RIGHT,
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT;

    companion object {
        fun fromAssNumpad(code: Int): SubtitleAlignment = when (code) {
            1 -> BOTTOM_LEFT
            2 -> BOTTOM_CENTER
            3 -> BOTTOM_RIGHT
            4 -> MIDDLE_LEFT
            5 -> MIDDLE_CENTER
            6 -> MIDDLE_RIGHT
            7 -> TOP_LEFT
            8 -> TOP_CENTER
            9 -> TOP_RIGHT
            else -> BOTTOM_CENTER
        }
    }
}

@Immutable
data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val alignment: SubtitleAlignment = SubtitleAlignment.BOTTOM_CENTER,
    val isItalic: Boolean = false,
    val isBold: Boolean = false,
    val assStyleColorArgb: Long? = null
)

@Immutable
data class ParsedSubtitleDocument(
    val title: String,
    val cues: List<SubtitleCue>
) {
    /**
     * Queries active cues at [playerPositionMs] adjusted by [delayMs].
     * Positive [delayMs] shifts subtitles later in the video (`lookupMs = playerPositionMs - delayMs`).
     */
    fun activeCuesAt(playerPositionMs: Long, delayMs: Long): List<SubtitleCue> {
        if (cues.isEmpty()) return emptyList()
        val lookupMs = playerPositionMs - delayMs
        if (lookupMs < 0L) return emptyList()
        return cues.filter { cue -> lookupMs >= cue.startMs && lookupMs <= cue.endMs }
    }
}
