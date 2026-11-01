package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PlayerSettings
import com.example.torrent.TorrServerConnectionState
import com.example.torrent.TorrServerTorrent
import com.example.torrent.TorrentIntentHelper
import com.example.torrent.TorrentQualityFilter
import com.example.torrent.TorrentSearchResult
import com.example.torrent.TorrentSortMode
import com.example.torrent.TorrentTrackerGroup
import com.example.ui.TorrentsUiState
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.localizedShortLabel
import com.example.ui.i18n.localizedTitle
import com.example.ui.i18n.tr

data class TorrentsActions(
    val onQueryChange: (String) -> Unit,
    val onSubmitSearch: (String?) -> Unit,
    val onSelectTrackerFilter: (TorrentTrackerGroup) -> Unit,
    val onSelectQualityFilter: (TorrentQualityFilter) -> Unit,
    val onSelectSortMode: (TorrentSortMode) -> Unit,
    val onCheckConnection: () -> Unit,
    val onWatchResult: (TorrentSearchResult) -> Unit,
    val onWatchRawLink: (String, String) -> Unit,
    val onPickTorrentFileForWatch: () -> Unit,
    val onPickTorrentFileForExternal: () -> Unit,
    val onSaveServerConfig: (String, String, String) -> Unit,
    val onSelectInspectorVoiceover: (String?) -> Unit,
    val onPlayInspectorEpisode: (Int) -> Unit,
    val onForceDirectStream: (String, String) -> Unit,
    val onDismissInspector: () -> Unit,
    val onLoadSavedTorrents: () -> Unit,
    val onOpenSavedTorrent: (TorrServerTorrent) -> Unit,
    val onDropSavedCache: (String) -> Unit,
    val onRemoveSavedTorrent: (String) -> Unit,
    val onOpenReleaseDetails: (TorrentSearchResult) -> Unit = {},
    val onDismissReleaseDetails: () -> Unit = {}
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TorrentsTabContent(
    state: TorrentsUiState,
    settings: PlayerSettings,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String?) -> Unit,
    onSelectTrackerFilter: (TorrentTrackerGroup) -> Unit,
    onSelectQualityFilter: (TorrentQualityFilter) -> Unit,
    onSelectSortMode: (TorrentSortMode) -> Unit,
    onCheckConnection: () -> Unit,
    onWatchResult: (TorrentSearchResult) -> Unit,
    onOpenExternalResult: (TorrentSearchResult) -> Unit,
    onCopyMagnet: (String, String) -> Unit,
    onOpenManualDialog: () -> Unit,
    onOpenServerConfigDialog: () -> Unit,
    onLoadSavedTorrents: () -> Unit,
    onOpenSavedTorrent: (TorrServerTorrent) -> Unit,
    onOpenExternalLink: (magnetUri: String, title: String) -> Unit,
    onDropSavedCache: (String) -> Unit,
    onRemoveSavedTorrent: (String) -> Unit,
    onOpenReleaseDetails: (TorrentSearchResult) -> Unit = {},
    onDismissReleaseDetails: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isEnglish = LocalIsEnglish.current
    var showSavedTorrentsSheet by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val isTorrServerInstalled = remember(context) {
        TorrentIntentHelper.isTorrServerAppInstalled(context)
    }

    LaunchedEffect(Unit) {
        if (state.connectionState is TorrServerConnectionState.Idle) {
            onCheckConnection()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .testTag("torrents_lazy_list"),
        contentPadding = PaddingValues(
            horizontal = if (settings.compactUiMode) 12.dp else 16.dp,
            vertical = if (settings.compactUiMode) 5.dp else 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (settings.compactUiMode) 8.dp else 12.dp)
    ) {
        item(key = "torrserver_status_card") {
            TorrServerStatusBanner(
                connectionState = state.connectionState,
                configuredUrl = settings.torrServerUrl,
                isTorrServerInstalled = isTorrServerInstalled,
                onCheckConnection = onCheckConnection,
                onOpenManualDialog = onOpenManualDialog,
                onOpenSavedSheet = {
                    onLoadSavedTorrents()
                    showSavedTorrentsSheet = true
                },
                onOpenServerConfig = onOpenServerConfigDialog,
                onLaunchTorrServerApp = { TorrentIntentHelper.launchTorrServerApp(context) }
            )
        }

        item(key = "torrent_search_bar") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.torrents_search_placeholder),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = tr("Очистить запрос", "Clear query"))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onSubmitSearch(null)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("torrent_search_input")
                )

                FilledIconButton(
                    onClick = {
                        focusManager.clearFocus()
                        onSubmitSearch(null)
                    },
                    enabled = state.query.isNotBlank() && !state.isSearching,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("torrent_search_submit_button")
                ) {
                    if (state.isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = tr("Найти торренты", "Search torrents"))
                    }
                }
            }
        }

        item(key = "tracker_filter_row") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = TorrentTrackerGroup.entries,
                    key = { it.id }
                ) { group ->
                    val count = state.trackerCounts[group]
                    val groupLabel = group.localizedLabel(isEnglish)
                    val chipText = if (state.hasSearched && count != null && count > 0) {
                        "$groupLabel ($count)"
                    } else {
                        groupLabel
                    }
                    FilterChip(
                        selected = state.trackerFilter == group,
                        onClick = { onSelectTrackerFilter(group) },
                        label = { Text(chipText) },
                        modifier = Modifier.testTag("tracker_filter_${group.id}")
                    )
                }
            }
        }

        if (state.hasSearched || state.rawResults.isNotEmpty()) {
            item(key = "quality_and_sort_row") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item(key = "sort_dropdown_chip") {
                            Box {
                                AssistChip(
                                    onClick = { showSortMenu = true },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Sort,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text(state.sortMode.localizedShortLabel(isEnglish)) },
                                    modifier = Modifier.testTag("torrent_sort_chip")
                                )
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    TorrentSortMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(mode.localizedTitle(isEnglish)) },
                                            onClick = {
                                                showSortMenu = false
                                                onSelectSortMode(mode)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = TorrentQualityFilter.entries,
                            key = { it.id }
                        ) { qFilter ->
                            FilterChip(
                                selected = state.qualityFilter == qFilter,
                                onClick = { onSelectQualityFilter(qFilter) },
                                label = { Text(qFilter.localizedLabel(isEnglish)) },
                                modifier = Modifier.testTag("quality_filter_${qFilter.id}")
                            )
                        }
                    }

                    if (!state.isSearching && state.rawResults.isNotEmpty()) {
                        val sourcesSummary = if (state.activeSources.isNotEmpty()) {
                            tr(
                                " • Источники: ${state.activeSources.joinToString(", ")}",
                                " • Sources: ${state.activeSources.joinToString(", ")}"
                            )
                        } else {
                            ""
                        }
                        Text(
                            text = tr(
                                "Раздач: ${state.filteredResults.size} из ${state.rawResults.size}$sourcesSummary",
                                "Torrents: ${state.filteredResults.size} of ${state.rawResults.size}$sourcesSummary"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        when {
            state.isSearching -> {
                item(key = "searching_progress_card") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("torrent_searching_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.6.dp
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = tr("Поиск по трекерам…", "Searching trackers…"),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = tr(
                                        "Опрос RuTracker, Rutor и подключённых индексаторов",
                                        "Querying RuTracker, Rutor, and connected indexers"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            !state.hasSearched -> {
                item(key = "initial_torrents_hero") {
                    TorrentsInitialHeroCard(
                        recentQueries = state.recentQueries,
                        onSelectRecentQuery = { q ->
                            onQueryChange(q)
                            onSubmitSearch(q)
                        },
                        onOpenManualDialog = onOpenManualDialog
                    )
                }
            }

            state.filteredResults.isEmpty() -> {
                item(key = "empty_torrent_results") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("torrent_empty_results_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (state.rawResults.isNotEmpty()) {
                                    tr("Нет раздач под выбранный фильтр", "No torrents match the selected filter")
                                } else {
                                    tr("Раздачи не найдены", "No torrents found")
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = state.searchMessage
                                    ?: tr(
                                        "Попробуйте изменить запрос, сбросить фильтр качества или проверить адрес индексатора в настройках TorrServer.",
                                        "Try changing your query, resetting the quality filter, or checking the indexer URL in TorrServer settings."
                                    ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (state.rawResults.isNotEmpty()) {
                                    FilledTonalButton(
                                        onClick = {
                                            onSelectTrackerFilter(TorrentTrackerGroup.ALL)
                                            onSelectQualityFilter(TorrentQualityFilter.ALL)
                                        }
                                    ) {
                                        Text(tr("Сбросить фильтры", "Reset filters"))
                                    }
                                } else {
                                    FilledTonalButton(onClick = { onSubmitSearch(null) }) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tr("Повторить поиск", "Retry search"))
                                    }
                                }

                                OutlinedButton(onClick = onOpenManualDialog) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.action_open_magnet_or_file))
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                itemsIndexed(
                    items = state.filteredResults,
                    key = { idx, item -> "${item.id}_$idx" }
                ) { index, item ->
                    TorrentResultCard(
                        item = item,
                        index = index,
                        onWatch = { onWatchResult(item) },
                        onOpenDetails = { onOpenReleaseDetails(item) },
                        onOpenExternal = { onOpenExternalResult(item) },
                        onCopyMagnet = { magnet -> onCopyMagnet(magnet, item.title) }
                    )
                }
            }
        }

        item(key = "torrents_bottom_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (state.selectedReleaseDetails != null) {
        TorrentReleaseDetailsModalSheet(
            details = state.selectedReleaseDetails,
            onWatch = onWatchResult,
            onOpenExternal = onOpenExternalResult,
            onCopyMagnet = onCopyMagnet,
            onDismiss = onDismissReleaseDetails
        )
    }

    if (showSavedTorrentsSheet) {
        TorrServerSavedTorrentsSheet(
            savedTorrents = state.savedServerTorrents,
            isLoading = state.isLoadingSavedTorrents,
            onRefresh = onLoadSavedTorrents,
            onOpenTorrent = onOpenSavedTorrent,
            onOpenExternal = onOpenExternalLink,
            onDropCache = onDropSavedCache,
            onRemoveTorrent = onRemoveSavedTorrent,
            onOpenServerConfig = onOpenServerConfigDialog,
            onDismiss = { showSavedTorrentsSheet = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TorrServerStatusBanner(
    connectionState: TorrServerConnectionState,
    configuredUrl: String,
    isTorrServerInstalled: Boolean,
    onCheckConnection: () -> Unit,
    onOpenManualDialog: () -> Unit,
    onOpenSavedSheet: () -> Unit,
    onOpenServerConfig: () -> Unit,
    onLaunchTorrServerApp: () -> Unit
) {
    val isOnline = connectionState is TorrServerConnectionState.Connected
    val isChecking = connectionState is TorrServerConnectionState.Checking

    val statusLabel = when (connectionState) {
        is TorrServerConnectionState.Connected -> {
            val verSuffix = connectionState.version
                .takeIf { it.isNotBlank() && !it.equals("Онлайн", ignoreCase = true) && !it.equals("Online", ignoreCase = true) }
                ?.let { " ($it)" }
                .orEmpty()
            "${stringResource(R.string.torrserver_online_badge)}$verSuffix"
        }
        TorrServerConnectionState.Checking -> tr("Проверка TorrServer…", "Checking TorrServer…")
        is TorrServerConnectionState.Disconnected -> stringResource(R.string.torrserver_offline_badge)
        TorrServerConnectionState.Idle -> "TorrServer: $configuredUrl"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isOnline) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("torrserver_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenServerConfig)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOnline) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                )
                        )
                    }
                    Column {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isOnline) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = configuredUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCheckConnection,
                        modifier = Modifier.testTag("torrserver_ping_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = tr("Проверить соединение с TorrServer", "Test TorrServer connection")
                        )
                    }
                    IconButton(
                        onClick = onOpenServerConfig,
                        modifier = Modifier.testTag("torrserver_quick_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = tr("Настроить адрес TorrServer", "Configure TorrServer URL")
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onOpenManualDialog,
                    modifier = Modifier.testTag("torrents_open_manual_dialog_button")
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_open_magnet_or_file))
                }

                OutlinedButton(
                    onClick = onOpenSavedSheet,
                    modifier = Modifier.testTag("torrents_open_saved_list_button")
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_saved_torrserver_list))
                }

                if (!isOnline && isTorrServerInstalled) {
                    OutlinedButton(
                        onClick = onLaunchTorrServerApp,
                        modifier = Modifier.testTag("torrents_launch_torrserver_app_button")
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Запустить TorrServer", "Launch TorrServer"))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TorrentsInitialHeroCard(
    recentQueries: List<String>,
    onSelectRecentQuery: (String) -> Unit,
    onOpenManualDialog: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("torrents_initial_hero_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = tr("Поиск торрентов и стриминг через TorrServer", "Torrent Search & TorrServer Streaming"),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = tr(
                    "Приоритетная выдача с RuTracker, Rutor, Kinozal, NNM-Club и аниме-трекеров. Любую раздачу или .torrent файл можно сразу смотреть в плеере с Anime4K и внешними озвучками (.mka) либо открыть в стороннем торрент-клиенте.",
                    "Priority results from RuTracker, Rutor, Kinozal, NNM-Club, and anime trackers. Stream any torrent or .torrent file directly in the player with Anime4K and external audio (.mka), or open in an external client."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (recentQueries.isNotEmpty()) {
                Text(
                    text = tr("Недавние запросы", "Recent queries"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recentQueries.forEach { q ->
                        AssistChip(
                            onClick = { onSelectRecentQuery(q) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(q) }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenManualDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("Вставить Magnet-ссылку или выбрать .torrent файл", "Paste Magnet link or select .torrent file"))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TorrentResultCard(
    item: TorrentSearchResult,
    index: Int,
    onWatch: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenExternal: () -> Unit,
    onCopyMagnet: (String) -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    val trackerContainerColor = when (item.trackerGroup) {
        TorrentTrackerGroup.RUTRACKER -> MaterialTheme.colorScheme.primaryContainer
        TorrentTrackerGroup.RUTOR -> MaterialTheme.colorScheme.secondaryContainer
        TorrentTrackerGroup.ANIME -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val trackerTextColor = when (item.trackerGroup) {
        TorrentTrackerGroup.RUTRACKER -> MaterialTheme.colorScheme.onPrimaryContainer
        TorrentTrackerGroup.RUTOR -> MaterialTheme.colorScheme.onSecondaryContainer
        TorrentTrackerGroup.ANIME -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("torrent_result_card_$index")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = trackerContainerColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.trackerName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = trackerTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (!item.qualityBadge.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = item.qualityBadge,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (item.sizeLabel.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = item.sizeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenDetails,
                        modifier = Modifier.testTag("torrent_card_details_button_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = tr("Подробнее о раздаче", "Release details"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("torrent_card_menu_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = tr("Дополнительные действия", "More actions")
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(tr("Подробная информация о раздаче", "Detailed release info")) },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenDetails()
                                }
                            )
                            if (!item.magnetUri.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text(tr("Скопировать Magnet-ссылку", "Copy Magnet link")) },
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onCopyMagnet(item.magnetUri)
                                    }
                                )
                            }
                            if (!item.topicUrl.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text(tr("Страница раздачи на трекере", "Open tracker topic page")) },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        TorrentIntentHelper.openUrlInBrowser(context, item.topicUrl)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenDetails)
                    .padding(vertical = 2.dp)
                    .testTag("torrent_card_title_$index"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tr("Нажмите для полного названия и описания ▸", "Tap for full title & release details ▸"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tr("▲ Сиды: ${item.seeders}", "▲ Seeds: ${item.seeders}"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (item.seeders > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = tr("▼ Пиры: ${item.leechers}", "▼ Peers: ${item.leechers}"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.publishedLabel.isNotBlank()) {
                    Text(
                        text = "• ${item.publishedLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onWatch,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("watch_torrent_button_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_watch_torrent),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onOpenExternal,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("external_torrent_button_$index")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_open_external_torrent),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
