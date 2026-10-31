package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.EpisodeItem
import com.example.model.FrameInterpolationMode
import com.example.model.VideoChapter
import com.example.model.VideoResolutionFormatter
import com.example.player.RefreshRatePlan
import com.example.ui.components.ResolutionBadgeChip
import com.example.ui.components.formatTimeMs
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.localizedBadge
import com.example.ui.i18n.localizedSubtitle
import com.example.ui.i18n.localizedTitle
import com.example.ui.i18n.tr
import kotlin.math.roundToInt

/**
 * Modal bottom sheet for real-time Frame Interpolation (60/120/165 FPS MEMC)
 * and adaptive LTPO display refresh rate telemetry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameInterpolationModalSheet(
    frameInterpolationEnabled: Boolean,
    interpolationMode: FrameInterpolationMode,
    highRefreshRateEnabled: Boolean,
    ltpoAdaptiveEnabled: Boolean,
    sourceFrameRate: Float,
    targetDisplayHz: Float,
    refreshPlan: RefreshRatePlan,
    onToggleFrameInterpolation: (Boolean) -> Unit,
    onSelectInterpolationMode: (FrameInterpolationMode) -> Unit,
    onToggleHighRefreshRate: (Boolean) -> Unit,
    onToggleLtpoAdaptive: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val isEnglish = LocalIsEnglish.current
    val effectiveMode = if (frameInterpolationEnabled) interpolationMode else FrameInterpolationMode.OFF

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MotionPhotosOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = tr(
                            "Интерполяция кадров и частота LTPO",
                            "Frame Interpolation & LTPO Refresh Rate"
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = tr(
                            "Реалтайм сглаживание движения (GPU MEMC) и адаптивная герцовка экрана",
                            "Real-time GPU motion smoothing (MEMC) and adaptive display refresh rate"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live FPS -> Display Hz Telemetry Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("interpolation_telemetry_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tr("Исходный поток", "Source Stream"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (sourceFrameRate > 1f) {
                                    "${((sourceFrameRate * 100f).roundToInt() / 100f)} FPS"
                                } else {
                                    "23.976 FPS"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = tr("Вывод на экран", "Display Output"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val outFps = refreshPlan.targetRenderFps.roundToInt()
                            val outHz = targetDisplayHz.roundToInt()
                            Text(
                                text = tr("$outFps FPS @ $outHz Гц", "$outFps FPS @ $outHz Hz"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (effectiveMode.isEnabled) {
                                tr(
                                    "Активен шейдер межкадровой интерполяции (${effectiveMode.localizedTitle(isEnglish)}) — буферизация предыдущего кадра в FBO и субпиксельное сглаживание движения.",
                                    "Active frame interpolation shader (${effectiveMode.localizedTitle(isEnglish)}) — previous frame FBO buffering and sub-pixel motion smoothing."
                                )
                            } else if (ltpoAdaptiveEnabled) {
                                tr(
                                    "LTPO активен: частота экрана автоматически синхронизирована кратно исходному FPS без генерации промежуточных кадров.",
                                    "LTPO active: display refresh rate is integer-synced to the native video FPS without generating intermediate frames."
                                )
                            } else {
                                tr(
                                    "Стандартный вывод кадров без интерполяции.",
                                    "Standard native frame output without interpolation."
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Master Switch for Frame Interpolation
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Включить интерполяцию кадров", "Enable Frame Interpolation"),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = tr(
                                "Повышение плавности 24/30p видео до 60/120 FPS (по умолчанию выключено)",
                                "Smooth 24/30p video up to 60/120 FPS (disabled by default)"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = frameInterpolationEnabled,
                        onCheckedChange = onToggleFrameInterpolation,
                        modifier = Modifier.testTag("player_interpolation_enable_switch")
                    )
                }
            }

            // Frame Interpolation Mode Selection
            Text(
                text = tr(
                    "Режим интерполяции кадров (GPU MEMC)",
                    "Frame Interpolation Mode (GPU MEMC)"
                ),
                style = MaterialTheme.typography.titleSmall
            )

            FrameInterpolationMode.entries.forEach { mode ->
                val selected = effectiveMode == mode
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectInterpolationMode(mode) }
                        .testTag("interpolation_mode_card_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectInterpolationMode(mode) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.localizedTitle(isEnglish),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = mode.localizedSubtitle(isEnglish),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = mode.localizedBadge(isEnglish),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Display Refresh Rate & LTPO Controls
            Text(
                text = tr(
                    "Управление частотой экрана (LTPO / 120 Гц)",
                    "Display Refresh Rate Control (LTPO / 120 Hz)"
                ),
                style = MaterialTheme.typography.titleSmall
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Адаптивная частота LTPO", "LTPO Adaptive Refresh Rate"),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = tr(
                                    "Автоматически подбирать кратную герцовку экрана под FPS видео (24p → 48/96/120 Гц) и снижать частоту на паузе",
                                    "Automatically match display refresh rate to integer multiples of video FPS (24p → 48/96/120 Hz) and lower Hz when paused"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = ltpoAdaptiveEnabled,
                            onCheckedChange = onToggleLtpoAdaptive,
                            modifier = Modifier.testTag("player_ltpo_adaptive_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Высокая частота 90 / 120 / 165 Гц", "High Refresh Rate 90 / 120 / 165 Hz"),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = tr(
                                    "Разрешить использование максимальной частоты обновления дисплея",
                                    "Allow using the display's maximum refresh rate"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = highRefreshRateEnabled,
                            onCheckedChange = onToggleHighRefreshRate,
                            modifier = Modifier.testTag("player_high_refresh_switch")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated independent modal sheet for switching episodes inside the current folder/season.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodesModalSheet(
    playlist: List<EpisodeItem>,
    currentIndex: Int,
    onSelectEpisodeIndex: (Int) -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredEpisodes = remember(playlist, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            playlist.mapIndexed { index, item -> index to item }
        } else {
            playlist.mapIndexedNotNull { index, item ->
                if (item.cleanTitle.contains(q, ignoreCase = true) ||
                    item.fileName.contains(q, ignoreCase = true) ||
                    (item.episodeNumber != null && item.episodeNumber.toString().contains(q))
                ) {
                    index to item
                } else {
                    null
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("episodes_modal_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Column {
                        Text(
                            text = tr("Серии (${playlist.size})", "Episodes (${playlist.size})"),
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (playlist.isNotEmpty()) {
                            Text(
                                text = tr(
                                    "Сейчас: ${currentIndex + 1} из ${playlist.size}",
                                    "Now playing: ${currentIndex + 1} of ${playlist.size}"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (playlist.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onPreviousEpisode,
                            enabled = currentIndex > 0,
                            modifier = Modifier.testTag("episodes_sheet_prev_button")
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Пред.", "Prev"))
                        }
                        FilledTonalButton(
                            onClick = onNextEpisode,
                            enabled = currentIndex < playlist.lastIndex,
                            modifier = Modifier.testTag("episodes_sheet_next_button")
                        ) {
                            Text(tr("След.", "Next"))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (playlist.size > 4) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(tr("Поиск по номеру или названию серии…", "Search by episode number or title…")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = tr("Очистить", "Clear"))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("episodes_sheet_search_input")
                )
            }

            if (filteredEpisodes.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr("Серии по запросу не найдены", "No episodes match your search"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = filteredEpisodes,
                        key = { _, pair -> pair.second.uri }
                    ) { _, (originalIndex, ep) ->
                        val isCurrent = originalIndex == currentIndex
                        val resolutionBadge = remember(ep.videoWidth, ep.videoHeight, ep.fileName) {
                            VideoResolutionFormatter.formatBadge(
                                width = ep.videoWidth,
                                height = ep.videoHeight,
                                fileName = ep.fileName
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            border = if (isCurrent) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectEpisodeIndex(originalIndex)
                                    onDismiss()
                                }
                                .testTag("episodes_sheet_item_$originalIndex")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCurrent) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${ep.episodeNumber ?: (originalIndex + 1)}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = ep.cleanTitle,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (resolutionBadge != null) {
                                            ResolutionBadgeChip(badge = resolutionBadge, compact = true)
                                        }
                                    }

                                    val metaParts = buildList {
                                        if (ep.durationMs > 0L) add(formatTimeMs(ep.durationMs))
                                        if (ep.autoAudioTracks.isNotEmpty()) {
                                            add(tr("Озвучек: ${ep.autoAudioTracks.size}", "Audio: ${ep.autoAudioTracks.size}"))
                                        }
                                        if (ep.autoSubtitleTracks.isNotEmpty()) {
                                            add(tr("Субтитров: ${ep.autoSubtitleTracks.size}", "Subs: ${ep.autoSubtitleTracks.size}"))
                                        }
                                    }
                                    if (metaParts.isNotEmpty()) {
                                        Text(
                                            text = metaParts.joinToString(" • "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (ep.progressFraction > 0.9f && !isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = tr("Просмотрено", "Watched"),
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

/**
 * Dedicated independent modal sheet for embedded container Chapters (OP / ED / Scenes).
 */
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("chapters_modal_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = tr("Главы видео (${chapters.size})", "Video Chapters (${chapters.size})"),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = tr(
                                "Оглавление контейнера (Интро, Опенинг, Сцены, Эндинг)",
                                "Embedded container chapters (Intro, Opening, Scenes, Ending)"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (chapters.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onPreviousChapter,
                            modifier = Modifier.testTag("chapters_sheet_prev_button")
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        FilledTonalButton(
                            onClick = onNextChapter,
                            modifier = Modifier.testTag("chapters_sheet_next_button")
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (chapters.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tr("В этом видеофайле нет встроенных глав", "No embedded chapters in this video file"),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = tr(
                                "Встроенные главы автоматически считываются из контейнеров MKV / MP4 и из тегов разделов файла.",
                                "Embedded chapters are automatically parsed from MKV / MP4 containers and chapter tags."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = chapters,
                        key = { idx, ch -> "${idx}_${ch.startTimeMs}" }
                    ) { index, chapter ->
                        val isCurrent = index == activeChapterIndex
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            border = if (isCurrent) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectChapter(chapter)
                                    onDismiss()
                                }
                                .testTag("chapter_item_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCurrent) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val durationSec = ((chapter.endTimeMs - chapter.startTimeMs).coerceAtLeast(0L) / 1000L)
                                    Text(
                                        text = if (durationSec > 0L) {
                                            tr(
                                                "${formatTimeMs(chapter.startTimeMs)} – ${formatTimeMs(chapter.endTimeMs)} (${durationSec} сек)",
                                                "${formatTimeMs(chapter.startTimeMs)} – ${formatTimeMs(chapter.endTimeMs)} (${durationSec}s)"
                                            )
                                        } else {
                                            formatTimeMs(chapter.startTimeMs)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = formatTimeMs(chapter.startTimeMs),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
    val exact = chapters.indexOfFirst { ch ->
        positionMs >= ch.startTimeMs && (ch.endTimeMs <= ch.startTimeMs || positionMs < ch.endTimeMs)
    }
    if (exact >= 0) return exact
    return chapters.indexOfLast { positionMs >= it.startTimeMs }
}
