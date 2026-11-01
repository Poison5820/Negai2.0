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
        val LEGACY_PLAYER_QUICK_ACTIONS_V2 = stringPreferencesKey("player_quick_actions_v2")
        val LEGACY_PLAYER_QUICK_ACTIONS_V1 = stringPreferencesKey("player_quick_actions")
        val PLAYER_UI_SCALE_PERCENT = intPreferencesKey("player_ui_scale_percent")
        val PLAYER_AUTO_DPI_SCALE = booleanPreferencesKey("player_auto_dpi_scale")
        val CONTROLS_AUTO_HIDE_SECONDS = intPreferencesKey("controls_auto_hide_seconds")

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
        val LEGACY_DEFAULT_PLAYER_ORIENTATION_V1 = stringPreferencesKey("default_player_orientation")

        val DECODER_MODE = stringPreferencesKey("decoder_mode")
        val DEFAULT_SCALE_MODE = stringPreferencesKey("default_scale_mode")
        val HIGH_REFRESH_RATE_ENABLED = booleanPreferencesKey("high_refresh_rate_enabled")
        val LTPO_ADAPTIVE_REFRESH_ENABLED = booleanPreferencesKey("ltpo_adaptive_refresh_enabled")
        val FRAME_INTERPOLATION_ENABLED = booleanPreferencesKey("frame_interpolation_enabled_v2")
        val LEGACY_FRAME_INTERPOLATION_ENABLED_V1 = booleanPreferencesKey("frame_interpolation_enabled")
        val FRAME_INTERPOLATION_MODE = stringPreferencesKey("frame_interpolation_mode_v2")
        val LEGACY_FRAME_INTERPOLATION_MODE_V1 = stringPreferencesKey("frame_interpolation_mode")
        val ANIME4K_ENABLED = booleanPreferencesKey("anime4k_enabled_v2")
        val LEGACY_ANIME4K_ENABLED_V1 = booleanPreferencesKey("anime4k_enabled")
        val ANIME4K_PRESET = stringPreferencesKey("anime4k_preset_v2")
        val LEGACY_ANIME4K_PRESET_V1 = stringPreferencesKey("anime4k_preset")
        val ANIME4K_STRENGTH = floatPreferencesKey("anime4k_strength")

        val AUTO_BIND_AUDIO = booleanPreferencesKey("auto_bind_external_audio")
        val PREFER_EXTERNAL_AUDIO_ON_START = booleanPreferencesKey("prefer_external_audio_on_start")
        val DEFAULT_AUDIO_BOOST_PERCENT = intPreferencesKey("default_audio_boost_percent")
        val BLUETOOTH_AUDIO_AUTO_SYNC = booleanPreferencesKey("bluetooth_audio_auto_sync")
        val BLUETOOTH_AUDIO_OFFSET_MS = longPreferencesKey("bluetooth_audio_fine_offset_ms")

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
        val SKIP_DELETE_CONFIRMATION = booleanPreferencesKey("skip_delete_confirmation")
    }

    val settingsFlow: Flow<PlayerSettings> = context.playerDataStore.data.map { prefs ->
        val sp = bootPrefs(context)
        val hasBoot = sp.contains("theme_mode")

        val sortOpt = FolderSortOption.fromId(
            prefs[Keys.FOLDER_SORT_OPTION]
                ?: (if (hasBoot) sp.getString("sort_opt", null) else null)
                ?: FolderSortOption.NAME.id
        )
        val epSortOpt = EpisodeSortOption.fromId(
            prefs[Keys.EPISODE_SORT_OPTION]
                ?: (if (hasBoot) sp.getString("ep_sort_opt", null) else null)
                ?: EpisodeSortOption.EPISODE_NUMBER.id
        )
        val rawQuickActions = prefs[Keys.PLAYER_QUICK_ACTIONS_V3]
            ?: prefs[Keys.LEGACY_PLAYER_QUICK_ACTIONS_V2]
            ?: prefs[Keys.LEGACY_PLAYER_QUICK_ACTIONS_V1]
            ?: (if (hasBoot) sp.getString("quick_actions_v3", null) else null)

        val rawOrientation = prefs[Keys.DEFAULT_PLAYER_ORIENTATION_V2]
            ?: prefs[Keys.LEGACY_DEFAULT_PLAYER_ORIENTATION_V1]
            ?: (if (hasBoot) sp.getString("orientation", null) else null)
            ?: PlayerOrientationMode.VIDEO_ASPECT.id

        val rawFrameInterpEnabled = prefs[Keys.FRAME_INTERPOLATION_ENABLED]
            ?: prefs[Keys.LEGACY_FRAME_INTERPOLATION_ENABLED_V1]
            ?: (if (hasBoot) sp.getBoolean("frame_interp_enabled_v2", false) else false)

        val rawFrameInterpMode = prefs[Keys.FRAME_INTERPOLATION_MODE]
            ?: prefs[Keys.LEGACY_FRAME_INTERPOLATION_MODE_V1]
            ?: (if (hasBoot) sp.getString("frame_interp_mode_v2", null) else null)
            ?: FrameInterpolationMode.OFF.id

        val rawAnime4kEnabled = prefs[Keys.ANIME4K_ENABLED]
            ?: prefs[Keys.LEGACY_ANIME4K_ENABLED_V1]
            ?: (if (hasBoot) sp.getBoolean("a4k_enabled_v2", false) else false)

        val rawAnime4kPreset = prefs[Keys.ANIME4K_PRESET]
            ?: prefs[Keys.LEGACY_ANIME4K_PRESET_V1]
            ?: (if (hasBoot) sp.getString("a4k_preset_v2", null) else null)
            ?: Anime4KPreset.OFF.id

        PlayerSettings(
            appLanguage = AppLanguage.fromId(
                prefs[Keys.APP_LANGUAGE]
                    ?: (if (hasBoot) sp.getString("app_lang", null) else null)
                    ?: AppLanguage.SYSTEM.id
            ),
            themeMode = AppThemeMode.fromId(
                prefs[Keys.THEME_MODE]
                    ?: (if (hasBoot) sp.getString("theme_mode", null) else null)
                    ?: AppThemeMode.OLED_BLACK.id
            ),
            playerControlsTheme = PlayerControlsTheme.fromId(
                prefs[Keys.PLAYER_CONTROLS_THEME]
                    ?: (if (hasBoot) sp.getString("controls_theme", null) else null)
                    ?: PlayerControlsTheme.CLASSIC_OLED.id
            ),
            compactUiMode = true,
            showEpisodeThumbnails = prefs[Keys.SHOW_EPISODE_THUMBNAILS]
                ?: (if (hasBoot && sp.contains("show_thumbs")) sp.getBoolean("show_thumbs", true) else true),
            showEpisodesButtonInHud = prefs[Keys.SHOW_EPISODES_BUTTON_IN_HUD]
                ?: (if (hasBoot && sp.contains("show_ep_hud")) sp.getBoolean("show_ep_hud", true) else true),
            showHudBadges = true,
            folderSortOption = sortOpt,
            folderSortAscending = prefs[Keys.FOLDER_SORT_ASCENDING]
                ?: (if (hasBoot && sp.contains("sort_asc")) sp.getBoolean("sort_asc", sortOpt.defaultAscending) else sortOpt.defaultAscending),
            episodeSortOption = epSortOpt,
            episodeSortAscending = prefs[Keys.EPISODE_SORT_ASCENDING]
                ?: (if (hasBoot && sp.contains("ep_sort_asc")) sp.getBoolean("ep_sort_asc", epSortOpt.defaultAscending) else epSortOpt.defaultAscending),
            quickActions = QuickActionsSerializer.parse(rawQuickActions),
            playerUiScalePercent = (
                prefs[Keys.PLAYER_UI_SCALE_PERCENT]
                    ?: (if (hasBoot && sp.contains("player_ui_scale")) sp.getInt("player_ui_scale", 100) else 100)
                ).coerceIn(75, 140),
            playerAutoDpiScale = true,
            controlsAutoHideSeconds = (
                prefs[Keys.CONTROLS_AUTO_HIDE_SECONDS]
                    ?: (if (hasBoot && sp.contains("controls_hide_sec")) sp.getInt("controls_hide_sec", 4) else 4)
                ).coerceIn(2, 15),

            resumePlayback = prefs[Keys.RESUME_PLAYBACK]
                ?: (if (hasBoot && sp.contains("resume_playback")) sp.getBoolean("resume_playback", true) else true),
            playbackEndMode = PlaybackEndMode.fromId(
                prefs[Keys.PLAYBACK_END_MODE]
                    ?: (if (hasBoot) sp.getString("playback_end_mode", null) else null)
                    ?: PlaybackEndMode.AUTO_NEXT.id
            ),
            enableEmbeddedChapters = true,
            showChapterMarkersOnTimeline = prefs[Keys.SHOW_CHAPTER_MARKERS]
                ?: (if (hasBoot && sp.contains("show_chapter_markers")) sp.getBoolean("show_chapter_markers", true) else true),
            seekStepSeconds = (
                prefs[Keys.SEEK_STEP_SECONDS]
                    ?: (if (hasBoot && sp.contains("seek_step_sec")) sp.getInt("seek_step_sec", 10) else 10)
                ).coerceIn(3, 60),
            defaultPlaybackSpeed = (
                prefs[Keys.DEFAULT_PLAYBACK_SPEED]
                    ?: (if (hasBoot && sp.contains("default_speed")) sp.getFloat("default_speed", 1.0f) else 1.0f)
                ).coerceIn(0.25f, 3.0f),
            showSkipIntroButton = prefs[Keys.SHOW_SKIP_INTRO_BUTTON]
                ?: (if (hasBoot && sp.contains("show_skip_intro")) sp.getBoolean("show_skip_intro", true) else true),
            skipIntroSeconds = (
                prefs[Keys.SKIP_INTRO_SECONDS]
                    ?: (if (hasBoot && sp.contains("skip_intro_sec")) sp.getInt("skip_intro_sec", 85) else 85)
                ).coerceIn(5, 600),
            autoEnterPip = prefs[Keys.AUTO_ENTER_PIP]
                ?: (if (hasBoot && sp.contains("auto_pip")) sp.getBoolean("auto_pip", true) else true),
            backgroundAudioPlay = prefs[Keys.BACKGROUND_AUDIO_PLAY]
                ?: (if (hasBoot && sp.contains("bg_audio_play")) sp.getBoolean("bg_audio_play", false) else false),

            gesturesBrightnessVolume = prefs[Keys.GESTURES_BRIGHTNESS_VOLUME]
                ?: (if (hasBoot && sp.contains("gest_bright_vol")) sp.getBoolean("gest_bright_vol", true) else true),
            gestureHorizontalSeek = prefs[Keys.GESTURE_HORIZONTAL_SEEK]
                ?: (if (hasBoot && sp.contains("gest_horiz_seek")) sp.getBoolean("gest_horiz_seek", true) else true),
            gestureDoubleTapSeek = prefs[Keys.GESTURE_DOUBLE_TAP_SEEK]
                ?: (if (hasBoot && sp.contains("gest_double_tap")) sp.getBoolean("gest_double_tap", true) else true),
            gestureLongPress2x = prefs[Keys.GESTURE_LONG_PRESS_2X]
                ?: (if (hasBoot && sp.contains("gest_long_press")) sp.getBoolean("gest_long_press", true) else true),
            longPressBoostSpeed = (
                prefs[Keys.LONG_PRESS_BOOST_SPEED]
                    ?: (if (hasBoot && sp.contains("long_press_speed")) sp.getFloat("long_press_speed", 2.0f) else 2.0f)
                ).coerceIn(1.25f, 3.0f),
            defaultPlayerOrientation = PlayerOrientationMode.fromId(rawOrientation),

            decoderMode = DecoderMode.fromId(
                prefs[Keys.DECODER_MODE]
                    ?: (if (hasBoot) sp.getString("decoder_mode", null) else null)
                    ?: DecoderMode.HW_PLUS.id
            ),
            defaultScaleMode = VideoScaleMode.fromId(
                prefs[Keys.DEFAULT_SCALE_MODE]
                    ?: (if (hasBoot) sp.getString("scale_mode", null) else null)
                    ?: VideoScaleMode.FIT.id
            ),
            highRefreshRateEnabled = true,
            ltpoAdaptiveRefreshEnabled = true,
            frameInterpolationEnabled = rawFrameInterpEnabled,
            frameInterpolationMode = FrameInterpolationMode.fromId(rawFrameInterpMode),
            anime4kEnabled = rawAnime4kEnabled,
            anime4kPreset = Anime4KPreset.fromId(rawAnime4kPreset),
            anime4kStrength = (
                prefs[Keys.ANIME4K_STRENGTH]
                    ?: (if (hasBoot && sp.contains("a4k_strength")) sp.getFloat("a4k_strength", 1.0f) else 1.0f)
                ).coerceIn(0.2f, 1.5f),

            autoBindExternalAudio = prefs[Keys.AUTO_BIND_AUDIO]
                ?: (if (hasBoot && sp.contains("auto_bind_audio")) sp.getBoolean("auto_bind_audio", true) else true),
            preferExternalAudioOnStart = prefs[Keys.PREFER_EXTERNAL_AUDIO_ON_START]
                ?: (if (hasBoot && sp.contains("prefer_ext_audio")) sp.getBoolean("prefer_ext_audio", true) else true),
            defaultAudioBoostPercent = (
                prefs[Keys.DEFAULT_AUDIO_BOOST_PERCENT]
                    ?: (if (hasBoot && sp.contains("default_audio_boost")) sp.getInt("default_audio_boost", 100) else 100)
                ).coerceIn(100, 200),
            bluetoothAudioAutoSync = true,
            bluetoothAudioOffsetMs = (
                prefs[Keys.BLUETOOTH_AUDIO_OFFSET_MS]
                    ?: (if (hasBoot && sp.contains("bt_fine_offset_ms")) sp.getLong("bt_fine_offset_ms", 0L) else 0L)
                ).coerceIn(-300L, 300L),

            autoBindExternalSubtitles = prefs[Keys.AUTO_BIND_SUBS]
                ?: (if (hasBoot && sp.contains("auto_bind_subs")) sp.getBoolean("auto_bind_subs", true) else true),
            subtitleFontSizeSp = (
                prefs[Keys.SUB_FONT_SIZE]
                    ?: (if (hasBoot && sp.contains("sub_font_size")) sp.getFloat("sub_font_size", 21f) else 21f)
                ).coerceIn(13f, 42f),
            subtitleColorArgb = prefs[Keys.SUB_COLOR_ARGB]
                ?: (if (hasBoot && sp.contains("sub_color_argb")) sp.getLong("sub_color_argb", SubtitleColorPreset.WHITE.argb) else SubtitleColorPreset.WHITE.argb),
            subtitleBackgroundEnabled = prefs[Keys.SUB_BG_ENABLED]
                ?: (if (hasBoot && sp.contains("sub_bg_enabled")) sp.getBoolean("sub_bg_enabled", true) else true),

            torrServerUrl = (
                prefs[Keys.TORRSERVER_URL]
                    ?: (if (hasBoot) sp.getString("torr_url", null) else null)
                    ?: "http://127.0.0.1:8090"
                ).trim().ifBlank { "http://127.0.0.1:8090" },
            torrServerUsername = prefs[Keys.TORRSERVER_USERNAME]
                ?: (if (hasBoot) sp.getString("torr_user", null) else null)
                ?: "",
            torrServerPassword = prefs[Keys.TORRSERVER_PASSWORD]
                ?: (if (hasBoot) sp.getString("torr_pass", null) else null)
                ?: "",
            torrServerAutoPlaySingle = prefs[Keys.TORRSERVER_AUTO_PLAY_SINGLE]
                ?: (if (hasBoot && sp.contains("torr_auto_single")) sp.getBoolean("torr_auto_single", true) else true),
            torrServerSaveToDb = prefs[Keys.TORRSERVER_SAVE_TO_DB]
                ?: (if (hasBoot && sp.contains("torr_save_db")) sp.getBoolean("torr_save_db", true) else true),
            torrentIndexerUrl = (
                prefs[Keys.TORRENT_INDEXER_URL]
                    ?: (if (hasBoot) sp.getString("torr_indexer_url", null) else null)
                    ?: "https://jacred.xyz"
                ).trim().ifBlank { "https://jacred.xyz" },
            torrentIndexerApiKey = prefs[Keys.TORRENT_INDEXER_API_KEY]
                ?: (if (hasBoot) sp.getString("torr_indexer_key", null) else null)
                ?: "",
            enableDirectRutorSearch = prefs[Keys.ENABLE_DIRECT_RUTOR_SEARCH]
                ?: (if (hasBoot && sp.contains("enable_rutor")) sp.getBoolean("enable_rutor", true) else true),
            enableAnimeTrackerSearch = prefs[Keys.ENABLE_ANIME_TRACKER_SEARCH]
                ?: (if (hasBoot && sp.contains("enable_anime_tracker")) sp.getBoolean("enable_anime_tracker", true) else true),
            enableTrackerDoh = prefs[Keys.ENABLE_TRACKER_DOH]
                ?: (if (hasBoot && sp.contains("enable_doh")) sp.getBoolean("enable_doh", true) else true),

            ignoreNomediaFolders = prefs[Keys.IGNORE_NOMEDIA_FOLDERS]
                ?: (if (hasBoot && sp.contains("ignore_nomedia")) sp.getBoolean("ignore_nomedia", true) else true),
            excludeCameraAndScreenshots = prefs[Keys.EXCLUDE_CAMERA_AND_SCREENSHOTS]
                ?: (if (hasBoot && sp.contains("exclude_camera")) sp.getBoolean("exclude_camera", false) else false),
            minVideoSizeMb = (
                prefs[Keys.MIN_VIDEO_SIZE_MB]
                    ?: (if (hasBoot && sp.contains("min_video_mb")) sp.getInt("min_video_mb", 0) else 0)
                ).coerceIn(0, 500),
            skipDeleteConfirmation = prefs[Keys.SKIP_DELETE_CONFIRMATION]
                ?: (if (hasBoot && sp.contains("skip_delete_confirm")) sp.getBoolean("skip_delete_confirm", false) else false)
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
        bootPrefs(context).edit().putBoolean("compact_ui", true).apply()
        context.playerDataStore.edit { it[Keys.COMPACT_UI_MODE] = true }
    }

    suspend fun updateShowEpisodeThumbnails(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("show_thumbs", enabled).apply()
        context.playerDataStore.edit { it[Keys.SHOW_EPISODE_THUMBNAILS] = enabled }
    }

    suspend fun updateShowEpisodesButtonInHud(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("show_ep_hud", enabled).apply()
        context.playerDataStore.edit { it[Keys.SHOW_EPISODES_BUTTON_IN_HUD] = enabled }
    }

    suspend fun updateShowHudBadges(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("show_hud_badges", true).apply()
        context.playerDataStore.edit { it[Keys.SHOW_HUD_BADGES] = true }
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
        bootPrefs(context).edit().putBoolean("sort_asc", ascending).apply()
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
        bootPrefs(context).edit().putBoolean("ep_sort_asc", ascending).apply()
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
        val serialized = QuickActionsSerializer.serialize(actions)
        bootPrefs(context).edit().putString("quick_actions_v3", serialized).apply()
        context.playerDataStore.edit {
            it[Keys.PLAYER_QUICK_ACTIONS_V3] = serialized
        }
    }

    suspend fun updatePlayerUiScalePercent(percent: Int) {
        val clamped = percent.coerceIn(75, 140)
        bootPrefs(context).edit().putInt("player_ui_scale", clamped).apply()
        context.playerDataStore.edit { it[Keys.PLAYER_UI_SCALE_PERCENT] = clamped }
    }

    suspend fun updatePlayerAutoDpiScale(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("player_auto_dpi", true).apply()
        context.playerDataStore.edit { it[Keys.PLAYER_AUTO_DPI_SCALE] = true }
    }

    suspend fun updateControlsAutoHideSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(2, 15)
        bootPrefs(context).edit().putInt("controls_hide_sec", clamped).apply()
        context.playerDataStore.edit { it[Keys.CONTROLS_AUTO_HIDE_SECONDS] = clamped }
    }

    suspend fun updateResumePlayback(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("resume_playback", enabled).apply()
        context.playerDataStore.edit { it[Keys.RESUME_PLAYBACK] = enabled }
    }

    suspend fun setLastPlayedVideoUri(uri: String) {
        context.playerDataStore.edit { it[Keys.LAST_PLAYED_VIDEO_URI] = uri }
    }

    suspend fun clearLastPlayedVideoUri() {
        context.playerDataStore.edit { it[Keys.LAST_PLAYED_VIDEO_URI] = "" }
    }

    suspend fun updatePlaybackEndMode(mode: PlaybackEndMode) {
        bootPrefs(context).edit().putString("playback_end_mode", mode.id).apply()
        context.playerDataStore.edit { it[Keys.PLAYBACK_END_MODE] = mode.id }
    }

    suspend fun updateEnableEmbeddedChapters(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("enable_embedded_chapters", true).apply()
        context.playerDataStore.edit { it[Keys.ENABLE_EMBEDDED_CHAPTERS] = true }
    }

    suspend fun updateShowChapterMarkersOnTimeline(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("show_chapter_markers", enabled).apply()
        context.playerDataStore.edit { it[Keys.SHOW_CHAPTER_MARKERS] = enabled }
    }

    suspend fun updateSeekStepSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(3, 60)
        bootPrefs(context).edit().putInt("seek_step_sec", clamped).apply()
        context.playerDataStore.edit { it[Keys.SEEK_STEP_SECONDS] = clamped }
    }

    suspend fun updateDefaultPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        bootPrefs(context).edit().putFloat("default_speed", clamped).apply()
        context.playerDataStore.edit { it[Keys.DEFAULT_PLAYBACK_SPEED] = clamped }
    }

    suspend fun updateShowSkipIntroButton(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("show_skip_intro", enabled).apply()
        context.playerDataStore.edit { it[Keys.SHOW_SKIP_INTRO_BUTTON] = enabled }
    }

    suspend fun updateSkipIntroSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(5, 600)
        bootPrefs(context).edit().putInt("skip_intro_sec", clamped).apply()
        context.playerDataStore.edit { it[Keys.SKIP_INTRO_SECONDS] = clamped }
    }

    suspend fun updateAutoEnterPip(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("auto_pip", enabled).apply()
        context.playerDataStore.edit { it[Keys.AUTO_ENTER_PIP] = enabled }
    }

    suspend fun updateBackgroundAudioPlay(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("bg_audio_play", enabled).apply()
        context.playerDataStore.edit { it[Keys.BACKGROUND_AUDIO_PLAY] = enabled }
    }

    suspend fun updateGesturesBrightnessVolume(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("gest_bright_vol", enabled).apply()
        context.playerDataStore.edit { it[Keys.GESTURES_BRIGHTNESS_VOLUME] = enabled }
    }

    suspend fun updateGestureHorizontalSeek(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("gest_horiz_seek", enabled).apply()
        context.playerDataStore.edit { it[Keys.GESTURE_HORIZONTAL_SEEK] = enabled }
    }

    suspend fun updateGestureDoubleTapSeek(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("gest_double_tap", enabled).apply()
        context.playerDataStore.edit { it[Keys.GESTURE_DOUBLE_TAP_SEEK] = enabled }
    }

    suspend fun updateGestureLongPress2x(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("gest_long_press", enabled).apply()
        context.playerDataStore.edit { it[Keys.GESTURE_LONG_PRESS_2X] = enabled }
    }

    suspend fun updateLongPressBoostSpeed(speed: Float) {
        val clamped = speed.coerceIn(1.25f, 3.0f)
        bootPrefs(context).edit().putFloat("long_press_speed", clamped).apply()
        context.playerDataStore.edit { it[Keys.LONG_PRESS_BOOST_SPEED] = clamped }
    }

    suspend fun updateDefaultPlayerOrientation(mode: PlayerOrientationMode) {
        bootPrefs(context).edit().putString("orientation", mode.id).apply()
        context.playerDataStore.edit { it[Keys.DEFAULT_PLAYER_ORIENTATION_V2] = mode.id }
    }

    suspend fun updateDecoderMode(mode: DecoderMode) {
        bootPrefs(context).edit().putString("decoder_mode", mode.id).apply()
        context.playerDataStore.edit { it[Keys.DECODER_MODE] = mode.id }
    }

    suspend fun updateDefaultScaleMode(mode: VideoScaleMode) {
        bootPrefs(context).edit().putString("scale_mode", mode.id).apply()
        context.playerDataStore.edit { it[Keys.DEFAULT_SCALE_MODE] = mode.id }
    }

    suspend fun updateHighRefreshRateEnabled(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("high_hz", true).apply()
        context.playerDataStore.edit { it[Keys.HIGH_REFRESH_RATE_ENABLED] = true }
    }

    suspend fun updateLtpoAdaptiveRefreshEnabled(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("ltpo_hz", true).apply()
        context.playerDataStore.edit { it[Keys.LTPO_ADAPTIVE_REFRESH_ENABLED] = true }
    }

    suspend fun updateFrameInterpolationEnabled(enabled: Boolean) {
        context.playerDataStore.edit { prefs ->
            prefs[Keys.FRAME_INTERPOLATION_ENABLED] = enabled
            if (enabled && FrameInterpolationMode.fromId(
                    prefs[Keys.FRAME_INTERPOLATION_MODE]
                        ?: prefs[Keys.LEGACY_FRAME_INTERPOLATION_MODE_V1]
                        ?: FrameInterpolationMode.OFF.id
                ) == FrameInterpolationMode.OFF
            ) {
                prefs[Keys.FRAME_INTERPOLATION_MODE] = FrameInterpolationMode.MAX_FPS.id
            }
        }
    }

    suspend fun updateFrameInterpolationMode(mode: FrameInterpolationMode) {
        bootPrefs(context).edit()
            .putString("frame_interp_mode_v2", mode.id)
            .putBoolean("frame_interp_enabled_v2", mode != FrameInterpolationMode.OFF)
            .apply()
        context.playerDataStore.edit { prefs ->
            prefs[Keys.FRAME_INTERPOLATION_MODE] = mode.id
            prefs[Keys.FRAME_INTERPOLATION_ENABLED] = (mode != FrameInterpolationMode.OFF)
        }
    }

    suspend fun updateAnime4KEnabled(enabled: Boolean) {
        context.playerDataStore.edit { prefs ->
            prefs[Keys.ANIME4K_ENABLED] = enabled
            if (enabled && Anime4KPreset.fromId(
                    prefs[Keys.ANIME4K_PRESET]
                        ?: prefs[Keys.LEGACY_ANIME4K_PRESET_V1]
                        ?: Anime4KPreset.OFF.id
                ) == Anime4KPreset.OFF
            ) {
                prefs[Keys.ANIME4K_PRESET] = Anime4KPreset.ANIME4K_UPSCALE_CNN.id
            }
        }
    }

    suspend fun updateAnime4KPreset(preset: Anime4KPreset) {
        bootPrefs(context).edit()
            .putString("a4k_preset_v2", preset.id)
            .putBoolean("a4k_enabled_v2", preset != Anime4KPreset.OFF)
            .apply()
        context.playerDataStore.edit { prefs ->
            prefs[Keys.ANIME4K_PRESET] = preset.id
            prefs[Keys.ANIME4K_ENABLED] = (preset != Anime4KPreset.OFF)
        }
    }

    suspend fun updateAnime4KStrength(strength: Float) {
        val clamped = strength.coerceIn(0.2f, 1.5f)
        bootPrefs(context).edit().putFloat("a4k_strength", clamped).apply()
        context.playerDataStore.edit {
            it[Keys.ANIME4K_STRENGTH] = clamped
        }
    }

    suspend fun updateAutoBindAudio(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("auto_bind_audio", enabled).apply()
        context.playerDataStore.edit { it[Keys.AUTO_BIND_AUDIO] = enabled }
    }

    suspend fun updatePreferExternalAudioOnStart(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("prefer_ext_audio", enabled).apply()
        context.playerDataStore.edit { it[Keys.PREFER_EXTERNAL_AUDIO_ON_START] = enabled }
    }

    suspend fun updateDefaultAudioBoostPercent(percent: Int) {
        val clamped = percent.coerceIn(100, 200)
        bootPrefs(context).edit().putInt("default_audio_boost", clamped).apply()
        context.playerDataStore.edit { it[Keys.DEFAULT_AUDIO_BOOST_PERCENT] = clamped }
    }

    suspend fun updateBluetoothAudioAutoSync(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("bt_auto_sync", true).apply()
        context.playerDataStore.edit { it[Keys.BLUETOOTH_AUDIO_AUTO_SYNC] = true }
    }

    suspend fun updateBluetoothAudioOffsetMs(offsetMs: Long) {
        val clamped = offsetMs.coerceIn(-300L, 300L)
        bootPrefs(context).edit().putLong("bt_fine_offset_ms", clamped).apply()
        context.playerDataStore.edit { it[Keys.BLUETOOTH_AUDIO_OFFSET_MS] = clamped }
    }

    suspend fun updateAutoBindSubtitles(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("auto_bind_subs", enabled).apply()
        context.playerDataStore.edit { it[Keys.AUTO_BIND_SUBS] = enabled }
    }

    suspend fun updateSubtitleFontSize(sizeSp: Float) {
        val clamped = sizeSp.coerceIn(13f, 42f)
        bootPrefs(context).edit().putFloat("sub_font_size", clamped).apply()
        context.playerDataStore.edit {
            it[Keys.SUB_FONT_SIZE] = clamped
        }
    }

    suspend fun updateSubtitleColor(argb: Long) {
        bootPrefs(context).edit().putLong("sub_color_argb", argb).apply()
        context.playerDataStore.edit { it[Keys.SUB_COLOR_ARGB] = argb }
    }

    suspend fun updateSubtitleBackground(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("sub_bg_enabled", enabled).apply()
        context.playerDataStore.edit { it[Keys.SUB_BG_ENABLED] = enabled }
    }

    suspend fun updateIgnoreNomediaFolders(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("ignore_nomedia", enabled).apply()
        context.playerDataStore.edit { it[Keys.IGNORE_NOMEDIA_FOLDERS] = enabled }
    }

    suspend fun updateExcludeCameraAndScreenshots(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("exclude_camera", enabled).apply()
        context.playerDataStore.edit { it[Keys.EXCLUDE_CAMERA_AND_SCREENSHOTS] = enabled }
    }

    suspend fun updateMinVideoSizeMb(sizeMb: Int) {
        val clamped = sizeMb.coerceIn(0, 500)
        bootPrefs(context).edit().putInt("min_video_mb", clamped).apply()
        context.playerDataStore.edit { it[Keys.MIN_VIDEO_SIZE_MB] = clamped }
    }

    suspend fun updateSkipDeleteConfirmation(skip: Boolean) {
        bootPrefs(context).edit().putBoolean("skip_delete_confirm", skip).apply()
        context.playerDataStore.edit { it[Keys.SKIP_DELETE_CONFIRMATION] = skip }
    }

    suspend fun updateTorrServerUrl(url: String) {
        val cleaned = url.trim().ifBlank { "http://127.0.0.1:8090" }
        bootPrefs(context).edit().putString("torr_url", cleaned).apply()
        context.playerDataStore.edit { it[Keys.TORRSERVER_URL] = cleaned }
    }

    suspend fun updateTorrServerAuth(username: String, password: String) {
        bootPrefs(context).edit()
            .putString("torr_user", username.trim())
            .putString("torr_pass", password)
            .apply()
        context.playerDataStore.edit { prefs ->
            prefs[Keys.TORRSERVER_USERNAME] = username.trim()
            prefs[Keys.TORRSERVER_PASSWORD] = password
        }
    }

    suspend fun updateTorrServerAutoPlaySingle(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("torr_auto_single", enabled).apply()
        context.playerDataStore.edit { it[Keys.TORRSERVER_AUTO_PLAY_SINGLE] = enabled }
    }

    suspend fun updateTorrServerSaveToDb(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("torr_save_db", enabled).apply()
        context.playerDataStore.edit { it[Keys.TORRSERVER_SAVE_TO_DB] = enabled }
    }

    suspend fun updateTorrentIndexerConfig(url: String, apiKey: String) {
        val cleanedUrl = url.trim().ifBlank { "https://jacred.xyz" }
        bootPrefs(context).edit()
            .putString("torr_indexer_url", cleanedUrl)
            .putString("torr_indexer_key", apiKey.trim())
            .apply()
        context.playerDataStore.edit { prefs ->
            prefs[Keys.TORRENT_INDEXER_URL] = cleanedUrl
            prefs[Keys.TORRENT_INDEXER_API_KEY] = apiKey.trim()
        }
    }

    suspend fun updateEnableDirectRutorSearch(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("enable_rutor", enabled).apply()
        context.playerDataStore.edit { it[Keys.ENABLE_DIRECT_RUTOR_SEARCH] = enabled }
    }

    suspend fun updateEnableAnimeTrackerSearch(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("enable_anime_tracker", enabled).apply()
        context.playerDataStore.edit { it[Keys.ENABLE_ANIME_TRACKER_SEARCH] = enabled }
    }

    suspend fun updateEnableTrackerDoh(enabled: Boolean) {
        bootPrefs(context).edit().putBoolean("enable_doh", enabled).apply()
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
         * and layout density on frame #1 with zero flash and zero settings loss across updates.
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
                compactUiMode = true,
                showEpisodeThumbnails = sp.getBoolean("show_thumbs", true),
                showEpisodesButtonInHud = sp.getBoolean("show_ep_hud", true),
                showHudBadges = true,
                folderSortOption = sortOpt,
                folderSortAscending = sp.getBoolean("sort_asc", sortOpt.defaultAscending),
                episodeSortOption = epSortOpt,
                episodeSortAscending = sp.getBoolean("ep_sort_asc", epSortOpt.defaultAscending),
                quickActions = QuickActionsSerializer.parse(sp.getString("quick_actions_v3", null)),
                playerUiScalePercent = sp.getInt("player_ui_scale", 100).coerceIn(75, 140),
                playerAutoDpiScale = true,
                controlsAutoHideSeconds = sp.getInt("controls_hide_sec", 4).coerceIn(2, 15),

                resumePlayback = sp.getBoolean("resume_playback", true),
                playbackEndMode = PlaybackEndMode.fromId(
                    sp.getString("playback_end_mode", PlaybackEndMode.AUTO_NEXT.id) ?: PlaybackEndMode.AUTO_NEXT.id
                ),
                enableEmbeddedChapters = true,
                showChapterMarkersOnTimeline = sp.getBoolean("show_chapter_markers", true),
                seekStepSeconds = sp.getInt("seek_step_sec", 10).coerceIn(3, 60),
                defaultPlaybackSpeed = sp.getFloat("default_speed", 1.0f).coerceIn(0.25f, 3.0f),
                showSkipIntroButton = sp.getBoolean("show_skip_intro", true),
                skipIntroSeconds = sp.getInt("skip_intro_sec", 85).coerceIn(5, 600),
                autoEnterPip = sp.getBoolean("auto_pip", true),
                backgroundAudioPlay = sp.getBoolean("bg_audio_play", false),

                gesturesBrightnessVolume = sp.getBoolean("gest_bright_vol", true),
                gestureHorizontalSeek = sp.getBoolean("gest_horiz_seek", true),
                gestureDoubleTapSeek = sp.getBoolean("gest_double_tap", true),
                gestureLongPress2x = sp.getBoolean("gest_long_press", true),
                longPressBoostSpeed = sp.getFloat("long_press_speed", 2.0f).coerceIn(1.25f, 3.0f),
                defaultPlayerOrientation = PlayerOrientationMode.fromId(
                    sp.getString("orientation", PlayerOrientationMode.VIDEO_ASPECT.id) ?: PlayerOrientationMode.VIDEO_ASPECT.id
                ),

                decoderMode = DecoderMode.fromId(
                    sp.getString("decoder_mode", DecoderMode.HW_PLUS.id) ?: DecoderMode.HW_PLUS.id
                ),
                defaultScaleMode = VideoScaleMode.fromId(
                    sp.getString("scale_mode", VideoScaleMode.FIT.id) ?: VideoScaleMode.FIT.id
                ),
                highRefreshRateEnabled = true,
                ltpoAdaptiveRefreshEnabled = true,
                frameInterpolationEnabled = sp.getBoolean("frame_interp_enabled_v2", false),
                frameInterpolationMode = FrameInterpolationMode.fromId(
                    sp.getString("frame_interp_mode_v2", FrameInterpolationMode.OFF.id) ?: FrameInterpolationMode.OFF.id
                ),
                anime4kEnabled = sp.getBoolean("a4k_enabled_v2", false),
                anime4kPreset = Anime4KPreset.fromId(
                    sp.getString("a4k_preset_v2", Anime4KPreset.OFF.id) ?: Anime4KPreset.OFF.id
                ),
                anime4kStrength = sp.getFloat("a4k_strength", 1.0f).coerceIn(0.2f, 1.5f),

                autoBindExternalAudio = sp.getBoolean("auto_bind_audio", true),
                preferExternalAudioOnStart = sp.getBoolean("prefer_ext_audio", true),
                defaultAudioBoostPercent = sp.getInt("default_audio_boost", 100).coerceIn(100, 200),
                bluetoothAudioAutoSync = true,
                bluetoothAudioOffsetMs = sp.getLong("bt_fine_offset_ms", 0L).coerceIn(-300L, 300L),

                autoBindExternalSubtitles = sp.getBoolean("auto_bind_subs", true),
                subtitleFontSizeSp = sp.getFloat("sub_font_size", 21f).coerceIn(13f, 42f),
                subtitleColorArgb = sp.getLong("sub_color_argb", SubtitleColorPreset.WHITE.argb),
                subtitleBackgroundEnabled = sp.getBoolean("sub_bg_enabled", true),

                torrServerUrl = sp.getString("torr_url", "http://127.0.0.1:8090") ?: "http://127.0.0.1:8090",
                torrServerUsername = sp.getString("torr_user", "") ?: "",
                torrServerPassword = sp.getString("torr_pass", "") ?: "",
                torrServerAutoPlaySingle = sp.getBoolean("torr_auto_single", true),
                torrServerSaveToDb = sp.getBoolean("torr_save_db", true),
                torrentIndexerUrl = sp.getString("torr_indexer_url", "https://jacred.xyz") ?: "https://jacred.xyz",
                torrentIndexerApiKey = sp.getString("torr_indexer_key", "") ?: "",
                enableDirectRutorSearch = sp.getBoolean("enable_rutor", true),
                enableAnimeTrackerSearch = sp.getBoolean("enable_anime_tracker", true),
                enableTrackerDoh = sp.getBoolean("enable_doh", true),

                ignoreNomediaFolders = sp.getBoolean("ignore_nomedia", true),
                excludeCameraAndScreenshots = sp.getBoolean("exclude_camera", false),
                minVideoSizeMb = sp.getInt("min_video_mb", 0).coerceIn(0, 500),
                skipDeleteConfirmation = sp.getBoolean("skip_delete_confirm", false)
            )
        }

        private fun writeBootSettingsSnapshot(context: Context, s: PlayerSettings) {
            bootPrefs(context).edit()
                .putString("app_lang", s.appLanguage.id)
                .putString("theme_mode", s.themeMode.id)
                .putString("controls_theme", s.playerControlsTheme.id)
                .putBoolean("compact_ui", true)
                .putBoolean("show_thumbs", s.showEpisodeThumbnails)
                .putBoolean("show_ep_hud", s.showEpisodesButtonInHud)
                .putBoolean("show_hud_badges", true)
                .putString("sort_opt", s.folderSortOption.id)
                .putBoolean("sort_asc", s.folderSortAscending)
                .putString("ep_sort_opt", s.episodeSortOption.id)
                .putBoolean("ep_sort_asc", s.episodeSortAscending)
                .putString("quick_actions_v3", QuickActionsSerializer.serialize(s.quickActions))
                .putInt("player_ui_scale", s.playerUiScalePercent)
                .putBoolean("player_auto_dpi", true)
                .putInt("controls_hide_sec", s.controlsAutoHideSeconds)
                .putBoolean("resume_playback", s.resumePlayback)
                .putString("playback_end_mode", s.playbackEndMode.id)
                .putBoolean("enable_embedded_chapters", true)
                .putBoolean("show_chapter_markers", s.showChapterMarkersOnTimeline)
                .putInt("seek_step_sec", s.seekStepSeconds)
                .putFloat("default_speed", s.defaultPlaybackSpeed)
                .putBoolean("show_skip_intro", s.showSkipIntroButton)
                .putInt("skip_intro_sec", s.skipIntroSeconds)
                .putBoolean("auto_pip", s.autoEnterPip)
                .putBoolean("bg_audio_play", s.backgroundAudioPlay)
                .putBoolean("gest_bright_vol", s.gesturesBrightnessVolume)
                .putBoolean("gest_horiz_seek", s.gestureHorizontalSeek)
                .putBoolean("gest_double_tap", s.gestureDoubleTapSeek)
                .putBoolean("gest_long_press", s.gestureLongPress2x)
                .putFloat("long_press_speed", s.longPressBoostSpeed)
                .putString("orientation", s.defaultPlayerOrientation.id)
                .putString("decoder_mode", s.decoderMode.id)
                .putString("scale_mode", s.defaultScaleMode.id)
                .putBoolean("high_hz", true)
                .putBoolean("ltpo_hz", true)
                .putBoolean("frame_interp_enabled_v2", s.frameInterpolationEnabled)
                .putString("frame_interp_mode_v2", s.frameInterpolationMode.id)
                .putBoolean("a4k_enabled_v2", s.anime4kEnabled)
                .putString("a4k_preset_v2", s.anime4kPreset.id)
                .putFloat("a4k_strength", s.anime4kStrength)
                .putBoolean("auto_bind_audio", s.autoBindExternalAudio)
                .putBoolean("prefer_ext_audio", s.preferExternalAudioOnStart)
                .putInt("default_audio_boost", s.defaultAudioBoostPercent)
                .putBoolean("bt_auto_sync", true)
                .putLong("bt_fine_offset_ms", s.bluetoothAudioOffsetMs)
                .putBoolean("auto_bind_subs", s.autoBindExternalSubtitles)
                .putFloat("sub_font_size", s.subtitleFontSizeSp)
                .putLong("sub_color_argb", s.subtitleColorArgb)
                .putBoolean("sub_bg_enabled", s.subtitleBackgroundEnabled)
                .putString("torr_url", s.torrServerUrl)
                .putString("torr_user", s.torrServerUsername)
                .putString("torr_pass", s.torrServerPassword)
                .putBoolean("torr_auto_single", s.torrServerAutoPlaySingle)
                .putBoolean("torr_save_db", s.torrServerSaveToDb)
                .putString("torr_indexer_url", s.torrentIndexerUrl)
                .putString("torr_indexer_key", s.torrentIndexerApiKey)
                .putBoolean("enable_rutor", s.enableDirectRutorSearch)
                .putBoolean("enable_anime_tracker", s.enableAnimeTrackerSearch)
                .putBoolean("enable_doh", s.enableTrackerDoh)
                .putBoolean("ignore_nomedia", s.ignoreNomediaFolders)
                .putBoolean("exclude_camera", s.excludeCameraAndScreenshots)
                .putInt("min_video_mb", s.minVideoSizeMb)
                .putBoolean("skip_delete_confirm", s.skipDeleteConfirmation)
                .apply()
        }
    }
}
