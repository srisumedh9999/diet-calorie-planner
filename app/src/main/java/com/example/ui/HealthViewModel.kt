package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = HealthRepository(db)

    // Current date for tracking meals
    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    init {
        viewModelScope.launch {
            if (repository.getProfileDirect() == null) {
                repository.saveProfile(UserProfile())
            }
        }
    }

    // Observe user profile, falling back to default while loading
    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    // Observe meals for the current selected date
    val dailyMeals: StateFlow<List<DailyMealLog>> = _selectedDate
        .flatMapLatest { date -> repository.getMealsForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Observe weight history logs
    val weightHistory: StateFlow<List<WeightProgressLog>> = repository.weightHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // State for generated/active diet plan
    private val _dietPlanText = MutableStateFlow<String?>(null)
    val dietPlanText: StateFlow<String?> = _dietPlanText.asStateFlow()

    private val _isGeneratingDietPlan = MutableStateFlow(false)
    val isGeneratingDietPlan: StateFlow<Boolean> = _isGeneratingDietPlan.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()

    // Date navigation
    fun changeSelectedDate(daysOffset: Int) {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val date = format.parse(_selectedDate.value) ?: Date()
            val calendar = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, daysOffset)
            }
            _selectedDate.value = format.format(calendar.time)
        } catch (e: Exception) {
            _selectedDate.value = getCurrentDateString()
        }
    }

    private fun getCurrentDateString(): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(Date())
    }

    // Update profile factors
    fun updateProfile(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        isMale: Boolean,
        activityLevel: String,
        goal: String,
        dietPreference: String
    ) {
        viewModelScope.launch {
            val previousWeight = userProfile.value.weightKg
            val updated = UserProfile(
                id = 1,
                weightKg = weightKg,
                heightCm = heightCm,
                age = age,
                isMale = isMale,
                activityLevel = activityLevel,
                goal = goal,
                dietPreference = dietPreference
            )
            repository.saveProfile(updated)
            if (previousWeight != weightKg || weightHistory.value.isEmpty()) {
                repository.logWeight(WeightProgressLog(weightKg = weightKg, date = getCurrentDateString()))
            }
        }
    }

    // Direct BMI calculator update (weight & height)
    fun updateBmiMeasurements(weightKg: Double, heightCm: Double) {
        if (weightKg <= 0.0 || heightCm <= 0.0) return
        val current = userProfile.value
        updateProfile(
            weightKg = weightKg,
            heightCm = heightCm,
            age = current.age,
            isMale = current.isMale,
            activityLevel = current.activityLevel,
            goal = current.goal,
            dietPreference = current.dietPreference
        )
    }

    // Add meal item
    fun addMeal(name: String, calories: Int, protein: Double, carbs: Double, fat: Double) {
        viewModelScope.launch {
            val meal = DailyMealLog(
                date = _selectedDate.value,
                mealName = name,
                calories = calories,
                proteinGrams = protein,
                carbsGrams = carbs,
                fatGrams = fat
            )
            repository.logMeal(meal)
        }
    }

    // Delete meal
    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            repository.deleteMeal(mealId)
        }
    }

    // Log independent weight record
    fun logWeightUpdate(weight: Double, dateString: String) {
        viewModelScope.launch {
            repository.logWeight(WeightProgressLog(weightKg = weight, date = dateString))
        }
    }

    // Delete weight record
    fun deleteWeightRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteWeightLog(id)
        }
    }

    // Primary function: Generate Diet Plan
    fun generateDietPlan(useAI: Boolean) {
        viewModelScope.launch {
            _isGeneratingDietPlan.value = true
            _generationError.value = null
            _dietPlanText.value = null

            val profile = userProfile.value
            val targetCalories = profile.calculateTdee()
            val macros = profile.getMacros(targetCalories)

            if (useAI) {
                // Read API Key from BuildConfig or let user supply environment
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _generationError.value = "Gemini API key is set to default. Register a key in AI Studio Secrets to unlock real AI wellness content! Defaulting to physical nutrition system plan."
                    // Fallback automatically to high-grade local planner
                    _dietPlanText.value = buildLocalDietPlan(profile, targetCalories, macros)
                    _isGeneratingDietPlan.value = false
                } else {
                    val result = repository.generateAIDietPlan(
                        weightKg = profile.weightKg,
                        heightCm = profile.heightCm,
                        age = profile.age,
                        isMale = profile.isMale,
                        activityLevel = profile.activityLevel,
                        goal = profile.goal,
                        dietPreference = profile.dietPreference,
                        targetCalories = targetCalories,
                        macros = macros,
                        apiKey = apiKey
                    )
                    result.onSuccess { plan ->
                        _dietPlanText.value = plan
                    }.onFailure { error ->
                        _generationError.value = "Gemini AI unavailable (${error.localizedMessage ?: "Network error"}). Showing Standard Diet Plan instead."
                        _dietPlanText.value = buildLocalDietPlan(profile, targetCalories, macros)
                    }
                    _isGeneratingDietPlan.value = false
                }
            } else {
                // Standard programmatic generation
                _dietPlanText.value = buildLocalDietPlan(profile, targetCalories, macros)
                _isGeneratingDietPlan.value = false
            }
        }
    }

    // Extremely rich algorithmic plan generator (Veg, Non-Veg, Vegan, Goals, Gym levels matched)
    private fun buildLocalDietPlan(
        profile: UserProfile,
        calories: Int,
        macros: MacroBreakdown
    ): String {
        val pref = profile.dietPreference
        val goal = profile.goal
        val gym = profile.activityLevel

        val isVeg = pref.equals("Veg", ignoreCase = true)
        val isVegan = pref.equals("Vegan", ignoreCase = true)

        val b = StringBuilder()
        b.append("🌱 **Diet Plan Overview**\n")
        b.append("Nutrition Plan tailored for a secondary target of **$calories Calories**.\n")
        b.append("🎯 **Goal**: $goal | **Gym Level**: $gym (${profile.activityLevel} Activity)\n")
        b.append("📊 **Target Daily Macronutrients**:\n")
        b.append("• **Protein**: ${macros.proteinGrams}g (~${macros.proteinGrams * 4} kcal)\n")
        b.append("• **Carbs**: ${macros.carbsGrams}g (~${macros.carbsGrams * 4} kcal)\n")
        b.append("• **Fats**: ${macros.fatGrams}g (~${macros.fatGrams * 9} kcal)\n\n")

        b.append("💡 **Gym & Nutrition Strategy**:\n")
        when (goal) {
            "Lose Weight" -> {
                b.append("• Maintain a steady 500 kcal deficit. Prioritize protein to protect lean skeletal muscle.\n")
                b.append("• Drink 3-4 liters of water to increase metabolism, suppress hunger, and aid kidney action.\n")
                b.append("• Perform moderate resistance gym training paired with light cardio to optimize weight reduction.\n\n")
            }
            "Gain Weight" -> {
                b.append("• Maintain an anabolic surplus. Focus on protein synthesis and progressive overload in the gym.\n")
                b.append("• Consume calorie-dense whole foods like nuts, avocados, complex carbs, and clean proteins.\n")
                b.append("• Limit heavy cardio; target 3-4 heavy gym lifting sessions per week for steady size gains.\n\n")
            }
            else -> {
                b.append("• Focus on body recomposition. Equalize energy intake with metabolic output for balance.\n")
                b.append("• Keep meal frequencies high, aiming for nourishing snacks to avoid blood sugar drops.\n")
                b.append("• Train with mixed intensity gym sessions to preserve joint longevity, stamina, and agility.\n\n")
            }
        }

        b.append("📝 **Daily Sample Meals**:\n\n")

        // Breakfast
        b.append("🥞 **Breakfast (approx. ${(calories * 0.25).toInt()} kcal)**:\n")
        if (isVegan) {
            b.append("• Tofu scramble (150g) with spinach, bell peppers, and turmeric.\n")
            b.append("• 2 slices of whole grain sprouted toast with 1 tbsp almond abundance butter.\n")
            b.append("• Black coffee or Earl Grey tea.\n")
        } else if (isVeg) {
            b.append("• 3 Scrambled Egg Whites + 1 whole egg OR Paneer bhurji (100g).\n")
            b.append("• 1 cup steel-cut cooked oatmeal topped with fresh berries and chia seeds.\n")
            b.append("• Glass of warm skimmed or almond milk.\n")
        } else {
            b.append("• 3 Whole Eggs (scrambled or boiled) with mushrooms and spinach.\n")
            b.append("• 1 toasted bagel or sourdough slice with avocado smash.\n")
            b.append("• Green tea or black sugar-free coffee.\n")
        }
        b.append("\n")

        // Lunch
        b.append("🍲 **Lunch (approx. ${(calories * 0.35).toInt()} kcal)**:\n")
        if (isVegan) {
            b.append("• Hearty Brown Lentils (1.5 cups) simmered with cumin and diced carrots.\n")
            b.append("• Grilled tempeh cutlets (100g) over a bed of quinoa (1 cup).\n")
            b.append("• Steamed green broccoli and asparagus tossed in sesame oil.\n")
        } else if (isVeg) {
            b.append("• Low-fat Cottage Cheese/Paneer curry (150g) in fresh tomato-basil gravy.\n")
            b.append("• 1 cup basmati brown rice or 2 whole wheat rotis.\n")
            b.append("• Large green garden salad with cucumber, radishes, and lemon drizzle.\n")
        } else {
            b.append("• Herb-roasted chicken breast (150g) or steamed Cod fillet.\n")
            b.append("• Roasted sweet potatoes (150g) seasoned with sea salt and paprika.\n")
            b.append("• Mixed green leaf salad with olive oil dressing (1 tbsp).\n")
        }
        b.append("\n")

        // Snack
        b.append("🍏 **Evening Athletic Snack (approx. ${(calories * 0.15).toInt()} kcal)**:\n")
        if (isVegan) {
            b.append("• Dry roasted almonds (30g) paired with a medium crisp red apple.\n")
            b.append("• 1 scoop Plant Protein isolate shake with water.\n")
        } else if (isVeg) {
            b.append("• 1 cup Low-fat Greek yogurt (Greek style) with sliced walnuts and honey.\n")
            b.append("• Warm roasted salted chickpeas (1/2 cup).\n")
        } else {
            b.append("• Smoked turkey whole wheat roll-up with romaine lettuce.\n")
            b.append("• A handful of raw unsalted cashews or mixed pumpkin seeds.\n")
        }
        b.append("\n")

        // Dinner
        b.append("🥗 **Dinner (approx. ${(calories * 0.25).toInt()} kcal)**:\n")
        if (isVegan) {
            b.append("• Plant-based high protein burger patty or chickpea steak (120g).\n")
            b.append("• Sautéed yellow squash, zucchini, and mushrooms in garlic broth.\n")
            b.append("• 1/2 cup wild rice or steamed cauliflower rice.\n")
        } else if (isVeg) {
            b.append("• Slow-cooked Black Beans or Chickpeas bowl (1.5 cups) topped with diced avocado.\n")
            b.append("• Grilled paneer skewers (100g) alongside cherry tomato relish.\n")
            b.append("• Steamed baby spinach topped with sunflower seeds.\n")
        } else {
            b.append("• Pan-seared Fresh Salmon fillet (150g) packed with rich omega-3 fats.\n")
            b.append("• Air-fried asparagus spears and baby brussels sprouts.\n")
            b.append("• Grilled bell peppers and sweet onions.\n")
        }
        
        return b.toString()
    }
}
