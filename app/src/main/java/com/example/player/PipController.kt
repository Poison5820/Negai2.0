package com.example.player

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.R
import kotlin.math.roundToInt

/**
 * Manages Picture-in-Picture (PiP) mode with custom RemoteActions:
 * 1. Rewind step
 * 2. Play / Pause
 * 3. Fast-Forward step
 * While episode switching is simultaneously exposed in the foreground MediaSession notification.
 */
object PipController {

    const val ACTION_PIP_CONTROL = "com.example.kino4k.ACTION_PIP_CONTROL"
    const val EXTRA_PIP_COMMAND = "extra_pip_command"

    const val CMD_REWIND_10 = 1
    const val CMD_PLAY_PAUSE = 2
    const val CMD_FORWARD_10 = 3

    private const val REQ_REWIND = 4101
    private const val REQ_PLAY_PAUSE = 4102
    private const val REQ_FORWARD = 4103

    fun isPipSupported(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    fun updatePipParams(
        activity: Activity,
        isPlayerActive: Boolean,
        isPlaying: Boolean,
        videoWidth: Int,
        videoHeight: Int,
        autoEnterEnabled: Boolean = true,
        seekStepSeconds: Int = 10
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !isPipSupported(activity)) return
        runCatching {
            val params = buildPipParams(
                context = activity,
                isPlayerActive = isPlayerActive,
                isPlaying = isPlaying,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                autoEnterEnabled = autoEnterEnabled,
                seekStepSeconds = seekStepSeconds
            )
            activity.setPictureInPictureParams(params)
        }
    }

    fun enterPipNow(
        activity: Activity,
        isPlaying: Boolean,
        videoWidth: Int,
        videoHeight: Int,
        autoEnterEnabled: Boolean = true,
        seekStepSeconds: Int = 10
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !isPipSupported(activity)) return false
        return runCatching {
            val params = buildPipParams(
                context = activity,
                isPlayerActive = true,
                isPlaying = isPlaying,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                autoEnterEnabled = autoEnterEnabled,
                seekStepSeconds = seekStepSeconds
            )
            activity.enterPictureInPictureMode(params)
        }.getOrDefault(false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildPipParams(
        context: Context,
        isPlayerActive: Boolean,
        isPlaying: Boolean,
        videoWidth: Int,
        videoHeight: Int,
        autoEnterEnabled: Boolean = true,
        seekStepSeconds: Int = 10
    ): PictureInPictureParams {
        val rewindTitle = "${context.getString(R.string.pip_rewind_10)} (${seekStepSeconds} сек)"
        val forwardTitle = "${context.getString(R.string.pip_forward_10)} (${seekStepSeconds} сек)"

        val rewindAction = createRemoteAction(
            context = context,
            iconRes = R.drawable.ic_pip_replay_10,
            title = rewindTitle,
            requestCode = REQ_REWIND,
            command = CMD_REWIND_10
        )

        val playPauseAction = createRemoteAction(
            context = context,
            iconRes = if (isPlaying) R.drawable.ic_pip_pause else R.drawable.ic_pip_play,
            title = context.getString(if (isPlaying) R.string.pip_pause else R.string.pip_play),
            requestCode = REQ_PLAY_PAUSE,
            command = CMD_PLAY_PAUSE
        )

        val forwardAction = createRemoteAction(
            context = context,
            iconRes = R.drawable.ic_pip_forward_10,
            title = forwardTitle,
            requestCode = REQ_FORWARD,
            command = CMD_FORWARD_10
        )

        val rawRatio = if (videoWidth > 0 && videoHeight > 0) {
            (videoWidth.toFloat() / videoHeight.toFloat()).coerceIn(0.45f, 2.35f)
        } else {
            16f / 9f
        }
        val rational = Rational((rawRatio * 1000f).roundToInt(), 1000)

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(rational)
            .setActions(listOf(rewindAction, playPauseAction, forwardAction))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(isPlayerActive && isPlaying && autoEnterEnabled)
            builder.setSeamlessResizeEnabled(true)
        }

        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteAction(
        context: Context,
        iconRes: Int,
        title: String,
        requestCode: Int,
        command: Int
    ): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_PIP_COMMAND, command)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = Icon.createWithResource(context, iconRes)
        return RemoteAction(icon, title, title, pendingIntent)
    }

    fun registerReceiver(
        context: Context,
        onRewind10: () -> Unit,
        onPlayPause: () -> Unit,
        onForward10: () -> Unit
    ): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != ACTION_PIP_CONTROL) return
                when (intent.getIntExtra(EXTRA_PIP_COMMAND, 0)) {
                    CMD_REWIND_10 -> onRewind10()
                    CMD_PLAY_PAUSE -> onPlayPause()
                    CMD_FORWARD_10 -> onForward10()
                }
            }
        }
        val filter = IntentFilter(ACTION_PIP_CONTROL)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        return receiver
    }
}
