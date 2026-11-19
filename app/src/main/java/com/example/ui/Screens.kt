package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyMealLog
import com.example.data.UserProfile
import com.example.data.WeightProgressLog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainAppScreen(viewModel: HealthViewModel) {
    var selectedTab by remember { mutableStateOf(0) }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val dailyMeals by viewModel.dailyMeals.collectAsStateWithLifecycle()
    val weightHistory by viewModel.weightHistory.collectAsStateWithLifecycle()
    val currentSelectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    val bmr = userProfile.calculateBmr()
    val tdee = userProfile.calculateTdee()
    val macros = userProfile.getMacros(tdee)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Calculator") },
                    label = { Text("Calculator") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = MintTertiary,
                        indicatorColor = MintTertiary,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Daily Tracker") },
                    label = { Text("Tracker") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = MintTertiary,
                        indicatorColor = MintTertiary,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Diet Plan") },
                    label = { Text("Diet Plan") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = MintTertiary,
                        indicatorColor = MintTertiary,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
            ) {
                when (selectedTab) {
                    0 -> CalculatorTab(
                        profile = userProfile,
                        tdee = tdee,
                        bmr = bmr,
                        onUpdateBmiMetrics = { w, h ->
                            viewModel.updateBmiMeasurements(w, h)
                        },
                        onUpdateProfile = { w, h, a, g, act, goal, pref ->
                            viewModel.updateProfile(w, h, a, g, act, goal, pref)
                        }
                    )
                    1 -> TrackerTab(
                        meals = dailyMeals,
                        targetCalories = tdee,
                        macros = macros,
                        dateString = currentSelectedDate,
                        currentWeightKg = userProfile.weightKg,
                        weightHistory = weightHistory,
                        onChangeDate = { offset -> viewModel.changeSelectedDate(offset) },
                        onAddMeal = { n, c, p, carb, f -> viewModel.addMeal(n, c, p, carb, f) },
                        onDeleteMeal = { id -> viewModel.deleteMeal(id) },
                        onAddWeight = { w -> viewModel.logWeightUpdate(w, currentSelectedDate) },
                        onDeleteWeight = { id -> viewModel.deleteWeightRecord(id) }
                    )
                    2 -> DietPlanTab(
                        viewModel = viewModel,
                        profile = userProfile
                    )
                }
            }
        }
    }
}

// ======================== TAB 1: CALCULATOR ========================

@Composable
fun CalculatorTab(
    profile: UserProfile,
    tdee: Int,
    bmr: Double,
    onUpdateBmiMetrics: (Double, Double) -> Unit,
    onUpdateProfile: (Double, Double, Int, Boolean, String, String, String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "METABOLIC PROFILE",
                    color = MintTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Health Intelligence",
                    color = OnSurfaceLight,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { showEditDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("edit_profile_button")
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Update", color = Color.White)
            }
        }

        // Interactive BMI Calculator Component
        BmiCalculatorCard(
            weightKg = profile.weightKg,
            heightCm = profile.heightCm,
            onApplyMetrics = onUpdateBmiMetrics
        )

        // Energy Requirements (BMR & TDEE) Block
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "DAILY ENERGY REQUIREMENTS",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    EnergyRequirementCard(
                        title = "Alimentary TDEE Target",
                        value = "$tdee",
                        sub = "kcal / day",
                        desc = "Total daily energy needed according to fitness goal: ${profile.goal}.",
                        modifier = Modifier.weight(1f),
                        primaryColor = MintTertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    EnergyRequirementCard(
                        title = "Basal Metabolic Rate",
                        value = "${bmr.toInt()}",
                        sub = "kcal / day",
                        desc = "Minimum energy needed to maintain basic bodily functions.",
                        modifier = Modifier.weight(1f),
                        primaryColor = AccentGold
                    )
                }
            }
        }

        // Macro Splits Card Block
        val macros = profile.getMacros(tdee)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "OPTIMIZED TARGET MACRONUTRIENTS",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                MacroRatioBar(
                    protein = macros.proteinGrams,
                    carbs = macros.carbsGrams,
                    fat = macros.fatGrams
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MacroBreakdownRow(name = "Protein", grams = "${macros.proteinGrams}g", cal = "${macros.proteinGrams * 4} kcal", color = MintTertiary)
                    MacroBreakdownRow(name = "Carbs", grams = "${macros.carbsGrams}g", cal = "${macros.carbsGrams * 4} kcal", color = AccentGold)
                    MacroBreakdownRow(name = "Fats", grams = "${macros.fatGrams}g", cal = "${macros.fatGrams * 9} kcal", color = SoftRed)
                }
            }
        }

        // User profile review
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("YOUR BASE PHYSICAL METRICS", color = MintTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProfileFeatureBadge(label = "Weight", value = "${profile.weightKg} kg")
                    ProfileFeatureBadge(label = "Height", value = "${profile.heightCm} cm")
                    ProfileFeatureBadge(label = "Age", value = "${profile.age} yr")
                    ProfileFeatureBadge(label = "Goal", value = profile.goal)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProfileFeatureBadge(label = "Gym Level", value = profile.activityLevel)
                    ProfileFeatureBadge(label = "Diet Style", value = profile.dietPreference)
                    ProfileFeatureBadge(label = "Gender", value = if (profile.isMale) "Male" else "Female")
                }
            }
        }
    }

    // Dialog for Updating Profile
    if (showEditDialog) {
        ProfileEditDialog(
            currentProfile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { w, h, a, g, act, goal, pref ->
                onUpdateProfile(w, h, a, g, act, goal, pref)
                showEditDialog = false
            }
        )
    }
}



@Composable
fun EnergyRequirementCard(
    title: String,
    value: String,
    sub: String,
    desc: String,
    modifier: Modifier = Modifier,
    primaryColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BackgroundDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = primaryColor, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text(sub, color = TextGray, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(desc, color = TextGray, fontSize = 9.sp, lineHeight = 12.sp)
        }
    }
}

@Composable
fun MacroRatioBar(protein: Int, carbs: Int, fat: Int) {
    val totalGrams = (protein + carbs + fat).coerceAtLeast(1)
    val pPr = protein.toFloat() / totalGrams
    val cPr = carbs.toFloat() / totalGrams
    val fPr = fat.toFloat() / totalGrams

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
        ) {
            if (pPr > 0) Surface(modifier = Modifier.weight(pPr), color = MintTertiary) {}
            if (cPr > 0) Surface(modifier = Modifier.weight(cPr), color = AccentGold) {}
            if (fPr > 0) Surface(modifier = Modifier.weight(fPr), color = SoftRed) {}
        }
    }
}

@Composable
fun MacroBreakdownRow(name: String, grams: String, cal: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(name, color = OnSurfaceLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("$grams ($cal)", color = TextGray, fontSize = 10.sp)
        }
    }
}

@Composable
fun ProfileFeatureBadge(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundDark.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = TextGray, fontSize = 9.sp)
            Text(value, color = OnSurfaceLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ======================== TAB 2: MEAL & WEIGHT TRACKER ========================

@Composable
fun TrackerTab(
    meals: List<DailyMealLog>,
    targetCalories: Int,
    macros: com.example.data.MacroBreakdown,
    dateString: String,
    currentWeightKg: Double,
    weightHistory: List<WeightProgressLog>,
    onChangeDate: (Int) -> Unit,
    onAddMeal: (String, Int, Double, Double, Double) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onAddWeight: (Double) -> Unit,
    onDeleteWeight: (Long) -> Unit
) {
    var showAddMealDialog by remember { mutableStateOf(false) }
    var showAddWeightDialog by remember { mutableStateOf(false) }

    val consumedCalories = meals.sumOf { it.calories }
    val consumedProtein = meals.sumOf { it.proteinGrams }
    val consumedCarbs = meals.sumOf { it.carbsGrams }
    val consumedFat = meals.sumOf { it.fatGrams }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Selector Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onChangeDate(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Day", tint = MintTertiary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "DAILY MEAL RECORD", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (dateString == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) "Today" else dateString,
                        color = OnSurfaceLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { onChangeDate(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", tint = MintTertiary)
                }
            }
        }

        // Circular Calorie Rings / Gauges Component
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ring Drawing
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = if (targetCalories > 0) consumedCalories.toFloat() / targetCalories else 0f
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Track
                            drawArc(
                                color = BackgroundDark,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Filled
                            drawArc(
                                color = TrackerRingColor,
                                startAngle = -90f,
                                sweepAngle = (progress * 360).coerceAtMost(360f),
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$consumedCalories",
                                color = OnSurfaceLight,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "of $targetCalories",
                                color = TextGray,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "kcal",
                                color = MintTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Side stats showing protein, carbs, fat progress values
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroLinearTracker(
                            name = "Protein Progress",
                            consumed = consumedProtein.toInt(),
                            target = macros.proteinGrams,
                            color = MintTertiary
                        )
                        MacroLinearTracker(
                            name = "Carbs Progress",
                            consumed = consumedCarbs.toInt(),
                            target = macros.carbsGrams,
                            color = AccentGold
                        )
                        MacroLinearTracker(
                            name = "Fat Progress",
                            consumed = consumedFat.toInt(),
                            target = macros.fatGrams,
                            color = SoftRed
                        )
                    }
                }
            }
        }

        // Section header for food logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MEAL LOG ENTRIES", color = OnSurfaceLight, fontSize = 16.sp, fontWeight = FontWeight.Black)
                IconButton(
                    onClick = { showAddMealDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ForestGreenPrimary)
                        .testTag("add_meal_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Meal", tint = Color.White)
                }
            }
        }

        // Meal Lists items
        if (meals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Empty Meals", tint = TextGray, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No food logged for today yet.", color = TextGray, fontSize = 13.sp)
                        Text("Tap '+' to log your breakfast, lunch or snack!", color = MintTertiary, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(meals, key = { it.id }) { meal ->
                MealLogItemCard(meal = meal, onDelete = { onDeleteMeal(meal.id) })
            }
        }

        // Weight tracking section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("WEIGHT CHECK-INS & METRICS", color = OnSurfaceLight, fontSize = 16.sp, fontWeight = FontWeight.Black)
                IconButton(
                    onClick = { showAddWeightDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .testTag("add_weight_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log Weight", tint = MintTertiary)
                }
            }
        }

        if (weightHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Text(
                        "No weight entries recorded. Tap '+' or calculate BMI to log check-ins.",
                        color = TextGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val sortedWeight = weightHistory.sortedByDescending { it.timestamp }
            items(sortedWeight, key = { it.id }) { weightRecord ->
                WeightHistoryCard(weightRecord = weightRecord, onDelete = { onDeleteWeight(weightRecord.id) })
            }
        }
    }

    // Modal dialog to Add a Meal
    if (showAddMealDialog) {
        MealAddDialog(
            onDismiss = { showAddMealDialog = false },
            onSave = { n, c, p, carb, f ->
                onAddMeal(n, c, p, carb, f)
                showAddMealDialog = false
            }
        )
    }

    // Modal dialog to Log Weight Check-in
    if (showAddWeightDialog) {
        WeightLogDialog(
            initialWeightKg = currentWeightKg,
            onDismiss = { showAddWeightDialog = false },
            onSave = { weight ->
                onAddWeight(weight)
                showAddWeightDialog = false
            }
        )
    }
}

@Composable
fun MacroLinearTracker(name: String, consumed: Int, target: Int, color: Color) {
    val progress = if (target > 0) consumed.toFloat() / target else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, color = OnSurfaceLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("$consumed / $target g", color = TextGray, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = BackgroundDark,
        )
    }
}

@Composable
fun MealLogItemCard(meal: DailyMealLog, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.mealName, color = OnSurfaceLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${meal.calories} kcal", color = MintTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("P: ${meal.proteinGrams.toInt()}g", color = TextGray, fontSize = 11.sp)
                    Text("C: ${meal.carbsGrams.toInt()}g", color = TextGray, fontSize = 11.sp)
                    Text("F: ${meal.fatGrams.toInt()}g", color = TextGray, fontSize = 11.sp)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Log", tint = SoftRed)
            }
        }
    }
}

@Composable
fun WeightHistoryCard(weightRecord: WeightProgressLog, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = "Trend", tint = MintTertiary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Verified Gym Weight Info", color = TextGray, fontSize = 10.sp)
                Text("${weightRecord.weightKg} kg", color = OnSurfaceLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text(weightRecord.date, color = TextGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove check-in", tint = SoftRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ======================== TAB 3: DIET PANELS & GEMINI ========================

@Composable
fun DietPlanTab(
    viewModel: HealthViewModel,
    profile: UserProfile
) {
    val dietPlanText by viewModel.dietPlanText.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingDietPlan.collectAsStateWithLifecycle()
    val errorStr by viewModel.generationError.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI planner Header
        Column {
            Text(
                text = "DIETARY STRATEGY DESIGNER",
                color = MintTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Target Nutrition & Planner",
                color = OnSurfaceLight,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Active metrics summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SPECIFICATIONS TO INTEGRATE:",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).background(BackgroundDark, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Column {
                            Text("DIET", fontSize = 9.sp, color = TextGray)
                            Text(profile.dietPreference, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MintTertiary)
                        }
                    }
                    Box(modifier = Modifier.weight(1f).background(BackgroundDark, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Column {
                            Text("GOAL", fontSize = 9.sp, color = TextGray)
                            Text(profile.goal, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                        }
                    }
                    Box(modifier = Modifier.weight(1f).background(BackgroundDark, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Column {
                            Text("ENERGY", fontSize = 9.sp, color = TextGray)
                            Text("${profile.calculateTdee()} kcal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        }
                    }
                }
            }
        }

        // Buttons for triggering diet plan creation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Algorithmic instant plan button
            Button(
                onClick = { viewModel.generateDietPlan(useAI = false) },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, TextGray),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("algo_plan_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Manual")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Standard Diet", fontSize = 14.sp)
            }

            // Gemini AI generation button
            Button(
                onClick = { viewModel.generateDietPlan(useAI = true) },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("gemini_ai_button")
            ) {
                Icon(Icons.Default.Star, contentDescription = "Gemini AI")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gemini AI ✨", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Display Loading or text result
        if (isGenerating) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MintTertiary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Intelligent Wellness Compilation...", color = OnSurfaceLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Structuring nutrients based on gym intensity...", color = TextGray, fontSize = 11.sp)
                }
            }
        }

        // Non-null warning display (API keys missing etc)
        errorStr?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "warning", tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(error, color = Color.White, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }

        // Textual display of nutritional plan
        dietPlanText?.let { plan ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DIET PROTOCOL SHEET", color = MintTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Badge(containerColor = ForestGreenPrimary) {
                            Text("Active Meal Guide", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Render formatting lines with bold **text** support
                    val paragraphs = plan.split("\n")
                    paragraphs.forEach { para ->
                        if (para.isNotBlank()) {
                            Text(
                                text = parseBoldMarkdown(para),
                                color = OnSurfaceLight,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        if (dietPlanText == null && !isGenerating) {
            // Initial Empty State
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Initial Planner", tint = TextGray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Ready for Nutritional Calculations", color = OnSurfaceLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Select 'Standard Diet' or 'Gemini AI ✨' to compile a professional daily menu.", color = TextGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ======================== DIALOG LAYOUTS ========================

@Composable
fun ProfileEditDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Int, Boolean, String, String, String) -> Unit
) {
    var weightInput by remember { mutableStateOf(currentProfile.weightKg.toString()) }
    var heightInput by remember { mutableStateOf(currentProfile.heightCm.toString()) }
    var ageInput by remember { mutableStateOf(currentProfile.age.toString()) }
    var isMale by remember { mutableStateOf(currentProfile.isMale) }
    var selectedActivity by remember { mutableStateOf(currentProfile.activityLevel) }
    var selectedGoal by remember { mutableStateOf(currentProfile.goal) }
    var selectedPref by remember { mutableStateOf(currentProfile.dietPreference) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("RECONSTRUCT METRICS", color = MintTertiary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Updating stats changes calculated BMI, TDEE, and AI suggestions instantly.", color = TextGray, fontSize = 12.sp)

                // Row for inputs
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it; validationError = null },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintTertiary,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = OnSurfaceLight,
                            unfocusedTextColor = OnSurfaceLight
                        ),
                        modifier = Modifier.weight(1f).testTag("dialog_weight_input")
                    )
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it; validationError = null },
                        label = { Text("Height (cm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintTertiary,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = OnSurfaceLight,
                            unfocusedTextColor = OnSurfaceLight
                        ),
                        modifier = Modifier.weight(1f).testTag("dialog_height_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it; validationError = null },
                        label = { Text("Age (yrs)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintTertiary,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = OnSurfaceLight,
                            unfocusedTextColor = OnSurfaceLight
                        ),
                        modifier = Modifier.weight(1f).testTag("dialog_age_input")
                    )

                    // Gender toggle
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gender", color = TextGray, fontSize = 11.sp)
                        Row(modifier = Modifier.fillMaxWidth().background(BackgroundDark, RoundedCornerShape(8.dp))) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isMale = true }
                                    .background(if (isMale) ForestGreenPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Male", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isMale = false }
                                    .background(if (!isMale) ForestGreenPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Female", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Activity levels
                Text("Activity Gym Level", color = TextGray, fontSize = 11.sp)
                FlowSelectionBar(
                    options = listOf("Sedentary", "Light", "Moderate", "Active", "Extreme"),
                    selected = selectedActivity,
                    onSelect = { selectedActivity = it }
                )

                // Goals
                Text("Primary Gym/Fitness Goal", color = TextGray, fontSize = 11.sp)
                FlowSelectionBar(
                    options = listOf("Lose Weight", "Maintain", "Gain Weight"),
                    selected = selectedGoal,
                    onSelect = { selectedGoal = it }
                )

                // Dietary type preference Veg/Non-Veg
                Text("Dietary Preference", color = TextGray, fontSize = 11.sp)
                FlowSelectionBar(
                    options = listOf("Veg", "Non-Veg", "Vegan"),
                    selected = selectedPref,
                    onSelect = { selectedPref = it }
                )

                validationError?.let { err ->
                    Text(err, color = SoftRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextGray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val w = weightInput.trim().toDoubleOrNull()
                            val h = heightInput.trim().toDoubleOrNull()
                            val a = ageInput.trim().toIntOrNull()
                            when {
                                w == null || w !in 15.0..400.0 -> validationError = "Enter a valid weight (15–400 kg)."
                                h == null || h !in 50.0..275.0 -> validationError = "Enter a valid height (50–275 cm)."
                                a == null || a !in 10..120 -> validationError = "Enter a valid age (10–120 yrs)."
                                else -> onSave(w, h, a, isMale, selectedActivity, selectedGoal, selectedPref)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintTertiary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("dialog_save_profile")
                    ) {
                        Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowSelectionBar(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { opt ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected == opt) ForestGreenPrimary else BackgroundDark)
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = opt,
                    color = if (selected == opt) Color.White else TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MealAddDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calsStr by remember { mutableStateOf("") }
    var protStr by remember { mutableStateOf("") }
    var carbStr by remember { mutableStateOf("") }
    var fatStr by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("LOG DIETARY MEAL", color = MintTertiary, fontSize = 20.sp, fontWeight = FontWeight.Black)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; validationError = null },
                    label = { Text("Meal Name (e.g., Eggs Sandwich)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MintTertiary,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = OnSurfaceLight,
                        unfocusedTextColor = OnSurfaceLight
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_meal_name_input")
                )

                OutlinedTextField(
                    value = calsStr,
                    onValueChange = { calsStr = it; validationError = null },
                    label = { Text("Calories (kcal)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MintTertiary,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = OnSurfaceLight,
                        unfocusedTextColor = OnSurfaceLight
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_meal_calories_input")
                )

                Text("Optional Nutrition Specs (g)", color = TextGray, fontSize = 12.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protStr,
                        onValueChange = { protStr = it },
                        label = { Text("Protein") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintTertiary,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = OnSurfaceLight,
                            unfocusedTextColor = OnSurfaceLight
                        ),
                        modifier = Modifier.weight(1f).testTag("dialog_meal_protein_input")
                    )
                    OutlinedTextField(
                        value = carbStr,
                        onValueChange = { carbStr = it },
                        label = { Text("Carbs") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintTertiary,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = OnSurfaceLight,
                            unfocusedTextColor = OnSurfaceLight
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fatStr,
                        onValueChange = { fatStr = it },
                        label = { Text("Fats") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintTertiary,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = OnSurfaceLight,
                            unfocusedTextColor = OnSurfaceLight
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                validationError?.let { err ->
                    Text(err, color = SoftRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextGray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val cals = calsStr.trim().toIntOrNull()
                            when {
                                name.isBlank() -> validationError = "Please enter a meal name."
                                cals == null || cals < 0 -> validationError = "Please enter valid calories (0 or more)."
                                else -> onSave(
                                    name.trim(),
                                    cals,
                                    (protStr.trim().toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                                    (carbStr.trim().toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                                    (fatStr.trim().toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintTertiary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("dialog_save_meal")
                    ) {
                        Text("Add Food", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WeightLogDialog(
    initialWeightKg: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var weightText by remember { mutableStateOf(initialWeightKg.toString()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("LOG WEIGHT CHECK-IN", color = MintTertiary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it; validationError = null },
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MintTertiary,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = OnSurfaceLight,
                        unfocusedTextColor = OnSurfaceLight
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_checkin_weight_input")
                )
                validationError?.let { err ->
                    Text(err, color = SoftRed, fontSize = 12.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextGray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val w = weightText.trim().toDoubleOrNull()
                            if (w == null || w !in 15.0..400.0) {
                                validationError = "Enter a valid weight (15–400 kg)."
                            } else {
                                onSave(w)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintTertiary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("dialog_save_weight")
                    ) {
                        Text("Save Check-In", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun parseBoldMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val regex = Regex("\\*\\*(.+?)\\*\\*")
        regex.findAll(text).forEach { match ->
            if (match.range.first > currentIndex) {
                append(text.substring(currentIndex, match.range.first))
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MintTertiary)) {
                append(match.groupValues[1])
            }
            currentIndex = match.range.last + 1
        }
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}
