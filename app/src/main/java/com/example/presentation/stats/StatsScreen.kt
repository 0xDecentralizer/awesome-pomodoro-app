package com.example.presentation.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DayTotal
import com.example.domain.model.LabelTotal
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.summaryStats.collectAsState()
    val weeklyData by viewModel.weeklyTotals.collectAsState()
    val labelTotals by viewModel.labelBreakdown.collectAsState()
    val monthlyData by viewModel.monthlyTotals.collectAsState()
    val yearlyData by viewModel.yearlyTotals.collectAsState()

    val currentStreak = stats?.currentStreak ?: 0
    val bestStreak = stats?.bestStreak ?: 0
    val totalWorkedDays = stats?.daysWorked ?: 0
    val totalHours = (stats?.allTimeMinutes ?: 0) / 60f
    val todayMinutes = stats?.todayMinutes ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 1. STREAK AND SUMMARY CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Streaks row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔥", style = MaterialTheme.typography.headlineLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Current Streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$currentStreak Days", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🏆", style = MaterialTheme.typography.headlineLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Best Streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$bestStreak Days", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // High-level aggregates row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = "⏱️", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = String.format(Locale.US, "%.1fh", totalHours), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Total Hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = "📅", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "$totalWorkedDays d", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Days Focused", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = "🎯", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${todayMinutes}m", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Today's Focus", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 2. WEEKLY BAR CHART SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Weekly Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                WeeklyActivityChart(weeklyData = weeklyData)
            }
        }

        // MONTHLY BAR CHART SECTION (Daily breakdown)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Monthly Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                MonthlyActivityChart(monthlyData = monthlyData)
            }
        }

        // YEARLY BAR CHART SECTION (Monthly breakdown)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Yearly Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                YearlyActivityChart(yearlyData = yearlyData)
            }
        }

        // 3. MONTHLY CONTRIBUTION HEATMAP
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Monthly Grid Heatmap",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                MonthlyHeatmapGrid(monthlyData = monthlyData)
            }
        }

        // 4. CATEGORY BREAKDOWN SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Category Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (labelTotals.isEmpty()) {
                    Text(
                        text = "No category data logged yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                } else {
                    val grandTotalMinutes = labelTotals.sumOf { it.totalMinutes }.toFloat()
                    labelTotals.sortedByDescending { it.totalMinutes }.forEach { total ->
                        val percent = if (grandTotalMinutes > 0) total.totalMinutes / grandTotalMinutes else 0f
                        val labelColor = try {
                            Color(android.graphics.Color.parseColor(total.labelColorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${total.labelEmoji} ${total.labelName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${total.totalMinutes}m (${(percent * 100).toInt()}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { percent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = labelColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ChartItem(
    val key: Any,
    val value: Float,
    val labelAbove: String,
    val labelBelow: String,
    val isHighlighted: Boolean = false
)

fun formatMinutesToDuration(minutes: Int): String {
    if (minutes == 0) return "0m"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

@Composable
fun ActivityBarChart(
    items: List<ChartItem>,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false
) {
    val barColor = MaterialTheme.colorScheme.primary
    val maxVal = items.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f

    val rowModifier = if (scrollable) {
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    } else {
        modifier.fillMaxWidth()
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = if (scrollable) Arrangement.spacedBy(12.dp) else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        items.forEach { item ->
            val barHeightPercent = item.value / maxVal
            val colModifier = if (scrollable) {
                Modifier.width(48.dp)
            } else {
                Modifier.weight(1f)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = colModifier
            ) {
                // Value label above bar
                Text(
                    text = item.labelAbove,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // Bar representation
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(100.dp * barHeightPercent.coerceAtLeast(0.03f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (item.isHighlighted) barColor else barColor.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Label
                Text(
                    text = item.labelBelow,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = if (item.isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (item.isHighlighted) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun WeeklyActivityChart(
    weeklyData: List<DayTotal>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val last7Days = (0..6).map { today.minusDays(6L - it) }
    val dataMap = weeklyData.associateBy { it.epochDay }

    val items = last7Days.map { date ->
        val epoch = date.toEpochDay()
        val totalMinutes = dataMap[epoch]?.totalMinutes ?: 0
        ChartItem(
            key = epoch,
            value = totalMinutes.toFloat(),
            labelAbove = formatMinutesToDuration(totalMinutes),
            labelBelow = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
            isHighlighted = date == today
        )
    }

    ActivityBarChart(items = items, modifier = modifier)
}

@Composable
fun MonthlyActivityChart(
    monthlyData: List<DayTotal>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val firstDayOfMonth = today.withDayOfMonth(1)
    val daysInMonth = today.lengthOfMonth()
    val dataMap = monthlyData.associateBy { it.epochDay }

    val items = (1..daysInMonth).map { day ->
        val date = firstDayOfMonth.withDayOfMonth(day)
        val totalMinutes = dataMap[date.toEpochDay()]?.totalMinutes ?: 0
        ChartItem(
            key = date.toEpochDay(),
            value = totalMinutes.toFloat(),
            labelAbove = formatMinutesToDuration(totalMinutes),
            labelBelow = "$day",
            isHighlighted = date == today
        )
    }

    ActivityBarChart(items = items, modifier = modifier, scrollable = true)
}

@Composable
fun YearlyActivityChart(
    yearlyData: List<MonthTotal>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val currentMonthNum = today.monthValue

    val items = yearlyData.map { monthTotal ->
        val monthDate = LocalDate.of(today.year, monthTotal.monthNumber, 1)
        val monthLabel = monthDate.month.getDisplayName(TextStyle.SHORT, Locale.US)
        ChartItem(
            key = monthTotal.monthNumber,
            value = monthTotal.totalMinutes.toFloat(),
            labelAbove = formatMinutesToDuration(monthTotal.totalMinutes),
            labelBelow = monthLabel,
            isHighlighted = monthTotal.monthNumber == currentMonthNum
        )
    }

    ActivityBarChart(items = items, modifier = modifier)
}

@Composable
fun MonthlyHeatmapGrid(
    monthlyData: List<DayTotal>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val daysInMonth = today.lengthOfMonth()
    val firstDayOfMonth = today.withDayOfMonth(1)
    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7 // 0 = Sun, 1 = Mon ...

    val totalCells = daysInMonth + dayOfWeekOffset
    val dataMap = monthlyData.associateBy { it.epochDay }
    val primaryColor = MaterialTheme.colorScheme.primary

    // Column containing weekdays headings + grid
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Grid of squares
        var cellIndex = 0
        val rows = (totalCells + 6) / 7

        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (c in 0 until 7) {
                    if (cellIndex < dayOfWeekOffset || cellIndex >= totalCells) {
                        // Empty spacer cells for padding start/end of month
                        Box(modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f))
                    } else {
                        val dayOfMonth = cellIndex - dayOfWeekOffset + 1
                        val cellDate = firstDayOfMonth.withDayOfMonth(dayOfMonth)
                        val totalMinutes = dataMap[cellDate.toEpochDay()]?.totalMinutes ?: 0

                        val color = when {
                            totalMinutes == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            totalMinutes < 25 -> primaryColor.copy(alpha = 0.2f)
                            totalMinutes < 50 -> primaryColor.copy(alpha = 0.5f)
                            else -> primaryColor
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$dayOfMonth",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                fontWeight = if (cellDate == today) FontWeight.Bold else FontWeight.Normal,
                                color = if (totalMinutes >= 50) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    cellIndex++
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
