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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.PlaybackEndMode
import com.example.model.PlayerControlsTheme
import com.example.model.PlayerOrientationMode
import com.example.model.SleepTimerPreset
import com.example.model.VideoScaleMode
import com.example.ui.components.PlayerControlsThemeSelectorSection
import com.example.ui.components.formatTimeMs
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.localizedBadge
import com.example.ui.i18n.localizedShortLabel
import com.example.ui.i18n.localizedSubtitle
import com.example.ui.i18n.localizedTitle
import com.example.ui.i18n.tr
import kotlin.math.roundToInt

enum class VerticalGestureMode {
    NONE,
    BRIGHTNESS,
    VOLUME
}

enum class GestureHudType {
    BRIGHTNESS,
    VOLUME
}

data class GestureHudState(
    val type: GestureHudType,
    val percent: Int
)

@Composable
fun VerticalGestureHudCard(
    mode: VerticalGestureMode,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val type = if (mode == VerticalGestureMode.BRIGHTNESS) GestureHudType.BRIGHTNESS else GestureHudType.VOLUME
    val pct = (fraction.coerceIn(0f, 1f) * 100f).roundToInt()
    VerticalGestureHudCard(
        hudState = GestureHudState(type = type, percent = pct),
        modifier = modifier
    )
}

@Composable
fun VerticalGestureHudCard(
    hudState: GestureHudState,
    modifier: Modifier = Modifier
) {
    Surface(
        color = PlayerPopupSheetContainerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = modifier.testTag("vertical_gesture_hud")
    ) {
        Column(
            modifier = Modifier
                .width(138.dp)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (hudState.type == GestureHudType.BRIGHTNESS) {
                    Icons.Default.BrightnessHigh
                } else {
                    Icons.Default.VolumeUp
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = if (hudState.type == GestureHudType.BRIGHTNESS) {
                    tr("Яркость ${hudState.percent}%", "Brightness ${hudState.percent}%")
                } else {
                    tr("Громкость ${hudState.percent}%", "Volume ${hudState.percent}%")
                },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            LinearProgressIndicator(
                progress = { (hudState.percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun LongPressSpeedPill(
    speed: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        color = PlayerPopupSheetContainerColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
        modifier = modifier.testTag("long_press_speed_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = tr("Ускорение ${speed}x", "Speed Boost ${speed}x"),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
fun ScreenLockFloatingPill(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = PlayerPopupSheetContainerColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onUnlock)
            .testTag("unlock_screen_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = tr("Разблокировать экран", "Unlock screen"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Column {
                Text(
                    text = tr("Экран заблокирован", "Screen Locked"),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Text(
                    text = tr("Нажмите для разблокировки", "Tap to unlock controls"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerToolsModalSheet(
    playbackSpeed: Float,
    scaleMode: VideoScaleMode,
    audioBoostPercent: Int,
    sleepTimerPreset: SleepTimerPreset,
    sleepTimerRemainingMs: Long,
    abLoopPointAMs: Long?,
    abLoopPointBMs: Long?,
    currentPositionMs: Long,
    playbackEndMode: PlaybackEndMode,
    controlsTheme: PlayerControlsTheme = PlayerControlsTheme.CLASSIC_OLED,
    activeOrientationMode: PlayerOrientationMode = PlayerOrientationMode.VIDEO_ASPECT,
    playerUiScalePercent: Int = 100,
    playerAutoDpiScale: Boolean = true,
    controlsAutoHideSeconds: Int = 4,
    skipIntroSeconds: Int = 85,
    onSelectSpeed: (Float) -> Unit,
    onSelectScaleMode: (VideoScaleMode) -> Unit,
    onSetAudioBoostPercent: (Int) -> Unit,
    onSetSleepTimer: (SleepTimerPreset) -> Unit,
    onMarkAbPointA: () -> Unit,
    onMarkAbPointB: () -> Unit,
    onClearAbLoop: () -> Unit,
    onSelectPlaybackEndMode: (PlaybackEndMode) -> Unit,
    onSelectControlsTheme: (PlayerControlsTheme) -> Unit = {},
    onSelectOrientationMode: (PlayerOrientationMode) -> Unit = {},
    onUpdatePlayerUiScalePercent: (Int) -> Unit = {},
    onUpdatePlayerAutoDpiScale: (Boolean) -> Unit = {},
    onUpdateControlsAutoHideSeconds: (Int) -> Unit = {},
    onUpdateSkipIntroSeconds: (Int) -> Unit = {},
    onLockControls: () -> Unit,
    onOpenQuickActionsCustomizer: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val isEnglish = LocalIsEnglish.current
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tr("Инструменты просмотра", "Playback Tools"),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            onOpenQuickActionsCustomizer()
                        },
                        modifier = Modifier.testTag("sheet_open_quick_actions_customizer")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Кнопки", "Buttons"))
                    }
                    FilledTonalButton(
                        onClick = {
                            onLockControls()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("sheet_lock_controls_button")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Блок", "Lock"))
                    }
                }
            }

            // 0. Player Controls & Progress Bar Theme
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = tr(
                            "Тема кнопок и полосы (${controlsTheme.localizedTitle(isEnglish)})",
                            "Controls & Timeline Theme (${controlsTheme.localizedTitle(isEnglish)})"
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                PlayerControlsThemeSelectorSection(
                    selectedTheme = controlsTheme,
                    onSelectTheme = onSelectControlsTheme,
                    compactChipsOnly = true
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 0a. Global App & Player UI Scale (75% - 140% + Auto DPI) & HUD Auto-Hide Timeout
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr(
                            "Масштаб интерфейса ($playerUiScalePercent%)",
                            "UI & Text Scale ($playerUiScalePercent%)"
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
                    FilledTonalButton(
                        onClick = { onUpdatePlayerUiScalePercent(100) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("tools_reset_ui_scale_btn")
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("100%", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Slider(
                    value = playerUiScalePercent.coerceIn(75, 140).toFloat(),
                    onValueChange = { onUpdatePlayerUiScalePercent(it.roundToInt().coerceIn(75, 140)) },
                    valueRange = 75f..140f,
                    modifier = Modifier.testTag("tools_ui_scale_slider")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(80, 90, 100, 110, 125, 140).forEach { pct ->
                        FilterChip(
                            selected = playerUiScalePercent == pct,
                            onClick = { onUpdatePlayerUiScalePercent(pct) },
                            label = { Text("$pct%") },
                            modifier = Modifier.testTag("tools_ui_scale_chip_$pct")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tr(
                        "Время скрытия кнопок плеера: $controlsAutoHideSeconds сек",
                        "HUD Controls Auto-Hide: ${controlsAutoHideSeconds}s"
                    ),
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 3, 4, 5, 7, 10, 15).forEach { sec ->
                        FilterChip(
                            selected = controlsAutoHideSeconds == sec,
                            onClick = { onUpdateControlsAutoHideSeconds(sec) },
                            label = { Text(tr("$sec с", "${sec}s")) },
                            modifier = Modifier.testTag("tools_autohide_chip_$sec")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 0b. Screen Orientation Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = tr(
                            "Поворот экрана (${activeOrientationMode.localizedTitle(isEnglish)})",
                            "Screen Rotation (${activeOrientationMode.localizedTitle(isEnglish)})"
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerOrientationMode.entries.forEach { mode ->
                        FilterChip(
                            selected = activeOrientationMode == mode,
                            onClick = { onSelectOrientationMode(mode) },
                            label = { Text(mode.localizedBadge(isEnglish)) },
                            modifier = Modifier.testTag("tools_orientation_chip_${mode.id}")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 1. Playback Speed
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = tr(
                            "Скорость воспроизведения (${playbackSpeed}x)",
                            "Playback Speed (${playbackSpeed}x)"
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    speeds.forEach { spd ->
                        FilterChip(
                            selected = kotlin.math.abs(playbackSpeed - spd) < 0.02f,
                            onClick = { onSelectSpeed(spd) },
                            label = { Text("${spd}x") },
                            modifier = Modifier.testTag("speed_chip_${spd}x")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 2. Video Scale / Aspect Ratio
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = tr("Масштаб и соотношение сторон", "Aspect Ratio & Scale"),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoScaleMode.entries.forEach { mode ->
                        FilterChip(
                            selected = scaleMode == mode,
                            onClick = { onSelectScaleMode(mode) },
                            label = { Text(mode.localizedTitle(isEnglish)) },
                            modifier = Modifier.testTag("scale_chip_${mode.name}")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 3. Software Audio Boost (100% - 200%)
            AudioBoostControlSection(
                audioBoostPercent = audioBoostPercent,
                onSetAudioBoostPercent = onSetAudioBoostPercent
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 4. A-B Loop Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = tr(
                            "Повтор отрезка A–B (сейчас ${formatTimeMs(currentPositionMs)})",
                            "A–B Segment Loop (now ${formatTimeMs(currentPositionMs)})"
                        ),
                        style = MaterialTheme.typography.titleSmall
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
                            .testTag("ab_mark_a_button")
                    ) {
                        Text(
                            if (abLoopPointAMs != null) {
                                "A: ${formatTimeMs(abLoopPointAMs)}"
                            } else {
                                tr("Точка A", "Point A")
                            }
                        )
                    }
                    FilledTonalButton(
                        onClick = onMarkAbPointB,
                        enabled = abLoopPointAMs != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ab_mark_b_button")
                    ) {
                        Text(
                            if (abLoopPointBMs != null) {
                                "B: ${formatTimeMs(abLoopPointBMs)}"
                            } else {
                                tr("Точка B", "Point B")
                            }
                        )
                    }
                    if (abLoopPointAMs != null || abLoopPointBMs != null) {
                        OutlinedButton(
                            onClick = onClearAbLoop,
                            modifier = Modifier.testTag("ab_clear_button")
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = tr("Сбросить A-B", "Clear A-B loop"),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 5. Sleep Timer
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    val timerTitle = if (sleepTimerPreset == SleepTimerPreset.OFF) {
                        tr("Таймер сна", "Sleep Timer")
                    } else if (sleepTimerPreset == SleepTimerPreset.END_OF_EPISODE) {
                        tr("Таймер сна: До конца серии", "Sleep Timer: Until end of episode")
                    } else {
                        tr(
                            "Таймер сна: осталось ${formatTimeMs(sleepTimerRemainingMs)}",
                            "Sleep Timer: ${formatTimeMs(sleepTimerRemainingMs)} left"
                        )
                    }
                    Text(timerTitle, style = MaterialTheme.typography.titleSmall)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SleepTimerPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = sleepTimerPreset == preset,
                            onClick = { onSetSleepTimer(preset) },
                            label = { Text(preset.localizedBadge(isEnglish)) },
                            modifier = Modifier.testTag("sleep_timer_chip_${preset.id}")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 6. Playback End Mode
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = tr("Режим перехода между сериями", "Episode Transition Mode"),
                    style = MaterialTheme.typography.titleSmall
                )
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
                            .testTag("sheet_end_mode_${mode.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode.localizedTitle(isEnglish), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    mode.localizedSubtitle(isEnglish),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectPlaybackEndMode(mode) }
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
fun AudioBoostControlSection(
    audioBoostPercent: Int,
    onSetAudioBoostPercent: (Int) -> Unit
) {
    val boostPresets = listOf(100, 125, 150, 175, 200)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = tr("Усиление громкости (Audio Boost)", "Volume Boost (Audio Boost)"),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = "$audioBoostPercent%",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (audioBoostPercent > 100) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        Slider(
            value = audioBoostPercent.toFloat(),
            onValueChange = { onSetAudioBoostPercent(it.roundToInt()) },
            valueRange = 100f..200f,
            steps = 19,
            modifier = Modifier.testTag("player_audio_boost_slider")
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            boostPresets.forEach { pct ->
                FilterChip(
                    selected = audioBoostPercent == pct,
                    onClick = { onSetAudioBoostPercent(pct) },
                    label = { Text(if (pct == 100) tr("100% (Норма)", "100% (Normal)") else "$pct%") },
                    modifier = Modifier.testTag("player_boost_chip_$pct")
                )
            }
        }
    }
}
