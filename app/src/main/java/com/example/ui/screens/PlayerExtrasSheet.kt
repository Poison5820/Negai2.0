package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.PlaybackEndMode
import com.example.model.PlayerControlsTheme
import com.example.model.PlayerOrientationMode
import com.example.model.SleepTimerPreset
import com.example.ui.components.PlayerControlsThemeSelectorSection
import com.example.ui.components.formatTimeMs
import kotlin.math.roundToInt

internal enum class VerticalGestureMode {
    NONE,
    BRIGHTNESS,
    VOLUME
}

@Composable
internal fun VerticalGestureHudCard(
    mode: VerticalGestureMode,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    if (mode == VerticalGestureMode.NONE) return
    val clamped = fraction.coerceIn(0f, 1f)
    val percent = (clamped * 100f).roundToInt()
    val isBrightness = mode == VerticalGestureMode.BRIGHTNESS

    val icon = when {
        isBrightness && percent >= 50 -> Icons.Default.BrightnessHigh
        isBrightness -> Icons.Default.BrightnessLow
        percent == 0 -> Icons.AutoMirrored.Filled.VolumeOff
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }
    val label = if (isBrightness) "Яркость" else "Громкость"

    Surface(
        color = Color.Black.copy(alpha = 0.88f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
        modifier = modifier
            .width(210.dp)
            .testTag("vertical_gesture_hud")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "$label • $percent%",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            LinearProgressIndicator(
                progress = { clamped },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.22f)
            )
        }
    }
}

@Composable
internal fun LongPressSpeedPill(
    speed: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.86f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier.testTag("long_press_speed_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "${speed}x Ускорение",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
internal fun ScreenLockFloatingPill(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.86f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onUnlock)
            .testTag("player_unlock_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Разблокировать управление плеером",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Экран заблокирован • Нажмите, чтобы снять",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PlayerToolsModalSheet(
    audioBoostPercent: Int,
    sleepTimerPreset: SleepTimerPreset,
    sleepTimerRemainingMs: Long,
    abLoopStartMs: Long?,
    abLoopEndMs: Long?,
    currentPositionMs: Long,
    playbackEndMode: PlaybackEndMode,
    orientationMode: PlayerOrientationMode,
    playerControlsTheme: PlayerControlsTheme = PlayerControlsTheme.CLASSIC_OLED,
    onSetAudioBoostPercent: (Int) -> Unit,
    onSetSleepTimer: (SleepTimerPreset) -> Unit,
    onMarkAbPointA: () -> Unit,
    onMarkAbPointB: () -> Unit,
    onClearAbLoop: () -> Unit,
    onSelectPlaybackEndMode: (PlaybackEndMode) -> Unit,
    onSelectOrientationMode: (PlayerOrientationMode) -> Unit,
    onSelectControlsTheme: (PlayerControlsTheme) -> Unit = {},
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
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Инструменты плеера", style = MaterialTheme.typography.titleLarge)
            }

            // 0. Main Player Controls & Timeline Theme
            Text("Стиль кнопок и полосы плеера", style = MaterialTheme.typography.titleSmall)
            PlayerControlsThemeSelectorSection(
                selectedTheme = playerControlsTheme,
                onSelectTheme = onSelectControlsTheme,
                compactChipsOnly = true
            )

            HorizontalDivider()

            // 1. Audio Volume Boost (100% - 200%)
            AudioBoostControlSection(
                audioBoostPercent = audioBoostPercent,
                onSetAudioBoostPercent = onSetAudioBoostPercent
            )

            HorizontalDivider()

            // 2. Sleep Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Text("Таймер сна", style = MaterialTheme.typography.titleSmall)
                }
                if (sleepTimerPreset != SleepTimerPreset.OFF) {
                    val statusText = if (sleepTimerPreset.stopAtEndOfEpisode) {
                        "Пауза в конце серии"
                    } else {
                        "Осталось ${formatTimeMs(sleepTimerRemainingMs)}"
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SleepTimerPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = sleepTimerPreset == preset,
                        onClick = { onSetSleepTimer(preset) },
                        label = { Text(preset.label) },
                        modifier = Modifier.testTag("sleep_timer_chip_${preset.id}")
                    )
                }
            }

            HorizontalDivider()

            // 3. A-B Segment Loop
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Text("Повтор фрагмента A–B", style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    text = "Сейчас: ${formatTimeMs(currentPositionMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onMarkAbPointA,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ab_loop_set_a_button")
                ) {
                    Text(
                        text = abLoopStartMs?.let { "A: ${formatTimeMs(it)}" } ?: "Точка A"
                    )
                }
                FilledTonalButton(
                    onClick = onMarkAbPointB,
                    enabled = abLoopStartMs != null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ab_loop_set_b_button")
                ) {
                    Text(
                        text = abLoopEndMs?.let { "B: ${formatTimeMs(it)}" } ?: "Точка B"
                    )
                }
                if (abLoopStartMs != null || abLoopEndMs != null) {
                    OutlinedButton(
                        onClick = onClearAbLoop,
                        modifier = Modifier.testTag("ab_loop_clear_button")
                    ) {
                        Text("Сброс")
                    }
                }
            }

            HorizontalDivider()

            // 4. Screen Orientation Lock
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
                Text("Ориентация экрана", style = MaterialTheme.typography.titleSmall)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayerOrientationMode.entries.forEach { mode ->
                    FilterChip(
                        selected = orientationMode == mode,
                        onClick = { onSelectOrientationMode(mode) },
                        label = { Text(mode.title) },
                        modifier = Modifier.testTag("orientation_chip_${mode.id}")
                    )
                }
            }

            HorizontalDivider()

            // 5. End of Episode Behavior
            Text("По завершении серии", style = MaterialTheme.typography.titleSmall)
            PlaybackEndMode.entries.forEach { mode ->
                val selected = playbackEndMode == mode
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectPlaybackEndMode(mode) }
                        .testTag("playback_end_mode_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectPlaybackEndMode(mode) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AudioBoostControlSection(
    audioBoostPercent: Int,
    onSetAudioBoostPercent: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
                Text("Усиление громкости (DSP)", style = MaterialTheme.typography.titleSmall)
            }
            Surface(
                color = if (audioBoostPercent > 100) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$audioBoostPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (audioBoostPercent > 100) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        Slider(
            value = audioBoostPercent.toFloat(),
            onValueChange = { onSetAudioBoostPercent(it.roundToInt()) },
            valueRange = 100f..200f,
            modifier = Modifier.testTag("audio_boost_slider")
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(100, 125, 150, 175, 200).forEach { pct ->
                FilterChip(
                    selected = audioBoostPercent == pct,
                    onClick = { onSetAudioBoostPercent(pct) },
                    label = { Text("$pct%") }
                )
            }
        }
    }
}
