package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthconnect.HealthConnectStatus
import com.example.ui.HealthUiState
import com.example.ui.NavigationTab
import com.example.ui.components.StepProgressGauge
import com.example.ui.components.WeeklyStepsChart
import com.example.ui.theme.BpCrisis
import com.example.ui.theme.BpElevated
import com.example.ui.theme.BpNormal
import com.example.ui.theme.BpStage1
import com.example.ui.theme.BpStage2
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealAccent

@Composable
fun DashboardScreen(
    state: HealthUiState,
    onNavigateTab: (NavigationTab) -> Unit,
    onSimulateStep: () -> Unit,
    onTriggerSync: () -> Unit,
    onOpenLogBp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Health Connect Integration Banner
        HealthConnectSyncCard(
            status = state.healthConnectStatus,
            message = state.healthConnectMessage,
            onSyncClick = onTriggerSync
        )

        // Primary Step Gauge
        val today = state.todaySteps
        val steps = today?.stepCount ?: 0
        val goal = today?.goalSteps ?: 10000
        val dist = today?.distanceMeters ?: 0f
        val cals = today?.caloriesBurned ?: 0
        val activeMins = today?.activeMinutes ?: 0

        StepProgressGauge(
            currentSteps = steps,
            goalSteps = goal,
            distanceMeters = dist,
            caloriesBurned = cals,
            activeMinutes = activeMins,
            onSimulateStep = onSimulateStep
        )

        // Quick Outdoor Workout Start Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTab(NavigationTab.WORKOUT) }
                .testTag("dashboard_start_workout_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "Workout",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Outdoor GPS Workout",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Track real-time route, pace & split distance",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start Workout",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Cardiovascular Vitals Snapshot Row
        Text(
            text = "Cardiovascular Vitals",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Blood Pressure Snapshot Card
            val latestBp = state.latestBp
            val bpColor = when (latestBp?.category) {
                "Normal" -> BpNormal
                "Elevated" -> BpElevated
                "Stage 1", "Hypertension Stage 1" -> BpStage1
                "Stage 2", "Hypertension Stage 2" -> BpStage2
                else -> BpNormal
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateTab(NavigationTab.VITALS) }
                    .testTag("dashboard_bp_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorHeart,
                            contentDescription = "Blood Pressure",
                            tint = CoralHeart,
                            modifier = Modifier.size(20.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = bpColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = latestBp?.category ?: "Normal",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = bpColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (latestBp != null) "${latestBp.systolic}/${latestBp.diastolic}" else "--/--",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Blood Pressure (mmHg)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Heart Rate Snapshot Card
            val latestHr = state.latestHr
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateTab(NavigationTab.VITALS) }
                    .testTag("dashboard_hr_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart Rate",
                            tint = CoralHeart,
                            modifier = Modifier.size(20.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TealAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = latestHr?.contextTag ?: "Resting",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (latestHr != null) "${latestHr.bpm}" else "--",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Heart Rate (bpm)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Weekly Activity Chart
        WeeklyStepsChart(
            dailySteps = state.recent7Days,
            goalSteps = goal
        )

        // AI Health Insight Preview Card
        val report = state.healthReport
        if (report != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTab(NavigationTab.INSIGHTS) }
                    .testTag("dashboard_ai_insight_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Insight",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = report.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = report.summary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HealthConnectSyncCard(
    status: HealthConnectStatus,
    message: String?,
    onSyncClick: () -> Unit
) {
    val isConnected = status == HealthConnectStatus.CONNECTED
    val bg = if (isConnected) Color(0xFF064E3B).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
    val accent = if (isConnected) EmeraldPrimary else TealAccent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("health_connect_sync_card"),
        shape = RoundedCornerShape(16.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Sync,
                    contentDescription = "Health Connect",
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Google Health Connect",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = message ?: "Sync steps, BP & HR records",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Button(
                onClick = onSyncClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.copy(alpha = 0.2f),
                    contentColor = accent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("health_connect_sync_button")
            ) {
                Text(
                    text = if (isConnected) "Sync Now" else "Connect",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
