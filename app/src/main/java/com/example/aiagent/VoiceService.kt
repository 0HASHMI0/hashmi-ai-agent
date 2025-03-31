package com.example.aiagent

import android.app.Service
import android.content.Intent
import android.media.*
import android.os.IBinder
import kotlinx.coroutines.*
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoiceService : Service() {
    private lateinit var audioRecord: AudioRecord
    private lateinit var audioTrack: AudioTrack
    private var tflite: Interpreter? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isListening = false
    
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initAudio()
    }

    private fun initAudio() {
        val sampleRate = 16000
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        )
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufferSize * 2
        )
        
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            audioFormat,
            minBufferSize,
            AudioTrack.MODE_STREAM
        )
    }

    fun startListening() {
        if (isListening) return
        
        isListening = true
        scope.launch {
            audioRecord.startRecording()
            audioTrack.play()
            
            val buffer = ByteArray(1024)
            while (isListening) {
                val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    processAudio(buffer, bytesRead)
                }
            }
        }
    }

    private val wakeWordDetector by lazy { WakeWordDetector(this) }
    private var isAwake = false
    private val audioHistory = ByteArray(16000 * 2) // 1s buffer
    private var historyPos = 0

    private fun processAudio(buffer: ByteArray, length: Int) {
        // Store audio in rolling buffer
        System.arraycopy(buffer, 0, audioHistory, historyPos, length)
        historyPos = (historyPos + length) % audioHistory.size

        if (wakeWordDetector.processAudio(buffer)) {
            isAwake = true
            sendBroadcast(Intent(ACTION_WAKE_WORD_DETECTED))
        }

        if (isAwake) {
            // Process voice commands
            val inputBuffer = ByteBuffer.wrap(buffer)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
            
            tflite?.run(inputBuffer, ByteBuffer.allocate(1024))
        }
        
        audioTrack.write(buffer, 0, length)
    }

    fun resetWakeState() {
        isAwake = false
    }

    fun stopListening() {
        isListening = false
        audioRecord.stop()
        audioTrack.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        audioRecord.release()
        audioTrack.release()
        tflite?.close()
        scope.cancel()
    }
}
