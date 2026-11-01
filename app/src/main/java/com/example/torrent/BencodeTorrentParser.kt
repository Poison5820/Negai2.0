package com.example.torrent

import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale

data class ParsedTorrentMetadata(
    val infoHashHex: String,
    val displayName: String,
    val trackers: List<String>,
    val totalLengthBytes: Long,
    val magnetUri: String,
    val fileEntries: List<TorrentReleaseFileEntry> = emptyList()
)

object BencodeTorrentParser {

    val DEFAULT_PUBLIC_TRACKERS = listOf(
        "http://bt.t-ru.org/ann?magnet",
        "http://bt2.t-ru.org/ann?magnet",
        "http://bt3.t-ru.org/ann?magnet",
        "http://bt4.t-ru.org/ann?magnet",
        "http://retracker.local/announce",
        "udp://opentor.net:6969",
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.stealth.si:80/announce",
        "udp://exodus.desync.com:6969/announce",
        "udp://tracker.torrent.eu.org:451/announce"
    )

    private val HEX_40_REGEX = Regex("""(?i)\b([0-9a-f]{40})\b""")
    private val BTIH_MAGNET_REGEX = Regex("""(?i)xt=urn:btih:([0-9a-zA-Z]{32,40})""")

    fun parseTorrentBytes(bytes: ByteArray, fallbackName: String = "Torrent"): ParsedTorrentMetadata? {
        if (bytes.isEmpty() || bytes[0] != 'd'.code.toByte()) return null
        return runCatching {
            val cursor = Cursor(bytes, 1) // skip opening 'd'
            var infoStart = -1
            var infoEnd = -1
            var announceSingle: String? = null
            val announceList = mutableListOf<String>()
            var parsedName: String? = null
            var parsedNameUtf8: String? = null
            var totalLength = 0L
            val parsedFiles = mutableListOf<TorrentReleaseFileEntry>()

            while (cursor.pos < bytes.size && bytes[cursor.pos] != 'e'.code.toByte()) {
                val key = cursor.readByteStringUtf8() ?: break
                when (key) {
                    "announce" -> {
                        announceSingle = cursor.readByteStringUtf8()
                    }
                    "announce-list" -> {
                        cursor.collectStringsInsideElement(announceList)
                    }
                    "info" -> {
                        infoStart = cursor.pos
                        if (cursor.peekChar() == 'd') {
                            cursor.pos++
                            while (cursor.pos < bytes.size && bytes[cursor.pos] != 'e'.code.toByte()) {
                                val infoKey = cursor.readByteStringUtf8() ?: break
                                when (infoKey) {
                                    "name" -> parsedName = cursor.readByteStringUtf8()
                                    "name.utf-8" -> parsedNameUtf8 = cursor.readByteStringUtf8()
                                    "length" -> totalLength += cursor.readInteger() ?: 0L
                                    "files" -> totalLength += cursor.parseFilesInList(parsedFiles)
                                    else -> cursor.skipElement()
                                }
                            }
                            if (cursor.pos < bytes.size && bytes[cursor.pos] == 'e'.code.toByte()) {
                                cursor.pos++
                            }
                        } else {
                            cursor.skipElement()
                        }
                        infoEnd = cursor.pos
                    }
                    else -> cursor.skipElement()
                }
            }

            if (infoStart < 0 || infoEnd <= infoStart || infoEnd > bytes.size) {
                return@runCatching null
            }

            val sha1 = MessageDigest.getInstance("SHA-1")
            sha1.update(bytes, infoStart, infoEnd - infoStart)
            val hashHex = sha1.digest().joinToString("") { "%02x".format(it) }

            val name = (parsedNameUtf8 ?: parsedName)?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackName
            if (parsedFiles.isEmpty() && totalLength > 0L) {
                parsedFiles.add(
                    TorrentReleaseFileEntry(
                        path = name,
                        sizeLabel = formatBencodeSize(totalLength)
                    )
                )
            }
            val trackers = buildList {
                announceSingle?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
                announceList.forEach { tr ->
                    val cleaned = tr.trim()
                    if (cleaned.isNotEmpty() && cleaned !in this) add(cleaned)
                }
                DEFAULT_PUBLIC_TRACKERS.forEach { tr ->
                    if (tr !in this) add(tr)
                }
            }

            val magnet = buildMagnetUri(hashHex, name, trackers)
            ParsedTorrentMetadata(
                infoHashHex = hashHex,
                displayName = name,
                trackers = trackers,
                totalLengthBytes = totalLength,
                magnetUri = magnet,
                fileEntries = parsedFiles
            )
        }.getOrNull()
    }

    fun extractInfoHash(rawInput: String?): String? {
        if (rawInput.isNullOrBlank()) return null
        val trimmed = rawInput.trim()

        BTIH_MAGNET_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.let { token ->
            if (token.length == 40 && token.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                return token.lowercase(Locale.ROOT)
            }
            if (token.length == 32) {
                decodeBase32ToHex(token)?.let { return it }
            }
        }

        if (trimmed.length == 40 && trimmed.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            return trimmed.lowercase(Locale.ROOT)
        }

        HEX_40_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.let {
            return it.lowercase(Locale.ROOT)
        }

        return null
    }

    fun buildMagnetUri(
        infoHashHex: String,
        displayName: String? = null,
        trackers: List<String> = DEFAULT_PUBLIC_TRACKERS
    ): String {
        val cleanHash = infoHashHex.trim().lowercase(Locale.ROOT)
        return buildString {
            append("magnet:?xt=urn:btih:")
            append(cleanHash)
            if (!displayName.isNullOrBlank()) {
                append("&dn=")
                append(urlEncode(displayName))
            }
            trackers.take(12).forEach { tr ->
                if (tr.isNotBlank()) {
                    append("&tr=")
                    append(urlEncode(tr))
                }
            }
        }
    }

    fun enrichMagnetUri(
        rawMagnet: String?,
        infoHashHex: String?,
        displayName: String? = null,
        extraTrackers: List<String> = DEFAULT_PUBLIC_TRACKERS
    ): String? {
        val trimmedMagnet = rawMagnet?.trim()?.takeIf { it.startsWith("magnet:", ignoreCase = true) }
        if (trimmedMagnet == null) {
            val hash = extractInfoHash(infoHashHex) ?: return null
            return buildMagnetUri(hash, displayName, extraTrackers)
        }
        val lowerMagnet = trimmedMagnet.lowercase(Locale.ROOT)
        return buildString {
            append(trimmedMagnet)
            if (!displayName.isNullOrBlank() && !lowerMagnet.contains("&dn=")) {
                append("&dn=")
                append(urlEncode(displayName))
            }
            extraTrackers.take(8).forEach { tr ->
                val encodedTr = urlEncode(tr)
                if (tr.isNotBlank() &&
                    !lowerMagnet.contains(encodedTr.lowercase(Locale.ROOT)) &&
                    !lowerMagnet.contains(tr.lowercase(Locale.ROOT))
                ) {
                    append("&tr=")
                    append(encodedTr)
                }
            }
        }
    }

    fun ensurePlayableOrMagnetLink(rawInput: String, fallbackTitle: String = ""): String {
        val trimmed = rawInput.trim()
        if (trimmed.startsWith("magnet:", ignoreCase = true)) {
            return enrichMagnetUri(trimmed, null, fallbackTitle) ?: trimmed
        }
        if (trimmed.length == 40 && trimmed.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            return buildMagnetUri(trimmed, fallbackTitle)
        }
        return trimmed
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun decodeBase32ToHex(base32: String): String? {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val clean = base32.uppercase(Locale.ROOT).trimEnd('=')
        var buffer = 0
        var bitsLeft = 0
        val out = ByteArray(20)
        var outIndex = 0
        for (ch in clean) {
            val v = alphabet.indexOf(ch)
            if (v < 0) return null
            buffer = (buffer shl 5) or v
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                if (outIndex >= out.size) return null
                out[outIndex++] = ((buffer shr bitsLeft) and 0xFF).toByte()
            }
        }
        if (outIndex != 20) return null
        return out.joinToString("") { "%02x".format(it) }
    }

    private class Cursor(val data: ByteArray, var pos: Int) {
        fun peekChar(): Char? = if (pos < data.size) data[pos].toInt().toChar() else null

        fun readInteger(): Long? {
            if (peekChar() != 'i') return null
            pos++
            val end = indexOfByte('e'.code.toByte(), pos)
            if (end < 0) return null
            val numStr = String(data, pos, end - pos, Charsets.US_ASCII)
            pos = end + 1
            return numStr.toLongOrNull()
        }

        fun readByteStringRaw(): ByteArray? {
            val colon = indexOfByte(':'.code.toByte(), pos)
            if (colon <= pos) return null
            val lenStr = String(data, pos, colon - pos, Charsets.US_ASCII)
            val len = lenStr.toIntOrNull() ?: return null
            if (len < 0 || colon + 1 + len > data.size) return null
            val slice = data.copyOfRange(colon + 1, colon + 1 + len)
            pos = colon + 1 + len
            return slice
        }

        fun readByteStringUtf8(): String? =
            readByteStringRaw()?.let { String(it, Charsets.UTF_8) }

        fun skipElement() {
            when (peekChar()) {
                'i' -> {
                    readInteger()
                }
                'l', 'd' -> {
                    pos++
                    while (pos < data.size && data[pos] != 'e'.code.toByte()) {
                        skipElement()
                    }
                    if (pos < data.size && data[pos] == 'e'.code.toByte()) pos++
                }
                in '0'..'9' -> {
                    val colon = indexOfByte(':'.code.toByte(), pos)
                    if (colon <= pos) {
                        pos = data.size
                        return
                    }
                    val len = String(data, pos, colon - pos, Charsets.US_ASCII).toIntOrNull()
                    if (len == null || len < 0) {
                        pos = data.size
                        return
                    }
                    pos = (colon + 1 + len).coerceAtMost(data.size)
                }
                else -> pos++
            }
        }

        fun collectStringsInsideElement(target: MutableList<String>) {
            when (peekChar()) {
                'l' -> {
                    pos++
                    while (pos < data.size && data[pos] != 'e'.code.toByte()) {
                        collectStringsInsideElement(target)
                    }
                    if (pos < data.size && data[pos] == 'e'.code.toByte()) pos++
                }
                in '0'..'9' -> {
                    readByteStringUtf8()?.let { target.add(it) }
                }
                else -> skipElement()
            }
        }

        fun parseFilesInList(targetFiles: MutableList<TorrentReleaseFileEntry>): Long {
            if (peekChar() != 'l') {
                skipElement()
                return 0L
            }
            pos++
            var sum = 0L
            while (pos < data.size && data[pos] != 'e'.code.toByte()) {
                if (peekChar() == 'd') {
                    pos++
                    var fileLen = 0L
                    val pathSegments = mutableListOf<String>()
                    val utf8Segments = mutableListOf<String>()
                    while (pos < data.size && data[pos] != 'e'.code.toByte()) {
                        val k = readByteStringUtf8() ?: break
                        when (k) {
                            "length" -> fileLen = readInteger() ?: 0L
                            "path" -> collectStringsInsideElement(pathSegments)
                            "path.utf-8" -> collectStringsInsideElement(utf8Segments)
                            else -> skipElement()
                        }
                    }
                    if (pos < data.size && data[pos] == 'e'.code.toByte()) pos++
                    sum += fileLen
                    val chosenPath = (if (utf8Segments.isNotEmpty()) utf8Segments else pathSegments)
                        .filter { it.isNotBlank() && !it.startsWith("_____padding_file") }
                        .joinToString("/")
                    if (chosenPath.isNotBlank() && targetFiles.size < 250) {
                        targetFiles.add(
                            TorrentReleaseFileEntry(
                                path = chosenPath,
                                sizeLabel = formatBencodeSize(fileLen)
                            )
                        )
                    }
                } else {
                    skipElement()
                }
            }
            if (pos < data.size && data[pos] == 'e'.code.toByte()) pos++
            return sum
        }

        private fun indexOfByte(target: Byte, from: Int): Int {
            for (i in from until data.size) {
                if (data[i] == target) return i
            }
            return -1
        }
    }

    private fun formatBencodeSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}
