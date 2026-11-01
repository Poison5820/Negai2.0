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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.HiddenFolderEntity
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.EpisodeSortOption
import com.example.model.FolderSortOption
import com.example.model.PlayerControlsTheme
import com.example.ui.components.PlayerControlsThemeSelectorSection
import com.example.ui.components.formatFileSize
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.localizedActiveBadge
import com.example.ui.i18n.localizedActiveHint
import com.example.ui.i18n.localizedAscBadge
import com.example.ui.i18n.localizedDescBadge
import com.example.ui.i18n.localizedShortLabel
import com.example.ui.i18n.localizedSubtitle
import com.example.ui.i18n.localizedTitle
import com.example.ui.i18n.tr

@Composable
internal fun FolderSortHeaderBar(
    folderCount: Int,
    sortOption: FolderSortOption,
    ascending: Boolean,
    onOpenSortSheet: () -> Unit,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnglish = LocalIsEnglish.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = tr("Папки ($folderCount)", "Folders ($folderCount)"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(onClick = onOpenSortSheet)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("folders_sort_chip"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${sortOption.localizedShortLabel(isEnglish)} • ${sortOption.localizedActiveBadge(ascending, isEnglish)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onToggleDirection)
                        .testTag("folders_sort_direction_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (ascending) {
                            Icons.Default.ArrowUpward
                        } else {
                            Icons.Default.ArrowDownward
                        },
                        contentDescription = if (ascending) {
                            tr("По возрастанию: переключить на убывание", "Ascending: switch to descending")
                        } else {
                            tr("По убыванию: переключить на возрастание", "Descending: switch to ascending")
                        },
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderSortBottomSheet(
    currentOption: FolderSortOption,
    ascending: Boolean,
    onSelectOption: (FolderSortOption) -> Unit,
    onSetDirection: (Boolean) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = tr("Сортировка папок", "Sort Folders"),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Direction toggle pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SortDirectionPill(
                    selected = ascending,
                    label = tr(
                        "По возрастанию (${currentOption.localizedAscBadge(false)})",
                        "Ascending (${currentOption.localizedAscBadge(true)})"
                    ),
                    icon = Icons.Default.ArrowUpward,
                    onClick = { onSetDirection(true) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("folder_sort_dir_asc")
                )
                SortDirectionPill(
                    selected = !ascending,
                    label = tr(
                        "По убыванию (${currentOption.localizedDescBadge(false)})",
                        "Descending (${currentOption.localizedDescBadge(true)})"
                    ),
                    icon = Icons.Default.ArrowDownward,
                    onClick = { onSetDirection(false) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("folder_sort_dir_desc")
                )
            }

            FolderSortOption.entries.forEach { option ->
                val selected = currentOption == option
                val effectiveAsc = if (selected) ascending else option.defaultAscending
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
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
                        .clickable { onSelectOption(option) }
                        .testTag("folder_sort_option_${option.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = option.icon(),
                            contentDescription = null,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.localizedTitle(isEnglish),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = option.localizedActiveHint(effectiveAsc, isEnglish),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = if (ascending) {
                                    Icons.Default.ArrowUpward
                                } else {
                                    Icons.Default.ArrowDownward
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectOption(option) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortDirectionPill(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun FolderSortOption.icon(): ImageVector = when (this) {
    FolderSortOption.NAME -> Icons.Default.SortByAlpha
    FolderSortOption.LAST_PLAYED -> Icons.Default.History
    FolderSortOption.DATE_MODIFIED -> Icons.Default.Schedule
    FolderSortOption.EPISODE_COUNT -> Icons.AutoMirrored.Filled.FormatListBulleted
    FolderSortOption.TOTAL_SIZE -> Icons.Default.Storage
    FolderSortOption.UNWATCHED -> Icons.Default.PlaylistPlay
}

@Composable
internal fun EpisodeSortHeaderBar(
    episodeCount: Int,
    sortOption: EpisodeSortOption,
    ascending: Boolean,
    onOpenSortSheet: () -> Unit,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnglish = LocalIsEnglish.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = tr("Видео ($episodeCount)", "Videos ($episodeCount)"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(onClick = onOpenSortSheet)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("episodes_sort_chip"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${sortOption.localizedShortLabel(isEnglish)} • ${sortOption.localizedActiveBadge(ascending, isEnglish)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onToggleDirection)
                        .testTag("episodes_sort_direction_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (ascending) {
                            Icons.Default.ArrowUpward
                        } else {
                            Icons.Default.ArrowDownward
                        },
                        contentDescription = if (ascending) {
                            tr("По возрастанию: переключить на убывание", "Ascending: switch to descending")
                        } else {
                            tr("По убыванию: переключить на возрастание", "Descending: switch to ascending")
                        },
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EpisodeSortBottomSheet(
    currentOption: EpisodeSortOption,
    ascending: Boolean,
    onSelectOption: (EpisodeSortOption) -> Unit,
    onSetDirection: (Boolean) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = tr("Сортировка внутри папки", "Sort Inside Folder"),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SortDirectionPill(
                    selected = ascending,
                    label = tr(
                        "По возрастанию (${currentOption.localizedAscBadge(false)})",
                        "Ascending (${currentOption.localizedAscBadge(true)})"
                    ),
                    icon = Icons.Default.ArrowUpward,
                    onClick = { onSetDirection(true) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("episode_sort_dir_asc")
                )
                SortDirectionPill(
                    selected = !ascending,
                    label = tr(
                        "По убыванию (${currentOption.localizedDescBadge(false)})",
                        "Descending (${currentOption.localizedDescBadge(true)})"
                    ),
                    icon = Icons.Default.ArrowDownward,
                    onClick = { onSetDirection(false) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("episode_sort_dir_desc")
                )
            }

            EpisodeSortOption.entries.forEach { option ->
                val selected = currentOption == option
                val effectiveAsc = if (selected) ascending else option.defaultAscending
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
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
                        .clickable { onSelectOption(option) }
                        .testTag("episode_sort_option_${option.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = option.icon(),
                            contentDescription = null,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.localizedTitle(isEnglish),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = option.localizedActiveHint(effectiveAsc, isEnglish),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = if (ascending) {
                                    Icons.Default.ArrowUpward
                                } else {
                                    Icons.Default.ArrowDownward
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectOption(option) }
                        )
                    }
                }
            }
        }
    }
}

private fun EpisodeSortOption.icon(): ImageVector = when (this) {
    EpisodeSortOption.EPISODE_NUMBER -> Icons.AutoMirrored.Filled.FormatListBulleted
    EpisodeSortOption.DATE_MODIFIED -> Icons.Default.Schedule
    EpisodeSortOption.FILE_SIZE -> Icons.Default.Storage
    EpisodeSortOption.DURATION -> Icons.Default.Timer
    EpisodeSortOption.RESOLUTION -> Icons.Default.AspectRatio
    EpisodeSortOption.LAST_PLAYED -> Icons.Default.History
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun QuickThemeBottomSheet(
    currentTheme: AppThemeMode,
    currentControlsTheme: PlayerControlsTheme = PlayerControlsTheme.CLASSIC_OLED,
    currentLanguage: AppLanguage = AppLanguage.SYSTEM,
    onSelectTheme: (AppThemeMode) -> Unit,
    onSelectControlsTheme: (PlayerControlsTheme) -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit = {},
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = tr("Язык интерфейса", "Interface Language"),
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AppLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = currentLanguage == lang,
                        onClick = { onSelectLanguage(lang) },
                        label = { Text(lang.localizedTitle(isEnglish)) },
                        modifier = Modifier.testTag("quick_language_chip_${lang.id}")
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(tr("Темы оформления (OLED)", "App Themes (OLED)"), style = MaterialTheme.typography.titleLarge)
            }
            AppThemeMode.entries.forEach { mode ->
                val selected = currentTheme == mode
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
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
                        .clickable { onSelectTheme(mode) }
                        .testTag("quick_theme_option_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.localizedTitle(isEnglish), style = MaterialTheme.typography.titleSmall)
                            Text(
                                mode.localizedSubtitle(isEnglish),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(selected = selected, onClick = { onSelectTheme(mode) })
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(tr("Тема кнопок и полосы плеера", "Player Controls & Timeline Theme"), style = MaterialTheme.typography.titleMedium)
            }

            PlayerControlsThemeSelectorSection(
                selectedTheme = currentControlsTheme,
                onSelectTheme = onSelectControlsTheme,
                compactChipsOnly = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HiddenFoldersBottomSheet(
    hiddenFolders: List<HiddenFolderEntity>,
    onPickFolderToExclude: () -> Unit,
    onUnhide: (String) -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tr("Исключения (${hiddenFolders.size})", "Exclusions (${hiddenFolders.size})"),
                    style = MaterialTheme.typography.titleLarge
                )
                FilledTonalButton(
                    onClick = onPickFolderToExclude,
                    modifier = Modifier.testTag("sheet_pick_exclusion_folder_button")
                ) {
                    Text(tr("Добавить папку", "Add Folder"))
                }
            }
            Text(
                text = tr(
                    "Папки из списка исключений и все их подпапки скрываются из библиотеки при сканировании хранилища.",
                    "Excluded folders and all their subfolders are hidden from the library during storage scanning."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hiddenFolders.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(
                            "Список исключений пуст. Вы можете добавить любую папку через меню «⋮» на карточке папки или кнопкой выше.",
                            "The exclusion list is empty. You can exclude any folder via the '⋮' menu on a folder card or using the button above."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                hiddenFolders.forEachIndexed { idx, hidden ->
                    Card(
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = hidden.folderName,
                                    style = MaterialTheme.typography.titleMedium,
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
                            FilledTonalButton(
                                onClick = { onUnhide(hidden.folderId) },
                                modifier = Modifier.testTag("unhide_folder_button_$idx")
                            ) {
                                Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("Вернуть", "Restore"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OpenStreamUrlDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Открыть видео по ссылке", "Open Video Stream URL")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(tr("URL потока (.mp4, .mkv, .m3u8)", "Stream URL (.mp4, .mkv, .m3u8)")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_url_input")
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(tr("Название (необязательно)", "Title (optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(url, title) },
                enabled = url.isNotBlank(),
                modifier = Modifier.testTag("stream_url_confirm_button")
            ) {
                Text(tr("Воспроизвести", "Play"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Отмена", "Cancel"))
            }
        }
    )
}

@Composable
internal fun PermanentDeleteConfirmDialog(
    title: String,
    message: String,
    totalSizeBytes: Long = 0L,
    onConfirm: (dontAskAgain: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var dontAskAgain by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(30.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val sizeFormatted = formatFileSize(totalSizeBytes)
                if (totalSizeBytes > 0L && sizeFormatted.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = tr(
                                    "Будет освобождено на накопителе: $sizeFormatted",
                                    "Storage space to be freed: $sizeFormatted"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { dontAskAgain = !dontAskAgain }
                        .testTag("delete_dont_ask_again_row")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = dontAskAgain,
                            onCheckedChange = { dontAskAgain = it },
                            modifier = Modifier.testTag("delete_dont_ask_again_checkbox")
                        )
                        Text(
                            text = tr("Больше не показывать это уведомление", "Don't show this confirmation again"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(dontAskAgain) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_permanent_delete_button")
            ) {
                Text(tr("Удалить навсегда", "Delete Forever"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_permanent_delete_button")
            ) {
                Text(tr("Отмена", "Cancel"))
            }
        }
    )
}

@Composable
internal fun RenameMediaDialog(
    currentName: String,
    isFolder: Boolean,
    onConfirmRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = if (!isFolder && currentName.contains('.')) {
        currentName.substringAfterLast('.', "")
    } else {
        ""
    }
    val initialBaseName = if (ext.isNotBlank()) {
        currentName.substringBeforeLast('.')
    } else {
        currentName
    }
    var newName by rememberSaveable(currentName) { mutableStateOf(initialBaseName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("rename_media_dialog"),
        title = {
            Text(
                text = if (isFolder) {
                    tr("Переименовать папку", "Rename Folder")
                } else {
                    tr("Переименовать файл", "Rename File")
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = {
                        Text(
                            if (isFolder) tr("Новое имя папки", "New folder name")
                            else tr("Новое имя файла", "New file name")
                        )
                    },
                    suffix = if (ext.isNotBlank()) {
                        {
                            Text(
                                text = ".$ext",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_media_input")
                )
                Text(
                    text = if (isFolder) {
                        tr(
                            "Папка на накопителе и все привязанные пути в библиотеке будут обновлены.",
                            "The folder on storage and all linked library paths will be updated."
                        )
                    } else {
                        tr(
                            "Расширение файла (.$ext) сохранится автоматически, как и прогресс просмотра.",
                            "The file extension (.$ext) and playback progress will be preserved automatically."
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = newName.trim()
                    if (trimmed.isNotEmpty()) {
                        val finalName = if (ext.isNotBlank() && !trimmed.endsWith(".$ext", ignoreCase = true)) {
                            "$trimmed.$ext"
                        } else {
                            trimmed
                        }
                        onConfirmRename(finalName)
                    }
                },
                enabled = newName.trim().isNotEmpty(),
                modifier = Modifier.testTag("rename_media_confirm_button")
            ) {
                Text(tr("Сохранить", "Save"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("rename_media_cancel_button")
            ) {
                Text(tr("Отмена", "Cancel"))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun MediaFileInfoModalSheet(
    info: com.example.data.DetailedMediaFileInfo,
    onRenameRequest: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.testTag("media_file_info_sheet")
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (info.isFolder) {
                            tr("Сведения о папке", "Folder Information")
                        } else {
                            tr("Сведения о файле", "File Information")
                        },
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("media_file_info_title")
                    )
                    Text(
                        text = info.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onRenameRequest != null) {
                    FilledTonalButton(
                        onClick = onRenameRequest,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("media_file_info_rename_button")
                    ) {
                        Text(tr("Переименовать", "Rename"))
                    }
                }
            }

            // General / Container card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = tr("Контейнер и хранение", "Container & Storage"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TechnicalInfoRow(
                        label = tr("Формат", "Format"),
                        value = info.containerFormat
                    )
                    TechnicalInfoRow(
                        label = tr("Размер", "Size"),
                        value = if (info.sizeBytes > 0L) {
                            "${info.sizeFormatted} (${String.format(java.util.Locale.US, "%,d", info.sizeBytes)} B)"
                        } else {
                            info.sizeFormatted
                        }
                    )
                    if (info.durationMs > 0L) {
                        TechnicalInfoRow(
                            label = tr("Длительность", "Duration"),
                            value = info.durationFormatted
                        )
                    }
                    if (info.overallBitrateLabel != "—") {
                        TechnicalInfoRow(
                            label = tr("Общий поток", "Overall Bitrate"),
                            value = info.overallBitrateLabel
                        )
                    }
                    if (info.isFolder) {
                        TechnicalInfoRow(
                            label = tr("Видеофайлов", "Video Files"),
                            value = "${info.episodeCount} (${tr("просмотрено", "watched")}: ${info.watchedCount})"
                        )
                    } else if (info.chaptersCount > 0) {
                        TechnicalInfoRow(
                            label = tr("Встроенные главы", "Chapters"),
                            value = "${info.chaptersCount}"
                        )
                    }
                    if (info.lastModifiedFormatted != "—") {
                        TechnicalInfoRow(
                            label = tr("Изменён", "Modified"),
                            value = info.lastModifiedFormatted
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        text = tr("Полный путь", "Full Path"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = info.fullPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("media_file_info_full_path")
                    )
                }
            }

            // Video stream card
            info.videoStream?.let { v ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr("Видеопоток", "Video Stream"),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(7.dp)
                            ) {
                                Text(
                                    text = v.resolutionBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        TechnicalInfoRow(
                            label = tr("Кодек", "Codec"),
                            value = "${v.codecName} (${v.mimeType})"
                        )
                        TechnicalInfoRow(
                            label = tr("Профиль", "Profile"),
                            value = v.profileLevel
                        )
                        TechnicalInfoRow(
                            label = tr("Разрешение", "Resolution"),
                            value = if (v.width > 0 && v.height > 0) {
                                "${v.width} × ${v.height} (${v.aspectRatioLabel})"
                            } else {
                                "${v.resolutionBadge} (${v.aspectRatioLabel})"
                            }
                        )
                        TechnicalInfoRow(
                            label = tr("Частота кадров", "Frame Rate"),
                            value = v.frameRateLabel
                        )
                        TechnicalInfoRow(
                            label = tr("Цвет / Диапазон", "Color / Dynamic Range"),
                            value = v.colorAndHdrLabel
                        )
                        TechnicalInfoRow(
                            label = tr("Битрейт видео", "Video Bitrate"),
                            value = v.bitrateLabel
                        )
                    }
                }
            }

            // Audio streams card
            if (info.audioStreams.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = tr(
                                "Аудиодорожки (${info.audioStreams.size})",
                                "Audio Tracks (${info.audioStreams.size})"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        info.audioStreams.forEachIndexed { idx, a ->
                            if (idx > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "#${a.index} • ${a.title}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = if (a.isExternal) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (a.isExternal) {
                                                tr("Внешняя", "External")
                                            } else {
                                                tr("Встроенная", "Embedded")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (a.isExternal) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${a.codecName} • ${a.channelsLabel} • ${a.sampleRateLabel} • ${a.bitrateLabel} • ${a.language}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Subtitle streams card
            if (info.subtitleStreams.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = tr(
                                "Субтитры (${info.subtitleStreams.size})",
                                "Subtitles (${info.subtitleStreams.size})"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        info.subtitleStreams.forEachIndexed { idx, s ->
                            if (idx > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "#${s.index} • ${s.title}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${s.formatName} • ${s.language}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = if (s.isExternal) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (s.isExternal) {
                                            tr("Внешние", "External")
                                        } else {
                                            tr("Встроенные", "Embedded")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (s.isExternal) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

@Composable
private fun TechnicalInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f)
        )
    }
}


