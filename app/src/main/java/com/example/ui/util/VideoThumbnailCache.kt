package com.example.ui.util

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import coil.size.Scale

/**
 * High-performance centralized video frame thumbnail loader & cache.
 *
 * Why standard video thumbnails stuttered during scrolling:
 * 1. Default Coil setup without [DiskCache] never persisted decoded video frames,
 *    causing MediaMetadataRetriever to re-open and decode multi-gigabyte video files
 *    every time an item was scrolled back into view.
 * 2. Unconstrained frame decoding extracted full 4K / 1080p raw bitmaps (30+ MB each),
 *    causing massive GC pressure and micro-stutters.
 *
 * This singleton provides:
 * - 25% app heap [MemoryCache] for instant 0ms thumbnail display on scroll.
 * - 200 MB persistent [DiskCache] in app cache dir, surviving process restarts.
 * - Hardware downsampling to target thumbnail dimensions (336x198 px) via [Precision.INEXACT].
 * - Deterministic memory & disk cache keys per video URI.
 */
object VideoThumbnailCache {

    @Volatile
    private var instance: ImageLoader? = null

    fun getImageLoader(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    add(VideoFrameDecoder.Factory())
                }
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.25)
                        .strongReferencesEnabled(true)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("video_thumbnails"))
                        .maxSizeBytes(200L * 1024L * 1024L) // 200 MB persistent thumbnail cache
                        .build()
                }
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .respectCacheHeaders(false)
                .crossfade(100)
                .build()
                .also { instance = it }
        }
    }

    fun buildEpisodeThumbnailRequest(
        context: Context,
        videoUri: String,
        targetWidthPx: Int = 336,
        targetHeightPx: Int = 198
    ): ImageRequest {
        val parsedUri = Uri.parse(videoUri)
        val cacheKey = "video_thumb_${videoUri.hashCode()}_2000"
        return ImageRequest.Builder(context)
            .data(parsedUri)
            .videoFrameMillis(2000L)
            .size(width = targetWidthPx, height = targetHeightPx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(100)
            .build()
    }
}
