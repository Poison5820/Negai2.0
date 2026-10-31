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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.EpisodeItem
import com.example.model.PlayerSettings
import com.example.torrent.BencodeTorrentParser
import com.example.torrent.TorrServerClient
import com.example.torrent.TorrServerTorrent
import com.example.torrent.TorrentInspectorState
import com.example.torrent.TorrentIntentHelper
import com.example.ui.components.formatFileSize
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.tr
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TorrentInspectorBottomSheet(
    inspectorState: TorrentInspectorState,
    onDismiss: () -> Unit,
    onSelectVoiceover: (String?) -> Unit,
    onPlayEpisode: (Int) -> Unit,
    onForceDirectStream: (String, String) -> Unit,
    onRetryLink: (String, String) -> Unit,
    onOpenExternal: (magnetUri: String?, torrentFileUrl: String?, title: String) -> Unit,
    onOpenServerConfig: () -> Unit
) {
    if (inspectorState is TorrentInspectorState.Hidden) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isTorrServerInstalled = remember(context) {
        TorrentIntentHelper.isTorrServerAppInstalled(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.testTag("torrent_inspector_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (inspectorState) {
                TorrentInspectorState.Hidden -> Unit

                is TorrentInspectorState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.6.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Подготовка потока TorrServer", "Preparing TorrServer Stream"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = inspectorState.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Text(
                        text = inspectorState.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val directCandidate = inspectorState.magnetOrLink ?: inspectorState.torrentFileUrl
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (inspectorState.canForceDirectStream && !directCandidate.isNullOrBlank()) {
                            FilledTonalButton(
                                onClick = {
                                    onForceDirectStream(directCandidate, inspectorState.title)
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("inspector_force_direct_stream_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("Запустить поток сразу", "Start stream immediately"))
                            }
                        }

                        if (!directCandidate.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    onOpenExternal(
                                        inspectorState.magnetOrLink,
                                        inspectorState.torrentFileUrl,
                                        inspectorState.title
                                    )
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("inspector_loading_open_external_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_open_external_torrent))
                            }
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(tr("Отмена", "Cancel"))
                        }
                    }
                }

                is TorrentInspectorState.Ready -> {
                    val bundle = inspectorState.bundle
                    val torrent = inspectorState.torrent

                    Text(
                        text = torrent.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InspectorMetaChip(tr("${bundle.episodes.size} видео", "${bundle.episodes.size} videos"))
                        if (torrent.torrentSize > 0L) {
                            InspectorMetaChip(formatFileSize(torrent.torrentSize))
                        }
                        if (torrent.connectedSeeders > 0 || torrent.activePeers > 0) {
                            InspectorMetaChip(
                                tr(
                                    "Пиры: ${torrent.activePeers} (сиды: ${torrent.connectedSeeders})",
                                    "Peers: ${torrent.activePeers} (seeds: ${torrent.connectedSeeders})"
                                )
                            )
                        }
                        if (bundle.subtitleTrackCount > 0) {
                            InspectorMetaChip(
                                tr(
                                    "Субтитры: ${bundle.subtitleTrackCount}",
                                    "Subtitles: ${bundle.subtitleTrackCount}"
                                )
                            )
                        }
                    }

                    if (bundle.availableVoiceovers.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = tr("Внешняя озвучка (.mka / аудиопапки)", "External voiceover (.mka / audio folders)"),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = bundle.selectedVoiceover == null,
                                    onClick = { onSelectVoiceover(null) },
                                    label = { Text(tr("Все дорожки", "All tracks")) }
                                )
                                bundle.availableVoiceovers.forEach { voice ->
                                    FilterChip(
                                        selected = bundle.selectedVoiceover == voice,
                                        onClick = { onSelectVoiceover(voice) },
                                        label = { Text(voice) }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onPlayEpisode(0) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("inspector_play_first_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (bundle.episodes.size > 1) {
                                    tr("Смотреть с 1-й", "Watch from ep. 1")
                                } else {
                                    stringResource(R.string.action_watch_torrent)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onOpenExternal(
                                    inspectorState.magnetOrLink,
                                    inspectorState.torrentFileUrl,
                                    torrent.title
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("inspector_ready_open_external_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                            .testTag("inspector_episodes_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(
                            items = bundle.episodes,
                            key = { idx, ep -> "${ep.uri}_$idx" }
                        ) { index, episode ->
                            InspectorEpisodeRow(
                                episode = episode,
                                index = index,
                                onClick = { onPlayEpisode(index) }
                            )
                        }
                    }
                }

                is TorrentInspectorState.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = inspectorState.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = inspectorState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    val retryCandidate = inspectorState.magnetOrLink ?: inspectorState.torrentFileUrl
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTorrServerInstalled) {
                            Button(
                                onClick = { TorrentIntentHelper.launchTorrServerApp(context) },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("inspector_launch_torrserver_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("Открыть TorrServer", "Open TorrServer"))
                            }
                        }

                        if (!retryCandidate.isNullOrBlank()) {
                            FilledTonalButton(
                                onClick = { onRetryLink(retryCandidate, inspectorState.title) },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("inspector_retry_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("Повторить", "Retry"))
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenServerConfig()
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("inspector_configure_server_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Настроить сервер", "Configure server"))
                        }

                        if (!retryCandidate.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    onOpenExternal(
                                        inspectorState.magnetOrLink,
                                        inspectorState.torrentFileUrl,
                                        inspectorState.title
                                    )
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("inspector_error_open_external_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_open_external_torrent))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorMetaChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun InspectorEpisodeRow(
    episode: EpisodeItem,
    index: Int,
    onClick: () -> Unit
) {
    val audioPrefix = tr("Аудио", "Audio")
    val subsPrefix = tr("Субтитры", "Subtitles")
    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("inspector_episode_row_$index")
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = tr("Воспроизвести ${episode.cleanTitle}", "Play ${episode.cleanTitle}"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = episode.episodeBadge ?: "#${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = episode.cleanTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val subMeta = buildString {
                    val sz = formatFileSize(episode.sizeBytes)
                    if (sz.isNotEmpty()) append(sz)
                    if (episode.autoAudioTracks.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append("$audioPrefix: ${episode.autoAudioTracks.size}")
                    }
                    if (episode.autoSubtitleTracks.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append("$subsPrefix: ${episode.autoSubtitleTracks.size}")
                    }
                }
                if (subMeta.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (episode.autoAudioTracks.isNotEmpty()) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (episode.autoSubtitleTracks.isNotEmpty()) {
                            Icon(
                                Icons.Default.Subtitles,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = subMeta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrServerSavedTorrentsSheet(
    savedTorrents: List<TorrServerTorrent>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onOpenTorrent: (TorrServerTorrent) -> Unit,
    onOpenExternal: (magnetUri: String, title: String) -> Unit,
    onDropCache: (String) -> Unit,
    onRemoveTorrent: (String) -> Unit,
    onOpenServerConfig: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.testTag("saved_torrserver_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Торренты на TorrServer", "Torrents on TorrServer"),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = tr(
                            "Сохранённые раздачи для быстрого продолжения просмотра",
                            "Saved torrents for quick resume"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_saved_torrents_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = tr("Обновить список торрентов сервера", "Refresh server torrents")
                        )
                    }
                }
            }

            if (savedTorrents.isEmpty() && !isLoading) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = tr(
                                "Список сохранённых раздач пуст или TorrServer не запущен.",
                                "Saved torrent list is empty or TorrServer is not running."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenServerConfig()
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Проверить адрес TorrServer", "Check TorrServer address"))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(
                        items = savedTorrents,
                        key = { idx, item -> item.hash.ifBlank { "saved_$idx" } }
                    ) { index, item ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onDismiss()
                                    onOpenTorrent(item)
                                }
                                .testTag("saved_torrent_card_$index")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val meta = buildString {
                                    if (item.torrentSize > 0L) {
                                        append(formatFileSize(item.torrentSize))
                                    }
                                    if (item.statString.isNotBlank()) {
                                        if (isNotEmpty()) append(" • ")
                                        append(item.statString)
                                    }
                                }
                                if (meta.isNotEmpty()) {
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenTorrent(item)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(tr("Смотреть", "Watch"))
                                    }

                                    if (item.hash.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                onOpenExternal(
                                                    BencodeTorrentParser.buildMagnetUri(item.hash, item.title),
                                                    item.title
                                                )
                                            }
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = tr("Открыть во внешнем приложении", "Open in external app")
                                            )
                                        }
                                        IconButton(onClick = { onDropCache(item.hash) }) {
                                            Icon(
                                                Icons.Default.CleaningServices,
                                                contentDescription = tr("Сбросить буфер раздачи", "Drop torrent cache")
                                            )
                                        }
                                        IconButton(onClick = { onRemoveTorrent(item.hash) }) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = tr("Удалить торрент с сервера", "Remove torrent from server"),
                                                tint = MaterialTheme.colorScheme.error
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
    }
}

@Composable
fun ManualTorrentInputDialog(
    onWatchLink: (rawLink: String, title: String) -> Unit,
    onOpenExternalLink: (rawLink: String, title: String) -> Unit,
    onPickTorrentFileForWatch: () -> Unit,
    onPickTorrentFileForExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    var linkInput by rememberSaveable { mutableStateOf("") }
    var titleInput by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Открыть Magnet или .torrent", "Open Magnet or .torrent")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr(
                        "Вставьте Magnet-ссылку, InfoHash или прямую ссылку на .torrent, либо выберите файл с устройства.",
                        "Paste a Magnet link, InfoHash, or direct .torrent URL, or pick a file from your device."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = linkInput,
                    onValueChange = { linkInput = it },
                    label = { Text("Magnet / InfoHash / URL .torrent") },
                    placeholder = { Text("magnet:?xt=urn:btih:…") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_torrent_link_input")
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(tr("Название (необязательно)", "Title (optional)")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Text(
                    text = tr("Локальный .torrent файл на устройстве:", "Local .torrent file on device:"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onPickTorrentFileForWatch()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("pick_local_torrent_watch_button")
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Выбрать .torrent для просмотра", "Pick .torrent to watch"))
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onPickTorrentFileForExternal()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("pick_local_torrent_external_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Выбрать .torrent для другого приложения", "Pick .torrent for external app"))
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val cleaned = linkInput.trim()
                        if (cleaned.isNotEmpty()) {
                            onDismiss()
                            onOpenExternalLink(cleaned, titleInput.trim())
                        }
                    },
                    enabled = linkInput.isNotBlank(),
                    modifier = Modifier.testTag("manual_torrent_external_button")
                ) {
                    Text(tr("В приложение", "In App"))
                }
                Button(
                    onClick = {
                        val cleaned = linkInput.trim()
                        if (cleaned.isNotEmpty()) {
                            onDismiss()
                            onWatchLink(cleaned, titleInput.trim())
                        }
                    },
                    enabled = linkInput.isNotBlank(),
                    modifier = Modifier.testTag("manual_torrent_watch_button")
                ) {
                    Text(stringResource(R.string.action_watch_torrent))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Закрыть", "Close"))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TorrServerQuickConfigDialog(
    settings: PlayerSettings,
    onSave: (url: String, username: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEnglish = LocalIsEnglish.current

    var urlInput by rememberSaveable(settings.torrServerUrl) {
        mutableStateOf(settings.torrServerUrl)
    }
    var userInput by rememberSaveable(settings.torrServerUsername) {
        mutableStateOf(settings.torrServerUsername)
    }
    var passInput by rememberSaveable(settings.torrServerPassword) {
        mutableStateOf(settings.torrServerPassword)
    }
    var isTesting by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var testOk by remember { mutableStateOf<Boolean?>(null) }

    val isTorrServerInstalled = remember(context) {
        TorrentIntentHelper.isTorrServerAppInstalled(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Настройка TorrServer", "TorrServer Configuration")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = urlInput.contains("127.0.0.1"),
                        onClick = { urlInput = "http://127.0.0.1:8090" },
                        label = { Text("127.0.0.1:8090") }
                    )
                    FilterChip(
                        selected = urlInput.startsWith("http://192.168."),
                        onClick = {
                            if (!urlInput.startsWith("http://192.168.")) {
                                urlInput = "http://192.168.1.100:8090"
                            }
                        },
                        label = { Text("192.168.x.x") }
                    )
                }

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text(tr("Адрес TorrServer", "TorrServer URL")) },
                    placeholder = { Text("http://127.0.0.1:8090") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_torrserver_url_input")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        label = { Text(tr("Логин", "Username")) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = passInput,
                        onValueChange = { passInput = it },
                        label = { Text(tr("Пароль", "Password")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (testResultText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (testOk == true) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (testOk == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = testResultText.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testOk == true) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                isTesting = true
                                val norm = TorrServerClient.normalizeBaseUrl(urlInput)
                                val res = TorrServerClient.ping(norm, userInput, passInput)
                                isTesting = false
                                res.fold(
                                    onSuccess = { ver ->
                                        testOk = true
                                        testResultText = if (isEnglish) "Online: TorrServer $ver" else "Онлайн: TorrServer $ver"
                                    },
                                    onFailure = { err ->
                                        testOk = false
                                        testResultText = err.message ?: if (isEnglish) "No response from server" else "Нет ответа от сервера"
                                    }
                                )
                            }
                        },
                        enabled = !isTesting,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Проверить", "Test"))
                    }

                    if (isTorrServerInstalled) {
                        OutlinedButton(
                            onClick = { TorrentIntentHelper.launchTorrServerApp(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr("Запустить App", "Launch App"))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val norm = TorrServerClient.normalizeBaseUrl(urlInput)
                    onSave(norm, userInput, passInput)
                    onDismiss()
                },
                modifier = Modifier.testTag("quick_torrserver_save_button")
            ) {
                Text(tr("Сохранить", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Отмена", "Cancel"))
            }
        }
    )
}
