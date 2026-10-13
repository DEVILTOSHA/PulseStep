package com.example.export

import android.content.Context
import android.content.Intent
import com.example.data.local.BloodPressureEntry
import com.example.data.local.DailyStepRecord
import com.example.data.local.HeartRateEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HealthReportExporter {

    fun generateTextReport(
        dailySteps: List<DailyStepRecord>,
        bpLogs: List<BloodPressureEntry>,
        hrLogs: List<HeartRateEntry>,
        userName: String = "PulseStep User"
    ): String {
        val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
        val shortDate = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val todayStr = shortDate.format(Date())

        val totalSteps = dailySteps.sumOf { it.stepCount }
        val avgSteps = if (dailySteps.isNotEmpty()) totalSteps / dailySteps.size else 0
        val totalDistKm = dailySteps.sumOf { it.distanceMeters.toDouble() } / 1000.0

        val avgSys = if (bpLogs.isNotEmpty()) bpLogs.map { it.systolic }.average().toInt() else 0
        val avgDia = if (bpLogs.isNotEmpty()) bpLogs.map { it.diastolic }.average().toInt() else 0
        val avgPulse = if (bpLogs.isNotEmpty()) bpLogs.map { it.pulse }.average().toInt() else 0
        val avgHr = if (hrLogs.isNotEmpty()) hrLogs.map { it.bpm }.average().toInt() else 0

        val builder = StringBuilder()
        builder.appendLine("========================================")
        builder.appendLine("   PULSESTEP HEALTH & CARDIOVASCULAR REPORT")
        builder.appendLine("========================================")
        builder.appendLine("Generated on: $todayStr")
        builder.appendLine("Patient / User: $userName")
        builder.appendLine()

        builder.appendLine("--- 1. PHYSICAL ACTIVITY SUMMARY ---")
        builder.appendLine("• Days Monitored: ${dailySteps.size}")
        builder.appendLine("• Total Step Count: %,d steps".format(Locale.US, totalSteps))
        builder.appendLine("• Daily Step Average: %,d steps/day".format(Locale.US, avgSteps))
        builder.appendLine("• Total Distance: %.2f km".format(Locale.US, totalDistKm))
        builder.appendLine("• WHO 10k Target: ${if (avgSteps >= 10000) "Achieved" else "In Progress (%,d steps from target)".format(Locale.US, (10000 - avgSteps).coerceAtLeast(0))}")
        builder.appendLine()

        builder.appendLine("--- 2. BLOOD PRESSURE LOGS (AHA Staging) ---")
        if (bpLogs.isEmpty()) {
            builder.appendLine("No blood pressure readings recorded.")
        } else {
            builder.appendLine("• 30-Day Average: $avgSys / $avgDia mmHg (Pulse: $avgPulse bpm)")
            builder.appendLine("• Log History:")
            bpLogs.take(20).forEach { entry ->
                val date = dateFormat.format(Date(entry.timestamp))
                val noteStr = if (entry.note.isNotBlank()) " | Note: ${entry.note}" else ""
                builder.appendLine("  [$date] ${entry.systolic}/${entry.diastolic} mmHg (Pulse: ${entry.pulse} bpm) - ${entry.category}$noteStr")
            }
        }
        builder.appendLine()

        builder.appendLine("--- 3. HEART RATE & RHYTHM ---")
        if (hrLogs.isEmpty()) {
            builder.appendLine("No heart rate records available.")
        } else {
            builder.appendLine("• Average Monitored Pulse: $avgHr bpm")
            val restingLogs = hrLogs.filter { it.contextTag.contains("Resting", ignoreCase = true) }
            if (restingLogs.isNotEmpty()) {
                val avgResting = restingLogs.map { it.bpm }.average().toInt()
                builder.appendLine("• Average Resting Heart Rate: $avgResting bpm")
            }
        }
        builder.appendLine()

        builder.appendLine("========================================")
        builder.appendLine("CLINICAL NOTICE:")
        builder.appendLine("PulseStep is a wellness and activity companion. Data recorded via manual logs, Google Health Connect, and optical camera PPG pulse sensor. Please consult your physician for diagnostic and clinical decisions.")
        builder.appendLine("========================================")

        return builder.toString()
    }

    fun shareReport(
        context: Context,
        dailySteps: List<DailyStepRecord>,
        bpLogs: List<BloodPressureEntry>,
        hrLogs: List<HeartRateEntry>
    ) {
        val reportText = generateTextReport(dailySteps, bpLogs, hrLogs)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportText)
            putExtra(Intent.EXTRA_SUBJECT, "PulseStep Cardiovascular & Activity Health Report")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Health Report with Doctor")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
