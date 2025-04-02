package com.example.aiagent.utils

import com.example.aiagent.model.LocalModelInfo
import com.example.aiagent.model.ModelSource
import io.mockk.every
import io.mockk.mockk

object TestUtils {
    fun createMockModelInfo(
        id: String = "test-model",
        name: String = "Test Model",
        source: ModelSource = ModelSource.Asset("test_model.tflite")
    ): LocalModelInfo {
        return mockk<LocalModelInfo>().apply {
            every { modelId } returns id
            every { displayName } returns name
            every { modelSource } returns source
        }
    }

    fun createMockContext(): android.content.Context {
        return mockk(relaxed = true)
    }
}