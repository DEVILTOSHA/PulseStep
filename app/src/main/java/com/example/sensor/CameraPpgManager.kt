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

    // Signal processing state. Only touched from analysisExecutor, except resets in startScanning()
    // which happen before the analyzer is bound.
    private val detector = PpgPeakDetector()
    private val wavePointsHistory = ArrayDeque<Float>()
    private val maxWavePoints = 80

    private var scanStartTime = 0L
    private val scanDurationMs = 15000L

    // The 15 s countdown only runs while the finger stays on the lens
    private var coveredSinceMs = -1L
    @Volatile private var scanFinished = false

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
        detector.reset()
        wavePointsHistory.clear()
        coveredSinceMs = -1L
        scanFinished = false

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
                // If camera setup fails (e.g. on emulator without camera hardware), fallback to simulation.
                // The state's isSimulated flag tells the UI, which then won't save the result.
                startSimulationLoop()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImageFrame(imageProxy: ImageProxy) {
        if (scanFinished) {
            imageProxy.close()
            return
        }
        val plane = imageProxy.planes.firstOrNull() ?: run {
            imageProxy.close()
            return
        }

        // Average the central half of the frame (where the fingertip is), honouring row padding.
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride.coerceAtLeast(4) // RGBA_8888
        val width = imageProxy.width
        val height = imageProxy.height
        val frameTimeMs = imageProxy.imageInfo.timestamp / 1_000_000L

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0L
        val sampleStep = 4
        var y = height / 4
        while (y < height * 3 / 4) {
            var x = width / 4
            while (x < width * 3 / 4) {
                val idx = y * rowStride + x * pixelStride
                if (idx + 2 < buffer.limit()) {
                    sumR += buffer.get(idx).toInt() and 0xFF
                    sumG += buffer.get(idx + 1).toInt() and 0xFF
                    sumB += buffer.get(idx + 2).toInt() and 0xFF
                    count++
                }
                x += sampleStep
            }
            y += sampleStep
        }
        imageProxy.close()

        if (count == 0L) return

        val avgR = sumR.toDouble() / count
        val avgG = sumG.toDouble() / count
        val avgB = sumB.toDouble() / count

        // Finger over a lit flash: frame is bright and strongly red
        val isCovered = avgR > 80.0 && (avgR / (avgG + avgB + 1.0)) > 1.6

        if (!isCovered) {
            // Lifting the finger restarts the measurement; stale beats would skew the result
            coveredSinceMs = -1L
            detector.reset()
            wavePointsHistory.clear()
            _scanState.value = _scanState.value.copy(
                isFingerCovered = false,
                currentBpm = 0,
                remainingSeconds = (scanDurationMs / 1000L).toInt(),
                progressPercent = 0,
                wavePoints = emptyList()
            )
            return
        }

        if (coveredSinceMs < 0L) coveredSinceMs = frameTimeMs
        var coveredMs = frameTimeMs - coveredSinceMs

        val result = detector.addSample(avgR, frameTimeMs)
        wavePointsHistory.addLast(result.waveValue)
        if (wavePointsHistory.size > maxWavePoints) {
            wavePointsHistory.removeFirst()
        }

        val bpm = result.bpm
        val hasReliableBpm = bpm in 40..180
        var isComplete = false
        if (coveredMs >= scanDurationMs) {
            if (hasReliableBpm) {
                isComplete = true
            } else {
                // 15 s of contact but no steady rhythm (movement, pressing too hard): start a fresh window
                coveredSinceMs = frameTimeMs
                coveredMs = 0L
                detector.reset()
            }
        }
        val remaining = ((scanDurationMs - coveredMs + 999L) / 1000L).coerceAtLeast(0L).toInt()
        val progress = ((coveredMs.toFloat() / scanDurationMs) * 100f).toInt().coerceIn(0, 100)

        _scanState.value = _scanState.value.copy(
            isFingerCovered = true,
            currentBpm = if (hasReliableBpm) bpm else 0,
            pulseStrength = if (result.isPeak) 1f else (result.waveValue.coerceAtLeast(0f)),
            remainingSeconds = remaining,
            progressPercent = progress,
            isScanComplete = isComplete,
            finalBpm = if (isComplete) bpm else 0,
            wavePoints = wavePointsHistory.toList()
        )

        if (isComplete) {
            scanFinished = true
            ContextCompat.getMainExecutor(context).execute { stopCamera() }
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
                val bpm = baseBpm + (sin(phase * 0.1f) * 2).toInt()

                _scanState.value = PpgScanState(
                    isScanning = true,
                    isFingerCovered = true,
                    currentBpm = bpm,
                    pulseStrength = 0.85f,
                    progressPercent = progress,
                    remainingSeconds = remaining,
                    isScanComplete = isComplete,
                    finalBpm = if (isComplete) bpm else 0,
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
