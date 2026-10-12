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
    val isSensorActive: Boolean = false
)

class MainHealthViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PulseStepDatabase.getDatabase(application)
    private val repository = HealthDataRepository(database)
    val healthConnectManager = HealthConnectManager(application)
    val workoutTracker = WorkoutLocationTracker(application)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayKey: String = dateFormat.format(Date())

    private val _currentTab = MutableStateFlow(NavigationTab.DASHBOARD)
    private val _healthReport = MutableStateFlow<HealthInsightReport?>(null)
    private val _isLoadingInsights = MutableStateFlow(false)
    private val _isSensorActive = MutableStateFlow(false)

    lateinit var sensorManager: PedometerSensorManager
        private set

    val uiState: StateFlow<HealthUiState> = combine(
        _currentTab,
        repository.observeTodaySteps(todayKey),
        repository.recent7DaysSteps,
        repository.latestBloodPressure,
        repository.allBloodPressureLogs,
        repository.latestHeartRate,
        repository.allHeartRateLogs,
        repository.recentWorkouts,
        workoutTracker.liveMetrics,
        healthConnectManager.syncStatus,
        healthConnectManager.lastSyncMessage,
        _healthReport,
        _isLoadingInsights,
        _isSensorActive
    ) { params ->
        val currentTab = params[0] as NavigationTab
        val todaySteps = params[1] as? DailyStepRecord
        val recent7Days = (params[2] as? List<DailyStepRecord>) ?: emptyList()
        val latestBp = params[3] as? BloodPressureEntry
        val allBpLogs = (params[4] as? List<BloodPressureEntry>) ?: emptyList()
        val latestHr = params[5] as? HeartRateEntry
        val allHrLogs = (params[6] as? List<HeartRateEntry>) ?: emptyList()
        val recentWorkouts = (params[7] as? List<WorkoutSession>) ?: emptyList()
        val liveWorkout = (params[8] as? WorkoutLiveMetrics) ?: WorkoutLiveMetrics()
        val healthConnectStatus = (params[9] as? HealthConnectStatus) ?: HealthConnectStatus.NEEDS_PERMISSIONS
        val healthConnectMessage = params[10] as? String
        val healthReport = params[11] as? HealthInsightReport
        val isLoadingInsights = (params[12] as? Boolean) ?: false
        val isSensorActive = (params[13] as? Boolean) ?: false

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
            isSensorActive = isSensorActive
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
            repository.seedSampleDataIfEmpty()
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
            val newDist = newSteps * 0.76f
            val newCals = (newSteps * 0.045f).toInt()
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

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
        workoutTracker.stopWorkout()
    }
}
