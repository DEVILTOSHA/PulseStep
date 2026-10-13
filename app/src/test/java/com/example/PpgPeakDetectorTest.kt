package com.example

import com.example.sensor.PpgPeakDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class PpgPeakDetectorTest {

    /** Fingertip red brightness: dips on each beat, plus a dicrotic bump, slow drift and sensor noise. */
    private fun syntheticRed(tMs: Long, bpm: Double, noise: Random): Double {
        val phase = 2 * PI * (tMs / 1000.0) * (bpm / 60.0)
        val pulse = 3.0 * sin(phase) + 0.9 * sin(2 * phase + PI / 2)
        val drift = 4.0 * sin(2 * PI * tMs / 20_000.0)
        return 160.0 - pulse + drift + noise.nextDouble(-0.15, 0.15)
    }

    private fun runScan(bpm: Double, seconds: Int = 15, fps: Int = 30, seed: Int = 1): PpgPeakDetector.Result {
        val detector = PpgPeakDetector()
        val noise = Random(seed)
        val frameMs = 1000L / fps
        var last: PpgPeakDetector.Result? = null
        var t = 0L
        while (t < seconds * 1000L) {
            last = detector.addSample(syntheticRed(t, bpm, noise), t)
            t += frameMs
        }
        return last!!
    }

    @Test
    fun `measures a resting pulse within 3 bpm`() {
        for (seed in 1..5) {
            val result = runScan(bpm = 72.0, seed = seed)
            assertTrue("seed $seed gave ${result.bpm}", result.bpm in 69..75)
        }
    }

    @Test
    fun `measures slow and fast pulses`() {
        val slow = runScan(bpm = 52.0)
        assertTrue("slow gave ${slow.bpm}", slow.bpm in 49..55)
        val fast = runScan(bpm = 130.0)
        assertTrue("fast gave ${fast.bpm}", fast.bpm in 125..135)
    }

    @Test
    fun `flat signal reports no pulse`() {
        val detector = PpgPeakDetector()
        val noise = Random(7)
        var result: PpgPeakDetector.Result? = null
        for (i in 0 until 450) {
            result = detector.addSample(160.0 + noise.nextDouble(-0.01, 0.01), i * 33L)
        }
        assertEquals(0, result!!.bpm)
    }

    @Test
    fun `reset clears previous beats`() {
        val detector = PpgPeakDetector()
        val noise = Random(3)
        for (i in 0 until 450) detector.addSample(syntheticRed(i * 33L, 72.0, noise), i * 33L)
        detector.reset()
        val after = detector.addSample(160.0, 20_000L)
        assertEquals(0, after.bpm)
        assertEquals(0, after.beatCount)
    }
}
