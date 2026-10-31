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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.data.HiddenFolderEntity
import com.example.data.LibraryRootEntity
import com.example.model.Anime4KPreset
import com.example.model.AppThemeMode
import com.example.model.DecoderMode
import com.example.model.FrameInterpolationMode
import com.example.model.PlaybackEndMode
import com.example.model.PlayerOrientationMode
import com.example.model.PlayerSettings
import com.example.model.QuickActionsSerializer
import com.example.model.SubtitleColorPreset
import com.example.model.VideoScaleMode
import com.example.player.DisplayRefreshRateController
import com.example.ui.components.PlayerControlsThemeSelectorSection
import kotlin.math.roundToInt

@Composable
internal fun AppearanceSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions,
    onOpenQuickActionsCustomizer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsGroupCard(title = "Тема оформления и режим OLED") {
            AppThemeMode.entries.forEach { mode ->
                val selected = settings.themeMode == mode
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { actions.onUpdateThemeMode(mode) }
                        .testTag("settings_theme_option_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(mode.title, style = MaterialTheme.typography.titleSmall)
                                if (mode.isPureOled) {
                                    Surface(
                                        color = Color.Black,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(mode.previewAccentArgb))
                                    ) {
                                        Text(
                                            text = "OLED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(mode.previewAccentArgb),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = mode.subtitle,
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

        SettingsGroupCard(title = "Тема кнопок и полосы плеера") {
            PlayerControlsThemeSelectorSection(
                selectedTheme = settings.playerControlsTheme,
                onSelectTheme = actions.onUpdatePlayerControlsTheme
            )
        }

        SettingsGroupCard(title = "Интерфейс библиотеки и HUD плеера") {
            SettingsToggleRow(
                title = "Компактный интерфейс и сгруппированные кнопки",
                subtitle = "Уменьшает лишние отступы и группирует панели действий на главной странице и в плеере",
                checked = settings.compactUiMode,
                onCheckedChange = actions.onUpdateCompactUiMode,
                testTag = "settings_compact_ui_switch"
            )

            SettingsDivider()

            SettingsToggleRow(
                title = "Превью кадров в списке серий",
                subtitle = "Извлекать миниатюры из видеофайлов при открытии папки сериала",
                checked = settings.showEpisodeThumbnails,
                onCheckedChange = actions.onUpdateShowEpisodeThumbnails,
                testTag = "settings_episode_thumbnails_switch"
            )

            SettingsDivider()

            SettingsToggleRow(
                title = "Кнопка «Серии / Главы» в верхней панели плеера",
                subtitle = "Быстрый вызов списка серий плейлиста и встроенных глав прямо из шапки плеера",
                checked = settings.showEpisodesButtonInHud,
                onCheckedChange = actions.onUpdateShowEpisodesButtonInHud,
                testTag = "settings_hud_episodes_button_switch"
            )

            SettingsDivider()

            SettingsToggleRow(
                title = "Индикаторы декодера, MEMC, Гц и Anime4K в шапке плеера",
                subtitle = "Отображать компактные значки активного декодера, частоты экрана, интерполяции кадров и шейдера",
                checked = settings.showHudBadges,
                onCheckedChange = actions.onUpdateShowHudBadges,
                testTag = "settings_hud_badges_switch"
            )
        }

        SettingsGroupCard(title = "Быстрые клавиши плеера") {
            QuickActionsSettingsSummaryContent(
                activeActions = settings.quickActions,
                onOpenCustomizer = onOpenQuickActionsCustomizer,
                onResetDefault = {
                    actions.onUpdateQuickActions(QuickActionsSerializer.DEFAULT_ACTIONS)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaybackSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsGroupCard(title = "Продолжение и поведение") {
            SettingsToggleRow(
                title = "Воспроизведение с последней точки",
                subtitle = "Автоматически продолжать видео с места остановки при открытии",
                checked = settings.resumePlayback,
                onCheckedChange = actions.onUpdateResumePlayback,
                testTag = "settings_resume_playback_switch"
            )

            SettingsDivider()

            SettingsToggleRow(
                title = "Автопереход в Картинку в картинке (PiP)",
                subtitle = "Сворачивать активное видео в плавающее окно при выходе на главный экран",
                checked = settings.autoEnterPip,
                onCheckedChange = actions.onUpdateAutoEnterPip,
                testTag = "settings_auto_pip_switch"
            )

            SettingsDivider()

            SettingsToggleRow(
                title = "Фоновое воспроизведение звука",
                subtitle = "Продолжать воспроизведение аудиодорожки при выключенном экране или сворачивании без PiP",
                checked = settings.backgroundAudioPlay,
                onCheckedChange = actions.onUpdateBackgroundAudioPlay,
                testTag = "settings_background_audio_switch"
            )
        }

        SettingsGroupCard(title = "Таймлайн, скорость и пропуск заставки") {
            Text("Шаг быстрой перемотки", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 10, 15, 30).forEach { sec ->
                    FilterChip(
                        selected = settings.seekStepSeconds == sec,
                        onClick = { actions.onUpdateSeekStepSeconds(sec) },
                        label = { Text("$sec сек") },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }

            SettingsDivider()

            Text("Скорость воспроизведения по умолчанию", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                    FilterChip(
                        selected = kotlin.math.abs(settings.defaultPlaybackSpeed - spd) < 0.02f,
                        onClick = { actions.onUpdateDefaultPlaybackSpeed(spd) },
                        label = { Text("${spd}x") },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }

            SettingsDivider()

            SettingsToggleRow(
                title = "Кнопка пропуска опенинга",
                subtitle = "Показывать быструю кнопку пропуска заставки над таймлайном",
                checked = settings.showSkipIntroButton,
                onCheckedChange = actions.onUpdateShowSkipIntro,
                testTag = "settings_skip_intro_switch"
            )

            if (settings.showSkipIntroButton) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Длительность пропуска заставки",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(60, 85, 90, 120).forEach { sec ->
                        FilterChip(
                            selected = settings.skipIntroSeconds == sec,
                            onClick = { actions.onUpdateSkipIntroSeconds(sec) },
                            label = { Text("+${sec} сек") },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
            }
        }

        SettingsGroupCard(title = "Встроенные главы (MKV / MP4 / WebM)") {
            SettingsToggleRow(
                title = "Поддержка встроенных глав видео",
                subtitle = "Автоматически извлекать оглавление (Опенинг, Эндинг, части) из контейнеров MKV и MP4",
                checked = settings.enableEmbeddedChapters,
                onCheckedChange = actions.onUpdateEnableEmbeddedChapters,
                testTag = "settings_embedded_chapters_switch"
            )

            if (settings.enableEmbeddedChapters) {
                SettingsDivider()

                SettingsToggleRow(
                    title = "Метки глав на полосе прокрутки",
                    subtitle = "Отображать засечки глав на таймлайне и кнопки перехода по главам в плеере",
                    checked = settings.showChapterMarkersOnTimeline,
                    onCheckedChange = actions.onUpdateShowChapterMarkersOnTimeline,
                    testTag = "settings_chapter_markers_switch"
                )
            }
        }

        SettingsGroupCard(title = "По окончании серии") {
            PlaybackEndMode.entries.forEach { mode ->
                SettingsRadioRow(
                    title = mode.title,
                    subtitle = mode.description,
                    selected = settings.playbackEndMode == mode,
                    onClick = { actions.onUpdatePlaybackEndMode(mode) },
                    testTag = "settings_end_mode_${mode.id}"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GesturesSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsGroupCard(title = "Сенсорные жесты в плеере") {
            SettingsToggleRow(
                title = "Вертикальные свайпы яркости и громкости",
                subtitle = "Свайп слева меняет яркость экрана, справа — громкость звука",
                checked = settings.gesturesBrightnessVolume,
                onCheckedChange = actions.onUpdateGesturesBrightnessVolume,
                testTag = "settings_gestures_bv_switch"
            )

            SettingsDivider()

            SettingsToggleRow(
                title = "Горизонтальный свайп для перемотки",
                subtitle = "Плавная перемотка проведением пальца влево или вправо по кадру",
                checked = settings.gestureHorizontalSeek,
                onCheckedChange = actions.onUpdateGestureHorizontalSeek,
                testTag = "settings_gesture_hseek_switch"
            )

            SettingsDivider()

            SettingsToggleRow(
                title = "Двойное нажатие по краям экрана",
                subtitle = "Быстрая перемотка на ±${settings.seekStepSeconds} сек двойным тапом слева/справа",
                checked = settings.gestureDoubleTapSeek,
                onCheckedChange = actions.onUpdateGestureDoubleTapSeek,
                testTag = "settings_gesture_dtap_switch"
            )

            SettingsDivider()

            SettingsToggleRow(
                title = "Долгое нажатие для ускорения",
                subtitle = "Удержание пальца на видео временно ускоряет воспроизведение",
                checked = settings.gestureLongPress2x,
                onCheckedChange = actions.onUpdateGestureLongPress2x,
                testTag = "settings_gesture_2x_switch"
            )

            if (settings.gestureLongPress2x) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Скорость при удержании",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1.5f, 2.0f, 2.5f, 3.0f).forEach { spd ->
                        FilterChip(
                            selected = kotlin.math.abs(settings.longPressBoostSpeed - spd) < 0.02f,
                            onClick = { actions.onUpdateLongPressBoostSpeed(spd) },
                            label = { Text("${spd}x") },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
            }
        }

        SettingsGroupCard(title = "Ориентация экрана плеера по умолчанию") {
            PlayerOrientationMode.entries.forEach { mode ->
                val subtitle = when (mode) {
                    PlayerOrientationMode.VIDEO_ASPECT ->
                        "Горизонтальные видео открываются в альбоме, вертикальные — в портрете"
                    PlayerOrientationMode.LANDSCAPE ->
                        "Принудительно поворачивать экран горизонтально для любого видео"
                    PlayerOrientationMode.PORTRAIT ->
                        "Всегда оставлять экран вертикальным"
                    PlayerOrientationMode.AUTO ->
                        "Следовать системному датчику автоповорота устройства"
                }
                SettingsRadioRow(
                    title = mode.title,
                    subtitle = subtitle,
                    selected = settings.defaultPlayerOrientation == mode,
                    onClick = { actions.onUpdateDefaultPlayerOrientation(mode) },
                    testTag = "settings_orientation_${mode.id}"
                )
            }
        }
    }
}

@Composable
internal fun VideoAndAnime4KSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    val context = LocalContext.current
    val refreshInfo = remember(context) {
        DisplayRefreshRateController.queryDisplayRefreshRates(context)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsGroupCard(title = "Частота экрана, LTPO и интерполяция кадров (MEMC)") {
            val modesStr = refreshInfo.supportedModesHz.joinToString(" / ") { "${it}Hz" }
            val ltpoSuffix = if (refreshInfo.isLtpoCapable) " • Поддержка LTPO / VRR" else ""
            Text(
                text = "Экран устройства: до ${refreshInfo.maxSupportedHz.roundToInt()} Гц ($modesStr)$ltpoSuffix",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            SettingsToggleRow(
                title = "Высокая частота обновления экрана (90 / 120 / 144 / 165 Гц)",
                subtitle = "Разрешать частоты выше 60 Гц для плавного интерфейса и интерполяции кадров",
                checked = settings.highRefreshRateEnabled,
                onCheckedChange = actions.onUpdateHighRefreshRateEnabled,
                testTag = "settings_high_hz_switch"
            )

            SettingsDivider()

            SettingsToggleRow(
                title = "Экономия энергии LTPO дисплея",
                subtitle = "Адаптивно подбирать кратную герцовку под частоту кадров видео, ускорение (1.5x–3x) и интерполяцию, снижая Гц на паузе",
                checked = settings.ltpoAdaptiveRefreshEnabled,
                onCheckedChange = actions.onUpdateLtpoAdaptiveRefreshEnabled,
                testTag = "settings_ltpo_switch"
            )

            SettingsDivider()

            SettingsToggleRow(
                title = "Интерполяция кадров (GPU MEMC)",
                subtitle = "Фазово-корректная генерация промежуточных кадров на GPU без рывков и с защитой на смене сцен",
                checked = settings.frameInterpolationEnabled,
                onCheckedChange = actions.onUpdateFrameInterpolationEnabled,
                testTag = "settings_memc_switch"
            )

            if (settings.frameInterpolationEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                FrameInterpolationMode.entries
                    .filter { it != FrameInterpolationMode.OFF }
                    .forEach { mode ->
                        SettingsRadioRow(
                            title = mode.title,
                            subtitle = mode.description,
                            selected = settings.frameInterpolationMode == mode,
                            onClick = { actions.onUpdateFrameInterpolationMode(mode) },
                            testTag = "settings_memc_mode_${mode.id}"
                        )
                    }
            }
        }

        SettingsGroupCard(title = "Декодирование видео (MediaCodec)") {
            DecoderMode.entries.forEach { mode ->
                SettingsRadioRow(
                    title = mode.title,
                    subtitle = mode.description,
                    selected = settings.decoderMode == mode,
                    onClick = { actions.onUpdateDecoderMode(mode) },
                    testTag = "settings_decoder_${mode.id}"
                )
            }
        }

        SettingsGroupCard(title = "Масштабирование кадра по умолчанию") {
            VideoScaleMode.entries.forEach { mode ->
                SettingsRadioRow(
                    title = mode.title,
                    subtitle = null,
                    selected = settings.defaultScaleMode == mode,
                    onClick = { actions.onUpdateDefaultScaleMode(mode) },
                    testTag = "settings_scale_${mode.id}"
                )
            }
        }

        SettingsGroupCard(title = "Anime4K — Реалтайм GLSL-апскейлинг") {
            SettingsToggleRow(
                title = "Включить Anime4K по умолчанию",
                subtitle = "Шейдерное улучшение контуров и чёткости на GPU в реальном времени",
                checked = settings.anime4kEnabled,
                onCheckedChange = actions.onUpdateAnime4KEnabled,
                testTag = "settings_anime4k_switch"
            )

            Spacer(modifier = Modifier.height(8.dp))
            Anime4KPreset.entries.filter { it != Anime4KPreset.OFF }.forEach { preset ->
                SettingsRadioRow(
                    title = preset.title,
                    subtitle = preset.description,
                    selected = settings.anime4kPreset == preset,
                    onClick = { actions.onUpdateAnime4KPreset(preset) },
                    testTag = "settings_anime4k_preset_${preset.id}"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Интенсивность шейдера: ${(settings.anime4kStrength * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = settings.anime4kStrength,
                onValueChange = actions.onUpdateAnime4KStrength,
                valueRange = 0.25f..1.5f
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AudioSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsGroupCard(title = "Внешние аудиодорожки и озвучка") {
            SettingsToggleRow(
                title = "Автопривязка внешних аудиодорожек",
                subtitle = "Находить .mka, .mp3, .aac, .flac, .wav в папке сериала и подпапках озвучки",
                checked = settings.autoBindExternalAudio,
                onCheckedChange = actions.onUpdateAutoBindAudio,
                testTag = "settings_autobind_audio_switch"
            )

            SettingsDivider()

            SettingsToggleRow(
                title = "Автоматически включать внешнюю озвучку",
                subtitle = "При наличии внешней дорожки выбирать её вместо встроенного аудиопотока",
                checked = settings.preferExternalAudioOnStart,
                onCheckedChange = actions.onUpdatePreferExternalAudioOnStart,
                testTag = "settings_prefer_ext_audio_switch"
            )
        }

        SettingsGroupCard(title = "Усиление громкости (Audio Boost)") {
            Text(
                text = "Громкость при запуске: ${settings.defaultAudioBoostPercent}%",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Позволяет усилить тихие дорожки озвучки выше 100% через аппаратный LoudnessEnhancer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = settings.defaultAudioBoostPercent.toFloat(),
                onValueChange = { actions.onUpdateDefaultAudioBoostPercent(it.roundToInt()) },
                valueRange = 100f..200f
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(100, 125, 150, 175, 200).forEach { pct ->
                    FilterChip(
                        selected = settings.defaultAudioBoostPercent == pct,
                        onClick = { actions.onUpdateDefaultAudioBoostPercent(pct) },
                        label = { Text("$pct%") },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubtitlesSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsGroupCard(title = "Автозагрузка субтитров") {
            SettingsToggleRow(
                title = "Автопривязка внешних субтитров (SRT / ASS)",
                subtitle = "Автоматически подключать подходящие субтитры из папки и подпапок Subs",
                checked = settings.autoBindExternalSubtitles,
                onCheckedChange = actions.onUpdateAutoBindSubs,
                testTag = "settings_autobind_subs_switch"
            )
        }

        SettingsGroupCard(title = "Оформление и предпросмотр") {
            Text(
                text = "Размер шрифта: ${settings.subtitleFontSizeSp.roundToInt()} sp",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = settings.subtitleFontSizeSp,
                onValueChange = actions.onUpdateSubtitleFontSize,
                valueRange = 14f..40f,
                modifier = Modifier.testTag("settings_subtitle_size_slider")
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text("Цвет текста субтитров", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SubtitleColorPreset.entries.forEach { colorPreset ->
                    val isSelected = settings.subtitleColorArgb == colorPreset.argb
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(colorPreset.argb))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.45f),
                                shape = CircleShape
                            )
                            .clickable { actions.onUpdateSubtitleColor(colorPreset.argb) }
                            .testTag("settings_sub_color_${colorPreset.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = colorPreset.label,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            SettingsDivider()

            SettingsToggleRow(
                title = "Полупрозрачная подложка под текстом",
                subtitle = "Повышает читаемость субтитров на ярких сценах",
                checked = settings.subtitleBackgroundEnabled,
                onCheckedChange = actions.onUpdateSubtitleBg,
                testTag = "settings_subtitle_bg_switch"
            )

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF060912))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(vertical = 18.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = if (settings.subtitleBackgroundEnabled) {
                        Modifier
                            .background(Color(0xB806080E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    } else {
                        Modifier
                    }
                ) {
                    Text(
                        text = "Пример отображения субтитров SRT / ASS",
                        color = Color(settings.subtitleColorArgb),
                        fontSize = settings.subtitleFontSizeSp.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StorageAndExclusionsSettingsPage(
    settings: PlayerSettings,
    hasStorageAccess: Boolean,
    hasAllFilesAccess: Boolean,
    libraryRoots: List<LibraryRootEntity>,
    hiddenFolders: List<HiddenFolderEntity>,
    recentCount: Int,
    actions: SettingsActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsGroupCard(title = "Доступ к хранилищу устройства") {
            val statusTitle = when {
                hasAllFilesAccess -> "Полный доступ к хранилищу активен"
                hasStorageAccess -> "Активен базовый доступ к видеофайлам"
                else -> "Доступ к хранилищу не предоставлен"
            }
            val statusDesc = when {
                hasAllFilesAccess -> "Все папки памяти сканируются автоматически вместе с внешними аудио (.mka/.mp3) и субтитрами (.srt/.ass)."
                hasStorageAccess -> "Видео доступны через MediaStore. Для автоматического поиска соседних папок озвучки (.mka) и субтитров рекомендуется разрешить полный доступ."
                else -> "Разрешите доступ к хранилищу, чтобы плеер автоматически нашёл все папки с видео, а ненужные вы могли добавить в исключения."
            }
            Text(statusTitle, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                statusDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasAllFilesAccess) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = actions.onRequestStorageAccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_grant_storage_button")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (!hasStorageAccess) "Разрешить доступ к хранилищу" else "Разрешить доступ ко всем папкам"
                    )
                }
            }
        }

        SettingsGroupCard(title = "Исключения и фильтрация папок") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = actions.onPickFolderToExclude,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_add_exclusion_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Добавить папку")
                }

                OutlinedButton(
                    onClick = actions.onOpenExclusionsSheet,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_manage_hidden_button")
                ) {
                    Icon(Icons.Outlined.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Список (${hiddenFolders.size})")
                }
            }

            SettingsDivider()

            SettingsToggleRow(
                title = "Пропускать папки с файлом .nomedia",
                subtitle = "Игнорировать служебные кеши мессенджеров и приложений",
                checked = settings.ignoreNomediaFolders,
                onCheckedChange = actions.onUpdateIgnoreNomediaFolders,
                testTag = "settings_ignore_nomedia_switch"
            )

            SettingsDivider()

            SettingsToggleRow(
                title = "Исключить Камеру и Скриншоты",
                subtitle = "Скрывать личные ролики из DCIM/Camera и записи экрана Screenshots",
                checked = settings.excludeCameraAndScreenshots,
                onCheckedChange = actions.onUpdateExcludeCameraAndScreenshots,
                testTag = "settings_exclude_camera_switch"
            )

            SettingsDivider()

            Text("Минимальный размер видеофайла", style = MaterialTheme.typography.titleSmall)
            Text(
                "Отсекает короткие стикеры и мелкие клипы при сканировании",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "Все файлы", 5 to "От 5 МБ", 20 to "От 20 МБ", 50 to "От 50 МБ").forEach { (mb, label) ->
                    FilterChip(
                        selected = settings.minVideoSizeMb == mb,
                        onClick = { actions.onUpdateMinVideoSizeMb(mb) },
                        label = { Text(label) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }
        }

        SettingsGroupCard(title = "Внешние накопители (USB / SD)") {
            Text(
                "При необходимости вы можете вручную подключить папку с внешнего USB-накопителя или сетевого провайдера документов:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (libraryRoots.isNotEmpty()) {
                libraryRoots.forEach { root ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = root.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { actions.onRemoveSafRoot(root.treeUri) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Отключить папку")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            OutlinedButton(
                onClick = actions.onAddSafFolder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Подключить внешнюю папку (SAF)")
            }
        }

        SettingsGroupCard(title = "Данные и сброс") {
            OutlinedButton(
                onClick = actions.onClearPlaybackHistory,
                enabled = recentCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Очистить историю просмотра ($recentCount)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = actions.onResetAllSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Сбросить настройки по умолчанию")
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SettingsRadioRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 10.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
