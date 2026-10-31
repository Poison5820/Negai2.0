package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.HiddenFolderEntity
import com.example.data.LibraryRootEntity
import com.example.data.PlaybackProgressEntity
import com.example.model.Anime4KPreset
import com.example.model.AppVersionInfo
import com.example.model.FolderSortOption
import com.example.model.FrameInterpolationMode
import com.example.model.PlayerSettings
import com.example.model.VideoFolderItem
import com.example.torrent.ExternalOpenOutcome
import com.example.torrent.TorrentIntentHelper
import com.example.ui.HomeTab
import com.example.ui.TorrentsUiState
import com.example.ui.components.LastPlayedFileButton
import com.example.ui.components.formatFileSize
import com.example.ui.i18n.tr
import com.example.ui.theme.NegaiBrandFontFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    currentTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    visibleFolders: List<VideoFolderItem>,
    hiddenFolders: List<HiddenFolderEntity>,
    recentItems: List<PlaybackProgressEntity>,
    lastPlayedFile: PlaybackProgressEntity?,
    libraryRoots: List<LibraryRootEntity>,
    settings: PlayerSettings,
    torrentsState: TorrentsUiState,
    torrentsActions: TorrentsActions,
    hasStorageAccess: Boolean,
    hasAllFilesAccess: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isScanning: Boolean,
    onRequestStorageAccess: () -> Unit,
    onAddSafFolder: () -> Unit,
    onPickFolderToExclude: () -> Unit,
    onOpenSingleFile: () -> Unit,
    onPlayStreamUrl: (String, String) -> Unit,
    onRescan: () -> Unit,
    onOpenFolder: (VideoFolderItem) -> Unit,
    onHideFolder: (VideoFolderItem) -> Unit,
    onUnhideFolder: (String) -> Unit,
    onResumeRecent: (PlaybackProgressEntity) -> Unit,
    onOpenLastPlayed: ((() -> Unit)?) -> Unit,
    onSelectFolderSortOption: (FolderSortOption) -> Unit,
    onSetFolderSortDirection: (Boolean) -> Unit,
    onToggleFolderSortDirection: () -> Unit,
    settingsActions: SettingsActions
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val missingLastPlayedMessage = stringResource(R.string.last_played_missing_snackbar)

    var showExclusionsSheet by rememberSaveable { mutableStateOf(false) }
    var showStreamDialog by rememberSaveable { mutableStateOf(false) }
    var showQuickThemeSheet by rememberSaveable { mutableStateOf(false) }
    var showVersionPlanetarySheet by rememberSaveable { mutableStateOf(false) }
    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var showManualTorrentDialog by rememberSaveable { mutableStateOf(false) }
    var showTorrServerConfigDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = currentTab != HomeTab.FOLDERS) {
        onSelectTab(HomeTab.FOLDERS)
    }

    val openExternalTorrentWithFeedback: (String?, String?, String) -> Unit = { magnetUri, torrentUrl, title ->
        val outcome = TorrentIntentHelper.openTorrentInExternalApp(
            context = context,
            magnetUri = magnetUri,
            torrentFileUrl = torrentUrl,
            title = title
        )
        val snackMessage = when (outcome) {
            ExternalOpenOutcome.OPENED_CHOOSER -> null
            ExternalOpenOutcome.OPENED_BROWSER_DOWNLOAD -> "Открыта загрузка .torrent в браузере"
            ExternalOpenOutcome.COPIED_MAGNET_NO_EXTERNAL_APP ->
                "Внешний торрент-клиент не найден — Magnet-ссылка скопирована в буфер обмена"
            ExternalOpenOutcome.ERROR -> "Не удалось открыть ссылку во внешнем приложении"
        }
        if (snackMessage != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = snackMessage,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val copyMagnetWithFeedback: (String, String) -> Unit = { magnet, title ->
        if (TorrentIntentHelper.copyToClipboard(context, magnet, title)) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Magnet-ссылка скопирована в буфер обмена",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val handleExcludeFolderWithUndo: (VideoFolderItem) -> Unit = { folder ->
        onHideFolder(folder)
        coroutineScope.launch {
            val res = snackbarHostState.showSnackbar(
                message = "Папка «${folder.name}» добавлена в исключения",
                actionLabel = "Отменить",
                duration = SnackbarDuration.Short
            )
            if (res == SnackbarResult.ActionPerformed) {
                onUnhideFolder(folder.id)
            }
        }
    }

    val handleOpenLastPlayed = {
        onOpenLastPlayed {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = missingLastPlayedMessage,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val effectiveSettingsActions = remember(settingsActions) {
        settingsActions.copy(
            onOpenExclusionsSheet = { showExclusionsSheet = true }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "negai",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = NegaiBrandFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showVersionPlanetarySheet = true }
                                    .testTag("app_version_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Text(
                                        text = AppVersionInfo.BADGE_TEXT,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (settings.effectiveFrameInterpolationMode != FrameInterpolationMode.OFF) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(7.dp)
                                ) {
                                    Text(
                                        text = settings.effectiveFrameInterpolationMode.badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (settings.effectiveAnime4KPreset != Anime4KPreset.OFF) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(7.dp)
                                ) {
                                    Text(
                                        text = settings.effectiveAnime4KPreset.badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showQuickThemeSheet = true },
                            modifier = Modifier.testTag("top_bar_theme_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Темы оформления и кнопок плеера"
                            )
                        }
                        IconButton(
                            onClick = onRescan,
                            modifier = Modifier.testTag("rescan_library_button")
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Обновить библиотеку"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        NavigationBarItem(
                            selected = currentTab == HomeTab.FOLDERS,
                            onClick = { onSelectTab(HomeTab.FOLDERS) },
                            icon = {
                                Icon(
                                    if (currentTab == HomeTab.FOLDERS) Icons.Filled.Folder else Icons.Outlined.Folder,
                                    contentDescription = stringResource(R.string.nav_folders)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_folders)) },
                            modifier = Modifier.testTag("nav_tab_folders")
                        )
                        NavigationBarItem(
                            selected = currentTab == HomeTab.RECENT,
                            onClick = { onSelectTab(HomeTab.RECENT) },
                            icon = {
                                Icon(
                                    if (currentTab == HomeTab.RECENT) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = stringResource(R.string.nav_recent)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_recent)) },
                            modifier = Modifier.testTag("nav_tab_recent")
                        )
                        NavigationBarItem(
                            selected = currentTab == HomeTab.TORRENTS,
                            onClick = { onSelectTab(HomeTab.TORRENTS) },
                            icon = {
                                Icon(
                                    if (currentTab == HomeTab.TORRENTS) Icons.Filled.CloudDownload else Icons.Outlined.CloudDownload,
                                    contentDescription = stringResource(R.string.nav_torrents)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_torrents)) },
                            modifier = Modifier.testTag("nav_tab_torrents")
                        )
                        NavigationBarItem(
                            selected = currentTab == HomeTab.SETTINGS,
                            onClick = { onSelectTab(HomeTab.SETTINGS) },
                            icon = {
                                Icon(
                                    if (currentTab == HomeTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.nav_settings)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_settings)) },
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NavigationRailItem(
                            selected = currentTab == HomeTab.FOLDERS,
                            onClick = { onSelectTab(HomeTab.FOLDERS) },
                            icon = {
                                Icon(
                                    if (currentTab == HomeTab.FOLDERS) Icons.Filled.Folder else Icons.Outlined.Folder,
                                    contentDescription = stringResource(R.string.nav_folders)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_folders)) },
                            modifier = Modifier.testTag("rail_tab_folders")
                        )
                        NavigationRailItem(
                            selected = currentTab == HomeTab.RECENT,
                            onClick = { onSelectTab(HomeTab.RECENT) },
                            icon = {
                                Icon(
                                    if (currentTab == HomeTab.RECENT) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = stringResource(R.string.nav_recent)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_recent)) },
                            modifier = Modifier.testTag("rail_tab_recent")
                        )
                        NavigationRailItem(
                            selected = currentTab == HomeTab.TORRENTS,
                            onClick = { onSelectTab(HomeTab.TORRENTS) },
                            icon = {
                                Icon(
                                    if (currentTab == HomeTab.TORRENTS) Icons.Filled.CloudDownload else Icons.Outlined.CloudDownload,
                                    contentDescription = stringResource(R.string.nav_torrents)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_torrents)) },
                            modifier = Modifier.testTag("rail_tab_torrents")
                        )
                        NavigationRailItem(
                            selected = currentTab == HomeTab.SETTINGS,
                            onClick = { onSelectTab(HomeTab.SETTINGS) },
                            icon = {
                                Icon(
                                    if (currentTab == HomeTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.nav_settings)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_settings)) },
                            modifier = Modifier.testTag("rail_tab_settings")
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (currentTab) {
                        HomeTab.FOLDERS -> FoldersTabContent(
                            visibleFolders = visibleFolders,
                            hiddenCount = hiddenFolders.size,
                            lastPlayedFile = lastPlayedFile,
                            resumePlaybackEnabled = settings.resumePlayback,
                            compactMode = settings.compactUiMode,
                            hasStorageAccess = hasStorageAccess,
                            hasAllFilesAccess = hasAllFilesAccess,
                            sortOption = settings.folderSortOption,
                            sortAscending = settings.folderSortAscending,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            isScanning = isScanning,
                            onRequestStorageAccess = onRequestStorageAccess,
                            onAddSafFolder = onAddSafFolder,
                            onOpenSingleFile = onOpenSingleFile,
                            onOpenStreamDialog = { showStreamDialog = true },
                            onOpenExclusionsSheet = { showExclusionsSheet = true },
                            onOpenSortSheet = { showSortSheet = true },
                            onToggleSortDirection = onToggleFolderSortDirection,
                            onOpenFolder = onOpenFolder,
                            onHideFolder = handleExcludeFolderWithUndo,
                            onOpenLastPlayed = handleOpenLastPlayed
                        )

                        HomeTab.RECENT -> RecentTabContent(
                            recentItems = recentItems,
                            compactMode = settings.compactUiMode,
                            onResumeRecent = onResumeRecent,
                            onOpenSingleFile = onOpenSingleFile,
                            onClearHistory = effectiveSettingsActions.onClearPlaybackHistory
                        )

                        HomeTab.TORRENTS -> TorrentsTabContent(
                            state = torrentsState,
                            settings = settings,
                            onQueryChange = torrentsActions.onQueryChange,
                            onSubmitSearch = torrentsActions.onSubmitSearch,
                            onSelectTrackerFilter = torrentsActions.onSelectTrackerFilter,
                            onSelectQualityFilter = torrentsActions.onSelectQualityFilter,
                            onSelectSortMode = torrentsActions.onSelectSortMode,
                            onCheckConnection = torrentsActions.onCheckConnection,
                            onWatchResult = torrentsActions.onWatchResult,
                            onOpenExternalResult = { item ->
                                openExternalTorrentWithFeedback(
                                    item.magnetUri,
                                    item.torrentFileUrl,
                                    item.title
                                )
                            },
                            onCopyMagnet = copyMagnetWithFeedback,
                            onOpenManualDialog = { showManualTorrentDialog = true },
                            onOpenServerConfigDialog = { showTorrServerConfigDialog = true },
                            onLoadSavedTorrents = torrentsActions.onLoadSavedTorrents,
                            onOpenSavedTorrent = torrentsActions.onOpenSavedTorrent,
                            onOpenExternalLink = { magnet, title ->
                                openExternalTorrentWithFeedback(magnet, null, title)
                            },
                            onDropSavedCache = torrentsActions.onDropSavedCache,
                            onRemoveSavedTorrent = torrentsActions.onRemoveSavedTorrent
                        )

                        HomeTab.SETTINGS -> SettingsTabContent(
                            settings = settings,
                            hasStorageAccess = hasStorageAccess,
                            hasAllFilesAccess = hasAllFilesAccess,
                            libraryRoots = libraryRoots,
                            hiddenFolders = hiddenFolders,
                            recentCount = recentItems.size,
                            actions = effectiveSettingsActions
                        )
                    }
                }
            }
        }
    }

    if (showExclusionsSheet) {
        HiddenFoldersBottomSheet(
            hiddenFolders = hiddenFolders,
            onPickFolderToExclude = onPickFolderToExclude,
            onUnhide = onUnhideFolder,
            onDismiss = { showExclusionsSheet = false }
        )
    }

    if (showVersionPlanetarySheet) {
        VersionPlanetaryModalSheet(
            onDismiss = { showVersionPlanetarySheet = false }
        )
    }

    if (showQuickThemeSheet) {
        QuickThemeBottomSheet(
            currentTheme = settings.themeMode,
            currentControlsTheme = settings.playerControlsTheme,
            currentLanguage = settings.appLanguage,
            onSelectTheme = { mode ->
                effectiveSettingsActions.onUpdateThemeMode(mode)
            },
            onSelectControlsTheme = { controlsTheme ->
                effectiveSettingsActions.onUpdatePlayerControlsTheme(controlsTheme)
            },
            onSelectLanguage = { lang ->
                effectiveSettingsActions.onUpdateAppLanguage(lang)
            },
            onDismiss = { showQuickThemeSheet = false }
        )
    }

    if (showSortSheet) {
        FolderSortBottomSheet(
            currentOption = settings.folderSortOption,
            ascending = settings.folderSortAscending,
            onSelectOption = { option ->
                onSelectFolderSortOption(option)
                showSortSheet = false
            },
            onSetDirection = { asc ->
                onSetFolderSortDirection(asc)
            },
            onDismiss = { showSortSheet = false }
        )
    }

    if (showStreamDialog) {
        OpenStreamUrlDialog(
            onConfirm = { url, title ->
                showStreamDialog = false
                onPlayStreamUrl(url, title)
            },
            onDismiss = { showStreamDialog = false }
        )
    }

    if (showManualTorrentDialog) {
        ManualTorrentInputDialog(
            onWatchLink = { rawLink, title ->
                torrentsActions.onWatchRawLink(rawLink, title)
            },
            onOpenExternalLink = { rawLink, title ->
                val isMagnetOrHash = rawLink.startsWith("magnet:", ignoreCase = true) ||
                    !rawLink.startsWith("http", ignoreCase = true)
                openExternalTorrentWithFeedback(
                    if (isMagnetOrHash) rawLink else null,
                    if (!isMagnetOrHash) rawLink else null,
                    title.ifBlank { "Торрент" }
                )
            },
            onPickTorrentFileForWatch = torrentsActions.onPickTorrentFileForWatch,
            onPickTorrentFileForExternal = torrentsActions.onPickTorrentFileForExternal,
            onDismiss = { showManualTorrentDialog = false }
        )
    }

    if (showTorrServerConfigDialog) {
        TorrServerQuickConfigDialog(
            settings = settings,
            onSave = torrentsActions.onSaveServerConfig,
            onDismiss = { showTorrServerConfigDialog = false }
        )
    }

    TorrentInspectorBottomSheet(
        inspectorState = torrentsState.inspectorState,
        onDismiss = torrentsActions.onDismissInspector,
        onSelectVoiceover = torrentsActions.onSelectInspectorVoiceover,
        onPlayEpisode = torrentsActions.onPlayInspectorEpisode,
        onForceDirectStream = torrentsActions.onForceDirectStream,
        onRetryLink = { link, title -> torrentsActions.onWatchRawLink(link, title) },
        onOpenExternal = { magnetUri, torrentUrl, title ->
            openExternalTorrentWithFeedback(magnetUri, torrentUrl, title)
        },
        onOpenServerConfig = { showTorrServerConfigDialog = true }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoldersTabContent(
    visibleFolders: List<VideoFolderItem>,
    hiddenCount: Int,
    lastPlayedFile: PlaybackProgressEntity?,
    resumePlaybackEnabled: Boolean,
    compactMode: Boolean = true,
    hasStorageAccess: Boolean,
    hasAllFilesAccess: Boolean,
    sortOption: FolderSortOption,
    sortAscending: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isScanning: Boolean,
    onRequestStorageAccess: () -> Unit,
    onAddSafFolder: () -> Unit,
    onOpenSingleFile: () -> Unit,
    onOpenStreamDialog: () -> Unit,
    onOpenExclusionsSheet: () -> Unit,
    onOpenSortSheet: () -> Unit,
    onToggleSortDirection: () -> Unit,
    onOpenFolder: (VideoFolderItem) -> Unit,
    onHideFolder: (VideoFolderItem) -> Unit,
    onOpenLastPlayed: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .testTag("folders_lazy_list"),
        contentPadding = PaddingValues(
            horizontal = if (compactMode) 12.dp else 16.dp,
            vertical = if (compactMode) 4.dp else 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (compactMode) 8.dp else 12.dp)
    ) {
        if (lastPlayedFile != null && searchQuery.isBlank()) {
            item(key = "last_played_file_card") {
                LastPlayedFileButton(
                    item = lastPlayedFile,
                    resumeEnabled = resumePlaybackEnabled,
                    compactMode = compactMode,
                    onClick = onOpenLastPlayed
                )
            }
        }

        item(key = "search_bar") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Поиск папок и серий…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Очистить поиск")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_folders_input")
            )
        }

        item(key = "quick_actions") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!hasStorageAccess) {
                            CompactHomeActionPill(
                                label = tr("Доступ", "Access"),
                                icon = Icons.Default.FolderOpen,
                                emphasized = true,
                                onClick = onRequestStorageAccess,
                                testTag = "grant_storage_access_button"
                            )
                        } else if (!hasAllFilesAccess) {
                            CompactHomeActionPill(
                                label = tr("Доступ", "Access"),
                                icon = Icons.Default.FolderOpen,
                                tonal = true,
                                onClick = onRequestStorageAccess,
                                testTag = "grant_all_files_access_button"
                            )
                        }

                        CompactHomeActionPill(
                            label = tr("Файл", "File"),
                            icon = Icons.Default.VideoFile,
                            tonal = true,
                            onClick = onOpenSingleFile,
                            testTag = "open_file_button"
                        )

                        CompactHomeActionPill(
                            label = "URL",
                            icon = Icons.Default.Link,
                            onClick = onOpenStreamDialog,
                            testTag = "open_url_button"
                        )

                        if (hasStorageAccess) {
                            CompactHomeActionPill(
                                label = tr("Папка", "Folder"),
                                icon = Icons.Default.CreateNewFolder,
                                onClick = onAddSafFolder,
                                testTag = "add_folder_button"
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenExclusionsSheet)
                            .testTag("open_exclusions_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (hiddenCount > 0) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = if (hiddenCount > 0) {
                                    "Исключения ($hiddenCount)"
                                } else {
                                    "Исключения"
                                },
                                tint = if (hiddenCount > 0) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            if (hiddenCount > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(1.dp)
                                ) {
                                    Text(
                                        text = hiddenCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 0.5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (visibleFolders.isEmpty() && searchQuery.isBlank()) {
            item(key = "empty_hero") {
                StorageStatusEmptyCard(
                    hasStorageAccess = hasStorageAccess,
                    isScanning = isScanning,
                    hasHiddenFolders = hiddenCount > 0,
                    hiddenCount = hiddenCount,
                    onRequestStorageAccess = onRequestStorageAccess,
                    onAddExternalFolder = onAddSafFolder,
                    onOpenExclusions = onOpenExclusionsSheet
                )
            }
        } else if (visibleFolders.isEmpty()) {
            item(key = "no_search_results") {
                Card(
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "По запросу «$searchQuery» ничего не найдено",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (hiddenCount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = onOpenExclusionsSheet) {
                                Text("Проверить исключения ($hiddenCount)")
                            }
                        }
                    }
                }
            }
        } else {
            item(key = "folder_sort_bar") {
                FolderSortHeaderBar(
                    folderCount = visibleFolders.size,
                    sortOption = sortOption,
                    ascending = sortAscending,
                    onOpenSortSheet = onOpenSortSheet,
                    onToggleDirection = onToggleSortDirection
                )
            }

            itemsIndexed(
                items = visibleFolders,
                key = { _, f -> f.id }
            ) { index, folder ->
                VideoFolderCard(
                    folder = folder,
                    index = index,
                    compactMode = compactMode,
                    onClick = { onOpenFolder(folder) },
                    onHide = { onHideFolder(folder) }
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CompactHomeActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String,
    emphasized: Boolean = false,
    tonal: Boolean = false
) {
    val bgColor = when {
        emphasized -> MaterialTheme.colorScheme.primary
        tonal -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = when {
        emphasized -> MaterialTheme.colorScheme.onPrimary
        tonal -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StorageStatusEmptyCard(
    hasStorageAccess: Boolean,
    isScanning: Boolean,
    hasHiddenFolders: Boolean,
    hiddenCount: Int,
    onRequestStorageAccess: () -> Unit,
    onAddExternalFolder: () -> Unit,
    onOpenExclusions: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_library_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                !hasStorageAccess -> {
                    Text(
                        text = stringResource(R.string.storage_access_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.storage_access_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onRequestStorageAccess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hero_grant_storage_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_grant_storage))
                    }
                    OutlinedButton(
                        onClick = onAddExternalFolder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hero_add_folder_button")
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Или выбрать отдельную папку")
                    }
                }

                isScanning -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = stringResource(R.string.scanning_storage_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = stringResource(R.string.scanning_storage_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                hasHiddenFolders -> {
                    Text(
                        text = "Все найденные папки сейчас в исключениях ($hiddenCount)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Вы можете вернуть нужные папки обратно из списка исключений.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onOpenExclusions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hero_open_hidden_button")
                    ) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Открыть исключения ($hiddenCount)")
                    }
                }

                else -> {
                    Text(
                        text = stringResource(R.string.empty_storage_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.empty_storage_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FilledTonalButton(
                        onClick = onAddExternalFolder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hero_add_folder_button")
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Подключить внешнюю папку (USB / SD)")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoFolderCard(
    folder: VideoFolderItem,
    index: Int,
    compactMode: Boolean = true,
    onClick: () -> Unit,
    onHide: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val cardCorner = if (compactMode) 16.dp else 20.dp

    Card(
        shape = RoundedCornerShape(cardCorner),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cardCorner))
            .clickable(onClick = onClick)
            .testTag("folder_card_$index")
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compactMode) 12.dp else 16.dp,
                vertical = if (compactMode) 10.dp else 16.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compactMode) 10.dp else 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compactMode) 44.dp else 54.dp)
                        .clip(RoundedCornerShape(if (compactMode) 12.dp else 15.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (compactMode) 22.dp else 26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val folderMeta = buildString {
                        append("${folder.episodes.size} видео")
                        val sizeLabel = formatFileSize(folder.totalSizeBytes)
                        if (sizeLabel.isNotEmpty()) {
                            append(" • ")
                            append(sizeLabel)
                        }
                        append(" • ")
                        append(folder.displayPath)
                    }
                    Text(
                        text = folderMeta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("folder_menu_button_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Меню папки ${folder.name}"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("В исключения") },
                            leadingIcon = {
                                Icon(Icons.Outlined.VisibilityOff, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onHide()
                            },
                            modifier = Modifier.testTag("hide_folder_menu_item_$index")
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = folder.totalAutoAudioCount > 0 || folder.totalAutoSubCount > 0 || folder.watchedCount > 0
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (folder.totalAutoAudioCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Авто-аудио (${folder.totalAutoAudioCount})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        if (folder.totalAutoSubCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Subtitles,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "SRT / ASS (${folder.totalAutoSubCount})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        if (folder.watchedCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Просмотрено: ${folder.watchedCount}/${folder.episodes.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (folder.watchedCount > 0 && folder.episodes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { folder.watchedCount.toFloat() / folder.episodes.size.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }
            }
        }
    }
}
