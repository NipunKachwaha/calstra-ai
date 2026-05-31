package com.apoorvdarshan.ai.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.apoorvdarshan.ai.AppContainer
import com.apoorvdarshan.ai.R
import com.apoorvdarshan.ai.models.*
import com.apoorvdarshan.ai.ui.components.*
import com.apoorvdarshan.ai.ui.theme.AppColors
import com.apoorvdarshan.ai.ui.theme.AppThemeColor
import com.apoorvdarshan.ai.ui.theme.glowShadow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class SettingsSheet {
    AI_PROVIDER, AI_MODEL, API_KEY, CUSTOM_BASE_URL, SPEECH_PROVIDER, SPEECH_LANGUAGE, SPEECH_KEY,
    FALLBACK_PROVIDER, FALLBACK_MODEL, FALLBACK_KEY, FALLBACK_BASE_URL,
    GENDER, BIRTHDAY, HEIGHT, WEIGHT, BODY_FAT, GOAL_BODY_FAT, ACTIVITY, GOAL, GOAL_WEIGHT, GOAL_SPEED,
    CALORIES, PROTEIN, CARBS, FAT, OPTIONAL_NUTRIENTS,
    APPEARANCE, THEME_COLOR, WEEK_START
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, nav: NavHostController) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(container))
    val ui by vm.ui.collectAsState()
    val profile = ui.profile

    var sheet by remember { mutableStateOf<SettingsSheet?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearFoodDialog by remember { mutableStateOf(false) }
    var showRecalcDialog by remember { mutableStateOf(false) }
    var invalidGoalWeightMessage by remember { mutableStateOf<String?>(null) }
    var showMaxPinnedAlert by remember { mutableStateOf(false) }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }
    val activityContext = LocalContext.current

    val notifDeniedMsg = stringResource(R.string.settings_notifications_denied)
    val healthDeniedMsg = stringResource(R.string.settings_health_denied)
    val healthUnavailableMsg = stringResource(R.string.settings_health_unavailable)

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.setNotificationsEnabled(true)
        else permissionDeniedMessage = notifDeniedMsg
    }

    val healthConnectLauncher = rememberLauncherForActivityResult(
        contract = container.health.permissionRequestContract()
    ) { granted ->
        if (granted.containsAll(container.health.permissions)) vm.setHealthConnectEnabled(true)
        else permissionDeniedMessage = healthDeniedMsg
    }

    fun onNotificationsToggle(enabled: Boolean) {
        if (!enabled) {
            vm.setNotificationsEnabled(false)
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            vm.setNotificationsEnabled(true)
        } else {
            val granted = ContextCompat.checkSelfPermission(
                activityContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) vm.setNotificationsEnabled(true)
            else notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun onHealthConnectToggle(enabled: Boolean) {
        if (!enabled) {
            vm.setHealthConnectEnabled(false)
            return
        }
        if (!container.health.isAvailable()) {
            permissionDeniedMessage = healthUnavailableMsg
            return
        }
        healthConnectLauncher.launch(container.health.permissions)
    }

    fun openBatteryOptimizationSettings() {
        val intents = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                add(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:${activityContext.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                add(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            add(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:${activityContext.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        for (intent in intents) {
            if (runCatching { activityContext.startActivity(intent) }.isSuccess) return
        }
    }

    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (ui.appearanceMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val glassTheme = remember(isDarkTheme) { if (isDarkTheme) DarkGlass else LightGlass }
    
    val scrollState = rememberScrollState()

    CompositionLocalProvider(LocalGlassTheme provides glassTheme) {
        val glass = LocalGlassTheme.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(glass.bgTop, glass.bgBottom)))
        ) {
            AmbientOrbs(scrollState = scrollState)

            Scaffold(containerColor = Color.Transparent) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section 1 — Personal Info
                    SectionCard(title = stringResource(R.string.settings_section_personal)) {
                        profile?.let { p ->
                            SettingRow(stringResource(R.string.settings_gender), stringResource(p.gender.displayNameRes), icon = Icons.Outlined.Person, inlineMenu = true) { sheet = SettingsSheet.GENDER }
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(stringResource(R.string.settings_birthday), birthdayDisplay(p), icon = Icons.Outlined.Cake) { sheet = SettingsSheet.BIRTHDAY }
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(
                                stringResource(R.string.settings_height),
                                if (ui.useMetric) stringResource(R.string.height_cm_format, p.heightCm.toInt())
                                else feetInchesLabel(p.heightCm.toInt()),
                                icon = Icons.Outlined.Height
                            ) { sheet = SettingsSheet.HEIGHT }
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(
                                stringResource(R.string.settings_weight),
                                if (ui.useMetric) String.format(Locale.US, "%.1f kg", p.weightKg)
                                else String.format(Locale.US, "%.1f lbs", p.weightKg * 2.20462),
                                icon = Icons.Outlined.MonitorWeight
                            ) { sheet = SettingsSheet.WEIGHT }
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(
                                stringResource(R.string.settings_body_fat),
                                p.bodyFatPercentage?.let { "${(it * 100).toInt()}%" } ?: stringResource(R.string.settings_not_set),
                                icon = Icons.Outlined.Percent
                            ) { sheet = SettingsSheet.BODY_FAT }

                            if (p.bodyFatPercentage != null) {
                                HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                                SettingRow(
                                    stringResource(R.string.settings_goal_body_fat),
                                    p.goalBodyFatPercentage?.let { "${(it * 100).toInt()}%" } ?: stringResource(R.string.settings_not_set),
                                    icon = Icons.Outlined.TrackChanges
                                ) { sheet = SettingsSheet.GOAL_BODY_FAT }
                                HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                                ToggleRow(
                                    stringResource(R.string.settings_use_body_fat_bmr),
                                    p.useBodyFatInBMR ?: true,
                                    icon = Icons.Outlined.Calculate,
                                    onChange = { newValue ->
                                        vm.updateProfileAndRecompute { it.copy(useBodyFatInBMR = newValue) }
                                    }
                                )
                                Text(
                                    stringResource(R.string.settings_use_body_fat_bmr_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textPrimary.copy(alpha = 0.55f),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Section 2 — Goals & Nutrition
                    SectionCard(title = stringResource(R.string.settings_section_goals)) {
                        profile?.let { p ->
                            SettingRow(stringResource(R.string.settings_weight_goal), stringResource(p.goal.displayNameRes), icon = Icons.Outlined.Equalizer, inlineMenu = true) { sheet = SettingsSheet.GOAL }
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(stringResource(R.string.settings_activity_level), stringResource(p.activityLevel.displayNameRes), icon = Icons.AutoMirrored.Outlined.DirectionsRun, inlineMenu = true) { sheet = SettingsSheet.ACTIVITY }
                            if (p.goal != WeightGoal.MAINTAIN) {
                                HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                                SettingRow(
                                    stringResource(R.string.settings_weekly_change),
                                    p.weeklyChangeKg?.let {
                                        if (ui.useMetric) String.format(Locale.US, "%.2f kg/wk", it)
                                        else String.format(Locale.US, "%.2f lbs/wk", it * 2.20462)
                                    } ?: stringResource(R.string.settings_weekly_default),
                                    icon = Icons.Outlined.Speed
                                ) { sheet = SettingsSheet.GOAL_SPEED }
                                HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                                SettingRow(
                                    stringResource(R.string.settings_goal_weight),
                                    p.goalWeightKg?.let {
                                        if (ui.useMetric) String.format(Locale.US, "%.1f kg", it)
                                        else String.format(Locale.US, "%.1f lbs", it * 2.20462)
                                    } ?: stringResource(R.string.settings_not_set),
                                    icon = Icons.AutoMirrored.Outlined.TrendingUp
                                ) { sheet = SettingsSheet.GOAL_WEIGHT }
                            }
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(stringResource(R.string.settings_calories), stringResource(R.string.kcal_value_format, p.effectiveCalories), icon = Icons.Outlined.LocalFireDepartment) { sheet = SettingsSheet.CALORIES }
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            
                            val openMacro = { macro: AutoBalanceMacro, target: SettingsSheet ->
                                val isPinned = p.isPinned(macro)
                                if (!isPinned && p.pinnedCount >= 2) showMaxPinnedAlert = true
                                else sheet = target
                            }
                            MacroSettingRow(
                                label = stringResource(R.string.macro_protein),
                                value = p.effectiveProtein,
                                pinned = p.isPinned(AutoBalanceMacro.PROTEIN),
                                onClick = { openMacro(AutoBalanceMacro.PROTEIN, SettingsSheet.PROTEIN) }
                            )
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            MacroSettingRow(
                                label = stringResource(R.string.macro_carbs),
                                value = p.effectiveCarbs,
                                pinned = p.isPinned(AutoBalanceMacro.CARBS),
                                onClick = { openMacro(AutoBalanceMacro.CARBS, SettingsSheet.CARBS) }
                            )
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            MacroSettingRow(
                                label = stringResource(R.string.macro_fat),
                                value = p.effectiveFat,
                                pinned = p.isPinned(AutoBalanceMacro.FAT),
                                onClick = { openMacro(AutoBalanceMacro.FAT, SettingsSheet.FAT) }
                            )
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(
                                "Other Nutrient Goals",
                                optionalNutrientSummary(ui.optionalNutrientGoals),
                                icon = Icons.Outlined.DataUsage
                            ) { sheet = SettingsSheet.OPTIONAL_NUTRIENTS }
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { showRecalcDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    tint = AppColors.Calorie,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                Text(stringResource(R.string.settings_recalculate_goals), color = AppColors.Calorie, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    // Section 3 — App Settings
                    SectionCard(title = stringResource(R.string.settings_section_app)) {
                        SettingRow(
                            stringResource(R.string.settings_appearance),
                            when (ui.appearanceMode) {
                                "light" -> stringResource(R.string.settings_appearance_light)
                                "dark" -> stringResource(R.string.settings_appearance_dark)
                                else -> stringResource(R.string.settings_appearance_system)
                            },
                            icon = Icons.Outlined.Brightness6
                        ) { sheet = SettingsSheet.APPEARANCE }
                        HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                        SettingRow(
                            stringResource(R.string.settings_theme_color),
                            stringResource(ui.appThemeColor.displayNameRes),
                            icon = Icons.Outlined.Palette
                        ) { sheet = SettingsSheet.THEME_COLOR }
                        HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                        ToggleRow(stringResource(R.string.settings_metric_units), ui.useMetric, icon = Icons.Outlined.Straighten, onChange = vm::setUseMetric)
                        HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                        SettingRow(
                            stringResource(R.string.settings_week_starts),
                            if (ui.weekStartsOnMonday) stringResource(R.string.settings_week_monday) else stringResource(R.string.settings_week_sunday),
                            icon = Icons.Outlined.CalendarToday
                        ) { sheet = SettingsSheet.WEEK_START }
                        HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                        ToggleRow(stringResource(R.string.settings_notifications), ui.notificationsEnabled, icon = Icons.Outlined.Notifications, onChange = ::onNotificationsToggle)
                        if (ui.notificationsEnabled) {
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(
                                stringResource(R.string.settings_battery_opt),
                                stringResource(R.string.settings_battery_opt_value),
                                icon = Icons.Outlined.BatteryAlert
                            ) { openBatteryOptimizationSettings() }
                        }
                    }

                    // Section 4 — AI Provider
                    SectionCard(title = stringResource(R.string.settings_section_ai)) {
                        SettingRow(stringResource(R.string.settings_ai_provider), stringResource(ui.selectedAI.displayNameRes), icon = Icons.Outlined.SmartToy) { sheet = SettingsSheet.AI_PROVIDER }
                        HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                        SettingRow(stringResource(R.string.settings_ai_model), ui.selectedModel.ifEmpty { stringResource(R.string.settings_ai_model_unset) }, icon = Icons.Outlined.Tune) { sheet = SettingsSheet.AI_MODEL }
                        if (ui.selectedAI.requiresApiKey) {
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(stringResource(R.string.settings_api_key), ui.apiKeyMasked.ifEmpty { stringResource(R.string.settings_not_set) }, icon = Icons.Outlined.Key) { sheet = SettingsSheet.API_KEY }
                        }
                        if (ui.selectedAI.requiresCustomEndpoint || ui.selectedAI == AIProvider.OLLAMA) {
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(
                                if (ui.selectedAI.requiresCustomEndpoint) stringResource(R.string.settings_base_url) else stringResource(R.string.settings_server_url),
                                stringResource(R.string.settings_tap_to_edit),
                                icon = Icons.Outlined.Link
                            ) { sheet = SettingsSheet.CUSTOM_BASE_URL }
                        }
                    }

                    // Section 4b — Custom AI Instructions
                    SectionCard(title = stringResource(R.string.settings_section_custom_instructions)) {
                        CustomInstructionsBlock(
                            initial = ui.userContext,
                            placeholder = stringResource(R.string.settings_custom_instructions_placeholder),
                            onSave = { vm.setUserContext(it) }
                        )
                        Text(
                            stringResource(R.string.settings_custom_instructions_footer),
                            fontSize = 12.sp,
                            color = glass.textPrimary.copy(alpha = 0.55f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }

                    // Section 4c — Fallback Provider
                    SectionCard(title = stringResource(R.string.settings_section_fallback)) {
                        ToggleRow(
                            stringResource(R.string.settings_enable_fallback),
                            ui.fallbackEnabled,
                            icon = Icons.Outlined.Refresh,
                            onChange = { vm.setFallbackEnabled(it) }
                        )
                        if (ui.fallbackEnabled) {
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(
                                stringResource(R.string.settings_ai_provider),
                                stringResource(ui.fallbackProvider.displayNameRes),
                                icon = Icons.Outlined.SmartToy
                            ) { sheet = SettingsSheet.FALLBACK_PROVIDER }
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(
                                stringResource(R.string.settings_ai_model),
                                ui.fallbackModel.ifEmpty { stringResource(R.string.settings_ai_model_unset) },
                                icon = Icons.Outlined.Tune
                            ) { sheet = SettingsSheet.FALLBACK_MODEL }
                            if (ui.fallbackProvider.requiresApiKey) {
                                HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                                SettingRow(
                                    stringResource(R.string.settings_api_key),
                                    ui.fallbackApiKeyMasked.ifEmpty { stringResource(R.string.settings_not_set) },
                                    icon = Icons.Outlined.Key
                                ) { sheet = SettingsSheet.FALLBACK_KEY }
                            }
                            if (ui.fallbackProvider.requiresCustomEndpoint || ui.fallbackProvider == AIProvider.OLLAMA) {
                                HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                                SettingRow(
                                    if (ui.fallbackProvider.requiresCustomEndpoint) stringResource(R.string.settings_base_url) else stringResource(R.string.settings_server_url),
                                    stringResource(R.string.settings_tap_to_edit),
                                    icon = Icons.Outlined.Link
                                ) { sheet = SettingsSheet.FALLBACK_BASE_URL }
                            }
                            Text(
                                stringResource(R.string.settings_fallback_footer),
                                fontSize = 12.sp,
                                color = glass.textPrimary.copy(alpha = 0.55f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // Section 5 — Speech-to-Text
                    SectionCard(title = stringResource(R.string.settings_section_speech)) {
                        SettingRow(stringResource(R.string.settings_ai_provider), stringResource(ui.selectedSpeech.displayNameRes), icon = Icons.Outlined.Mic) { sheet = SettingsSheet.SPEECH_PROVIDER }
                        HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                        SettingRow(
                            stringResource(R.string.settings_speech_language),
                            stringResource(ui.selectedSpeechLanguage.displayNameRes),
                            icon = Icons.Outlined.Language
                        ) { sheet = SettingsSheet.SPEECH_LANGUAGE }
                        HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                        Text(
                            stringResource(ui.selectedSpeech.descriptionRes),
                            fontSize = 12.sp,
                            color = glass.textPrimary.copy(alpha = 0.55f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        if (ui.selectedSpeech.requiresApiKey) {
                            HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                            SettingRow(
                                stringResource(R.string.settings_api_key),
                                ui.speechApiKeyMasked.ifEmpty { stringResource(R.string.settings_not_set) },
                                icon = Icons.Outlined.Key
                            ) { sheet = SettingsSheet.SPEECH_KEY }
                        }
                    }

                    // Section 6 — Health & Data
                    SectionCard(title = stringResource(R.string.settings_section_health)) {
                        ToggleRow(stringResource(R.string.settings_health_connect), ui.healthConnectEnabled, icon = Icons.Outlined.Favorite, onChange = ::onHealthConnectToggle)
                        HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { showClearFoodDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(stringResource(R.string.settings_clear_food_log), color = Color(0xFFFF9500), style = MaterialTheme.typography.bodyLarge)
                        }
                        HorizontalDivider(color = glass.divider, thickness = 0.5.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { showDeleteDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(stringResource(R.string.settings_delete_all_data), color = Color(0xFFFF3B30), style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                } // End of main scrollable Column
            } // End of Scaffold

            // Sheets
            sheet?.let { s ->
                SettingsSheets(
                    sheet = s,
                    ui = ui,
                    vm = vm,
                    onDismiss = { sheet = null },
                    onInvalidGoalWeight = { invalidGoalWeightMessage = it }
                )
            }

            val dialogShape = RoundedCornerShape(24.dp)

            if (showClearFoodDialog) {
                AlertDialog(
                    onDismissRequest = { showClearFoodDialog = false },
                    title = { Text(stringResource(R.string.settings_clear_food_title)) },
                    text = { Text(stringResource(R.string.settings_clear_food_message)) },
                    confirmButton = {
                        TextButton(onClick = { vm.clearFoodLog(); showClearFoodDialog = false }) {
                            Text(stringResource(R.string.action_clear), color = Color(0xFFFF3B30)) 
                        }
                    },
                    dismissButton = { 
                        TextButton(onClick = { showClearFoodDialog = false }) { 
                            Text(stringResource(R.string.action_cancel), color = glass.textPrimary.copy(alpha = 0.6f)) 
                        } 
                    },
                    containerColor = glass.bgBottom,
                    titleContentColor = glass.textPrimary,
                    textContentColor = glass.textPrimary.copy(alpha = 0.8f),
                    shape = dialogShape,
                    modifier = Modifier.border(1.dp, glass.border, dialogShape)
                )
            }

            if (showDeleteDialog) {
                val context = LocalContext.current
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(stringResource(R.string.settings_delete_all_title)) },
                    text = { Text(stringResource(R.string.settings_delete_all_message)) },
                    confirmButton = {
                        TextButton(onClick = { 
                            vm.deleteAllData {
                                showDeleteDialog = false
                                (context as? android.app.Activity)?.recreate()
                            }
                        }) {
                            Text(stringResource(R.string.action_delete), color = Color(0xFFFF3B30)) 
                        }
                    },
                    dismissButton = { 
                        TextButton(onClick = { showDeleteDialog = false }) { 
                            Text(stringResource(R.string.action_cancel), color = glass.textPrimary.copy(alpha = 0.6f)) 
                        } 
                    },
                    containerColor = glass.bgBottom,
                    titleContentColor = glass.textPrimary,
                    textContentColor = glass.textPrimary.copy(alpha = 0.8f),
                    shape = dialogShape,
                    modifier = Modifier.border(1.dp, glass.border, dialogShape)
                )
            }

            if (showMaxPinnedAlert) {
                AlertDialog(
                    onDismissRequest = { showMaxPinnedAlert = false },
                    title = { Text(stringResource(R.string.settings_max_pinned_title)) },
                    text = { Text(stringResource(R.string.settings_max_pinned_message)) },
                    confirmButton = { 
                        TextButton(onClick = { showMaxPinnedAlert = false }) { 
                            Text(stringResource(R.string.action_ok), color = AppColors.Calorie)
                        } 
                    },
                    containerColor = glass.bgBottom,
                    titleContentColor = glass.textPrimary,
                    textContentColor = glass.textPrimary.copy(alpha = 0.8f),
                    shape = dialogShape,
                    modifier = Modifier.border(1.dp, glass.border, dialogShape)
                )
            }

            if (showRecalcDialog) {
                AlertDialog(
                    onDismissRequest = { showRecalcDialog = false },
                    title = { Text(stringResource(R.string.settings_recalc_title)) },
                    text = { Text(stringResource(R.string.settings_recalc_message)) },
                    confirmButton = {
                        TextButton(onClick = { vm.recalculateGoals(); showRecalcDialog = false }) {
                            Text(stringResource(R.string.settings_recalc_confirm), color = AppColors.Calorie) 
                        }
                    },
                    dismissButton = { 
                        TextButton(onClick = { showRecalcDialog = false }) { 
                            Text(stringResource(R.string.action_cancel), color = glass.textPrimary.copy(alpha = 0.6f)) 
                        } 
                    },
                    containerColor = glass.bgBottom,
                    titleContentColor = glass.textPrimary,
                    textContentColor = glass.textPrimary.copy(alpha = 0.8f),
                    shape = dialogShape,
                    modifier = Modifier.border(1.dp, glass.border, dialogShape)
                )
            }

            invalidGoalWeightMessage?.let { msg ->
                AlertDialog(
                    onDismissRequest = { invalidGoalWeightMessage = null },
                    title = { Text(stringResource(R.string.settings_invalid_goal_title)) },
                    text = { Text(msg) },
                    confirmButton = {
                        TextButton(onClick = { invalidGoalWeightMessage = null }) {
                            Text(stringResource(R.string.action_ok), color = AppColors.Calorie) 
                        }
                    },
                    containerColor = glass.bgBottom,
                    titleContentColor = glass.textPrimary,
                    textContentColor = glass.textPrimary.copy(alpha = 0.8f),
                    shape = dialogShape,
                    modifier = Modifier.border(1.dp, glass.border, dialogShape)
                )
            }

            permissionDeniedMessage?.let { msg ->
                AlertDialog(
                    onDismissRequest = { permissionDeniedMessage = null },
                    title = { Text(stringResource(R.string.settings_permission_title)) },
                    text = { Text(msg) },
                    confirmButton = {
                        TextButton(onClick = { permissionDeniedMessage = null }) {
                            Text(stringResource(R.string.action_ok), color = AppColors.Calorie) 
                        }
                    },
                    containerColor = glass.bgBottom,
                    titleContentColor = glass.textPrimary,
                    textContentColor = glass.textPrimary.copy(alpha = 0.8f),
                    shape = dialogShape,
                    modifier = Modifier.border(1.dp, glass.border, dialogShape)
                )
            }

        } // End of Box
    } // End of CompositionLocalProvider
} // End of SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheets(
    sheet: SettingsSheet,
    ui: SettingsUiState,
    vm: SettingsViewModel,
    onDismiss: () -> Unit,
    onInvalidGoalWeight: (String) -> Unit
) {
    val glass = LocalGlassTheme.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val invalidLoseMsg = stringResource(R.string.settings_invalid_goal_lose)
    val invalidGainMsg = stringResource(R.string.settings_invalid_goal_gain)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        sheetState = state, 
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = glass.bgTop
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            when (sheet) {
                SettingsSheet.AI_PROVIDER -> ListSheet(
                    title = stringResource(R.string.sheet_ai_provider),
                    items = AIProvider.values().toList(),
                    label = { stringResource(it.displayNameRes) },
                    selected = { it == ui.selectedAI },
                    onSelect = { vm.selectProvider(it); onDismiss() }
                )
                SettingsSheet.AI_MODEL -> ListSheet(
                    title = stringResource(R.string.sheet_model),
                    items = ui.selectedAI.models,
                    label = { it },
                    selected = { it == ui.selectedModel },
                    onSelect = { vm.selectModel(it); onDismiss() },
                    footer = if (ui.selectedAI.supportsCustomModelName) stringResource(R.string.sheet_model_footer) else null,
                    customField = if (ui.selectedAI.supportsCustomModelName) {
                        { m -> vm.selectModel(m); onDismiss() }
                    } else null
                )
                SettingsSheet.API_KEY -> ApiKeySheet(
                    title = stringResource(R.string.sheet_api_key_format, stringResource(ui.selectedAI.displayNameRes)),
                    placeholder = stringResource(ui.selectedAI.apiKeyPlaceholderRes),
                    onSave = { vm.setApiKey(it); onDismiss() }
                )
                SettingsSheet.CUSTOM_BASE_URL -> {
                    val existing = remember { runBlocking { vm.container.prefs.customBaseUrl(ui.selectedAI).first().orEmpty() } }
                    TextFieldSheet(
                        title = stringResource(R.string.settings_custom_url_title),
                        initial = existing,
                        placeholder = stringResource(R.string.settings_custom_url_placeholder),
                        onSave = { vm.setCustomBaseUrl(ui.selectedAI, it); onDismiss() }
                    )
                }
                SettingsSheet.SPEECH_PROVIDER -> ListSheet(
                    title = stringResource(R.string.sheet_speech_engine),
                    items = SpeechProvider.values().toList(),
                    label = { stringResource(it.displayNameRes) },
                    selected = { it == ui.selectedSpeech },
                    onSelect = { vm.selectSpeech(it); onDismiss() }
                )
                SettingsSheet.SPEECH_LANGUAGE -> ListSheet(
                    title = stringResource(R.string.sheet_speech_language),
                    items = SpeechLanguage.optionsFor(ui.selectedSpeech),
                    label = { stringResource(it.displayNameRes) },
                    selected = { it == ui.selectedSpeechLanguage },
                    onSelect = { vm.selectSpeechLanguage(it); onDismiss() },
                    subtitle = {
                        when (it) {
                            SpeechLanguage.PROVIDER_AUTO -> stringResource(R.string.speech_language_provider_auto_subtitle)
                            SpeechLanguage.DEVICE -> stringResource(R.string.speech_language_device_subtitle)
                            else -> null
                        }
                    }
                )
                SettingsSheet.SPEECH_KEY -> ApiKeySheet(
                    title = stringResource(R.string.sheet_speech_api_key_format, stringResource(ui.selectedSpeech.displayNameRes)),
                    placeholder = stringResource(ui.selectedSpeech.apiKeyPlaceholderRes),
                    onSave = {
                        vm.setSpeechApiKey(it)
                        onDismiss()
                    }
                )
                SettingsSheet.FALLBACK_PROVIDER -> ListSheet(
                    title = stringResource(R.string.sheet_ai_provider),
                    items = AIProvider.values().toList(),
                    label = { stringResource(it.displayNameRes) },
                    selected = { it == ui.fallbackProvider },
                    onSelect = { vm.selectFallbackProvider(it); onDismiss() }
                )
                SettingsSheet.FALLBACK_MODEL -> {
                    val opts = if (ui.fallbackProvider == ui.selectedAI)
                        ui.fallbackProvider.models.filter { it != ui.selectedModel }
                    else ui.fallbackProvider.models
                    ListSheet(
                        title = stringResource(R.string.sheet_model),
                        items = opts,
                        label = { it },
                        selected = { it == ui.fallbackModel },
                        onSelect = { vm.selectFallbackModel(it); onDismiss() },
                        footer = if (ui.fallbackProvider.supportsCustomModelName) stringResource(R.string.sheet_model_footer) else null,
                        customField = if (ui.fallbackProvider.supportsCustomModelName) {
                            { m -> vm.selectFallbackModel(m); onDismiss() }
                        } else null
                    )
                }
                SettingsSheet.FALLBACK_KEY -> ApiKeySheet(
                    title = stringResource(R.string.sheet_api_key_format, stringResource(ui.fallbackProvider.displayNameRes)),
                    placeholder = stringResource(ui.fallbackProvider.apiKeyPlaceholderRes),
                    onSave = { vm.setFallbackApiKey(it); onDismiss() }
                )
                SettingsSheet.FALLBACK_BASE_URL -> {
                    val existing = remember { runBlocking { vm.container.prefs.customBaseUrl(ui.fallbackProvider).first().orEmpty() } }
                    TextFieldSheet(
                        title = stringResource(R.string.settings_custom_url_title),
                        initial = existing,
                        placeholder = stringResource(R.string.settings_custom_url_placeholder),
                        onSave = { vm.setCustomBaseUrl(ui.fallbackProvider, it); onDismiss() }
                    )
                }
                SettingsSheet.GENDER -> ListSheet(
                    title = stringResource(R.string.sheet_gender),
                    items = Gender.values().toList(),
                    label = { stringResource(it.displayNameRes) },
                    selected = { it == ui.profile?.gender },
                    onSelect = { g -> vm.updateProfileAndRecompute { it.copy(gender = g) }; onDismiss() },
                    icon = { genderIcon(it) }
                )
                SettingsSheet.HEIGHT -> {
                    val cm = ui.profile?.heightCm?.toInt() ?: 175
                    HeightSheet(
                        current = cm,
                        useMetric = ui.useMetric,
                        onSave = { newCm -> vm.updateProfileAndRecompute { it.copy(heightCm = newCm.toDouble()) }; onDismiss() }
                    )
                }
                SettingsSheet.WEIGHT -> {
                    val kg = ui.profile?.weightKg ?: 70.0
                    WeightSheet(
                        titleText = stringResource(R.string.sheet_weight),
                        current = kg,
                        useMetric = ui.useMetric,
                        onSave = { newKg -> vm.saveCurrentWeight(newKg); onDismiss() }
                    )
                }
                SettingsSheet.BODY_FAT -> BodyFatSheet(
                    current = ui.profile?.bodyFatPercentage,
                    onSave = { bf ->
                        vm.updateProfileAndRecompute {
                            it.copy(
                                bodyFatPercentage = bf,
                                goalBodyFatPercentage = if (bf == null) null else it.goalBodyFatPercentage
                            )
                        }
                        onDismiss()
                    }
                )
                SettingsSheet.GOAL_BODY_FAT -> GoalBodyFatSheet(
                    currentGoal = ui.profile?.goalBodyFatPercentage,
                    currentBodyFat = ui.profile?.bodyFatPercentage,
                    onSave = { goal -> vm.updateProfile { it.copy(goalBodyFatPercentage = goal) }; onDismiss() }
                )
                SettingsSheet.ACTIVITY -> ListSheet(
                    title = stringResource(R.string.sheet_activity_level),
                    items = ActivityLevel.values().toList(),
                    label = { stringResource(it.displayNameRes) },
                    subtitle = { stringResource(it.subtitleRes) },
                    selected = { it == ui.profile?.activityLevel },
                    onSelect = { a -> vm.updateProfileAndRecompute { it.copy(activityLevel = a) }; onDismiss() },
                    icon = { activityIcon(it) }
                )
                SettingsSheet.GOAL -> ListSheet(
                    title = stringResource(R.string.sheet_goal),
                    items = WeightGoal.values().toList(),
                    label = { stringResource(it.displayNameRes) },
                    selected = { it == ui.profile?.goal },
                    icon = { goalIcon(it) },
                    onSelect = { g ->
                        vm.updateProfileAndRecompute { p ->
                            when (g) {
                                WeightGoal.MAINTAIN ->
                                    p.copy(goal = g, weeklyChangeKg = null, goalWeightKg = null)
                                else -> {
                                    val gw = p.goalWeightKg
                                    val mismatched = gw != null && (
                                        (g == WeightGoal.LOSE && gw >= p.weightKg) ||
                                        (g == WeightGoal.GAIN && gw <= p.weightKg)
                                    )
                                    p.copy(
                                        goal = g,
                                        weeklyChangeKg = p.weeklyChangeKg ?: 0.5,
                                        goalWeightKg = if (mismatched) null else p.goalWeightKg
                                    )
                                }
                            }
                        }
                        onDismiss()
                    }
                )
                SettingsSheet.GOAL_WEIGHT -> {
                    val kg = ui.profile?.goalWeightKg ?: (ui.profile?.weightKg ?: 70.0)
                    WeightSheet(
                        titleText = stringResource(R.string.sheet_target_weight),
                        current = kg,
                        useMetric = ui.useMetric,
                        onSave = { newKg ->
                            val p = ui.profile
                            val current = p?.weightKg
                            val invalid = p != null && current != null && (
                                (p.goal == WeightGoal.LOSE && newKg >= current) ||
                                (p.goal == WeightGoal.GAIN && newKg <= current)
                            )
                            if (invalid) {
                                onInvalidGoalWeight(
                                    if (p!!.goal == WeightGoal.LOSE)
                                        invalidLoseMsg
                                    else
                                        invalidGainMsg
                                )
                            } else {
                                vm.updateProfile { it.copy(goalWeightKg = newKg) }
                                onDismiss()
                            }
                        }
                    )
                }
                SettingsSheet.GOAL_SPEED -> GoalSpeedSheet(
                    current = ui.profile?.weeklyChangeKg ?: 0.5,
                    goal = ui.profile?.goal ?: WeightGoal.MAINTAIN,
                    useMetric = ui.useMetric,
                    onSave = { kg -> vm.updateProfileAndRecompute { it.copy(weeklyChangeKg = kg) }; onDismiss() }
                )
                SettingsSheet.BIRTHDAY -> BirthdaySheet(
                    current = ui.profile?.birthday ?: Instant.now(),
                    onSave = { newInstant ->
                        vm.updateProfile { it.copy(birthday = newInstant) }
                        onDismiss()
                    }
                )
                SettingsSheet.APPEARANCE -> ListSheet(
                    title = stringResource(R.string.sheet_appearance),
                    items = listOf(
                        "system" to stringResource(R.string.settings_appearance_system),
                        "light" to stringResource(R.string.settings_appearance_light),
                        "dark" to stringResource(R.string.settings_appearance_dark)
                    ),
                    label = { it.second },
                    selected = { it.first == ui.appearanceMode },
                    onSelect = { vm.setAppearanceMode(it.first); onDismiss() },
                    icon = { appearanceIcon(it.first) }
                )
                SettingsSheet.THEME_COLOR -> ThemeColorSheet(
                    selected = ui.appThemeColor,
                    onSelect = { vm.setAppThemeColor(it); onDismiss() }
                )
                SettingsSheet.WEEK_START -> ListSheet(
                    title = stringResource(R.string.sheet_week_starts),
                    items = listOf(
                        false to stringResource(R.string.settings_week_sunday),
                        true to stringResource(R.string.settings_week_monday)
                    ),
                    label = { it.second },
                    selected = { it.first == ui.weekStartsOnMonday },
                    onSelect = { vm.setWeekStartsOnMonday(it.first); onDismiss() }
                )
                SettingsSheet.CALORIES -> NutritionPickerSheet(
                    label = stringResource(R.string.macro_calories), unit = stringResource(R.string.unit_kcal),
                    currentValue = ui.profile?.effectiveCalories ?: 2000,
                    range = 800..6000, step = 50,
                    onSave = { v ->
                        vm.updateProfile { it.copy(customCalories = v) }
                        onDismiss()
                    }
                )
                SettingsSheet.PROTEIN -> NutritionPickerSheet(
                    label = stringResource(R.string.macro_protein), unit = stringResource(R.string.unit_g),
                    currentValue = ui.profile?.effectiveProtein ?: 0,
                    range = 10..500, step = 5,
                    onSave = { v ->
                        vm.updateProfile { it.copy(customProtein = v) }
                        onDismiss()
                    },
                    onResetToAuto = if (ui.profile?.isPinned(AutoBalanceMacro.PROTEIN) == true) {
                        {
                            vm.updateProfile { it.copy(customProtein = null) }
                            onDismiss()
                        }
                    } else null
                )
                SettingsSheet.CARBS -> NutritionPickerSheet(
                    label = stringResource(R.string.macro_carbs), unit = stringResource(R.string.unit_g),
                    currentValue = ui.profile?.effectiveCarbs ?: 0,
                    range = 0..800, step = 5,
                    onSave = { v ->
                        vm.updateProfile { it.copy(customCarbs = v) }
                        onDismiss()
                    },
                    onResetToAuto = if (ui.profile?.isPinned(AutoBalanceMacro.CARBS) == true) {
                        {
                            vm.updateProfile { it.copy(customCarbs = null) }
                            onDismiss()
                        }
                    } else null
                )
                SettingsSheet.FAT -> NutritionPickerSheet(
                    label = stringResource(R.string.macro_fat), unit = stringResource(R.string.unit_g),
                    currentValue = ui.profile?.effectiveFat ?: 0,
                    range = 10..300, step = 5,
                    onSave = { v ->
                        vm.updateProfile { it.copy(customFat = v) }
                        onDismiss()
                    },
                    onResetToAuto = if (ui.profile?.isPinned(AutoBalanceMacro.FAT) == true) {
                        {
                            vm.updateProfile { it.copy(customFat = null) }
                            onDismiss()
                        }
                    } else null
                )
                SettingsSheet.OPTIONAL_NUTRIENTS -> OptionalNutrientGoalsSheet(
                    goals = ui.optionalNutrientGoals,
                    estimating = ui.estimatingOptionalNutrientGoals,
                    error = ui.optionalNutrientGoalError,
                    onChange = vm::setOptionalNutrientGoals,
                    onEstimate = vm::estimateOptionalNutrientGoals,
                    onDismiss = onDismiss
                )
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun OptionalNutrientGoalsSheet(
    goals: OptionalNutrientGoals,
    estimating: Boolean,
    error: String?,
    onChange: (OptionalNutrientGoals) -> Unit,
    onEstimate: () -> Unit,
    onDismiss: () -> Unit
) {
    var editing by remember { mutableStateOf<OptionalNutrient?>(null) }
    val nutrient = editing
    val glass = LocalGlassTheme.current

    if (nutrient != null) {
        TextButton(onClick = { editing = null }) {
            Text("Other Nutrients", color = AppColors.Calorie)
        }
        Spacer(Modifier.height(4.dp))
        NutritionPickerSheet(
            label = nutrient.displayName,
            unit = nutrient.unit,
            currentValue = goals.valueFor(nutrient),
            range = nutrient.pickerRange(),
            step = nutrient.pickerStep(),
            onSave = { value ->
                onChange(goals.withValue(nutrient, value))
                editing = null
            }
        )
        return
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Other Nutrient Goals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text("Done", color = AppColors.Calorie) }
    }
    Text(
        "Separate from calorie, protein, carbs, and fat targets.",
        style = MaterialTheme.typography.bodySmall,
        color = glass.textPrimary.copy(alpha = 0.6f)
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onEstimate,
        enabled = !estimating,
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Calorie),
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (estimating) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("Estimating...")
        } else {
            Text("Estimate with AI")
        }
    }
    if (!error.isNullOrBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(
            error,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFD32F2F)
        )
    }
    Spacer(Modifier.height(12.dp))
    LazyColumn(
        Modifier.fillMaxWidth().heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(OptionalNutrient.values().toList()) { item ->
            OptionalNutrientGoalRow(
                nutrient = item,
                value = goals.valueFor(item),
                onClick = { editing = item }
            )
        }
    }
    TextButton(
        onClick = { onChange(OptionalNutrientGoals.Default) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Reset to Defaults", color = glass.textPrimary.copy(alpha = 0.6f))
    }
}

@Composable
private fun OptionalNutrientGoalRow(
    nutrient: OptionalNutrient,
    value: Int,
    onClick: () -> Unit
) {
    val glass = LocalGlassTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(glass.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.DataUsage,
            contentDescription = null,
            tint = AppColors.Calorie,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(nutrient.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = glass.textPrimary)
        Text(
            "$value${nutrient.unit}",
            style = MaterialTheme.typography.bodyMedium,
            color = glass.textPrimary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = glass.textPrimary.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ThemeColorSheet(
    selected: AppThemeColor,
    onSelect: (AppThemeColor) -> Unit
) {
    val glass = LocalGlassTheme.current
    Text(stringResource(R.string.sheet_theme_color), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
    Spacer(Modifier.height(12.dp))
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AppThemeColor.values().toList()) { themeColor ->
            val isSel = selected == themeColor
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(glass.surface)
                    .clickable { onSelect(themeColor) }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeColorSwatch(themeColor, Modifier.size(30.dp))
                Spacer(Modifier.width(14.dp))
                Text(
                    stringResource(themeColor.displayNameRes),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = glass.textPrimary
                )
                if (isSel) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.sheet_selected_a11y),
                        tint = AppColors.Calorie,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.sheet_theme_color_footer),
        style = MaterialTheme.typography.bodySmall,
        color = glass.textPrimary.copy(alpha = 0.6f)
    )
}

@Composable
private fun ThemeColorSwatch(themeColor: AppThemeColor, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(themeColor.start, themeColor.end)))
    )
}

@Composable
private fun <T> ListSheet(
    title: String,
    items: List<T>,
    label: @Composable (T) -> String,
    selected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    icon: ((T) -> ImageVector?)? = null,
    subtitle: (@Composable (T) -> String?)? = null,
    footer: String? = null,
    customField: ((String) -> Unit)? = null
) {
    val glass = LocalGlassTheme.current
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
    Spacer(Modifier.height(12.dp))
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { item ->
            val isSel = selected(item)
            val rowIcon = icon?.invoke(item)
            val sub = subtitle?.invoke(item)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(glass.surface)
                    .clickable { onSelect(item) }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rowIcon != null) {
                    Icon(rowIcon, contentDescription = null, tint = AppColors.Calorie, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        label(item),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = glass.textPrimary
                    )
                    if (!sub.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textPrimary.copy(alpha = 0.6f)
                        )
                    }
                }
                if (isSel) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.sheet_selected_a11y),
                        tint = AppColors.Calorie,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    if (customField != null) {
        footer?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = glass.textPrimary.copy(alpha = 0.6f))
        }
        var custom by remember { mutableStateOf("") }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = custom,
            onValueChange = { custom = it },
            placeholder = { Text(stringResource(R.string.sheet_any_model_id), color = glass.textPrimary.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = glass.textPrimary,
                unfocusedTextColor = glass.textPrimary,
                focusedContainerColor = glass.surface.copy(alpha = 0.3f),
                unfocusedContainerColor = glass.surface.copy(alpha = 0.3f),
                focusedBorderColor = AppColors.Calorie,
                unfocusedBorderColor = glass.border,
                cursorColor = AppColors.Calorie
            )
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { if (custom.isNotBlank()) customField(custom.trim()) },
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Calorie),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.action_save), color = Color.White) }
    }
}

@Composable
private fun ApiKeySheet(title: String, placeholder: String, onSave: (String) -> Unit) {
    val glass = LocalGlassTheme.current
    var value by remember { mutableStateOf("") }
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        placeholder = { Text(placeholder, color = glass.textPrimary.copy(alpha = 0.4f)) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = glass.textPrimary,
            unfocusedTextColor = glass.textPrimary,
            focusedContainerColor = glass.surface.copy(alpha = 0.3f),
            unfocusedContainerColor = glass.surface.copy(alpha = 0.3f),
            focusedBorderColor = AppColors.Calorie,
            unfocusedBorderColor = glass.border,
            cursorColor = AppColors.Calorie
        )
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { onSave(value) },
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Calorie),
        modifier = Modifier.fillMaxWidth()
    ) { Text(stringResource(R.string.action_save), color = Color.White) }
    Spacer(Modifier.height(4.dp))
    TextButton(onClick = { onSave("") }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_clear_key), color = AppColors.Calorie) }
}

@Composable
private fun TextFieldSheet(title: String, initial: String, placeholder: String, onSave: (String) -> Unit) {
    val glass = LocalGlassTheme.current
    var value by remember { mutableStateOf(initial) }
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        placeholder = { Text(placeholder, color = glass.textPrimary.copy(alpha = 0.4f)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = glass.textPrimary,
            unfocusedTextColor = glass.textPrimary,
            focusedContainerColor = glass.surface.copy(alpha = 0.3f),
            unfocusedContainerColor = glass.surface.copy(alpha = 0.3f),
            focusedBorderColor = AppColors.Calorie,
            unfocusedBorderColor = glass.border,
            cursorColor = AppColors.Calorie
        )
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { onSave(value.trim()) },
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Calorie),
        modifier = Modifier.fillMaxWidth()
    ) { Text(stringResource(R.string.action_save), color = Color.White) }
}

@Composable
private fun HeightSheet(current: Int, useMetric: Boolean, onSave: (Int) -> Unit) {
    val glass = LocalGlassTheme.current
    var cm by remember(current) { mutableStateOf(current) }
    var metric by remember { mutableStateOf(useMetric) }
    Text(stringResource(R.string.sheet_height), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
    Spacer(Modifier.height(12.dp))
    UnitToggle(stringResource(R.string.unit_cm), stringResource(R.string.unit_ft_in), metric, { metric = it }, Modifier.fillMaxWidth())
    Spacer(Modifier.height(20.dp))
    if (metric) NumericWheelPicker(cm, { cm = it }, 100, 250, stringResource(R.string.unit_cm))
    else FeetInchesWheelPicker(cm, { cm = it })
    Spacer(Modifier.height(16.dp))
    GradientSaveButton { onSave(cm) }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun WeightSheet(titleText: String, current: Double, useMetric: Boolean, onSave: (Double) -> Unit) {
    val glass = LocalGlassTheme.current
    var kg by remember(current) { mutableStateOf(current) }
    var metric by remember { mutableStateOf(useMetric) }
    Text(titleText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
    Spacer(Modifier.height(12.dp))
    UnitToggle(stringResource(R.string.unit_kg), stringResource(R.string.unit_lbs), metric, { metric = it }, Modifier.fillMaxWidth())
    Spacer(Modifier.height(20.dp))
    if (metric) {
        SplitDecimalWheelPicker(kg, { kg = it }, 30, 250, stringResource(R.string.unit_kg))
    } else {
        SplitDecimalWheelPicker(kg * 2.20462, { lbs -> kg = lbs / 2.20462 }, 66, 551, stringResource(R.string.unit_lbs))
    }
    Spacer(Modifier.height(16.dp))
    GradientSaveButton { onSave(kg) }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun BodyFatSheet(current: Double?, onSave: (Double?) -> Unit) {
    val glass = LocalGlassTheme.current
    var pct by remember(current) { mutableStateOf((current ?: 0.20) * 100) }
    Text(stringResource(R.string.sheet_body_fat_percent), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
    Spacer(Modifier.height(12.dp))
    DecimalWheelPicker(pct, { pct = it }, 5.0, 60.0, 0.5, stringResource(R.string.unit_percent))
    Spacer(Modifier.height(12.dp))
    GradientSaveButton { onSave(pct / 100.0) }
    Spacer(Modifier.height(4.dp))
    TextButton(onClick = { onSave(null) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_clear), color = AppColors.Calorie) }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun GoalBodyFatSheet(currentGoal: Double?, currentBodyFat: Double?, onSave: (Double?) -> Unit) {
    val glass = LocalGlassTheme.current
    val seed = currentGoal ?: currentBodyFat ?: 0.15
    var pct by remember(currentGoal) { mutableStateOf(seed * 100) }
    Text(stringResource(R.string.sheet_goal_body_fat), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
    if (currentBodyFat != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.sheet_goal_body_fat_currently, (currentBodyFat * 100).toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = glass.textPrimary.copy(alpha = 0.55f)
        )
    }
    Spacer(Modifier.height(12.dp))
    DecimalWheelPicker(pct, { pct = it }, 3.0, 60.0, 0.5, stringResource(R.string.unit_percent))
    Spacer(Modifier.height(12.dp))
    GradientSaveButton { onSave(pct / 100.0) }
    Spacer(Modifier.height(4.dp))
    TextButton(onClick = { onSave(null) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_remove_goal), color = AppColors.Calorie) }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun GoalSpeedSheet(current: Double, goal: WeightGoal, useMetric: Boolean, onSave: (Double) -> Unit) {
    val glass = LocalGlassTheme.current
    Text("Weekly Change", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
    Spacer(Modifier.height(12.dp))
    val unit = if (goal == WeightGoal.LOSE) "loss" else "gain"
    val options = listOf(
        Triple(0.25, "Slow", "0.25 ${if (useMetric) "kg" else "lbs"}/week $unit"),
        Triple(0.5, "Recommended", "0.5 ${if (useMetric) "kg" else "lbs"}/week $unit"),
        Triple(1.0, "Fast", "1.0 ${if (useMetric) "kg" else "lbs"}/week $unit")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((kg, title, subtitle) in options) {
            val isSel = kotlin.math.abs(kg - current) < 0.01
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(glass.surface)
                    .clickable { onSave(kg) }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = glass.textPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary.copy(alpha = 0.6f)
                    )
                }
                if (isSel) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = AppColors.Calorie,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun NutritionPickerSheet(
    label: String,
    unit: String,
    currentValue: Int,
    range: IntRange,
    step: Int,
    onSave: (Int) -> Unit,
    onResetToAuto: (() -> Unit)? = null
) {
    val glass = LocalGlassTheme.current
    val items = remember(range, step) { (range.first..range.last step step).toList() }
    val snapped = (currentValue / step) * step
    val initial = snapped.coerceIn(range.first, range.last).let { v ->
        items.minByOrNull { kotlin.math.abs(it - v) } ?: items.first()
    }
    var selected by remember(initial) { mutableStateOf(initial) }
    Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
    Spacer(Modifier.height(12.dp))
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        com.apoorvdarshan.ai.ui.components.WheelPicker(
            items = items,
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier.width(120.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            unit,
            style = MaterialTheme.typography.titleMedium,
            color = glass.textPrimary.copy(alpha = 0.6f)
        )
    }
    Spacer(Modifier.height(16.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.CalorieGradient)
            .clickable { onSave(selected) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Save",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium
        )
    }
    if (onResetToAuto != null) {
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onResetToAuto,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Reset to Auto-balance",
                color = glass.textPrimary.copy(alpha = 0.6f)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun MacrosSheet(
    profile: com.apoorvdarshan.ai.models.UserProfile?,
    onSaveCalories: (Int?) -> Unit,
    onSaveMacro: (AutoBalanceMacro, Int?) -> Unit,
    onClearPin: (AutoBalanceMacro) -> Unit
) {
    val glass = LocalGlassTheme.current
    profile ?: return
    var caloriesText by remember(profile) { mutableStateOf(profile.effectiveCalories.toString()) }
    var proteinText by remember(profile) { mutableStateOf(profile.effectiveProtein.toString()) }
    var carbsText by remember(profile) { mutableStateOf(profile.effectiveCarbs.toString()) }
    var fatText by remember(profile) { mutableStateOf(profile.effectiveFat.toString()) }
    Text("Macros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
    Text(
        "Pin up to 2 macros to exact grams. Unpinned ones auto-balance from remaining calories.",
        style = MaterialTheme.typography.bodySmall,
        color = glass.textPrimary.copy(alpha = 0.6f)
    )
    Spacer(Modifier.height(12.dp))
    MacroField("Calories", caloriesText, { caloriesText = it }, "kcal") {
        caloriesText.toIntOrNull()?.let { onSaveCalories(it) }
    }
    Spacer(Modifier.height(6.dp))
    MacroField(
        label = "Protein (${if (profile.isPinned(AutoBalanceMacro.PROTEIN)) "pinned" else "auto"})",
        value = proteinText,
        onChange = { proteinText = it },
        unit = "g",
        pinned = profile.isPinned(AutoBalanceMacro.PROTEIN),
        onClearPin = { onClearPin(AutoBalanceMacro.PROTEIN) }
    ) { proteinText.toIntOrNull()?.let { onSaveMacro(AutoBalanceMacro.PROTEIN, it) } }
    Spacer(Modifier.height(6.dp))
    MacroField(
        label = "Carbs (${if (profile.isPinned(AutoBalanceMacro.CARBS)) "pinned" else "auto"})",
        value = carbsText,
        onChange = { carbsText = it },
        unit = "g",
        pinned = profile.isPinned(AutoBalanceMacro.CARBS),
        onClearPin = { onClearPin(AutoBalanceMacro.CARBS) }
    ) { carbsText.toIntOrNull()?.let { onSaveMacro(AutoBalanceMacro.CARBS, it) } }
    Spacer(Modifier.height(6.dp))
    MacroField(
        label = "Fat (${if (profile.isPinned(AutoBalanceMacro.FAT)) "pinned" else "auto"})",
        value = fatText,
        onChange = { fatText = it },
        unit = "g",
        pinned = profile.isPinned(AutoBalanceMacro.FAT),
        onClearPin = { onClearPin(AutoBalanceMacro.FAT) }
    ) { fatText.toIntOrNull()?.let { onSaveMacro(AutoBalanceMacro.FAT, it) } }
}

@Composable
private fun MacroField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    unit: String,
    pinned: Boolean = false,
    onClearPin: (() -> Unit)? = null,
    onPin: () -> Unit
) {
    val glass = LocalGlassTheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            suffix = { Text(unit) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = glass.textPrimary,
                unfocusedTextColor = glass.textPrimary,
                focusedContainerColor = glass.surface.copy(alpha = 0.3f),
                unfocusedContainerColor = glass.surface.copy(alpha = 0.3f),
                focusedBorderColor = AppColors.Calorie,
                unfocusedBorderColor = glass.border,
                cursorColor = AppColors.Calorie,
                focusedLabelColor = AppColors.Calorie,
                unfocusedLabelColor = glass.textPrimary.copy(alpha = 0.6f),
                focusedSuffixColor = glass.textPrimary.copy(alpha = 0.6f),
                unfocusedSuffixColor = glass.textPrimary.copy(alpha = 0.6f)
            )
        )
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = { if (pinned) onClearPin?.invoke() else onPin() }) {
            Text(if (pinned) "Clear" else "Pin", color = AppColors.Calorie)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    val glass = LocalGlassTheme.current
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = glass.textPrimary.copy(alpha = 0.55f),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(glass.surface)
                .border(1.dp, Brush.linearGradient(listOf(glass.borderStrong, glass.border, glass.shimmer)), RoundedCornerShape(18.dp))
                .glowShadow(AppColors.Calorie.copy(alpha = 0.04f), radius = 24.dp, offsetY = 8.dp)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) { content() }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    inlineMenu: Boolean = false,
    onClick: () -> Unit
) {
    val glass = LocalGlassTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppColors.Calorie,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
        }
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = glass.textPrimary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = glass.textPrimary.copy(alpha = 0.6f))
        Icon(
            if (inlineMenu) Icons.Filled.UnfoldMore else Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = glass.textPrimary.copy(alpha = 0.4f),
            modifier = if (inlineMenu) Modifier.size(18.dp) else Modifier
        )
    }
}

@Composable
private fun MacroSettingRow(
    label: String,
    value: Int,
    pinned: Boolean,
    onClick: () -> Unit
) {
    val glass = LocalGlassTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.DataUsage,
            contentDescription = null,
            tint = AppColors.Calorie,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = glass.textPrimary)
        Text(
            if (pinned) "${value}g" else "${value}g · auto",
            style = MaterialTheme.typography.bodyMedium,
            color = glass.textPrimary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            if (pinned) Icons.Filled.Lock else Icons.Outlined.LockOpen,
            contentDescription = if (pinned) "Pinned" else "Auto",
            tint = if (pinned) AppColors.Calorie
                   else glass.textPrimary.copy(alpha = 0.55f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun CustomInstructionsBlock(
    initial: String,
    placeholder: String,
    onSave: (String) -> Unit
) {
    val glass = LocalGlassTheme.current
    var text by remember(initial) { mutableStateOf(initial) }
    var saved by remember(initial) { mutableStateOf(initial) }
    val hasChanges = text != saved
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(placeholder, color = glass.textPrimary.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = glass.textPrimary,
                unfocusedTextColor = glass.textPrimary,
                focusedContainerColor = glass.surface.copy(alpha = 0.3f),
                unfocusedContainerColor = glass.surface.copy(alpha = 0.3f),
                focusedBorderColor = AppColors.Calorie,
                unfocusedBorderColor = glass.border,
                cursorColor = AppColors.Calorie
            )
        )
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = {
                onSave(text)
                saved = text.trim()
                text = saved
            },
            enabled = hasChanges,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = if (hasChanges) AppColors.Calorie else glass.textPrimary.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.settings_save),
                color = if (hasChanges) AppColors.Calorie else glass.textPrimary.copy(alpha = 0.4f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    icon: ImageVector? = null,
    onChange: (Boolean) -> Unit
) {
    val glass = LocalGlassTheme.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppColors.Calorie,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
        }
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = glass.textPrimary)
        Switch(
            checked = checked, 
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.Calorie,
                uncheckedThumbColor = glass.textPrimary.copy(alpha = 0.7f),
                uncheckedTrackColor = glass.surfaceHover,
                uncheckedBorderColor = glass.border
            )
        )
    }
}

private fun feetInchesLabel(cm: Int): String {
    val totalInches = (cm / 2.54).toInt()
    val feet = totalInches / 12
    val inches = totalInches % 12
    return "$feet' $inches\""
}

private fun optionalNutrientSummary(goals: OptionalNutrientGoals): String =
    "Fiber ${goals.fiber}g, Sodium ${goals.sodium}mg"

private fun OptionalNutrient.pickerRange(): IntRange = when (this) {
    OptionalNutrient.SUGAR -> 0..200
    OptionalNutrient.ADDED_SUGAR -> 0..100
    OptionalNutrient.FIBER -> 0..100
    OptionalNutrient.SATURATED_FAT -> 0..80
    OptionalNutrient.CHOLESTEROL -> 0..1000
    OptionalNutrient.SODIUM -> 0..5000
    OptionalNutrient.POTASSIUM -> 0..7000
    OptionalNutrient.TRANS_FAT -> 0..10
    OptionalNutrient.CALCIUM -> 300..2000
    OptionalNutrient.IRON -> 5..45
    OptionalNutrient.MAGNESIUM -> 100..800
    OptionalNutrient.ZINC -> 3..40
    OptionalNutrient.VITAMIN_A -> 300..3000
    OptionalNutrient.VITAMIN_C -> 20..500
    OptionalNutrient.VITAMIN_D -> 5..100
    OptionalNutrient.VITAMIN_B12 -> 1..20
    OptionalNutrient.VITAMIN_E -> 5..100
    OptionalNutrient.VITAMIN_K -> 30..300
    OptionalNutrient.FOLATE -> 100..1000
    OptionalNutrient.OMEGA3 -> 0..10
}

private fun OptionalNutrient.pickerStep(): Int = when (this) {
    OptionalNutrient.FIBER,
    OptionalNutrient.SATURATED_FAT,
    OptionalNutrient.TRANS_FAT,
    OptionalNutrient.IRON,
    OptionalNutrient.ZINC,
    OptionalNutrient.VITAMIN_D,
    OptionalNutrient.VITAMIN_B12,
    OptionalNutrient.VITAMIN_E,
    OptionalNutrient.OMEGA3 -> 1
    OptionalNutrient.CHOLESTEROL -> 25
    OptionalNutrient.SODIUM,
    OptionalNutrient.POTASSIUM,
    OptionalNutrient.CALCIUM,
    OptionalNutrient.VITAMIN_A,
    OptionalNutrient.FOLATE -> 50
    OptionalNutrient.MAGNESIUM -> 25
    OptionalNutrient.VITAMIN_C,
    OptionalNutrient.VITAMIN_K -> 10
    OptionalNutrient.SUGAR,
    OptionalNutrient.ADDED_SUGAR -> 5
}

private val birthdayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

private fun birthdayDisplay(profile: UserProfile): String {
    val date = profile.birthday.atZone(ZoneId.systemDefault()).toLocalDate()
    return "${date.format(birthdayFormatter)} (age ${profile.age})"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdaySheet(current: Instant, onSave: (Instant) -> Unit) {
    val glass = LocalGlassTheme.current
    val localDate = current.atZone(ZoneId.systemDefault()).toLocalDate()
    val initialMillis = localDate.atStartOfDay(java.time.ZoneOffset.UTC)
        .toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    Text("Birthday", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glass.textPrimary)
    Spacer(Modifier.height(8.dp))
    DatePicker(
        state = state,
        title = null,
        headline = null,
        showModeToggle = false,
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
    Spacer(Modifier.height(12.dp))
    GradientSaveButton {
        val millis = state.selectedDateMillis ?: return@GradientSaveButton
        val newDate = Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
        val newInstant = newDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        onSave(newInstant)
    }
    Spacer(Modifier.height(8.dp))
}

private fun genderIcon(g: Gender): ImageVector = when (g) {
    Gender.MALE -> Icons.Outlined.Male
    Gender.FEMALE -> Icons.Outlined.Female
    Gender.OTHER -> Icons.Outlined.Wc
}

private fun activityIcon(a: ActivityLevel): ImageVector = when (a) {
    ActivityLevel.SEDENTARY -> Icons.Outlined.SelfImprovement
    ActivityLevel.LIGHT -> Icons.AutoMirrored.Outlined.DirectionsWalk
    ActivityLevel.MODERATE -> Icons.AutoMirrored.Outlined.DirectionsRun
    ActivityLevel.ACTIVE -> Icons.Outlined.LocalDining
    ActivityLevel.VERY_ACTIVE -> Icons.Outlined.FitnessCenter
    ActivityLevel.EXTRA_ACTIVE -> Icons.Outlined.SportsMartialArts
}

private fun goalIcon(g: WeightGoal): ImageVector = when (g) {
    WeightGoal.LOSE -> Icons.AutoMirrored.Filled.TrendingDown
    WeightGoal.MAINTAIN -> Icons.AutoMirrored.Filled.TrendingFlat
    WeightGoal.GAIN -> Icons.AutoMirrored.Outlined.TrendingUp
}

private fun appearanceIcon(key: String): ImageVector = when (key) {
    "light" -> Icons.Outlined.LightMode
    "dark" -> Icons.Outlined.DarkMode
    else -> Icons.Outlined.SettingsBrightness
}

@Composable
private fun GradientSaveButton(
    text: String = "Save",
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val brush = Brush.linearGradient(listOf(AppColors.CalorieStart, AppColors.CalorieEnd))
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) brush else Brush.linearGradient(listOf(AppColors.Calorie.copy(alpha = 0.4f), AppColors.Calorie.copy(alpha = 0.4f))))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

data class GlassTheme(
    val surface: Color, val surfaceHover: Color, val border: Color,
    val borderStrong: Color, val shimmer: Color, val divider: Color,
    val bgTop: Color, val bgBottom: Color, val textPrimary: Color,
    val orbA: Color, val orbB: Color, val orbC: Color
)

val DarkGlass = GlassTheme(
    surface = Color(0x26FFFFFF), surfaceHover = Color(0x33FFFFFF), border = Color(0x40FFFFFF),
    borderStrong = Color(0x66FFFFFF), shimmer = Color(0x0DFFFFFF), divider = Color(0x18FFFFFF),
    bgTop = Color(0xFF0D0D0F), bgBottom = Color(0xFF14101A), textPrimary = Color.White,
    orbA = Color(0x40FF6B8A), orbB = Color(0x28C94B7D), orbC = Color(0x18FF8C5A)
)

val LightGlass = GlassTheme(
    surface = Color(0x40FFFFFF), surfaceHover = Color(0x66FFFFFF), border = Color(0x20000000),
    borderStrong = Color(0x40000000), shimmer = Color(0x0D000000), divider = Color(0x10000000),
    bgTop = Color(0xFFF2F2F7), bgBottom = Color(0xFFE5E5EA), textPrimary = Color(0xFF1C1C1E),
    orbA = Color(0x40FF6B8A), orbB = Color(0x28C94B7D), orbC = Color(0x18FF8C5A)
)

val LocalGlassTheme = staticCompositionLocalOf { DarkGlass }

@Composable
private fun AmbientOrbs(scrollState: ScrollState) {
    val glass = LocalGlassTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val parallax = scrollState.value * 0.15f
        val orbDrift = drift * 30f
        drawCircle(Brush.radialGradient(listOf(glass.orbA, Color.Transparent), center = Offset(size.width * 0.15f, 180f - parallax + orbDrift), radius = 340f), radius = 340f, center = Offset(size.width * 0.15f, 180f - parallax + orbDrift))
        drawCircle(Brush.radialGradient(listOf(glass.orbB, Color.Transparent), center = Offset(size.width * 0.82f, 320f - parallax * 0.5f - orbDrift), radius = 260f), radius = 260f, center = Offset(size.width * 0.82f, 320f - parallax * 0.5f - orbDrift))
    }
}
