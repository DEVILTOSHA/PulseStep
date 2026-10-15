package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepDao {
    @Query("SELECT * FROM daily_steps ORDER BY dateKey DESC")
    fun getAllDailySteps(): Flow<List<DailyStepRecord>>

    @Query("SELECT * FROM daily_steps WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailyStep(dateKey: String): DailyStepRecord?

    @Query("SELECT * FROM daily_steps WHERE dateKey = :dateKey LIMIT 1")
    fun observeDailyStep(dateKey: String): Flow<DailyStepRecord?>

    @Query("SELECT * FROM daily_steps ORDER BY dateKey DESC LIMIT 7")
    fun getRecent7Days(): Flow<List<DailyStepRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DailyStepRecord)

    @Query("UPDATE daily_steps SET syncedToHealthConnect = :synced WHERE dateKey = :dateKey")
    suspend fun updateSyncStatus(dateKey: String, synced: Boolean)

    @Query("UPDATE daily_steps SET goalSteps = :newGoal")
    suspend fun updateAllStepGoals(newGoal: Int)

    @Query("DELETE FROM daily_steps")
    suspend fun clearAllDailySteps()
}

@Dao
interface BloodPressureDao {
    @Query("SELECT * FROM blood_pressure_logs ORDER BY timestamp DESC")
    fun getAllBloodPressureLogs(): Flow<List<BloodPressureEntry>>

    @Query("SELECT * FROM blood_pressure_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBloodPressure(): Flow<BloodPressureEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BloodPressureEntry): Long

    @Query("DELETE FROM blood_pressure_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE blood_pressure_logs SET syncedToHealthConnect = :synced WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, synced: Boolean)

    @Query("DELETE FROM blood_pressure_logs")
    suspend fun clearAllBloodPressureLogs()
}

@Dao
interface HeartRateDao {
    @Query("SELECT * FROM heart_rate_logs ORDER BY timestamp DESC")
    fun getAllHeartRateLogs(): Flow<List<HeartRateEntry>>

    @Query("SELECT * FROM heart_rate_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestHeartRate(): Flow<HeartRateEntry?>

    @Query("SELECT * FROM heart_rate_logs ORDER BY timestamp DESC LIMIT 20")
    fun getRecentHeartRates(): Flow<List<HeartRateEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HeartRateEntry): Long

    @Query("DELETE FROM heart_rate_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE heart_rate_logs SET syncedToHealthConnect = :synced WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, synced: Boolean)

    @Query("DELETE FROM heart_rate_logs")
    suspend fun clearAllHeartRateLogs()
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllWorkouts(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC LIMIT 5")
    fun getRecentWorkouts(): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession): Long

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workout_sessions")
    suspend fun clearAllWorkouts()
}
