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