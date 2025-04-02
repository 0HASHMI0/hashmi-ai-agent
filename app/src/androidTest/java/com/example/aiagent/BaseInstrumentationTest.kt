package com.example.aiagent

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.core.spec.style.BehaviorSpec
import org.junit.runner.RunWith
import org.koin.test.KoinTest

@RunWith(AndroidJUnit4::class)
abstract class BaseInstrumentationTest : BehaviorSpec(), KoinTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val testDispatcher = StandardTestDispatcher()
}