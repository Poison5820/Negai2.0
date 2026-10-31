package com.example.model

/**
 * Customizable quick-action buttons displayed in the video player's bottom HUD toolbar.
 * Users can freely add, remove, and reorder these buttons.
 */
enum class PlayerQuickAction(
    val id: String,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val isInstantAction: Boolean = false
) {
    AUDIO(
        id = "AUDIO",
        title = "Аудиодорожки и синхронизация",
        shortTitle = "Аудио",
        subtitle = "Переключение озвучек, задержка ±5 с, ресинхронизация и внешние .mka / .mp3"
    ),
    SUBTITLES(
        id = "SUBTITLES",
        title = "Субтитры (SRT / ASS)",
        shortTitle = "Субтитры",
        subtitle = "Выбор субтитров, настройка задержки, размера шрифта и цвета"
    ),
    EPISODES(
        id = "EPISODES",
        title = "Список серий",
        shortTitle = "Серии",
        subtitle = "Быстрый переход между эпизодами текущей папки или торрента"
    ),
    CHAPTERS(
        id = "CHAPTERS",
        title = "Встроенные главы",
        shortTitle = "Главы",
        subtitle = "Оглавление MKV / MP4: быстрый переход к опенингу, частям и эндингу"
    ),
    SPEED(
        id = "SPEED",
        title = "Скорость воспроизведения",
        shortTitle = "Скорость",
        subtitle = "Плавная регулировка 0.25x–3.0x с шагом 0.05x и быстрые пресеты"
    ),
    SCALE(
        id = "SCALE",
        title = "Масштаб кадра",
        shortTitle = "Масштаб",
        subtitle = "Смена кадрирования в одно касание: Вписать → Заполнение → Растянуть",
        isInstantAction = true
    ),
    FRAME_INTERPOLATION(
        id = "FRAME_INTERPOLATION",
        title = "Интерполяция кадров (MEMC)",
        shortTitle = "Плавность",
        subtitle = "Сглаживание движений MEMC до 60 / 90 / 120 / 165 к/с с поддержкой LTPO"
    ),
    ANIME4K(
        id = "ANIME4K",
        title = "Апскейлинг Anime4K",
        shortTitle = "Anime4K",
        subtitle = "Шейдерное улучшение контуров CNN / CNNX и сравнение До/После"
    ),
    AUDIO_BOOST(
        id = "AUDIO_BOOST",
        title = "Усиление звука (DSP)",
        shortTitle = "Усиление",
        subtitle = "Аппаратное повышение громкости тихих дорожек до 200% (+12 дБ)"
    ),
    SKIP_INTRO(
        id = "SKIP_INTRO",
        title = "Пропуск опенинга (+85с)",
        shortTitle = "+85с",
        subtitle = "Мгновенная перемотка заставки вперёд на заданное число секунд",
        isInstantAction = true
    ),
    SLEEP_TIMER(
        id = "SLEEP_TIMER",
        title = "Таймер сна",
        shortTitle = "Сон",
        subtitle = "Автоматическая пауза через 15–60 минут или в конце серии"
    ),
    AB_LOOP(
        id = "AB_LOOP",
        title = "Повтор фрагмента A–B",
        shortTitle = "A–B",
        subtitle = "Быстрая установка точек A и B одним нажатием для зацикливания",
        isInstantAction = true
    ),
    ORIENTATION(
        id = "ORIENTATION",
        title = "Ориентация экрана",
        shortTitle = "Поворот",
        subtitle = "Быстрое переключение: По формату → Альбом → Портрет → Датчик",
        isInstantAction = true
    ),
    LOCK(
        id = "LOCK",
        title = "Блокировка экрана",
        shortTitle = "Замок",
        subtitle = "Защита от случайных касаний и свайпов во время просмотра",
        isInstantAction = true
    ),
    PIP(
        id = "PIP",
        title = "Картинка в картинке (PiP)",
        shortTitle = "PiP",
        subtitle = "Свернуть плеер в компактное окно поверх других приложений",
        isInstantAction = true
    ),
    DECODER(
        id = "DECODER",
        title = "Декодер видео (HW+ / SW)",
        shortTitle = "Декодер",
        subtitle = "Выбор аппаратного MediaCodec или программного декодирования (также в верхней панели)"
    ),
    TOOLS(
        id = "TOOLS",
        title = "Все инструменты («Ещё»)",
        shortTitle = "Ещё",
        subtitle = "Сводное меню дополнительных функций и режимов завершения серии"
    );

    companion object {
        val defaultActions: List<PlayerQuickAction>
            get() = QuickBarPreset.DEFAULT.actions

        fun fromIdOrNull(id: String): PlayerQuickAction? =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }
    }
}

enum class QuickBarPreset(
    val id: String,
    val title: String,
    val actions: List<PlayerQuickAction>
) {
    DEFAULT(
        id = "DEFAULT",
        title = "По полезности (Стандарт)",
        actions = listOf(
            PlayerQuickAction.AUDIO,
            PlayerQuickAction.SUBTITLES,
            PlayerQuickAction.EPISODES,
            PlayerQuickAction.CHAPTERS,
            PlayerQuickAction.SPEED,
            PlayerQuickAction.SCALE,
            PlayerQuickAction.FRAME_INTERPOLATION,
            PlayerQuickAction.ANIME4K,
            PlayerQuickAction.AUDIO_BOOST,
            PlayerQuickAction.TOOLS
        )
    ),
    ANIME(
        id = "ANIME",
        title = "Аниме-марафон",
        actions = listOf(
            PlayerQuickAction.AUDIO,
            PlayerQuickAction.SUBTITLES,
            PlayerQuickAction.EPISODES,
            PlayerQuickAction.CHAPTERS,
            PlayerQuickAction.SKIP_INTRO,
            PlayerQuickAction.SPEED,
            PlayerQuickAction.SCALE,
            PlayerQuickAction.FRAME_INTERPOLATION,
            PlayerQuickAction.ANIME4K,
            PlayerQuickAction.TOOLS
        )
    ),
    MINIMAL(
        id = "MINIMAL",
        title = "Компактный",
        actions = listOf(
            PlayerQuickAction.AUDIO,
            PlayerQuickAction.SUBTITLES,
            PlayerQuickAction.EPISODES,
            PlayerQuickAction.SPEED,
            PlayerQuickAction.TOOLS
        )
    ),
    PRO(
        id = "PRO",
        title = "Полный набор",
        actions = listOf(
            PlayerQuickAction.AUDIO,
            PlayerQuickAction.SUBTITLES,
            PlayerQuickAction.EPISODES,
            PlayerQuickAction.CHAPTERS,
            PlayerQuickAction.SPEED,
            PlayerQuickAction.SCALE,
            PlayerQuickAction.FRAME_INTERPOLATION,
            PlayerQuickAction.ANIME4K,
            PlayerQuickAction.AUDIO_BOOST,
            PlayerQuickAction.SKIP_INTRO,
            PlayerQuickAction.AB_LOOP,
            PlayerQuickAction.SLEEP_TIMER,
            PlayerQuickAction.TOOLS
        )
    )
}

object QuickActionsSerializer {

    private const val EMPTY_SENTINEL = "NONE"

    val DEFAULT_ACTIONS: List<PlayerQuickAction> = QuickBarPreset.DEFAULT.actions

    fun parse(raw: String?): List<PlayerQuickAction> {
        if (raw == null || raw.isBlank()) return DEFAULT_ACTIONS
        if (raw.trim().equals(EMPTY_SENTINEL, ignoreCase = true)) return emptyList()

        val parsed = raw
            .split(',')
            .mapNotNull { token -> PlayerQuickAction.fromIdOrNull(token) }
            .distinct()

        return parsed.ifEmpty { DEFAULT_ACTIONS }
    }

    fun serialize(actions: List<PlayerQuickAction>): String {
        val unique = actions.distinct()
        if (unique.isEmpty()) return EMPTY_SENTINEL
        return unique.joinToString(",") { it.id }
    }

    fun moveItem(
        actions: List<PlayerQuickAction>,
        fromIndex: Int,
        toIndex: Int
    ): List<PlayerQuickAction> {
        if (actions.size <= 1) return actions
        if (fromIndex !in actions.indices || toIndex !in actions.indices || fromIndex == toIndex) {
            return actions
        }
        val mutable = actions.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        return mutable
    }

    fun moveUp(actions: List<PlayerQuickAction>, index: Int): List<PlayerQuickAction> =
        if (index > 0 && index in actions.indices) moveItem(actions, index, index - 1) else actions

    fun moveDown(actions: List<PlayerQuickAction>, index: Int): List<PlayerQuickAction> =
        if (index >= 0 && index < actions.lastIndex) moveItem(actions, index, index + 1) else actions

    fun addAction(
        actions: List<PlayerQuickAction>,
        action: PlayerQuickAction
    ): List<PlayerQuickAction> {
        if (action in actions) return actions
        return actions + action
    }

    fun removeAction(
        actions: List<PlayerQuickAction>,
        action: PlayerQuickAction
    ): List<PlayerQuickAction> = actions.filterNot { it == action }

    fun availableToAdd(activeActions: List<PlayerQuickAction>): List<PlayerQuickAction> {
        val activeSet = activeActions.toSet()
        return PlayerQuickAction.entries.filterNot { it in activeSet }
    }
}
