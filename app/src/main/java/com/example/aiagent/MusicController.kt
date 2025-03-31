package com.example.aiagent

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri

class MusicController(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null
    
    fun play(uri: Uri) {
        stop()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, uri)
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            prepare()
            start()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun setVolume(level: Float) {
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (level * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(),
            0
        )
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
}
