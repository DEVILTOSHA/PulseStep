package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blood_pressure_logs")
data class BloodPressureEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val systolic: Int,    // mmHg
    val diastolic: Int,   // mmHg
    val pulse: Int,       // bpm
    val timestamp: Long = System.currentTimeMillis(),
    val category: String, // Normal, Elevated, Stage 1, Stage 2, Crisis
    val note: String = "",
    val syncedToHealthConnect: Boolean = false
)
