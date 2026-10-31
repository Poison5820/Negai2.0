package com.example.subtitles

import com.example.model.SubtitleFormat
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.Locale

object SubtitleParser {

    private val SRT_TIMING_REGEX = Regex(
        """(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})"""
    )
    private val ASS_OVERRIDE_BLOCK_REGEX = Regex("""\{([^}]*)\}""")
    private val ASS_ALIGNMENT_REGEX = Regex("""\\an([1-9])""")
    private val HTML_TAG_REGEX = Regex("""<[^>]+>""")

    private data class AssStyleDef(
        val name: String,
        val alignment: SubtitleAlignment,
        val bold: Boolean,
        val italic: Boolean,
        val primaryColorArgb: Long?
    )

    /**
     * Decodes raw subtitle file bytes supporting UTF-8, UTF-16 LE/BE BOMs,
     * and automatic fallback to Windows-1251 for Cyrillic SRT/ASS files.
     */
    fun decodeSubtitleBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }

        return try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: Exception) {
            val win1251 = runCatching { Charset.forName("windows-1251") }.getOrDefault(Charsets.UTF_8)
            String(bytes, win1251)
        }
    }

    fun parse(
        rawContent: String,
        formatHint: SubtitleFormat? = null,
        title: String = "Subtitles"
    ): ParsedSubtitleDocument {
        val normalized = rawContent.replace("\r\n", "\n").replace('\r', '\n').trimStart('\uFEFF')
        val detectedFormat = when {
            normalized.contains("[Events]", ignoreCase = true) ||
                normalized.contains("[Script Info]", ignoreCase = true) ||
                formatHint == SubtitleFormat.ASS -> SubtitleFormat.ASS
            else -> SubtitleFormat.SRT
        }

        val cues = when (detectedFormat) {
            SubtitleFormat.ASS -> parseAss(normalized)
            SubtitleFormat.SRT, SubtitleFormat.VTT -> parseSrtOrVtt(normalized)
        }

        return ParsedSubtitleDocument(
            title = title,
            cues = cues.sortedBy { it.startMs }
        )
    }

    fun parseSrtOrVtt(content: String): List<SubtitleCue> {
        val result = mutableListOf<SubtitleCue>()
        val lines = content.split('\n')
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.equals("WEBVTT", ignoreCase = true)) {
                i++
                continue
            }

            var timingMatch = SRT_TIMING_REGEX.find(line)
            if (timingMatch == null && i + 1 < lines.size) {
                val nextLine = lines[i + 1].trim()
                val nextMatch = SRT_TIMING_REGEX.find(nextLine)
                if (nextMatch != null) {
                    timingMatch = nextMatch
                    i++
                }
            }

            if (timingMatch != null) {
                val startMs = parseSrtTime(
                    timingMatch.groupValues[1],
                    timingMatch.groupValues[2],
                    timingMatch.groupValues[3],
                    timingMatch.groupValues[4]
                )
                val endMs = parseSrtTime(
                    timingMatch.groupValues[5],
                    timingMatch.groupValues[6],
                    timingMatch.groupValues[7],
                    timingMatch.groupValues[8]
                )
                i++
                val textLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().isNotEmpty()) {
                    textLines.add(lines[i].trim())
                    i++
                }

                val rawBlock = textLines.joinToString("\n")
                if (rawBlock.isNotBlank() && endMs > startMs) {
                    val isItalic = rawBlock.contains("<i>", ignoreCase = true) || rawBlock.contains("{\\i1}")
                    val isBold = rawBlock.contains("<b>", ignoreCase = true) || rawBlock.contains("{\\b1}")
                    val alignCode = ASS_ALIGNMENT_REGEX.find(rawBlock)?.groupValues?.get(1)?.toIntOrNull()
                    val alignment = if (alignCode != null) {
                        SubtitleAlignment.fromAssNumpad(alignCode)
                    } else {
                        SubtitleAlignment.BOTTOM_CENTER
                    }

                    val cleaned = rawBlock
                        .replace(ASS_OVERRIDE_BLOCK_REGEX, "")
                        .replace(HTML_TAG_REGEX, "")
                        .replace("&nbsp;", " ")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .trim()

                    if (cleaned.isNotEmpty()) {
                        result.add(
                            SubtitleCue(
                                startMs = startMs,
                                endMs = endMs,
                                text = cleaned,
                                alignment = alignment,
                                isItalic = isItalic,
                                isBold = isBold
                            )
                        )
                    }
                }
            } else {
                i++
            }
        }
        return result
    }

    fun parseAss(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val styles = mutableMapOf<String, AssStyleDef>()

        var currentSection = ""
        var styleColumns: List<String> = emptyList()
        var eventColumns: List<String> = emptyList()

        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith(";")) continue

            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length - 1).lowercase(Locale.ROOT)
                continue
            }

            when (currentSection) {
                "v4+ styles", "v4 styles" -> {
                    if (line.startsWith("Format:", ignoreCase = true)) {
                        styleColumns = line.substringAfter(':')
                            .split(',')
                            .map { it.trim().lowercase(Locale.ROOT) }
                    } else if (line.startsWith("Style:", ignoreCase = true) && styleColumns.isNotEmpty()) {
                        val values = line.substringAfter(':').split(',', limit = styleColumns.size)
                            .map { it.trim() }
                        val map = styleColumns.zip(values).toMap()
                        val name = map["name"] ?: continue
                        val alignRaw = map["alignment"]?.toIntOrNull() ?: 2
                        val alignment = if (currentSection == "v4 styles") {
                            // Legacy SSA alignment mapping to numpad
                            when (alignRaw) {
                                1 -> SubtitleAlignment.BOTTOM_LEFT
                                2 -> SubtitleAlignment.BOTTOM_CENTER
                                3 -> SubtitleAlignment.BOTTOM_RIGHT
                                5 -> SubtitleAlignment.TOP_LEFT
                                6 -> SubtitleAlignment.TOP_CENTER
                                7 -> SubtitleAlignment.TOP_RIGHT
                                9 -> SubtitleAlignment.MIDDLE_LEFT
                                10 -> SubtitleAlignment.MIDDLE_CENTER
                                11 -> SubtitleAlignment.MIDDLE_RIGHT
                                else -> SubtitleAlignment.BOTTOM_CENTER
                            }
                        } else {
                            SubtitleAlignment.fromAssNumpad(alignRaw)
                        }
                        val bold = map["bold"]?.let { it == "-1" || it == "1" } ?: false
                        val italic = map["italic"]?.let { it == "-1" || it == "1" } ?: false
                        val primaryColor = map["primarycolour"]?.let { parseAssColor(it) }
                        styles[name.lowercase(Locale.ROOT)] = AssStyleDef(
                            name = name,
                            alignment = alignment,
                            bold = bold,
                            italic = italic,
                            primaryColorArgb = primaryColor
                        )
                    }
                }

                "events" -> {
                    if (line.startsWith("Format:", ignoreCase = true)) {
                        eventColumns = line.substringAfter(':')
                            .split(',')
                            .map { it.trim().lowercase(Locale.ROOT) }
                    } else if (line.startsWith("Dialogue:", ignoreCase = true)) {
                        val cols = eventColumns.ifEmpty {
                            listOf(
                                "layer", "start", "end", "style", "name",
                                "marginl", "marginr", "marginv", "effect", "text"
                            )
                        }
                        val payload = line.substringAfter(':')
                        val parts = payload.split(',', limit = cols.size)
                        if (parts.size < cols.size) continue

                        val startIdx = cols.indexOf("start")
                        val endIdx = cols.indexOf("end")
                        val styleIdx = cols.indexOf("style")
                        val textIdx = cols.indexOf("text")
                        if (startIdx < 0 || endIdx < 0 || textIdx < 0) continue

                        val startMs = parseAssTime(parts[startIdx].trim()) ?: continue
                        val endMs = parseAssTime(parts[endIdx].trim()) ?: continue
                        if (endMs <= startMs) continue

                        val styleName = if (styleIdx >= 0) {
                            parts[styleIdx].trim().trimStart('*').lowercase(Locale.ROOT)
                        } else {
                            "default"
                        }
                        val styleDef = styles[styleName] ?: styles["default"]
                        val rawText = parts[textIdx]

                        // Skip pure ASS vector polygon drawings ({\p1}...{\p0})
                        if (rawText.contains("\\p1") || rawText.contains("\\p2") || rawText.contains("\\p4")) {
                            continue
                        }

                        val overrideAlign = ASS_ALIGNMENT_REGEX.find(rawText)
                            ?.groupValues?.get(1)?.toIntOrNull()
                            ?.let { SubtitleAlignment.fromAssNumpad(it) }

                        val isItalic = rawText.contains("\\i1") || (styleDef?.italic == true && !rawText.contains("\\i0"))
                        val isBold = rawText.contains("\\b1") || (styleDef?.bold == true && !rawText.contains("\\b0"))

                        val cleanText = rawText
                            .replace(ASS_OVERRIDE_BLOCK_REGEX, "")
                            .replace("\\N", "\n")
                            .replace("\\n", "\n")
                            .replace("\\h", "\u00A0")
                            .trim()

                        if (cleanText.isNotEmpty()) {
                            cues.add(
                                SubtitleCue(
                                    startMs = startMs,
                                    endMs = endMs,
                                    text = cleanText,
                                    alignment = overrideAlign ?: styleDef?.alignment ?: SubtitleAlignment.BOTTOM_CENTER,
                                    isItalic = isItalic,
                                    isBold = isBold,
                                    assStyleColorArgb = styleDef?.primaryColorArgb
                                )
                            )
                        }
                    }
                }
            }
        }
        return cues
    }

    private fun parseSrtTime(h: String, m: String, s: String, msRaw: String): Long {
        val hours = h.toLongOrNull() ?: 0L
        val mins = m.toLongOrNull() ?: 0L
        val secs = s.toLongOrNull() ?: 0L
        val millis = msRaw.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return (((hours * 60L + mins) * 60L) + secs) * 1000L + millis
    }

    private fun parseAssTime(value: String): Long? {
        // Format: H:MM:SS.cc (centiseconds) or H:MM:SS.mmm
        val match = Regex("""^(\d+):(\d{2}):(\d{2})[.:](\d{1,3})$""").find(value) ?: return null
        val hours = match.groupValues[1].toLongOrNull() ?: return null
        val mins = match.groupValues[2].toLongOrNull() ?: return null
        val secs = match.groupValues[3].toLongOrNull() ?: return null
        val fracStr = match.groupValues[4]
        val millis = when (fracStr.length) {
            1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
            2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
            else -> fracStr.take(3).toLongOrNull() ?: 0L
        }
        return (((hours * 60L + mins) * 60L) + secs) * 1000L + millis
    }

    private fun parseAssColor(raw: String): Long? {
        // ASS format: &HAABBGGRR or &HBBGGRR (where AA=00 is opaque, FF is transparent)
        val hex = raw.trim().removePrefix("&H").removePrefix("&h").removeSuffix("&")
        val parsed = hex.toLongOrNull(16) ?: return null
        val b: Long
        val g: Long
        val r: Long
        val alpha: Long
        if (hex.length >= 8) {
            val assAlpha = (parsed shr 24) and 0xFFL
            alpha = (255L - assAlpha).coerceIn(0L, 255L)
            b = (parsed shr 16) and 0xFFL
            g = (parsed shr 8) and 0xFFL
            r = parsed and 0xFFL
        } else {
            alpha = 0xFFL
            b = (parsed shr 16) and 0xFFL
            g = (parsed shr 8) and 0xFFL
            r = parsed and 0xFFL
        }
        return (alpha shl 24) or (r shl 16) or (g shl 8) or b
    }
}
