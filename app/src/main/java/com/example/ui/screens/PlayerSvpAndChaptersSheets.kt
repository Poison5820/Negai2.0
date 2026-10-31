package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.EpisodeItem
import com.example.model.FrameInterpolationMode
import com.example.model.PlayerSettings
import com.example.model.VideoChapter
import com.example.player.DisplayRefreshRateController
import com.example.player.RefreshRatePlan
import com.example.ui.components.ResolutionBadgeChip
import com.example.ui.components.formatTimeMs
import kotlin.math.roundToInt

enum class EpisodesChaptersTab {
    EPISODES,
    CHAPTERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameInterpolationModalSheet(
    settings: PlayerSettings,
    sourceFrameRate: Float,
    playbackSpeed: Float = 1.0f,
    refreshPlan: RefreshRatePlan? = null,
    splitCompareEnabled: Boolean,
    splitPosition: Float,
    onToggleHighRefreshRate: (Boolean) -> Unit,
    onToggleLtpoAdaptive: (Boolean) -> Unit = {},
    onToggleInterpolationEnabled: (Boolean) -> Unit,
    onSelectInterpolationMode: (FrameInterpolationMode) -> Unit,
    onToggleSplitCompare: (Boolean) -> Unit,
    onChangeSplitPosition: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val refreshInfo = remember(settings.highRefreshRateEnabled, settings.ltpoAdaptiveRefreshEnabled) {
        DisplayRefreshRateController.queryDisplayRefreshRates(context)
    }
    val effectivePlan = refreshPlan ?: remember(
        refreshInfo,
        sourceFrameRate,
        playbackSpeed,
        settings.highRefreshRateEnabled,
        settings.ltpoAdaptiveRefreshEnabled,
        settings.effectiveFrameInterpolationMode
    ) {
        DisplayRefreshRateController.computeOptimalRefreshPlan(
            displayInfo = refreshInfo,
            isPlayerActive = true,
            isPlaying = true,
            sourceVideoFps = sourceFrameRate,
            playbackSpeed = playbackSpeed,
            highRefreshRateEnabled = settings.highRefreshRateEnabled,
            ltpoAdaptiveEnabled = settings.ltpoAdaptiveRefreshEnabled,
            interpolationMode = settings.effectiveFrameInterpolationMode
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Интерполяция кадров (MEMC) и LTPO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (refreshInfo.isLtpoCapable) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "LTPO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = effectivePlan.statusSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Режимы экрана: ${refreshInfo.supportedModesHz.joinToString("/")} Гц",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // LTPO Adaptive Display Power Saving switch
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Экономия энергии LTPO дисплея",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Подбирает кратную частоту под FPS видео, ускорение (${formatPlaybackSpeedLabel(playbackSpeed)}) и MEMC; сбрасывает Гц на паузе",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.ltpoAdaptiveRefreshEnabled,
                        onCheckedChange = onToggleLtpoAdaptive,
                        modifier = Modifier.testTag("ltpo_adaptive_refresh_switch")
                    )
                }
            }

            // High refresh rate switch
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Высокая частота экрана (90–165 Гц)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Блокирует сброс дисплея до 24/60 Гц во время видео",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.highRefreshRateEnabled,
                        onCheckedChange = onToggleHighRefreshRate,
                        modifier = Modifier.testTag("svp_high_refresh_switch")
                    )
                }
            }

            // Interpolation master switch
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Интерполяция кадров (GPU MEMC)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Оптический поток + компенсация движения в реальном времени",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.frameInterpolationEnabled,
                        onCheckedChange = onToggleInterpolationEnabled,
                        modifier = Modifier.testTag("svp_enable_switch")
                    )
                }
            }

            Text(
                text = "Профиль интерполяции",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            FrameInterpolationMode.entries.forEach { mode ->
                val isSelected = if (mode == FrameInterpolationMode.OFF) {
                    !settings.frameInterpolationEnabled || settings.frameInterpolationMode == FrameInterpolationMode.OFF
                } else {
                    settings.frameInterpolationEnabled && settings.frameInterpolationMode == mode
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (mode == FrameInterpolationMode.OFF) {
                                onToggleInterpolationEnabled(false)
                            } else {
                                onSelectInterpolationMode(mode)
                            }
                        }
                        .testTag("svp_mode_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (mode == FrameInterpolationMode.OFF) {
                                    onToggleInterpolationEnabled(false)
                                } else {
                                    onSelectInterpolationMode(mode)
                                }
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = mode.badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column {
                        Text("Сравнение До / После", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Слева исходный ритм кадров, справа сглаживание MEMC",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = splitCompareEnabled,
                    onCheckedChange = onToggleSplitCompare,
                    enabled = settings.effectiveFrameInterpolationMode.isEnabled,
                    modifier = Modifier.testTag("svp_split_compare_switch")
                )
            }

            if (splitCompareEnabled && settings.effectiveFrameInterpolationMode.isEnabled) {
                Slider(
                    value = splitPosition,
                    onValueChange = onChangeSplitPosition,
                    valueRange = 0.1f..0.9f,
                    modifier = Modifier.testTag("svp_split_slider")
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodesModalSheet(
    playlist: List<EpisodeItem>,
    currentEpisodeIndex: Int,
    onSelectEpisodeIndex: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentEpisodeIndex) {
        if (currentEpisodeIndex in playlist.indices) {
            listState.scrollToItem((currentEpisodeIndex - 1).coerceAtLeast(0))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 22.dp)
                .testTag("episodes_modal_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Список серий",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${(currentEpisodeIndex + 1).coerceAtMost(playlist.size.coerceAtLeast(1))} из ${playlist.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (playlist.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "В текущем плейлисте нет доступных серий.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(
                        items = playlist,
                        key = { idx, ep -> "${idx}_${ep.uri}" }
                    ) { idx, ep ->
                        val isCurrent = idx == currentEpisodeIndex
                        Surface(
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectEpisodeIndex(idx)
                                    onDismiss()
                                }
                                .testTag("player_episode_row_$idx")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = ep.episodeBadge ?: "#${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = ep.cleanTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ep.resolutionBadge?.let { resBadge ->
                                        ResolutionBadgeChip(
                                            badge = resBadge,
                                            compact = true
                                        )
                                    }
                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Воспроизводится",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (ep.durationMs > 0L) {
                                        Text(
                                            text = formatTimeMs(ep.durationMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (ep.progressFraction > 0.02f && !isCurrent) {
                                    LinearProgressIndicator(
                                        progress = { ep.progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersModalSheet(
    chapters: List<VideoChapter>,
    currentPositionMs: Long,
    onSelectChapter: (VideoChapter) -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeChapterIndex = remember(chapters, currentPositionMs) {
        findActiveChapterIndex(chapters, currentPositionMs)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 22.dp)
                .testTag("chapters_modal_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmarks,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Главы видео (${chapters.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                if (chapters.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(
                            onClick = onPreviousChapter,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("sheet_prev_chapter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Предыдущая глава",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onNextChapter,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("sheet_next_chapter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Следующая глава",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (chapters.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Встроенные главы не найдены",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Текущий видеофайл не содержит встроенной разметки глав Matroska (MKV/WebM), MP4 Nero (chpl) или ID3.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val currentChapterTitle = chapters.getOrNull(activeChapterIndex)?.title
                if (!currentChapterTitle.isNullOrBlank()) {
                    Text(
                        text = "Сейчас: $currentChapterTitle",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val chapterListState = rememberLazyListState()
                LaunchedEffect(activeChapterIndex) {
                    if (activeChapterIndex in chapters.indices) {
                        chapterListState.scrollToItem((activeChapterIndex - 1).coerceAtLeast(0))
                    }
                }

                LazyColumn(
                    state = chapterListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 390.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(
                        items = chapters,
                        key = { idx, ch -> "${idx}_${ch.startTimeMs}" }
                    ) { idx, chapter ->
                        val isCurrent = idx == activeChapterIndex
                        Surface(
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectChapter(chapter)
                                    onDismiss()
                                }
                                .testTag("player_chapter_row_$idx")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = formatTimeMs(chapter.startTimeMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (chapter.durationMs > 0L) {
                                        Text(
                                            text = "Длительность: ${formatTimeMs(chapter.durationMs)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Активная глава",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun findActiveChapterIndex(chapters: List<VideoChapter>, positionMs: Long): Int {
    if (chapters.isEmpty()) return -1
    var found = -1
    for (i in chapters.indices) {
        if (positionMs >= chapters[i].startTimeMs) {
            found = i
        } else {
            break
        }
    }
    return found
}
