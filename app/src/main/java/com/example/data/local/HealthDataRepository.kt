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

    suspend fun seedSampleDataIfEmpty() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        val todayKey = dateFormat.format(calendar.time)

        val existing = dailyStepDao.getDailyStep(todayKey)
        if (existing == null) {
            // Seed today
            dailyStepDao.insertOrUpdate(
                DailyStepRecord(
                    dateKey = todayKey,
                    stepCount = 6842,
                    distanceMeters = 5120f,
                    caloriesBurned = 345,
                    activeMinutes = 48,
                    goalSteps = 10000,
                    syncedToHealthConnect = true
                )
            )

            // Seed past 6 days
            val pastSteps = listOf(8420, 10350, 7890, 11200, 9450, 6300)
            for (i in 1..6) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val key = dateFormat.format(calendar.time)
                val steps = pastSteps[i - 1]
                val dist = steps * 0.76f
                val cals = (steps * 0.045f).toInt()
                val mins = (steps / 130)
                dailyStepDao.insertOrUpdate(
                    DailyStepRecord(
                        dateKey = key,
                        stepCount = steps,
                        distanceMeters = dist,
                        caloriesBurned = cals,
                        activeMinutes = mins,
                        goalSteps = 10000,
                        syncedToHealthConnect = true
                    )
                )
            }

            // Seed sample Blood Pressure readings
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L
            bloodPressureDao.insert(
                BloodPressureEntry(
                    systolic = 118,
                    diastolic = 76,
                    pulse = 68,
                    timestamp = now - (dayMillis * 0.2).toLong(),
                    category = "Normal",
                    note = "Morning reading, after 5 min rest",
                    syncedToHealthConnect = true
                )
            )
            bloodPressureDao.insert(
                BloodPressureEntry(
                    systolic = 124,
                    diastolic = 79,
                    pulse = 74,
                    timestamp = now - dayMillis,
                    category = "Elevated",
                    note = "Afternoon coffee check",
                    syncedToHealthConnect = true
                )
            )
            bloodPressureDao.insert(
                BloodPressureEntry(
                    systolic = 115,
                    diastolic = 74,
                    pulse = 66,
                    timestamp = now - (dayMillis * 2),
                    category = "Normal",
                    note = "Evening reading",
                    syncedToHealthConnect = true
                )
            )
            bloodPressureDao.insert(
                BloodPressureEntry(
                    systolic = 132,
                    diastolic = 84,
                    pulse = 82,
                    timestamp = now - (dayMillis * 4),
                    category = "Stage 1",
                    note = "Post-work meeting stress",
                    syncedToHealthConnect = true
                )
            )

            // Seed sample Heart Rates
            heartRateDao.insert(HeartRateEntry(bpm = 70, contextTag = "Resting", timestamp = now - 3600000L, syncedToHealthConnect = true))
            heartRateDao.insert(HeartRateEntry(bpm = 112, contextTag = "Active", timestamp = now - (dayMillis * 0.5).toLong(), syncedToHealthConnect = true))
            heartRateDao.insert(HeartRateEntry(bpm = 65, contextTag = "Resting", timestamp = now - dayMillis, syncedToHealthConnect = true))
            heartRateDao.insert(HeartRateEntry(bpm = 128, contextTag = "Post-Workout", timestamp = now - (dayMillis * 1.5).toLong(), syncedToHealthConnect = true))
            heartRateDao.insert(HeartRateEntry(bpm = 72, contextTag = "Walking", timestamp = now - (dayMillis * 2), syncedToHealthConnect = true))

            // Seed sample past workout
            workoutSessionDao.insert(
                WorkoutSession(
                    startTime = now - (dayMillis * 1.5).toLong(),
                    endTime = now - (dayMillis * 1.5).toLong() + 1800000L,
                    activityType = "Walking",
                    totalSteps = 3420,
                    distanceMeters = 2560f,
                    durationSeconds = 1800L,
                    caloriesBurned = 168,
                    avgPaceSecondsPerKm = 703,
                    routePointsJson = """[{"lat":37.7749,"lng":-122.4194},{"lat":37.7758,"lng":-122.4182},{"lat":37.7770,"lng":-122.4170},{"lat":37.7785,"lng":-122.4162}]"""
                )
            )
        }
    }
}
