package com.example

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.example.location.WorkoutLocationTracker
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutLocationTrackerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun location(lat: Double, lng: Double, accuracy: Float = 5f) = Location("gps").apply {
        latitude = lat
        longitude = lng
        this.accuracy = accuracy
        time = System.currentTimeMillis()
    }

    @Test
    fun `without GPS distance comes from steps times stride`() {
        val tracker = WorkoutLocationTracker(context)
        tracker.startWorkout("Walking", useGps = false, userStrideMeters = 0.8f)
        tracker.addStepDelta(100)
        assertEquals(80f, tracker.liveMetrics.value.distanceMeters, 0.01f)
    }

    @Test
    fun `with GPS steps do not add distance on top of the route`() {
        val tracker = WorkoutLocationTracker(context)
        tracker.startWorkout("Walking", useGps = true, userStrideMeters = 0.8f)
        tracker.onLocationChanged(location(12.9716, 77.5946))
        tracker.onLocationChanged(location(12.9725, 77.5946)) // ~100 m north
        val gpsDistance = tracker.liveMetrics.value.distanceMeters
        tracker.addStepDelta(130)
        assertEquals(gpsDistance, tracker.liveMetrics.value.distanceMeters, 0.01f)
        assertEquals(130, tracker.liveMetrics.value.currentSteps)
    }

    @Test
    fun `coarse GPS fixes are ignored`() {
        val tracker = WorkoutLocationTracker(context)
        tracker.startWorkout("Walking", useGps = true)
        tracker.onLocationChanged(location(12.9716, 77.5946))
        tracker.onLocationChanged(location(12.9800, 77.5946, accuracy = 500f))
        assertEquals(0f, tracker.liveMetrics.value.distanceMeters, 0.01f)
        assertEquals(1, tracker.liveMetrics.value.routePoints.size)
    }
}
