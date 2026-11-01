package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
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
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * High-performance asynchronous 16:9 (320×180) video frame thumbnail loader and persistent disk cache.
 *
 * Optimizations in v2.5:
 * 1. Standardized 16:9 (320×180) resolution with aspect-preserving center-crop so 4:3 and 21:9
 *    videos are never stretched.
 * 2. Uses [Bitmap.Config.RGB_565] (2 bytes/pixel instead of 4 bytes/pixel) for 50% lower RAM usage.
 * 3. Shared concurrency semaphore across both foreground requests and background folder prefetching
 *    to prevent I/O and CPU contention during fast scrolling.
 */
object VideoThumbnailCache {

    private const val DISK_CACHE_SUBDIR = "video_thumbnails"
    private const val MAX_DISK_CACHE_BYTES = 200L * 1024L * 1024L // 200 MB
    private const val DEFAULT_FRAME_MILLIS = 2000L
    const val DEFAULT_THUMB_WIDTH = 320
    const val DEFAULT_THUMB_HEIGHT = 180

    @Volatile
    private var instance: ImageLoader? = null

    private val prefetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    internal val sharedExtractionSemaphore = Semaphore(2)

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
        width: Int = DEFAULT_THUMB_WIDTH,
        height: Int = DEFAULT_THUMB_HEIGHT
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
        width: Int = DEFAULT_THUMB_WIDTH,
        height: Int = DEFAULT_THUMB_HEIGHT
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
                            .maxSizePercent(0.20)
                            .strongReferencesEnabled(true)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(thumbDir)
                            .maxSizeBytes(MAX_DISK_CACHE_BYTES)
                            .build()
                    }
                    .allowRgb565(true)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .respectCacheHeaders(false)
                    .crossfade(false)
                    .build()
                    .also { instance = it }
            }
        }
    }

    fun buildEpisodeThumbnailRequest(
        context: Context,
        videoUri: String,
        targetWidthPx: Int = DEFAULT_THUMB_WIDTH,
        targetHeightPx: Int = DEFAULT_THUMB_HEIGHT
    ): ImageRequest {
        val cacheKey = generateKey(videoUri, DEFAULT_FRAME_MILLIS, targetWidthPx, targetHeightPx)
        val cachedFile = getCachedFile(context, videoUri, DEFAULT_FRAME_MILLIS, targetWidthPx, targetHeightPx)

        val dataTarget: Any = cachedFile ?: Uri.parse(videoUri)

        return ImageRequest.Builder(context)
            .data(dataTarget)
            .videoFrameMillis(DEFAULT_FRAME_MILLIS)
            .size(width = targetWidthPx, height = targetHeightPx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }

    /**
     * Asynchronously pre-generates disk cache thumbnails for a list of episodes in the background,
     * governed by [sharedExtractionSemaphore] so foreground scrolling stays 120Hz smooth.
     */
    fun prefetchFolderThumbnails(
        context: Context,
        videoUris: List<String>,
        targetWidthPx: Int = DEFAULT_THUMB_WIDTH,
        targetHeightPx: Int = DEFAULT_THUMB_HEIGHT
    ) {
        val appContext = context.applicationContext
        val thumbDir = getThumbnailDir(appContext)
        prefetchScope.launch {
            val imageLoader = getImageLoader(appContext)
            for (uriStr in videoUris) {
                val key = generateKey(uriStr, DEFAULT_FRAME_MILLIS, targetWidthPx, targetHeightPx)
                val targetFile = File(thumbDir, "$key.jpg")
                if (!targetFile.exists() || targetFile.length() <= 0L) {
                    sharedExtractionSemaphore.withPermit {
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
                    }
                }
                val req = buildEpisodeThumbnailRequest(appContext, uriStr, targetWidthPx, targetHeightPx)
                imageLoader.enqueue(req)
            }
        }
    }

    fun extractVideoThumbnail(
        context: Context,
        videoUri: String,
        frameMillis: Long,
        targetWidth: Int = DEFAULT_THUMB_WIDTH,
        targetHeight: Int = DEFAULT_THUMB_HEIGHT,
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

            val outW = if (targetWidth > 0) targetWidth else DEFAULT_THUMB_WIDTH
            val outH = if (targetHeight > 0) targetHeight else DEFAULT_THUMB_HEIGHT
            val timeUs = (if (frameMillis > 0L) frameMillis else DEFAULT_FRAME_MILLIS) * 1000L

            val rawBitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                try {
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        outW * 2,
                        outH * 2
                    )
                } catch (_: Throwable) {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
            } else {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }

            if (rawBitmap == null) return false

            val finalBitmap = centerCropToRgb565(rawBitmap, outW, outH)
            if (finalBitmap != rawBitmap) {
                rawBitmap.recycle()
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

    /**
     * Center-crops [src] to exact ([targetWidth] × [targetHeight]) in [Bitmap.Config.RGB_565]
     * so 4:3, 16:9, and 21:9 frames preserve natural proportions and consume 50% less RAM.
     */
    internal fun centerCropToRgb565(
        src: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val srcW = src.width
        val srcH = src.height
        if (srcW <= 0 || srcH <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return src
        }

        val srcAspect = srcW.toFloat() / srcH.toFloat()
        val dstAspect = targetWidth.toFloat() / targetHeight.toFloat()

        val cropRect = if (srcAspect > dstAspect) {
            // Source is wider than target (e.g. 21:9 -> 16:9) -> crop left/right
            val croppedW = (srcH * dstAspect).toInt().coerceIn(1, srcW)
            val left = (srcW - croppedW) / 2
            Rect(left, 0, left + croppedW, srcH)
        } else {
            // Source is taller than target (e.g. 4:3 -> 16:9) -> crop top/bottom
            val croppedH = (srcW / dstAspect).toInt().coerceIn(1, srcH)
            val top = (srcH - croppedH) / 2
            Rect(0, top, srcW, top + croppedH)
        }

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val dstRect = Rect(0, 0, targetWidth, targetHeight)
        canvas.drawBitmap(src, cropRect, dstRect, paint)
        return output
    }

    fun pruneDiskCacheIfNeeded(dir: File, maxSizeBytes: Long = MAX_DISK_CACHE_BYTES) {
        try {
            val files = dir.listFiles() ?: return
            var totalSize = files.sumOf { it.length() }
            if (totalSize <= maxSizeBytes) return

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
            val sizes = listOf(
                DEFAULT_THUMB_WIDTH to DEFAULT_THUMB_HEIGHT,
                336 to 198,
                372 to 222
            )
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

private class VideoThumbnailDiskCacheInterceptor(
    private val context: Context,
    private val thumbnailDir: File
) : Interceptor {

    private val activeExtractions = ConcurrentHashMap<String, Deferred<Boolean>>()
    private val extractionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

        val targetWidth = VideoThumbnailCache.DEFAULT_THUMB_WIDTH
        val targetHeight = VideoThumbnailCache.DEFAULT_THUMB_HEIGHT
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
                VideoThumbnailCache.sharedExtractionSemaphore.withPermit {
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
