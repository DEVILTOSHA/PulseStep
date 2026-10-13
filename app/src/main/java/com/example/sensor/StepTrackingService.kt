package com.example.sensor

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.StepRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.NumberFormat

/**
 * Keeps the pedometer listening while the app is in the background or closed.
 * Runs as a `health` foreground service, which requires ACTIVITY_RECOGNITION on Android 10+.
 */
class StepTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var stepRecorder: StepRecorder
    private lateinit var pedometer: PedometerSensorManager

    override fun onCreate() {
        super.onCreate()
        stepRecorder = StepRecorder.getInstance(this)
        pedometer = PedometerSensorManager(this) { delta ->
            scope.launch {
                val total = stepRecorder.recordSteps(delta)
                updateNotification(total)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(null),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                } else {
                    0
                }
            )
        } catch (_: Exception) {
            // Permission revoked or background-start restriction: give up quietly instead of crashing.
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch { stepRecorder.ensureTodayRecord() }
        pedometer.startListening()
        return START_STICKY
    }

    override fun onDestroy() {
        pedometer.stopListening()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification(todaySteps: Int) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification(todaySteps))
    }

    private fun buildNotification(todaySteps: Int?): Notification {
        ensureChannel()
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = if (todaySteps == null) {
            "Counting your steps"
        } else {
            "${NumberFormat.getIntegerInstance().format(todaySteps)} steps today"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle("PulseStep")
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Step tracking", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shows while PulseStep counts steps in the background"
                }
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "step_tracking"
        private const val NOTIFICATION_ID = 1001

        fun hasRequiredPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED

        /** Starts tracking. Returns false (and does nothing) when activity-recognition permission is missing. */
        fun start(context: Context): Boolean {
            if (!hasRequiredPermission(context)) return false
            return try {
                ContextCompat.startForegroundService(context, Intent(context, StepTrackingService::class.java))
                true
            } catch (_: Exception) {
                false
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StepTrackingService::class.java))
        }
    }
}
