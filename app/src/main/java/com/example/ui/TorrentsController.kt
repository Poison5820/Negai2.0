package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.PlayerPreferencesRepository
import com.example.model.EpisodeItem
import com.example.model.PlayerSettings
import com.example.torrent.BencodeTorrentParser
import com.example.torrent.TorrServerClient
import com.example.torrent.TorrServerConnectionState
import com.example.torrent.TorrServerTorrent
import com.example.torrent.TorrentInspectorState
import com.example.torrent.TorrentQualityFilter
import com.example.torrent.TorrentSearchResult
import com.example.torrent.TorrentSearchService
import com.example.torrent.TorrentSortMode
import com.example.torrent.TorrentTrackerGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TorrentsUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val rawResults: List<TorrentSearchResult> = emptyList(),
    val filteredResults: List<TorrentSearchResult> = emptyList(),
    val trackerCounts: Map<TorrentTrackerGroup, Int> = emptyMap(),
    val trackerFilter: TorrentTrackerGroup = TorrentTrackerGroup.ALL,
    val qualityFilter: TorrentQualityFilter = TorrentQualityFilter.ALL,
    val sortMode: TorrentSortMode = TorrentSortMode.PRIORITY,
    val activeSources: List<String> = emptyList(),
    val searchMessage: String? = null,
    val recentQueries: List<String> = emptyList(),
    val connectionState: TorrServerConnectionState = TorrServerConnectionState.Idle,
    val inspectorState: TorrentInspectorState = TorrentInspectorState.Hidden,
    val savedServerTorrents: List<TorrServerTorrent> = emptyList(),
    val isLoadingSavedTorrents: Boolean = false
)

class TorrentsController(
    private val application: Application,
    private val scope: CoroutineScope,
    private val settingsFlow: StateFlow<PlayerSettings>,
    private val preferencesRepository: PlayerPreferencesRepository,
    private val onStartPlayerPlaylist: (List<EpisodeItem>, Int) -> Unit
) {

    private val _state = MutableStateFlow(TorrentsUiState())
    val state: StateFlow<TorrentsUiState> = _state.asStateFlow()

    private var activeSearchJob: Job? = null
    private var activeResolveJob: Job? = null

    fun checkTorrServerConnection() {
        scope.launch {
            val s = settingsFlow.value
            val normalizedUrl = TorrServerClient.normalizeBaseUrl(s.torrServerUrl)
            _state.update { it.copy(connectionState = TorrServerConnectionState.Checking) }
            val result = TorrServerClient.ping(
                rawBaseUrl = normalizedUrl,
                username = s.torrServerUsername,
                password = s.torrServerPassword
            )
            _state.update { current ->
                current.copy(
                    connectionState = result.fold(
                        onSuccess = { ver ->
                            TorrServerConnectionState.Connected(version = ver, baseUrl = normalizedUrl)
                        },
                        onFailure = { err ->
                            TorrServerConnectionState.Disconnected(
                                reason = err.message ?: "Нет ответа",
                                baseUrl = normalizedUrl
                            )
                        }
                    )
                )
            }
        }
    }

    fun updateQuery(newQuery: String) {
        _state.update { it.copy(query = newQuery) }
    }

    fun submitSearch(overrideQuery: String? = null) {
        val targetQuery = (overrideQuery ?: _state.value.query).trim()
        if (targetQuery.isEmpty()) return

        activeSearchJob?.cancel()
        activeSearchJob = scope.launch {
            val s = preferencesRepository.settingsFlow.first()
            _state.update { current ->
                val updatedRecent = (listOf(targetQuery) + current.recentQueries.filterNot {
                    it.equals(targetQuery, ignoreCase = true)
                }).take(8)
                current.copy(
                    query = targetQuery,
                    isSearching = true,
                    hasSearched = true,
                    searchMessage = null,
                    recentQueries = updatedRecent
                )
            }

            val responseResult = TorrentSearchService.searchAll(
                rawQuery = targetQuery,
                indexerUrl = s.torrentIndexerUrl,
                indexerApiKey = s.torrentIndexerApiKey,
                enableDirectRutor = s.enableDirectRutorSearch,
                enableAnimeNyaa = s.enableAnimeTrackerSearch,
                enableDoh = s.enableTrackerDoh
            )

            responseResult.fold(
                onSuccess = { response ->
                    val counts = computeTrackerCounts(response.results)
                    _state.update { current ->
                        val activeTracker = if (
                            current.trackerFilter != TorrentTrackerGroup.ALL &&
                            (counts[current.trackerFilter] ?: 0) == 0
                        ) {
                            TorrentTrackerGroup.ALL
                        } else {
                            current.trackerFilter
                        }
                        val sorted = TorrentSearchService.sortAndFilter(
                            results = response.results,
                            trackerFilter = activeTracker,
                            qualityFilter = current.qualityFilter,
                            sortMode = current.sortMode
                        )
                        current.copy(
                            isSearching = false,
                            rawResults = response.results,
                            filteredResults = sorted,
                            trackerCounts = counts,
                            trackerFilter = activeTracker,
                            activeSources = response.activeSources,
                            searchMessage = response.warningMessage
                        )
                    }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            isSearching = false,
                            rawResults = emptyList(),
                            filteredResults = emptyList(),
                            trackerCounts = emptyMap(),
                            searchMessage = "Ошибка поиска: ${err.localizedMessage ?: "проверьте подключение к сети"}"
                        )
                    }
                }
            )
        }
    }

    fun setTrackerFilter(group: TorrentTrackerGroup) {
        _state.update { current ->
            val sorted = TorrentSearchService.sortAndFilter(
                results = current.rawResults,
                trackerFilter = group,
                qualityFilter = current.qualityFilter,
                sortMode = current.sortMode
            )
            current.copy(trackerFilter = group, filteredResults = sorted)
        }
    }

    fun setQualityFilter(filter: TorrentQualityFilter) {
        _state.update { current ->
            val sorted = TorrentSearchService.sortAndFilter(
                results = current.rawResults,
                trackerFilter = current.trackerFilter,
                qualityFilter = filter,
                sortMode = current.sortMode
            )
            current.copy(qualityFilter = filter, filteredResults = sorted)
        }
    }

    fun setSortMode(mode: TorrentSortMode) {
        _state.update { current ->
            val sorted = TorrentSearchService.sortAndFilter(
                results = current.rawResults,
                trackerFilter = current.trackerFilter,
                qualityFilter = current.qualityFilter,
                sortMode = mode
            )
            current.copy(sortMode = mode, filteredResults = sorted)
        }
    }

    fun openSearchResultForPlayback(result: TorrentSearchResult) {
        resolveAndOpenTorrent(
            title = result.title,
            magnetUri = result.magnetUri,
            torrentFileUrl = result.torrentFileUrl,
            localBytes = null,
            localFileName = "release.torrent"
        )
    }

    fun openRawLinkForPlayback(rawLinkOrMagnet: String, customTitle: String = "") {
        val cleaned = rawLinkOrMagnet.trim()
        if (cleaned.isEmpty()) return
        val normalized = BencodeTorrentParser.ensurePlayableOrMagnetLink(cleaned, customTitle)
        val isMagnet = normalized.startsWith("magnet:", ignoreCase = true)
        val title = customTitle.trim().ifBlank {
            if (isMagnet) "Magnet-раздача" else cleaned.substringAfterLast('/').substringBefore('?').ifBlank { "Торрент" }
        }
        resolveAndOpenTorrent(
            title = title,
            magnetUri = if (isMagnet) normalized else null,
            torrentFileUrl = if (!isMagnet) normalized else null,
            localBytes = null,
            localFileName = "release.torrent"
        )
    }

    fun openLocalTorrentUriForPlayback(uri: Uri) {
        activeResolveJob?.cancel()
        activeResolveJob = scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                readLocalTorrentBytesAndName(uri)
            }
            if (loaded == null) {
                _state.update {
                    it.copy(
                        inspectorState = TorrentInspectorState.Error(
                            title = "Локальный .torrent",
                            message = "Не удалось прочитать выбранный .torrent файл.",
                            magnetOrLink = null,
                            torrentFileUrl = null
                        )
                    )
                }
                return@launch
            }
            val (bytes, fileName) = loaded
            val parsed = BencodeTorrentParser.parseTorrentBytes(bytes, fileName.removeSuffix(".torrent"))
            val title = parsed?.displayName ?: fileName.removeSuffix(".torrent")
            resolveAndOpenTorrent(
                title = title,
                magnetUri = parsed?.magnetUri,
                torrentFileUrl = null,
                localBytes = bytes,
                localFileName = fileName
            )
        }
    }

    suspend fun extractMagnetFromLocalTorrent(uri: Uri): Pair<String?, String> =
        withContext(Dispatchers.IO) {
            val loaded = readLocalTorrentBytesAndName(uri) ?: return@withContext (null to "Торрент")
            val (bytes, fileName) = loaded
            val parsed = BencodeTorrentParser.parseTorrentBytes(bytes, fileName.removeSuffix(".torrent"))
            (parsed?.magnetUri to (parsed?.displayName ?: fileName))
        }

    private fun resolveAndOpenTorrent(
        title: String,
        magnetUri: String?,
        torrentFileUrl: String?,
        localBytes: ByteArray?,
        localFileName: String
    ) {
        activeResolveJob?.cancel()
        activeResolveJob = scope.launch {
            val s = preferencesRepository.settingsFlow.first()
            _state.update {
                it.copy(
                    inspectorState = TorrentInspectorState.Loading(
                        title = title,
                        statusText = "Подключение к TorrServer…",
                        magnetOrLink = magnetUri,
                        torrentFileUrl = torrentFileUrl,
                        canForceDirectStream = !magnetUri.isNullOrBlank() || !torrentFileUrl.isNullOrBlank()
                    )
                )
            }

            val result = TorrServerClient.resolveTorrentWithFiles(
                rawBaseUrl = s.torrServerUrl,
                username = s.torrServerUsername,
                password = s.torrServerPassword,
                magnetUri = magnetUri,
                torrentFileUrl = torrentFileUrl,
                localTorrentBytes = localBytes,
                localFileName = localFileName,
                title = title,
                saveToDb = s.torrServerSaveToDb,
                onStatusUpdate = { statusMsg ->
                    _state.update { current ->
                        val curInspector = current.inspectorState
                        if (curInspector is TorrentInspectorState.Loading) {
                            current.copy(inspectorState = curInspector.copy(statusText = statusMsg))
                        } else {
                            current
                        }
                    }
                }
            )

            result.fold(
                onSuccess = { torrent ->
                    _state.update {
                        it.copy(
                            connectionState = TorrServerConnectionState.Connected(
                                version = "Онлайн",
                                baseUrl = TorrServerClient.normalizeBaseUrl(s.torrServerUrl)
                            )
                        )
                    }

                    if (torrent.files.isEmpty()) {
                        // Metadata still warming up after timeout: offer direct stream or retry
                        val linkForDirect = torrent.hash.ifBlank { magnetUri ?: torrentFileUrl.orEmpty() }
                        if (linkForDirect.isNotBlank() && s.torrServerAutoPlaySingle) {
                            forceStartDirectStream(linkForDirect, title)
                        } else {
                            _state.update {
                                it.copy(
                                    inspectorState = TorrentInspectorState.Error(
                                        title = title,
                                        message = "TorrServer ещё ищет пиров для загрузки списка файлов. Вы можете повторить попытку или открыть торрент во внешнем приложении.",
                                        magnetOrLink = magnetUri ?: torrent.hash.takeIf { h -> h.isNotBlank() }?.let { h ->
                                            BencodeTorrentParser.buildMagnetUri(h, title)
                                        },
                                        torrentFileUrl = torrentFileUrl
                                    )
                                )
                            }
                        }
                        return@fold
                    }

                    val bundle = TorrServerClient.buildPlaylistFromTorrent(
                        rawBaseUrl = s.torrServerUrl,
                        torrent = torrent,
                        selectedVoiceover = null
                    )

                    if (bundle.episodes.isEmpty()) {
                        _state.update {
                            it.copy(
                                inspectorState = TorrentInspectorState.Error(
                                    title = title,
                                    message = "Внутри торрента не найдено видеофайлов для воспроизведения.",
                                    magnetOrLink = magnetUri,
                                    torrentFileUrl = torrentFileUrl
                                )
                            )
                        }
                        return@fold
                    }

                    // Auto-play immediately if single video file and no separate external voiceover choice required
                    if (s.torrServerAutoPlaySingle &&
                        bundle.episodes.size == 1 &&
                        bundle.availableVoiceovers.isEmpty()
                    ) {
                        _state.update { it.copy(inspectorState = TorrentInspectorState.Hidden) }
                        onStartPlayerPlaylist(bundle.episodes, 0)
                    } else {
                        _state.update {
                            it.copy(
                                inspectorState = TorrentInspectorState.Ready(
                                    torrent = torrent,
                                    bundle = bundle,
                                    magnetOrLink = magnetUri ?: BencodeTorrentParser.buildMagnetUri(torrent.hash, title),
                                    torrentFileUrl = torrentFileUrl
                                )
                            )
                        }
                    }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            connectionState = TorrServerConnectionState.Disconnected(
                                reason = err.message ?: "Нет связи",
                                baseUrl = TorrServerClient.normalizeBaseUrl(s.torrServerUrl)
                            ),
                            inspectorState = TorrentInspectorState.Error(
                                title = title,
                                message = err.message ?: "Не удалось подключиться к TorrServer",
                                magnetOrLink = magnetUri,
                                torrentFileUrl = torrentFileUrl
                            )
                        )
                    }
                }
            )
        }
    }

    fun selectInspectorVoiceover(voiceover: String?) {
        val currentInspector = _state.value.inspectorState as? TorrentInspectorState.Ready ?: return
        val baseUrl = settingsFlow.value.torrServerUrl
        val updatedBundle = TorrServerClient.buildPlaylistFromTorrent(
            rawBaseUrl = baseUrl,
            torrent = currentInspector.torrent,
            selectedVoiceover = voiceover
        )
        _state.update {
            it.copy(
                inspectorState = currentInspector.copy(bundle = updatedBundle)
            )
        }
    }

    fun playInspectorEpisode(episodeIndex: Int) {
        val ready = _state.value.inspectorState as? TorrentInspectorState.Ready ?: return
        if (ready.bundle.episodes.isEmpty()) return
        val safeIdx = episodeIndex.coerceIn(ready.bundle.episodes.indices)
        _state.update { it.copy(inspectorState = TorrentInspectorState.Hidden) }
        onStartPlayerPlaylist(ready.bundle.episodes, safeIdx)
    }

    fun forceStartDirectStream(linkOrMagnet: String, title: String) {
        activeResolveJob?.cancel()
        val s = settingsFlow.value
        val directUrl = TorrServerClient.buildDirectFallbackStreamUrl(
            rawBaseUrl = s.torrServerUrl,
            linkOrHash = linkOrMagnet,
            title = title
        )
        val ep = EpisodeItem(
            uri = directUrl,
            fileName = title.ifBlank { "TorrServer Stream" },
            cleanTitle = title.ifBlank { "TorrServer Stream" },
            episodeNumber = null,
            seasonNumber = null,
            folderId = "torrserver_direct",
            folderName = "TorrServer"
        )
        _state.update { it.copy(inspectorState = TorrentInspectorState.Hidden) }
        onStartPlayerPlaylist(listOf(ep), 0)
    }

    fun dismissInspector() {
        activeResolveJob?.cancel()
        _state.update { it.copy(inspectorState = TorrentInspectorState.Hidden) }
    }

    fun loadSavedServerTorrents() {
        scope.launch {
            val s = preferencesRepository.settingsFlow.first()
            _state.update { it.copy(isLoadingSavedTorrents = true) }
            val res = TorrServerClient.listTorrents(
                rawBaseUrl = s.torrServerUrl,
                username = s.torrServerUsername,
                password = s.torrServerPassword
            )
            _state.update { current ->
                res.fold(
                    onSuccess = { list ->
                        current.copy(
                            isLoadingSavedTorrents = false,
                            savedServerTorrents = list,
                            connectionState = TorrServerConnectionState.Connected(
                                version = "Онлайн",
                                baseUrl = TorrServerClient.normalizeBaseUrl(s.torrServerUrl)
                            )
                        )
                    },
                    onFailure = { err ->
                        current.copy(
                            isLoadingSavedTorrents = false,
                            connectionState = TorrServerConnectionState.Disconnected(
                                reason = err.message ?: "Нет связи",
                                baseUrl = TorrServerClient.normalizeBaseUrl(s.torrServerUrl)
                            )
                        )
                    }
                )
            }
        }
    }

    fun openSavedServerTorrent(torrent: TorrServerTorrent) {
        scope.launch {
            val s = preferencesRepository.settingsFlow.first()
            val hydrated = if (torrent.files.isNotEmpty()) {
                torrent
            } else {
                TorrServerClient.getTorrent(
                    rawBaseUrl = s.torrServerUrl,
                    username = s.torrServerUsername,
                    password = s.torrServerPassword,
                    hash = torrent.hash,
                    fallbackTitle = torrent.title
                ).getOrDefault(torrent)
            }

            if (hydrated.files.isEmpty()) {
                openRawLinkForPlayback(hydrated.hash, hydrated.title)
                return@launch
            }

            val bundle = TorrServerClient.buildPlaylistFromTorrent(
                rawBaseUrl = s.torrServerUrl,
                torrent = hydrated,
                selectedVoiceover = null
            )
            if (bundle.episodes.isEmpty()) return@launch

            if (s.torrServerAutoPlaySingle &&
                bundle.episodes.size == 1 &&
                bundle.availableVoiceovers.isEmpty()
            ) {
                onStartPlayerPlaylist(bundle.episodes, 0)
            } else {
                _state.update {
                    it.copy(
                        inspectorState = TorrentInspectorState.Ready(
                            torrent = hydrated,
                            bundle = bundle,
                            magnetOrLink = BencodeTorrentParser.buildMagnetUri(hydrated.hash, hydrated.title),
                            torrentFileUrl = null
                        )
                    )
                }
            }
        }
    }

    fun removeSavedServerTorrent(hash: String) {
        scope.launch {
            val s = preferencesRepository.settingsFlow.first()
            TorrServerClient.removeTorrent(
                rawBaseUrl = s.torrServerUrl,
                username = s.torrServerUsername,
                password = s.torrServerPassword,
                hash = hash
            )
            loadSavedServerTorrents()
        }
    }

    fun dropSavedServerTorrentCache(hash: String) {
        scope.launch {
            val s = preferencesRepository.settingsFlow.first()
            TorrServerClient.dropTorrentCache(
                rawBaseUrl = s.torrServerUrl,
                username = s.torrServerUsername,
                password = s.torrServerPassword,
                hash = hash
            )
        }
    }

    private fun computeTrackerCounts(items: List<TorrentSearchResult>): Map<TorrentTrackerGroup, Int> {
        val counts = mutableMapOf<TorrentTrackerGroup, Int>()
        counts[TorrentTrackerGroup.ALL] = items.size
        for (item in items) {
            counts[item.trackerGroup] = (counts[item.trackerGroup] ?: 0) + 1
        }
        return counts
    }

    private fun readLocalTorrentBytesAndName(uri: Uri): Pair<ByteArray, String>? {
        return runCatching {
            val cr = application.contentResolver
            var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "release.torrent"
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        val name = cursor.getString(nameIdx)
                        if (!name.isNullOrBlank()) displayName = name
                    }
                }
            }
            val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
            if (bytes.isEmpty()) return@runCatching null
            bytes to displayName
        }.getOrNull()
    }
}
