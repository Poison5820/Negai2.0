package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.ui.components.InstantSlider
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
 * All buttons are clean icon-only pills except EPISODES, which displays the episode counter (e.g. 1/12).
 * Scrolling or touching the bar resets and holds the HUD auto-hide timer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlayerQuickActionsBar(
    quickActions: List<PlayerQuickAction>,
    playbackState: PlaybackRuntimeState,
    settings: PlayerSettings,
    scaleMode: VideoScaleMode,
    orientationMode: PlayerOrientationMode,
    pipSupported: Boolean,
    onTriggerAction: (PlayerQuickAction) -> Unit,
    onLongPressAction: (PlayerQuickAction) -> Unit = {},
    onOpenCustomizer: () -> Unit,
    onUserInteraction: () -> Unit = {},
    onScrollStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val visibleActions = remember(quickActions, pipSupported) {
        quickActions.filter { action ->
            action != PlayerQuickAction.CHAPTERS &&
                action != PlayerQuickAction.AUDIO_BOOST &&
                (action != PlayerQuickAction.PIP || pipSupported)
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress) {
        onScrollStateChanged(listState.isScrollInProgress)
        if (listState.isScrollInProgress) {
            onUserInteraction()
        }
    }
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.isScrollInProgress) {
            onUserInteraction()
        }
    }

    val chipGap = if (settings.compactUiMode) 6.dp else 8.dp
    val buttonShape = RoundedCornerShape(12.dp)
    val accentFree = settings.playerControlsTheme.isAccentFree
    val frameless = settings.playerControlsTheme.isFrameless

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed || it.previousPressed }) {
                            onUserInteraction()
                        }
                    }
                }
            }
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

            Surface(
                color = chipState.containerColor,
                shape = buttonShape,
                border = chipState.border,
                modifier = Modifier
                    .animateItem()
                    .defaultMinSize(minWidth = 42.dp, minHeight = 36.dp)
                    .clip(buttonShape)
                    .combinedClickable(
                        onClick = {
                            onUserInteraction()
                            onTriggerAction(action)
                        },
                        onLongClick = {
                            onUserInteraction()
                            onLongPressAction(action)
                        }
                    )
                    .semantics { contentDescription = chipState.accessibilityLabel }
                    .testTag(chipState.testTag)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (chipState.counterBadge != null) 10.dp else 11.dp,
                        vertical = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = action.iconVector(),
                        contentDescription = chipState.accessibilityLabel,
                        tint = chipState.iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                    if (chipState.counterBadge != null) {
                        Text(
                            text = chipState.counterBadge,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        item(key = "customize_quick_actions_btn") {
            val customizeDesc = tr("Настроить быстрые клавиши плеера", "Customize player quick actions")
            val customizeBorder = when {
                frameless -> null
                accentFree -> BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            }
            Surface(
                color = if (frameless) Color.Transparent else Color.White.copy(alpha = 0.14f),
                shape = buttonShape,
                border = customizeBorder,
                modifier = Modifier
                    .defaultMinSize(minWidth = 42.dp, minHeight = 36.dp)
                    .clip(buttonShape)
                    .clickable {
                        onUserInteraction()
                        onOpenCustomizer()
                    }
                    .semantics { contentDescription = customizeDesc }
                    .testTag("player_customize_quick_actions_button")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = customizeDesc,
                        tint = if (accentFree) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private data class QuickChipVisualState(
    val accessibilityLabel: String,
    val counterBadge: String? = null,
    val iconTint: Color,
    val containerColor: Color,
    val border: BorderStroke? = null,
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
        accentFree -> Color.White.copy(alpha = 0.22f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
    }
    val activeSecondaryBg = when {
        frameless -> Color.Transparent
        accentFree -> Color.White.copy(alpha = 0.22f)
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
    }
    val neutralBg = if (frameless) Color.Transparent else Color.White.copy(alpha = 0.12f)
    val activeBorder = when {
        frameless -> null
        accentFree -> BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
    }

    return when (action) {
        PlayerQuickAction.SPEED -> {
            val isModified = abs(playbackState.playbackSpeed - 1.0f) >= 0.02f
            val speedStr = formatPlaybackSpeedLabel(playbackState.playbackSpeed)
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "Speed $speedStr" else "Скорость $speedStr",
                iconTint = if (isModified) primary else Color.White,
                containerColor = if (isModified) activePrimaryBg else neutralBg,
                border = if (isModified) activeBorder else null,
                testTag = "player_quick_chip_SPEED"
            )
        }

        PlayerQuickAction.AUDIO -> {
            val modified = playbackState.audioDelayMs != 0L || playbackState.audioBoostPercent > 100
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "Audio & Boost" else "Аудио и усиление",
                iconTint = primary,
                containerColor = if (modified) activePrimaryBg else neutralBg,
                border = if (modified) activeBorder else null,
                testTag = "player_open_audio_sheet_chip"
            )
        }

        PlayerQuickAction.SUBTITLES -> {
            val subActive = playbackState.selectedSubtitleId != "off"
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "Subtitles" else "Субтитры",
                iconTint = if (subActive) primary else Color.White,
                containerColor = if (subActive) activePrimaryBg else neutralBg,
                border = if (subActive) activeBorder else null,
                testTag = "player_open_subtitles_sheet_chip"
            )
        }

        PlayerQuickAction.DECODER -> {
            QuickChipVisualState(
                accessibilityLabel = (if (isEnglish) "Decoder " else "Декодер ") + playbackState.activeDecoderMode.shortBadge,
                iconTint = primary,
                containerColor = neutralBg,
                testTag = "player_open_decoder_sheet_chip"
            )
        }

        PlayerQuickAction.FRAME_INTERPOLATION -> {
            val activeMemc = settings.effectiveFrameInterpolationMode
            val isOn = activeMemc != FrameInterpolationMode.OFF
            val badge = if (isOn) activeMemc.localizedBadge(isEnglish) else null
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "Frame Interpolation ${badge ?: "Off"}" else "Интерполяция кадров ${badge ?: "Выкл"}",
                counterBadge = badge,
                iconTint = if (isOn) primary else Color.White,
                containerColor = if (isOn) activePrimaryBg else neutralBg,
                border = if (isOn) activeBorder else null,
                testTag = "player_quick_chip_FRAME_INTERPOLATION"
            )
        }

        PlayerQuickAction.ANIME4K -> {
            val activePreset = settings.effectiveAnime4KPreset
            val isOn = activePreset != Anime4KPreset.OFF
            val badge = if (isOn) activePreset.localizedShortLabel(isEnglish) else null
            QuickChipVisualState(
                accessibilityLabel = "Anime4K ${badge ?: if (isEnglish) "Off" else "Выкл"}",
                counterBadge = badge,
                iconTint = if (isOn) secondary else Color.White,
                containerColor = if (isOn) activeSecondaryBg else neutralBg,
                border = if (isOn) activeBorder else null,
                testTag = "player_open_anime4k_sheet_chip"
            )
        }

        PlayerQuickAction.EPISODES -> {
            val total = playbackState.playlist.size.coerceAtLeast(1)
            val currentNum = (playbackState.currentEpisodeIndex + 1).coerceIn(1, total)
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "Episodes $currentNum/$total" else "Серии $currentNum/$total",
                counterBadge = "$currentNum/$total",
                iconTint = primary,
                containerColor = neutralBg,
                testTag = "player_quick_chip_EPISODES"
            )
        }

        PlayerQuickAction.CHAPTERS -> {
            val chapterCount = playbackState.chapters.size
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "Chapters ($chapterCount)" else "Главы ($chapterCount)",
                iconTint = if (chapterCount > 0) primary else Color.White,
                containerColor = if (chapterCount > 0) activePrimaryBg else neutralBg,
                testTag = "player_quick_chip_CHAPTERS"
            )
        }

        PlayerQuickAction.SCALE -> {
            val isZoomed = scaleMode != VideoScaleMode.FIT
            QuickChipVisualState(
                accessibilityLabel = scaleMode.localizedShortLabel(isEnglish),
                iconTint = if (isZoomed) primary else Color.White,
                containerColor = if (isZoomed) activePrimaryBg else neutralBg,
                border = if (isZoomed) activeBorder else null,
                testTag = "player_quick_chip_SCALE"
            )
        }

        PlayerQuickAction.AUDIO_BOOST -> {
            val boosted = playbackState.audioBoostPercent > 100
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "Audio Boost" else "Усиление звука",
                iconTint = if (boosted) primary else Color.White,
                containerColor = if (boosted) activePrimaryBg else neutralBg,
                border = if (boosted) activeBorder else null,
                testTag = "player_quick_chip_AUDIO_BOOST"
            )
        }

        PlayerQuickAction.SLEEP_TIMER -> {
            val active = playbackState.isSleepTimerActive
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "Sleep Timer" else "Таймер сна",
                iconTint = if (active) primary else Color.White,
                containerColor = if (active) activePrimaryBg else neutralBg,
                border = if (active) activeBorder else null,
                testTag = "player_quick_chip_SLEEP_TIMER"
            )
        }

        PlayerQuickAction.AB_LOOP -> {
            val hasA = playbackState.abLoopStartMs != null
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "A–B Loop" else "Повтор A–B",
                iconTint = if (hasA) primary else Color.White,
                containerColor = if (hasA) activePrimaryBg else neutralBg,
                border = if (hasA) activeBorder else null,
                testTag = "player_quick_chip_AB_LOOP"
            )
        }

        PlayerQuickAction.SKIP_INTRO -> {
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "+${settings.skipIntroSeconds}s Intro" else "+${settings.skipIntroSeconds}с Опенинг",
                iconTint = primary,
                containerColor = neutralBg,
                testTag = "player_quick_chip_SKIP_INTRO"
            )
        }

        PlayerQuickAction.ORIENTATION -> {
            val lockedOrient = orientationMode != PlayerOrientationMode.AUTO
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "Screen Orientation" else "Ориентация экрана",
                iconTint = if (lockedOrient) primary else Color.White,
                containerColor = if (lockedOrient) activePrimaryBg else neutralBg,
                border = if (lockedOrient) activeBorder else null,
                testTag = "player_quick_chip_ORIENTATION"
            )
        }

        PlayerQuickAction.LOCK -> {
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "Lock Screen" else "Блокировка экрана",
                iconTint = Color.White,
                containerColor = neutralBg,
                testTag = "player_quick_chip_LOCK"
            )
        }

        PlayerQuickAction.PIP -> {
            QuickChipVisualState(
                accessibilityLabel = "PiP",
                iconTint = primary,
                containerColor = neutralBg,
                testTag = "player_quick_chip_PIP"
            )
        }

        PlayerQuickAction.TOOLS -> {
            val hasActiveTool = playbackState.isSleepTimerActive ||
                playbackState.isAbLoopActive ||
                playbackState.audioBoostPercent > 100
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "More Tools" else "Все инструменты",
                iconTint = if (hasActiveTool) primary else Color.White,
                containerColor = if (hasActiveTool) activePrimaryBg else neutralBg,
                border = if (hasActiveTool) activeBorder else null,
                testTag = "player_open_tools_sheet_chip"
            )
        }

        PlayerQuickAction.FILE_INFO -> {
            QuickChipVisualState(
                accessibilityLabel = if (isEnglish) "File Info" else "Сведения о файле",
                iconTint = primary,
                containerColor = neutralBg,
                testTag = "player_quick_chip_FILE_INFO"
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
 * Dedicated Skip Intro Interval configuration bottom sheet opened by long-pressing
 * the Skip Intro button directly in the player. Supports presets + arbitrary custom intervals.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SkipIntroConfigModalSheet(
    skipIntroSeconds: Int,
    onUpdateSkipIntroSeconds: (Int) -> Unit,
    onSkipNow: () -> Unit,
    onDismiss: () -> Unit
) {
    var customInputText by remember(skipIntroSeconds) { mutableStateOf(skipIntroSeconds.toString()) }
    val presets = listOf(60, 75, 85, 90, 110, 120)

    val sheetBg = playerPopupSheetContainerColor()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        scrimColor = PlayerPopupSheetScrimColor,
        modifier = Modifier.testTag("skip_intro_config_modal_sheet")
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = tr("Интервал пропуска опенинга", "Skip Intro Interval"),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = tr(
                                "Выберите готовый пресет или укажите любое количество секунд",
                                "Pick a preset or enter any custom duration in seconds"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = tr("+${skipIntroSeconds} с", "+${skipIntroSeconds} s"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Presets
            Text(
                text = tr("Готовые интервалы", "Presets"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { sec ->
                    FilterChip(
                        selected = skipIntroSeconds == sec,
                        onClick = {
                            onUpdateSkipIntroSeconds(sec)
                            customInputText = sec.toString()
                        },
                        label = { Text(tr("+${sec} с", "+${sec}s"), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("skip_intro_preset_chip_$sec")
                    )
                }
            }

            // Smooth Slider for arbitrary custom value (5s .. 240s) + direct input (5s .. 600s)
            Text(
                text = tr("Произвольный интервал (5–600 сек)", "Custom Interval (5–600 sec)"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            InstantSlider(
                value = skipIntroSeconds.coerceIn(5, 240).toFloat(),
                onValueChange = { raw ->
                    val v = raw.roundToInt().coerceIn(5, 240)
                    onUpdateSkipIntroSeconds(v)
                    customInputText = v.toString()
                },
                valueRange = 5f..240f,
                modifier = Modifier.testTag("skip_intro_custom_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val next = (skipIntroSeconds - 5).coerceIn(5, 600)
                        onUpdateSkipIntroSeconds(next)
                        customInputText = next.toString()
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("skip_intro_minus_5_btn")
                ) {
                    Text(tr("-5с", "-5s"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        val next = (skipIntroSeconds - 1).coerceIn(5, 600)
                        onUpdateSkipIntroSeconds(next)
                        customInputText = next.toString()
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("skip_intro_minus_1_btn")
                ) {
                    Text(tr("-1с", "-1s"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        val next = (skipIntroSeconds + 1).coerceIn(5, 600)
                        onUpdateSkipIntroSeconds(next)
                        customInputText = next.toString()
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("skip_intro_plus_1_btn")
                ) {
                    Text(tr("+1с", "+1s"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        val next = (skipIntroSeconds + 5).coerceIn(5, 600)
                        onUpdateSkipIntroSeconds(next)
                        customInputText = next.toString()
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("skip_intro_plus_5_btn")
                ) {
                    Text(tr("+5с", "+5s"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customInputText,
                    onValueChange = { text ->
                        val digits = text.filter { it.isDigit() }.take(3)
                        customInputText = digits
                        digits.toIntOrNull()?.let { parsed ->
                            if (parsed in 5..600) {
                                onUpdateSkipIntroSeconds(parsed)
                            }
                        }
                    },
                    label = { Text(tr("Точное значение (сек)", "Exact value (sec)"), style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("skip_intro_custom_input")
                )

                FilledTonalButton(
                    onClick = {
                        customInputText.toIntOrNull()?.let { parsed ->
                            onUpdateSkipIntroSeconds(parsed.coerceIn(5, 600))
                        }
                        onSkipNow()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("skip_intro_apply_and_skip_btn")
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Пропустить (+${skipIntroSeconds}с)", "Skip (+${skipIntroSeconds}s)"), style = MaterialTheme.typography.labelMedium)
                }
            }
            }
        }
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
        containerColor = PlayerPopupSheetContainerColor,
        scrimColor = PlayerPopupSheetScrimColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        tr("Скорость воспроизведения", "Playback Speed"),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = formatPlaybackSpeedLabel(playbackSpeed),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val next = (((playbackSpeed - 0.05f) * 20f).roundToInt() / 20f).coerceAtLeast(0.25f)
                        onSelectSpeed(next)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("0.05x", style = MaterialTheme.typography.labelSmall)
                }

                FilledTonalButton(
                    onClick = { onSelectSpeed(1.0f) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("1.0x Норма", "1.0x Normal"), style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = {
                        val next = (((playbackSpeed + 0.05f) * 20f).roundToInt() / 20f).coerceAtMost(3.0f)
                        onSelectSpeed(next)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("0.05x", style = MaterialTheme.typography.labelSmall)
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f).forEach { preset ->
                    FilterChip(
                        selected = abs(playbackSpeed - preset) < 0.02f,
                        onClick = { onSelectSpeed(preset) },
                        label = { Text(formatPlaybackSpeedLabel(preset), style = MaterialTheme.typography.labelSmall) },
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
        containerColor = PlayerPopupSheetContainerColor,
        scrimColor = PlayerPopupSheetScrimColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = tr("Серии в плейлисте (${playlist.size})", "Episodes in Playlist (${playlist.size})"),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            playlist.forEachIndexed { idx, ep ->
                val isCurrent = idx == currentEpisodeIndex
                Surface(
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    } else {
                        PlayerPopupCardColor
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = if (isCurrent) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
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
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White.copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = ep.episodeBadge ?: "#${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }

                            Text(
                                text = ep.cleanTitle,
                                style = MaterialTheme.typography.bodySmall,
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
                                trackColor = Color.White.copy(alpha = 0.12f)
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
        containerColor = PlayerPopupSheetContainerColor,
        scrimColor = PlayerPopupSheetScrimColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                style = MaterialTheme.typography.labelSmall,
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
        containerColor = PlayerPopupSheetContainerColor,
        scrimColor = PlayerPopupSheetScrimColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        modifier = Modifier.size(20.dp)
                    )
                    Text(tr("Таймер сна", "Sleep Timer"), style = MaterialTheme.typography.titleMedium)
                }

                if (sleepTimerPreset != SleepTimerPreset.OFF) {
                    val statusText = if (sleepTimerPreset.stopAtEndOfEpisode) {
                        tr("До конца серии", "Until end of episode")
                    } else {
                        formatTimeMs(sleepTimerRemainingMs)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SleepTimerPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = sleepTimerPreset == preset,
                        onClick = { onSetSleepTimer(preset) },
                        label = { Text(preset.localizedTitle(isEnglish), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("sleep_sheet_chip_${preset.id}")
                    )
                }
            }
        }
    }
}
