package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.export.HealthReportExporter
import com.example.ui.MainHealthViewModel
import com.example.ui.NavigationTab
import com.example.ui.components.CameraPpgScannerSheet
import com.example.ui.components.EditGoalsDialog
import com.example.ui.components.LogBloodPressureDialog
import com.example.ui.components.LogHeartRateDialog
import com.example.ui.components.ProfileSettingsDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.VitalsScreen
import com.example.ui.screens.WorkoutScreen
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PulseStepApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseStepApp(viewModel: MainHealthViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showLogBpDialog by remember { mutableStateOf(false) }
    var showLogHrDialog by remember { mutableStateOf(false) }
    var showEditGoalsDialog by remember { mutableStateOf(false) }
    var showPpgScanner by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var initialPulseForBp by remember { mutableIntStateOf(72) }

    // Camera Runtime Permission Launcher for Optical PPG
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        showPpgScanner = true
    }

    // Android Sensor and Location Runtime Permissions
    val sensorPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true) {
            viewModel.startSensorTracking()
        }
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.workoutTracker.fetchImmediateLastKnownLocation()
            viewModel.workoutTracker.startLocationUpdates()
        }
    }

    // Health Connect Permission Launcher
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.triggerHealthConnectSync()
    }

    LaunchedEffect(Unit) {
        val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.workoutTracker.fetchImmediateLastKnownLocation()
            viewModel.workoutTracker.startLocationUpdates()
        }

        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        sensorPermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    // BackHandler: return to Dashboard if on a sub-screen
    BackHandler(enabled = uiState.currentTab != NavigationTab.DASHBOARD) {
        viewModel.selectTab(NavigationTab.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PulseStep",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    // Sync action button
                    IconButton(
                        onClick = {
                            try {
                                healthConnectPermissionLauncher.launch(viewModel.healthConnectManager.permissions)
                            } catch (_: Exception) {
                                viewModel.triggerHealthConnectSync()
                            }
                        },
                        modifier = Modifier.testTag("top_bar_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Health Connect",
                            tint = EmeraldPrimary
                        )
                    }

                    // Quick Optical Camera PPG Pulse Scan
                    IconButton(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.testTag("top_bar_ppg_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Scan Pulse with Camera",
                            tint = CoralHeart
                        )
                    }

                    // Doctor Health Report Share Action
                    IconButton(
                        onClick = {
                            HealthReportExporter.shareReport(
                                context,
                                uiState.recent7Days,
                                uiState.allBpLogs,
                                uiState.allHrLogs
                            )
                        },
                        modifier = Modifier.testTag("top_bar_share_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Health Report",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Profile & Settings
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.testTag("top_bar_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = uiState.currentTab == tab
                    val (icon, testTag) = when (tab) {
                        NavigationTab.DASHBOARD -> Icons.AutoMirrored.Filled.DirectionsWalk to "nav_tab_today"
                        NavigationTab.WORKOUT -> Icons.AutoMirrored.Filled.DirectionsRun to "nav_tab_workout"
                        NavigationTab.VITALS -> Icons.Default.MonitorHeart to "nav_tab_vitals"
                        NavigationTab.ANALYTICS -> Icons.Default.BarChart to "nav_tab_analytics"
                        NavigationTab.INSIGHTS -> Icons.Default.AutoAwesome to "nav_tab_insights"
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag(testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                NavigationTab.DASHBOARD -> {
                    DashboardScreen(
                        state = uiState,
                        onNavigateTab = { viewModel.selectTab(it) },
                        onSimulateStep = { viewModel.simulateStepIncrement(100) },
                        onTriggerSync = {
                            try {
                                healthConnectPermissionLauncher.launch(viewModel.healthConnectManager.permissions)
                            } catch (_: Exception) {
                                viewModel.triggerHealthConnectSync()
                            }
                        },
                        onOpenLogBp = { showLogBpDialog = true }
                    )
                }
                NavigationTab.WORKOUT -> {
                    WorkoutScreen(
                        liveMetrics = uiState.liveWorkout,
                        recentWorkouts = uiState.recentWorkouts,
                        onStartWorkout = { act, sim -> viewModel.startWorkout(act, sim) },
                        onPauseWorkout = { viewModel.pauseWorkout() },
                        onResumeWorkout = { viewModel.resumeWorkout() },
                        onFinishWorkout = { viewModel.finishWorkout() },
                        onRefreshLocation = {
                            viewModel.workoutTracker.fetchImmediateLastKnownLocation()
                            viewModel.workoutTracker.startLocationUpdates()
                        }
                    )
                }
                NavigationTab.VITALS -> {
                    VitalsScreen(
                        latestBp = uiState.latestBp,
                        allBpLogs = uiState.allBpLogs,
                        latestHr = uiState.latestHr,
                        allHrLogs = uiState.allHrLogs,
                        onOpenLogBpDialog = {
                            initialPulseForBp = 72
                            showLogBpDialog = true
                        },
                        onDeleteBpEntry = { viewModel.deleteBloodPressure(it) },
                        onOpenLogHrDialog = { showLogHrDialog = true },
                        onDeleteHrEntry = { viewModel.deleteHeartRate(it) },
                        onOpenPpgScanner = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }
                NavigationTab.ANALYTICS -> {
                    AnalyticsScreen(
                        recent7Days = uiState.recent7Days,
                        currentGoal = uiState.todaySteps?.goalSteps ?: 10000,
                        onOpenEditGoalsDialog = { showEditGoalsDialog = true }
                    )
                }
                NavigationTab.INSIGHTS -> {
                    InsightsScreen(
                        report = uiState.healthReport,
                        isLoading = uiState.isLoadingInsights,
                        onRefreshInsights = { viewModel.refreshHealthInsights() }
                    )
                }
            }
        }
    }

    // Dialogs & Scanner Sheets
    if (showPpgScanner) {
        CameraPpgScannerSheet(
            onDismiss = { showPpgScanner = false },
            onSaveHeartRate = { bpm ->
                viewModel.logHeartRate(bpm, "Optical PPG Camera")
                showPpgScanner = false
            },
            onLogBpWithPulse = { pulseBpm ->
                initialPulseForBp = pulseBpm
                showPpgScanner = false
                showLogBpDialog = true
            }
        )
    }

    if (showLogBpDialog) {
        LogBloodPressureDialog(
            initialPulse = initialPulseForBp,
            onDismiss = { showLogBpDialog = false },
            onConfirm = { sys, dia, pulse, note ->
                viewModel.logBloodPressure(sys, dia, pulse, note)
                showLogBpDialog = false
            }
        )
    }

    if (showLogHrDialog) {
        LogHeartRateDialog(
            onDismiss = { showLogHrDialog = false },
            onConfirm = { bpm, context ->
                viewModel.logHeartRate(bpm, context)
                showLogHrDialog = false
            }
        )
    }

    if (showEditGoalsDialog) {
        EditGoalsDialog(
            currentGoal = uiState.todaySteps?.goalSteps ?: 10000,
            onDismiss = { showEditGoalsDialog = false },
            onSaveGoal = { newGoal ->
                viewModel.updateStepGoal(newGoal)
                showEditGoalsDialog = false
            }
        )
    }

    if (showProfileDialog) {
        ProfileSettingsDialog(
            currentWeightKg = uiState.userWeightKg,
            currentStrideLengthCm = uiState.userStrideCm,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { weightKg, strideLengthCm ->
                viewModel.updateProfile(weightKg, strideLengthCm)
                showProfileDialog = false
            },
            onResetCleanSlate = {
                viewModel.resetAllDataToCleanSlate()
            }
        )
    }
}
