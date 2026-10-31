package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtitles.SubtitleAlignment
import com.example.subtitles.SubtitleCue
import com.example.ui.theme.ManropeFontFamily
import java.util.Locale

@Composable
fun SubtitleOverlay(
    cues: List<SubtitleCue>,
    fontSizeSp: Float,
    colorArgb: Long,
    showBackground: Boolean,
    isInPipMode: Boolean,
    bottomControlsPadding: Boolean,
    modifier: Modifier = Modifier
) {
    if (cues.isEmpty()) return

    val effectiveSizeSp = if (isInPipMode) {
        (fontSizeSp * 0.52f).coerceIn(10f, 16f)
    } else {
        fontSizeSp
    }
    val textColor = Color(colorArgb)

    val topCues = cues.filter {
        it.alignment in setOf(
            SubtitleAlignment.TOP_LEFT,
            SubtitleAlignment.TOP_CENTER,
            SubtitleAlignment.TOP_RIGHT
        )
    }
    val midCues = cues.filter {
        it.alignment in setOf(
            SubtitleAlignment.MIDDLE_LEFT,
            SubtitleAlignment.MIDDLE_CENTER,
            SubtitleAlignment.MIDDLE_RIGHT
        )
    }
    val bottomCues = cues.filter {
        it.alignment in setOf(
            SubtitleAlignment.BOTTOM_LEFT,
            SubtitleAlignment.BOTTOM_CENTER,
            SubtitleAlignment.BOTTOM_RIGHT
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = if (isInPipMode) 8.dp else 24.dp,
                vertical = if (isInPipMode) 6.dp else 18.dp
            )
            .testTag("subtitle_overlay_container")
    ) {
        if (topCues.isNotEmpty()) {
            SubtitleGroupColumn(
                cues = topCues,
                fontSizeSp = effectiveSizeSp,
                textColor = textColor,
                showBackground = showBackground,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (bottomControlsPadding && !isInPipMode) 48.dp else 4.dp)
            )
        }

        if (midCues.isNotEmpty()) {
            SubtitleGroupColumn(
                cues = midCues,
                fontSizeSp = effectiveSizeSp,
                textColor = textColor,
                showBackground = showBackground,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (bottomCues.isNotEmpty()) {
            SubtitleGroupColumn(
                cues = bottomCues,
                fontSizeSp = effectiveSizeSp,
                textColor = textColor,
                showBackground = showBackground,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (bottomControlsPadding && !isInPipMode) 92.dp else 6.dp)
            )
        }
    }
}

@Composable
private fun SubtitleGroupColumn(
    cues: List<SubtitleCue>,
    fontSizeSp: Float,
    textColor: Color,
    showBackground: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        cues.forEach { cue ->
            val align = when (cue.alignment) {
                SubtitleAlignment.BOTTOM_LEFT,
                SubtitleAlignment.MIDDLE_LEFT,
                SubtitleAlignment.TOP_LEFT -> TextAlign.Start

                SubtitleAlignment.BOTTOM_RIGHT,
                SubtitleAlignment.MIDDLE_RIGHT,
                SubtitleAlignment.TOP_RIGHT -> TextAlign.End

                else -> TextAlign.Center
            }

            Box(
                modifier = if (showBackground) {
                    Modifier
                        .background(
                            color = Color(0xB806080E),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                } else {
                    Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                }
            ) {
                Text(
                    text = cue.text,
                    textAlign = align,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.28f).sp,
                        fontWeight = if (cue.isBold) FontWeight.Bold else FontWeight.SemiBold,
                        fontStyle = if (cue.isItalic) FontStyle.Italic else FontStyle.Normal,
                        color = textColor,
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(1.5f, 2.0f),
                            blurRadius = 5f
                        )
                    )
                )
            }
        }
    }
}

fun formatTimeMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatDelaySeconds(delayMs: Long): String {
    val sign = when {
        delayMs > 0L -> "+"
        delayMs < 0L -> "-"
        else -> ""
    }
    val absSec = kotlin.math.abs(delayMs) / 1000.0
    return String.format(Locale.US, "%s%.1f с", sign, absSec)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f ГБ", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.1f МБ", mb)
    }
}
