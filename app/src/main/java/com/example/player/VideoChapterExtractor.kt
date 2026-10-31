package com.example.player

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.system.Os
import androidx.annotation.OptIn
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import com.example.model.VideoChapter
import com.example.torrent.TorrServerClient
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.min
import okhttp3.Request

/**
 * Extracts embedded video chapters from:
 * 1. Matroska / WebM (.mkv, .webm) EBML `Chapters` (`0x1043A770`) via primary/secondary `SeekHead` or Segment scan.
 * 2. MP4 / M4V / MOV (`moov` -> `udta` -> `chpl` Nero chapter table).
 * 3. Media3 `Tracks` metadata fallback (ID3 `CHAP` frames).
 *
 * Supports local `file://`, SAF `content://` (via `Os.pread`), and `http(s)://` TorrServer / WebDAV streams
 * via bounded HTTP Range requests.
 */
object VideoChapterExtractor {

    private const val EBML_ID = 0x1A45DFA3L
    private const val SEGMENT_ID = 0x18538067L
    private const val SEEK_HEAD_ID = 0x114D9B74L
    private const val SEEK_ID = 0x4DBBL
    private const val SEEK_ENTRY_ID = 0x53ABL
    private const val SEEK_POSITION_ID = 0x53ACL
    private const val CLUSTER_ID = 0x1F43B675L
    private const val CHAPTERS_ID = 0x1043A770L
    private const val EDITION_ENTRY_ID = 0x45B9L
    private const val EDITION_FLAG_HIDDEN_ID = 0x45BDL
    private const val EDITION_FLAG_DEFAULT_ID = 0x45DBL
    private const val CHAPTER_ATOM_ID = 0xB6L
    private const val CHAPTER_TIME_START_ID = 0x91L
    private const val CHAPTER_TIME_END_ID = 0x92L
    private const val CHAPTER_FLAG_HIDDEN_ID = 0x98L
    private const val CHAPTER_FLAG_ENABLED_ID = 0x4598L
    private const val CHAPTER_DISPLAY_ID = 0x80L
    private const val CHAP_STRING_ID = 0x85L
    private const val CHAP_LANGUAGE_ID = 0x437CL
    private const val CHAP_LANGUAGE_IETF_ID = 0x437DL

    interface SeekableReader : AutoCloseable {
        val length: Long
        fun readAt(position: Long, buffer: ByteArray, offset: Int, count: Int): Int
    }

    fun extractChapters(
        context: Context,
        videoUri: String,
        totalDurationMs: Long = 0L,
        torrServerBaseUrl: String = "",
        torrServerAuthHeader: String? = null
    ): List<VideoChapter> {
        if (videoUri.isBlank()) return emptyList()
        return runCatching {
            val parsedUri = Uri.parse(videoUri)
            val auth = if (
                torrServerAuthHeader != null &&
                torrServerBaseUrl.isNotBlank() &&
                videoUri.startsWith(torrServerBaseUrl, ignoreCase = true)
            ) {
                torrServerAuthHeader
            } else {
                null
            }
            openReader(context, parsedUri, videoUri, auth)?.use { reader ->
                extractFromReader(reader, totalDurationMs)
            }.orEmpty()
        }.getOrElse { emptyList() }
    }

    @OptIn(UnstableApi::class)
    fun extractFromMedia3Tracks(tracks: Tracks, totalDurationMs: Long = 0L): List<VideoChapter> {
        val raw = mutableListOf<RawChapter>()
        for (group in tracks.groups) {
            for (i in 0 until group.length) {
                val metadata = group.getTrackFormat(i).metadata ?: continue
                for (entryIdx in 0 until metadata.length()) {
                    val entry = metadata.get(entryIdx)
                    if (entry is ChapterFrame) {
                        var title = entry.chapterId
                        for (subIdx in 0 until entry.subFrameCount) {
                            val sub = entry.getSubFrame(subIdx)
                            if (sub is TextInformationFrame && sub.id == "TIT2") {
                                val candidate = sub.values.firstOrNull()?.trim()
                                if (!candidate.isNullOrBlank()) {
                                    title = candidate
                                    break
                                }
                            }
                        }
                        val start = entry.startTimeMs.toLong().coerceAtLeast(0L)
                        val end = entry.endTimeMs.toLong().takeIf { it > start } ?: 0L
                        raw.add(RawChapter(startTimeMs = start, endTimeMs = end, title = title))
                    }
                }
            }
        }
        return normalizeChapters(raw, totalDurationMs)
    }

    fun parseMkvBytes(bytes: ByteArray, totalDurationMs: Long = 0L): List<VideoChapter> {
        return ByteArrayReader(bytes).use { parseMatroskaChapters(it, totalDurationMs) }
    }

    fun parseMp4Bytes(bytes: ByteArray, totalDurationMs: Long = 0L): List<VideoChapter> {
        return ByteArrayReader(bytes).use { parseMp4Chapters(it, totalDurationMs) }
    }

    fun extractFromReader(reader: SeekableReader, totalDurationMs: Long = 0L): List<VideoChapter> {
        if (reader.length < 16L) return emptyList()
        val head = ByteArray(12)
        val n = reader.readFullyUpTo(0L, head, 12)
        if (n < 8) return emptyList()

        // Detect EBML (Matroska / WebM): starts with 0x1A 0x45 0xDF 0xA3
        if ((head[0].toInt() and 0xFF) == 0x1A &&
            (head[1].toInt() and 0xFF) == 0x45 &&
            (head[2].toInt() and 0xFF) == 0xDF &&
            (head[3].toInt() and 0xFF) == 0xA3
        ) {
            return parseMatroskaChapters(reader, totalDurationMs)
        }

        // Detect ISO BMFF (MP4 / M4V / MOV): bytes 4..7 typically 'ftyp', 'moov', 'mdat', 'free', 'wide'
        val boxType = head.decodeToString(4, 8)
        if (boxType in setOf("ftyp", "moov", "mdat", "free", "skip", "wide", "styp")) {
            return parseMp4Chapters(reader, totalDurationMs)
        }

        return emptyList()
    }

    // ---------------- Matroska / EBML Parser ----------------

    private data class EbmlHeader(
        val id: Long,
        val dataSize: Long, // -1L = unknown size
        val headerBytes: Int
    )

    private data class RawChapter(
        val startTimeMs: Long,
        val endTimeMs: Long,
        val title: String
    )

    private fun parseMatroskaChapters(reader: SeekableReader, totalDurationMs: Long): List<VideoChapter> {
        val fileLen = reader.length
        val ebml = readEbmlHeader(reader, 0L) ?: return emptyList()
        if (ebml.id != EBML_ID || ebml.dataSize < 0L) return emptyList()

        val segOffset = ebml.headerBytes + ebml.dataSize
        val segment = readEbmlHeader(reader, segOffset) ?: return emptyList()
        if (segment.id != SEGMENT_ID) return emptyList()

        val segmentDataStart = segOffset + segment.headerBytes
        val segmentEnd = if (segment.dataSize >= 0L) {
            min(fileLen, segmentDataStart + segment.dataSize)
        } else {
            fileLen
        }

        var chaptersOffset: Long? = null
        val secondarySeekHeads = mutableListOf<Long>()

        // Walk initial Segment elements (before first media Cluster)
        var cursor = segmentDataStart
        val maxInitialScan = min(segmentEnd, segmentDataStart + 2L * 1024L * 1024L)
        var elementsVisited = 0
        while (cursor + 4 <= maxInitialScan && elementsVisited < 64) {
            elementsVisited++
            val hdr = readEbmlHeader(reader, cursor) ?: break
            val elemDataStart = cursor + hdr.headerBytes
            when (hdr.id) {
                CHAPTERS_ID -> {
                    chaptersOffset = cursor
                    break
                }
                SEEK_HEAD_ID -> {
                    if (hdr.dataSize in 1..262_144) {
                        val buf = ByteArray(hdr.dataSize.toInt())
                        if (reader.readFullyUpTo(elemDataStart, buf, buf.size) == buf.size) {
                            val entries = parseSeekHeadEntries(buf)
                            entries[CHAPTERS_ID]?.let { relPos ->
                                chaptersOffset = segmentDataStart + relPos
                            }
                            entries[SEEK_HEAD_ID]?.let { relPos ->
                                val target = segmentDataStart + relPos
                                if (target != cursor && target in segmentDataStart until segmentEnd) {
                                    secondarySeekHeads.add(target)
                                }
                            }
                        }
                    }
                    if (chaptersOffset != null) break
                }
                CLUSTER_ID -> break
            }
            if (hdr.dataSize < 0L) break
            cursor = elemDataStart + hdr.dataSize
        }

        // Follow secondary SeekHead if Chapters wasn't listed in primary SeekHead
        if (chaptersOffset == null && secondarySeekHeads.isNotEmpty()) {
            for (secPos in secondarySeekHeads.take(3)) {
                val secHdr = readEbmlHeader(reader, secPos) ?: continue
                if (secHdr.id == SEEK_HEAD_ID && secHdr.dataSize in 1..262_144) {
                    val buf = ByteArray(secHdr.dataSize.toInt())
                    val start = secPos + secHdr.headerBytes
                    if (reader.readFullyUpTo(start, buf, buf.size) == buf.size) {
                        val entries = parseSeekHeadEntries(buf)
                        entries[CHAPTERS_ID]?.let { relPos ->
                            chaptersOffset = segmentDataStart + relPos
                        }
                        if (chaptersOffset != null) break
                    }
                }
            }
        }

        val targetOffset = chaptersOffset ?: return emptyList()
        if (targetOffset < segmentDataStart || targetOffset >= segmentEnd) return emptyList()
        val chapHdr = readEbmlHeader(reader, targetOffset) ?: return emptyList()
        if (chapHdr.id != CHAPTERS_ID || chapHdr.dataSize <= 0L || chapHdr.dataSize > 2L * 1024L * 1024L) {
            return emptyList()
        }

        val chapBytes = ByteArray(chapHdr.dataSize.toInt())
        val readCount = reader.readFullyUpTo(targetOffset + chapHdr.headerBytes, chapBytes, chapBytes.size)
        if (readCount <= 0) return emptyList()

        val rawChapters = parseChaptersPayload(chapBytes, 0, readCount)
        return normalizeChapters(rawChapters, totalDurationMs)
    }

    private fun parseSeekHeadEntries(buf: ByteArray): Map<Long, Long> {
        val result = LinkedHashMap<Long, Long>()
        var pos = 0
        while (pos < buf.size) {
            val hdr = readEbmlHeaderFromBytes(buf, pos, buf.size) ?: break
            val dataStart = pos + hdr.headerBytes
            val dataEnd = if (hdr.dataSize >= 0) min(buf.size, dataStart + hdr.dataSize.toInt()) else buf.size
            if (hdr.id == SEEK_ID) {
                var seekId: Long? = null
                var seekPos: Long? = null
                var sub = dataStart
                while (sub < dataEnd) {
                    val subHdr = readEbmlHeaderFromBytes(buf, sub, dataEnd) ?: break
                    val subStart = sub + subHdr.headerBytes
                    val subLen = subHdr.dataSize.toInt().coerceAtLeast(0)
                    if (subStart + subLen > dataEnd) break
                    when (subHdr.id) {
                        SEEK_ENTRY_ID -> seekId = readUnsignedInt(buf, subStart, subLen)
                        SEEK_POSITION_ID -> seekPos = readUnsignedInt(buf, subStart, subLen)
                    }
                    sub = subStart + subLen
                }
                if (seekId != null && seekPos != null && seekId !in result) {
                    result[seekId] = seekPos
                }
            }
            pos = dataEnd
        }
        return result
    }

    private data class ParsedEdition(
        val isDefault: Boolean,
        val isHidden: Boolean,
        val chapters: List<RawChapter>
    )

    private fun parseChaptersPayload(buf: ByteArray, start: Int, limit: Int): List<RawChapter> {
        val editions = mutableListOf<ParsedEdition>()
        var pos = start
        while (pos < limit) {
            val hdr = readEbmlHeaderFromBytes(buf, pos, limit) ?: break
            val dataStart = pos + hdr.headerBytes
            val dataEnd = if (hdr.dataSize >= 0) min(limit, dataStart + hdr.dataSize.toInt()) else limit
            if (hdr.id == EDITION_ENTRY_ID) {
                editions.add(parseEditionEntry(buf, dataStart, dataEnd))
            }
            pos = dataEnd
        }
        val visibleEditions = editions.filter { !it.isHidden && it.chapters.isNotEmpty() }
            .ifEmpty { editions.filter { it.chapters.isNotEmpty() } }
        val chosen = visibleEditions.firstOrNull { it.isDefault } ?: visibleEditions.firstOrNull()
        return chosen?.chapters.orEmpty()
    }

    private fun parseEditionEntry(buf: ByteArray, start: Int, limit: Int): ParsedEdition {
        var isDefault = false
        var isHidden = false
        val chapters = mutableListOf<RawChapter>()
        var pos = start
        while (pos < limit) {
            val hdr = readEbmlHeaderFromBytes(buf, pos, limit) ?: break
            val dataStart = pos + hdr.headerBytes
            val dataEnd = if (hdr.dataSize >= 0) min(limit, dataStart + hdr.dataSize.toInt()) else limit
            val len = dataEnd - dataStart
            when (hdr.id) {
                EDITION_FLAG_HIDDEN_ID -> isHidden = readUnsignedInt(buf, dataStart, len) != 0L
                EDITION_FLAG_DEFAULT_ID -> isDefault = readUnsignedInt(buf, dataStart, len) != 0L
                CHAPTER_ATOM_ID -> parseChapterAtomsInto(buf, dataStart, dataEnd, chapters)
            }
            pos = dataEnd
        }
        return ParsedEdition(isDefault = isDefault, isHidden = isHidden, chapters = chapters)
    }

    private fun parseChapterAtomsInto(
        buf: ByteArray,
        start: Int,
        limit: Int,
        out: MutableList<RawChapter>
    ) {
        var timeStartNs: Long? = null
        var timeEndNs: Long = 0L
        var flagHidden = false
        var flagEnabled = true
        val displays = mutableListOf<Pair<String, String>>() // (lang, title)
        val nestedAtoms = mutableListOf<IntRange>()

        var pos = start
        while (pos < limit) {
            val hdr = readEbmlHeaderFromBytes(buf, pos, limit) ?: break
            val dataStart = pos + hdr.headerBytes
            val dataEnd = if (hdr.dataSize >= 0) min(limit, dataStart + hdr.dataSize.toInt()) else limit
            val len = dataEnd - dataStart
            when (hdr.id) {
                CHAPTER_TIME_START_ID -> timeStartNs = readUnsignedInt(buf, dataStart, len)
                CHAPTER_TIME_END_ID -> timeEndNs = readUnsignedInt(buf, dataStart, len)
                CHAPTER_FLAG_HIDDEN_ID -> flagHidden = readUnsignedInt(buf, dataStart, len) != 0L
                CHAPTER_FLAG_ENABLED_ID -> flagEnabled = readUnsignedInt(buf, dataStart, len) != 0L
                CHAPTER_DISPLAY_ID -> {
                    parseChapterDisplay(buf, dataStart, dataEnd)?.let { displays.add(it) }
                }
                CHAPTER_ATOM_ID -> nestedAtoms.add(dataStart until dataEnd)
            }
            pos = dataEnd
        }

        if (!flagHidden && flagEnabled && timeStartNs != null) {
            val chosenTitle = pickPreferredLanguageTitle(displays)
            out.add(
                RawChapter(
                    startTimeMs = (timeStartNs / 1_000_000L).coerceAtLeast(0L),
                    endTimeMs = (timeEndNs / 1_000_000L).coerceAtLeast(0L),
                    title = chosenTitle
                )
            )
        }
        for (range in nestedAtoms) {
            parseChapterAtomsInto(buf, range.first, range.last + 1, out)
        }
    }

    private fun parseChapterDisplay(buf: ByteArray, start: Int, limit: Int): Pair<String, String>? {
        var title = ""
        var lang = ""
        var pos = start
        while (pos < limit) {
            val hdr = readEbmlHeaderFromBytes(buf, pos, limit) ?: break
            val dataStart = pos + hdr.headerBytes
            val dataEnd = if (hdr.dataSize >= 0) min(limit, dataStart + hdr.dataSize.toInt()) else limit
            val len = dataEnd - dataStart
            when (hdr.id) {
                CHAP_STRING_ID -> {
                    title = buf.decodeToString(dataStart, dataEnd).trimEnd('\u0000').trim()
                }
                CHAP_LANGUAGE_ID, CHAP_LANGUAGE_IETF_ID -> {
                    if (lang.isEmpty()) {
                        lang = buf.decodeToString(dataStart, dataEnd).trimEnd('\u0000').trim().lowercase()
                    }
                }
            }
            pos = dataEnd
        }
        return if (title.isNotEmpty()) lang to title else null
    }

    private fun pickPreferredLanguageTitle(displays: List<Pair<String, String>>): String {
        if (displays.isEmpty()) return ""
        displays.firstOrNull { (lang, _) -> lang.startsWith("ru") }?.let { return it.second }
        displays.firstOrNull { (lang, _) -> lang.startsWith("en") }?.let { return it.second }
        return displays.first().second
    }

    private fun readEbmlHeader(reader: SeekableReader, offset: Long): EbmlHeader? {
        val scratch = ByteArray(16)
        val n = reader.readFullyUpTo(offset, scratch, 16)
        if (n < 2) return null
        return readEbmlHeaderFromBytes(scratch, 0, n)
    }

    private fun readEbmlHeaderFromBytes(buf: ByteArray, pos: Int, limit: Int): EbmlHeader? {
        if (pos >= limit) return null
        val firstIdByte = buf[pos].toInt() and 0xFF
        val idLen = vintLength(firstIdByte)
        if (idLen !in 1..4 || pos + idLen >= limit) return null

        var id = 0L
        for (i in 0 until idLen) {
            id = (id shl 8) or (buf[pos + i].toLong() and 0xFFL)
        }

        val sizePos = pos + idLen
        val firstSizeByte = buf[sizePos].toInt() and 0xFF
        val sizeLen = vintLength(firstSizeByte)
        if (sizeLen !in 1..8 || sizePos + sizeLen > limit) return null

        val mask = (0xFF shr sizeLen).toLong()
        var size = firstSizeByte.toLong() and mask
        var allOnes = (size == mask)
        for (i in 1 until sizeLen) {
            val b = buf[sizePos + i].toLong() and 0xFFL
            if (b != 0xFFL) allOnes = false
            size = (size shl 8) or b
        }

        val finalSize = if (allOnes) -1L else size
        return EbmlHeader(id = id, dataSize = finalSize, headerBytes = idLen + sizeLen)
    }

    private fun vintLength(firstByte: Int): Int {
        if (firstByte == 0) return 0
        var mask = 0x80
        var len = 1
        while (len <= 8 && (firstByte and mask) == 0) {
            mask = mask shr 1
            len++
        }
        return len
    }

    private fun readUnsignedInt(buf: ByteArray, offset: Int, length: Int): Long {
        if (length <= 0) return 0L
        var v = 0L
        val safeLen = min(length, 8)
        for (i in 0 until safeLen) {
            v = (v shl 8) or (buf[offset + i].toLong() and 0xFFL)
        }
        return v
    }

    // ---------------- MP4 / ISO BMFF (`moov` -> `udta` -> `chpl`) Parser ----------------

    private fun parseMp4Chapters(reader: SeekableReader, totalDurationMs: Long): List<VideoChapter> {
        val fileLen = reader.length
        var offset = 0L
        val hdr = ByteArray(16)
        var boxesVisited = 0

        while (offset + 8 <= fileLen && boxesVisited < 64) {
            boxesVisited++
            val readN = reader.readFullyUpTo(offset, hdr, 16)
            if (readN < 8) break
            val size32 = readUInt32Be(hdr, 0)
            val type = hdr.decodeToString(4, 8)
            var headerSize = 8L
            val boxSize = when (size32) {
                1L -> {
                    if (readN < 16) break
                    headerSize = 16L
                    readInt64Be(hdr, 8)
                }
                0L -> fileLen - offset
                else -> size32
            }
            if (boxSize < headerSize) break

            if (type == "moov") {
                val moovPayloadSize = boxSize - headerSize
                if (moovPayloadSize in 8..(16L * 1024L * 1024L)) {
                    val moovBytes = ByteArray(moovPayloadSize.toInt())
                    val actual = reader.readFullyUpTo(offset + headerSize, moovBytes, moovBytes.size)
                    if (actual >= 8) {
                        val raw = parseMoovForChpl(moovBytes, 0, actual)
                        if (raw.isNotEmpty()) {
                            return normalizeChapters(raw, totalDurationMs)
                        }
                    }
                }
                break
            }

            offset += boxSize
        }
        return emptyList()
    }

    private fun parseMoovForChpl(buf: ByteArray, start: Int, limit: Int): List<RawChapter> {
        var pos = start
        while (pos + 8 <= limit) {
            val size32 = readUInt32Be(buf, pos)
            val type = buf.decodeToString(pos + 4, pos + 8)
            val boxSize = if (size32 == 0L) (limit - pos).toLong() else size32
            if (boxSize < 8L || pos + boxSize > limit) break
            val nextPos = pos + boxSize.toInt()
            if (type == "udta") {
                val chpl = parseUdtaForChpl(buf, pos + 8, nextPos)
                if (chpl.isNotEmpty()) return chpl
            }
            pos = nextPos
        }
        return emptyList()
    }

    private fun parseUdtaForChpl(buf: ByteArray, start: Int, limit: Int): List<RawChapter> {
        var pos = start
        while (pos + 8 <= limit) {
            val size32 = readUInt32Be(buf, pos)
            val type = buf.decodeToString(pos + 4, pos + 8)
            val boxSize = if (size32 == 0L) (limit - pos).toLong() else size32
            if (boxSize < 8L || pos + boxSize > limit) break
            val nextPos = pos + boxSize.toInt()
            if (type == "chpl") {
                return parseChplBox(buf, pos + 8, nextPos)
            }
            pos = nextPos
        }
        return emptyList()
    }

    private fun parseChplBox(buf: ByteArray, start: Int, limit: Int): List<RawChapter> {
        if (start + 5 > limit) return emptyList()
        val version = buf[start].toInt() and 0xFF
        // version + 3-byte flags = 4 bytes; v1 has 4 reserved bytes before 1-byte count; v0 has 1 reserved byte before count
        var cursor = if (version >= 1) start + 8 else start + 5
        if (cursor >= limit) return emptyList()

        val chapterCount = buf[cursor].toInt() and 0xFF
        cursor += 1
        val out = ArrayList<RawChapter>(chapterCount)
        for (i in 0 until chapterCount) {
            if (cursor + 9 > limit) break
            val startTicks100Ns = readInt64Be(buf, cursor)
            cursor += 8
            val titleLen = buf[cursor].toInt() and 0xFF
            cursor += 1
            if (cursor + titleLen > limit) break
            val title = buf.decodeToString(cursor, cursor + titleLen).trim()
            cursor += titleLen
            val startMs = (startTicks100Ns / 10_000L).coerceAtLeast(0L)
            out.add(RawChapter(startTimeMs = startMs, endTimeMs = 0L, title = title))
        }
        return out
    }

    private fun readUInt32Be(buf: ByteArray, offset: Int): Long {
        return ((buf[offset].toLong() and 0xFFL) shl 24) or
            ((buf[offset + 1].toLong() and 0xFFL) shl 16) or
            ((buf[offset + 2].toLong() and 0xFFL) shl 8) or
            (buf[offset + 3].toLong() and 0xFFL)
    }

    private fun readInt64Be(buf: ByteArray, offset: Int): Long {
        var res = 0L
        for (i in 0 until 8) {
            res = (res shl 8) or (buf[offset + i].toLong() and 0xFFL)
        }
        return res
    }

    // ---------------- Normalization & Reader Implementations ----------------

    private fun normalizeChapters(raw: List<RawChapter>, totalDurationMs: Long): List<VideoChapter> {
        if (raw.isEmpty()) return emptyList()
        val sorted = raw
            .sortedBy { it.startTimeMs }
            .distinctBy { it.startTimeMs / 50L } // deduplicate within 50ms tolerance

        return sorted.mapIndexed { idx, item ->
            val nextStart = sorted.getOrNull(idx + 1)?.startTimeMs
            val computedEnd = when {
                item.endTimeMs > item.startTimeMs -> item.endTimeMs
                nextStart != null && nextStart > item.startTimeMs -> nextStart
                totalDurationMs > item.startTimeMs -> totalDurationMs
                else -> item.startTimeMs
            }
            val cleanTitle = item.title.ifBlank { "Глава ${idx + 1}" }
            VideoChapter(
                index = idx,
                title = cleanTitle,
                startTimeMs = item.startTimeMs,
                endTimeMs = computedEnd
            )
        }
    }

    private fun SeekableReader.readFullyUpTo(position: Long, buf: ByteArray, maxLen: Int): Int {
        var total = 0
        while (total < maxLen) {
            val n = readAt(position + total, buf, total, maxLen - total)
            if (n <= 0) break
            total += n
        }
        return total
    }

    private fun openReader(
        context: Context,
        uri: Uri,
        rawUrl: String,
        authHeader: String?
    ): SeekableReader? {
        return when (uri.scheme?.lowercase()) {
            "file" -> {
                val path = uri.path ?: return null
                val f = File(path)
                if (!f.exists() || !f.canRead()) return null
                LocalFileReader(f)
            }
            "content" -> {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
                if (pfd.statSize <= 0L) {
                    pfd.close()
                    return null
                }
                PfdSeekableReader(pfd)
            }
            "http", "https" -> HttpRangeReader.open(rawUrl, authHeader)
            else -> null
        }
    }

    private class ByteArrayReader(private val data: ByteArray) : SeekableReader {
        override val length: Long get() = data.size.toLong()
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, count: Int): Int {
            if (position < 0L || position >= data.size || count <= 0) return -1
            val avail = min(count, (data.size - position).toInt())
            System.arraycopy(data, position.toInt(), buffer, offset, avail)
            return avail
        }
        override fun close() = Unit
    }

    private class LocalFileReader(file: File) : SeekableReader {
        private val raf = RandomAccessFile(file, "r")
        override val length: Long = raf.length()
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, count: Int): Int {
            if (position < 0L || position >= length) return -1
            synchronized(raf) {
                raf.seek(position)
                return raf.read(buffer, offset, count)
            }
        }
        override fun close() {
            runCatching { raf.close() }
        }
    }

    private class PfdSeekableReader(private val pfd: ParcelFileDescriptor) : SeekableReader {
        override val length: Long = pfd.statSize
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, count: Int): Int {
            if (position < 0L || position >= length) return -1
            return runCatching {
                Os.pread(pfd.fileDescriptor, buffer, offset, count, position)
            }.getOrDefault(-1)
        }
        override fun close() {
            runCatching { pfd.close() }
        }
    }

    private class HttpRangeReader private constructor(
        private val url: String,
        private val authHeader: String?,
        override val length: Long,
        initialChunk: ByteArray
    ) : SeekableReader {
        private data class Block(val start: Long, val data: ByteArray)
        private val blocks = ArrayList<Block>(6)
        private var networkFetches = 1

        init {
            if (initialChunk.isNotEmpty()) {
                blocks.add(Block(0L, initialChunk))
            }
        }

        override fun readAt(position: Long, buffer: ByteArray, offset: Int, count: Int): Int {
            if (position < 0L || position >= length || count <= 0) return -1
            for (b in blocks) {
                if (position >= b.start && position < b.start + b.data.size) {
                    val inBlockOffset = (position - b.start).toInt()
                    val avail = min(count, b.data.size - inBlockOffset)
                    System.arraycopy(b.data, inBlockOffset, buffer, offset, avail)
                    return avail
                }
            }
            if (networkFetches >= 6) return -1
            networkFetches++
            val fetchLen = maxOf(count.toLong(), 64L * 1024L).coerceAtMost(256L * 1024L)
            val endInclusive = min(length - 1L, position + fetchLen - 1L)
            val chunk = fetchRange(url, authHeader, position, endInclusive)?.first ?: return -1
            if (chunk.isEmpty()) return -1
            if (blocks.size >= 6) blocks.removeAt(0)
            blocks.add(Block(position, chunk))
            val avail = min(count, chunk.size)
            System.arraycopy(chunk, 0, buffer, offset, avail)
            return avail
        }

        override fun close() = Unit

        companion object {
            fun open(url: String, authHeader: String?): HttpRangeReader? {
                val (initialBytes, totalLen) = fetchRange(url, authHeader, 0L, 65_535L) ?: return null
                if (totalLen <= 0L || initialBytes.isEmpty()) return null
                return HttpRangeReader(url, authHeader, totalLen, initialBytes)
            }

            private fun fetchRange(
                url: String,
                authHeader: String?,
                start: Long,
                endInclusive: Long
            ): Pair<ByteArray, Long>? {
                val reqBuilder = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=$start-$endInclusive")
                    .get()
                if (authHeader != null) {
                    reqBuilder.header("Authorization", authHeader)
                }
                return runCatching {
                    TorrServerClient.httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                        if (resp.code != 206 && resp.code != 200) return@use null
                        val contentRange = resp.header("Content-Range")
                        // Support only true partial-content responses so we never download whole stream
                        if (resp.code == 200 && contentRange == null) {
                            val cl = resp.body?.contentLength() ?: -1L
                            if (cl <= 0L || cl > 512L * 1024L) return@use null
                        }
                        val total = contentRange
                            ?.substringAfterLast('/')
                            ?.trim()
                            ?.toLongOrNull()
                            ?: resp.body?.contentLength()
                            ?: -1L
                        val bytes = resp.body?.bytes() ?: return@use null
                        bytes to total
                    }
                }.getOrNull()
            }
        }
    }
}
