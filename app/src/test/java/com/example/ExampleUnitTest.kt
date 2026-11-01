package com.example

import com.example.data.FolderSorter
import com.example.data.MediaAutoMatcher
import com.example.data.RawCompanionFile
import com.example.model.Anime4KPreset
import com.example.model.AppThemeMode
import com.example.model.EpisodeItem
import com.example.model.FolderSortOption
import com.example.model.PlaybackEndMode
import com.example.model.PlayerOrientationMode
import com.example.model.PlayerQuickAction
import com.example.model.PlayerSettings
import com.example.model.QuickActionsSerializer
import com.example.model.QuickBarPreset
import com.example.model.SubtitleFormat
import com.example.model.VideoFolderItem
import com.example.player.AudioBoostController
import com.example.subtitles.SubtitleAlignment
import com.example.subtitles.SubtitleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun mediaAutoMatcher_bindsExternalAudioAndSubtitlesByEpisodeAndFolder() {
        val candidates = listOf(
            RawCompanionFile(
                uri = "content://audio/ep01_anilibria.mka",
                fileName = "[SubsPlease] Frieren - 01 (1080p) [A1B2C3D4].AniLibria.mka",
                parentFolderName = "RUS Audio",
                isSameDirectoryAsVideo = false
            ),
            RawCompanionFile(
                uri = "content://audio/ep01_studioband.mka",
                fileName = "Frieren_E01_RUS.mka",
                parentFolderName = "StudioBand",
                isSameDirectoryAsVideo = false
            ),
            RawCompanionFile(
                uri = "content://audio/ep02_anilibria.mka",
                fileName = "[SubsPlease] Frieren - 02 (1080p).AniLibria.mka",
                parentFolderName = "RUS Audio",
                isSameDirectoryAsVideo = false
            )
        )

        val matchedEp1 = MediaAutoMatcher.matchAudioTracksForEpisode(
            videoFileName = "[SubsPlease] Frieren - 01 (1080p) [A1B2C3D4].mkv",
            totalVideosInFolder = 12,
            candidates = candidates
        )

        assertEquals(2, matchedEp1.size)
        assertEquals("content://audio/ep01_anilibria.mka", matchedEp1[0].uri)
        assertEquals("RUS Audio", matchedEp1[0].sourceFolderName)
        assertEquals("content://audio/ep01_studioband.mka", matchedEp1[1].uri)
    }

    @Test
    fun srtParser_and_subtitleDelay_workAccurately() {
        val srt = """
            1
            00:00:02,000 --> 00:00:05,000
            <i>Первая строка субтитров SRT</i>

            2
            00:00:06,500 --> 00:00:09,000
            Вторая строка
        """.trimIndent()

        val doc = SubtitleParser.parse(srt, SubtitleFormat.SRT, "Test.srt")
        assertEquals(2, doc.cues.size)
        assertTrue(doc.cues[0].isItalic)
        assertEquals("Первая строка субтитров SRT", doc.cues[0].text)

        // At 5300ms with 0ms delay, cue 1 (2000..5000) has already ended
        assertEquals(0, doc.activeCuesAt(playerPositionMs = 5300L, delayMs = 0L).size)
        // With +500ms positive delay, lookupMs = 5300 - 500 = 4800ms -> cue 1 is active!
        assertEquals(1, doc.activeCuesAt(playerPositionMs = 5300L, delayMs = 500L).size)
    }

    @Test
    fun assParser_parsesStylesOverridesAndMultilineDialogue() {
        val ass = """
            [Script Info]
            ScriptType: v4.00+

            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, Bold, Italic, Alignment
            Style: Default,Rubik,42,&H00FFFFFF,-1,0,2
            Style: Sign,Rubik,36,&H0000EAFF,0,0,8

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:04.50,Sign,,0,0,0,,{\an8}Надпись сверху экрана
            Dialogue: 0,0:00:01.50,0:00:04.00,Default,,0,0,0,,Привет, мир!\NВторая строка с запятой, внутри текста
        """.trimIndent()

        val doc = SubtitleParser.parse(ass, SubtitleFormat.ASS, "Episode01.ass")
        assertEquals(2, doc.cues.size)
        assertEquals(SubtitleAlignment.TOP_CENTER, doc.cues[0].alignment)
        assertEquals("Надпись сверху экрана", doc.cues[0].text)
        assertEquals("Привет, мир!\nВторая строка с запятой, внутри текста", doc.cues[1].text)
        assertTrue(doc.cues[1].isBold)
    }

    @Test
    fun anime4kPresets_resolveByExactNames() {
        assertEquals(Anime4KPreset.ANIME4K_UPSCALE_CNN, Anime4KPreset.fromId("Anime4K_Upscale_CNN"))
        assertEquals(Anime4KPreset.ANIME4K_UPSCALE_CNNX, Anime4KPreset.fromId("Anime4K_Upscale_CNNX"))
    }

    @Test
    fun oledThemes_defaultToPureBlack() {
        val defaultSettings = PlayerSettings()
        assertEquals(AppThemeMode.OLED_BLACK, defaultSettings.themeMode)
        assertTrue(defaultSettings.themeMode.isPureOled)
        assertEquals(0xFF000000L, AppThemeMode.OLED_BLACK.previewBgArgb)
        assertEquals(0xFF000000L, AppThemeMode.SAKURA_OLED.previewBgArgb)
        assertEquals(AppThemeMode.SAKURA_OLED, AppThemeMode.fromId("sakura_oled"))
    }

    @Test
    fun folderSorter_sortsByAllCriteriaAndDirections() {
        fun dummyEpisode(idx: Int, size: Long = 100L) = EpisodeItem(
            uri = "content://ep/$idx",
            fileName = "ep$idx.mkv",
            cleanTitle = "Episode $idx",
            episodeNumber = idx,
            seasonNumber = 1,
            folderId = "f",
            folderName = "Folder",
            sizeBytes = size
        )

        val folderSeason10 = VideoFolderItem(
            id = "s10",
            name = "Anime Season 10",
            displayPath = "/Anime/S10",
            rootUri = "root",
            episodes = List(24) { dummyEpisode(it, 50_000_000L) },
            totalAutoAudioCount = 0,
            totalAutoSubCount = 0,
            watchedCount = 4,
            totalSizeBytes = 1_200_000_000L,
            lastModified = 1_000L,
            lastPlayedAt = 5_000L
        )
        val folderSeason2 = VideoFolderItem(
            id = "s2",
            name = "Anime Season 2",
            displayPath = "/Anime/S2",
            rootUri = "root",
            episodes = List(12) { dummyEpisode(it, 80_000_000L) },
            totalAutoAudioCount = 0,
            totalAutoSubCount = 0,
            watchedCount = 11,
            totalSizeBytes = 960_000_000L,
            lastModified = 3_000L,
            lastPlayedAt = 9_000L
        )
        val folderMovie = VideoFolderItem(
            id = "mov",
            name = "Cyber Movie",
            displayPath = "/Movies",
            rootUri = "root",
            episodes = listOf(dummyEpisode(1, 4_000_000_000L)),
            totalAutoAudioCount = 0,
            totalAutoSubCount = 0,
            watchedCount = 0,
            totalSizeBytes = 4_000_000_000L,
            lastModified = 2_000L,
            lastPlayedAt = 0L
        )

        val raw = listOf(folderSeason10, folderMovie, folderSeason2)

        // 1. Natural Name ASC: Season 2 before Season 10 before Cyber Movie
        val byNameAsc = FolderSorter.sortFolders(raw, FolderSortOption.NAME, ascending = true)
        assertEquals(listOf("s2", "s10", "mov"), byNameAsc.map { it.id })

        // 2. Natural Name DESC
        val byNameDesc = FolderSorter.sortFolders(raw, FolderSortOption.NAME, ascending = false)
        assertEquals(listOf("mov", "s10", "s2"), byNameDesc.map { it.id })

        // 3. Last Played DESC (most recent first)
        val byPlayedDesc = FolderSorter.sortFolders(raw, FolderSortOption.LAST_PLAYED, ascending = false)
        assertEquals(listOf("s2", "s10", "mov"), byPlayedDesc.map { it.id })

        // 4. Date Modified DESC (newest first)
        val byDateDesc = FolderSorter.sortFolders(raw, FolderSortOption.DATE_MODIFIED, ascending = false)
        assertEquals(listOf("s2", "mov", "s10"), byDateDesc.map { it.id })

        // 5. Episode Count DESC (24 -> 12 -> 1)
        val byCountDesc = FolderSorter.sortFolders(raw, FolderSortOption.EPISODE_COUNT, ascending = false)
        assertEquals(listOf("s10", "s2", "mov"), byCountDesc.map { it.id })

        // 6. Total Size DESC (4GB -> 1.2GB -> 960MB)
        val bySizeDesc = FolderSorter.sortFolders(raw, FolderSortOption.TOTAL_SIZE, ascending = false)
        assertEquals(listOf("mov", "s10", "s2"), bySizeDesc.map { it.id })

        // 7. Unwatched Count DESC (20 unwatched -> 1 unwatched (tie broken by name: s2 then mov))
        val byUnwatchedDesc = FolderSorter.sortFolders(raw, FolderSortOption.UNWATCHED, ascending = false)
        assertEquals(listOf("s10", "s2", "mov"), byUnwatchedDesc.map { it.id })
    }

    @Test
    fun playerExtras_audioBoostOrientationAndPlaybackEndModesWorkAccurately() {
        // 100% -> 0 mB, 150% -> +600 mB (+6 dB), 200% -> +1200 mB (+12 dB), clamped outside [100, 200]
        assertEquals(0, AudioBoostController.percentToGainMillibels(100))
        assertEquals(600, AudioBoostController.percentToGainMillibels(150))
        assertEquals(1200, AudioBoostController.percentToGainMillibels(200))
        assertEquals(0, AudioBoostController.percentToGainMillibels(50))
        assertEquals(1200, AudioBoostController.percentToGainMillibels(260))

        // Orientation cycle: VIDEO_ASPECT -> LANDSCAPE -> PORTRAIT -> AUTO -> VIDEO_ASPECT
        assertEquals(PlayerOrientationMode.LANDSCAPE, PlayerOrientationMode.VIDEO_ASPECT.next())
        assertEquals(PlayerOrientationMode.PORTRAIT, PlayerOrientationMode.LANDSCAPE.next())
        assertEquals(PlayerOrientationMode.AUTO, PlayerOrientationMode.PORTRAIT.next())
        assertEquals(PlayerOrientationMode.VIDEO_ASPECT, PlayerOrientationMode.AUTO.next())

        // PlaybackEndMode resolution
        assertEquals(PlaybackEndMode.PAUSE_AT_END, PlaybackEndMode.fromId("pause_at_end"))
        assertEquals(PlaybackEndMode.REPEAT_ONE, PlaybackEndMode.fromId("REPEAT_ONE"))
        assertEquals(PlaybackEndMode.AUTO_NEXT, PlaybackEndMode.fromId("unknown"))
    }

    @Test
    fun quickActionsSerializer_supportsAddRemoveReorderAndPersistence() {
        // Default actions sorted by usefulness (AUDIO, SUBTITLES, EPISODES, SPEED, ...),
        // CHAPTERS moved above seekbar, AUDIO_BOOST merged into AUDIO, DECODER on top bar
        val defaults = QuickActionsSerializer.parse(null)
        assertTrue(PlayerQuickAction.SPEED in defaults)
        assertTrue(PlayerQuickAction.AUDIO in defaults)
        assertTrue(PlayerQuickAction.CHAPTERS !in defaults)
        assertTrue(PlayerQuickAction.AUDIO_BOOST !in defaults)
        assertTrue(PlayerQuickAction.DECODER !in defaults)

        // Add new button (e.g. SLEEP_TIMER) without duplicates
        val withSleep = QuickActionsSerializer.addAction(defaults, PlayerQuickAction.SLEEP_TIMER)
        assertEquals(defaults.size + 1, withSleep.size)
        assertEquals(PlayerQuickAction.SLEEP_TIMER, withSleep.last())
        assertEquals(withSleep, QuickActionsSerializer.addAction(withSleep, PlayerQuickAction.SLEEP_TIMER))

        // Drag reorder: move SPEED to index 0
        val speedIdx = withSleep.indexOf(PlayerQuickAction.SPEED)
        val movedToFront = QuickActionsSerializer.moveItem(withSleep, speedIdx, 0)
        assertEquals(PlayerQuickAction.SPEED, movedToFront.first())
        assertEquals(withSleep.size, movedToFront.size)

        // Stepper moveDown & moveUp round-trip
        val steppedDown = QuickActionsSerializer.moveDown(movedToFront, 0)
        assertEquals(PlayerQuickAction.SPEED, steppedDown[1])
        val steppedBackUp = QuickActionsSerializer.moveUp(steppedDown, 1)
        assertEquals(movedToFront, steppedBackUp)

        // Remove button & serialize/parse round-trip
        val afterRemove = QuickActionsSerializer.removeAction(steppedBackUp, PlayerQuickAction.ANIME4K)
        assertTrue(PlayerQuickAction.ANIME4K !in afterRemove)
        val raw = QuickActionsSerializer.serialize(afterRemove)
        assertEquals(afterRemove, QuickActionsSerializer.parse(raw))

        // Explicit empty toolbar ("NONE" sentinel) survives persistence
        val emptyRaw = QuickActionsSerializer.serialize(emptyList())
        assertEquals(emptyList<PlayerQuickAction>(), QuickActionsSerializer.parse(emptyRaw))

        // Presets have distinct valid actions
        QuickBarPreset.entries.forEach { preset ->
            assertEquals(preset.actions.distinct(), preset.actions)
        }
    }

    @Test
    fun playbackResumeHelper_computesSmartResumeAndResetsFinishedVideos() {
        val duration24m = 24 * 60_000L // 1,440,000 ms

        // Disabled -> starts at 0
        assertEquals(
            0L,
            com.example.data.PlaybackResumeHelper.computeResumePositionMs(
                positionMs = 420_000L,
                durationMs = duration24m,
                resumeEnabled = false
            )
        )

        // Negligible < 1s -> starts at 0
        assertEquals(
            0L,
            com.example.data.PlaybackResumeHelper.computeResumePositionMs(
                positionMs = 600L,
                durationMs = duration24m,
                resumeEnabled = true
            )
        )

        // Mid-episode -> resumes at exact saved timestamp
        assertEquals(
            420_000L,
            com.example.data.PlaybackResumeHelper.computeResumePositionMs(
                positionMs = 420_000L,
                durationMs = duration24m,
                resumeEnabled = true
            )
        )

        // Finished / near-end (>= 98% and <= 15s left) -> restarts from 0
        assertEquals(
            0L,
            com.example.data.PlaybackResumeHelper.computeResumePositionMs(
                positionMs = duration24m - 5_000L,
                durationMs = duration24m,
                resumeEnabled = true
            )
        )
    }

    @Test
    fun storageAccessHelper_excludesExactAndNestedSubfoldersAndCameraPaths() {
        val excluded = setOf("file:///storage/emulated/0/Telegram", "/storage/emulated/0/Movies/Temp")

        // Exact folder excluded
        assertTrue(
            com.example.data.StorageAccessHelper.isFolderExcluded(
                folderId = "file:///storage/emulated/0/Telegram",
                localDirPath = "/storage/emulated/0/Telegram",
                excludedIds = excluded
            )
        )

        // Nested subfolder inside excluded parent is also excluded
        assertTrue(
            com.example.data.StorageAccessHelper.isFolderExcluded(
                folderId = "file:///storage/emulated/0/Telegram/Telegram Video",
                localDirPath = "/storage/emulated/0/Telegram/Telegram Video",
                excludedIds = excluded
            )
        )

        // Unrelated anime folder is NOT excluded
        org.junit.Assert.assertFalse(
            com.example.data.StorageAccessHelper.isFolderExcluded(
                folderId = "file:///storage/emulated/0/Anime/Frieren",
                localDirPath = "/storage/emulated/0/Anime/Frieren",
                excludedIds = excluded
            )
        )

        // Camera & Screenshots filter detection
        assertTrue(com.example.data.StorageAccessHelper.isCameraOrScreenshotsPath("/storage/emulated/0/DCIM/Camera"))
        assertTrue(com.example.data.StorageAccessHelper.isCameraOrScreenshotsPath("/storage/emulated/0/Pictures/Screenshots"))
        org.junit.Assert.assertFalse(com.example.data.StorageAccessHelper.isCameraOrScreenshotsPath("/storage/emulated/0/Movies/Anime"))

        // 8 simplified settings categories (Audio & Subtitles merged)
        assertEquals(8, com.example.model.SettingsCategory.entries.size)
    }
}

