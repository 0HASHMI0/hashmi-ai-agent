package com.example.aiagent

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.MockKAnnotations
import org.junit.jupiter.api.BeforeEach

abstract class BaseTest : BehaviorSpec() {
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }
}