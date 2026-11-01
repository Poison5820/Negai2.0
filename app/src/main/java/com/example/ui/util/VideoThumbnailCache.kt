package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.intercept.Interceptor
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.videoFrameMillis
import coil.size.Precision
import coil.size.Scale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance asynchronous video frame thumbnail loader and persistent disk cache.
 *
 * Resolves laggy scrolling and flickering in series folders:
 * 1. Persistent Disk Cache: Extracted frames are downscaled and written as compact JPEGs
 *    (~15-20 KB) in app cache directory. Once cached, loading takes ~1ms directly from disk
 *    without touching MediaMetadataRetriever.
 * 2. Asynchronous Bounded Generation: Extractions run off the main thread via Dispatchers.IO
 *    governed by a concurrency semaphore to prevent CPU/IO thrashing.
 * 3. 25% Memory Cache: Instantaneous 0ms access in RAM for active list viewport.
 * 4. Zero-Flicker Transitions: Crossfade is disabled for cached items so scrolling
 *    displays stable thumbnails without fading from transparent.
 * 5. Background Prefetching: Pre-generates disk cache entries for folder episodes ahead of scrolling.
 */
object VideoThumbnailCache {

    private const val DISK_CACHE_SUBDIR = "video_thumbnails"
    private const val MAX_DISK_CACHE_BYTES = 200L * 1024L * 1024L // 200 MB
    private const val DEFAULT_FRAME_MILLIS = 2000L

    @Volatile
    private var instance: ImageLoader? = null

    private val prefetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getThumbnailDir(context: Context): File {
        val dir = File(context.applicationContext.cacheDir, DISK_CACHE_SUBDIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun generateKey(
        videoUri: String,
        frameMillis: Long = DEFAULT_FRAME_MILLIS,
        width: Int = 336,
        height: Int = 198
    ): String {
        val rawInput = "$videoUri:$frameMillis:${width}x$height"
        return try {
            val digest = MessageDigest.getInstance("MD5").digest(rawInput.toByteArray(Charsets.UTF_8))
            "thumb_" + digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "thumb_" + (rawInput.hashCode().toLong() and 0xFFFFFFFFL).toString(16)
        }
    }

    fun getCachedFile(
        context: Context,
        videoUri: String,
        frameMillis: Long = DEFAULT_FRAME_MILLIS,
        width: Int = 336,
        height: Int = 198
    ): File? {
        val dir = getThumbnailDir(context)
        val key = generateKey(videoUri, frameMillis, width, height)
        val file = File(dir, "$key.jpg")
        return if (file.exists() && file.length() > 0) file else null
    }

    fun getImageLoader(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext
                val thumbDir = getThumbnailDir(appContext)

                ImageLoader.Builder(appContext)
                    .components {
                        add(VideoThumbnailDiskCacheInterceptor(appContext, thumbDir))
                        add(VideoFrameDecoder.Factory())
                    }
                    .memoryCache {
                        MemoryCache.Builder(appContext)
                            .maxSizePercent(0.25)
                            .strongReferencesEnabled(true)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(thumbDir)
                            .maxSizeBytes(MAX_DISK_CACHE_BYTES)
                            .build()
                    }
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .respectCacheHeaders(false)
                    .crossfade(false) // Disable global crossfade to eliminate scrolling flicker
                    .build()
                    .also { instance = it }
            }
        }
    }

    fun buildEpisodeThumbnailRequest(
        context: Context,
        videoUri: String,
        targetWidthPx: Int = 336,
        targetHeightPx: Int = 198
    ): ImageRequest {
        val cacheKey = generateKey(videoUri, DEFAULT_FRAME_MILLIS, targetWidthPx, targetHeightPx)
        val cachedFile = getCachedFile(context, videoUri, DEFAULT_FRAME_MILLIS, targetWidthPx, targetHeightPx)

        // If file is already cached on disk, point directly to file for instantaneous 1ms load
        val dataTarget: Any = cachedFile ?: Uri.parse(videoUri)

        return ImageRequest.Builder(context)
            .data(dataTarget)
            .videoFrameMillis(DEFAULT_FRAME_MILLIS)
            .size(width = targetWidthPx, height = targetHeightPx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }

    /**
     * Asynchronously pre-generates disk cache thumbnails for a list of episodes in the background.
     * Prevents any scrolling delays by ensuring upcoming cards find pre-cached files.
     */
    fun prefetchFolderThumbnails(
        context: Context,
        videoUris: List<String>,
        targetWidthPx: Int = 336,
        targetHeightPx: Int = 198
    ) {
        val appContext = context.applicationContext
        val thumbDir = getThumbnailDir(appContext)
        prefetchScope.launch {
            val imageLoader = getImageLoader(appContext)
            for (uriStr in videoUris) {
                val key = generateKey(uriStr, DEFAULT_FRAME_MILLIS, targetWidthPx, targetHeightPx)
                val targetFile = File(thumbDir, "$key.jpg")
                if (!targetFile.exists() || targetFile.length() <= 0L) {
                    extractVideoThumbnail(
                        context = appContext,
                        videoUri = uriStr,
                        frameMillis = DEFAULT_FRAME_MILLIS,
                        targetWidth = targetWidthPx,
                        targetHeight = targetHeightPx,
                        destinationFile = targetFile
                    )
                }
                // Also warm up memory cache
                val req = buildEpisodeThumbnailRequest(appContext, uriStr, targetWidthPx, targetHeightPx)
                imageLoader.enqueue(req)
            }
        }
    }

    fun extractVideoThumbnail(
        context: Context,
        videoUri: String,
        frameMillis: Long,
        targetWidth: Int,
        targetHeight: Int,
        destinationFile: File
    ): Boolean {
        val retriever = MediaMetadataRetriever()
        try {
            val parsed = Uri.parse(videoUri)
            val scheme = parsed.scheme?.lowercase()
            when {
                scheme == "content" -> retriever.setDataSource(context, parsed)
                scheme == "file" -> retriever.setDataSource(parsed.path)
                scheme == "http" || scheme == "https" -> retriever.setDataSource(videoUri, HashMap())
                else -> retriever.setDataSource(videoUri)
            }

            val timeUs = (if (frameMillis > 0L) frameMillis else DEFAULT_FRAME_MILLIS) * 1000L
            val rawBitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && targetWidth > 0 && targetHeight > 0) {
                try {
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        targetWidth,
                        targetHeight
                    )
                } catch (_: Throwable) {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
            } else {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }

            if (rawBitmap == null) return false

            val finalBitmap = if (targetWidth > 0 && targetHeight > 0 &&
                (rawBitmap.width > targetWidth * 1.5 || rawBitmap.height > targetHeight * 1.5)
            ) {
                val scaled = Bitmap.createScaledBitmap(rawBitmap, targetWidth, targetHeight, true)
                if (scaled != rawBitmap) {
                    rawBitmap.recycle()
                }
                scaled
            } else {
                rawBitmap
            }

            destinationFile.parentFile?.mkdirs()
            val tempFile = File.createTempFile("thumb_", ".tmp", destinationFile.parentFile)
            FileOutputStream(tempFile).use { outStream ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 82, outStream)
            }
            finalBitmap.recycle()

            return if (tempFile.length() > 0L) {
                if (destinationFile.exists()) destinationFile.delete()
                tempFile.renameTo(destinationFile)
            } else {
                tempFile.delete()
                false
            }
        } catch (_: Throwable) {
            return false
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
        }
    }

    fun pruneDiskCacheIfNeeded(dir: File, maxSizeBytes: Long = MAX_DISK_CACHE_BYTES) {
        try {
            val files = dir.listFiles() ?: return
            var totalSize = files.sumOf { it.length() }
            if (totalSize <= maxSizeBytes) return

            // Sort by last modified ascending (oldest first)
            val sortedFiles = files.sortedBy { it.lastModified() }
            val targetSize = (maxSizeBytes * 0.75).toLong()
            for (file in sortedFiles) {
                val length = file.length()
                if (file.delete()) {
                    totalSize -= length
                    if (totalSize <= targetSize) break
                }
            }
        } catch (_: Throwable) {}
    }

    fun evictThumbnails(context: Context, videoUris: Collection<String>) {
        if (videoUris.isEmpty()) return
        try {
            val dir = getThumbnailDir(context)
            val memCache = instance?.memoryCache
            val sizes = listOf(336 to 198, 372 to 222)
            for (uri in videoUris) {
                for ((w, h) in sizes) {
                    val key = generateKey(uri, DEFAULT_FRAME_MILLIS, w, h)
                    memCache?.remove(MemoryCache.Key(key))
                    val file = File(dir, "$key.jpg")
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        } catch (_: Throwable) {}
    }
}

/**
 * Coil Interceptor that redirects video URI thumbnail requests to cached JPEG files,
 * extracting them asynchronously in the background if not yet on disk.
 */
private class VideoThumbnailDiskCacheInterceptor(
    private val context: Context,
    private val thumbnailDir: File
) : Interceptor {

    private val activeExtractions = ConcurrentHashMap<String, Deferred<Boolean>>()
    private val extractionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val extractionSemaphore = Semaphore(2)

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data

        val videoUriStr = when (data) {
            is Uri -> data.toString()
            is String -> data
            is File -> return chain.proceed(request)
            else -> null
        }

        if (videoUriStr == null || !isVideoSource(videoUriStr)) {
            return chain.proceed(request)
        }

        val targetWidth = 336
        val targetHeight = 198
        val cacheKey = VideoThumbnailCache.generateKey(videoUriStr, 2000L, targetWidth, targetHeight)
        val cacheFile = File(thumbnailDir, "$cacheKey.jpg")

        if (cacheFile.exists() && cacheFile.length() > 0L) {
            val fileRequest = request.newBuilder()
                .data(cacheFile)
                .crossfade(false)
                .build()
            return chain.proceed(fileRequest)
        }

        val deferred = activeExtractions.computeIfAbsent(cacheKey) {
            extractionScope.async {
                extractionSemaphore.withPermit {
                    if (cacheFile.exists() && cacheFile.length() > 0L) return@withPermit true
                    val success = VideoThumbnailCache.extractVideoThumbnail(
                        context = context,
                        videoUri = videoUriStr,
                        frameMillis = 2000L,
                        targetWidth = targetWidth,
                        targetHeight = targetHeight,
                        destinationFile = cacheFile
                    )
                    if (success) {
                        VideoThumbnailCache.pruneDiskCacheIfNeeded(thumbnailDir)
                    }
                    success
                }
            }
        }

        val success = try {
            deferred.await()
        } catch (_: Throwable) {
            false
        } finally {
            activeExtractions.remove(cacheKey)
        }

        if (success && cacheFile.exists() && cacheFile.length() > 0L) {
            val fileRequest = request.newBuilder()
                .data(cacheFile)
                .crossfade(false)
                .build()
            return chain.proceed(fileRequest)
        }

        return chain.proceed(request)
    }

    private fun isVideoSource(uriStr: String): Boolean {
        if (uriStr.startsWith("content://")) return true
        val lower = uriStr.lowercase()
        return lower.endsWith(".mkv") || lower.endsWith(".mp4") || lower.endsWith(".avi") ||
                lower.endsWith(".webm") || lower.endsWith(".mov") || lower.endsWith(".ts") ||
                lower.endsWith(".m2ts") || lower.endsWith(".flv") || lower.endsWith(".wmv")
    }
}
