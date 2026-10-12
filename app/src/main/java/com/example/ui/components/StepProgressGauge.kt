package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealAccent
import java.util.Locale

@Composable
fun StepProgressGauge(
    currentSteps: Int,
    goalSteps: Int,
    distanceMeters: Float,
    caloriesBurned: Int,
    activeMinutes: Int,
    onSimulateStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (currentSteps.toFloat() / goalSteps.coerceAtLeast(1).toFloat()).coerceIn(0f, 1.5f)
    val animatedProgress by animateFloatAsState(
        targetValue = (progress.coerceAtMost(1f)),
        animationSpec = tween(durationMillis = 800),
        label = "stepProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("step_progress_gauge_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Arc Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                val primaryColor = MaterialTheme.colorScheme.primary
                val accentColor = TealAccent

                Canvas(modifier = Modifier.size(200.dp)) {
                    val strokeWidth = 16.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Background Track Arc (240 degrees)
                    drawArc(
                        color = trackColor,
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active Progress Arc
                    val sweepAngle = 240f * animatedProgress
                    if (sweepAngle > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(primaryColor, accentColor, primaryColor),
                                center = Offset(size.width / 2, size.height / 2)
                            ),
                            startAngle = 150f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Inner Step Counter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = "Steps",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%,d", currentSteps),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Goal: %,d steps".format(Locale.US, goalSteps),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val percent = ((currentSteps.toFloat() / goalSteps) * 100).toInt()
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "$percent%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Metrics Row: Distance, Calories, Active Minutes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    icon = Icons.Default.DirectionsRun,
                    value = "%.2f km".format(Locale.US, distanceMeters / 1000f),
                    label = "Distance",
                    tint = TealAccent
                )
                MetricItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "$caloriesBurned kcal",
                    label = "Burned",
                    tint = Color(0xFFFF7043)
                )
                MetricItem(
                    icon = Icons.Default.Timer,
                    value = "$activeMinutes min",
                    label = "Active",
                    tint = EmeraldPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test / Simulation Action (Pedometer trigger)
            Button(
                onClick = onSimulateStep,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulate_step_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Simulate steps",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Simulate Walking (+100 steps)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
