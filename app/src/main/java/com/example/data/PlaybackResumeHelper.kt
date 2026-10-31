package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File

object PlaybackResumeHelper {

    const val MIN_RESUME_POSITION_MS = 1_000L

    /**
     * Computes the target playback start position in milliseconds.
     * Returns 0L when resume is disabled, saved position is negligible (< 1s),
     * or the video was already watched right up to the end.
     */
    fun computeResumePositionMs(
        positionMs: Long,
        durationMs: Long,
        resumeEnabled: Boolean
    ): Long {
        if (!resumeEnabled || positionMs < MIN_RESUME_POSITION_MS) return 0L
        if (durationMs <= 0L) return positionMs

        val remainingMs = durationMs - positionMs
        if (remainingMs <= 3_000L) return 0L
        if (durationMs >= 20_000L &&
            positionMs >= (durationMs * 98L) / 100L &&
            remainingMs <= 15_000L
        ) {
            return 0L
        }

        return positionMs.coerceAtMost((durationMs - 1_000L).coerceAtLeast(0L))
    }

    fun hasActiveResumePoint(
        positionMs: Long,
        durationMs: Long,
        resumeEnabled: Boolean = true
    ): Boolean = computeResumePositionMs(positionMs, durationMs, resumeEnabled) > 0L

    fun isLocalFileUri(rawUri: String): Boolean {
        if (rawUri.isBlank()) return false
        val scheme = runCatching { Uri.parse(rawUri).scheme?.lowercase() }.getOrNull()
        return scheme == "file" || scheme == "content"
    }

    /**
     * Verifies whether a local video file (`file://` or `content://`) still physically exists
     * and remains readable by the application. Returns false if deleted, moved, or inaccessible.
     */
    fun isMediaFileExists(context: Context, rawUri: String): Boolean {
        if (rawUri.isBlank()) return false
        val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return false
        return when (uri.scheme?.lowercase()) {
            "file" -> {
                val path = uri.path ?: return false
                val file = File(path)
                file.exists() && file.isFile
            }
            "content" -> {
                runCatching {
                    if (DocumentsContract.isDocumentUri(context, uri)) {
                        val doc = DocumentFile.fromSingleUri(context, uri)
                        if (doc == null || !doc.exists()) return@runCatching false
                    }
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
                }.getOrDefault(false)
            }
            else -> false
        }
    }

    /**
     * Extracts a canonical `(authority|documentId)` key for SAF Document URIs so that
     * the same physical file opened via "Open File" vs a SAF Folder Tree shares resume state.
     */
    fun extractCanonicalDocumentKey(context: Context, rawUri: String): String? {
        if (rawUri.isBlank()) return null
        return runCatching {
            val uri = Uri.parse(rawUri)
            if (uri.scheme.equals("content", ignoreCase = true) &&
                DocumentsContract.isDocumentUri(context, uri)
            ) {
                val authority = uri.authority ?: return@runCatching null
                val docId = DocumentsContract.getDocumentId(uri)
                if (docId.isNullOrBlank()) null else "$authority|$docId"
            } else {
                null
            }
        }.getOrNull()
    }
}
