package com.example.sensor

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

data class PpgScanState(
    val isScanning: Boolean = false,
    val isFingerCovered: Boolean = false,
    val currentBpm: Int = 0,
    val pulseStrength: Float = 0f,
    val progressPercent: Int = 0, // 0 to 100 for 15s scan
    val remainingSeconds: Int = 15,
    val isScanComplete: Boolean = false,
    val finalBpm: Int = 0,
    val isSimulated: Boolean = false,
    val wavePoints: List<Float> = emptyList() // Normalized between -1f and 1f for live graph
)

class CameraPpgManager(private val context: Context) {

    private val _scanState = MutableStateFlow(PpgScanState())
    val scanState: StateFlow<PpgScanState> = _scanState.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private var simulationJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // Signal processing buffers
    private val rollingRedValues = ArrayDeque<Double>()
    private val maxBufferLength = 60
    private var lastPeakTime = 0L
    private val detectedIntervals = mutableListOf<Long>()
    private val wavePointsHistory = ArrayDeque<Float>()
    private val maxWavePoints = 80

    private var scanStartTime = 0L
    private val scanDurationMs = 15000L

    fun startScanning(lifecycleOwner: LifecycleOwner, simulate: Boolean = false) {
        stopScanning()

        _scanState.value = PpgScanState(
            isScanning = true,
            isFingerCovered = simulate,
            currentBpm = if (simulate) 72 else 0,
            remainingSeconds = 15,
            isSimulated = simulate
        )
        scanStartTime = System.currentTimeMillis()
        rollingRedValues.clear()
        detectedIntervals.clear()
        wavePointsHistory.clear()

        if (simulate) {
            startSimulationLoop()
        } else {
            startCameraPipeline(lifecycleOwner)
        }
    }

    private fun startCameraPipeline(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    processImageFrame(imageProxy)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )

                // Turn on camera torch (flashlight)
                camera?.cameraControl?.enableTorch(true)
            } catch (_: Exception) {
                // If camera setup fails (e.g. on emulator without camera hardware), fallback to simulation
                startSimulationLoop()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImageFrame(imageProxy: ImageProxy) {
        val plane = imageProxy.planes.firstOrNull() ?: run {
            imageProxy.close()
            return
        }

        val buffer: ByteBuffer = plane.buffer
        val totalBytes = buffer.remaining()
        val pixelStride = plane.pixelStride.coerceAtLeast(1)

        // Sample center pixels to compute average R, G, B
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0L

        val step = (pixelStride * 8).coerceAtLeast(4)
        var i = 0
        while (i < totalBytes - 3) {
            val r = buffer.get(i).toInt() and 0xFF
            val g = buffer.get(i + 1).toInt() and 0xFF
            val b = buffer.get(i + 2).toInt() and 0xFF

            sumR += r
            sumG += g
            sumB += b
            count++
            i += step
        }
        imageProxy.close()

        if (count == 0L) return

        val avgR = sumR.toDouble() / count
        val avgG = sumG.toDouble() / count
        val avgB = sumB.toDouble() / count

        // Check finger covering: high red content compared to green and blue
        val isCovered = avgR > 80.0 && (avgR / (avgG + avgB + 1.0)) > 1.6

        val now = System.currentTimeMillis()
        val elapsed = now - scanStartTime
        val remaining = ((scanDurationMs - elapsed) / 1000L).coerceAtLeast(0L).toInt()
        val progress = ((elapsed.toFloat() / scanDurationMs) * 100f).toInt().coerceIn(0, 100)

        if (!isCovered) {
            _scanState.value = _scanState.value.copy(
                isFingerCovered = false,
                remainingSeconds = remaining,
                progressPercent = progress
            )
            return
        }

        // Add to red buffer
        rollingRedValues.addLast(avgR)
        if (rollingRedValues.size > maxBufferLength) {
            rollingRedValues.removeFirst()
        }

        // Compute baseline (DC component)
        val baseline = rollingRedValues.average()
        val acSignal = avgR - baseline

        // Normalize AC signal for visualization
        val normalizedWave = (acSignal / 15.0).toFloat().coerceIn(-1f, 1f)
        wavePointsHistory.addLast(normalizedWave)
        if (wavePointsHistory.size > maxWavePoints) {
            wavePointsHistory.removeFirst()
        }

        // Peak detection
        var currentBpm = _scanState.value.currentBpm
        if (acSignal > 1.8 && (now - lastPeakTime) > 350L) { // Min 350ms = max 171 BPM
            if (lastPeakTime > 0L) {
                val interval = now - lastPeakTime
                if (interval in 350..1500) { // Valid 40 to 171 BPM range
                    detectedIntervals.add(interval)
                    if (detectedIntervals.size > 8) detectedIntervals.removeAt(0)
                    val avgInterval = detectedIntervals.average()
                    if (avgInterval > 0) {
                        currentBpm = (60000.0 / avgInterval).toInt()
                    }
                }
            }
            lastPeakTime = now
        }

        val isComplete = elapsed >= scanDurationMs && currentBpm > 45

        _scanState.value = _scanState.value.copy(
            isFingerCovered = true,
            currentBpm = currentBpm,
            pulseStrength = (acSignal.toFloat() / 10f).coerceIn(0f, 1f),
            remainingSeconds = remaining,
            progressPercent = progress,
            isScanComplete = isComplete,
            finalBpm = if (isComplete) currentBpm else 0,
            wavePoints = wavePointsHistory.toList()
        )

        if (isComplete) {
            stopCamera()
        }
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = coroutineScope.launch {
            var phase = 0f
            val baseBpm = 72
            scanStartTime = System.currentTimeMillis()

            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = now - scanStartTime
                val remaining = ((scanDurationMs - elapsed) / 1000L).coerceAtLeast(0L).toInt()
                val progress = ((elapsed.toFloat() / scanDurationMs) * 100f).toInt().coerceIn(0, 100)

                // Realistic physiological PPG pulse wave equation: fundamental wave + dicrotic notch
                phase += 0.22f
                val ppgValue = (sin(phase) + 0.35 * sin(phase * 2.0 + PI * 0.5)).toFloat().coerceIn(-1f, 1f)

                wavePointsHistory.addLast(ppgValue)
                if (wavePointsHistory.size > maxWavePoints) {
                    wavePointsHistory.removeFirst()
                }

                val isComplete = elapsed >= scanDurationMs

                _scanState.value = PpgScanState(
                    isScanning = true,
                    isFingerCovered = true,
                    currentBpm = baseBpm + (sin(phase * 0.1f) * 2).toInt(),
                    pulseStrength = 0.85f,
                    progressPercent = progress,
                    remainingSeconds = remaining,
                    isScanComplete = isComplete,
                    finalBpm = if (isComplete) baseBpm else 0,
                    isSimulated = true,
                    wavePoints = wavePointsHistory.toList()
                )

                if (isComplete) break
                delay(33L) // ~30 fps
            }
        }
    }

    fun stopScanning() {
        simulationJob?.cancel()
        simulationJob = null
        stopCamera()
        _scanState.value = _scanState.value.copy(isScanning = false)
    }

    private fun stopCamera() {
        try {
            camera?.cameraControl?.enableTorch(false)
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}
        camera = null
    }
}
