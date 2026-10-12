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
        val avgSteps = if (dailySteps.isNotEmpty()) dailySteps.map { it.stepCount }.average().toInt() else 7000
        val latestBp = bpLogs.firstOrNull()
        val avgHr = if (hrLogs.isNotEmpty()) hrLogs.map { it.bpm }.average().toInt() else 72

        val bpCategory = latestBp?.let { classifyBloodPressure(it.systolic, it.diastolic) } ?: BpClassification.NORMAL
        val activityLevel = when {
            avgSteps >= 10000 -> "Optimal (Meeting WHO 10k target)"
            avgSteps >= 7500 -> "Active (Good daily consistency)"
            avgSteps >= 5000 -> "Moderately Active"
            else -> "Low Activity (Under 5k steps/day)"
        }
        val hrCategory = classifyHeartRate(avgHr)

        // Try calling Gemini AI for an intelligent, contextual assessment
        val geminiPrompt = """
            You are a cardiovascular wellness and fitness expert advisor. Analyze the following user health telemetry:
            - Weekly Average Steps: $avgSteps steps/day
            - Activity Status: $activityLevel
            - Latest Blood Pressure: ${latestBp?.systolic ?: 120}/${latestBp?.diastolic ?: 80} mmHg (${bpCategory.label})
            - Average Resting Heart Rate: $avgHr bpm ($hrCategory)
            
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
                bpStatus = bpCategory.label,
                activityStatus = activityLevel,
                heartRateStatus = "$avgHr bpm ($hrCategory)",
                recommendations = if (bullets.isNotEmpty()) bullets else listOf(
                    "Maintain 30 minutes of brisk daily walking to support optimal blood pressure.",
                    "Incorporate potassium-rich foods and stay hydrated throughout your walks.",
                    "Log blood pressure twice weekly at the same time for reliable trends."
                ),
                isAiGenerated = true
            )
        }

        // Clinical rule-based engine fallback
        val recommendations = mutableListOf<String>()
        if (bpCategory != BpClassification.NORMAL) {
            recommendations.add("Consider 20-30 minutes of aerobic walking daily, which can help reduce systolic pressure by 4-9 mmHg.")
        } else {
            recommendations.add("Great cardiovascular efficiency! Your blood pressure is in the optimal range.")
        }

        if (avgSteps < 8000) {
            recommendations.add("Gradually increase your step goal by 500 steps each day toward the 10,000 daily milestone.")
        } else {
            recommendations.add("Outstanding activity consistency! You are meeting evidence-based step targets.")
        }

        recommendations.add("Stay well-hydrated and log your morning resting pulse to monitor cardiovascular recovery.")

        return HealthInsightReport(
            title = "Clinical Cardiovascular Overview",
            summary = "Your 7-day average of $avgSteps steps/day supports healthy circulation. Recent blood pressure reading of ${latestBp?.systolic ?: 120}/${latestBp?.diastolic ?: 80} mmHg is categorized as ${bpCategory.label}.",
            bpStatus = bpCategory.label,
            activityStatus = activityLevel,
            heartRateStatus = "$avgHr bpm ($hrCategory)",
            recommendations = recommendations,
            isAiGenerated = false
        )
    }
}
