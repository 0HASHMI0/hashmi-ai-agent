package com.example.aiagent.model.routing

import android.content.Context
import com.example.aiagent.model.LocalModelInfo
import com.example.aiagent.model.ModelRoute
import com.example.aiagent.model.local.LocalModelLoader

/**
 * Decides where to execute models based on performance, cost, and availability
 */
/**
 * Decides execution path between local and cloud models.
 * Implements fallback logic and performance optimization.
 *
 * @property localLoader Local model execution handler
 * @property cloudGateway Remote model execution handler
 *
 * Example:
 * ```
 * val router = ModelRouter(localLoader, cloudGateway)
 * val result = router.execute("model1", input, preferLocal = true)
 * ```
 */
class ModelRouter(
    private val context: Context,
    private val localModelLoader: LocalModelLoader
) {
    fun decideExecutionRoute(
        modelId: String,
        preferLocal: Boolean = true
    ): ModelRoute {
        return if (preferLocal && localModelLoader.getLoadedModels().any { it.modelId == modelId }) {
            ModelRoute.Local(LocalModelInfo(modelId, "local"))
        } else {
            ModelRoute.Cloud("https://openrouter.ai/api/v1/models/$modelId")
        }
    }

    suspend fun evaluateRoutePerformance(route: ModelRoute): Double {
        return when (route) {
            is ModelRoute.Local -> 1.0 // Full performance score for local
            is ModelRoute.Cloud -> 0.8 // Slightly penalize cloud latency
        }
    }

    fun getCostEstimate(route: ModelRoute): Double {
        return when (route) {
            is ModelRoute.Local -> 0.0 // No cost for local execution
            is ModelRoute.Cloud -> 0.001 // Estimated cost per token
        }
    }
}