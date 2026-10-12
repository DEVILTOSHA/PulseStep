package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val activityType: String, // Walking, Jogging, Hiking
    val totalSteps: Int,
    val distanceMeters: Float,
    val durationSeconds: Long,
    val caloriesBurned: Int,
    val avgPaceSecondsPerKm: Int,
    val routePointsJson: String = "" // Serialized list of coordinates
)
