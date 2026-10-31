package com.example.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity

/**
 * Foreground MediaSessionService powering the system MediaStyle player notification
 * with Previous Episode (`Предыдущая серия`), Play/Pause, and Next Episode (`Следующая серия`)
 * controls both in background and during Picture-in-Picture (PiP) playback.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var engine: PlayerEngine

    private val playerSwapListener: (ExoPlayer) -> Unit = { newExoPlayer ->
        mediaSession?.player = wrapForEpisodeNotification(newExoPlayer)
    }

    override fun onCreate() {
        super.onCreate()
        engine = PlayerEngine.getInstance(this)
        engine.registerPlayerSwapListener(playerSwapListener)

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, wrapForEpisodeNotification(engine.exoPlayer))
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    /**
     * Wraps ExoPlayer so the notification's Previous button switches directly to the
     * previous episode (rather than only rewinding to 0:00) and keeps Previous/Next
     * episode commands advertised in the system media notification.
     */
    private fun wrapForEpisodeNotification(exoPlayer: ExoPlayer): Player {
        return object : ForwardingPlayer(exoPlayer) {
            override fun seekToPrevious() {
                if (hasPreviousMediaItem()) {
                    seekToPreviousMediaItem()
                } else {
                    seekTo(0L)
                }
            }

            override fun seekToNext() {
                if (hasNextMediaItem()) {
                    seekToNextMediaItem()
                }
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands()
                    .buildUpon()
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> mediaItemCount > 1 || currentPosition > 0L
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> hasNextMediaItem()
                    else -> super.isCommandAvailable(command)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        engine.unregisterPlayerSwapListener(playerSwapListener)
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
