package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.ui.util.VideoThumbnailCache
import kotlinx.coroutines.launch
import com.example.data.DetailedMediaFileInfo
import com.example.data.MediaFileInfoExtractor
import com.example.data.PlaybackResumeHelper
import com.example.model.EpisodeItem
import com.example.model.EpisodeSortOption
import com.example.model.VideoFolderItem
import com.example.ui.components.ResolutionBadgeChip
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatTimeMs
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folder: VideoFolderItem,
    showThumbnails: Boolean,
    compactMode: Boolean = true,
    resumePlaybackEnabled: Boolean,
    skipDeleteConfirmation: Boolean = false,
    sortOption: EpisodeSortOption = EpisodeSortOption.EPISODE_NUMBER,
    sortAscending: Boolean = true,
    onSelectSortOption: (EpisodeSortOption) -> Unit = {},
    onSetSortDirection: (Boolean) -> Unit = {},
    onToggleSortDirection: () -> Unit = {},
    onBack: () -> Unit,
    onPlayEpisode: (Int) -> Unit,
    onHideFolder: () -> Unit,
    onDeleteFolder: () -> Unit = {},
    onDeleteEpisodes: (List<EpisodeItem>) -> Unit = {},
    onRenameEpisode: (EpisodeItem, String) -> Unit = { _, _ -> },
    onRenameFolder: (String) -> Unit = {},
    onSetSkipDeleteConfirmation: (Boolean) -> Unit = {}
) {
    var showEpisodeSortSheet by rememberSaveable { mutableStateOf(false) }
    var selectedEpisodeUris by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var pendingDeleteEpisodes by remember { mutableStateOf<List<EpisodeItem>>(emptyList()) }
    var showDeleteEntireFolderConfirm by rememberSaveable { mutableStateOf(false) }
    var pendingRenameEpisode by remember { mutableStateOf<EpisodeItem?>(null) }
    var showRenameFolderDialog by rememberSaveable { mutableStateOf(false) }
    var activeFileInfo by remember { mutableStateOf<Pair<EpisodeItem?, DetailedMediaFileInfo>?>(null) }

    val isEnglish = LocalIsEnglish.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val isSelectionMode = selectedEpisodeUris.isNotEmpty()

    LaunchedEffect(folder.episodes) {
        if (selectedEpisodeUris.isNotEmpty()) {
            val validUris = folder.episodes.map { it.uri }.toSet()
            val filtered = selectedEpisodeUris.filter { it in validUris }
            if (filtered.size != selectedEpisodeUris.size) {
                selectedEpisodeUris = filtered
            }
        }
    }

    BackHandler {
        if (isSelectionMode) {
            selectedEpisodeUris = emptyList()
        } else {
            onBack()
        }
    }

    val toggleEpisodeSelection: (String) -> Unit = { uri ->
        selectedEpisodeUris = if (uri in selectedEpisodeUris) {
            selectedEpisodeUris - uri
        } else {
            selectedEpisodeUris + uri
        }
    }

    val executeEpisodesDelete: (List<EpisodeItem>) -> Unit = { episodesToDelete ->
        if (episodesToDelete.isNotEmpty()) {
            val count = episodesToDelete.size
            val singleTitle = episodesToDelete.firstOrNull()?.cleanTitle.orEmpty()
            onDeleteEpisodes(episodesToDelete)
            selectedEpisodeUris = emptyList()
            coroutineScope.launch {
                val msg = if (count == 1) {
                    if (isEnglish) "File \"$singleTitle\" deleted from storage"
                    else "Файл «$singleTitle» удалён с накопителя"
                } else {
                    if (isEnglish) "Deleted files from storage: $count"
                    else "Удалено файлов с накопителя: $count"
                }
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val requestDeleteEpisodes: (List<EpisodeItem>) -> Unit = { episodesToDelete ->
        if (episodesToDelete.isNotEmpty()) {
            if (skipDeleteConfirmation) {
                executeEpisodesDelete(episodesToDelete)
            } else {
                pendingDeleteEpisodes = episodesToDelete
            }
        }
    }

    val requestDeleteEntireFolder: () -> Unit = {
        if (skipDeleteConfirmation) {
            onDeleteFolder()
        } else {
            showDeleteEntireFolderConfirm = true
        }
    }

    val context = LocalContext.current
    val videoThumbnailLoader = remember(context) {
        VideoThumbnailCache.getImageLoader(context)
    }

    LaunchedEffect(folder.episodes, showThumbnails) {
        if (showThumbnails && folder.episodes.isNotEmpty()) {
            VideoThumbnailCache.prefetchFolderThumbnails(
                context = context,
                videoUris = folder.episodes.map { it.uri },
                targetWidthPx = 320,
                targetHeightPx = 180
            )
        }
    }

    val resumeCandidateIndex = remember(folder.episodes, resumePlaybackEnabled) {
        val activePlayed = folder.episodes.withIndex()
            .filter { (_, ep) ->
                PlaybackResumeHelper.hasActiveResumePoint(
                    positionMs = ep.resumePositionMs,
                    durationMs = ep.durationMs,
                    resumeEnabled = true
                )
            }
            .maxByOrNull { (_, ep) -> ep.lastPlayedAt }
            ?.index

        activePlayed ?: folder.episodes.indexOfFirst { ep ->
            ep.durationMs <= 0L || ep.progressFraction < 0.85f
        }.takeIf { it >= 0 } ?: 0
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                val selectedItems = folder.episodes.filter { it.uri in selectedEpisodeUris }
                val totalSelectedBytes = selectedItems.sumOf { it.sizeBytes }
                val allSelected = folder.episodes.isNotEmpty() && selectedEpisodeUris.size == folder.episodes.size

                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { selectedEpisodeUris = emptyList() },
                            modifier = Modifier.testTag("close_episode_selection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("Отменить выбор", "Clear selection")
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = tr(
                                    "Выбрано файлов: ${selectedEpisodeUris.size}",
                                    "Selected files: ${selectedEpisodeUris.size}"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("episode_selection_count_text")
                            )
                            val sizeStr = formatFileSize(totalSelectedBytes)
                            if (totalSelectedBytes > 0L && sizeStr.isNotEmpty()) {
                                Text(
                                    text = sizeStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                selectedEpisodeUris = if (allSelected) {
                                    emptyList()
                                } else {
                                    folder.episodes.map { it.uri }
                                }
                            },
                            modifier = Modifier.testTag("select_all_episodes_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = tr("Выбрать все файлы", "Select all files"),
                                tint = if (allSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        IconButton(
                            onClick = { requestDeleteEpisodes(selectedItems) },
                            modifier = Modifier.testTag("delete_selected_episodes_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = tr("Удалить выбранные файлы", "Delete selected files"),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(
                                    "${folder.episodes.size} эп. • ${folder.displayPath}",
                                    "${folder.episodes.size} ep. • ${folder.displayPath}"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("folder_detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = tr("Назад к списку папок", "Back to folders")
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                activeFileInfo = null to MediaFileInfoExtractor.extractFolderInfo(context, folder)
                            },
                            modifier = Modifier.testTag("folder_detail_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = tr("Сведения о папке", "Folder Info")
                            )
                        }
                        IconButton(
                            onClick = onHideFolder,
                            modifier = Modifier.testTag("folder_detail_hide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = tr("Добавить папку в исключения", "Exclude folder")
                            )
                        }
                        IconButton(
                            onClick = requestDeleteEntireFolder,
                            modifier = Modifier.testTag("folder_detail_delete_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = tr("Удалить папку с накопителя", "Delete folder from storage"),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        floatingActionButton = {
            if (folder.episodes.isNotEmpty() && !isSelectionMode) {
                val targetEp = folder.episodes[resumeCandidateIndex]
                val resumePos = PlaybackResumeHelper.computeResumePositionMs(
                    positionMs = targetEp.resumePositionMs,
                    durationMs = targetEp.durationMs,
                    resumeEnabled = resumePlaybackEnabled
                )
                val label = if (resumePos > 0L) {
                    tr(
                        "Продолжить ${targetEp.episodeBadge ?: ""} (${formatTimeMs(resumePos)})".replace("  ", " "),
                        "Resume ${targetEp.episodeBadge ?: ""} (${formatTimeMs(resumePos)})".replace("  ", " ")
                    )
                } else if (resumeCandidateIndex > 0) {
                    tr(
                        "Смотреть ${targetEp.episodeBadge ?: "с ${resumeCandidateIndex + 1} серии"}",
                        "Watch ${targetEp.episodeBadge ?: "from ep. ${resumeCandidateIndex + 1}"}"
                    )
                } else {
                    tr("Смотреть с 1 серии", "Watch from episode 1")
                }
                ExtendedFloatingActionButton(
                    onClick = { onPlayEpisode(resumeCandidateIndex) },
                    icon = {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    },
                    text = { Text(label) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("folder_play_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val selectedSet = remember(selectedEpisodeUris) { selectedEpisodeUris.toSet() }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .testTag("episode_list"),
                contentPadding = PaddingValues(
                    start = if (compactMode) 12.dp else 16.dp,
                    end = if (compactMode) 12.dp else 16.dp,
                    top = if (compactMode) 4.dp else 8.dp,
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(if (compactMode) 8.dp else 12.dp)
            ) {
                if (folder.totalAutoAudioCount > 0 || folder.totalAutoSubCount > 0) {
                    item(key = "auto_bind_banner") {
                        AutoBindSummaryBanner(
                            audioCount = folder.totalAutoAudioCount,
                            subCount = folder.totalAutoSubCount
                        )
                    }
                }

                if (folder.episodes.isNotEmpty()) {
                    item(key = "episode_sort_bar") {
                        EpisodeSortHeaderBar(
                            episodeCount = folder.episodes.size,
                            sortOption = sortOption,
                            ascending = sortAscending,
                            onOpenSortSheet = { showEpisodeSortSheet = true },
                            onToggleDirection = onToggleSortDirection
                        )
                    }
                }

                itemsIndexed(
                    items = folder.episodes,
                    key = { _, ep -> ep.uri },
                    contentType = { _, _ -> "episode_card" }
                ) { index, episode ->
                    val isSelected = episode.uri in selectedSet
                    EpisodeRowCard(
                        episode = episode,
                        index = index,
                        showThumbnail = showThumbnails,
                        compactMode = compactMode,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        imageLoader = videoThumbnailLoader,
                        onClick = {
                            if (isSelectionMode) {
                                toggleEpisodeSelection(episode.uri)
                            } else {
                                onPlayEpisode(index)
                            }
                        },
                        onLongClick = {
                            toggleEpisodeSelection(episode.uri)
                        },
                        onToggleSelect = {
                            toggleEpisodeSelection(episode.uri)
                        },
                        onRename = {
                            pendingRenameEpisode = episode
                        },
                        onShowInfo = {
                            activeFileInfo = episode to MediaFileInfoExtractor.extractEpisodeInfo(context, episode)
                        },
                        onDeleteSingle = {
                            requestDeleteEpisodes(listOf(episode))
                        }
                    )
                }
            }
        }
    }

    if (showEpisodeSortSheet) {
        EpisodeSortBottomSheet(
            currentOption = sortOption,
            ascending = sortAscending,
            onSelectOption = { option ->
                onSelectSortOption(option)
                showEpisodeSortSheet = false
            },
            onSetDirection = { asc ->
                onSetSortDirection(asc)
            },
            onDismiss = { showEpisodeSortSheet = false }
        )
    }

    if (showDeleteEntireFolderConfirm) {
        PermanentDeleteConfirmDialog(
            title = tr("Удалить папку «${folder.name}»?", "Delete folder \"${folder.name}\"?"),
            message = tr(
                "Папка «${folder.name}» и все файлы внутри неё (${folder.episodes.size} видео) будут безвозвратно удалены с накопителя устройства.",
                "Folder \"${folder.name}\" and all files inside it (${folder.episodes.size} videos) will be permanently deleted from device storage."
            ),
            totalSizeBytes = folder.totalSizeBytes,
            onConfirm = { dontAskAgain ->
                if (dontAskAgain) {
                    onSetSkipDeleteConfirmation(true)
                }
                showDeleteEntireFolderConfirm = false
                onDeleteFolder()
            },
            onDismiss = {
                showDeleteEntireFolderConfirm = false
            }
        )
    }

    if (pendingDeleteEpisodes.isNotEmpty()) {
        val count = pendingDeleteEpisodes.size
        val totalSize = pendingDeleteEpisodes.sumOf { it.sizeBytes }
        val singleEp = pendingDeleteEpisodes.firstOrNull()
        val dialogTitle = if (count == 1 && singleEp != null) {
            tr("Удалить файл «${singleEp.cleanTitle}»?", "Delete file \"${singleEp.cleanTitle}\"?")
        } else {
            tr("Удалить выбранные файлы ($count)?", "Delete selected files ($count)?")
        }
        val dialogMessage = if (count == 1 && singleEp != null) {
            tr(
                "Видеофайл «${singleEp.fileName}» будет безвозвратно удалён с накопителя устройства.",
                "Video file \"${singleEp.fileName}\" will be permanently deleted from device storage."
            )
        } else {
            tr(
                "Выбранные видеофайлы ($count шт.) будут безвозвратно удалены с накопителя устройства.",
                "The selected video files ($count items) will be permanently deleted from device storage."
            )
        }

        PermanentDeleteConfirmDialog(
            title = dialogTitle,
            message = dialogMessage,
            totalSizeBytes = totalSize,
            onConfirm = { dontAskAgain ->
                if (dontAskAgain) {
                    onSetSkipDeleteConfirmation(true)
                }
                val targets = pendingDeleteEpisodes
                pendingDeleteEpisodes = emptyList()
                executeEpisodesDelete(targets)
            },
            onDismiss = {
                pendingDeleteEpisodes = emptyList()
            }
        )
    }

    pendingRenameEpisode?.let { episodeToRename ->
        RenameMediaDialog(
            currentName = episodeToRename.fileName,
            isFolder = false,
            onConfirmRename = { newName ->
                pendingRenameEpisode = null
                onRenameEpisode(episodeToRename, newName)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = if (isEnglish) "File renamed to \"$newName\"" else "Файл переименован в «$newName»",
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onDismiss = { pendingRenameEpisode = null }
        )
    }

    if (showRenameFolderDialog) {
        RenameMediaDialog(
            currentName = folder.name,
            isFolder = true,
            onConfirmRename = { newName ->
                showRenameFolderDialog = false
                onRenameFolder(newName)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = if (isEnglish) "Folder renamed to \"$newName\"" else "Папка переименована в «$newName»",
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onDismiss = { showRenameFolderDialog = false }
        )
    }

    activeFileInfo?.let { (epTarget, info) ->
        MediaFileInfoModalSheet(
            info = info,
            onRenameRequest = {
                activeFileInfo = null
                if (epTarget != null) {
                    pendingRenameEpisode = epTarget
                } else {
                    showRenameFolderDialog = true
                }
            },
            onDismiss = { activeFileInfo = null }
        )
    }
}

@Composable
private fun AutoBindSummaryBanner(
    audioCount: Int,
    subCount: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Автопривязка дорожек активна", "Auto Track Binding Active"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val audioLabel = tr("Найдено внешних аудио: $audioCount", "External audio found: $audioCount")
                val subLabel = tr("Внешних SRT/ASS: $subCount", "External SRT/ASS: $subCount")
                Text(
                    text = buildString {
                        if (audioCount > 0) append(audioLabel)
                        if (audioCount > 0 && subCount > 0) append(" • ")
                        if (subCount > 0) append(subLabel)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun EpisodeRowCard(
    episode: EpisodeItem,
    index: Int,
    showThumbnail: Boolean,
    compactMode: Boolean = true,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onRename: () -> Unit = {},
    onShowInfo: () -> Unit = {},
    onDeleteSingle: () -> Unit = {}
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val corner = if (compactMode) 15.dp else 18.dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (isSelectionMode) {
                        onLongClick()
                    } else {
                        menuExpanded = true
                    }
                }
            )
            .testTag("episode_card_$index"),
        shape = RoundedCornerShape(corner),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (compactMode) 10.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compactMode) 10.dp else 14.dp)
            ) {
                if (showThumbnail) {
                    val thumbRequest = remember(episode.uri) {
                        VideoThumbnailCache.buildEpisodeThumbnailRequest(
                            context = context,
                            videoUri = episode.uri,
                            targetWidthPx = 320,
                            targetHeightPx = 180
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(if (compactMode) 112.dp else 124.dp)
                            .height(if (compactMode) 63.dp else 70.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF192236),
                                        Color(0xFF0E1320)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = thumbRequest,
                            imageLoader = imageLoader,
                            contentDescription = episode.cleanTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Black.copy(alpha = 0.62f)
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        episode.episodeBadge?.let { badge ->
                            Surface(
                                color = Color.Black.copy(alpha = 0.76f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(5.dp)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!showThumbnail && episode.episodeBadge != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = episode.episodeBadge!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = episode.cleanTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        episode.resolutionBadge?.let { resBadge ->
                            ResolutionBadgeChip(
                                badge = resBadge,
                                compact = compactMode,
                                modifier = Modifier.testTag("episode_resolution_badge_$index")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    val metaLine = remember(episode.dimensionsLabel, episode.sizeBytes, episode.resumePositionMs, episode.durationMs) {
                        buildString {
                            episode.dimensionsLabel?.let { dims ->
                                append(dims)
                            }
                            val sizeStr = formatFileSize(episode.sizeBytes)
                            if (sizeStr.isNotEmpty()) {
                                if (isNotEmpty()) append(" • ")
                                append(sizeStr)
                            }
                            if (episode.resumePositionMs > 0L && episode.durationMs > 0L) {
                                if (isNotEmpty()) append(" • ")
                                append("${formatTimeMs(episode.resumePositionMs)} / ${formatTimeMs(episode.durationMs)}")
                            } else if (episode.durationMs > 0L) {
                                if (isNotEmpty()) append(" • ")
                                append(formatTimeMs(episode.durationMs))
                            }
                        }
                    }
                    if (metaLine.isNotEmpty()) {
                        Text(
                            text = metaLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (episode.autoAudioTracks.isNotEmpty() || episode.autoSubtitleTracks.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (episode.autoAudioTracks.isNotEmpty()) {
                                val firstAudio = episode.autoAudioTracks.first()
                                TrackPillBadge(
                                    icon = Icons.Default.GraphicEq,
                                    text = if (episode.autoAudioTracks.size == 1) {
                                        firstAudio.sourceFolderName.ifBlank { firstAudio.extension.uppercase() }
                                    } else {
                                        tr(
                                            "${episode.autoAudioTracks.size} внеш. аудио",
                                            "${episode.autoAudioTracks.size} ext. audio"
                                        )
                                    },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            if (episode.autoSubtitleTracks.isNotEmpty()) {
                                val formats = episode.autoSubtitleTracks
                                    .map { it.format.label }
                                    .distinct()
                                    .joinToString("/")
                                TrackPillBadge(
                                    icon = Icons.Default.Subtitles,
                                    text = "$formats (${episode.autoSubtitleTracks.size})",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.testTag("episode_select_checkbox_$index")
                    )
                } else {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("episode_menu_button_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = tr("Меню файла ${episode.cleanTitle}", "File menu ${episode.cleanTitle}")
                            )
                        }
                        if (menuExpanded) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(tr("Переименовать", "Rename")) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onRename()
                                    },
                                    modifier = Modifier.testTag("rename_episode_menu_item_$index")
                                )
                                DropdownMenuItem(
                                    text = { Text(tr("Сведения о файле", "File Info")) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Info, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onShowInfo()
                                    },
                                    modifier = Modifier.testTag("info_episode_menu_item_$index")
                                )
                                DropdownMenuItem(
                                    text = { Text(tr("Выбрать", "Select")) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleSelect()
                                    },
                                    modifier = Modifier.testTag("select_episode_menu_item_$index")
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = tr("Удалить с накопителя", "Delete from storage"),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteSingle()
                                    },
                                    modifier = Modifier.testTag("delete_episode_menu_item_$index")
                                )
                            }
                        }
                    }
                }
            }

            if (episode.progressFraction > 0.01f) {
                LinearProgressIndicator(
                    progress = { episode.progressFraction },
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

@Composable
private fun TrackPillBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
