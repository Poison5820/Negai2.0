package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.HiddenFolderEntity
import com.example.data.LibraryRootEntity
import com.example.model.Anime4KPreset
import com.example.model.AppThemeMode
import com.example.model.DecoderMode
import com.example.model.FrameInterpolationMode
import com.example.model.PlaybackEndMode
import com.example.model.PlayerControlsTheme
import com.example.model.PlayerOrientationMode
import com.example.model.PlayerQuickAction
import com.example.model.PlayerSettings
import com.example.model.SettingsCategory
import com.example.model.SubtitleColorPreset
import com.example.model.VideoScaleMode
import kotlin.math.roundToInt

data class SettingsActions(
    val onRequestStorageAccess: () -> Unit,
    val onAddSafFolder: () -> Unit,
    val onRemoveSafRoot: (String) -> Unit,
    val onPickFolderToExclude: () -> Unit,
    val onOpenExclusionsSheet: () -> Unit,
    val onUnhideFolder: (String) -> Unit,
    val onClearPlaybackHistory: () -> Unit,
    val onResetAllSettings: () -> Unit,
    val onUpdateThemeMode: (AppThemeMode) -> Unit,
    val onUpdatePlayerControlsTheme: (PlayerControlsTheme) -> Unit = {},
    val onUpdateCompactUiMode: (Boolean) -> Unit = {},
    val onUpdateShowEpisodeThumbnails: (Boolean) -> Unit,
    val onUpdateShowEpisodesButtonInHud: (Boolean) -> Unit = {},
    val onUpdateShowHudBadges: (Boolean) -> Unit = {},
    val onUpdateQuickActions: (List<PlayerQuickAction>) -> Unit,
    val onUpdateResumePlayback: (Boolean) -> Unit,
    val onUpdatePlaybackEndMode: (PlaybackEndMode) -> Unit,
    val onUpdateEnableEmbeddedChapters: (Boolean) -> Unit = {},
    val onUpdateShowChapterMarkersOnTimeline: (Boolean) -> Unit = {},
    val onUpdateSeekStepSeconds: (Int) -> Unit,
    val onUpdateDefaultPlaybackSpeed: (Float) -> Unit,
    val onUpdateShowSkipIntro: (Boolean) -> Unit,
    val onUpdateSkipIntroSeconds: (Int) -> Unit,
    val onUpdateAutoEnterPip: (Boolean) -> Unit,
    val onUpdateBackgroundAudioPlay: (Boolean) -> Unit,
    val onUpdateGesturesBrightnessVolume: (Boolean) -> Unit,
    val onUpdateGestureHorizontalSeek: (Boolean) -> Unit,
    val onUpdateGestureDoubleTapSeek: (Boolean) -> Unit,
    val onUpdateGestureLongPress2x: (Boolean) -> Unit,
    val onUpdateLongPressBoostSpeed: (Float) -> Unit,
    val onUpdateDefaultPlayerOrientation: (PlayerOrientationMode) -> Unit,
    val onUpdateDecoderMode: (DecoderMode) -> Unit,
    val onUpdateDefaultScaleMode: (VideoScaleMode) -> Unit,
    val onUpdateHighRefreshRateEnabled: (Boolean) -> Unit = {},
    val onUpdateLtpoAdaptiveRefreshEnabled: (Boolean) -> Unit = {},
    val onUpdateFrameInterpolationEnabled: (Boolean) -> Unit = {},
    val onUpdateFrameInterpolationMode: (FrameInterpolationMode) -> Unit = {},
    val onUpdateAnime4KEnabled: (Boolean) -> Unit,
    val onUpdateAnime4KPreset: (Anime4KPreset) -> Unit,
    val onUpdateAnime4KStrength: (Float) -> Unit,
    val onUpdateAutoBindAudio: (Boolean) -> Unit,
    val onUpdatePreferExternalAudioOnStart: (Boolean) -> Unit,
    val onUpdateDefaultAudioBoostPercent: (Int) -> Unit,
    val onUpdateAutoBindSubs: (Boolean) -> Unit,
    val onUpdateSubtitleFontSize: (Float) -> Unit,
    val onUpdateSubtitleColor: (Long) -> Unit,
    val onUpdateSubtitleBg: (Boolean) -> Unit,
    val onUpdateTorrServerUrl: (String) -> Unit = {},
    val onUpdateTorrServerAuth: (String, String) -> Unit = { _, _ -> },
    val onUpdateTorrServerAutoPlaySingle: (Boolean) -> Unit = {},
    val onUpdateTorrServerSaveToDb: (Boolean) -> Unit = {},
    val onUpdateTorrentIndexerConfig: (String, String) -> Unit = { _, _ -> },
    val onUpdateEnableDirectRutorSearch: (Boolean) -> Unit = {},
    val onUpdateEnableAnimeTrackerSearch: (Boolean) -> Unit = {},
    val onUpdateEnableTrackerDoh: (Boolean) -> Unit = {},
    val onUpdateIgnoreNomediaFolders: (Boolean) -> Unit,
    val onUpdateExcludeCameraAndScreenshots: (Boolean) -> Unit,
    val onUpdateMinVideoSizeMb: (Int) -> Unit
)

@Composable
fun SettingsTabContent(
    settings: PlayerSettings,
    hasStorageAccess: Boolean,
    hasAllFilesAccess: Boolean,
    libraryRoots: List<LibraryRootEntity>,
    hiddenFolders: List<HiddenFolderEntity>,
    recentCount: Int,
    actions: SettingsActions
) {
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var showQuickActionsCustomizer by rememberSaveable { mutableStateOf(false) }

    val activeCategory = SettingsCategory.entries.firstOrNull { it.id == selectedCategoryId }

    BackHandler(enabled = activeCategory != null) {
        selectedCategoryId = null
    }

    if (activeCategory == null) {
        SettingsCategoryHub(
            settings = settings,
            hasStorageAccess = hasStorageAccess,
            hasAllFilesAccess = hasAllFilesAccess,
            excludedCount = hiddenFolders.size,
            onSelectCategory = { cat -> selectedCategoryId = cat.id }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { selectedCategoryId = null },
                        modifier = Modifier.testTag("settings_category_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад к категориям настроек"
                        )
                    }
                    Icon(
                        imageVector = activeCategory.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = activeCategory.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("settings_category_detail_list"),
                contentPadding = PaddingValues(
                    horizontal = if (settings.compactUiMode) 12.dp else 16.dp,
                    vertical = if (settings.compactUiMode) 6.dp else 10.dp
                ),
                verticalArrangement = Arrangement.spacedBy(if (settings.compactUiMode) 10.dp else 14.dp)
            ) {
                item(key = activeCategory.id) {
                    when (activeCategory) {
                        SettingsCategory.APPEARANCE -> AppearanceSettingsPage(
                            settings = settings,
                            actions = actions,
                            onOpenQuickActionsCustomizer = { showQuickActionsCustomizer = true }
                        )

                        SettingsCategory.PLAYBACK -> PlaybackSettingsPage(
                            settings = settings,
                            actions = actions
                        )

                        SettingsCategory.GESTURES -> GesturesSettingsPage(
                            settings = settings,
                            actions = actions
                        )

                        SettingsCategory.VIDEO_ANIME4K -> VideoAndAnime4KSettingsPage(
                            settings = settings,
                            actions = actions
                        )

                        SettingsCategory.AUDIO -> AudioSettingsPage(
                            settings = settings,
                            actions = actions
                        )

                        SettingsCategory.SUBTITLES -> SubtitlesSettingsPage(
                            settings = settings,
                            actions = actions
                        )

                        SettingsCategory.TORRSERVER -> TorrServerSettingsPage(
                            settings = settings,
                            actions = actions
                        )

                        SettingsCategory.STORAGE -> StorageAndExclusionsSettingsPage(
                            settings = settings,
                            hasStorageAccess = hasStorageAccess,
                            hasAllFilesAccess = hasAllFilesAccess,
                            libraryRoots = libraryRoots,
                            hiddenFolders = hiddenFolders,
                            recentCount = recentCount,
                            actions = actions
                        )
                    }
                }

                item(key = "category_bottom_spacer") {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (showQuickActionsCustomizer) {
        QuickActionsCustomizerModalSheet(
            activeActions = settings.quickActions,
            onUpdateQuickActions = actions.onUpdateQuickActions,
            onDismiss = { showQuickActionsCustomizer = false }
        )
    }
}

@Composable
private fun SettingsCategoryHub(
    settings: PlayerSettings,
    hasStorageAccess: Boolean,
    hasAllFilesAccess: Boolean,
    excludedCount: Int,
    onSelectCategory: (SettingsCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .testTag("settings_lazy_list"),
        contentPadding = PaddingValues(
            horizontal = if (settings.compactUiMode) 12.dp else 16.dp,
            vertical = if (settings.compactUiMode) 8.dp else 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (settings.compactUiMode) 8.dp else 10.dp)
    ) {
        item(key = "hub_intro") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Категории настроек",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "negai v2.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        items(
            items = SettingsCategory.entries,
            key = { it.id }
        ) { category ->
            val summary = categorySummary(
                category = category,
                settings = settings,
                hasStorageAccess = hasStorageAccess,
                hasAllFilesAccess = hasAllFilesAccess,
                excludedCount = excludedCount
            )
            Card(
                shape = RoundedCornerShape(if (settings.compactUiMode) 15.dp else 18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = if (settings.compactUiMode) 64.dp else 72.dp)
                    .clip(RoundedCornerShape(if (settings.compactUiMode) 15.dp else 18.dp))
                    .clickable { onSelectCategory(category) }
                    .testTag("settings_category_${category.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (settings.compactUiMode) 12.dp else 16.dp,
                            vertical = if (settings.compactUiMode) 11.dp else 14.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = category.icon(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(key = "hub_bottom_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun SettingsCategory.icon(): ImageVector = when (this) {
    SettingsCategory.APPEARANCE -> Icons.Default.Palette
    SettingsCategory.PLAYBACK -> Icons.Default.PlayCircleOutline
    SettingsCategory.GESTURES -> Icons.Default.Swipe
    SettingsCategory.VIDEO_ANIME4K -> Icons.Default.AutoAwesome
    SettingsCategory.AUDIO -> Icons.Default.GraphicEq
    SettingsCategory.SUBTITLES -> Icons.Default.Subtitles
    SettingsCategory.TORRSERVER -> Icons.Default.Dns
    SettingsCategory.STORAGE -> Icons.Default.Storage
}

private fun categorySummary(
    category: SettingsCategory,
    settings: PlayerSettings,
    hasStorageAccess: Boolean,
    hasAllFilesAccess: Boolean,
    excludedCount: Int
): String = when (category) {
    SettingsCategory.APPEARANCE -> {
        val densityTxt = if (settings.compactUiMode) "Компактно" else "Просторно"
        "${settings.themeMode.title} • Плеер: ${settings.playerControlsTheme.shortBadge} • $densityTxt"
    }
    SettingsCategory.PLAYBACK -> {
        val resumeTxt = if (settings.resumePlayback) "Продолжение" else "С начала"
        val chapTxt = if (settings.enableEmbeddedChapters) "Главы вкл" else "Главы выкл"
        "$resumeTxt • $chapTxt • Шаг ${settings.seekStepSeconds} сек"
    }
    SettingsCategory.GESTURES -> {
        val boostTxt = if (settings.gestureLongPress2x) "Удержание ${settings.longPressBoostSpeed}x" else "Удержание выкл"
        "$boostTxt • Ориентация: ${settings.defaultPlayerOrientation.shortBadge}"
    }
    SettingsCategory.VIDEO_ANIME4K -> {
        val memc = settings.effectiveFrameInterpolationMode.badgeLabel
        val a4k = if (settings.anime4kEnabled) settings.anime4kPreset.badgeLabel else "Выкл"
        val hz = when {
            settings.ltpoAdaptiveRefreshEnabled && settings.highRefreshRateEnabled -> "LTPO Авто"
            settings.highRefreshRateEnabled -> "Max Hz"
            else -> "60Hz"
        }
        "${settings.decoderMode.shortBadge} • $memc • $hz • Anime4K: $a4k"
    }
    SettingsCategory.AUDIO -> {
        val autoAud = if (settings.autoBindExternalAudio) "Авто-привязка вкл" else "Авто-привязка выкл"
        "$autoAud • Усиление ${settings.defaultAudioBoostPercent}%"
    }
    SettingsCategory.SUBTITLES -> {
        val colorLabel = SubtitleColorPreset.fromArgb(settings.subtitleColorArgb).label
        "${settings.subtitleFontSizeSp.roundToInt()} sp • $colorLabel • SRT / ASS"
    }
    SettingsCategory.TORRSERVER -> {
        val indexerHost = settings.torrentIndexerUrl
            .substringBefore(',')
            .trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
        val dohLabel = if (settings.enableTrackerDoh) "DoH вкл" else "DoH выкл"
        "${settings.torrServerUrl.removePrefix("http://")} • $indexerHost • $dohLabel"
    }
    SettingsCategory.STORAGE -> {
        val accessLabel = when {
            hasAllFilesAccess -> "Полный доступ"
            hasStorageAccess -> "Доступ к медиа"
            else -> "Требуется доступ"
        }
        "$accessLabel • Исключений: $excludedCount"
    }
}
