package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Anime4KPreset
import com.example.model.DecoderMode
import com.example.model.EpisodeItem
import com.example.model.PlayerSettings
import com.example.model.SubtitleColorPreset
import com.example.model.VideoScaleMode
import com.example.player.AudioDelayProcessor
import com.example.player.AudioTrackUiOption
import com.example.player.SubtitleTrackUiOption
import com.example.ui.components.formatDelaySeconds
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTracksModalSheet(
    audioOptions: List<AudioTrackUiOption>,
    audioBoostPercent: Int,
    audioDelayMs: Long = 0L,
    onSelectAudio: (AudioTrackUiOption) -> Unit,
    onSetAudioBoostPercent: (Int) -> Unit,
    onAdjustAudioDelayMs: (Long) -> Unit = {},
    onSetAudioDelayMs: (Long) -> Unit = {},
    onResyncAudio: () -> Unit = {},
    onPickExternalAudioFile: () -> Unit,
    onDismiss: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Аудиодорожки", style = MaterialTheme.typography.titleLarge)
            }

            Button(
                onClick = onPickExternalAudioFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_attach_audio_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Подключить внешнюю аудиодорожку (.mka, .mp3, .aac…)")
            }

            if (audioOptions.isEmpty()) {
                Text(
                    text = "Аудиодорожки в контейнере не обнаружены. Подключите внешний аудиофайл кнопкой выше.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                audioOptions.forEachIndexed { idx, opt ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (opt.isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectAudio(opt) }
                            .testTag("audio_track_option_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = opt.isSelected,
                                onClick = { onSelectAudio(opt) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opt.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                color = if (opt.isExternal) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = opt.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (opt.isExternal) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Audio Synchronization & Delay Controls (±5s, ±50/250ms steps + stream resync)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Синхронизация и задержка аудио", style = MaterialTheme.typography.titleSmall)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = AudioDelayProcessor.formatDelayLabel(audioDelayMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("audio_delay_value_text")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onAdjustAudioDelayMs(-250L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_delay_minus_250")
                ) { Text("-250мс") }

                OutlinedButton(
                    onClick = { onAdjustAudioDelayMs(-50L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_delay_minus_50")
                ) { Text("-50мс") }

                FilledTonalButton(
                    onClick = { onSetAudioDelayMs(0L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_delay_reset")
                ) { Text("0 мс") }

                OutlinedButton(
                    onClick = { onAdjustAudioDelayMs(50L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_delay_plus_50")
                ) { Text("+50мс") }

                OutlinedButton(
                    onClick = { onAdjustAudioDelayMs(250L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_delay_plus_250")
                ) { Text("+250мс") }
            }

            Slider(
                value = audioDelayMs.toFloat().coerceIn(-5000f, 5000f),
                onValueChange = { rawMs ->
                    val snappedMs = (rawMs / 50f).roundToInt() * 50L
                    onSetAudioDelayMs(snappedMs)
                },
                valueRange = -5000f..5000f,
                modifier = Modifier.testTag("audio_delay_slider")
            )

            FilledTonalButton(
                onClick = onResyncAudio,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_resync_button")
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Синхронизировать аудиопоток с видео")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            AudioBoostControlSection(
                audioBoostPercent = audioBoostPercent,
                onSetAudioBoostPercent = onSetAudioBoostPercent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubtitleControlModalSheet(
    subtitleOptions: List<SubtitleTrackUiOption>,
    subtitleDelayMs: Long,
    settings: PlayerSettings,
    onSelectSubtitle: (SubtitleTrackUiOption) -> Unit,
    onLoadExternalSubtitleFile: () -> Unit,
    onAdjustDelayMs: (Long) -> Unit,
    onSetDelayMs: (Long) -> Unit,
    onSetFontSizeSp: (Float) -> Unit,
    onSetColorArgb: (Long) -> Unit,
    onSetBackgroundEnabled: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
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
                Icon(Icons.Default.Subtitles, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Субтитры (SRT / ASS)", style = MaterialTheme.typography.titleLarge)
            }

            Button(
                onClick = onLoadExternalSubtitleFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_load_subtitle_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Загрузить внешние субтитры (.SRT / .ASS)")
            }

            // Track Selection
            Text("Активная дорожка субтитров", style = MaterialTheme.typography.titleSmall)
            subtitleOptions.forEachIndexed { idx, opt ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (opt.isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectSubtitle(opt) }
                        .testTag("subtitle_track_option_$idx")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = opt.isSelected,
                            onClick = { onSelectSubtitle(opt) }
                        )
                        Text(
                            text = opt.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = opt.badge,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Subtitle Delay Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Задержка субтитров", style = MaterialTheme.typography.titleSmall)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = formatDelaySeconds(subtitleDelayMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("subtitle_delay_value_text")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onAdjustDelayMs(-500L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_minus_500")
                ) { Text("-0.5с") }

                OutlinedButton(
                    onClick = { onAdjustDelayMs(-100L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_minus_100")
                ) { Text("-0.1с") }

                FilledTonalButton(
                    onClick = { onSetDelayMs(0L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_reset")
                ) { Text("0.0с") }

                OutlinedButton(
                    onClick = { onAdjustDelayMs(100L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_plus_100")
                ) { Text("+0.1с") }

                OutlinedButton(
                    onClick = { onAdjustDelayMs(500L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_plus_500")
                ) { Text("+0.5с") }
            }

            Slider(
                value = (subtitleDelayMs / 1000f).coerceIn(-10f, 10f),
                onValueChange = { sec ->
                    val snappedMs = (sec * 10f).roundToInt() * 100L
                    onSetDelayMs(snappedMs)
                },
                valueRange = -10f..10f,
                modifier = Modifier.testTag("subtitle_delay_slider")
            )

            HorizontalDivider()

            // Subtitle Font Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Размер шрифта", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${settings.subtitleFontSizeSp.roundToInt()} sp",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = settings.subtitleFontSizeSp,
                onValueChange = onSetFontSizeSp,
                valueRange = 14f..40f,
                modifier = Modifier.testTag("player_subtitle_font_size_slider")
            )

            // Subtitle Color Swatches
            Text("Цвет субтитров", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SubtitleColorPreset.entries.forEach { preset ->
                    val selected = settings.subtitleColorArgb == preset.argb
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(preset.argb))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                            .clickable { onSetColorArgb(preset.argb) }
                            .testTag("player_subtitle_color_${preset.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = preset.label,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Тёмная подложка под текстом", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.subtitleBackgroundEnabled,
                    onCheckedChange = onSetBackgroundEnabled
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Anime4KControlModalSheet(
    settings: PlayerSettings,
    splitCompareEnabled: Boolean,
    splitPosition: Float,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (Anime4KPreset) -> Unit,
    onChangeStrength: (Float) -> Unit,
    onToggleSplitCompare: (Boolean) -> Unit,
    onChangeSplitPosition: (Float) -> Unit,
    onDismiss: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Anime4K — Реалтайм апскейлинг", style = MaterialTheme.typography.titleLarge)
            }

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
                        Text("Активировать Anime4K", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Многопроходный OpenGL ES шейдер реконструкции линий",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.anime4kEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("player_anime4k_enable_switch")
                    )
                }
            }

            Text("Пресеты качества", style = MaterialTheme.typography.titleSmall)
            Anime4KPreset.entries.forEach { preset ->
                val isSelected = settings.effectiveAnime4KPreset == preset ||
                    (!settings.anime4kEnabled && preset == Anime4KPreset.OFF)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (preset == Anime4KPreset.OFF) {
                                onToggleEnabled(false)
                            } else {
                                onSelectPreset(preset)
                            }
                        }
                        .testTag("anime4k_preset_${preset.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (preset == Anime4KPreset.OFF) {
                                    onToggleEnabled(false)
                                } else {
                                    onSelectPreset(preset)
                                }
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = preset.badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Интенсивность реконструкции", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${(settings.anime4kStrength * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = settings.anime4kStrength,
                onValueChange = onChangeStrength,
                enabled = settings.anime4kEnabled,
                valueRange = 0.25f..1.5f,
                modifier = Modifier.testTag("anime4k_strength_slider")
            )

            // Split-Screen Live Comparison
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
                    Icon(Icons.Default.Compare, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Column {
                        Text("Сравнение До / После", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Слева оригинал, справа улучшение Anime4K",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = splitCompareEnabled,
                    onCheckedChange = onToggleSplitCompare,
                    enabled = settings.anime4kEnabled,
                    modifier = Modifier.testTag("anime4k_split_compare_switch")
                )
            }

            if (splitCompareEnabled && settings.anime4kEnabled) {
                Slider(
                    value = splitPosition,
                    onValueChange = onChangeSplitPosition,
                    valueRange = 0.1f..0.9f,
                    modifier = Modifier.testTag("anime4k_split_slider")
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DecoderAndVideoModalSheet(
    activeDecoderMode: DecoderMode,
    activeCodecName: String,
    isHardwareAccelerated: Boolean,
    videoWidth: Int,
    videoHeight: Int,
    playbackSpeed: Float,
    scaleMode: VideoScaleMode,
    playlist: List<EpisodeItem>,
    currentEpisodeIndex: Int,
    onSelectDecoderMode: (DecoderMode) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectScaleMode: (VideoScaleMode) -> Unit,
    onSelectEpisodeIndex: (Int) -> Unit,
    onDismiss: () -> Unit
) {
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
                Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Аппаратный декодер и видео", style = MaterialTheme.typography.titleLarge)
            }

            // Live Codec Telemetry Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Кодек: $activeCodecName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Поток: ${videoWidth}×${videoHeight} • Ускорение: ${if (isHardwareAccelerated) "Аппаратное (MediaCodec GPU)" else "Программное (CPU)"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("Режим декодера", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DecoderMode.entries.forEach { mode ->
                    FilterChip(
                        selected = activeDecoderMode == mode,
                        onClick = { onSelectDecoderMode(mode) },
                        label = { Text(mode.title) },
                        modifier = Modifier.testTag("decoder_mode_chip_${mode.id}")
                    )
                }
            }

            Text("Скорость воспроизведения", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                    FilterChip(
                        selected = kotlin.math.abs(playbackSpeed - spd) < 0.02f,
                        onClick = { onSelectSpeed(spd) },
                        label = { Text("${spd}x") }
                    )
                }
            }

            Text("Масштаб кадра", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoScaleMode.entries.forEach { mode ->
                    FilterChip(
                        selected = scaleMode == mode,
                        onClick = { onSelectScaleMode(mode) },
                        label = { Text(mode.title) }
                    )
                }
            }

            if (playlist.size > 1) {
                HorizontalDivider()
                Text("Серии в плейлисте (${playlist.size})", style = MaterialTheme.typography.titleSmall)
                playlist.forEachIndexed { idx, ep ->
                    val current = idx == currentEpisodeIndex
                    Surface(
                        color = if (current) {
                            MaterialTheme.colorScheme.primaryContainer
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
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = ep.episodeBadge ?: "#${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = ep.cleanTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
