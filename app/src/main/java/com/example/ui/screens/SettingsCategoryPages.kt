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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

    // Language selector
    SettingsSectionCard(
        title = tr("Язык интерфейса", "Interface Language"),
        subtitle = tr(
            "Переключение между системным языком, русским и английским в реальном времени",
            "Switch between system auto-detection, Russian, and English in real time"
        ),
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
        Text(
            text = settings.appLanguage.localizedSubtitle(isEnglish),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    SettingsSectionCard(
        title = tr("Цветовая тема OLED", "OLED Color Theme"),
        subtitle = tr(
            "Выберите акцентную палитру приложения с глубоким черным фоном #000000",
            "Select the app accent palette on a true #000000 OLED black background"
        ),
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
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { actions.onUpdateThemeMode(mode) }
                        .testTag("theme_mode_option_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(mode.previewBgArgb))
                                .border(1.5.dp, Color(mode.previewAccentArgb), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(mode.previewAccentArgb))
                            )
                        }
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
        subtitle = tr(
            "Визуальный стиль центральных кнопок Play/Pause, чипов и таймлайна в плеере (8 вариантов, включая без фона и без акцентов)",
            "Visual style of center Play/Pause controls, action chips, and timeline in the player (8 presets, including frameless and accent-free)"
        ),
        icon = Icons.Default.Tune
    ) {
        PlayerControlsThemeSelectorSection(
            selectedTheme = settings.playerControlsTheme,
            onSelectTheme = actions.onUpdatePlayerControlsTheme
        )
    }

    SettingsSectionCard(
        title = tr("Быстрые кнопки в плеере", "Player Quick Action Buttons"),
        subtitle = tr(
            "Выберите, какие кнопки показывать в нижней панели видеоплеера, и настройте их порядок",
            "Choose which buttons appear in the bottom bar of the video player and reorder them"
        ),
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
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = settings.quickActions.joinToString(" • ") { it.localizedShortTitle(isEnglish) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            FilledTonalButton(
                onClick = { showQuickBarCustomizer = true },
                modifier = Modifier.testTag("open_quick_actions_customizer_from_settings")
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Настроить", "Customize"))
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
        title = tr("Масштаб элементов плеера (DPI) и автоскрытие", "Player UI Scale (DPI) & HUD Auto-Hide"),
        subtitle = tr(
            "Адаптация размера кнопок и текста под плотность пикселей (DPI) экрана и настройка таймера скрытия элементов",
            "Adapt player button and text sizes to screen DPI density and configure the HUD auto-hide timer"
        ),
        icon = Icons.Default.Tune
    ) {
        SettingToggleRow(
            title = tr("Автоучёт DPI экрана устройства", "Automatic Screen DPI Scaling"),
            subtitle = tr(
                "Автоматически подбирать базовый масштаб элементов плеера под физическую ширину и DPI дисплея",
                "Automatically adjust base player HUD scale according to physical screen width and DPI"
            ),
            checked = settings.playerAutoDpiScale,
            onCheckedChange = actions.onUpdatePlayerAutoDpiScale,
            testTag = "player_auto_dpi_switch"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tr(
                    "Масштаб элементов и текста плеера: ${settings.playerUiScalePercent}%",
                    "Player UI & Text Scale: ${settings.playerUiScalePercent}%"
                ),
                style = MaterialTheme.typography.titleSmall
            )
            if (settings.playerUiScalePercent != 100) {
                OutlinedButton(
                    onClick = { actions.onUpdatePlayerUiScalePercent(100) },
                    modifier = Modifier.testTag("reset_player_ui_scale_btn")
                ) {
                    Text("100%")
                }
            }
        }

        Slider(
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
                "Время до скрытия элементов плеера: ${settings.controlsAutoHideSeconds} сек",
                "Player HUD Controls Auto-Hide Delay: ${settings.controlsAutoHideSeconds} s"
            ),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = tr(
                "При прокрутке нижней панели кнопок или перемещении ползунка таймер автоматически сбрасывается",
                "Scrolling the bottom button bar or scrubbing the timeline automatically resets the timer"
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Slider(
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
        title = tr("Плотность и элементы интерфейса", "UI Density & Elements"),
        subtitle = tr(
            "Настройка компактности карточек, превью кадров и информационных плашек",
            "Configure card density, episode frame thumbnails, and HUD badges"
        ),
        icon = Icons.Default.Tune
    ) {
        SettingToggleRow(
            title = tr("Компактный режим интерфейса", "Compact UI Mode"),
            subtitle = tr(
                "Уменьшенные отступы карточек папок и серий — помещается больше элементов на экране",
                "Reduced padding on folder and episode cards so more items fit on screen"
            ),
            checked = settings.compactUiMode,
            onCheckedChange = actions.onUpdateCompactUiMode,
            testTag = "compact_ui_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Показывать превью кадров у серий", "Show Episode Frame Thumbnails"),
            subtitle = tr(
                "Извлекать миниатюры из видеофайлов внутри папок (отключите для мгновенного открытия больших папок)",
                "Extract video frame thumbnails inside folders (disable for instant loading of huge folders)"
            ),
            checked = settings.showEpisodeThumbnails,
            onCheckedChange = actions.onUpdateShowEpisodeThumbnails,
            testTag = "show_thumbnails_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Кнопка «Серии» в верхней панели плеера", "Show 'Episodes' Button in Top Player Bar"),
            subtitle = tr(
                "Быстрый вызов переключателя эпизодов прямо из верхней шапки во время просмотра сериала",
                "Quickly open the episode switcher directly from the top header while watching a series"
            ),
            checked = settings.showEpisodesButtonInHud,
            onCheckedChange = actions.onUpdateShowEpisodesButtonInHud,
            testTag = "show_episodes_hud_button_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Технические бейджи в плеере (Разрешение / Гц / Аудио)", "Technical HUD Badges (Resolution / Hz / Audio)"),
            subtitle = tr(
                "Отображать чипы 4K/1080p, частоту экрана (120 Гц), активный Anime4K и внешнюю озвучку в верхней панели плеера",
                "Display 4K/1080p resolution, refresh rate (120 Hz), active Anime4K, and external dub chips in the top player bar"
            ),
            checked = settings.showHudBadges,
            onCheckedChange = actions.onUpdateShowHudBadges,
            testTag = "show_hud_badges_switch"
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
        subtitle = tr(
            "Автоматическое продолжение просмотра и переход между сериями",
            "Automatic playback resume and episode transitions"
        ),
        icon = Icons.Default.PlayArrow
    ) {
        SettingToggleRow(
            title = stringResource(R.string.settings_resume_playback_title),
            subtitle = stringResource(R.string.settings_resume_playback_subtitle),
            checked = settings.resumePlayback,
            onCheckedChange = actions.onUpdateResumePlayback,
            testTag = "resume_playback_switch"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            text = tr("Действие по окончании серии", "Action When Episode Ends"),
            style = MaterialTheme.typography.titleSmall
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
                            onClick = { actions.onUpdatePlaybackEndMode(mode) }
                        )
                    }
                }
            }
        }
    }

    SettingsSectionCard(
        title = tr("Встроенные главы (Chapters) в видео", "Embedded Video Chapters"),
        subtitle = tr(
            "Распознавание глав контейнера MKV / MP4 (Опенинг, Часть A, Часть B, Эндинг) и отображение засечек на полосе прокрутки",
            "Parse MKV / MP4 container chapters (Opening, Part A, Part B, Ending) and display markers on the timeline"
        ),
        icon = Icons.Default.PlayArrow
    ) {
        SettingToggleRow(
            title = tr("Поддержка встроенных глав (Chapters)", "Enable Embedded Chapters"),
            subtitle = tr(
                "Извлекать оглавление из файла и показывать текущую главу и список глав в плеере",
                "Extract chapter list from video containers and display the current chapter in the player"
            ),
            checked = settings.enableEmbeddedChapters,
            onCheckedChange = actions.onUpdateEnableEmbeddedChapters,
            testTag = "enable_embedded_chapters_switch"
        )
        if (settings.enableEmbeddedChapters) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingToggleRow(
                title = tr("Маркеры глав на полосе прокрутки", "Chapter Markers on Timeline"),
                subtitle = tr(
                    "Рисовать вертикальные деления глав прямо на таймлайне плеера",
                    "Draw vertical chapter ticks directly on the player timeline bar"
                ),
                checked = settings.showChapterMarkersOnTimeline,
                onCheckedChange = actions.onUpdateShowChapterMarkersOnTimeline,
                testTag = "show_chapter_markers_switch"
            )
        }
    }

    SettingsSectionCard(
        title = tr("Перемотка, скорость и пропуск опенинга", "Seeking, Speed & Skip Intro"),
        subtitle = tr(
            "Точная настройка шага перемотки и кнопки быстрого пропуска заставки",
            "Fine-tune seek step interval and the quick intro skip button"
        ),
        icon = Icons.Default.Speed
    ) {
        Text(
            text = tr(
                "Шаг перемотки кнопками и двойным тапом: ${settings.seekStepSeconds} сек",
                "Button & double-tap seek step: ${settings.seekStepSeconds} s"
            ),
            style = MaterialTheme.typography.titleSmall
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
                "Стандартная скорость воспроизведения: ${settings.defaultPlaybackSpeed}x",
                "Default playback speed: ${settings.defaultPlaybackSpeed}x"
            ),
            style = MaterialTheme.typography.titleSmall
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
            title = tr("Кнопка быстрого пропуска опенинга (+Интро)", "Quick Skip Intro Button (+Intro)"),
            subtitle = tr(
                "Показывать в плеере кнопку мгновенного прыжка вперёд на длину заставки",
                "Show a one-tap button in the player to jump ahead by the intro duration"
            ),
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
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = tr(
                    "Совет: удерживайте кнопку «+${settings.skipIntroSeconds}с Опенинг» прямо в плеере для быстрой настройки произвольного интервала",
                    "Tip: long-press the '+${settings.skipIntroSeconds}s Intro' button directly in the player to configure any custom interval"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Slider(
                value = settings.skipIntroSeconds.coerceIn(5, 240).toFloat(),
                onValueChange = { actions.onUpdateSkipIntroSeconds(it.roundToInt().coerceIn(5, 240)) },
                valueRange = 5f..240f,
                modifier = Modifier.testTag("settings_skip_intro_custom_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { actions.onUpdateSkipIntroSeconds((settings.skipIntroSeconds - 5).coerceIn(5, 600)) },
                    modifier = Modifier.weight(1f).testTag("settings_skip_intro_minus_5")
                ) {
                    Text(tr("-5 с", "-5s"))
                }
                OutlinedButton(
                    onClick = { actions.onUpdateSkipIntroSeconds((settings.skipIntroSeconds - 1).coerceIn(5, 600)) },
                    modifier = Modifier.weight(1f).testTag("settings_skip_intro_minus_1")
                ) {
                    Text(tr("-1 с", "-1s"))
                }
                OutlinedButton(
                    onClick = { actions.onUpdateSkipIntroSeconds((settings.skipIntroSeconds + 1).coerceIn(5, 600)) },
                    modifier = Modifier.weight(1f).testTag("settings_skip_intro_plus_1")
                ) {
                    Text(tr("+1 с", "+1s"))
                }
                OutlinedButton(
                    onClick = { actions.onUpdateSkipIntroSeconds((settings.skipIntroSeconds + 5).coerceIn(5, 600)) },
                    modifier = Modifier.weight(1f).testTag("settings_skip_intro_plus_5")
                ) {
                    Text(tr("+5 с", "+5s"))
                }
            }
        }
    }

    SettingsSectionCard(
        title = tr("Картинка в картинке (PiP) и фоновый режим", "Picture-in-Picture (PiP) & Background Audio"),
        subtitle = tr(
            "Работа плеера при сворачивании приложения на рабочий стол",
            "Player behavior when minimizing the app to the home screen"
        ),
        icon = Icons.Default.PlayArrow
    ) {
        SettingToggleRow(
            title = tr("Автоматический вход в PiP при сворачивании", "Auto-Enter PiP When Minimized"),
            subtitle = tr(
                "Переводить активное видео в плавающее окно поверх других приложений при жесте «Домой»",
                "Switch active video into a floating window over other apps when swiping Home"
            ),
            checked = settings.autoEnterPip,
            onCheckedChange = actions.onUpdateAutoEnterPip,
            testTag = "auto_pip_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Продолжать звук в фоне", "Continue Audio in Background"),
            subtitle = tr(
                "Не ставить видео на паузу при выключении экрана или переходе в другое приложение без PiP",
                "Keep audio playing when locking the screen or switching apps without PiP"
            ),
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
        subtitle = tr(
            "Включайте или отключайте отдельные жесты для защиты от случайных касаний",
            "Enable or disable individual gestures to prevent accidental touches"
        ),
        icon = Icons.Default.TouchApp
    ) {
        SettingToggleRow(
            title = tr("Вертикальные свайпы яркости и громкости", "Vertical Brightness & Volume Swipes"),
            subtitle = tr(
                "Свайп вверх/вниз слева меняет яркость экрана, справа — громкость звука",
                "Swipe up/down on the left side to adjust screen brightness, or on the right side for volume"
            ),
            checked = settings.gesturesBrightnessVolume,
            onCheckedChange = actions.onUpdateGesturesBrightnessVolume,
            testTag = "gesture_bright_vol_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Горизонтальный свайп для точной перемотки", "Horizontal Swipe for Fine Seeking"),
            subtitle = tr(
                "Проведите пальцем влево или вправо по кадру для плавного скраббинга с предпросмотром времени",
                "Slide left or right across the video frame for smooth scrubbing with live time preview"
            ),
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
            subtitle = tr(
                "Двойной тап по левому или правому краю кадра мгновенно перематывает видео",
                "Double-tap the left or right edge of the frame to seek backward or forward instantly"
            ),
            checked = settings.gestureDoubleTapSeek,
            onCheckedChange = actions.onUpdateGestureDoubleTapSeek,
            testTag = "gesture_double_tap_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Удержание пальца для ускорения", "Long-Press Speed Boost"),
            subtitle = tr(
                "Временно ускорять воспроизведение пока палец удерживается на экране",
                "Temporarily speed up playback while holding your finger on the screen"
            ),
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
                style = MaterialTheme.typography.titleSmall
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
        title = tr("Ориентация экрана в плеере", "Player Screen Orientation"),
        subtitle = tr(
            "Как поворачивать экран при открытии видеофайла",
            "How the screen rotates when opening a video file"
        ),
        icon = Icons.Default.TouchApp
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
        title = tr("Частота экрана (LTPO / 120 Гц) и интерполяция кадров", "Display Refresh Rate (LTPO / 120 Hz) & Frame Interpolation"),
        subtitle = tr(
            "Автоматическая адаптация герцовки дисплея под FPS видео и сглаживание движения (MEMC)",
            "Automatic display refresh rate matching to video FPS and GPU motion smoothing (MEMC)"
        ),
        icon = Icons.Default.Speed
    ) {
        SettingToggleRow(
            title = tr("Высокая частота обновления экрана (90 / 120 / 165 Гц)", "High Display Refresh Rate (90 / 120 / 165 Hz)"),
            subtitle = tr(
                "Разрешить использование максимальной частоты экрана устройства для плавной анимации и видео",
                "Allow the app to use the device's maximum display refresh rate for smooth UI and video"
            ),
            checked = settings.highRefreshRateEnabled,
            onCheckedChange = actions.onUpdateHighRefreshRateEnabled,
            testTag = "high_refresh_rate_switch"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SettingToggleRow(
            title = tr("Адаптивная частота экрана LTPO", "LTPO Adaptive Display Refresh Rate"),
            subtitle = tr(
                "Автоматически выбирать кратную частоту дисплея под FPS ролика (например, 48/72/96/120 Гц для 24p или 60/120 Гц для 60p) и снижать герцовку на паузе для экономии заряда",
                "Automatically select an exact integer-multiple refresh rate for the video's FPS (e.g. 48/72/96/120 Hz for 24p or 60/120 Hz for 60p) and lower Hz when paused to save battery"
            ),
            checked = settings.ltpoAdaptiveRefreshEnabled,
            onCheckedChange = actions.onUpdateLtpoAdaptiveRefreshEnabled,
            testTag = "ltpo_adaptive_refresh_switch"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SettingToggleRow(
            title = tr("Включить интерполяцию кадров (MEMC)", "Enable Frame Interpolation (MEMC)"),
            subtitle = tr(
                "Реалтайм генерация промежуточных кадров на GPU для повышения плавности 24/30 FPS видео (по умолчанию выключено)",
                "Real-time GPU intermediate frame synthesis for smoother 24/30 FPS video (off by default)"
            ),
            checked = settings.frameInterpolationEnabled,
            onCheckedChange = actions.onUpdateFrameInterpolationEnabled,
            testTag = "frame_interpolation_enable_switch"
        )

        Text(
            text = tr("Профиль интерполяции кадров", "Frame Interpolation Profile"),
            style = MaterialTheme.typography.titleSmall
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
                            onClick = { actions.onUpdateFrameInterpolationMode(mode) }
                        )
                    }
                }
            }
        }
    }

    SettingsSectionCard(
        title = tr("Декодер видео (Media3 / ExoPlayer)", "Video Decoder (Media3 / ExoPlayer)"),
        subtitle = tr(
            "Управление аппаратным ускорением 4K HEVC 10-bit / AV1 / VP9",
            "Hardware acceleration mode for 4K HEVC 10-bit / AV1 / VP9"
        ),
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
                            onClick = { actions.onUpdateDecoderMode(mode) }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            text = tr("Масштаб кадра по умолчанию", "Default Aspect Ratio & Scale"),
            style = MaterialTheme.typography.titleSmall
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
        title = tr("Anime4K GLSL — Шейдерный апскейлинг", "Anime4K GLSL — Real-Time Shader Upscaling"),
        subtitle = tr(
            "Двухпроходное восстановление контуров и повышение чёткости в реальном времени на GPU (по умолчанию выключено)",
            "Two-pass real-time GPU line reconstruction and sharpening (off by default)"
        ),
        icon = Icons.Default.AutoAwesome
    ) {
        SettingToggleRow(
            title = stringResource(R.string.anime4k_enable),
            subtitle = tr(
                "Активировать шейдерный конвейер OpenGL ES при воспроизведении",
                "Activate the OpenGL ES shader pipeline during video playback"
            ),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.localizedTitle(isEnglish), style = MaterialTheme.typography.titleSmall)
                            Text(
                                preset.localizedDescription(isEnglish),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
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
    val btOffsetPresets = listOf(-250L, -200L, -150L, -100L, -50L, 0L)

    SettingsSectionCard(
        title = tr("Внешние звуковые дорожки и озвучки", "External Audio Tracks & Voiceovers"),
        subtitle = tr(
            "Автоматическое сопоставление файлов из папок Rus Sound, Audio, Озвучка по номеру серии",
            "Automatic episode-matched audio binding from Rus Sound, Audio, and Dub subfolders"
        ),
        icon = Icons.Default.GraphicEq
    ) {
        SettingToggleRow(
            title = tr("Автоподключение внешних аудиодорожек", "Auto-Bind External Audio Tracks"),
            subtitle = tr(
                "Сканировать соседние папки и автоматически прикреплять внешние .mka / .mp3 / .flac / .ac3 к видео",
                "Scan sibling folders and automatically attach matching .mka / .mp3 / .flac / .ac3 files to episodes"
            ),
            checked = settings.autoBindExternalAudio,
            onCheckedChange = actions.onUpdateAutoBindAudio,
            testTag = "auto_bind_audio_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Приоритет внешней озвучки при запуске", "Prefer External Voiceover on Startup"),
            subtitle = tr(
                "Автоматически выбирать найденную внешнюю русскую озвучку вместо встроенной японской/английской дорожки",
                "Automatically select discovered external voiceover tracks instead of the embedded audio track"
            ),
            checked = settings.preferExternalAudioOnStart,
            onCheckedChange = actions.onUpdatePreferExternalAudioOnStart,
            testTag = "prefer_external_audio_switch"
        )
    }

    SettingsSectionCard(
        title = tr("Bluetooth-синхронизация (A2DP / LE Audio / TWS)", "Bluetooth Audio Sync (A2DP / LE Audio / TWS)"),
        subtitle = tr(
            "Автоматическая компенсация задержки беспроводных наушников и колонок без обрезки звука",
            "Automatic wireless headphone & speaker latency compensation without clipping PCM samples"
        ),
        icon = Icons.Default.BluetoothAudio
    ) {
        SettingToggleRow(
            title = tr("Автоподстройка задержки для Bluetooth", "Automatic Bluetooth Latency Compensation"),
            subtitle = tr(
                "Автоматически определять подключение Bluetooth-наушников (A2DP / BLE Audio) в реальном времени и сдвигать часы видео для идеального совпадения губ и речи",
                "Detect Bluetooth headphones (A2DP / BLE Audio) in real time and shift the video clock for accurate lip-sync"
            ),
            checked = settings.bluetoothAudioAutoSync,
            onCheckedChange = actions.onUpdateBluetoothAudioAutoSync,
            testTag = "bluetooth_auto_sync_switch"
        )

        if (settings.bluetoothAudioAutoSync) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Базовая компенсация для Bluetooth A2DP", "Base Compensation for Bluetooth A2DP"),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = tr(
                            "Применяется только когда звук идёт через Bluetooth (для LE Audio масштабируется автоматически)",
                            "Applied only when audio routes through Bluetooth (automatically scaled for LE Audio)"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr("${settings.bluetoothAudioOffsetMs} мс", "${settings.bluetoothAudioOffsetMs} ms"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = settings.bluetoothAudioOffsetMs.toFloat(),
                onValueChange = { actions.onUpdateBluetoothAudioOffsetMs((it / 10f).roundToInt() * 10L) },
                valueRange = -400f..0f,
                modifier = Modifier.testTag("bluetooth_offset_slider")
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                btOffsetPresets.forEach { presetMs ->
                    FilterChip(
                        selected = settings.bluetoothAudioOffsetMs == presetMs,
                        onClick = { actions.onUpdateBluetoothAudioOffsetMs(presetMs) },
                        label = {
                            Text(
                                when (presetMs) {
                                    -150L -> tr("-150 мс (Стандарт TWS)", "-150 ms (Standard TWS)")
                                    -200L -> tr("-200 мс (Кодек LDAC/AAC)", "-200 ms (LDAC/AAC Codec)")
                                    -100L -> tr("-100 мс (aptX LL / Игровой)", "-100 ms (aptX LL / Gaming)")
                                    else -> tr("${presetMs} мс", "${presetMs} ms")
                                }
                            )
                        },
                        modifier = Modifier.testTag("bt_offset_preset_$presetMs")
                    )
                }
            }
        }
    }

    SettingsSectionCard(
        title = tr("Программное усиление громкости (Audio Boost)", "Software Volume Boost (Audio Boost)"),
        subtitle = tr(
            "Усиление тихих озвучек до 200% с защитой от хрипов и клиппинга (Soft-Knee Limiter)",
            "Boost quiet voiceovers up to 200% with soft-knee limiter clipping protection"
        ),
        icon = Icons.Default.GraphicEq
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("Уровень громкости по умолчанию", "Default Volume Level"),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "${settings.defaultAudioBoostPercent}%",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (settings.defaultAudioBoostPercent > 100) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        Slider(
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
        subtitle = tr(
            "Поддержка встроенных и внешних .SRT / .ASS / .SSA / .VTT с очисткой тегов форматирования",
            "Support for embedded and external .SRT / .ASS / .SSA / .VTT with formatting tag cleanup"
        ),
        icon = Icons.Default.Subtitles
    ) {
        SettingToggleRow(
            title = tr("Автоподключение внешних субтитров", "Auto-Bind External Subtitles"),
            subtitle = tr(
                "Автоматически находить субтитры в текущей папке и подпапках Subs / Subtitles / Rus Sub",
                "Automatically discover subtitles in the current folder and Subs / Subtitles / Rus Sub subfolders"
            ),
            checked = settings.autoBindExternalSubtitles,
            onCheckedChange = actions.onUpdateAutoBindSubs,
            testTag = "auto_bind_subs_switch"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Live preview box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A0F18))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .padding(vertical = 22.dp, horizontal = 16.dp),
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
                style = MaterialTheme.typography.titleMedium,
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
            Text(stringResource(R.string.subtitle_font_size_label))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { actions.onUpdateSubtitleFontSize(settings.subtitleFontSizeSp - 2f) }
                ) {
                    Icon(Icons.Default.Remove, contentDescription = tr("Уменьшить шрифт", "Decrease font size"))
                }
                Text(
                    text = "${settings.subtitleFontSizeSp.toInt()} sp",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = { actions.onUpdateSubtitleFontSize(settings.subtitleFontSizeSp + 2f) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = tr("Увеличить шрифт", "Increase font size"))
                }
            }
        }

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
            subtitle = tr(
                "Полупрозрачная плашка позади текста для идеальной читаемости на светлых сценах",
                "Translucent dark box behind subtitle text for clear readability on bright scenes"
            ),
            checked = settings.subtitleBackgroundEnabled,
            onCheckedChange = actions.onUpdateSubtitleBg,
            testTag = "subtitle_bg_switch"
        )
    }
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
        subtitle = if (hasAllFilesAccess) {
            tr(
                "Полный доступ ко всему хранилищу активен (включая внешние .mka и .ass)",
                "Full storage access is active (including external .mka audio and .ass subtitles)"
            )
        } else if (hasStorageAccess) {
            tr(
                "Базовый доступ к видео активен. Для автопоиска внешних .mka/.ass рекомендуется полный доступ",
                "Basic video access is active. Full storage access is recommended for automatic .mka/.ass discovery"
            )
        } else {
            tr(
                "Разрешение на чтение памяти ещё не предоставлено",
                "Storage read permission has not been granted yet"
            )
        },
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
                style = MaterialTheme.typography.titleSmall
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
                            Text(root.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                root.treeUri,
                                style = MaterialTheme.typography.bodySmall,
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
        title = tr("Умные фильтры сканирования", "Smart Library Scan Filters"),
        subtitle = tr(
            "Отсеивание системного мусора, коротких клипов и записей экрана",
            "Filter out system clutter, short clips, and screen recordings"
        ),
        icon = Icons.Default.Storage
    ) {
        SettingToggleRow(
            title = tr("Скрывать системные папки с файлом .nomedia", "Ignore System Folders with .nomedia"),
            subtitle = tr(
                "Игнорировать кэш мессенджеров и игр при прямом сканировании хранилища",
                "Skip messenger and game caches during direct storage scanning"
            ),
            checked = settings.ignoreNomediaFolders,
            onCheckedChange = actions.onUpdateIgnoreNomediaFolders,
            testTag = "ignore_nomedia_switch"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingToggleRow(
            title = tr("Скрывать папку Камеры (DCIM) и Записи экрана", "Hide Camera (DCIM) & Screen Recordings"),
            subtitle = tr(
                "Оставить в библиотеке только фильмы, сериалы, аниме и загрузки",
                "Keep only movies, series, anime, and downloads in your library"
            ),
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
            style = MaterialTheme.typography.titleSmall
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
        subtitle = tr(
            "Папки из этого списка и все их подпапки полностью исключены из библиотеки",
            "Folders in this list and all their subfolders are excluded from the library"
        ),
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
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = hidden.displayPath,
                                style = MaterialTheme.typography.bodySmall,
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
        subtitle = tr(
            "Настройки удаления файлов, очистка истории просмотра и возврат настроек по умолчанию",
            "File deletion preferences, clear watch history, or restore all settings to defaults"
        ),
        icon = Icons.Default.RestartAlt
    ) {
        SettingToggleRow(
            title = tr("Запрашивать подтверждение перед удалением", "Ask Confirmation Before Deleting"),
            subtitle = tr(
                "Показывать предупреждение перед безвозвратным удалением папок и серий с накопителя",
                "Show warning dialog before permanently deleting folders and episodes from storage"
            ),
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
    subtitle: String,
    icon: ImageVector = Icons.Default.HighQuality,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
internal fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
