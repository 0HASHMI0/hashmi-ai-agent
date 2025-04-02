package com.example.aiagent

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.aiagent.model.local.LocalModelLoader
import com.example.aiagent.model.local.LocalModelLoaderImpl
import com.example.aiagent.ModelSource

class AIModelManager(context: Context) {
    private val OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat"
    private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    private var tflite: Interpreter? = null
    private val appContext = context.applicationContext
    val client = OkHttpClient()
    private var openRouterKey: String? = null
    private val localModelLoader = LocalModelLoaderImpl(appContext)

    suspend fun loadModel(modelSource: ModelSource) {
        withContext(Dispatchers.IO) {
            try {
                val modelFile = when (modelSource) {
                    is ModelSource.Asset -> loadFromAssets(modelSource.path)
                    is ModelSource.LocalFile -> {
                        if (!localModelLoader.loadModel(modelSource.path)) {
                            throw IOException("Model not found")
                        }
                        val path = "${appContext.filesDir}/${modelSource.path}"
                        loadFromFile(path)
                    }
                    is ModelSource.HuggingFace -> {
                        val localPath = "${modelSource.repoId}_${modelSource.filename}"
                        if (localModelLoader.loadModel(localPath)) {
                            loadFromFile("${appContext.filesDir}/$localPath")
                        } else {
                            val model = downloadModel(modelSource.repoId, modelSource.filename)
                            if (!localModelLoader.saveModel(localPath, model.array())) {
                                throw IOException("Failed to save downloaded model")
                            }
                            model
                        }
                    }
                }
                tflite = Interpreter(modelFile)
            } catch (e: Exception) {
                throw RuntimeException("Failed to load model: ${e.message}")
            }
        }
    }

    private fun loadFromAssets(path: String): MappedByteBuffer {
        val fd = appContext.assets.openFd(path)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        )
    }

    private fun loadFromFile(path: String): MappedByteBuffer {
        val file = File(path)
        return FileInputStream(file).channel.map(
            FileChannel.MapMode.READ_ONLY,
            0,
            file.length()
        )
    }

    private suspend fun downloadModel(repoId: String, filename: String): MappedByteBuffer {
        return withContext(Dispatchers.IO) {
            val url = "https://huggingface.co/$repoId/resolve/main/$filename"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("Failed to download model: ${response.code}")
            }

            val tempFile = File.createTempFile("model", ".tflite")
            response.body?.byteStream()?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            loadFromFile(tempFile.absolutePath)
        }
    }

    private fun MappedByteBuffer.array(): ByteArray {
        val array = ByteArray(remaining())
        get(array)
        return array
    }

    fun setOpenRouterKey(key: String) {
        openRouterKey = key
    }

    suspend fun generateResponse(input: Any): String {
        return withContext(Dispatchers.Default) {
            try {
                when (input) {
                    is String -> {
                        if (openRouterKey != null) {
                            queryOpenRouter(input)
                        } else {
                            runLocalModel(input)
                        }
                    }
                    is ByteArray -> {
                        // Handle image/audio input
                        processMultimodalInput(input)
                    }
                    else -> "Unsupported input type"
                }
            } catch (e: Exception) {
                "Error processing request: ${e.message}"
            }
        }
    }

    private suspend fun queryOpenRouter(prompt: String): String {
        try {
            val json = JSONObject().apply {
                put("model", "openai/gpt-3.5-turbo")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            val request = Request.Builder()
                .url(OPENROUTER_API_URL)
                .header("Authorization", "Bearer $openRouterKey")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("OpenRouter API error: ${response.code}")
            }

            return response.body?.use { body ->
                val responseJson = JSONObject(body.string())
                responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } ?: throw IOException("Empty response body")
        } catch (e: Exception) {
            throw IOException("Failed to query OpenRouter: ${e.message}")
        }
    }



    private fun runLocalModel(input: String): String {
        val inputBuffer = ByteBuffer.allocateDirect(input.length)
            .order(ByteOrder.nativeOrder())
            .put(input.toByteArray())
        
        val outputBuffer = ByteBuffer.allocateDirect(256)
            .order(ByteOrder.nativeOrder())

        tflite?.run(inputBuffer, outputBuffer)
        
        outputBuffer.rewind()
        val outputBytes = ByteArray(outputBuffer.remaining())
        outputBuffer.get(outputBytes)
        return String(outputBytes).trim()
    }

    private suspend fun processMultimodalInput(input: ByteArray): String {
        return withContext(Dispatchers.IO) {
            try {
                // Basic implementation for image/audio processing
                "Processed multimodal input (${input.size} bytes)"
            } catch (e: Exception) {
                "Error processing input: ${e.message}"
            }
        }
    }

    fun close() {
        tflite?.close()
    }

}
