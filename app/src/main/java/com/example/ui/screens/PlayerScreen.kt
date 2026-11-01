package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.anime4k.Anime4KVideoSurface
import com.example.model.Anime4KPreset
import com.example.model.DecoderMode
import com.example.model.FrameInterpolationMode
import com.example.model.PlaybackEndMode
import com.example.model.PlayerControlsTheme
import com.example.model.PlayerOrientationMode
import com.example.model.PlayerQuickAction
import com.example.model.PlayerSettings
import com.example.model.SleepTimerPreset
import com.example.model.VideoChapter
import com.example.model.VideoScaleMode
import com.example.player.AudioTrackUiOption
import com.example.player.PipController
import com.example.player.PlaybackRuntimeState
import com.example.player.RefreshRatePlan
import com.example.player.SubtitleTrackUiOption
import com.example.ui.components.SubtitleOverlay
import com.example.ui.components.ThemedPlayerCenterTransport
import com.example.ui.components.ThemedPlayerSeekBar
import com.example.ui.components.formatTimeMs
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.localizedTitle
import com.example.ui.i18n.tr
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private enum class ActivePlayerSheet {
    NONE,
    SPEED,
    AUDIO,
    SUBTITLES,
    FRAME_INTERPOLATION,
    ANIME4K,
    DECODER,
    EPISODES,
    CHAPTERS,
    AUDIO_BOOST,
    SLEEP_TIMER,
    SKIP_INTRO_CONFIG,
    TOOLS,
    CUSTOMIZE_QUICK_ACTIONS
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    playbackState: PlaybackRuntimeState,
    positionMsFlow: StateFlow<Long> = MutableStateFlow(playbackState.currentPositionMs),
    settings: PlayerSettings,
    targetDisplayHz: Float = 120f,
    refreshPlan: RefreshRatePlan = RefreshRatePlan(targetDisplayHz = targetDisplayHz, targetRenderFps = targetDisplayHz),
    isInPipMode: Boolean,
    scaleMode: VideoScaleMode,
    splitCompareEnabled: Boolean,
    splitPosition: Float,
    onSurfaceReady: (Surface) -> Unit,
    onSurfaceDestroyed: (Surface) -> Unit,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekByMs: (Long) -> Unit,
    onSeekToMs: (Long) -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onSelectEpisodeIndex: (Int) -> Unit,
    onSelectChapter: (VideoChapter) -> Unit = {},
    onPreviousChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onSelectAudio: (AudioTrackUiOption) -> Unit,
    onPickExternalAudio: () -> Unit,
    onSetAudioBoostPercent: (Int) -> Unit,
    onAdjustAudioDelay: (Long) -> Unit = {},
    onSetAudioDelay: (Long) -> Unit = {},
    onResyncAudio: () -> Unit = {},
    onToggleBluetoothAutoSync: (Boolean) -> Unit = {},
    onSelectSubtitle: (SubtitleTrackUiOption) -> Unit,
    onPickExternalSubtitle: () -> Unit,
    onAdjustSubtitleDelay: (Long) -> Unit,
    onSetSubtitleDelay: (Long) -> Unit,
    onSetSubtitleFontSize: (Float) -> Unit,
    onSetSubtitleColor: (Long) -> Unit,
    onSetSubtitleBg: (Boolean) -> Unit,
    onToggleHighRefreshRate: (Boolean) -> Unit = {},
    onToggleLtpoAdaptive: (Boolean) -> Unit = {},
    onToggleFrameInterpolation: (Boolean) -> Unit = {},
    onSelectFrameInterpolationMode: (FrameInterpolationMode) -> Unit = {},
    onToggleAnime4KEnabled: (Boolean) -> Unit,
    onSelectAnime4KPreset: (Anime4KPreset) -> Unit,
    onChangeAnime4KStrength: (Float) -> Unit,
    onToggleSplitCompare: (Boolean) -> Unit,
    onChangeSplitPos: (Float) -> Unit,
    onSelectDecoderMode: (DecoderMode) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSetTempSpeedBoost: (Boolean) -> Unit,
    onSelectScaleMode: (VideoScaleMode) -> Unit,
    onSetSleepTimer: (SleepTimerPreset) -> Unit,
    onMarkAbPointA: () -> Unit,
    onMarkAbPointB: () -> Unit,
    onClearAbLoop: () -> Unit,
    onSelectPlaybackEndMode: (PlaybackEndMode) -> Unit,
    onSelectControlsTheme: (PlayerControlsTheme) -> Unit = {},
    onUpdateQuickActions: (List<PlayerQuickAction>) -> Unit,
    onUpdateSkipIntroSeconds: (Int) -> Unit = {},
    onUpdatePlayerUiScalePercent: (Int) -> Unit = {},
    onUpdatePlayerAutoDpiScale: (Boolean) -> Unit = {},
    onUpdateControlsAutoHideSeconds: (Int) -> Unit = {},
    onDismissResumeBanner: () -> Unit,
    onRestartFromBeginning: () -> Unit,
    onUiActiveChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var isScreenLocked by rememberSaveable { mutableStateOf(false) }
    var lockedPillVisible by remember { mutableStateOf(false) }
    var orientationMode by rememberSaveable(settings.defaultPlayerOrientation) {
        mutableStateOf(settings.defaultPlayerOrientation)
    }
    var activeSheet by remember { mutableStateOf(ActivePlayerSheet.NONE) }
    var interactionTick by remember { mutableIntStateOf(0) }

    // Intercept Back: when locked, show unlock pill instead of accidentally exiting player!
    BackHandler(enabled = !isInPipMode) {
        if (isScreenLocked) {
            lockedPillVisible = true
            interactionTick++
        } else {
            onBack()
        }
    }

    // Cumulative Double-Tap Seek State (±10s, ±20s, ±30s)
    var doubleTapAccumulatedSec by remember { mutableIntStateOf(0) }
    var doubleTapFeedbackTick by remember { mutableIntStateOf(0) }

    // Horizontal Swipe Seek Gesture State
    var isSwipeSeeking by remember { mutableStateOf(false) }
    var swipeStartPositionMs by remember { mutableLongStateOf(0L) }
    var swipeTargetPositionMs by remember { mutableLongStateOf(0L) }
    var swipeAccumulatedPx by remember { mutableFloatStateOf(0f) }

    // Vertical Brightness / Volume Swipe Gesture State
    var verticalGestureMode by remember { mutableStateOf(VerticalGestureMode.NONE) }
    var verticalGestureFraction by remember { mutableFloatStateOf(0.5f) }

    // Live state references so pointerInput lambdas always read current values without recomposing
    val latestPlaybackState by rememberUpdatedState(playbackState)
    val latestPositionFlow by rememberUpdatedState(positionMsFlow)
    val latestSettings by rememberUpdatedState(settings)
    val latestOnSeekByMs by rememberUpdatedState(onSeekByMs)
    val latestOnSeekToMs by rememberUpdatedState(onSeekToMs)
    val latestOnSetTempSpeedBoost by rememberUpdatedState(onSetTempSpeedBoost)

    // Timeline slider scrubbing & bottom quick bar scrolling state
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubValueMs by remember { mutableFloatStateOf(0f) }
    var isQuickBarScrolling by remember { mutableStateOf(false) }

    val isUiActive = controlsVisible ||
        activeSheet != ActivePlayerSheet.NONE ||
        isScrubbing ||
        isSwipeSeeking ||
        isQuickBarScrolling ||
        verticalGestureMode != VerticalGestureMode.NONE ||
        playbackState.isTempSpeedBoostActive

    LaunchedEffect(isUiActive) {
        onUiActiveChanged(isUiActive)
    }

    // Screen keep-awake while playing
    DisposableEffect(playbackState.isPlaying) {
        view.keepScreenOn = playbackState.isPlaying
        onDispose { view.keepScreenOn = false }
    }

    // Apply & dynamically update Activity screen orientation based on video aspect ratio
    DisposableEffect(orientationMode, playbackState.videoWidth, playbackState.videoHeight) {
        val activity = context as? Activity
        activity?.requestedOrientation = orientationMode.resolveActivityOrientation(
            videoWidth = playbackState.videoWidth,
            videoHeight = playbackState.videoHeight
        )
        onDispose {}
    }

    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = lp
            }
            latestOnSetTempSpeedBoost(false)
        }
    }

    // Fullscreen immersive mode: keep system bars permanently hidden while the player is active.
    // Toggling system bars on tap causes heavy WindowManager layout stalls and visible screen freezes.
    DisposableEffect(isInPipMode) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (!isInPipMode) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val windowRestore = (context as? Activity)?.window
            if (windowRestore != null) {
                WindowCompat.getInsetsController(windowRestore, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val autoHideTimeoutMs = settings.controlsAutoHideSeconds.coerceIn(2, 15) * 1000L
    LaunchedEffect(
        controlsVisible,
        playbackState.isPlaying,
        activeSheet,
        interactionTick,
        isScrubbing,
        isSwipeSeeking,
        isQuickBarScrolling,
        verticalGestureMode,
        autoHideTimeoutMs
    ) {
        if (controlsVisible &&
            !isScreenLocked &&
            playbackState.isPlaying &&
            activeSheet == ActivePlayerSheet.NONE &&
            !isScrubbing &&
            !isSwipeSeeking &&
            !isQuickBarScrolling &&
            verticalGestureMode == VerticalGestureMode.NONE
        ) {
            delay(autoHideTimeoutMs)
            controlsVisible = false
        }
    }

    LaunchedEffect(isScreenLocked, lockedPillVisible, interactionTick) {
        if (isScreenLocked && lockedPillVisible) {
            delay(3200L)
            lockedPillVisible = false
        }
    }

    LaunchedEffect(doubleTapFeedbackTick) {
        if (doubleTapAccumulatedSec != 0) {
            delay(750L)
            doubleTapAccumulatedSec = 0
        }
    }

    LaunchedEffect(playbackState.resumeEventId, playbackState.resumedFromPositionMs) {
        if (playbackState.resumedFromPositionMs != null) {
            delay(5500L)
            onDismissResumeBanner()
        }
    }

    val pipSupported = remember(context) { PipController.isPipSupported(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // 1) Tap / Double-Tap ±10s / Long-Press 2.0x Speed Boost (or Unlock toggle when locked)
            .pointerInput(isInPipMode, isScreenLocked) {
                if (isInPipMode) return@pointerInput
                if (isScreenLocked) {
                    detectTapGestures(
                        onTap = {
                            lockedPillVisible = !lockedPillVisible
                            interactionTick++
                        }
                    )
                    return@pointerInput
                }

                var boostTriggeredThisPress = false
                detectTapGestures(
                    onPress = {
                        boostTriggeredThisPress = false
                        try {
                            tryAwaitRelease()
                        } finally {
                            if (boostTriggeredThisPress) {
                                boostTriggeredThisPress = false
                                latestOnSetTempSpeedBoost(false)
                            }
                        }
                    },
                    onTap = {
                        controlsVisible = !controlsVisible
                        interactionTick++
                    },
                    onDoubleTap = { offset ->
                        if (!latestSettings.gestureDoubleTapSeek) return@detectTapGestures
                        val stepSec = latestSettings.seekStepSeconds.coerceAtLeast(3)
                        val stepMs = stepSec * 1000L
                        val isLeftHalf = offset.x < size.width / 2f
                        if (isLeftHalf) {
                            latestOnSeekByMs(-stepMs)
                            doubleTapAccumulatedSec = if (doubleTapAccumulatedSec < 0) {
                                doubleTapAccumulatedSec - stepSec
                            } else {
                                -stepSec
                            }
                        } else {
                            latestOnSeekByMs(stepMs)
                            doubleTapAccumulatedSec = if (doubleTapAccumulatedSec > 0) {
                                doubleTapAccumulatedSec + stepSec
                            } else {
                                stepSec
                            }
                        }
                        doubleTapFeedbackTick++
                        interactionTick++
                    },
                    onLongPress = {
                        if (latestSettings.gestureLongPress2x && latestPlaybackState.isPlaying) {
                            boostTriggeredThisPress = true
                            latestOnSetTempSpeedBoost(true)
                        }
                    }
                )
            }
            // 2) Horizontal Drag/Swipe Seek Gesture across screen
            .pointerInput(isInPipMode, isScreenLocked) {
                if (isInPipMode || isScreenLocked) return@pointerInput
                var ignoreEdgeSwipe = false
                detectHorizontalDragGestures(
                    onDragStart = { startOffset ->
                        if (!latestSettings.gestureHorizontalSeek) {
                            ignoreEdgeSwipe = true
                            return@detectHorizontalDragGestures
                        }
                        val edgeGuardPx = 24.dp.toPx()
                        ignoreEdgeSwipe = startOffset.x < edgeGuardPx ||
                            startOffset.x > (size.width - edgeGuardPx)
                        if (!ignoreEdgeSwipe) {
                            val currentPos = latestPositionFlow.value
                                .coerceAtLeast(latestPlaybackState.currentPositionMs)
                                .coerceAtLeast(0L)
                            isSwipeSeeking = true
                            swipeStartPositionMs = currentPos
                            swipeTargetPositionMs = currentPos
                            swipeAccumulatedPx = 0f
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (!ignoreEdgeSwipe && isSwipeSeeking && size.width > 0) {
                            change.consume()
                            swipeAccumulatedPx += dragAmount
                            val liveDuration = latestPlaybackState.durationMs
                            val spanMs = if (liveDuration in 1L..90_000L) {
                                liveDuration
                            } else {
                                90_000L
                            }
                            val deltaMs = ((swipeAccumulatedPx / size.width.toFloat()) * spanMs.toFloat()).toLong()
                            val maxDur = liveDuration.takeIf { it > 0L }
                                ?: (swipeStartPositionMs + 3_600_000L)
                            swipeTargetPositionMs = (swipeStartPositionMs + deltaMs).coerceIn(0L, maxDur)
                        }
                    },
                    onDragEnd = {
                        if (isSwipeSeeking) {
                            latestOnSeekToMs(swipeTargetPositionMs)
                            isSwipeSeeking = false
                            interactionTick++
                        }
                        ignoreEdgeSwipe = false
                    },
                    onDragCancel = {
                        isSwipeSeeking = false
                        ignoreEdgeSwipe = false
                    }
                )
            }
            // 3) Vertical Drag Gesture: Left Half = Screen Brightness, Right Half = Media Volume
            .pointerInput(isInPipMode, isScreenLocked) {
                if (isInPipMode || isScreenLocked) return@pointerInput
                var ignoreVerticalSwipe = false
                detectVerticalDragGestures(
                    onDragStart = { startOffset ->
                        if (!latestSettings.gesturesBrightnessVolume) {
                            ignoreVerticalSwipe = true
                            return@detectVerticalDragGestures
                        }
                        val edgeGuardPx = 28.dp.toPx()
                        ignoreVerticalSwipe = startOffset.x < edgeGuardPx ||
                            startOffset.x > (size.width - edgeGuardPx) ||
                            startOffset.y < edgeGuardPx ||
                            startOffset.y > (size.height - edgeGuardPx)

                        if (!ignoreVerticalSwipe && size.width > 0 && size.height > 0) {
                            val isLeftHalf = startOffset.x < size.width / 2f
                            if (isLeftHalf) {
                                val win = (context as? Activity)?.window
                                val currentWinBright = win?.attributes?.screenBrightness ?: -1f
                                val initBright = if (currentWinBright in 0.01f..1.0f) {
                                    currentWinBright
                                } else {
                                    runCatching {
                                        Settings.System.getInt(
                                            context.contentResolver,
                                            Settings.System.SCREEN_BRIGHTNESS
                                        ) / 255f
                                    }.getOrDefault(0.55f).coerceIn(0.05f, 1.0f)
                                }
                                verticalGestureFraction = initBright
                                verticalGestureMode = VerticalGestureMode.BRIGHTNESS
                            } else {
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                verticalGestureFraction = (curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
                                verticalGestureMode = VerticalGestureMode.VOLUME
                            }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (!ignoreVerticalSwipe && verticalGestureMode != VerticalGestureMode.NONE && size.height > 0) {
                            change.consume()
                            val delta = -dragAmount / (size.height * 0.72f)
                            if (verticalGestureMode == VerticalGestureMode.BRIGHTNESS) {
                                val nextB = (verticalGestureFraction + delta).coerceIn(0.01f, 1.0f)
                                verticalGestureFraction = nextB
                                (context as? Activity)?.window?.let { win ->
                                    val lp = win.attributes
                                    lp.screenBrightness = nextB
                                    win.attributes = lp
                                }
                            } else if (verticalGestureMode == VerticalGestureMode.VOLUME) {
                                val nextV = (verticalGestureFraction + delta).coerceIn(0f, 1f)
                                verticalGestureFraction = nextV
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                val targetIndex = (nextV * maxVol).roundToInt().coerceIn(0, maxVol)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, 0)
                            }
                        }
                    },
                    onDragEnd = {
                        verticalGestureMode = VerticalGestureMode.NONE
                        ignoreVerticalSwipe = false
                        interactionTick++
                    },
                    onDragCancel = {
                        verticalGestureMode = VerticalGestureMode.NONE
                        ignoreVerticalSwipe = false
                    }
                )
            }
            .testTag("video_player_stage")
    ) {
        // 1. Real-time Hardware Decoder + GPU MEMC Frame Interpolation + Anime4K OpenGL ES 2.0 Surface
        val hasVisualProcessing = settings.effectiveAnime4KPreset != Anime4KPreset.OFF ||
            settings.effectiveFrameInterpolationMode.isEnabled
        Anime4KVideoSurface(
            videoWidth = playbackState.videoWidth,
            videoHeight = playbackState.videoHeight,
            preset = settings.effectiveAnime4KPreset,
            strength = settings.anime4kStrength,
            interpolationMode = settings.effectiveFrameInterpolationMode,
            isPlaying = playbackState.isPlaying,
            sourceFrameRate = playbackState.sourceFrameRate,
            playbackSpeed = playbackState.playbackSpeed,
            highRefreshRateEnabled = settings.highRefreshRateEnabled,
            targetDisplayHz = refreshPlan.targetDisplayHz,
            targetRenderFps = refreshPlan.targetRenderFps,
            surfaceFrameRateHz = refreshPlan.surfaceFrameRateHz,
            frameRateCompatibility = refreshPlan.frameRateCompatibility,
            memcActive = refreshPlan.memcActive,
            splitCompareEnabled = splitCompareEnabled && hasVisualProcessing,
            splitPosition = splitPosition,
            scaleMode = scaleMode,
            onSurfaceReady = onSurfaceReady,
            onSurfaceDestroyed = onSurfaceDestroyed,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Real-time SRT / ASS / Embedded Subtitle Renderer
        SubtitleOverlay(
            cues = playbackState.activeSubtitleCues,
            fontSizeSp = settings.subtitleFontSizeSp,
            colorArgb = settings.subtitleColorArgb,
            showBackground = settings.subtitleBackgroundEnabled,
            isInPipMode = isInPipMode,
            bottomControlsPadding = controlsVisible && !isScreenLocked && !isInPipMode,
            modifier = Modifier.fillMaxSize()
        )

        // Split-Screen Comparison Corner Badges
        if (splitCompareEnabled && hasVisualProcessing && !isInPipMode) {
            val afterBadge = buildString {
                if (settings.effectiveFrameInterpolationMode.isEnabled) {
                    append(settings.effectiveFrameInterpolationMode.badgeLabel)
                }
                if (settings.effectiveAnime4KPreset != Anime4KPreset.OFF) {
                    if (isNotEmpty()) append(" + ")
                    append(settings.effectiveAnime4KPreset.badgeLabel)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 20.dp, vertical = 68.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(color = Color.Black.copy(alpha = 0.78f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = tr("ДО: ОРИГИНАЛ", "BEFORE: ORIGINAL"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = tr("ПОСЛЕ: $afterBadge", "AFTER: $afterBadge"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Long-Press Speed Boost Top Floating Pill
        AnimatedVisibility(
            visible = playbackState.isTempSpeedBoostActive && !isInPipMode,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(100)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 18.dp)
        ) {
            LongPressSpeedPill(speed = playbackState.playbackSpeed)
        }

        // Auto-Dismissing Resume Playback Floating Pill ("Продолжено с MM:SS" + "С начала")
        AnimatedVisibility(
            visible = playbackState.resumedFromPositionMs != null &&
                !isInPipMode &&
                !isScreenLocked &&
                !playbackState.isTempSpeedBoostActive,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(100)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 56.dp, start = 14.dp, end = 14.dp)
        ) {
            val resumedPos = playbackState.resumedFromPositionMs ?: 0L
            Surface(
                color = Color(0xFF0E131F).copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)),
                modifier = Modifier.testTag("player_resumed_banner")
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = tr("Продолжено с ${formatTimeMs(resumedPos)}", "Resumed from ${formatTimeMs(resumedPos)}"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onRestartFromBeginning()
                                interactionTick++
                            }
                            .testTag("player_restart_from_zero_button")
                    ) {
                        Text(
                            text = tr("С начала", "From start"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismissResumeBanner,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = tr("Скрыть уведомление", "Dismiss banner"),
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Buffering Spinner
        if (playbackState.isBuffering &&
            !isInPipMode &&
            !isSwipeSeeking &&
            verticalGestureMode == VerticalGestureMode.NONE
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Vertical Brightness / Volume Gesture Live OLED HUD
        AnimatedVisibility(
            visible = verticalGestureMode != VerticalGestureMode.NONE && !isInPipMode,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(100)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            VerticalGestureHudCard(
                mode = verticalGestureMode,
                fraction = verticalGestureFraction
            )
        }

        // Horizontal Swipe Seek Gesture Live OLED HUD
        AnimatedVisibility(
            visible = isSwipeSeeking && !isInPipMode,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(100)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val deltaMs = swipeTargetPositionMs - swipeStartPositionMs
            val isForward = deltaMs >= 0L
            val deltaSign = if (isForward) "+" else "-"
            val absDeltaSec = abs(deltaMs) / 1000L
            val progressFrac = if (playbackState.durationMs > 0L) {
                (swipeTargetPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            Surface(
                color = Color(0xFF0E131F).copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)),
                modifier = Modifier
                    .width(190.dp)
                    .testTag("swipe_seek_hud")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = tr("${deltaSign}${absDeltaSec} сек", "${deltaSign}${absDeltaSec} s"),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${formatTimeMs(swipeTargetPositionMs)} / ${formatTimeMs(playbackState.durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    if (playbackState.durationMs > 0L) {
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { progressFrac },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Double-Tap ±10s Feedback Pill (Clean frosted glass capsule with vector icon, no emoji)
        AnimatedVisibility(
            visible = doubleTapAccumulatedSec != 0 &&
                !isSwipeSeeking &&
                verticalGestureMode == VerticalGestureMode.NONE &&
                !isInPipMode,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(100)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val isForward = doubleTapAccumulatedSec > 0
            val absSec = abs(doubleTapAccumulatedSec)
            val signPrefix = if (isForward) "+" else "-"
            Surface(
                color = Color(0xFF0E131F).copy(alpha = 0.65f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)),
                modifier = Modifier.testTag("double_tap_seek_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isForward) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = tr("$signPrefix$absSec сек", "$signPrefix$absSec s"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (isForward) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Locked Screen Floating Unlock Pill
        AnimatedVisibility(
            visible = isScreenLocked && lockedPillVisible && !isInPipMode,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(100)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = 28.dp)
        ) {
            ScreenLockFloatingPill(
                onUnlock = {
                    isScreenLocked = false
                    lockedPillVisible = false
                    controlsVisible = true
                    interactionTick++
                }
            )
        }

        // Error Banner
        playbackState.errorMessage?.let { err ->
            if (!isInPipMode) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp, start = 24.dp, end = 24.dp)
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // 3. Full Player HUD (fast 100ms lightweight graphicsLayer transition, eliminates frame drops & freeze)
        val showHud = controlsVisible &&
            !isScreenLocked &&
            !isInPipMode &&
            !isSwipeSeeking &&
            verticalGestureMode == VerticalGestureMode.NONE &&
            !playbackState.isTempSpeedBoostActive

        val hudAlphaAnim = remember { Animatable(if (showHud) 1f else 0f) }
        LaunchedEffect(showHud) {
            hudAlphaAnim.animateTo(
                targetValue = if (showHud) 1f else 0f,
                animationSpec = tween(durationMillis = 80, easing = LinearOutSlowInEasing)
            )
        }

        if (showHud || hudAlphaAnim.value > 0.001f) {
            val isEnglish = LocalIsEnglish.current
            val currentConfigOrientation = LocalConfiguration.current.orientation
            val toggleOrientationOneTap: () -> Unit = {
                val currentlyHorizontal = when (orientationMode) {
                    PlayerOrientationMode.LANDSCAPE -> true
                    PlayerOrientationMode.PORTRAIT -> false
                    PlayerOrientationMode.VIDEO_ASPECT -> {
                        if (playbackState.videoWidth > 0 && playbackState.videoHeight > 0) {
                            playbackState.videoWidth >= playbackState.videoHeight
                        } else {
                            currentConfigOrientation != android.content.res.Configuration.ORIENTATION_PORTRAIT
                        }
                    }
                    PlayerOrientationMode.AUTO -> {
                        currentConfigOrientation != android.content.res.Configuration.ORIENTATION_PORTRAIT
                    }
                }
                orientationMode = orientationMode.toggleLandscapePortrait(currentlyHorizontal)
                interactionTick++
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = hudAlphaAnim.value
                    }
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.82f),
                            0.28f to Color.Transparent,
                            0.68f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.92f)
                        )
                    )
                    .windowInsetsPadding(WindowInsets.displayCutout)
            ) {
                // TOP BAR (compact, grouped controls + resolution badge + direct Episodes access)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Закрыть плеер", "Close player"),
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playbackState.currentEpisode?.cleanTitle ?: tr("Воспроизведение", "Now Playing"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subInfo = buildString {
                            playbackState.currentEpisode?.folderName?.let { append(it) }
                            if (playbackState.playlist.size > 1 && !settings.showEpisodesButtonInHud) {
                                if (isEnglish) {
                                    append(" • Episode ${playbackState.currentEpisodeIndex + 1}/${playbackState.playlist.size}")
                                } else {
                                    append(" • Серия ${playbackState.currentEpisodeIndex + 1}/${playbackState.playlist.size}")
                                }
                            }
                        }
                        if (subInfo.isNotEmpty()) {
                            Text(
                                text = subInfo,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val accentFreeHud = settings.playerControlsTheme.isAccentFree
                    val framelessHud = settings.playerControlsTheme.isFrameless
                    val hudAccentColor = if (accentFreeHud) Color.White.copy(alpha = 0.90f) else MaterialTheme.colorScheme.primary
                    val hudPillBorder = when {
                        framelessHud -> null
                        accentFreeHud -> BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    }
                    val hudPillBg = if (framelessHud) Color.Transparent else Color.White.copy(alpha = 0.14f)

                    // Clear video resolution badge (4K, 1080p, 720p, etc.)
                    val resolutionBadge = remember(
                        playbackState.videoWidth,
                        playbackState.videoHeight,
                        playbackState.currentEpisode?.resolutionBadge
                    ) {
                        com.example.model.VideoResolutionFormatter.badgeFromDimensions(
                            playbackState.videoWidth,
                            playbackState.videoHeight
                        ) ?: playbackState.currentEpisode?.resolutionBadge
                    }
                    if (resolutionBadge != null) {
                        com.example.ui.components.ResolutionBadgeChip(
                            badge = resolutionBadge,
                            modifier = Modifier.testTag("player_resolution_badge")
                        )
                    }

                    // Direct immediate access to Episodes in Player Top Bar
                    if (settings.showEpisodesButtonInHud) {
                        val epButtonLabel = if (playbackState.playlist.size > 1) {
                            tr(
                                "Серии ${playbackState.currentEpisodeIndex + 1}/${playbackState.playlist.size}",
                                "Episodes ${playbackState.currentEpisodeIndex + 1}/${playbackState.playlist.size}"
                            )
                        } else {
                            tr("Серии", "Episodes")
                        }
                        Surface(
                            color = hudPillBg,
                            shape = RoundedCornerShape(10.dp),
                            border = hudPillBorder,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    interactionTick++
                                    activeSheet = ActivePlayerSheet.EPISODES
                                }
                                .testTag("player_top_episodes_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = epButtonLabel,
                                    tint = hudAccentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = epButtonLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Grouped compact top-right utility buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        IconButton(
                            onClick = toggleOrientationOneTap,
                            modifier = Modifier.testTag("player_orientation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = tr(
                                    "Ориентация экрана: ${orientationMode.title}",
                                    "Screen orientation: ${orientationMode.localizedTitle(true)}"
                                ),
                                tint = if (orientationMode != PlayerOrientationMode.AUTO && !accentFreeHud) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White
                                }
                            )
                        }

                        IconButton(
                            onClick = {
                                isScreenLocked = true
                                lockedPillVisible = true
                                controlsVisible = false
                            },
                            modifier = Modifier.testTag("player_lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = tr(
                                    "Заблокировать экран от случайных нажатий",
                                    "Lock screen against accidental touches"
                                ),
                                tint = Color.White
                            )
                        }

                        if (pipSupported) {
                            IconButton(
                                onClick = onEnterPip,
                                modifier = Modifier.testTag("player_enter_pip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = tr(
                                        "Режим Картинка в картинке (PiP)",
                                        "Picture-in-Picture (PiP) mode"
                                    ),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // CENTER TRANSPORT (styled by settings.playerControlsTheme)
                val stepSec = settings.seekStepSeconds.coerceAtLeast(3)
                val stepMs = stepSec * 1000L
                ThemedPlayerCenterTransport(
                    controlsTheme = settings.playerControlsTheme,
                    isPlaying = playbackState.isPlaying,
                    hasPlaylist = playbackState.playlist.isNotEmpty(),
                    hasNextEpisode = playbackState.hasNextEpisode,
                    seekStepSeconds = stepSec,
                    onPreviousEpisode = {
                        onPreviousEpisode()
                        interactionTick++
                    },
                    onRewind = {
                        onSeekByMs(-stepMs)
                        interactionTick++
                    },
                    onTogglePlayPause = {
                        onTogglePlayPause()
                        interactionTick++
                    },
                    onForward = {
                        onSeekByMs(stepMs)
                        interactionTick++
                    },
                    onNextEpisode = {
                        onNextEpisode()
                        interactionTick++
                    },
                    modifier = Modifier.align(Alignment.Center)
                )

                // BOTTOM CONTROLS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Isolated Timeline Composable: collects positionMsFlow independently so 4Hz playback
                    // ticks never recompose the entire PlayerScreen, OpenGL Surface, or QuickActionsBar!
                    PlayerTimelineSection(
                        controlsTheme = settings.playerControlsTheme,
                        positionMsFlow = positionMsFlow,
                        durationMs = playbackState.durationMs,
                        chapters = playbackState.chapters,
                        showChapterMarkers = settings.showChapterMarkersOnTimeline,
                        abLoopStartMs = playbackState.abLoopStartMs,
                        abLoopEndMs = playbackState.abLoopEndMs,
                        showSkipIntroButton = settings.showSkipIntroButton,
                        skipIntroSeconds = settings.skipIntroSeconds,
                        isScrubbing = isScrubbing,
                        scrubValueMs = scrubValueMs,
                        isSwipeSeeking = isSwipeSeeking,
                        swipeTargetPositionMs = swipeTargetPositionMs,
                        onScrubChange = { value ->
                            isScrubbing = true
                            scrubValueMs = value
                        },
                        onScrubFinished = {
                            onSeekToMs(scrubValueMs.toLong())
                            isScrubbing = false
                            interactionTick++
                        },
                        onClearAbLoop = {
                            onClearAbLoop()
                            interactionTick++
                        },
                        onSkipIntro = {
                            onSeekByMs(settings.skipIntroSeconds * 1000L)
                            interactionTick++
                        },
                        onLongPressSkipIntro = {
                            interactionTick++
                            activeSheet = ActivePlayerSheet.SKIP_INTRO_CONFIG
                        },
                        onOpenChaptersSheet = {
                            interactionTick++
                            activeSheet = ActivePlayerSheet.CHAPTERS
                        },
                        onPreviousChapter = {
                            onPreviousChapter()
                            interactionTick++
                        },
                        onNextChapter = {
                            onNextChapter()
                            interactionTick++
                        }
                    )

                    PlayerQuickActionsBar(
                        quickActions = settings.quickActions,
                        playbackState = playbackState,
                        settings = settings,
                        scaleMode = scaleMode,
                        orientationMode = orientationMode,
                        pipSupported = pipSupported,
                        onTriggerAction = { action ->
                            interactionTick++
                            when (action) {
                                PlayerQuickAction.SPEED -> activeSheet = ActivePlayerSheet.SPEED
                                PlayerQuickAction.AUDIO -> activeSheet = ActivePlayerSheet.AUDIO
                                PlayerQuickAction.SUBTITLES -> activeSheet = ActivePlayerSheet.SUBTITLES
                                PlayerQuickAction.DECODER -> activeSheet = ActivePlayerSheet.DECODER
                                PlayerQuickAction.FRAME_INTERPOLATION -> activeSheet = ActivePlayerSheet.FRAME_INTERPOLATION
                                PlayerQuickAction.ANIME4K -> activeSheet = ActivePlayerSheet.ANIME4K
                                PlayerQuickAction.EPISODES -> activeSheet = ActivePlayerSheet.EPISODES
                                PlayerQuickAction.CHAPTERS -> activeSheet = ActivePlayerSheet.CHAPTERS
                                PlayerQuickAction.SCALE -> onSelectScaleMode(scaleMode.next())
                                PlayerQuickAction.AUDIO_BOOST -> activeSheet = ActivePlayerSheet.AUDIO
                                PlayerQuickAction.SLEEP_TIMER -> activeSheet = ActivePlayerSheet.SLEEP_TIMER
                                PlayerQuickAction.AB_LOOP -> {
                                    when {
                                        playbackState.abLoopStartMs == null -> onMarkAbPointA()
                                        playbackState.abLoopEndMs == null -> onMarkAbPointB()
                                        else -> onClearAbLoop()
                                    }
                                }
                                PlayerQuickAction.SKIP_INTRO -> onSeekByMs(settings.skipIntroSeconds * 1000L)
                                PlayerQuickAction.ORIENTATION -> toggleOrientationOneTap()
                                PlayerQuickAction.LOCK -> {
                                    isScreenLocked = true
                                    lockedPillVisible = true
                                    controlsVisible = false
                                }
                                PlayerQuickAction.PIP -> {
                                    if (pipSupported) onEnterPip()
                                }
                                PlayerQuickAction.TOOLS -> activeSheet = ActivePlayerSheet.TOOLS
                            }
                        },
                        onLongPressAction = { action ->
                            interactionTick++
                            if (action == PlayerQuickAction.SKIP_INTRO) {
                                activeSheet = ActivePlayerSheet.SKIP_INTRO_CONFIG
                            }
                        },
                        onOpenCustomizer = {
                            interactionTick++
                            activeSheet = ActivePlayerSheet.CUSTOMIZE_QUICK_ACTIONS
                        },
                        onUserInteraction = {
                            interactionTick++
                        },
                        onScrollStateChanged = { scrolling ->
                            isQuickBarScrolling = scrolling
                            if (scrolling) interactionTick++
                        }
                    )
                }
            }
        }
    }

    if (activeSheet != ActivePlayerSheet.NONE) {
        val baseSheetDensity = LocalDensity.current
        val compactSheetDensity = remember(baseSheetDensity) {
            Density(
                density = baseSheetDensity.density * 0.90f,
                fontScale = baseSheetDensity.fontScale * 0.88f
            )
        }
        CompositionLocalProvider(LocalDensity provides compactSheetDensity) {
            when (activeSheet) {
                ActivePlayerSheet.SPEED -> SpeedQuickModalSheet(
                    playbackSpeed = playbackState.playbackSpeed,
                    onSelectSpeed = onSelectSpeed,
                    onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                )

                ActivePlayerSheet.AUDIO -> AudioTracksModalSheet(
                    audioOptions = playbackState.audioTrackOptions,
                    audioBoostPercent = playbackState.audioBoostPercent,
                    audioDelayMs = playbackState.audioDelayMs,
                    effectiveAudioDelayMs = playbackState.effectiveAudioDelayMs,
                    bluetoothRoute = playbackState.bluetoothAudioRoute,
                    bluetoothAutoSyncEnabled = playbackState.bluetoothAutoSyncEnabled,
                    bluetoothAppliedOffsetMs = playbackState.bluetoothAppliedOffsetMs,
                    bluetoothConfiguredOffsetMs = playbackState.bluetoothConfiguredOffsetMs,
                    lastAudioResyncTimestampMs = playbackState.lastAudioResyncTimestampMs,
                    onToggleBluetoothAutoSync = onToggleBluetoothAutoSync,
                    onSelectAudio = onSelectAudio,
                    onSetAudioBoostPercent = onSetAudioBoostPercent,
                    onAdjustAudioDelayMs = onAdjustAudioDelay,
                    onSetAudioDelayMs = onSetAudioDelay,
                    onResyncAudio = onResyncAudio,
                    onPickExternalAudioFile = onPickExternalAudio,
                    onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                )

                ActivePlayerSheet.SUBTITLES -> SubtitleControlModalSheet(
                    subtitleOptions = playbackState.subtitleTrackOptions,
                    subtitleDelayMs = playbackState.subtitleDelayMs,
                    settings = settings,
                    onSelectSubtitle = onSelectSubtitle,
                    onLoadExternalSubtitleFile = onPickExternalSubtitle,
                    onAdjustDelayMs = onAdjustSubtitleDelay,
                    onSetDelayMs = onSetSubtitleDelay,
                    onSetFontSizeSp = onSetSubtitleFontSize,
                    onSetColorArgb = onSetSubtitleColor,
                    onSetBackgroundEnabled = onSetSubtitleBg,
                    onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                )

                ActivePlayerSheet.FRAME_INTERPOLATION -> FrameInterpolationModalSheet(
                    frameInterpolationEnabled = settings.frameInterpolationEnabled,
                    interpolationMode = settings.frameInterpolationMode,
                    highRefreshRateEnabled = settings.highRefreshRateEnabled,
                    ltpoAdaptiveEnabled = settings.ltpoAdaptiveRefreshEnabled,
                    sourceFrameRate = playbackState.sourceFrameRate,
                    targetDisplayHz = targetDisplayHz,
                    refreshPlan = refreshPlan,
                    isAnime4kAutoFastActive = settings.isAnime4kAutoDowngradedForMemc,
                    onToggleFrameInterpolation = onToggleFrameInterpolation,
                    onSelectInterpolationMode = onSelectFrameInterpolationMode,
                    onToggleHighRefreshRate = onToggleHighRefreshRate,
                    onToggleLtpoAdaptive = onToggleLtpoAdaptive,
                    onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                )

                ActivePlayerSheet.ANIME4K -> Anime4KControlModalSheet(
                    settings = settings,
                    splitCompareEnabled = splitCompareEnabled,
                    splitPosition = splitPosition,
                    onToggleEnabled = onToggleAnime4KEnabled,
                    onSelectPreset = onSelectAnime4KPreset,
                    onChangeStrength = onChangeAnime4KStrength,
                    onToggleSplitCompare = onToggleSplitCompare,
                    onChangeSplitPosition = onChangeSplitPos,
                    onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                )

                ActivePlayerSheet.DECODER -> DecoderAndVideoModalSheet(
                    activeDecoderMode = playbackState.activeDecoderMode,
                    activeCodecName = playbackState.activeVideoCodecName,
                    isHardwareAccelerated = playbackState.isHardwareAccelerated,
                    videoWidth = playbackState.videoWidth,
                    videoHeight = playbackState.videoHeight,
                    onSelectDecoderMode = onSelectDecoderMode,
                    onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                )

                ActivePlayerSheet.EPISODES -> EpisodesModalSheet(
                    playlist = playbackState.playlist,
                    currentIndex = playbackState.currentEpisodeIndex,
                    onSelectEpisodeIndex = onSelectEpisodeIndex,
                    onPreviousEpisode = onPreviousEpisode,
                    onNextEpisode = onNextEpisode,
                    onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                )

                ActivePlayerSheet.CHAPTERS -> {
                    val sheetPosMs by positionMsFlow.collectAsStateWithLifecycle()
                    ChaptersModalSheet(
                        chapters = if (settings.enableEmbeddedChapters) playbackState.chapters else emptyList(),
                        currentPositionMs = sheetPosMs,
                        onSelectChapter = onSelectChapter,
                        onPreviousChapter = onPreviousChapter,
                        onNextChapter = onNextChapter,
                        onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                    )
                }

                ActivePlayerSheet.AUDIO_BOOST -> AudioBoostQuickModalSheet(
                    audioBoostPercent = playbackState.audioBoostPercent,
                    onSetAudioBoostPercent = onSetAudioBoostPercent,
                    onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                )

                ActivePlayerSheet.SLEEP_TIMER -> SleepTimerQuickModalSheet(
                    sleepTimerPreset = playbackState.sleepTimerPreset,
                    sleepTimerRemainingMs = playbackState.sleepTimerRemainingMs,
                    onSetSleepTimer = onSetSleepTimer,
                    onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                )

                ActivePlayerSheet.SKIP_INTRO_CONFIG -> SkipIntroConfigModalSheet(
                    skipIntroSeconds = settings.skipIntroSeconds,
                    onUpdateSkipIntroSeconds = onUpdateSkipIntroSeconds,
                    onSkipNow = {
                        onSeekByMs(settings.skipIntroSeconds * 1000L)
                        interactionTick++
                    },
                    onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                )

                ActivePlayerSheet.TOOLS -> {
                    val toolsPosMs by positionMsFlow.collectAsStateWithLifecycle()
                    PlayerToolsModalSheet(
                        playbackSpeed = playbackState.playbackSpeed,
                        scaleMode = scaleMode,
                        audioBoostPercent = playbackState.audioBoostPercent,
                        sleepTimerPreset = playbackState.sleepTimerPreset,
                        sleepTimerRemainingMs = playbackState.sleepTimerRemainingMs,
                        abLoopPointAMs = playbackState.abLoopStartMs,
                        abLoopPointBMs = playbackState.abLoopEndMs,
                        currentPositionMs = toolsPosMs,
                        playbackEndMode = playbackState.playbackEndMode,
                        controlsTheme = settings.playerControlsTheme,
                        activeOrientationMode = orientationMode,
                        playerUiScalePercent = settings.playerUiScalePercent,
                        playerAutoDpiScale = settings.playerAutoDpiScale,
                        controlsAutoHideSeconds = settings.controlsAutoHideSeconds,
                        skipIntroSeconds = settings.skipIntroSeconds,
                        onSelectSpeed = onSelectSpeed,
                        onSelectScaleMode = onSelectScaleMode,
                        onSetAudioBoostPercent = onSetAudioBoostPercent,
                        onSetSleepTimer = onSetSleepTimer,
                        onMarkAbPointA = onMarkAbPointA,
                        onMarkAbPointB = onMarkAbPointB,
                        onClearAbLoop = onClearAbLoop,
                        onSelectPlaybackEndMode = onSelectPlaybackEndMode,
                        onSelectControlsTheme = onSelectControlsTheme,
                        onSelectOrientationMode = { orientationMode = it },
                        onUpdatePlayerUiScalePercent = onUpdatePlayerUiScalePercent,
                        onUpdatePlayerAutoDpiScale = onUpdatePlayerAutoDpiScale,
                        onUpdateControlsAutoHideSeconds = onUpdateControlsAutoHideSeconds,
                        onUpdateSkipIntroSeconds = onUpdateSkipIntroSeconds,
                        onLockControls = {
                            isScreenLocked = true
                            lockedPillVisible = true
                            controlsVisible = false
                        },
                        onOpenQuickActionsCustomizer = {
                            activeSheet = ActivePlayerSheet.CUSTOMIZE_QUICK_ACTIONS
                        },
                        onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                    )
                }

                ActivePlayerSheet.CUSTOMIZE_QUICK_ACTIONS -> QuickActionsCustomizerSheet(
                    selectedActions = settings.quickActions,
                    onUpdateActions = onUpdateQuickActions,
                    onDismiss = { activeSheet = ActivePlayerSheet.NONE }
                )

                ActivePlayerSheet.NONE -> Unit
            }
        }
    }
}

@Composable
internal fun rememberPlayerEffectiveScale(settings: PlayerSettings): Float {
    val config = LocalConfiguration.current
    val autoDpiFactor = remember(config.smallestScreenWidthDp, config.densityDpi, settings.playerAutoDpiScale) {
        if (!settings.playerAutoDpiScale) {
            1.0f
        } else {
            val sw = config.smallestScreenWidthDp
            when {
                sw <= 0 -> 1.0f
                sw < 340 -> 0.88f
                sw < 380 -> 0.94f
                sw in 380..440 -> 1.0f
                sw in 441..599 -> 1.06f
                else -> 1.14f
            }
        }
    }
    val userScale = (settings.playerUiScalePercent.coerceIn(75, 140) / 100f)
    return (autoDpiFactor * userScale).coerceIn(0.68f, 1.55f)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerTimelineSection(
    controlsTheme: PlayerControlsTheme,
    positionMsFlow: StateFlow<Long>,
    durationMs: Long,
    chapters: List<VideoChapter>,
    showChapterMarkers: Boolean,
    abLoopStartMs: Long?,
    abLoopEndMs: Long?,
    showSkipIntroButton: Boolean,
    skipIntroSeconds: Int,
    isScrubbing: Boolean,
    scrubValueMs: Float,
    isSwipeSeeking: Boolean,
    swipeTargetPositionMs: Long,
    onScrubChange: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    onClearAbLoop: () -> Unit,
    onSkipIntro: () -> Unit,
    onLongPressSkipIntro: () -> Unit = {},
    onOpenChaptersSheet: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit
) {
    val livePositionMs by positionMsFlow.collectAsStateWithLifecycle()
    val totalDur = durationMs.coerceAtLeast(1L)
    val displayPos = when {
        isScrubbing -> scrubValueMs.toLong()
        isSwipeSeeking -> swipeTargetPositionMs
        else -> livePositionMs.coerceIn(0L, totalDur)
    }

    val activeChapterIndex = remember(chapters, displayPos) {
        findActiveChapterIndex(chapters, displayPos)
    }
    val activeChapter = chapters.getOrNull(activeChapterIndex)

    if (abLoopStartMs != null || chapters.isNotEmpty() || showSkipIntroButton) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (abLoopStartMs != null) {
                val aStr = formatTimeMs(abLoopStartMs)
                val bStr = abLoopEndMs?.let { formatTimeMs(it) } ?: "…"
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onClearAbLoop)
                        .testTag("player_ab_loop_active_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "A–B: $aStr → $bStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = tr("Сбросить повтор A–B", "Clear A–B loop"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            if (chapters.isNotEmpty()) {
                val shownChapter = activeChapter ?: chapters.first()
                Surface(
                    color = Color(0xFF0E131F).copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenChaptersSheet)
                        .testTag("player_active_chapter_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = shownChapter.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (showSkipIntroButton) {
                Surface(
                    color = Color(0xFF0E131F).copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = onSkipIntro,
                            onLongClick = onLongPressSkipIntro
                        )
                        .testTag("player_skip_intro_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = tr("+${skipIntroSeconds}с Опенинг", "+${skipIntroSeconds}s Intro"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatTimeMs(displayPos),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
        ThemedPlayerSeekBar(
            controlsTheme = controlsTheme,
            positionMs = displayPos,
            durationMs = durationMs,
            isScrubbing = isScrubbing,
            chapters = chapters,
            showChapterMarkers = showChapterMarkers,
            abLoopStartMs = abLoopStartMs,
            abLoopEndMs = abLoopEndMs,
            onScrubChange = onScrubChange,
            onScrubFinished = onScrubFinished,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatTimeMs(durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
