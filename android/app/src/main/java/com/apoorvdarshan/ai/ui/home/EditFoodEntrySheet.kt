package com.apoorvdarshan.ai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apoorvdarshan.ai.R
import com.apoorvdarshan.ai.models.FoodEntry
import com.apoorvdarshan.ai.models.MealType
import com.apoorvdarshan.ai.models.ServingUnitOption
import com.apoorvdarshan.ai.ui.theme.LocalGlassTheme
import com.apoorvdarshan.ai.ui.theme.AppColors
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Edit page for an existing FoodEntry. Visually identical to [FoodResultSheet]
 * (the first-time review page), so the edit experience matches the logging
 * experience. Differences from FoodResultSheet:
 * - Top-right action says "Save" instead of "Log".
 * - Initial values come from the existing entry; save mutates it via onSave.
 * Deletion is handled by swipe-to-delete on the Home food log list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFoodEntrySheet(
    entry: FoodEntry,
    onSave: (FoodEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassTheme.current
    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    val baseServing = entry.servingSizeGrams ?: 100.0
    val servingUnitOptions = remember(entry.servingUnitOptions, baseServing) {
        ServingUnitOption.normalizedOptions(entry.servingUnitOptions, baseServing)
    }
    var name by remember { mutableStateOf(entry.name) }
    var selectedServingUnitId by remember {
        mutableStateOf(ServingUnitOption.initialUnitId(entry.selectedServingUnit, servingUnitOptions))
    }
    var servingGrams by remember { mutableStateOf(baseServing) }
    var servingQuantityText by remember {
        mutableStateOf(
            ServingUnitOption.initialQuantityText(
                totalGrams = baseServing,
                selectedUnitId = selectedServingUnitId,
                selectedQuantity = entry.selectedServingQuantity,
                options = servingUnitOptions
            )
        )
    }
    val selectedServingOption = ServingUnitOption.optionMatching(selectedServingUnitId, servingUnitOptions)
    val selectedServingQuantity = servingQuantityText.toDoubleOrNull()?.takeIf { it > 0 }
    val scale = if (baseServing > 0) servingGrams / baseServing else 1.0
    var mealType by remember { mutableStateOf(entry.mealType) }
    var moreNutritionExpanded by remember { mutableStateOf(false) }
    var mealMenuExpanded by remember { mutableStateOf(false) }
    var servingMenuExpanded by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val initialLoggedAt = remember(entry.id, entry.timestamp) { entry.timestamp.atZone(zone) }
    var loggedDate by remember(entry.id, entry.timestamp) { mutableStateOf(initialLoggedAt.toLocalDate()) }
    var loggedTime by remember(entry.id, entry.timestamp) { mutableStateOf(initialLoggedAt.toLocalTime().withSecond(0).withNano(0)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.US) }

    fun scaledInt(v: Int) = (v * scale).roundToInt()
    fun scaledDouble(v: Double) = ((v * scale) * 10).roundToInt() / 10.0
    fun scaledD(v: Double?) = v?.let { ((it * scale) * 10).roundToInt() / 10.0 }

    fun buildUpdated(): FoodEntry = entry.copy(
        name = name.trim().ifEmpty { entry.name },
        calories = scaledInt(entry.calories),
        protein = scaledDouble(entry.protein),
        carbs = scaledDouble(entry.carbs),
        fat = scaledDouble(entry.fat),
        timestamp = loggedDate.atTime(loggedTime).atZone(zone).toInstant(),
        mealType = mealType,
        sugar = scaledD(entry.sugar),
        addedSugar = scaledD(entry.addedSugar),
        fiber = scaledD(entry.fiber),
        saturatedFat = scaledD(entry.saturatedFat),
        monounsaturatedFat = scaledD(entry.monounsaturatedFat),
        polyunsaturatedFat = scaledD(entry.polyunsaturatedFat),
        cholesterol = scaledD(entry.cholesterol),
        sodium = scaledD(entry.sodium),
        potassium = scaledD(entry.potassium),
        servingSizeGrams = servingGrams,
        servingUnitOptions = servingUnitOptions,
        selectedServingUnit = if (servingUnitOptions.isEmpty()) null else selectedServingOption.unit,
        selectedServingQuantity = if (servingUnitOptions.isEmpty()) null else selectedServingQuantity
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = glass.bgTop // Glass Theme UI
    ) {
        SheetReviewToolbar(
            title = stringResource(R.string.sheet_edit_food),
            primaryLabel = stringResource(R.string.action_save),
            onCancel = onDismiss,
            onPrimary = { onSave(buildUpdated()) }
        )

        val gUnit = stringResource(R.string.unit_g)
        val mgUnit = stringResource(R.string.unit_mg)
        val micros = listOf(
            Triple(stringResource(R.string.sheet_micro_sugar), scaledD(entry.sugar), gUnit),
            Triple(stringResource(R.string.sheet_micro_added_sugar), scaledD(entry.addedSugar), gUnit),
            Triple(stringResource(R.string.sheet_micro_fiber), scaledD(entry.fiber), gUnit),
            Triple(stringResource(R.string.sheet_micro_saturated_fat), scaledD(entry.saturatedFat), gUnit),
            Triple(stringResource(R.string.sheet_micro_mono_fat), scaledD(entry.monounsaturatedFat), gUnit),
            Triple(stringResource(R.string.sheet_micro_poly_fat), scaledD(entry.polyunsaturatedFat), gUnit),
            Triple(stringResource(R.string.sheet_micro_cholesterol), scaledD(entry.cholesterol), mgUnit),
            Triple(stringResource(R.string.sheet_micro_sodium), scaledD(entry.sodium), mgUnit),
            Triple(stringResource(R.string.sheet_micro_potassium), scaledD(entry.potassium), mgUnit)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                val ctx = LocalContext.current
                val container = (ctx.applicationContext as com.apoorvdarshan.ai.FudAIApp).container
                val bitmap = remember(entry.imageFilename) {
                    entry.imageFilename?.let { container.imageStore.load(it) }
                }
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                    } else {
                        Text(entry.emoji ?: "🍽", fontSize = 80.sp)
                    }
                }
            }

            item { SheetSectionHeader(stringResource(R.string.sheet_food_details)) }
            item {
                SheetPillRow {
                    Text(stringResource(R.string.sheet_name), fontSize = 17.sp, modifier = Modifier.padding(end = 8.dp), color = glass.textPrimary)
                    Spacer(Modifier.weight(1f))
                    androidx.compose.foundation.text.BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = glass.textPrimary,
                            fontSize = 17.sp,
                            textAlign = TextAlign.End
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Calorie),
                        modifier = Modifier.weight(2f)
                    )
                }
            }

            item { SheetSectionHeader(stringResource(R.string.sheet_serving)) }
            item {
                ServingQuantityCard(
                    quantityText = servingQuantityText,
                    onQuantityChange = { newValue ->
                        servingQuantityText = newValue
                        newValue.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                            servingGrams = it * selectedServingOption.gramsPerUnit
                        }
                    },
                    selectedUnitId = selectedServingUnitId,
                    onSelectedUnitChange = { optionId ->
                        selectedServingUnitId = optionId
                        val option = ServingUnitOption.optionMatching(optionId, servingUnitOptions)
                        val quantity = if (option.gramsPerUnit > 0) servingGrams / option.gramsPerUnit else servingGrams
                        servingQuantityText = ServingUnitOption.formatQuantity(quantity)
                    },
                    servingSizeGrams = servingGrams,
                    unitOptions = servingUnitOptions,
                    menuExpanded = servingMenuExpanded,
                    onMenuExpandedChange = { servingMenuExpanded = it },
                    gramUnit = stringResource(R.string.unit_g)
                )
            }

            item { SheetSectionHeader(stringResource(R.string.sheet_nutrition)) }
            item {
                SheetPillCard {
                    SheetNutritionRow(stringResource(R.string.nutrition_label_calories), "${scaledInt(entry.calories)}", stringResource(R.string.unit_kcal))
                    SheetHairline()
                    SheetNutritionRow(stringResource(R.string.nutrition_label_protein), "${scaledDouble(entry.protein)}", stringResource(R.string.unit_g))
                    SheetHairline()
                    SheetNutritionRow(stringResource(R.string.nutrition_label_carbs), "${scaledDouble(entry.carbs)}", stringResource(R.string.unit_g))
                    SheetHairline()
                    SheetNutritionRow(stringResource(R.string.nutrition_label_fat), "${scaledDouble(entry.fat)}", stringResource(R.string.unit_g))
                }
            }

            if (micros.any { it.second != null }) {
                item {
                    SheetPillRow(onClick = { moreNutritionExpanded = !moreNutritionExpanded }) {
                        Text(stringResource(R.string.sheet_more_nutrition), fontSize = 17.sp, modifier = Modifier.weight(1f), color = glass.textPrimary)
                        Icon(
                            if (moreNutritionExpanded) Icons.Filled.KeyboardArrowDown
                            else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = glass.textPrimary.copy(alpha = 0.5f)
                        )
                    }
                }
                if (moreNutritionExpanded) {
                    item {
                        SheetPillCard {
                            val present = micros.filter { it.second != null }
                            present.forEachIndexed { idx, (label, value, unit) ->
                                if (idx > 0) SheetHairline()
                                SheetNutritionRow(label, String.format("%.1f", value), unit, dim = true)
                            }
                        }
                    }
                }
            }

            item { SheetSectionHeader(stringResource(R.string.sheet_meal)) }
            item {
                SheetPillRow(onClick = { mealMenuExpanded = true }) {
                    Text(stringResource(R.string.sheet_meal_type), fontSize = 17.sp, modifier = Modifier.weight(1f), color = glass.textPrimary)
                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                sheetMealIcon(mealType),
                                contentDescription = null,
                                tint = AppColors.Calorie,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(mealType.displayNameRes),
                                fontSize = 17.sp,
                                color = AppColors.Calorie,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.UnfoldMore,
                                contentDescription = null,
                                tint = AppColors.Calorie
                            )
                        }
                        DropdownMenu(
                            expanded = mealMenuExpanded,
                            onDismissRequest = { mealMenuExpanded = false },
                            modifier = Modifier.background(glass.bgTop)
                        ) {
                            for (m in MealType.entries) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(sheetMealIcon(m), contentDescription = null, tint = AppColors.Calorie)
                                    },
                                    text = { Text(stringResource(m.displayNameRes), color = glass.textPrimary) },
                                    onClick = {
                                        mealType = m
                                        mealMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item { SheetSectionHeader("Date & Time") }
            item {
                SheetPillCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date", fontSize = 17.sp, modifier = Modifier.weight(1f), color = glass.textPrimary)
                        Text(
                            loggedDate.format(dateFormatter),
                            fontSize = 17.sp,
                            color = AppColors.Calorie,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    SheetHairline()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Time", fontSize = 17.sp, modifier = Modifier.weight(1f), color = glass.textPrimary)
                        Text(
                            loggedTime.format(timeFormatter),
                            fontSize = 17.sp,
                            color = AppColors.Calorie,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = loggedDate.atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )
        val dialogShape = RoundedCornerShape(24.dp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = dialogShape,
            colors = DatePickerDefaults.colors(containerColor = glass.bgBottom),
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        loggedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("Done", color = AppColors.Calorie)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { 
                    Text("Cancel", color = glass.textPrimary.copy(alpha = 0.6f)) 
                }
            }
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = AppColors.Calorie,
                    todayDateBorderColor = AppColors.Calorie,
                    currentYearContentColor = AppColors.Calorie,
                    selectedYearContainerColor = AppColors.Calorie,
                    dayContentColor = glass.textPrimary,
                    headlineContentColor = glass.textPrimary,
                    titleContentColor = glass.textPrimary.copy(alpha = 0.6f),
                    weekdayContentColor = glass.textPrimary.copy(alpha = 0.6f),
                    yearContentColor = glass.textPrimary
                )
            )
        }
    }

    if (showTimePicker) {
        EditFoodTimeDialog(
            initialTime = loggedTime,
            onConfirm = {
                loggedTime = it
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun EditFoodTimeDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassTheme.current
    val dialogShape = RoundedCornerShape(24.dp)
    var hourText by remember(initialTime) { mutableStateOf(initialTime.hour.toString().padStart(2, '0')) }
    var minuteText by remember(initialTime) { mutableStateOf(initialTime.minute.toString().padStart(2, '0')) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Time") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = hourText,
                    onValueChange = { hourText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Hour") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = glass.textPrimary,
                        unfocusedTextColor = glass.textPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = AppColors.Calorie,
                        unfocusedBorderColor = glass.border,
                        focusedLabelColor = AppColors.Calorie,
                        unfocusedLabelColor = glass.textPrimary.copy(alpha = 0.6f)
                    )
                )
                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { minuteText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Minute") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = glass.textPrimary,
                        unfocusedTextColor = glass.textPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = AppColors.Calorie,
                        unfocusedBorderColor = glass.border,
                        focusedLabelColor = AppColors.Calorie,
                        unfocusedLabelColor = glass.textPrimary.copy(alpha = 0.6f)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: initialTime.hour
                val minute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: initialTime.minute
                onConfirm(LocalTime.of(hour, minute))
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