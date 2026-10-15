package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.data.local.WorkoutSession
import com.example.location.WorkoutLiveMetrics
import com.example.ui.components.EmergencySosDialog
import com.example.ui.components.GoogleMapRouteView
import com.example.ui.components.RouteMapCanvas
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutScreen(
    liveMetrics: WorkoutLiveMetrics,
    recentWorkouts: List<WorkoutSession>,
    onStartWorkout: (activityType: String, simulate: Boolean) -> Unit,
    onPauseWorkout: () -> Unit,
    onResumeWorkout: () -> Unit,
    onFinishWorkout: () -> Unit,
    onRefreshLocation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedActivity by remember { mutableStateOf("Walking") }
    var simulateMovement by remember { mutableStateOf(false) }
    var isGpsEnabled by remember { mutableStateOf(true) }
    var showSosDialog by remember { mutableStateOf(false) }
    val activities = listOf("Walking", "Jogging", "Hiking")

    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            onRefreshLocation()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GPS Outdoor Workout",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Real-time phone sensor step detection & GPS path tracking",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Operable Safety SOS button: ONLY pops up if GPS is enabled!
                    AnimatedVisibility(visible = isGpsEnabled) {
                        Button(
                            onClick = { showSosDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3838)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("workout_sos_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Emergency SOS",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SOS",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // GPS Enable / Disable Status Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isGpsEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                                contentDescription = "GPS Status",
                                tint = if (isGpsEnabled) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isGpsEnabled) "GPS Location: ENABLED" else "GPS Location: DISABLED",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGpsEnabled) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isGpsEnabled) "Safety SOS & Real-Time Route Active" else "Enable GPS to activate live path and Safety SOS",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isGpsEnabled,
                            onCheckedChange = { isGpsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary),
                            modifier = Modifier.testTag("workout_gps_toggle")
                        )
                    }
                }

                // Permission Warning Card if not granted
                if (!hasLocationPermission && isGpsEnabled) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Location Permission Needed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                                Text(
                                    text = "Allow location access to enable live GPS pinpoint tracking.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Allow", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }

        // Interactive Google Maps & Route Tracking
        item {
            GoogleMapRouteView(
                routePoints = liveMetrics.routePoints,
                currentPosition = liveMetrics.currentPosition,
                isWorkoutRunning = liveMetrics.isRunning && !liveMetrics.isPaused,
                accuracyMeters = liveMetrics.accuracyMeters,
                onRefreshLocation = onRefreshLocation
            )
        }

        // Live Metrics Dashboard Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("workout_metrics_card"),
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
                    // Big Timer Display
                    val hours = liveMetrics.elapsedTimeSeconds / 3600
                    val minutes = (liveMetrics.elapsedTimeSeconds % 3600) / 60
                    val seconds = liveMetrics.elapsedTimeSeconds % 60
                    val timerStr = "%02d:%02d:%02d".format(Locale.US, hours, minutes, seconds)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DURATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = timerStr,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (liveMetrics.isRunning) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (liveMetrics.isPaused) Color(0xFFFFA502).copy(alpha = 0.2f)
                                        else EmeraldPrimary.copy(alpha = 0.2f)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (liveMetrics.isPaused) "PAUSED" else "TRACKING LIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (liveMetrics.isPaused) Color(0xFFFFA502) else EmeraldPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4 Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WorkoutStat(
                            value = "%.2f".format(Locale.US, liveMetrics.distanceMeters / 1000f),
                            unit = "km",
                            label = "Distance"
                        )
                        WorkoutStat(
                            value = String.format(Locale.US, "%,d", liveMetrics.currentSteps),
                            unit = "steps",
                            label = "Cadence"
                        )
                        val paceMin = liveMetrics.currentPaceMinPerKm.toInt()
                        val paceSec = ((liveMetrics.currentPaceMinPerKm - paceMin) * 60).toInt()
                        val paceStr = if (liveMetrics.currentPaceMinPerKm > 0.05f) "%d'%02d\"".format(Locale.US, paceMin, paceSec) else "--'--\""
                        WorkoutStat(
                            value = paceStr,
                            unit = "/km",
                            label = "Pace"
                        )
                        WorkoutStat(
                            value = "${liveMetrics.caloriesBurned}",
                            unit = "kcal",
                            label = "Burned"
                        )
                    }
                }
            }
        }

        // Workout Action Controls
        item {
            if (!liveMetrics.isRunning) {
                // Setup and Start Panel
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activities.forEach { act ->
                            FilterChip(
                                selected = selectedActivity == act,
                                onClick = { selectedActivity = act },
                                label = { Text(act) },
                                leadingIcon = {
                                    val icon = when (act) {
                                        "Jogging" -> Icons.AutoMirrored.Filled.DirectionsRun
                                        "Hiking" -> Icons.Default.Hiking
                                        else -> Icons.AutoMirrored.Filled.DirectionsWalk
                                    }
                                    Icon(imageVector = icon, contentDescription = act, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }

                    // Simulation mode switch for testing
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Simulate Outdoor Trail",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Generates live route polyline & strides for testing",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = simulateMovement,
                            onCheckedChange = { simulateMovement = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                        )
                    }

                    Button(
                        onClick = { onStartWorkout(selectedActivity, simulateMovement) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_workout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Start Outdoor $selectedActivity", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // In-Progress Controls (Pause / Resume / Finish)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (liveMetrics.isPaused) {
                        Button(
                            onClick = onResumeWorkout,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("resume_workout_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPauseWorkout,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("pause_workout_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onFinishWorkout,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("finish_workout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralHeart),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Finish")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Finish", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recent Workouts Section
        item {
            Text(
                text = "Recent Sessions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (recentWorkouts.isEmpty()) {
            item {
                Text(
                    text = "No recorded sessions yet. Start one above!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(recentWorkouts) { session ->
                WorkoutHistoryCard(session)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSosDialog) {
        EmergencySosDialog(
            currentGpsPoint = liveMetrics.currentPosition ?: liveMetrics.routePoints.lastOrNull(),
            onDismiss = { showSosDialog = false }
        )
    }
}

@Composable
private fun WorkoutStat(value: String, unit: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = " $unit",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkoutHistoryCard(session: WorkoutSession) {
    val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(session.startTime))
    val minutes = session.durationSeconds / 60
    val secs = session.durationSeconds % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (session.activityType) {
                        "Jogging" -> Icons.AutoMirrored.Filled.DirectionsRun
                        "Hiking" -> Icons.Default.Hiking
                        else -> Icons.AutoMirrored.Filled.DirectionsWalk
                    }
                    Icon(imageVector = icon, contentDescription = session.activityType, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = session.activityType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "%.2f km • %,d steps".format(Locale.US, session.distanceMeters / 1000f, session.totalSteps),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "%d min %02d sec • %d kcal".format(Locale.US, minutes, secs, session.caloriesBurned),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
