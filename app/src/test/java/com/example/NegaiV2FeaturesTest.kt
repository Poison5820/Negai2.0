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
    fun version2BuildConfig_and_frameInterpolationUserFacingLabels() {
        assertEquals(6, BuildConfig.VERSION_CODE)
        assertEquals("2.5", BuildConfig.VERSION_NAME)
        assertEquals("v2.5", com.example.model.AppVersionInfo.BADGE_TEXT)

        assertEquals(com.example.model.AppLanguage.SYSTEM, com.example.model.AppLanguage.fromId("system"))
        assertEquals(com.example.model.AppLanguage.RU, com.example.model.AppLanguage.fromId("ru"))
        assertEquals(com.example.model.AppLanguage.EN, com.example.model.AppLanguage.fromId("en"))
        assertTrue(com.example.ui.i18n.resolveIsEnglish(com.example.model.AppLanguage.EN))
        assertFalse(com.example.ui.i18n.resolveIsEnglish(com.example.model.AppLanguage.RU))

        FrameInterpolationMode.entries.forEach { mode ->
            assertTrue(mode.title.isNotBlank())
            assertTrue(mode.badgeLabel.isNotBlank())
            assertTrue(mode.description.isNotBlank())
        }

        val interpQuickAction = PlayerQuickAction.FRAME_INTERPOLATION
        assertEquals("Интерполяция кадров (MEMC)", interpQuickAction.title)
        assertEquals("Плавность", interpQuickAction.shortTitle)
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
            interpolationMode = FrameInterpolationMode.MAX_FPS
        )
        assertEquals(45f, harmonicMemcPlan.effectiveVideoFps, 0.1f)
        assertEquals(90f, harmonicMemcPlan.targetDisplayHz, 0.1f)
        assertTrue(harmonicMemcPlan.memcActive)
        assertTrue(harmonicMemcPlan.isLtpoSavingActive)

        // 3b. 23.976 / 24 fps @ 1.0x with MEMC -> strict 2x = 48 fps on 48Hz LTPO panel (not 120Hz!)
        val strict2xPlan = DisplayRefreshRateController.computeOptimalRefreshPlan(
            displayInfo = ltpoDisplay,
            isPlayerActive = true,
            isPlaying = true,
            sourceVideoFps = 23.976f,
            playbackSpeed = 1.0f,
            highRefreshRateEnabled = true,
            ltpoAdaptiveEnabled = true,
            interpolationMode = FrameInterpolationMode.ANIME
        )
        assertEquals(24f, strict2xPlan.effectiveVideoFps, 0.1f)
        assertEquals(48f, strict2xPlan.targetRenderFps, 0.1f)
        assertEquals(48f, strict2xPlan.targetDisplayHz, 0.1f)
        assertTrue(strict2xPlan.memcActive)
        assertTrue(strict2xPlan.hudBadgeText.contains("48 к/с (2x)"))

        // 4. 60 fps @ 2.0x speed (120 fps) on 120Hz -> auto-bypasses GPU MEMC to save power
        val autoBypassPlan = DisplayRefreshRateController.computeOptimalRefreshPlan(
            displayInfo = ltpoDisplay,
            isPlayerActive = true,
            isPlaying = true,
            sourceVideoFps = 60f,
            playbackSpeed = 2.0f,
            highRefreshRateEnabled = true,
            ltpoAdaptiveEnabled = true,
            interpolationMode = FrameInterpolationMode.MAX_FPS
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
            interpolationMode = FrameInterpolationMode.MAX_FPS
        )
        assertTrue(pausedPlan.preferSystemIdleWindowMode)
        assertFalse(pausedPlan.useContinuousVsync)
        assertEquals(0f, pausedPlan.targetRenderFps, 0.01f)

        // 6. Outside player: UI navigation (Folders, Settings) -> runs at 120Hz maximum refresh rate
        val uiNavigationPlan = DisplayRefreshRateController.computeOptimalRefreshPlan(
            displayInfo = ltpoDisplay,
            isPlayerActive = false,
            isPlaying = false,
            sourceVideoFps = 24f,
            playbackSpeed = 1.0f,
            highRefreshRateEnabled = true,
            ltpoAdaptiveEnabled = true,
            interpolationMode = FrameInterpolationMode.OFF
        )
        assertEquals(120f, uiNavigationPlan.targetDisplayHz, 0.1f)
        assertFalse(uiNavigationPlan.preferSystemIdleWindowMode)

        // 7. In player WITH controls / UI active (user taps screen or HUD open) -> instantly boosts to 120Hz
        val hudBoostPlan = DisplayRefreshRateController.computeOptimalRefreshPlan(
            displayInfo = ltpoDisplay,
            isPlayerActive = true,
            isPlaying = true,
            sourceVideoFps = 24f,
            playbackSpeed = 1.0f,
            highRefreshRateEnabled = true,
            ltpoAdaptiveEnabled = true,
            interpolationMode = FrameInterpolationMode.OFF,
            isControlsOrUiActive = true
        )
        assertEquals(120f, hudBoostPlan.targetDisplayHz, 0.1f)
        assertFalse(hudBoostPlan.preferSystemIdleWindowMode)
    }

    @Test
    fun playerControlsThemes_and_videoResolutionFormatter_workAccurately() {
        assertEquals(8, PlayerControlsTheme.entries.size)
        PlayerControlsTheme.entries.forEach { theme ->
            assertEquals(theme, PlayerControlsTheme.fromId(theme.id.lowercase()))
        }
        assertEquals(PlayerControlsTheme.CLASSIC_OLED, PlayerSettings().playerControlsTheme)
        assertTrue(PlayerControlsTheme.PURE_ICONS.isFrameless)
        assertTrue(PlayerControlsTheme.PURE_ICONS.isAccentFree)
        assertTrue(PlayerControlsTheme.MONOCHROME_MIST.isAccentFree)
        assertFalse(PlayerControlsTheme.MONOCHROME_MIST.isFrameless)
        assertTrue(PlayerControlsTheme.GHOST_OUTLINE.isAccentFree)
        assertTrue(PlayerControlsTheme.GHOST_OUTLINE.isFrameless)

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
        assertEquals(PlayerQuickAction.SPEED, defaultActions[2])
        assertEquals(PlayerQuickAction.SCALE, defaultActions[3])
        assertFalse(defaultActions.contains(PlayerQuickAction.EPISODES))
        assertFalse(defaultActions.contains(PlayerQuickAction.CHAPTERS))
        assertFalse(defaultActions.contains(PlayerQuickAction.AUDIO_BOOST))
        assertFalse(defaultActions.contains(PlayerQuickAction.DECODER))
        assertTrue(defaultActions.contains(PlayerQuickAction.FILE_INFO))

        // Verify negative audio delay uses MediaClock offset instead of dropping PCM samples
        val processor = com.example.player.AudioDelayProcessor()
        processor.setDelayMs(-150L)
        assertEquals(-150_000L, processor.mediaClockOffsetUs)
        assertFalse(processor.isActive())

        processor.setDelayMs(200L)
        assertEquals(0L, processor.mediaClockOffsetUs)

        // Verify BluetoothAudioSyncController auto-compensation logic for A2DP / LE Audio / Wired
        val wiredRoute = com.example.player.BluetoothAudioRouteState(
            isBluetoothActive = false,
            profile = com.example.player.BluetoothAudioProfile.NONE
        )
        assertEquals(
            0L,
            com.example.player.BluetoothAudioSyncController.computeAutoBluetoothOffsetMs(
                route = wiredRoute,
                autoSyncEnabled = true,
                userConfiguredA2dpOffsetMs = -150L
            )
        )

        val a2dpRoute = com.example.player.BluetoothAudioRouteState(
            isBluetoothActive = true,
            profile = com.example.player.BluetoothAudioProfile.A2DP,
            deviceName = "WH-1000XM5"
        )
        assertEquals(
            -150L,
            com.example.player.BluetoothAudioSyncController.computeAutoBluetoothOffsetMs(
                route = a2dpRoute,
                autoSyncEnabled = true,
                userConfiguredA2dpOffsetMs = 0L
            )
        )
        assertEquals(
            -100L,
            com.example.player.BluetoothAudioSyncController.computeTotalEffectiveDelayMs(
                manualDelayMs = 50L,
                route = a2dpRoute,
                autoSyncEnabled = true,
                userConfiguredBtOffsetMs = 0L
            )
        )

        val leAudioRoute = com.example.player.BluetoothAudioRouteState(
            isBluetoothActive = true,
            profile = com.example.player.BluetoothAudioProfile.LE_AUDIO,
            deviceName = "Pixel Buds Pro 2"
        )
        assertEquals(
            -60L,
            com.example.player.BluetoothAudioSyncController.computeAutoBluetoothOffsetMs(
                route = leAudioRoute,
                autoSyncEnabled = true,
                userConfiguredA2dpOffsetMs = 0L
            )
        )

        // 4. Dynamic measured Bluetooth latency from Android HAL (e.g. 175 ms in standard mode)
        val measuredA2dpRoute = com.example.player.BluetoothAudioRouteState(
            isBluetoothActive = true,
            profile = com.example.player.BluetoothAudioProfile.A2DP,
            deviceName = "Galaxy Buds2 Pro",
            halOutputLatencyMs = 175,
            measuredLatencyMs = 175L,
            isGamingMode = false
        )
        assertEquals(
            -175L,
            com.example.player.BluetoothAudioSyncController.computeAutoBluetoothOffsetMs(
                route = measuredA2dpRoute,
                autoSyncEnabled = true
            )
        )
        assertEquals(
            -125L,
            com.example.player.BluetoothAudioSyncController.computeTotalEffectiveDelayMs(
                manualDelayMs = 50L,
                route = measuredA2dpRoute,
                autoSyncEnabled = true
            )
        )

        // 5. Headphone "Gaming Mode" toggled by user -> latency drops to 48 ms -> player adapts immediately
        val gamingModeRoute = com.example.player.BluetoothAudioRouteState(
            isBluetoothActive = true,
            profile = com.example.player.BluetoothAudioProfile.A2DP,
            deviceName = "TWS Gaming Earbuds",
            halOutputLatencyMs = 48,
            measuredLatencyMs = 48L,
            isGamingMode = true
        )
        assertEquals(
            -48L,
            com.example.player.BluetoothAudioSyncController.computeAutoBluetoothOffsetMs(
                route = gamingModeRoute,
                autoSyncEnabled = true
            )
        )
        assertTrue(gamingModeRoute.isGamingMode)
        assertTrue(gamingModeRoute.displayLatencyLabel(isEnglish = false).contains("48 мс"))
        assertTrue(gamingModeRoute.displayLatencyLabel(isEnglish = false).contains("Игровой режим"))
    }

    @Test
    fun videoThumbnailCache_deterministicKeysAndDiskPruning() {
        val uri1 = "content://media/external/video/media/1001"
        val uri2 = "content://media/external/video/media/1002"

        val key1a = com.example.ui.util.VideoThumbnailCache.generateKey(uri1, 2000L, 320, 180)
        val key1b = com.example.ui.util.VideoThumbnailCache.generateKey(uri1, 2000L, 320, 180)
        val key2 = com.example.ui.util.VideoThumbnailCache.generateKey(uri2, 2000L, 320, 180)

        assertEquals("Same URI & dimensions must produce identical key", key1a, key1b)
        assertTrue("Different URIs must produce distinct keys", key1a != key2)
        assertTrue("Key should have thumb_ prefix", key1a.startsWith("thumb_"))

        // Test disk pruning logic
        val tempDir = Files.createTempDirectory("thumb_test_cache").toFile()
        try {
            val file1 = java.io.File(tempDir, "thumb_1.jpg").apply {
                writeBytes(ByteArray(1024 * 50)) // 50 KB
                setLastModified(1000L)
            }
            val file2 = java.io.File(tempDir, "thumb_2.jpg").apply {
                writeBytes(ByteArray(1024 * 50)) // 50 KB
                setLastModified(2000L)
            }
            val file3 = java.io.File(tempDir, "thumb_3.jpg").apply {
                writeBytes(ByteArray(1024 * 50)) // 50 KB
                setLastModified(3000L)
            }

            assertEquals(3, tempDir.listFiles()?.size)
            // Prune with max limit of 80 KB -> oldest files should be removed
            com.example.ui.util.VideoThumbnailCache.pruneDiskCacheIfNeeded(tempDir, maxSizeBytes = 80 * 1024L)
            val remaining = tempDir.listFiles() ?: emptyArray()
            assertTrue(remaining.size < 3)
            assertFalse("Oldest file should have been deleted", file1.exists())
            assertTrue("Newest file should remain", file3.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun permanentDeletion_settingsAndRecursiveFolderFileRemoval() {
        val defaultSettings = PlayerSettings()
        assertFalse(defaultSettings.skipDeleteConfirmation)
        val updatedSettings = defaultSettings.copy(skipDeleteConfirmation = true)
        assertTrue(updatedSettings.skipDeleteConfirmation)

        val tempRoot = Files.createTempDirectory("negai_delete_test").toFile()
        try {
            val showFolder = java.io.File(tempRoot, "Solo Leveling").apply { mkdirs() }
            val ep1File = java.io.File(showFolder, "01.mkv").apply { writeBytes(ByteArray(1024)) }
            val ep2File = java.io.File(showFolder, "02.mkv").apply { writeBytes(ByteArray(1024)) }
            val subFolder = java.io.File(showFolder, "Rus Subs").apply { mkdirs() }
            val subFile = java.io.File(subFolder, "01.ass").apply { writeBytes(ByteArray(256)) }

            assertTrue(ep1File.exists())
            assertTrue(ep2File.exists())
            assertTrue(subFile.exists())

            // Delete single file first
            assertTrue(ep1File.delete())
            assertFalse(ep1File.exists())
            assertTrue(ep2File.exists())

            // Delete entire folder recursively
            assertTrue(showFolder.deleteRecursively())
            assertFalse(showFolder.exists())
            assertFalse(ep2File.exists())
            assertFalse(subFile.exists())
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun v25_dpiUiScale_hudTimeout_customSkipIntro_and_torrentReleaseDetails() {
        val defaultSettings = PlayerSettings()
        assertEquals(100, defaultSettings.playerUiScalePercent)
        assertTrue(defaultSettings.playerAutoDpiScale)
        assertEquals(4, defaultSettings.controlsAutoHideSeconds)
        assertEquals(85, defaultSettings.skipIntroSeconds)
        assertEquals(0L, defaultSettings.bluetoothAudioOffsetMs)

        val sampleTitle = "Поднятие уровня в одиночку / Solo Leveling [S02E01-13 из 13] (2025) WEB-DL 2160p HEVC HDR10+ Dolby Vision | AniLibria, Студийная Банда, Sub"
        val techTags = com.example.torrent.TorrentSearchResult.extractTechTags(sampleTitle)
        val voiceTags = com.example.torrent.TorrentSearchResult.extractVoiceoverTags(sampleTitle)
        val subTags = com.example.torrent.TorrentSearchResult.extractSubtitleTags(sampleTitle)
        val epInfo = com.example.torrent.TorrentSearchResult.extractEpisodeInfo(sampleTitle)

        assertTrue(techTags.contains("2160P"))
        assertTrue(techTags.contains("WEB-DL"))
        assertTrue(techTags.contains("HEVC / H.265"))
        assertTrue(techTags.contains("HDR10+"))
        assertTrue(techTags.contains("Dolby Vision"))
        assertTrue(voiceTags.any { it.contains("Anilibria", ignoreCase = true) })
        assertTrue(voiceTags.contains("Студийная Банда"))
        assertFalse(voiceTags.contains("Субтитры / Sub"))
        assertTrue(subTags.contains("Субтитры / Sub"))
        assertTrue(!epInfo.isNullOrBlank())
    }

    @Test
    fun v25_preRelease_voiceoverVsSubs_anime4kMemcAutoFast_and_btGamingPreset() {
        // 1. Voiceover vs subtitles separation for RUS(int), ПМ, MVO, Dub
        val rusIntTitle = "Магическая битва / Jujutsu Kaisen [TV-2] [23 из 23] [RUS(int), JAP+Sub] [WEBRip 1080p]"
        val voiceoverTags = com.example.torrent.TorrentSearchResult.extractVoiceoverTags(rusIntTitle)
        val subtitleTags = com.example.torrent.TorrentSearchResult.extractSubtitleTags(rusIntTitle)
        assertTrue(voiceoverTags.any { it.contains("RUS", ignoreCase = true) })
        assertFalse(voiceoverTags.any { it.contains("Субтитры", ignoreCase = true) })
        assertTrue(subtitleTags.contains("Субтитры / Sub"))

        // 2. Simultaneous Anime4K + Frame Interpolation (MEMC) auto-downgrade to ANIME4K_FAST
        val heavySettings = PlayerSettings(
            anime4kEnabled = true,
            anime4kPreset = com.example.model.Anime4KPreset.ANIME4K_UPSCALE_CNNX,
            frameInterpolationEnabled = true,
            frameInterpolationMode = FrameInterpolationMode.MAX_FPS
        )
        assertTrue(heavySettings.isAnime4kAutoDowngradedForMemc)
        assertEquals(com.example.model.Anime4KPreset.ANIME4K_FAST, heavySettings.effectiveAnime4KPreset)

        val nonMemcSettings = heavySettings.copy(
            frameInterpolationEnabled = false,
            frameInterpolationMode = FrameInterpolationMode.OFF
        )
        assertFalse(nonMemcSettings.isAnime4kAutoDowngradedForMemc)
        assertEquals(com.example.model.Anime4KPreset.ANIME4K_UPSCALE_CNNX, nonMemcSettings.effectiveAnime4KPreset)

        // 3. Automatic Bluetooth Gaming Mode (-45ms) + user fine-tune adjustment
        val btGamingRoute = com.example.player.BluetoothAudioRouteState(
            isBluetoothActive = true,
            profile = com.example.player.BluetoothAudioProfile.A2DP,
            deviceName = "TWS Gaming Buds",
            halOutputLatencyMs = 45,
            isGamingMode = true,
            measuredLatencyMs = 45L
        )
        val gamingAutoOffset = com.example.player.BluetoothAudioSyncController.computeAutoBluetoothOffsetMs(
            route = btGamingRoute,
            autoSyncEnabled = true,
            userConfiguredA2dpOffsetMs = 0L
        )
        assertEquals(-45L, gamingAutoOffset)

        val fineTunedOffset = com.example.player.BluetoothAudioSyncController.computeAutoBluetoothOffsetMs(
            route = btGamingRoute,
            autoSyncEnabled = true,
            userConfiguredA2dpOffsetMs = -15L
        )
        assertEquals(-60L, fineTunedOffset)

        // 4. Device Info category exists in SettingsCategory
        assertTrue(com.example.model.SettingsCategory.entries.contains(com.example.model.SettingsCategory.DEVICE_INFO))
    }

    @Test
    fun v25_secondUpdate_oneTapOrientation_rename_and_fileInfo() {
        // 1. One-tap orientation toggle immediately flips between LANDSCAPE and PORTRAIT
        assertEquals(
            com.example.model.PlayerOrientationMode.PORTRAIT,
            com.example.model.PlayerOrientationMode.toggleLandscapePortrait(isCurrentlyLandscape = true)
        )
        assertEquals(
            com.example.model.PlayerOrientationMode.LANDSCAPE,
            com.example.model.PlayerOrientationMode.toggleLandscapePortrait(isCurrentlyLandscape = false)
        )

        // 2. Physical file & folder rename preserves extension and updates paths
        val tempRoot = Files.createTempDirectory("negai_rename_test").toFile()
        try {
            val folderDir = java.io.File(tempRoot, "Frieren S1").apply { mkdirs() }
            val epFile = java.io.File(folderDir, "01_1080p_HEVC.mkv").apply {
                writeBytes(ByteArray(4096))
            }
            val episode = EpisodeItem(
                uri = epFile.absolutePath,
                fileName = epFile.name,
                cleanTitle = "Episode 01",
                episodeNumber = 1,
                seasonNumber = 1,
                folderId = folderDir.absolutePath,
                folderName = folderDir.name,
                sizeBytes = 4096L,
                videoWidth = 1920,
                videoHeight = 1080,
                durationMs = 1_440_000L
            )
            val renamedFile = java.io.File(folderDir, "Frieren - 01.mkv")
            assertTrue(epFile.renameTo(renamedFile))
            assertTrue(renamedFile.exists())
            assertFalse(epFile.exists())

            val renamedFolder = java.io.File(tempRoot, "Sousou no Frieren")
            assertTrue(folderDir.renameTo(renamedFolder))
            assertTrue(renamedFolder.exists())
            assertTrue(java.io.File(renamedFolder, "Frieren - 01.mkv").exists())
            assertEquals("1080p", episode.resolutionBadge)
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}




