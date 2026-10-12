package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class WorkoutLiveMetrics(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedTimeSeconds: Long = 0,
    val distanceMeters: Float = 0f,
    val currentSteps: Int = 0,
    val currentPaceMinPerKm: Float = 0f, // e.g. 5.5 = 5m 30s
    val caloriesBurned: Int = 0,
    val routePoints: List<LatLngPoint> = emptyList(),
    val activityType: String = "Walking",
    val isSimulated: Boolean = false
)

class WorkoutLocationTracker(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val _liveMetrics = MutableStateFlow(WorkoutLiveMetrics())
    val liveMetrics: StateFlow<WorkoutLiveMetrics> = _liveMetrics.asStateFlow()

    private var lastLocation: Location? = null
    private var simulatedIndex = 0

    // Realistic park walking path coordinates for simulation fallback
    private val simulatedBaseCoordinates = listOf(
        LatLngPoint(37.774929, -122.419416),
        LatLngPoint(37.775345, -122.418820),
        LatLngPoint(37.775892, -122.418210),
        LatLngPoint(37.776510, -122.417530),
        LatLngPoint(37.777120, -122.416950),
        LatLngPoint(37.777890, -122.416240),
        LatLngPoint(37.778610, -122.415890),
        LatLngPoint(37.779240, -122.416410),
        LatLngPoint(37.779680, -122.417250),
        LatLngPoint(37.780120, -122.418190),
        LatLngPoint(37.779830, -122.419120),
        LatLngPoint(37.779110, -122.419840),
        LatLngPoint(37.778150, -122.420230),
        LatLngPoint(37.777240, -122.420100),
        LatLngPoint(37.776110, -122.419750),
        LatLngPoint(37.775210, -122.419510)
    )

    fun startWorkout(activityType: String, simulateMovement: Boolean = false) {
        lastLocation = null
        simulatedIndex = 0
        _liveMetrics.value = WorkoutLiveMetrics(
            isRunning = true,
            isPaused = false,
            elapsedTimeSeconds = 0,
            distanceMeters = 0f,
            currentSteps = 0,
            currentPaceMinPerKm = 0f,
            caloriesBurned = 0,
            routePoints = if (simulateMovement) listOf(simulatedBaseCoordinates[0]) else emptyList(),
            activityType = activityType,
            isSimulated = simulateMovement
        )

        if (!simulateMovement) {
            requestGpsUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestGpsUpdates() {
        try {
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    2f,
                    this
                )
            } else if (locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    2f,
                    this
                )
            }
        } catch (_: SecurityException) {
            // Handled gracefully, user may not have granted permissions yet
        }
    }

    fun pauseWorkout() {
        _liveMetrics.value = _liveMetrics.value.copy(isPaused = true)
        stopGpsUpdates()
    }

    fun resumeWorkout() {
        _liveMetrics.value = _liveMetrics.value.copy(isPaused = false)
        if (!_liveMetrics.value.isSimulated) {
            requestGpsUpdates()
        }
    }

    fun stopWorkout() {
        _liveMetrics.value = _liveMetrics.value.copy(isRunning = false, isPaused = false)
        stopGpsUpdates()
    }

    private fun stopGpsUpdates() {
        try {
            locationManager?.removeUpdates(this)
        } catch (_: Exception) {}
    }

    fun onTickSecond() {
        val current = _liveMetrics.value
        if (!current.isRunning || current.isPaused) return

        val newTime = current.elapsedTimeSeconds + 1

        if (current.isSimulated) {
            // Advance along simulated trail every ~3 seconds
            var newDist = current.distanceMeters
            var newSteps = current.currentSteps
            val points = current.routePoints.toMutableList()

            if (newTime % 3L == 0L) {
                simulatedIndex = (simulatedIndex + 1) % simulatedBaseCoordinates.size
                val nextPt = simulatedBaseCoordinates[simulatedIndex]
                points.add(nextPt)
                newDist += 18.5f // ~18.5 meters every 3s (~6.1 m/s jog or brisk walk)
                newSteps += 24
            }

            val km = newDist / 1000f
            val pace = if (km > 0.05f) (newTime / 60f) / km else 0f
            val cals = (newSteps * 0.048f + (newDist * 0.035f)).toInt()

            _liveMetrics.value = current.copy(
                elapsedTimeSeconds = newTime,
                distanceMeters = newDist,
                currentSteps = newSteps,
                currentPaceMinPerKm = pace,
                caloriesBurned = cals,
                routePoints = points
            )
        } else {
            val km = current.distanceMeters / 1000f
            val pace = if (km > 0.05f) (newTime / 60f) / km else 0f
            val cals = (current.currentSteps * 0.045f + (current.distanceMeters * 0.032f)).toInt()

            _liveMetrics.value = current.copy(
                elapsedTimeSeconds = newTime,
                currentPaceMinPerKm = pace,
                caloriesBurned = cals
            )
        }
    }

    fun addStepDelta(delta: Int) {
        val current = _liveMetrics.value
        if (!current.isRunning || current.isPaused) return

        val newSteps = current.currentSteps + delta
        val strideLength = when (current.activityType) {
            "Jogging" -> 0.95f
            "Hiking" -> 0.70f
            else -> 0.76f
        }
        val addedDist = if (!current.isSimulated) delta * strideLength else 0f
        val newDist = current.distanceMeters + addedDist
        val km = newDist / 1000f
        val pace = if (km > 0.05f) (current.elapsedTimeSeconds / 60f) / km else 0f
        val cals = (newSteps * 0.045f + (newDist * 0.032f)).toInt()

        _liveMetrics.value = current.copy(
            currentSteps = newSteps,
            distanceMeters = newDist,
            currentPaceMinPerKm = pace,
            caloriesBurned = cals
        )
    }

    override fun onLocationChanged(location: Location) {
        val current = _liveMetrics.value
        if (!current.isRunning || current.isPaused || current.isSimulated) return

        val point = LatLngPoint(location.latitude, location.longitude, location.time)
        val points = current.routePoints.toMutableList()
        points.add(point)

        var addedDistance = 0f
        lastLocation?.let {
            addedDistance = it.distanceTo(location)
        }
        lastLocation = location

        val newDistance = current.distanceMeters + addedDistance
        val km = newDistance / 1000f
        val pace = if (km > 0.05f) (current.elapsedTimeSeconds / 60f) / km else 0f

        _liveMetrics.value = current.copy(
            distanceMeters = newDistance,
            currentPaceMinPerKm = pace,
            routePoints = points
        )
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
