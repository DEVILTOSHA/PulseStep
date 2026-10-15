package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyStepRecord::class,
        BloodPressureEntry::class,
        HeartRateEntry::class,
        WorkoutSession::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PulseStepDatabase : RoomDatabase() {
    abstract fun dailyStepDao(): DailyStepDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

    companion object {
        @Volatile
        private var INSTANCE: PulseStepDatabase? = null

        fun getDatabase(context: Context): PulseStepDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PulseStepDatabase::class.java,
                    "pulsestep_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
