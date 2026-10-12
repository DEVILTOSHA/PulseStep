package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class PedometerSensorManager(
    private val context: Context,
    private val onStepDetected: (stepDelta: Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    private var initialCounterValue: Float = -1f
    private var lastReportedCounterSteps: Int = 0

    // Accelerometer fallback peak detection
    private var lastMagnitude = 0.0
    private var lastStepTimestamp = 0L
    private val stepThreshold = 11.5 // Gravity is ~9.8 m/s^2, peak above 11.5 indicates stride
    private val minStepIntervalMs = 280L

    var isTracking: Boolean = false
        private set

    init {
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun startListening() {
        if (isTracking || sensorManager == null) return
        isTracking = true

        // Priority 1: Step Detector (fires per step)
        var registered = false
        stepDetectorSensor?.let {
            registered = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // Priority 2: Step Counter (tracks cumulative)
        if (!registered) {
            stepCounterSensor?.let {
                registered = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }

        // Priority 3: Accelerometer fallback (works everywhere, including emulators)
        if (!registered) {
            accelerometerSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    fun stopListening() {
        if (!isTracking) return
        isTracking = false
        sensorManager?.unregisterListener(this)
        initialCounterValue = -1f
        lastReportedCounterSteps = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isTracking) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                // Event value 1.0 means 1 step detected
                if (event.values[0] == 1.0f) {
                    onStepDetected(1)
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val totalBootSteps = event.values[0]
                if (initialCounterValue < 0f) {
                    initialCounterValue = totalBootSteps
                    lastReportedCounterSteps = 0
                } else {
                    val currentSteps = (totalBootSteps - initialCounterValue).toInt()
                    val delta = currentSteps - lastReportedCounterSteps
                    if (delta > 0) {
                        lastReportedCounterSteps = currentSteps
                        onStepDetected(delta)
                    }
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble())
                val now = System.currentTimeMillis()

                if (magnitude > stepThreshold && lastMagnitude <= stepThreshold) {
                    if (now - lastStepTimestamp > minStepIntervalMs) {
                        lastStepTimestamp = now
                        onStepDetected(1)
                    }
                }
                lastMagnitude = magnitude
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    fun injectSimulatedSteps(count: Int) {
        onStepDetected(count)
    }
}
