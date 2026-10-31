package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.Anime4KPreset
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.DecoderMode
import com.example.model.EpisodeSortOption
import com.example.model.FolderSortOption
import com.example.model.FrameInterpolationMode
import com.example.model.PlaybackEndMode
import com.example.model.PlayerControlsTheme
import com.example.model.PlayerOrientationMode
import com.example.model.PlayerQuickAction
import com.example.model.PlayerSettings
import com.example.model.QuickActionsSerializer
import com.example.model.SubtitleColorPreset
import com.example.model.VideoScaleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kino4k_player_settings"
)

class PlayerPreferencesRepository(private val context: Context) {

    private object Keys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("app_theme_mode")
        val PLAYER_CONTROLS_THEME = stringPreferencesKey("player_controls_theme")
        val COMPACT_UI_MODE = booleanPreferencesKey("compact_ui_mode")
        val SHOW_EPISODE_THUMBNAILS = booleanPreferencesKey("show_episode_thumbnails")
        val SHOW_EPISODES_BUTTON_IN_HUD = booleanPreferencesKey("show_episodes_button_in_hud")
        val SHOW_HUD_BADGES = booleanPreferencesKey("show_hud_badges")
        val FOLDER_SORT_OPTION = stringPreferencesKey("folder_sort_option")
        val FOLDER_SORT_ASCENDING = booleanPreferencesKey("folder_sort_ascending")
        val EPISODE_SORT_OPTION = stringPreferencesKey("episode_sort_option")
        val EPISODE_SORT_ASCENDING = booleanPreferencesKey("episode_sort_ascending")
        val PLAYER_QUICK_ACTIONS_V3 = stringPreferencesKey("player_quick_actions_v3")

        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback_from_last_pos")
        val LAST_PLAYED_VIDEO_URI = stringPreferencesKey("last_played_video_uri")
        val PLAYBACK_END_MODE = stringPreferencesKey("playback_end_mode")
        val ENABLE_EMBEDDED_CHAPTERS = booleanPreferencesKey("enable_embedded_chapters")
        val SHOW_CHAPTER_MARKERS = booleanPreferencesKey("show_chapter_markers_on_timeline")
        val SEEK_STEP_SECONDS = intPreferencesKey("seek_step_seconds")
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val SHOW_SKIP_INTRO_BUTTON = booleanPreferencesKey("show_skip_intro_button")
        val SKIP_INTRO_SECONDS = intPreferencesKey("skip_intro_seconds")
        val AUTO_ENTER_PIP = booleanPreferencesKey("auto_enter_pip")
        val BACKGROUND_AUDIO_PLAY = booleanPreferencesKey("background_audio_play")

        val GESTURES_BRIGHTNESS_VOLUME = booleanPreferencesKey("gestures_brightness_volume")
        val GESTURE_HORIZONTAL_SEEK = booleanPreferencesKey("gesture_horizontal_seek")
        val GESTURE_DOUBLE_TAP_SEEK = booleanPreferencesKey("gesture_double_tap_seek")
        val GESTURE_LONG_PRESS_2X = booleanPreferencesKey("gesture_long_press_2x")
        val LONG_PRESS_BOOST_SPEED = floatPreferencesKey("long_press_boost_speed")
        val DEFAULT_PLAYER_ORIENTATION_V2 = stringPreferencesKey("default_player_orientation_v2")

        val DECODER_MODE = stringPreferencesKey("decoder_mode")
        val DEFAULT_SCALE_MODE = stringPreferencesKey("default_scale_mode")
        val HIGH_REFRESH_RATE_ENABLED = booleanPreferencesKey("high_refresh_rate_enabled")
        val LTPO_ADAPTIVE_REFRESH_ENABLED = booleanPreferencesKey("ltpo_adaptive_refresh_enabled")
        val FRAME_INTERPOLATION_ENABLED = booleanPreferencesKey("frame_interpolation_enabled_v2")
        val FRAME_INTERPOLATION_MODE = stringPreferencesKey("frame_interpolation_mode_v2")
        val ANIME4K_ENABLED = booleanPreferencesKey("anime4k_enabled_v2")
        val ANIME4K_PRESET = stringPreferencesKey("anime4k_preset_v2")
        val ANIME4K_STRENGTH = floatPreferencesKey("anime4k_strength")

        val AUTO_BIND_AUDIO = booleanPreferencesKey("auto_bind_external_audio")
        val PREFER_EXTERNAL_AUDIO_ON_START = booleanPreferencesKey("prefer_external_audio_on_start")
        val DEFAULT_AUDIO_BOOST_PERCENT = intPreferencesKey("default_audio_boost_percent")
        val BLUETOOTH_AUDIO_AUTO_SYNC = booleanPreferencesKey("bluetooth_audio_auto_sync")
        val BLUETOOTH_AUDIO_OFFSET_MS = longPreferencesKey("bluetooth_audio_offset_ms")

        val AUTO_BIND_SUBS = booleanPreferencesKey("auto_bind_external_subtitles")
        val SUB_FONT_SIZE = floatPreferencesKey("subtitle_font_size_sp")
        val SUB_COLOR_ARGB = longPreferencesKey("subtitle_color_argb")
        val SUB_BG_ENABLED = booleanPreferencesKey("subtitle_bg_enabled")

        val TORRSERVER_URL = stringPreferencesKey("torrserver_url")
        val TORRSERVER_USERNAME = stringPreferencesKey("torrserver_username")
        val TORRSERVER_PASSWORD = stringPreferencesKey("torrserver_password")
        val TORRSERVER_AUTO_PLAY_SINGLE = booleanPreferencesKey("torrserver_auto_play_single")
        val TORRSERVER_SAVE_TO_DB = booleanPreferencesKey("torrserver_save_to_db")
        val TORRENT_INDEXER_URL = stringPreferencesKey("torrent_indexer_url")
        val TORRENT_INDEXER_API_KEY = stringPreferencesKey("torrent_indexer_api_key")
        val ENABLE_DIRECT_RUTOR_SEARCH = booleanPreferencesKey("enable_direct_rutor_search")
        val ENABLE_ANIME_TRACKER_SEARCH = booleanPreferencesKey("enable_anime_tracker_search")
        val ENABLE_TRACKER_DOH = booleanPreferencesKey("enable_tracker_doh")

        val IGNORE_NOMEDIA_FOLDERS = booleanPreferencesKey("ignore_nomedia_folders")
        val EXCLUDE_CAMERA_AND_SCREENSHOTS = booleanPreferencesKey("exclude_camera_and_screenshots")
        val MIN_VIDEO_SIZE_MB = intPreferencesKey("min_video_size_mb")
    }

    val settingsFlow: Flow<PlayerSettings> = context.playerDataStore.data.map { prefs ->
        val sortOpt = FolderSortOption.fromId(
            prefs[Keys.FOLDER_SORT_OPTION] ?: FolderSortOption.NAME.id
        )
        val epSortOpt = EpisodeSortOption.fromId(
            prefs[Keys.EPISODE_SORT_OPTION] ?: EpisodeSortOption.EPISODE_NUMBER.id
        )
        PlayerSettings(
            appLanguage = AppLanguage.fromId(prefs[Keys.APP_LANGUAGE] ?: AppLanguage.SYSTEM.id),
            themeMode = AppThemeMode.fromId(prefs[Keys.THEME_MODE] ?: AppThemeMode.OLED_BLACK.id),
            playerControlsTheme = PlayerControlsTheme.fromId(
                prefs[Keys.PLAYER_CONTROLS_THEME] ?: PlayerControlsTheme.CLASSIC_OLED.id
            ),
            compactUiMode = prefs[Keys.COMPACT_UI_MODE] ?: true,
            showEpisodeThumbnails = prefs[Keys.SHOW_EPISODE_THUMBNAILS] ?: true,
            showEpisodesButtonInHud = prefs[Keys.SHOW_EPISODES_BUTTON_IN_HUD] ?: true,
            showHudBadges = prefs[Keys.SHOW_HUD_BADGES] ?: true,
            folderSortOption = sortOpt,
            folderSortAscending = prefs[Keys.FOLDER_SORT_ASCENDING] ?: sortOpt.defaultAscending,
            episodeSortOption = epSortOpt,
            episodeSortAscending = prefs[Keys.EPISODE_SORT_ASCENDING] ?: epSortOpt.defaultAscending,
            quickActions = QuickActionsSerializer.parse(prefs[Keys.PLAYER_QUICK_ACTIONS_V3]),

            resumePlayback = prefs[Keys.RESUME_PLAYBACK] ?: true,
            playbackEndMode = PlaybackEndMode.fromId(
                prefs[Keys.PLAYBACK_END_MODE] ?: PlaybackEndMode.AUTO_NEXT.id
            ),
            enableEmbeddedChapters = prefs[Keys.ENABLE_EMBEDDED_CHAPTERS] ?: true,
            showChapterMarkersOnTimeline = prefs[Keys.SHOW_CHAPTER_MARKERS] ?: true,
            seekStepSeconds = (prefs[Keys.SEEK_STEP_SECONDS] ?: 10).coerceIn(3, 60),
            defaultPlaybackSpeed = (prefs[Keys.DEFAULT_PLAYBACK_SPEED] ?: 1.0f).coerceIn(0.25f, 3.0f),
            showSkipIntroButton = prefs[Keys.SHOW_SKIP_INTRO_BUTTON] ?: true,
            skipIntroSeconds = (prefs[Keys.SKIP_INTRO_SECONDS] ?: 85).coerceIn(15, 180),
            autoEnterPip = prefs[Keys.AUTO_ENTER_PIP] ?: true,
            backgroundAudioPlay = prefs[Keys.BACKGROUND_AUDIO_PLAY] ?: false,

            gesturesBrightnessVolume = prefs[Keys.GESTURES_BRIGHTNESS_VOLUME] ?: true,
            gestureHorizontalSeek = prefs[Keys.GESTURE_HORIZONTAL_SEEK] ?: true,
            gestureDoubleTapSeek = prefs[Keys.GESTURE_DOUBLE_TAP_SEEK] ?: true,
            gestureLongPress2x = prefs[Keys.GESTURE_LONG_PRESS_2X] ?: true,
            longPressBoostSpeed = (prefs[Keys.LONG_PRESS_BOOST_SPEED] ?: 2.0f).coerceIn(1.25f, 3.0f),
            defaultPlayerOrientation = PlayerOrientationMode.fromId(
                prefs[Keys.DEFAULT_PLAYER_ORIENTATION_V2] ?: PlayerOrientationMode.VIDEO_ASPECT.id
            ),

            decoderMode = DecoderMode.fromId(prefs[Keys.DECODER_MODE] ?: DecoderMode.HW_PLUS.id),
            defaultScaleMode = VideoScaleMode.fromId(
                prefs[Keys.DEFAULT_SCALE_MODE] ?: VideoScaleMode.FIT.id
            ),
            highRefreshRateEnabled = prefs[Keys.HIGH_REFRESH_RATE_ENABLED] ?: true,
            ltpoAdaptiveRefreshEnabled = prefs[Keys.LTPO_ADAPTIVE_REFRESH_ENABLED] ?: true,
            frameInterpolationEnabled = prefs[Keys.FRAME_INTERPOLATION_ENABLED] ?: false,
            frameInterpolationMode = FrameInterpolationMode.fromId(
                prefs[Keys.FRAME_INTERPOLATION_MODE] ?: FrameInterpolationMode.OFF.id
            ),
            anime4kEnabled = prefs[Keys.ANIME4K_ENABLED] ?: false,
            anime4kPreset = Anime4KPreset.fromId(
                prefs[Keys.ANIME4K_PRESET] ?: Anime4KPreset.OFF.id
            ),
            anime4kStrength = (prefs[Keys.ANIME4K_STRENGTH] ?: 1.0f).coerceIn(0.2f, 1.5f),

            autoBindExternalAudio = prefs[Keys.AUTO_BIND_AUDIO] ?: true,
            preferExternalAudioOnStart = prefs[Keys.PREFER_EXTERNAL_AUDIO_ON_START] ?: true,
            defaultAudioBoostPercent = (prefs[Keys.DEFAULT_AUDIO_BOOST_PERCENT] ?: 100).coerceIn(100, 200),
            bluetoothAudioAutoSync = prefs[Keys.BLUETOOTH_AUDIO_AUTO_SYNC] ?: true,
            bluetoothAudioOffsetMs = (prefs[Keys.BLUETOOTH_AUDIO_OFFSET_MS] ?: -150L).coerceIn(-600L, 200L),

            autoBindExternalSubtitles = prefs[Keys.AUTO_BIND_SUBS] ?: true,
            subtitleFontSizeSp = (prefs[Keys.SUB_FONT_SIZE] ?: 21f).coerceIn(13f, 42f),
            subtitleColorArgb = prefs[Keys.SUB_COLOR_ARGB] ?: SubtitleColorPreset.WHITE.argb,
            subtitleBackgroundEnabled = prefs[Keys.SUB_BG_ENABLED] ?: true,

            torrServerUrl = (prefs[Keys.TORRSERVER_URL] ?: "http://127.0.0.1:8090").trim()
                .ifBlank { "http://127.0.0.1:8090" },
            torrServerUsername = prefs[Keys.TORRSERVER_USERNAME] ?: "",
            torrServerPassword = prefs[Keys.TORRSERVER_PASSWORD] ?: "",
            torrServerAutoPlaySingle = prefs[Keys.TORRSERVER_AUTO_PLAY_SINGLE] ?: true,
            torrServerSaveToDb = prefs[Keys.TORRSERVER_SAVE_TO_DB] ?: true,
            torrentIndexerUrl = (prefs[Keys.TORRENT_INDEXER_URL] ?: "https://jacred.xyz").trim()
                .ifBlank { "https://jacred.xyz" },
            torrentIndexerApiKey = prefs[Keys.TORRENT_INDEXER_API_KEY] ?: "",
            enableDirectRutorSearch = prefs[Keys.ENABLE_DIRECT_RUTOR_SEARCH] ?: true,
            enableAnimeTrackerSearch = prefs[Keys.ENABLE_ANIME_TRACKER_SEARCH] ?: true,
            enableTrackerDoh = prefs[Keys.ENABLE_TRACKER_DOH] ?: true,

            ignoreNomediaFolders = prefs[Keys.IGNORE_NOMEDIA_FOLDERS] ?: true,
            excludeCameraAndScreenshots = prefs[Keys.EXCLUDE_CAMERA_AND_SCREENSHOTS] ?: false,
            minVideoSizeMb = (prefs[Keys.MIN_VIDEO_SIZE_MB] ?: 0).coerceIn(0, 500)
        ).also { snapshot ->
            writeBootSettingsSnapshot(context, snapshot)
        }
    }

    val lastPlayedVideoUriFlow: Flow<String?> = context.playerDataStore.data.map { prefs ->
        prefs[Keys.LAST_PLAYED_VIDEO_URI]
    }

    fun readBootSettingsSnapshot(): PlayerSettings = readBootSettingsSnapshot(context)

    suspend fun updateAppLanguage(language: AppLanguage) {
        bootPrefs(context).edit().putString("app_lang", language.id).apply()
        context.playerDataStore.edit { it[Keys.APP_LANGUAGE] = language.id }
    }

    suspend fun updateThemeMode(mode: AppThemeMode) {
        bootPrefs(context).edit().putString("theme_mode", mode.id).apply()
        context.playerDataStore.edit { it[Keys.THEME_MODE] = mode.id }
    }

    suspend fun updatePlayerControlsTheme(theme: PlayerControlsTheme) {
        bootPrefs(context).edit().putString("controls_theme", theme.id).apply()
        context.playerDataStore.edit { it[Keys.PLAYER_CONTROLS_THEME] = theme.id }
    }

    suspend fun updateCompactUiMode(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("compact_ui", enabled).apply()
        context.playerDataStore.edit { it[Keys.COMPACT_UI_MODE] = enabled }
    }

    suspend fun updateShowEpisodeThumbnails(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("show_thumbs", enabled).apply()
        context.playerDataStore.edit { it[Keys.SHOW_EPISODE_THUMBNAILS] = enabled }
    }

    suspend fun updateShowEpisodesButtonInHud(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.SHOW_EPISODES_BUTTON_IN_HUD] = enabled }
    }

    suspend fun updateShowHudBadges(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.SHOW_HUD_BADGES] = enabled }
    }

    suspend fun selectFolderSortOption(option: FolderSortOption) {
        context.playerDataStore.edit { prefs ->
            val currentOption = FolderSortOption.fromId(
                prefs[Keys.FOLDER_SORT_OPTION] ?: FolderSortOption.NAME.id
            )
            if (currentOption == option) {
                val currentAsc = prefs[Keys.FOLDER_SORT_ASCENDING] ?: option.defaultAscending
                prefs[Keys.FOLDER_SORT_ASCENDING] = !currentAsc
            } else {
                prefs[Keys.FOLDER_SORT_OPTION] = option.id
                prefs[Keys.FOLDER_SORT_ASCENDING] = option.defaultAscending
            }
        }
    }

    suspend fun updateFolderSortDirection(ascending: Boolean) {
        context.playerDataStore.edit { it[Keys.FOLDER_SORT_ASCENDING] = ascending }
    }

    suspend fun toggleFolderSortDirection() {
        context.playerDataStore.edit { prefs ->
            val currentOption = FolderSortOption.fromId(
                prefs[Keys.FOLDER_SORT_OPTION] ?: FolderSortOption.NAME.id
            )
            val currentAsc = prefs[Keys.FOLDER_SORT_ASCENDING] ?: currentOption.defaultAscending
            prefs[Keys.FOLDER_SORT_ASCENDING] = !currentAsc
        }
    }

    suspend fun selectEpisodeSortOption(option: EpisodeSortOption) {
        context.playerDataStore.edit { prefs ->
            val currentOption = EpisodeSortOption.fromId(
                prefs[Keys.EPISODE_SORT_OPTION] ?: EpisodeSortOption.EPISODE_NUMBER.id
            )
            if (currentOption == option) {
                val currentAsc = prefs[Keys.EPISODE_SORT_ASCENDING] ?: option.defaultAscending
                prefs[Keys.EPISODE_SORT_ASCENDING] = !currentAsc
            } else {
                prefs[Keys.EPISODE_SORT_OPTION] = option.id
                prefs[Keys.EPISODE_SORT_ASCENDING] = option.defaultAscending
            }
        }
    }

    suspend fun updateEpisodeSortDirection(ascending: Boolean) {
        context.playerDataStore.edit { it[Keys.EPISODE_SORT_ASCENDING] = ascending }
    }

    suspend fun toggleEpisodeSortDirection() {
        context.playerDataStore.edit { prefs ->
            val currentOption = EpisodeSortOption.fromId(
                prefs[Keys.EPISODE_SORT_OPTION] ?: EpisodeSortOption.EPISODE_NUMBER.id
            )
            val currentAsc = prefs[Keys.EPISODE_SORT_ASCENDING] ?: currentOption.defaultAscending
            prefs[Keys.EPISODE_SORT_ASCENDING] = !currentAsc
        }
    }

    suspend fun updateQuickActions(actions: List<PlayerQuickAction>) {
        context.playerDataStore.edit {
            it[Keys.PLAYER_QUICK_ACTIONS_V3] = QuickActionsSerializer.serialize(actions)
        }
    }

    suspend fun updateResumePlayback(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.RESUME_PLAYBACK] = enabled }
    }

    suspend fun setLastPlayedVideoUri(uri: String) {
        context.playerDataStore.edit { it[Keys.LAST_PLAYED_VIDEO_URI] = uri }
    }

    suspend fun clearLastPlayedVideoUri() {
        context.playerDataStore.edit { it[Keys.LAST_PLAYED_VIDEO_URI] = "" }
    }

    suspend fun updatePlaybackEndMode(mode: PlaybackEndMode) {
        context.playerDataStore.edit { it[Keys.PLAYBACK_END_MODE] = mode.id }
    }

    suspend fun updateEnableEmbeddedChapters(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.ENABLE_EMBEDDED_CHAPTERS] = enabled }
    }

    suspend fun updateShowChapterMarkersOnTimeline(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.SHOW_CHAPTER_MARKERS] = enabled }
    }

    suspend fun updateSeekStepSeconds(seconds: Int) {
        context.playerDataStore.edit { it[Keys.SEEK_STEP_SECONDS] = seconds.coerceIn(3, 60) }
    }

    suspend fun updateDefaultPlaybackSpeed(speed: Float) {
        context.playerDataStore.edit { it[Keys.DEFAULT_PLAYBACK_SPEED] = speed.coerceIn(0.25f, 3.0f) }
    }

    suspend fun updateShowSkipIntroButton(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.SHOW_SKIP_INTRO_BUTTON] = enabled }
    }

    suspend fun updateSkipIntroSeconds(seconds: Int) {
        context.playerDataStore.edit { it[Keys.SKIP_INTRO_SECONDS] = seconds.coerceIn(15, 180) }
    }

    suspend fun updateAutoEnterPip(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.AUTO_ENTER_PIP] = enabled }
    }

    suspend fun updateBackgroundAudioPlay(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.BACKGROUND_AUDIO_PLAY] = enabled }
    }

    suspend fun updateGesturesBrightnessVolume(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.GESTURES_BRIGHTNESS_VOLUME] = enabled }
    }

    suspend fun updateGestureHorizontalSeek(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.GESTURE_HORIZONTAL_SEEK] = enabled }
    }

    suspend fun updateGestureDoubleTapSeek(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.GESTURE_DOUBLE_TAP_SEEK] = enabled }
    }

    suspend fun updateGestureLongPress2x(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.GESTURE_LONG_PRESS_2X] = enabled }
    }

    suspend fun updateLongPressBoostSpeed(speed: Float) {
        context.playerDataStore.edit { it[Keys.LONG_PRESS_BOOST_SPEED] = speed.coerceIn(1.25f, 3.0f) }
    }

    suspend fun updateDefaultPlayerOrientation(mode: PlayerOrientationMode) {
        context.playerDataStore.edit { it[Keys.DEFAULT_PLAYER_ORIENTATION_V2] = mode.id }
    }

    suspend fun updateDecoderMode(mode: DecoderMode) {
        context.playerDataStore.edit { it[Keys.DECODER_MODE] = mode.id }
    }

    suspend fun updateDefaultScaleMode(mode: VideoScaleMode) {
        context.playerDataStore.edit { it[Keys.DEFAULT_SCALE_MODE] = mode.id }
    }

    suspend fun updateHighRefreshRateEnabled(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("high_hz", enabled).apply()
        context.playerDataStore.edit { it[Keys.HIGH_REFRESH_RATE_ENABLED] = enabled }
    }

    suspend fun updateLtpoAdaptiveRefreshEnabled(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("ltpo_hz", enabled).apply()
        context.playerDataStore.edit { it[Keys.LTPO_ADAPTIVE_REFRESH_ENABLED] = enabled }
    }

    suspend fun updateFrameInterpolationEnabled(enabled: Boolean) {
        context.playerDataStore.edit { prefs ->
            prefs[Keys.FRAME_INTERPOLATION_ENABLED] = enabled
            if (enabled && FrameInterpolationMode.fromId(
                    prefs[Keys.FRAME_INTERPOLATION_MODE] ?: FrameInterpolationMode.OFF.id
                ) == FrameInterpolationMode.OFF
            ) {
                prefs[Keys.FRAME_INTERPOLATION_MODE] = FrameInterpolationMode.MAX_FPS.id
            }
        }
    }

    suspend fun updateFrameInterpolationMode(mode: FrameInterpolationMode) {
        context.playerDataStore.edit { prefs ->
            prefs[Keys.FRAME_INTERPOLATION_MODE] = mode.id
            prefs[Keys.FRAME_INTERPOLATION_ENABLED] = (mode != FrameInterpolationMode.OFF)
        }
    }

    suspend fun updateAnime4KEnabled(enabled: Boolean) {
        context.playerDataStore.edit { prefs ->
            prefs[Keys.ANIME4K_ENABLED] = enabled
            if (enabled && Anime4KPreset.fromId(
                    prefs[Keys.ANIME4K_PRESET] ?: Anime4KPreset.OFF.id
                ) == Anime4KPreset.OFF
            ) {
                prefs[Keys.ANIME4K_PRESET] = Anime4KPreset.ANIME4K_UPSCALE_CNN.id
            }
        }
    }

    suspend fun updateAnime4KPreset(preset: Anime4KPreset) {
        context.playerDataStore.edit { prefs ->
            prefs[Keys.ANIME4K_PRESET] = preset.id
            prefs[Keys.ANIME4K_ENABLED] = (preset != Anime4KPreset.OFF)
        }
    }

    suspend fun updateAnime4KStrength(strength: Float) {
        context.playerDataStore.edit {
            it[Keys.ANIME4K_STRENGTH] = strength.coerceIn(0.2f, 1.5f)
        }
    }

    suspend fun updateAutoBindAudio(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.AUTO_BIND_AUDIO] = enabled }
    }

    suspend fun updatePreferExternalAudioOnStart(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.PREFER_EXTERNAL_AUDIO_ON_START] = enabled }
    }

    suspend fun updateDefaultAudioBoostPercent(percent: Int) {
        context.playerDataStore.edit { it[Keys.DEFAULT_AUDIO_BOOST_PERCENT] = percent.coerceIn(100, 200) }
    }

    suspend fun updateBluetoothAudioAutoSync(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("bt_auto_sync", enabled).apply()
        context.playerDataStore.edit { it[Keys.BLUETOOTH_AUDIO_AUTO_SYNC] = enabled }
    }

    suspend fun updateBluetoothAudioOffsetMs(offsetMs: Long) {
        val clamped = offsetMs.coerceIn(-600L, 200L)
        bootPrefs(context).edit().putLong("bt_offset_ms", clamped).apply()
        context.playerDataStore.edit { it[Keys.BLUETOOTH_AUDIO_OFFSET_MS] = clamped }
    }

    suspend fun updateAutoBindSubtitles(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.AUTO_BIND_SUBS] = enabled }
    }

    suspend fun updateSubtitleFontSize(sizeSp: Float) {
        context.playerDataStore.edit {
            it[Keys.SUB_FONT_SIZE] = sizeSp.coerceIn(13f, 42f)
        }
    }

    suspend fun updateSubtitleColor(argb: Long) {
        context.playerDataStore.edit { it[Keys.SUB_COLOR_ARGB] = argb }
    }

    suspend fun updateSubtitleBackground(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.SUB_BG_ENABLED] = enabled }
    }

    suspend fun updateIgnoreNomediaFolders(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.IGNORE_NOMEDIA_FOLDERS] = enabled }
    }

    suspend fun updateExcludeCameraAndScreenshots(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.EXCLUDE_CAMERA_AND_SCREENSHOTS] = enabled }
    }

    suspend fun updateMinVideoSizeMb(sizeMb: Int) {
        context.playerDataStore.edit { it[Keys.MIN_VIDEO_SIZE_MB] = sizeMb.coerceIn(0, 500) }
    }

    suspend fun updateTorrServerUrl(url: String) {
        val cleaned = url.trim().ifBlank { "http://127.0.0.1:8090" }
        context.playerDataStore.edit { it[Keys.TORRSERVER_URL] = cleaned }
    }

    suspend fun updateTorrServerAuth(username: String, password: String) {
        context.playerDataStore.edit { prefs ->
            prefs[Keys.TORRSERVER_USERNAME] = username.trim()
            prefs[Keys.TORRSERVER_PASSWORD] = password
        }
    }

    suspend fun updateTorrServerAutoPlaySingle(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.TORRSERVER_AUTO_PLAY_SINGLE] = enabled }
    }

    suspend fun updateTorrServerSaveToDb(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.TORRSERVER_SAVE_TO_DB] = enabled }
    }

    suspend fun updateTorrentIndexerConfig(url: String, apiKey: String) {
        val cleanedUrl = url.trim().ifBlank { "https://jacred.xyz" }
        context.playerDataStore.edit { prefs ->
            prefs[Keys.TORRENT_INDEXER_URL] = cleanedUrl
            prefs[Keys.TORRENT_INDEXER_API_KEY] = apiKey.trim()
        }
    }

    suspend fun updateEnableDirectRutorSearch(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.ENABLE_DIRECT_RUTOR_SEARCH] = enabled }
    }

    suspend fun updateEnableAnimeTrackerSearch(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.ENABLE_ANIME_TRACKER_SEARCH] = enabled }
    }

    suspend fun updateEnableTrackerDoh(enabled: Boolean) {
        context.playerDataStore.edit { it[Keys.ENABLE_TRACKER_DOH] = enabled }
    }

    suspend fun resetAllSettingsToDefaults() {
        bootPrefs(context).edit().clear().apply()
        context.playerDataStore.edit { prefs ->
            val savedLastPlayed = prefs[Keys.LAST_PLAYED_VIDEO_URI]
            prefs.clear()
            if (!savedLastPlayed.isNullOrBlank()) {
                prefs[Keys.LAST_PLAYED_VIDEO_URI] = savedLastPlayed
            }
        }
    }

    companion object {
        private const val BOOT_SNAPSHOT_PREFS = "kino4k_boot_snapshot"

        private fun bootPrefs(context: Context) =
            context.applicationContext.getSharedPreferences(BOOT_SNAPSHOT_PREFS, Context.MODE_PRIVATE)

        /**
         * Reads synchronous boot snapshot in < 0.5ms so [com.example.MainActivity] and
         * [com.example.ui.KinoViewModel] render the user's exact saved theme, sort order,
         * and layout density on frame #1 with zero flash.
         */
        fun readBootSettingsSnapshot(context: Context): PlayerSettings {
            val sp = bootPrefs(context)
            if (!sp.contains("theme_mode")) return PlayerSettings()
            val sortOpt = FolderSortOption.fromId(sp.getString("sort_opt", FolderSortOption.NAME.id) ?: FolderSortOption.NAME.id)
            val epSortOpt = EpisodeSortOption.fromId(sp.getString("ep_sort_opt", EpisodeSortOption.EPISODE_NUMBER.id) ?: EpisodeSortOption.EPISODE_NUMBER.id)
            return PlayerSettings(
                appLanguage = AppLanguage.fromId(sp.getString("app_lang", AppLanguage.SYSTEM.id)),
                themeMode = AppThemeMode.fromId(sp.getString("theme_mode", AppThemeMode.OLED_BLACK.id) ?: AppThemeMode.OLED_BLACK.id),
                playerControlsTheme = PlayerControlsTheme.fromId(
                    sp.getString("controls_theme", PlayerControlsTheme.CLASSIC_OLED.id) ?: PlayerControlsTheme.CLASSIC_OLED.id
                ),
                compactUiMode = sp.getBoolean("compact_ui", true),
                showEpisodeThumbnails = sp.getBoolean("show_thumbs", true),
                showEpisodesButtonInHud = sp.getBoolean("show_ep_hud", true),
                showHudBadges = sp.getBoolean("show_hud_badges", true),
                folderSortOption = sortOpt,
                folderSortAscending = sp.getBoolean("sort_asc", sortOpt.defaultAscending),
                episodeSortOption = epSortOpt,
                episodeSortAscending = sp.getBoolean("ep_sort_asc", epSortOpt.defaultAscending),
                quickActions = QuickActionsSerializer.parse(sp.getString("quick_actions_v3", null)),
                defaultPlayerOrientation = PlayerOrientationMode.fromId(
                    sp.getString("orientation", PlayerOrientationMode.VIDEO_ASPECT.id) ?: PlayerOrientationMode.VIDEO_ASPECT.id
                ),
                defaultScaleMode = VideoScaleMode.fromId(
                    sp.getString("scale_mode", VideoScaleMode.FIT.id) ?: VideoScaleMode.FIT.id
                ),
                highRefreshRateEnabled = sp.getBoolean("high_hz", true),
                ltpoAdaptiveRefreshEnabled = sp.getBoolean("ltpo_hz", true),
                frameInterpolationEnabled = sp.getBoolean("frame_interp_enabled_v2", false),
                frameInterpolationMode = FrameInterpolationMode.fromId(
                    sp.getString("frame_interp_mode_v2", FrameInterpolationMode.OFF.id) ?: FrameInterpolationMode.OFF.id
                ),
                anime4kEnabled = sp.getBoolean("a4k_enabled_v2", false),
                anime4kPreset = Anime4KPreset.fromId(
                    sp.getString("a4k_preset_v2", Anime4KPreset.OFF.id) ?: Anime4KPreset.OFF.id
                ),
                bluetoothAudioAutoSync = sp.getBoolean("bt_auto_sync", true),
                bluetoothAudioOffsetMs = sp.getLong("bt_offset_ms", -150L).coerceIn(-600L, 200L),
                torrServerUrl = sp.getString("torr_url", "http://127.0.0.1:8090") ?: "http://127.0.0.1:8090"
            )
        }

        private fun writeBootSettingsSnapshot(context: Context, s: PlayerSettings) {
            bootPrefs(context).edit()
                .putString("app_lang", s.appLanguage.id)
                .putString("theme_mode", s.themeMode.id)
                .putString("controls_theme", s.playerControlsTheme.id)
                .putBoolean("compact_ui", s.compactUiMode)
                .putBoolean("show_thumbs", s.showEpisodeThumbnails)
                .putBoolean("show_ep_hud", s.showEpisodesButtonInHud)
                .putBoolean("show_hud_badges", s.showHudBadges)
                .putString("sort_opt", s.folderSortOption.id)
                .putBoolean("sort_asc", s.folderSortAscending)
                .putString("ep_sort_opt", s.episodeSortOption.id)
                .putBoolean("ep_sort_asc", s.episodeSortAscending)
                .putString("quick_actions_v3", QuickActionsSerializer.serialize(s.quickActions))
                .putString("orientation", s.defaultPlayerOrientation.id)
                .putString("scale_mode", s.defaultScaleMode.id)
                .putBoolean("high_hz", s.highRefreshRateEnabled)
                .putBoolean("ltpo_hz", s.ltpoAdaptiveRefreshEnabled)
                .putBoolean("frame_interp_enabled_v2", s.frameInterpolationEnabled)
                .putString("frame_interp_mode_v2", s.frameInterpolationMode.id)
                .putBoolean("a4k_enabled_v2", s.anime4kEnabled)
                .putString("a4k_preset_v2", s.anime4kPreset.id)
                .putBoolean("bt_auto_sync", s.bluetoothAudioAutoSync)
                .putLong("bt_offset_ms", s.bluetoothAudioOffsetMs)
                .putString("torr_url", s.torrServerUrl)
                .apply()
        }
    }
}
