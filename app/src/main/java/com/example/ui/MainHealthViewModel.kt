package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BloodPressureEntry
import com.example.data.local.DailyStepRecord
import com.example.data.local.HealthDataRepository
import com.example.data.local.HeartRateEntry
import com.example.data.local.PulseStepDatabase
import com.example.data.local.WorkoutSession
import com.example.healthconnect.HealthConnectManager
import com.example.healthconnect.HealthConnectStatus
import com.example.insights.ClinicalInsightsEngine
import com.example.insights.HealthInsightReport
import com.example.location.WorkoutLocationTracker
import com.example.location.WorkoutLiveMetrics
import com.example.sensor.PedometerSensorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class NavigationTab(val label: String) {
    DASHBOARD("Today"),
    WORKOUT("Workout"),
    VITALS("Vitals"),
    ANALYTICS("Analytics"),
    INSIGHTS("AI Insights")
}

data class HealthUiState(
    val currentTab: NavigationTab = NavigationTab.DASHBOARD,
    val todaySteps: DailyStepRecord? = null,
    val recent7Days: List<DailyStepRecord> = emptyList(),
    val latestBp: BloodPressureEntry? = null,
    val allBpLogs: List<BloodPressureEntry> = emptyList(),
    val latestHr: HeartRateEntry? = null,
    val allHrLogs: List<HeartRateEntry> = emptyList(),
    val recentWorkouts: List<WorkoutSession> = emptyList(),
    val liveWorkout: WorkoutLiveMetrics = WorkoutLiveMetrics(),
    val healthConnectStatus: HealthConnectStatus = HealthConnectStatus.NEEDS_PERMISSIONS,
    val healthConnectMessage: String? = null,
    val healthReport: HealthInsightReport? = null,
    val isLoadingInsights: Boolean = false,
    val isSensorActive: Boolean = false,
    val userWeightKg: Int = 70,
    val userStrideCm: Int = 76
)

class MainHealthViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PulseStepDatabase.getDatabase(application)
    private val repository = HealthDataRepository(database)
    val healthConnectManager = HealthConnectManager(application)
    val workoutTracker = WorkoutLocationTracker(application)
    private val prefs = application.getSharedPreferences("pulse_step_prefs", android.content.Context.MODE_PRIVATE)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayKey: String = dateFormat.format(Date())

    private val _currentTab = MutableStateFlow(NavigationTab.DASHBOARD)
    private val _healthReport = MutableStateFlow<HealthInsightReport?>(null)
    private val _isLoadingInsights = MutableStateFlow(false)
    private val _isSensorActive = MutableStateFlow(false)
    private val _userWeightKg = MutableStateFlow(prefs.getInt("user_weight_kg", 70))
    private val _userStrideCm = MutableStateFlow(prefs.getInt("user_stride_cm", 76))

    lateinit var sensorManager: PedometerSensorManager
        private set

    val uiState: StateFlow<HealthUiState> = combine(
        combine(
            _currentTab,
            repository.observeTodaySteps(todayKey),
            repository.recent7DaysSteps,
            repository.latestBloodPressure,
            repository.allBloodPressureLogs
        ) { tab, todaySteps, recent7Days, latestBp, allBpLogs ->
            listOf(tab, todaySteps, recent7Days, latestBp, allBpLogs)
        },
        combine(
            repository.latestHeartRate,
            repository.allHeartRateLogs,
            repository.recentWorkouts,
            workoutTracker.liveMetrics,
            healthConnectManager.syncStatus
        ) { latestHr, allHrLogs, recentWorkouts, liveWorkout, healthConnectStatus ->
            listOf(latestHr, allHrLogs, recentWorkouts, liveWorkout, healthConnectStatus)
        },
        combine(
            healthConnectManager.lastSyncMessage,
            _healthReport,
            _isLoadingInsights,
            _isSensorActive,
            _userWeightKg
        ) { healthConnectMessage, healthReport, isLoadingInsights, isSensorActive, userWeightKg ->
            listOf(healthConnectMessage, healthReport, isLoadingInsights, isSensorActive, userWeightKg)
        },
        _userStrideCm
    ) { p1, p2, p3, userStrideCm ->
        val currentTab = p1[0] as NavigationTab
        val todaySteps = p1[1] as? DailyStepRecord
        val recent7Days = (p1[2] as? List<DailyStepRecord>) ?: emptyList()
        val latestBp = p1[3] as? BloodPressureEntry
        val allBpLogs = (p1[4] as? List<BloodPressureEntry>) ?: emptyList()

        val latestHr = p2[0] as? HeartRateEntry
        val allHrLogs = (p2[1] as? List<HeartRateEntry>) ?: emptyList()
        val recentWorkouts = (p2[2] as? List<WorkoutSession>) ?: emptyList()
        val liveWorkout = (p2[3] as? WorkoutLiveMetrics) ?: WorkoutLiveMetrics()
        val healthConnectStatus = (p2[4] as? HealthConnectStatus) ?: HealthConnectStatus.NEEDS_PERMISSIONS

        val healthConnectMessage = p3[0] as? String
        val healthReport = p3[1] as? HealthInsightReport
        val isLoadingInsights = (p3[2] as? Boolean) ?: false
        val isSensorActive = (p3[3] as? Boolean) ?: false
        val userWeightKg = (p3[4] as? Int) ?: 70

        HealthUiState(
            currentTab = currentTab,
            todaySteps = todaySteps,
            recent7Days = recent7Days,
            latestBp = latestBp,
            allBpLogs = allBpLogs,
            latestHr = latestHr,
            allHrLogs = allHrLogs,
            recentWorkouts = recentWorkouts,
            liveWorkout = liveWorkout,
            healthConnectStatus = healthConnectStatus,
            healthConnectMessage = healthConnectMessage,
            healthReport = healthReport,
            isLoadingInsights = isLoadingInsights,
            isSensorActive = isSensorActive,
            userWeightKg = userWeightKg,
            userStrideCm = userStrideCm
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HealthUiState()
    )

    init {
        // Initialize sensor manager
        sensorManager = PedometerSensorManager(application) { stepDelta ->
            onStepDetected(stepDelta)
        }

        viewModelScope.launch {
            if (!prefs.getBoolean("clean_slate_v2_initialized", false)) {
                repository.clearAllData()
                prefs.edit().putBoolean("clean_slate_v2_initialized", true).apply()
            }
            repository.initFreshStateIfEmpty()
            healthConnectManager.checkAvailabilityAndPermissions()
            startSensorTracking()
            refreshHealthInsights()
        }

        // Workout second ticker loop
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                workoutTracker.onTickSecond()
            }
        }
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun startSensorTracking() {
        sensorManager.startListening()
        _isSensorActive.value = true
    }

    fun stopSensorTracking() {
        sensorManager.stopListening()
        _isSensorActive.value = false
    }

    private fun onStepDetected(stepDelta: Int) {
        viewModelScope.launch {
            val existing = repository.getTodaySteps(todayKey)
            val currentSteps = existing?.stepCount ?: 0
            val goal = existing?.goalSteps ?: 10000
            val newSteps = currentSteps + stepDelta
            val strideMeters = _userStrideCm.value / 100f
            val newDist = newSteps * strideMeters
            val calorieWeightFactor = (_userWeightKg.value / 70f).coerceIn(0.5f, 3.0f)
            val newCals = (newSteps * 0.045f * calorieWeightFactor).toInt()
            val newMins = newSteps / 130

            val updatedRecord = DailyStepRecord(
                dateKey = todayKey,
                stepCount = newSteps,
                distanceMeters = newDist,
                caloriesBurned = newCals,
                activeMinutes = newMins,
                goalSteps = goal,
                syncedToHealthConnect = false
            )
            repository.saveOrUpdateSteps(updatedRecord)

            // Also feed into live workout if active
            workoutTracker.addStepDelta(stepDelta)
        }
    }

    fun updateProfile(weightKg: Int, strideLengthCm: Int) {
        viewModelScope.launch {
            prefs.edit()
                .putInt("user_weight_kg", weightKg)
                .putInt("user_stride_cm", strideLengthCm)
                .apply()
            _userWeightKg.value = weightKg
            _userStrideCm.value = strideLengthCm

            // Recalculate today's distance & calories if today's record exists
            val existing = repository.getTodaySteps(todayKey)
            if (existing != null) {
                val strideMeters = strideLengthCm / 100f
                val newDist = existing.stepCount * strideMeters
                val calorieWeightFactor = (weightKg / 70f).coerceIn(0.5f, 3.0f)
                val newCals = (existing.stepCount * 0.045f * calorieWeightFactor).toInt()
                repository.saveOrUpdateSteps(
                    existing.copy(
                        distanceMeters = newDist,
                        caloriesBurned = newCals
                    )
                )
            }
        }
    }

    fun simulateStepIncrement(steps: Int = 100) {
        sensorManager.injectSimulatedSteps(steps)
    }

    fun startWorkout(activityType: String, simulateMovement: Boolean = false) {
        workoutTracker.startWorkout(activityType, simulateMovement)
    }

    fun pauseWorkout() {
        workoutTracker.pauseWorkout()
    }

    fun resumeWorkout() {
        workoutTracker.resumeWorkout()
    }

    fun finishWorkout() {
        val current = workoutTracker.liveMetrics.value
        if (!current.isRunning) return

        viewModelScope.launch {
            val session = WorkoutSession(
                startTime = System.currentTimeMillis() - (current.elapsedTimeSeconds * 1000L),
                endTime = System.currentTimeMillis(),
                activityType = current.activityType,
                totalSteps = current.currentSteps,
                distanceMeters = current.distanceMeters,
                durationSeconds = current.elapsedTimeSeconds,
                caloriesBurned = current.caloriesBurned,
                avgPaceSecondsPerKm = (current.currentPaceMinPerKm * 60f).toInt(),
                routePointsJson = "[${current.routePoints.joinToString(",") { "{\"lat\":${it.latitude},\"lng\":${it.longitude}}" }}]"
            )
            repository.saveWorkoutSession(session)
            workoutTracker.stopWorkout()
        }
    }

    fun logBloodPressure(systolic: Int, diastolic: Int, pulse: Int, note: String) {
        viewModelScope.launch {
            val category = ClinicalInsightsEngine.classifyBloodPressure(systolic, diastolic).label
            repository.logBloodPressure(systolic, diastolic, pulse, category, note)
            // Attempt Health Connect sync
            healthConnectManager.syncBloodPressureRecord(systolic.toDouble(), diastolic.toDouble(), Instant.now())
            refreshHealthInsights()
        }
    }

    fun deleteBloodPressure(id: Long) {
        viewModelScope.launch {
            repository.deleteBloodPressure(id)
            refreshHealthInsights()
        }
    }

    fun logHeartRate(bpm: Int, context: String) {
        viewModelScope.launch {
            repository.logHeartRate(bpm, context)
            healthConnectManager.syncHeartRateRecord(bpm.toLong(), Instant.now())
            refreshHealthInsights()
        }
    }

    fun deleteHeartRate(id: Long) {
        viewModelScope.launch {
            repository.deleteHeartRate(id)
            refreshHealthInsights()
        }
    }

    fun updateStepGoal(newGoal: Int) {
        viewModelScope.launch {
            repository.updateStepGoal(newGoal)
            val today = repository.getTodaySteps(todayKey)
            if (today != null) {
                repository.saveOrUpdateSteps(today.copy(goalSteps = newGoal))
            }
        }
    }

    fun triggerHealthConnectSync() {
        viewModelScope.launch {
            val today = repository.getTodaySteps(todayKey)
            if (today != null) {
                val now = Instant.now()
                val startOfDay = now.minusSeconds(86400)
                val success = healthConnectManager.syncStepsRecord(today.stepCount.toLong(), startOfDay, now)
                if (success) {
                    repository.markStepsSynced(todayKey)
                }
            }
            healthConnectManager.markSimulatedSyncSuccess()
        }
    }

    fun refreshHealthInsights() {
        viewModelScope.launch {
            _isLoadingInsights.value = true
            val steps = uiState.value.recent7Days
            val bps = uiState.value.allBpLogs
            val hrs = uiState.value.allHrLogs

            val report = ClinicalInsightsEngine.generateHealthReport(steps, bps, hrs)
            _healthReport.value = report
            _isLoadingInsights.value = false
        }
    }

    fun resetAllDataToCleanSlate() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.initFreshStateIfEmpty()
            workoutTracker.stopWorkout()
            refreshHealthInsights()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
        workoutTracker.stopWorkout()
    }
}
