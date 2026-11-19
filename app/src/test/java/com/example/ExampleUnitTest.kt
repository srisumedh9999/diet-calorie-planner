package com.example

import com.example.data.UserProfile
import com.example.ui.calculateBmiValue
import com.example.ui.getBmiHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `calculateBmiValue computes standard BMI accurately`() {
        val bmi = calculateBmiValue(weightKg = 70.0, heightCm = 175.0)
        assertEquals(22.86, bmi, 0.01)
    }

    @Test
    fun `calculateBmiValue returns zero for non-positive inputs`() {
        assertEquals(0.0, calculateBmiValue(0.0, 175.0), 0.001)
        assertEquals(0.0, calculateBmiValue(70.0, 0.0), 0.001)
        assertEquals(0.0, calculateBmiValue(-60.0, 170.0), 0.001)
    }

    @Test
    fun `getBmiHealthStatus returns accurate health status categories`() {
        assertEquals("Enter Metrics", getBmiHealthStatus(0.0).category)
        assertEquals("Underweight", getBmiHealthStatus(17.2).category)
        assertEquals("Healthy Range", getBmiHealthStatus(22.5).category)
        assertEquals("Overweight", getBmiHealthStatus(27.4).category)
        assertEquals("Obese", getBmiHealthStatus(32.1).category)
    }

    @Test
    fun `UserProfile BMR TDEE and macros match goals`() {
        val profile = UserProfile(
            weightKg = 70.0,
            heightCm = 175.0,
            age = 25,
            isMale = true,
            activityLevel = "Moderate",
            goal = "Maintain"
        )
        val bmr = profile.calculateBmr()
        assertEquals(1673.75, bmr, 0.01)

        val tdee = profile.calculateTdee()
        assertEquals((1673.75 * 1.55).toInt(), tdee)

        val loseTdee = profile.copy(goal = "Lose Weight").calculateTdee()
        assertTrue(loseTdee < tdee)

        val gainTdee = profile.copy(goal = "Gain Weight").calculateTdee()
        assertTrue(gainTdee > tdee)

        val macros = profile.getMacros(tdee)
        assertTrue(macros.proteinGrams > 0)
        assertTrue(macros.carbsGrams > 0)
        assertTrue(macros.fatGrams > 0)
    }
}
