package com.jiliu.launcher.model

import android.net.Uri

/**
 * Music track model
 */
data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val uri: Uri,
    val albumArt: Uri? = null,
    val size: Long = 0
) {
    val durationFormatted: String
        get() {
            val minutes = duration / 1000 / 60
            val seconds = duration / 1000 % 60
            return String.format("%d:%02d", minutes, seconds)
        }
}

/**
 * Music playback state
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentTrack: MusicTrack? = null,
    val position: Long = 0,
    val duration: Long = 0,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false
)

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

/**
 * Playlist model
 */
data class Playlist(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val tracks: MutableList<MusicTrack> = mutableListOf(),
    val coverUri: Uri? = null
)
