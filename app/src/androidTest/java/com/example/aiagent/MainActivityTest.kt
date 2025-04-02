package com.example.aiagent

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.example.aiagent.BaseInstrumentationTest
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test

class MainActivityTest : BaseInstrumentationTest() {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun shouldShowMainInterface() {
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun shouldInitializeComponents() {
        activityRule.scenario.onActivity { activity ->
            activity.aiModelManager shouldNotBe null
        }
    }
}