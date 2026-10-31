package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.data.PlaybackResumeHelper
import com.example.model.EpisodeItem
import com.example.model.EpisodeSortOption
import com.example.model.VideoFolderItem
import com.example.ui.components.ResolutionBadgeChip
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatTimeMs
import com.example.ui.i18n.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folder: VideoFolderItem,
    showThumbnails: Boolean,
    compactMode: Boolean = true,
    resumePlaybackEnabled: Boolean,
    sortOption: EpisodeSortOption = EpisodeSortOption.EPISODE_NUMBER,
    sortAscending: Boolean = true,
    onSelectSortOption: (EpisodeSortOption) -> Unit = {},
    onSetSortDirection: (Boolean) -> Unit = {},
    onToggleSortDirection: () -> Unit = {},
    onBack: () -> Unit,
    onPlayEpisode: (Int) -> Unit,
    onHideFolder: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showEpisodeSortSheet by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val videoThumbnailLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
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
        topBar = {
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
                        onClick = onHideFolder,
                        modifier = Modifier.testTag("folder_detail_hide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = tr("Добавить папку в исключения", "Exclude folder")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (folder.episodes.isNotEmpty()) {
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
                    key = { _, ep -> ep.uri }
                ) { index, episode ->
                    EpisodeRowCard(
                        episode = episode,
                        index = index,
                        showThumbnail = showThumbnails,
                        compactMode = compactMode,
                        imageLoader = videoThumbnailLoader,
                        onClick = { onPlayEpisode(index) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeRowCard(
    episode: EpisodeItem,
    index: Int,
    showThumbnail: Boolean,
    compactMode: Boolean = true,
    imageLoader: ImageLoader,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val corner = if (compactMode) 15.dp else 18.dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .clickable(onClick = onClick)
            .testTag("episode_card_$index"),
        shape = RoundedCornerShape(corner),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                    Box(
                        modifier = Modifier
                            .width(if (compactMode) 112.dp else 124.dp)
                            .height(if (compactMode) 66.dp else 74.dp)
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
                            model = ImageRequest.Builder(context)
                                .data(Uri.parse(episode.uri))
                                .videoFrameMillis(2000L)
                                .crossfade(true)
                                .build(),
                            imageLoader = imageLoader,
                            contentDescription = episode.cleanTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.Black.copy(alpha = 0.62f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
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
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
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

                    val metaLine = buildString {
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
