package com.apoorvdarshan.ai.ui.progress

import androidx.annotation.StringRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apoorvdarshan.ai.AppContainer
import com.apoorvdarshan.ai.R
import com.apoorvdarshan.ai.models.BodyFatEntry
import com.apoorvdarshan.ai.models.WeightEntry
import com.apoorvdarshan.ai.ui.components.DecimalWheelPicker
import com.apoorvdarshan.ai.ui.components.SplitDecimalWheelPicker
import com.apoorvdarshan.ai.ui.theme.AppColors
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.geometry.CornerRadius
import com.apoorvdarshan.ai.ui.theme.glowShadow

// ─────────────────────────────────────────────────────────────────────────────
//  Glass Theme System (Same as Coach/About screen)
// ─────────────────────────────────────────────────────────────────────────────

data class GlassTheme(
    val surface: Color,
    val surfaceHover: Color,
    val border: Color,
    val borderStrong: Color,
    val shimmer: Color,
    val divider: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val textPrimary: Color,
    val orbA: Color,
    val orbB: Color,
    val orbC: Color
)

val DarkGlass = GlassTheme(
    surface      = Color(0x26FFFFFF),
    surfaceHover = Color(0x33FFFFFF),
    border       = Color(0x40FFFFFF),
    borderStrong = Color(0x66FFFFFF),
    shimmer      = Color(0x0DFFFFFF),
    divider      = Color(0x18FFFFFF),
    bgTop        = Color(0xFF0D0D0F),
    bgBottom     = Color(0xFF14101A),
    textPrimary  = Color.White,
    orbA         = Color(0x40FF6B8A),
    orbB         = Color(0x28C94B7D),
    orbC         = Color(0x18FF8C5A)
)

val LightGlass = GlassTheme(
    surface      = Color(0x40FFFFFF),
    surfaceHover = Color(0x66FFFFFF),
    border       = Color(0x20000000),
    borderStrong = Color(0x40000000),
    shimmer      = Color(0x0D000000),
    divider      = Color(0x10000000),
    bgTop        = Color(0xFFF2F2F7),
    bgBottom     = Color(0xFFE5E5EA),
    textPrimary  = Color(0xFF1C1C1E),
    orbA         = Color(0x40FF6B8A),
    orbB         = Color(0x28C94B7D),
    orbC         = Color(0x18FF8C5A)
)

val LocalGlassTheme = staticCompositionLocalOf { DarkGlass }

// ─────────────────────────────────────────────────────────────────────────────
//  Data & Enums
// ─────────────────────────────────────────────────────────────────────────────

enum class TimeRange(@get:StringRes val labelRes: Int, val days: Int) {
    WEEK(R.string.progress_range_week, 7),
    MONTH(R.string.progress_range_month, 30),
    THREE_MONTHS(R.string.progress_range_3m, 90),
    SIX_MONTHS(R.string.progress_range_6m, 180),
    YEAR(R.string.progress_range_year, 365),
    ALL_TIME(R.string.progress_range_all, 3650);

    fun dateRange(today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> {
        val start = today.minusDays((days - 1).toLong())
        return start to today
    }
}

enum class BodyMetric { WEIGHT, BODY_FAT }

// ─────────────────────────────────────────────────────────────────────────────
//  Main Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(container: AppContainer) {
    val vm: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory(container))
    val ui by vm.ui.collectAsState()
    val foods by container.foodRepository.entries.collectAsState(initial = emptyList())
    val useMetric by container.prefs.useMetric.collectAsState(initial = true)

    var range by remember { mutableStateOf(TimeRange.WEEK) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddBodyFatDialog by remember { mutableStateOf(false) }
    var showAllWeights by remember { mutableStateOf(false) }
    var bodyMetric by remember { mutableStateOf(BodyMetric.WEIGHT) }

    val listState = rememberLazyListState()

    // Theme logic
    val appearance by container.prefs.appearanceMode.collectAsState(initial = "system")
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (appearance) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
    val glassTheme = remember(isDarkTheme) { if (isDarkTheme) DarkGlass else LightGlass }

    // Filter weights + body fats to range
    val (rangeStartDate, rangeEndDate) = range.dateRange()
    val zone = ZoneId.systemDefault()
    val rangeStart = rangeStartDate.atStartOfDay(zone).toInstant()
    val rangeEnd = rangeEndDate.atTime(23, 59, 59).atZone(zone).toInstant()
    val filteredWeights = ui.entries.filter { it.date in rangeStart..rangeEnd }.sortedBy { it.date }
    val filteredBodyFats = ui.bodyFatEntries.filter { it.date in rangeStart..rangeEnd }.sortedBy { it.date }
    
    val bodyFatAvailable = ui.bodyFatEntries.isNotEmpty()
        || ui.profile?.bodyFatPercentage != null
        || ui.profile?.goalBodyFatPercentage != null

    val dailyCalories = remember(foods, range) {
        val today = LocalDate.now()
        (0 until range.days).mapNotNull { offset ->
            val day = today.minusDays(offset.toLong())
            val cals = foods
                .filter { it.timestamp.atZone(zone).toLocalDate() == day }
                .sumOf { it.calories }
            if (cals == 0) null else day to cals
        }.reversed()
    }

    val macroAverages = remember(foods, range) {
        val today = LocalDate.now()
        var p = 0; var c = 0; var f = 0; var n = 0
        for (offset in 0 until range.days) {
            val day = today.minusDays(offset.toLong())
            val dayEntries = foods.filter { it.timestamp.atZone(zone).toLocalDate() == day }
            if (dayEntries.isEmpty()) continue
            p += dayEntries.sumOf { it.protein }.toInt()
            c += dayEntries.sumOf { it.carbs }.toInt()
            f += dayEntries.sumOf { it.fat }.toInt()
            n += 1
        }
        if (n == 0) Triple(0, 0, 0) else Triple(p / n, c / n, f / n)
    }

    CompositionLocalProvider(LocalGlassTheme provides glassTheme) {
        val glass = LocalGlassTheme.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(glass.bgTop, glass.bgBottom)))
        ) {
            AmbientOrbs(listState = listState)

            Scaffold(containerColor = Color.Transparent) { padding ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { TimeRangePicker(selected = range, onSelect = { range = it }) }

                    item {
                        if (bodyFatAvailable) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                BodyMetricToggle(selected = bodyMetric, onSelect = { bodyMetric = it })
                                CardSection {
                                    when (bodyMetric) {
                                        BodyMetric.WEIGHT -> WeightSection(
                                            entries = filteredWeights,
                                            latest = ui.entries.maxByOrNull { it.date },
                                            goalKg = ui.profile?.goalWeightKg,
                                            useMetric = useMetric,
                                            onLogWeight = { showAddDialog = true }
                                        )
                                        BodyMetric.BODY_FAT -> BodyFatSection(
                                            entries = filteredBodyFats,
                                            latest = ui.bodyFatEntries.maxByOrNull { it.date }?.bodyFatFraction
                                                ?: ui.profile?.bodyFatPercentage,
                                            goalFraction = ui.profile?.goalBodyFatPercentage,
                                            onLogBodyFat = { showAddBodyFatDialog = true }
                                        )
                                    }
                                }
                            }
                        } else {
                            CardSection {
                                WeightSection(
                                    entries = filteredWeights,
                                    latest = ui.entries.maxByOrNull { it.date },
                                    goalKg = ui.profile?.goalWeightKg,
                                    useMetric = useMetric,
                                    onLogWeight = { showAddDialog = true }
                                )
                            }
                        }
                    }

                    if (ui.entries.isNotEmpty()) {
                        item {
                            WeightHistoryLink(count = ui.entries.size) { showAllWeights = true }
                        }
                    }

                    item {
                        CardSection {
                            CalorieSection(
                                dailyCalories = dailyCalories,
                                calorieGoal = ui.profile?.effectiveCalories ?: 2000
                            )
                        }
                    }

                    ui.profile?.let { p ->
                        item {
                            CardSection {
                                MacroAveragesSection(
                                    avgProtein = macroAverages.first,
                                    avgCarbs = macroAverages.second,
                                    avgFat = macroAverages.third,
                                    proteinGoal = p.effectiveProtein,
                                    carbsGoal = p.effectiveCarbs,
                                    fatGoal = p.effectiveFat
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val seedKg = ui.entries.maxByOrNull { it.date }?.weightKg ?: ui.profile?.weightKg ?: 70.0
        AddWeightDialog(useMetric = useMetric, initialKg = seedKg, onDismiss = { showAddDialog = false }) { kg ->
            vm.addWeight(kg); showAddDialog = false
        }
    }
    if (showAddBodyFatDialog) {
        val seedFraction = ui.bodyFatEntries.maxByOrNull { it.date }?.bodyFatFraction ?: ui.profile?.bodyFatPercentage ?: 0.20
        AddBodyFatDialog(initialFraction = seedFraction, onDismiss = { showAddBodyFatDialog = false }) { fraction ->
            vm.addBodyFat(fraction); showAddBodyFatDialog = false
        }
    }
    if (showAllWeights) {
        CompositionLocalProvider(LocalGlassTheme provides glassTheme) {
            AllWeightHistorySheet(
                entries = ui.entries.sortedByDescending { it.date },
                useMetric = useMetric,
                onDelete = vm::deleteWeight,
                onDismiss = { showAllWeights = false }
            )
        }
    }
    if (ui.goalReached) {
        CompositionLocalProvider(LocalGlassTheme provides glassTheme) {
            val glass = LocalGlassTheme.current
            AlertDialog(
                onDismissRequest = { vm.dismissGoalReached() },
                title = { Text(stringResource(R.string.progress_goal_reached_title), fontWeight = FontWeight.SemiBold, color = glass.textPrimary) },
                text = { Text(stringResource(R.string.progress_goal_reached_message), color = glass.textPrimary.copy(alpha = 0.8f)) },
                confirmButton = { TextButton(onClick = { vm.dismissGoalReached() }) { Text(stringResource(R.string.action_keep_going), color = AppColors.Calorie) } },
                containerColor = glass.bgBottom,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(1.dp, glass.border, RoundedCornerShape(20.dp))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ambient Orbs
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AmbientOrbs(listState: LazyListState) {
    val glass = LocalGlassTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val parallax = listState.firstVisibleItemScrollOffset * 0.15f
        val orbDrift = drift * 30f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glass.orbA, Color.Transparent),
                center = Offset(size.width * 0.15f, 180f - parallax + orbDrift),
                radius = 340f
            ),
            radius = 340f,
            center = Offset(size.width * 0.15f, 180f - parallax + orbDrift)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glass.orbB, Color.Transparent),
                center = Offset(size.width * 0.82f, 320f - parallax * 0.5f - orbDrift),
                radius = 260f
            ),
            radius = 260f,
            center = Offset(size.width * 0.82f, 320f - parallax * 0.5f - orbDrift)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glass.orbC, Color.Transparent),
                center = Offset(size.width * 0.6f, size.height * 0.75f + parallax * 0.3f),
                radius = 300f
            ),
            radius = 300f,
            center = Offset(size.width * 0.6f, size.height * 0.75f + parallax * 0.3f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI Components (Glassified)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimeRangePicker(selected: TimeRange, onSelect: (TimeRange) -> Unit) {
    val glass = LocalGlassTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(glass.surface)
            .border(1.dp, glass.border, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        for (r in TimeRange.values()) {
            val isSel = r == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isSel) glass.surfaceHover else Color.Transparent
                    )
                    .then(
                        if (isSel) Modifier.border(0.5.dp, glass.borderStrong, RoundedCornerShape(9.dp))
                        else Modifier
                    )
                    .clickable { onSelect(r) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(r.labelRes),
                    fontSize = 13.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSel) glass.textPrimary else glass.textPrimary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun BodyMetricToggle(selected: BodyMetric, onSelect: (BodyMetric) -> Unit) {
    val glass = LocalGlassTheme.current
    val labelWeight = stringResource(R.string.progress_metric_weight)
    val labelBodyFat = stringResource(R.string.progress_metric_body_fat)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(glass.surface)
            .border(1.dp, glass.border, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(BodyMetric.WEIGHT to labelWeight, BodyMetric.BODY_FAT to labelBodyFat).forEach { (metric, label) ->
            val isSelected = metric == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isSelected) AppColors.Calorie.copy(alpha = 0.15f) else Color.Transparent
                    )
                    .then(
                        if (isSelected) Modifier.border(0.5.dp, AppColors.Calorie.copy(0.3f), RoundedCornerShape(9.dp))
                        else Modifier
                    )
                    .clickable { onSelect(metric) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AppColors.Calorie else glass.textPrimary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun CardSection(content: @Composable () -> Unit) {
    val glass = LocalGlassTheme.current
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(glass.surface)
            .border(
                1.dp,
                Brush.linearGradient(listOf(glass.borderStrong, glass.border, glass.shimmer)),
                RoundedCornerShape(20.dp)
            )
            .glowShadow(AppColors.Calorie.copy(alpha = 0.04f), radius = 24.dp, offsetY = 8.dp)
            .padding(18.dp)
    ) { content() }
}

@Composable
private fun WeightSection(
    entries: List<WeightEntry>,
    latest: WeightEntry?,
    goalKg: Double?,
    useMetric: Boolean,
    onLogWeight: () -> Unit
) {
    val glass = LocalGlassTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.progress_weight_section), 
                fontSize = 18.sp, 
                fontWeight = FontWeight.SemiBold,
                color = glass.textPrimary
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onLogWeight)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AddCircle, null, tint = AppColors.Calorie, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.progress_log_weight), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.Calorie)
            }
        }
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.progress_log_first_weight),
                    fontSize = 15.sp,
                    color = glass.textPrimary.copy(alpha = 0.55f)
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                latest?.let { StatBadge(stringResource(R.string.progress_stat_current), formatWeight(it.weightKg, useMetric)) }
                goalKg?.let { StatBadge(stringResource(R.string.progress_stat_goal), formatWeight(it, useMetric)) }
            }
            WeightChartCanvas(entries = entries, goalKg = goalKg, useMetric = useMetric)
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    val glass = LocalGlassTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = glass.textPrimary)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = glass.textPrimary.copy(alpha = 0.5f))
    }
}

@Composable
private fun WeightChartCanvas(entries: List<WeightEntry>, goalKg: Double?, useMetric: Boolean) {
    val glass = LocalGlassTheme.current
    val displayKg = { kg: Double -> if (useMetric) kg else kg * 2.20462 }
    val displayWeights = entries.map { displayKg(it.weightKg) } + listOfNotNull(goalKg?.let(displayKg))
    val minW = displayWeights.min()
    val maxW = displayWeights.max()
    val pad = maxOf((maxW - minW) * 0.15, 2.0)
    val yMin = minW - pad
    val yMax = maxW + pad
    val tStart = entries.first().date.toEpochMilli()
    val tEnd = entries.last().date.toEpochMilli()
    val singleEntry = entries.size == 1
    val tRange = maxOf(1L, tEnd - tStart)
    
    val goalLineColor = AppColors.Calorie.copy(alpha = 0.6f)
    val gridColor = glass.borderStrong.copy(alpha = 0.2f)
    val secondaryColor = glass.textPrimary.copy(alpha = 0.45f)
    val ticks = niceAxisTicks(yMin, yMax, count = 5)
    val zone = ZoneId.systemDefault()
    val xLabelFmt = DateTimeFormatter.ofPattern("MMM d", Locale.US).withZone(zone)

    Row(Modifier.fillMaxWidth().height(180.dp)) {
        Canvas(Modifier.weight(1f).fillMaxSize()) {
            val w = size.width; val h = size.height
            ticks.forEach { tick ->
                val y = h - (((tick - yMin) / (yMax - yMin)).toFloat() * h)
                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
            }
            for (i in 0..4) {
                val x = (i.toFloat() / 4f) * w
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f), end = Offset(x, h),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
                )
            }
            goalKg?.let { gk ->
                val gv = displayKg(gk)
                val y = h - (((gv - yMin) / (yMax - yMin)).toFloat() * h)
                drawLine(
                    color = goalLineColor,
                    start = Offset(0f, y), end = Offset(w, y),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                )
            }
            val xFor: (WeightEntry) -> Float = { e ->
                if (singleEntry) w / 2f
                else ((e.date.toEpochMilli() - tStart).toDouble() / tRange * w).toFloat()
            }
            val path = Path()
            entries.forEachIndexed { i, e ->
                val x = xFor(e)
                val y = h - (((displayKg(e.weightKg) - yMin) / (yMax - yMin)).toFloat() * h)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, AppColors.Calorie, style = Stroke(width = 5f))
            entries.forEach { e ->
                val x = xFor(e)
                val y = h - (((displayKg(e.weightKg) - yMin) / (yMax - yMin)).toFloat() * h)
                drawCircle(AppColors.Calorie, radius = 5.5f, center = Offset(x, y))
            }
        }
        Column(
            Modifier.width(36.dp).fillMaxSize().padding(start = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ticks.reversed().forEach { tick ->
                Text(formatTick(tick), fontSize = 11.sp, color = secondaryColor)
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp, end = 36.dp)) {
        Text(xLabelFmt.format(entries.first().date), fontSize = 11.sp, color = secondaryColor)
        Spacer(Modifier.weight(1f))
        if (!singleEntry) {
            Text(xLabelFmt.format(entries.last().date), fontSize = 11.sp, color = secondaryColor)
        }
    }
}

private fun niceAxisTicks(min: Double, max: Double, count: Int): List<Double> {
    val range = max - min
    if (range <= 0) return listOf(min)
    val rawStep = range / (count - 1)
    val mag = Math.pow(10.0, Math.floor(Math.log10(rawStep)))
    val normalized = rawStep / mag
    val niceStep = when {
        normalized < 1.5 -> 1.0
        normalized < 3.0 -> 2.0
        normalized < 7.0 -> 5.0
        else -> 10.0
    } * mag
    val firstTick = Math.ceil(min / niceStep) * niceStep
    val out = mutableListOf<Double>()
    var v = firstTick
    while (v <= max + 1e-9) {
        out.add(v)
        v += niceStep
    }
    return out
}

private fun formatTick(value: Double): String =
    if (value >= 1000) String.format(Locale.US, "%,d", value.toInt())
    else if (value == value.toInt().toDouble()) value.toInt().toString()
    else String.format(Locale.US, "%.1f", value)

@Composable
private fun WeightHistoryLink(count: Int, onClick: () -> Unit) {
    val glass = LocalGlassTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(glass.surface)
            .border(1.dp, glass.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(glass.surface, glass.shimmer)))
                .border(0.5.dp, glass.border, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ListAlt, null, tint = AppColors.Calorie, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.progress_weight_history), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
            Text(
                stringResource(R.string.progress_history_count_format, count),
                fontSize = 13.sp,
                color = glass.textPrimary.copy(alpha = 0.55f)
            )
        }
        Icon(Icons.Filled.ChevronRight, null, tint = glass.textPrimary.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CalorieSection(dailyCalories: List<Pair<LocalDate, Int>>, calorieGoal: Int) {
    val glass = LocalGlassTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.progress_calories_section), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
            Spacer(Modifier.weight(1f))
            if (dailyCalories.isNotEmpty()) {
                val avg = dailyCalories.sumOf { it.second } / dailyCalories.size
                Text(
                    stringResource(R.string.progress_avg_format, avg),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = glass.textPrimary.copy(alpha = 0.6f)
                )
            }
        }
        if (dailyCalories.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.progress_no_food),
                    fontSize = 15.sp,
                    color = glass.textPrimary.copy(alpha = 0.55f)
                )
            }
        } else {
            CalorieBarChart(dailyCalories = dailyCalories, goal = calorieGoal)
        }
    }
}

@Composable
private fun CalorieBarChart(dailyCalories: List<Pair<LocalDate, Int>>, goal: Int) {
    val glass = LocalGlassTheme.current
    val maxValue = dailyCalories.maxOf { it.second }.coerceAtLeast(goal).toDouble()
    val gradientStart = AppColors.CalorieStart
    val gradientEnd = AppColors.CalorieEnd
    val goalColor = AppColors.Calorie.copy(alpha = 0.6f)
    val gridColor = glass.borderStrong.copy(alpha = 0.2f)
    val secondaryColor = glass.textPrimary.copy(alpha = 0.45f)
    val density = LocalDensity.current
    val ticks = niceAxisTicks(0.0, maxValue, count = 5)
    val yTop = ticks.last().coerceAtLeast(maxValue)
    val xLabelFmt = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    Column {
        Row(Modifier.fillMaxWidth().height(180.dp)) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxSize()) {
                val barAreaWidthPx = with(density) { maxWidth.toPx() }
                val n = dailyCalories.size
                val gap = 4f
                val maxBarPx = with(density) { 60.dp.toPx() }
                val rawWidth = (barAreaWidthPx - gap * (n - 1)) / n
                val barWidth = rawWidth.coerceIn(2f, maxBarPx)
                val totalGroupW = barWidth * n + gap * (n - 1)
                val startX = ((barAreaWidthPx - totalGroupW) / 2f).coerceAtLeast(0f)

                Canvas(Modifier.fillMaxSize()) {
                    val pxW = size.width; val pxH = size.height
                    ticks.forEach { tick ->
                        val y = pxH - ((tick / yTop).toFloat() * pxH)
                        drawLine(gridColor, Offset(0f, y), Offset(pxW, y), strokeWidth = 1f)
                    }
                    for (i in 0 until n) {
                        val cx = startX + i * (barWidth + gap) + barWidth / 2f
                        drawLine(
                            color = gridColor,
                            start = Offset(cx, 0f), end = Offset(cx, pxH),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
                        )
                    }
                    val goalY = pxH - ((goal / yTop).toFloat() * pxH)
                    drawLine(
                        color = goalColor,
                        start = Offset(0f, goalY), end = Offset(pxW, goalY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                    )
                    dailyCalories.forEachIndexed { i, (_, cals) ->
                        val barH = ((cals / yTop).toFloat() * pxH)
                        val x = startX + i * (barWidth + gap)
                        val y = pxH - barH
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(gradientEnd, gradientStart),
                                startY = y, endY = pxH
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
            }
            Column(
                Modifier.width(44.dp).fillMaxSize().padding(start = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ticks.reversed().forEach { tick ->
                    Text(formatTick(tick), fontSize = 11.sp, color = secondaryColor)
                }
            }
        }
        
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            BoxWithConstraints(Modifier.weight(1f)) {
                val areaWidthPx = with(density) { maxWidth.toPx() }
                val n = dailyCalories.size
                val gap = 4f
                val maxBarPx = with(density) { 60.dp.toPx() }
                val rawWidth = (areaWidthPx - gap * (n - 1)) / n
                val barWidth = rawWidth.coerceIn(2f, maxBarPx)
                val totalGroupW = barWidth * n + gap * (n - 1)
                val startX = ((areaWidthPx - totalGroupW) / 2f).coerceAtLeast(0f)
                val slotPx = barWidth + gap
                val slotDp = with(density) { slotPx.toDp() }
                val minLabelDp = 40.dp
                val slotStep = if (slotDp >= minLabelDp) 1
                    else Math.ceil((minLabelDp.value / slotDp.value).toDouble()).toInt().coerceAtLeast(1)
                val pickedIndices = buildList {
                    var i = 0
                    while (i < n) { add(i); i += slotStep }
                    if (last() != n - 1) add(n - 1)
                }.distinct()
                val labelBoxWidth = if (slotStep == 1) slotDp else minLabelDp.coerceAtLeast(slotDp)
                pickedIndices.forEach { i ->
                    val cxPx = startX + i * (barWidth + gap) + barWidth / 2f
                    val cxDp = with(density) { cxPx.toDp() }
                    Box(
                        Modifier
                            .width(labelBoxWidth)
                            .offset(x = cxDp - labelBoxWidth / 2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            xLabelFmt.format(dailyCalories[i].first),
                            fontSize = 11.sp,
                            color = secondaryColor,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.width(44.dp))
        }
    }
}

@Composable
private fun MacroAveragesSection(
    avgProtein: Int, avgCarbs: Int, avgFat: Int,
    proteinGoal: Int, carbsGoal: Int, fatGoal: Int
) {
    val glass = LocalGlassTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.progress_macro_averages), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
        MacroProgressRow(stringResource(R.string.macro_protein), avgProtein, proteinGoal)
        MacroProgressRow(stringResource(R.string.macro_carbs), avgCarbs, carbsGoal)
        MacroProgressRow(stringResource(R.string.macro_fat), avgFat, fatGoal)
    }
}

@Composable
private fun MacroProgressRow(label: String, current: Int, goal: Int) {
    val glass = LocalGlassTheme.current
    val progress = if (goal > 0) (current.toFloat() / goal).coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = glass.textPrimary)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.progress_macro_progress_format, current, goal),
                fontSize = 14.sp,
                color = glass.textPrimary.copy(alpha = 0.6f)
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(8.dp)) {
            val w = maxWidth
            Box(
                Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(glass.border.copy(alpha = 0.3f))
            )
            val barWidth = (w * progress).coerceAtLeast(6.dp)
            Box(
                Modifier
                    .width(barWidth)
                    .height(8.dp)
                    .glowShadow(AppColors.Calorie.copy(alpha = 0.5f), radius = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.CalorieGradient)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllWeightHistorySheet(
    entries: List<WeightEntry>,
    useMetric: Boolean,
    onDelete: (java.util.UUID) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassTheme.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US).withZone(ZoneId.systemDefault())
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = glass.bgTop // Adapting sheet to theme background
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.progress_weight_history), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done), color = AppColors.Calorie, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(12.dp))
            entries.forEach { entry ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(formatWeight(entry.weightKg, useMetric), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
                        Text(fmt.format(entry.date), fontSize = 13.sp, color = glass.textPrimary.copy(alpha = 0.55f))
                    }
                    IconButton(onClick = { onDelete(entry.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = glass.textPrimary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    }
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(glass.divider))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AddWeightDialog(
    useMetric: Boolean,
    initialKg: Double,
    onDismiss: () -> Unit,
    onSubmit: (Double) -> Unit
) {
    var pickerKg by remember { mutableStateOf(initialKg) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(stringResource(R.string.progress_log_weight_title), fontWeight = FontWeight.SemiBold) },
        text = {
            if (useMetric) {
                SplitDecimalWheelPicker(
                    value = pickerKg.coerceIn(30.0, 250.0),
                    onValueChange = { pickerKg = it },
                    min = 30,
                    max = 250,
                    unit = stringResource(R.string.unit_kg)
                )
            } else {
                val lbs = (pickerKg * 2.20462).coerceIn(60.0, 500.0)
                SplitDecimalWheelPicker(
                    value = lbs,
                    onValueChange = { newLbs -> pickerKg = newLbs / 2.20462 },
                    min = 60,
                    max = 500,
                    unit = stringResource(R.string.unit_lbs)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(pickerKg) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Calorie)
            ) { Text(stringResource(R.string.action_save), color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

private fun formatWeight(kg: Double, useMetric: Boolean): String =
    if (useMetric) String.format(Locale.US, "%.1f kg", kg)
    else String.format(Locale.US, "%.1f lbs", kg * 2.20462)


@Composable
private fun BodyFatSection(
    entries: List<BodyFatEntry>,
    latest: Double?,
    goalFraction: Double?,
    onLogBodyFat: () -> Unit
) {
    val glass = LocalGlassTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.progress_metric_body_fat), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onLogBodyFat)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AddCircle, null, tint = AppColors.Calorie, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.progress_log_body_fat), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.Calorie)
            }
        }
        if (entries.isEmpty() && latest == null) {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.progress_log_first_body_fat),
                    fontSize = 15.sp,
                    color = glass.textPrimary.copy(alpha = 0.55f)
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                latest?.let { StatBadge(stringResource(R.string.progress_stat_current), formatPercent(it)) }
                goalFraction?.let { StatBadge(stringResource(R.string.progress_stat_goal), formatPercent(it)) }
            }
            if (entries.isNotEmpty()) {
                BodyFatChartCanvas(entries = entries, goalFraction = goalFraction)
            }
        }
    }
}

@Composable
private fun BodyFatChartCanvas(entries: List<BodyFatEntry>, goalFraction: Double?) {
    val glass = LocalGlassTheme.current
    val percents = entries.map { it.bodyFatFraction * 100 } + listOfNotNull(goalFraction?.let { it * 100 })
    val minP = percents.min()
    val maxP = percents.max()
    val pad = maxOf((maxP - minP) * 0.15, 1.0)
    val yMin = (minP - pad).coerceAtLeast(0.0)
    val yMax = maxP + pad
    val tStart = entries.first().date.toEpochMilli()
    val tEnd = entries.last().date.toEpochMilli()
    val singleEntry = entries.size == 1
    val tRange = maxOf(1L, tEnd - tStart)
    
    val goalLineColor = AppColors.Calorie.copy(alpha = 0.6f)
    val gridColor = glass.borderStrong.copy(alpha = 0.2f)
    val secondaryColor = glass.textPrimary.copy(alpha = 0.45f)
    val ticks = niceAxisTicks(yMin, yMax, count = 5)
    val zone = ZoneId.systemDefault()
    val xLabelFmt = DateTimeFormatter.ofPattern("MMM d", Locale.US).withZone(zone)

    Row(Modifier.fillMaxWidth().height(180.dp)) {
        Canvas(Modifier.weight(1f).fillMaxSize()) {
            val w = size.width; val h = size.height
            ticks.forEach { tick ->
                val y = h - (((tick - yMin) / (yMax - yMin)).toFloat() * h)
                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
            }
            for (i in 0..4) {
                val x = (i.toFloat() / 4f) * w
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f), end = Offset(x, h),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
                )
            }
            goalFraction?.let { g ->
                val gPct = g * 100
                val y = h - (((gPct - yMin) / (yMax - yMin)).toFloat() * h)
                drawLine(
                    color = goalLineColor,
                    start = Offset(0f, y), end = Offset(w, y),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                )
            }
            val xFor: (BodyFatEntry) -> Float = { e ->
                if (singleEntry) w / 2f
                else ((e.date.toEpochMilli() - tStart).toDouble() / tRange * w).toFloat()
            }
            val path = Path()
            entries.forEachIndexed { i, e ->
                val x = xFor(e)
                val y = h - (((e.bodyFatFraction * 100 - yMin) / (yMax - yMin)).toFloat() * h)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, AppColors.Calorie, style = Stroke(width = 5f))
            entries.forEach { e ->
                val x = xFor(e)
                val y = h - (((e.bodyFatFraction * 100 - yMin) / (yMax - yMin)).toFloat() * h)
                drawCircle(AppColors.Calorie, radius = 5.5f, center = Offset(x, y))
            }
        }
        Column(
            Modifier.width(40.dp).fillMaxSize().padding(start = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ticks.reversed().forEach { tick ->
                Text(formatPercentTick(tick), fontSize = 11.sp, color = secondaryColor)
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp, end = 40.dp)) {
        Text(xLabelFmt.format(entries.first().date), fontSize = 11.sp, color = secondaryColor)
        Spacer(Modifier.weight(1f))
        if (!singleEntry) {
            Text(xLabelFmt.format(entries.last().date), fontSize = 11.sp, color = secondaryColor)
        }
    }
}

private fun formatPercentTick(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()}%"
    else String.format(Locale.US, "%.1f%%", rounded)
}

@Composable
private fun AddBodyFatDialog(
    initialFraction: Double,
    onDismiss: () -> Unit,
    onSubmit: (Double) -> Unit
) {
    var pct by remember { mutableStateOf(initialFraction * 100) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(stringResource(R.string.progress_log_body_fat_title), fontWeight = FontWeight.SemiBold) },
        text = {
            DecimalWheelPicker(
                value = pct.coerceIn(3.0, 60.0),
                onValueChange = { pct = it },
                min = 3.0,
                max = 60.0,
                step = 0.5,
                unit = stringResource(R.string.unit_percent)
            )
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(pct / 100.0) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Calorie)
            ) { Text(stringResource(R.string.action_save), color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

private fun formatPercent(fraction: Double): String =
    String.format(Locale.US, "%.1f%%", fraction * 100)
