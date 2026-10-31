package com.example.data

import android.content.Context
import com.example.model.EpisodeItem
import com.example.model.ExternalAudioTrack
import com.example.model.ExternalSubtitleTrack
import com.example.model.SubtitleFormat
import com.example.model.VideoFolderItem
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Fast synchronous/background binary snapshot cache for scanned [VideoFolderItem] library entries.
 * Eliminates the cold-start empty state and "Сканирование хранилища…" flash on app launch
 * while background delta-scanning silently refreshes any changes.
 */
object FolderLibraryCache {

    private const val CACHE_FILE_NAME = "folders_library_cache_v2.bin"
    private const val MAGIC_HEADER = 0x4B344643 // "K4FC"
    private const val FORMAT_VERSION = 2
    private const val MAX_COLLECTION_ITEMS = 50_000
    private const val MAX_STRING_BYTES = 65_536

    private val ioLock = Any()

    fun read(context: Context): List<VideoFolderItem> {
        val file = File(context.filesDir, CACHE_FILE_NAME)
        return readFromFile(file)
    }

    fun write(context: Context, folders: List<VideoFolderItem>) {
        val file = File(context.filesDir, CACHE_FILE_NAME)
        writeToFile(file, folders)
    }

    fun readFromFile(file: File): List<VideoFolderItem> = synchronized(ioLock) {
        if (!file.exists() || file.length() <= 12L) return emptyList()
        return runCatching {
            DataInputStream(BufferedInputStream(FileInputStream(file), 32 * 1024)).use { input ->
                val magic = input.readInt()
                val version = input.readInt()
                if (magic != MAGIC_HEADER || version != FORMAT_VERSION) {
                    return emptyList()
                }
                val folderCount = input.readInt().coerceIn(0, MAX_COLLECTION_ITEMS)
                val folders = ArrayList<VideoFolderItem>(folderCount.coerceAtMost(512))
                repeat(folderCount) {
                    val id = readString(input)
                    val name = readString(input)
                    val displayPath = readString(input)
                    val rootUri = readString(input)
                    val totalAutoAudioCount = input.readInt()
                    val totalAutoSubCount = input.readInt()
                    val watchedCount = input.readInt()
                    val totalSizeBytes = input.readLong()
                    val lastModified = input.readLong()
                    val lastPlayedAt = input.readLong()
                    val isHidden = input.readBoolean()

                    val epCount = input.readInt().coerceIn(0, MAX_COLLECTION_ITEMS)
                    val episodes = ArrayList<EpisodeItem>(epCount.coerceAtMost(1024))
                    repeat(epCount) {
                        episodes.add(readEpisode(input))
                    }

                    folders.add(
                        VideoFolderItem(
                            id = id,
                            name = name,
                            displayPath = displayPath,
                            rootUri = rootUri,
                            episodes = episodes,
                            totalAutoAudioCount = totalAutoAudioCount,
                            totalAutoSubCount = totalAutoSubCount,
                            watchedCount = watchedCount,
                            totalSizeBytes = totalSizeBytes,
                            lastModified = lastModified,
                            lastPlayedAt = lastPlayedAt,
                            isHidden = isHidden
                        )
                    )
                }
                folders
            }
        }.getOrElse { emptyList() }
    }

    fun writeToFile(file: File, folders: List<VideoFolderItem>) = synchronized(ioLock) {
        runCatching {
            file.parentFile?.mkdirs()
            val tmpFile = File(file.parentFile, "${file.name}.tmp")
            DataOutputStream(BufferedOutputStream(FileOutputStream(tmpFile), 32 * 1024)).use { out ->
                out.writeInt(MAGIC_HEADER)
                out.writeInt(FORMAT_VERSION)
                out.writeInt(folders.size)
                for (folder in folders) {
                    writeString(out, folder.id)
                    writeString(out, folder.name)
                    writeString(out, folder.displayPath)
                    writeString(out, folder.rootUri)
                    out.writeInt(folder.totalAutoAudioCount)
                    out.writeInt(folder.totalAutoSubCount)
                    out.writeInt(folder.watchedCount)
                    out.writeLong(folder.totalSizeBytes)
                    out.writeLong(folder.lastModified)
                    out.writeLong(folder.lastPlayedAt)
                    out.writeBoolean(folder.isHidden)

                    out.writeInt(folder.episodes.size)
                    for (ep in folder.episodes) {
                        writeEpisode(out, ep)
                    }
                }
                out.flush()
            }
            if (!tmpFile.renameTo(file)) {
                tmpFile.copyTo(file, overwrite = true)
                tmpFile.delete()
            }
        }
    }

    private fun writeEpisode(out: DataOutputStream, ep: EpisodeItem) {
        writeString(out, ep.uri)
        writeString(out, ep.fileName)
        writeString(out, ep.cleanTitle)
        if (ep.episodeNumber != null) {
            out.writeBoolean(true)
            out.writeInt(ep.episodeNumber)
        } else {
            out.writeBoolean(false)
        }
        if (ep.seasonNumber != null) {
            out.writeBoolean(true)
            out.writeInt(ep.seasonNumber)
        } else {
            out.writeBoolean(false)
        }
        writeString(out, ep.folderId)
        writeString(out, ep.folderName)
        out.writeLong(ep.sizeBytes)
        out.writeLong(ep.lastModified)
        out.writeInt(ep.videoWidth)
        out.writeInt(ep.videoHeight)
        out.writeLong(ep.resumePositionMs)
        out.writeLong(ep.durationMs)
        out.writeLong(ep.lastPlayedAt)

        out.writeInt(ep.autoAudioTracks.size)
        for (aud in ep.autoAudioTracks) {
            writeString(out, aud.uri)
            writeString(out, aud.displayName)
            writeString(out, aud.sourceFolderName)
            writeString(out, aud.extension)
            out.writeBoolean(aud.isAutoMatched)
        }

        out.writeInt(ep.autoSubtitleTracks.size)
        for (sub in ep.autoSubtitleTracks) {
            writeString(out, sub.uri)
            writeString(out, sub.displayName)
            writeString(out, sub.sourceFolderName)
            writeString(out, sub.format.name)
            out.writeBoolean(sub.isAutoMatched)
        }
    }

    private fun readEpisode(input: DataInputStream): EpisodeItem {
        val uri = readString(input)
        val fileName = readString(input)
        val cleanTitle = readString(input)
        val episodeNumber = if (input.readBoolean()) input.readInt() else null
        val seasonNumber = if (input.readBoolean()) input.readInt() else null
        val folderId = readString(input)
        val folderName = readString(input)
        val sizeBytes = input.readLong()
        val lastModified = input.readLong()
        val videoWidth = input.readInt()
        val videoHeight = input.readInt()
        val resumePositionMs = input.readLong()
        val durationMs = input.readLong()
        val lastPlayedAt = input.readLong()

        val audCount = input.readInt().coerceIn(0, 1024)
        val audioTracks = ArrayList<ExternalAudioTrack>(audCount)
        repeat(audCount) {
            audioTracks.add(
                ExternalAudioTrack(
                    uri = readString(input),
                    displayName = readString(input),
                    sourceFolderName = readString(input),
                    extension = readString(input),
                    isAutoMatched = input.readBoolean()
                )
            )
        }

        val subCount = input.readInt().coerceIn(0, 1024)
        val subTracks = ArrayList<ExternalSubtitleTrack>(subCount)
        repeat(subCount) {
            val subUri = readString(input)
            val disp = readString(input)
            val srcFolder = readString(input)
            val fmtName = readString(input)
            val isAuto = input.readBoolean()
            val fmt = SubtitleFormat.entries.firstOrNull { it.name == fmtName } ?: SubtitleFormat.SRT
            subTracks.add(
                ExternalSubtitleTrack(
                    uri = subUri,
                    displayName = disp,
                    sourceFolderName = srcFolder,
                    format = fmt,
                    isAutoMatched = isAuto
                )
            )
        }

        return EpisodeItem(
            uri = uri,
            fileName = fileName,
            cleanTitle = cleanTitle,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber,
            folderId = folderId,
            folderName = folderName,
            sizeBytes = sizeBytes,
            lastModified = lastModified,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            autoAudioTracks = audioTracks,
            autoSubtitleTracks = subTracks,
            resumePositionMs = resumePositionMs,
            durationMs = durationMs,
            lastPlayedAt = lastPlayedAt
        )
    }

    private fun writeString(out: DataOutputStream, value: String) {
        val bytes = value.encodeToByteArray()
        val len = bytes.size.coerceAtMost(MAX_STRING_BYTES)
        out.writeInt(len)
        if (len > 0) {
            out.write(bytes, 0, len)
        }
    }

    private fun readString(input: DataInputStream): String {
        val len = input.readInt()
        if (len <= 0) return ""
        if (len > MAX_STRING_BYTES) {
            throw IllegalStateException("Corrupted string length: $len")
        }
        val buf = ByteArray(len)
        input.readFully(buf)
        return buf.decodeToString()
    }
}
