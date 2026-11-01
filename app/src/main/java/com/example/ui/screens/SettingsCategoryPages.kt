package com.example.ui.screens

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.view.WindowManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import com.example.ui.components.InstantSlider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.HiddenFolderEntity
import com.example.data.LibraryRootEntity
import com.example.model.Anime4KPreset
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.DecoderMode
import com.example.model.FrameInterpolationMode
import com.example.model.PlaybackEndMode
import com.example.model.PlayerOrientationMode
import com.example.model.PlayerQuickAction
import com.example.model.PlayerSettings
import com.example.model.SubtitleColorPreset
import com.example.model.VideoScaleMode
import com.example.ui.components.PlayerControlsThemeSelectorSection
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.localizedBadge
import com.example.ui.i18n.localizedDescription
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.localizedShortTitle
import com.example.ui.i18n.localizedSubtitle
import com.example.ui.i18n.localizedTitle
import com.example.ui.i18n.tr
import kotlin.math.roundToInt

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.VolumeUp
import com.example.player.AudioDelayProcessor
import com.example.player.BluetoothAudioSyncController

// ============================================================================
// 1. ОФОРМЛЕНИЕ И ИНТЕРФЕЙС / APPEARANCE & INTERFACE
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppearanceSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions,
    onOpenQuickActionsCustomizer: (() -> Unit)? = null
) {
    val isEnglish = LocalIsEnglish.current
    var showQuickBarCustomizer by rememberSaveable { mutableStateOf(false) }

    SettingsSectionCard(
        title = tr("Язык интерфейса", "Interface Language"),
        icon = Icons.Default.Language
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AppLanguage.entries.forEach { lang ->
                val selected = settings.appLanguage == lang
                FilterChip(
                    selected = selected,
                    onClick = { actions.onUpdateAppLanguage(lang) },
                    label = { Text(lang.localizedTitle(isEnglish)) },
                    modifier = Modifier.testTag("settings_language_chip_${lang.id}")
                )
            }
        }
    }

    SettingsSectionCard(
        title = tr("Цветовая тема OLED", "OLED Color Theme"),
        icon = Icons.Default.Palette
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { mode ->
                val selected = settings.themeMode == mode
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { actions.onUpdateThemeMode(mode) }
                        .testTag("theme_mode_option_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(mode.previewBgArgb))
                                .border(1.5.dp, Color(mode.previewAccentArgb), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(mode.previewAccentArgb))
                            )
                        }
                        Text(
                            text = mode.localizedTitle(isEnglish),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = selected,
                            onClick = { actions.onUpdateThemeMode(mode) }
                        )
                    }
                }
            }
        }
    }

    SettingsSectionCard(
        title = tr("Тема кнопок и полосы плеера", "Player Controls & Timeline Theme"),
        icon = Icons.Default.Tune
    ) {
        PlayerControlsThemeSelectorSection(
            selectedTheme = settings.playerControlsTheme,
            onSelectTheme = actions.onUpdatePlayerControlsTheme
        )
    }

    SettingsSectionCard(
        title = tr("Быстрые кнопки в плеере", "Player Quick Action Buttons"),
        icon = Icons.Default.Tune
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr(
                        "Активно кнопок: ${settings.quickActions.size} из ${PlayerQuickAction.entries.size - 2}",
                        "Active buttons: ${settings.quickActions.size} of ${PlayerQuickAction.entries.size - 2}"
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = settings.quickActions.joinToString(" • ") { it.localizedShortTitle(isEnglish) },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            FilledTonalButton(
                onClick = { showQuickBarCustomizer = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("open_quick_actions_customizer_from_settings")
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Настроить", "Customize"), style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (showQuickBarCustomizer) {
        QuickActionsCustomizerSheet(
            selectedActions = settings.quickActions,
            onUpdateActions = actions.onUpdateQuickActions,
            onDismiss = { showQuickBarCustomizer = false }
        )
    }

    SettingsSectionCard(
        title = tr("Масштаб интерфейса и автоскрытие", "UI Scale & HUD Auto-Hide"),
        icon = Icons.Default.Tune
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tr(
                    "Масштаб интерфейса и текста: ${settings.playerUiScalePercent}%",
                    "App UI & Text Scale: ${settings.playerUiScalePercent}%"
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = { actions.onUpdatePlayerUiScalePercent(100) },
                enabled = settings.playerUiScalePercent != 100,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("reset_player_ui_scale_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = tr("Сбросить масштаб до 100%", "Reset scale to 100%"),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "100%",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }

        InstantSlider(
            value = settings.playerUiScalePercent.coerceIn(75, 140).toFloat(),
            onValueChange = { actions.onUpdatePlayerUiScalePercent(it.roundToInt().coerceIn(75, 140)) },
            valueRange = 75f..140f,
            modifier = Modifier.testTag("settings_player_ui_scale_slider")
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(75, 85, 100, 115, 125, 140).forEach { pct ->
                FilterChip(
                    selected = settings.playerUiScalePercent == pct,
                    onClick = { actions.onUpdatePlayerUiScalePercent(pct) },
                    label = { Text("$pct%") },
                    modifier = Modifier.testTag("settings_player_ui_scale_chip_$pct")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            text = tr(
                "Время скрытия кнопок плеера: ${settings.controlsAutoHideSeconds} сек",
                "Player Controls Auto-Hide Delay: ${settings.controlsAutoHideSeconds} s"
            ),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )

        InstantSlider(
            value = settings.controlsAutoHideSeconds.coerceIn(2, 15).toFloat(),
            onValueChange = { actions.onUpdateControlsAutoHideSeconds(it.roundToInt().coerceIn(2, 15)) },
            valueRange = 2f..15f,
            steps = 12,
            modifier = Modifier.testTag("settings_controls_autohide_slider")
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(2, 3, 4, 5, 7, 10, 15).forEach { sec ->
                FilterChip(
                    selected = settings.controlsAutoHideSeconds == sec,
                    onClick = { actions.onUpdateControlsAutoHideSeconds(sec) },
                    label = { Text(tr("$sec сек", "${sec}s")) },
                    modifier = Modifier.testTag("settings_controls_autohide_chip_$sec")
                )
            }
        }
    }

    SettingsSectionCard(
        title = tr("Элементы интерфейса", "Interface Elements"),
        icon = Icons.Default.HighQuality
    ) {
        SettingToggleRow(
            title = tr("Показывать превью кадров у серий", "Show Episode Frame Thumbnails"),
            checked = settings.showEpisodeThumbnails,
            onCheckedChange = actions.onUpdateShowEpisodeThumbnails,
            testTag = "show_thumbnails_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Кнопка «Серии» в верхней панели плеера", "Show 'Episodes' Button in Top Player Bar"),
            checked = settings.showEpisodesButtonInHud,
            onCheckedChange = actions.onUpdateShowEpisodesButtonInHud,
            testTag = "show_episodes_hud_button_switch"
        )
    }
}

// ============================================================================
// 2. ВОСПРОИЗВЕДЕНИЕ И ТАЙМЛАЙН / PLAYBACK & TIMELINE
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaybackSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    val isEnglish = LocalIsEnglish.current
    val seekSteps = listOf(5, 10, 15, 30)
    val defaultSpeeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val introDurations = listOf(60, 75, 85, 90, 120)

    SettingsSectionCard(
        title = tr("Поведение при запуске и завершении", "Startup & End-of-Playback Behavior"),
        icon = Icons.Default.PlayArrow
    ) {
        SettingToggleRow(
            title = stringResource(R.string.settings_resume_playback_title),
            checked = settings.resumePlayback,
            onCheckedChange = actions.onUpdateResumePlayback,
            testTag = "resume_playback_switch"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            text = tr("Действие по окончании серии", "Action When Episode Ends"),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlaybackEndMode.entries.forEach { mode ->
                val selected = settings.playbackEndMode == mode
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { actions.onUpdatePlaybackEndMode(mode) }
                        .testTag("playback_end_mode_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = mode.localizedTitle(isEnglish),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = selected,
                            onClick = { actions.onUpdatePlaybackEndMode(mode) }
                        )
                    }
                }
            }
        }
    }

    SettingsSectionCard(
        title = tr("Полоса прокрутки (таймлайн)", "Timeline & Seekbar"),
        icon = Icons.Default.Bookmarks
    ) {
        SettingToggleRow(
            title = tr("Маркеры глав на полосе прокрутки", "Chapter Markers on Timeline"),
            checked = settings.showChapterMarkersOnTimeline,
            onCheckedChange = actions.onUpdateShowChapterMarkersOnTimeline,
            testTag = "show_chapter_markers_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Отображать буферизацию (кеш) серии", "Show Buffered Stream Cache on Timeline"),
            checked = settings.showBufferedProgressOnTimeline,
            onCheckedChange = actions.onUpdateShowBufferedProgressOnTimeline,
            testTag = "show_buffered_progress_switch"
        )
    }

    SettingsSectionCard(
        title = tr("Перемотка, скорость и пропуск опенинга", "Seeking, Speed & Skip Intro"),
        icon = Icons.Default.Speed
    ) {
        Text(
            text = tr(
                "Шаг перемотки: ${settings.seekStepSeconds} сек",
                "Seek step: ${settings.seekStepSeconds} s"
            ),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            seekSteps.forEach { sec ->
                FilterChip(
                    selected = settings.seekStepSeconds == sec,
                    onClick = { actions.onUpdateSeekStepSeconds(sec) },
                    label = { Text(tr("$sec сек", "${sec}s")) },
                    modifier = Modifier.testTag("seek_step_chip_$sec")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            text = tr(
                "Скорость воспроизведения по умолчанию: ${settings.defaultPlaybackSpeed}x",
                "Default playback speed: ${settings.defaultPlaybackSpeed}x"
            ),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            defaultSpeeds.forEach { spd ->
                FilterChip(
                    selected = kotlin.math.abs(settings.defaultPlaybackSpeed - spd) < 0.02f,
                    onClick = { actions.onUpdateDefaultPlaybackSpeed(spd) },
                    label = { Text("${spd}x") },
                    modifier = Modifier.testTag("default_speed_chip_$spd")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SettingToggleRow(
            title = tr("Кнопка пропуска опенинга (+Интро)", "Skip Intro Button (+Intro)"),
            checked = settings.showSkipIntroButton,
            onCheckedChange = actions.onUpdateShowSkipIntro,
            testTag = "skip_intro_switch"
        )

        if (settings.showSkipIntroButton) {
            Text(
                text = tr(
                    "Длина пропуска опенинга: +${settings.skipIntroSeconds} сек",
                    "Skip intro duration: +${settings.skipIntroSeconds} s"
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                introDurations.forEach { sec ->
                    FilterChip(
                        selected = settings.skipIntroSeconds == sec,
                        onClick = { actions.onUpdateSkipIntroSeconds(sec) },
                        label = { Text(tr("+$sec сек", "+${sec}s")) },
                        modifier = Modifier.testTag("skip_intro_sec_$sec")
                    )
                }
            }
            InstantSlider(
                value = settings.skipIntroSeconds.coerceIn(5, 240).toFloat(),
                onValueChange = { actions.onUpdateSkipIntroSeconds(it.roundToInt().coerceIn(5, 240)) },
                valueRange = 5f..240f,
                modifier = Modifier.testTag("settings_skip_intro_custom_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { actions.onUpdateSkipIntroSeconds((settings.skipIntroSeconds - 5).coerceIn(5, 600)) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("settings_skip_intro_minus_5")
                ) {
                    Text(tr("-5 с", "-5s"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { actions.onUpdateSkipIntroSeconds((settings.skipIntroSeconds - 1).coerceIn(5, 600)) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("settings_skip_intro_minus_1")
                ) {
                    Text(tr("-1 с", "-1s"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { actions.onUpdateSkipIntroSeconds((settings.skipIntroSeconds + 1).coerceIn(5, 600)) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("settings_skip_intro_plus_1")
                ) {
                    Text(tr("+1 с", "+1s"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { actions.onUpdateSkipIntroSeconds((settings.skipIntroSeconds + 5).coerceIn(5, 600)) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("settings_skip_intro_plus_5")
                ) {
                    Text(tr("+5 с", "+5s"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }

    SettingsSectionCard(
        title = tr("Картинка в картинке (PiP) и фоновый режим", "Picture-in-Picture (PiP) & Background Audio"),
        icon = Icons.Default.PictureInPictureAlt
    ) {
        SettingToggleRow(
            title = tr("Автоматический вход в PiP при сворачивании", "Auto-Enter PiP When Minimized"),
            checked = settings.autoEnterPip,
            onCheckedChange = actions.onUpdateAutoEnterPip,
            testTag = "auto_pip_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Продолжать звук в фоне", "Continue Audio in Background"),
            checked = settings.backgroundAudioPlay,
            onCheckedChange = actions.onUpdateBackgroundAudioPlay,
            testTag = "background_audio_switch"
        )
    }
}

// ============================================================================
// 3. ЖЕСТЫ И УПРАВЛЕНИЕ / GESTURES & CONTROLS
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GesturesSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    val isEnglish = LocalIsEnglish.current
    val boostSpeeds = listOf(1.5f, 2.0f, 2.5f, 3.0f)

    SettingsSectionCard(
        title = tr("Сенсорные жесты в видеоплеере", "Touch Gestures in Video Player"),
        icon = Icons.Default.TouchApp
    ) {
        SettingToggleRow(
            title = tr("Вертикальные свайпы яркости и громкости", "Vertical Brightness & Volume Swipes"),
            checked = settings.gesturesBrightnessVolume,
            onCheckedChange = actions.onUpdateGesturesBrightnessVolume,
            testTag = "gesture_bright_vol_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Горизонтальный свайп для перемотки", "Horizontal Swipe for Seeking"),
            checked = settings.gestureHorizontalSeek,
            onCheckedChange = actions.onUpdateGestureHorizontalSeek,
            testTag = "gesture_horiz_seek_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr(
                "Двойное нажатие для перемотки (±${settings.seekStepSeconds} сек)",
                "Double-Tap to Seek (±${settings.seekStepSeconds}s)"
            ),
            checked = settings.gestureDoubleTapSeek,
            onCheckedChange = actions.onUpdateGestureDoubleTapSeek,
            testTag = "gesture_double_tap_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Удержание пальца для ускорения", "Long-Press Speed Boost"),
            checked = settings.gestureLongPress2x,
            onCheckedChange = actions.onUpdateGestureLongPress2x,
            testTag = "gesture_long_press_switch"
        )

        if (settings.gestureLongPress2x) {
            Text(
                text = tr(
                    "Скорость при удержании пальца: ${settings.longPressBoostSpeed}x",
                    "Long-press boost speed: ${settings.longPressBoostSpeed}x"
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                boostSpeeds.forEach { spd ->
                    FilterChip(
                        selected = kotlin.math.abs(settings.longPressBoostSpeed - spd) < 0.02f,
                        onClick = { actions.onUpdateLongPressBoostSpeed(spd) },
                        label = { Text("${spd}x") },
                        modifier = Modifier.testTag("boost_speed_chip_$spd")
                    )
                }
            }
        }
    }

    SettingsSectionCard(
        title = tr("Ориентация экрана при запуске видео", "Default Video Screen Orientation"),
        icon = Icons.Default.ScreenRotation
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlayerOrientationMode.entries.forEach { mode ->
                val selected = settings.defaultPlayerOrientation == mode
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { actions.onUpdateDefaultPlayerOrientation(mode) }
                        .testTag("orientation_mode_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = mode.localizedTitle(isEnglish),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = selected,
                            onClick = { actions.onUpdateDefaultPlayerOrientation(mode) }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. ВИДЕО, ДЕКОДЕР И ANIME4K / VIDEO, DECODER & ANIME4K
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VideoAndAnime4KSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    VideoSettingsPage(settings = settings, actions = actions)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VideoSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    val isEnglish = LocalIsEnglish.current

    SettingsSectionCard(
        title = tr("Интерполяция кадров (MEMC)", "Frame Interpolation (MEMC)"),
        icon = Icons.Default.MotionPhotosOn
    ) {
        SettingToggleRow(
            title = tr("Включить интерполяцию кадров (MEMC)", "Enable Frame Interpolation (MEMC)"),
            checked = settings.frameInterpolationEnabled,
            onCheckedChange = actions.onUpdateFrameInterpolationEnabled,
            testTag = "frame_interpolation_enable_switch"
        )

        Text(
            text = tr("Профиль интерполяции кадров", "Frame Interpolation Profile"),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FrameInterpolationMode.entries.forEach { mode ->
                val selected = if (mode == FrameInterpolationMode.OFF) {
                    !settings.frameInterpolationEnabled
                } else {
                    settings.frameInterpolationEnabled && settings.frameInterpolationMode == mode
                }
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { actions.onUpdateFrameInterpolationMode(mode) }
                        .testTag("settings_interpolation_mode_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = mode.localizedTitle(isEnglish),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = selected,
                            onClick = { actions.onUpdateFrameInterpolationMode(mode) }
                        )
                    }
                }
            }
        }
    }

    SettingsSectionCard(
        title = tr("Декодер видео", "Video Decoder"),
        icon = Icons.Default.Memory
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DecoderMode.entries.forEach { mode ->
                val selected = settings.decoderMode == mode
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { actions.onUpdateDecoderMode(mode) }
                        .testTag("decoder_mode_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = mode.localizedTitle(isEnglish),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = selected,
                            onClick = { actions.onUpdateDecoderMode(mode) }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            text = tr("Масштаб кадра по умолчанию", "Default Aspect Ratio & Scale"),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoScaleMode.entries.forEach { scale ->
                FilterChip(
                    selected = settings.defaultScaleMode == scale,
                    onClick = { actions.onUpdateDefaultScaleMode(scale) },
                    label = { Text(scale.localizedTitle(isEnglish)) },
                    modifier = Modifier.testTag("default_scale_${scale.name}")
                )
            }
        }
    }

    SettingsSectionCard(
        title = tr("Anime4K — Реалтайм апскейлинг", "Anime4K — Real-Time Upscaling"),
        icon = Icons.Default.AutoAwesome
    ) {
        SettingToggleRow(
            title = stringResource(R.string.anime4k_enable),
            checked = settings.anime4kEnabled,
            onCheckedChange = actions.onUpdateAnime4KEnabled,
            testTag = "anime4k_enable_switch"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Anime4KPreset.entries.filter { it != Anime4KPreset.OFF }.forEach { preset ->
                val selected = settings.anime4kPreset == preset && settings.anime4kEnabled
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            actions.onUpdateAnime4KPreset(preset)
                            if (!settings.anime4kEnabled) actions.onUpdateAnime4KEnabled(true)
                        }
                        .testTag("anime4k_preset_${preset.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = preset.localizedTitle(isEnglish),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = selected,
                            onClick = {
                                actions.onUpdateAnime4KPreset(preset)
                                if (!settings.anime4kEnabled) actions.onUpdateAnime4KEnabled(true)
                            }
                        )
                    }
                }
            }
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.anime4k_strength), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${(settings.anime4kStrength * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            InstantSlider(
                value = settings.anime4kStrength,
                onValueChange = actions.onUpdateAnime4KStrength,
                valueRange = 0.1f..1.5f,
                enabled = settings.anime4kEnabled,
                modifier = Modifier.testTag("settings_anime4k_strength_slider")
            )
        }
    }
}

// ============================================================================
// 5. АУДИО И ОЗВУЧКА / AUDIO & VOICEOVER
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AudioSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    val boostPresets = listOf(100, 125, 150, 175, 200)
    val fineTuneOffsetMs = if (settings.bluetoothAudioOffsetMs == BluetoothAudioSyncController.DEFAULT_BT_OFFSET_MS) {
        0L
    } else {
        settings.bluetoothAudioOffsetMs.coerceIn(-300L, 300L)
    }

    SettingsSectionCard(
        title = tr("Внешние звуковые дорожки и озвучки", "External Audio Tracks & Voiceovers"),
        icon = Icons.Default.GraphicEq
    ) {
        SettingToggleRow(
            title = tr("Автоподключение внешних аудиодорожек", "Auto-Bind External Audio Tracks"),
            checked = settings.autoBindExternalAudio,
            onCheckedChange = actions.onUpdateAutoBindAudio,
            testTag = "auto_bind_audio_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Приоритет внешней озвучки при запуске", "Prefer External Voiceover on Startup"),
            checked = settings.preferExternalAudioOnStart,
            onCheckedChange = actions.onUpdatePreferExternalAudioOnStart,
            testTag = "prefer_external_audio_switch"
        )
    }

    SettingsSectionCard(
        title = tr("Синхронизация звука Bluetooth", "Bluetooth Audio Sync"),
        icon = Icons.Default.BluetoothAudio
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("Ручная коррекция к авто-задержке Bluetooth", "Manual Fine-Tune for Auto Bluetooth Delay"),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = AudioDelayProcessor.formatDelayLabel(fineTuneOffsetMs),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("bluetooth_fine_tune_value_text")
            )
        }

        InstantSlider(
            value = fineTuneOffsetMs.toFloat(),
            onValueChange = { actions.onUpdateBluetoothAudioOffsetMs(((it / 5f).roundToInt() * 5L).coerceIn(-300L, 300L)) },
            valueRange = -300f..300f,
            modifier = Modifier.testTag("bluetooth_offset_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { actions.onUpdateBluetoothAudioOffsetMs((fineTuneOffsetMs - 50L).coerceIn(-300L, 300L)) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("bt_offset_minus_50")
            ) {
                Text(tr("-50 мс", "-50ms"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            OutlinedButton(
                onClick = { actions.onUpdateBluetoothAudioOffsetMs((fineTuneOffsetMs - 10L).coerceIn(-300L, 300L)) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("bt_offset_minus_10")
            ) {
                Text(tr("-10 мс", "-10ms"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            FilledTonalButton(
                onClick = { actions.onUpdateBluetoothAudioOffsetMs(0L) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("bt_offset_reset")
            ) {
                Text(tr("0 мс", "0ms"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            OutlinedButton(
                onClick = { actions.onUpdateBluetoothAudioOffsetMs((fineTuneOffsetMs + 10L).coerceIn(-300L, 300L)) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("bt_offset_plus_10")
            ) {
                Text(tr("+10 мс", "+10ms"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            OutlinedButton(
                onClick = { actions.onUpdateBluetoothAudioOffsetMs((fineTuneOffsetMs + 50L).coerceIn(-300L, 300L)) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("bt_offset_plus_50")
            ) {
                Text(tr("+50 мс", "+50ms"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }

    SettingsSectionCard(
        title = tr("Программное усиление громкости (Audio Boost)", "Software Volume Boost (Audio Boost)"),
        icon = Icons.Default.VolumeUp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("Уровень громкости по умолчанию", "Default Volume Level"),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = "${settings.defaultAudioBoostPercent}%",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (settings.defaultAudioBoostPercent > 100) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        InstantSlider(
            value = settings.defaultAudioBoostPercent.toFloat(),
            onValueChange = { actions.onUpdateDefaultAudioBoostPercent(it.roundToInt()) },
            valueRange = 100f..200f,
            steps = 19,
            modifier = Modifier.testTag("default_audio_boost_slider")
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            boostPresets.forEach { pct ->
                FilterChip(
                    selected = settings.defaultAudioBoostPercent == pct,
                    onClick = { actions.onUpdateDefaultAudioBoostPercent(pct) },
                    label = {
                        Text(if (pct == 100) tr("100% (Норма)", "100% (Normal)") else "$pct%")
                    },
                    modifier = Modifier.testTag("audio_boost_preset_$pct")
                )
            }
        }
    }
}

// ============================================================================
// 6. СУБТИТРЫ (SRT / ASS) / SUBTITLES (SRT / ASS)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubtitlesSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    val isEnglish = LocalIsEnglish.current

    SettingsSectionCard(
        title = tr("Автоподключение и отображение субтитров", "Subtitle Auto-Binding & Styling"),
        icon = Icons.Default.Subtitles
    ) {
        SettingToggleRow(
            title = tr("Автоподключение внешних субтитров", "Auto-Bind External Subtitles"),
            checked = settings.autoBindExternalSubtitles,
            onCheckedChange = actions.onUpdateAutoBindSubs,
            testTag = "auto_bind_subs_switch"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Live preview box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0F18))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(vertical = 18.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            val bgColor = if (settings.subtitleBackgroundEnabled) {
                Color.Black.copy(alpha = 0.55f)
            } else {
                Color.Transparent
            }
            Text(
                text = tr(
                    "Пример отображения субтитров (1080p / 4K)",
                    "Subtitle appearance preview (1080p / 4K)"
                ),
                color = Color(settings.subtitleColorArgb),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.subtitle_font_size_label), style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { actions.onUpdateSubtitleFontSize(settings.subtitleFontSizeSp - 2f) }
                ) {
                    Icon(Icons.Default.Remove, contentDescription = tr("Уменьшить шрифт", "Decrease font size"))
                }
                Text(
                    text = "${settings.subtitleFontSizeSp.toInt()} sp",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = { actions.onUpdateSubtitleFontSize(settings.subtitleFontSizeSp + 2f) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = tr("Увеличить шрифт", "Increase font size"))
                }
            }
        }

        InstantSlider(
            value = settings.subtitleFontSizeSp.coerceIn(12f, 36f),
            onValueChange = actions.onUpdateSubtitleFontSize,
            valueRange = 12f..36f,
            steps = 24,
            modifier = Modifier.testTag("settings_subtitle_font_size_slider")
        )

        Text(stringResource(R.string.subtitle_color_label), style = MaterialTheme.typography.bodyMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SubtitleColorPreset.entries.forEach { preset ->
                FilterChip(
                    selected = settings.subtitleColorArgb == preset.argb,
                    onClick = { actions.onUpdateSubtitleColor(preset.argb) },
                    label = { Text(preset.localizedLabel(isEnglish)) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(preset.argb))
                        )
                    }
                )
            }
        }

        SettingToggleRow(
            title = stringResource(R.string.subtitle_bg_label),
            checked = settings.subtitleBackgroundEnabled,
            onCheckedChange = actions.onUpdateSubtitleBg,
            testTag = "subtitle_bg_switch"
        )
    }
}

// ============================================================================
// 5. АУДИО И СУБТИТРЫ (ОБЪЕДИНЕННАЯ СТРАНИЦА) / AUDIO & SUBTITLES
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AudioAndSubtitlesSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    AudioSettingsPage(settings = settings, actions = actions)
    SubtitlesSettingsPage(settings = settings, actions = actions)
}

@Composable
internal fun StorageAndExclusionsSettingsPage(
    settings: PlayerSettings,
    hasStorageAccess: Boolean,
    hasAllFilesAccess: Boolean,
    libraryRoots: List<LibraryRootEntity>,
    hiddenFolders: List<HiddenFolderEntity>,
    recentCount: Int = 0,
    actions: SettingsActions
) {
    StorageSettingsPage(
        settings = settings,
        hasStorageAccess = hasStorageAccess,
        hasAllFilesAccess = hasAllFilesAccess,
        libraryRoots = libraryRoots,
        hiddenFolders = hiddenFolders,
        actions = actions
    )
}

// ============================================================================
// 7. ХРАНИЛИЩЕ, ФИЛЬТРЫ И ИСКЛЮЧЕНИЯ / STORAGE, FILTERS & EXCLUSIONS
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StorageSettingsPage(
    settings: PlayerSettings,
    hasStorageAccess: Boolean,
    hasAllFilesAccess: Boolean,
    libraryRoots: List<LibraryRootEntity>,
    hiddenFolders: List<HiddenFolderEntity>,
    actions: SettingsActions
) {
    val minSizeOptions = listOf(0, 5, 15, 50, 100)

    SettingsSectionCard(
        title = tr("Доступ к памяти и подключенные папки", "Storage Access & Attached Folders"),
        icon = Icons.Default.Security
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!hasAllFilesAccess) {
                Button(
                    onClick = actions.onRequestStorageAccess,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_grant_storage_button")
                ) {
                    Text(tr("Полный доступ", "Full Access"))
                }
            }
            FilledTonalButton(
                onClick = actions.onAddSafFolder,
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_add_saf_folder_button")
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Добавить папку (SAF)", "Add Folder (SAF)"))
            }
        }

        if (libraryRoots.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = tr(
                    "Подключенные вручную папки (${libraryRoots.size})",
                    "Manually Attached Folders (${libraryRoots.size})"
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            libraryRoots.forEach { root ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(root.displayName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text(
                                root.treeUri,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { actions.onRemoveSafRoot(root.treeUri) }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = tr("Отключить папку", "Remove folder"),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    SettingsSectionCard(
        title = tr("Фильтры сканирования", "Library Scan Filters"),
        icon = Icons.Default.Storage
    ) {
        SettingToggleRow(
            title = tr("Скрывать системные папки с файлом .nomedia", "Ignore System Folders with .nomedia"),
            checked = settings.ignoreNomediaFolders,
            onCheckedChange = actions.onUpdateIgnoreNomediaFolders,
            testTag = "ignore_nomedia_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Скрывать папку Камеры (DCIM) и Записи экрана", "Hide Camera (DCIM) & Screen Recordings"),
            checked = settings.excludeCameraAndScreenshots,
            onCheckedChange = actions.onUpdateExcludeCameraAndScreenshots,
            testTag = "exclude_camera_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            text = tr(
                "Минимальный размер видеофайла: ${if (settings.minVideoSizeMb == 0) "Без лимита" else "от ${settings.minVideoSizeMb} МБ"}",
                "Minimum video file size: ${if (settings.minVideoSizeMb == 0) "No limit" else "${settings.minVideoSizeMb}+ MB"}"
            ),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            minSizeOptions.forEach { mb ->
                FilterChip(
                    selected = settings.minVideoSizeMb == mb,
                    onClick = { actions.onUpdateMinVideoSizeMb(mb) },
                    label = { Text(if (mb == 0) tr("Все файлы", "All files") else tr("от $mb МБ", "${mb}+ MB")) },
                    modifier = Modifier.testTag("min_size_chip_$mb")
                )
            }
        }
    }

    SettingsSectionCard(
        title = "${stringResource(R.string.exclusions_title)} (${hiddenFolders.size})",
        icon = Icons.Default.VisibilityOff
    ) {
        FilledTonalButton(
            onClick = actions.onPickFolderToExclude,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_pick_exclusion_folder_button")
        ) {
            Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_pick_exclusion_folder))
        }

        if (hiddenFolders.isEmpty()) {
            Text(
                text = stringResource(R.string.exclusions_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            hiddenFolders.forEach { hidden ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = hidden.folderName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = hidden.displayPath,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(onClick = { actions.onUnhideFolder(hidden.folderId) }) {
                            Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_remove_from_exclusions))
                        }
                    }
                }
            }
        }
    }

    SettingsSectionCard(
        title = tr("Обслуживание и сброс", "Maintenance & Reset"),
        icon = Icons.Default.RestartAlt
    ) {
        SettingToggleRow(
            title = tr("Запрашивать подтверждение перед удалением", "Ask Confirmation Before Deleting"),
            checked = !settings.skipDeleteConfirmation,
            onCheckedChange = { askConfirm -> actions.onUpdateSkipDeleteConfirmation(!askConfirm) },
            testTag = "confirm_before_delete_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = actions.onClearPlaybackHistory,
                modifier = Modifier
                    .weight(1f)
                    .testTag("clear_history_button")
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Очистить историю", "Clear History"))
            }
            OutlinedButton(
                onClick = actions.onResetAllSettings,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("reset_settings_button")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Сбросить всё", "Reset All"))
            }
        }
    }
}

// ============================================================================
// SHARED BUILDING BLOCKS
// ============================================================================
@Composable
internal fun SettingsSectionCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.HighQuality,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
internal fun SettingToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

// ============================================================================
// 9. ИНФОРМАЦИЯ ОБ УСТРОЙСТВЕ И ДЕКОДЕРАХ / DEVICE INFO & DECODERS
// ============================================================================
internal data class DeviceDecoderItem(
    val codecName: String,
    val mimeType: String,
    val friendlyFormat: String,
    val isVideo: Boolean,
    val isHardwareAccelerated: Boolean,
    val maxResolutionLabel: String?
)

internal enum class DecoderFilterMode {
    ALL,
    VIDEO,
    AUDIO,
    HW_ONLY,
    SW_ONLY
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DeviceInfoSettingsPage() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val displayMetrics = context.resources.displayMetrics

    val displayData = remember(context, configuration) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { context.display }.getOrNull() ?: wm?.defaultDisplay
        } else {
            wm?.defaultDisplay
        }

        val currentHz = display?.refreshRate?.roundToInt() ?: 60
        val supportedRates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && display != null) {
            display.supportedModes
                .map { it.refreshRate.roundToInt() }
                .distinct()
                .sortedDescending()
        } else {
            listOf(currentHz)
        }

        val supportedResolutions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && display != null) {
            display.supportedModes
                .map { "${it.physicalWidth}×${it.physicalHeight}" }
                .distinct()
        } else {
            emptyList()
        }

        val widthPx = displayMetrics.widthPixels
        val heightPx = displayMetrics.heightPixels
        val densityDpi = configuration.densityDpi
        val densityScale = displayMetrics.density
        val widthDp = configuration.screenWidthDp
        val heightDp = configuration.screenHeightDp

        Triple(
            "$widthPx × $heightPx px",
            "$widthDp × $heightDp dp • $densityDpi dpi (${String.format("%.2f", densityScale)}x)",
            Triple(currentHz, supportedRates.ifEmpty { listOf(currentHz) }, supportedResolutions)
        )
    }

    val decoders = remember {
        queryDeviceDecoders()
    }
    var filterMode by rememberSaveable { mutableStateOf(DecoderFilterMode.ALL) }

    val hwCount = remember(decoders) { decoders.count { it.isHardwareAccelerated } }
    val swCount = remember(decoders) { decoders.count { !it.isHardwareAccelerated } }
    val videoCount = remember(decoders) { decoders.count { it.isVideo } }
    val audioCount = remember(decoders) { decoders.count { !it.isVideo } }

    val filteredDecoders = remember(decoders, filterMode) {
        decoders.filter { item ->
            when (filterMode) {
                DecoderFilterMode.ALL -> true
                DecoderFilterMode.VIDEO -> item.isVideo
                DecoderFilterMode.AUDIO -> !item.isVideo
                DecoderFilterMode.HW_ONLY -> item.isHardwareAccelerated
                DecoderFilterMode.SW_ONLY -> !item.isHardwareAccelerated
            }
        }
    }

    SettingsSectionCard(
        title = tr("Экран, разрешение и частота обновления", "Display Resolution & Refresh Rate"),
        subtitle = tr(
            "Физическое разрешение матрицы, плотность пикселей (DPI) и поддерживаемые режимы герцовки",
            "Physical panel resolution, pixel density (DPI), and supported display refresh rates"
        ),
        icon = Icons.Default.HighQuality
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("device_info_display_card"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeviceInfoMetricRow(
                label = tr("Устройство / SoC", "Device / SoC"),
                value = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (${Build.HARDWARE})"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DeviceInfoMetricRow(
                label = tr("Разрешение экрана (пиксели)", "Screen Resolution (Pixels)"),
                value = displayData.first
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DeviceInfoMetricRow(
                label = tr("Логический размер и DPI", "Logical Viewport & DPI"),
                value = displayData.second
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DeviceInfoMetricRow(
                label = tr("Текущая частота экрана", "Current Refresh Rate"),
                value = tr("${displayData.third.first} Гц", "${displayData.third.first} Hz")
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val hzUnit = tr("Гц", "Hz")
            DeviceInfoMetricRow(
                label = tr("Поддерживаемые частоты (LTPO / Display)", "Supported Refresh Rates"),
                value = displayData.third.second.joinToString(", ") { "$it $hzUnit" }
            )
            if (displayData.third.third.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DeviceInfoMetricRow(
                    label = tr("Режимы матрицы (Display Modes)", "Native Display Modes"),
                    value = displayData.third.third.joinToString(", ")
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DeviceInfoMetricRow(
                label = tr("Версия Android / API", "Android Version / API"),
                value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            )
        }
    }

    SettingsSectionCard(
        title = tr("Аппаратные и программные декодеры", "Hardware & Software Media Decoders"),
        subtitle = tr(
            "Аппаратные (GPU/SoC: $hwCount) и программные (CPU: $swCount) декодеры видео ($videoCount) и аудио ($audioCount)",
            "Hardware (GPU/SoC: $hwCount) and software (CPU: $swCount) video ($videoCount) & audio ($audioCount) decoders"
        ),
        icon = Icons.Default.Memory
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("device_info_decoders_card"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterMode == DecoderFilterMode.ALL,
                    onClick = { filterMode = DecoderFilterMode.ALL },
                    label = { Text(tr("Все (${decoders.size})", "All (${decoders.size})")) },
                    modifier = Modifier.testTag("decoder_filter_all")
                )
                FilterChip(
                    selected = filterMode == DecoderFilterMode.HW_ONLY,
                    onClick = { filterMode = DecoderFilterMode.HW_ONLY },
                    label = { Text(tr("Аппаратные HW ($hwCount)", "Hardware HW ($hwCount)")) },
                    modifier = Modifier.testTag("decoder_filter_hw")
                )
                FilterChip(
                    selected = filterMode == DecoderFilterMode.SW_ONLY,
                    onClick = { filterMode = DecoderFilterMode.SW_ONLY },
                    label = { Text(tr("Программные SW ($swCount)", "Software SW ($swCount)")) },
                    modifier = Modifier.testTag("decoder_filter_sw")
                )
                FilterChip(
                    selected = filterMode == DecoderFilterMode.VIDEO,
                    onClick = { filterMode = DecoderFilterMode.VIDEO },
                    label = { Text(tr("Видео ($videoCount)", "Video ($videoCount)")) },
                    modifier = Modifier.testTag("decoder_filter_video")
                )
                FilterChip(
                    selected = filterMode == DecoderFilterMode.AUDIO,
                    onClick = { filterMode = DecoderFilterMode.AUDIO },
                    label = { Text(tr("Аудио ($audioCount)", "Audio ($audioCount)")) },
                    modifier = Modifier.testTag("decoder_filter_audio")
                )
            }

            filteredDecoders.forEach { decoder ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                    border = BorderStroke(
                        1.dp,
                        if (decoder.isHardwareAccelerated) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = decoder.friendlyFormat,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (decoder.maxResolutionLabel != null) {
                                    Text(
                                        text = "• ${decoder.maxResolutionLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = "${decoder.codecName} (${decoder.mimeType})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (decoder.isHardwareAccelerated) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = if (decoder.isHardwareAccelerated) {
                                    tr("HW Аппаратный", "HW Accelerated")
                                } else {
                                    tr("SW Программный", "SW Software")
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (decoder.isHardwareAccelerated) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
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

@Composable
private fun DeviceInfoMetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.48f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.52f)
        )
    }
}

private fun queryDeviceDecoders(): List<DeviceDecoderItem> {
    val result = mutableListOf<DeviceDecoderItem>()
    runCatching {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
        for (info in codecList) {
            if (info.isEncoder) continue
            val isHw = isHardwareCodec(info)
            for (mime in info.supportedTypes) {
                val lowerMime = mime.lowercase()
                val isVideo = lowerMime.startsWith("video/")
                val isAudio = lowerMime.startsWith("audio/")
                if (!isVideo && !isAudio) continue

                val maxRes = if (isVideo) {
                    runCatching {
                        val caps = info.getCapabilitiesForType(mime)?.videoCapabilities
                        val maxW = caps?.supportedWidths?.upper ?: 0
                        val maxH = caps?.supportedHeights?.upper ?: 0
                        if (maxW > 0 && maxH > 0) "${maxW}×${maxH}" else null
                    }.getOrNull()
                } else {
                    null
                }

                result += DeviceDecoderItem(
                    codecName = info.name,
                    mimeType = lowerMime,
                    friendlyFormat = friendlyMimeName(lowerMime),
                    isVideo = isVideo,
                    isHardwareAccelerated = isHw,
                    maxResolutionLabel = maxRes
                )
            }
        }
    }

    if (result.isEmpty()) {
        return listOf(
            DeviceDecoderItem("c2.qti.hevc.decoder", "video/hevc", "HEVC / H.265", true, true, "3840×2160"),
            DeviceDecoderItem("c2.qti.avc.decoder", "video/avc", "AVC / H.264", true, true, "3840×2160"),
            DeviceDecoderItem("c2.android.av1-dav1d.decoder", "video/av01", "AV1", true, false, "3840×2160"),
            DeviceDecoderItem("c2.android.aac.decoder", "audio/mp4a-latm", "AAC Audio", false, false, null),
            DeviceDecoderItem("c2.android.opus.decoder", "audio/opus", "Opus Audio", false, false, null),
            DeviceDecoderItem("c2.android.flac.decoder", "audio/flac", "FLAC Lossless", false, false, null)
        )
    }

    return result
        .distinctBy { "${it.codecName}|${it.mimeType}" }
        .sortedWith(
            compareByDescending<DeviceDecoderItem> { it.isVideo }
                .thenByDescending { it.isHardwareAccelerated }
                .thenBy { it.friendlyFormat }
        )
}

private fun isHardwareCodec(info: MediaCodecInfo): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return info.isHardwareAccelerated && !info.isSoftwareOnly
    }
    val lower = info.name.lowercase()
    return !lower.startsWith("omx.google.") &&
        !lower.startsWith("c2.android.") &&
        !lower.contains(".sw.")
}

private fun friendlyMimeName(mime: String): String = when (mime) {
    "video/hevc" -> "HEVC / H.265"
    "video/avc" -> "AVC / H.264"
    "video/av01" -> "AV1"
    "video/x-vnd.on2.vp9" -> "VP9"
    "video/x-vnd.on2.vp8" -> "VP8"
    "video/dolby-vision" -> "Dolby Vision"
    "video/mp4v-es" -> "MPEG-4"
    "video/mpeg2" -> "MPEG-2"
    "audio/mp4a-latm" -> "AAC"
    "audio/opus" -> "Opus"
    "audio/flac" -> "FLAC"
    "audio/ac3" -> "Dolby AC-3"
    "audio/eac3", "audio/eac3-joc" -> "Dolby E-AC-3 / Atmos"
    "audio/vnd.dts", "audio/vnd.dts.hd" -> "DTS / DTS-HD"
    "audio/mpeg" -> "MP3"
    "audio/vorbis" -> "Vorbis"
    "audio/raw" -> "PCM / RAW"
    else -> mime.substringAfter('/').uppercase()
}

