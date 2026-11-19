package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintTertiary
import com.example.ui.theme.OnSurfaceLight
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextGray
import java.util.Locale
import kotlin.math.roundToInt

data class BmiHealthStatus(
    val category: String,
    val color: Color,
    val guidance: String,
    val icon: ImageVector
)

fun calculateBmiValue(weightKg: Double, heightCm: Double): Double {
    if (weightKg <= 0.0 || heightCm <= 0.0) return 0.0
    val heightM = heightCm / 100.0
    return weightKg / (heightM * heightM)
}

fun getBmiHealthStatus(bmi: Double): BmiHealthStatus {
    return when {
        bmi <= 0.0 -> BmiHealthStatus(
            category = "Enter Metrics",
            color = TextGray,
            guidance = "Enter positive weight and height values to evaluate your BMI.",
            icon = Icons.Default.Info
        )
        bmi < 18.5 -> BmiHealthStatus(
            category = "Underweight",
            color = Color(0xFF60A5FA),
            guidance = "Below healthy range — focus on nutrient-dense meals and strength training.",
            icon = Icons.Default.Info
        )
        bmi < 25.0 -> BmiHealthStatus(
            category = "Healthy Range",
            color = Color(0xFF34D399),
            guidance = "Optimal BMI range — maintain balanced macros and consistent exercise.",
            icon = Icons.Default.CheckCircle
        )
        bmi < 30.0 -> BmiHealthStatus(
            category = "Overweight",
            color = Color(0xFFFBBF24),
            guidance = "Above healthy range — consider a moderate calorie deficit and daily movement.",
            icon = Icons.Default.Warning
        )
        else -> BmiHealthStatus(
            category = "Obese",
            color = Color(0xFFF87171),
            guidance = "Elevated health risk — prioritize structured nutrition and guided activity.",
            icon = Icons.Default.Warning
        )
    }
}

@Composable
fun BmiCalculatorCard(
    weightKg: Double,
    heightCm: Double,
    onApplyMetrics: (weightKg: Double, heightCm: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var useImperial by remember { mutableStateOf(false) }

    fun formatClean(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else String.format(Locale.US, "%.1f", rounded)
    }

    var weightText by remember { mutableStateOf(formatClean(weightKg)) }
    var heightText by remember { mutableStateOf(formatClean(heightCm)) }
    var justSaved by remember { mutableStateOf(false) }

    // Keep text fields synced when external profile updates (e.g., via full ProfileEditDialog)
    LaunchedEffect(weightKg, heightCm) {
        if (useImperial) {
            weightText = formatClean(weightKg * 2.20462)
            heightText = formatClean(heightCm / 2.54)
        } else {
            weightText = formatClean(weightKg)
            heightText = formatClean(heightCm)
        }
    }

    val parsedWeight = weightText.trim().toDoubleOrNull()
    val parsedHeight = heightText.trim().toDoubleOrNull()

    val effectiveWeightKg = when {
        parsedWeight == null || parsedWeight <= 0.0 -> null
        useImperial -> parsedWeight / 2.20462
        else -> parsedWeight
    }
    val effectiveHeightCm = when {
        parsedHeight == null || parsedHeight <= 0.0 -> null
        useImperial -> parsedHeight * 2.54
        else -> parsedHeight
    }

    val isInputValid = effectiveWeightKg != null && effectiveHeightCm != null &&
        effectiveWeightKg in 15.0..400.0 && effectiveHeightCm in 50.0..275.0

    val computedBmi = if (isInputValid) {
        calculateBmiValue(effectiveWeightKg!!, effectiveHeightCm!!)
    } else {
        0.0
    }

    val status = getBmiHealthStatus(computedBmi)
    val animatedStatusColor by animateColorAsState(targetValue = status.color, label = "bmiStatusColor")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header + Metric/Imperial Unit Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BMI CALCULATOR",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Body Mass Index",
                        color = OnSurfaceLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Unit Switcher (Metric vs Imperial)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundDark)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    UnitChip(
                        label = "kg / cm",
                        selected = !useImperial,
                        onClick = {
                            if (useImperial) {
                                val wKg = effectiveWeightKg ?: weightKg
                                val hCm = effectiveHeightCm ?: heightCm
                                useImperial = false
                                weightText = formatClean(wKg)
                                heightText = formatClean(hCm)
                                justSaved = false
                            }
                        },
                        modifier = Modifier.testTag("bmi_unit_metric")
                    )
                    UnitChip(
                        label = "lbs / in",
                        selected = useImperial,
                        onClick = {
                            if (!useImperial) {
                                val wKg = effectiveWeightKg ?: weightKg
                                val hCm = effectiveHeightCm ?: heightCm
                                useImperial = true
                                weightText = formatClean(wKg * 2.20462)
                                heightText = formatClean(hCm / 2.54)
                                justSaved = false
                            }
                        },
                        modifier = Modifier.testTag("bmi_unit_imperial")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Direct Weight and Height Input Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { input ->
                        weightText = input
                        justSaved = false
                    },
                    label = { Text(if (useImperial) "Weight (lbs)" else "Weight (kg)") },
                    singleLine = true,
                    isError = weightText.isNotEmpty() && effectiveWeightKg == null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MintTertiary,
                        unfocusedBorderColor = TextGray.copy(alpha = 0.6f),
                        focusedLabelColor = MintTertiary,
                        unfocusedLabelColor = TextGray,
                        focusedTextColor = OnSurfaceLight,
                        unfocusedTextColor = OnSurfaceLight,
                        errorBorderColor = Color(0xFFF87171)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bmi_weight_input")
                )

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { input ->
                        heightText = input
                        justSaved = false
                    },
                    label = { Text(if (useImperial) "Height (in)" else "Height (cm)") },
                    singleLine = true,
                    isError = heightText.isNotEmpty() && effectiveHeightCm == null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (isInputValid) {
                                onApplyMetrics(effectiveWeightKg!!, effectiveHeightCm!!)
                                justSaved = true
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MintTertiary,
                        unfocusedBorderColor = TextGray.copy(alpha = 0.6f),
                        focusedLabelColor = MintTertiary,
                        unfocusedLabelColor = TextGray,
                        focusedTextColor = OnSurfaceLight,
                        unfocusedTextColor = OnSurfaceLight,
                        errorBorderColor = Color(0xFFF87171)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bmi_height_input")
                )
            }

            // Validation Feedback when invalid
            AnimatedVisibility(visible = !isInputValid && (weightText.isNotEmpty() || heightText.isNotEmpty())) {
                Text(
                    text = if (useImperial) {
                        "Please enter a valid weight (33–880 lbs) and height (20–108 in)."
                    } else {
                        "Please enter a valid weight (15–400 kg) and height (50–275 cm)."
                    },
                    color = Color(0xFFF87171),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Result + Health Status Indicator Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = BackgroundDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "YOUR BMI",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isInputValid) String.format(Locale.US, "%.1f", computedBmi) else "--",
                            color = OnSurfaceLight,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("bmi_result_value")
                        )
                    }

                    // Health Status Indicator Pill
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = animatedStatusColor.copy(alpha = 0.16f),
                        modifier = Modifier.testTag("bmi_status_indicator")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = status.icon,
                                contentDescription = status.category,
                                tint = animatedStatusColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = status.category,
                                color = animatedStatusColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Responsive Visual BMI Gauge
            BmiGauge(bmi = computedBmi)

            Spacer(modifier = Modifier.height(10.dp))

            // Health guidance & Healthy Weight Range
            Text(
                text = status.guidance,
                color = TextGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            if (effectiveHeightCm != null && effectiveHeightCm in 50.0..275.0) {
                val heightM = effectiveHeightCm / 100.0
                val minHealthyKg = 18.5 * heightM * heightM
                val maxHealthyKg = 24.9 * heightM * heightM
                val rangeText = if (useImperial) {
                    val minLbs = minHealthyKg * 2.20462
                    val maxLbs = maxHealthyKg * 2.20462
                    String.format(Locale.US, "Healthy weight range for your height: %.1f – %.1f lbs", minLbs, maxLbs)
                } else {
                    String.format(Locale.US, "Healthy weight range for your height: %.1f – %.1f kg", minHealthyKg, maxHealthyKg)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = rangeText,
                    color = MintTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explicit Calculate & Save Action
            val hasUnsavedChanges = isInputValid &&
                (kotlin.math.abs(effectiveWeightKg!! - weightKg) > 0.05 ||
                    kotlin.math.abs(effectiveHeightCm!! - heightCm) > 0.05)

            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (isInputValid) {
                        onApplyMetrics(effectiveWeightKg!!, effectiveHeightCm!!)
                        justSaved = true
                    }
                },
                enabled = isInputValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasUnsavedChanges) MintTertiary else ForestGreenPrimary,
                    contentColor = if (hasUnsavedChanges) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("bmi_calculate_button")
            ) {
                Text(
                    text = when {
                        justSaved && !hasUnsavedChanges -> "BMI Calculated & Saved"
                        hasUnsavedChanges -> "Calculate & Update Profile"
                        else -> "Save BMI Check-In"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun UnitChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ForestGreenPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BmiGauge(bmi: Double) {
    val clippedBmi = bmi.coerceIn(15.0, 35.0)
    val floatPos = if (bmi > 0.0) {
        ((clippedBmi - 15.0) / 20.0).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            val pointerSize = 24.dp
            val availableTravel = (maxWidth - pointerSize).coerceAtLeast(0.dp)
            val pointerOffset = availableTravel * floatPos

            // Background Spectrum bar using gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF60A5FA), // Blue - Underweight
                                Color(0xFF34D399), // Green - Normal
                                Color(0xFFFBBF24), // Yellow - Overweight
                                Color(0xFFF87171)  // Red - Obese
                            )
                        )
                    )
            )

            // Responsive Pointer Indicator
            if (bmi > 0.0) {
                Box(
                    modifier = Modifier
                        .size(pointerSize)
                        .align(Alignment.CenterStart)
                        .offset(x = pointerOffset)
                        .background(OnSurfaceLight, CircleShape)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundDark, CircleShape)
                    )
                }
            }
        }

        // Scale Reference Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("<18.5 Under", color = Color(0xFF60A5FA), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text("18.5–24.9 Normal", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text("25–29.9 Over", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text("30+ Obese", color = Color(0xFFF87171), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}
