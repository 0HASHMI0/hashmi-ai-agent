package com.example.aiagent.model

import com.example.aiagent.BaseTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ModelManagerTest : BaseTest() {
    private val mockContext = mockk<android.content.Context>(relaxed = true)
    private val modelManager = ModelManager(mockContext)

    @Test
    fun `should initialize all components`() {
        modelManager.shouldBeInstanceOf<ModelManager>()
    }

    @Test
    fun `should execute local model successfully`() = runTest {
        // Given
        val testInput = "Test input"
        
        // When
        val result = modelManager.executeModel("test-model", testInput, true)
        
        // Then
        result.isSuccess shouldBe true
    }

    @Test
    fun `should handle execution failures gracefully`() = runTest {
        // Given
        val testInput = "Invalid input"
        
        // When 
        val result = modelManager.executeModel("invalid-model", testInput, true)
        
        // Then
        result.isFailure shouldBe true
    }
}