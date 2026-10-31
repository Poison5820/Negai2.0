package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.model.EpisodeItem
import com.example.model.VideoFolderItem
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafMediaScanner(private val context: Context) {

    private data class RawVideoEntry(
        val uri: String,
        val fileName: String,
        val sizeBytes: Long,
        val lastModified: Long,
        val videoWidth: Int = 0,
        val videoHeight: Int = 0,
        val durationMs: Long = 0L
    )

    private data class MediaStoreVideoMeta(
        val width: Int,
        val height: Int,
        val durationMs: Long
    )

    suspend fun scanAllSources(
        roots: List<LibraryRootEntity>,
        hasAllFilesAccess: Boolean,
        hasMediaReadPermission: Boolean,
        autoBindAudio: Boolean,
        autoBindSubtitles: Boolean,
        ignoreNomediaFolders: Boolean,
        excludeCameraAndScreenshots: Boolean,
        minVideoSizeMb: Int,
        progressMap: Map<String, PlaybackProgressEntity>,
        hiddenIds: Set<String>
    ): List<VideoFolderItem> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<VideoFolderItem>()
        val seenLocalDirPaths = mutableSetOf<String>()
        val minSizeBytes = minVideoSizeMb.coerceAtLeast(0).toLong() * 1024L * 1024L
        val mediaMetaMap = if (hasAllFilesAccess || hasMediaReadPermission) {
            queryMediaStoreVideoMetadataMap()
        } else {
            emptyMap()
        }

        // 1. Primary mode: scan entire device storage automatically via File API when granted
        if (hasAllFilesAccess) {
            val storageRoots = StorageAccessHelper.discoverStorageRoots(context)
            for (volume in storageRoots) {
                scanStorageVolumeRecursive(
                    dir = volume.rootDir,
                    volumeRootDir = volume.rootDir,
                    volumeDisplayName = volume.displayName,
                    autoBindAudio = autoBindAudio,
                    autoBindSubtitles = autoBindSubtitles,
                    ignoreNomediaFolders = ignoreNomediaFolders,
                    excludeCameraAndScreenshots = excludeCameraAndScreenshots,
                    minSizeBytes = minSizeBytes,
                    progressMap = progressMap,
                    mediaMetaMap = mediaMetaMap,
                    hiddenIds = hiddenIds,
                    seenLocalDirPaths = seenLocalDirPaths,
                    depth = 0,
                    outFolders = discovered
                )
            }
        } else if (hasMediaReadPermission) {
            // 2. Fallback mode: scan entire video library via MediaStore when READ_MEDIA_VIDEO is granted
            scanMediaStoreVideos(
                excludeCameraAndScreenshots = excludeCameraAndScreenshots,
                minSizeBytes = minSizeBytes,
                progressMap = progressMap,
                hiddenIds = hiddenIds,
                seenLocalDirPaths = seenLocalDirPaths,
                outFolders = discovered
            )
        }

        // 3. Also scan any extra user-connected SAF directory trees (e.g. USB OTG or external SD folders)
        for (root in roots) {
            val treeUri = runCatching { Uri.parse(root.treeUri) }.getOrNull() ?: continue
            val resolvedLocal = StorageAccessHelper.resolveLocalFileFromTreeUri(context, treeUri)
            if (resolvedLocal != null) {
                val canon = runCatching { resolvedLocal.canonicalPath }.getOrDefault(resolvedLocal.absolutePath)
                if (hasAllFilesAccess && seenLocalDirPaths.any { it == canon || it.startsWith("$canon/") }) {
                    // Already covered by full storage scan
                    continue
                }
            }

            val docTree = DocumentFile.fromTreeUri(context, treeUri) ?: continue
            if (!docTree.exists() || !docTree.isDirectory) continue

            val rootLabel = docTree.name?.takeIf { it.isNotBlank() } ?: root.displayName
            scanDocumentDirectoryRecursive(
                dir = docTree,
                rootUri = root.treeUri,
                displayPath = rootLabel,
                autoBindAudio = autoBindAudio,
                autoBindSubtitles = autoBindSubtitles,
                ignoreNomediaFolders = ignoreNomediaFolders,
                minSizeBytes = minSizeBytes,
                progressMap = progressMap,
                mediaMetaMap = mediaMetaMap,
                hiddenIds = hiddenIds,
                depth = 0,
                outFolders = discovered
            )
        }

        discovered.sortedWith { a, b ->
            MediaAutoMatcher.naturalCompare(a.name, b.name)
        }
    }

    private fun scanStorageVolumeRecursive(
        dir: File,
        volumeRootDir: File,
        volumeDisplayName: String,
        autoBindAudio: Boolean,
        autoBindSubtitles: Boolean,
        ignoreNomediaFolders: Boolean,
        excludeCameraAndScreenshots: Boolean,
        minSizeBytes: Long,
        progressMap: Map<String, PlaybackProgressEntity>,
        mediaMetaMap: Map<String, MediaStoreVideoMeta>,
        hiddenIds: Set<String>,
        seenLocalDirPaths: MutableSet<String>,
        depth: Int,
        outFolders: MutableList<VideoFolderItem>
    ) {
        if (depth > 8) return
        val canonicalPath = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
        if (!seenLocalDirPaths.add(canonicalPath)) return

        val folderId = Uri.fromFile(dir).toString()
        if (StorageAccessHelper.isFolderExcluded(folderId, dir.absolutePath, hiddenIds)) {
            return
        }
        if (excludeCameraAndScreenshots && StorageAccessHelper.isCameraOrScreenshotsPath(dir.absolutePath)) {
            return
        }

        // Skip restricted Android app private directories
        if (depth == 1 && dir.name.equals("Android", ignoreCase = true)) {
            val mediaDir = File(dir, "media")
            if (mediaDir.exists() && mediaDir.isDirectory) {
                scanStorageVolumeRecursive(
                    dir = mediaDir,
                    volumeRootDir = volumeRootDir,
                    volumeDisplayName = volumeDisplayName,
                    autoBindAudio = autoBindAudio,
                    autoBindSubtitles = autoBindSubtitles,
                    ignoreNomediaFolders = ignoreNomediaFolders,
                    excludeCameraAndScreenshots = excludeCameraAndScreenshots,
                    minSizeBytes = minSizeBytes,
                    progressMap = progressMap,
                    mediaMetaMap = mediaMetaMap,
                    hiddenIds = hiddenIds,
                    seenLocalDirPaths = seenLocalDirPaths,
                    depth = depth + 1,
                    outFolders = outFolders
                )
            }
            return
        }

        val children = dir.listFiles() ?: return
        if (depth > 0 && ignoreNomediaFolders && children.any { it.name.equals(".nomedia", ignoreCase = true) }) {
            return
        }

        val directVideos = mutableListOf<RawVideoEntry>()
        val companionAudio = mutableListOf<RawCompanionFile>()
        val companionSubs = mutableListOf<RawCompanionFile>()
        val subDirs = mutableListOf<File>()
        val folderName = if (depth == 0) volumeDisplayName else dir.name

        for (child in children) {
            val name = child.name
            if (name.startsWith(".")) continue
            if (child.isDirectory) {
                subDirs.add(child)
            } else if (child.isFile) {
                val uriStr = Uri.fromFile(child).toString()
                when {
                    MediaAutoMatcher.isVideoFile(name) -> {
                        val fileLen = child.length()
                        if (fileLen >= minSizeBytes) {
                            val meta = mediaMetaMap[child.absolutePath.lowercase()]
                                ?: mediaMetaMap["name:${name.lowercase()}:$fileLen"]
                            directVideos.add(
                                RawVideoEntry(
                                    uri = uriStr,
                                    fileName = name,
                                    sizeBytes = fileLen,
                                    lastModified = child.lastModified(),
                                    videoWidth = meta?.width ?: 0,
                                    videoHeight = meta?.height ?: 0,
                                    durationMs = meta?.durationMs ?: 0L
                                )
                            )
                        }
                    }
                    MediaAutoMatcher.isAudioFile(name) -> {
                        companionAudio.add(
                            RawCompanionFile(
                                uri = uriStr,
                                fileName = name,
                                parentFolderName = folderName,
                                isSameDirectoryAsVideo = true
                            )
                        )
                    }
                    MediaAutoMatcher.isSubtitleFile(name) -> {
                        companionSubs.add(
                            RawCompanionFile(
                                uri = uriStr,
                                fileName = name,
                                parentFolderName = folderName,
                                isSameDirectoryAsVideo = true
                            )
                        )
                    }
                }
            }
        }

        if (directVideos.isNotEmpty()) {
            for (subDir in subDirs) {
                collectNestedCompanionsFromLocalDir(
                    subDir = subDir,
                    labelPrefix = subDir.name,
                    depth = 0,
                    outAudio = companionAudio,
                    outSubs = companionSubs
                )
            }

            val relPath = dir.absolutePath
                .removePrefix(volumeRootDir.absolutePath)
                .trimStart('/')
            val displayPath = if (relPath.isEmpty()) {
                volumeDisplayName
            } else {
                "$volumeDisplayName / ${relPath.replace("/", " / ")}"
            }

            outFolders.add(
                assembleFolderItem(
                    folderId = folderId,
                    folderName = folderName,
                    displayPath = displayPath,
                    rootUri = Uri.fromFile(volumeRootDir).toString(),
                    rawVideos = directVideos,
                    companionAudio = if (autoBindAudio) companionAudio else emptyList(),
                    companionSubs = if (autoBindSubtitles) companionSubs else emptyList(),
                    progressMap = progressMap,
                    isHidden = false
                )
            )
        }

        for (subDir in subDirs) {
            scanStorageVolumeRecursive(
                dir = subDir,
                volumeRootDir = volumeRootDir,
                volumeDisplayName = volumeDisplayName,
                autoBindAudio = autoBindAudio,
                autoBindSubtitles = autoBindSubtitles,
                ignoreNomediaFolders = ignoreNomediaFolders,
                excludeCameraAndScreenshots = excludeCameraAndScreenshots,
                minSizeBytes = minSizeBytes,
                progressMap = progressMap,
                mediaMetaMap = mediaMetaMap,
                hiddenIds = hiddenIds,
                seenLocalDirPaths = seenLocalDirPaths,
                depth = depth + 1,
                outFolders = outFolders
            )
        }
    }

    private fun scanMediaStoreVideos(
        excludeCameraAndScreenshots: Boolean,
        minSizeBytes: Long,
        progressMap: Map<String, PlaybackProgressEntity>,
        hiddenIds: Set<String>,
        seenLocalDirPaths: MutableSet<String>,
        outFolders: MutableList<VideoFolderItem>
    ) {
        runCatching {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val projection = buildList {
                add(MediaStore.Video.Media._ID)
                add(MediaStore.Video.Media.DISPLAY_NAME)
                add(MediaStore.Video.Media.SIZE)
                add(MediaStore.Video.Media.DATE_MODIFIED)
                add(MediaStore.Video.Media.WIDTH)
                add(MediaStore.Video.Media.HEIGHT)
                add(MediaStore.Video.Media.DURATION)
                add(MediaStore.Video.Media.BUCKET_ID)
                add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(MediaStore.Video.Media.RELATIVE_PATH)
                }
            }.toTypedArray()

            data class BucketGroup(
                val folderId: String,
                val folderName: String,
                val displayPath: String,
                val localPathHint: String?,
                val videos: MutableList<RawVideoEntry> = mutableListOf()
            )

            val grouped = linkedMapOf<String, BucketGroup>()

            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val fileName = cursor.getString(nameCol) ?: continue
                    val sizeBytes = cursor.getLong(sizeCol)
                    if (sizeBytes < minSizeBytes) continue
                    val dateModifiedSec = cursor.getLong(dateCol)
                    val vidW = if (widthCol >= 0) cursor.getInt(widthCol).coerceAtLeast(0) else 0
                    val vidH = if (heightCol >= 0) cursor.getInt(heightCol).coerceAtLeast(0) else 0
                    val durMs = if (durationCol >= 0) cursor.getLong(durationCol).coerceAtLeast(0L) else 0L
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "default" else "default"
                    val bucketName = (if (bucketNameCol >= 0) cursor.getString(bucketNameCol) else null)
                        ?.takeIf { it.isNotBlank() } ?: "Видео"
                    val relPath = (if (relPathCol >= 0) cursor.getString(relPathCol) else null)
                        ?.trim('/')
                        .orEmpty()

                    val virtualPath = if (relPath.isNotEmpty()) "/storage/emulated/0/$relPath" else null
                    if (excludeCameraAndScreenshots && virtualPath != null &&
                        StorageAccessHelper.isCameraOrScreenshotsPath(virtualPath)
                    ) {
                        continue
                    }

                    val folderId = if (virtualPath != null) {
                        Uri.fromFile(File(virtualPath)).toString()
                    } else {
                        "mediastore://bucket/$bucketId"
                    }

                    if (StorageAccessHelper.isFolderExcluded(folderId, virtualPath, hiddenIds)) {
                        continue
                    }

                    val displayPath = if (relPath.isNotEmpty()) {
                        "Хранилище / ${relPath.replace("/", " / ")}"
                    } else {
                        "Хранилище / $bucketName"
                    }

                    val contentUri = ContentUris.withAppendedId(collection, id).toString()
                    val group = grouped.getOrPut(folderId) {
                        BucketGroup(
                            folderId = folderId,
                            folderName = bucketName,
                            displayPath = displayPath,
                            localPathHint = virtualPath
                        )
                    }
                    group.videos.add(
                        RawVideoEntry(
                            uri = contentUri,
                            fileName = fileName,
                            sizeBytes = sizeBytes,
                            lastModified = dateModifiedSec * 1000L,
                            videoWidth = vidW,
                            videoHeight = vidH,
                            durationMs = durMs
                        )
                    )
                }
            }

            for (group in grouped.values) {
                if (group.videos.isEmpty()) continue
                group.localPathHint?.let { seenLocalDirPaths.add(it) }
                outFolders.add(
                    assembleFolderItem(
                        folderId = group.folderId,
                        folderName = group.folderName,
                        displayPath = group.displayPath,
                        rootUri = group.folderId,
                        rawVideos = group.videos,
                        companionAudio = emptyList(),
                        companionSubs = emptyList(),
                        progressMap = progressMap,
                        isHidden = false
                    )
                )
            }
        }
    }

    private fun scanDocumentDirectoryRecursive(
        dir: DocumentFile,
        rootUri: String,
        displayPath: String,
        autoBindAudio: Boolean,
        autoBindSubtitles: Boolean,
        ignoreNomediaFolders: Boolean,
        minSizeBytes: Long,
        progressMap: Map<String, PlaybackProgressEntity>,
        mediaMetaMap: Map<String, MediaStoreVideoMeta>,
        hiddenIds: Set<String>,
        depth: Int,
        outFolders: MutableList<VideoFolderItem>
    ) {
        if (depth > 5) return
        val folderId = dir.uri.toString()
        if (StorageAccessHelper.isFolderExcluded(folderId, null, hiddenIds)) return

        val children = runCatching { dir.listFiles() }.getOrNull() ?: return
        if (depth > 0 && ignoreNomediaFolders &&
            children.any { it.name?.equals(".nomedia", ignoreCase = true) == true }
        ) {
            return
        }

        val directVideos = mutableListOf<RawVideoEntry>()
        val companionAudio = mutableListOf<RawCompanionFile>()
        val companionSubs = mutableListOf<RawCompanionFile>()
        val subDirs = mutableListOf<DocumentFile>()

        val dirName = dir.name?.takeIf { it.isNotBlank() } ?: displayPath.substringAfterLast('/')

        for (child in children) {
            val name = child.name ?: continue
            if (name.startsWith(".")) continue
            if (child.isDirectory) {
                subDirs.add(child)
            } else if (child.isFile) {
                when {
                    MediaAutoMatcher.isVideoFile(name) -> {
                        val len = child.length()
                        if (len >= minSizeBytes) {
                            val meta = mediaMetaMap["name:${name.lowercase()}:$len"]
                            directVideos.add(
                                RawVideoEntry(
                                    uri = child.uri.toString(),
                                    fileName = name,
                                    sizeBytes = len,
                                    lastModified = child.lastModified(),
                                    videoWidth = meta?.width ?: 0,
                                    videoHeight = meta?.height ?: 0,
                                    durationMs = meta?.durationMs ?: 0L
                                )
                            )
                        }
                    }
                    MediaAutoMatcher.isAudioFile(name) -> {
                        companionAudio.add(
                            RawCompanionFile(
                                uri = child.uri.toString(),
                                fileName = name,
                                parentFolderName = dirName,
                                isSameDirectoryAsVideo = true
                            )
                        )
                    }
                    MediaAutoMatcher.isSubtitleFile(name) -> {
                        companionSubs.add(
                            RawCompanionFile(
                                uri = child.uri.toString(),
                                fileName = name,
                                parentFolderName = dirName,
                                isSameDirectoryAsVideo = true
                            )
                        )
                    }
                }
            }
        }

        if (directVideos.isNotEmpty()) {
            for (subDir in subDirs) {
                collectNestedCompanionsFromDocDir(
                    subDir = subDir,
                    labelPrefix = subDir.name ?: "Audio",
                    depth = 0,
                    outAudio = companionAudio,
                    outSubs = companionSubs
                )
            }

            outFolders.add(
                assembleFolderItem(
                    folderId = folderId,
                    folderName = dirName,
                    displayPath = displayPath,
                    rootUri = rootUri,
                    rawVideos = directVideos,
                    companionAudio = if (autoBindAudio) companionAudio else emptyList(),
                    companionSubs = if (autoBindSubtitles) companionSubs else emptyList(),
                    progressMap = progressMap,
                    isHidden = false
                )
            )
        }

        for (subDir in subDirs) {
            val subName = subDir.name ?: continue
            scanDocumentDirectoryRecursive(
                dir = subDir,
                rootUri = rootUri,
                displayPath = "$displayPath / $subName",
                autoBindAudio = autoBindAudio,
                autoBindSubtitles = autoBindSubtitles,
                ignoreNomediaFolders = ignoreNomediaFolders,
                minSizeBytes = minSizeBytes,
                progressMap = progressMap,
                mediaMetaMap = mediaMetaMap,
                hiddenIds = hiddenIds,
                depth = depth + 1,
                outFolders = outFolders
            )
        }
    }

    private fun collectNestedCompanionsFromDocDir(
        subDir: DocumentFile,
        labelPrefix: String,
        depth: Int,
        outAudio: MutableList<RawCompanionFile>,
        outSubs: MutableList<RawCompanionFile>
    ) {
        if (depth > 2) return
        val children = runCatching { subDir.listFiles() }.getOrNull() ?: return
        val hasVideosInside = children.any { it.isFile && it.name != null && MediaAutoMatcher.isVideoFile(it.name!!) }
        if (hasVideosInside) return

        for (item in children) {
            val itemName = item.name ?: continue
            if (itemName.startsWith(".")) continue
            if (item.isDirectory) {
                collectNestedCompanionsFromDocDir(
                    subDir = item,
                    labelPrefix = "$labelPrefix / $itemName",
                    depth = depth + 1,
                    outAudio = outAudio,
                    outSubs = outSubs
                )
            } else if (item.isFile) {
                when {
                    MediaAutoMatcher.isAudioFile(itemName) -> {
                        outAudio.add(
                            RawCompanionFile(
                                uri = item.uri.toString(),
                                fileName = itemName,
                                parentFolderName = labelPrefix,
                                isSameDirectoryAsVideo = false
                            )
                        )
                    }
                    MediaAutoMatcher.isSubtitleFile(itemName) -> {
                        outSubs.add(
                            RawCompanionFile(
                                uri = item.uri.toString(),
                                fileName = itemName,
                                parentFolderName = labelPrefix,
                                isSameDirectoryAsVideo = false
                            )
                        )
                    }
                }
            }
        }
    }

    private fun collectNestedCompanionsFromLocalDir(
        subDir: File,
        labelPrefix: String,
        depth: Int,
        outAudio: MutableList<RawCompanionFile>,
        outSubs: MutableList<RawCompanionFile>
    ) {
        if (depth > 2) return
        val children = subDir.listFiles() ?: return
        if (children.any { it.isFile && MediaAutoMatcher.isVideoFile(it.name) }) return

        for (item in children) {
            if (item.name.startsWith(".")) continue
            if (item.isDirectory) {
                collectNestedCompanionsFromLocalDir(
                    subDir = item,
                    labelPrefix = "$labelPrefix / ${item.name}",
                    depth = depth + 1,
                    outAudio = outAudio,
                    outSubs = outSubs
                )
            } else if (item.isFile) {
                val uriStr = Uri.fromFile(item).toString()
                when {
                    MediaAutoMatcher.isAudioFile(item.name) -> {
                        outAudio.add(
                            RawCompanionFile(
                                uri = uriStr,
                                fileName = item.name,
                                parentFolderName = labelPrefix,
                                isSameDirectoryAsVideo = false
                            )
                        )
                    }
                    MediaAutoMatcher.isSubtitleFile(item.name) -> {
                        outSubs.add(
                            RawCompanionFile(
                                uri = uriStr,
                                fileName = item.name,
                                parentFolderName = labelPrefix,
                                isSameDirectoryAsVideo = false
                            )
                        )
                    }
                }
            }
        }
    }

    private fun assembleFolderItem(
        folderId: String,
        folderName: String,
        displayPath: String,
        rootUri: String,
        rawVideos: List<RawVideoEntry>,
        companionAudio: List<RawCompanionFile>,
        companionSubs: List<RawCompanionFile>,
        progressMap: Map<String, PlaybackProgressEntity>,
        isHidden: Boolean
    ): VideoFolderItem {
        val sortedVideos = rawVideos.sortedWith { a, b ->
            MediaAutoMatcher.naturalCompare(a.fileName, b.fileName)
        }
        val totalVideos = sortedVideos.size

        val episodes = sortedVideos.map { raw ->
            val parsed = MediaAutoMatcher.parseEpisodeInfo(raw.fileName)
            val matchedAudio = MediaAutoMatcher.matchAudioTracksForEpisode(
                videoFileName = raw.fileName,
                totalVideosInFolder = totalVideos,
                candidates = companionAudio
            )
            val matchedSubs = MediaAutoMatcher.matchSubtitleTracksForEpisode(
                videoFileName = raw.fileName,
                totalVideosInFolder = totalVideos,
                candidates = companionSubs
            )
            val progress = progressMap[raw.uri]

            EpisodeItem(
                uri = raw.uri,
                fileName = raw.fileName,
                cleanTitle = parsed.cleanTitle,
                episodeNumber = parsed.episode,
                seasonNumber = parsed.season,
                folderId = folderId,
                folderName = folderName,
                sizeBytes = raw.sizeBytes,
                lastModified = raw.lastModified,
                videoWidth = if (raw.videoWidth > 0) raw.videoWidth else (progress?.videoWidth ?: 0),
                videoHeight = if (raw.videoHeight > 0) raw.videoHeight else (progress?.videoHeight ?: 0),
                autoAudioTracks = matchedAudio,
                autoSubtitleTracks = matchedSubs,
                resumePositionMs = progress?.positionMs ?: 0L,
                durationMs = (progress?.durationMs ?: 0L).coerceAtLeast(raw.durationMs),
                lastPlayedAt = progress?.updatedAt ?: 0L
            )
        }

        val totalAutoAudio = episodes.sumOf { it.autoAudioTracks.size }
        val totalAutoSubs = episodes.sumOf { it.autoSubtitleTracks.size }
        val watchedCount = episodes.count { ep ->
            ep.durationMs > 0L && ep.progressFraction >= 0.85f
        }
        val totalSizeBytes = episodes.sumOf { it.sizeBytes }
        val lastModified = episodes.maxOfOrNull { it.lastModified } ?: 0L
        val lastPlayedAt = episodes.maxOfOrNull { ep ->
            progressMap[ep.uri]?.updatedAt ?: 0L
        } ?: 0L

        return VideoFolderItem(
            id = folderId,
            name = folderName,
            displayPath = displayPath,
            rootUri = rootUri,
            episodes = episodes,
            totalAutoAudioCount = totalAutoAudio,
            totalAutoSubCount = totalAutoSubs,
            watchedCount = watchedCount,
            totalSizeBytes = totalSizeBytes,
            lastModified = lastModified,
            lastPlayedAt = lastPlayedAt,
            isHidden = isHidden
        )
    }

    private fun queryMediaStoreVideoMetadataMap(): Map<String, MediaStoreVideoMeta> {
        val out = HashMap<String, MediaStoreVideoMeta>(256)
        runCatching {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            @Suppress("DEPRECATION")
            val projection = buildList {
                add(MediaStore.Video.Media.DISPLAY_NAME)
                add(MediaStore.Video.Media.SIZE)
                add(MediaStore.Video.Media.WIDTH)
                add(MediaStore.Video.Media.HEIGHT)
                add(MediaStore.Video.Media.DURATION)
                add(MediaStore.Video.Media.DATA)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(MediaStore.Video.Media.RELATIVE_PATH)
                }
            }.toTypedArray()

            context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                @Suppress("DEPRECATION")
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val relCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val w = if (widthCol >= 0) cursor.getInt(widthCol).coerceAtLeast(0) else 0
                    val h = if (heightCol >= 0) cursor.getInt(heightCol).coerceAtLeast(0) else 0
                    val dur = if (durCol >= 0) cursor.getLong(durCol).coerceAtLeast(0L) else 0L
                    if (w <= 0 && h <= 0 && dur <= 0L) continue

                    val meta = MediaStoreVideoMeta(width = w, height = h, durationMs = dur)
                    val fileName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L

                    if (dataCol >= 0) {
                        val absPath = cursor.getString(dataCol)
                        if (!absPath.isNullOrBlank()) {
                            out[absPath.lowercase()] = meta
                        }
                    }
                    if (relCol >= 0 && !fileName.isNullOrBlank()) {
                        val rel = cursor.getString(relCol)?.trim('/')
                        if (!rel.isNullOrEmpty()) {
                            out["/storage/emulated/0/$rel/$fileName".lowercase()] = meta
                        }
                    }
                    if (!fileName.isNullOrBlank() && sizeBytes > 0L) {
                        out.putIfAbsent("name:${fileName.lowercase()}:$sizeBytes", meta)
                    }
                }
            }
        }
        return out
    }

    fun resolveStandaloneVideoEpisode(uri: Uri): EpisodeItem {
        val name = resolveDisplayName(uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Видео"
        val parsed = MediaAutoMatcher.parseEpisodeInfo(name)
        return EpisodeItem(
            uri = uri.toString(),
            fileName = name,
            cleanTitle = parsed.cleanTitle.ifBlank { name },
            episodeNumber = parsed.episode,
            seasonNumber = parsed.season,
            folderId = "standalone",
            folderName = "Одиночное видео"
        )
    }

    fun resolveDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()
    }
}
