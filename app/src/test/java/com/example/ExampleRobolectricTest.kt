package com.example

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.ui.BmiCalculatorCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Diet & Calorie Planner", appName)
    }

    @Test
    fun `bmi calculator updates result and health status indicator on weight and height input`() {
        var appliedWeight: Double? = null
        var appliedHeight: Double? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    BmiCalculatorCard(
                        weightKg = 70.0,
                        heightCm = 175.0,
                        onApplyMetrics = { w, h ->
                            appliedWeight = w
                            appliedHeight = h
                        }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Default 70kg, 175cm -> BMI 22.9, Healthy Range
        composeTestRule.onNodeWithTag("bmi_result_value").assertTextContains("22.9")
        composeTestRule.onNodeWithTag("bmi_status_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Healthy Range").assertIsDisplayed()

        // Update to Overweight (90kg, 175cm -> 29.4)
        composeTestRule.onNodeWithTag("bmi_weight_input").performTextReplacement("90")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bmi_result_value").assertTextContains("29.4")
        composeTestRule.onNodeWithText("Overweight").assertIsDisplayed()

        // Update to Obese (110kg, 175cm -> 35.9)
        composeTestRule.onNodeWithTag("bmi_weight_input").performTextReplacement("110")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bmi_result_value").assertTextContains("35.9")
        composeTestRule.onNodeWithText("Obese").assertIsDisplayed()

        // Update to Underweight (50kg, 180cm -> 15.4)
        composeTestRule.onNodeWithTag("bmi_weight_input").performTextReplacement("50")
        composeTestRule.onNodeWithTag("bmi_height_input").performTextReplacement("180")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bmi_result_value").assertTextContains("15.4")
        composeTestRule.onNodeWithText("Underweight").assertIsDisplayed()

        // Save metrics
        composeTestRule.onNodeWithTag("bmi_calculate_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertNotNull(appliedWeight)
        assertNotNull(appliedHeight)
        assertEquals(50.0, appliedWeight!!, 0.01)
        assertEquals(180.0, appliedHeight!!, 0.01)
    }

    @Test
    fun `bmi calculator supports unit toggle between metric and imperial`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    BmiCalculatorCard(
                        weightKg = 70.0,
                        heightCm = 175.0,
                        onApplyMetrics = { _, _ -> }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("bmi_unit_imperial").performClick()
        composeTestRule.waitForIdle()
        // Converted BMI should stay ~22.9 and Healthy Range
        composeTestRule.onNodeWithTag("bmi_result_value").assertTextContains("22.9")
        composeTestRule.onNodeWithText("Healthy Range").assertIsDisplayed()
    }
}
