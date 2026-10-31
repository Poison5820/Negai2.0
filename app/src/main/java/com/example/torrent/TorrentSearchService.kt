package com.example.torrent

import android.util.Xml
import com.example.ui.components.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToLong

data class TorrentSearchResponse(
    val results: List<TorrentSearchResult>,
    val activeSources: List<String>,
    val warningMessage: String? = null
)

data class TrackerSourceDiagnostic(
    val sourceName: String,
    val isHealthy: Boolean,
    val activeMirror: String?,
    val latencyMs: Long,
    val detailMessage: String
)

internal sealed interface MirrorFetchOutcome<out T> {
    data class Success<T>(val data: T, val mirrorUrl: String, val latencyMs: Long) : MirrorFetchOutcome<T>
    data class AntiBotBlocked(val reason: AntiBotBlockReason, val mirrorUrl: String) : MirrorFetchOutcome<Nothing>
    data class NetworkFailure(val message: String, val mirrorUrl: String) : MirrorFetchOutcome<Nothing>
}

object TorrentSearchService {

    val DEFAULT_JACRED_MIRRORS = listOf(
        "https://jacred.xyz",
        "https://jacred.pro",
        "https://jac-red.ru",
        "https://jr.maxvol.pro",
        "https://jacred.viewbox.dev"
    )

    val RUTOR_MIRRORS = listOf(
        "https://rutor.info",
        "https://rutor.is",
        "https://6tor.org",
        "http://rutor.info",
        "http://rutor.is"
    )

    val NYAA_MIRRORS = listOf(
        "https://nyaa.si",
        "https://nyaa.iss.one",
        "https://nyaa.land"
    )

    val ANILIBRIA_MIRRORS = listOf(
        "https://anilibria.top",
        "https://api.anilibria.tv"
    )

    val APIBAY_MIRRORS = listOf(
        "https://apibay.org"
    )

    suspend fun searchAll(
        rawQuery: String,
        indexerUrl: String,
        indexerApiKey: String,
        enableDirectRutor: Boolean,
        enableAnimeNyaa: Boolean,
        enableDoh: Boolean = true
    ): Result<TorrentSearchResponse> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            return@withContext Result.success(TorrentSearchResponse(emptyList(), emptyList()))
        }

        val httpClient = ResilientTrackerNetwork.client(enableDoh)

        runCatching {
            coroutineScope {
                val indexerDeferred = async {
                    runCatching { searchConfiguredIndexer(query, indexerUrl, indexerApiKey, httpClient) }.getOrNull()
                }
                val rutorDeferred = async {
                    if (enableDirectRutor) {
                        runCatching { searchRutorDirect(query, httpClient) }.getOrNull()
                    } else {
                        null
                    }
                }
                val nyaaDeferred = async {
                    if (enableAnimeNyaa) {
                        runCatching { searchNyaaRss(query, httpClient) }.getOrNull()
                    } else {
                        null
                    }
                }
                val aniLibriaDeferred = async {
                    if (enableAnimeNyaa) {
                        runCatching { searchAniLibriaApi(query, httpClient) }.getOrNull()
                    } else {
                        null
                    }
                }
                val apiBayDeferred = async {
                    runCatching { searchApiBay(query, httpClient) }.getOrNull()
                }

                val collected = mutableListOf<TorrentSearchResult>()
                val activeSources = mutableListOf<String>()
                var anyEndpointReachable = false
                val blockReasons = mutableSetOf<String>()

                indexerDeferred.await()?.let { outcome ->
                    when (outcome) {
                        is MirrorFetchOutcome.Success -> {
                            anyEndpointReachable = true
                            val (label, items) = outcome.data
                            if (items.isNotEmpty()) {
                                activeSources.add(label)
                                collected.addAll(items)
                            }
                        }
                        is MirrorFetchOutcome.AntiBotBlocked -> blockReasons.add(outcome.reason.descriptionRu)
                        is MirrorFetchOutcome.NetworkFailure -> Unit
                    }
                }

                rutorDeferred.await()?.let { outcome ->
                    when (outcome) {
                        is MirrorFetchOutcome.Success -> {
                            anyEndpointReachable = true
                            if (outcome.data.isNotEmpty()) {
                                activeSources.add("Rutor Direct")
                                collected.addAll(outcome.data)
                            }
                        }
                        is MirrorFetchOutcome.AntiBotBlocked -> blockReasons.add(outcome.reason.descriptionRu)
                        is MirrorFetchOutcome.NetworkFailure -> Unit
                    }
                }

                nyaaDeferred.await()?.let { outcome ->
                    when (outcome) {
                        is MirrorFetchOutcome.Success -> {
                            anyEndpointReachable = true
                            if (outcome.data.isNotEmpty()) {
                                activeSources.add("Nyaa")
                                collected.addAll(outcome.data)
                            }
                        }
                        is MirrorFetchOutcome.AntiBotBlocked -> blockReasons.add(outcome.reason.descriptionRu)
                        is MirrorFetchOutcome.NetworkFailure -> Unit
                    }
                }

                aniLibriaDeferred.await()?.let { outcome ->
                    when (outcome) {
                        is MirrorFetchOutcome.Success -> {
                            anyEndpointReachable = true
                            if (outcome.data.isNotEmpty()) {
                                activeSources.add("AniLibria")
                                collected.addAll(outcome.data)
                            }
                        }
                        is MirrorFetchOutcome.AntiBotBlocked -> Unit
                        is MirrorFetchOutcome.NetworkFailure -> Unit
                    }
                }

                apiBayDeferred.await()?.let { outcome ->
                    when (outcome) {
                        is MirrorFetchOutcome.Success -> {
                            anyEndpointReachable = true
                            if (outcome.data.isNotEmpty()) {
                                activeSources.add("DHT")
                                collected.addAll(outcome.data)
                            }
                        }
                        is MirrorFetchOutcome.AntiBotBlocked -> Unit
                        is MirrorFetchOutcome.NetworkFailure -> Unit
                    }
                }

                val deduplicated = deduplicateResults(collected)
                val warning = when {
                    deduplicated.isNotEmpty() -> null
                    anyEndpointReachable ->
                        "По запросу «$query» раздач не найдено. Попробуйте упростить название или использовать оригинальное имя."
                    blockReasons.isNotEmpty() ->
                        "Трекеры включили антибот-защиту (${blockReasons.joinToString(", ")}). Включите обход DNS (DoH) или проверьте зеркала в настройках."
                    else ->
                        "Источники поиска временно недоступны в вашей сети. Проверьте подключение к интернету или зеркала индексатора в настройках."
                }

                TorrentSearchResponse(
                    results = sortAndFilter(
                        results = deduplicated,
                        trackerFilter = TorrentTrackerGroup.ALL,
                        qualityFilter = TorrentQualityFilter.ALL,
                        sortMode = TorrentSortMode.PRIORITY
                    ),
                    activeSources = activeSources,
                    warningMessage = warning
                )
            }
        }
    }

    suspend fun diagnoseSources(
        indexerUrl: String,
        indexerApiKey: String,
        enableDirectRutor: Boolean,
        enableAnimeNyaa: Boolean,
        enableDoh: Boolean = true
    ): List<TrackerSourceDiagnostic> = withContext(Dispatchers.IO) {
        MirrorHealthRegistry.resetAll()
        val client = ResilientTrackerNetwork.client(enableDoh)
        val probeQuery = "matrix"

        coroutineScope {
            val tasks = buildList {
                add(async {
                    val outcome = searchConfiguredIndexer(probeQuery, indexerUrl, indexerApiKey, client)
                    outcomeToDiagnostic("Индексатор RuTracker / Rutor (JacRed / Jackett)", outcome) { (_, list) ->
                        "Найдено раздач: ${list.size}"
                    }
                })
                if (enableDirectRutor) {
                    add(async {
                        val outcome = searchRutorDirect(probeQuery, client)
                        outcomeToDiagnostic("Зеркала Rutor (прямой опрос)", outcome) { list ->
                            "Доступен, раздач: ${list.size}"
                        }
                    })
                }
                if (enableAnimeNyaa) {
                    add(async {
                        val nyaaOut = searchNyaaRss("naruto", client)
                        if (nyaaOut is MirrorFetchOutcome.Success) {
                            outcomeToDiagnostic("Аниме-трекеры (Nyaa / AniLibria)", nyaaOut) { list ->
                                "Доступен, раздач: ${list.size}"
                            }
                        } else {
                            val aniOut = searchAniLibriaApi("naruto", client)
                            outcomeToDiagnostic("Аниме-трекеры (AniLibria резерв)", aniOut) { list ->
                                "Доступен, релизов: ${list.size}"
                            }
                        }
                    })
                }
                add(async {
                    val outcome = searchApiBay(probeQuery, client)
                    outcomeToDiagnostic("Глобальный DHT-индекс", outcome) { list ->
                        "Доступен, раздач: ${list.size}"
                    }
                })
            }
            tasks.awaitAll()
        }
    }

    private fun <T> outcomeToDiagnostic(
        name: String,
        outcome: MirrorFetchOutcome<T>?,
        successDetail: (T) -> String
    ): TrackerSourceDiagnostic {
        return when (outcome) {
            is MirrorFetchOutcome.Success -> {
                val host = outcome.mirrorUrl.removePrefix("https://").removePrefix("http://").substringBefore('/')
                TrackerSourceDiagnostic(
                    sourceName = name,
                    isHealthy = true,
                    activeMirror = host,
                    latencyMs = outcome.latencyMs,
                    detailMessage = "$host • ${outcome.latencyMs} мс • ${successDetail(outcome.data)}"
                )
            }
            is MirrorFetchOutcome.AntiBotBlocked -> TrackerSourceDiagnostic(
                sourceName = name,
                isHealthy = false,
                activeMirror = null,
                latencyMs = 0L,
                detailMessage = outcome.reason.descriptionRu
            )
            is MirrorFetchOutcome.NetworkFailure -> TrackerSourceDiagnostic(
                sourceName = name,
                isHealthy = false,
                activeMirror = null,
                latencyMs = 0L,
                detailMessage = outcome.message
            )
            null -> TrackerSourceDiagnostic(
                sourceName = name,
                isHealthy = false,
                activeMirror = null,
                latencyMs = 0L,
                detailMessage = "Нет ответа от зеркал"
            )
        }
    }

    fun sortAndFilter(
        results: List<TorrentSearchResult>,
        trackerFilter: TorrentTrackerGroup,
        qualityFilter: TorrentQualityFilter,
        sortMode: TorrentSortMode
    ): List<TorrentSearchResult> {
        val filtered = results.filter { item ->
            val trackerOk = trackerFilter == TorrentTrackerGroup.ALL || item.trackerGroup == trackerFilter
            val qualityOk = qualityFilter.matches(item)
            trackerOk && qualityOk
        }

        val comparator: Comparator<TorrentSearchResult> = when (sortMode) {
            TorrentSortMode.PRIORITY -> {
                compareByDescending<TorrentSearchResult> { it.seeders > 0 }
                    .thenBy { priorityTierFor(it.trackerGroup) }
                    .thenByDescending { it.seeders }
                    .thenBy { it.trackerGroup.priorityRank }
                    .thenByDescending { it.sizeBytes }
            }
            TorrentSortMode.SEEDERS -> {
                compareByDescending<TorrentSearchResult> { it.seeders }
                    .thenBy { it.trackerGroup.priorityRank }
                    .thenByDescending { it.sizeBytes }
            }
            TorrentSortMode.SIZE -> {
                compareByDescending<TorrentSearchResult> { it.sizeBytes }
                    .thenByDescending { it.seeders }
            }
            TorrentSortMode.DATE -> {
                compareByDescending<TorrentSearchResult> { it.publishedEpochMs }
                    .thenByDescending { it.seeders }
                    .thenBy { it.trackerGroup.priorityRank }
            }
        }

        return filtered.sortedWith(comparator)
    }

    private fun priorityTierFor(group: TorrentTrackerGroup): Int = when (group) {
        TorrentTrackerGroup.RUTRACKER -> 0
        TorrentTrackerGroup.RUTOR -> 1
        TorrentTrackerGroup.KINOZAL, TorrentTrackerGroup.NNMCLUB -> 2
        TorrentTrackerGroup.ANIME -> 3
        TorrentTrackerGroup.OTHER, TorrentTrackerGroup.ALL -> 4
    }

    internal fun deduplicateResults(raw: List<TorrentSearchResult>): List<TorrentSearchResult> {
        if (raw.isEmpty()) return emptyList()
        val byKey = LinkedHashMap<String, TorrentSearchResult>()
        for (item in raw) {
            if (item.title.isBlank() || item.primaryLink.isNullOrBlank()) continue
            val key = item.infoHash?.takeIf { it.isNotBlank() }
                ?: "${item.trackerGroup.id}|${item.title.lowercase(Locale.ROOT)}"
            val existing = byKey[key]
            if (existing == null) {
                byKey[key] = item
            } else {
                val preferNew = item.trackerGroup.priorityRank < existing.trackerGroup.priorityRank ||
                    (item.trackerGroup == existing.trackerGroup && item.seeders > existing.seeders)
                val merged = if (preferNew) {
                    item.copy(
                        seeders = maxOf(item.seeders, existing.seeders),
                        leechers = maxOf(item.leechers, existing.leechers),
                        magnetUri = item.magnetUri ?: existing.magnetUri,
                        torrentFileUrl = item.torrentFileUrl ?: existing.torrentFileUrl,
                        topicUrl = item.topicUrl ?: existing.topicUrl
                    )
                } else {
                    existing.copy(
                        seeders = maxOf(item.seeders, existing.seeders),
                        leechers = maxOf(item.leechers, existing.leechers),
                        magnetUri = existing.magnetUri ?: item.magnetUri,
                        torrentFileUrl = existing.torrentFileUrl ?: item.torrentFileUrl,
                        topicUrl = existing.topicUrl ?: item.topicUrl
                    )
                }
                byKey[key] = merged
            }
        }
        return byKey.values.toList()
    }

    private suspend fun searchConfiguredIndexer(
        query: String,
        rawIndexerUrl: String,
        rawApiKey: String,
        httpClient: OkHttpClient
    ): MirrorFetchOutcome<Pair<String, List<TorrentSearchResult>>> {
        val apiKey = rawApiKey.trim()
        val userUrls = ResilientTrackerNetwork.parseMirrorUrls(
            rawInput = rawIndexerUrl,
            defaultMirrors = emptyList(),
            includeDefaultsAsFallback = false
        )

        // 1. If user provided Jackett / Torznab API key, try their Jackett/Torznab server(s) first
        if (apiKey.isNotEmpty() && userUrls.isNotEmpty()) {
            for (userBase in userUrls) {
                val jackettOut = fetchJackettJson(userBase, apiKey, query, httpClient)
                if (jackettOut is MirrorFetchOutcome.Success && jackettOut.data.isNotEmpty()) {
                    return MirrorFetchOutcome.Success(
                        data = "Jackett" to jackettOut.data,
                        mirrorUrl = userBase,
                        latencyMs = jackettOut.latencyMs
                    )
                }
                val torznabOut = fetchTorznabXml(userBase, apiKey, query, httpClient)
                if (torznabOut is MirrorFetchOutcome.Success && torznabOut.data.isNotEmpty()) {
                    return MirrorFetchOutcome.Success(
                        data = "Torznab" to torznabOut.data,
                        mirrorUrl = userBase,
                        latencyMs = torznabOut.latencyMs
                    )
                }
            }
        }

        // 2. Build resilient JacRed mirror pool: user's custom mirrors first + all built-in fallback mirrors
        val jacRedCandidates = ResilientTrackerNetwork.parseMirrorUrls(
            rawInput = rawIndexerUrl,
            defaultMirrors = DEFAULT_JACRED_MIRRORS,
            includeDefaultsAsFallback = true
        )

        val jacRedOutcome = queryMirrorsWithFailover(
            candidates = jacRedCandidates,
            isNonEmpty = { it.isNotEmpty() }
        ) { mirror ->
            fetchJacRedJson(mirror, apiKey, query, httpClient)
        }

        return when (jacRedOutcome) {
            is MirrorFetchOutcome.Success -> {
                val hostLabel = jacRedOutcome.mirrorUrl
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .substringBefore('/')
                MirrorFetchOutcome.Success(
                    data = "JacRed ($hostLabel)" to jacRedOutcome.data,
                    mirrorUrl = jacRedOutcome.mirrorUrl,
                    latencyMs = jacRedOutcome.latencyMs
                )
            }
            is MirrorFetchOutcome.AntiBotBlocked -> jacRedOutcome
            is MirrorFetchOutcome.NetworkFailure -> jacRedOutcome
        }
    }

    internal suspend fun <T> queryMirrorsWithFailover(
        candidates: List<String>,
        batchSize: Int = 2,
        isNonEmpty: (T) -> Boolean,
        fetcher: suspend (String) -> MirrorFetchOutcome<T>
    ): MirrorFetchOutcome<T> = coroutineScope {
        val ranked = MirrorHealthRegistry.rankMirrors(candidates)
        if (ranked.isEmpty()) {
            return@coroutineScope MirrorFetchOutcome.NetworkFailure("Список зеркал пуст", "")
        }

        var reachableEmptySuccess: MirrorFetchOutcome.Success<T>? = null
        var lastAntiBot: MirrorFetchOutcome.AntiBotBlocked? = null
        var lastNetworkError: MirrorFetchOutcome.NetworkFailure? = null

        for (batch in ranked.chunked(batchSize.coerceAtLeast(1))) {
            val batchOutcomes = batch.map { mirror ->
                async {
                    val outcome = runCatching { fetcher(mirror) }.getOrElse { err ->
                        MirrorFetchOutcome.NetworkFailure(
                            message = err.localizedMessage ?: "Ошибка соединения",
                            mirrorUrl = mirror
                        )
                    }
                    when (outcome) {
                        is MirrorFetchOutcome.Success -> {
                            MirrorHealthRegistry.recordSuccess(mirror, outcome.latencyMs)
                        }
                        is MirrorFetchOutcome.AntiBotBlocked -> {
                            MirrorHealthRegistry.recordFailure(
                                mirrorUrl = mirror,
                                reason = outcome.reason.descriptionRu,
                                isAntiBot = true
                            )
                        }
                        is MirrorFetchOutcome.NetworkFailure -> {
                            MirrorHealthRegistry.recordFailure(
                                mirrorUrl = mirror,
                                reason = outcome.message,
                                isAntiBot = false
                            )
                        }
                    }
                    outcome
                }
            }.awaitAll()

            val nonEmptyHit = batchOutcomes
                .filterIsInstance<MirrorFetchOutcome.Success<T>>()
                .filter { isNonEmpty(it.data) }
                .minByOrNull { it.latencyMs }

            if (nonEmptyHit != null) {
                return@coroutineScope nonEmptyHit
            }

            if (reachableEmptySuccess == null) {
                reachableEmptySuccess = batchOutcomes
                    .filterIsInstance<MirrorFetchOutcome.Success<T>>()
                    .firstOrNull()
            }
            batchOutcomes.filterIsInstance<MirrorFetchOutcome.AntiBotBlocked>().firstOrNull()?.let {
                lastAntiBot = it
            }
            batchOutcomes.filterIsInstance<MirrorFetchOutcome.NetworkFailure>().firstOrNull()?.let {
                lastNetworkError = it
            }

            // If a mirror answered cleanly with valid empty JSON/XML (not HTML wall), no need to hammer all mirrors
            if (reachableEmptySuccess != null) {
                return@coroutineScope reachableEmptySuccess
            }
        }

        reachableEmptySuccess
            ?: lastAntiBot
            ?: lastNetworkError
            ?: MirrorFetchOutcome.NetworkFailure("Все зеркала недоступны", ranked.first())
    }

    internal fun parseJacRedJson(jsonText: String): List<TorrentSearchResult> {
        val trimmed = jsonText.trim()
        val array = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val root = JSONObject(trimmed)
                root.optJSONArray("Results")
                    ?: root.optJSONArray("results")
                    ?: root.optJSONArray("torrents")
                    ?: return emptyList()
            }
            else -> return emptyList()
        }

        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val title = obj.optString("title").trim()
                    .ifBlank { obj.optString("name").trim() }
                    .ifBlank { obj.optString("Title").trim() }
                if (title.isBlank()) continue

                val cleanTitle = cleanHtmlEntities(title)
                val rawTracker = obj.optString("tracker").trim()
                    .ifBlank { obj.optString("Tracker").trim() }
                    .ifBlank { "Tracker" }
                val rawMagnet = obj.optString("magnet").trim()
                    .ifBlank { obj.optString("MagnetUri").trim() }
                    .takeIf { it.startsWith("magnet:", ignoreCase = true) }
                val linkUrl = obj.optString("link").trim()
                    .ifBlank { obj.optString("Link").trim() }
                    .takeIf { it.startsWith("http", ignoreCase = true) }
                val topicUrl = obj.optString("url").trim()
                    .ifBlank { obj.optString("Details").trim() }
                    .takeIf { it.startsWith("http", ignoreCase = true) }

                val hash = BencodeTorrentParser.extractInfoHash(rawMagnet)
                    ?: BencodeTorrentParser.extractInfoHash(obj.optString("hash"))
                    ?: BencodeTorrentParser.extractInfoHash(obj.optString("InfoHash"))

                // Enrich magnet with RuTracker (bt.t-ru.org) & open UDP trackers so TorrServer bypasses web anti-bot
                val finalMagnet = BencodeTorrentParser.enrichMagnetUri(rawMagnet, hash, cleanTitle)
                if (finalMagnet == null && linkUrl == null) continue

                val sizeBytes = obj.optLong("size", obj.optLong("Size", 0L)).coerceAtLeast(0L)
                val sizeLabel = obj.optString("sizeName").trim()
                    .ifBlank { if (sizeBytes > 0L) formatFileSize(sizeBytes) else "" }
                val seeders = obj.optInt("sid", obj.optInt("Seeders", 0)).coerceAtLeast(0)
                val leechers = obj.optInt("pir", obj.optInt("Peers", 0)).coerceAtLeast(0)
                val dateRaw = obj.optString("createTime").trim()
                    .ifBlank { obj.optString("PublishDate").trim() }
                val dateLabel = formatCompactDate(dateRaw)

                val trackerGroup = TorrentTrackerGroup.classify(rawTracker)
                val displayTracker = TorrentTrackerGroup.formatDisplayTracker(rawTracker)

                add(
                    TorrentSearchResult(
                        id = "jac:${hash ?: i}:$rawTracker",
                        title = cleanTitle,
                        trackerName = displayTracker,
                        trackerGroup = trackerGroup,
                        magnetUri = finalMagnet,
                        torrentFileUrl = linkUrl,
                        topicUrl = topicUrl,
                        infoHash = hash,
                        sizeBytes = sizeBytes,
                        sizeLabel = sizeLabel,
                        seeders = seeders,
                        leechers = leechers,
                        publishedLabel = dateLabel
                    )
                )
            }
        }
    }

    private fun fetchJacRedJson(
        baseUrl: String,
        apiKey: String,
        query: String,
        httpClient: OkHttpClient
    ): MirrorFetchOutcome<List<TorrentSearchResult>> {
        val encoded = urlEncode(query)
        val url = buildString {
            append(baseUrl.trimEnd('/'))
            append("/api/v1.0/torrents?search=")
            append(encoded)
            if (apiKey.isNotBlank()) {
                append("&apikey=")
                append(urlEncode(apiKey))
            }
        }
        return when (val resp = httpGetValidated(url, baseUrl, ExpectedResponseKind.JSON, httpClient)) {
            is MirrorFetchOutcome.Success -> {
                val parsed = runCatching { parseJacRedJson(resp.data) }.getOrNull()
                    ?: return MirrorFetchOutcome.NetworkFailure("Некорректный JSON от зеркала", baseUrl)
                MirrorFetchOutcome.Success(parsed, baseUrl, resp.latencyMs)
            }
            is MirrorFetchOutcome.AntiBotBlocked -> resp
            is MirrorFetchOutcome.NetworkFailure -> resp
        }
    }

    private fun fetchJackettJson(
        baseUrl: String,
        apiKey: String,
        query: String,
        httpClient: OkHttpClient
    ): MirrorFetchOutcome<List<TorrentSearchResult>> {
        val cleanBase = baseUrl.trimEnd('/')
        val url = if (cleanBase.contains("/api/v2.0/")) {
            "$cleanBase?apikey=${urlEncode(apiKey)}&Query=${urlEncode(query)}"
        } else {
            "$cleanBase/api/v2.0/indexers/all/results?apikey=${urlEncode(apiKey)}&Query=${urlEncode(query)}"
        }
        return when (val resp = httpGetValidated(url, baseUrl, ExpectedResponseKind.JSON, httpClient)) {
            is MirrorFetchOutcome.Success -> {
                val parsed = runCatching { parseJacRedJson(resp.data) }.getOrNull()
                    ?: return MirrorFetchOutcome.NetworkFailure("Некорректный ответ Jackett", baseUrl)
                MirrorFetchOutcome.Success(parsed, baseUrl, resp.latencyMs)
            }
            is MirrorFetchOutcome.AntiBotBlocked -> resp
            is MirrorFetchOutcome.NetworkFailure -> resp
        }
    }

    internal fun parseTorznabOrNyaaXml(xmlText: String, defaultTracker: String): List<TorrentSearchResult> {
        if (xmlText.isBlank()) return emptyList()
        return runCatching {
            val results = mutableListOf<TorrentSearchResult>()
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlText))

            var inItem = false
            var title = ""
            var link = ""
            var guid = ""
            var pubDate = ""
            var tracker = defaultTracker
            var magnetAttr: String? = null
            var infoHash: String? = null
            var sizeBytes = 0L
            var sizeText = ""
            var seeders = 0
            var peers = 0

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name.lowercase(Locale.ROOT)
                        if (tag == "item") {
                            inItem = true
                            title = ""
                            link = ""
                            guid = ""
                            pubDate = ""
                            tracker = defaultTracker
                            magnetAttr = null
                            infoHash = null
                            sizeBytes = 0L
                            sizeText = ""
                            seeders = 0
                            peers = 0
                        } else if (inItem) {
                            when (tag) {
                                "title" -> title = parser.nextText().trim()
                                "link" -> link = parser.nextText().trim()
                                "guid" -> guid = parser.nextText().trim()
                                "pubdate" -> pubDate = parser.nextText().trim()
                                "size" -> sizeBytes = parser.nextText().trim().toLongOrNull() ?: sizeBytes
                                "jackettindexer" -> tracker = parser.nextText().trim().ifBlank { defaultTracker }
                                "nyaa:infohash" -> infoHash = parser.nextText().trim()
                                "nyaa:seeders" -> seeders = parser.nextText().trim().toIntOrNull() ?: 0
                                "nyaa:leechers" -> peers = parser.nextText().trim().toIntOrNull() ?: 0
                                "nyaa:size" -> {
                                    sizeText = parser.nextText().trim()
                                    sizeBytes = parseHumanSizeToBytes(sizeText)
                                }
                                "enclosure" -> {
                                    val encUrl = parser.getAttributeValue(null, "url").orEmpty()
                                    if (encUrl.isNotBlank() && link.isBlank()) link = encUrl
                                    val len = parser.getAttributeValue(null, "length")?.toLongOrNull()
                                    if (len != null && len > 0L) sizeBytes = len
                                }
                                "torznab:attr" -> {
                                    val attrName = parser.getAttributeValue(null, "name")?.lowercase(Locale.ROOT)
                                    val attrVal = parser.getAttributeValue(null, "value").orEmpty()
                                    when (attrName) {
                                        "seeders" -> seeders = attrVal.toIntOrNull() ?: seeders
                                        "peers" -> peers = attrVal.toIntOrNull() ?: peers
                                        "magneturl" -> if (attrVal.startsWith("magnet:", ignoreCase = true)) magnetAttr = attrVal
                                        "infohash" -> if (attrVal.isNotBlank()) infoHash = attrVal
                                        "size" -> sizeBytes = attrVal.toLongOrNull() ?: sizeBytes
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("item", ignoreCase = true) && inItem) {
                            inItem = false
                            val cleanTitle = cleanHtmlEntities(title)
                            val rawMagnet = when {
                                !magnetAttr.isNullOrBlank() -> magnetAttr
                                link.startsWith("magnet:", ignoreCase = true) -> link
                                else -> null
                            }
                            val hash = BencodeTorrentParser.extractInfoHash(rawMagnet)
                                ?: BencodeTorrentParser.extractInfoHash(infoHash)
                            val resolvedMagnet = BencodeTorrentParser.enrichMagnetUri(rawMagnet, hash, cleanTitle)
                            val torrentUrl = link.takeIf { it.startsWith("http", ignoreCase = true) }
                            val topicUrl = guid.takeIf { it.startsWith("http", ignoreCase = true) }

                            if (cleanTitle.isNotBlank() && (resolvedMagnet != null || torrentUrl != null)) {
                                val group = TorrentTrackerGroup.classify(tracker)
                                val displayTracker = TorrentTrackerGroup.formatDisplayTracker(tracker)
                                val formattedSize = sizeText.ifBlank {
                                    if (sizeBytes > 0L) formatFileSize(sizeBytes) else ""
                                }
                                results.add(
                                    TorrentSearchResult(
                                        id = "xml:${hash ?: results.size}:$displayTracker",
                                        title = cleanTitle,
                                        trackerName = displayTracker,
                                        trackerGroup = group,
                                        magnetUri = resolvedMagnet,
                                        torrentFileUrl = torrentUrl,
                                        topicUrl = topicUrl,
                                        infoHash = hash,
                                        sizeBytes = sizeBytes,
                                        sizeLabel = formattedSize,
                                        seeders = seeders,
                                        leechers = peers.coerceAtLeast(0),
                                        publishedLabel = formatCompactDate(pubDate)
                                    )
                                )
                            }
                        }
                    }
                }
                event = parser.next()
            }
            results
        }.getOrDefault(emptyList())
    }

    private fun fetchTorznabXml(
        baseUrl: String,
        apiKey: String,
        query: String,
        httpClient: OkHttpClient
    ): MirrorFetchOutcome<List<TorrentSearchResult>> {
        val clean = baseUrl.trimEnd('/')
        val endpoint = if (clean.endsWith("/api")) {
            "$clean?t=search&apikey=${urlEncode(apiKey)}&q=${urlEncode(query)}"
        } else {
            "$clean/api/v2.0/indexers/all/results/torznab/api?t=search&apikey=${urlEncode(apiKey)}&q=${urlEncode(query)}"
        }
        return when (val resp = httpGetValidated(endpoint, baseUrl, ExpectedResponseKind.XML, httpClient)) {
            is MirrorFetchOutcome.Success -> MirrorFetchOutcome.Success(
                data = parseTorznabOrNyaaXml(resp.data, "Torznab"),
                mirrorUrl = baseUrl,
                latencyMs = resp.latencyMs
            )
            is MirrorFetchOutcome.AntiBotBlocked -> resp
            is MirrorFetchOutcome.NetworkFailure -> resp
        }
    }

    private suspend fun searchNyaaRss(
        query: String,
        httpClient: OkHttpClient
    ): MirrorFetchOutcome<List<TorrentSearchResult>> {
        return queryMirrorsWithFailover(
            candidates = NYAA_MIRRORS,
            isNonEmpty = { it.isNotEmpty() }
        ) { mirror ->
            val url = "${mirror.trimEnd('/')}/?page=rss&q=${urlEncode(query)}&c=1_0&f=0"
            when (val resp = httpGetValidated(url, mirror, ExpectedResponseKind.XML, httpClient)) {
                is MirrorFetchOutcome.Success -> MirrorFetchOutcome.Success(
                    data = parseTorznabOrNyaaXml(resp.data, "Nyaa"),
                    mirrorUrl = mirror,
                    latencyMs = resp.latencyMs
                )
                is MirrorFetchOutcome.AntiBotBlocked -> resp
                is MirrorFetchOutcome.NetworkFailure -> resp
            }
        }
    }

    private suspend fun searchAniLibriaApi(
        query: String,
        httpClient: OkHttpClient
    ): MirrorFetchOutcome<List<TorrentSearchResult>> {
        return queryMirrorsWithFailover(
            candidates = ANILIBRIA_MIRRORS,
            isNonEmpty = { it.isNotEmpty() }
        ) { mirror ->
            val clean = mirror.trimEnd('/')
            val url = if (clean.contains("api.anilibria.tv", ignoreCase = true)) {
                "$clean/v3/title/search?search=${urlEncode(query)}&limit=15"
            } else {
                "$clean/api/v1/app/search/releases?query=${urlEncode(query)}"
            }
            when (val resp = httpGetValidated(url, mirror, ExpectedResponseKind.JSON, httpClient)) {
                is MirrorFetchOutcome.Success -> MirrorFetchOutcome.Success(
                    data = parseAniLibriaJson(resp.data),
                    mirrorUrl = mirror,
                    latencyMs = resp.latencyMs
                )
                is MirrorFetchOutcome.AntiBotBlocked -> resp
                is MirrorFetchOutcome.NetworkFailure -> resp
            }
        }
    }

    internal fun parseAniLibriaJson(jsonText: String): List<TorrentSearchResult> {
        val trimmed = jsonText.trim()
        if (trimmed.isEmpty()) return emptyList()
        return runCatching {
            val releasesArray: JSONArray = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val root = JSONObject(trimmed)
                    root.optJSONArray("list")
                        ?: root.optJSONArray("data")
                        ?: return@runCatching emptyList()
                }
                else -> return@runCatching emptyList()
            }

            buildList {
                for (i in 0 until releasesArray.length()) {
                    val rel = releasesArray.optJSONObject(i) ?: continue
                    val nameObj = rel.optJSONObject("name") ?: rel.optJSONObject("names")
                    val ruName = nameObj?.optString("main")?.ifBlank { nameObj.optString("ru") }.orEmpty().trim()
                    val enName = nameObj?.optString("english")?.ifBlank { nameObj.optString("en") }.orEmpty().trim()
                    val baseTitle = listOf(ruName, enName).filter { it.isNotBlank() }.distinct().joinToString(" / ")
                        .ifBlank { rel.optString("alias", "AniLibria Release") }

                    val torrentsArr = rel.optJSONArray("torrents")
                        ?: rel.optJSONObject("torrents")?.optJSONArray("list")
                        ?: continue

                    for (tIdx in 0 until torrentsArr.length()) {
                        val tObj = torrentsArr.optJSONObject(tIdx) ?: continue
                        val rawMagnet = tObj.optString("magnet").trim().takeIf { it.startsWith("magnet:", ignoreCase = true) }
                        val hash = BencodeTorrentParser.extractInfoHash(rawMagnet)
                            ?: BencodeTorrentParser.extractInfoHash(tObj.optString("hash"))
                        val qualityStr = tObj.optJSONObject("quality")?.optString("description")
                            ?: tObj.optJSONObject("quality")?.optString("string")
                            ?: tObj.optString("label")
                        val epDesc = tObj.optString("description")
                            .ifBlank { tObj.optJSONObject("episodes")?.optString("string").orEmpty() }
                        val fullTitle = buildString {
                            append(cleanHtmlEntities(baseTitle))
                            if (epDesc.isNotBlank()) append(" [").append(epDesc.trim()).append("]")
                            if (qualityStr.isNotBlank()) append(" [").append(qualityStr.trim()).append("]")
                        }
                        val magnet = BencodeTorrentParser.enrichMagnetUri(rawMagnet, hash, fullTitle) ?: continue
                        val sizeBytes = tObj.optLong("size", tObj.optLong("total_size", 0L)).coerceAtLeast(0L)
                        val seeds = tObj.optInt("seeders", 0).coerceAtLeast(0)
                        val leech = tObj.optInt("leechers", 0).coerceAtLeast(0)
                        add(
                            TorrentSearchResult(
                                id = "anilibria:${hash ?: "$i-$tIdx"}",
                                title = fullTitle,
                                trackerName = "AniLibria",
                                trackerGroup = TorrentTrackerGroup.ANIME,
                                magnetUri = magnet,
                                torrentFileUrl = null,
                                topicUrl = null,
                                infoHash = hash,
                                sizeBytes = sizeBytes,
                                sizeLabel = if (sizeBytes > 0L) formatFileSize(sizeBytes) else "",
                                seeders = seeds,
                                leechers = leech
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private suspend fun searchRutorDirect(
        query: String,
        httpClient: OkHttpClient
    ): MirrorFetchOutcome<List<TorrentSearchResult>> {
        val encoded = urlEncode(query)
        return queryMirrorsWithFailover(
            candidates = RUTOR_MIRRORS,
            isNonEmpty = { it.isNotEmpty() }
        ) { mirror ->
            val url = "${mirror.trimEnd('/')}/search/0/0/000/2/$encoded"
            when (val resp = httpGetValidated(url, mirror, ExpectedResponseKind.HTML, httpClient)) {
                is MirrorFetchOutcome.Success -> {
                    val parsed = parseRutorHtml(resp.data, mirror)
                    // Verify HTML actually came from Rutor (contains search layout or index div)
                    val lower = resp.data.take(12_000).lowercase(Locale.ROOT)
                    if (parsed.isEmpty() && !lower.contains("rutor") && !lower.contains("index")) {
                        MirrorFetchOutcome.AntiBotBlocked(AntiBotBlockReason.UNEXPECTED_HTML_WALL, mirror)
                    } else {
                        MirrorFetchOutcome.Success(parsed, mirror, resp.latencyMs)
                    }
                }
                is MirrorFetchOutcome.AntiBotBlocked -> resp
                is MirrorFetchOutcome.NetworkFailure -> resp
            }
        }
    }

    internal fun parseRutorHtml(html: String, baseMirror: String = "https://rutor.info"): List<TorrentSearchResult> {
        val rowRegex = Regex("""(?is)<tr\s+class=["'](?:gai|tum)["'][^>]*>(.*?)</tr>""")
        val magnetRegex = Regex("""href=["'](magnet:\?xt=urn:btih:[^"']+)["']""", RegexOption.IGNORE_CASE)
        val dlRegex = Regex("""href=["']((?:https?://[^"']+)?/download/\d+)["']""", RegexOption.IGNORE_CASE)
        val topicAndTitleRegex = Regex(
            """<a\s+href=["']((?:https?://[^"']+)?/torrent/\d+[^"']*)["'][^>]*>(.*?)</a>""",
            RegexOption.IGNORE_CASE
        )
        val greenSeedsRegex = Regex("""class=["']green["'][^>]*>.*?>?\s*(\d+)\s*<""", RegexOption.IGNORE_CASE)
        val redPeersRegex = Regex("""class=["']red["'][^>]*>\s*(\d+)\s*<""", RegexOption.IGNORE_CASE)
        val sizeRegex = Regex("""(\d+(?:[.,]\d+)?)\s*(?:&nbsp;|\s)*(TB|GB|MB|ТБ|ГБ|МБ|GiB|MiB)""", RegexOption.IGNORE_CASE)
        val dateTdRegex = Regex("""<td[^>]*>(\d{1,2}\s*(?:&nbsp;|\s)*[А-Яа-яA-Za-z]{3}\s*(?:&nbsp;|\s)*\d{2,4})</td>""")

        val results = mutableListOf<TorrentSearchResult>()
        for (match in rowRegex.findAll(html)) {
            val rowHtml = match.groupValues[1]
            val rawMagnet = magnetRegex.find(rowHtml)?.groupValues?.getOrNull(1)?.let { cleanHtmlEntities(it) }
            val topicMatch = topicAndTitleRegex.find(rowHtml) ?: continue
            val rawTopicRef = topicMatch.groupValues[1]
            val rawTitle = stripHtmlTags(topicMatch.groupValues[2]).let { cleanHtmlEntities(it) }.trim()
            if (rawTitle.isBlank()) continue

            val rawDl = dlRegex.find(rowHtml)?.groupValues?.getOrNull(1)
            val fullDl = rawDl?.let {
                if (it.startsWith("http", ignoreCase = true)) it else "${baseMirror.trimEnd('/')}$it"
            }
            val fullTopic = if (rawTopicRef.startsWith("http", ignoreCase = true)) {
                rawTopicRef
            } else {
                "${baseMirror.trimEnd('/')}$rawTopicRef"
            }

            val hash = BencodeTorrentParser.extractInfoHash(rawMagnet)
            val enrichedMagnet = BencodeTorrentParser.enrichMagnetUri(rawMagnet, hash, rawTitle)
            if (enrichedMagnet == null && fullDl == null) continue

            val sizeMatch = sizeRegex.find(rowHtml)
            val sizeText = if (sizeMatch != null) {
                "${sizeMatch.groupValues[1].replace(',', '.')} ${sizeMatch.groupValues[2].uppercase(Locale.ROOT)}"
            } else {
                ""
            }
            val sizeBytes = parseHumanSizeToBytes(sizeText)
            val seeds = greenSeedsRegex.find(rowHtml)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val leech = redPeersRegex.find(rowHtml)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val dateStr = dateTdRegex.find(rowHtml)?.groupValues?.getOrNull(1)
                ?.replace("&nbsp;", " ")
                ?.replace(Regex("""\s+"""), " ")
                ?.trim()
                .orEmpty()

            results.add(
                TorrentSearchResult(
                    id = "rutor:${hash ?: rawTopicRef}",
                    title = rawTitle,
                    trackerName = "Rutor",
                    trackerGroup = TorrentTrackerGroup.RUTOR,
                    magnetUri = enrichedMagnet,
                    torrentFileUrl = fullDl,
                    topicUrl = fullTopic,
                    infoHash = hash,
                    sizeBytes = sizeBytes,
                    sizeLabel = sizeText,
                    seeders = seeds,
                    leechers = leech,
                    publishedLabel = dateStr
                )
            )
        }
        return results
    }

    private suspend fun searchApiBay(
        query: String,
        httpClient: OkHttpClient
    ): MirrorFetchOutcome<List<TorrentSearchResult>> {
        return queryMirrorsWithFailover(
            candidates = APIBAY_MIRRORS,
            isNonEmpty = { it.isNotEmpty() }
        ) { mirror ->
            val url = "${mirror.trimEnd('/')}/q.php?q=${urlEncode(query)}&cat=200"
            when (val resp = httpGetValidated(url, mirror, ExpectedResponseKind.JSON, httpClient)) {
                is MirrorFetchOutcome.Success -> {
                    val trimmed = resp.data.trim()
                    if (!trimmed.startsWith("[")) {
                        MirrorFetchOutcome.NetworkFailure("Некорректный JSON DHT", mirror)
                    } else {
                        val arr = JSONArray(trimmed)
                        val items = buildList {
                            for (i in 0 until arr.length()) {
                                val obj = arr.optJSONObject(i) ?: continue
                                val hash = obj.optString("info_hash").trim().lowercase(Locale.ROOT)
                                if (hash.length != 40 || hash.all { it == '0' }) continue
                                val name = cleanHtmlEntities(obj.optString("name").trim())
                                if (name.isBlank() || name.equals("No results returned", ignoreCase = true)) continue
                                val seeds = obj.optString("seeders").toIntOrNull() ?: obj.optInt("seeders", 0)
                                val leech = obj.optString("leechers").toIntOrNull() ?: obj.optInt("leechers", 0)
                                val sizeBytes = obj.optString("size").toLongOrNull() ?: obj.optLong("size", 0L)
                                val magnet = BencodeTorrentParser.buildMagnetUri(hash, name)
                                add(
                                    TorrentSearchResult(
                                        id = "dht:$hash",
                                        title = name,
                                        trackerName = "Global DHT",
                                        trackerGroup = TorrentTrackerGroup.OTHER,
                                        magnetUri = magnet,
                                        torrentFileUrl = null,
                                        topicUrl = null,
                                        infoHash = hash,
                                        sizeBytes = sizeBytes,
                                        sizeLabel = if (sizeBytes > 0L) formatFileSize(sizeBytes) else "",
                                        seeders = seeds,
                                        leechers = leech
                                    )
                                )
                            }
                        }
                        MirrorFetchOutcome.Success(items, mirror, resp.latencyMs)
                    }
                }
                is MirrorFetchOutcome.AntiBotBlocked -> resp
                is MirrorFetchOutcome.NetworkFailure -> resp
            }
        }
    }

    internal fun parseHumanSizeToBytes(raw: String): Long {
        if (raw.isBlank()) return 0L
        val m = Regex("""(\d+(?:[.,]\d+)?)\s*(TB|TiB|ТБ|GB|GiB|ГБ|MB|MiB|МБ|KB|KiB|КБ)""", RegexOption.IGNORE_CASE)
            .find(raw) ?: return 0L
        val value = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return 0L
        val unit = m.groupValues[2].uppercase(Locale.ROOT)
        val mult = when (unit) {
            "TB", "TIB", "ТБ" -> 1024.0 * 1024.0 * 1024.0 * 1024.0
            "GB", "GIB", "ГБ" -> 1024.0 * 1024.0 * 1024.0
            "MB", "MIB", "МБ" -> 1024.0 * 1024.0
            "KB", "KIB", "КБ" -> 1024.0
            else -> 1.0
        }
        return (value * mult).roundToLong()
    }

    private fun formatCompactDate(raw: String): String {
        if (raw.isBlank()) return ""
        Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(raw)?.let { m ->
            return "${m.groupValues[3]}.${m.groupValues[2]}.${m.groupValues[1]}"
        }
        Regex("""\b(\d{1,2}\s+[A-Za-z]{3}\s+\d{4})\b""").find(raw)?.let { m ->
            return m.groupValues[1]
        }
        return raw.take(16)
    }

    private fun stripHtmlTags(input: String): String =
        input.replace(Regex("""<[^>]+>"""), "")

    private fun cleanHtmlEntities(input: String): String =
        input
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")

    private fun httpGetValidated(
        url: String,
        mirrorBaseUrl: String,
        expectedKind: ExpectedResponseKind,
        client: OkHttpClient
    ): MirrorFetchOutcome<String> {
        val startMs = System.currentTimeMillis()
        val acceptHeader = when (expectedKind) {
            ExpectedResponseKind.JSON -> "application/json, text/plain;q=0.9, */*;q=0.5"
            ExpectedResponseKind.XML -> "application/rss+xml, application/xml, text/xml, */*;q=0.5"
            ExpectedResponseKind.HTML -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
            else -> "*/*"
        }
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", ResilientTrackerNetwork.BROWSER_USER_AGENT)
                .header("Accept", acceptHeader)
                .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                val latencyMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
                val body = resp.body?.string().orEmpty()
                val antiBot = ResilientTrackerNetwork.detectAntiBotOrBlock(
                    statusCode = resp.code,
                    headers = resp.headers,
                    body = body,
                    expectedKind = expectedKind
                )
                if (antiBot != null) {
                    return@use MirrorFetchOutcome.AntiBotBlocked(antiBot, mirrorBaseUrl)
                }
                if (!resp.isSuccessful) {
                    return@use MirrorFetchOutcome.NetworkFailure("HTTP ${resp.code}", mirrorBaseUrl)
                }
                if (body.isBlank()) {
                    return@use MirrorFetchOutcome.NetworkFailure("Пустой ответ узла", mirrorBaseUrl)
                }
                MirrorFetchOutcome.Success(body, mirrorBaseUrl, latencyMs)
            }
        }.getOrElse { err ->
            MirrorFetchOutcome.NetworkFailure(
                message = err.localizedMessage ?: "Сетевая ошибка",
                mirrorUrl = mirrorBaseUrl
            )
        }
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}
