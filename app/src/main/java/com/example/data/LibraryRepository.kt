package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.model.EpisodeItem
import com.example.model.PlayerSettings
import com.example.model.VideoFolderItem
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LibraryRepository private constructor(
    private val appContext: Context,
    private val dao: KinoDao,
    val preferencesRepository: PlayerPreferencesRepository,
    private val scanner: SafMediaScanner
) {
    private data class ScanSettingsConfig(
        val autoBindAudio: Boolean,
        val autoBindSubs: Boolean,
        val ignoreNomedia: Boolean,
        val excludeCamera: Boolean,
        val minVideoSizeMb: Int
    )

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scanMutex = Mutex()
    @Volatile
    private var initialAutoScanTriggered = false

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _hasStorageAccess = MutableStateFlow(StorageAccessHelper.hasStorageAccess(appContext))
    val hasStorageAccessFlow: StateFlow<Boolean> = _hasStorageAccess.asStateFlow()

    private val _hasAllFilesAccess = MutableStateFlow(StorageAccessHelper.hasAllFilesAccess(appContext))
    val hasAllFilesAccessFlow: StateFlow<Boolean> = _hasAllFilesAccess.asStateFlow()

    private val initialCachedFolders: List<VideoFolderItem> = FolderLibraryCache.read(appContext)
    private val _allScannedFolders = MutableStateFlow(initialCachedFolders)

    private val _lastPlayedFile = MutableStateFlow<PlaybackProgressEntity?>(null)
    val lastPlayedFileFlow: StateFlow<PlaybackProgressEntity?> = _lastPlayedFile.asStateFlow()

    val libraryRootsFlow: Flow<List<LibraryRootEntity>> = dao.observeLibraryRoots()
    val hiddenFoldersFlow: Flow<List<HiddenFolderEntity>> = dao.observeHiddenFolders()
    val recentProgressFlow: Flow<List<PlaybackProgressEntity>> = dao.observeAllProgress()
    val settingsFlow: Flow<PlayerSettings> = preferencesRepository.settingsFlow

    val allFoldersWithHiddenFlagFlow: StateFlow<List<VideoFolderItem>> = combine(
        _allScannedFolders,
        hiddenFoldersFlow,
        recentProgressFlow
    ) { folders, hiddenList, progressList ->
        mergeFoldersWithState(folders, hiddenList, progressList)
    }.stateIn(
        repoScope,
        SharingStarted.Eagerly,
        initialCachedFolders
    )

    val visibleFoldersFlow: StateFlow<List<VideoFolderItem>> = allFoldersWithHiddenFlagFlow
        .map { folders -> folders.filter { !it.isHidden } }
        .stateIn(
            repoScope,
            SharingStarted.Eagerly,
            initialCachedFolders.filter { !it.isHidden }
        )

    init {
        repoScope.launch {
            runCatching {
                val legacyDemoDir = File(appContext.filesDir, "demo_media_root")
                if (legacyDemoDir.exists()) {
                    legacyDemoDir.deleteRecursively()
                }
            }

            val scanSettingsFlow = settingsFlow
                .map { s ->
                    ScanSettingsConfig(
                        autoBindAudio = s.autoBindExternalAudio,
                        autoBindSubs = s.autoBindExternalSubtitles,
                        ignoreNomedia = s.ignoreNomediaFolders,
                        excludeCamera = s.excludeCameraAndScreenshots,
                        minVideoSizeMb = s.minVideoSizeMb
                    )
                }
                .distinctUntilChanged()

            combine(
                libraryRootsFlow,
                scanSettingsFlow
            ) { roots, cfg ->
                roots to cfg
            }.collect { (roots, _) ->
                initialAutoScanTriggered = true
                val currentSettings = settingsFlow.first()
                val progressList = recentProgressFlow.first()
                val showSpinner = _allScannedFolders.value.isEmpty()
                performScan(
                    roots = roots,
                    settings = currentSettings,
                    progressList = progressList,
                    showLoadingIndicator = showSpinner
                )
            }
        }

        repoScope.launch {
            combine(
                recentProgressFlow,
                preferencesRepository.lastPlayedVideoUriFlow,
                _allScannedFolders
            ) { progressList, storedLastUri, _ ->
                progressList to storedLastUri
            }.collect { (progressList, storedLastUri) ->
                evaluateAndCleanAvailability(progressList, storedLastUri)
            }
        }
    }

    fun onStoragePermissionUpdated() {
        val prevAccess = _hasStorageAccess.value
        val prevAllFiles = _hasAllFilesAccess.value
        val nowAllFiles = StorageAccessHelper.hasAllFilesAccess(appContext)
        val nowAccess = StorageAccessHelper.hasStorageAccess(appContext)
        _hasAllFilesAccess.value = nowAllFiles
        _hasStorageAccess.value = nowAccess

        val permissionNewlyGranted = (nowAccess && !prevAccess) || (nowAllFiles && !prevAllFiles)
        val needsFallbackScan = nowAccess &&
            _allScannedFolders.value.isEmpty() &&
            !_isScanning.value &&
            initialAutoScanTriggered &&
            !scanMutex.isLocked

        if (permissionNewlyGranted || needsFallbackScan) {
            repoScope.launch {
                rescan(showIndicator = _allScannedFolders.value.isEmpty())
            }
        }
    }

    private suspend fun evaluateAndCleanAvailability(
        progressList: List<PlaybackProgressEntity>,
        storedLastUri: String?
    ) {
        val candidate: PlaybackProgressEntity? = when {
            storedLastUri == null -> {
                progressList.firstOrNull { PlaybackResumeHelper.isLocalFileUri(it.videoUri) }
            }
            storedLastUri.isBlank() -> null
            else -> {
                progressList.firstOrNull { it.videoUri == storedLastUri } ?: run {
                    val targetDocKey = PlaybackResumeHelper.extractCanonicalDocumentKey(appContext, storedLastUri)
                    if (targetDocKey != null) {
                        progressList.firstOrNull {
                            PlaybackResumeHelper.extractCanonicalDocumentKey(appContext, it.videoUri) == targetDocKey
                        }
                    } else {
                        null
                    }
                }
            }
        }

        if (candidate == null) {
            _lastPlayedFile.value = null
        } else if (PlaybackResumeHelper.isMediaFileExists(appContext, candidate.videoUri)) {
            _lastPlayedFile.value = candidate
        } else {
            _lastPlayedFile.value = null
            preferencesRepository.clearLastPlayedVideoUri()
            dao.deleteProgress(candidate.videoUri)
        }

        progressList.take(30).forEach { entry ->
            if (entry.videoUri != candidate?.videoUri &&
                PlaybackResumeHelper.isLocalFileUri(entry.videoUri) &&
                !PlaybackResumeHelper.isMediaFileExists(appContext, entry.videoUri)
            ) {
                dao.deleteProgress(entry.videoUri)
            }
        }
    }

    suspend fun checkAndGetValidLastPlayed(): PlaybackProgressEntity? = withContext(Dispatchers.IO) {
        val progressList = dao.getAllProgressSnapshot()
        val storedLastUri = preferencesRepository.lastPlayedVideoUriFlow.first()
        evaluateAndCleanAvailability(progressList, storedLastUri)
        _lastPlayedFile.value
    }

    fun refreshMediaAvailabilityAsync() {
        repoScope.launch {
            checkAndGetValidLastPlayed()
        }
    }

    private fun mergeFoldersWithState(
        folders: List<VideoFolderItem>,
        hiddenList: List<HiddenFolderEntity>,
        progressList: List<PlaybackProgressEntity>
    ): List<VideoFolderItem> {
        val hiddenIds = hiddenList.map { it.folderId }.toSet()
        val progressMap = progressList.associateBy { it.videoUri }
        return folders.map { folder ->
            val updatedEpisodes = folder.episodes.map { ep ->
                val p = progressMap[ep.uri]
                if (p != null) {
                    ep.copy(
                        resumePositionMs = p.positionMs,
                        durationMs = p.durationMs.coerceAtLeast(ep.durationMs),
                        videoWidth = if (ep.videoWidth > 0) ep.videoWidth else p.videoWidth,
                        videoHeight = if (ep.videoHeight > 0) ep.videoHeight else p.videoHeight,
                        lastPlayedAt = p.updatedAt
                    )
                } else {
                    ep
                }
            }
            val watched = updatedEpisodes.count { ep ->
                ep.durationMs > 0L && ep.progressFraction >= 0.85f
            }
            val latestPlayed = updatedEpisodes.maxOfOrNull { ep ->
                progressMap[ep.uri]?.updatedAt ?: 0L
            } ?: folder.lastPlayedAt
            val excluded = StorageAccessHelper.isFolderExcluded(
                folderId = folder.id,
                localDirPath = StorageAccessHelper.normalizeLocalPathKey(folder.id),
                excludedIds = hiddenIds
            )
            folder.copy(
                episodes = updatedEpisodes,
                watchedCount = watched,
                lastPlayedAt = latestPlayed,
                isHidden = excluded
            )
        }
    }

    suspend fun rescan(showIndicator: Boolean = true) {
        val roots = libraryRootsFlow.first()
        val settings = settingsFlow.first()
        val progress = recentProgressFlow.first()
        performScan(roots, settings, progress, showLoadingIndicator = showIndicator)
    }

    private suspend fun performScan(
        roots: List<LibraryRootEntity>,
        settings: PlayerSettings,
        progressList: List<PlaybackProgressEntity>,
        showLoadingIndicator: Boolean
    ) {
        scanMutex.withLock {
            val nowAllFiles = StorageAccessHelper.hasAllFilesAccess(appContext)
            val nowMediaPerm = StorageAccessHelper.hasMediaReadPermission(appContext)
            _hasAllFilesAccess.value = nowAllFiles
            _hasStorageAccess.value = nowAllFiles || nowMediaPerm

            if (showLoadingIndicator) {
                _isScanning.value = true
            }
            try {
                val hiddenIds = hiddenFoldersFlow.first().map { it.folderId }.toSet()
                val progressMap = progressList.associateBy { it.videoUri }

                val scanned = scanner.scanAllSources(
                    roots = roots,
                    hasAllFilesAccess = nowAllFiles,
                    hasMediaReadPermission = nowMediaPerm,
                    autoBindAudio = settings.autoBindExternalAudio,
                    autoBindSubtitles = settings.autoBindExternalSubtitles,
                    ignoreNomediaFolders = settings.ignoreNomediaFolders,
                    excludeCameraAndScreenshots = settings.excludeCameraAndScreenshots,
                    minVideoSizeMb = settings.minVideoSizeMb,
                    progressMap = progressMap,
                    hiddenIds = hiddenIds
                )
                _allScannedFolders.value = scanned
                FolderLibraryCache.write(appContext, scanned)
            } finally {
                _isScanning.value = false
            }
        }
    }

    suspend fun addSafFolderTree(treeUri: Uri) {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val doc = DocumentFile.fromTreeUri(appContext, treeUri)
        val displayName = doc?.name?.takeIf { it.isNotBlank() }
            ?: treeUri.lastPathSegment?.substringAfterLast(':')
            ?: "Внешняя папка"
        dao.insertLibraryRoot(
            LibraryRootEntity(
                treeUri = treeUri.toString(),
                displayName = displayName
            )
        )
    }

    suspend fun removeSafFolderTree(treeUri: String) {
        runCatching {
            appContext.contentResolver.releasePersistableUriPermission(
                Uri.parse(treeUri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        dao.deleteLibraryRoot(treeUri)
        checkAndGetValidLastPlayed()
    }

    suspend fun hideFolder(folder: VideoFolderItem) {
        dao.hideFolder(
            HiddenFolderEntity(
                folderId = folder.id,
                folderName = folder.name,
                displayPath = folder.displayPath
            )
        )
    }

    suspend fun excludeFolderBySafUri(treeUri: Uri) {
        val localFile = StorageAccessHelper.resolveLocalFileFromTreeUri(appContext, treeUri)
        if (localFile != null) {
            val folderId = Uri.fromFile(localFile).toString()
            val name = localFile.name.ifBlank { "Папка" }
            dao.hideFolder(
                HiddenFolderEntity(
                    folderId = folderId,
                    folderName = name,
                    displayPath = localFile.absolutePath
                )
            )
        } else {
            val doc = DocumentFile.fromTreeUri(appContext, treeUri)
            val folderId = doc?.uri?.toString() ?: treeUri.toString()
            val name = doc?.name?.takeIf { it.isNotBlank() }
                ?: treeUri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')
                ?: "Папка"
            dao.hideFolder(
                HiddenFolderEntity(
                    folderId = folderId,
                    folderName = name,
                    displayPath = treeUri.lastPathSegment ?: name
                )
            )
        }
    }

    suspend fun unhideFolder(folderId: String) {
        dao.unhideFolder(folderId)
        rescan(showIndicator = false)
    }

    suspend fun clearPlaybackHistory() {
        dao.clearAllProgress()
        preferencesRepository.clearLastPlayedVideoUri()
        _lastPlayedFile.value = null
    }

    suspend fun savePlaybackProgress(
        episode: EpisodeItem,
        positionMs: Long,
        durationMs: Long,
        videoWidth: Int = 0,
        videoHeight: Int = 0,
        manualAudioUri: String? = null,
        manualAudioName: String? = null,
        manualSubtitleUri: String? = null,
        manualSubtitleName: String? = null
    ) {
        val existing = getSavedProgressForEpisode(episode)
        if (existing != null && existing.videoUri != episode.uri) {
            dao.deleteProgress(existing.videoUri)
        }
        val resolvedWidth = when {
            videoWidth > 0 -> videoWidth
            episode.videoWidth > 0 -> episode.videoWidth
            else -> existing?.videoWidth ?: 0
        }
        val resolvedHeight = when {
            videoHeight > 0 -> videoHeight
            episode.videoHeight > 0 -> episode.videoHeight
            else -> existing?.videoHeight ?: 0
        }
        val entity = PlaybackProgressEntity(
            videoUri = episode.uri,
            title = episode.cleanTitle,
            folderId = episode.folderId,
            folderName = episode.folderName,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(existing?.durationMs ?: 0L),
            videoWidth = resolvedWidth,
            videoHeight = resolvedHeight,
            updatedAt = System.currentTimeMillis(),
            manualAudioUri = manualAudioUri ?: existing?.manualAudioUri,
            manualAudioName = manualAudioName ?: existing?.manualAudioName,
            manualSubtitleUri = manualSubtitleUri ?: existing?.manualSubtitleUri,
            manualSubtitleName = manualSubtitleName ?: existing?.manualSubtitleName
        )
        dao.upsertProgress(entity)
        if (PlaybackResumeHelper.isLocalFileUri(episode.uri)) {
            preferencesRepository.setLastPlayedVideoUri(episode.uri)
            if (PlaybackResumeHelper.isMediaFileExists(appContext, episode.uri)) {
                _lastPlayedFile.value = entity
            }
        }
    }

    suspend fun getSavedProgress(videoUri: String): PlaybackProgressEntity? =
        dao.getProgressForVideo(videoUri)

    suspend fun getSavedProgressForEpisode(episode: EpisodeItem): PlaybackProgressEntity? =
        withContext(Dispatchers.IO) {
            dao.getProgressForVideo(episode.uri)?.let { return@withContext it }
            val targetDocKey = PlaybackResumeHelper.extractCanonicalDocumentKey(appContext, episode.uri)
                ?: return@withContext null
            dao.getAllProgressSnapshot().firstOrNull { entry ->
                PlaybackResumeHelper.extractCanonicalDocumentKey(appContext, entry.videoUri) == targetDocKey
            }
        }

    fun resolveStandaloneVideo(uri: Uri): EpisodeItem {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        return scanner.resolveStandaloneVideoEpisode(uri)
    }

    fun resolveStandaloneEpisodeFromProgress(progress: PlaybackProgressEntity): EpisodeItem {
        val parsed = MediaAutoMatcher.parseEpisodeInfo(progress.title)
        return EpisodeItem(
            uri = progress.videoUri,
            fileName = progress.title,
            cleanTitle = parsed.cleanTitle.ifBlank { progress.title },
            episodeNumber = parsed.episode,
            seasonNumber = parsed.season,
            folderId = progress.folderId,
            folderName = progress.folderName,
            videoWidth = progress.videoWidth,
            videoHeight = progress.videoHeight,
            resumePositionMs = progress.positionMs,
            durationMs = progress.durationMs,
            lastPlayedAt = progress.updatedAt
        )
    }

    fun resolveFileDisplayName(uri: Uri): String? = scanner.resolveDisplayName(uri)

    companion object {
        @Volatile
        private var instance: LibraryRepository? = null

        fun getInstance(context: Context): LibraryRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val app = context.applicationContext
                    val db = KinoDatabase.getInstance(app)
                    val prefs = PlayerPreferencesRepository(app)
                    val scanner = SafMediaScanner(app)
                    LibraryRepository(app, db.kinoDao(), prefs, scanner).also { instance = it }
                }
            }
        }
    }
}
