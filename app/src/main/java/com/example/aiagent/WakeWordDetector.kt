package com.example.aiagent

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WakeWordDetector(context: Context) {
    private val tflite: Interpreter
    private val audioBuffer = ByteBuffer.allocateDirect(16000 * 2) // 1s of 16kHz audio

    init {
        val model = context.assets.open("wakeword_model.tflite").use { 
            ByteBuffer.allocateDirect(it.available()).apply {
                it.read(array())
            }
        }
        tflite = Interpreter(model)
    }

    fun processAudio(input: ByteArray): Boolean {
        audioBuffer.rewind()
        audioBuffer.put(input)
        val output = arrayOf(0f)
        tflite.run(audioBuffer, output)
        return output[0] > 0.8f // Detection threshold
    }

    fun close() {
        tflite.close()
    }
}
