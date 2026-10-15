package com.example.data.local

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HealthDataRepository(private val database: PulseStepDatabase) {
    private val dailyStepDao = database.dailyStepDao()
    private val bloodPressureDao = database.bloodPressureDao()
    private val heartRateDao = database.heartRateDao()
    private val workoutSessionDao = database.workoutSessionDao()

    val recent7DaysSteps: Flow<List<DailyStepRecord>> = dailyStepDao.getRecent7Days()
    val allBloodPressureLogs: Flow<List<BloodPressureEntry>> = bloodPressureDao.getAllBloodPressureLogs()
    val latestBloodPressure: Flow<BloodPressureEntry?> = bloodPressureDao.getLatestBloodPressure()
    val allHeartRateLogs: Flow<List<HeartRateEntry>> = heartRateDao.getAllHeartRateLogs()
    val latestHeartRate: Flow<HeartRateEntry?> = heartRateDao.getLatestHeartRate()
    val recentWorkouts: Flow<List<WorkoutSession>> = workoutSessionDao.getRecentWorkouts()

    fun observeTodaySteps(todayKey: String): Flow<DailyStepRecord?> {
        return dailyStepDao.observeDailyStep(todayKey)
    }

    suspend fun getTodaySteps(todayKey: String): DailyStepRecord? {
        return dailyStepDao.getDailyStep(todayKey)
    }

    suspend fun saveOrUpdateSteps(record: DailyStepRecord) {
        dailyStepDao.insertOrUpdate(record)
    }

    suspend fun updateStepGoal(newGoal: Int) {
        dailyStepDao.updateAllStepGoals(newGoal)
    }

    suspend fun logBloodPressure(systolic: Int, diastolic: Int, pulse: Int, category: String, note: String): Long {
        val entry = BloodPressureEntry(
            systolic = systolic,
            diastolic = diastolic,
            pulse = pulse,
            category = category,
            note = note,
            timestamp = System.currentTimeMillis()
        )
        // Also add pulse as heart rate entry
        if (pulse > 0) {
            heartRateDao.insert(
                HeartRateEntry(
                    bpm = pulse,
                    contextTag = "Resting (BP Log)",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        return bloodPressureDao.insert(entry)
    }

    suspend fun deleteBloodPressure(id: Long) {
        bloodPressureDao.deleteById(id)
    }

    suspend fun logHeartRate(bpm: Int, contextTag: String): Long {
        val entry = HeartRateEntry(
            bpm = bpm,
            contextTag = contextTag,
            timestamp = System.currentTimeMillis()
        )
        return heartRateDao.insert(entry)
    }

    suspend fun deleteHeartRate(id: Long) {
        heartRateDao.deleteById(id)
    }

    suspend fun saveWorkoutSession(session: WorkoutSession): Long {
        return workoutSessionDao.insert(session)
    }

    suspend fun markStepsSynced(dateKey: String) {
        dailyStepDao.updateSyncStatus(dateKey, true)
    }

    suspend fun clearAllData() {
        dailyStepDao.clearAllDailySteps()
        bloodPressureDao.clearAllBloodPressureLogs()
        heartRateDao.clearAllHeartRateLogs()
        workoutSessionDao.clearAllWorkouts()
    }

    suspend fun initFreshStateIfEmpty() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayKey = dateFormat.format(Date())

        val existing = dailyStepDao.getDailyStep(todayKey)
        if (existing == null) {
            // Clean slate start: 0 steps, pristine metrics
            dailyStepDao.insertOrUpdate(
                DailyStepRecord(
                    dateKey = todayKey,
                    stepCount = 0,
                    distanceMeters = 0f,
                    caloriesBurned = 0,
                    activeMinutes = 0,
                    goalSteps = 10000,
                    syncedToHealthConnect = false
                )
            )
        }
    }
}
