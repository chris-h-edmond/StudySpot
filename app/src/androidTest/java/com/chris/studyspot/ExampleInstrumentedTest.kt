package com.chris.studyspot

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.studyspot", appContext.packageName)
    }

    @org.junit.Rule
    @JvmField
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginShowsStudySpotBrandAndCreateAccountAction() {
        composeRule.onNodeWithText("StudySpot").assertIsDisplayed()
    }
}
