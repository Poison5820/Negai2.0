package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import com.example.model.EpisodeItem
import com.example.model.FrameInterpolationMode
import com.example.model.PlayerOrientationMode
import com.example.model.PlayerQuickAction
import com.example.model.PlayerSettings
import com.example.model.SleepTimerPreset
import com.example.model.VideoScaleMode
import com.example.player.PlaybackRuntimeState
import com.example.ui.components.ResolutionBadgeChip
import com.example.ui.components.formatTimeMs
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.localizedBadge
import com.example.ui.i18n.localizedShortLabel
import com.example.ui.i18n.localizedTitle
import com.example.ui.i18n.tr
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dynamic, customizable bottom quick-actions bar in the video player HUD.
 * Renders the user's chosen buttons in exact order plus a compact trailing customize button.
 */
@Composable
internal fun PlayerQuickActionsBar(
    quickActions: List<PlayerQuickAction>,
    playbackState: PlaybackRuntimeState,
    settings: PlayerSettings,
    scaleMode: VideoScaleMode,
    orientationMode: PlayerOrientationMode,
    pipSupported: Boolean,
    onTriggerAction: (PlayerQuickAction) -> Unit,
    onOpenCustomizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleActions = quickActions.filter { action ->
        action != PlayerQuickAction.PIP || pipSupported
    }

    val chipGap = if (settings.compactUiMode) 6.dp else 8.dp
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("player_quick_actions_row"),
        horizontalArrangement = Arrangement.spacedBy(chipGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(
            items = visibleActions,
            key = { it.id }
        ) { action ->
            val chipState = resolveQuickChipVisuals(
                action = action,
                playbackState = playbackState,
                settings = settings,
                scaleMode = scaleMode,
                orientationMode = orientationMode
            )

            AssistChip(
                onClick = { onTriggerAction(action) },
                label = {
                    Text(
                        text = chipState.label,
                        color = Color.White,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = action.iconVector(),
                        contentDescription = null,
                        tint = chipState.iconTint,
                        modifier = Modifier.size(17.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = chipState.containerColor
                ),
                border = null,
                modifier = Modifier
                    .animateItem()
                    .testTag(chipState.testTag)
            )
        }

        item(key = "customize_quick_actions_btn") {
            val accentFree = settings.playerControlsTheme.isAccentFree
            val frameless = settings.playerControlsTheme.isFrameless
            AssistChip(
                onClick = onOpenCustomizer,
                label = {
                    Text(
                        text = if (visibleActions.isEmpty()) tr("Настроить кнопки", "Customize buttons") else tr("Кнопки", "Buttons"),
                        color = Color.White,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = tr("Настроить быстрые клавиши плеера", "Customize player quick actions"),
                        tint = if (accentFree) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (frameless) Color.Transparent else Color.White.copy(alpha = 0.14f)
                ),
                border = if (frameless) {
                    null
                } else if (accentFree) {
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                },
                modifier = Modifier.testTag("player_customize_quick_actions_button")
            )
        }
    }
}

private data class QuickChipVisualState(
    val label: String,
    val iconTint: Color,
    val containerColor: Color,
    val testTag: String
)

@Composable
private fun resolveQuickChipVisuals(
    action: PlayerQuickAction,
    playbackState: PlaybackRuntimeState,
    settings: PlayerSettings,
    scaleMode: VideoScaleMode,
    orientationMode: PlayerOrientationMode
): QuickChipVisualState {
    val isEnglish = LocalIsEnglish.current
    val accentFree = settings.playerControlsTheme.isAccentFree
    val frameless = settings.playerControlsTheme.isFrameless
    val primary = if (accentFree) Color.White.copy(alpha = 0.90f) else MaterialTheme.colorScheme.primary
    val secondary = if (accentFree) Color.White.copy(alpha = 0.90f) else MaterialTheme.colorScheme.secondary
    val activePrimaryBg = when {
        frameless -> Color.Transparent
        accentFree -> Color.White.copy(alpha = 0.20f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
    }
    val activeSecondaryBg = when {
        frameless -> Color.Transparent
        accentFree -> Color.White.copy(alpha = 0.20f)
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
    }
    val neutralBg = if (frameless) Color.Transparent else Color.White.copy(alpha = 0.12f)

    return when (action) {
        PlayerQuickAction.SPEED -> {
            val isModified = abs(playbackState.playbackSpeed - 1.0f) >= 0.02f
            val speedStr = formatPlaybackSpeedLabel(playbackState.playbackSpeed)
            QuickChipVisualState(
                label = if (isEnglish) "Speed • $speedStr" else "Скорость • $speedStr",
                iconTint = if (isModified) primary else Color.White,
                containerColor = if (isModified) activePrimaryBg else neutralBg,
                testTag = "player_quick_chip_SPEED"
            )
        }

        PlayerQuickAction.AUDIO -> {
            val unit = if (isEnglish) "ms" else "мс"
            val delaySuffix = when {
                playbackState.audioDelayMs > 0L -> " • +${playbackState.audioDelayMs}$unit"
                playbackState.audioDelayMs < 0L -> " • ${playbackState.audioDelayMs}$unit"
                else -> ""
            }
            val boostSuffix = if (playbackState.audioBoostPercent > 100) {
                " • ${playbackState.audioBoostPercent}%"
            } else {
                ""
            }
            val modified = playbackState.audioDelayMs != 0L || playbackState.audioBoostPercent > 100
            val prefix = if (isEnglish) "Audio" else "Аудио"
            QuickChipVisualState(
                label = "$prefix (${playbackState.audioTrackOptions.size})$delaySuffix$boostSuffix",
                iconTint = primary,
                containerColor = if (modified) activePrimaryBg else neutralBg,
                testTag = "player_open_audio_sheet_chip"
            )
        }

        PlayerQuickAction.SUBTITLES -> {
            val subActive = playbackState.selectedSubtitleId != "off"
            QuickChipVisualState(
                label = if (isEnglish) {
                    if (subActive) "Subtitles • ON" else "Subtitles"
                } else {
                    if (subActive) "Субтитры • Вкл" else "Субтитры"
                },
                iconTint = if (subActive) primary else Color.White,
                containerColor = if (subActive) activePrimaryBg else neutralBg,
                testTag = "player_open_subtitles_sheet_chip"
            )
        }

        PlayerQuickAction.DECODER -> {
            QuickChipVisualState(
                label = (if (isEnglish) "Decoder • " else "Декодер • ") + playbackState.activeDecoderMode.shortBadge,
                iconTint = primary,
                containerColor = neutralBg,
                testTag = "player_open_decoder_sheet_chip"
            )
        }

        PlayerQuickAction.FRAME_INTERPOLATION -> {
            val activeMemc = settings.effectiveFrameInterpolationMode
            val isOn = activeMemc != FrameInterpolationMode.OFF
            QuickChipVisualState(
                label = if (isEnglish) {
                    if (isOn) "Smooth • ${activeMemc.badgeLabel}" else "Smooth • OFF"
                } else {
                    if (isOn) "Плавность • ${activeMemc.badgeLabel}" else "Плавность • Выкл"
                },
                iconTint = if (isOn) primary else Color.White,
                containerColor = if (isOn) activePrimaryBg else neutralBg,
                testTag = "player_quick_chip_FRAME_INTERPOLATION"
            )
        }

        PlayerQuickAction.ANIME4K -> {
            val activePreset = settings.effectiveAnime4KPreset
            val isOn = activePreset != Anime4KPreset.OFF
            QuickChipVisualState(
                label = if (isOn) "Anime4K • ${activePreset.badgeLabel}" else if (isEnglish) "Anime4K • OFF" else "Anime4K • Выкл",
                iconTint = if (isOn) secondary else Color.White,
                containerColor = if (isOn) activeSecondaryBg else neutralBg,
                testTag = "player_open_anime4k_sheet_chip"
            )
        }

        PlayerQuickAction.EPISODES -> {
            val total = playbackState.playlist.size.coerceAtLeast(1)
            val currentNum = (playbackState.currentEpisodeIndex + 1).coerceIn(1, total)
            QuickChipVisualState(
                label = if (isEnglish) "Episodes ($currentNum/$total)" else "Серии ($currentNum/$total)",
                iconTint = primary,
                containerColor = neutralBg,
                testTag = "player_quick_chip_EPISODES"
            )
        }

        PlayerQuickAction.CHAPTERS -> {
            val chapterCount = playbackState.chapters.size
            QuickChipVisualState(
                label = if (isEnglish) {
                    if (chapterCount > 0) "Chapters ($chapterCount)" else "Chapters"
                } else {
                    if (chapterCount > 0) "Главы ($chapterCount)" else "Главы"
                },
                iconTint = if (chapterCount > 0) primary else Color.White,
                containerColor = if (chapterCount > 0) activePrimaryBg else neutralBg,
                testTag = "player_quick_chip_CHAPTERS"
            )
        }

        PlayerQuickAction.SCALE -> {
            val isZoomed = scaleMode != VideoScaleMode.FIT
            QuickChipVisualState(
                label = scaleMode.localizedShortLabel(isEnglish),
                iconTint = if (isZoomed) primary else Color.White,
                containerColor = if (isZoomed) activePrimaryBg else neutralBg,
                testTag = "player_quick_chip_SCALE"
            )
        }

        PlayerQuickAction.AUDIO_BOOST -> {
            val boosted = playbackState.audioBoostPercent > 100
            QuickChipVisualState(
                label = if (isEnglish) "Boost • ${playbackState.audioBoostPercent}%" else "Усиление • ${playbackState.audioBoostPercent}%",
                iconTint = if (boosted) primary else Color.White,
                containerColor = if (boosted) activePrimaryBg else neutralBg,
                testTag = "player_quick_chip_AUDIO_BOOST"
            )
        }

        PlayerQuickAction.SLEEP_TIMER -> {
            val active = playbackState.isSleepTimerActive
            val text = when {
                playbackState.sleepTimerPreset.stopAtEndOfEpisode -> if (isEnglish) "Sleep • End of ep." else "Сон • До конца"
                active -> if (isEnglish) "Sleep • ${formatTimeMs(playbackState.sleepTimerRemainingMs)}" else "Сон • ${formatTimeMs(playbackState.sleepTimerRemainingMs)}"
                else -> if (isEnglish) "Sleep Timer" else "Таймер сна"
            }
            QuickChipVisualState(
                label = text,
                iconTint = if (active) primary else Color.White,
                containerColor = if (active) activePrimaryBg else neutralBg,
                testTag = "player_quick_chip_SLEEP_TIMER"
            )
        }

        PlayerQuickAction.AB_LOOP -> {
            val hasA = playbackState.abLoopStartMs != null
            val hasB = playbackState.abLoopEndMs != null
            val text = when {
                hasA && hasB -> if (isEnglish) "A–B • Loop" else "A–B • Цикл"
                hasA -> if (isEnglish) "A–B • Set B" else "A–B • Задать B"
                else -> if (isEnglish) "A–B Loop" else "Повтор A–B"
            }
            QuickChipVisualState(
                label = text,
                iconTint = if (hasA) primary else Color.White,
                containerColor = if (hasA) activePrimaryBg else neutralBg,
                testTag = "player_quick_chip_AB_LOOP"
            )
        }

        PlayerQuickAction.SKIP_INTRO -> {
            QuickChipVisualState(
                label = if (isEnglish) "+85s Intro" else "+85с Опенинг",
                iconTint = primary,
                containerColor = neutralBg,
                testTag = "player_quick_chip_SKIP_INTRO"
            )
        }

        PlayerQuickAction.ORIENTATION -> {
            val lockedOrient = orientationMode != PlayerOrientationMode.AUTO
            QuickChipVisualState(
                label = if (isEnglish) "Rotate • ${orientationMode.localizedBadge(true)}" else "Экран • ${orientationMode.shortBadge}",
                iconTint = if (lockedOrient) primary else Color.White,
                containerColor = if (lockedOrient) activePrimaryBg else neutralBg,
                testTag = "player_quick_chip_ORIENTATION"
            )
        }

        PlayerQuickAction.LOCK -> {
            QuickChipVisualState(
                label = if (isEnglish) "Lock" else "Блокировка",
                iconTint = Color.White,
                containerColor = neutralBg,
                testTag = "player_quick_chip_LOCK"
            )
        }

        PlayerQuickAction.PIP -> {
            QuickChipVisualState(
                label = "PiP",
                iconTint = primary,
                containerColor = neutralBg,
                testTag = "player_quick_chip_PIP"
            )
        }

        PlayerQuickAction.TOOLS -> {
            val hasActiveTool = playbackState.isSleepTimerActive ||
                playbackState.isAbLoopActive ||
                playbackState.audioBoostPercent > 100
            val toolsLabel = when {
                playbackState.sleepTimerPreset.stopAtEndOfEpisode -> if (isEnglish) "More • End of ep." else "Ещё • До конца"
                playbackState.isSleepTimerActive -> if (isEnglish) "More • ${formatTimeMs(playbackState.sleepTimerRemainingMs)}" else "Ещё • ${formatTimeMs(playbackState.sleepTimerRemainingMs)}"
                else -> if (isEnglish) "More" else "Ещё"
            }
            QuickChipVisualState(
                label = toolsLabel,
                iconTint = if (hasActiveTool) primary else Color.White,
                containerColor = if (hasActiveTool) activePrimaryBg else neutralBg,
                testTag = "player_open_tools_sheet_chip"
            )
        }
    }
}

internal fun formatPlaybackSpeedLabel(speed: Float): String {
    val roundedHundredths = (speed * 100).roundToInt()
    return if (roundedHundredths % 10 == 0) {
        String.format(Locale.US, "%.1fx", roundedHundredths / 100f)
    } else {
        String.format(Locale.US, "%.2fx", roundedHundredths / 100f)
    }
}

/**
 * Dedicated Playback Speed bottom sheet opened by the SPEED quick button.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SpeedQuickModalSheet(
    playbackSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        tr("Скорость воспроизведения", "Playback Speed"),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = formatPlaybackSpeedLabel(playbackSpeed),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Slider(
                value = playbackSpeed,
                onValueChange = { raw ->
                    val stepped = ((raw * 20f).roundToInt() / 20f).coerceIn(0.25f, 3.0f)
                    onSelectSpeed(stepped)
                },
                valueRange = 0.25f..3.0f,
                modifier = Modifier.testTag("speed_sheet_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val next = (((playbackSpeed - 0.05f) * 20f).roundToInt() / 20f).coerceAtLeast(0.25f)
                        onSelectSpeed(next)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("0.05x")
                }

                FilledTonalButton(
                    onClick = { onSelectSpeed(1.0f) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("1.0x Норма", "1.0x Normal"))
                }

                OutlinedButton(
                    onClick = {
                        val next = (((playbackSpeed + 0.05f) * 20f).roundToInt() / 20f).coerceAtMost(3.0f)
                        onSelectSpeed(next)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("0.05x")
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f).forEach { preset ->
                    FilterChip(
                        selected = abs(playbackSpeed - preset) < 0.02f,
                        onClick = { onSelectSpeed(preset) },
                        label = { Text(formatPlaybackSpeedLabel(preset)) },
                        modifier = Modifier.testTag("speed_preset_chip_$preset")
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Playlist / Episodes bottom sheet opened by the EPISODES quick button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EpisodesQuickModalSheet(
    playlist: List<EpisodeItem>,
    currentEpisodeIndex: Int,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    text = tr("Серии в плейлисте (${playlist.size})", "Episodes in Playlist (${playlist.size})"),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            playlist.forEachIndexed { idx, ep ->
                val isCurrent = idx == currentEpisodeIndex
                Surface(
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = if (isCurrent) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onSelectEpisodeIndex(idx)
                            onDismiss()
                        }
                        .testTag("playlist_sheet_episode_$idx")
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = ep.episodeBadge ?: "#${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = ep.cleanTitle,
                                style = MaterialTheme.typography.bodyMedium,
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
                        }

                        if (ep.progressFraction > 0f) {
                            LinearProgressIndicator(
                                progress = { ep.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Audio Boost bottom sheet opened by the AUDIO_BOOST quick button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioBoostQuickModalSheet(
    audioBoostPercent: Int,
    onSetAudioBoostPercent: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AudioBoostControlSection(
                audioBoostPercent = audioBoostPercent,
                onSetAudioBoostPercent = onSetAudioBoostPercent
            )
            Text(
                text = tr(
                    "Использует системный DSP LoudnessEnhancer для усиления тихих озвучек без изменения скорости речи.",
                    "Uses system DSP LoudnessEnhancer to amplify quiet voiceovers without altering speech pitch or speed."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Dedicated Sleep Timer bottom sheet opened by the SLEEP_TIMER quick button.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SleepTimerQuickModalSheet(
    sleepTimerPreset: SleepTimerPreset,
    sleepTimerRemainingMs: Long,
    onSetSleepTimer: (SleepTimerPreset) -> Unit,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
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
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(tr("Таймер сна", "Sleep Timer"), style = MaterialTheme.typography.titleLarge)
                }

                if (sleepTimerPreset != SleepTimerPreset.OFF) {
                    val statusText = if (sleepTimerPreset.stopAtEndOfEpisode) {
                        tr("До конца серии", "Until end of episode")
                    } else {
                        formatTimeMs(sleepTimerRemainingMs)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SleepTimerPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = sleepTimerPreset == preset,
                        onClick = { onSetSleepTimer(preset) },
                        label = { Text(preset.localizedTitle(isEnglish)) },
                        modifier = Modifier.testTag("sleep_sheet_chip_${preset.id}")
                    )
                }
            }
        }
    }
}
