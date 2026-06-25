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
                    text = "Weekly Activity (min)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                WeeklyActivityChart(weeklyData = weeklyData)
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

@Composable
fun WeeklyActivityChart(
    weeklyData: List<DayTotal>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val daysCount = 7
        val canvasWidth = size.width
        val canvasHeight = size.height

        val textHeight = 24.dp.toPx()
        val graphHeight = canvasHeight - textHeight
        val barWidth = 28.dp.toPx()
        val spacing = (canvasWidth - (barWidth * daysCount)) / (daysCount + 1)

        val today = LocalDate.now()
        val last7Days = (0..6).map { today.minusDays(6L - it) }

        val dataMap = weeklyData.associateBy { it.epochDay }
        val maxMinutes = weeklyData.maxOfOrNull { it.totalMinutes }?.toFloat()?.coerceAtLeast(30f) ?: 60f

        last7Days.forEachIndexed { index, date ->
            val epoch = date.toEpochDay()
            val totalMinutes = dataMap[epoch]?.totalMinutes ?: 0
            val barHeightPercent = totalMinutes / maxMinutes
            val currentBarHeight = graphHeight * barHeightPercent

            val x = spacing + index * (barWidth + spacing)
            val y = graphHeight - currentBarHeight

            // Draw Bar with rounded corners
            drawRoundRect(
                color = if (date == today) barColor else barColor.copy(alpha = 0.5f),
                topLeft = Offset(x, y),
                size = Size(barWidth, currentBarHeight.coerceAtLeast(4.dp.toPx())),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Draw Day Text
            val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
            // Draw text manually using native canvas or basic layout.
            // Since drawText requires full Paint, let's keep it simple or let Android paint handle text.
            // To be robust, we'll draw thin indicator lines or write simple text labels.
        }
    }

    // Days labels rendered as Compose views below Canvas
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val today = LocalDate.now()
        val last7Days = (0..6).map { today.minusDays(6L - it) }
        last7Days.forEach { date ->
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
                style = MaterialTheme.typography.bodySmall,
                color = if (date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )
        }
    }
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
