package com.example.ui.i18n

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import com.example.model.QuickBarPreset
import com.example.model.SleepTimerPreset
import com.example.model.SubtitleColorPreset
import com.example.model.VideoScaleMode
import com.example.torrent.TorrentQualityFilter
import com.example.torrent.TorrentSortMode
import com.example.torrent.TorrentTrackerGroup
import java.util.Locale

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.SYSTEM }
val LocalIsEnglish = staticCompositionLocalOf { false }

fun resolveIsEnglish(
    appLanguage: AppLanguage,
    systemLanguage: String = Locale.getDefault().language
): Boolean = when (appLanguage) {
    AppLanguage.EN -> true
    AppLanguage.RU -> false
    AppLanguage.SYSTEM -> systemLanguage.equals("en", ignoreCase = true)
}

private class LocaleDelegatingContextWrapper(
    base: Context,
    private val localizedResourcesContext: Context
) : ContextWrapper(base) {
    override fun getResources() = localizedResourcesContext.resources
    override fun getAssets() = localizedResourcesContext.assets
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

@Composable
fun ProvideAppLocalization(
    appLanguage: AppLanguage,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val hostActivity = remember(baseContext) { baseContext.findComponentActivity() }

    val targetLocale = remember(appLanguage, baseConfig) {
        when (appLanguage) {
            AppLanguage.RU -> Locale("ru")
            AppLanguage.EN -> Locale.ENGLISH
            AppLanguage.SYSTEM -> {
                val sysLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    baseConfig.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    baseConfig.locale
                }
                if (sysLocale != null && sysLocale.language.equals("en", ignoreCase = true)) {
                    Locale.ENGLISH
                } else {
                    Locale("ru")
                }
            }
        }
    }

    val isEnglish = remember(targetLocale) {
        targetLocale.language.equals("en", ignoreCase = true)
    }

    val localizedConfig = remember(baseContext, baseConfig, targetLocale) {
        Locale.setDefault(targetLocale)
        val config = Configuration(baseConfig)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(targetLocale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = targetLocale
        }
        config.setLayoutDirection(targetLocale)
        @Suppress("DEPRECATION")
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        @Suppress("DEPRECATION")
        baseContext.applicationContext?.resources?.updateConfiguration(
            config,
            baseContext.applicationContext.resources.displayMetrics
        )
        config
    }

    val currentRegistryOwner = LocalActivityResultRegistryOwner.current ?: hostActivity
    val currentBackDispatcherOwner = LocalOnBackPressedDispatcherOwner.current ?: hostActivity

    val values = buildList {
        add(LocalContext provides baseContext)
        add(LocalConfiguration provides localizedConfig)
        add(LocalAppLanguage provides appLanguage)
        add(LocalIsEnglish provides isEnglish)
        if (currentRegistryOwner != null) {
            add(LocalActivityResultRegistryOwner provides currentRegistryOwner)
        }
        if (currentBackDispatcherOwner != null) {
            add(LocalOnBackPressedDispatcherOwner provides currentBackDispatcherOwner)
        }
    }.toTypedArray()

    CompositionLocalProvider(*values) {
        content()
    }
}

@Composable
@ReadOnlyComposable
fun tr(ru: String, en: String): String =
    if (LocalIsEnglish.current) en else ru

// ---------------- Model Localization Extensions ----------------

fun AppLanguage.localizedSubtitle(isEnglish: Boolean): String = when (this) {
    AppLanguage.SYSTEM -> if (isEnglish) "Follows Android system locale (Russian / English)" else "Автоматический выбор по системному языку Android"
    AppLanguage.RU -> if (isEnglish) "Russian interface language" else "Полный интерфейс на русском языке"
    AppLanguage.EN -> if (isEnglish) "Full interface in English" else "Полный интерфейс на английском языке"
}

fun DecoderMode.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    DecoderMode.HW_PLUS -> "Hardware Decoding (HW+)"
    DecoderMode.HW_STRICT -> "Strict Hardware (HW)"
    DecoderMode.SOFTWARE_PREFER -> "Software Decoder (SW)"
}

fun DecoderMode.localizedSubtitle(isEnglish: Boolean): String = localizedDescription(isEnglish)

fun DecoderMode.localizedDescription(isEnglish: Boolean): String = if (!isEnglish) description else when (this) {
    DecoderMode.HW_PLUS -> "Prioritizes hardware MediaCodec chips (HEVC/AVC/AV1/VP9) with automatic fallback for non-standard profiles."
    DecoderMode.HW_STRICT -> "Exclusively uses GPU/SoC hardware decoders for maximum battery efficiency."
    DecoderMode.SOFTWARE_PREFER -> "Prioritizes CPU software codecs for rare or damaged video streams."
}

fun Anime4KPreset.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    Anime4KPreset.OFF -> "Off (Original)"
    Anime4KPreset.ANIME4K_FAST -> "Anime4K_Fast (ThinLines + Deblur)"
    Anime4KPreset.ANIME4K_UPSCALE_CNN -> "Anime4K_Upscale_CNN"
    Anime4KPreset.ANIME4K_UPSCALE_CNNX -> "Anime4K_Upscale_CNNX"
}

fun Anime4KPreset.localizedSubtitle(isEnglish: Boolean): String = localizedDescription(isEnglish)

fun Anime4KPreset.localizedDescription(isEnglish: Boolean): String = if (!isEnglish) description else when (this) {
    Anime4KPreset.OFF -> "Direct hardware decoder output without GPU post-processing."
    Anime4KPreset.ANIME4K_FAST -> "Fast line thinning and deblurring with minimal GPU load."
    Anime4KPreset.ANIME4K_UPSCALE_CNN -> "Balanced neural GLSL upscaling: CReLU line reconstruction and halo suppression."
    Anime4KPreset.ANIME4K_UPSCALE_CNNX -> "Maximum clarity: deep Sobel gradient reconstruction, dense CNNX convolution, and subpixel anti-aliasing."
}

fun FrameInterpolationMode.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    FrameInterpolationMode.OFF -> "Off (Original)"
    FrameInterpolationMode.CINEMA -> "Soft Cinema (×2 / 48–60 fps)"
    FrameInterpolationMode.SMOOTH_60 -> "Smooth 60 fps"
    FrameInterpolationMode.MAX_FPS -> "Max Display Rate (90–165 Hz)"
    FrameInterpolationMode.ANIME -> "Anime 2D (Contour Guard)"
}

fun FrameInterpolationMode.localizedSubtitle(isEnglish: Boolean): String = localizedDescription(isEnglish)

fun FrameInterpolationMode.localizedDescription(isEnglish: Boolean): String = if (!isEnglish) description else when (this) {
    FrameInterpolationMode.OFF -> "Direct native frame output (24/30 fps) without intermediate phase synthesis."
    FrameInterpolationMode.CINEMA -> "Gentle 2× panning smoothing (48/50/60 fps) preserving natural cinematic cadence."
    FrameInterpolationMode.SMOOTH_60 -> "Bidirectional MEMC motion compensation up to stable 60 fps with LTPO power savings."
    FrameInterpolationMode.MAX_FPS -> "Phase-locked frame interpolation matched to 90 / 120 / 144 / 165 Hz displays."
    FrameInterpolationMode.ANIME -> "2D animation mode: high refresh rate with thin line, subtitle, and scene-cut protection."
}

fun FrameInterpolationMode.localizedBadge(isEnglish: Boolean): String = if (!isEnglish) badgeLabel else when (this) {
    FrameInterpolationMode.OFF -> "OFF"
    FrameInterpolationMode.CINEMA -> "MEMC ×2"
    FrameInterpolationMode.SMOOTH_60 -> "MEMC 60"
    FrameInterpolationMode.MAX_FPS -> "MEMC MAX"
    FrameInterpolationMode.ANIME -> "MEMC 2D"
}

fun VideoScaleMode.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    VideoScaleMode.FIT -> "Fit to Screen"
    VideoScaleMode.ZOOM -> "Zoom (Fill / Crop)"
    VideoScaleMode.STRETCH -> "Stretch"
}

fun VideoScaleMode.localizedShortLabel(isEnglish: Boolean): String = if (!isEnglish) shortBadge else when (this) {
    VideoScaleMode.FIT -> "Fit"
    VideoScaleMode.ZOOM -> "Fill"
    VideoScaleMode.STRETCH -> "Stretch"
}

fun SubtitleColorPreset.localizedLabel(isEnglish: Boolean): String = if (!isEnglish) label else when (this) {
    SubtitleColorPreset.WHITE -> "White"
    SubtitleColorPreset.YELLOW -> "Cinema Yellow"
    SubtitleColorPreset.CYAN -> "Neon Cyan"
    SubtitleColorPreset.MINT -> "Mint"
    SubtitleColorPreset.AMBER -> "Amber"
    SubtitleColorPreset.SAKURA -> "Sakura Pink"
}

fun AppThemeMode.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    AppThemeMode.OLED_BLACK -> "Pure OLED (#000000)"
    AppThemeMode.SAKURA_OLED -> "Cyber Sakura OLED (#000000)"
    AppThemeMode.OBSIDIAN_DARK -> "Cinema Obsidian (Dark)"
    AppThemeMode.DAYLIGHT_LIGHT -> "Daylight (Light)"
    AppThemeMode.DYNAMIC_OLED -> "Material You + OLED (#000000)"
}

fun AppThemeMode.localizedSubtitle(isEnglish: Boolean): String = if (!isEnglish) subtitle else when (this) {
    AppThemeMode.OLED_BLACK -> "True black background turning off AMOLED subpixels with cyan accents"
    AppThemeMode.SAKURA_OLED -> "Pure OLED black background with neon crimson anime accents"
    AppThemeMode.OBSIDIAN_DARK -> "Soft deep graphite-blue home theater palette"
    AppThemeMode.DAYLIGHT_LIGHT -> "High-contrast light palette for bright environments"
    AppThemeMode.DYNAMIC_OLED -> "Android 12+ wallpaper accents on a pure OLED black background"
}

fun PlayerControlsTheme.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    PlayerControlsTheme.CLASSIC_OLED -> "Classic OLED"
    PlayerControlsTheme.PURE_ICONS -> "Pure Icons (Frameless)"
    PlayerControlsTheme.MONOCHROME_MIST -> "Monochrome (No Accents)"
    PlayerControlsTheme.GHOST_OUTLINE -> "Ghost Outline"
    PlayerControlsTheme.GLASS_CAPSULE -> "Glass Capsule"
    PlayerControlsTheme.CYBER_NEON -> "Cyber Neon"
    PlayerControlsTheme.MINIMAL_RING -> "Minimal Ring"
    PlayerControlsTheme.CINEMA_TACTILE -> "Studio Console"
}

fun PlayerControlsTheme.localizedSubtitle(isEnglish: Boolean): String = if (!isEnglish) subtitle else when (this) {
    PlayerControlsTheme.CLASSIC_OLED -> "Compact frosted-glass buttons with an accent Play key and slim timeline"
    PlayerControlsTheme.PURE_ICONS -> "Completely transparent buttons without backgrounds, borders, or accent colors"
    PlayerControlsTheme.MONOCHROME_MIST -> "Calm dark-gray smoked glass and white icons with zero distracting accent colors"
    PlayerControlsTheme.GHOST_OUTLINE -> "Frameless transparent buttons with a subtle neutral outline and no accent colors"
    PlayerControlsTheme.GLASS_CAPSULE -> "Translucent frosted pills with specular edge highlights and capsule bar"
    PlayerControlsTheme.CYBER_NEON -> "Frosted glass with neon accent glow and laser halo seek bar"
    PlayerControlsTheme.MINIMAL_RING -> "Compact airy rings with glass haze and minimalist thread timeline"
    PlayerControlsTheme.CINEMA_TACTILE -> "Compact frosted-glass studio deck with accent backlight and editing bar"
}

fun PlayerControlsTheme.localizedBadge(isEnglish: Boolean): String = if (!isEnglish) shortBadge else when (this) {
    PlayerControlsTheme.CLASSIC_OLED -> "Classic"
    PlayerControlsTheme.PURE_ICONS -> "Frameless"
    PlayerControlsTheme.MONOCHROME_MIST -> "Monochrome"
    PlayerControlsTheme.GHOST_OUTLINE -> "Outline"
    PlayerControlsTheme.GLASS_CAPSULE -> "Glass"
    PlayerControlsTheme.CYBER_NEON -> "Neon"
    PlayerControlsTheme.MINIMAL_RING -> "Ring"
    PlayerControlsTheme.CINEMA_TACTILE -> "Studio"
}

fun SleepTimerPreset.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) label else when (this) {
    SleepTimerPreset.OFF -> "Off"
    SleepTimerPreset.MIN_15 -> "15 min"
    SleepTimerPreset.MIN_30 -> "30 min"
    SleepTimerPreset.MIN_45 -> "45 min"
    SleepTimerPreset.MIN_60 -> "60 min"
    SleepTimerPreset.END_OF_EPISODE -> "End of episode"
}

fun SleepTimerPreset.localizedBadge(isEnglish: Boolean): String = localizedTitle(isEnglish)

fun PlaybackEndMode.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    PlaybackEndMode.AUTO_NEXT -> "Auto-play next episode"
    PlaybackEndMode.PAUSE_AT_END -> "Pause at end of episode"
    PlaybackEndMode.REPEAT_ONE -> "Repeat current episode"
}

fun PlaybackEndMode.localizedSubtitle(isEnglish: Boolean): String = localizedDescription(isEnglish)

fun PlaybackEndMode.localizedDescription(isEnglish: Boolean): String = if (!isEnglish) description else when (this) {
    PlaybackEndMode.AUTO_NEXT -> "Seamlessly start the next episode in the folder without stopping"
    PlaybackEndMode.PAUSE_AT_END -> "Stop playback after each episode finishes"
    PlaybackEndMode.REPEAT_ONE -> "Loop the active video continuously"
}

fun PlaybackEndMode.localizedBadge(isEnglish: Boolean): String = when (this) {
    PlaybackEndMode.AUTO_NEXT -> if (isEnglish) "Auto-Next" else "Автопереход"
    PlaybackEndMode.PAUSE_AT_END -> if (isEnglish) "Pause" else "Пауза"
    PlaybackEndMode.REPEAT_ONE -> if (isEnglish) "Repeat 1" else "Повтор 1"
}

fun PlayerOrientationMode.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    PlayerOrientationMode.VIDEO_ASPECT -> "By Video Aspect"
    PlayerOrientationMode.LANDSCAPE -> "Landscape (16:9)"
    PlayerOrientationMode.PORTRAIT -> "Portrait (Vertical)"
    PlayerOrientationMode.AUTO -> "Sensor (System)"
}

fun PlayerOrientationMode.localizedSubtitle(isEnglish: Boolean): String = if (!isEnglish) subtitle else when (this) {
    PlayerOrientationMode.VIDEO_ASPECT -> "Horizontal videos open in landscape, vertical videos in portrait"
    PlayerOrientationMode.LANDSCAPE -> "Always rotate horizontally with 180° sensor flip support"
    PlayerOrientationMode.PORTRAIT -> "Always keep vertical portrait orientation"
    PlayerOrientationMode.AUTO -> "Follow Android system auto-rotate setting"
}

fun PlayerOrientationMode.localizedBadge(isEnglish: Boolean): String = if (!isEnglish) shortBadge else when (this) {
    PlayerOrientationMode.VIDEO_ASPECT -> "By Aspect"
    PlayerOrientationMode.LANDSCAPE -> "Landscape"
    PlayerOrientationMode.PORTRAIT -> "Portrait"
    PlayerOrientationMode.AUTO -> "Sensor"
}

fun FolderSortOption.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    FolderSortOption.NAME -> "By Name"
    FolderSortOption.LAST_PLAYED -> "Recently Watched"
    FolderSortOption.DATE_MODIFIED -> "Date Modified"
    FolderSortOption.EPISODE_COUNT -> "Episode Count"
    FolderSortOption.TOTAL_SIZE -> "Folder Size"
    FolderSortOption.UNWATCHED -> "Unwatched Episodes"
}

fun FolderSortOption.localizedShortLabel(isEnglish: Boolean): String = if (!isEnglish) shortLabel else when (this) {
    FolderSortOption.NAME -> "Name"
    FolderSortOption.LAST_PLAYED -> "Recent"
    FolderSortOption.DATE_MODIFIED -> "Date"
    FolderSortOption.EPISODE_COUNT -> "Episodes"
    FolderSortOption.TOTAL_SIZE -> "Size"
    FolderSortOption.UNWATCHED -> "Unwatched"
}

fun FolderSortOption.localizedAscBadge(isEnglish: Boolean): String = if (!isEnglish) shortAscBadge else when (this) {
    FolderSortOption.NAME -> "A→Z"
    FolderSortOption.LAST_PLAYED -> "Oldest"
    FolderSortOption.DATE_MODIFIED -> "Oldest"
    FolderSortOption.EPISODE_COUNT -> "1→99"
    FolderSortOption.TOTAL_SIZE -> "MB→GB"
    FolderSortOption.UNWATCHED -> "Fewer"
}

fun FolderSortOption.localizedDescBadge(isEnglish: Boolean): String = if (!isEnglish) shortDescBadge else when (this) {
    FolderSortOption.NAME -> "Z→A"
    FolderSortOption.LAST_PLAYED -> "Recent"
    FolderSortOption.DATE_MODIFIED -> "Newest"
    FolderSortOption.EPISODE_COUNT -> "99→1"
    FolderSortOption.TOTAL_SIZE -> "GB→MB"
    FolderSortOption.UNWATCHED -> "More"
}

fun FolderSortOption.localizedActiveBadge(ascending: Boolean, isEnglish: Boolean): String =
    if (ascending) localizedAscBadge(isEnglish) else localizedDescBadge(isEnglish)

fun FolderSortOption.localizedActiveHint(ascending: Boolean, isEnglish: Boolean): String = if (!isEnglish) {
    activeHint(ascending)
} else when (this) {
    FolderSortOption.NAME -> if (ascending) "A → Z (natural season & episode order)" else "Z → A (reverse order)"
    FolderSortOption.LAST_PLAYED -> if (ascending) "Least recently opened first" else "Recently watched folders on top"
    FolderSortOption.DATE_MODIFIED -> if (ascending) "Older files first" else "Newest files first"
    FolderSortOption.EPISODE_COUNT -> if (ascending) "Fewest videos first" else "Most videos first"
    FolderSortOption.TOTAL_SIZE -> if (ascending) "Smaller folders first" else "Largest folders first"
    FolderSortOption.UNWATCHED -> if (ascending) "Fewer unwatched episodes first" else "More unwatched episodes first"
}

fun EpisodeSortOption.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    EpisodeSortOption.EPISODE_NUMBER -> "By Episode & Name"
    EpisodeSortOption.DATE_MODIFIED -> "Date Modified"
    EpisodeSortOption.FILE_SIZE -> "File Size"
    EpisodeSortOption.DURATION -> "Duration"
    EpisodeSortOption.RESOLUTION -> "Video Resolution"
    EpisodeSortOption.LAST_PLAYED -> "Recently Watched"
}

fun EpisodeSortOption.localizedShortLabel(isEnglish: Boolean): String = if (!isEnglish) shortLabel else when (this) {
    EpisodeSortOption.EPISODE_NUMBER -> "Episode"
    EpisodeSortOption.DATE_MODIFIED -> "Date"
    EpisodeSortOption.FILE_SIZE -> "Size"
    EpisodeSortOption.DURATION -> "Length"
    EpisodeSortOption.RESOLUTION -> "Quality"
    EpisodeSortOption.LAST_PLAYED -> "Watched"
}

fun EpisodeSortOption.localizedAscBadge(isEnglish: Boolean): String = if (!isEnglish) shortAscBadge else when (this) {
    EpisodeSortOption.EPISODE_NUMBER -> "1→99"
    EpisodeSortOption.DATE_MODIFIED -> "Oldest"
    EpisodeSortOption.FILE_SIZE -> "MB→GB"
    EpisodeSortOption.DURATION -> "Short"
    EpisodeSortOption.RESOLUTION -> "SD→4K"
    EpisodeSortOption.LAST_PLAYED -> "Oldest"
}

fun EpisodeSortOption.localizedDescBadge(isEnglish: Boolean): String = if (!isEnglish) shortDescBadge else when (this) {
    EpisodeSortOption.EPISODE_NUMBER -> "99→1"
    EpisodeSortOption.DATE_MODIFIED -> "Newest"
    EpisodeSortOption.FILE_SIZE -> "GB→MB"
    EpisodeSortOption.DURATION -> "Long"
    EpisodeSortOption.RESOLUTION -> "4K→SD"
    EpisodeSortOption.LAST_PLAYED -> "Recent"
}

fun EpisodeSortOption.localizedActiveBadge(ascending: Boolean, isEnglish: Boolean): String =
    if (ascending) localizedAscBadge(isEnglish) else localizedDescBadge(isEnglish)

fun EpisodeSortOption.localizedActiveHint(ascending: Boolean, isEnglish: Boolean): String = if (!isEnglish) {
    activeHint(ascending)
} else when (this) {
    EpisodeSortOption.EPISODE_NUMBER -> if (ascending) "1 → 99 & A → Z in episode order" else "99 → 1 in reverse order"
    EpisodeSortOption.DATE_MODIFIED -> if (ascending) "Oldest video files first" else "Newest video files first"
    EpisodeSortOption.FILE_SIZE -> if (ascending) "Smallest files first" else "Largest files first"
    EpisodeSortOption.DURATION -> if (ascending) "Shortest episodes first" else "Longest episodes first"
    EpisodeSortOption.RESOLUTION -> if (ascending) "From SD up to 4K / 8K" else "From 4K / 8K down to SD"
    EpisodeSortOption.LAST_PLAYED -> if (ascending) "Unwatched & older first" else "Recently played on top"
}

fun PlayerQuickAction.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    PlayerQuickAction.AUDIO -> "Audio Tracks & Sync"
    PlayerQuickAction.SUBTITLES -> "Subtitles (SRT / ASS)"
    PlayerQuickAction.EPISODES -> "Episode List"
    PlayerQuickAction.CHAPTERS -> "Embedded Chapters"
    PlayerQuickAction.SPEED -> "Playback Speed"
    PlayerQuickAction.ANIME4K -> "Anime4K Upscaling"
    PlayerQuickAction.FRAME_INTERPOLATION -> "Frame Interpolation (MEMC)"
    PlayerQuickAction.DECODER -> "Video Decoder (HW+ / SW)"
    PlayerQuickAction.SCALE -> "Aspect Ratio & Scale"
    PlayerQuickAction.AUDIO_BOOST -> "Audio Boost (DSP)"
    PlayerQuickAction.SLEEP_TIMER -> "Sleep Timer"
    PlayerQuickAction.AB_LOOP -> "A–B Segment Loop"
    PlayerQuickAction.SKIP_INTRO -> "Skip Intro (+85s)"
    PlayerQuickAction.ORIENTATION -> "Screen Orientation"
    PlayerQuickAction.PIP -> "Picture-in-Picture (PiP)"
    PlayerQuickAction.LOCK -> "Screen Lock"
    PlayerQuickAction.FILE_INFO -> "File Information"
    PlayerQuickAction.TOOLS -> "All Tools (More)"
}

fun PlayerQuickAction.localizedShortTitle(isEnglish: Boolean): String = if (!isEnglish) shortTitle else when (this) {
    PlayerQuickAction.AUDIO -> "Audio"
    PlayerQuickAction.SUBTITLES -> "Subs"
    PlayerQuickAction.EPISODES -> "Episodes"
    PlayerQuickAction.CHAPTERS -> "Chapters"
    PlayerQuickAction.SPEED -> "Speed"
    PlayerQuickAction.ANIME4K -> "Anime4K"
    PlayerQuickAction.FRAME_INTERPOLATION -> "MEMC"
    PlayerQuickAction.DECODER -> "Decoder"
    PlayerQuickAction.SCALE -> "Scale"
    PlayerQuickAction.AUDIO_BOOST -> "Boost"
    PlayerQuickAction.SLEEP_TIMER -> "Timer"
    PlayerQuickAction.AB_LOOP -> "A–B"
    PlayerQuickAction.SKIP_INTRO -> "+85s"
    PlayerQuickAction.ORIENTATION -> "Rotate"
    PlayerQuickAction.PIP -> "PiP"
    PlayerQuickAction.LOCK -> "Lock"
    PlayerQuickAction.FILE_INFO -> "Info"
    PlayerQuickAction.TOOLS -> "More"
}

fun PlayerQuickAction.localizedDescription(isEnglish: Boolean): String = if (!isEnglish) subtitle else when (this) {
    PlayerQuickAction.AUDIO -> "Switch voiceovers, external .mka, DSP boost, and ±5s audio delay"
    PlayerQuickAction.SUBTITLES -> "Select subtitle track, adjust timing delay, font size, and color"
    PlayerQuickAction.EPISODES -> "Independent modal window with all episodes of the current folder"
    PlayerQuickAction.CHAPTERS -> "Independent modal window with embedded MKV/MP4 chapters"
    PlayerQuickAction.SPEED -> "Fine playback rate control from 0.25x to 3.0x"
    PlayerQuickAction.ANIME4K -> "Real-time GLSL CNN/CNNX upscaling and Before/After split comparison"
    PlayerQuickAction.FRAME_INTERPOLATION -> "Real-time MEMC motion smoothing up to 60/120/165 Hz with LTPO"
    PlayerQuickAction.DECODER -> "Switch between HW+, Strict HW, and Software (SW) video decoding"
    PlayerQuickAction.SCALE -> "Cycle Fit, Fill/Crop, and 16:9 / 4:3 aspect ratios"
    PlayerQuickAction.AUDIO_BOOST -> "Hardware LoudnessEnhancer up to 200% for quiet tracks"
    PlayerQuickAction.SLEEP_TIMER -> "Automatically pause playback after 15–60 min or at end of episode"
    PlayerQuickAction.AB_LOOP -> "Set start point A and end point B to repeat a scene"
    PlayerQuickAction.SKIP_INTRO -> "Instant forward jump by the configured intro length"
    PlayerQuickAction.ORIENTATION -> "Cycle Auto-Video, Landscape, Portrait, or System Sensor"
    PlayerQuickAction.PIP -> "Minimize video into a floating Picture-in-Picture window"
    PlayerQuickAction.LOCK -> "Lock all player controls against accidental touches"
    PlayerQuickAction.FILE_INFO -> "Detailed technical information on codecs, resolution, audio tracks and subtitles"
    PlayerQuickAction.TOOLS -> "Open the complete player tools & end-of-episode settings sheet"
}

fun Anime4KPreset.localizedShortLabel(isEnglish: Boolean): String = when (this) {
    Anime4KPreset.OFF -> if (isEnglish) "Off" else "Выкл"
    Anime4KPreset.ANIME4K_FAST -> if (isEnglish) "Fast" else "Быстрый"
    Anime4KPreset.ANIME4K_UPSCALE_CNN -> "CNN"
    Anime4KPreset.ANIME4K_UPSCALE_CNNX -> "CNNX"
}

fun QuickBarPreset.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    QuickBarPreset.DEFAULT -> "Standard (Balanced)"
    QuickBarPreset.ANIME -> "Anime Marathon"
    QuickBarPreset.MINIMAL -> "Compact Minimal"
    QuickBarPreset.PRO -> "Full Arsenal"
}

fun QuickBarPreset.localizedSubtitle(isEnglish: Boolean): String = when (this) {
    QuickBarPreset.DEFAULT -> if (isEnglish) "Audio, Subtitles, Episodes, Chapters, Speed, Scale, MEMC, Anime4K, Boost, More" else "Аудио, Субтитры, Серии, Главы, Скорость, Масштаб, Плавность, Anime4K, Усиление, Ещё"
    QuickBarPreset.ANIME -> if (isEnglish) "Audio, Subtitles, Episodes, Chapters, +85s Intro, Speed, Scale, MEMC, Anime4K, More" else "Аудио, Субтитры, Серии, Главы, Опенинг +85с, Скорость, Масштаб, Плавность, Anime4K, Ещё"
    QuickBarPreset.MINIMAL -> if (isEnglish) "Audio, Subtitles, Episodes, Speed, More" else "Только Аудио, Субтитры, Серии, Скорость и Ещё"
    QuickBarPreset.PRO -> if (isEnglish) "All core actions including A–B Loop, Sleep Timer, Audio Boost & Skip Intro" else "Полный набор инструментов включая A–B повтор, Таймер сна, Усиление и Пропуск опенинга"
}

fun TorrentTrackerGroup.localizedLabel(isEnglish: Boolean): String = if (!isEnglish) label else when (this) {
    TorrentTrackerGroup.ALL -> "All"
    TorrentTrackerGroup.RUTRACKER -> "RuTracker"
    TorrentTrackerGroup.RUTOR -> "Rutor"
    TorrentTrackerGroup.KINOZAL -> "Kinozal"
    TorrentTrackerGroup.NNMCLUB -> "NNM-Club"
    TorrentTrackerGroup.ANIME -> "Anime"
    TorrentTrackerGroup.OTHER -> "Other"
}

fun TorrentQualityFilter.localizedLabel(isEnglish: Boolean): String = if (!isEnglish) label else when (this) {
    TorrentQualityFilter.ALL -> "All Qualities"
    TorrentQualityFilter.UHD_4K -> "4K / 2160p"
    TorrentQualityFilter.FHD_1080 -> "1080p"
    TorrentQualityFilter.HD_720 -> "720p"
    TorrentQualityFilter.HEVC_HDR -> "HEVC / HDR"
}

fun TorrentSortMode.localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
    TorrentSortMode.PRIORITY -> "Priority (RuTracker / Rutor)"
    TorrentSortMode.SEEDERS -> "By Seeders Count"
    TorrentSortMode.SIZE -> "By Torrent Size"
    TorrentSortMode.DATE -> "By Date Added"
}

fun TorrentSortMode.localizedShortLabel(isEnglish: Boolean): String = if (!isEnglish) shortLabel else when (this) {
    TorrentSortMode.PRIORITY -> "Priority"
    TorrentSortMode.SEEDERS -> "Seeds"
    TorrentSortMode.SIZE -> "Size"
    TorrentSortMode.DATE -> "Newest"
}
