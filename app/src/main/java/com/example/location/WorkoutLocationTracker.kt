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
    val isSimulated: Boolean = false,
    val usesGps: Boolean = true
)

class WorkoutLocationTracker(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val _liveMetrics = MutableStateFlow(WorkoutLiveMetrics())
    val liveMetrics: StateFlow<WorkoutLiveMetrics> = _liveMetrics.asStateFlow()

    private var lastLocation: Location? = null
    private var simulatedIndex = 0
    private var strideMeters = 0.76f

    // Realistic park walking path coordinates in India (Lodhi Gardens / Central Park loop) for simulation fallback
    private val simulatedBaseCoordinates = listOf(
        LatLngPoint(28.593320, 77.219750),
        LatLngPoint(28.593840, 77.220310),
        LatLngPoint(28.594410, 77.220940),
        LatLngPoint(28.595120, 77.221520),
        LatLngPoint(28.595830, 77.221980),
        LatLngPoint(28.596610, 77.222340),
        LatLngPoint(28.597320, 77.221870),
        LatLngPoint(28.597910, 77.221120),
        LatLngPoint(28.598350, 77.220260),
        LatLngPoint(28.597980, 77.219410),
        LatLngPoint(28.597240, 77.218720),
        LatLngPoint(28.596350, 77.218290),
        LatLngPoint(28.595410, 77.218380),
        LatLngPoint(28.594520, 77.218790),
        LatLngPoint(28.593820, 77.219210)
    )

    /**
     * Distance comes from exactly one source: GPS fixes when [useGps] is on, otherwise steps × stride.
     * [userStrideMeters] is the walking stride from the user's profile.
     */
    fun startWorkout(
        activityType: String,
        simulateMovement: Boolean = false,
        useGps: Boolean = true,
        userStrideMeters: Float = 0.76f
    ) {
        stopGpsUpdates()
        lastLocation = null
        simulatedIndex = 0
        strideMeters = when (activityType) {
            "Jogging" -> userStrideMeters * 1.25f
            "Hiking" -> userStrideMeters * 0.92f
            else -> userStrideMeters
        }
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
            isSimulated = simulateMovement,
            usesGps = useGps
        )

        if (!simulateMovement && useGps) {
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
        // Don't count the straight line between where we paused and where we resumed
        lastLocation = null
        if (!_liveMetrics.value.isSimulated && _liveMetrics.value.usesGps) {
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
        // Simulated trails and GPS workouts measure distance from the route instead
        val addedDist = if (!current.isSimulated && !current.usesGps) delta * strideMeters else 0f
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
        if (!current.isRunning || current.isPaused || current.isSimulated || !current.usesGps) return
        // Ignore coarse fixes; they add zig-zag jitter to both the route and the distance
        if (location.hasAccuracy() && location.accuracy > 30f) return

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
