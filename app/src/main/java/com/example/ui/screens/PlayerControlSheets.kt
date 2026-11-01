package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.player.BluetoothAudioRouteState
import com.example.player.SubtitleTrackUiOption
import com.example.ui.components.formatDelaySeconds
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.localizedDescription
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.localizedSubtitle
import com.example.ui.i18n.localizedTitle
import com.example.ui.i18n.tr
import kotlin.math.roundToInt

internal val PlayerPopupSheetContainerColor = Color(0xFF0E131F).copy(alpha = 0.65f)
internal val PlayerPopupSheetScrimColor = Color.Black.copy(alpha = 0.18f)
internal val PlayerPopupCardColor = Color(0xFF141B29).copy(alpha = 0.65f)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AudioTracksModalSheet(
    audioOptions: List<AudioTrackUiOption>,
    audioBoostPercent: Int,
    audioDelayMs: Long = 0L,
    effectiveAudioDelayMs: Long = audioDelayMs,
    bluetoothRoute: BluetoothAudioRouteState = BluetoothAudioRouteState(),
    bluetoothAutoSyncEnabled: Boolean = true,
    bluetoothAppliedOffsetMs: Long = 0L,
    bluetoothConfiguredOffsetMs: Long = -150L,
    lastAudioResyncTimestampMs: Long = 0L,
    onToggleBluetoothAutoSync: (Boolean) -> Unit = {},
    onSelectAudio: (AudioTrackUiOption) -> Unit,
    onSetAudioBoostPercent: (Int) -> Unit,
    onAdjustAudioDelayMs: (Long) -> Unit = {},
    onSetAudioDelayMs: (Long) -> Unit = {},
    onResyncAudio: () -> Unit = {},
    onPickExternalAudioFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEnglish = LocalIsEnglish.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PlayerPopupSheetContainerColor,
        scrimColor = PlayerPopupSheetScrimColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    tr("Аудиодорожки и синхронизация", "Audio Tracks & Sync"),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Button(
                onClick = onPickExternalAudioFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_attach_audio_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    tr("Подключить внешнюю аудиодорожку (.mka, .mp3, .aac…)", "Attach External Audio (.mka, .mp3, .aac…)"),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (audioOptions.isEmpty()) {
                Text(
                    text = tr(
                        "Аудиодорожки в контейнере не обнаружены. Подключите внешний аудиофайл кнопкой выше.",
                        "No audio tracks found in container. Attach an external audio file above."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                audioOptions.forEachIndexed { idx, opt ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (opt.isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                PlayerPopupCardColor
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectAudio(opt) }
                            .testTag("audio_track_option_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = opt.isSelected,
                                onClick = { onSelectAudio(opt) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opt.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                color = if (opt.isExternal) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                } else {
                                    Color.White.copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = opt.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (opt.isExternal) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            // Bluetooth Route & Auto-Compensation Card
            Surface(
                color = if (bluetoothRoute.isBluetoothActive) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f)
                } else {
                    PlayerPopupCardColor
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (bluetoothRoute.isBluetoothActive) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_bluetooth_sync_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val routeTitle = if (bluetoothRoute.isBluetoothActive) {
                        val namePart = bluetoothRoute.deviceName?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                        "${bluetoothRoute.profile.localizedBadge(isEnglish)}$namePart"
                    } else {
                        if (isEnglish) "Audio Output: Speaker / Wired" else "Вывод звука: Динамик / Провод"
                    }
                    Text(
                        text = routeTitle,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    val routeSub = if (bluetoothRoute.isBluetoothActive) {
                        val modeLabel = if (bluetoothRoute.isGamingMode) {
                            tr("Игровой режим", "Gaming Mode")
                        } else {
                            tr("Авто-профиль", "Auto profile")
                        }
                        tr(
                            "Авто-компенсация Bluetooth ($modeLabel): ${AudioDelayProcessor.formatDelayLabel(bluetoothAppliedOffsetMs)}",
                            "Bluetooth auto-compensation ($modeLabel): ${AudioDelayProcessor.formatDelayLabel(bluetoothAppliedOffsetMs)}"
                        )
                    } else {
                        tr(
                            "Авто-компенсация задержки активируется при подключении Bluetooth",
                            "Auto latency compensation activates when Bluetooth connects"
                        )
                    }

                    Text(
                        text = routeSub,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Audio Synchronization & Manual Fine-Tune Offset Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tr("Синхронизация аудио и видео", "Audio & Video Sync Offset"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = tr(
                            "Итоговый сдвиг потока: ${AudioDelayProcessor.formatDelayLabel(effectiveAudioDelayMs)}",
                            "Effective stream offset: ${AudioDelayProcessor.formatDelayLabel(effectiveAudioDelayMs)}"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("effective_audio_delay_text")
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = AudioDelayProcessor.formatDelayLabel(effectiveAudioDelayMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
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
                ) { Text("-250мс", style = MaterialTheme.typography.labelSmall) }

                OutlinedButton(
                    onClick = { onAdjustAudioDelayMs(-50L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_delay_minus_50")
                ) { Text("-50мс", style = MaterialTheme.typography.labelSmall) }

                FilledTonalButton(
                    onClick = { onSetAudioDelayMs(0L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_delay_reset")
                ) { Text("0 мс", style = MaterialTheme.typography.labelSmall) }

                OutlinedButton(
                    onClick = { onAdjustAudioDelayMs(50L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_delay_plus_50")
                ) { Text("+50мс", style = MaterialTheme.typography.labelSmall) }

                OutlinedButton(
                    onClick = { onAdjustAudioDelayMs(250L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_delay_plus_250")
                ) { Text("+250мс", style = MaterialTheme.typography.labelSmall) }
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
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    tr("Синхронизировать аудиопоток с видео", "Resynchronize Audio & Video Streams"),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (lastAudioResyncTimestampMs > 0L) {
                Text(
                    text = tr(
                        "✓ Аудио- и видеопоток пересинхронизированы по текущему таймкоду (сдвиг: ${AudioDelayProcessor.formatDelayLabel(effectiveAudioDelayMs)})",
                        "✓ Audio & video streams resynchronized to current position (offset: ${AudioDelayProcessor.formatDelayLabel(effectiveAudioDelayMs)})"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("audio_resync_status_text")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

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
    val isEnglish = LocalIsEnglish.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PlayerPopupSheetContainerColor,
        scrimColor = PlayerPopupSheetScrimColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Subtitles,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(tr("Субтитры (SRT / ASS)", "Subtitles (SRT / ASS)"), style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = onLoadExternalSubtitleFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_load_subtitle_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    tr("Загрузить внешние субтитры (.SRT / .ASS)", "Load External Subtitles (.SRT / .ASS)"),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Track Selection
            Text(tr("Активная дорожка субтитров", "Active Subtitle Track"), style = MaterialTheme.typography.bodyMedium)
            subtitleOptions.forEachIndexed { idx, opt ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (opt.isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                        } else {
                            PlayerPopupCardColor
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
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = opt.isSelected,
                            onClick = { onSelectSubtitle(opt) }
                        )
                        Text(
                            text = if (isEnglish && opt.label == "Выключены") "Off" else opt.label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
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
                Text(tr("Задержка субтитров", "Subtitle Delay"), style = MaterialTheme.typography.bodyMedium)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = formatDelaySeconds(subtitleDelayMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 3.dp)
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
                ) { Text(tr("-0.5с", "-0.5s"), style = MaterialTheme.typography.labelSmall) }

                OutlinedButton(
                    onClick = { onAdjustDelayMs(-100L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_minus_100")
                ) { Text(tr("-0.1с", "-0.1s"), style = MaterialTheme.typography.labelSmall) }

                FilledTonalButton(
                    onClick = { onSetDelayMs(0L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_reset")
                ) { Text(tr("0.0с", "0.0s"), style = MaterialTheme.typography.labelSmall) }

                OutlinedButton(
                    onClick = { onAdjustDelayMs(100L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_plus_100")
                ) { Text(tr("+0.1с", "+0.1s"), style = MaterialTheme.typography.labelSmall) }

                OutlinedButton(
                    onClick = { onAdjustDelayMs(500L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_plus_500")
                ) { Text(tr("+0.5с", "+0.5s"), style = MaterialTheme.typography.labelSmall) }
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
                Text(tr("Размер шрифта", "Font Size"), style = MaterialTheme.typography.bodyMedium)
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
            Text(tr("Цвет субтитров", "Subtitle Color"), style = MaterialTheme.typography.bodyMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SubtitleColorPreset.entries.forEach { preset ->
                    val selected = settings.subtitleColorArgb == preset.argb
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                                contentDescription = preset.localizedLabel(isEnglish),
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
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
                Text(tr("Тёмная подложка под текстом", "Dark Background Box"), style = MaterialTheme.typography.bodySmall)
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
    val isEnglish = LocalIsEnglish.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PlayerPopupSheetContainerColor,
        scrimColor = PlayerPopupSheetScrimColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    tr("Anime4K — Реалтайм апскейлинг", "Anime4K — Real-Time Upscaling"),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (settings.isAnime4kAutoDowngradedForMemc) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("anime4k_memc_auto_fast_banner")
                ) {
                    Text(
                        text = tr(
                            "⚡ Авто-оптимизация GPU: при одновременной работе с интерполяцией кадров (MEMC) Anime4K автоматически переключён в быстрый однопроходный режим (Anime4K_Fast) для стабильного FPS без рывков.",
                            "⚡ Auto GPU Optimization: while Frame Interpolation (MEMC) is active, Anime4K automatically runs in fast single-pass mode (Anime4K_Fast) to prevent frame drops."
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = PlayerPopupCardColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tr("Активировать Anime4K", "Enable Anime4K"), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            tr(
                                "OpenGL ES шейдер реконструкции линий",
                                "OpenGL ES line reconstruction shader"
                            ),
                            style = MaterialTheme.typography.labelSmall,
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

            Text(tr("Пресеты качества", "Quality Presets"), style = MaterialTheme.typography.bodyMedium)
            Anime4KPreset.entries.forEach { preset ->
                val isSelected = settings.effectiveAnime4KPreset == preset ||
                    (!settings.anime4kEnabled && preset == Anime4KPreset.OFF)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            PlayerPopupCardColor
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
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
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            Text(preset.localizedTitle(isEnglish), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                preset.localizedDescription(isEnglish),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = preset.badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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
                Text(tr("Интенсивность реконструкции", "Reconstruction Strength"), style = MaterialTheme.typography.bodyMedium)
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
                    Icon(Icons.Default.Compare, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Column {
                        Text(tr("Сравнение До / После", "Before / After Comparison"), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            tr(
                                "Слева оригинал, справа улучшение Anime4K",
                                "Original on the left, Anime4K on the right"
                            ),
                            style = MaterialTheme.typography.labelSmall,
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
    onSelectDecoderMode: (DecoderMode) -> Unit,
    onDismiss: () -> Unit
) {
    val isEnglish = LocalIsEnglish.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PlayerPopupSheetContainerColor,
        scrimColor = PlayerPopupSheetScrimColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    tr("Аппаратный декодер и видео", "Hardware Decoder & Video"),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Live Codec Telemetry Box
            Surface(
                color = PlayerPopupCardColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tr("Кодек: $activeCodecName", "Codec: $activeCodecName"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isEnglish) {
                            "Stream: ${videoWidth}×${videoHeight} • Acceleration: ${if (isHardwareAccelerated) "Hardware (MediaCodec GPU)" else "Software (CPU)"}"
                        } else {
                            "Поток: ${videoWidth}×${videoHeight} • Ускорение: ${if (isHardwareAccelerated) "Аппаратное (MediaCodec GPU)" else "Программное (CPU)"}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(tr("Режим декодера", "Decoder Mode"), style = MaterialTheme.typography.bodyMedium)
            DecoderMode.entries.forEach { mode ->
                val selected = activeDecoderMode == mode
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    } else {
                        PlayerPopupCardColor
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectDecoderMode(mode) }
                        .testTag("decoder_mode_chip_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectDecoderMode(mode) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.localizedTitle(isEnglish), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = mode.localizedSubtitle(isEnglish),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = mode.shortBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
