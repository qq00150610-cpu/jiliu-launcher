package com.jiliu.launcher.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jiliu.launcher.App
import com.jiliu.launcher.R
import com.jiliu.launcher.model.MusicTrack
import com.jiliu.launcher.model.PlaybackState
import com.jiliu.launcher.model.RepeatMode
import com.jiliu.launcher.ui.main.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FloatingMusicService : Service() {

    private val binder = MusicBinder()
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private var currentTrack: MusicTrack? = null
    private var isPlaying = false

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun play() {
        isPlaying = true
        updatePlaybackState()
        updateNotification()
    }

    private fun pause() {
        isPlaying = false
        updatePlaybackState()
        updateNotification()
    }

    private fun playNext() {
        // Play next track in playlist
        updatePlaybackState()
        updateNotification()
    }

    private fun playPrevious() {
        // Play previous track in playlist
        updatePlaybackState()
        updateNotification()
    }

    private fun updatePlaybackState() {
        _playbackState.value = PlaybackState(
            isPlaying = isPlaying,
            currentTrack = currentTrack
        )
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, FloatingMusicService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val previousIntent = Intent(this, FloatingMusicService::class.java).apply {
            action = ACTION_PREVIOUS
        }
        val previousPendingIntent = PendingIntent.getService(
            this, 2, previousIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, FloatingMusicService::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            this, 3, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val albumArt = try {
            currentTrack?.albumArt?.let { uri ->
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (e: Exception) {
            null
        }

        return NotificationCompat.Builder(this, App.CHANNEL_MUSIC)
            .setContentTitle(currentTrack?.title ?: "未播放")
            .setContentText(currentTrack?.artist ?: "")
            .setSmallIcon(R.drawable.ic_music)
            .setLargeIcon(albumArt ?: BitmapFactory.decodeResource(resources, R.drawable.ic_music))
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_previous, "上一曲", previousPendingIntent)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "暂停" else "播放",
                playPausePendingIntent
            )
            .addAction(R.drawable.ic_next, "下一曲", nextPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(isPlaying)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun setCurrentTrack(track: MusicTrack) {
        currentTrack = track
        updatePlaybackState()
        updateNotification()
    }

    inner class MusicBinder : Binder() {
        fun getService(): FloatingMusicService = this@FloatingMusicService
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val ACTION_PLAY = "com.jiliu.launcher.action.PLAY"
        const val ACTION_PAUSE = "com.jiliu.launcher.action.PAUSE"
        const val ACTION_NEXT = "com.jiliu.launcher.action.NEXT"
        const val ACTION_PREVIOUS = "com.jiliu.launcher.action.PREVIOUS"
        const val ACTION_STOP = "com.jiliu.launcher.action.STOP"
        private const val NOTIFICATION_ID = 1002
    }
}
