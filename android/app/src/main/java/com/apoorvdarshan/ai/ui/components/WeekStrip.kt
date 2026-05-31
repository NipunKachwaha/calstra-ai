package com.apoorvdarshan.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apoorvdarshan.ai.ui.theme.AppColors
import com.apoorvdarshan.ai.ui.theme.LocalGlassTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val TOTAL_WEEKS = 53
private val CURRENT_WEEK_INDEX = TOTAL_WEEKS - 1

@Composable
fun WeekEnergyStrip(
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    weekStartsOnMonday: Boolean = false
) {
    val firstDow = remember(weekStartsOnMonday) {
        if (weekStartsOnMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    }
    val today = remember { LocalDate.now() }
    val startOfCurrentWeek = remember(today, firstDow) {
        val daysBack = ((today.dayOfWeek.value - firstDow.value) + 7) % 7
        today.minusDays(daysBack.toLong())
    }
    val targetIndex = remember(selectedDate, startOfCurrentWeek) {
        val selectedWeekStart = run {
            val daysBack = ((selectedDate.dayOfWeek.value - firstDow.value) + 7) % 7
            selectedDate.minusDays(daysBack.toLong())
        }
        val weeksDiff = ChronoUnit.WEEKS.between(startOfCurrentWeek, selectedWeekStart).toInt()
        (CURRENT_WEEK_INDEX + weeksDiff).coerceIn(0, TOTAL_WEEKS - 1)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = targetIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(Unit) {
        listState.scrollToItem(targetIndex)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val pageWidth = maxWidth
        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxWidth()
        ) {
            items((0 until TOTAL_WEEKS).toList()) { weekIndex ->
                val weekStart = startOfCurrentWeek.plusWeeks((weekIndex - CURRENT_WEEK_INDEX).toLong())
                Box(modifier = Modifier.width(pageWidth)) {
                    WeekRow(
                        weekStart = weekStart,
                        selectedDate = selectedDate,
                        today = today,
                        onSelect = onSelect
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekRow(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp) // UPGRADE: Added spacing between the cards
    ) {
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            DayTile(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                onTap = { onSelect(date) },
                modifier = Modifier.weight(1f) // Ensures all 7 cards stay exactly the same size
            )
        }
    }
}

@Composable
private fun DayTile(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassTheme.current

    // UPGRADE: The entire DayTile is now a Card
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Brush.linearGradient(listOf(AppColors.CalorieStart, AppColors.CalorieEnd))
                else Brush.linearGradient(listOf(glass.surfaceHover, glass.surfaceHover))
            )
            .then(
                if (isToday && !isSelected) Modifier.border(1.dp, AppColors.Calorie.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                else if (!isSelected) Modifier.border(1.dp, glass.border, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
    ) {
        // Day of Week
        Text(
            text = narrowDay(date.dayOfWeek),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White.copy(alpha = 0.85f) else glass.textPrimary.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(4.dp))

        // Date Number
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else glass.textPrimary
        )

        // Small indicator dot under the number if it is exactly "Today" AND selected
        if (isToday && isSelected) {
            Spacer(Modifier.height(2.dp))
            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.White))
        }
    }
}

/** SwiftUI's `.dateTime.weekday(.narrow)` — single-letter short day name. */
private fun narrowDay(dow: DayOfWeek): String = when (dow) {
    DayOfWeek.MONDAY -> "M"
    DayOfWeek.TUESDAY -> "T"
    DayOfWeek.WEDNESDAY -> "W"
    DayOfWeek.THURSDAY -> "T"
    DayOfWeek.FRIDAY -> "F"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "S"
} 