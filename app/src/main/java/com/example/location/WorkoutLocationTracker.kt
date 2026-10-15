package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val currentPaceMinPerKm: Float = 0f,
    val caloriesBurned: Int = 0,
    val routePoints: List<LatLngPoint> = emptyList(),
    val activityType: String = "Walking",
    val isSimulated: Boolean = false,
    val currentPosition: LatLngPoint? = null,
    val isGpsLocked: Boolean = false,
    val accuracyMeters: Float = 0f,
    val provider: String = "GPS"
)

/**
 * High-Precision GPS & Location Tracker for PulseStep.
 * Connects directly to Google Play Services FusedLocationProviderClient (High Accuracy)
 * with concurrent fallback to Android native LocationManager (GPS, Network & Passive providers).
 * Never hardcodes artificial coordinates when real user coordinates are available.
 */
class WorkoutLocationTracker(private val context: Context) : LocationListener {

    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _liveMetrics = MutableStateFlow(WorkoutLiveMetrics())
    val liveMetrics: StateFlow<WorkoutLiveMetrics> = _liveMetrics.asStateFlow()

    private var lastLocation: Location? = null
    private var isFusedTrackingActive = false
    private var isLocationManagerTrackingActive = false
    private var simulationStepCount = 0

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            handleNewLocation(location, "Google Play Services")
        }
    }

    init {
        // Attempt initial location query on initialization
        fetchImmediateLastKnownLocation()
        startLocationUpdates()
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isGpsProviderEnabled(): Boolean {
        return try {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                    locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Actively fetches the user's pinpoint location right now using all available providers.
     */
    @SuppressLint("MissingPermission")
    fun fetchImmediateLastKnownLocation() {
        if (!hasLocationPermission()) return

        // 1. Google Play Services getCurrentLocation (forces fresh hardware/network satellite fix)
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        handleNewLocation(loc, "Google Fused (High Accuracy)")
                    }
                }
        } catch (_: Exception) {}

        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null && _liveMetrics.value.currentPosition == null) {
                        handleNewLocation(loc, "Google Fused (Balanced)")
                    }
                }
        } catch (_: Exception) {}

        // 2. Google Play Services cached lastLocation (instantaneous response)
        try {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && _liveMetrics.value.currentPosition == null) {
                    handleNewLocation(loc, "Google Fused Cache")
                }
            }
        } catch (_: Exception) {}

        // 3. Android LocationManager system providers
        try {
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val lastPassive = locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

            val bestCandidate = listOfNotNull(lastGps, lastNet, lastPassive)
                .maxByOrNull { it.time }

            if (bestCandidate != null && _liveMetrics.value.currentPosition == null) {
                handleNewLocation(bestCandidate, "Native Provider (${bestCandidate.provider})")
            }
        } catch (_: Exception) {}
    }

    /**
     * Registers continuous high-accuracy location tracking listeners.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        // 1. Google Play Services Fused Location Provider
        if (!isFusedTrackingActive) {
            try {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    1000L // 1-second update interval
                ).apply {
                    setMinUpdateIntervalMillis(500L)
                    setMinUpdateDistanceMeters(0f) // Report any micro-movement
                    setWaitForAccurateLocation(false)
                }.build()

                fusedClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                ).addOnSuccessListener {
                    isFusedTrackingActive = true
                }.addOnFailureListener {
                    isFusedTrackingActive = false
                }
            } catch (_: Exception) {
                isFusedTrackingActive = false
            }
        }

        // 2. Android Native LocationManager concurrent listeners
        if (!isLocationManagerTrackingActive && locationManager != null) {
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L,
                        0f,
                        this,
                        Looper.getMainLooper()
                    )
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000L,
                        0f,
                        this,
                        Looper.getMainLooper()
                    )
                }
                isLocationManagerTrackingActive = true
            } catch (_: Exception) {
                isLocationManagerTrackingActive = false
            }
        }
    }

    fun startWorkout(activityType: String, simulateMovement: Boolean = false) {
        lastLocation = null
        simulationStepCount = 0

        val currentPos = _liveMetrics.value.currentPosition
        val initialPoints = if (currentPos != null) {
            listOf(currentPos)
        } else {
            emptyList()
        }

        _liveMetrics.value = _liveMetrics.value.copy(
            isRunning = true,
            isPaused = false,
            elapsedTimeSeconds = 0,
            distanceMeters = 0f,
            currentSteps = 0,
            currentPaceMinPerKm = 0f,
            caloriesBurned = 0,
            routePoints = initialPoints,
            activityType = activityType,
            isSimulated = simulateMovement,
            currentPosition = currentPos
        )

        // Make sure listeners are active
        startLocationUpdates()
        fetchImmediateLastKnownLocation()
    }

    fun pauseWorkout() {
        _liveMetrics.value = _liveMetrics.value.copy(isPaused = true)
    }

    fun resumeWorkout() {
        _liveMetrics.value = _liveMetrics.value.copy(isPaused = false)
        startLocationUpdates()
    }

    fun stopWorkout() {
        _liveMetrics.value = _liveMetrics.value.copy(isRunning = false, isPaused = false)
    }

    fun stopAllTracking() {
        isFusedTrackingActive = false
        isLocationManagerTrackingActive = false
        try {
            fusedClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}

        try {
            locationManager?.removeUpdates(this)
        } catch (_: Exception) {}
    }

    fun onTickSecond() {
        val current = _liveMetrics.value
        if (!current.isRunning || current.isPaused) return

        val newTime = current.elapsedTimeSeconds + 1

        if (current.isSimulated) {
            // Generate simulated movement around the user's real location (or default starting point if no location)
            val baseLat = current.currentPosition?.latitude ?: 19.0760 // User's real location, or default if none
            val baseLng = current.currentPosition?.longitude ?: 72.8777

            simulationStepCount++
            // Create a realistic outdoor loop walk
            val angle = (simulationStepCount * 0.15) % (2 * Math.PI)
            val radius = 0.00045 + (0.00015 * Math.sin(angle * 3))
            val nextLat = baseLat + (radius * Math.cos(angle))
            val nextLng = baseLng + (radius * Math.sin(angle))
            val nextPoint = LatLngPoint(nextLat, nextLng)

            val points = current.routePoints.toMutableList()
            points.add(nextPoint)

            val addedDist = 1.35f // ~1.35m per second (~4.8 km/h walking pace)
            val newDist = current.distanceMeters + addedDist
            val newSteps = current.currentSteps + 2
            val km = newDist / 1000f
            val pace = if (km > 0.02f) (newTime / 60f) / km else 0f
            val cals = (newSteps * 0.045f + (newDist * 0.035f)).toInt()

            _liveMetrics.value = current.copy(
                elapsedTimeSeconds = newTime,
                distanceMeters = newDist,
                currentSteps = newSteps,
                currentPaceMinPerKm = pace,
                caloriesBurned = cals,
                routePoints = points,
                currentPosition = nextPoint,
                isGpsLocked = true,
                accuracyMeters = 3.5f,
                provider = "Simulation"
            )
        } else {
            val km = current.distanceMeters / 1000f
            val pace = if (km > 0.02f) (newTime / 60f) / km else 0f
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
        val pace = if (km > 0.02f) (current.elapsedTimeSeconds / 60f) / km else 0f
        val cals = (newSteps * 0.045f + (newDist * 0.032f)).toInt()

        _liveMetrics.value = current.copy(
            currentSteps = newSteps,
            distanceMeters = newDist,
            currentPaceMinPerKm = pace,
            caloriesBurned = cals
        )
    }

    private fun handleNewLocation(location: Location, providerName: String = "GPS") {
        val current = _liveMetrics.value
        val point = LatLngPoint(location.latitude, location.longitude, location.time)

        // Always update the live pinpoint location and accuracy
        if (!current.isRunning) {
            _liveMetrics.value = current.copy(
                currentPosition = point,
                isGpsLocked = true,
                accuracyMeters = location.accuracy,
                provider = providerName
            )
            return
        }

        if (current.isPaused || current.isSimulated) return

        // Compute incremental distance if previous point exists
        var addedDistance = 0f
        lastLocation?.let { prev ->
            val dist = prev.distanceTo(location)
            val timeDiffSec = ((location.time - prev.time).coerceAtLeast(500L)) / 1000f
            val speed = dist / timeDiffSec
            // Filter extreme GPS jitter jumps (> 50 m/s ~ 180 km/h)
            if (speed < 50f && dist > 0.4f) {
                addedDistance = dist
            }
        }
        lastLocation = location

        val points = current.routePoints.toMutableList()
        points.add(point)

        val newDistance = current.distanceMeters + addedDistance
        val km = newDistance / 1000f
        val pace = if (km > 0.02f) (current.elapsedTimeSeconds / 60f) / km else 0f

        _liveMetrics.value = current.copy(
            distanceMeters = newDistance,
            currentPaceMinPerKm = pace,
            routePoints = points,
            currentPosition = point,
            isGpsLocked = true,
            accuracyMeters = location.accuracy,
            provider = providerName
        )
    }

    override fun onLocationChanged(location: Location) {
        handleNewLocation(location, "Native LocationListener")
    }

    override fun onProviderEnabled(provider: String) {
        fetchImmediateLastKnownLocation()
    }

    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
