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
/**
 * Central manager for AI model operations including:
 * - Loading and executing models
 * - Routing between local/cloud execution
 * - Managing model credentials
 *
 * @property context Android context for system services
 *
 * Example:
 * ```
 * val modelManager = ModelManager(context)
 * val result = modelManager.executeModel("model1", inputData)
 * ```
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

    private val localLoader = object : LocalModelLoader {
        private val modelCache = mutableMapOf<String, Any>()
        private val loadedModels = mutableListOf<LocalModelInfo>()
        
        override suspend fun loadModel(modelId: String): Boolean {
            return try {
                // Implementation for loading model
                loadedModels.add(LocalModelInfo(modelId, "1.0"))
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun executeModel(modelId: String, input: Any): Result<Any> {
            return try {
                // Implementation for local model execution
                Result.success("Local model result")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override fun getLoadedModels(): List<LocalModelInfo> {
            return loadedModels.toList()
        }

        override fun clearModel(modelId: String) {
            loadedModels.removeAll { it.modelId == modelId }
            modelCache.remove(modelId)
        }

        suspend fun getAvailableModels(): List<String> {
            return listOf("model1", "model2")
        }
    }
    
    private val cloudGateway = CloudModelGateway(context, securePrefs as EncryptedSharedPreferences)
    private val modelRouter = ModelRouter(context, localLoader)

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
