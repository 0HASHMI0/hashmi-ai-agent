package com.example.aiagent.model

/**
 * Metadata for locally stored models
 */
data class LocalModelInfo(
    val modelId: String,
    val modelPath: String,
    val version: String = "1.0",
    val inputTypes: List<String> = listOf("text"),
    val outputTypes: List<String> = listOf("text"),
    val sizeMB: Int = 0,
    val requiredRAMMB: Int = 0
) {
    fun isCompatible(availableRAMMB: Int): Boolean {
        return availableRAMMB >= requiredRAMMB
    }
}