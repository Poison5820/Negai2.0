package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PlaybackProgressEntity
import com.example.data.PlaybackResumeHelper
import com.example.model.VideoResolutionFormatter
import com.example.ui.components.LastPlayedFileButton
import com.example.ui.components.ResolutionBadgeChip
import com.example.ui.components.formatTimeMs
import com.example.ui.i18n.tr

@Composable
internal fun RecentTabContent(
    recentItems: List<PlaybackProgressEntity>,
    compactMode: Boolean = false,
    onResumeRecent: (PlaybackProgressEntity) -> Unit,
    onOpenSingleFile: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    lastPlayedFile: PlaybackProgressEntity? = null,
    resumePlaybackEnabled: Boolean = true,
    onOpenLastPlayed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (recentItems.isEmpty() && lastPlayedFile == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = tr("История просмотра пуста", "Watch history is empty"),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = tr(
                            "Откройте любую папку или отдельный видеофайл — прогресс воспроизведения сохраняется автоматически.",
                            "Open any folder or video file — playback progress is saved automatically."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FilledTonalButton(onClick = onOpenSingleFile) {
                        Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Открыть видеофайл", "Open video file"))
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .testTag("recent_lazy_list"),
        contentPadding = PaddingValues(
            horizontal = if (compactMode) 12.dp else 16.dp,
            vertical = if (compactMode) 6.dp else 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (compactMode) 8.dp else 10.dp)
    ) {
        item(key = "recent_header_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tr("Недавно просмотренные (${recentItems.size})", "Recently watched (${recentItems.size})"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (recentItems.isNotEmpty()) {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("recent_clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Очистить", "Clear"))
                    }
                }
            }
        }

        if (lastPlayedFile != null) {
            item(key = "recent_last_played_hero") {
                LastPlayedFileButton(
                    item = lastPlayedFile,
                    resumeEnabled = resumePlaybackEnabled,
                    compactMode = compactMode,
                    onClick = onOpenLastPlayed
                )
            }
        }

        items(
            items = recentItems,
            key = { it.videoUri }
        ) { item ->
            val progress = if (item.durationMs > 0) {
                (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            val resumePosMs = PlaybackResumeHelper.computeResumePositionMs(
                positionMs = item.positionMs,
                durationMs = item.durationMs,
                resumeEnabled = resumePlaybackEnabled
            )
            Card(
                shape = RoundedCornerShape(if (compactMode) 14.dp else 18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(if (compactMode) 14.dp else 18.dp))
                    .clickable { onResumeRecent(item) }
                    .testTag("recent_item_card")
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (compactMode) 12.dp else 16.dp,
                        vertical = if (compactMode) 10.dp else 14.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (compactMode) 38.dp else 44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = tr("Продолжить", "Resume"),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
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
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (resBadge != null) {
                                    ResolutionBadgeChip(
                                        badge = resBadge,
                                        compact = true,
                                        modifier = Modifier.testTag("recent_item_resolution_badge")
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.folderName} • ${formatTimeMs(item.positionMs)} / ${formatTimeMs(item.durationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (resumePosMs > 0L) {
                                    Text(
                                        text = tr(
                                            "с ${formatTimeMs(resumePosMs)}",
                                            "from ${formatTimeMs(resumePosMs)}"
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(if (compactMode) 6.dp else 10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }
        }
    }
}
