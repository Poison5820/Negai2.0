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
    val qualityBadge: String? = detectQualityBadge(title),
    val voiceovers: List<String> = emptyList()
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

        private val STUDIO_VOICEOVER_REGEX = Regex(
            """(?iu)(?<![\p{L}\p{N}])(anilibria(?:\.tv)?|anidub|studio\s*band|студийная\s*банда|crunchyroll|wakanim|shiza(?:\s*project)?|dream\s*cast|dreamcast|anistar|jam(?:\s*club)?|sovetromantica|anicake|animedia|reanimedia|реанимедиа|flarrow\s*films|yakuji|kansai|hdrezka|резка|red\s*head\s*sound|rhs|jaskier|яскьер|lostfilm|лостфильм|кубик\s*в\s*кубе|kubik(?:\s*v\s*kube)?|tvshows|newstudio|baibako|alexfilm|coldfilm|viruseproject|rudub|ультрадокс|omskbird|pifagor|пифагор|мосфильм|невафильм|кириллица|св-дубль|амедиа|amedia|animaunt|animevost|onwave|youkai\s*studio)(?![\p{L}\p{N}])"""
        )

        private val VOICEOVER_TYPE_PATTERNS: List<Pair<Regex, String>> = listOf(
            Regex("""(?iu)(?<![\p{L}\p{N}])(дубляж|дублирован\p{L}*|dub)(?![\p{L}\p{N}])""") to "Дубляж / Dub",
            Regex("""(?iu)(?<![\p{L}\p{N}])(профессиональный\s+многоголос\p{L}*|пм)(?![\p{L}\p{N}])""") to "ПМ (Проф. многоголосый)",
            Regex("""(?iu)(?<![\p{L}\p{N}])(профессиональный\s+двухголос\p{L}*|пд)(?![\p{L}\p{N}])""") to "ПД (Проф. двухголосый)",
            Regex("""(?iu)(?<![\p{L}\p{N}])(любительский\s+многоголос\p{L}*|лм)(?![\p{L}\p{N}])""") to "ЛМ (Люб. многоголосый)",
            Regex("""(?iu)(?<![\p{L}\p{N}])(любительский\s+двухголос\p{L}*|лд)(?![\p{L}\p{N}])""") to "ЛД (Люб. двухголосый)",
            Regex("""(?iu)(?<![\p{L}\p{N}])(любительский\s+одноголос\p{L}*|ло)(?![\p{L}\p{N}])""") to "ЛО (Люб. одноголосый)",
            Regex("""(?iu)(?<![\p{L}\p{N}])(авторский\p{L}*|ап|avo)(?![\p{L}\p{N}])""") to "Авторский (AVO)",
            Regex("""(?iu)(?<![\p{L}\p{N}])(многоголос\p{L}*|mvo)(?![\p{L}\p{N}])""") to "Многоголосый (MVO)",
            Regex("""(?iu)(?<![\p{L}\p{N}])(двухголос\p{L}*|dvo)(?![\p{L}\p{N}])""") to "Двухголосый (DVO)",
            Regex("""(?iu)(?<![\p{L}\p{N}])(одноголос\p{L}*|vo|so)(?![\p{L}\p{N}])""") to "Одноголосый (VO)",
            Regex("""(?iu)rus\s*\(\s*int\s*\)""") to "RUS (встроенная озвучка)",
            Regex("""(?iu)rus\s*\(\s*ext\s*\)""") to "RUS (внешняя озвучка)",
            Regex("""(?iu)(?<![\p{L}\p{N}])(\d+\s*[xх]\s*rus|rus|rus\s*sound|русская\s+озвучка|озвучка)(?![\p{L}\p{N}])""") to "Русская озвучка (RUS)",
            Regex("""(?iu)(?<![\p{L}\p{N}])(лицензия|itunes|чистый\s+звук)(?![\p{L}\p{N}])""") to "Лицензия",
            Regex("""(?iu)(?<![\p{L}\p{N}])(original|eng|jap|jpn)(?![\p{L}\p{N}])""") to "Original"
        )

        private val SUBTITLE_PATTERNS: List<Pair<Regex, String>> = listOf(
            Regex("""(?iu)(?<![\p{L}\p{N}])(rus\s*sub\p{L}*|русские\s+субтитры)(?![\p{L}\p{N}])""") to "Русские субтитры",
            Regex("""(?iu)(?<![\p{L}\p{N}])(eng\s*sub\p{L}*|английские\s+субтитры)(?![\p{L}\p{N}])""") to "Английские субтитры",
            Regex("""(?iu)(?<![\p{L}\p{N}])(multi\s*sub\p{L}*)(?![\p{L}\p{N}])""") to "MultiSub",
            Regex("""(?iu)(?<![\p{L}\p{N}])(ass|srt)(?![\p{L}\p{N}])""") to "ASS / SRT",
            Regex("""(?iu)(?<![\p{L}\p{N}])(субтитры|subtitles|subs|sub|ст)(?![\p{L}\p{N}])""") to "Субтитры / Sub"
        )

        private val DESCRIPTION_VOICE_LINE_REGEX = Regex(
            """(?im)^\s*(?:перевод|озвучка|озвучивание|роли\s+озвучивали|аудио\s*\d*|audio\s*\d*)\s*:\s*(.+)$"""
        )

        private val DESCRIPTION_SUB_LINE_REGEX = Regex(
            """(?im)^\s*(?:субтитры|subtitles|вид\s+субтитров)\s*:\s*(.+)$"""
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

        private fun normalizeStudioName(raw: String): String {
            val compact = raw.trim().replace(Regex("""\s+"""), " ")
            return when (compact.lowercase(Locale.forLanguageTag("ru"))) {
                "anilibria", "anilibria.tv" -> "AniLibria"
                "anidub" -> "AniDUB"
                "студийная банда", "studio band" -> "Студийная Банда"
                "shiza", "shiza project" -> "SHIZA Project"
                "dream cast", "dreamcast" -> "Dream Cast"
                "anistar" -> "AniStar"
                "jam", "jam club" -> "Jam Club"
                "sovetromantica" -> "SovetRomantica"
                "reanimedia", "реанимедиа" -> "Реанимедиа"
                "flarrow films" -> "Flarrow Films"
                "кубик в кубе", "kubik", "kubik v kube" -> "Кубик в Кубе"
                "резка", "hdrezka" -> "HDRezka"
                "red head sound", "rhs" -> "Red Head Sound"
                "лостфильм", "lostfilm" -> "LostFilm"
                "яскьер", "jaskier" -> "Jaskier"
                "пифагор", "pifagor" -> "Пифагор"
                "амедиа", "amedia" -> "Амедиа"
                else -> compact.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }

        private fun isSubtitleOnlyToken(token: String): Boolean {
            val l = token.trim().lowercase(Locale.forLanguageTag("ru"))
            return l == "sub" || l == "subs" || l == "subtitles" ||
                l == "субтитры" || l == "субтитры / sub" || l == "ст" || l == "ass" || l == "srt"
        }

        fun extractVoiceoverTags(
            title: String,
            extraVoices: List<String> = emptyList(),
            descriptionText: String? = null
        ): List<String> {
            val found = linkedSetOf<String>()

            // 1. Explicit voices from JacRed API or tracker metadata
            for (v in extraVoices) {
                val cleaned = v.trim()
                if (cleaned.isNotEmpty() && !isSubtitleOnlyToken(cleaned)) {
                    found.add(cleaned)
                }
            }

            // 2. Named studios in title
            for (match in STUDIO_VOICEOVER_REGEX.findAll(title)) {
                found.add(normalizeStudioName(match.value))
            }

            // 3. Voiceover types & Russian audio markers in title
            for ((regex, label) in VOICEOVER_TYPE_PATTERNS) {
                if (regex.containsMatchIn(title)) {
                    if (label == "Русская озвучка (RUS)" &&
                        (found.contains("RUS (встроенная озвучка)") || found.contains("RUS (внешняя озвучка)"))
                    ) {
                        continue
                    }
                    found.add(label)
                }
            }

            // 4. Parse description text for studios and explicit Перевод / Озвучка / Аудио lines
            if (!descriptionText.isNullOrBlank()) {
                for (match in STUDIO_VOICEOVER_REGEX.findAll(descriptionText)) {
                    found.add(normalizeStudioName(match.value))
                }
                for ((regex, label) in VOICEOVER_TYPE_PATTERNS) {
                    if (label != "Original" && label != "Русская озвучка (RUS)" && regex.containsMatchIn(descriptionText)) {
                        found.add(label)
                    }
                }
                for (match in DESCRIPTION_VOICE_LINE_REGEX.findAll(descriptionText)) {
                    val value = match.groupValues.getOrNull(1)?.trim().orEmpty()
                    if (value.isNotEmpty() &&
                        !value.equals("нет", ignoreCase = true) &&
                        !value.equals("отсутствует", ignoreCase = true) &&
                        !value.equals("none", ignoreCase = true) &&
                        !isSubtitleOnlyToken(value)
                    ) {
                        val clipped = value.take(95).trimEnd(',', ';', '.')
                        if (clipped.isNotEmpty()) {
                            found.add(clipped)
                        }
                    }
                }
            }

            return found.toList()
        }

        fun extractSubtitleTags(
            title: String,
            descriptionText: String? = null
        ): List<String> {
            val found = linkedSetOf<String>()
            for ((regex, label) in SUBTITLE_PATTERNS) {
                if (regex.containsMatchIn(title)) {
                    if (label == "Субтитры / Sub" &&
                        (found.contains("Русские субтитры") || found.contains("Английские субтитры"))
                    ) {
                        continue
                    }
                    found.add(label)
                }
            }

            if (!descriptionText.isNullOrBlank()) {
                for (match in DESCRIPTION_SUB_LINE_REGEX.findAll(descriptionText)) {
                    val value = match.groupValues.getOrNull(1)?.trim().orEmpty()
                    if (value.isNotEmpty() &&
                        !value.equals("нет", ignoreCase = true) &&
                        !value.equals("отсутствуют", ignoreCase = true) &&
                        !value.equals("none", ignoreCase = true)
                    ) {
                        val clipped = value.take(75).trimEnd(',', ';', '.')
                        if (clipped.isNotEmpty()) {
                            found.add(clipped)
                        }
                    }
                }
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
    val parsedVoiceoverTags: List<String> = TorrentSearchResult.extractVoiceoverTags(result.title, result.voiceovers),
    val parsedSubtitleTags: List<String> = TorrentSearchResult.extractSubtitleTags(result.title),
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
