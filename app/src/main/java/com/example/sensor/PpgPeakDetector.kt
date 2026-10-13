package com.example.sensor

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Turns a stream of fingertip red-channel brightness samples into heart beats.
 *
 * Pipeline: subtract a moving-average baseline (removes slow drift), invert (more blood in the
 * fingertip means *less* red light back, so systolic beats are brightness dips), smooth over 3
 * samples, then mark a beat at each local maximum that rises above half the signal's recent
 * standard deviation. BPM is the median of recent beat-to-beat intervals, with outliers rejected,
 * so one noisy frame can't swing the reading.
 */
class PpgPeakDetector(
    private val baselineWindow: Int = 45,      // ~1.5 s at 30 fps
    private val amplitudeWindow: Int = 90,     // ~3 s at 30 fps
    private val minIntervalMs: Long = 333L,    // 180 bpm
    private val maxIntervalMs: Long = 1500L,   // 40 bpm
    private val maxIntervals: Int = 10
) {
    data class Result(
        /** Smoothed pulse signal scaled to roughly -1..1, for drawing. */
        val waveValue: Float,
        val isPeak: Boolean,
        /** 0 until enough consistent beats have been seen. */
        val bpm: Int,
        val beatCount: Int
    )

    private val raw = ArrayDeque<Double>()
    private val ac = ArrayDeque<Double>()
    private val smoothed = ArrayDeque<Double>()
    private val intervals = ArrayDeque<Long>()
    private var prevTime = -1L
    private var lastPeakTime = -1L
    private var rejectedStreak = 0

    fun reset() {
        raw.clear()
        ac.clear()
        smoothed.clear()
        intervals.clear()
        prevTime = -1L
        lastPeakTime = -1L
        rejectedStreak = 0
    }

    fun addSample(redMean: Double, timeMs: Long): Result {
        raw.push(redMean, baselineWindow)
        val baseline = raw.average()
        ac.push(baseline - redMean, 3)
        smoothed.push(ac.average(), amplitudeWindow)

        val std = stdDev(smoothed)
        val n = smoothed.size
        var isPeak = false

        // The previous sample is a peak if it's higher than both neighbours and clearly above noise.
        if (n >= 3 && raw.size >= baselineWindow / 2 && std > MIN_AMPLITUDE) {
            val a = smoothed[n - 3]
            val b = smoothed[n - 2]
            val c = smoothed[n - 1]
            if (b > a && b >= c && b > 0.5 * std) {
                val peakTime = prevTime
                if (lastPeakTime < 0L) {
                    lastPeakTime = peakTime
                    isPeak = true
                } else {
                    val interval = peakTime - lastPeakTime
                    when {
                        interval < minIntervalMs -> Unit // dicrotic notch or noise: ignore
                        interval > maxIntervalMs -> {   // missed beats: restart the interval chain
                            lastPeakTime = peakTime
                            isPeak = true
                        }
                        else -> {
                            if (isConsistent(interval)) intervals.push(interval, maxIntervals)
                            lastPeakTime = peakTime
                            isPeak = true
                        }
                    }
                }
            }
        }
        prevTime = timeMs

        val wave = if (std > 0.0) (smoothed.last() / (2.5 * std)).toFloat().coerceIn(-1f, 1f) else 0f
        return Result(waveValue = wave, isPeak = isPeak, bpm = currentBpm(), beatCount = intervals.size)
    }

    private fun currentBpm(): Int {
        if (intervals.size < MIN_INTERVALS_FOR_BPM) return 0
        return (60_000.0 / median(intervals)).toInt()
    }

    /**
     * Rejects intervals more than 30% away from the running median once a few beats exist.
     * Three rejections in a row mean the history itself was wrong (e.g. started on noise), so start over.
     */
    private fun isConsistent(interval: Long): Boolean {
        if (intervals.size < 3) return true
        val med = median(intervals)
        if (abs(interval - med) <= 0.3 * med) {
            rejectedStreak = 0
            return true
        }
        if (++rejectedStreak >= 3) {
            intervals.clear()
            rejectedStreak = 0
            return true
        }
        return false
    }

    private fun <T> ArrayDeque<T>.push(value: T, max: Int) {
        addLast(value)
        while (size > max) removeFirst()
    }

    private fun median(values: Collection<Long>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid].toDouble()
    }

    private fun stdDev(values: Collection<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }

    companion object {
        /** Below this the signal is flat (no finger pulse visible) and nothing counts as a beat. */
        private const val MIN_AMPLITUDE = 0.05
        const val MIN_INTERVALS_FOR_BPM = 5
    }
}
