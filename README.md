# negai — Hardware 4K / HDR Video & Anime Player for Android

[![Version](https://img.shields.io/badge/version-2.2.0-00E5FF?style=flat-square)](#)
[![Platform](https://img.shields.io/badge/platform-Android_8.0%2B_(API_26%2B)-3DDC84?style=flat-square&logo=android)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x_%7C_Jetpack_Compose-7F52FF?style=flat-square&logo=kotlin)](#)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer_%2B_OpenGL_ES-FF6D00?style=flat-square)](#)
<br>

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/Poison5820/Negai2.0">
  <img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="75">
</a>

<br>

**Languages / Языки:** [🇷🇺 Русский](#-русский) • [🇬🇧 English](#-english)

---

## 🇷🇺 Русский

**negai** — производительный локальный и сетевой видеоплеер для Android с поддержкой аппаратного декодирования 4K / 10-bit HEVC / AV1, шейдерного апскейлинга **Anime4K**, адаптивного управления частотой экрана **LTPO (24–120 Гц)** и **интерполяции кадров (MEMC)**, автоматической синхронизации задержки звука для **Bluetooth-наушников**, внешних звуковых дорожек и субтитров, а также прямой интеграцией с **TorrServer**.

### ✨ Основные возможности

#### 🎬 Видео, экран и рендеринг
- **Аппаратное декодирование (HW+ / HW / SW)**: поддержка контейнеров `.mkv`, `.mp4`, `.webm`, `.avi`, `.ts`, `.m2ts` и кодеков H.264, HEVC (H.265 10-bit HDR10 / HLG), VP9 и AV1 на базе AndroidX Media3 (ExoPlayer).
- **Anime4K (OpenGL ES 2.0)**: реалтайм шейдерная реконструкция контуров и повышение чёткости рисовки с режимами **Mode A (Быстрый)**, **Mode B (Сбалансированный)** и **Mode C (Высокое качество)**, плавной регулировкой силы эффекта и интерактивной шторкой сравнения **До / После** прямо поверх кадра. По умолчанию выключен.
- **Адаптивная частота экрана LTPO & 60/90/120 Гц**: автоматический подбор оптимальной частоты обновления дисплея под исходную частоту видео (`23.976` / `24` / `25` / `30` / `60 fps`) для устранения телесинового джаддера (3:2 pulldown).
- **Интерполяция кадров (MEMC)**: алгоритмы сглаживания движения **Плавный 60 fps**, **Чёткий (Аниме/24p)** и **Максимум 120 Гц** с компенсацией строба на панорамах. По умолчанию выключена.

#### 🎧 Аудио, внешние озвучки и Bluetooth-синхронизация
- **Автоподхват внешних аудиодорожек**: автоматическое обнаружение и синхронное воспроизведение внешних файлов озвучки (`.mka`, `.aac`, `.ac3`, `.eac3`, `.dts`, `.flac`, `.ogg`, `.opus`, `.mp3`, `.wav`) из папок `Audio`, `Rus`, `Sound`, `Dub`, `Озвучка` по номеру серии.
- **Автоматическая компенсация Bluetooth-задержки**: отслеживание подключения и переключения Bluetooth-устройств в реальном времени (`AudioDeviceCallback`) для профилей **Bluetooth A2DP**, **Bluetooth LE Audio (BLE)** и **Hearing Aid** с применением компенсирующего сдвига `MediaClock` без обрезки и потери PCM-семплов, а также поддержкой ручной калибровки базового отступа (`0 … -500 мс`).
- **Ручная синхронизация и усиление**: точная подстройка задержки звука **±5.0 с** (шаги `±50 мс` / `±250 мс`), мгновенная кнопка **«Ресинхр. поток»** и программное усиление громкости **Audio Boost до 200%** с защитой от клиппинга (`tanh` soft-limiter).

#### 💬 Субтитры и навигация по сериям и главам
- **Субтитры SRT / ASS / SSA / VTT**: автоматическое сопоставление внешних субтитров с видео по номеру эпизода, настройка размера шрифта (`14–34 sp`), цвета текста, полупрозрачной подложки и сдвига тайминга (`±500 мс`).
- **Раздельные окна «Серии» и «Главы»**: независимый переключатель эпизодов текущего сезона и отдельное окно встроенных глав контейнера (`Chapters` — OP / ED / Сцены) с маркерами на таймлайне.
- **Инструменты просмотра**: повтор фрагмента **A–B**, таймер сна, быстрый пропуск опенинга (**+85 с** или настраиваемый шаг), запоминание позиции просмотра и режим **Картинка в картинке (PiP)**.

#### 🌐 TorrServer и торрент-стриминг
- **Интеграция с TorrServer**: прямое воспроизведение `.torrent` и `magnet:`-ссылок без ожидания полной загрузки, выбор папок с озвучками внутри многосерийных раздач и управление кэшем сервера.
- **Поиск по трекерам и Torznab / Jackett / Prowlarr**: встроенный поиск раздач с фильтрацией по качеству (`4K`, `1080p`, `HEVC`) и поддержкой DNS-over-HTTPS (DoH).

#### 🎨 Интерфейс и локализация
- **Двуязычный интерфейс**: переключение языка **Системный / Русский / English** в реальном времени без перезапуска приложения.
- **6 OLED-тем приложения и 6 тем элементов плеера**: включая компактный дизайн центральных кнопок с эффектом матового стекла и настраиваемую панель быстрых действий.

### 🛠 Сборка и проверка

```bash
# Сборка отладочного APK
gradle assembleDebug

# Запуск модульных и Robolectric-тестов
gradle :app:testDebugUnitTest
```

> Подробный список изменений каждого выпуска автоматически формируется в разделе **GitHub Releases** при публикации новой версии.

---

## 🇬🇧 English

**negai** is a high-performance local and network video player for Android featuring hardware-accelerated 4K / 10-bit HEVC / AV1 decoding, real-time **Anime4K** OpenGL ES shader upscaling, **LTPO adaptive display refresh rate (24–120 Hz)**, **Frame Interpolation (MEMC)**, automatic **Bluetooth audio latency compensation**, external audio & subtitle auto-binding, and native **TorrServer** streaming.

### ✨ Key Features

#### 🎬 Video, Display & Rendering
- **Hardware Decoding (HW+ / HW / SW)**: Plays `.mkv`, `.mp4`, `.webm`, `.avi`, `.ts`, `.m2ts` containers with H.264, HEVC (H.265 10-bit HDR10 / HLG), VP9, and AV1 codecs powered by AndroidX Media3 (ExoPlayer).
- **Anime4K (OpenGL ES 2.0)**: Real-time shader line reconstruction and sharpening with **Mode A (Fast)**, **Mode B (Balanced)**, and **Mode C (High Quality)** presets, adjustable strength slider, and an interactive **Before / After** split-screen comparison overlay. Disabled by default.
- **LTPO Adaptive Refresh Rate & 60/90/120 Hz**: Automatically matches the display refresh rate to the video's native frame rate (`23.976` / `24` / `25` / `30` / `60 fps`) to eliminate 3:2 pulldown judder.
- **Frame Interpolation (MEMC)**: Motion smoothing modes (**Smooth 60 fps**, **Sharp Anime/24p**, and **Max 120 Hz**) to reduce panning stutter on high-refresh-rate OLED panels. Disabled by default.

#### 🎧 Audio, External Dubs & Bluetooth Sync
- **Automatic External Audio Binding**: Automatically discovers and synchronizes external audio tracks (`.mka`, `.aac`, `.ac3`, `.eac3`, `.dts`, `.flac`, `.ogg`, `.opus`, `.mp3`, `.wav`) located in `Audio`, `Rus`, `Sound`, or `Dub` subfolders by matching episode numbers.
- **Automatic Bluetooth Latency Compensation**: Real-time audio route monitoring (`AudioDeviceCallback`) detects **Bluetooth A2DP**, **Bluetooth LE Audio (BLE)**, and **Hearing Aid** connections and applies a sample-preserving `MediaClock` offset (`-150 ms` for A2DP, `-60 ms` for BLE Audio, `0 ms` when disconnected) without dropping PCM audio samples, plus user-configurable base offset calibration (`0 … -500 ms`).
- **Manual Fine-Tuning & Audio Boost**: Fine audio delay control within **±5.0 s** (`±50 ms` / `±250 ms` steps), a one-tap **Resync Stream** button, and software **Audio Boost up to 200%** with a soft-knee `tanh` limiter to prevent clipping.

#### 💬 Subtitles, Episodes & Chapters
- **SRT / ASS / SSA / VTT Subtitles**: Automatic external subtitle matching by episode number, customizable font size (`14–34 sp`), text color, dark background box, and `±500 ms` timing offset.
- **Dedicated Episodes & Chapters Windows**: Separate modal sheets for switching folder/season episodes and jumping to embedded container chapters (`OP`, `ED`, scenes) with timeline markers.
- **Playback Utilities**: **A–B Loop** repeat, sleep timer, customizable **Skip Intro** button (**+85 s** default), automatic playback resume, and **Picture-in-Picture (PiP)** mode.

#### 🌐 TorrServer & Torrent Streaming
- **Native TorrServer Integration**: Stream `.torrent` files and `magnet:` links directly without waiting for full downloads, inspect multi-folder voiceover packs, and manage server cache.
- **Built-in Tracker Search & Torznab / Jackett / Prowlarr**: Search torrents with quality filters (`4K`, `1080p`, `HEVC`) and optional DNS-over-HTTPS (DoH) bypass.

#### 🎨 Interface & Localization
- **Bilingual UI**: Switch between **System Auto**, **Русский**, and **English** instantly without restarting the app.
- **6 OLED App Themes & 6 Player HUD Themes**: Includes compact frosted-glass center transport controls with accent glow and a customizable quick-actions bar.

### 🛠 Build & Verification

```bash
# Build debug APK
gradle assembleDebug

# Run unit & Robolectric tests
gradle :app:testDebugUnitTest
```

> Release notes and detailed changelogs are generated automatically in **GitHub Releases** for each semantic version tag.
