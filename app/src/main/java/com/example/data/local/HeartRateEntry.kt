package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heart_rate_logs")
data class HeartRateEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bpm: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val contextTag: String = "Resting", // Resting, Active, Post-Workout, Walking
    val syncedToHealthConnect: Boolean = false
)
