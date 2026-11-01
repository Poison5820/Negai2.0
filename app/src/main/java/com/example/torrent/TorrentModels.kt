package com.example.torrent

import com.example.model.EpisodeItem
import java.util.Locale

enum class TorrentTrackerGroup(
    val id: String,
    val label: String,
    val priorityRank: Int
) {
    ALL("ALL", "Все", 0),
    RUTRACKER("RUTRACKER", "RuTracker", 1),
    RUTOR("RUTOR", "Rutor", 2),
    KINOZAL("KINOZAL", "Kinozal", 3),
    NNMCLUB("NNMCLUB", "NNM-Club", 4),
    ANIME("ANIME", "Аниме", 5),
    OTHER("OTHER", "Другие", 6);

    companion object {
        fun classify(rawTrackerName: String): TorrentTrackerGroup {
            val lower = rawTrackerName.trim().lowercase(Locale.ROOT)
            return when {
                lower.contains("rutracker") || lower.contains("рутрекер") -> RUTRACKER
                lower.contains("rutor") || lower.contains("рутор") -> RUTOR
                lower.contains("kinozal") || lower.contains("кинозал") -> KINOZAL
                lower.contains("nnm") || lower.contains("noname") -> NNMCLUB
                lower.contains("anilibria") ||
                    lower.contains("nyaa") ||
                    lower.contains("anidub") ||
                    lower.contains("animelayer") ||
                    lower.contains("erai") -> ANIME
                else -> OTHER
            }
        }

        fun formatDisplayTracker(rawTrackerName: String): String {
            val trimmed = rawTrackerName.trim()
            if (trimmed.isEmpty()) return "Tracker"
            val lower = trimmed.lowercase(Locale.ROOT)
            return when {
                lower.contains("rutracker") -> "RuTracker"
                lower.contains("rutor") -> "Rutor"
                lower.contains("kinozal") -> "Kinozal"
                lower.contains("nnm") -> "NNM-Club"
                lower.contains("selezen") -> "Selezen"
                lower.contains("anilibria") -> "AniLibria"
                lower.contains("nyaa") -> "Nyaa"
                lower.contains("megapeer") -> "MegaPeer"
                lower.contains("bitru") -> "Bitru"
                lower.contains("toloka") -> "Toloka"
                lower.contains("lostfilm") -> "LostFilm"
                else -> trimmed.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
            }
        }
    }
}

enum class TorrentSortMode(
    val id: String,
    val title: String,
    val shortLabel: String
) {
    PRIORITY("PRIORITY", "Приоритет (RuTracker / Rutor)", "Приоритет"),
    SEEDERS("SEEDERS", "По количеству сидов", "Сиды"),
    SIZE("SIZE", "По размеру раздачи", "Размер"),
    DATE("DATE", "По дате добавления", "Новые");

    companion object {
        fun fromId(id: String): TorrentSortMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PRIORITY
    }
}

enum class TorrentQualityFilter(
    val id: String,
    val label: String
) {
    ALL("ALL", "Все качества"),
    UHD_4K("UHD_4K", "4K / 2160p"),
    FHD_1080("FHD_1080", "1080p"),
    HD_720("HD_720", "720p"),
    HEVC_HDR("HEVC_HDR", "HEVC / HDR");

    fun matches(result: TorrentSearchResult): Boolean {
        if (this == ALL) return true
        val hay = "${result.title} ${result.qualityBadge.orEmpty()}".lowercase(Locale.ROOT)
        return when (this) {
            ALL -> true
            UHD_4K -> hay.contains("2160p") || hay.contains("4k") || hay.contains("uhd")
            FHD_1080 -> hay.contains("1080p") || hay.contains("1080i") || hay.contains("fhd")
            HD_720 -> hay.contains("720p")
            HEVC_HDR -> hay.contains("hevc") || hay.contains("x265") || hay.contains("h265") ||
                hay.contains("h.265") || hay.contains("10bit") || hay.contains("10-bit") ||
                hay.contains("hdr") || hay.contains("dolby vision") || hay.contains("dovi")
        }
    }
}

data class TorrentSearchResult(
    val id: String,
    val title: String,
    val trackerName: String,
    val trackerGroup: TorrentTrackerGroup,
    val magnetUri: String?,
    val torrentFileUrl: String?,
    val topicUrl: String?,
    val infoHash: String?,
    val sizeBytes: Long,
    val sizeLabel: String,
    val seeders: Int,
    val leechers: Int,
    val publishedLabel: String = "",
    val publishedEpochMs: Long = 0L,
    val qualityBadge: String? = detectQualityBadge(title)
) {
    val primaryLink: String?
        get() = magnetUri?.takeIf { it.isNotBlank() } ?: torrentFileUrl?.takeIf { it.isNotBlank() }

    companion object {
        private val QUALITY_4K = Regex("""(?i)\b(2160p|4k|uhd)\b""")
        private val QUALITY_1080 = Regex("""(?i)\b(1080p|1080i)\b""")
        private val QUALITY_720 = Regex("""(?i)\b720p\b""")
        private val CODEC_HEVC = Regex("""(?i)\b(hevc|x265|h\.?265|10-?bit|hdr10\+?|dovi|dolby\s*vision)\b""")
        private val SOURCE_REMUX = Regex("""(?i)\b(bdremux|remux|bluray|blu-ray|bdrip)\b""")

        fun detectQualityBadge(title: String): String? {
            val parts = mutableListOf<String>()
            when {
                QUALITY_4K.containsMatchIn(title) -> parts.add("4K")
                QUALITY_1080.containsMatchIn(title) -> parts.add("1080p")
                QUALITY_720.containsMatchIn(title) -> parts.add("720p")
            }
            if (SOURCE_REMUX.containsMatchIn(title)) {
                if (title.contains("remux", ignoreCase = true)) {
                    parts.add("Remux")
                } else {
                    parts.add("BDRip")
                }
            }
            if (CODEC_HEVC.containsMatchIn(title)) {
                if (title.contains("hdr", ignoreCase = true) || title.contains("dovi", ignoreCase = true)) {
                    parts.add("HDR")
                } else {
                    parts.add("HEVC")
                }
            }
            return if (parts.isEmpty()) null else parts.joinToString(" • ")
        }

        private val TECH_TOKEN_REGEXES = listOf(
            Regex("""(?i)\b(2160p|4k|uhd|1080p|1080i|720p|480p)\b""") to { m: String -> m.uppercase(Locale.ROOT) },
            Regex("""(?i)\b(bdremux|remux|blu-?ray|bdrip|web-?dl|webrip|hdrip|tvrip|dvb)\b""") to { m: String ->
                when (m.lowercase(Locale.ROOT).replace("-", "")) {
                    "bdremux", "remux" -> "BDRemux"
                    "bluray" -> "Blu-Ray"
                    "bdrip" -> "BDRip"
                    "webdl" -> "WEB-DL"
                    "webrip" -> "WEBRip"
                    "hdrip" -> "HDRip"
                    else -> m.uppercase(Locale.ROOT)
                }
            },
            Regex("""(?i)\b(hevc|x265|h\.?265|av1|avc|x264|h\.?264)\b""") to { m: String ->
                when (m.lowercase(Locale.ROOT).replace(".", "")) {
                    "hevc", "x265", "h265" -> "HEVC / H.265"
                    "avc", "x264", "h264" -> "AVC / H.264"
                    "av1" -> "AV1"
                    else -> m.uppercase(Locale.ROOT)
                }
            },
            Regex("""(?i)\b(dolby\s*vision|dovi|hdr10\+|hdr10|hdr|10-?bit|8-?bit)(?![\p{L}\p{N}])""") to { m: String ->
                val l = m.lowercase(Locale.ROOT)
                when {
                    l.contains("dolby") || l == "dovi" -> "Dolby Vision"
                    l == "hdr10+" -> "HDR10+"
                    l.startsWith("hdr") -> m.uppercase(Locale.ROOT)
                    l.contains("10") -> "10-bit"
                    else -> "8-bit"
                }
            },
            Regex("""(?i)\b(truehd|atmos|dts-?hd(?:\s*ma)?|dts|ddp?\s*5\.1|ac-?3|eac-?3|flac|aac|opus|mp3)\b""") to { m: String ->
                m.uppercase(Locale.ROOT).replace(Regex("""\s+"""), " ")
            }
        )

        private val VOICEOVER_REGEX = Regex(
            """(?iu)(?<![\p{L}\p{N}])(anilibria|anidub|studio\s*band|студийная\s*банда|crunchyroll|wakanim|shiza|jam|sovetromantica|anicake|animedia|yakuji|kansai|hdrezka|резка|red\s*head\s*sound|rhs|jaskier|яскьер|lostfilm|лостфильм|кубик\s*в\s*кубе|kubik|tvshows|newstudio|baibako|alexfilm|coldfilm|omskbird|pifagor|пифагор|дубляж|dub|mvo|dvo|avo|sub|субтитры|original)(?![\p{L}\p{N}])"""
        )

        private val EPISODE_INFO_REGEX = Regex(
            """(?iu)(\bS\d{1,2}(?:E\d{1,3}(?:-E?\d{1,3})?)?\b|\[\d{1,3}\s*(?:из|of|-)\s*\d{1,3}[^\]]*\]|\(\d{1,3}\s*(?:из|of|-)\s*\d{1,3}[^)]*\)|(?<![\p{L}\p{N}])\d{1,2}\s*сезон(?![\p{L}\p{N}])|(?<![\p{L}\p{N}])\d{1,3}\s*сери[ияй](?![\p{L}\p{N}]))"""
        )

        fun extractTechTags(title: String): List<String> {
            val found = linkedSetOf<String>()
            for ((regex, formatter) in TECH_TOKEN_REGEXES) {
                for (match in regex.findAll(title)) {
                    found.add(formatter(match.value))
                }
            }
            return found.toList()
        }

        fun extractVoiceoverTags(title: String): List<String> {
            val found = linkedSetOf<String>()
            for (match in VOICEOVER_REGEX.findAll(title)) {
                val raw = match.value.trim().replace(Regex("""\s+"""), " ")
                val normalized = when (raw.lowercase(Locale.forLanguageTag("ru"))) {
                    "дубляж", "dub" -> "Дубляж / Dub"
                    "субтитры", "sub" -> "Субтитры / Sub"
                    "студийная банда", "studio band" -> "Студийная Банда"
                    "кубик в кубе", "kubik" -> "Кубик в Кубе"
                    "резка", "hdrezka" -> "HDRezka"
                    "лостфильм", "lostfilm" -> "LostFilm"
                    "яскьер", "jaskier" -> "Jaskier"
                    "пифагор", "pifagor" -> "Пифагор"
                    else -> raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                }
                found.add(normalized)
            }
            return found.toList()
        }

        fun extractEpisodeInfo(title: String): String? {
            val matches = EPISODE_INFO_REGEX.findAll(title)
                .map { it.value.trim('[', ']', '(', ')', ' ') }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()
            return if (matches.isEmpty()) null else matches.joinToString(" • ")
        }
    }
}

data class TorrentReleaseFileEntry(
    val path: String,
    val sizeLabel: String = ""
)

data class TorrentReleaseDetails(
    val result: TorrentSearchResult,
    val isLoading: Boolean = false,
    val parsedTechTags: List<String> = TorrentSearchResult.extractTechTags(result.title),
    val parsedVoiceoverTags: List<String> = TorrentSearchResult.extractVoiceoverTags(result.title),
    val parsedEpisodeInfo: String? = TorrentSearchResult.extractEpisodeInfo(result.title),
    val descriptionText: String = "",
    val fileEntries: List<TorrentReleaseFileEntry> = emptyList(),
    val infoNote: String? = null
)

data class TorrServerFileEntry(
    val id: Int,
    val path: String,
    val fileName: String,
    val parentFolder: String,
    val lengthBytes: Long
)

data class TorrServerTorrent(
    val hash: String,
    val title: String,
    val stat: Int = 0,
    val statString: String = "",
    val torrentSize: Long = 0L,
    val connectedSeeders: Int = 0,
    val activePeers: Int = 0,
    val files: List<TorrServerFileEntry> = emptyList()
)

data class TorrServerPlaylistBundle(
    val hash: String,
    val title: String,
    val episodes: List<EpisodeItem>,
    val availableVoiceovers: List<String>,
    val selectedVoiceover: String?,
    val subtitleTrackCount: Int
)

sealed interface TorrServerConnectionState {
    data object Idle : TorrServerConnectionState
    data object Checking : TorrServerConnectionState
    data class Connected(val version: String, val baseUrl: String) : TorrServerConnectionState
    data class Disconnected(val reason: String, val baseUrl: String) : TorrServerConnectionState
}

sealed interface TorrentInspectorState {
    data object Hidden : TorrentInspectorState

    data class Loading(
        val title: String,
        val statusText: String,
        val magnetOrLink: String?,
        val torrentFileUrl: String?,
        val canForceDirectStream: Boolean = true
    ) : TorrentInspectorState

    data class Ready(
        val torrent: TorrServerTorrent,
        val bundle: TorrServerPlaylistBundle,
        val magnetOrLink: String?,
        val torrentFileUrl: String?
    ) : TorrentInspectorState

    data class Error(
        val title: String,
        val message: String,
        val magnetOrLink: String?,
        val torrentFileUrl: String?
    ) : TorrentInspectorState
}
