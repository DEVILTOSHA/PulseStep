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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.HeartRateEntry
import com.example.ui.theme.CoralHeart

@Composable
fun HeartRateTrendChart(
    entries: List<HeartRateEntry>,
    modifier: Modifier = Modifier
) {
    val recent = entries.take(12).reversed()
    val avgBpm = if (recent.isNotEmpty()) recent.map { it.bpm }.average().toInt() else 72

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("heart_rate_trend_chart_card"),
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
                    text = "Heart Rate Variations",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Avg $avgBpm bpm",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CoralHeart
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recent.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No heart rate records yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    val minBpm = (recent.minOfOrNull { it.bpm } ?: 60).toFloat().coerceAtLeast(40f)
                    val maxBpm = (recent.maxOfOrNull { it.bpm } ?: 120).toFloat().coerceAtMost(180f) + 10f
                    val bpmRange = (maxBpm - minBpm).coerceAtLeast(20f)

                    val stepX = if (recent.size > 1) width / (recent.size - 1) else width / 2

                    val linePath = Path()
                    val fillPath = Path()

                    recent.forEachIndexed { i, entry ->
                        val x = if (recent.size > 1) i * stepX else width / 2
                        val y = height - (((entry.bpm - minBpm) / bpmRange) * height)

                        if (i == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }

                        if (i == recent.lastIndex) {
                            fillPath.lineTo(x, height)
                            fillPath.close()
                        }
                    }

                    // Fill Gradient
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CoralHeart.copy(alpha = 0.25f),
                                CoralHeart.copy(alpha = 0.0f)
                            )
                        )
                    )

                    // Line Stroke
                    drawPath(
                        path = linePath,
                        color = CoralHeart,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Circles on points
                    recent.forEachIndexed { i, entry ->
                        val x = if (recent.size > 1) i * stepX else width / 2
                        val y = height - (((entry.bpm - minBpm) / bpmRange) * height)
                        drawCircle(
                            color = CoralHeart,
                            radius = 3.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}
