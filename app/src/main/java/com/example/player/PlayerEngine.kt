package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.view.Surface
import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.LibraryRepository
import com.example.data.MediaAutoMatcher
import com.example.data.PlaybackResumeHelper
import com.example.model.DecoderMode
import com.example.model.EpisodeItem
import com.example.model.ExternalAudioTrack
import com.example.model.ExternalSubtitleTrack
import com.example.model.PlaybackEndMode
import com.example.model.SleepTimerPreset
import com.example.model.VideoChapter
import com.example.subtitles.ParsedSubtitleDocument
import com.example.subtitles.SubtitleAlignment
import com.example.subtitles.SubtitleCue
import com.example.subtitles.SubtitleParser
import com.example.torrent.TorrServerClient
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class AudioTrackUiOption(
    val id: String,
    val label: String,
    val badge: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean,
    val isExternal: Boolean
)

@Immutable
data class SubtitleTrackUiOption(
    val id: String,
    val label: String,
    val badge: String,
    val isExternal: Boolean,
    val externalTrack: ExternalSubtitleTrack? = null,
    val embeddedGroupIndex: Int = -1,
    val embeddedTrackIndex: Int = -1,
    val isSelected: Boolean = false
)

@Immutable
data class PlaybackRuntimeState(
    val playlist: List<EpisodeItem> = emptyList(),
    val currentEpisodeIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val videoWidth: Int = 1280,
    val videoHeight: Int = 720,
    val sourceFrameRate: Float = 0f,
    val chapters: List<VideoChapter> = emptyList(),
    val activeVideoCodecName: String = "Ожидание потока…",
    val isHardwareAccelerated: Boolean = true,
    val activeDecoderMode: DecoderMode = DecoderMode.HW_PLUS,
    val audioTrackOptions: List<AudioTrackUiOption> = emptyList(),
    val subtitleTrackOptions: List<SubtitleTrackUiOption> = emptyList(),
    val selectedSubtitleId: String = "off",
    val subtitleDelayMs: Long = 0L,
    val activeSubtitleCues: List<SubtitleCue> = emptyList(),
    val audioBoostPercent: Int = 100,
    val audioDelayMs: Long = 0L,
    val bluetoothAudioRoute: BluetoothAudioRouteState = BluetoothAudioRouteState(),
    val bluetoothAutoSyncEnabled: Boolean = true,
    val bluetoothConfiguredOffsetMs: Long = BluetoothAudioSyncController.DEFAULT_BT_OFFSET_MS,
    val bluetoothAppliedOffsetMs: Long = 0L,
    val effectiveAudioDelayMs: Long = 0L,
    val playbackEndMode: PlaybackEndMode = PlaybackEndMode.AUTO_NEXT,
    val sleepTimerPreset: SleepTimerPreset = SleepTimerPreset.OFF,
    val sleepTimerRemainingMs: Long = 0L,
    val abLoopStartMs: Long? = null,
    val abLoopEndMs: Long? = null,
    val isTempSpeedBoostActive: Boolean = false,
    val resumedFromPositionMs: Long? = null,
    val resumeEventId: Long = 0L,
    val errorMessage: String? = null
) {
    val currentEpisode: EpisodeItem?
        get() = playlist.getOrNull(currentEpisodeIndex)

    val hasPreviousEpisode: Boolean
        get() = currentEpisodeIndex > 0

    val hasNextEpisode: Boolean
        get() = currentEpisodeIndex in 0 until (playlist.size - 1)

    val isSleepTimerActive: Boolean
        get() = sleepTimerPreset != SleepTimerPreset.OFF &&
            (sleepTimerPreset.stopAtEndOfEpisode || sleepTimerRemainingMs > 0L)

    val isAbLoopActive: Boolean
        get() = abLoopStartMs != null && abLoopEndMs != null && abLoopEndMs > abLoopStartMs
}

@OptIn(UnstableApi::class)
class PlayerEngine private constructor(
    private val appContext: Context,
    private val repository: LibraryRepository
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackRuntimeState())
    val state: StateFlow<PlaybackRuntimeState> = _state.asStateFlow()

    /**
     * High-frequency playback position decoupled from [PlaybackRuntimeState] so timeline
     * ticks never trigger full-screen recomposition of the player HUD or video surface.
     */
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val playerChangedListeners = mutableListOf<(ExoPlayer) -> Unit>()

    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer
        get() = ensureExoPlayer()

    private var configuredDecoderMode: DecoderMode = DecoderMode.HW_PLUS
    private var highRefreshOrInterpActive: Boolean = true

    private var boundSurface: Surface? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // Per-episode extra manual tracks added during session (keyed by videoUri)
    private val extraAudioByVideo = mutableMapOf<String, MutableList<ExternalAudioTrack>>()
    private val extraSubsByVideo = mutableMapOf<String, MutableList<ExternalSubtitleTrack>>()
    private val autoAppliedDefaultAudioForVideo = mutableSetOf<String>()

    private val audioBoostController = AudioBoostController()
    private val audioDelayProcessor = AudioDelayProcessor()
    private var manualAudioDelayMs: Long = 0L
    private var bluetoothAutoSyncEnabled: Boolean = true
    private var bluetoothConfiguredOffsetMs: Long = BluetoothAudioSyncController.DEFAULT_BT_OFFSET_MS
    private val bluetoothSyncController = BluetoothAudioSyncController(appContext) {
        recalculateEffectiveAudioDelay(resyncIfPlaying = true)
    }
    private var activeExternalSubtitleDoc: ParsedSubtitleDocument? = null
    private var latestEmbeddedCues: List<SubtitleCue> = emptyList()
    private var positionTickerJob: Job? = null
    private var chapterExtractionJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var sleepDeadlineElapsedMs: Long = 0L
    private var basePlaybackSpeed: Float = 1.0f
    @Volatile
    private var preferExternalAudioOnStart: Boolean = true
    @Volatile
    private var enableEmbeddedChapters: Boolean = true
    @Volatile
    private var latestTorrServerBaseUrl: String = "http://127.0.0.1:8090"
    @Volatile
    private var latestTorrServerAuthHeader: String? = null
    private var lastSavedTickCount = 0
    private var lastTrackedEpisodeIndex: Int = -1
    private var lastKnownDurationForTrackedEpisodeMs: Long = 0L
    @Volatile
    private var detectedVideoWidth: Int = 0
    @Volatile
    private var detectedVideoHeight: Int = 0
    @Volatile
    private var lastFramePtsUs: Long = C.TIME_UNSET
    @Volatile
    private var smoothedPtsDeltaUs: Float = 0f
    @Volatile
    private var ptsSampleCount: Int = 0

    private val videoFrameMetadataListener = VideoFrameMetadataListener { presentationTimeUs, _, format, _ ->
        val headerFps = format.frameRate
        if (headerFps > 1f) {
            if (kotlin.math.abs(_state.value.sourceFrameRate - headerFps) > 0.2f) {
                _state.update { it.copy(sourceFrameRate = headerFps) }
            }
            return@VideoFrameMetadataListener
        }
        val prevPts = lastFramePtsUs
        lastFramePtsUs = presentationTimeUs
        if (prevPts != C.TIME_UNSET) {
            val deltaUs = presentationTimeUs - prevPts
            if (deltaUs in 10_000L..100_000L) {
                smoothedPtsDeltaUs = if (ptsSampleCount == 0) {
                    deltaUs.toFloat()
                } else {
                    smoothedPtsDeltaUs * 0.82f + deltaUs.toFloat() * 0.18f
                }
                ptsSampleCount++
                if (ptsSampleCount >= 6 && smoothedPtsDeltaUs > 1_000f) {
                    val estimatedFps = DisplayRefreshRateController.normalizeSourceFps(
                        1_000_000f / smoothedPtsDeltaUs
                    )
                    if (kotlin.math.abs(_state.value.sourceFrameRate - estimatedFps) > 0.4f) {
                        _state.update { it.copy(sourceFrameRate = estimatedFps) }
                    }
                }
            } else if (deltaUs < 0L || deltaUs > 500_000L) {
                ptsSampleCount = 0
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            audioBoostController.onAudioSessionIdChanged(audioSessionId)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val pos = _exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: _positionMs.value
            _positionMs.value = pos
            _state.update { it.copy(isPlaying = isPlaying, currentPositionMs = pos) }
            if (isPlaying) {
                startPositionTicker()
            } else {
                stopPositionTicker()
                persistCurrentProgress()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady &&
                reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                _state.value.sleepTimerPreset == SleepTimerPreset.END_OF_EPISODE
            ) {
                clearSleepTimerInternal()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = _exoPlayer ?: return
            val buffering = playbackState == Player.STATE_BUFFERING
            val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: _state.value.durationMs
            val pos = player.currentPosition.coerceAtLeast(0L)
            _positionMs.value = pos
            if (dur > 0L) {
                lastKnownDurationForTrackedEpisodeMs = dur
            }
            _state.update { prev ->
                val updatedChapters = if (dur > 0L && prev.chapters.isNotEmpty() && prev.chapters.last().endTimeMs < dur) {
                    prev.chapters.mapIndexed { idx, ch ->
                        if (idx == prev.chapters.lastIndex && ch.endTimeMs <= ch.startTimeMs) {
                            ch.copy(endTimeMs = dur)
                        } else {
                            ch
                        }
                    }
                } else {
                    prev.chapters
                }
                prev.copy(
                    isBuffering = buffering,
                    durationMs = dur,
                    currentPositionMs = pos,
                    chapters = updatedChapters
                )
            }
            if (playbackState == Player.STATE_ENDED) {
                stopPositionTicker()
                if (_state.value.sleepTimerPreset == SleepTimerPreset.END_OF_EPISODE) {
                    clearSleepTimerInternal()
                }
                persistCurrentProgress()
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                val par = videoSize.pixelWidthHeightRatio.takeIf { it > 0f } ?: 1f
                val displayW = (videoSize.width * par).roundToInt().coerceAtLeast(1)
                val displayH = videoSize.height.coerceAtLeast(1)
                val swapped = videoSize.unappliedRotationDegrees == 90 || videoSize.unappliedRotationDegrees == 270
                val finalW = if (swapped) displayH else displayW
                val finalH = if (swapped) displayW else displayH
                detectedVideoWidth = finalW
                detectedVideoHeight = finalH
                _state.update {
                    it.copy(
                        videoWidth = finalW,
                        videoHeight = finalH
                    )
                }
                persistCurrentProgress()
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            val playlist = _state.value.playlist
            if (oldPosition.mediaItemIndex != newPosition.mediaItemIndex &&
                oldPosition.mediaItemIndex in playlist.indices
            ) {
                val outgoingEp = playlist[oldPosition.mediaItemIndex]
                val outgoingPos = oldPosition.positionMs.coerceAtLeast(0L)
                val outgoingDur = lastKnownDurationForTrackedEpisodeMs.coerceAtLeast(outgoingEp.durationMs)
                if (outgoingPos > 0L || outgoingDur > 0L) {
                    engineScope.launch {
                        repository.savePlaybackProgress(
                            episode = outgoingEp,
                            positionMs = outgoingPos,
                            durationMs = outgoingDur,
                            videoWidth = detectedVideoWidth,
                            videoHeight = detectedVideoHeight
                        )
                    }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = _exoPlayer ?: return
            val idx = player.currentMediaItemIndex
            val playlist = _state.value.playlist
            if (idx in playlist.indices) {
                val prevIdx = lastTrackedEpisodeIndex
                val prevDur = lastKnownDurationForTrackedEpisodeMs
                lastTrackedEpisodeIndex = idx
                val currentDur = player.duration.takeIf { d -> d != C.TIME_UNSET && d > 0L } ?: 0L
                val pos = player.currentPosition.coerceAtLeast(0L)
                _positionMs.value = pos
                lastKnownDurationForTrackedEpisodeMs = currentDur

                _state.update {
                    it.copy(
                        currentEpisodeIndex = idx,
                        currentPositionMs = pos,
                        durationMs = currentDur,
                        chapters = emptyList(),
                        abLoopStartMs = null,
                        abLoopEndMs = null,
                        errorMessage = null
                    )
                }
                val newEp = playlist[idx]
                detectedVideoWidth = newEp.videoWidth
                detectedVideoHeight = newEp.videoHeight
                lastFramePtsUs = C.TIME_UNSET
                ptsSampleCount = 0
                onEpisodeActivated(newEp)

                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
                ) {
                    engineScope.launch {
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                            prevIdx in playlist.indices &&
                            prevIdx != idx &&
                            prevDur > 0L
                        ) {
                            repository.savePlaybackProgress(
                                episode = playlist[prevIdx],
                                positionMs = prevDur,
                                durationMs = prevDur
                            )
                        }
                        val settings = repository.settingsFlow.first()
                        val saved = repository.getSavedProgressForEpisode(newEp)
                        val resumePos = PlaybackResumeHelper.computeResumePositionMs(
                            positionMs = saved?.positionMs ?: newEp.resumePositionMs,
                            durationMs = saved?.durationMs ?: newEp.durationMs,
                            resumeEnabled = settings.resumePlayback
                        )
                        val effectivePos = if (resumePos > 0L &&
                            player.currentMediaItemIndex == idx &&
                            player.currentPosition < 2_500L
                        ) {
                            player.seekTo(idx, resumePos)
                            _positionMs.value = resumePos
                            _state.update {
                                it.copy(
                                    currentPositionMs = resumePos,
                                    durationMs = saved?.durationMs ?: it.durationMs,
                                    resumedFromPositionMs = resumePos,
                                    resumeEventId = System.currentTimeMillis()
                                )
                            }
                            resumePos
                        } else {
                            player.currentPosition.coerceAtLeast(0L)
                        }
                        repository.savePlaybackProgress(
                            episode = newEp,
                            positionMs = effectivePos,
                            durationMs = saved?.durationMs ?: newEp.durationMs
                        )
                    }
                }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            refreshTracksState(tracks)
        }

        override fun onCues(cueGroup: CueGroup) {
            if (_state.value.selectedSubtitleId.startsWith("emb:")) {
                latestEmbeddedCues = cueGroup.cues.mapNotNull { cue ->
                    val txt = cue.text?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    SubtitleCue(
                        startMs = 0L,
                        endMs = Long.MAX_VALUE,
                        text = txt,
                        alignment = SubtitleAlignment.BOTTOM_CENTER
                    )
                }
                _state.update { it.copy(activeSubtitleCues = latestEmbeddedCues) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            stopPositionTicker()
            _state.update {
                it.copy(
                    errorMessage = "Ошибка воспроизведения: ${error.localizedMessage ?: error.errorCodeName}",
                    isBuffering = false
                )
            }
            repository.refreshMediaAvailabilityAsync()
        }
    }

    private val analyticsListener = object : AnalyticsListener {
        override fun onVideoDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            val lower = decoderName.lowercase()
            val isSw = lower.startsWith("omx.google.") ||
                lower.startsWith("c2.android.") ||
                lower.contains("ffmpeg") ||
                lower.contains("dav1d") ||
                lower.contains("libvpx")
            _state.update {
                it.copy(
                    activeVideoCodecName = decoderName,
                    isHardwareAccelerated = !isSw
                )
            }
        }

        override fun onVideoInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
        ) {
            if (format.width > 0 && format.height > 0) {
                val par = format.pixelWidthHeightRatio.takeIf { it > 0f } ?: 1f
                val parW = (format.width * par).roundToInt().coerceAtLeast(1)
                val parH = format.height.coerceAtLeast(1)
                val rotated = format.rotationDegrees == 90 || format.rotationDegrees == 270
                val dispW = if (rotated) parH else parW
                val dispH = if (rotated) parW else parH
                val fps = format.frameRate.takeIf { it > 1f } ?: 0f
                detectedVideoWidth = dispW
                detectedVideoHeight = dispH

                _state.update { state ->
                    val codecTag = format.sampleMimeType?.substringAfter('/')?.uppercase() ?: ""
                    val updatedName = if (state.activeVideoCodecName.startsWith("Ожидание")) {
                        codecTag.ifBlank { state.activeVideoCodecName }
                    } else {
                        state.activeVideoCodecName
                    }
                    state.copy(
                        videoWidth = dispW,
                        videoHeight = dispH,
                        sourceFrameRate = if (fps > 0f) fps else state.sourceFrameRate,
                        activeVideoCodecName = updatedName
                    )
                }
            }
        }
    }

    init {
        // Lightweight settings observer only — heavy ExoPlayer & MediaCodec creation is deferred
        // until ensureExoPlayer() is invoked when actual video playback begins!
        val boot = repository.preferencesRepository.readBootSettingsSnapshot()
        configuredDecoderMode = boot.decoderMode
        highRefreshOrInterpActive = boot.highRefreshRateEnabled || boot.effectiveFrameInterpolationMode.isEnabled
        bluetoothAutoSyncEnabled = boot.bluetoothAudioAutoSync
        bluetoothConfiguredOffsetMs = boot.bluetoothAudioOffsetMs
        _state.update {
            it.copy(
                activeDecoderMode = boot.decoderMode,
                playbackEndMode = boot.playbackEndMode,
                bluetoothAutoSyncEnabled = boot.bluetoothAudioAutoSync,
                bluetoothConfiguredOffsetMs = boot.bluetoothAudioOffsetMs
            )
        }
        recalculateEffectiveAudioDelay(resyncIfPlaying = false)

        engineScope.launch {
            repository.settingsFlow.collect { s ->
                val prevChaptersEnabled = enableEmbeddedChapters
                val prevBtAuto = bluetoothAutoSyncEnabled
                val prevBtOffset = bluetoothConfiguredOffsetMs
                preferExternalAudioOnStart = s.preferExternalAudioOnStart
                enableEmbeddedChapters = s.enableEmbeddedChapters
                bluetoothAutoSyncEnabled = s.bluetoothAudioAutoSync
                bluetoothConfiguredOffsetMs = s.bluetoothAudioOffsetMs
                latestTorrServerBaseUrl = TorrServerClient.normalizeBaseUrl(s.torrServerUrl)
                latestTorrServerAuthHeader = TorrServerClient.buildBasicAuthHeader(
                    s.torrServerUsername,
                    s.torrServerPassword
                )
                val wantNoRateSwitch = s.effectiveFrameInterpolationMode.isEnabled ||
                    (s.highRefreshRateEnabled && !s.ltpoAdaptiveRefreshEnabled)
                highRefreshOrInterpActive = wantNoRateSwitch
                _exoPlayer?.let { activePlayer ->
                    applyFrameRateStrategy(activePlayer, wantNoRateSwitch)
                }
                if (prevBtAuto != s.bluetoothAudioAutoSync || prevBtOffset != s.bluetoothAudioOffsetMs) {
                    recalculateEffectiveAudioDelay(resyncIfPlaying = false)
                }
                if (prevChaptersEnabled != s.enableEmbeddedChapters) {
                    if (!s.enableEmbeddedChapters) {
                        chapterExtractionJob?.cancel()
                        _state.update { it.copy(chapters = emptyList()) }
                    } else {
                        _state.value.currentEpisode?.let { ep -> loadEmbeddedChaptersAsync(ep) }
                    }
                }
            }
        }
    }

    @Synchronized
    private fun ensureExoPlayer(): ExoPlayer {
        _exoPlayer?.let { return it }
        val player = createConfiguredExoPlayer(configuredDecoderMode)
        _exoPlayer = player
        applyFrameRateStrategy(player, highRefreshOrInterpActive)
        applyPlaybackEndModeToExo(_state.value.playbackEndMode)
        audioBoostController.onAudioSessionIdChanged(player.audioSessionId)
        boundSurface?.let { surf ->
            if (surf.isValid) player.setVideoSurface(surf)
        }
        return player
    }

    private fun applyFrameRateStrategy(player: ExoPlayer, keepPanelAtHighRefresh: Boolean) {
        player.videoChangeFrameRateStrategy = if (keepPanelAtHighRefresh) {
            C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF
        } else {
            C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS
        }
    }

    private fun createConfiguredExoPlayer(mode: DecoderMode): ExoPlayer {
        val renderersFactory = object : DefaultRenderersFactory(appContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(audioDelayProcessor))
                    .build()
            }

            override fun buildAudioRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                audioSink: AudioSink,
                eventHandler: android.os.Handler,
                eventListener: androidx.media3.exoplayer.audio.AudioRendererEventListener,
                out: java.util.ArrayList<androidx.media3.exoplayer.Renderer>
            ) {
                super.buildAudioRenderers(
                    context,
                    extensionRendererMode,
                    mediaCodecSelector,
                    enableDecoderFallback,
                    audioSink,
                    eventHandler,
                    eventListener,
                    out
                )
                for (i in out.indices) {
                    if (out[i] is androidx.media3.exoplayer.audio.MediaCodecAudioRenderer) {
                        out[i] = object : androidx.media3.exoplayer.audio.MediaCodecAudioRenderer(
                            context,
                            codecAdapterFactory,
                            mediaCodecSelector,
                            enableDecoderFallback,
                            eventHandler,
                            eventListener,
                            audioSink
                        ) {
                            override fun getPositionUs(): Long {
                                val baseUs = super.getPositionUs()
                                if (baseUs == C.TIME_UNSET || baseUs == Long.MIN_VALUE) return baseUs
                                val offsetUs = audioDelayProcessor.mediaClockOffsetUs
                                return if (offsetUs < 0L) {
                                    (baseUs + offsetUs).coerceAtLeast(0L)
                                } else {
                                    baseUs
                                }
                            }
                        }
                    }
                }
            }
        }.apply {
            when (mode) {
                DecoderMode.HW_PLUS -> {
                    setEnableDecoderFallback(true)
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                    setMediaCodecSelector(MediaCodecSelector.DEFAULT)
                }
                DecoderMode.HW_STRICT -> {
                    setEnableDecoderFallback(false)
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                    setMediaCodecSelector(MediaCodecSelector.DEFAULT)
                }
                DecoderMode.SOFTWARE_PREFER -> {
                    setEnableDecoderFallback(true)
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                    setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                        val defaultList = MediaCodecSelector.DEFAULT.getDecoderInfos(
                            mimeType,
                            requiresSecureDecoder,
                            requiresTunnelingDecoder
                        )
                        defaultList.sortedBy { info ->
                            val n = info.name.lowercase()
                            val isSw = n.startsWith("c2.android.") || n.startsWith("omx.google.") || !info.hardwareAccelerated
                            if (isSw) 0 else 1
                        }
                    }
                }
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(appContext, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
            .also { player ->
                player.addListener(playerListener)
                player.addAnalyticsListener(analyticsListener)
                player.setVideoFrameMetadataListener(videoFrameMetadataListener)
            }
    }

    fun registerPlayerSwapListener(listener: (ExoPlayer) -> Unit) {
        playerChangedListeners.add(listener)
    }

    fun unregisterPlayerSwapListener(listener: (ExoPlayer) -> Unit) {
        playerChangedListeners.remove(listener)
    }

    fun ensurePlaybackServiceStarted() {
        if (controllerFuture != null) return
        runCatching {
            val token = SessionToken(
                appContext,
                ComponentName(appContext, PlaybackService::class.java)
            )
            val future = MediaController.Builder(appContext, token).buildAsync()
            controllerFuture = future
            future.addListener({}, MoreExecutors.directExecutor())
        }
    }

    fun attachVideoSurface(surface: Surface) {
        boundSurface = surface
        ensureExoPlayer().setVideoSurface(surface)
    }

    fun detachVideoSurface(surface: Surface) {
        if (boundSurface === surface) {
            _exoPlayer?.clearVideoSurface(surface)
            boundSurface = null
        }
    }

    fun startPlaylist(
        episodes: List<EpisodeItem>,
        startIndex: Int,
        autoResume: Boolean = true
    ) {
        if (episodes.isEmpty()) return
        // Persist any currently playing episode before replacing the playlist (e.g., when switching while in PiP)
        persistCurrentProgress()
        val safeIndex = startIndex.coerceIn(episodes.indices)
        val player = ensureExoPlayer()
        ensurePlaybackServiceStarted()

        engineScope.launch {
            val settings = repository.settingsFlow.first()
            bluetoothAutoSyncEnabled = settings.bluetoothAudioAutoSync
            bluetoothConfiguredOffsetMs = settings.bluetoothAudioOffsetMs
            bluetoothSyncController.refreshRouteState()
            recalculateEffectiveAudioDelay(resyncIfPlaying = false)
            if (settings.decoderMode != configuredDecoderMode) {
                switchDecoderMode(settings.decoderMode)
            }
            val activePlayer = ensureExoPlayer()
            applyFrameRateStrategy(
                activePlayer,
                settings.effectiveFrameInterpolationMode.isEnabled ||
                    (settings.highRefreshRateEnabled && !settings.ltpoAdaptiveRefreshEnabled)
            )

            for (ep in episodes) {
                val saved = repository.getSavedProgressForEpisode(ep)
                if (saved?.manualAudioUri != null) {
                    val list = extraAudioByVideo.getOrPut(ep.uri) { mutableListOf() }
                    if (list.none { it.uri == saved.manualAudioUri }) {
                        list.add(
                            ExternalAudioTrack(
                                uri = saved.manualAudioUri,
                                displayName = saved.manualAudioName ?: "Внешнее аудио",
                                sourceFolderName = "Вручную",
                                extension = MediaAutoMatcher.extensionOf(saved.manualAudioName ?: "mka"),
                                isAutoMatched = false
                            )
                        )
                    }
                }
                if (saved?.manualSubtitleUri != null) {
                    val list = extraSubsByVideo.getOrPut(ep.uri) { mutableListOf() }
                    if (list.none { it.uri == saved.manualSubtitleUri }) {
                        val subName = saved.manualSubtitleName ?: "Субтитры.srt"
                        list.add(
                            ExternalSubtitleTrack(
                                uri = saved.manualSubtitleUri,
                                displayName = subName,
                                sourceFolderName = "Вручную",
                                format = MediaAutoMatcher.subtitleFormatOf(subName),
                                isAutoMatched = false
                            )
                        )
                    }
                }
            }

            val startEpisode = episodes[safeIndex]
            val savedProgress = repository.getSavedProgressForEpisode(startEpisode)
            val effectiveSavedDuration = (savedProgress?.durationMs ?: 0L).coerceAtLeast(startEpisode.durationMs)
            val effectiveSavedPos = savedProgress?.positionMs ?: startEpisode.resumePositionMs
            val startPos = PlaybackResumeHelper.computeResumePositionMs(
                positionMs = effectiveSavedPos,
                durationMs = effectiveSavedDuration,
                resumeEnabled = autoResume && settings.resumePlayback
            )

            latestTorrServerBaseUrl = TorrServerClient.normalizeBaseUrl(settings.torrServerUrl)
            latestTorrServerAuthHeader = TorrServerClient.buildBasicAuthHeader(
                settings.torrServerUsername,
                settings.torrServerPassword
            )
            lastTrackedEpisodeIndex = safeIndex
            lastKnownDurationForTrackedEpisodeMs = effectiveSavedDuration
            detectedVideoWidth = startEpisode.videoWidth.takeIf { it > 0 } ?: (savedProgress?.videoWidth ?: 0)
            detectedVideoHeight = startEpisode.videoHeight.takeIf { it > 0 } ?: (savedProgress?.videoHeight ?: 0)
            lastFramePtsUs = C.TIME_UNSET
            ptsSampleCount = 0
            autoAppliedDefaultAudioForVideo.clear()
            val sources = episodes.map { buildMediaSourceForEpisode(it) }

            val defaultSpd = settings.defaultPlaybackSpeed.coerceIn(0.25f, 3.0f)
            basePlaybackSpeed = defaultSpd
            activePlayer.playbackParameters = PlaybackParameters(defaultSpd)
            val appliedBoost = audioBoostController.setBoostPercent(settings.defaultAudioBoostPercent)
            _positionMs.value = startPos

            _state.update {
                it.copy(
                    playlist = episodes,
                    currentEpisodeIndex = safeIndex,
                    currentPositionMs = startPos,
                    durationMs = effectiveSavedDuration,
                    videoWidth = if (detectedVideoWidth > 0) detectedVideoWidth else it.videoWidth,
                    videoHeight = if (detectedVideoHeight > 0) detectedVideoHeight else it.videoHeight,
                    chapters = emptyList(),
                    sourceFrameRate = 0f,
                    playbackSpeed = defaultSpd,
                    isTempSpeedBoostActive = false,
                    audioBoostPercent = appliedBoost,
                    audioDelayMs = 0L,
                    subtitleDelayMs = 0L,
                    abLoopStartMs = null,
                    abLoopEndMs = null,
                    resumedFromPositionMs = startPos.takeIf { pos -> pos > 0L },
                    resumeEventId = System.currentTimeMillis(),
                    errorMessage = null
                )
            }

            audioDelayProcessor.setDelayMs(0L)
            activePlayer.setMediaSources(sources, safeIndex, startPos)
            activePlayer.prepare()
            activePlayer.playWhenReady = true

            if (PlaybackResumeHelper.isMediaFileExists(appContext, startEpisode.uri) ||
                !PlaybackResumeHelper.isLocalFileUri(startEpisode.uri)
            ) {
                repository.savePlaybackProgress(
                    episode = startEpisode,
                    positionMs = startPos,
                    durationMs = effectiveSavedDuration,
                    videoWidth = detectedVideoWidth,
                    videoHeight = detectedVideoHeight
                )
            }

            onEpisodeActivated(startEpisode)
            startPositionTicker()
        }
    }

    private fun buildMediaSourceForEpisode(episode: EpisodeItem): MediaSource {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("negai/2.0 ExoPlayer")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(45_000)

        val authHeader = latestTorrServerAuthHeader
        if (authHeader != null && episode.uri.startsWith(latestTorrServerBaseUrl, ignoreCase = true)) {
            httpFactory.setDefaultRequestProperties(mapOf("Authorization" to authHeader))
        }

        val dataSourceFactory = DefaultDataSource.Factory(appContext, httpFactory)
        val extractorsFactory = DefaultExtractorsFactory()
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

        val metadata = MediaMetadata.Builder()
            .setTitle(episode.cleanTitle)
            .setArtist(episode.folderName)
            .setAlbumTitle(episode.folderName)
            .setSubtitle(episode.episodeBadge ?: episode.folderName)
            .setDisplayTitle(episode.cleanTitle)
            .build()

        val videoItem = MediaItem.Builder()
            .setUri(episode.uri)
            .setMediaId(episode.uri)
            .setMediaMetadata(metadata)
            .build()

        val videoSource = mediaSourceFactory.createMediaSource(videoItem)
        val allExtAudio = combinedExternalAudioFor(episode)
        if (allExtAudio.isEmpty()) {
            return videoSource
        }

        val audioSources = allExtAudio.map { ext ->
            val audioItem = MediaItem.Builder()
                .setUri(ext.uri)
                .setMediaId("ext_audio:${ext.uri}")
                .build()
            mediaSourceFactory.createMediaSource(audioItem)
        }

        return MergingMediaSource(
            /* adjustPeriodTimeOffsets = */ true,
            /* clipDurations = */ false,
            videoSource,
            *audioSources.toTypedArray()
        )
    }

    private fun combinedExternalAudioFor(episode: EpisodeItem): List<ExternalAudioTrack> {
        val manual = extraAudioByVideo[episode.uri].orEmpty()
        val existingUris = episode.autoAudioTracks.map { it.uri }.toSet()
        return episode.autoAudioTracks + manual.filter { it.uri !in existingUris }
    }

    private fun combinedExternalSubtitlesFor(episode: EpisodeItem): List<ExternalSubtitleTrack> {
        val manual = extraSubsByVideo[episode.uri].orEmpty()
        val existingUris = episode.autoSubtitleTracks.map { it.uri }.toSet()
        return manual.filter { it.uri !in existingUris } + episode.autoSubtitleTracks
    }

    private fun onEpisodeActivated(episode: EpisodeItem) {
        engineScope.launch {
            val settings = repository.settingsFlow.first()
            enableEmbeddedChapters = settings.enableEmbeddedChapters
            if (settings.enableEmbeddedChapters) {
                loadEmbeddedChaptersAsync(episode)
            } else {
                _state.update { it.copy(chapters = emptyList()) }
            }

            val extSubs = combinedExternalSubtitlesFor(episode)
            if (extSubs.isNotEmpty() && (settings.autoBindExternalSubtitles || !extSubs.first().isAutoMatched)) {
                selectExternalSubtitle(extSubs.first())
            } else {
                activeExternalSubtitleDoc = null
                _state.update {
                    it.copy(
                        selectedSubtitleId = "off",
                        activeSubtitleCues = emptyList()
                    )
                }
                _exoPlayer?.let { refreshTracksState(it.currentTracks) }
            }
        }
    }

    private fun loadEmbeddedChaptersAsync(episode: EpisodeItem) {
        chapterExtractionJob?.cancel()
        val targetUri = episode.uri
        val knownDur = _state.value.durationMs.coerceAtLeast(episode.durationMs)
        val baseUrl = latestTorrServerBaseUrl
        val auth = latestTorrServerAuthHeader
        chapterExtractionJob = engineScope.launch {
            val extracted = withContext(Dispatchers.IO) {
                VideoChapterExtractor.extractChapters(
                    context = appContext,
                    videoUri = targetUri,
                    totalDurationMs = knownDur,
                    torrServerBaseUrl = baseUrl,
                    torrServerAuthHeader = auth
                )
            }
            if (_state.value.currentEpisode?.uri == targetUri && enableEmbeddedChapters && extracted.isNotEmpty()) {
                val latestDur = _state.value.durationMs.coerceAtLeast(knownDur)
                val updated = extracted.mapIndexed { idx, ch ->
                    if (idx == extracted.lastIndex && ch.endTimeMs <= ch.startTimeMs && latestDur > ch.startTimeMs) {
                        ch.copy(endTimeMs = latestDur)
                    } else {
                        ch
                    }
                }
                _state.update { it.copy(chapters = updated) }
            }
        }
    }

    private fun refreshTracksState(tracks: Tracks) {
        val player = _exoPlayer ?: return
        val episode = _state.value.currentEpisode ?: return
        val extAudioList = combinedExternalAudioFor(episode)
        val extSubList = combinedExternalSubtitlesFor(episode)

        val audioGroups = tracks.groups
            .mapIndexedNotNull { idx, group ->
                if (group.type == C.TRACK_TYPE_AUDIO) idx to group else null
            }

        val embeddedAudioCount = (audioGroups.size - extAudioList.size).coerceAtLeast(0)

        val hasManualExtAudio = extAudioList.any { !it.isAutoMatched }
        if (extAudioList.isNotEmpty() &&
            (preferExternalAudioOnStart || hasManualExtAudio) &&
            audioGroups.size >= extAudioList.size &&
            episode.uri !in autoAppliedDefaultAudioForVideo
        ) {
            autoAppliedDefaultAudioForVideo.add(episode.uri)
            val targetAudioListIdx = extAudioList.indexOfFirst { !it.isAutoMatched }
                .takeIf { it >= 0 } ?: 0
            val targetGroupPair = audioGroups.getOrNull(embeddedAudioCount + targetAudioListIdx)
            if (targetGroupPair != null) {
                val override = TrackSelectionOverride(targetGroupPair.second.mediaTrackGroup, 0)
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .addOverride(override)
                    .build()
                return
            }
        }

        val audioOptions = mutableListOf<AudioTrackUiOption>()
        audioGroups.forEachIndexed { audioSeqIndex, (rawGroupIndex, group) ->
            val isExt = audioSeqIndex >= embeddedAudioCount && extAudioList.isNotEmpty()
            val extMeta = if (isExt) extAudioList.getOrNull(audioSeqIndex - embeddedAudioCount) else null

            for (tIdx in 0 until group.length) {
                val format = group.getTrackFormat(tIdx)
                val selected = group.isTrackSelected(tIdx)
                val label = if (extMeta != null) {
                    extMeta.formattedLabel
                } else {
                    buildEmbeddedTrackLabel(format, audioSeqIndex + 1, "Аудиодорожка")
                }
                val badge = when {
                    extMeta != null && extMeta.isAutoMatched -> "АВТО • ${extMeta.extension.uppercase()}"
                    extMeta != null -> "ВНЕШ • ${extMeta.extension.uppercase()}"
                    else -> (format.sampleMimeType?.substringAfter('/')?.uppercase() ?: "ВСТРОЕННАЯ")
                }
                audioOptions.add(
                    AudioTrackUiOption(
                        id = "aud:$rawGroupIndex:$tIdx",
                        label = label,
                        badge = badge,
                        groupIndex = rawGroupIndex,
                        trackIndex = tIdx,
                        isSelected = selected,
                        isExternal = isExt
                    )
                )
            }
        }

        val currentSubId = _state.value.selectedSubtitleId
        val subOptions = mutableListOf<SubtitleTrackUiOption>()
        subOptions.add(
            SubtitleTrackUiOption(
                id = "off",
                label = "Выключены",
                badge = "OFF",
                isExternal = false,
                isSelected = currentSubId == "off"
            )
        )

        for (extSub in extSubList) {
            val id = "ext:${extSub.uri}"
            subOptions.add(
                SubtitleTrackUiOption(
                    id = id,
                    label = extSub.formattedLabel,
                    badge = if (extSub.isAutoMatched) "АВТО • ${extSub.format.label}" else "ВНЕШ • ${extSub.format.label}",
                    isExternal = true,
                    externalTrack = extSub,
                    isSelected = currentSubId == id
                )
            )
        }

        tracks.groups.forEachIndexed { rawGroupIdx, group ->
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (tIdx in 0 until group.length) {
                    val fmt = group.getTrackFormat(tIdx)
                    val id = "emb:$rawGroupIdx:$tIdx"
                    subOptions.add(
                        SubtitleTrackUiOption(
                            id = id,
                            label = buildEmbeddedTrackLabel(fmt, tIdx + 1, "Встроенные субтитры"),
                            badge = fmt.sampleMimeType?.substringAfter('/')?.uppercase() ?: "MKV",
                            isExternal = false,
                            embeddedGroupIndex = rawGroupIdx,
                            embeddedTrackIndex = tIdx,
                            isSelected = currentSubId == id
                        )
                    )
                }
            }
        }

        val fallbackId3Chapters = if (enableEmbeddedChapters && _state.value.chapters.isEmpty()) {
            VideoChapterExtractor.extractFromMedia3Tracks(tracks, _state.value.durationMs)
        } else {
            emptyList()
        }

        _state.update {
            it.copy(
                audioTrackOptions = audioOptions,
                subtitleTrackOptions = subOptions,
                chapters = if (it.chapters.isEmpty() && fallbackId3Chapters.isNotEmpty()) {
                    fallbackId3Chapters
                } else {
                    it.chapters
                }
            )
        }
    }

    private fun buildEmbeddedTrackLabel(format: Format, number: Int, fallbackPrefix: String): String {
        val parts = mutableListOf<String>()
        format.label?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        format.language?.takeIf { it.isNotBlank() && it != "und" }?.uppercase()?.let { lang ->
            if (parts.none { it.contains(lang, ignoreCase = true) }) parts.add(lang)
        }
        if (format.channelCount > 0) {
            val ch = when (format.channelCount) {
                1 -> "Mono"
                2 -> "2.0 Stereo"
                6 -> "5.1 Surround"
                8 -> "7.1 Surround"
                else -> "${format.channelCount}ch"
            }
            parts.add(ch)
        }
        return if (parts.isEmpty()) "$fallbackPrefix #$number" else parts.joinToString(" • ")
    }

    fun selectAudioTrack(option: AudioTrackUiOption) {
        val player = ensureExoPlayer()
        val group = player.currentTracks.groups.getOrNull(option.groupIndex) ?: return
        val override = TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex)
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .addOverride(override)
            .build()
    }

    fun attachManualExternalAudio(uri: Uri) {
        val episode = _state.value.currentEpisode ?: return
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val name = repository.resolveFileDisplayName(uri) ?: uri.lastPathSegment ?: "ExternalAudio.mka"
        val ext = MediaAutoMatcher.extensionOf(name).ifBlank { "mka" }
        val track = ExternalAudioTrack(
            uri = uri.toString(),
            displayName = name,
            sourceFolderName = "Внешний файл",
            extension = ext,
            isAutoMatched = false
        )
        val list = extraAudioByVideo.getOrPut(episode.uri) { mutableListOf() }
        list.removeAll { it.uri == track.uri }
        list.add(0, track)

        autoAppliedDefaultAudioForVideo.remove(episode.uri)

        engineScope.launch {
            repository.savePlaybackProgress(
                episode = episode,
                positionMs = (_exoPlayer?.currentPosition ?: _positionMs.value).coerceAtLeast(0L),
                durationMs = _state.value.durationMs,
                manualAudioUri = track.uri,
                manualAudioName = name
            )
            rebuildCurrentPlaylistPreservingPosition()
        }
    }

    fun selectSubtitleOption(option: SubtitleTrackUiOption) {
        val player = ensureExoPlayer()
        when {
            option.id == "off" -> {
                activeExternalSubtitleDoc = null
                latestEmbeddedCues = emptyList()
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .build()
                _state.update {
                    it.copy(
                        selectedSubtitleId = "off",
                        activeSubtitleCues = emptyList()
                    )
                }
                refreshTracksState(player.currentTracks)
            }
            option.isExternal && option.externalTrack != null -> {
                selectExternalSubtitle(option.externalTrack)
            }
            option.embeddedGroupIndex >= 0 -> {
                activeExternalSubtitleDoc = null
                val group = player.currentTracks.groups.getOrNull(option.embeddedGroupIndex) ?: return
                val override = TrackSelectionOverride(group.mediaTrackGroup, option.embeddedTrackIndex)
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .addOverride(override)
                    .build()
                _state.update {
                    it.copy(
                        selectedSubtitleId = option.id,
                        activeSubtitleCues = emptyList()
                    )
                }
                refreshTracksState(player.currentTracks)
            }
        }
    }

    private fun selectExternalSubtitle(track: ExternalSubtitleTrack) {
        val player = ensureExoPlayer()
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .build()

        engineScope.launch {
            val parsedDoc = withContext(Dispatchers.IO) {
                loadSubtitleDocument(track)
            }
            activeExternalSubtitleDoc = parsedDoc
            val activeCues = parsedDoc?.activeCuesAt(
                playerPositionMs = player.currentPosition.coerceAtLeast(0L),
                delayMs = _state.value.subtitleDelayMs
            ).orEmpty()

            _state.update {
                it.copy(
                    selectedSubtitleId = "ext:${track.uri}",
                    activeSubtitleCues = activeCues
                )
            }
            refreshTracksState(player.currentTracks)
        }
    }

    fun attachManualExternalSubtitle(uri: Uri) {
        val episode = _state.value.currentEpisode ?: return
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val name = repository.resolveFileDisplayName(uri) ?: uri.lastPathSegment ?: "Subtitles.srt"
        val format = MediaAutoMatcher.subtitleFormatOf(name)
        val track = ExternalSubtitleTrack(
            uri = uri.toString(),
            displayName = name,
            sourceFolderName = "Внешний файл",
            format = format,
            isAutoMatched = false
        )
        val list = extraSubsByVideo.getOrPut(episode.uri) { mutableListOf() }
        list.removeAll { it.uri == track.uri }
        list.add(0, track)

        engineScope.launch {
            repository.savePlaybackProgress(
                episode = episode,
                positionMs = (_exoPlayer?.currentPosition ?: _positionMs.value).coerceAtLeast(0L),
                durationMs = _state.value.durationMs,
                manualSubtitleUri = track.uri,
                manualSubtitleName = name
            )
            selectExternalSubtitle(track)
        }
    }

    private fun loadSubtitleDocument(track: ExternalSubtitleTrack): ParsedSubtitleDocument? {
        return runCatching {
            val uri = Uri.parse(track.uri)
            val bytes = when (uri.scheme?.lowercase()) {
                "file" -> uri.path?.let { File(it).readBytes() }
                "http", "https" -> {
                    val reqBuilder = okhttp3.Request.Builder().url(track.uri).get()
                    val authHeader = latestTorrServerAuthHeader
                    if (authHeader != null && track.uri.startsWith(latestTorrServerBaseUrl, ignoreCase = true)) {
                        reqBuilder.header("Authorization", authHeader)
                    }
                    TorrServerClient.httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                        if (resp.isSuccessful) resp.body?.bytes() else null
                    }
                }
                else -> appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return null

            val decodedText = SubtitleParser.decodeSubtitleBytes(bytes)
            SubtitleParser.parse(
                rawContent = decodedText,
                formatHint = track.format,
                title = track.displayName
            )
        }.getOrNull()
    }

    fun adjustSubtitleDelay(deltaMs: Long) {
        setSubtitleDelay((_state.value.subtitleDelayMs + deltaMs).coerceIn(-60_000L, 60_000L))
    }

    fun setSubtitleDelay(delayMs: Long) {
        val clamped = delayMs.coerceIn(-60_000L, 60_000L)
        val pos = (_exoPlayer?.currentPosition ?: _positionMs.value).coerceAtLeast(0L)
        val cues = activeExternalSubtitleDoc?.activeCuesAt(
            playerPositionMs = pos,
            delayMs = clamped
        ) ?: latestEmbeddedCues
        _state.update {
            it.copy(
                subtitleDelayMs = clamped,
                activeSubtitleCues = cues
            )
        }
    }

    fun switchDecoderMode(newMode: DecoderMode) {
        configuredDecoderMode = newMode
        val existingPlayer = _exoPlayer
        if (existingPlayer == null) {
            _state.update { it.copy(activeDecoderMode = newMode) }
            engineScope.launch { repository.preferencesRepository.updateDecoderMode(newMode) }
            return
        }
        if (_state.value.activeDecoderMode == newMode) return

        val wasPlaying = existingPlayer.playWhenReady && existingPlayer.playbackState != Player.STATE_IDLE
        val savedPos = existingPlayer.currentPosition.coerceAtLeast(0L)
        val savedIndex = existingPlayer.currentMediaItemIndex.coerceAtLeast(0)
        val playlist = _state.value.playlist
        val speed = _state.value.playbackSpeed

        existingPlayer.removeListener(playerListener)
        existingPlayer.removeAnalyticsListener(analyticsListener)
        existingPlayer.clearVideoFrameMetadataListener(videoFrameMetadataListener)
        boundSurface?.let { existingPlayer.clearVideoSurface(it) }

        val freshPlayer = createConfiguredExoPlayer(newMode)
        applyFrameRateStrategy(freshPlayer, highRefreshOrInterpActive)
        freshPlayer.playbackParameters = PlaybackParameters(speed)
        boundSurface?.let { freshPlayer.setVideoSurface(it) }
        _exoPlayer = freshPlayer
        applyPlaybackEndModeToExo(_state.value.playbackEndMode)
        audioBoostController.onAudioSessionIdChanged(freshPlayer.audioSessionId)

        _state.update {
            it.copy(
                activeDecoderMode = newMode,
                activeVideoCodecName = "Переключение на ${newMode.shortBadge}…"
            )
        }

        playerChangedListeners.forEach { it(freshPlayer) }
        existingPlayer.release()

        if (playlist.isNotEmpty()) {
            val sources = playlist.map { buildMediaSourceForEpisode(it) }
            freshPlayer.setMediaSources(sources, savedIndex.coerceIn(playlist.indices), savedPos)
            freshPlayer.prepare()
            freshPlayer.playWhenReady = wasPlaying
        }

        engineScope.launch {
            repository.preferencesRepository.updateDecoderMode(newMode)
        }
    }

    private fun rebuildCurrentPlaylistPreservingPosition() {
        val player = _exoPlayer ?: return
        val playlist = _state.value.playlist
        if (playlist.isEmpty()) return
        val idx = player.currentMediaItemIndex.coerceIn(playlist.indices)
        val pos = player.currentPosition.coerceAtLeast(0L)
        val wasPlaying = player.playWhenReady
        val sources = playlist.map { buildMediaSourceForEpisode(it) }
        player.setMediaSources(sources, idx, pos)
        player.prepare()
        player.playWhenReady = wasPlaying
    }

    fun togglePlayPause() {
        val player = ensureExoPlayer()
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
                _positionMs.value = 0L
            }
            player.play()
        }
    }

    fun seekBy(deltaMs: Long) {
        val player = ensureExoPlayer()
        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: Long.MAX_VALUE
        val target = (player.currentPosition + deltaMs).coerceIn(0L, dur)
        player.seekTo(target)
        _positionMs.value = target
        updatePositionAndSubtitlesNow(forceSyncRuntimePosition = true)
        persistCurrentProgress()
    }

    fun seekTo(positionMs: Long) {
        val player = ensureExoPlayer()
        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: Long.MAX_VALUE
        val target = positionMs.coerceIn(0L, dur)
        player.seekTo(target)
        _positionMs.value = target
        updatePositionAndSubtitlesNow(forceSyncRuntimePosition = true)
        persistCurrentProgress()
    }

    fun seekToChapter(chapter: VideoChapter) {
        seekTo(chapter.startTimeMs)
    }

    fun seekToNextChapter() {
        val chapters = _state.value.chapters
        if (chapters.isEmpty()) return
        val pos = (_exoPlayer?.currentPosition ?: _positionMs.value).coerceAtLeast(0L)
        val next = chapters.firstOrNull { it.startTimeMs > pos + 400L } ?: return
        seekTo(next.startTimeMs)
    }

    fun seekToPreviousChapter() {
        val chapters = _state.value.chapters
        if (chapters.isEmpty()) return
        val pos = (_exoPlayer?.currentPosition ?: _positionMs.value).coerceAtLeast(0L)
        // If > 3 seconds into current chapter, rewind to current chapter start; otherwise jump to previous chapter
        val currentIdx = chapters.indexOfLast { pos >= it.startTimeMs }
        if (currentIdx < 0) {
            seekTo(0L)
            return
        }
        val currentChap = chapters[currentIdx]
        val target = if (pos - currentChap.startTimeMs > 3_000L || currentIdx == 0) {
            currentChap.startTimeMs
        } else {
            chapters[currentIdx - 1].startTimeMs
        }
        seekTo(target)
    }

    fun dismissResumeBanner() {
        _state.update { it.copy(resumedFromPositionMs = null) }
    }

    fun restartFromBeginning() {
        dismissResumeBanner()
        seekTo(0L)
    }

    fun playPreviousEpisode() {
        val player = ensureExoPlayer()
        if (player.hasPreviousMediaItem()) {
            playEpisodeAtIndex(player.previousMediaItemIndex)
        } else {
            seekTo(0L)
        }
    }

    fun playNextEpisode() {
        val player = ensureExoPlayer()
        if (player.hasNextMediaItem()) {
            playEpisodeAtIndex(player.nextMediaItemIndex)
        }
    }

    fun playEpisodeAtIndex(index: Int) {
        val player = ensureExoPlayer()
        val playlist = _state.value.playlist
        if (index in playlist.indices && index != player.currentMediaItemIndex) {
            val outgoingEp = _state.value.currentEpisode
            val outgoingPos = player.currentPosition.coerceAtLeast(0L)
            val outgoingDur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: _state.value.durationMs
            val outgoingW = detectedVideoWidth
            val outgoingH = detectedVideoHeight
            val targetEpisode = playlist[index]
            engineScope.launch {
                if (outgoingEp != null && (outgoingPos > 0L || outgoingDur > 0L)) {
                    repository.savePlaybackProgress(
                        episode = outgoingEp,
                        positionMs = outgoingPos,
                        durationMs = outgoingDur,
                        videoWidth = outgoingW,
                        videoHeight = outgoingH
                    )
                }
                val settings = repository.settingsFlow.first()
                val saved = repository.getSavedProgressForEpisode(targetEpisode)
                val effectiveDur = (saved?.durationMs ?: 0L).coerceAtLeast(targetEpisode.durationMs)
                val resumePos = PlaybackResumeHelper.computeResumePositionMs(
                    positionMs = saved?.positionMs ?: targetEpisode.resumePositionMs,
                    durationMs = effectiveDur,
                    resumeEnabled = settings.resumePlayback
                )
                lastTrackedEpisodeIndex = index
                lastKnownDurationForTrackedEpisodeMs = effectiveDur
                _positionMs.value = resumePos
                _state.update {
                    it.copy(
                        currentEpisodeIndex = index,
                        currentPositionMs = resumePos,
                        durationMs = effectiveDur,
                        chapters = emptyList(),
                        resumedFromPositionMs = resumePos.takeIf { p -> p > 0L },
                        resumeEventId = System.currentTimeMillis(),
                        abLoopStartMs = null,
                        abLoopEndMs = null,
                        errorMessage = null
                    )
                }
                player.seekTo(index, resumePos)
                player.playWhenReady = true
                if (PlaybackResumeHelper.isMediaFileExists(appContext, targetEpisode.uri) ||
                    !PlaybackResumeHelper.isLocalFileUri(targetEpisode.uri)
                ) {
                    repository.savePlaybackProgress(
                        episode = targetEpisode,
                        positionMs = resumePos,
                        durationMs = effectiveDur
                    )
                }
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        basePlaybackSpeed = clamped
        _exoPlayer?.playbackParameters = PlaybackParameters(clamped)
        _state.update {
            it.copy(
                playbackSpeed = clamped,
                isTempSpeedBoostActive = false
            )
        }
    }

    fun setTemporarySpeedBoost(active: Boolean, boostSpeed: Float = 2.0f) {
        val player = _exoPlayer ?: return
        if (active) {
            if (!_state.value.isTempSpeedBoostActive) {
                basePlaybackSpeed = _state.value.playbackSpeed
            }
            val target = boostSpeed.coerceIn(1.25f, 3.0f)
            player.playbackParameters = PlaybackParameters(target)
            _state.update {
                it.copy(
                    playbackSpeed = target,
                    isTempSpeedBoostActive = true
                )
            }
        } else if (_state.value.isTempSpeedBoostActive) {
            player.playbackParameters = PlaybackParameters(basePlaybackSpeed)
            _state.update {
                it.copy(
                    playbackSpeed = basePlaybackSpeed,
                    isTempSpeedBoostActive = false
                )
            }
        }
    }

    fun setAudioBoostPercent(percent: Int) {
        val applied = audioBoostController.setBoostPercent(percent)
        _state.update { it.copy(audioBoostPercent = applied) }
    }

    fun adjustAudioDelayMs(deltaMs: Long) {
        setAudioDelayMs(manualAudioDelayMs + deltaMs)
    }

    fun setAudioDelayMs(delayMs: Long) {
        manualAudioDelayMs = AudioDelayProcessor.clampDelayMs(delayMs)
        recalculateEffectiveAudioDelay(resyncIfPlaying = false)
    }

    fun refreshBluetoothAudioRoute() {
        bluetoothSyncController.refreshRouteState()
        recalculateEffectiveAudioDelay(resyncIfPlaying = false)
    }

    private fun recalculateEffectiveAudioDelay(resyncIfPlaying: Boolean) {
        val route = bluetoothSyncController.currentRouteState
        val btApplied = BluetoothAudioSyncController.computeAutoBluetoothOffsetMs(
            route = route,
            autoSyncEnabled = bluetoothAutoSyncEnabled,
            userConfiguredA2dpOffsetMs = bluetoothConfiguredOffsetMs
        )
        val totalEffective = BluetoothAudioSyncController.computeTotalEffectiveDelayMs(
            manualDelayMs = manualAudioDelayMs,
            route = route,
            autoSyncEnabled = bluetoothAutoSyncEnabled,
            userConfiguredBtOffsetMs = bluetoothConfiguredOffsetMs
        )
        audioDelayProcessor.setDelayMs(totalEffective)
        _state.update {
            it.copy(
                audioDelayMs = manualAudioDelayMs,
                bluetoothAudioRoute = route,
                bluetoothAutoSyncEnabled = bluetoothAutoSyncEnabled,
                bluetoothConfiguredOffsetMs = bluetoothConfiguredOffsetMs,
                bluetoothAppliedOffsetMs = btApplied,
                effectiveAudioDelayMs = totalEffective
            )
        }
        if (resyncIfPlaying && _exoPlayer?.isPlaying == true) {
            engineScope.launch {
                delay(220L)
                if (_exoPlayer?.isPlaying == true) {
                    resyncAudioStream()
                }
            }
        }
    }

    fun resyncAudioStream() {
        val player = _exoPlayer ?: return
        bluetoothSyncController.refreshRouteState()
        recalculateEffectiveAudioDelay(resyncIfPlaying = false)
        audioDelayProcessor.forceResyncFlush()
        val pos = player.currentPosition.coerceAtLeast(0L)
        val idx = player.currentMediaItemIndex.coerceAtLeast(0)
        player.seekTo(idx, pos)
        _positionMs.value = pos
        updatePositionAndSubtitlesNow(forceSyncRuntimePosition = true)
    }

    fun setPlaybackEndMode(mode: PlaybackEndMode) {
        _state.update { it.copy(playbackEndMode = mode) }
        applyPlaybackEndModeToExo(mode)
        engineScope.launch {
            repository.preferencesRepository.updatePlaybackEndMode(mode)
        }
    }

    private fun applyPlaybackEndModeToExo(mode: PlaybackEndMode) {
        val player = _exoPlayer ?: return
        val sleepEndEpisode = _state.value.sleepTimerPreset == SleepTimerPreset.END_OF_EPISODE
        when (mode) {
            PlaybackEndMode.AUTO_NEXT -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.pauseAtEndOfMediaItems = sleepEndEpisode
            }
            PlaybackEndMode.PAUSE_AT_END -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.pauseAtEndOfMediaItems = true
            }
            PlaybackEndMode.REPEAT_ONE -> {
                player.repeatMode = if (sleepEndEpisode) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
                player.pauseAtEndOfMediaItems = sleepEndEpisode
            }
        }
    }

    fun setSleepTimer(preset: SleepTimerPreset) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null

        when {
            preset == SleepTimerPreset.OFF -> {
                clearSleepTimerInternal()
            }

            preset.stopAtEndOfEpisode -> {
                _state.update {
                    it.copy(
                        sleepTimerPreset = preset,
                        sleepTimerRemainingMs = 0L
                    )
                }
                applyPlaybackEndModeToExo(_state.value.playbackEndMode)
            }

            preset.durationMinutes > 0 -> {
                val durationMs = preset.durationMinutes * 60_000L
                sleepDeadlineElapsedMs = System.currentTimeMillis() + durationMs
                _state.update {
                    it.copy(
                        sleepTimerPreset = preset,
                        sleepTimerRemainingMs = durationMs
                    )
                }
                applyPlaybackEndModeToExo(_state.value.playbackEndMode)
                sleepTimerJob = engineScope.launch {
                    while (isActive) {
                        val rem = (sleepDeadlineElapsedMs - System.currentTimeMillis()).coerceAtLeast(0L)
                        _state.update { it.copy(sleepTimerRemainingMs = rem) }
                        if (rem <= 0L) {
                            _exoPlayer?.pause()
                            persistCurrentProgress()
                            clearSleepTimerInternal()
                            break
                        }
                        delay(1_000L)
                    }
                }
            }
        }
    }

    private fun clearSleepTimerInternal() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _state.update {
            it.copy(
                sleepTimerPreset = SleepTimerPreset.OFF,
                sleepTimerRemainingMs = 0L
            )
        }
        applyPlaybackEndModeToExo(_state.value.playbackEndMode)
    }

    fun markAbLoopPointA() {
        val pos = (_exoPlayer?.currentPosition ?: _positionMs.value).coerceAtLeast(0L)
        val currentEnd = _state.value.abLoopEndMs
        val validEnd = currentEnd?.takeIf { it > pos + 500L }
        _state.update {
            it.copy(
                abLoopStartMs = pos,
                abLoopEndMs = validEnd
            )
        }
    }

    fun markAbLoopPointB() {
        val pos = (_exoPlayer?.currentPosition ?: _positionMs.value).coerceAtLeast(0L)
        val start = _state.value.abLoopStartMs ?: 0L
        if (pos > start + 500L) {
            _state.update {
                it.copy(
                    abLoopStartMs = start,
                    abLoopEndMs = pos
                )
            }
        }
    }

    fun clearAbLoop() {
        _state.update {
            it.copy(
                abLoopStartMs = null,
                abLoopEndMs = null
            )
        }
    }

    fun pauseAndPersist() {
        val player = _exoPlayer ?: return
        setTemporarySpeedBoost(false)
        stopPositionTicker()
        player.pause()
        persistCurrentProgress()
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = engineScope.launch {
            while (isActive) {
                val player = _exoPlayer
                if (player == null || !player.isPlaying) {
                    break
                }
                updatePositionAndSubtitlesNow(forceSyncRuntimePosition = false)
                lastSavedTickCount++
                if (lastSavedTickCount >= 12) {
                    lastSavedTickCount = 0
                    persistCurrentProgress()
                }
                delay(80L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    private fun updatePositionAndSubtitlesNow(forceSyncRuntimePosition: Boolean = false) {
        val player = _exoPlayer ?: return
        var pos = player.currentPosition.coerceAtLeast(0L)
        val currentState = _state.value
        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: currentState.durationMs

        // Enforce A-B segment loop when both A and B markers are set
        val loopA = currentState.abLoopStartMs
        val loopB = currentState.abLoopEndMs
        if (loopA != null && loopB != null && loopB > loopA && pos >= loopB) {
            player.seekTo(loopA)
            pos = loopA
        }

        _positionMs.value = pos

        val extDoc = activeExternalSubtitleDoc
        val cues = if (extDoc != null) {
            extDoc.activeCuesAt(pos, currentState.subtitleDelayMs)
        } else if (currentState.selectedSubtitleId.startsWith("emb:")) {
            latestEmbeddedCues
        } else {
            emptyList()
        }

        if (forceSyncRuntimePosition ||
            dur != currentState.durationMs ||
            cues != currentState.activeSubtitleCues
        ) {
            _state.update {
                it.copy(
                    currentPositionMs = pos,
                    durationMs = dur,
                    activeSubtitleCues = cues
                )
            }
        }
    }

    fun persistCurrentProgress() {
        val player = _exoPlayer ?: return
        val currentState = _state.value
        if (currentState.errorMessage != null) return
        val ep = currentState.currentEpisode ?: return
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: currentState.durationMs
        _positionMs.value = pos
        if (dur <= 0L && pos <= 0L) return
        engineScope.launch {
            if (PlaybackResumeHelper.isLocalFileUri(ep.uri) &&
                !PlaybackResumeHelper.isMediaFileExists(appContext, ep.uri)
            ) {
                repository.refreshMediaAvailabilityAsync()
                return@launch
            }
            repository.savePlaybackProgress(
                episode = ep,
                positionMs = pos,
                durationMs = dur,
                videoWidth = detectedVideoWidth,
                videoHeight = detectedVideoHeight
            )
        }
    }

    companion object {
        @Volatile
        private var instance: PlayerEngine? = null

        fun getInstance(context: Context): PlayerEngine {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val app = context.applicationContext
                    PlayerEngine(app, LibraryRepository.getInstance(app)).also { instance = it }
                }
            }
        }
    }
}
