package com.example

import com.example.anime4k.FrameInterpolationShaders
import com.example.data.FolderLibraryCache
import com.example.data.FolderSorter
import com.example.model.EpisodeItem
import com.example.model.EpisodeSortOption
import com.example.model.FrameInterpolationMode
import com.example.model.PlayerControlsTheme
import com.example.model.PlayerQuickAction
import com.example.model.PlayerSettings
import com.example.model.VideoFolderItem
import com.example.model.VideoResolutionFormatter
import com.example.player.DisplayRefreshInfo
import com.example.player.DisplayRefreshRateController
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NegaiV2FeaturesTest {

    @Test
    fun version2BuildConfig_and_noSvpMisnomerInUserFacingLabels() {
        assertEquals(2, BuildConfig.VERSION_CODE)
        assertEquals("2.0", BuildConfig.VERSION_NAME)

        FrameInterpolationMode.entries.forEach { mode ->
            assertFalse(
                "Mode title must not contain SVP: ${mode.title}",
                mode.title.contains("SVP", ignoreCase = true)
            )
            assertFalse(
                "Mode badge must not contain SVP: ${mode.badgeLabel}",
                mode.badgeLabel.contains("SVP", ignoreCase = true)
            )
            assertFalse(
                "Mode description must not contain SVP: ${mode.description}",
                mode.description.contains("SVP", ignoreCase = true)
            )
        }

        val interpQuickAction = PlayerQuickAction.FRAME_INTERPOLATION
        assertFalse(interpQuickAction.title.contains("SVP", ignoreCase = true))
        assertFalse(interpQuickAction.shortTitle.contains("SVP", ignoreCase = true))
        assertFalse(interpQuickAction.subtitle.contains("SVP", ignoreCase = true))
    }

    @Test
    fun memcShaders_useExactZeroMvHighpAndPhaseConsistentBidirectionalWarp() {
        assertTrue(FrameInterpolationShaders.MEMC_MOTION_ESTIMATE_FRAGMENT.contains("128.0 / 255.0"))
        assertTrue(FrameInterpolationShaders.MEMC_MOTION_ESTIMATE_FRAGMENT.contains("precision highp float"))
        assertTrue(FrameInterpolationShaders.MEMC_SYNTHESIZE_FRAGMENT.contains("128.0 / 255.0"))
        assertTrue(FrameInterpolationShaders.MEMC_SYNTHESIZE_FRAGMENT.contains("vTexCoord - mv * t"))
        assertTrue(FrameInterpolationShaders.MEMC_SYNTHESIZE_FRAGMENT.contains("vTexCoord + mv * (1.0 - t)"))
    }

    @Test
    fun ltpoRefreshPlan_adaptsToVideoCadenceSpeedAccelerationMemcAndPause() {
        val ltpoDisplay = DisplayRefreshInfo(
            currentHz = 120f,
            minSupportedHz = 24f,
            maxSupportedHz = 120f,
            supportedModesHz = listOf(24, 30, 48, 60, 90, 120)
        )

        // 1. 23.976 fps @ 1.0x with MEMC OFF -> exact 24Hz LTPO cadence
        val native24Plan = DisplayRefreshRateController.computeOptimalRefreshPlan(
            displayInfo = ltpoDisplay,
            isPlayerActive = true,
            isPlaying = true,
            sourceVideoFps = 23.976f,
            playbackSpeed = 1.0f,
            highRefreshRateEnabled = true,
            ltpoAdaptiveEnabled = true,
            interpolationMode = FrameInterpolationMode.OFF
        )
        assertEquals(24f, native24Plan.targetDisplayHz, 0.1f)
        assertTrue(native24Plan.isLtpoSavingActive)
        assertFalse(native24Plan.memcActive)

        // 2. 24 fps @ 2.0x speed acceleration with MEMC OFF -> 48Hz LTPO cadence
        val boost2xNativePlan = DisplayRefreshRateController.computeOptimalRefreshPlan(
            displayInfo = ltpoDisplay,
            isPlayerActive = true,
            isPlaying = true,
            sourceVideoFps = 24f,
            playbackSpeed = 2.0f,
            highRefreshRateEnabled = true,
            ltpoAdaptiveEnabled = true,
            interpolationMode = FrameInterpolationMode.OFF
        )
        assertEquals(48f, boost2xNativePlan.effectiveVideoFps, 0.1f)
        assertEquals(48f, boost2xNativePlan.targetDisplayHz, 0.1f)
        assertTrue(boost2xNativePlan.isLtpoSavingActive)

        // 3. 30 fps @ 1.5x speed (45 fps) with MEMC MAX -> picks clean 2x harmonic 90Hz instead of 120Hz
        val harmonicMemcPlan = DisplayRefreshRateController.computeOptimalRefreshPlan(
            displayInfo = ltpoDisplay,
            isPlayerActive = true,
            isPlaying = true,
            sourceVideoFps = 30f,
            playbackSpeed = 1.5f,
            highRefreshRateEnabled = true,
            ltpoAdaptiveEnabled = true,
            interpolationMode = FrameInterpolationMode.SVP_MAX
        )
        assertEquals(45f, harmonicMemcPlan.effectiveVideoFps, 0.1f)
        assertEquals(90f, harmonicMemcPlan.targetDisplayHz, 0.1f)
        assertTrue(harmonicMemcPlan.memcActive)
        assertTrue(harmonicMemcPlan.isLtpoSavingActive)

        // 4. 60 fps @ 2.0x speed (120 fps) on 120Hz -> auto-bypasses GPU MEMC to save power
        val autoBypassPlan = DisplayRefreshRateController.computeOptimalRefreshPlan(
            displayInfo = ltpoDisplay,
            isPlayerActive = true,
            isPlaying = true,
            sourceVideoFps = 60f,
            playbackSpeed = 2.0f,
            highRefreshRateEnabled = true,
            ltpoAdaptiveEnabled = true,
            interpolationMode = FrameInterpolationMode.SVP_MAX
        )
        assertFalse(autoBypassPlan.memcActive)
        assertTrue(autoBypassPlan.memcAutoBypassedBySpeed)
        assertEquals(120f, autoBypassPlan.targetDisplayHz, 0.1f)

        // 5. Paused player -> drops continuous VSync and lets LTPO panel idle down
        val pausedPlan = DisplayRefreshRateController.computeOptimalRefreshPlan(
            displayInfo = ltpoDisplay,
            isPlayerActive = true,
            isPlaying = false,
            sourceVideoFps = 24f,
            playbackSpeed = 1.0f,
            highRefreshRateEnabled = true,
            ltpoAdaptiveEnabled = true,
            interpolationMode = FrameInterpolationMode.SVP_MAX
        )
        assertTrue(pausedPlan.preferSystemIdleWindowMode)
        assertFalse(pausedPlan.useContinuousVsync)
        assertEquals(0f, pausedPlan.targetRenderFps, 0.01f)
    }

    @Test
    fun playerControlsThemes_and_videoResolutionFormatter_workAccurately() {
        assertEquals(5, PlayerControlsTheme.entries.size)
        PlayerControlsTheme.entries.forEach { theme ->
            assertEquals(theme, PlayerControlsTheme.fromId(theme.id.lowercase()))
        }
        assertEquals(PlayerControlsTheme.CLASSIC_OLED, PlayerSettings().playerControlsTheme)

        assertEquals("4K", VideoResolutionFormatter.formatBadge(3840, 2160, "ep01.mkv"))
        assertEquals("1440p", VideoResolutionFormatter.formatBadge(2560, 1440, "ep01.mkv"))
        assertEquals("1080p", VideoResolutionFormatter.formatBadge(1920, 1080, "ep01.mkv"))
        assertEquals("1080p", VideoResolutionFormatter.formatBadge(1080, 1920, "vertical_short.mp4"))
        assertEquals("720p", VideoResolutionFormatter.formatBadge(1280, 720, "ep01.mkv"))
        assertEquals("1080p", VideoResolutionFormatter.formatBadge(0, 0, "[SubsPlease] Anime - 05 (1080p) [ABCD].mkv"))
        assertEquals("4K", VideoResolutionFormatter.formatBadge(0, 0, "Movie.2025.2160p.WEB-DL.mkv"))
        assertNull(VideoResolutionFormatter.formatBadge(0, 0, "01.mkv"))
    }

    @Test
    fun episodeSorter_and_cacheV2_supportResolutionSizeAndEpisodeOrdering() {
        val ep720 = EpisodeItem(
            uri = "file:///a/02.mkv",
            fileName = "02_720p.mkv",
            cleanTitle = "Episode 2",
            episodeNumber = 2,
            seasonNumber = 1,
            folderId = "f1",
            folderName = "Show",
            sizeBytes = 500_000_000L,
            videoWidth = 1280,
            videoHeight = 720,
            durationMs = 1_400_000L
        )
        val ep4k = EpisodeItem(
            uri = "file:///a/01.mkv",
            fileName = "01_2160p.mkv",
            cleanTitle = "Episode 1",
            episodeNumber = 1,
            seasonNumber = 1,
            folderId = "f1",
            folderName = "Show",
            sizeBytes = 3_200_000_000L,
            videoWidth = 3840,
            videoHeight = 2160,
            durationMs = 1_500_000L
        )
        val ep1080 = EpisodeItem(
            uri = "file:///a/03.mkv",
            fileName = "03_1080p.mkv",
            cleanTitle = "Episode 3",
            episodeNumber = 3,
            seasonNumber = 1,
            folderId = "f1",
            folderName = "Show",
            sizeBytes = 1_200_000_000L,
            videoWidth = 1920,
            videoHeight = 1080,
            durationMs = 1_450_000L
        )

        val byEpisodeAsc = FolderSorter.sortEpisodes(
            listOf(ep720, ep1080, ep4k),
            EpisodeSortOption.EPISODE_NUMBER,
            ascending = true
        )
        assertEquals(listOf(ep4k, ep720, ep1080), byEpisodeAsc)

        val byResolutionDesc = FolderSorter.sortEpisodes(
            listOf(ep720, ep1080, ep4k),
            EpisodeSortOption.RESOLUTION,
            ascending = false
        )
        assertEquals(listOf(ep4k, ep1080, ep720), byResolutionDesc)
        assertEquals("4K", byResolutionDesc[0].resolutionBadge)
        assertEquals("1080p", byResolutionDesc[1].resolutionBadge)
        assertEquals("720p", byResolutionDesc[2].resolutionBadge)

        // Verify v2 binary cache preserves videoWidth & videoHeight
        val tempDir = Files.createTempDirectory("negai_v2_cache").toFile()
        try {
            val cacheFile = tempDir.resolve("folders_v2.bin")
            val folder = VideoFolderItem(
                id = "f1",
                name = "Show",
                displayPath = "/Anime/Show",
                rootUri = "file:///a",
                episodes = byEpisodeAsc,
                totalAutoAudioCount = 0,
                totalAutoSubCount = 0,
                watchedCount = 0
            )
            FolderLibraryCache.writeToFile(cacheFile, listOf(folder))
            val restored = FolderLibraryCache.readFromFile(cacheFile)
            assertEquals(1, restored?.size)
            assertEquals(3840, restored!![0].episodes[0].videoWidth)
            assertEquals(2160, restored[0].episodes[0].videoHeight)
            assertEquals("4K", restored[0].episodes[0].resolutionBadge)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun audioDelayProcessor_and_sortedBottomQuickActions_behaveCorrectly() {
        assertEquals(0L, com.example.player.AudioDelayProcessor.clampDelayMs(0L))
        assertEquals(5000L, com.example.player.AudioDelayProcessor.clampDelayMs(8500L))
        assertEquals(-5000L, com.example.player.AudioDelayProcessor.clampDelayMs(-7200L))
        assertEquals("0 мс", com.example.player.AudioDelayProcessor.formatDelayLabel(0L))
        assertEquals("+250 мс", com.example.player.AudioDelayProcessor.formatDelayLabel(250L))
        assertEquals("-150 мс", com.example.player.AudioDelayProcessor.formatDelayLabel(-150L))

        val defaultActions = PlayerQuickAction.defaultActions
        assertEquals(PlayerQuickAction.AUDIO, defaultActions[0])
        assertEquals(PlayerQuickAction.SUBTITLES, defaultActions[1])
        assertEquals(PlayerQuickAction.EPISODES, defaultActions[2])
        assertEquals(PlayerQuickAction.CHAPTERS, defaultActions[3])
        assertFalse(defaultActions.contains(PlayerQuickAction.DECODER))
    }
}
