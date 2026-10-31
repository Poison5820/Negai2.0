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
    }
}

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
