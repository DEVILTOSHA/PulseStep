package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class DailyStepRecord(
    @PrimaryKey
    val dateKey: String, // Format: "yyyy-MM-dd"
    val stepCount: Int,
    val distanceMeters: Float,
    val caloriesBurned: Int,
    val activeMinutes: Int,
    val goalSteps: Int = 10000,
    val syncedToHealthConnect: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
