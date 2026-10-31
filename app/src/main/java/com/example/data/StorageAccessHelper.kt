package com.example.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
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
}
