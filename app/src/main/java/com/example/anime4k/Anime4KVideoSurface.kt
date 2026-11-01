package com.example.anime4k

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.Anime4KPreset
import com.example.model.FrameInterpolationMode
import com.example.model.VideoScaleMode
import com.example.player.DisplayRefreshRateController
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicInteger
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun Anime4KVideoSurface(
    videoWidth: Int,
    videoHeight: Int,
    preset: Anime4KPreset,
    strength: Float,
    interpolationMode: FrameInterpolationMode = FrameInterpolationMode.OFF,
    isPlaying: Boolean = false,
    sourceFrameRate: Float = 0f,
    playbackSpeed: Float = 1.0f,
    highRefreshRateEnabled: Boolean = true,
    targetDisplayHz: Float = 120f,
    targetRenderFps: Float = targetDisplayHz,
    surfaceFrameRateHz: Float = 0f,
    frameRateCompatibility: Int = Surface.FRAME_RATE_COMPATIBILITY_DEFAULT,
    memcActive: Boolean = interpolationMode.isEnabled && isPlaying,
    splitCompareEnabled: Boolean,
    splitPosition: Float,
    scaleMode: VideoScaleMode,
    onSurfaceReady: (Surface) -> Unit,
    onSurfaceDestroyed: (Surface) -> Unit,
    modifier: Modifier = Modifier
) {
    val holderRef = remember { SurfaceCallbackBridge() }
    holderRef.onReady = onSurfaceReady
    holderRef.onDestroyed = onSurfaceDestroyed

    AndroidView(
        modifier = modifier,
        factory = { context ->
            Anime4KGlSurfaceView(context, holderRef).apply {
                updateParameters(
                    videoWidth = videoWidth,
                    videoHeight = videoHeight,
                    preset = preset,
                    strength = strength,
                    interpolationMode = interpolationMode,
                    isPlaying = isPlaying,
                    sourceFrameRate = sourceFrameRate,
                    playbackSpeed = playbackSpeed,
                    highRefreshRateEnabled = highRefreshRateEnabled,
                    targetDisplayHz = targetDisplayHz,
                    targetRenderFps = targetRenderFps,
                    surfaceFrameRateHz = surfaceFrameRateHz,
                    frameRateCompatibility = frameRateCompatibility,
                    memcActive = memcActive,
                    splitCompare = splitCompareEnabled,
                    splitPos = splitPosition,
                    scaleMode = scaleMode
                )
            }
        },
        update = { glView ->
            glView.updateParameters(
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                preset = preset,
                strength = strength,
                interpolationMode = interpolationMode,
                isPlaying = isPlaying,
                sourceFrameRate = sourceFrameRate,
                playbackSpeed = playbackSpeed,
                highRefreshRateEnabled = highRefreshRateEnabled,
                targetDisplayHz = targetDisplayHz,
                targetRenderFps = targetRenderFps,
                surfaceFrameRateHz = surfaceFrameRateHz,
                frameRateCompatibility = frameRateCompatibility,
                memcActive = memcActive,
                splitCompare = splitCompareEnabled,
                splitPos = splitPosition,
                scaleMode = scaleMode
            )
        }
    )

    LaunchedEffect(
        preset,
        strength,
        interpolationMode,
        isPlaying,
        sourceFrameRate,
        playbackSpeed,
        highRefreshRateEnabled,
        targetDisplayHz,
        targetRenderFps,
        memcActive,
        splitCompareEnabled,
        splitPosition,
        scaleMode,
        videoWidth,
        videoHeight
    ) {
        holderRef.glView?.requestSingleRedrawIfIdle()
    }

    DisposableEffect(Unit) {
        onDispose {
            holderRef.glView?.releaseResources()
        }
    }
}

private class SurfaceCallbackBridge {
    var glView: Anime4KGlSurfaceView? = null
    var onReady: ((Surface) -> Unit)? = null
    var onDestroyed: ((Surface) -> Unit)? = null
}

private class Anime4KGlSurfaceView(
    context: Context,
    private val bridge: SurfaceCallbackBridge
) : GLSurfaceView(context), Choreographer.FrameCallback {

    private val renderer: Anime4KRenderer
    private var vsyncActive = false
    private var wantVsyncInterpolation = false
    private var latestSurfaceRateActive = true
    private var latestSurfaceFps = 0f
    private var latestCompatibility = Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
    private var latestRenderFpsCap = 120f
    private var lastPacedVsyncNano = 0L

    init {
        bridge.glView = this
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        renderer = Anime4KRenderer(
            requestRedraw = { requestRender() },
            onSurfaceCreatedCallback = { surface ->
                post {
                    applyDisplaySurfaceRateHint()
                    bridge.onReady?.invoke(surface)
                }
            }
        )
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)
        applyDisplaySurfaceRateHint()
        syncChoreographerLoop()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopChoreographerLoop()
        super.surfaceDestroyed(holder)
    }

    override fun onDetachedFromWindow() {
        stopChoreographerLoop()
        super.onDetachedFromWindow()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!vsyncActive || !wantVsyncInterpolation) {
            vsyncActive = false
            renderer.setVsyncLoopRunning(false)
            return
        }

        // OptionalLTPO/60Hz pacing guard when panel physical VSync is temporarily higher than targetRenderFps
        val minStepNs = if (latestRenderFpsCap in 24f..90f) {
            ((1_000_000_000f / latestRenderFpsCap) * 0.80f).toLong()
        } else {
            0L
        }
        if (minStepNs == 0L || lastPacedVsyncNano == 0L || (frameTimeNanos - lastPacedVsyncNano) >= minStepNs) {
            lastPacedVsyncNano = frameTimeNanos
            renderer.updateDisplayFrameNano(frameTimeNanos)
            requestRender()
        }

        Choreographer.getInstance().postFrameCallback(this)
    }

    fun requestSingleRedrawIfIdle() {
        if (!vsyncActive) {
            requestRender()
        }
    }

    private fun syncChoreographerLoop() {
        if (wantVsyncInterpolation && isAttachedToWindow) {
            if (!vsyncActive) {
                vsyncActive = true
                lastPacedVsyncNano = 0L
                renderer.setVsyncLoopRunning(true)
                Choreographer.getInstance().postFrameCallback(this)
            }
        } else {
            stopChoreographerLoop()
        }
    }

    private fun stopChoreographerLoop() {
        renderer.setVsyncLoopRunning(false)
        if (vsyncActive) {
            vsyncActive = false
            lastPacedVsyncNano = 0L
            runCatching { Choreographer.getInstance().removeFrameCallback(this) }
        }
    }

    private fun applyDisplaySurfaceRateHint() {
        DisplayRefreshRateController.configureSurfaceFrameRate(
            surface = holder?.surface,
            targetFps = latestSurfaceFps,
            highRefreshOrInterpActive = latestSurfaceRateActive,
            compatibility = latestCompatibility
        )
    }

    fun updateParameters(
        videoWidth: Int,
        videoHeight: Int,
        preset: Anime4KPreset,
        strength: Float,
        interpolationMode: FrameInterpolationMode,
        isPlaying: Boolean,
        sourceFrameRate: Float,
        playbackSpeed: Float,
        highRefreshRateEnabled: Boolean,
        targetDisplayHz: Float,
        targetRenderFps: Float,
        surfaceFrameRateHz: Float,
        frameRateCompatibility: Int,
        memcActive: Boolean,
        splitCompare: Boolean,
        splitPos: Float,
        scaleMode: VideoScaleMode
    ) {
        val effectiveMemcMode = if (memcActive && interpolationMode.isEnabled) {
            interpolationMode
        } else {
            FrameInterpolationMode.OFF
        }

        renderer.setConfig(
            videoW = videoWidth,
            videoH = videoHeight,
            preset = preset,
            strength = strength,
            interpolationMode = effectiveMemcMode,
            sourceFps = sourceFrameRate,
            playbackSpeed = playbackSpeed,
            splitCompare = splitCompare,
            splitPos = splitPos,
            scaleMode = scaleMode
        )

        val resolvedSurfaceFps = when {
            surfaceFrameRateHz > 0f -> surfaceFrameRateHz
            effectiveMemcMode.isEnabled && effectiveMemcMode.targetFps <= 60 -> 60f
            effectiveMemcMode.isEnabled -> targetDisplayHz.coerceAtLeast(60f)
            !isPlaying -> 0f
            highRefreshRateEnabled -> targetDisplayHz.coerceAtLeast(60f)
            else -> 0f
        }
        val surfaceActive = resolvedSurfaceFps >= 12f
        val rateChanged = abs(resolvedSurfaceFps - latestSurfaceFps) > 0.4f ||
            surfaceActive != latestSurfaceRateActive ||
            frameRateCompatibility != latestCompatibility

        latestSurfaceFps = resolvedSurfaceFps
        latestSurfaceRateActive = surfaceActive
        latestCompatibility = frameRateCompatibility
        latestRenderFpsCap = targetRenderFps.coerceAtLeast(24f)
        val prevWantVsync = wantVsyncInterpolation
        wantVsyncInterpolation = effectiveMemcMode.isEnabled && isPlaying

        if (rateChanged) {
            applyDisplaySurfaceRateHint()
        }
        syncChoreographerLoop()
        if (!wantVsyncInterpolation || !prevWantVsync) {
            requestRender()
        }
    }

    fun releaseResources() {
        stopChoreographerLoop()
        val currentSurface = renderer.currentDecoderSurface
        if (currentSurface != null) {
            bridge.onDestroyed?.invoke(currentSurface)
        }
        queueEvent { renderer.releaseGl() }
    }
}

private class Anime4KRenderer(
    private val requestRedraw: () -> Unit,
    private val onSurfaceCreatedCallback: (Surface) -> Unit
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private class OesProgram(val id: Int) {
        val aPosition = if (id != 0) GLES20.glGetAttribLocation(id, "aPosition") else -1
        val aTexCoord = if (id != 0) GLES20.glGetAttribLocation(id, "aTexCoord") else -1
        val uTexMatrix = if (id != 0) GLES20.glGetUniformLocation(id, "uTexMatrix") else -1
        val uTex = if (id != 0) GLES20.glGetUniformLocation(id, "uTex") else -1
    }

    private class Anime4KPassProgram(val id: Int) {
        val aPosition = if (id != 0) GLES20.glGetAttribLocation(id, "aPosition") else -1
        val aTexCoord = if (id != 0) GLES20.glGetAttribLocation(id, "aTexCoord") else -1
        val uTex = if (id != 0) GLES20.glGetUniformLocation(id, "uTex") else -1
        val uOrigTex = if (id != 0) GLES20.glGetUniformLocation(id, "uOrigTex") else -1
        val uTexelSize = if (id != 0) GLES20.glGetUniformLocation(id, "uTexelSize") else -1
        val uStrength = if (id != 0) GLES20.glGetUniformLocation(id, "uStrength") else -1
        val uSplitMode = if (id != 0) GLES20.glGetUniformLocation(id, "uSplitMode") else -1
        val uSplitPos = if (id != 0) GLES20.glGetUniformLocation(id, "uSplitPos") else -1
        val uIsUltra = if (id != 0) GLES20.glGetUniformLocation(id, "uIsUltra") else -1
    }

    private class MemcEstimateProgram(val id: Int) {
        val aPosition = if (id != 0) GLES20.glGetAttribLocation(id, "aPosition") else -1
        val aTexCoord = if (id != 0) GLES20.glGetAttribLocation(id, "aTexCoord") else -1
        val uPrevTex = if (id != 0) GLES20.glGetUniformLocation(id, "uPrevTex") else -1
        val uCurrTex = if (id != 0) GLES20.glGetUniformLocation(id, "uCurrTex") else -1
        val uTexelSize = if (id != 0) GLES20.glGetUniformLocation(id, "uTexelSize") else -1
        val uSearchRadius = if (id != 0) GLES20.glGetUniformLocation(id, "uSearchRadius") else -1
    }

    private class MemcSmoothProgram(val id: Int) {
        val aPosition = if (id != 0) GLES20.glGetAttribLocation(id, "aPosition") else -1
        val aTexCoord = if (id != 0) GLES20.glGetAttribLocation(id, "aTexCoord") else -1
        val uMotionTex = if (id != 0) GLES20.glGetUniformLocation(id, "uMotionTex") else -1
        val uMotionTexel = if (id != 0) GLES20.glGetUniformLocation(id, "uMotionTexel") else -1
    }

    private class MemcSynthProgram(val id: Int) {
        val aPosition = if (id != 0) GLES20.glGetAttribLocation(id, "aPosition") else -1
        val aTexCoord = if (id != 0) GLES20.glGetAttribLocation(id, "aTexCoord") else -1
        val uPrevTex = if (id != 0) GLES20.glGetUniformLocation(id, "uPrevTex") else -1
        val uCurrTex = if (id != 0) GLES20.glGetUniformLocation(id, "uCurrTex") else -1
        val uMotionTex = if (id != 0) GLES20.glGetUniformLocation(id, "uMotionTex") else -1
        val uTexelSize = if (id != 0) GLES20.glGetUniformLocation(id, "uTexelSize") else -1
        val uAlpha = if (id != 0) GLES20.glGetUniformLocation(id, "uAlpha") else -1
        val uStrength = if (id != 0) GLES20.glGetUniformLocation(id, "uStrength") else -1
        val uEdgeProtect = if (id != 0) GLES20.glGetUniformLocation(id, "uEdgeProtect") else -1
        val uSplitCompare = if (id != 0) GLES20.glGetUniformLocation(id, "uSplitCompare") else -1
        val uSplitX = if (id != 0) GLES20.glGetUniformLocation(id, "uSplitX") else -1
    }

    private val pendingFrames = AtomicInteger(0)
    @Volatile private var vsyncLoopRunning: Boolean = false
    private val texMatrix = FloatArray(16)

    @Volatile var currentDecoderSurface: Surface? = null
        private set
    private var surfaceTexture: SurfaceTexture? = null
    private var oesTexId: Int = 0

    @Volatile private var surfaceW: Int = 1
    @Volatile private var surfaceH: Int = 1
    @Volatile private var videoW: Int = 1280
    @Volatile private var videoH: Int = 720
    @Volatile private var activePreset: Anime4KPreset = Anime4KPreset.ANIME4K_UPSCALE_CNN
    @Volatile private var activeStrength: Float = 1.0f
    @Volatile private var activeInterpolation: FrameInterpolationMode = FrameInterpolationMode.MAX_FPS
    @Volatile private var activeSourceFps: Float = 0f
    @Volatile private var activePlaybackSpeed: Float = 1.0f
    @Volatile private var splitCompareEnabled: Boolean = false
    @Volatile private var splitPosition: Float = 0.5f
    @Volatile private var activeScaleMode: VideoScaleMode = VideoScaleMode.FIT
    @Volatile private var latestVsyncNano: Long = 0L

    private var maxTextureSize: Int = 2048
    private var hasValidFrame: Boolean = false
    private var decodedFramesCount: Long = 0L
    private var lastFramePtsNs: Long = -1L
    private var measuredPtsIntervalNs: Float = 41_666_668f // ~24 fps fallback
    private var lastDrawVsyncNano: Long = 0L
    private var interpPhase: Float = 0f

    // Cached GL programs (zero glGetUniformLocation per frame)
    private var progPassthrough = OesProgram(0)
    private var progLumaExtract = OesProgram(0)
    private var progFastFinal = Anime4KPassProgram(0)
    private var progCnnRestore = Anime4KPassProgram(0)
    private var progCnnxStructure = Anime4KPassProgram(0)
    private var progSubpixelFinal = Anime4KPassProgram(0)
    private var progMemcEstimate = MemcEstimateProgram(0)
    private var progMemcSmooth = MemcSmoothProgram(0)
    private var progMemcSynth = MemcSynthProgram(0)

    // Offscreen FBO targets
    private var fboPrev = FboTarget()
    private var fboCurr = FboTarget()
    private var fboMotionRaw = FboTarget()
    private var fboMotionSmooth = FboTarget()
    private var fboSrc = FboTarget()
    private var fboMid = FboTarget()
    private var fboUp = FboTarget()

    private val quadVertices: FloatBuffer = ByteBuffer.allocateDirect(4 * 4 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(
                floatArrayOf(
                    -1f, -1f, 0f, 0f,
                     1f, -1f, 1f, 0f,
                    -1f,  1f, 0f, 1f,
                     1f,  1f, 1f, 1f
                )
            )
            position(0)
        }

    fun setVsyncLoopRunning(running: Boolean) {
        vsyncLoopRunning = running
        if (!running) {
            lastDrawVsyncNano = 0L
        }
    }

    fun updateDisplayFrameNano(frameTimeNanos: Long) {
        latestVsyncNano = frameTimeNanos
    }

    fun setConfig(
        videoW: Int,
        videoH: Int,
        preset: Anime4KPreset,
        strength: Float,
        interpolationMode: FrameInterpolationMode,
        sourceFps: Float,
        playbackSpeed: Float,
        splitCompare: Boolean,
        splitPos: Float,
        scaleMode: VideoScaleMode
    ) {
        if (videoW > 0 && videoH > 0) {
            this.videoW = videoW
            this.videoH = videoH
        }
        this.activePreset = preset
        this.activeStrength = strength.coerceIn(0.1f, 1.5f)
        this.activeInterpolation = interpolationMode
        this.activeSourceFps = sourceFps
        this.activePlaybackSpeed = playbackSpeed.coerceIn(0.25f, 3.0f)
        this.splitCompareEnabled = splitCompare
        this.splitPosition = splitPos.coerceIn(0.05f, 0.95f)
        this.activeScaleMode = scaleMode
    }

    override fun onFrameAvailable(st: SurfaceTexture?) {
        pendingFrames.incrementAndGet()
        // CRITICAL: When Choreographer VSync loop is running, do NOT call requestRender()
        // from MediaCodec's callback thread — otherwise eglSwapBuffers queues extra frames
        // between VSync ticks, saturating the 3-buffer SurfaceView queue and causing stutter!
        if (!vsyncLoopRunning) {
            requestRedraw()
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Matrix.setIdentityM(texMatrix, 0)
        val maxBuf = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxBuf, 0)
        maxTextureSize = maxBuf[0].coerceIn(1024, 3840)

        oesTexId = createOesTexture()
        surfaceTexture?.release()
        currentDecoderSurface?.release()

        val st = SurfaceTexture(oesTexId).also {
            it.setOnFrameAvailableListener(this)
        }
        surfaceTexture = st
        val surf = Surface(st)
        currentDecoderSurface = surf
        hasValidFrame = false
        decodedFramesCount = 0L
        lastFramePtsNs = -1L
        lastDrawVsyncNano = 0L
        interpPhase = 0f
        pendingFrames.set(0)

        progPassthrough = OesProgram(buildProgram(Anime4KShaders.OES_VERTEX_SHADER, Anime4KShaders.OES_PASSTHROUGH_FRAGMENT))
        progLumaExtract = OesProgram(buildProgram(Anime4KShaders.OES_VERTEX_SHADER, Anime4KShaders.OES_LUMA_EXTRACT_FRAGMENT))
        progFastFinal = Anime4KPassProgram(buildProgram(Anime4KShaders.QUAD_VERTEX_SHADER, Anime4KShaders.ANIME4K_FAST_FINAL_FRAGMENT))
        progCnnRestore = Anime4KPassProgram(buildProgram(Anime4KShaders.QUAD_VERTEX_SHADER, Anime4KShaders.ANIME4K_CNN_RESTORE_FRAGMENT))
        progCnnxStructure = Anime4KPassProgram(buildProgram(Anime4KShaders.QUAD_VERTEX_SHADER, Anime4KShaders.ANIME4K_CNNX_STRUCTURE_DEBLUR_FRAGMENT))
        progSubpixelFinal = Anime4KPassProgram(buildProgram(Anime4KShaders.QUAD_VERTEX_SHADER, Anime4KShaders.ANIME4K_SUBPIXEL_UPSCALE_FINAL_FRAGMENT))

        progMemcEstimate = MemcEstimateProgram(buildProgram(Anime4KShaders.QUAD_VERTEX_SHADER, FrameInterpolationShaders.MEMC_MOTION_ESTIMATE_FRAGMENT))
        progMemcSmooth = MemcSmoothProgram(buildProgram(Anime4KShaders.QUAD_VERTEX_SHADER, FrameInterpolationShaders.MEMC_MOTION_SMOOTH_FRAGMENT))
        progMemcSynth = MemcSynthProgram(buildProgram(Anime4KShaders.QUAD_VERTEX_SHADER, FrameInterpolationShaders.MEMC_SYNTHESIZE_FRAGMENT))

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_BLEND)

        onSurfaceCreatedCallback(surf)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceW = width.coerceAtLeast(1)
        surfaceH = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, surfaceW, surfaceH)
    }

    private fun latchLatestSurfaceTexture(st: SurfaceTexture): Boolean {
        return runCatching {
            st.updateTexImage()
            st.getTransformMatrix(texMatrix)
            hasValidFrame = true
            val ptsNs = st.timestamp
            if (ptsNs > 0L) {
                if (lastFramePtsNs > 0L) {
                    val ptsDelta = ptsNs - lastFramePtsNs
                    when {
                        // Seek / episode transition / backward jump: re-seed MEMC cleanly
                        ptsDelta < 0L || ptsDelta > 450_000_000L -> {
                            decodedFramesCount = 0L
                            interpPhase = 0f
                        }
                        ptsDelta in 10_000_000L..100_000_000L -> {
                            measuredPtsIntervalNs = measuredPtsIntervalNs * 0.80f + ptsDelta.toFloat() * 0.20f
                        }
                    }
                }
                lastFramePtsNs = ptsNs
            }
            true
        }.getOrDefault(false)
    }

    override fun onDrawFrame(gl: GL10?) {
        val st = surfaceTexture ?: return
        val memcMode = activeInterpolation
        val preset = activePreset
        val strength = activeStrength
        val splitMode = if (splitCompareEnabled) 1f else 0f
        val splitPos = splitPosition

        val nowNano = latestVsyncNano.takeIf { it > 0L } ?: System.nanoTime()
        val vsyncDeltaNs = if (lastDrawVsyncNano > 0L) {
            (nowNano - lastDrawVsyncNano).coerceIn(4_000_000L, 50_000_000L)
        } else {
            8_333_333L
        }
        lastDrawVsyncNano = nowNano

        // Compute nominal real-time interval between source frames accounting for playback speed
        val speed = activePlaybackSpeed
        val baseSrcIntervalNs = if (activeSourceFps > 1f) {
            1_000_000_000f / activeSourceFps.coerceIn(10f, 165f)
        } else {
            measuredPtsIntervalNs.coerceIn(12_000_000f, 85_000_000f)
        }
        val nominalFrameIntervalNs = (baseSrcIntervalNs / speed).coerceIn(6_000_000f, 160_000_000f)
        val phaseStep = (vsyncDeltaNs.toFloat() / nominalFrameIntervalNs).coerceIn(0.05f, 1.0f)

        var latchedNewFrame = false
        val queued = pendingFrames.get()

        if (!memcMode.isEnabled || !vsyncLoopRunning) {
            // Non-MEMC or paused: immediately consume latest frame whenever available
            if (queued > 0) {
                pendingFrames.set(0)
                latchedNewFrame = latchLatestSurfaceTexture(st)
                decodedFramesCount = 0L
                interpPhase = 1f
            }
        } else {
            // Phase-Locked MEMC: hold single early-arriving frame (3:2 pulldown jitter) until
            // current interpolation cycle reaches >= 0.84 so sub-frames 0.6..0.85 are never skipped!
            val shouldLatchNow = queued > 0 && (
                decodedFramesCount < 2L ||
                    queued >= 2 ||
                    (interpPhase + phaseStep) >= 0.84f
                )

            if (shouldLatchNow) {
                pendingFrames.set(0)
                latchedNewFrame = latchLatestSurfaceTexture(st)
                if (latchedNewFrame) {
                    val nextUnwrapped = interpPhase + phaseStep
                    interpPhase = if (decodedFramesCount >= 2L && nextUnwrapped >= 1.0f) {
                        (nextUnwrapped - 1.0f).coerceIn(0f, phaseStep.coerceAtMost(0.48f))
                    } else {
                        0f
                    }
                }
            } else if (decodedFramesCount >= 2L) {
                interpPhase = (interpPhase + phaseStep).coerceAtMost(0.985f)
            }
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, surfaceW, surfaceH)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (!hasValidFrame) return

        val vp = computeOutputViewport(surfaceW, surfaceH, videoW, videoH, activeScaleMode)

        // Fast zero-overhead direct OES path when both MEMC and Anime4K are OFF
        if (!memcMode.isEnabled && (preset == Anime4KPreset.OFF || progLumaExtract.id == 0)) {
            drawOesDirect(vp, progPassthrough)
            return
        }

        val srcW = min(videoW.coerceAtLeast(64), maxTextureSize)
        val srcH = min(videoH.coerceAtLeast(64), maxTextureSize)
        val upW = min(vp.w.coerceAtLeast(srcW), maxTextureSize)
        val upH = min(vp.h.coerceAtLeast(srcH), maxTextureSize)

        val activeSourceFbo: FboTarget
        val referenceOrigTex: Int

        if (memcMode.isEnabled && progMemcEstimate.id != 0 && progMemcSmooth.id != 0 && progMemcSynth.id != 0) {
            val motionW = (srcW / 4).coerceIn(96, 480)
            val motionH = (srcH / 4).coerceIn(54, 270)
            val fbosReady = fboPrev.ensure(srcW, srcH) &&
                fboCurr.ensure(srcW, srcH) &&
                fboMotionRaw.ensure(motionW, motionH) &&
                fboMotionSmooth.ensure(motionW, motionH)

            if (!fbosReady) {
                drawOesDirect(vp, progPassthrough)
                return
            }

            if (latchedNewFrame || decodedFramesCount == 0L) {
                val tmp = fboPrev
                fboPrev = fboCurr
                fboCurr = tmp

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboCurr.fboId)
                GLES20.glViewport(0, 0, fboCurr.width, fboCurr.height)
                drawOesToFbo(progLumaExtract)

                if (decodedFramesCount == 0L) {
                    // Seed fboPrev on frame #1 or after seek so there is zero black flash or stale ghosting
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboPrev.fboId)
                    GLES20.glViewport(0, 0, fboPrev.width, fboPrev.height)
                    drawOesToFbo(progLumaExtract)
                }
                decodedFramesCount++

                // Compute multi-scale optical flow ONCE per decoded source frame
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboMotionRaw.fboId)
                GLES20.glViewport(0, 0, fboMotionRaw.width, fboMotionRaw.height)
                drawMemcEstimatePass(
                    prevTex = fboPrev.texId,
                    currTex = fboCurr.texId,
                    texelW = 1f / motionW.toFloat(),
                    texelH = 1f / motionH.toFloat(),
                    searchRadius = if (memcMode.isAnimeContourProtected) 1.2f else 1.4f
                )

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboMotionSmooth.fboId)
                GLES20.glViewport(0, 0, fboMotionSmooth.width, fboMotionSmooth.height)
                drawMemcSmoothPass(
                    motionTex = fboMotionRaw.texId,
                    texelW = 1f / motionW.toFloat(),
                    texelH = 1f / motionH.toFloat()
                )
            }

            val phaseAlpha = when {
                decodedFramesCount < 2L -> 1f
                memcMode == FrameInterpolationMode.CINEMA -> {
                    // Filmic smooth 2x cadence interpolation
                    val clampedPhase = interpPhase.coerceIn(0f, 1f)
                    (0.5f - 0.5f * kotlin.math.cos(clampedPhase * Math.PI.toFloat())).coerceIn(0f, 1f)
                }
                else -> interpPhase.coerceIn(0f, 1f)
            }

            if (preset == Anime4KPreset.OFF) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                GLES20.glViewport(vp.x, vp.y, vp.w, vp.h)
                drawMemcSynthPass(
                    prevTex = fboPrev.texId,
                    currTex = fboCurr.texId,
                    motionTex = fboMotionSmooth.texId,
                    texelW = 1f / srcW.toFloat(),
                    texelH = 1f / srcH.toFloat(),
                    alpha = phaseAlpha,
                    motionStrength = memcMode.motionStrength,
                    edgeProtect = if (memcMode.isAnimeContourProtected) 1.0f else 0.50f,
                    splitCompare = if (splitCompareEnabled) 1 else 0,
                    splitX = splitPosition
                )
                return
            }

            if (!fboSrc.ensure(srcW, srcH)) {
                drawOesDirect(vp, progPassthrough)
                return
            }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboSrc.fboId)
            GLES20.glViewport(0, 0, fboSrc.width, fboSrc.height)
            drawMemcSynthPass(
                prevTex = fboPrev.texId,
                currTex = fboCurr.texId,
                motionTex = fboMotionSmooth.texId,
                texelW = 1f / srcW.toFloat(),
                texelH = 1f / srcH.toFloat(),
                alpha = phaseAlpha,
                motionStrength = memcMode.motionStrength,
                edgeProtect = if (memcMode.isAnimeContourProtected) 1.0f else 0.50f,
                splitCompare = 0,
                splitX = splitPosition
            )
            activeSourceFbo = fboSrc
            referenceOrigTex = if (phaseAlpha < 0.5f) fboPrev.texId else fboCurr.texId
        } else {
            if (!fboSrc.ensure(srcW, srcH)) {
                drawOesDirect(vp, progPassthrough)
                return
            }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboSrc.fboId)
            GLES20.glViewport(0, 0, fboSrc.width, fboSrc.height)
            drawOesToFbo(progLumaExtract)
            activeSourceFbo = fboSrc
            referenceOrigTex = fboSrc.texId
        }

        // Step B: Run Anime4K Shader Passes on activeSourceFbo.
        // When MEMC interpolation is active at high refresh rates, automatically use single-pass ANIME4K_FAST
        // instead of multi-pass CNN/CNNX FBO upscaling to prevent GPU bandwidth bottlenecks and stutter.
        val effectiveShaderPreset = if (memcMode != FrameInterpolationMode.OFF &&
            (preset == Anime4KPreset.ANIME4K_UPSCALE_CNN || preset == Anime4KPreset.ANIME4K_UPSCALE_CNNX)
        ) {
            Anime4KPreset.ANIME4K_FAST
        } else {
            preset
        }

        when (effectiveShaderPreset) {
            Anime4KPreset.ANIME4K_FAST -> {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                GLES20.glViewport(vp.x, vp.y, vp.w, vp.h)
                drawTex2DPass(
                    program = progFastFinal,
                    inputTex = activeSourceFbo.texId,
                    origTex = referenceOrigTex,
                    texelW = 1f / activeSourceFbo.width.toFloat(),
                    texelH = 1f / activeSourceFbo.height.toFloat(),
                    strength = strength,
                    splitMode = splitMode,
                    splitPos = splitPos,
                    isUltra = 0f
                )
            }

            Anime4KPreset.ANIME4K_UPSCALE_CNN -> {
                if (!fboUp.ensure(upW, upH)) {
                    drawOesDirect(vp, progPassthrough)
                    return
                }
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboUp.fboId)
                GLES20.glViewport(0, 0, fboUp.width, fboUp.height)
                drawTex2DPass(
                    program = progCnnRestore,
                    inputTex = activeSourceFbo.texId,
                    origTex = 0,
                    texelW = 1f / activeSourceFbo.width.toFloat(),
                    texelH = 1f / activeSourceFbo.height.toFloat(),
                    strength = strength,
                    splitMode = 0f,
                    splitPos = 0.5f,
                    isUltra = 0f
                )

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                GLES20.glViewport(vp.x, vp.y, vp.w, vp.h)
                drawTex2DPass(
                    program = progSubpixelFinal,
                    inputTex = fboUp.texId,
                    origTex = referenceOrigTex,
                    texelW = 1f / fboUp.width.toFloat(),
                    texelH = 1f / fboUp.height.toFloat(),
                    strength = strength,
                    splitMode = splitMode,
                    splitPos = splitPos,
                    isUltra = 0f
                )
            }

            Anime4KPreset.ANIME4K_UPSCALE_CNNX -> {
                if (!fboMid.ensure(srcW, srcH) || !fboUp.ensure(upW, upH)) {
                    drawOesDirect(vp, progPassthrough)
                    return
                }
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboMid.fboId)
                GLES20.glViewport(0, 0, fboMid.width, fboMid.height)
                drawTex2DPass(
                    program = progCnnxStructure,
                    inputTex = activeSourceFbo.texId,
                    origTex = 0,
                    texelW = 1f / activeSourceFbo.width.toFloat(),
                    texelH = 1f / activeSourceFbo.height.toFloat(),
                    strength = strength,
                    splitMode = 0f,
                    splitPos = 0.5f,
                    isUltra = 1f
                )

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboUp.fboId)
                GLES20.glViewport(0, 0, fboUp.width, fboUp.height)
                drawTex2DPass(
                    program = progCnnRestore,
                    inputTex = fboMid.texId,
                    origTex = 0,
                    texelW = 1f / fboMid.width.toFloat(),
                    texelH = 1f / fboMid.height.toFloat(),
                    strength = strength * 1.15f,
                    splitMode = 0f,
                    splitPos = 0.5f,
                    isUltra = 1f
                )

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                GLES20.glViewport(vp.x, vp.y, vp.w, vp.h)
                drawTex2DPass(
                    program = progSubpixelFinal,
                    inputTex = fboUp.texId,
                    origTex = referenceOrigTex,
                    texelW = 1f / fboUp.width.toFloat(),
                    texelH = 1f / fboUp.height.toFloat(),
                    strength = strength,
                    splitMode = splitMode,
                    splitPos = splitPos,
                    isUltra = 1f
                )
            }

            Anime4KPreset.OFF -> drawOesDirect(vp, progPassthrough)
        }
    }

    private fun drawOesDirect(vp: ViewportRect, program: OesProgram) {
        if (program.id == 0) return
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(vp.x, vp.y, vp.w, vp.h)
        drawOesToFbo(program)
    }

    private fun drawOesToFbo(program: OesProgram) {
        if (program.id == 0) return
        GLES20.glUseProgram(program.id)
        bindQuadAttributes(program.aPosition, program.aTexCoord)

        if (program.uTexMatrix >= 0) {
            GLES20.glUniformMatrix4fv(program.uTexMatrix, 1, false, texMatrix, 0)
        }
        if (program.uTex >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTexId)
            GLES20.glUniform1i(program.uTex, 0)
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        unbindQuadAttributes(program.aPosition, program.aTexCoord)
    }

    private fun drawMemcEstimatePass(
        prevTex: Int,
        currTex: Int,
        texelW: Float,
        texelH: Float,
        searchRadius: Float
    ) {
        val p = progMemcEstimate
        if (p.id == 0) return
        GLES20.glUseProgram(p.id)
        bindQuadAttributes(p.aPosition, p.aTexCoord)

        if (p.uPrevTex >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, prevTex)
            GLES20.glUniform1i(p.uPrevTex, 0)
        }
        if (p.uCurrTex >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currTex)
            GLES20.glUniform1i(p.uCurrTex, 1)
        }
        if (p.uTexelSize >= 0) GLES20.glUniform2f(p.uTexelSize, texelW, texelH)
        if (p.uSearchRadius >= 0) GLES20.glUniform1f(p.uSearchRadius, searchRadius)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        unbindQuadAttributes(p.aPosition, p.aTexCoord)
    }

    private fun drawMemcSmoothPass(motionTex: Int, texelW: Float, texelH: Float) {
        val p = progMemcSmooth
        if (p.id == 0) return
        GLES20.glUseProgram(p.id)
        bindQuadAttributes(p.aPosition, p.aTexCoord)

        if (p.uMotionTex >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, motionTex)
            GLES20.glUniform1i(p.uMotionTex, 0)
        }
        if (p.uMotionTexel >= 0) GLES20.glUniform2f(p.uMotionTexel, texelW, texelH)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        unbindQuadAttributes(p.aPosition, p.aTexCoord)
    }

    private fun drawMemcSynthPass(
        prevTex: Int,
        currTex: Int,
        motionTex: Int,
        texelW: Float,
        texelH: Float,
        alpha: Float,
        motionStrength: Float,
        edgeProtect: Float,
        splitCompare: Int,
        splitX: Float
    ) {
        val p = progMemcSynth
        if (p.id == 0) return
        GLES20.glUseProgram(p.id)
        bindQuadAttributes(p.aPosition, p.aTexCoord)

        if (p.uPrevTex >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, prevTex)
            GLES20.glUniform1i(p.uPrevTex, 0)
        }
        if (p.uCurrTex >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currTex)
            GLES20.glUniform1i(p.uCurrTex, 1)
        }
        if (p.uMotionTex >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, motionTex)
            GLES20.glUniform1i(p.uMotionTex, 2)
        }
        if (p.uTexelSize >= 0) GLES20.glUniform2f(p.uTexelSize, texelW, texelH)
        if (p.uAlpha >= 0) GLES20.glUniform1f(p.uAlpha, alpha)
        if (p.uStrength >= 0) GLES20.glUniform1f(p.uStrength, motionStrength)
        if (p.uEdgeProtect >= 0) GLES20.glUniform1f(p.uEdgeProtect, edgeProtect)
        if (p.uSplitCompare >= 0) GLES20.glUniform1i(p.uSplitCompare, splitCompare)
        if (p.uSplitX >= 0) GLES20.glUniform1f(p.uSplitX, splitX)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        unbindQuadAttributes(p.aPosition, p.aTexCoord)
    }

    private fun drawTex2DPass(
        program: Anime4KPassProgram,
        inputTex: Int,
        origTex: Int,
        texelW: Float,
        texelH: Float,
        strength: Float,
        splitMode: Float,
        splitPos: Float,
        isUltra: Float
    ) {
        if (program.id == 0) return
        GLES20.glUseProgram(program.id)
        bindQuadAttributes(program.aPosition, program.aTexCoord)

        if (program.uTex >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTex)
            GLES20.glUniform1i(program.uTex, 0)
        }

        if (program.uOrigTex >= 0 && origTex != 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, origTex)
            GLES20.glUniform1i(program.uOrigTex, 1)
        }

        if (program.uTexelSize >= 0) GLES20.glUniform2f(program.uTexelSize, texelW, texelH)
        if (program.uStrength >= 0) GLES20.glUniform1f(program.uStrength, strength)
        if (program.uSplitMode >= 0) GLES20.glUniform1f(program.uSplitMode, splitMode)
        if (program.uSplitPos >= 0) GLES20.glUniform1f(program.uSplitPos, splitPos)
        if (program.uIsUltra >= 0) GLES20.glUniform1f(program.uIsUltra, isUltra)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        unbindQuadAttributes(program.aPosition, program.aTexCoord)
    }

    private fun bindQuadAttributes(aPos: Int, aUv: Int) {
        quadVertices.position(0)
        if (aPos >= 0) {
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 16, quadVertices)
        }
        quadVertices.position(2)
        if (aUv >= 0) {
            GLES20.glEnableVertexAttribArray(aUv)
            GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 16, quadVertices)
        }
    }

    private fun unbindQuadAttributes(aPos: Int, aUv: Int) {
        if (aPos >= 0) GLES20.glDisableVertexAttribArray(aPos)
        if (aUv >= 0) GLES20.glDisableVertexAttribArray(aUv)
    }

    private data class ViewportRect(val x: Int, val y: Int, val w: Int, val h: Int)

    private fun computeOutputViewport(
        surfW: Int,
        surfH: Int,
        vidW: Int,
        vidH: Int,
        mode: VideoScaleMode
    ): ViewportRect {
        if (mode == VideoScaleMode.STRETCH || vidW <= 0 || vidH <= 0) {
            return ViewportRect(0, 0, surfW, surfH)
        }
        val surfAspect = surfW.toFloat() / surfH.toFloat()
        val vidAspect = vidW.toFloat() / vidH.toFloat()

        return when (mode) {
            VideoScaleMode.FIT -> {
                if (vidAspect > surfAspect) {
                    val outH = (surfW / vidAspect).roundToInt().coerceAtLeast(1)
                    ViewportRect(0, (surfH - outH) / 2, surfW, outH)
                } else {
                    val outW = (surfH * vidAspect).roundToInt().coerceAtLeast(1)
                    ViewportRect((surfW - outW) / 2, 0, outW, surfH)
                }
            }
            VideoScaleMode.ZOOM -> {
                if (vidAspect > surfAspect) {
                    val outW = (surfH * vidAspect).roundToInt().coerceAtLeast(1)
                    ViewportRect((surfW - outW) / 2, 0, outW, surfH)
                } else {
                    val outH = (surfW / vidAspect).roundToInt().coerceAtLeast(1)
                    ViewportRect(0, (surfH - outH) / 2, surfW, outH)
                }
            }
            VideoScaleMode.STRETCH -> ViewportRect(0, 0, surfW, surfH)
        }
    }

    private fun createOesTexture(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ids[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return ids[0]
    }

    private fun buildProgram(vertSrc: String, fragSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        if (vs == 0 || fs == 0) return 0
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e("Anime4KRenderer", "Program link failed: ${GLES20.glGetProgramInfoLog(prog)}")
            GLES20.glDeleteProgram(prog)
            return 0
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("Anime4KRenderer", "Shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun releaseGl() {
        fboPrev.release()
        fboCurr.release()
        fboMotionRaw.release()
        fboMotionSmooth.release()
        fboSrc.release()
        fboMid.release()
        fboUp.release()
        listOf(
            progPassthrough.id,
            progLumaExtract.id,
            progFastFinal.id,
            progCnnRestore.id,
            progCnnxStructure.id,
            progSubpixelFinal.id,
            progMemcEstimate.id,
            progMemcSmooth.id,
            progMemcSynth.id
        )
            .filter { it != 0 }
            .forEach { GLES20.glDeleteProgram(it) }
        if (oesTexId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(oesTexId), 0)
            oesTexId = 0
        }
        runCatching { surfaceTexture?.release() }
        surfaceTexture = null
        runCatching { currentDecoderSurface?.release() }
        currentDecoderSurface = null
    }

    private class FboTarget {
        var fboId: Int = 0
        var texId: Int = 0
        var width: Int = 0
        var height: Int = 0

        fun ensure(w: Int, h: Int): Boolean {
            if (fboId != 0 && texId != 0 && width == w && height == h) return true
            release()
            val texArr = IntArray(1)
            val fboArr = IntArray(1)
            GLES20.glGenTextures(1, texArr, 0)
            GLES20.glGenFramebuffers(1, fboArr, 0)
            texId = texArr[0]
            fboId = fboArr[0]

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
            )

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,
                texId,
                0
            )
            val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                release()
                return false
            }
            width = w
            height = h
            return true
        }

        fun release() {
            if (fboId != 0) {
                GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
                fboId = 0
            }
            if (texId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(texId), 0)
                texId = 0
            }
            width = 0
            height = 0
        }
    }
}
