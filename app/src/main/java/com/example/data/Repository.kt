package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class HealthRepository(
    private val db: AppDatabase
) {
    private val userProfileDao = db.userProfileDao()
    private val dailyMealLogDao = db.dailyMealLogDao()
    private val weightProgressLogDao = db.weightProgressLogDao()

    // 1. Profile methods
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfileFlow()
        .flowOn(Dispatchers.IO)

    suspend fun getProfileDirect(): UserProfile? = withContext(Dispatchers.IO) {
        userProfileDao.getUserProfileDirect()
    }

    suspend fun saveProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userProfileDao.insertOrUpdateProfile(profile)
    }

    // 2. Meal progress methods
    fun getMealsForDate(date: String): Flow<List<DailyMealLog>> {
        return dailyMealLogDao.getMealsByDateFlow(date)
            .flowOn(Dispatchers.IO)
    }

    suspend fun logMeal(meal: DailyMealLog) = withContext(Dispatchers.IO) {
        dailyMealLogDao.insertMeal(meal)
    }

    suspend fun deleteMeal(mealId: Long) = withContext(Dispatchers.IO) {
        dailyMealLogDao.deleteMealById(mealId)
    }

    // 3. Weight log progress
    val weightHistory: Flow<List<WeightProgressLog>> = weightProgressLogDao.getAllWeightLogsFlow()
        .flowOn(Dispatchers.IO)

    suspend fun logWeight(weightLog: WeightProgressLog) = withContext(Dispatchers.IO) {
        weightProgressLogDao.insertWeightLog(weightLog)
        
        // Also update weight in user profile so calculator updates automatically
        val profile = userProfileDao.getUserProfileDirect() ?: UserProfile()
        userProfileDao.insertOrUpdateProfile(profile.copy(weightKg = weightLog.weightKg))
    }

    suspend fun deleteWeightLog(id: Long) = withContext(Dispatchers.IO) {
        weightProgressLogDao.deleteWeightLogById(id)
    }

    // 4. Gemini AI Diet Plan Generation
    suspend fun generateAIDietPlan(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        isMale: Boolean,
        activityLevel: String,
        goal: String,
        dietPreference: String,
        targetCalories: Int,
        macros: MacroBreakdown,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        // Build prompt
        val gender = if (isMale) "Male" else "Female"
        val prompt = """
            You are an elite, certified clinical sports nutritionist and expert personal trainer. 
            Generate a personalized, highly effective, clean eating sample diet plan for a client with these attributes:
            - Gender: $gender
            - Weight: $weightKg kg
            - Height: $heightCm cm
            - Age: $age years
            - Activity / Gym level: $activityLevel
            - Primary Physical Goal: $goal
            - Diet preference: $dietPreference (Vegetarian / Vegan / Non-Vegetarian)
            - Estimated Target Daily Intake: $targetCalories Calories
            - Target Macros Required: Protein: ${macros.proteinGrams}g, Carbs: ${macros.carbsGrams}g, Fats: ${macros.fatGrams}g

            Format your response inviting, professional, and clear. Avoid raw Markdown codeblocks or generic AI meta-commentary, and jump straight into the formatted plan. Include:
            1. **Daily Targets**: Briefly state the target stats and why they were chosen for this activity level.
            2. **Gym / Lifestyle Tip**: Tailor a quick 3-sentence tip to their fitness theme (e.g., maximizing protein synthesis and hydration).
            3. **Daily Meal Outline**: Specific breakfast, lunch, snack, and dinner suggestions with ingredient items and approximate macro splits for each meal, strictly adhering to their $dietPreference preference.
            
            Keep it clean, neat, and highly readable.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Gemini returned an empty response. Please verify your API key."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
