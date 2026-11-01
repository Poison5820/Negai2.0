package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.PlayerQuickAction
import com.example.model.QuickBarPreset
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.localizedDescription
import com.example.ui.i18n.localizedShortTitle
import com.example.ui.i18n.localizedSubtitle
import com.example.ui.i18n.localizedTitle
import com.example.ui.i18n.tr

/**
 * Interactive bottom sheet that lets the user choose which quick buttons appear
 * in the bottom player bar, reorder them with Up/Down controls, or apply a preset.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickActionsCustomizerSheet(
    selectedActions: List<PlayerQuickAction>,
    onUpdateActions: (List<PlayerQuickAction>) -> Unit,
    onDismiss: () -> Unit
) {
    val isEnglish = LocalIsEnglish.current
    val activeList = selectedActions
        .filterNot { it == PlayerQuickAction.CHAPTERS || it == PlayerQuickAction.AUDIO_BOOST }
        .distinct()
        .ifEmpty { PlayerQuickAction.defaultActions }
    val availableToAdd = com.example.model.QuickActionsSerializer.availableToAdd(activeList)

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
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = tr("Быстрые кнопки в плеере", "Player Quick Action Buttons"),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = tr(
                                "Выберите кнопки нижней панели и настройте их порядок",
                                "Choose bottom bar buttons and customize their order"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onUpdateActions(PlayerQuickAction.defaultActions) },
                    modifier = Modifier.testTag("quick_actions_reset_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Сброс", "Reset"))
                }
            }

            // Live miniature preview of the bottom bar
            Surface(
                color = Color(0xFF070C14),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(
                                "Предпросмотр панели (${activeList.size} кнопок)",
                                "Bar Preview (${activeList.size} buttons)"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = tr("Прокручивается свайпом ↔", "Horizontally scrollable ↔"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeList.forEachIndexed { idx, action ->
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(11.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = action.icon(),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = action.localizedShortTitle(isEnglish),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ready-made Presets
            Text(
                text = tr("Готовые наборы кнопок", "Quick Action Presets"),
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickBarPreset.entries.forEach { preset ->
                    val isPresetSelected = activeList == preset.actions
                    FilterChip(
                        selected = isPresetSelected,
                        onClick = { onUpdateActions(preset.actions) },
                        label = { Text(preset.localizedTitle(isEnglish)) },
                        modifier = Modifier.testTag("quick_preset_chip_${preset.id}")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Active Buttons (with Up / Down / Remove)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr(
                        "Активные кнопки в плеере (${activeList.size})",
                        "Active Player Buttons (${activeList.size})"
                    ),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = tr("Порядок слева направо", "Order from left to right"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activeList.forEachIndexed { index, action ->
                    ActiveQuickActionRow(
                        index = index,
                        totalCount = activeList.size,
                        action = action,
                        canRemove = activeList.size > 1,
                        onMoveUp = {
                            if (index > 0) {
                                val mutable = activeList.toMutableList()
                                val item = mutable.removeAt(index)
                                mutable.add(index - 1, item)
                                onUpdateActions(mutable)
                            }
                        },
                        onMoveDown = {
                            if (index < activeList.lastIndex) {
                                val mutable = activeList.toMutableList()
                                val item = mutable.removeAt(index)
                                mutable.add(index + 1, item)
                                onUpdateActions(mutable)
                            }
                        },
                        onRemove = {
                            if (activeList.size > 1) {
                                onUpdateActions(activeList.filterNot { it == action })
                            }
                        }
                    )
                }
            }

            // Available Buttons to Add
            if (availableToAdd.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = tr(
                        "Доступные кнопки для добавления (${availableToAdd.size})",
                        "Available Buttons to Add (${availableToAdd.size})"
                    ),
                    style = MaterialTheme.typography.titleSmall
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableToAdd.forEach { action ->
                        AvailableQuickActionRow(
                            action = action,
                            onAdd = {
                                onUpdateActions(activeList + action)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_actions_done_button")
            ) {
                Text(tr("Готово", "Done"))
            }
        }
    }
}

@Composable
private fun ActiveQuickActionRow(
    index: Int,
    totalCount: Int,
    action: PlayerQuickAction,
    canRemove: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val isEnglish = LocalIsEnglish.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_quick_action_${action.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${index + 1}. ${action.localizedTitle(isEnglish)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = action.localizedDescription(isEnglish),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("move_up_quick_action_${action.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = tr("Переместить левее", "Move earlier"),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("move_down_quick_action_${action.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = tr("Переместить правее", "Move later"),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onRemove,
                    enabled = canRemove,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("remove_quick_action_${action.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = tr("Убрать кнопку", "Remove button"),
                        tint = if (canRemove) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailableQuickActionRow(
    action: PlayerQuickAction,
    onAdd: () -> Unit
) {
    val isEnglish = LocalIsEnglish.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onAdd)
            .testTag("available_quick_action_${action.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.localizedTitle(isEnglish),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = action.localizedDescription(isEnglish),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = onAdd,
                modifier = Modifier.testTag("add_quick_action_button_${action.id}")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(tr("Добавить", "Add"))
            }
        }
    }
}

internal fun PlayerQuickAction.icon(): ImageVector = when (this) {
    PlayerQuickAction.AUDIO -> Icons.Default.GraphicEq
    PlayerQuickAction.SUBTITLES -> Icons.Default.Subtitles
    PlayerQuickAction.EPISODES -> Icons.AutoMirrored.Filled.PlaylistPlay
    PlayerQuickAction.CHAPTERS -> Icons.Default.Bookmark
    PlayerQuickAction.SPEED -> Icons.Default.Speed
    PlayerQuickAction.ANIME4K -> Icons.Default.AutoAwesome
    PlayerQuickAction.FRAME_INTERPOLATION -> Icons.Default.MotionPhotosOn
    PlayerQuickAction.DECODER -> Icons.Default.Memory
    PlayerQuickAction.SCALE -> Icons.Default.AspectRatio
    PlayerQuickAction.AUDIO_BOOST -> Icons.Default.GraphicEq
    PlayerQuickAction.SLEEP_TIMER -> Icons.Default.Bedtime
    PlayerQuickAction.AB_LOOP -> Icons.Default.Repeat
    PlayerQuickAction.SKIP_INTRO -> Icons.Default.FastForward
    PlayerQuickAction.ORIENTATION -> Icons.Default.ScreenRotation
    PlayerQuickAction.PIP -> Icons.Default.PictureInPictureAlt
    PlayerQuickAction.LOCK -> Icons.Default.LockOpen
    PlayerQuickAction.TOOLS -> Icons.Default.Tune
}

internal fun PlayerQuickAction.iconVector(): ImageVector = icon()

@Composable
fun QuickActionsCustomizerModalSheet(
    activeActions: List<PlayerQuickAction>,
    onUpdateQuickActions: (List<PlayerQuickAction>) -> Unit,
    onDismiss: () -> Unit
) {
    QuickActionsCustomizerSheet(
        selectedActions = activeActions,
        onUpdateActions = onUpdateQuickActions,
        onDismiss = onDismiss
    )
}
