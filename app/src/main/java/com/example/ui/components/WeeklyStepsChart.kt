package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyStepRecord
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun WeeklyStepsChart(
    dailySteps: List<DailyStepRecord>,
    goalSteps: Int = 10000,
    modifier: Modifier = Modifier
) {
    // Chronological order (oldest to newest)
    val orderedDays = dailySteps.take(7).reversed()
    val maxSteps = ((orderedDays.maxOfOrNull { it.stepCount } ?: 10000).coerceAtLeast(goalSteps) * 1.15f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_steps_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Activity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Goal: %,d".format(Locale.US, goalSteps),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val primaryColor = MaterialTheme.colorScheme.primary
            val dimColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)

            // Canvas for bars
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val count = orderedDays.size.coerceAtLeast(1)
                val barWidth = (canvasWidth / count) * 0.45f
                val spacing = canvasWidth / count

                // Draw Goal Dashed Line
                val goalY = canvasHeight - (canvasHeight * (goalSteps / maxSteps))
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(0f, goalY),
                    end = Offset(canvasWidth, goalY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                orderedDays.forEachIndexed { index, record ->
                    val barHeight = (canvasHeight * (record.stepCount / maxSteps)).coerceAtLeast(4.dp.toPx())
                    val x = (index * spacing) + (spacing - barWidth) / 2
                    val y = canvasHeight - barHeight

                    val isToday = index == orderedDays.lastIndex
                    val isGoalMet = record.stepCount >= goalSteps
                    val barColor = when {
                        isToday -> primaryColor
                        isGoalMet -> primaryColor.copy(alpha = 0.85f)
                        else -> dimColor
                    }

                    // Rounded Bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day labels row in pure Compose
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val outFormat = SimpleDateFormat("EEE", Locale.US)

                orderedDays.forEachIndexed { index, record ->
                    val dayName = try {
                        val parsed = inFormat.parse(record.dateKey)
                        if (parsed != null) outFormat.format(parsed) else record.dateKey.takeLast(2)
                    } catch (_: Exception) {
                        record.dateKey.takeLast(2)
                    }
                    val isToday = index == orderedDays.lastIndex

                    Text(
                        text = dayName,
                        fontSize = 11.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
