package com.example.aiagent.model.cloud

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import com.example.aiagent.model.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Handles cloud-based model execution via OpenRouter API
 */
class CloudModelGateway(
    private val context: Context,
    private val securePrefs: EncryptedSharedPreferences
) {
    private val httpClient = OkHttpClient()

    suspend fun executeModel(modelId: String, input: Any): Result<Any> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = securePrefs.getString(ModelManager.OPENROUTER_API_KEY, null)
                    ?: return@withContext Result.failure<Any>(
                        IllegalStateException("OpenRouter API key not configured")
                    )

                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat")
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(input.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                Result.success(JSONObject(responseBody))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun testConnection(): Boolean {
        return securePrefs.contains(ModelManager.OPENROUTER_API_KEY)
    }
}