package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BloodPressureEntry
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.TealAccent

@Composable
fun BloodPressureChart(
    entries: List<BloodPressureEntry>,
    modifier: Modifier = Modifier
) {
    val recentEntries = entries.take(7).reversed() // Chronological order

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("blood_pressure_chart_card"),
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
                    text = "Blood Pressure Trend",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendBadge(label = "Systolic (120 ref)", color = CoralHeart)
                    Spacer(modifier = Modifier.width(10.dp))
                    LegendBadge(label = "Diastolic (80 ref)", color = TealAccent)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recentEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No readings logged yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    val minPressure = 50f
                    val maxPressure = 170f
                    val range = maxPressure - minPressure

                    fun toY(value: Float): Float {
                        return height - ((value - minPressure) / range * height)
                    }

                    // Draw AHA 120 (Systolic target) and 80 (Diastolic target) reference dashed lines
                    val y120 = toY(120f)
                    val y80 = toY(80f)

                    drawLine(
                        color = Color(0xFF2ED573).copy(alpha = 0.35f),
                        start = Offset(0f, y120),
                        end = Offset(width, y120),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                    drawLine(
                        color = Color(0xFF2ED573).copy(alpha = 0.35f),
                        start = Offset(0f, y80),
                        end = Offset(width, y80),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    val stepX = if (recentEntries.size > 1) width / (recentEntries.size - 1) else width / 2

                    val systolicPath = Path()
                    val diastolicPath = Path()

                    recentEntries.forEachIndexed { i, entry ->
                        val x = if (recentEntries.size > 1) (i * stepX) else (width / 2)
                        val sysY = toY(entry.systolic.toFloat())
                        val diaY = toY(entry.diastolic.toFloat())

                        if (i == 0) {
                            systolicPath.moveTo(x, sysY)
                            diastolicPath.moveTo(x, diaY)
                        } else {
                            systolicPath.lineTo(x, sysY)
                            diastolicPath.lineTo(x, diaY)
                        }

                        // Connecting vertical line between systolic and diastolic
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.25f),
                            start = Offset(x, sysY),
                            end = Offset(x, diaY),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Data circles
                        drawCircle(
                            color = CoralHeart,
                            radius = 4.dp.toPx(),
                            center = Offset(x, sysY)
                        )
                        drawCircle(
                            color = TealAccent,
                            radius = 4.dp.toPx(),
                            center = Offset(x, diaY)
                        )
                    }

                    // Stroke trend lines
                    if (recentEntries.size > 1) {
                        drawPath(
                            path = systolicPath,
                            color = CoralHeart,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = diastolicPath,
                            color = TealAccent,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendBadge(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(8.dp),
            shape = CircleShape,
            color = color
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
