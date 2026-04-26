package com.jiliu.launcher.ui.player

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.load
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ActivityPlayerBinding
import com.jiliu.launcher.service.MusicPlaybackService
import com.jiliu.launcher.util.MediaStoreUtils
import kotlinx.coroutines.*
import java.io.File

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val controller: MediaController?
        get() = if (controllerFuture.isDone) controllerFuture.get() else null

    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentPlaylist = mutableListOf<MediaStoreUtils.MediaItem>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadMusic()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnPlayPause.setOnClickListener {
            controller?.let { mediaController ->
                if (mediaController.isPlaying) {
                    mediaController.pause()
                } else {
                    mediaController.play()
                }
            }
        }

        binding.btnPrevious.setOnClickListener {
            playPrevious()
        }

        binding.btnNext.setOnClickListener {
            playNext()
        }

        binding.btnRepeat.setOnClickListener {
            toggleRepeatMode()
        }

        binding.btnShuffle.setOnClickListener {
            toggleShuffle()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    controller?.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Load local files button
        binding.btnLocalFiles.setOnClickListener {
            // Open file picker for video/audio
        }
    }

    private fun loadMusic() {
        scope.launch {
            try {
                val audioList = MediaStoreUtils.getAllAudio(this@PlayerActivity)
                val videoList = MediaStoreUtils.getAllVideo(this@PlayerActivity)
                
                currentPlaylist.clear()
                currentPlaylist.addAll(audioList)
                currentPlaylist.addAll(videoList)
                
                if (currentPlaylist.isNotEmpty()) {
                    setupPlaylist()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupPlaylist() {
        val sessionToken = SessionToken(this, ComponentName(this, MusicPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            updateUI()
        }, MoreExecutors.directExecutor())
    }

    private fun updateUI() {
        controller?.let { mediaController ->
            // Update play/pause button
            binding.btnPlayPause.setImageResource(
                if (mediaController.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )

            // Update seek bar
            binding.seekBar.max = mediaController.duration.toInt()
            binding.seekBar.progress = mediaController.currentPosition.toInt()

            // Update time
            binding.tvCurrentTime.text = formatTime(mediaController.currentPosition)
            binding.tvTotalTime.text = formatTime(mediaController.duration)

            // Update track info
            val mediaItem = mediaController.currentMediaItem
            mediaItem?.let { item ->
                binding.tvTrackTitle.text = item.mediaMetadata.title ?: "未知曲目"
                binding.tvArtist.text = item.mediaMetadata.artist ?: "未知艺术家"
                
                // Load album art
                item.mediaMetadata.artworkUri?.let { uri ->
                    binding.ivAlbumArt.load(uri)
                }
            }

            // Update repeat mode
            binding.btnRepeat.setImageResource(
                when (mediaController.repeatMode) {
                    Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
                    Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat
                    else -> R.drawable.ic_repeat
                }
            )

            // Update shuffle
            binding.btnShuffle.alpha = if (mediaController.shuffleModeEnabled) 1f else 0.5f
        }
    }

    private fun playTrack(mediaItem: MediaStoreUtils.MediaItem) {
        val item = MediaItem.Builder()
            .setUri(mediaItem.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(mediaItem.name)
                    .setArtist(mediaItem.artist ?: "Unknown")
                    .setAlbumTitle(mediaItem.album ?: "Unknown")
                    .build()
            )
            .build()

        controller?.apply {
            setMediaItem(item)
            prepare()
            play()
        }
    }

    private fun playPrevious() {
        controller?.let { mediaController ->
            if (mediaController.currentPosition > 3000) {
                mediaController.seekTo(0)
            } else {
                mediaController.seekToPreviousMediaItem()
            }
        }
    }

    private fun playNext() {
        controller?.seekToNextMediaItem()
    }

    private fun toggleRepeatMode() {
        controller?.let { mediaController ->
            mediaController.repeatMode = when (mediaController.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            updateUI()
        }
    }

    private fun toggleShuffle() {
        controller?.let { mediaController ->
            mediaController.shuffleModeEnabled = !mediaController.shuffleModeEnabled
            updateUI()
        }
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 1000 / 60) % 60
        val hours = ms / 1000 / 60 / 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateUI()
            binding.root.postDelayed(this, 1000)
        }
    }

    override fun onStart() {
        super.onStart()
        binding.root.post(updateRunnable)
    }

    override fun onStop() {
        super.onStop()
        binding.root.removeCallbacks(updateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaController.releaseFuture(controllerFuture)
        scope.cancel()
    }
}
