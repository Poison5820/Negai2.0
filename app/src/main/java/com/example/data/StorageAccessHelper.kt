package com.example.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.example.model.VideoFolderItem
import java.io.File

data class StorageVolumeRoot(
    val rootDir: File,
    val displayName: String
)

object StorageAccessHelper {

    fun hasAllFilesAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasMediaReadPermission(context: Context): Boolean {
        if (hasAllFilesAccess(context)) return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    fun hasStorageAccess(context: Context): Boolean =
        hasAllFilesAccess(context) || hasMediaReadPermission(context)

    fun runtimeMediaPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun createManageAllFilesIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun createManageAllFilesFallbackIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
    }

    fun discoverStorageRoots(context: Context): List<StorageVolumeRoot> {
        val result = mutableListOf<StorageVolumeRoot>()
        val seenPaths = mutableSetOf<String>()

        fun addCandidate(dir: File?, label: String) {
            if (dir == null || !dir.exists() || !dir.isDirectory) return
            val canonical = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
            if (seenPaths.add(canonical)) {
                result.add(StorageVolumeRoot(rootDir = dir, displayName = label))
            }
        }

        val primary = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
        addCandidate(primary, "Внутренняя память")

        val externalAppDirs = runCatching { context.getExternalFilesDirs(null) }.getOrNull().orEmpty()
        externalAppDirs.filterNotNull().forEachIndexed { idx, appDir ->
            // Typical path: /storage/<volume>/Android/data/<pkg>/files -> walk up 4 parents
            val volumeRoot = appDir.parentFile?.parentFile?.parentFile?.parentFile
            if (volumeRoot != null) {
                val label = if (idx == 0) "Внутренняя память" else "Накопитель (${volumeRoot.name})"
                addCandidate(volumeRoot, label)
            }
        }

        return result
    }

    /**
     * Resolves a SAF Tree URI (e.g. `primary:Movies/Temp` or `<volume>:DCIM`) to a physical [File]
     * on mounted storage when possible, so folder exclusions added via SAF match File-scanned folders.
     */
    fun resolveLocalFileFromTreeUri(context: Context, treeUri: Uri): File? {
        return runCatching {
            val docId = DocumentsContract.getTreeDocumentId(treeUri) ?: return@runCatching null
            val parts = docId.split(":", limit = 2)
            val volumeId = parts.getOrNull(0).orEmpty()
            val relPath = parts.getOrNull(1).orEmpty().trim('/')

            if (volumeId.equals("primary", ignoreCase = true)) {
                val base = Environment.getExternalStorageDirectory()
                return@runCatching if (relPath.isEmpty()) base else File(base, relPath)
            }

            val roots = discoverStorageRoots(context)
            val matchedVolume = roots.firstOrNull {
                it.rootDir.name.equals(volumeId, ignoreCase = true)
            }?.rootDir ?: return@runCatching null

            if (relPath.isEmpty()) matchedVolume else File(matchedVolume, relPath)
        }.getOrNull()
    }

    /**
     * Normalizes a folder URI or file path to a canonical slash-delimited lowercase-comparable path
     * when it represents a local filesystem directory, allowing parent-folder exclusions to match
     * both the excluded directory itself and any nested subdirectories.
     */
    fun normalizeLocalPathKey(idOrPath: String): String? {
        if (idOrPath.isBlank()) return null
        if (idOrPath.startsWith("/")) {
            return idOrPath.trimEnd('/').ifEmpty { "/" }
        }
        val uri = runCatching { Uri.parse(idOrPath) }.getOrNull() ?: return null
        if (uri.scheme.equals("file", ignoreCase = true)) {
            return uri.path?.trimEnd('/')?.ifEmpty { "/" }
        }
        return null
    }

    fun isFolderExcluded(
        folderId: String,
        localDirPath: String?,
        excludedIds: Set<String>
    ): Boolean {
        if (excludedIds.isEmpty()) return false
        if (folderId in excludedIds) return true

        val candidatePath = localDirPath?.trimEnd('/')?.ifEmpty { "/" }
            ?: normalizeLocalPathKey(folderId)

        if (candidatePath != null) {
            for (excluded in excludedIds) {
                val exPath = normalizeLocalPathKey(excluded) ?: continue
                if (candidatePath == exPath || candidatePath.startsWith("$exPath/")) {
                    return true
                }
            }
        }

        // Also support hierarchical SAF tree prefixes
        for (excluded in excludedIds) {
            if (folderId.startsWith("$excluded/") || folderId.startsWith("$excluded%2F")) {
                return true
            }
        }
        return false
    }

    fun isCameraOrScreenshotsPath(dirPath: String): Boolean {
        val norm = dirPath.replace('\\', '/').trimEnd('/').lowercase()
        return norm.endsWith("/dcim/camera") ||
            norm.endsWith("/dcim/screenshots") ||
            norm.endsWith("/pictures/screenshots") ||
            norm.endsWith("/movies/screenshots") ||
            norm.contains("/dcim/camera/") ||
            norm.contains("/pictures/screenshots/")
    }

    /**
     * Resolves a raw path, `file://` URI, SAF document/tree URI, or MediaStore URI to a local [File]
     * when it resides on an accessible filesystem volume.
     */
    fun resolveLocalFileFromUri(context: Context, rawUriOrPath: String): File? {
        if (rawUriOrPath.isBlank()) return null
        if (rawUriOrPath.startsWith("/")) {
            return File(rawUriOrPath)
        }
        val uri = runCatching { Uri.parse(rawUriOrPath) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme == "file") {
            return uri.path?.let { File(it) }
        }
        if (scheme == "content") {
            // 1. SAF Document URI
            val fromDocId = runCatching {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    resolveDocIdToFile(context, docId)
                } else {
                    null
                }
            }.getOrNull()
            if (fromDocId != null) return fromDocId

            // 2. SAF Tree URI
            val fromTree = resolveLocalFileFromTreeUri(context, uri)
            if (fromTree != null) return fromTree

            // 3. MediaStore content URI
            if (uri.authority == MediaStore.AUTHORITY) {
                val fromMediaStore = runCatching {
                    @Suppress("DEPRECATION")
                    val projection = buildList {
                        add(MediaStore.MediaColumns.DATA)
                        add(MediaStore.MediaColumns.DISPLAY_NAME)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            add(MediaStore.MediaColumns.RELATIVE_PATH)
                        }
                    }.toTypedArray()
                    context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            @Suppress("DEPRECATION")
                            val dataIdx = c.getColumnIndex(MediaStore.MediaColumns.DATA)
                            if (dataIdx >= 0) {
                                val dataPath = c.getString(dataIdx)
                                if (!dataPath.isNullOrBlank()) return@use File(dataPath)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val relIdx = c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                                val nameIdx = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                                val rel = if (relIdx >= 0) c.getString(relIdx)?.trim('/') else null
                                val name = if (nameIdx >= 0) c.getString(nameIdx) else null
                                if (!rel.isNullOrBlank() && !name.isNullOrBlank()) {
                                    return@use File("/storage/emulated/0/$rel/$name")
                                }
                            }
                        }
                        null
                    }
                }.getOrNull()
                if (fromMediaStore != null) return fromMediaStore
            }
        }
        return null
    }

    private fun resolveDocIdToFile(context: Context, docId: String?): File? {
        if (docId.isNullOrBlank()) return null
        val parts = docId.split(":", limit = 2)
        val volumeId = parts.getOrNull(0).orEmpty()
        val relPath = parts.getOrNull(1).orEmpty().trim('/')
        if (volumeId.equals("primary", ignoreCase = true)) {
            val base = Environment.getExternalStorageDirectory()
            return if (relPath.isEmpty()) base else File(base, relPath)
        }
        val roots = discoverStorageRoots(context)
        val matchedVolume = roots.firstOrNull {
            it.rootDir.name.equals(volumeId, ignoreCase = true)
        }?.rootDir ?: return null
        return if (relPath.isEmpty()) matchedVolume else File(matchedVolume, relPath)
    }

    /**
     * Permanently deletes a single file (video, audio, or subtitle) from storage.
     */
    fun deleteSingleMediaUriFromStorage(context: Context, rawUri: String): Boolean {
        if (rawUri.isBlank()) return false
        var deleted = false

        // 1. Try direct File deletion first when path is resolvable
        val localFile = resolveLocalFileFromUri(context, rawUri)
        if (localFile != null && localFile.exists()) {
            deleted = runCatching {
                if (localFile.isDirectory) localFile.deleteRecursively() else localFile.delete()
            }.getOrDefault(false)
        }

        // 2. Also clean up via ContentResolver / SAF / MediaStore if applicable
        val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
        if (uri != null && uri.scheme.equals("content", ignoreCase = true)) {
            if (!deleted) {
                deleted = runCatching {
                    DocumentsContract.deleteDocument(context.contentResolver, uri)
                }.getOrDefault(false)
            }
            if (!deleted) {
                deleted = runCatching {
                    DocumentFile.fromSingleUri(context, uri)?.delete() == true
                }.getOrDefault(false)
            }
            val crDeleted = runCatching {
                context.contentResolver.delete(uri, null, null) > 0
            }.getOrDefault(false)
            if (crDeleted) deleted = true
        }

        return deleted || (localFile != null && !localFile.exists())
    }

    /**
     * Permanently deletes an entire video folder and all its contents from device storage.
     */
    fun deleteFolderFromStorage(context: Context, folder: VideoFolderItem): Boolean {
        var anyDeleted = false

        // 1. Delete all episode files and companion tracks inside the folder
        for (ep in folder.episodes) {
            if (deleteSingleMediaUriFromStorage(context, ep.uri)) {
                anyDeleted = true
            }
            for (audio in ep.autoAudioTracks) {
                deleteSingleMediaUriFromStorage(context, audio.uri)
            }
            for (sub in ep.autoSubtitleTracks) {
                deleteSingleMediaUriFromStorage(context, sub.uri)
            }
        }

        // 2. Delete the physical directory recursively if resolvable to a local File
        val localDir = resolveLocalFileFromUri(context, folder.id)
            ?: folder.episodes.firstOrNull()?.let { resolveLocalFileFromUri(context, it.uri)?.parentFile }

        if (localDir != null && localDir.exists() && localDir.isDirectory) {
            val dirRemoved = runCatching { localDir.deleteRecursively() }.getOrDefault(false)
            if (dirRemoved || !localDir.exists()) {
                anyDeleted = true
            }
        }

        // 3. Also attempt SAF Document / Tree deletion if the folder ID is a content:// URI
        if (folder.id.startsWith("content://", ignoreCase = true)) {
            val folderUri = runCatching { Uri.parse(folder.id) }.getOrNull()
            if (folderUri != null) {
                val safDeleted = runCatching {
                    DocumentsContract.deleteDocument(context.contentResolver, folderUri)
                }.getOrDefault(false) || runCatching {
                    DocumentFile.fromTreeUri(context, folderUri)?.delete() == true
                }.getOrDefault(false) || runCatching {
                    DocumentFile.fromSingleUri(context, folderUri)?.delete() == true
                }.getOrDefault(false)
                if (safDeleted) anyDeleted = true
            }
        }

        return anyDeleted
    }
}
