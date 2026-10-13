package com.example.insights

import com.example.ai.GeminiClient
import com.example.data.local.BloodPressureEntry
import com.example.data.local.DailyStepRecord
import com.example.data.local.HeartRateEntry

enum class BpClassification(val label: String, val description: String) {
    NORMAL("Normal", "Systolic < 120 and Diastolic < 80 mmHg. Excellent cardiovascular baseline."),
    ELEVATED("Elevated", "Systolic 120–129 and Diastolic < 80 mmHg. Lifestyle modifications recommended."),
    STAGE_1("Hypertension Stage 1", "Systolic 130–139 or Diastolic 80–89 mmHg. Monitor regularly and consult healthcare provider."),
    STAGE_2("Hypertension Stage 2", "Systolic ≥ 140 or Diastolic ≥ 90 mmHg. Medical attention advised."),
    CRISIS("Hypertensive Crisis", "Systolic > 180 and/or Diastolic > 120 mmHg. Seek immediate medical attention.")
}

data class HealthInsightReport(
    val title: String,
    val summary: String,
    val bpStatus: String,
    val activityStatus: String,
    val heartRateStatus: String,
    val recommendations: List<String>,
    val isAiGenerated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

object ClinicalInsightsEngine {

    fun classifyBloodPressure(systolic: Int, diastolic: Int): BpClassification {
        return when {
            systolic > 180 || diastolic > 120 -> BpClassification.CRISIS
            systolic >= 140 || diastolic >= 90 -> BpClassification.STAGE_2
            systolic in 130..139 || diastolic in 80..89 -> BpClassification.STAGE_1
            systolic in 120..129 && diastolic < 80 -> BpClassification.ELEVATED
            systolic in 70..119 && diastolic in 40..79 -> BpClassification.NORMAL
            else -> BpClassification.NORMAL
        }
    }

    fun classifyHeartRate(bpm: Int): String {
        return when {
            bpm < 60 -> "Athletic / Low Resting"
            bpm in 60..100 -> "Normal Resting"
            else -> "Elevated / Active"
        }
    }

    suspend fun generateHealthReport(
        dailySteps: List<DailyStepRecord>,
        bpLogs: List<BloodPressureEntry>,
        hrLogs: List<HeartRateEntry>
    ): HealthInsightReport {
        val avgSteps = if (dailySteps.isNotEmpty()) dailySteps.map { it.stepCount }.average().toInt() else 0
        val latestBp = bpLogs.firstOrNull()
        val avgHr = if (hrLogs.isNotEmpty()) hrLogs.map { it.bpm }.average().toInt() else 0

        val bpCategory = latestBp?.let { classifyBloodPressure(it.systolic, it.diastolic) }
        val activityLevel = when {
            avgSteps >= 10000 -> "Optimal (Meeting WHO 10k target)"
            avgSteps >= 7500 -> "Active (Good daily consistency)"
            avgSteps >= 5000 -> "Moderately Active"
            avgSteps > 0 -> "Getting Started (Keep building steps!)"
            else -> "Clean Slate (Ready for your first walk)"
        }
        val hrCategory = if (avgHr > 0) classifyHeartRate(avgHr) else "Awaiting Check"
        val hrStatusString = if (avgHr > 0) "$avgHr bpm ($hrCategory)" else "No logs recorded"
        val bpStatusString = bpCategory?.label ?: "Awaiting First BP Log"

        // Try calling Gemini AI for an intelligent, contextual assessment if user has data
        if (avgSteps > 0 || latestBp != null || avgHr > 0) {
            val geminiPrompt = """
                You are a cardiovascular wellness and fitness expert advisor. Analyze the following user health telemetry:
                - Weekly Average Steps: $avgSteps steps/day
                - Activity Status: $activityLevel
                - Latest Blood Pressure: ${if (latestBp != null) "${latestBp.systolic}/${latestBp.diastolic} mmHg (${bpCategory?.label})" else "Not logged yet"}
                - Average Resting Heart Rate: ${if (avgHr > 0) "$avgHr bpm ($hrCategory)" else "Not logged yet"}
                
                Provide a concise, encouraging 2-sentence health evaluation followed by 3 actionable bullet points for improvement.
                Tone: empowering, clinically sound, accessible.
            """.trimIndent()

            val aiResult = GeminiClient.askGemini(geminiPrompt)

            if (aiResult.isSuccess) {
                val aiText = aiResult.getOrNull() ?: ""
                val lines = aiText.lines().filter { it.isNotBlank() }
                val summary = lines.take(2).joinToString(" ")
                val bullets = lines.filter { it.trim().startsWith("-") || it.trim().startsWith("*") || it.trim().matches(Regex("^\\d+\\..*")) }
                    .map { it.replace(Regex("^[-*\\d.]+\\s*"), "").trim() }
                    .filter { it.isNotEmpty() }
                    .take(3)

                return HealthInsightReport(
                    title = "Gemini AI Health Assessment",
                    summary = if (summary.isNotEmpty()) summary else aiText,
                    bpStatus = bpStatusString,
                    activityStatus = activityLevel,
                    heartRateStatus = hrStatusString,
                    recommendations = if (bullets.isNotEmpty()) bullets else listOf(
                        "Maintain 30 minutes of brisk daily walking to support optimal blood pressure.",
                        "Incorporate potassium-rich foods and stay hydrated throughout your walks.",
                        "Log blood pressure twice weekly at the same time for reliable trends."
                    ),
                    isAiGenerated = true
                )
            }
        }

        // Clinical rule-based engine fallback
        val recommendations = mutableListOf<String>()
        if (latestBp == null && avgSteps == 0) {
            recommendations.add("Start tracking your daily stride: aim for an initial 3,000 to 5,000 steps today.")
            recommendations.add("Take a resting heart rate reading using the Camera PPG Flash Pulse Scanner.")
            recommendations.add("Log your baseline blood pressure with a certified oscillometric arm cuff.")
        } else {
            if (bpCategory != null && bpCategory != BpClassification.NORMAL) {
                recommendations.add("Consider 20-30 minutes of aerobic walking daily, which can help reduce systolic pressure by 4-9 mmHg.")
            } else {
                recommendations.add("Keep up good cardiovascular habits! Consistent physical movement stabilizes resting blood pressure.")
            }

            if (avgSteps < 8000) {
                recommendations.add("Gradually increase your step goal by 500 steps each day toward the 10,000 daily milestone.")
            } else {
                recommendations.add("Outstanding activity consistency! You are meeting evidence-based step targets.")
            }

            recommendations.add("Stay well-hydrated and log your morning resting pulse to monitor cardiovascular recovery.")
        }

        val summaryText = if (avgSteps == 0 && latestBp == null) {
            "Welcome to PulseStep! Your tracker is fresh and ready. Begin walking and logging cardiovascular vitals to unlock personalized trend analysis."
        } else {
            "Your average of $avgSteps steps/day supports healthy circulation. Current blood pressure status: $bpStatusString."
        }

        return HealthInsightReport(
            title = "Clinical Cardiovascular Overview",
            summary = summaryText,
            bpStatus = bpStatusString,
            activityStatus = activityLevel,
            heartRateStatus = hrStatusString,
            recommendations = recommendations,
            isAiGenerated = false
        )
    }
}
