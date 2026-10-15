package com.example.sensor

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * High-pitch acoustic siren & synchronized camera flash strobe controller for Emergency SOS.
 * Generates piercing 3,200 Hz - 3,600 Hz audio waves designed for long-distance acoustic projection
 * and flashes the device hardware camera torch in lockstep.
 */
class EmergencySignalController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var activeJob: Job? = null
    private var isTorchOn = false
    private var torchCameraId: String? = null

    init {
        torchCameraId = findFlashCameraId()
    }

    private fun findFlashCameraId(): String? {
        val cm = cameraManager ?: return null
        return try {
            cm.cameraIdList.firstOrNull { id ->
                try {
                    val chars = cm.getCameraCharacteristics(id)
                    val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val facing = chars.get(CameraCharacteristics.LENS_FACING)
                    hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
                } catch (_: Exception) {
                    false
                }
            } ?: cm.cameraIdList.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Start the synchronized high-pitch siren and camera flash strobe.
     * @param mode "PIERCING_SIREN" for alternating 3,600 Hz / 3,000 Hz long-range audio warble,
     *             or "MORSE_SOS" for international Morse Code (... --- ...) audio blasts.
     */
    fun startSignal(scope: CoroutineScope, mode: String = "PIERCING_SIREN") {
        stopSignal()

        // Maximize alarm stream volume so sound carries as far as physically possible
        try {
            audioManager?.let { am ->
                val maxAlarmVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVol, 0)
            }
        } catch (_: Exception) {}

        activeJob = scope.launch(Dispatchers.Default) {
            var audioTrack: AudioTrack? = null
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = (minBufferSize * 2).coerceAtLeast(8192)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()

                if (mode == "MORSE_SOS") {
                    runMorseSosLoop(audioTrack, sampleRate)
                } else {
                    runPiercingSirenLoop(audioTrack, sampleRate)
                }

            } catch (_: Exception) {
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Exception) {}
                setFlashlight(false)
            }
        }
    }

    /**
     * Alternating dual high-pitch piercing siren (3,500 Hz and 2,900 Hz).
     * Alternates every 180ms with synchronized camera flash strobe.
     */
    private suspend fun kotlinx.coroutines.CoroutineScope.runPiercingSirenLoop(
        audioTrack: AudioTrack,
        sampleRate: Int
    ) {
        val frequencies = floatArrayOf(3500f, 2900f)
        var freqIndex = 0

        while (isActive) {
            val freq = frequencies[freqIndex % frequencies.size]
            freqIndex++

            // Turn Camera Flash ON during sound pulse
            setFlashlight(true)
            vibratePulse(160)

            // Play 180ms high-pitch tone
            playToneBurst(audioTrack, sampleRate, freq, durationMs = 180)

            // Quick 50ms strobe gap
            setFlashlight(false)
            playSilence(audioTrack, sampleRate, durationMs = 50)
        }
    }

    /**
     * International Morse Code SOS (... --- ...).
     * 3 short bursts, 3 long bursts, 3 short bursts with matching camera flash strobe.
     */
    private suspend fun kotlinx.coroutines.CoroutineScope.runMorseSosLoop(
        audioTrack: AudioTrack,
        sampleRate: Int
    ) {
        val sosFrequency = 3300f // 3.3 kHz high pitch

        while (isActive) {
            // S: 3 short bursts
            repeat(3) {
                if (!isActive) return
                setFlashlight(true)
                vibratePulse(150)
                playToneBurst(audioTrack, sampleRate, sosFrequency, durationMs = 150)
                setFlashlight(false)
                playSilence(audioTrack, sampleRate, durationMs = 120)
            }

            // Gap between letters
            playSilence(audioTrack, sampleRate, durationMs = 250)

            // O: 3 long bursts
            repeat(3) {
                if (!isActive) return
                setFlashlight(true)
                vibratePulse(400)
                playToneBurst(audioTrack, sampleRate, sosFrequency, durationMs = 450)
                setFlashlight(false)
                playSilence(audioTrack, sampleRate, durationMs = 150)
            }

            // Gap between letters
            playSilence(audioTrack, sampleRate, durationMs = 250)

            // S: 3 short bursts
            repeat(3) {
                if (!isActive) return
                setFlashlight(true)
                vibratePulse(150)
                playToneBurst(audioTrack, sampleRate, sosFrequency, durationMs = 150)
                setFlashlight(false)
                playSilence(audioTrack, sampleRate, durationMs = 120)
            }

            // Word pause between SOS cycles
            playSilence(audioTrack, sampleRate, durationMs = 800)
        }
    }

    private fun playToneBurst(
        audioTrack: AudioTrack,
        sampleRate: Int,
        frequencyHz: Float,
        durationMs: Int
    ) {
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val angularFrequency = 2.0 * PI * frequencyHz / sampleRate

        for (i in 0 until numSamples) {
            // Full scale high pitch sine wave
            val sample = sin(i * angularFrequency)
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        audioTrack.write(buffer, 0, buffer.size)
    }

    private fun playSilence(
        audioTrack: AudioTrack,
        sampleRate: Int,
        durationMs: Int
    ) {
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples) // zeros = silence
        audioTrack.write(buffer, 0, buffer.size)
    }

    private fun setFlashlight(enabled: Boolean) {
        if (isTorchOn == enabled) return
        val id = torchCameraId ?: return
        try {
            cameraManager?.setTorchMode(id, enabled)
            isTorchOn = enabled
        } catch (_: Exception) {}
    }

    private fun vibratePulse(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun stopSignal() {
        activeJob?.cancel()
        activeJob = null
        setFlashlight(false)
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }
}
