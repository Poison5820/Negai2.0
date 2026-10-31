package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.model.PlayerQuickAction
import com.example.model.QuickActionsSerializer
import com.example.model.QuickBarPreset

fun PlayerQuickAction.iconVector(): ImageVector = when (this) {
    PlayerQuickAction.SPEED -> Icons.Default.Speed
    PlayerQuickAction.AUDIO -> Icons.Default.GraphicEq
    PlayerQuickAction.SUBTITLES -> Icons.Default.Subtitles
    PlayerQuickAction.DECODER -> Icons.Default.Memory
    PlayerQuickAction.FRAME_INTERPOLATION -> Icons.Default.Bolt
    PlayerQuickAction.ANIME4K -> Icons.Default.AutoAwesome
    PlayerQuickAction.EPISODES -> Icons.AutoMirrored.Filled.PlaylistPlay
    PlayerQuickAction.CHAPTERS -> Icons.Default.Bookmarks
    PlayerQuickAction.SCALE -> Icons.Default.AspectRatio
    PlayerQuickAction.AUDIO_BOOST -> Icons.AutoMirrored.Filled.VolumeUp
    PlayerQuickAction.SLEEP_TIMER -> Icons.Default.Bedtime
    PlayerQuickAction.AB_LOOP -> Icons.Default.Repeat
    PlayerQuickAction.SKIP_INTRO -> Icons.Default.FastForward
    PlayerQuickAction.ORIENTATION -> Icons.Default.ScreenRotation
    PlayerQuickAction.LOCK -> Icons.Default.Lock
    PlayerQuickAction.PIP -> Icons.Default.PictureInPictureAlt
    PlayerQuickAction.TOOLS -> Icons.Default.Tune
}

/**
 * Interactive ModalBottomSheet for adding, removing, and reordering quick buttons
 * in the player's bottom toolbar (supports drag-and-drop handle, up/down arrows, and presets).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickActionsCustomizerModalSheet(
    activeActions: List<PlayerQuickAction>,
    onUpdateQuickActions: (List<PlayerQuickAction>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val availableActions = remember(activeActions) {
        QuickActionsSerializer.availableToAdd(activeActions)
    }

    val density = LocalDensity.current
    val rowSpacingPx = with(density) { 8.dp.toPx() }
    val itemHeightsPx = remember { mutableStateMapOf<String, Int>() }
    var draggedActionId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val latestActions by rememberUpdatedState(activeActions)
    val latestOnUpdate by rememberUpdatedState(onUpdateQuickActions)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .testTag("quick_actions_customizer_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Быстрые клавиши плеера",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Перетаскивайте за ≡ или меняйте порядок стрелками",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { onUpdateQuickActions(QuickActionsSerializer.DEFAULT_ACTIONS) },
                    modifier = Modifier.testTag("quick_actions_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Сбросить кнопки по умолчанию",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Live OLED Preview Strip
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Предпросмотр панели в плеере (${activeActions.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = Color(0xFF060810),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (activeActions.isEmpty()) {
                        Text(
                            text = "Панель очищена — в плеере останется кнопка быстрой настройки",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(
                                items = activeActions,
                                key = { _, item -> "preview_${item.id}" }
                            ) { index, action ->
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = "${index + 1}. ${action.shortTitle}",
                                            color = Color.White
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = action.iconVector(),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color.White.copy(alpha = 0.12f)
                                    ),
                                    border = null,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }

            // Curated Presets Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Готовые наборы кнопок:",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickBarPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = activeActions == preset.actions,
                            onClick = { onUpdateQuickActions(preset.actions) },
                            label = { Text(preset.title) },
                            modifier = Modifier.testTag("quick_actions_preset_${preset.id}")
                        )
                    }
                }
            }

            HorizontalDivider()

            // Active Buttons (Draggable + Up/Down/Remove)
            Text(
                text = "Активные кнопки на экране (${activeActions.size})",
                style = MaterialTheme.typography.titleMedium
            )

            if (activeActions.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "На панели сейчас нет кнопок. Добавьте нужные клавиши из списка ниже или выберите готовый набор.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    activeActions.forEachIndexed { index, action ->
                        val isDragging = draggedActionId == action.id
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (isDragging) 1.5.dp else 1.dp,
                                color = if (isDragging) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDragging) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 2f else 0f)
                                .graphicsLayer {
                                    if (isDragging) {
                                        translationY = dragOffsetY
                                        scaleX = 1.02f
                                        scaleY = 1.02f
                                    }
                                }
                                .onSizeChanged { sz ->
                                    itemHeightsPx[action.id] = sz.height
                                }
                                .testTag("quick_action_active_row_${action.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Drag Handle (48dp touch area)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .pointerInput(action.id) {
                                            detectVerticalDragGestures(
                                                onDragStart = {
                                                    draggedActionId = action.id
                                                    dragOffsetY = 0f
                                                },
                                                onDragEnd = {
                                                    draggedActionId = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggedActionId = null
                                                    dragOffsetY = 0f
                                                },
                                                onVerticalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount
                                                    val curIdx = latestActions.indexOf(action)
                                                    if (curIdx >= 0) {
                                                        val selfStep = ((itemHeightsPx[action.id] ?: 168) + rowSpacingPx)
                                                            .coerceAtLeast(80f)
                                                        if (dragOffsetY > selfStep * 0.58f && curIdx < latestActions.lastIndex) {
                                                            val nextId = latestActions[curIdx + 1].id
                                                            val nextStep = (itemHeightsPx[nextId] ?: 168) + rowSpacingPx
                                                            latestOnUpdate(
                                                                QuickActionsSerializer.moveItem(
                                                                    latestActions,
                                                                    curIdx,
                                                                    curIdx + 1
                                                                )
                                                            )
                                                            dragOffsetY -= nextStep
                                                        } else if (dragOffsetY < -selfStep * 0.58f && curIdx > 0) {
                                                            val prevId = latestActions[curIdx - 1].id
                                                            val prevStep = (itemHeightsPx[prevId] ?: 168) + rowSpacingPx
                                                            latestOnUpdate(
                                                                QuickActionsSerializer.moveItem(
                                                                    latestActions,
                                                                    curIdx,
                                                                    curIdx - 1
                                                                )
                                                            )
                                                            dragOffsetY += prevStep
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        .testTag("quick_action_drag_handle_${action.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragIndicator,
                                        contentDescription = "Перетащить кнопку ${action.title}",
                                        tint = if (isDragging) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                // Action Icon Badge
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = action.iconVector(),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Action Title & Type Badge
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}. ${action.shortTitle}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (action.isInstantAction) "1 касание" else "Меню",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = action.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Move Up / Move Down / Remove Buttons (48dp touch targets)
                                IconButton(
                                    onClick = {
                                        onUpdateQuickActions(
                                            QuickActionsSerializer.moveUp(activeActions, index)
                                        )
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.testTag("quick_action_up_${action.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Переместить ${action.shortTitle} выше"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        onUpdateQuickActions(
                                            QuickActionsSerializer.moveDown(activeActions, index)
                                        )
                                    },
                                    enabled = index < activeActions.lastIndex,
                                    modifier = Modifier.testTag("quick_action_down_${action.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Переместить ${action.shortTitle} ниже"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        onUpdateQuickActions(
                                            QuickActionsSerializer.removeAction(activeActions, action)
                                        )
                                    },
                                    modifier = Modifier.testTag("quick_action_remove_${action.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveCircleOutline,
                                        contentDescription = "Убрать ${action.shortTitle} с панели",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Available Buttons to Add
            Text(
                text = "Доступные для добавления (${availableActions.size})",
                style = MaterialTheme.typography.titleMedium
            )

            if (availableActions.isEmpty()) {
                Text(
                    text = "Все доступные быстрые кнопки уже добавлены на панель плеера.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableActions.forEach { action ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onUpdateQuickActions(
                                        QuickActionsSerializer.addAction(activeActions, action)
                                    )
                                }
                                .testTag("quick_action_available_row_${action.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = action.iconVector(),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = action.title,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = action.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        onUpdateQuickActions(
                                            QuickActionsSerializer.addAction(activeActions, action)
                                        )
                                    },
                                    modifier = Modifier.testTag("quick_action_add_${action.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = "Добавить ${action.title} на панель",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Summary preview block used inside SettingsTabContent to display active quick buttons
 * and launch the interactive QuickActionsCustomizerModalSheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionsSettingsSummaryContent(
    activeActions: List<PlayerQuickAction>,
    onOpenCustomizer: () -> Unit,
    onResetDefault: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Выберите нужные кнопки (серии, главы, интерполяция кадров, скорость, аудио, декодер, масштаб, A–B, таймер сна и др.) и настройте их точный порядок на нижней панели плеера.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (activeActions.isEmpty()) {
            Text(
                text = "Активные кнопки не выбраны (отображается только кнопка настройки).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activeActions.forEachIndexed { idx, action ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = action.iconVector(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "${idx + 1}. ${action.shortTitle}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenCustomizer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_open_quick_actions_editor_button")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Настроить кнопки")
            }

            if (activeActions != QuickActionsSerializer.DEFAULT_ACTIONS) {
                OutlinedButton(
                    onClick = onResetDefault,
                    modifier = Modifier.testTag("settings_reset_quick_actions_button")
                ) {
                    Text("Сброс")
                }
            }
        }
    }
}
