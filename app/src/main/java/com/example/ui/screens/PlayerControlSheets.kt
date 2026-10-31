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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTracksModalSheet(
    audioOptions: List<AudioTrackUiOption>,
    audioBoostPercent: Int,
    audioDelayMs: Long = 0L,
    effectiveAudioDelayMs: Long = audioDelayMs,
    bluetoothRoute: BluetoothAudioRouteState = BluetoothAudioRouteState(),
    bluetoothAutoSyncEnabled: Boolean = true,
    bluetoothAppliedOffsetMs: Long = 0L,
    onToggleBluetoothAutoSync: (Boolean) -> Unit = {},
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

            // Bluetooth Route & Dynamic Latency Compensation Card (Gaming Mode auto-detection)
            val isEnglish = LocalIsEnglish.current
            Surface(
                color = if (bluetoothRoute.isBluetoothActive) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (bluetoothRoute.isBluetoothActive) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_bluetooth_sync_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            val routeTitle = if (bluetoothRoute.isBluetoothActive) {
                                val namePart = bluetoothRoute.deviceName?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                                "${bluetoothRoute.profile.badgeRu}$namePart"
                            } else {
                                if (isEnglish) "Audio Output: Speaker / Wired" else "Вывод звука: Динамик / Провод"
                            }
                            Text(
                                text = routeTitle,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        // Measured Latency Badge (Gaming Mode / Standard)
                        Surface(
                            color = if (bluetoothRoute.isGamingMode) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else if (bluetoothRoute.isBluetoothActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = bluetoothRoute.displayLatencyLabel(isEnglish),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (bluetoothRoute.isGamingMode) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else if (bluetoothRoute.isBluetoothActive) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val routeSub = if (bluetoothRoute.isBluetoothActive) {
                        val gamingDetail = if (bluetoothRoute.isGamingMode) {
                            if (isEnglish) "Gaming Mode detected • ultra-low audio latency active." else "Обнаружен игровой режим • минимальная задержка звука."
                        } else {
                            if (isEnglish) "Standard audio buffer • adapts automatically if gaming mode is toggled on headphones." else "Стандартный аудиобуфер • при включении игрового режима на наушниках плеер подстроится автоматически."
                        }
                        "${if (isEnglish) "Auto-compensation" else "Авто-компенсация"}: ${AudioDelayProcessor.formatDelayLabel(bluetoothAppliedOffsetMs)} • $gamingDetail"
                    } else {
                        if (isEnglish) {
                            "0 ms latency • Connect Bluetooth headphones and latency will be detected automatically."
                        } else {
                            "Задержка 0 мс • При подключении Bluetooth задержка и игровой режим определяются автоматически."
                        }
                    }

                    Text(
                        text = routeSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Audio Synchronization & Delay Controls (±5s, ±50/250ms steps + stream resync)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ручная коррекция задержки аудио", style = MaterialTheme.typography.titleSmall)
                    if (bluetoothAppliedOffsetMs != 0L) {
                        Text(
                            text = "Итого с учётом Bluetooth: ${AudioDelayProcessor.formatDelayLabel(effectiveAudioDelayMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
    val isEnglish = LocalIsEnglish.current
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
                Text(tr("Субтитры (SRT / ASS)", "Subtitles (SRT / ASS)"), style = MaterialTheme.typography.titleLarge)
            }

            Button(
                onClick = onLoadExternalSubtitleFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_load_subtitle_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("Загрузить внешние субтитры (.SRT / .ASS)", "Load External Subtitles (.SRT / .ASS)"))
            }

            // Track Selection
            Text(tr("Активная дорожка субтитров", "Active Subtitle Track"), style = MaterialTheme.typography.titleSmall)
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
                            text = if (isEnglish && opt.label == "Выключены") "Off" else opt.label,
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
                Text(tr("Задержка субтитров", "Subtitle Delay"), style = MaterialTheme.typography.titleSmall)
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
                ) { Text(tr("-0.5с", "-0.5s")) }

                OutlinedButton(
                    onClick = { onAdjustDelayMs(-100L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_minus_100")
                ) { Text(tr("-0.1с", "-0.1s")) }

                FilledTonalButton(
                    onClick = { onSetDelayMs(0L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_reset")
                ) { Text(tr("0.0с", "0.0s")) }

                OutlinedButton(
                    onClick = { onAdjustDelayMs(100L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_plus_100")
                ) { Text(tr("+0.1с", "+0.1s")) }

                OutlinedButton(
                    onClick = { onAdjustDelayMs(500L) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_delay_plus_500")
                ) { Text(tr("+0.5с", "+0.5s")) }
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
                Text(tr("Размер шрифта", "Font Size"), style = MaterialTheme.typography.titleSmall)
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
            Text(tr("Цвет субтитров", "Subtitle Color"), style = MaterialTheme.typography.titleSmall)
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
                                contentDescription = preset.localizedLabel(isEnglish),
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
                Text(tr("Тёмная подложка под текстом", "Dark Background Box"), style = MaterialTheme.typography.bodyMedium)
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
                Text(
                    tr("Anime4K — Реалтайм апскейлинг", "Anime4K — Real-Time Upscaling"),
                    style = MaterialTheme.typography.titleLarge
                )
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
                        Text(tr("Активировать Anime4K", "Enable Anime4K"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            tr(
                                "Многопроходный OpenGL ES шейдер реконструкции линий",
                                "Multi-pass OpenGL ES line reconstruction shader"
                            ),
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

            Text(tr("Пресеты качества", "Quality Presets"), style = MaterialTheme.typography.titleSmall)
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
                            Text(preset.localizedTitle(isEnglish), style = MaterialTheme.typography.titleSmall)
                            Text(
                                preset.localizedDescription(isEnglish),
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
                Text(tr("Интенсивность реконструкции", "Reconstruction Strength"), style = MaterialTheme.typography.titleSmall)
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
                        Text(tr("Сравнение До / После", "Before / After Comparison"), style = MaterialTheme.typography.titleSmall)
                        Text(
                            tr(
                                "Слева оригинал, справа улучшение Anime4K",
                                "Original on the left, Anime4K on the right"
                            ),
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
    onSelectDecoderMode: (DecoderMode) -> Unit,
    onDismiss: () -> Unit
) {
    val isEnglish = LocalIsEnglish.current
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
                Text(
                    tr("Аппаратный декодер и видео", "Hardware Decoder & Video"),
                    style = MaterialTheme.typography.titleLarge
                )
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(tr("Режим декодера", "Decoder Mode"), style = MaterialTheme.typography.titleSmall)
            DecoderMode.entries.forEach { mode ->
                val selected = activeDecoderMode == mode
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectDecoderMode(mode) }
                        .testTag("decoder_mode_chip_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectDecoderMode(mode) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.localizedTitle(isEnglish), style = MaterialTheme.typography.titleSmall)
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
                                text = mode.shortBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
