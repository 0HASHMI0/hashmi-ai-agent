package com.example.aiagent.model

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.aiagent.model.local.LocalModelLoader
import com.example.aiagent.model.cloud.CloudModelGateway
import com.example.aiagent.model.routing.ModelRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Central manager for hybrid local/cloud AI model operations
 */
class ModelManager(context: Context) {
    // Secure credential storage
    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "model_credentials",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val localLoader = LocalModelLoaderImpl(context)
    private val cloudGateway = CloudModelGateway(context, securePrefs)
    private val modelRouter = ModelRouter(context)

    suspend fun executeModel(
        modelId: String,
        input: Any,
        preferLocal: Boolean = true
    ): Result<Any> = withContext(Dispatchers.IO) {
        try {
            // Get routing decision
            val route = modelRouter.decideExecutionRoute(modelId, preferLocal)
            
            when (route) {
                is ModelRoute.Local -> localLoader.executeModel(modelId, input)
                is ModelRoute.Cloud -> cloudGateway.executeModel(modelId, input)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun storeCloudCredential(key: String, value: String) {
        securePrefs.edit().putString(key, value).apply()
    }

    companion object {
        const val OPENROUTER_API_KEY = "openrouter_api_key"
    }
}

sealed class ModelRoute {
    data class Local(val modelInfo: LocalModelInfo) : ModelRoute()
    data class Cloud(val endpoint: String) : ModelRoute()
}
