package com.example.model

import android.content.pm.ActivityInfo
import androidx.compose.runtime.Immutable

enum class AppThemeMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val isDark: Boolean,
    val isPureOled: Boolean,
    val previewBgArgb: Long,
    val previewAccentArgb: Long
) {
    OLED_BLACK(
        id = "OLED_BLACK",
        title = "Чистый OLED (#000000)",
        subtitle = "Абсолютно чёрный фон с отключением субпикселей AMOLED и циановым акцентом",
        isDark = true,
        isPureOled = true,
        previewBgArgb = 0xFF000000L,
        previewAccentArgb = 0xFF00E5FFL
    ),
    SAKURA_OLED(
        id = "SAKURA_OLED",
        title = "Кибер-Сакура OLED (#000000)",
        subtitle = "Чистый OLED-чёрный фон с неоново-малиновыми акцентами в аниме-стилистике",
        isDark = true,
        isPureOled = true,
        previewBgArgb = 0xFF000000L,
        previewAccentArgb = 0xFFFF4D8DL
    ),
    OBSIDIAN_DARK(
        id = "OBSIDIAN_DARK",
        title = "Кино-Обсидиан (Тёмная)",
        subtitle = "Мягкий глубокий графитово-синий оттенок домашнего кинотеатра",
        isDark = true,
        isPureOled = false,
        previewBgArgb = 0xFF0A0D14L,
        previewAccentArgb = 0xFF00E5FFL
    ),
    DAYLIGHT_LIGHT(
        id = "DAYLIGHT_LIGHT",
        title = "Светлая (Дневная)",
        subtitle = "Высококонтрастная светлая палитра для яркого освещения",
        isDark = false,
        isPureOled = false,
        previewBgArgb = 0xFFF6F8FCL,
        previewAccentArgb = 0xFF006876L
    ),
    DYNAMIC_OLED(
        id = "DYNAMIC_OLED",
        title = "Material You + OLED (#000000)",
        subtitle = "Системные акценты обоев Android 12+ на абсолютно чёрном OLED-фоне",
        isDark = true,
        isPureOled = true,
        previewBgArgb = 0xFF000000L,
        previewAccentArgb = 0xFF80D8FFL
    );

    companion object {
        fun fromId(id: String): AppThemeMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OLED_BLACK
    }
}

enum class Anime4KPreset(
    val id: String,
    val title: String,
    val badgeLabel: String,
    val description: String,
    val passCount: Int
) {
    OFF(
        id = "Off",
        title = "Выключен (Оригинал)",
        badgeLabel = "RAW",
        description = "Прямой вывод аппаратного декодера без постобработки GPU.",
        passCount = 1
    ),
    ANIME4K_FAST(
        id = "Anime4K_Fast",
        title = "Anime4K_Fast (ThinLines + Deblur)",
        badgeLabel = "A4K FAST",
        description = "Быстрое утончение контуров и устранение мыла при минимальной нагрузке на GPU.",
        passCount = 2
    ),
    ANIME4K_UPSCALE_CNN(
        id = "Anime4K_Upscale_CNN",
        title = "Anime4K_Upscale_CNN",
        badgeLabel = "CNN x2",
        description = "Сбалансированный нейросетевой GLSL-апскейлинг: восстановление линий CReLU и подавление ореолов.",
        passCount = 3
    ),
    ANIME4K_UPSCALE_CNNX(
        id = "Anime4K_Upscale_CNNX",
        title = "Anime4K_Upscale_CNNX",
        badgeLabel = "CNNX ULTRA",
        description = "Максимальная чёткость: глубокая реконструкция градиентов Собеля, плотная свёртка CNNX и субпиксельный антиалиасинг.",
        passCount = 4
    );

    companion object {
        fun fromId(id: String): Anime4KPreset =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OFF
    }
}

enum class FrameInterpolationMode(
    val id: String,
    val title: String,
    val badgeLabel: String,
    val description: String,
    val targetFps: Int,
    val motionStrength: Float,
    val isAnimeContourProtected: Boolean
) {
    OFF(
        id = "OFF",
        title = "Выключена (Оригинал)",
        badgeLabel = "OFF",
        description = "Прямой вывод исходных кадров декодера (24/30 к/с) без генерации промежуточных фаз.",
        targetFps = 0,
        motionStrength = 0f,
        isAnimeContourProtected = false
    ),
    CINEMA(
        id = "CINEMA",
        title = "Мягкое кино (×2 / 48–60 к/с)",
        badgeLabel = "MEMC ×2",
        description = "Деликатное двукратное сглаживание панорам (48/50/60 к/с) с сохранением кинематографической пластики.",
        targetFps = 60,
        motionStrength = 0.72f,
        isAnimeContourProtected = false
    ),
    SMOOTH_60(
        id = "SMOOTH_60",
        title = "Плавность 60 к/с",
        badgeLabel = "MEMC 60",
        description = "Двунаправленная компенсация движения MEMC до стабильных 60 к/с с экономией энергии на LTPO-экранах.",
        targetFps = 60,
        motionStrength = 1.0f,
        isAnimeContourProtected = false
    ),
    SVP_MAX(
        id = "SVP_MAX",
        title = "Максимум экрана (90–165 Гц)",
        badgeLabel = "MEMC MAX",
        description = "Фазово-синхронизированная интерполяция каждого кадра под частоту дисплея 90 / 120 / 144 / 165 Гц.",
        targetFps = 165,
        motionStrength = 1.0f,
        isAnimeContourProtected = false
    ),
    ANIME(
        id = "ANIME",
        title = "Аниме 2D (Защита контуров)",
        badgeLabel = "MEMC 2D",
        description = "Режим для 2D-анимации: высокая частота обновления с защитой тонких линий, субтитров и смен сцен.",
        targetFps = 165,
        motionStrength = 0.90f,
        isAnimeContourProtected = true
    );

    val isEnabled: Boolean
        get() = this != OFF

    companion object {
        fun fromId(id: String): FrameInterpolationMode {
            if (id.equals("MEMC_MAX", ignoreCase = true)) return SVP_MAX
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OFF
        }
    }
}

enum class PlayerControlsTheme(
    val id: String,
    val title: String,
    val shortBadge: String,
    val subtitle: String
) {
    CLASSIC_OLED(
        id = "CLASSIC_OLED",
        title = "Классический OLED",
        shortBadge = "Классика",
        subtitle = "Компактные матово-стеклянные кнопки с акцентной клавишей Пуск и тонкая полоса"
    ),
    GLASS_CAPSULE(
        id = "GLASS_CAPSULE",
        title = "Стеклянная капсула",
        shortBadge = "Стекло",
        subtitle = "Полупрозрачные матовые пилюли с бликом грани и объёмная капсульная полоса"
    ),
    CYBER_NEON(
        id = "CYBER_NEON",
        title = "Кибер-Неон",
        shortBadge = "Неон",
        subtitle = "Матовое стекло с неоновым свечением акцента и лазерная шкала с ореолом"
    ),
    MINIMAL_RING(
        id = "MINIMAL_RING",
        title = "Тонкое кольцо",
        shortBadge = "Кольцо",
        subtitle = "Компактные воздушные кольца со стеклянной дымкой и минималистичная нить"
    ),
    CINEMA_TACTILE(
        id = "CINEMA_TACTILE",
        title = "Студийный пульт",
        shortBadge = "Пульт",
        subtitle = "Компактный матово-стеклянный пульт с акцентной подсветкой и монтажной полосой"
    );

    companion object {
        fun fromId(id: String): PlayerControlsTheme =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CLASSIC_OLED
    }
}

enum class DecoderMode(
    val id: String,
    val title: String,
    val shortBadge: String,
    val description: String
) {
    HW_PLUS(
        id = "HW_PLUS",
        title = "Аппаратное декодирование (HW+)",
        shortBadge = "HW+",
        description = "Приоритет аппаратных чипов MediaCodec (HEVC/AVC/AV1/VP9) с авто-резервом при нестандартных профилях."
    ),
    HW_STRICT(
        id = "HW_STRICT",
        title = "Строго аппаратное (HW)",
        shortBadge = "HW",
        description = "Использует исключительно аппаратные декодеры GPU/SoC для максимальной энергоэффективности."
    ),
    SOFTWARE_PREFER(
        id = "SW_PREFER",
        title = "Программный декодер (SW)",
        shortBadge = "SW",
        description = "Приоритет программных кодеков CPU для редких и повреждённых видеопотоков."
    );

    companion object {
        fun fromId(id: String): DecoderMode =
            entries.firstOrNull { it.id == id } ?: HW_PLUS
    }
}

enum class VideoScaleMode(
    val id: String,
    val title: String,
    val shortBadge: String
) {
    FIT("FIT", "По размеру экрана", "Вписать"),
    ZOOM("ZOOM", "Заполнение (без полей)", "Заполнение"),
    STRETCH("STRETCH", "Растянуть", "Растянуть");

    fun next(): VideoScaleMode = when (this) {
        FIT -> ZOOM
        ZOOM -> STRETCH
        STRETCH -> FIT
    }

    companion object {
        fun fromId(id: String): VideoScaleMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: FIT
    }
}

enum class SubtitleFormat(val label: String) {
    SRT("SRT"),
    ASS("ASS"),
    VTT("VTT")
}

enum class SubtitleColorPreset(
    val id: String,
    val label: String,
    val argb: Long
) {
    WHITE("WHITE", "Белый", 0xFFFFFFFFL),
    YELLOW("YELLOW", "Кино-жёлтый", 0xFFFFEA00L),
    CYAN("CYAN", "Неоновый циан", 0xFF00E5FFL),
    MINT("MINT", "Мятный", 0xFF69F0AEL),
    AMBER("AMBER", "Янтарный", 0xFFFFAB40L),
    SAKURA("SAKURA", "Сакура", 0xFFFF80ABL);

    companion object {
        fun fromArgb(argb: Long): SubtitleColorPreset =
            entries.firstOrNull { it.argb == argb } ?: WHITE
    }
}

enum class FolderSortOption(
    val id: String,
    val title: String,
    val shortLabel: String,
    val ascHint: String,
    val descHint: String,
    val shortAscBadge: String,
    val shortDescBadge: String,
    val defaultAscending: Boolean
) {
    NAME(
        id = "NAME",
        title = "По названию",
        shortLabel = "Имя",
        ascHint = "А → Я (с учётом номеров сезонов)",
        descHint = "Я → А (в обратном порядке)",
        shortAscBadge = "А→Я",
        shortDescBadge = "Я→А",
        defaultAscending = true
    ),
    LAST_PLAYED(
        id = "LAST_PLAYED",
        title = "По недавнему просмотру",
        shortLabel = "Недавние",
        ascHint = "Давно открытые сверху",
        descHint = "Недавно просматриваемые сверху",
        shortAscBadge = "Старые",
        shortDescBadge = "Свежие",
        defaultAscending = false
    ),
    DATE_MODIFIED(
        id = "DATE_MODIFIED",
        title = "По дате изменения",
        shortLabel = "Дата",
        ascHint = "Сначала старые файлы",
        descHint = "Сначала новые файлы",
        shortAscBadge = "Старые",
        shortDescBadge = "Новые",
        defaultAscending = false
    ),
    EPISODE_COUNT(
        id = "EPISODE_COUNT",
        title = "По количеству серий",
        shortLabel = "Серии",
        ascHint = "По возрастанию числа видео",
        descHint = "По убыванию числа видео",
        shortAscBadge = "1→99",
        shortDescBadge = "99→1",
        defaultAscending = false
    ),
    TOTAL_SIZE(
        id = "TOTAL_SIZE",
        title = "По размеру папки",
        shortLabel = "Размер",
        ascHint = "Сначала компактные",
        descHint = "Сначала крупные",
        shortAscBadge = "МБ→ГБ",
        shortDescBadge = "ГБ→МБ",
        defaultAscending = false
    ),
    UNWATCHED(
        id = "UNWATCHED",
        title = "По непросмотренным сериям",
        shortLabel = "К просмотру",
        ascHint = "Меньше непросмотренных сверху",
        descHint = "Больше непросмотренных сверху",
        shortAscBadge = "Меньше",
        shortDescBadge = "Больше",
        defaultAscending = false
    );

    fun activeBadge(ascending: Boolean): String =
        if (ascending) shortAscBadge else shortDescBadge

    fun activeHint(ascending: Boolean): String =
        if (ascending) ascHint else descHint

    companion object {
        fun fromId(id: String): FolderSortOption =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NAME
    }
}

enum class EpisodeSortOption(
    val id: String,
    val title: String,
    val shortLabel: String,
    val ascHint: String,
    val descHint: String,
    val shortAscBadge: String,
    val shortDescBadge: String,
    val defaultAscending: Boolean
) {
    EPISODE_NUMBER(
        id = "EPISODE_NUMBER",
        title = "По серии и имени",
        shortLabel = "Серия",
        ascHint = "1 → 99 и А → Я по порядку серий",
        descHint = "99 → 1 в обратном порядке",
        shortAscBadge = "1→99",
        shortDescBadge = "99→1",
        defaultAscending = true
    ),
    DATE_MODIFIED(
        id = "DATE_MODIFIED",
        title = "По дате изменения",
        shortLabel = "Дата",
        ascHint = "Сначала старые видеофайлы",
        descHint = "Сначала новые видеофайлы",
        shortAscBadge = "Старые",
        shortDescBadge = "Новые",
        defaultAscending = false
    ),
    FILE_SIZE(
        id = "FILE_SIZE",
        title = "По размеру файла",
        shortLabel = "Размер",
        ascHint = "Сначала лёгкие файлы",
        descHint = "Сначала крупные файлы",
        shortAscBadge = "МБ→ГБ",
        shortDescBadge = "ГБ→МБ",
        defaultAscending = false
    ),
    DURATION(
        id = "DURATION",
        title = "По длительности",
        shortLabel = "Длина",
        ascHint = "Сначала короткие серии",
        descHint = "Сначала длинные серии",
        shortAscBadge = "Короткие",
        shortDescBadge = "Длинные",
        defaultAscending = false
    ),
    RESOLUTION(
        id = "RESOLUTION",
        title = "По разрешению кадра",
        shortLabel = "Качество",
        ascHint = "От SD к 4K / 8K",
        descHint = "От 4K / 8K к SD",
        shortAscBadge = "SD→4K",
        shortDescBadge = "4K→SD",
        defaultAscending = false
    ),
    LAST_PLAYED(
        id = "LAST_PLAYED",
        title = "По недавнему просмотру",
        shortLabel = "Просмотр",
        ascHint = "Непросмотренные и старые сверху",
        descHint = "Недавно открытые сверху",
        shortAscBadge = "Старые",
        shortDescBadge = "Свежие",
        defaultAscending = false
    );

    fun activeBadge(ascending: Boolean): String =
        if (ascending) shortAscBadge else shortDescBadge

    fun activeHint(ascending: Boolean): String =
        if (ascending) ascHint else descHint

    companion object {
        fun fromId(id: String): EpisodeSortOption =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: EPISODE_NUMBER
    }
}

enum class PlayerOrientationMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val shortBadge: String,
    val activityOrientation: Int
) {
    VIDEO_ASPECT(
        id = "VIDEO_ASPECT",
        title = "По формату видео",
        subtitle = "Горизонтальные видео — в альбомном режиме, вертикальные — в портретном",
        shortBadge = "По формату",
        activityOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    ),
    LANDSCAPE(
        id = "LANDSCAPE",
        title = "Альбомная (16:9)",
        subtitle = "Всегда поворачивать экран горизонтально с поддержкой переворота на 180°",
        shortBadge = "Альбом",
        activityOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    ),
    PORTRAIT(
        id = "PORTRAIT",
        title = "Портретная (вертикально)",
        subtitle = "Всегда удерживать вертикальную ориентацию экрана",
        shortBadge = "Портрет",
        activityOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    ),
    AUTO(
        id = "AUTO",
        title = "По датчику (системная)",
        subtitle = "Следовать системной настройке автоповорота Android",
        shortBadge = "Датчик",
        activityOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    );

    fun next(): PlayerOrientationMode = when (this) {
        VIDEO_ASPECT -> LANDSCAPE
        LANDSCAPE -> PORTRAIT
        PORTRAIT -> AUTO
        AUTO -> VIDEO_ASPECT
    }

    fun resolveActivityOrientation(videoWidth: Int, videoHeight: Int): Int {
        if (this != VIDEO_ASPECT) return activityOrientation
        return if (videoWidth > 0 && videoHeight > 0 && videoHeight > videoWidth) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    companion object {
        fun fromId(id: String): PlayerOrientationMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: VIDEO_ASPECT
    }
}

enum class PlaybackEndMode(
    val id: String,
    val title: String,
    val description: String
) {
    AUTO_NEXT(
        id = "AUTO_NEXT",
        title = "Автопереход к следующей серии",
        description = "Плавный запуск следующего эпизода в папке без остановки"
    ),
    PAUSE_AT_END(
        id = "PAUSE_AT_END",
        title = "Пауза в конце серии",
        description = "Останавливать воспроизведение после завершения каждого эпизода"
    ),
    REPEAT_ONE(
        id = "REPEAT_ONE",
        title = "Повтор текущей серии",
        description = "Бесконечно зацикливать активное видео"
    );

    companion object {
        fun fromId(id: String): PlaybackEndMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AUTO_NEXT
    }
}

enum class SleepTimerPreset(
    val id: String,
    val label: String,
    val durationMinutes: Int,
    val stopAtEndOfEpisode: Boolean = false
) {
    OFF("OFF", "Выкл", 0),
    MIN_15("MIN_15", "15 мин", 15),
    MIN_30("MIN_30", "30 мин", 30),
    MIN_45("MIN_45", "45 мин", 45),
    MIN_60("MIN_60", "60 мин", 60),
    END_OF_EPISODE("END_OF_EPISODE", "До конца серии", 0, stopAtEndOfEpisode = true)
}

enum class SettingsCategory(
    val id: String,
    val title: String
) {
    APPEARANCE("APPEARANCE", "Оформление и интерфейс"),
    PLAYBACK("PLAYBACK", "Воспроизведение и таймлайн"),
    GESTURES("GESTURES", "Жесты и управление"),
    VIDEO_ANIME4K("VIDEO_ANIME4K", "Видео, LTPO герцовка, интерполяция и Anime4K"),
    AUDIO("AUDIO", "Аудио и озвучка"),
    SUBTITLES("SUBTITLES", "Субтитры (SRT / ASS)"),
    TORRSERVER("TORRSERVER", "TorrServer и торренты"),
    STORAGE("STORAGE", "Хранилище и исключения")
}

@Immutable
data class VideoChapter(
    val index: Int,
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long
) {
    val durationMs: Long
        get() = (endTimeMs - startTimeMs).coerceAtLeast(0L)
}

@Immutable
data class PlayerSettings(
    // Appearance & Interface
    val themeMode: AppThemeMode = AppThemeMode.OLED_BLACK,
    val playerControlsTheme: PlayerControlsTheme = PlayerControlsTheme.CLASSIC_OLED,
    val compactUiMode: Boolean = true,
    val showEpisodeThumbnails: Boolean = true,
    val showEpisodesButtonInHud: Boolean = true,
    val showHudBadges: Boolean = true,
    val folderSortOption: FolderSortOption = FolderSortOption.NAME,
    val folderSortAscending: Boolean = true,
    val episodeSortOption: EpisodeSortOption = EpisodeSortOption.EPISODE_NUMBER,
    val episodeSortAscending: Boolean = true,
    val quickActions: List<PlayerQuickAction> = QuickActionsSerializer.DEFAULT_ACTIONS,

    // Playback, Chapters & Timeline
    val resumePlayback: Boolean = true,
    val playbackEndMode: PlaybackEndMode = PlaybackEndMode.AUTO_NEXT,
    val enableEmbeddedChapters: Boolean = true,
    val showChapterMarkersOnTimeline: Boolean = true,
    val seekStepSeconds: Int = 10,
    val defaultPlaybackSpeed: Float = 1.0f,
    val showSkipIntroButton: Boolean = true,
    val skipIntroSeconds: Int = 85,
    val autoEnterPip: Boolean = true,
    val backgroundAudioPlay: Boolean = false,

    // Gestures & Orientation
    val gesturesBrightnessVolume: Boolean = true,
    val gestureHorizontalSeek: Boolean = true,
    val gestureDoubleTapSeek: Boolean = true,
    val gestureLongPress2x: Boolean = true,
    val longPressBoostSpeed: Float = 2.0f,
    val defaultPlayerOrientation: PlayerOrientationMode = PlayerOrientationMode.VIDEO_ASPECT,

    // Video, LTPO Refresh Rate, MEMC & Anime4K
    val decoderMode: DecoderMode = DecoderMode.HW_PLUS,
    val defaultScaleMode: VideoScaleMode = VideoScaleMode.FIT,
    val highRefreshRateEnabled: Boolean = true,
    val ltpoAdaptiveRefreshEnabled: Boolean = true,
    val frameInterpolationEnabled: Boolean = false,
    val frameInterpolationMode: FrameInterpolationMode = FrameInterpolationMode.OFF,
    val anime4kEnabled: Boolean = false,
    val anime4kPreset: Anime4KPreset = Anime4KPreset.OFF,
    val anime4kStrength: Float = 1.0f,

    // Audio & Voiceover
    val autoBindExternalAudio: Boolean = true,
    val preferExternalAudioOnStart: Boolean = true,
    val defaultAudioBoostPercent: Int = 100,

    // Subtitles
    val autoBindExternalSubtitles: Boolean = true,
    val subtitleFontSizeSp: Float = 21f,
    val subtitleColorArgb: Long = SubtitleColorPreset.WHITE.argb,
    val subtitleBackgroundEnabled: Boolean = true,

    // TorrServer & Torrents
    val torrServerUrl: String = "http://127.0.0.1:8090",
    val torrServerUsername: String = "",
    val torrServerPassword: String = "",
    val torrServerAutoPlaySingle: Boolean = true,
    val torrServerSaveToDb: Boolean = true,
    val torrentIndexerUrl: String = "https://jacred.xyz",
    val torrentIndexerApiKey: String = "",
    val enableDirectRutorSearch: Boolean = true,
    val enableAnimeTrackerSearch: Boolean = true,
    val enableTrackerDoh: Boolean = true,

    // Storage & Exclusions
    val ignoreNomediaFolders: Boolean = true,
    val excludeCameraAndScreenshots: Boolean = false,
    val minVideoSizeMb: Int = 0
) {
    val effectiveAnime4KPreset: Anime4KPreset
        get() = if (anime4kEnabled && anime4kPreset != Anime4KPreset.OFF) {
            anime4kPreset
        } else {
            Anime4KPreset.OFF
        }

    val effectiveFrameInterpolationMode: FrameInterpolationMode
        get() = if (frameInterpolationEnabled && frameInterpolationMode != FrameInterpolationMode.OFF) {
            frameInterpolationMode
        } else {
            FrameInterpolationMode.OFF
        }
}

@Immutable
data class ExternalAudioTrack(
    val uri: String,
    val displayName: String,
    val sourceFolderName: String,
    val extension: String,
    val isAutoMatched: Boolean = true
) {
    val formattedLabel: String
        get() = if (sourceFolderName.isNotBlank()) {
            "$sourceFolderName • $displayName"
        } else {
            displayName
        }
}

@Immutable
data class ExternalSubtitleTrack(
    val uri: String,
    val displayName: String,
    val sourceFolderName: String,
    val format: SubtitleFormat,
    val isAutoMatched: Boolean = true
) {
    val formattedLabel: String
        get() = buildString {
            append("[${format.label}] ")
            if (sourceFolderName.isNotBlank()) {
                append(sourceFolderName)
                append(" • ")
            }
            append(displayName)
        }
}

object VideoResolutionFormatter {
    private val EXPLICIT_DIM_REGEX = Regex("""(?i)\b(\d{3,4})\s*[x×]\s*(\d{3,4})\b""")
    private val RESOLUTION_TAG_REGEX = Regex(
        """(?i)(?:^|[\s._\-\[\(\/])(4320p|8k|2160p|4k|uhd|1440p|2k|qhd|1080p|1080i|fhd|720p|576p|480p|360p)(?=$|[\s._\-\]\)\/])"""
    )

    fun badgeFromDimensions(width: Int, height: Int): String? {
        if (width <= 0 || height <= 0) return null
        val longSide = maxOf(width, height)
        val shortSide = minOf(width, height)
        return when {
            longSide >= 7000 || shortSide >= 3900 -> "8K"
            longSide >= 3500 || shortSide >= 1900 -> "4K"
            longSide >= 2400 || shortSide >= 1300 -> "1440p"
            longSide >= 1800 || shortSide >= 960 -> "1080p"
            longSide >= 1200 || shortSide >= 660 -> "720p"
            shortSide >= 540 -> "576p"
            shortSide >= 440 -> "480p"
            shortSide >= 320 -> "360p"
            else -> "${width}×${height}"
        }
    }

    fun parseTagFromFileName(fileName: String): String? {
        if (fileName.isBlank()) return null
        EXPLICIT_DIM_REGEX.find(fileName)?.let { match ->
            val w = match.groupValues[1].toIntOrNull() ?: 0
            val h = match.groupValues[2].toIntOrNull() ?: 0
            if (w in 240..16384 && h in 180..16384) {
                return badgeFromDimensions(w, h)
            }
        }
        val rawTag = RESOLUTION_TAG_REGEX.find(fileName)?.groupValues?.getOrNull(1)?.lowercase() ?: return null
        return when (rawTag) {
            "8k", "4320p" -> "8K"
            "4k", "2160p", "uhd" -> "4K"
            "1440p", "2k", "qhd" -> "1440p"
            "1080p", "1080i", "fhd" -> "1080p"
            "720p" -> "720p"
            "576p" -> "576p"
            "480p" -> "480p"
            "360p" -> "360p"
            else -> null
        }
    }

    fun formatBadge(width: Int, height: Int, fileName: String): String? =
        badgeFromDimensions(width, height) ?: parseTagFromFileName(fileName)

    fun estimatePixels(width: Int, height: Int, fileName: String): Long {
        if (width > 0 && height > 0) return width.toLong() * height.toLong()
        return when (parseTagFromFileName(fileName)) {
            "8K" -> 7680L * 4320L
            "4K" -> 3840L * 2160L
            "1440p" -> 2560L * 1440L
            "1080p" -> 1920L * 1080L
            "720p" -> 1280L * 720L
            "576p" -> 1024L * 576L
            "480p" -> 854L * 480L
            "360p" -> 640L * 360L
            else -> 0L
        }
    }
}

@Immutable
data class EpisodeItem(
    val uri: String,
    val fileName: String,
    val cleanTitle: String,
    val episodeNumber: Int?,
    val seasonNumber: Int?,
    val folderId: String,
    val folderName: String,
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val autoAudioTracks: List<ExternalAudioTrack> = emptyList(),
    val autoSubtitleTracks: List<ExternalSubtitleTrack> = emptyList(),
    val resumePositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayedAt: Long = 0L
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) {
            (resumePositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val episodeBadge: String?
        get() = episodeNumber?.let { num ->
            val epStr = num.toString().padStart(2, '0')
            if (seasonNumber != null) "S${seasonNumber.toString().padStart(2, '0')}E$epStr" else "EP $epStr"
        }

    val resolutionBadge: String?
        get() = VideoResolutionFormatter.formatBadge(videoWidth, videoHeight, fileName)

    val dimensionsLabel: String?
        get() = if (videoWidth > 0 && videoHeight > 0) "${videoWidth}×${videoHeight}" else null

    val effectivePixelCount: Long
        get() = VideoResolutionFormatter.estimatePixels(videoWidth, videoHeight, fileName)
}

@Immutable
data class VideoFolderItem(
    val id: String,
    val name: String,
    val displayPath: String,
    val rootUri: String,
    val episodes: List<EpisodeItem>,
    val totalAutoAudioCount: Int,
    val totalAutoSubCount: Int,
    val watchedCount: Int,
    val totalSizeBytes: Long = episodes.sumOf { it.sizeBytes },
    val lastModified: Long = episodes.maxOfOrNull { it.lastModified } ?: 0L,
    val lastPlayedAt: Long = 0L,
    val isHidden: Boolean = false
) {
    val unwatchedCount: Int
        get() = (episodes.size - watchedCount).coerceAtLeast(0)
}
