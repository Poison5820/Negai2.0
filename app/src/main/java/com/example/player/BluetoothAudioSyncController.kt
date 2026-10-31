package com.example.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Immutable

enum class BluetoothAudioProfile(
    val id: String,
    val badgeRu: String,
    val badgeEn: String,
    val defaultCompensationMs: Long
) {
    NONE("NONE", "Провод / Динамик", "Speaker / Wired", 0L),
    A2DP("A2DP", "Bluetooth A2DP", "Bluetooth A2DP", -150L),
    LE_AUDIO("LE_AUDIO", "Bluetooth LE Audio", "Bluetooth LE Audio", -60L),
    HEARING_AID("HEARING_AID", "Bluetooth Hearing Aid", "Bluetooth Hearing Aid", -80L),
    SCO("SCO", "Bluetooth Гарнитура (SCO)", "Bluetooth Headset (SCO)", -120L);

    fun localizedBadge(isEnglish: Boolean): String = if (isEnglish) badgeEn else badgeRu
}

@Immutable
data class BluetoothAudioRouteState(
    val isBluetoothActive: Boolean = false,
    val profile: BluetoothAudioProfile = BluetoothAudioProfile.NONE,
    val deviceName: String? = null,
    val halOutputLatencyMs: Int = 0
)

/**
 * Monitors active audio output routes via [AudioManager] + [AudioDeviceCallback] and computes
 * accurate Bluetooth audio latency compensation.
 *
 * Why standard Android players fail at Bluetooth A/V sync:
 * 1. A2DP codecs (SBC, AAC, aptX, LDAC) and TWS earbuds introduce 120–220 ms of air + codec
 *    buffering that is often under-reported by OEM `AudioTrack.getTimestamp()` HAL implementations.
 * 2. Connecting or disconnecting Bluetooth mid-playback leaves stale timestamp smoothing state inside
 *    ExoPlayer's `AudioTrackPositionTracker` unless the audio clock is explicitly re-anchored.
 * 3. Naive players implement negative audio delay by skipping/chopping leading PCM samples, which cuts
 *    off words after every seek/unpause. Instead, our engine applies negative delay (`< 0 ms`) directly
 *    to the master `MediaClock` so `MediaCodecVideoRenderer` naturally delays video frames to match the
 *    Bluetooth audio arrival time without dropping a single audio sample.
 */
class BluetoothAudioSyncController(
    context: Context,
    private val onRouteChanged: (BluetoothAudioRouteState) -> Unit
) {
    companion object {
        const val DEFAULT_BT_OFFSET_MS = -150L
        const val MIN_BT_OFFSET_MS = -600L
        const val MAX_BT_OFFSET_MS = 200L

        fun clampBluetoothOffsetMs(offsetMs: Long): Long =
            offsetMs.coerceIn(MIN_BT_OFFSET_MS, MAX_BT_OFFSET_MS)

        /**
         * Pure function to compute the automatic Bluetooth compensation offset (in milliseconds).
         * Negative values mean audio is advanced relative to video (video frames are delayed to wait
         * for Bluetooth transmission latency).
         */
        fun computeAutoBluetoothOffsetMs(
            route: BluetoothAudioRouteState,
            autoSyncEnabled: Boolean,
            userConfiguredA2dpOffsetMs: Long = DEFAULT_BT_OFFSET_MS
        ): Long {
            if (!autoSyncEnabled || !route.isBluetoothActive) return 0L
            val clampedUserOffset = clampBluetoothOffsetMs(userConfiguredA2dpOffsetMs)
            return when (route.profile) {
                BluetoothAudioProfile.NONE -> 0L
                BluetoothAudioProfile.A2DP -> clampedUserOffset
                BluetoothAudioProfile.LE_AUDIO -> {
                    // LE Audio (LC3) has ~40% of classic A2DP latency; if user kept default -150ms, use -60ms,
                    // otherwise scale proportionally or respect custom offset.
                    if (clampedUserOffset == DEFAULT_BT_OFFSET_MS) {
                        BluetoothAudioProfile.LE_AUDIO.defaultCompensationMs
                    } else {
                        (clampedUserOffset * 40L / 100L).coerceIn(MIN_BT_OFFSET_MS, MAX_BT_OFFSET_MS)
                    }
                }
                BluetoothAudioProfile.HEARING_AID -> {
                    if (clampedUserOffset == DEFAULT_BT_OFFSET_MS) {
                        BluetoothAudioProfile.HEARING_AID.defaultCompensationMs
                    } else {
                        (clampedUserOffset * 55L / 100L).coerceIn(MIN_BT_OFFSET_MS, MAX_BT_OFFSET_MS)
                    }
                }
                BluetoothAudioProfile.SCO -> {
                    if (clampedUserOffset == DEFAULT_BT_OFFSET_MS) {
                        BluetoothAudioProfile.SCO.defaultCompensationMs
                    } else {
                        clampedUserOffset
                    }
                }
            }
        }

        /**
         * Computes total effective audio delay combining manual per-track delay and automatic
         * Bluetooth route compensation.
         */
        fun computeTotalEffectiveDelayMs(
            manualDelayMs: Long,
            route: BluetoothAudioRouteState,
            autoSyncEnabled: Boolean,
            userConfiguredBtOffsetMs: Long = DEFAULT_BT_OFFSET_MS
        ): Long {
            val btOffset = computeAutoBluetoothOffsetMs(
                route = route,
                autoSyncEnabled = autoSyncEnabled,
                userConfiguredA2dpOffsetMs = userConfiguredBtOffsetMs
            )
            return AudioDelayProcessor.clampDelayMs(manualDelayMs + btOffset)
        }

        /**
         * Maps Android [AudioDeviceInfo.getType] to [BluetoothAudioProfile].
         */
        fun classifyDeviceType(deviceType: Int): BluetoothAudioProfile {
            return when (deviceType) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> BluetoothAudioProfile.A2DP
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> BluetoothAudioProfile.SCO
                AudioDeviceInfo.TYPE_HEARING_AID -> BluetoothAudioProfile.HEARING_AID
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        (deviceType == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            deviceType == AudioDeviceInfo.TYPE_BLE_SPEAKER)
                    ) {
                        BluetoothAudioProfile.LE_AUDIO
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        deviceType == AudioDeviceInfo.TYPE_BLE_BROADCAST
                    ) {
                        BluetoothAudioProfile.LE_AUDIO
                    } else {
                        BluetoothAudioProfile.NONE
                    }
                }
            }
        }
    }

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var currentRouteState: BluetoothAudioRouteState = queryCurrentRouteState()
        private set

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshRouteState()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshRouteState()
        }
    }

    init {
        runCatching {
            audioManager?.registerAudioDeviceCallback(deviceCallback, mainHandler)
        }
    }

    fun refreshRouteState(): BluetoothAudioRouteState {
        val updated = queryCurrentRouteState()
        val prev = currentRouteState
        currentRouteState = updated
        if (updated.isBluetoothActive != prev.isBluetoothActive ||
            updated.profile != prev.profile ||
            updated.deviceName != prev.deviceName
        ) {
            onRouteChanged(updated)
        }
        return updated
    }

    fun queryCurrentRouteState(): BluetoothAudioRouteState {
        val am = audioManager ?: return BluetoothAudioRouteState()
        val outputs = runCatching {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        }.getOrNull().orEmpty()

        // Prioritize A2DP / LE Audio / Hearing Aid over SCO when multiple profiles are registered
        var matchedProfile = BluetoothAudioProfile.NONE
        var matchedName: String? = null

        for (device in outputs) {
            val profile = classifyDeviceType(device.type)
            if (profile != BluetoothAudioProfile.NONE) {
                val isHigherPriority = when {
                    matchedProfile == BluetoothAudioProfile.NONE -> true
                    profile == BluetoothAudioProfile.LE_AUDIO -> true
                    profile == BluetoothAudioProfile.A2DP && matchedProfile == BluetoothAudioProfile.SCO -> true
                    else -> false
                }
                if (isHigherPriority) {
                    matchedProfile = profile
                    val rawName = runCatching { device.productName?.toString()?.trim() }.getOrNull()
                    matchedName = rawName?.takeIf { it.isNotBlank() } ?: profile.badgeEn
                }
            }
        }

        val halLatency = queryHalOutputLatencyMs(am)
        return BluetoothAudioRouteState(
            isBluetoothActive = matchedProfile != BluetoothAudioProfile.NONE,
            profile = matchedProfile,
            deviceName = matchedName,
            halOutputLatencyMs = halLatency
        )
    }

    private fun queryHalOutputLatencyMs(am: AudioManager): Int {
        return runCatching {
            val method = AudioManager::class.java.getMethod("getOutputLatency", Int::class.javaPrimitiveType)
            val latency = method.invoke(am, AudioManager.STREAM_MUSIC) as? Int ?: 0
            latency.coerceIn(0, 1000)
        }.getOrDefault(0)
    }

    fun release() {
        runCatching {
            audioManager?.unregisterAudioDeviceCallback(deviceCallback)
        }
    }
}
