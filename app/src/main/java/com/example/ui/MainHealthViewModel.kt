package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BloodPressureEntry
import com.example.data.local.DailyStepRecord
import com.example.data.local.DateKeys
import com.example.data.local.HealthDataRepository
import com.example.data.local.HeartRateEntry
import com.example.data.local.PulseStepDatabase
import com.example.data.local.StepRecorder
import com.example.data.local.WorkoutSession
import com.example.healthconnect.HealthConnectManager
import com.example.healthconnect.HealthConnectStatus
import com.example.insights.ClinicalInsightsEngine
import com.example.insights.HealthInsightReport
import com.example.location.WorkoutLocationTracker
import com.example.location.WorkoutLiveMetrics
import com.example.sensor.StepTrackingService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

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

@OptIn(ExperimentalCoroutinesApi::class)
class MainHealthViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PulseStepDatabase.getDatabase(application)
    private val repository = HealthDataRepository(database)
    private val stepRecorder = StepRecorder.getInstance(application)
    val healthConnectManager = HealthConnectManager(application)
    val workoutTracker = WorkoutLocationTracker(application)
    private val prefs = application.getSharedPreferences(StepRecorder.PREFS_NAME, android.content.Context.MODE_PRIVATE)

    // Re-checked every second so the "today" record rolls over at local midnight.
    private val _todayKey = MutableStateFlow(DateKeys.today())

    private val _currentTab = MutableStateFlow(NavigationTab.DASHBOARD)
    private val _healthReport = MutableStateFlow<HealthInsightReport?>(null)
    private val _isLoadingInsights = MutableStateFlow(false)
    private val _isSensorActive = MutableStateFlow(false)
    private val _userWeightKg = MutableStateFlow(prefs.getInt(StepRecorder.KEY_WEIGHT_KG, StepRecorder.DEFAULT_WEIGHT_KG))
    private val _userStrideCm = MutableStateFlow(prefs.getInt(StepRecorder.KEY_STRIDE_CM, StepRecorder.DEFAULT_STRIDE_CM))

    val uiState: StateFlow<HealthUiState> = combine(
        combine(
            _currentTab,
            _todayKey.flatMapLatest { repository.observeTodaySteps(it) },
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
        viewModelScope.launch {
            if (!prefs.getBoolean("clean_slate_v2_initialized", false)) {
                repository.clearAllData()
                prefs.edit().putBoolean("clean_slate_v2_initialized", true).apply()
            }
            stepRecorder.ensureTodayRecord()
            healthConnectManager.checkAvailabilityAndPermissions()
            startSensorTracking()
            refreshHealthInsights()
        }

        // Steps counted by the background service also feed a live workout
        viewModelScope.launch {
            stepRecorder.stepDeltas.collect { delta -> workoutTracker.addStepDelta(delta) }
        }

        // Workout second ticker loop + local-midnight rollover
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                workoutTracker.onTickSecond()
                val today = DateKeys.today()
                if (today != _todayKey.value) {
                    stepRecorder.ensureTodayRecord()
                    _todayKey.value = today
                }
            }
        }
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    /** Starts the background step service. No-op until activity-recognition permission is granted. */
    fun startSensorTracking() {
        _isSensorActive.value = StepTrackingService.start(getApplication())
    }

    fun stopSensorTracking() {
        StepTrackingService.stop(getApplication())
        _isSensorActive.value = false
    }

    fun updateProfile(weightKg: Int, strideLengthCm: Int) {
        viewModelScope.launch {
            prefs.edit()
                .putInt(StepRecorder.KEY_WEIGHT_KG, weightKg)
                .putInt(StepRecorder.KEY_STRIDE_CM, strideLengthCm)
                .apply()
            _userWeightKg.value = weightKg
            _userStrideCm.value = strideLengthCm
            stepRecorder.recalculateToday()
        }
    }

    fun simulateStepIncrement(steps: Int = 100) {
        viewModelScope.launch { stepRecorder.recordSteps(steps) }
    }

    fun startWorkout(activityType: String, simulateMovement: Boolean = false, useGps: Boolean = true) {
        workoutTracker.startWorkout(
            activityType = activityType,
            simulateMovement = simulateMovement,
            useGps = useGps,
            userStrideMeters = _userStrideCm.value / 100f
        )
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
            val id = repository.logBloodPressure(systolic, diastolic, pulse, category, note)
            // Attempt Health Connect sync
            if (healthConnectManager.syncBloodPressureRecord(systolic.toDouble(), diastolic.toDouble(), Instant.now())) {
                repository.markBloodPressureSynced(id)
            }
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
            val id = repository.logHeartRate(bpm, context)
            if (healthConnectManager.syncHeartRateRecord(bpm.toLong(), Instant.now())) {
                repository.markHeartRateSynced(id)
            }
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
        viewModelScope.launch { stepRecorder.updateStepGoal(newGoal) }
    }

    fun triggerHealthConnectSync() {
        viewModelScope.launch {
            // Re-check first: the status shown must reflect what the user actually granted.
            if (healthConnectManager.checkAvailabilityAndPermissions() != HealthConnectStatus.CONNECTED) return@launch
            val todayKey = DateKeys.today()
            val today = repository.getTodaySteps(todayKey) ?: return@launch
            val success = healthConnectManager.syncDailySteps(
                stepCount = today.stepCount.toLong(),
                dayStart = DateKeys.startOfDay(todayKey),
                dayEnd = DateKeys.startOfNextDay(todayKey),
                end = Instant.now()
            )
            if (success) {
                repository.markStepsSynced(todayKey)
            }
        }
    }

    fun refreshHealthInsights() {
        viewModelScope.launch {
            _isLoadingInsights.value = true
            // Read straight from the database: uiState may still hold its empty initial value.
            val steps = repository.recent7DaysSteps.first()
            val bps = repository.allBloodPressureLogs.first()
            val hrs = repository.allHeartRateLogs.first()

            val report = ClinicalInsightsEngine.generateHealthReport(steps, bps, hrs)
            _healthReport.value = report
            _isLoadingInsights.value = false
        }
    }

    fun resetAllDataToCleanSlate() {
        viewModelScope.launch {
            repository.clearAllData()
            stepRecorder.ensureTodayRecord()
            workoutTracker.stopWorkout()
            refreshHealthInsights()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // The step service intentionally keeps running after the UI goes away.
        workoutTracker.stopWorkout()
    }
}
