package com.example.torrent

import com.example.data.MediaAutoMatcher
import com.example.data.RawCompanionFile
import com.example.model.EpisodeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

object TorrServerClient {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val TORRENT_MEDIA_TYPE = "application/x-bittorrent".toMediaType()
    private val IPV4_OR_LOCAL_REGEX = Regex("""^(localhost|\d{1,3}(?:\.\d{1,3}){3})$""", RegexOption.IGNORE_CASE)

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val pingClient: OkHttpClient by lazy {
        httpClient.newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    fun normalizeBaseUrl(rawUrl: String): String {
        var cleaned = rawUrl.trim().trimEnd('/')
        if (cleaned.isEmpty()) return "http://127.0.0.1:8090"
        if (!cleaned.startsWith("http://", ignoreCase = true) &&
            !cleaned.startsWith("https://", ignoreCase = true)
        ) {
            cleaned = "http://$cleaned"
        }
        return runCatching {
            val uri = URI(cleaned)
            val host = uri.host ?: return@runCatching cleaned
            if (uri.port == -1 && IPV4_OR_LOCAL_REGEX.matches(host)) {
                val scheme = uri.scheme ?: "http"
                val path = uri.rawPath.orEmpty().trimEnd('/')
                "$scheme://$host:8090$path"
            } else {
                cleaned
            }
        }.getOrDefault(cleaned)
    }

    fun buildBasicAuthHeader(username: String, password: String): String? {
        val cleanUser = username.trim()
        if (cleanUser.isEmpty()) return null
        return Credentials.basic(cleanUser, password, Charsets.UTF_8)
    }

    suspend fun ping(
        rawBaseUrl: String,
        username: String = "",
        password: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = normalizeBaseUrl(rawBaseUrl)
            val reqBuilder = Request.Builder()
                .url("$baseUrl/echo")
                .get()
            buildBasicAuthHeader(username, password)?.let {
                reqBuilder.header("Authorization", it)
            }
            pingClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.code == 401) {
                    throw IllegalStateException("Требуется логин/пароль TorrServer (HTTP 401)")
                }
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Код ответа сервера: HTTP ${resp.code}")
                }
                val version = resp.body?.string()?.trim().orEmpty()
                version.ifBlank { "TorrServer" }
            }
        }
    }

    suspend fun addTorrent(
        rawBaseUrl: String,
        username: String = "",
        password: String = "",
        link: String,
        title: String,
        saveToDb: Boolean = true
    ): Result<TorrServerTorrent> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = normalizeBaseUrl(rawBaseUrl)
            val normalizedLink = BencodeTorrentParser.ensurePlayableOrMagnetLink(link, title)
            val payload = JSONObject().apply {
                put("action", "add")
                put("link", normalizedLink)
                put("title", title.ifBlank { "Torrent" })
                put("poster", "")
                put("save_to_db", saveToDb)
            }

            val reqBuilder = Request.Builder()
                .url("$baseUrl/torrents")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            buildBasicAuthHeader(username, password)?.let {
                reqBuilder.header("Authorization", it)
            }

            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.code == 401) {
                    throw IllegalStateException("TorrServer отклонил доступ (проверьте логин/пароль)")
                }
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Ошибка добавления в TorrServer (HTTP ${resp.code})")
                }
                val body = resp.body?.string().orEmpty()
                val fallbackHash = BencodeTorrentParser.extractInfoHash(normalizedLink).orEmpty()
                if (body.trim().startsWith("{")) {
                    parseTorrentJson(JSONObject(body), fallbackHash, title)
                } else {
                    val extracted = BencodeTorrentParser.extractInfoHash(body) ?: fallbackHash
                    if (extracted.isEmpty()) {
                        throw IllegalStateException("TorrServer не вернул хеш торрента")
                    }
                    TorrServerTorrent(hash = extracted, title = title)
                }
            }
        }
    }

    suspend fun uploadTorrentBytes(
        rawBaseUrl: String,
        username: String = "",
        password: String = "",
        bytes: ByteArray,
        fileName: String,
        title: String,
        saveToDb: Boolean = true
    ): Result<TorrServerTorrent> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = normalizeBaseUrl(rawBaseUrl)
            val parsedMeta = BencodeTorrentParser.parseTorrentBytes(bytes, title.ifBlank { fileName })
            val effectiveTitle = title.ifBlank { parsedMeta?.displayName ?: fileName }

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("save", if (saveToDb) "true" else "false")
                .addFormDataPart("title", effectiveTitle)
                .addFormDataPart(
                    "file",
                    fileName.ifBlank { "release.torrent" },
                    bytes.toRequestBody(TORRENT_MEDIA_TYPE)
                )
                .build()

            val reqBuilder = Request.Builder()
                .url("$baseUrl/torrent/upload")
                .post(multipart)
            buildBasicAuthHeader(username, password)?.let {
                reqBuilder.header("Authorization", it)
            }

            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    // Fallback: if server doesn't accept multipart upload, add via parsed magnet
                    val fallbackMagnet = parsedMeta?.magnetUri
                        ?: throw IllegalStateException("Ошибка загрузки .torrent (HTTP ${resp.code})")
                    return@runCatching addTorrent(
                        rawBaseUrl = baseUrl,
                        username = username,
                        password = password,
                        link = fallbackMagnet,
                        title = effectiveTitle,
                        saveToDb = saveToDb
                    ).getOrThrow()
                }
                val body = resp.body?.string().orEmpty()
                val fallbackHash = parsedMeta?.infoHashHex.orEmpty()
                if (body.trim().startsWith("{")) {
                    parseTorrentJson(JSONObject(body), fallbackHash, effectiveTitle)
                } else if (body.trim().startsWith("[")) {
                    val arr = JSONArray(body)
                    if (arr.length() > 0) {
                        parseTorrentJson(arr.getJSONObject(0), fallbackHash, effectiveTitle)
                    } else {
                        TorrServerTorrent(hash = fallbackHash, title = effectiveTitle)
                    }
                } else {
                    val hash = BencodeTorrentParser.extractInfoHash(body) ?: fallbackHash
                    TorrServerTorrent(hash = hash, title = effectiveTitle)
                }
            }
        }
    }

    suspend fun getTorrent(
        rawBaseUrl: String,
        username: String = "",
        password: String = "",
        hash: String,
        fallbackTitle: String = ""
    ): Result<TorrServerTorrent> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = normalizeBaseUrl(rawBaseUrl)
            val payload = JSONObject().apply {
                put("action", "get")
                put("hash", hash)
            }
            val reqBuilder = Request.Builder()
                .url("$baseUrl/torrents")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            buildBasicAuthHeader(username, password)?.let {
                reqBuilder.header("Authorization", it)
            }
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("HTTP ${resp.code}")
                }
                val body = resp.body?.string().orEmpty()
                parseTorrentJson(JSONObject(body), hash, fallbackTitle)
            }
        }
    }

    suspend fun listTorrents(
        rawBaseUrl: String,
        username: String = "",
        password: String = ""
    ): Result<List<TorrServerTorrent>> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = normalizeBaseUrl(rawBaseUrl)
            val payload = JSONObject().apply { put("action", "list") }
            val reqBuilder = Request.Builder()
                .url("$baseUrl/torrents")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            buildBasicAuthHeader(username, password)?.let {
                reqBuilder.header("Authorization", it)
            }
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("HTTP ${resp.code}")
                }
                val body = resp.body?.string().orEmpty().trim()
                if (!body.startsWith("[")) return@use emptyList()
                val arr = JSONArray(body)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val parsed = parseTorrentJson(obj, "", "")
                        if (parsed.hash.isNotBlank()) add(parsed)
                    }
                }
            }
        }
    }

    suspend fun removeTorrent(
        rawBaseUrl: String,
        username: String = "",
        password: String = "",
        hash: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = normalizeBaseUrl(rawBaseUrl)
            val payload = JSONObject().apply {
                put("action", "rem")
                put("hash", hash)
            }
            val reqBuilder = Request.Builder()
                .url("$baseUrl/torrents")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            buildBasicAuthHeader(username, password)?.let {
                reqBuilder.header("Authorization", it)
            }
            httpClient.newCall(reqBuilder.build()).execute().close()
        }
    }

    suspend fun dropTorrentCache(
        rawBaseUrl: String,
        username: String = "",
        password: String = "",
        hash: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = normalizeBaseUrl(rawBaseUrl)
            val payload = JSONObject().apply {
                put("action", "drop")
                put("hash", hash)
            }
            val reqBuilder = Request.Builder()
                .url("$baseUrl/torrents")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            buildBasicAuthHeader(username, password)?.let {
                reqBuilder.header("Authorization", it)
            }
            httpClient.newCall(reqBuilder.build()).execute().close()
        }
    }

    suspend fun resolveTorrentWithFiles(
        rawBaseUrl: String,
        username: String = "",
        password: String = "",
        magnetUri: String?,
        torrentFileUrl: String?,
        localTorrentBytes: ByteArray? = null,
        localFileName: String = "release.torrent",
        title: String,
        saveToDb: Boolean = true,
        onStatusUpdate: (String) -> Unit = {}
    ): Result<TorrServerTorrent> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = normalizeBaseUrl(rawBaseUrl)
            onStatusUpdate("Проверка подключения к TorrServer ($baseUrl)…")
            ping(baseUrl, username, password).getOrElse { err ->
                throw IllegalStateException(
                    "Не удалось подключиться к TorrServer ($baseUrl). " +
                        "Убедитесь, что TorrServer запущен или укажите IP-адрес сервера в настройках. (${err.message})"
                )
            }

            val initialTorrent: TorrServerTorrent = when {
                localTorrentBytes != null && localTorrentBytes.isNotEmpty() -> {
                    onStatusUpdate("Отправка .torrent файла на TorrServer…")
                    uploadTorrentBytes(
                        rawBaseUrl = baseUrl,
                        username = username,
                        password = password,
                        bytes = localTorrentBytes,
                        fileName = localFileName,
                        title = title,
                        saveToDb = saveToDb
                    ).getOrThrow()
                }

                !torrentFileUrl.isNullOrBlank() && magnetUri.isNullOrBlank() -> {
                    onStatusUpdate("Загрузка метаданных .torrent…")
                    val downloadedBytes = downloadRemoteTorrentBytes(torrentFileUrl)
                    if (downloadedBytes != null) {
                        uploadTorrentBytes(
                            rawBaseUrl = baseUrl,
                            username = username,
                            password = password,
                            bytes = downloadedBytes,
                            fileName = "tracker_release.torrent",
                            title = title,
                            saveToDb = saveToDb
                        ).getOrThrow()
                    } else {
                        val extractedHash = BencodeTorrentParser.extractInfoHash(torrentFileUrl)
                        val fallbackTarget = if (extractedHash != null) {
                            BencodeTorrentParser.buildMagnetUri(extractedHash, title)
                        } else {
                            torrentFileUrl
                        }
                        addTorrent(baseUrl, username, password, fallbackTarget, title, saveToDb).getOrThrow()
                    }
                }

                else -> {
                    val rawMagnet = magnetUri?.takeIf { it.isNotBlank() }
                    val targetLink = if (rawMagnet != null) {
                        BencodeTorrentParser.ensurePlayableOrMagnetLink(rawMagnet, title)
                    } else {
                        torrentFileUrl?.takeIf { it.isNotBlank() }
                            ?: throw IllegalArgumentException("Отсутствует ссылка на торрент")
                    }
                    onStatusUpdate("Передача Magnet-ссылки в TorrServer…")
                    addTorrent(baseUrl, username, password, targetLink, title, saveToDb).getOrThrow()
                }
            }

            if (initialTorrent.files.isNotEmpty()) {
                return@runCatching initialTorrent
            }

            if (initialTorrent.hash.isBlank()) {
                throw IllegalStateException("TorrServer не вернул список файлов торрента")
            }

            // Trigger background /stream?...&stat warmup so TorrServer actively fetches metadata from DHT/peers
            val warmupUrl = "$baseUrl/stream?link=${urlEncode(initialTorrent.hash)}&stat"
            launch {
                runCatching {
                    val req = Request.Builder().url(warmupUrl).get().apply {
                        buildBasicAuthHeader(username, password)?.let { header("Authorization", it) }
                    }.build()
                    httpClient.newCall(req).execute().close()
                }
            }

            var latest = initialTorrent
            for (attempt in 1..22) {
                delay(800L)
                val polled = getTorrent(baseUrl, username, password, initialTorrent.hash, title).getOrNull()
                if (polled != null) {
                    latest = polled
                    if (polled.files.isNotEmpty()) {
                        return@runCatching polled
                    }
                    val peersInfo = if (polled.activePeers > 0 || polled.connectedSeeders > 0) {
                        " (сидов: ${polled.connectedSeeders}, пиров: ${polled.activePeers})"
                    } else {
                        ""
                    }
                    onStatusUpdate("Получение списка файлов от пиров$peersInfo…")
                }
            }

            latest
        }
    }

    fun buildStreamUrl(
        rawBaseUrl: String,
        linkOrHash: String,
        fileIndex: Int,
        fileName: String
    ): String {
        val baseUrl = normalizeBaseUrl(rawBaseUrl)
        val safeName = fileName.substringAfterLast('/').substringAfterLast('\\')
            .ifBlank { "stream.mkv" }
        val encodedName = urlEncodePathSegment(safeName)
        val encodedLink = urlEncode(linkOrHash)
        return "$baseUrl/stream/$encodedName?link=$encodedLink&index=$fileIndex&play"
    }

    fun buildDirectFallbackStreamUrl(
        rawBaseUrl: String,
        linkOrHash: String,
        title: String
    ): String {
        val cleanLink = BencodeTorrentParser.ensurePlayableOrMagnetLink(linkOrHash, title)
        val safeTitle = title.replace(Regex("""[\\/:*?"<>|]"""), "_").take(60).ifBlank { "video" }
        return buildStreamUrl(rawBaseUrl, cleanLink, 1, "$safeTitle.mkv")
    }

    fun buildPlaylistFromTorrent(
        rawBaseUrl: String,
        torrent: TorrServerTorrent,
        selectedVoiceover: String? = null
    ): TorrServerPlaylistBundle {
        val baseUrl = normalizeBaseUrl(rawBaseUrl)
        val allFiles = torrent.files

        val rawVideoFiles = allFiles.filter { MediaAutoMatcher.isVideoFile(it.fileName) }
        val effectiveVideoFiles = if (rawVideoFiles.isNotEmpty()) {
            rawVideoFiles.sortedWith { a, b ->
                MediaAutoMatcher.naturalCompare(a.path, b.path)
            }
        } else {
            // Fallback if video lacks standard extension: pick non-audio/non-sub files > 1 MB
            allFiles
                .filter {
                    !MediaAutoMatcher.isAudioFile(it.fileName) &&
                        !MediaAutoMatcher.isSubtitleFile(it.fileName) &&
                        !it.fileName.endsWith(".txt", ignoreCase = true) &&
                        !it.fileName.endsWith(".nfo", ignoreCase = true) &&
                        !it.fileName.endsWith(".jpg", ignoreCase = true) &&
                        !it.fileName.endsWith(".png", ignoreCase = true)
                }
                .sortedByDescending { it.lengthBytes }
        }

        val audioEntries = allFiles.filter { MediaAutoMatcher.isAudioFile(it.fileName) }
        val subEntries = allFiles.filter { MediaAutoMatcher.isSubtitleFile(it.fileName) }

        val voiceoverGroups = audioEntries
            .map { it.parentFolder.ifBlank { "Внешняя озвучка" } }
            .distinct()
            .sortedWith(MediaAutoMatcher.naturalFileComparator)

        val activeVoiceover = selectedVoiceover?.takeIf { it in voiceoverGroups }

        val filteredAudioEntries = if (activeVoiceover != null) {
            audioEntries.filter { (it.parentFolder.ifBlank { "Внешняя озвучка" }) == activeVoiceover }
        } else {
            emptyList()
        }

        val folderId = "torrserver:${torrent.hash}"
        val folderName = torrent.title.ifBlank { "TorrServer" }

        val episodes = effectiveVideoFiles.map { videoFile ->
            val videoDir = videoFile.path.substringBeforeLast('/', "")
            val parsedInfo = MediaAutoMatcher.parseEpisodeInfo(videoFile.fileName)
            val streamUrl = buildStreamUrl(baseUrl, torrent.hash, videoFile.id, videoFile.fileName)

            val audioCompanions = filteredAudioEntries.map { aud ->
                val audDir = aud.path.substringBeforeLast('/', "")
                RawCompanionFile(
                    uri = buildStreamUrl(baseUrl, torrent.hash, aud.id, aud.fileName),
                    fileName = aud.fileName,
                    parentFolderName = aud.parentFolder.ifBlank { "Внешняя озвучка" },
                    isSameDirectoryAsVideo = audDir.equals(videoDir, ignoreCase = true)
                )
            }

            val subCompanions = subEntries.map { sub ->
                val subDir = sub.path.substringBeforeLast('/', "")
                RawCompanionFile(
                    uri = buildStreamUrl(baseUrl, torrent.hash, sub.id, sub.fileName),
                    fileName = sub.fileName,
                    parentFolderName = sub.parentFolder,
                    isSameDirectoryAsVideo = subDir.equals(videoDir, ignoreCase = true)
                )
            }

            val matchedAudio = MediaAutoMatcher.matchAudioTracksForEpisode(
                videoFileName = videoFile.fileName,
                totalVideosInFolder = effectiveVideoFiles.size,
                candidates = audioCompanions
            ).take(1)

            val matchedSubs = MediaAutoMatcher.matchSubtitleTracksForEpisode(
                videoFileName = videoFile.fileName,
                totalVideosInFolder = effectiveVideoFiles.size,
                candidates = subCompanions
            )

            EpisodeItem(
                uri = streamUrl,
                fileName = videoFile.fileName,
                cleanTitle = parsedInfo.cleanTitle.ifBlank { videoFile.fileName },
                episodeNumber = parsedInfo.episode,
                seasonNumber = parsedInfo.season,
                folderId = folderId,
                folderName = folderName,
                sizeBytes = videoFile.lengthBytes,
                autoAudioTracks = matchedAudio,
                autoSubtitleTracks = matchedSubs
            )
        }

        return TorrServerPlaylistBundle(
            hash = torrent.hash,
            title = folderName,
            episodes = episodes,
            availableVoiceovers = voiceoverGroups,
            selectedVoiceover = activeVoiceover,
            subtitleTrackCount = subEntries.size
        )
    }

    internal fun parseTorrentJson(
        json: JSONObject,
        fallbackHash: String,
        fallbackTitle: String
    ): TorrServerTorrent {
        val hash = json.optString("hash").trim().ifBlank { fallbackHash }.lowercase(Locale.ROOT)
        val rawTitle = json.optString("title").trim()
            .ifBlank { json.optString("name").trim() }
            .ifBlank { fallbackTitle }

        val filesList = mutableListOf<TorrServerFileEntry>()
        val statsArray = json.optJSONArray("file_stats")
            ?: json.optJSONObject("data")?.let { dataObj ->
                // Legacy / nested files fallback
                runCatching { JSONObject(dataObj.toString()).optJSONArray("files") }.getOrNull()
            }

        if (statsArray != null) {
            for (i in 0 until statsArray.length()) {
                val fObj = statsArray.optJSONObject(i) ?: continue
                val id = fObj.optInt("id", i + 1)
                val rawPath = fObj.optString("path").replace('\\', '/').trimStart('/')
                if (rawPath.isBlank()) continue
                val fileName = rawPath.substringAfterLast('/')
                val beforeFile = rawPath.substringBeforeLast('/', "")
                val parentFolder = beforeFile.substringAfterLast('/')
                val length = fObj.optLong("length", 0L)
                filesList.add(
                    TorrServerFileEntry(
                        id = id,
                        path = rawPath,
                        fileName = fileName,
                        parentFolder = parentFolder,
                        lengthBytes = length
                    )
                )
            }
        }

        return TorrServerTorrent(
            hash = hash,
            title = rawTitle,
            stat = json.optInt("stat", 0),
            statString = json.optString("stat_string", ""),
            torrentSize = json.optLong("torrent_size", filesList.sumOf { it.lengthBytes }),
            connectedSeeders = json.optInt("connected_seeders", 0),
            activePeers = json.optInt("active_peers", 0),
            files = filesList
        )
    }

    private fun downloadRemoteTorrentBytes(url: String): ByteArray? {
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", ResilientTrackerNetwork.BROWSER_USER_AGENT)
                .header("Accept", "application/x-bittorrent, application/octet-stream, */*")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8")
                .get()
                .build()
            ResilientTrackerNetwork.client(enableDoh = true).newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bytes = resp.body?.bytes() ?: return@use null
                if (bytes.isEmpty() || bytes.size > 5 * 1024 * 1024) return@use null
                if (bytes[0] != 'd'.code.toByte()) return@use null
                bytes
            }
        }.getOrNull()
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun urlEncodePathSegment(segment: String): String =
        URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
}
