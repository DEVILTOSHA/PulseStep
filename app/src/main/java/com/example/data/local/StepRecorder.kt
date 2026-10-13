package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single writer for daily step totals. Shared by the foreground [com.example.sensor.StepTrackingService]
 * and the UI, so every read-modify-write of today's record is serialized behind one mutex.
 */
class StepRecorder private constructor(context: Context) {

    private val repository = HealthDataRepository(PulseStepDatabase.getDatabase(context))
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    private val _stepDeltas = MutableSharedFlow<Int>(extraBufferCapacity = 64)
    /** Every recorded step delta, so a live workout can follow along. */
    val stepDeltas: SharedFlow<Int> = _stepDeltas.asSharedFlow()

    /** Adds [stepDelta] steps to today's record and returns today's new total. */
    suspend fun recordSteps(stepDelta: Int): Int {
        val newSteps = mutex.withLock {
            val todayKey = DateKeys.today()
            val existing = repository.getTodaySteps(todayKey)
            val newSteps = (existing?.stepCount ?: 0) + stepDelta
            val strideMeters = prefs.getInt(KEY_STRIDE_CM, DEFAULT_STRIDE_CM) / 100f
            val calorieWeightFactor = (prefs.getInt(KEY_WEIGHT_KG, DEFAULT_WEIGHT_KG) / 70f).coerceIn(0.5f, 3.0f)

            repository.saveOrUpdateSteps(
                DailyStepRecord(
                    dateKey = todayKey,
                    stepCount = newSteps,
                    distanceMeters = newSteps * strideMeters,
                    caloriesBurned = (newSteps * 0.045f * calorieWeightFactor).toInt(),
                    activeMinutes = newSteps / 130,
                    goalSteps = existing?.goalSteps ?: prefs.getInt(KEY_STEP_GOAL, DEFAULT_STEP_GOAL),
                    syncedToHealthConnect = false
                )
            )
            newSteps
        }
        _stepDeltas.emit(stepDelta)
        return newSteps
    }

    /** Recomputes today's distance and calories after a profile change. */
    suspend fun recalculateToday() {
        mutex.withLock {
            val existing = repository.getTodaySteps(DateKeys.today()) ?: return
            val strideMeters = prefs.getInt(KEY_STRIDE_CM, DEFAULT_STRIDE_CM) / 100f
            val calorieWeightFactor = (prefs.getInt(KEY_WEIGHT_KG, DEFAULT_WEIGHT_KG) / 70f).coerceIn(0.5f, 3.0f)
            repository.saveOrUpdateSteps(
                existing.copy(
                    distanceMeters = existing.stepCount * strideMeters,
                    caloriesBurned = (existing.stepCount * 0.045f * calorieWeightFactor).toInt()
                )
            )
        }
    }

    /** Persists a new goal for today and every future day; past days keep the goal they had. */
    suspend fun updateStepGoal(newGoal: Int) {
        mutex.withLock {
            prefs.edit().putInt(KEY_STEP_GOAL, newGoal).apply()
            repository.updateStepGoal(DateKeys.today(), newGoal)
        }
    }

    suspend fun ensureTodayRecord() {
        mutex.withLock {
            repository.initFreshStateIfEmpty(DateKeys.today(), prefs.getInt(KEY_STEP_GOAL, DEFAULT_STEP_GOAL))
        }
    }

    companion object {
        const val PREFS_NAME = "pulse_step_prefs"
        const val KEY_WEIGHT_KG = "user_weight_kg"
        const val KEY_STRIDE_CM = "user_stride_cm"
        const val KEY_STEP_GOAL = "step_goal"
        const val DEFAULT_WEIGHT_KG = 70
        const val DEFAULT_STRIDE_CM = 76
        const val DEFAULT_STEP_GOAL = 10000

        @Volatile
        private var INSTANCE: StepRecorder? = null

        fun getInstance(context: Context): StepRecorder {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepRecorder(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
