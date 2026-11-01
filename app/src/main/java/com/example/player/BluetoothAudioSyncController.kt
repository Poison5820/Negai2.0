package com.example.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Immutable
import kotlin.math.abs

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
    val halOutputLatencyMs: Int = 0,
    val isGamingMode: Boolean = false,
    val measuredLatencyMs: Long = 0L
) {
    fun displayLatencyLabel(isEnglish: Boolean): String {
        if (!isBluetoothActive) {
            return if (isEnglish) "0 ms (Speaker / Wired)" else "0 мс (Динамик / Провод)"
        }
        val ms = when {
            measuredLatencyMs > 0L -> measuredLatencyMs
            halOutputLatencyMs > 0 -> halOutputLatencyMs.toLong()
            else -> 150L
        }
        val gamingSuffix = if (isGamingMode) {
            if (isEnglish) " • Gaming Mode (Low Latency)" else " • Игровой режим (Низкая задержка)"
        } else {
            if (isEnglish) " • Standard" else " • Стандартный режим"
        }
        return "${ms} ${if (isEnglish) "ms" else "мс"}$gamingSuffix"
    }
}

/**
 * Monitors active audio output routes via [AudioManager] + [AudioDeviceCallback] and computes
 * accurate Bluetooth audio latency compensation in real time.
 *
 * When the user toggles "Gaming Mode" (or Low Latency mode) on their Bluetooth earbuds,
 * the Android HAL buffer latency reported by [AudioManager.getOutputLatency] shrinks
 * from standard ~150-200 ms down to ~40-70 ms. This controller dynamically detects the drop
 * and adjusts the player's MediaClock offset on the fly without requiring any manual toggle.
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
         *
         * If the Android HAL reports a measured output latency (e.g. 45 ms in Gaming Mode or 160 ms in standard mode),
         * that measured value is used directly.
         */
        fun computeAutoBluetoothOffsetMs(
            route: BluetoothAudioRouteState,
            autoSyncEnabled: Boolean = true,
            userConfiguredA2dpOffsetMs: Long = DEFAULT_BT_OFFSET_MS
        ): Long {
            if (!autoSyncEnabled || !route.isBluetoothActive) return 0L

            val clampedUserOffset = clampBluetoothOffsetMs(userConfiguredA2dpOffsetMs)
            // If the user explicitly selected a non-default preset (e.g. -45ms Gaming Mode, -60ms LE Audio, -200ms LDAC, or 0ms),
            // honor their selected preset directly so proprietary TWS Gaming Mode works even when Android HAL caches mixer latency.
            if (clampedUserOffset != DEFAULT_BT_OFFSET_MS) {
                return clampedUserOffset
            }

            val measured = when {
                route.measuredLatencyMs > 0L -> route.measuredLatencyMs
                route.halOutputLatencyMs > 0 -> route.halOutputLatencyMs.toLong()
                else -> 0L
            }

            if (measured > 0L) {
                return -clampBluetoothOffsetMs(measured)
            }

            return when (route.profile) {
                BluetoothAudioProfile.NONE -> 0L
                BluetoothAudioProfile.A2DP -> clampedUserOffset
                BluetoothAudioProfile.LE_AUDIO -> {
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
            autoSyncEnabled: Boolean = true,
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

    private val latencyPollRunnable = object : Runnable {
        override fun run() {
            if (currentRouteState.isBluetoothActive) {
                val am = audioManager
                if (am != null) {
                    val currentHal = queryHalOutputLatencyMs(am)
                    if (abs(currentHal - currentRouteState.halOutputLatencyMs) >= 6) {
                        refreshRouteState()
                    }
                }
                mainHandler.postDelayed(this, 1500L)
            }
        }
    }

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
        if (currentRouteState.isBluetoothActive) {
            mainHandler.postDelayed(latencyPollRunnable, 1500L)
        }
    }

    fun refreshRouteState(): BluetoothAudioRouteState {
        val updated = queryCurrentRouteState()
        val prev = currentRouteState
        currentRouteState = updated

        if (updated.isBluetoothActive && !prev.isBluetoothActive) {
            mainHandler.removeCallbacks(latencyPollRunnable)
            mainHandler.postDelayed(latencyPollRunnable, 1500L)
        } else if (!updated.isBluetoothActive && prev.isBluetoothActive) {
            mainHandler.removeCallbacks(latencyPollRunnable)
        }

        val latencyDiff = abs(updated.halOutputLatencyMs - prev.halOutputLatencyMs)
        if (updated.isBluetoothActive != prev.isBluetoothActive ||
            updated.profile != prev.profile ||
            updated.deviceName != prev.deviceName ||
            updated.isGamingMode != prev.isGamingMode ||
            latencyDiff >= 6
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
        val isGaming = matchedProfile != BluetoothAudioProfile.NONE && (
            (halLatency in 1..85) || matchedProfile == BluetoothAudioProfile.LE_AUDIO
        )
        val measuredMs = halLatency.toLong().coerceAtLeast(0L)

        return BluetoothAudioRouteState(
            isBluetoothActive = matchedProfile != BluetoothAudioProfile.NONE,
            profile = matchedProfile,
            deviceName = matchedName,
            halOutputLatencyMs = halLatency,
            isGamingMode = isGaming,
            measuredLatencyMs = measuredMs
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
        mainHandler.removeCallbacks(latencyPollRunnable)
        runCatching {
            audioManager?.unregisterAudioDeviceCallback(deviceCallback)
        }
    }
}
