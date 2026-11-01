package com.example

import android.content.pm.ActivityInfo
import com.example.anime4k.FrameInterpolationShaders
import com.example.data.FolderLibraryCache
import com.example.model.Anime4KPreset
import com.example.model.EpisodeItem
import com.example.model.ExternalAudioTrack
import com.example.model.ExternalSubtitleTrack
import com.example.model.FrameInterpolationMode
import com.example.model.PlayerOrientationMode
import com.example.model.PlayerQuickAction
import com.example.model.PlayerSettings
import com.example.model.QuickActionsSerializer
import com.example.model.SubtitleFormat
import com.example.model.VideoFolderItem
import com.example.player.VideoChapterExtractor
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterpolationOrientationAndChaptersTest {

    @Test
    fun defaultOrientationFollowsVideoAspect_horizontalLandscapeAndVerticalPortrait() {
        val defaults = PlayerSettings()
        assertEquals(PlayerOrientationMode.VIDEO_ASPECT, defaults.defaultPlayerOrientation)
        assertTrue(defaults.highRefreshRateEnabled)
        assertFalse(defaults.frameInterpolationEnabled)
        assertEquals(FrameInterpolationMode.OFF, defaults.frameInterpolationMode)
        assertFalse(defaults.anime4kEnabled)
        assertEquals(Anime4KPreset.OFF, defaults.anime4kPreset)
        assertTrue(defaults.compactUiMode)
        assertTrue(defaults.enableEmbeddedChapters)
        assertTrue(defaults.showChapterMarkersOnTimeline)
        assertTrue(defaults.showEpisodesButtonInHud)

        // 16:9 horizontal video -> landscape
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            PlayerOrientationMode.VIDEO_ASPECT.resolveActivityOrientation(1920, 1080)
        )
        // 1:1 square video -> landscape
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            PlayerOrientationMode.VIDEO_ASPECT.resolveActivityOrientation(1080, 1080)
        )
        // 9:16 vertical video -> portrait
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
            PlayerOrientationMode.VIDEO_ASPECT.resolveActivityOrientation(1080, 1920)
        )
        // Unknown dimensions (0x0 before first frame) -> safe landscape default
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            PlayerOrientationMode.VIDEO_ASPECT.resolveActivityOrientation(0, 0)
        )
    }

    @Test
    fun matroskaEbmlChapterExtractor_parsesEmbeddedChaptersAndComputesEndTimes() {
        val mkvBytes = buildMinimalMkvWithChapters(
            listOf(
                0L to "Пролог",
                90_000_000_000L to "Опенинг",
                175_000_000_000L to "Основная часть",
                1_320_000_000_000L to "Эндинг"
            )
        )

        val chapters = VideoChapterExtractor.parseMkvBytes(mkvBytes, totalDurationMs = 1_440_000L)
        assertEquals(4, chapters.size)

        assertEquals("Пролог", chapters[0].title)
        assertEquals(0L, chapters[0].startTimeMs)
        assertEquals(90_000L, chapters[0].endTimeMs)

        assertEquals("Опенинг", chapters[1].title)
        assertEquals(90_000L, chapters[1].startTimeMs)
        assertEquals(175_000L, chapters[1].endTimeMs)

        assertEquals("Основная часть", chapters[2].title)
        assertEquals(175_000L, chapters[2].startTimeMs)
        assertEquals(1_320_000L, chapters[2].endTimeMs)

        assertEquals("Эндинг", chapters[3].title)
        assertEquals(1_320_000L, chapters[3].startTimeMs)
        assertEquals(1_440_000L, chapters[3].endTimeMs)
    }

    @Test
    fun mp4NeroChplExtractor_parsesMoovUdtaChplChapters() {
        val mp4Bytes = buildMinimalMp4WithChpl(
            listOf(
                0L to "Intro",
                85_000L to "Part A",
                720_000L to "Part B"
            )
        )

        val chapters = VideoChapterExtractor.parseMp4Bytes(mp4Bytes, totalDurationMs = 1_420_000L)
        assertEquals(3, chapters.size)
        assertEquals("Intro", chapters[0].title)
        assertEquals(0L, chapters[0].startTimeMs)
        assertEquals(85_000L, chapters[0].endTimeMs)

        assertEquals("Part A", chapters[1].title)
        assertEquals(85_000L, chapters[1].startTimeMs)
        assertEquals(720_000L, chapters[1].endTimeMs)

        assertEquals("Part B", chapters[2].title)
        assertEquals(720_000L, chapters[2].startTimeMs)
        assertEquals(1_420_000L, chapters[2].endTimeMs)
    }

    @Test
    fun folderLibraryCache_roundTripsFoldersEpisodesAndCompanionTracksAtomically() {
        val tempDir = Files.createTempDirectory("kino4k_cache_test").toFile()
        try {
            val cacheFile = tempDir.resolve("folders_library_cache_v1.bin")
            val original = listOf(
                VideoFolderItem(
                    id = "file:///storage/emulated/0/Anime/Frieren",
                    name = "Sousou no Frieren",
                    displayPath = "/Anime/Frieren",
                    rootUri = "file:///storage/emulated/0",
                    episodes = listOf(
                        EpisodeItem(
                            uri = "file:///storage/emulated/0/Anime/Frieren/01.mkv",
                            fileName = "01.mkv",
                            cleanTitle = "Конец приключения",
                            episodeNumber = 1,
                            seasonNumber = 1,
                            folderId = "file:///storage/emulated/0/Anime/Frieren",
                            folderName = "Sousou no Frieren",
                            sizeBytes = 1_450_000_000L,
                            lastModified = 1_700_000_000L,
                            autoAudioTracks = listOf(
                                ExternalAudioTrack(
                                    uri = "file:///storage/emulated/0/Anime/Frieren/RUS/01.mka",
                                    displayName = "01.mka",
                                    sourceFolderName = "AniLibria",
                                    extension = "mka",
                                    isAutoMatched = true
                                )
                            ),
                            autoSubtitleTracks = listOf(
                                ExternalSubtitleTrack(
                                    uri = "file:///storage/emulated/0/Anime/Frieren/Subs/01.ass",
                                    displayName = "01.ass",
                                    sourceFolderName = "Subs",
                                    format = SubtitleFormat.ASS,
                                    isAutoMatched = true
                                )
                            ),
                            resumePositionMs = 420_000L,
                            durationMs = 1_440_000L,
                            lastPlayedAt = 1_700_050_000L
                        )
                    ),
                    totalAutoAudioCount = 1,
                    totalAutoSubCount = 1,
                    watchedCount = 0,
                    totalSizeBytes = 1_450_000_000L,
                    lastModified = 1_700_000_000L,
                    lastPlayedAt = 1_700_050_000L,
                    isHidden = false
                )
            )

            FolderLibraryCache.writeToFile(cacheFile, original)
            val restored = FolderLibraryCache.readFromFile(cacheFile)
            assertEquals(original, restored)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun frameInterpolationShadersAndQuickActions_serializeAndContainValidMemcUniforms() {
        assertEquals(FrameInterpolationMode.ANIME, FrameInterpolationMode.fromId("anime"))
        assertEquals(FrameInterpolationMode.MAX_FPS, FrameInterpolationMode.fromId("MAX_FPS"))
        assertTrue(FrameInterpolationShaders.MEMC_MOTION_ESTIMATE_FRAGMENT.contains("uPrevTex"))
        assertTrue(FrameInterpolationShaders.MEMC_MOTION_SMOOTH_FRAGMENT.contains("uMotionTex"))
        assertTrue(FrameInterpolationShaders.MEMC_SYNTHESIZE_FRAGMENT.contains("uAlpha"))
        assertTrue(FrameInterpolationShaders.MEMC_SYNTHESIZE_FRAGMENT.contains("uEdgeProtect"))

        val actions = listOf(
            PlayerQuickAction.FRAME_INTERPOLATION,
            PlayerQuickAction.SPEED,
            PlayerQuickAction.EPISODES,
            PlayerQuickAction.ANIME4K
        )
        val raw = QuickActionsSerializer.serialize(actions)
        assertEquals(actions, QuickActionsSerializer.parse(raw))
    }

    // ---------------- Binary Container Builders for Deterministic Tests ----------------

    private fun buildMinimalMkvWithChapters(chaptersNs: List<Pair<Long, String>>): ByteArray {
        val atomsOut = ByteArrayOutputStream()
        for ((startNs, title) in chaptersNs) {
            val titleBytes = title.toByteArray(Charsets.UTF_8)
            val chapString = ebmlElement(byteArrayOf(0x85.toByte()), titleBytes)
            val chapDisplay = ebmlElement(byteArrayOf(0x80.toByte()), chapString)
            val timeStart = ebmlElement(byteArrayOf(0x91.toByte()), uint64Bytes(startNs))
            val atomPayload = ByteArrayOutputStream().apply {
                write(timeStart)
                write(chapDisplay)
            }.toByteArray()
            atomsOut.write(ebmlElement(byteArrayOf(0xB6.toByte()), atomPayload))
        }
        val editionEntry = ebmlElement(byteArrayOf(0x45, 0xB9.toByte()), atomsOut.toByteArray())
        val chaptersElem = ebmlElement(
            byteArrayOf(0x10, 0x43, 0xA7.toByte(), 0x70),
            editionEntry
        )
        val ebmlHeader = ebmlElement(
            byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte()),
            byteArrayOf(0x42, 0x86.toByte(), 0x81.toByte(), 0x01)
        )
        val segmentElem = ebmlElement(
            byteArrayOf(0x18, 0x53, 0x80.toByte(), 0x67),
            chaptersElem
        )
        return ByteArrayOutputStream().apply {
            write(ebmlHeader)
            write(segmentElem)
        }.toByteArray()
    }

    private fun ebmlElement(id: ByteArray, payload: ByteArray): ByteArray {
        val len = payload.size
        // 4-byte VINT size: 0x10000000 | len
        val vint = byteArrayOf(
            (0x10 or ((len ushr 24) and 0x0F)).toByte(),
            ((len ushr 16) and 0xFF).toByte(),
            ((len ushr 8) and 0xFF).toByte(),
            (len and 0xFF).toByte()
        )
        return ByteArrayOutputStream(id.size + 4 + len).apply {
            write(id)
            write(vint)
            write(payload)
        }.toByteArray()
    }

    private fun uint64Bytes(v: Long): ByteArray = ByteArray(8) { i ->
        ((v ushr ((7 - i) * 8)) and 0xFFL).toByte()
    }

    private fun buildMinimalMp4WithChpl(chaptersMs: List<Pair<Long, String>>): ByteArray {
        val chplPayload = ByteArrayOutputStream()
        // version = 1 (1 byte) + flags = 0 (3 bytes) + reserved = 0 (4 bytes) + count (1 byte)
        chplPayload.write(1)
        chplPayload.write(byteArrayOf(0, 0, 0, 0, 0, 0, 0))
        chplPayload.write(chaptersMs.size)
        for ((startMs, title) in chaptersMs) {
            val ticks100Ns = startMs * 10_000L
            chplPayload.write(uint64Bytes(ticks100Ns))
            val titleUtf8 = title.toByteArray(Charsets.UTF_8)
            chplPayload.write(titleUtf8.size)
            chplPayload.write(titleUtf8)
        }
        val chplBox = mp4Box("chpl", chplPayload.toByteArray())
        val udtaBox = mp4Box("udta", chplBox)
        val moovBox = mp4Box("moov", udtaBox)
        val ftypBox = mp4Box("ftyp", "isom\u0000\u0000\u0002\u0000".toByteArray(Charsets.ISO_8859_1))
        return ByteArrayOutputStream().apply {
            write(ftypBox)
            write(moovBox)
        }.toByteArray()
    }

    private fun mp4Box(type: String, payload: ByteArray): ByteArray {
        val total = 8 + payload.size
        val sizeBytes = byteArrayOf(
            ((total ushr 24) and 0xFF).toByte(),
            ((total ushr 16) and 0xFF).toByte(),
            ((total ushr 8) and 0xFF).toByte(),
            (total and 0xFF).toByte()
        )
        return ByteArrayOutputStream(total).apply {
            write(sizeBytes)
            write(type.toByteArray(Charsets.US_ASCII))
            write(payload)
        }.toByteArray()
    }
}
