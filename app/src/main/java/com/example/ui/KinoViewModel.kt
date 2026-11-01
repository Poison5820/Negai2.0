package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FolderSorter
import com.example.data.HiddenFolderEntity
import com.example.data.LibraryRepository
import com.example.data.LibraryRootEntity
import com.example.data.PlaybackProgressEntity
import com.example.data.PlaybackResumeHelper
import com.example.model.Anime4KPreset
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.DecoderMode
import com.example.model.EpisodeItem
import com.example.model.EpisodeSortOption
import com.example.model.FolderSortOption
import com.example.model.FrameInterpolationMode
import com.example.model.PlaybackEndMode
import com.example.model.PlayerControlsTheme
import com.example.model.PlayerOrientationMode
import com.example.model.PlayerQuickAction
import com.example.model.PlayerSettings
import com.example.model.VideoChapter
import com.example.model.VideoFolderItem
import com.example.model.VideoScaleMode
import com.example.player.AudioTrackUiOption
import com.example.player.PlaybackRuntimeState
import com.example.player.PlayerEngine
import com.example.player.SubtitleTrackUiOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeTab {
    FOLDERS,
    RECENT,
    TORRENTS,
    SETTINGS
}

class KinoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LibraryRepository.getInstance(application)
    private val bootSettings: PlayerSettings = repository.preferencesRepository.readBootSettingsSnapshot()
    val playerEngine = PlayerEngine.getInstance(application)

    private val _currentTab = MutableStateFlow(HomeTab.FOLDERS)
    val currentTab: StateFlow<HomeTab> = _currentTab.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val _isPlayerOpen = MutableStateFlow(false)
    val isPlayerOpen: StateFlow<Boolean> = _isPlayerOpen.asStateFlow()

    private val _isPlayerUiActive = MutableStateFlow(true)
    val isPlayerUiActive: StateFlow<Boolean> = _isPlayerUiActive.asStateFlow()

    fun setPlayerUiActive(active: Boolean) {
        if (_isPlayerUiActive.value != active) {
            _isPlayerUiActive.value = active
        }
    }

    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _videoScaleMode = MutableStateFlow(bootSettings.defaultScaleMode)
    val videoScaleMode: StateFlow<VideoScaleMode> = _videoScaleMode.asStateFlow()

    private val _anime4kSplitCompare = MutableStateFlow(false)
    val anime4kSplitCompare: StateFlow<Boolean> = _anime4kSplitCompare.asStateFlow()

    private val _anime4kSplitPos = MutableStateFlow(0.5f)
    val anime4kSplitPos: StateFlow<Float> = _anime4kSplitPos.asStateFlow()

    val isScanning: StateFlow<Boolean> = repository.isScanning
    val hasStorageAccess: StateFlow<Boolean> = repository.hasStorageAccessFlow
    val hasAllFilesAccess: StateFlow<Boolean> = repository.hasAllFilesAccessFlow
    val playbackState: StateFlow<PlaybackRuntimeState> = playerEngine.state
    val playerPositionMs: StateFlow<Long> = playerEngine.positionMs

    val playerSettings: StateFlow<PlayerSettings> = repository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        bootSettings
    )

    val torrentsController = TorrentsController(
        application = application,
        scope = viewModelScope,
        settingsFlow = playerSettings,
        preferencesRepository = repository.preferencesRepository,
        onStartPlayerPlaylist = { episodes, startIndex ->
            playEpisodeList(episodes, startIndex)
        }
    )

    val torrentsState: StateFlow<TorrentsUiState> = torrentsController.state

    val hiddenFolders: StateFlow<List<HiddenFolderEntity>> = repository.hiddenFoldersFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val libraryRoots: StateFlow<List<LibraryRootEntity>> = repository.libraryRootsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val recentProgress: StateFlow<List<PlaybackProgressEntity>> = repository.recentProgressFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val lastPlayedFile: StateFlow<PlaybackProgressEntity?> = repository.lastPlayedFileFlow

    val filteredVisibleFolders: StateFlow<List<VideoFolderItem>> = combine(
        repository.visibleFoldersFlow,
        _searchQuery,
        playerSettings
    ) { folders, query, settings ->
        val trimmed = query.trim()
        val matching = if (trimmed.isEmpty()) {
            folders
        } else {
            folders.filter { folder ->
                folder.name.contains(trimmed, ignoreCase = true) ||
                    folder.episodes.any { ep -> ep.cleanTitle.contains(trimmed, ignoreCase = true) }
            }
        }
        FolderSorter.sortFolders(
            folders = matching,
            option = settings.folderSortOption,
            ascending = settings.folderSortAscending
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        FolderSorter.sortFolders(
            folders = repository.visibleFoldersFlow.value,
            option = bootSettings.folderSortOption,
            ascending = bootSettings.folderSortAscending
        )
    )

    val selectedFolder: StateFlow<VideoFolderItem?> = combine(
        repository.allFoldersWithHiddenFlagFlow,
        _selectedFolderId,
        playerSettings
    ) { allFolders, id, settings ->
        if (id == null) {
            null
        } else {
            allFolders.firstOrNull { it.id == id }?.let { folder ->
                folder.copy(
                    episodes = FolderSorter.sortEpisodes(
                        episodes = folder.episodes,
                        option = settings.episodeSortOption,
                        ascending = settings.episodeSortAscending
                    )
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun selectTab(tab: HomeTab) {
        _currentTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openFolder(folderId: String) {
        _selectedFolderId.value = folderId
    }

    fun closeFolderDetail() {
        _selectedFolderId.value = null
    }

    fun onPipModeChanged(inPip: Boolean) {
        _isInPipMode.value = inPip
    }

    fun onAppResumed() {
        repository.onStoragePermissionUpdated()
        repository.refreshMediaAvailabilityAsync()
    }

    fun onStoragePermissionResult() {
        repository.onStoragePermissionUpdated()
    }

    fun addFolderTree(uri: Uri) {
        viewModelScope.launch {
            repository.addSafFolderTree(uri)
        }
    }

    fun removeFolderTree(treeUri: String) {
        viewModelScope.launch {
            repository.removeSafFolderTree(treeUri)
        }
    }

    fun excludeFolderByUri(uri: Uri) {
        viewModelScope.launch {
            repository.excludeFolderBySafUri(uri)
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            repository.rescan(showIndicator = true)
        }
    }

    fun hideFolder(folder: VideoFolderItem) {
        viewModelScope.launch {
            repository.hideFolder(folder)
            if (_selectedFolderId.value == folder.id) {
                _selectedFolderId.value = null
            }
        }
    }

    fun unhideFolder(folderId: String) {
        viewModelScope.launch {
            repository.unhideFolder(folderId)
        }
    }

    fun deleteFolders(
        folders: List<VideoFolderItem>,
        onComplete: ((Int) -> Unit)? = null
    ) {
        if (folders.isEmpty()) return
        viewModelScope.launch {
            val deletedIds = folders.map { it.id }.toSet()
            if (_selectedFolderId.value in deletedIds) {
                _selectedFolderId.value = null
            }
            val count = repository.deleteFolders(folders)
            onComplete?.invoke(count)
        }
    }

    fun deleteEpisodes(
        folderId: String,
        episodes: List<EpisodeItem>,
        onComplete: ((Int) -> Unit)? = null
    ) {
        if (episodes.isEmpty()) return
        viewModelScope.launch {
            val count = repository.deleteEpisodes(folderId, episodes)
            if (repository.allFoldersWithHiddenFlagFlow.value.none { it.id == folderId }) {
                if (_selectedFolderId.value == folderId) {
                    _selectedFolderId.value = null
                }
            }
            onComplete?.invoke(count)
        }
    }

    fun renameFolder(
        folder: VideoFolderItem,
        newName: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val newFolderId = repository.renameFolder(folder, trimmed)
            if (newFolderId != null && _selectedFolderId.value == folder.id) {
                _selectedFolderId.value = newFolderId
            }
            onComplete?.invoke(newFolderId != null)
        }
    }

    fun renameEpisode(
        folderId: String,
        episode: EpisodeItem,
        newName: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val updated = repository.renameEpisode(folderId, episode, trimmed)
            onComplete?.invoke(updated != null)
        }
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch {
            repository.clearPlaybackHistory()
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            repository.preferencesRepository.resetAllSettingsToDefaults()
            playerEngine.setPlaybackEndMode(PlaybackEndMode.AUTO_NEXT)
            playerEngine.switchDecoderMode(DecoderMode.HW_PLUS)
            _videoScaleMode.value = VideoScaleMode.FIT
        }
    }

    private fun preparePlayerLaunchState() {
        _videoScaleMode.value = playerSettings.value.defaultScaleMode
        _isPlayerUiActive.value = true
        _isPlayerOpen.value = true
    }

    fun playEpisodeList(episodes: List<EpisodeItem>, startIndex: Int) {
        if (episodes.isEmpty()) return
        preparePlayerLaunchState()
        playerEngine.startPlaylist(
            episodes = episodes,
            startIndex = startIndex.coerceIn(episodes.indices),
            autoResume = true
        )
    }

    fun playFolderPlaylist(folder: VideoFolderItem, episodeIndex: Int) {
        playEpisodeList(folder.episodes, episodeIndex)
    }

    fun handleIncomingViewIntent(uri: Uri, mimeType: String? = null) {
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme == "magnet") {
            if (_isPlayerOpen.value) exitPlayer()
            _selectedFolderId.value = null
            _currentTab.value = HomeTab.TORRENTS
            torrentsController.openRawLinkForPlayback(uri.toString())
            return
        }
        if (mimeType.equals("application/x-bittorrent", ignoreCase = true) || isTorrentFileUri(uri)) {
            if (_isPlayerOpen.value) exitPlayer()
            _selectedFolderId.value = null
            _currentTab.value = HomeTab.TORRENTS
            torrentsController.openLocalTorrentUriForPlayback(uri)
            return
        }
        playStandaloneUri(uri)
    }

    fun playStandaloneUri(uri: Uri) {
        if (uri.scheme.equals("magnet", ignoreCase = true)) {
            handleIncomingViewIntent(uri, null)
            return
        }
        if (isTorrentFileUri(uri)) {
            handleIncomingViewIntent(uri, "application/x-bittorrent")
            return
        }
        val ep = repository.resolveStandaloneVideo(uri)
        preparePlayerLaunchState()
        playerEngine.startPlaylist(listOf(ep), 0, autoResume = true)
    }

    private fun isTorrentFileUri(uri: Uri): Boolean {
        val pathLower = uri.path?.lowercase().orEmpty()
        if (pathLower.endsWith(".torrent")) return true
        val app = getApplication<Application>()
        val resolvedType = runCatching { app.contentResolver.getType(uri) }.getOrNull()
        if (resolvedType.equals("application/x-bittorrent", ignoreCase = true)) return true
        val displayName = runCatching {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else {
                    null
                }
            }
        }.getOrNull()
        return displayName?.lowercase()?.endsWith(".torrent") == true
    }

    fun playStreamUrl(rawUrl: String, customTitle: String = "") {
        val url = rawUrl.trim()
        if (url.isEmpty()) return
        val title = customTitle.trim().ifEmpty {
            url.substringAfterLast('/').substringBefore('?').ifBlank { "Сетевой поток" }
        }
        val ep = EpisodeItem(
            uri = url,
            fileName = title,
            cleanTitle = title,
            episodeNumber = null,
            seasonNumber = null,
            folderId = "stream",
            folderName = "Сетевой поток"
        )
        preparePlayerLaunchState()
        playerEngine.startPlaylist(listOf(ep), 0, autoResume = true)
    }

    fun resumeRecentItem(progress: PlaybackProgressEntity) {
        viewModelScope.launch {
            if (PlaybackResumeHelper.isLocalFileUri(progress.videoUri) &&
                !PlaybackResumeHelper.isMediaFileExists(getApplication(), progress.videoUri)
            ) {
                repository.checkAndGetValidLastPlayed()
                return@launch
            }
            val allFolders = repository.allFoldersWithHiddenFlagFlow.value
            val matchingFolder = allFolders.firstOrNull { it.id == progress.folderId }
            if (matchingFolder != null) {
                val settings = playerSettings.value
                val sortedFolder = matchingFolder.copy(
                    episodes = FolderSorter.sortEpisodes(
                        episodes = matchingFolder.episodes,
                        option = settings.episodeSortOption,
                        ascending = settings.episodeSortAscending
                    )
                )
                val epIdx = sortedFolder.episodes.indexOfFirst { it.uri == progress.videoUri }
                if (epIdx >= 0) {
                    playFolderPlaylist(sortedFolder, epIdx)
                    return@launch
                }
            }
            val standalone = repository.resolveStandaloneEpisodeFromProgress(progress)
            preparePlayerLaunchState()
            playerEngine.startPlaylist(listOf(standalone), 0, autoResume = true)
        }
    }

    fun openLastPlayedFile(onMissingFile: (() -> Unit)? = null) {
        viewModelScope.launch {
            val validProgress = repository.checkAndGetValidLastPlayed()
            if (validProgress == null) {
                onMissingFile?.invoke()
                return@launch
            }
            resumeRecentItem(validProgress)
        }
    }

    fun dismissResumeBanner() {
        playerEngine.dismissResumeBanner()
    }

    fun restartEpisodeFromBeginning() {
        playerEngine.restartFromBeginning()
    }

    fun seekToChapter(chapter: VideoChapter) {
        playerEngine.seekToChapter(chapter)
    }

    fun seekToNextChapter() {
        playerEngine.seekToNextChapter()
    }

    fun seekToPreviousChapter() {
        playerEngine.seekToPreviousChapter()
    }

    fun exitPlayer() {
        playerEngine.pauseAndPersist()
        _isPlayerUiActive.value = true
        _isPlayerOpen.value = false
        repository.refreshMediaAvailabilityAsync()
    }

    // Settings Setters
    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.preferencesRepository.updateAppLanguage(language)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            repository.preferencesRepository.updateThemeMode(mode)
        }
    }

    fun setPlayerControlsTheme(theme: PlayerControlsTheme) {
        viewModelScope.launch {
            repository.preferencesRepository.updatePlayerControlsTheme(theme)
        }
    }

    fun setCompactUiMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateCompactUiMode(enabled)
        }
    }

    fun setShowEpisodeThumbnails(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateShowEpisodeThumbnails(enabled)
        }
    }

    fun setShowEpisodesButtonInHud(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateShowEpisodesButtonInHud(enabled)
        }
    }

    fun setShowHudBadges(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateShowHudBadges(enabled)
        }
    }

    fun selectFolderSortOption(option: FolderSortOption) {
        viewModelScope.launch {
            repository.preferencesRepository.selectFolderSortOption(option)
        }
    }

    fun setFolderSortDirection(ascending: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateFolderSortDirection(ascending)
        }
    }

    fun toggleFolderSortDirection() {
        viewModelScope.launch {
            repository.preferencesRepository.toggleFolderSortDirection()
        }
    }

    fun selectEpisodeSortOption(option: EpisodeSortOption) {
        viewModelScope.launch {
            repository.preferencesRepository.selectEpisodeSortOption(option)
        }
    }

    fun setEpisodeSortDirection(ascending: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateEpisodeSortDirection(ascending)
        }
    }

    fun toggleEpisodeSortDirection() {
        viewModelScope.launch {
            repository.preferencesRepository.toggleEpisodeSortDirection()
        }
    }

    fun setQuickActions(actions: List<PlayerQuickAction>) {
        viewModelScope.launch {
            repository.preferencesRepository.updateQuickActions(actions)
        }
    }

    fun setPlayerUiScalePercent(percent: Int) {
        viewModelScope.launch {
            repository.preferencesRepository.updatePlayerUiScalePercent(percent)
        }
    }

    fun setPlayerAutoDpiScale(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updatePlayerAutoDpiScale(enabled)
        }
    }

    fun setControlsAutoHideSeconds(seconds: Int) {
        viewModelScope.launch {
            repository.preferencesRepository.updateControlsAutoHideSeconds(seconds)
        }
    }

    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateResumePlayback(enabled)
        }
    }

    fun setPlaybackEndMode(mode: PlaybackEndMode) {
        playerEngine.setPlaybackEndMode(mode)
    }

    fun setEnableEmbeddedChapters(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateEnableEmbeddedChapters(enabled)
        }
    }

    fun setShowChapterMarkersOnTimeline(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateShowChapterMarkersOnTimeline(enabled)
        }
    }

    fun setSeekStepSeconds(seconds: Int) {
        viewModelScope.launch {
            repository.preferencesRepository.updateSeekStepSeconds(seconds)
        }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            repository.preferencesRepository.updateDefaultPlaybackSpeed(speed)
        }
    }

    fun setShowSkipIntroButton(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateShowSkipIntroButton(enabled)
        }
    }

    fun setSkipIntroSeconds(seconds: Int) {
        viewModelScope.launch {
            repository.preferencesRepository.updateSkipIntroSeconds(seconds)
        }
    }

    fun setAutoEnterPip(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateAutoEnterPip(enabled)
        }
    }

    fun setBackgroundAudioPlay(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateBackgroundAudioPlay(enabled)
        }
    }

    fun setGesturesBrightnessVolume(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateGesturesBrightnessVolume(enabled)
        }
    }

    fun setGestureHorizontalSeek(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateGestureHorizontalSeek(enabled)
        }
    }

    fun setGestureDoubleTapSeek(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateGestureDoubleTapSeek(enabled)
        }
    }

    fun setGestureLongPress2x(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateGestureLongPress2x(enabled)
        }
    }

    fun setLongPressBoostSpeed(speed: Float) {
        viewModelScope.launch {
            repository.preferencesRepository.updateLongPressBoostSpeed(speed)
        }
    }

    fun setDefaultPlayerOrientation(mode: PlayerOrientationMode) {
        viewModelScope.launch {
            repository.preferencesRepository.updateDefaultPlayerOrientation(mode)
        }
    }

    fun setDecoderMode(mode: DecoderMode) {
        playerEngine.switchDecoderMode(mode)
    }

    fun setDefaultScaleMode(mode: VideoScaleMode) {
        _videoScaleMode.value = mode
        viewModelScope.launch {
            repository.preferencesRepository.updateDefaultScaleMode(mode)
        }
    }

    fun setVideoScaleMode(mode: VideoScaleMode) {
        _videoScaleMode.value = mode
    }

    fun setHighRefreshRateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateHighRefreshRateEnabled(enabled)
        }
    }

    fun setLtpoAdaptiveRefreshEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateLtpoAdaptiveRefreshEnabled(enabled)
        }
    }

    fun setFrameInterpolationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateFrameInterpolationEnabled(enabled)
        }
    }

    fun setFrameInterpolationMode(mode: FrameInterpolationMode) {
        viewModelScope.launch {
            repository.preferencesRepository.updateFrameInterpolationMode(mode)
        }
    }

    fun setAnime4KEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateAnime4KEnabled(enabled)
        }
    }

    fun setAnime4KPreset(preset: Anime4KPreset) {
        viewModelScope.launch {
            repository.preferencesRepository.updateAnime4KPreset(preset)
        }
    }

    fun setAnime4KStrength(strength: Float) {
        viewModelScope.launch {
            repository.preferencesRepository.updateAnime4KStrength(strength)
        }
    }

    fun setAnime4KSplitCompare(enabled: Boolean) {
        _anime4kSplitCompare.value = enabled
    }

    fun setAnime4KSplitPos(pos: Float) {
        _anime4kSplitPos.value = pos.coerceIn(0.1f, 0.9f)
    }

    fun selectAudioTrack(option: AudioTrackUiOption) {
        playerEngine.selectAudioTrack(option)
    }

    fun attachExternalAudioFile(uri: Uri) {
        playerEngine.attachManualExternalAudio(uri)
    }

    fun setAutoBindAudio(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateAutoBindAudio(enabled)
        }
    }

    fun setPreferExternalAudioOnStart(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updatePreferExternalAudioOnStart(enabled)
        }
    }

    fun setDefaultAudioBoostPercent(percent: Int) {
        viewModelScope.launch {
            repository.preferencesRepository.updateDefaultAudioBoostPercent(percent)
        }
    }

    fun setBluetoothAudioAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateBluetoothAudioAutoSync(enabled)
        }
    }

    fun setBluetoothAudioOffsetMs(offsetMs: Long) {
        viewModelScope.launch {
            repository.preferencesRepository.updateBluetoothAudioOffsetMs(offsetMs)
        }
    }

    fun adjustAudioDelay(deltaMs: Long) {
        playerEngine.adjustAudioDelayMs(deltaMs)
    }

    fun setAudioDelay(delayMs: Long) {
        playerEngine.setAudioDelayMs(delayMs)
    }

    fun resyncAudioStream() {
        playerEngine.resyncAudioStream()
    }

    fun selectSubtitleTrack(option: SubtitleTrackUiOption) {
        playerEngine.selectSubtitleOption(option)
    }

    fun attachExternalSubtitleFile(uri: Uri) {
        playerEngine.attachManualExternalSubtitle(uri)
    }

    fun adjustSubtitleDelay(deltaMs: Long) {
        playerEngine.adjustSubtitleDelay(deltaMs)
    }

    fun setSubtitleDelay(delayMs: Long) {
        playerEngine.setSubtitleDelay(delayMs)
    }

    fun setAutoBindSubtitles(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateAutoBindSubtitles(enabled)
        }
    }

    fun setSubtitleFontSize(sizeSp: Float) {
        viewModelScope.launch {
            repository.preferencesRepository.updateSubtitleFontSize(sizeSp)
        }
    }

    fun setSubtitleColor(argb: Long) {
        viewModelScope.launch {
            repository.preferencesRepository.updateSubtitleColor(argb)
        }
    }

    fun setSubtitleBackground(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateSubtitleBackground(enabled)
        }
    }

    fun setIgnoreNomediaFolders(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateIgnoreNomediaFolders(enabled)
        }
    }

    fun setExcludeCameraAndScreenshots(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateExcludeCameraAndScreenshots(enabled)
        }
    }

    fun setMinVideoSizeMb(sizeMb: Int) {
        viewModelScope.launch {
            repository.preferencesRepository.updateMinVideoSizeMb(sizeMb)
        }
    }

    fun setSkipDeleteConfirmation(skip: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateSkipDeleteConfirmation(skip)
        }
    }

    fun setTorrServerUrl(url: String) {
        viewModelScope.launch {
            repository.preferencesRepository.updateTorrServerUrl(url)
            torrentsController.checkTorrServerConnection()
        }
    }

    fun setTorrServerAuth(username: String, password: String) {
        viewModelScope.launch {
            repository.preferencesRepository.updateTorrServerAuth(username, password)
        }
    }

    fun setTorrServerConfig(url: String, username: String, password: String) {
        viewModelScope.launch {
            repository.preferencesRepository.updateTorrServerUrl(url)
            repository.preferencesRepository.updateTorrServerAuth(username, password)
            torrentsController.checkTorrServerConnection()
        }
    }

    fun setTorrServerAutoPlaySingle(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateTorrServerAutoPlaySingle(enabled)
        }
    }

    fun setTorrServerSaveToDb(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateTorrServerSaveToDb(enabled)
        }
    }

    fun setTorrentIndexerConfig(url: String, apiKey: String) {
        viewModelScope.launch {
            repository.preferencesRepository.updateTorrentIndexerConfig(url, apiKey)
        }
    }

    fun setEnableDirectRutorSearch(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateEnableDirectRutorSearch(enabled)
        }
    }

    fun setEnableAnimeTrackerSearch(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateEnableAnimeTrackerSearch(enabled)
        }
    }

    fun setEnableTrackerDoh(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.updateEnableTrackerDoh(enabled)
        }
    }
}
