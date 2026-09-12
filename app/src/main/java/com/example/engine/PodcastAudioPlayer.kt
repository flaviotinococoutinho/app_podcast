package com.example.engine

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentTrackTitle: String = "",
    val currentFilePath: String? = null,
    val speed: Float = 1.0f,
    val isBuffering: Boolean = false
)

class PodcastAudioPlayer(private val context: Context, private val coroutineScope: CoroutineScope) {
    private val tag = "PodcastAudioPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun playFile(filePath: String, trackTitle: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(tag, "Audio file does not exist: $filePath")
            return
        }

        try {
            stop()

            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
            }

            mediaPlayer = player

            val duration = player.duration.toLong().coerceAtLeast(0L)
            _playbackState.value = _playbackState.value.copy(
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = duration,
                currentTrackTitle = trackTitle,
                currentFilePath = filePath
            )

            applyPlaybackSpeed(_playbackState.value.speed)

            player.setOnCompletionListener {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    currentPositionMs = _playbackState.value.durationMs
                )
                stopProgressTracking()
            }

            player.start()
            startProgressTracking()
        } catch (e: Exception) {
            Log.e(tag, "Error playing audio file: $filePath", e)
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
            stopProgressTracking()
        } else {
            player.start()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            startProgressTracking()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            val target = positionMs.toInt().coerceIn(0, player.duration)
            player.seekTo(target)
            _playbackState.value = _playbackState.value.copy(currentPositionMs = target.toLong())
        }
    }

    fun forward10() {
        val current = _playbackState.value.currentPositionMs
        val target = (current + 10_000L).coerceAtMost(_playbackState.value.durationMs)
        seekTo(target)
    }

    fun replay10() {
        val current = _playbackState.value.currentPositionMs
        val target = (current - 10_000L).coerceAtLeast(0L)
        seekTo(target)
    }

    fun cycleSpeed() {
        val speeds = listOf(0.8f, 1.0f, 1.25f, 1.5f, 2.0f)
        val currentIndex = speeds.indexOfFirst { kotlin.math.abs(it - _playbackState.value.speed) < 0.05f }
        val nextSpeed = if (currentIndex in 0 until speeds.size - 1) speeds[currentIndex + 1] else speeds[0]
        setSpeed(nextSpeed)
    }

    fun setSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(speed = speed)
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { player ->
                    val params = player.playbackParams
                    params.speed = speed
                    player.playbackParams = params
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to apply playback speed $speed", e)
            }
        }
    }

    fun stop() {
        stopProgressTracking()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w(tag, "Error releasing MediaPlayer", e)
        }
        mediaPlayer = null
        _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPositionMs = 0L)
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = player.currentPosition.toLong(),
                            durationMs = player.duration.toLong().coerceAtLeast(0L)
                        )
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
    }
}
