package com.example.aiagent

sealed class ModelSource {
    data class Asset(val path: String) : ModelSource()
    data class LocalFile(val path: String) : ModelSource()
    data class HuggingFace(val repoId: String, val filename: String) : ModelSource()

    val displayName: String
        get() = when (this) {
            is Asset -> "Built-in: ${path.substringAfterLast("/")}"
            is LocalFile -> "Local: ${path.substringAfterLast("/")}"
            is HuggingFace -> "HuggingFace: $filename"
        }
}