package com.example.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Immutable
import com.example.model.EpisodeItem
import com.example.model.VideoFolderItem
import com.example.player.VideoChapterExtractor
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatTimeMs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Immutable
data class VideoStreamTechnicalInfo(
    val codecName: String,
    val mimeType: String,
    val profileLevel: String,
    val width: Int,
    val height: Int,
    val resolutionBadge: String,
    val aspectRatioLabel: String,
    val frameRateLabel: String,
    val colorAndHdrLabel: String,
    val bitrateLabel: String
)

@Immutable
data class AudioStreamTechnicalInfo(
    val index: Int,
    val title: String,
    val codecName: String,
    val channelsLabel: String,
    val sampleRateLabel: String,
    val bitrateLabel: String,
    val language: String,
    val isExternal: Boolean
)

@Immutable
data class SubtitleStreamTechnicalInfo(
    val index: Int,
    val title: String,
    val formatName: String,
    val language: String,
    val isExternal: Boolean
)

@Immutable
data class DetailedMediaFileInfo(
    val isFolder: Boolean = false,
    val displayName: String,
    val fileName: String,
    val fullPath: String,
    val containerFormat: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val durationMs: Long,
    val durationFormatted: String,
    val overallBitrateLabel: String,
    val lastModifiedFormatted: String,
    val episodeCount: Int = 1,
    val watchedCount: Int = 0,
    val chaptersCount: Int = 0,
    val videoStream: VideoStreamTechnicalInfo? = null,
    val audioStreams: List<AudioStreamTechnicalInfo> = emptyList(),
    val subtitleStreams: List<SubtitleStreamTechnicalInfo> = emptyList()
)

object MediaFileInfoExtractor {

    fun extractEpisodeInfo(context: Context, episode: EpisodeItem): DetailedMediaFileInfo {
        val localFile = StorageAccessHelper.resolveLocalFileFromUri(context, episode.uri)
        val actualFileName = localFile?.name?.takeIf { it.isNotBlank() } ?: episode.fileName
        val fullPath = localFile?.absolutePath ?: Uri.decode(episode.uri)
        val sizeBytes = when {
            localFile != null && localFile.exists() && localFile.length() > 0L -> localFile.length()
            else -> episode.sizeBytes
        }
        val lastModifiedMs = when {
            localFile != null && localFile.exists() && localFile.lastModified() > 0L -> localFile.lastModified()
            else -> episode.lastModified
        }

        val ext = actualFileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val containerFormat = resolveContainerFormatLabel(ext)

        var durationMs = episode.durationMs
        var width = episode.videoWidth
        var height = episode.videoHeight
        var overallBitrateBps = 0L
        var videoFps = 0f
        var videoMime = ""
        var videoCodec = ""
        var videoProfileLevel = ""
        var videoBitrateBps = 0L
        var colorStandard = 0
        var colorTransfer = 0
        var hdrStaticInfoPresent = false

        val embeddedAudio = mutableListOf<AudioStreamTechnicalInfo>()
        val embeddedSubtitles = mutableListOf<SubtitleStreamTechnicalInfo>()

        // 1. Query MediaMetadataRetriever for container duration, dimensions, bitrate
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                setRetrieverDataSource(context, retriever, episode.uri, localFile)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durStr?.toLongOrNull()?.takeIf { it > 0L }?.let { durationMs = it }

                val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                wStr?.toIntOrNull()?.takeIf { it > 0 }?.let { width = it }
                hStr?.toIntOrNull()?.takeIf { it > 0 }?.let { height = it }

                val brStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                brStr?.toLongOrNull()?.takeIf { it > 0L }?.let { overallBitrateBps = it }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val fpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    fpsStr?.toFloatOrNull()?.takeIf { it > 0f }?.let { videoFps = it }
                }
            } finally {
                runCatching { retriever.release() }
            }
        }

        // 2. Query MediaExtractor for individual video, audio, and subtitle elementary streams
        runCatching {
            val extractor = MediaExtractor()
            try {
                setExtractorDataSource(context, extractor, episode.uri, localFile)
                val trackCount = extractor.trackCount
                var audioIdx = 1
                var subIdx = 1

                for (i in 0 until trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getStringOrDefault(MediaFormat.KEY_MIME, "")
                    when {
                        mime.startsWith("video/") -> {
                            if (videoMime.isEmpty()) {
                                videoMime = mime
                                videoCodec = friendlyVideoCodecName(mime, actualFileName)
                                val fw = format.getIntOrDefault(MediaFormat.KEY_WIDTH, width)
                                val fh = format.getIntOrDefault(MediaFormat.KEY_HEIGHT, height)
                                if (fw > 0) width = fw
                                if (fh > 0) height = fh
                                val fps = format.getFloatOrIntAsFloat(MediaFormat.KEY_FRAME_RATE, videoFps)
                                if (fps > 0f) videoFps = fps
                                val vBr = format.getIntOrDefault(MediaFormat.KEY_BIT_RATE, 0)
                                if (vBr > 0) videoBitrateBps = vBr.toLong()

                                val profile = format.getIntOrDefault("profile", 0)
                                val level = format.getIntOrDefault("level", 0)
                                if (profile > 0 || level > 0) {
                                    videoProfileLevel = "Profile $profile" + if (level > 0) " / Level $level" else ""
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    colorStandard = format.getIntOrDefault(MediaFormat.KEY_COLOR_STANDARD, 0)
                                    colorTransfer = format.getIntOrDefault(MediaFormat.KEY_COLOR_TRANSFER, 0)
                                    hdrStaticInfoPresent = format.containsKey(MediaFormat.KEY_HDR_STATIC_INFO)
                                }
                            }
                        }

                        mime.startsWith("audio/") -> {
                            val channels = format.getIntOrDefault(MediaFormat.KEY_CHANNEL_COUNT, 2)
                            val sampleRate = format.getIntOrDefault(MediaFormat.KEY_SAMPLE_RATE, 48000)
                            val aBr = format.getIntOrDefault(MediaFormat.KEY_BIT_RATE, 0)
                            val lang = format.getStringOrDefault(MediaFormat.KEY_LANGUAGE, "und")
                            val title = format.getStringOrDefault("title", "")
                            embeddedAudio.add(
                                AudioStreamTechnicalInfo(
                                    index = audioIdx++,
                                    title = title.ifBlank { "Аудиодорожка #${audioIdx - 1}" },
                                    codecName = friendlyAudioCodecName(mime),
                                    channelsLabel = formatChannelsLabel(channels),
                                    sampleRateLabel = String.format(Locale.US, "%.1f кГц", sampleRate / 1000f),
                                    bitrateLabel = if (aBr > 0) "${aBr / 1000} кбит/с" else "VBR",
                                    language = normalizeLanguageLabel(lang),
                                    isExternal = false
                                )
                            )
                        }

                        mime.startsWith("text/") || mime.startsWith("application/") -> {
                            val lang = format.getStringOrDefault(MediaFormat.KEY_LANGUAGE, "und")
                            val title = format.getStringOrDefault("title", "")
                            embeddedSubtitles.add(
                                SubtitleStreamTechnicalInfo(
                                    index = subIdx++,
                                    title = title.ifBlank { "Субтитры #${subIdx - 1}" },
                                    formatName = friendlySubtitleCodecName(mime),
                                    language = normalizeLanguageLabel(lang),
                                    isExternal = false
                                )
                            )
                        }
                    }
                }
            } finally {
                runCatching { extractor.release() }
            }
        }

        // 3. Fallback inference from filename if MediaExtractor didn't run or container is MKV with unindexed tracks
        if (videoCodec.isBlank()) {
            videoCodec = inferVideoCodecFromFileName(actualFileName, ext)
        }
        if (videoMime.isBlank()) {
            videoMime = inferVideoMimeFromCodec(videoCodec)
        }

        if (overallBitrateBps <= 0L && sizeBytes > 0L && durationMs > 0L) {
            overallBitrateBps = (sizeBytes * 8L * 1000L) / durationMs
        }

        val resBadge = resolveResolutionBadge(width, height, actualFileName)
        val aspectRatio = formatAspectRatio(width, height)
        val fpsLabel = if (videoFps > 0f) {
            if ( kotlin.math.abs(videoFps - videoFps.roundToInt()) < 0.05f ) {
                "${videoFps.roundToInt()} к/с"
            } else {
                String.format(Locale.US, "%.3f к/с", videoFps)
            }
        } else {
            "23.976 к/с"
        }
        val colorHdr = resolveColorAndHdrLabel(colorStandard, colorTransfer, hdrStaticInfoPresent, actualFileName)
        val videoBitrateLabel = when {
            videoBitrateBps > 0L -> formatBitrate(videoBitrateBps)
            overallBitrateBps > 0L -> "~${formatBitrate((overallBitrateBps * 0.85).toLong())}"
            else -> "Авто (VBR)"
        }

        val videoInfo = VideoStreamTechnicalInfo(
            codecName = videoCodec,
            mimeType = videoMime,
            profileLevel = videoProfileLevel.ifBlank { inferProfileLevel(videoCodec, colorHdr) },
            width = width,
            height = height,
            resolutionBadge = resBadge,
            aspectRatioLabel = aspectRatio,
            frameRateLabel = fpsLabel,
            colorAndHdrLabel = colorHdr,
            bitrateLabel = videoBitrateLabel
        )

        // Append external companion audio tracks
        val allAudioStreams = buildList {
            addAll(embeddedAudio)
            episode.autoAudioTracks.forEachIndexed { idx, extTrack ->
                add(
                    AudioStreamTechnicalInfo(
                        index = size + 1,
                        title = extTrack.displayName.ifBlank { "Внешняя аудиодорожка #${idx + 1}" },
                        codecName = extTrack.extension.uppercase(Locale.ROOT).ifBlank { "MKA / AAC" },
                        channelsLabel = "2.0 / 5.1",
                        sampleRateLabel = "48.0 кГц",
                        bitrateLabel = "Внешний файл",
                        language = extTrack.sourceFolderName.ifBlank { "Внешняя озвучка #${idx + 1}" },
                        isExternal = true
                    )
                )
            }
            if (isEmpty()) {
                add(
                    AudioStreamTechnicalInfo(
                        index = 1,
                        title = "Основная дорожка",
                        codecName = inferAudioCodecFromFileName(actualFileName),
                        channelsLabel = "2.0 Stereo",
                        sampleRateLabel = "48.0 кГц",
                        bitrateLabel = "VBR",
                        language = "Авто",
                        isExternal = false
                    )
                )
            }
        }

        // Append external companion subtitle tracks
        val allSubtitleStreams = buildList {
            addAll(embeddedSubtitles)
            episode.autoSubtitleTracks.forEachIndexed { idx, extSub ->
                add(
                    SubtitleStreamTechnicalInfo(
                        index = size + 1,
                        title = extSub.displayName.ifBlank { "Внешние субтитры #${idx + 1}" },
                        formatName = extSub.format.label,
                        language = extSub.sourceFolderName.ifBlank { "Внешние субтитры #${idx + 1}" },
                        isExternal = true
                    )
                )
            }
        }

        val chapters = runCatching {
            VideoChapterExtractor.extractChapters(
                context = context,
                videoUri = episode.uri,
                totalDurationMs = durationMs
            )
        }.getOrDefault(emptyList())

        val sizeStr = formatFileSize(sizeBytes).ifBlank { "${sizeBytes} B" }
        val durationStr = if (durationMs > 0L) formatTimeMs(durationMs) else "—"
        val overallBrStr = if (overallBitrateBps > 0L) formatBitrate(overallBitrateBps) else "—"
        val lastModStr = formatTimestamp(lastModifiedMs)

        return DetailedMediaFileInfo(
            isFolder = false,
            displayName = episode.cleanTitle.ifBlank { actualFileName },
            fileName = actualFileName,
            fullPath = fullPath,
            containerFormat = containerFormat,
            sizeBytes = sizeBytes,
            sizeFormatted = sizeStr,
            durationMs = durationMs,
            durationFormatted = durationStr,
            overallBitrateLabel = overallBrStr,
            lastModifiedFormatted = lastModStr,
            episodeCount = 1,
            watchedCount = if (episode.durationMs > 0L && episode.progressFraction >= 0.85f) 1 else 0,
            chaptersCount = chapters.size,
            videoStream = videoInfo,
            audioStreams = allAudioStreams,
            subtitleStreams = allSubtitleStreams
        )
    }

    fun extractFolderInfo(context: Context, folder: VideoFolderItem): DetailedMediaFileInfo {
        val localDir = StorageAccessHelper.resolveLocalFileFromUri(context, folder.id)
        val fullPath = localDir?.absolutePath ?: folder.displayPath.ifBlank { Uri.decode(folder.id) }
        val totalDurationMs = folder.episodes.sumOf { it.durationMs.coerceAtLeast(0L) }
        val totalSizeBytes = folder.totalSizeBytes.coerceAtLeast(
            folder.episodes.sumOf { it.sizeBytes.coerceAtLeast(0L) }
        )
        val overallBrBps = if (totalSizeBytes > 0L && totalDurationMs > 0L) {
            (totalSizeBytes * 8L * 1000L) / totalDurationMs
        } else {
            0L
        }

        val extensions = folder.episodes
            .map { it.fileName.substringAfterLast('.', "").uppercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .distinct()
        val containerSummary = if (extensions.isNotEmpty()) {
            extensions.joinToString(", ")
        } else {
            "Папка с видео"
        }

        val sampleEpisode = folder.episodes.firstOrNull()
        val videoStream = sampleEpisode?.let { ep ->
            val ext = ep.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            val codec = inferVideoCodecFromFileName(ep.fileName, ext)
            val w = ep.videoWidth
            val h = ep.videoHeight
            VideoStreamTechnicalInfo(
                codecName = codec,
                mimeType = inferVideoMimeFromCodec(codec),
                profileLevel = inferProfileLevel(codec, ""),
                width = w,
                height = h,
                resolutionBadge = resolveResolutionBadge(w, h, ep.fileName),
                aspectRatioLabel = formatAspectRatio(w, h),
                frameRateLabel = "23.976 к/с",
                colorAndHdrLabel = resolveColorAndHdrLabel(0, 0, false, ep.fileName),
                bitrateLabel = if (overallBrBps > 0L) formatBitrate(overallBrBps) else "VBR"
            )
        }

        val extAudioList = folder.episodes
            .flatMap { it.autoAudioTracks }
            .distinctBy { it.sourceFolderName.ifBlank { it.displayName } }
            .take(8)
            .mapIndexed { idx, track ->
                AudioStreamTechnicalInfo(
                    index = idx + 1,
                    title = track.sourceFolderName.ifBlank { track.displayName },
                    codecName = track.extension.uppercase(Locale.ROOT).ifBlank { "MKA / AAC" },
                    channelsLabel = "2.0 / 5.1",
                    sampleRateLabel = "48.0 кГц",
                    bitrateLabel = "Автопривязка (${folder.totalAutoAudioCount} ф.)",
                    language = "Внешняя озвучка",
                    isExternal = true
                )
            }

        val extSubList = folder.episodes
            .flatMap { it.autoSubtitleTracks }
            .distinctBy { it.sourceFolderName.ifBlank { it.format.label } }
            .take(8)
            .mapIndexed { idx, sub ->
                SubtitleStreamTechnicalInfo(
                    index = idx + 1,
                    title = sub.sourceFolderName.ifBlank { sub.displayName },
                    formatName = sub.format.label,
                    language = "Автопривязка (${folder.totalAutoSubCount} ф.)",
                    isExternal = true
                )
            }

        return DetailedMediaFileInfo(
            isFolder = true,
            displayName = folder.name,
            fileName = folder.name,
            fullPath = fullPath,
            containerFormat = containerSummary,
            sizeBytes = totalSizeBytes,
            sizeFormatted = formatFileSize(totalSizeBytes).ifBlank { "${totalSizeBytes} B" },
            durationMs = totalDurationMs,
            durationFormatted = if (totalDurationMs > 0L) formatTimeMs(totalDurationMs) else "—",
            overallBitrateLabel = if (overallBrBps > 0L) formatBitrate(overallBrBps) else "—",
            lastModifiedFormatted = formatTimestamp(folder.lastModified),
            episodeCount = folder.episodes.size,
            watchedCount = folder.watchedCount,
            chaptersCount = 0,
            videoStream = videoStream,
            audioStreams = extAudioList,
            subtitleStreams = extSubList
        )
    }

    private fun setRetrieverDataSource(
        context: Context,
        retriever: MediaMetadataRetriever,
        rawUri: String,
        localFile: File?
    ) {
        if (localFile != null && localFile.exists()) {
            retriever.setDataSource(localFile.absolutePath)
            return
        }
        val uri = Uri.parse(rawUri)
        when (uri.scheme?.lowercase(Locale.ROOT)) {
            "content" -> retriever.setDataSource(context, uri)
            "file" -> retriever.setDataSource(uri.path)
            "http", "https" -> retriever.setDataSource(rawUri, HashMap())
            else -> retriever.setDataSource(rawUri)
        }
    }

    private fun setExtractorDataSource(
        context: Context,
        extractor: MediaExtractor,
        rawUri: String,
        localFile: File?
    ) {
        if (localFile != null && localFile.exists()) {
            extractor.setDataSource(localFile.absolutePath)
            return
        }
        val uri = Uri.parse(rawUri)
        when (uri.scheme?.lowercase(Locale.ROOT)) {
            "content" -> extractor.setDataSource(context, uri, null)
            "file" -> uri.path?.let { extractor.setDataSource(it) }
            else -> extractor.setDataSource(rawUri)
        }
    }

    private fun resolveContainerFormatLabel(ext: String): String = when (ext) {
        "mkv" -> "Matroska (MKV)"
        "mp4", "m4v" -> "MPEG-4 Part 14 (MP4)"
        "webm" -> "WebM Media Container"
        "avi" -> "Audio Video Interleave (AVI)"
        "ts", "m2ts", "mts" -> "MPEG-2 Transport Stream (TS)"
        "mov" -> "QuickTime Movie (MOV)"
        "flv" -> "Flash Video (FLV)"
        "wmv" -> "Windows Media Video (WMV)"
        else -> ext.uppercase(Locale.ROOT).ifBlank { "Видеоконтейнер" }
    }

    private fun friendlyVideoCodecName(mime: String, fileName: String): String {
        val lower = mime.lowercase(Locale.ROOT)
        return when {
            lower.contains("hevc") || lower.contains("h265") || lower.contains("dolby-vision") -> "HEVC (H.265)"
            lower.contains("av01") || lower.contains("av1") -> "AV1 (AOMedia Video 1)"
            lower.contains("avc") || lower.contains("h264") -> "AVC (H.264 / MPEG-4)"
            lower.contains("vp9") || lower.contains("vp09") -> "VP9"
            lower.contains("vp8") -> "VP8"
            lower.contains("mpeg2") -> "MPEG-2 Video"
            lower.contains("mp4v") -> "MPEG-4 Visual"
            else -> inferVideoCodecFromFileName(fileName, "")
        }
    }

    private fun inferVideoCodecFromFileName(fileName: String, ext: String): String {
        val lower = fileName.lowercase(Locale.ROOT)
        return when {
            lower.contains("av1") -> "AV1 (AOMedia Video 1)"
            lower.contains("hevc") || lower.contains("x265") || lower.contains("h265") || lower.contains("h.265") -> "HEVC (H.265)"
            lower.contains("vp9") -> "VP9"
            lower.contains("x264") || lower.contains("h264") || lower.contains("h.264") || lower.contains("avc") -> "AVC (H.264)"
            ext == "webm" -> "VP9 / AV1"
            else -> "AVC (H.264) / HEVC"
        }
    }

    private fun inferVideoMimeFromCodec(codec: String): String {
        val lower = codec.lowercase(Locale.ROOT)
        return when {
            lower.contains("hevc") || lower.contains("265") -> "video/hevc"
            lower.contains("av1") -> "video/av01"
            lower.contains("vp9") -> "video/x-vnd.on2.vp9"
            else -> "video/avc"
        }
    }

    private fun inferProfileLevel(codec: String, colorHdr: String): String {
        val is10Bit = colorHdr.contains("10-bit", ignoreCase = true) || colorHdr.contains("HDR", ignoreCase = true)
        return when {
            codec.contains("HEVC") -> if (is10Bit) "Main 10 @ L5.1" else "Main @ L4.1"
            codec.contains("AV1") -> if (is10Bit) "Main Profile 10-bit" else "Main Profile 8-bit"
            codec.contains("AVC") -> if (is10Bit) "High 10 @ L4.1" else "High @ L4.1"
            else -> "Main Profile"
        }
    }

    private fun inferAudioCodecFromFileName(fileName: String): String {
        val lower = fileName.lowercase(Locale.ROOT)
        return when {
            lower.contains("flac") -> "FLAC (Lossless)"
            lower.contains("eac3") || lower.contains("ddp") || lower.contains("dd+") -> "Dolby Digital Plus (E-AC-3)"
            lower.contains("ac3") || lower.contains("dd5") -> "Dolby Digital (AC-3)"
            lower.contains("dts") -> "DTS Coherent Acoustics"
            lower.contains("opus") -> "Opus"
            else -> "AAC-LC"
        }
    }

    private fun friendlyAudioCodecName(mime: String): String {
        val lower = mime.lowercase(Locale.ROOT)
        return when {
            lower.contains("mp4a") || lower.contains("aac") -> "AAC (Advanced Audio Coding)"
            lower.contains("opus") -> "Opus Interactive Audio"
            lower.contains("flac") -> "FLAC (Free Lossless Audio Codec)"
            lower.contains("eac3") -> "E-AC-3 (Dolby Digital Plus)"
            lower.contains("ac3") -> "AC-3 (Dolby Digital)"
            lower.contains("dts") -> "DTS Audio"
            lower.contains("vorbis") -> "Vorbis"
            lower.contains("mpeg") || lower.contains("mp3") -> "MP3 (MPEG-1 Audio Layer III)"
            lower.contains("raw") || lower.contains("pcm") -> "PCM (Uncompressed)"
            else -> mime.substringAfter('/')
        }
    }

    private fun friendlySubtitleCodecName(mime: String): String {
        val lower = mime.lowercase(Locale.ROOT)
        return when {
            lower.contains("ssa") || lower.contains("ass") -> "SubStation Alpha (ASS/SSA)"
            lower.contains("subrip") || lower.contains("srt") -> "SubRip (SRT)"
            lower.contains("vtt") -> "WebVTT"
            lower.contains("pgs") -> "Presentation Graphic Stream (PGS)"
            lower.contains("vobsub") -> "VobSub (DVD)"
            else -> "Текстовые субтитры"
        }
    }

    private fun formatChannelsLabel(channels: Int): String = when (channels) {
        1 -> "1.0 Mono"
        2 -> "2.0 Stereo"
        6 -> "5.1 Surround (6 кан.)"
        8 -> "7.1 Surround (8 кан.)"
        else -> "$channels кан."
    }

    private fun normalizeLanguageLabel(lang: String): String {
        val clean = lang.trim().lowercase(Locale.ROOT)
        return when (clean) {
            "", "und", "unk", "null" -> "По умолчанию"
            "ru", "rus", "russian" -> "Русский (RU)"
            "en", "eng", "english" -> "Английский (EN)"
            "ja", "jpn", "japanese" -> "Японский (JA)"
            "uk", "ukr" -> "Украинский (UK)"
            "de", "deu", "ger" -> "Немецкий (DE)"
            "fr", "fra", "fre" -> "Французский (FR)"
            "es", "spa" -> "Испанский (ES)"
            "zh", "zho", "chi" -> "Китайский (ZH)"
            "ko", "kor" -> "Корейский (KO)"
            else -> lang.uppercase(Locale.ROOT)
        }
    }

    private fun resolveResolutionBadge(width: Int, height: Int, fileName: String): String {
        val maxDim = maxOf(width, height)
        val minDim = minOf(width, height)
        if (maxDim >= 3800 || minDim >= 2100) return "4K UHD"
        if (maxDim >= 2500 || minDim >= 1400) return "2K QHD"
        if (maxDim >= 1900 || minDim >= 1000) return "1080p Full HD"
        if (maxDim >= 1200 || minDim >= 700) return "720p HD"
        if (maxDim > 0 && minDim > 0) return "${minDim}p SD"

        val lower = fileName.lowercase(Locale.ROOT)
        return when {
            lower.contains("2160p") || lower.contains("4k") -> "4K UHD"
            lower.contains("1440p") || lower.contains("2k") -> "2K QHD"
            lower.contains("1080p") || lower.contains("fhd") -> "1080p Full HD"
            lower.contains("720p") -> "720p HD"
            else -> "1080p HD"
        }
    }

    private fun formatAspectRatio(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return "16:9"
        val ratio = width.toFloat() / height.toFloat()
        return when {
            kotlin.math.abs(ratio - 16f / 9f) < 0.06f -> "16:9"
            kotlin.math.abs(ratio - 21f / 9f) < 0.12f || ratio >= 2.30f -> "21:9 (Cinema)"
            kotlin.math.abs(ratio - 4f / 3f) < 0.06f -> "4:3"
            kotlin.math.abs(ratio - 18f / 9f) < 0.06f -> "18:9 (2:1)"
            else -> String.format(Locale.US, "%.2f:1", ratio)
        }
    }

    private fun resolveColorAndHdrLabel(
        colorStandard: Int,
        colorTransfer: Int,
        hdrStaticInfoPresent: Boolean,
        fileName: String
    ): String {
        val lower = fileName.lowercase(Locale.ROOT)
        if (colorTransfer == 6 || hdrStaticInfoPresent || lower.contains("hdr10") || lower.contains("hdr")) {
            return "10-bit HDR10 (BT.2020 / PQ)"
        }
        if (colorTransfer == 7 || lower.contains("hlg")) {
            return "10-bit HLG (BT.2020)"
        }
        if (lower.contains("dovi") || lower.contains("dv") || lower.contains("dolby.vision")) {
            return "10-bit Dolby Vision"
        }
        if (lower.contains("10bit") || lower.contains("10-bit") || lower.contains("hi10p") || colorStandard == 6) {
            return "10-bit SDR (BT.709)"
        }
        return "8-bit SDR (BT.709)"
    }

    private fun formatBitrate(bps: Long): String {
        val kbps = bps / 1000L
        return if (kbps >= 1000L) {
            String.format(Locale.US, "%.2f Мбит/с (%d кбит/с)", kbps / 1000f, kbps)
        } else {
            "$kbps кбит/с"
        }
    }

    private fun formatTimestamp(ms: Long): String {
        if (ms <= 0L) return "—"
        return runCatching {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(ms))
        }.getOrDefault("—")
    }

    private fun MediaFormat.getStringOrDefault(key: String, default: String): String =
        runCatching { getString(key) ?: default }.getOrDefault(default)

    private fun MediaFormat.getIntOrDefault(key: String, default: Int): Int =
        runCatching { if (containsKey(key)) getInteger(key) else default }.getOrDefault(default)

    private fun MediaFormat.getFloatOrIntAsFloat(key: String, default: Float): Float =
        runCatching {
            if (!containsKey(key)) default
            else runCatching { getFloat(key) }.getOrElse { getInteger(key).toFloat() }
        }.getOrDefault(default)
}
