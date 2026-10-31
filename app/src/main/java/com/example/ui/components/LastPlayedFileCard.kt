package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlaybackProgressEntity
import com.example.data.PlaybackResumeHelper
import com.example.model.VideoResolutionFormatter

@Composable
fun LastPlayedFileButton(
    item: PlaybackProgressEntity,
    resumeEnabled: Boolean,
    compactMode: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resumePosMs = PlaybackResumeHelper.computeResumePositionMs(
        positionMs = item.positionMs,
        durationMs = item.durationMs,
        resumeEnabled = resumeEnabled
    )
    val headerLabel = if (resumePosMs > 0L) {
        stringResource(R.string.last_played_continue_from, formatTimeMs(resumePosMs))
    } else {
        stringResource(R.string.last_played_open_file)
    }

    val progressFraction = if (item.durationMs > 0L) {
        (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val subtitleText = buildString {
        if (item.folderName.isNotBlank()) {
            append(item.folderName)
        }
        if (item.durationMs > 0L) {
            if (isNotEmpty()) append(" • ")
            append("${formatTimeMs(item.positionMs)} / ${formatTimeMs(item.durationMs)}")
        } else if (item.positionMs > 0L) {
            if (isNotEmpty()) append(" • ")
            append(formatTimeMs(item.positionMs))
        }
    }

    val corner = if (compactMode) 15.dp else 18.dp
    Card(
        shape = RoundedCornerShape(corner),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .clickable(onClick = onClick)
            .testTag("open_last_played_button")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (compactMode) 12.dp else 14.dp,
                        vertical = if (compactMode) 9.dp else 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compactMode) 10.dp else 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compactMode) 38.dp else 44.dp)
                        .clip(RoundedCornerShape(if (compactMode) 11.dp else 13.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = headerLabel,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(if (compactMode) 22.dp else 24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = headerLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val resBadge = VideoResolutionFormatter.formatBadge(
                        width = item.videoWidth,
                        height = item.videoHeight,
                        fileName = item.title
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (resBadge != null) {
                            ResolutionBadgeChip(
                                badge = resBadge,
                                compact = true,
                                modifier = Modifier.testTag("last_played_resolution_badge")
                            )
                        }
                    }
                    if (subtitleText.isNotEmpty()) {
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (progressFraction > 0.01f) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}
