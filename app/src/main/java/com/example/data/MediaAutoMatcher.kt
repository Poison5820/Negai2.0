package com.example.data

import com.example.model.ExternalAudioTrack
import com.example.model.ExternalSubtitleTrack
import com.example.model.SubtitleFormat
import java.util.Locale

data class RawCompanionFile(
    val uri: String,
    val fileName: String,
    val parentFolderName: String,
    val isSameDirectoryAsVideo: Boolean
)

data class ParsedEpisodeInfo(
    val stem: String,
    val normalizedStem: String,
    val cleanTitle: String,
    val season: Int?,
    val episode: Int?
)

object MediaAutoMatcher {

    val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "m4v", "ts", "m2ts", "3gp", "flv"
    )

    val AUDIO_EXTENSIONS = setOf(
        "mka", "mp3", "aac", "m4a", "flac", "ogg", "opus", "wav", "ac3", "eac3", "dts"
    )

    val SUBTITLE_EXTENSIONS = setOf(
        "srt", "ass", "ssa", "vtt"
    )

    // Strip technical tags so numbers inside "1080p", "x264", "265", "10bit", "[A1B2C3D4]" are never confused with episode numbers.
    private val TECH_NOISE_REGEX = Regex(
        """(?i)(\[[0-9a-f]{8}\]|\b(?:2160p|1440p|1080p|720p|576p|480p|360p|4k|8k|x264|x265|h\.?264|h\.?265|hevc|avc|av1|vp9|10bit|8bit|hi10p|aac\d*(?:\.\d+)?|flac|ac3|eac3|mp3|opus|bdrip|bdremux|bluray|web-dl|webrip|hdrip|dvdrip|hdr10\+?|dolby|5\.1|7\.1|2\.0|60fps|24fps)\b)"""
    )

    private val SEASON_EPISODE_SXXEXX = Regex("""(?i)\bS(\d{1,2})\s*E(\d{1,4})(?:v\d+)?\b""")
    private val SEASON_EPISODE_X = Regex("""(?i)\b(\d{1,2})x(\d{1,4})(?:v\d+)?\b""")
    private val EXPLICIT_EP_WORD = Regex(
        """(?i)(?:^|[\s._\-\[\(])(?:ep\.?|episode|e|серия|сер\.?|выпуск|ova|спэшл)\s*(\d{1,4})(?:v\d+)?(?=$|[\s._\-\]\)])"""
    )
    private val ANIME_DASH_EP = Regex(
        """(?:[-–—])\s*(\d{1,4})(?:v\d+)?(?=$|[\s._\[\(])"""
    )
    private val BRACKET_EP = Regex("""\[(\d{1,3})(?:v\d+)?\]""")
    private val LEADING_OR_STANDALONE_EP = Regex("""(?:^|[\s._])0*(\d{1,3})(?:v\d+)?(?=$|[\s._\-\[\(])""")

    fun extensionOf(fileName: String): String =
        fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)

    fun isVideoFile(fileName: String): Boolean = extensionOf(fileName) in VIDEO_EXTENSIONS
    fun isAudioFile(fileName: String): Boolean = extensionOf(fileName) in AUDIO_EXTENSIONS
    fun isSubtitleFile(fileName: String): Boolean = extensionOf(fileName) in SUBTITLE_EXTENSIONS

    fun subtitleFormatOf(fileName: String): SubtitleFormat = when (extensionOf(fileName)) {
        "ass", "ssa" -> SubtitleFormat.ASS
        "vtt" -> SubtitleFormat.VTT
        else -> SubtitleFormat.SRT
    }

    fun formatResolutionBadge(width: Int, height: Int, fileName: String): String? =
        com.example.model.VideoResolutionFormatter.formatBadge(width, height, fileName)

    fun parseResolutionTag(fileName: String): String? =
        com.example.model.VideoResolutionFormatter.parseTagFromFileName(fileName)

    fun parseEpisodeInfo(fileName: String): ParsedEpisodeInfo {
        val stem = fileName.substringBeforeLast('.', fileName).trim()
        val scrubbed = TECH_NOISE_REGEX.replace(stem, " ")

        var season: Int? = null
        var episode: Int? = null

        SEASON_EPISODE_SXXEXX.find(scrubbed)?.let { m ->
            season = m.groupValues[1].toIntOrNull()
            episode = m.groupValues[2].toIntOrNull()
        }

        if (episode == null) {
            SEASON_EPISODE_X.find(scrubbed)?.let { m ->
                season = m.groupValues[1].toIntOrNull()
                episode = m.groupValues[2].toIntOrNull()
            }
        }

        if (episode == null) {
            EXPLICIT_EP_WORD.find(scrubbed)?.let { m ->
                episode = m.groupValues[1].toIntOrNull()
            }
        }

        if (episode == null) {
            ANIME_DASH_EP.findAll(scrubbed).lastOrNull()?.let { m ->
                val candidate = m.groupValues[1].toIntOrNull()
                if (candidate != null && candidate !in 1950..2035) {
                    episode = candidate
                }
            }
        }

        if (episode == null) {
            BRACKET_EP.findAll(scrubbed).lastOrNull()?.let { m ->
                episode = m.groupValues[1].toIntOrNull()
            }
        }

        if (episode == null) {
            LEADING_OR_STANDALONE_EP.findAll(scrubbed)
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .lastOrNull { it in 1..999 && it !in 1950..2035 }
                ?.let { episode = it }
        }

        val cleanTitle = stem
            .replace(Regex("""^\[[^\]]+\]\s*"""), "")
            .replace(TECH_NOISE_REGEX, "")
            .replace('_', ' ')
            .replace(Regex("""\s{2,}"""), " ")
            .trim(' ', '-', '.', '_', '(', ')', '[', ']')
            .ifBlank { stem }

        val normalizedStem = stem
            .lowercase(Locale.ROOT)
            .replace(Regex("""[\s._\-\[\]\(\)]+"""), " ")
            .trim()

        return ParsedEpisodeInfo(
            stem = stem,
            normalizedStem = normalizedStem,
            cleanTitle = cleanTitle,
            season = season,
            episode = episode
        )
    }

    /**
     * Matches companion external audio files to a video episode based on:
     * 1. Exact or prefix filename stem match (e.g. `Movie.mkv` -> `Movie.rus.mka`)
     * 2. Episode & season number match within the same folder or companion audio subfolders
     * 3. Single-video folder fallback (when a movie folder has 1 video and external audio tracks)
     */
    fun matchAudioTracksForEpisode(
        videoFileName: String,
        totalVideosInFolder: Int,
        candidates: List<RawCompanionFile>
    ): List<ExternalAudioTrack> {
        if (candidates.isEmpty()) return emptyList()
        val videoInfo = parseEpisodeInfo(videoFileName)

        return candidates.mapNotNull { candidate ->
            val score = computeMatchScore(
                videoInfo = videoInfo,
                candidateFileName = candidate.fileName,
                isSameDirectory = candidate.isSameDirectoryAsVideo,
                totalVideosInFolder = totalVideosInFolder
            )
            if (score > 0) {
                val ext = extensionOf(candidate.fileName)
                val label = extractReadableTrackLabel(videoInfo.stem, candidate.fileName)
                score to ExternalAudioTrack(
                    uri = candidate.uri,
                    displayName = label,
                    sourceFolderName = if (candidate.isSameDirectoryAsVideo) "" else candidate.parentFolderName,
                    extension = ext,
                    isAutoMatched = true
                )
            } else {
                null
            }
        }
            .sortedWith(compareByDescending<Pair<Int, ExternalAudioTrack>> { it.first }
                .thenComparator { a, b -> naturalCompare(a.second.formattedLabel, b.second.formattedLabel) })
            .map { it.second }
    }

    /**
     * Matches companion external SRT/ASS/VTT subtitle files to a video episode.
     */
    fun matchSubtitleTracksForEpisode(
        videoFileName: String,
        totalVideosInFolder: Int,
        candidates: List<RawCompanionFile>
    ): List<ExternalSubtitleTrack> {
        if (candidates.isEmpty()) return emptyList()
        val videoInfo = parseEpisodeInfo(videoFileName)

        return candidates.mapNotNull { candidate ->
            val score = computeMatchScore(
                videoInfo = videoInfo,
                candidateFileName = candidate.fileName,
                isSameDirectory = candidate.isSameDirectoryAsVideo,
                totalVideosInFolder = totalVideosInFolder
            )
            if (score > 0) {
                val label = extractReadableTrackLabel(videoInfo.stem, candidate.fileName)
                score to ExternalSubtitleTrack(
                    uri = candidate.uri,
                    displayName = label,
                    sourceFolderName = if (candidate.isSameDirectoryAsVideo) "" else candidate.parentFolderName,
                    format = subtitleFormatOf(candidate.fileName),
                    isAutoMatched = true
                )
            } else {
                null
            }
        }
            .sortedWith(compareByDescending<Pair<Int, ExternalSubtitleTrack>> { it.first }
                .thenComparator { a, b -> naturalCompare(a.second.formattedLabel, b.second.formattedLabel) })
            .map { it.second }
    }

    private fun computeMatchScore(
        videoInfo: ParsedEpisodeInfo,
        candidateFileName: String,
        isSameDirectory: Boolean,
        totalVideosInFolder: Int
    ): Int {
        val candInfo = parseEpisodeInfo(candidateFileName)
        val dirBonus = if (isSameDirectory) 5 else 0

        // 1. Exact stem match (`Show - 01.mkv` == `Show - 01.mka`)
        if (candInfo.stem.equals(videoInfo.stem, ignoreCase = true)) {
            return 100 + dirBonus
        }

        // 2. Prefix stem match (`Show - 01.mkv` -> `Show - 01.RUS.AniLibria.mka`)
        if (candInfo.stem.startsWith(videoInfo.stem, ignoreCase = true)) {
            val nextChar = candInfo.stem.getOrNull(videoInfo.stem.length)
            if (nextChar == null || nextChar in charArrayOf('.', '_', '-', ' ', '[', '(')) {
                return 95 + dirBonus
            }
        }

        // 3. Episode number match (e.g. `[SubsPlease] Frieren - 03 (1080p).mkv` <-> `RUS Sound/Frieren_ep03.mka`)
        if (videoInfo.episode != null && candInfo.episode != null) {
            val seasonsCompatible = videoInfo.season == null ||
                candInfo.season == null ||
                videoInfo.season == candInfo.season
            if (seasonsCompatible && videoInfo.episode == candInfo.episode) {
                return 80 + dirBonus
            }
            // Different episode numbers -> definitely NOT for this episode!
            return 0
        }

        // 4. Single-movie folder: if there is only 1 video in the folder and the audio/sub has no conflicting episode number
        if (totalVideosInFolder == 1 && candInfo.episode == null) {
            return 60 + dirBonus
        }

        return 0
    }

    private fun extractReadableTrackLabel(videoStem: String, candidateFileName: String): String {
        val candStem = candidateFileName.substringBeforeLast('.', candidateFileName)
        if (candStem.startsWith(videoStem, ignoreCase = true) && candStem.length > videoStem.length) {
            val suffix = candStem.substring(videoStem.length)
                .trim(' ', '.', '_', '-', '[', ']', '(', ')')
            if (suffix.isNotBlank()) return suffix
        }
        return candStem
    }

    /**
     * Natural alphanumeric comparator so "Episode 2" sorts before "Episode 10".
     */
    val naturalFileComparator: Comparator<String> = Comparator { a, b -> naturalCompare(a, b) }

    fun naturalCompare(a: String, b: String): Int {
        var ia = 0
        var ib = 0
        val na = a.length
        val nb = b.length

        while (ia < na && ib < nb) {
            val ca = a[ia]
            val cb = b[ib]
            if (ca.isDigit() && cb.isDigit()) {
                var za = ia
                while (za < na && a[za] == '0') za++
                var ea = za
                while (ea < na && a[ea].isDigit()) ea++

                var zb = ib
                while (zb < nb && b[zb] == '0') zb++
                var eb = zb
                while (eb < nb && b[eb].isDigit()) eb++

                val lenA = ea - za
                val lenB = eb - zb
                if (lenA != lenB) return lenA.compareTo(lenB)

                for (k in 0 until lenA) {
                    val diff = a[za + k].compareTo(b[zb + k])
                    if (diff != 0) return diff
                }
                val zeroDiff = (za - ia).compareTo(zb - ib)
                if (zeroDiff != 0) return zeroDiff

                ia = ea
                ib = eb
            } else {
                val diff = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (diff != 0) return diff
                ia++
                ib++
            }
        }
        return (na - ia).compareTo(nb - ib)
    }
}
