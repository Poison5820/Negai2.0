package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.PlayerControlsTheme
import com.example.model.VideoChapter

/**
 * Compact badge displaying video resolution ("4K", "1440p", "1080p", "720p", etc.)
 * next to video items in folder lists, player playlists, and recent history.
 */
@Composable
fun ResolutionBadgeChip(
    badge: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val normalized = badge.uppercase()
    val isUltraHd = normalized == "8K" || normalized == "4K"
    val isHighDef = normalized == "1440P" || normalized == "1080P"

    val containerColor = when {
        isUltraHd -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f)
        isHighDef -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = when {
        isUltraHd -> MaterialTheme.colorScheme.primary
        isHighDef -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderStroke = when {
        isUltraHd -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
        isHighDef -> BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(if (compact) 5.dp else 6.dp),
        border = borderStroke,
        modifier = modifier
    ) {
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isUltraHd || isHighDef) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(
                horizontal = if (compact) 5.dp else 6.dp,
                vertical = if (compact) 1.5.dp else 2.dp
            )
        )
    }
}

private fun rewindIconForStep(stepSeconds: Int): ImageVector = when (stepSeconds) {
    5 -> Icons.Default.Replay5
    10 -> Icons.Default.Replay10
    30 -> Icons.Default.Replay30
    else -> Icons.Default.FastRewind
}

private fun forwardIconForStep(stepSeconds: Int): ImageVector = when (stepSeconds) {
    5 -> Icons.Default.Forward5
    10 -> Icons.Default.Forward10
    30 -> Icons.Default.Forward30
    else -> Icons.Default.FastForward
}

/**
 * Center player transport buttons styled according to [PlayerControlsTheme].
 * Preserves >=48dp touch targets and accessibility descriptions across all 5 themes.
 */
@Composable
fun ThemedPlayerCenterTransport(
    controlsTheme: PlayerControlsTheme,
    isPlaying: Boolean,
    hasPlaylist: Boolean,
    hasNextEpisode: Boolean,
    seekStepSeconds: Int,
    onPreviousEpisode: () -> Unit,
    onRewind: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onForward: () -> Unit,
    onNextEpisode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val secondary = MaterialTheme.colorScheme.secondary
    val rewindIcon = rewindIconForStep(seekStepSeconds)
    val forwardIcon = forwardIconForStep(seekStepSeconds)
    val playPauseIcon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
    val playPauseDesc = if (isPlaying) "Пауза" else "Воспроизвести"
    val rewindDesc = "Перемотать назад на $seekStepSeconds секунд"
    val forwardDesc = "Перемотать вперёд на $seekStepSeconds секунд"

    val frostedSideBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.22f),
            primary.copy(alpha = 0.14f)
        )
    )
    val frostedMainBrush = Brush.linearGradient(
        listOf(
            primary.copy(alpha = 0.88f),
            secondary.copy(alpha = 0.78f)
        )
    )

    when (controlsTheme) {
        PlayerControlsTheme.CLASSIC_OLED -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TransportButtonBox(
                    onClick = onPreviousEpisode,
                    enabled = hasPlaylist,
                    contentDescription = "Предыдущая серия",
                    testTag = "player_prev_episode_button",
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(frostedSideBrush, CircleShape)
                            .border(
                                1.dp,
                                Color.White.copy(alpha = if (hasPlaylist) 0.34f else 0.14f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = null,
                            tint = if (hasPlaylist) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                TransportButtonBox(
                    onClick = onRewind,
                    enabled = true,
                    contentDescription = rewindDesc,
                    testTag = "player_rewind_10_button",
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(frostedSideBrush, CircleShape)
                            .border(1.dp, primary.copy(alpha = 0.42f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = rewindIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(primary.copy(alpha = 0.34f), Color.Transparent)
                                ),
                                CircleShape
                            )
                    )
                    TransportButtonBox(
                        onClick = onTogglePlayPause,
                        enabled = true,
                        contentDescription = playPauseDesc,
                        testTag = "player_play_pause_button",
                        modifier = Modifier
                            .size(54.dp)
                            .background(frostedMainBrush, CircleShape)
                            .border(1.2.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                    ) {
                        Icon(
                            imageVector = playPauseIcon,
                            contentDescription = null,
                            tint = onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                TransportButtonBox(
                    onClick = onForward,
                    enabled = true,
                    contentDescription = forwardDesc,
                    testTag = "player_forward_10_button",
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(frostedSideBrush, CircleShape)
                            .border(1.dp, primary.copy(alpha = 0.42f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = forwardIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                TransportButtonBox(
                    onClick = onNextEpisode,
                    enabled = hasNextEpisode,
                    contentDescription = "Следующая серия",
                    testTag = "player_next_episode_button",
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(frostedSideBrush, CircleShape)
                            .border(
                                1.dp,
                                Color.White.copy(alpha = if (hasNextEpisode) 0.34f else 0.14f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = null,
                            tint = if (hasNextEpisode) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }

        PlayerControlsTheme.GLASS_CAPSULE -> {
            val glassBrush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.26f),
                    primary.copy(alpha = 0.18f)
                )
            )
            val centerGlassBrush = Brush.linearGradient(
                listOf(
                    primary.copy(alpha = 0.86f),
                    secondary.copy(alpha = 0.80f)
                )
            )
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TransportButtonBox(
                    onClick = onPreviousEpisode,
                    enabled = hasPlaylist,
                    contentDescription = "Предыдущая серия",
                    testTag = "player_prev_episode_button",
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 40.dp)
                            .background(glassBrush, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = null,
                            tint = if (hasPlaylist) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                TransportButtonBox(
                    onClick = onRewind,
                    enabled = true,
                    contentDescription = rewindDesc,
                    testTag = "player_rewind_10_button",
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(width = 50.dp, height = 48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 42.dp)
                            .background(glassBrush, RoundedCornerShape(18.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = rewindIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                TransportButtonBox(
                    onClick = onTogglePlayPause,
                    enabled = true,
                    contentDescription = playPauseDesc,
                    testTag = "player_play_pause_button",
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .size(width = 68.dp, height = 50.dp)
                        .background(centerGlassBrush, RoundedCornerShape(22.dp))
                        .border(1.2.dp, Color.White.copy(alpha = 0.58f), RoundedCornerShape(22.dp))
                ) {
                    Icon(
                        imageVector = playPauseIcon,
                        contentDescription = null,
                        tint = onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                TransportButtonBox(
                    onClick = onForward,
                    enabled = true,
                    contentDescription = forwardDesc,
                    testTag = "player_forward_10_button",
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(width = 50.dp, height = 48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 42.dp)
                            .background(glassBrush, RoundedCornerShape(18.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = forwardIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                TransportButtonBox(
                    onClick = onNextEpisode,
                    enabled = hasNextEpisode,
                    contentDescription = "Следующая серия",
                    testTag = "player_next_episode_button",
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 40.dp)
                            .background(glassBrush, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = null,
                            tint = if (hasNextEpisode) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }

        PlayerControlsTheme.CYBER_NEON -> {
            val neonGlassBrush = Brush.verticalGradient(
                listOf(
                    primary.copy(alpha = 0.24f),
                    secondary.copy(alpha = 0.16f)
                )
            )
            val sideShape = RoundedCornerShape(14.dp)
            val mainShape = RoundedCornerShape(18.dp)
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TransportButtonBox(
                    onClick = onPreviousEpisode,
                    enabled = hasPlaylist,
                    contentDescription = "Предыдущая серия",
                    testTag = "player_prev_episode_button",
                    shape = sideShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(neonGlassBrush, sideShape)
                            .border(
                                1.dp,
                                if (hasPlaylist) primary.copy(alpha = 0.68f) else Color.White.copy(alpha = 0.20f),
                                sideShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = null,
                            tint = if (hasPlaylist) Color.White else Color.White.copy(alpha = 0.32f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                TransportButtonBox(
                    onClick = onRewind,
                    enabled = true,
                    contentDescription = rewindDesc,
                    testTag = "player_rewind_10_button",
                    shape = sideShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(neonGlassBrush, sideShape)
                            .border(1.2.dp, primary.copy(alpha = 0.85f), sideShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = rewindIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        primary.copy(alpha = 0.44f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )
                    TransportButtonBox(
                        onClick = onTogglePlayPause,
                        enabled = true,
                        contentDescription = playPauseDesc,
                        testTag = "player_play_pause_button",
                        shape = mainShape,
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        primary.copy(alpha = 0.36f),
                                        secondary.copy(alpha = 0.28f)
                                    )
                                ),
                                mainShape
                            )
                            .border(1.8.dp, primary, mainShape)
                    ) {
                        Icon(
                            imageVector = playPauseIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                TransportButtonBox(
                    onClick = onForward,
                    enabled = true,
                    contentDescription = forwardDesc,
                    testTag = "player_forward_10_button",
                    shape = sideShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(neonGlassBrush, sideShape)
                            .border(1.2.dp, primary.copy(alpha = 0.85f), sideShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = forwardIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                TransportButtonBox(
                    onClick = onNextEpisode,
                    enabled = hasNextEpisode,
                    contentDescription = "Следующая серия",
                    testTag = "player_next_episode_button",
                    shape = sideShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(neonGlassBrush, sideShape)
                            .border(
                                1.dp,
                                if (hasNextEpisode) primary.copy(alpha = 0.68f) else Color.White.copy(alpha = 0.20f),
                                sideShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = null,
                            tint = if (hasNextEpisode) Color.White else Color.White.copy(alpha = 0.32f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }

        PlayerControlsTheme.MINIMAL_RING -> {
            val ringGlassBg = Color.White.copy(alpha = 0.14f)
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TransportButtonBox(
                    onClick = onPreviousEpisode,
                    enabled = hasPlaylist,
                    contentDescription = "Предыдущая серия",
                    testTag = "player_prev_episode_button",
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ringGlassBg, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = if (hasPlaylist) 0.48f else 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = null,
                            tint = if (hasPlaylist) Color.White else Color.White.copy(alpha = 0.32f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                TransportButtonBox(
                    onClick = onRewind,
                    enabled = true,
                    contentDescription = rewindDesc,
                    testTag = "player_rewind_10_button",
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(ringGlassBg, CircleShape)
                            .border(1.2.dp, Color.White.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = rewindIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                TransportButtonBox(
                    onClick = onTogglePlayPause,
                    enabled = true,
                    contentDescription = playPauseDesc,
                    testTag = "player_play_pause_button",
                    modifier = Modifier
                        .size(56.dp)
                        .background(primary.copy(alpha = 0.24f), CircleShape)
                        .border(1.8.dp, primary, CircleShape)
                ) {
                    Icon(
                        imageVector = playPauseIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                TransportButtonBox(
                    onClick = onForward,
                    enabled = true,
                    contentDescription = forwardDesc,
                    testTag = "player_forward_10_button",
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(ringGlassBg, CircleShape)
                            .border(1.2.dp, Color.White.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = forwardIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                TransportButtonBox(
                    onClick = onNextEpisode,
                    enabled = hasNextEpisode,
                    contentDescription = "Следующая серия",
                    testTag = "player_next_episode_button",
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ringGlassBg, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = if (hasNextEpisode) 0.48f else 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = null,
                            tint = if (hasNextEpisode) Color.White else Color.White.copy(alpha = 0.32f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        PlayerControlsTheme.CINEMA_TACTILE -> {
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.38f)),
                modifier = modifier
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransportButtonBox(
                        onClick = onPreviousEpisode,
                        enabled = hasPlaylist,
                        contentDescription = "Предыдущая серия",
                        testTag = "player_prev_episode_button",
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = null,
                            tint = if (hasPlaylist) Color.White else Color.White.copy(alpha = 0.32f),
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    TransportButtonBox(
                        onClick = onRewind,
                        enabled = true,
                        contentDescription = rewindDesc,
                        testTag = "player_rewind_10_button",
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = rewindIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    TransportButtonBox(
                        onClick = onTogglePlayPause,
                        enabled = true,
                        contentDescription = playPauseDesc,
                        testTag = "player_play_pause_button",
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(width = 62.dp, height = 48.dp)
                            .background(frostedMainBrush, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = playPauseIcon,
                            contentDescription = null,
                            tint = onPrimary,
                            modifier = Modifier.size(27.dp)
                        )
                    }

                    TransportButtonBox(
                        onClick = onForward,
                        enabled = true,
                        contentDescription = forwardDesc,
                        testTag = "player_forward_10_button",
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = forwardIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    TransportButtonBox(
                        onClick = onNextEpisode,
                        enabled = hasNextEpisode,
                        contentDescription = "Следующая серия",
                        testTag = "player_next_episode_button",
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = null,
                            tint = if (hasNextEpisode) Color.White else Color.White.copy(alpha = 0.32f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportButtonBox(
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    testTag: String,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Custom interactive timeline bar ("полоса") rendered in 5 distinct themes with
 * chapter markers, A–B repeat span highlight, and smooth tap + horizontal drag scrubbing.
 */
@Composable
fun ThemedPlayerSeekBar(
    controlsTheme: PlayerControlsTheme,
    positionMs: Long,
    durationMs: Long,
    isScrubbing: Boolean,
    chapters: List<VideoChapter>,
    showChapterMarkers: Boolean,
    abLoopStartMs: Long?,
    abLoopEndMs: Long?,
    onScrubChange: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    var barWidthPx by remember { mutableFloatStateOf(1f) }
    val latestDuration by rememberUpdatedState(safeDuration)
    val latestOnScrubChange by rememberUpdatedState(onScrubChange)
    val latestOnScrubFinished by rememberUpdatedState(onScrubFinished)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .onSizeChanged { sz ->
                barWidthPx = sz.width.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val horizontalPad = 10.dp.toPx()
                    val usable = (size.width - horizontalPad * 2f).coerceAtLeast(1f)
                    val frac = ((offset.x - horizontalPad) / usable).coerceIn(0f, 1f)
                    latestOnScrubChange(frac * latestDuration.toFloat())
                    latestOnScrubFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { startOffset ->
                        val horizontalPad = 10.dp.toPx()
                        val usable = (size.width - horizontalPad * 2f).coerceAtLeast(1f)
                        val frac = ((startOffset.x - horizontalPad) / usable).coerceIn(0f, 1f)
                        latestOnScrubChange(frac * latestDuration.toFloat())
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val horizontalPad = 10.dp.toPx()
                        val usable = (size.width - horizontalPad * 2f).coerceAtLeast(1f)
                        val frac = ((change.position.x - horizontalPad) / usable).coerceIn(0f, 1f)
                        latestOnScrubChange(frac * latestDuration.toFloat())
                    },
                    onDragEnd = {
                        latestOnScrubFinished()
                    },
                    onDragCancel = {
                        latestOnScrubFinished()
                    }
                )
            }
            .semantics {
                contentDescription = "Полоса перемотки видео"
            }
            .testTag("player_seek_slider"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            val padX = 10.dp.toPx()
            val trackStart = padX
            val trackEnd = (size.width - padX).coerceAtLeast(padX + 1f)
            val trackWidth = trackEnd - trackStart
            val cy = size.height / 2f
            val activeX = trackStart + trackWidth * progress

            // Optional A-B loop range fractions
            val abStartFrac = abLoopStartMs?.let { (it.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) }
            val abEndFrac = abLoopEndMs?.let { (it.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) }

            when (controlsTheme) {
                PlayerControlsTheme.CLASSIC_OLED -> {
                    val h = if (isScrubbing) 6.dp.toPx() else 4.dp.toPx()
                    val r = CornerRadius(h / 2f, h / 2f)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.28f),
                        topLeft = Offset(trackStart, cy - h / 2f),
                        size = Size(trackWidth, h),
                        cornerRadius = r
                    )
                    if (activeX > trackStart) {
                        drawRoundRect(
                            color = primary,
                            topLeft = Offset(trackStart, cy - h / 2f),
                            size = Size(activeX - trackStart, h),
                            cornerRadius = r
                        )
                    }
                    drawAbSpan(abStartFrac, abEndFrac, trackStart, trackWidth, cy, h + 4.dp.toPx(), tertiary)
                    drawChapterTicks(
                        enabled = showChapterMarkers && durationMs > 0L,
                        chapters = chapters,
                        durationMs = safeDuration,
                        positionMs = positionMs,
                        trackStart = trackStart,
                        trackWidth = trackWidth,
                        cy = cy,
                        halfTickPx = 4.5.dp.toPx(),
                        passedColor = secondary,
                        upcomingColor = Color.White.copy(alpha = 0.9f)
                    )
                    val thumbRadius = if (isScrubbing) 9.5.dp.toPx() else 7.5.dp.toPx()
                    drawCircle(
                        color = primary,
                        radius = thumbRadius,
                        center = Offset(activeX, cy)
                    )
                }

                PlayerControlsTheme.GLASS_CAPSULE -> {
                    val h = if (isScrubbing) 12.dp.toPx() else 10.dp.toPx()
                    val r = CornerRadius(h / 2f, h / 2f)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.16f),
                        topLeft = Offset(trackStart, cy - h / 2f),
                        size = Size(trackWidth, h),
                        cornerRadius = r
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.30f),
                        topLeft = Offset(trackStart, cy - h / 2f),
                        size = Size(trackWidth, h),
                        cornerRadius = r,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    if (activeX > trackStart + 1f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(primary, secondary),
                                startX = trackStart,
                                endX = activeX.coerceAtLeast(trackStart + 2f)
                            ),
                            topLeft = Offset(trackStart, cy - h / 2f),
                            size = Size(activeX - trackStart, h),
                            cornerRadius = r
                        )
                    }
                    drawAbSpan(abStartFrac, abEndFrac, trackStart, trackWidth, cy, h + 3.dp.toPx(), tertiary)
                    drawChapterTicks(
                        enabled = showChapterMarkers && durationMs > 0L,
                        chapters = chapters,
                        durationMs = safeDuration,
                        positionMs = positionMs,
                        trackStart = trackStart,
                        trackWidth = trackWidth,
                        cy = cy,
                        halfTickPx = (h / 2f) - 1.dp.toPx(),
                        passedColor = Color.White,
                        upcomingColor = Color.White.copy(alpha = 0.78f)
                    )
                    val pillW = if (isScrubbing) 20.dp.toPx() else 16.dp.toPx()
                    val pillH = if (isScrubbing) 16.dp.toPx() else 14.dp.toPx()
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(activeX - pillW / 2f, cy - pillH / 2f),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
                    )
                    drawRoundRect(
                        color = primary,
                        topLeft = Offset(activeX - pillW / 2f, cy - pillH / 2f),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(pillH / 2f, pillH / 2f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                PlayerControlsTheme.CYBER_NEON -> {
                    val coreH = if (isScrubbing) 5.dp.toPx() else 3.5.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.20f),
                        start = Offset(trackStart, cy),
                        end = Offset(trackEnd, cy),
                        strokeWidth = coreH,
                        cap = StrokeCap.Round
                    )
                    if (activeX > trackStart) {
                        // Outer neon laser aura
                        drawLine(
                            color = primary.copy(alpha = 0.34f),
                            start = Offset(trackStart, cy),
                            end = Offset(activeX, cy),
                            strokeWidth = coreH + 8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Bright inner beam
                        drawLine(
                            color = primary,
                            start = Offset(trackStart, cy),
                            end = Offset(activeX, cy),
                            strokeWidth = coreH,
                            cap = StrokeCap.Round
                        )
                    }
                    drawAbSpan(abStartFrac, abEndFrac, trackStart, trackWidth, cy, 10.dp.toPx(), tertiary)
                    drawChapterTicks(
                        enabled = showChapterMarkers && durationMs > 0L,
                        chapters = chapters,
                        durationMs = safeDuration,
                        positionMs = positionMs,
                        trackStart = trackStart,
                        trackWidth = trackWidth,
                        cy = cy,
                        halfTickPx = 5.dp.toPx(),
                        passedColor = primary,
                        upcomingColor = Color.White.copy(alpha = 0.85f)
                    )
                    val haloR = if (isScrubbing) 14.dp.toPx() else 11.dp.toPx()
                    drawCircle(
                        color = primary.copy(alpha = 0.32f),
                        radius = haloR,
                        center = Offset(activeX, cy)
                    )
                    drawCircle(
                        color = Color(0xFF050811),
                        radius = 6.5.dp.toPx(),
                        center = Offset(activeX, cy)
                    )
                    drawCircle(
                        color = primary,
                        radius = 6.5.dp.toPx(),
                        center = Offset(activeX, cy),
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                }

                PlayerControlsTheme.MINIMAL_RING -> {
                    val lineH = if (isScrubbing) 3.dp.toPx() else 2.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.28f),
                        start = Offset(trackStart, cy),
                        end = Offset(trackEnd, cy),
                        strokeWidth = lineH,
                        cap = StrokeCap.Round
                    )
                    if (activeX > trackStart) {
                        drawLine(
                            color = Color.White,
                            start = Offset(trackStart, cy),
                            end = Offset(activeX, cy),
                            strokeWidth = lineH + 0.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    drawAbSpan(abStartFrac, abEndFrac, trackStart, trackWidth, cy, 7.dp.toPx(), primary)
                    drawChapterTicks(
                        enabled = showChapterMarkers && durationMs > 0L,
                        chapters = chapters,
                        durationMs = safeDuration,
                        positionMs = positionMs,
                        trackStart = trackStart,
                        trackWidth = trackWidth,
                        cy = cy,
                        halfTickPx = 3.5.dp.toPx(),
                        passedColor = primary,
                        upcomingColor = Color.White.copy(alpha = 0.7f)
                    )
                    val ringR = if (isScrubbing) 8.5.dp.toPx() else 6.5.dp.toPx()
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.72f),
                        radius = ringR,
                        center = Offset(activeX, cy)
                    )
                    drawCircle(
                        color = primary,
                        radius = ringR,
                        center = Offset(activeX, cy),
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                }

                PlayerControlsTheme.CINEMA_TACTILE -> {
                    val slotH = if (isScrubbing) 14.dp.toPx() else 12.dp.toPx()
                    val r = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    drawRoundRect(
                        color = Color(0xCC0D121E),
                        topLeft = Offset(trackStart, cy - slotH / 2f),
                        size = Size(trackWidth, slotH),
                        cornerRadius = r
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.22f),
                        topLeft = Offset(trackStart, cy - slotH / 2f),
                        size = Size(trackWidth, slotH),
                        cornerRadius = r,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    if (activeX > trackStart) {
                        drawRoundRect(
                            color = primary.copy(alpha = 0.82f),
                            topLeft = Offset(trackStart, cy - slotH / 2f),
                            size = Size(activeX - trackStart, slotH),
                            cornerRadius = r
                        )
                    }
                    // Subtle studio ruler ticks every 10%
                    for (step in 1..9) {
                        val rx = trackStart + trackWidth * (step / 10f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.16f),
                            start = Offset(rx, cy + slotH / 2f - 3.dp.toPx()),
                            end = Offset(rx, cy + slotH / 2f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    drawAbSpan(abStartFrac, abEndFrac, trackStart, trackWidth, cy, slotH + 2.dp.toPx(), tertiary)
                    drawChapterTicks(
                        enabled = showChapterMarkers && durationMs > 0L,
                        chapters = chapters,
                        durationMs = safeDuration,
                        positionMs = positionMs,
                        trackStart = trackStart,
                        trackWidth = trackWidth,
                        cy = cy,
                        halfTickPx = slotH / 2f,
                        passedColor = Color.White,
                        upcomingColor = Color.White.copy(alpha = 0.85f)
                    )
                    // Vertical tactile studio playhead cursor
                    val needleW = if (isScrubbing) 6.dp.toPx() else 4.5.dp.toPx()
                    val needleH = if (isScrubbing) 24.dp.toPx() else 20.dp.toPx()
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(activeX - needleW / 2f, cy - needleH / 2f),
                        size = Size(needleW, needleH),
                        cornerRadius = CornerRadius(needleW / 2f, needleW / 2f)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAbSpan(
    abStartFrac: Float?,
    abEndFrac: Float?,
    trackStart: Float,
    trackWidth: Float,
    cy: Float,
    heightPx: Float,
    color: Color
) {
    if (abStartFrac == null) return
    val xA = trackStart + trackWidth * abStartFrac
    if (abEndFrac != null && abEndFrac > abStartFrac) {
        val xB = trackStart + trackWidth * abEndFrac
        drawRoundRect(
            color = color.copy(alpha = 0.36f),
            topLeft = Offset(xA, cy - heightPx / 2f),
            size = Size(xB - xA, heightPx),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        drawLine(
            color = color,
            start = Offset(xB, cy - heightPx / 2f - 2.dp.toPx()),
            end = Offset(xB, cy + heightPx / 2f + 2.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
    }
    drawLine(
        color = color,
        start = Offset(xA, cy - heightPx / 2f - 2.dp.toPx()),
        end = Offset(xA, cy + heightPx / 2f + 2.dp.toPx()),
        strokeWidth = 2.dp.toPx()
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChapterTicks(
    enabled: Boolean,
    chapters: List<VideoChapter>,
    durationMs: Long,
    positionMs: Long,
    trackStart: Float,
    trackWidth: Float,
    cy: Float,
    halfTickPx: Float,
    passedColor: Color,
    upcomingColor: Color
) {
    if (!enabled || chapters.isEmpty() || durationMs <= 0L) return
    chapters.forEach { chapter ->
        val frac = (chapter.startTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        if (frac in 0.008f..0.992f) {
            val x = trackStart + frac * trackWidth
            drawLine(
                color = if (positionMs >= chapter.startTimeMs) passedColor else upcomingColor,
                start = Offset(x, cy - halfTickPx),
                end = Offset(x, cy + halfTickPx),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

/**
 * Interactive selector + live preview for [PlayerControlsTheme] used in Settings,
 * Quick Theme sheet, and Player Tools sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerControlsThemeSelectorSection(
    selectedTheme: PlayerControlsTheme,
    onSelectTheme: (PlayerControlsTheme) -> Unit,
    compactChipsOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Live mini preview stage
        Surface(
            color = Color(0xFF060912),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThemedPlayerCenterTransport(
                    controlsTheme = selectedTheme,
                    isPlaying = true,
                    hasPlaylist = true,
                    hasNextEpisode = true,
                    seekStepSeconds = 10,
                    onPreviousEpisode = {},
                    onRewind = {},
                    onTogglePlayPause = {},
                    onForward = {},
                    onNextEpisode = {}
                )
                ThemedPlayerSeekBar(
                    controlsTheme = selectedTheme,
                    positionMs = 540_000L,
                    durationMs = 1_440_000L,
                    isScrubbing = false,
                    chapters = listOf(
                        VideoChapter(0, "Интро", 0L, 90_000L),
                        VideoChapter(1, "Опенинг", 90_000L, 180_000L),
                        VideoChapter(2, "Часть A", 720_000L, 1_320_000L)
                    ),
                    showChapterMarkers = true,
                    abLoopStartMs = null,
                    abLoopEndMs = null,
                    onScrubChange = {},
                    onScrubFinished = {}
                )
            }
        }

        if (compactChipsOnly) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayerControlsTheme.entries.forEach { theme ->
                    val isSelected = selectedTheme == theme
                    Surface(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTheme(theme) }
                            .testTag("controls_theme_chip_${theme.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = theme.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            PlayerControlsTheme.entries.forEach { theme ->
                val isSelected = selectedTheme == theme
                Surface(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTheme(theme) }
                        .testTag("controls_theme_option_${theme.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = theme.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = theme.shortBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = theme.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectTheme(theme) }
                        )
                    }
                }
            }
        }
    }
}
