package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyStepRecord
import com.example.ui.components.WeeklyStepsChart
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealAccent
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AnalyticsScreen(
    recent7Days: List<DailyStepRecord>,
    currentGoal: Int,
    onOpenEditGoalsDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSteps = recent7Days.sumOf { it.stepCount }
    val totalDistanceKm = recent7Days.sumOf { it.distanceMeters.toDouble() } / 1000.0
    val totalCalories = recent7Days.sumOf { it.caloriesBurned }
    val avgSteps = if (recent7Days.isNotEmpty()) totalSteps / recent7Days.size else 0
    val bestDay = recent7Days.maxByOrNull { it.stepCount }
    val goalsMetCount = recent7Days.count { it.stepCount >= it.goalSteps }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Weekly Analytics & Goals",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Detailed 7-day progress and targets",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary Metric Cards 2x2 Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    title = "7-Day Total",
                    value = String.format(Locale.US, "%,d", totalSteps),
                    unit = "steps",
                    icon = Icons.Default.Timeline,
                    tint = EmeraldPrimary
                )
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    title = "Daily Average",
                    value = String.format(Locale.US, "%,d", avgSteps),
                    unit = "steps/day",
                    icon = Icons.Default.EmojiEvents,
                    tint = TealAccent
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Distance",
                    value = "%.1f".format(Locale.US, totalDistanceKm),
                    unit = "km",
                    icon = Icons.Default.Route,
                    tint = Color(0xFF6366F1)
                )
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Energy",
                    value = String.format(Locale.US, "%,d", totalCalories),
                    unit = "kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    tint = Color(0xFFFF7043)
                )
            }
        }

        // Weekly Activity Chart
        item {
            WeeklyStepsChart(dailySteps = recent7Days, goalSteps = currentGoal)
        }

        // Goal Setting Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_management_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Daily Goal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%,d steps", currentGoal),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$goalsMetCount of 7 Days Hit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onOpenEditGoalsDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_goals_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Goal", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize Target Goals", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Best Day Highlight
        if (bestDay != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Trophy",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Weekly Peak Milestone",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${bestDay.dateKey}: %,d steps (%.2f km)".format(Locale.US, bestDay.stepCount, bestDay.distanceMeters / 1000f),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Daily Breakdown List
        item {
            Text(
                text = "Day-by-Day Log",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(recent7Days) { record ->
            val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outFormat = SimpleDateFormat("EEEE, MMM d", Locale.US)
            val prettyDate = try {
                val parsed = inFormat.parse(record.dateKey)
                if (parsed != null) outFormat.format(parsed) else record.dateKey
            } catch (_: Exception) {
                record.dateKey
            }
            val isGoalMet = record.stepCount >= record.goalSteps

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (isGoalMet) EmeraldPrimary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isGoalMet) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (isGoalMet) "Goal Met" else "Goal Incomplete",
                                tint = if (isGoalMet) EmeraldPrimary else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = prettyDate,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "%.2f km • %d kcal • %d mins".format(Locale.US, record.distanceMeters / 1000f, record.caloriesBurned, record.activeMinutes),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = String.format(Locale.US, "%,d", record.stepCount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (isGoalMet) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(tint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = " $unit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
            }
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
