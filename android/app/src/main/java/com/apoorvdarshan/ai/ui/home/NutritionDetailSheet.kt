package com.apoorvdarshan.ai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apoorvdarshan.ai.models.FoodEntry
import com.apoorvdarshan.ai.models.HomeTopNutrient
import com.apoorvdarshan.ai.models.OptionalNutrientGoals
import com.apoorvdarshan.ai.models.UserProfile
import com.apoorvdarshan.ai.ui.theme.LocalGlassTheme
import com.apoorvdarshan.ai.ui.theme.AppColors
import com.apoorvdarshan.ai.ui.theme.glowShadow

// Data class optimized for single-pass summation
private data class NutrientsSum(
    var calories: Int = 0, var protein: Int = 0, var carbs: Int = 0, var fat: Int = 0,
    var sugar: Double = 0.0, var addedSugar: Double = 0.0, var fiber: Double = 0.0,
    var saturatedFat: Double = 0.0, var monounsaturatedFat: Double = 0.0, var polyunsaturatedFat: Double = 0.0,
    var cholesterol: Double = 0.0, var sodium: Double = 0.0, var potassium: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDetailSheet(
    entries: List<FoodEntry>,
    profile: UserProfile?,
    homeTopNutrients: List<HomeTopNutrient>,
    optionalGoals: OptionalNutrientGoals,
    onHomeTopNutrientsChange: (List<HomeTopNutrient>) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showHomeCardsPicker by remember { mutableStateOf(false) }
    
    val glass = LocalGlassTheme.current

    // OPTIMIZATION: Single-pass loop to calculate all sums instead of 13 separate iterations
    val sums = remember(entries) {
        val s = NutrientsSum()
        for (e in entries) {
            s.calories += e.calories
            s.protein += e.protein.toInt()
            s.carbs += e.carbs.toInt()
            s.fat += e.fat.toInt()
            s.sugar += e.sugar ?: 0.0
            s.addedSugar += e.addedSugar ?: 0.0
            s.fiber += e.fiber ?: 0.0
            s.saturatedFat += e.saturatedFat ?: 0.0
            s.monounsaturatedFat += e.monounsaturatedFat ?: 0.0
            s.polyunsaturatedFat += e.polyunsaturatedFat ?: 0.0
            s.cholesterol += e.cholesterol ?: 0.0
            s.sodium += e.sodium ?: 0.0
            s.potassium += e.potassium ?: 0.0
        }
        s
    }

    fun fmt(v: Double): String = if (v == 0.0) "—" else String.format("%.1f", v)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = glass.bgTop // Glass Theme UI
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Nutrition Details", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Done", color = AppColors.Calorie) }
                }
            }

            item { SectionHeader("Home Cards") }
            item {
                Card {
                    HomeCardsRow(
                        selected = homeTopNutrients,
                        onClick = { showHomeCardsPicker = true }
                    )
                }
            }

            item { SectionHeader("Macros") }
            item {
                Card {
                    DetailRow(Icons.Filled.LocalFireDepartment, "Calories", "${sums.calories}", "kcal", goal = "${profile?.effectiveCalories ?: 2000}")
                    Hairline()
                    DetailRow(null, "Protein", "${sums.protein}", "g", goal = "${profile?.effectiveProtein ?: 150}", labelGlyph = "P")
                    Hairline()
                    DetailRow(null, "Carbs", "${sums.carbs}", "g", goal = "${profile?.effectiveCarbs ?: 220}", labelGlyph = "C")
                    Hairline()
                    DetailRow(null, "Fat", "${sums.fat}", "g", goal = "${profile?.effectiveFat ?: 70}", labelGlyph = "F")
                }
            }

            item { SectionHeader("Detailed Nutrition") }
            item {
                Card {
                    DetailRow(null, "Sugar", fmt(sums.sugar), "g", goal = "${optionalGoals.sugar}", labelGlyph = "S")
                    Hairline()
                    DetailRow(null, "Added Sugar", fmt(sums.addedSugar), "g", goal = "${optionalGoals.addedSugar}", labelGlyph = "+")
                    Hairline()
                    DetailRow(Icons.Filled.Spa, "Fiber", fmt(sums.fiber), "g", goal = "${optionalGoals.fiber}")
                    Hairline()
                    DetailRow(Icons.Filled.WaterDrop, "Saturated Fat", fmt(sums.saturatedFat), "g", goal = "${optionalGoals.saturatedFat}")
                    Hairline()
                    DetailRow(Icons.Filled.WaterDrop, "Mono Unsat. Fat", fmt(sums.monounsaturatedFat), "g")
                    Hairline()
                    DetailRow(Icons.Filled.WaterDrop, "Poly Unsat. Fat", fmt(sums.polyunsaturatedFat), "g")
                    Hairline()
                    DetailRow(Icons.Filled.Favorite, "Cholesterol", fmt(sums.cholesterol), "mg", goal = "${optionalGoals.cholesterol}")
                    Hairline()
                    DetailRow(Icons.Filled.Bolt, "Sodium", fmt(sums.sodium), "mg", goal = "${optionalGoals.sodium}")
                    Hairline()
                    DetailRow(Icons.Filled.Bolt, "Potassium", fmt(sums.potassium), "mg", goal = "${optionalGoals.potassium}")
                }
            }
        }
    }

    if (showHomeCardsPicker) {
        HomeTopNutrientPickerDialog(
            selected = homeTopNutrients,
            onSave = onHomeTopNutrientsChange,
            onDismiss = { showHomeCardsPicker = false }
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val glass = LocalGlassTheme.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(glass.surface)
            .border(1.dp, Brush.linearGradient(listOf(glass.borderStrong, glass.border, glass.shimmer)), RoundedCornerShape(18.dp))
            .glowShadow(AppColors.Calorie.copy(alpha = 0.04f), radius = 24.dp, offsetY = 8.dp)
    ) { content() }
}

@Composable
private fun SectionHeader(title: String) {
    val glass = LocalGlassTheme.current
    Text(
        title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = glass.textPrimary.copy(alpha = 0.55f),
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 4.dp)
    )
}

@Composable
private fun HomeCardsRow(
    selected: List<HomeTopNutrient>,
    onClick: () -> Unit
) {
    val glass = LocalGlassTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Filled.Spa, null, tint = AppColors.Calorie, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text("Home Nutrient Cards", fontSize = 17.sp, color = glass.textPrimary)
            Text(
                selected.joinToString(" / ") { it.displayName },
                fontSize = 13.sp,
                color = glass.textPrimary.copy(alpha = 0.55f)
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = glass.textPrimary.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun HomeTopNutrientPickerDialog(
    selected: List<HomeTopNutrient>,
    onSave: (List<HomeTopNutrient>) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(selected) { mutableStateOf(HomeTopNutrient.normalized(selected)) }
    val glass = LocalGlassTheme.current
    val dialogShape = RoundedCornerShape(24.dp)

    fun toggle(nutrient: HomeTopNutrient) {
        draft = if (nutrient in draft) {
            if (draft.size <= 1) draft else draft - nutrient
        } else {
            if (draft.size >= 3) draft else draft + nutrient
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Home Nutrient Cards") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Choose up to 3 cards for the top of Home.",
                    fontSize = 13.sp,
                    color = glass.textPrimary.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                HomeTopNutrient.entries.forEach { nutrient -> // Optimized: used .entries
                    val checked = nutrient in draft
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (checked) glass.surfaceHover else Color.Transparent)
                            .clickable { toggle(nutrient) }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked, 
                            onCheckedChange = { toggle(nutrient) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AppColors.Calorie,
                                checkmarkColor = Color.White,
                                uncheckedColor = glass.border
                            )
                        )
                        Column(Modifier.weight(1f)) {
                            Text(nutrient.displayName, fontWeight = FontWeight.Medium, color = glass.textPrimary)
                            Text(
                                nutrient.unit,
                                fontSize = 12.sp,
                                color = glass.textPrimary.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(HomeTopNutrient.normalized(draft))
                onDismiss()
            }) {
                Text("Done", color = AppColors.Calorie)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = glass.textPrimary.copy(alpha = 0.6f))
            }
        },
        containerColor = glass.bgBottom,
        titleContentColor = glass.textPrimary,
        textContentColor = glass.textPrimary.copy(alpha = 0.8f),
        shape = dialogShape,
        modifier = Modifier.border(1.dp, glass.border, dialogShape)
    )
}

@Composable
private fun DetailRow(
    icon: ImageVector?,
    label: String,
    value: String,
    unit: String,
    goal: String? = null,
    labelGlyph: String? = null
) {
    val glass = LocalGlassTheme.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, tint = AppColors.Calorie, modifier = Modifier.size(20.dp))
        } else if (labelGlyph != null) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.Calorie),
                contentAlignment = Alignment.Center
            ) {
                Text(labelGlyph, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            Spacer(Modifier.width(20.dp))
        }
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f), color = glass.textPrimary)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Calorie)
            Text(unit, fontSize = 13.sp, color = glass.textPrimary.copy(alpha = 0.6f))
        }
        goal?.let {
            Text(
                "/ $it",
                fontSize = 12.sp,
                color = glass.textPrimary.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun Hairline() {
    val glass = LocalGlassTheme.current
    Box(
        Modifier
            .padding(start = 14.dp)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(glass.border)
    )
}