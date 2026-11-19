package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val weightKg: Double = 70.0,
    val heightCm: Double = 175.0,
    val age: Int = 25,
    val isMale: Boolean = true,
    val activityLevel: String = "Moderate", // "Sedentary", "Light", "Moderate", "Active", "Extreme"
    val goal: String = "Maintain",         // "Lose Weight", "Maintain", "Gain Weight"
    val dietPreference: String = "Non-Veg" // "Veg", "Non-Veg", "Vegan"
) {
    // Standard Mifflin-St Jeor Equation for BMR
    fun calculateBmr(): Double {
        return if (isMale) {
            (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
        } else {
            (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
        }
    }

    // TDEE Multiplier
    fun calculateTdee(): Int {
        val bmr = calculateBmr()
        val factor = when (activityLevel) {
            "Sedentary" -> 1.2
            "Light" -> 1.375
            "Moderate" -> 1.55
            "Active" -> 1.725
            "Extreme" -> 1.9
            else -> 1.55
        }
        val tdee = bmr * factor
        
        // Adjust for goals
        return when (goal) {
            "Lose Weight" -> (tdee - 500).coerceAtLeast(1200.0).toInt()
            "Gain Weight" -> (tdee + 400).toInt()
            else -> tdee.toInt()
        }
    }

    // BMI Formula
    fun calculateBmi(): Double {
        val heightM = heightCm / 100.0
        return if (heightM > 0) weightKg / (heightM * heightM) else 0.0
    }

    // Get Macros breakdown
    fun getMacros(targetCal: Int): MacroBreakdown {
        // Protein: Lose -> 2.2g/kg, Maintain -> 1.8g/kg, Gain -> 2.0g/kg
        // Let's standardise macro %:
        // Lose Weight: 40% Protein, 30% Carbs, 30% Fats
        // Maintain: 25% Protein, 45% Carbs, 30% Fats
        // Gain Weight: 25% Protein, 50% Carbs, 25% Fats
        val proteinPct: Double
        val carbsPct: Double
        val fatPct: Double

        when (goal) {
            "Lose Weight" -> {
                proteinPct = 0.35
                carbsPct = 0.35
                fatPct = 0.30
            }
            "Gain Weight" -> {
                proteinPct = 0.25
                carbsPct = 0.50
                fatPct = 0.25
            }
            else -> {
                proteinPct = 0.25
                carbsPct = 0.45
                fatPct = 0.30
            }
        }

        val proteinCal = targetCal * proteinPct
        val carbsCal = targetCal * carbsPct
        val fatCal = targetCal * fatPct

        return MacroBreakdown(
            proteinGrams = (proteinCal / 4.0).toInt(),
            carbsGrams = (carbsCal / 4.0).toInt(),
            fatGrams = (fatCal / 9.0).toInt(),
            calories = targetCal
        )
    }
}

data class MacroBreakdown(
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val calories: Int
)

@Entity(tableName = "daily_meal_logs")
data class DailyMealLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // String representation format "yyyy-MM-dd"
    val mealName: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "weight_progress_logs")
data class WeightProgressLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // format "yyyy-MM-dd"
    val weightKg: Double,
    val timestamp: Long = System.currentTimeMillis()
)
