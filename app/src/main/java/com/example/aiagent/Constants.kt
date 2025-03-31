package com.example.aiagent

const val ACTION_AI_RESPONSE = "com.example.aiagent.ACTION_AI_RESPONSE"
const val EXTRA_AI_RESPONSE = "extra_ai_response"
const val ACTION_USER_MESSAGE = "com.example.aiagent.ACTION_USER_MESSAGE"
const val EXTRA_USER_MESSAGE = "extra_user_message"
const val ACTION_WAKE_WORD_DETECTED = "com.example.aiagent.WAKE_WORD_DETECTED"
const val WAKE_WORD = "hey agent"

sealed class ModelSource {
    data class Asset(val path: String) : ModelSource()
    data class LocalFile(val path: String) : ModelSource()
    data class HuggingFace(val repoId: String, val filename: String) : ModelSource()
}
