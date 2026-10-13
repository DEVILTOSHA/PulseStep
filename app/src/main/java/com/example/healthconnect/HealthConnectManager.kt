package com.example.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Pressure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

enum class HealthConnectStatus {
    CONNECTED,
    NEEDS_PERMISSIONS,
    NOT_INSTALLED,
    NOT_SUPPORTED
}

class HealthConnectManager(private val context: Context) {

    private val _syncStatus = MutableStateFlow(HealthConnectStatus.NEEDS_PERMISSIONS)
    val syncStatus: StateFlow<HealthConnectStatus> = _syncStatus.asStateFlow()

    private val _lastSyncMessage = MutableStateFlow<String?>("Health Connect ready to link")
    val lastSyncMessage: StateFlow<String?> = _lastSyncMessage.asStateFlow()

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getWritePermission(BloodPressureRecord::class)
    )

    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun checkAvailabilityAndPermissions(): HealthConnectStatus {
        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        val status = when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> {
                val client = healthConnectClient
                if (client != null) {
                    try {
                        val granted = client.permissionController.getGrantedPermissions()
                        if (granted.containsAll(permissions)) {
                            _lastSyncMessage.value = "Connected to Google Health Connect"
                            HealthConnectStatus.CONNECTED
                        } else {
                            _lastSyncMessage.value = "Tap to grant Health Connect permissions"
                            HealthConnectStatus.NEEDS_PERMISSIONS
                        }
                    } catch (e: Exception) {
                        _lastSyncMessage.value = "Permission check: ${e.localizedMessage}"
                        HealthConnectStatus.NEEDS_PERMISSIONS
                    }
                } else {
                    HealthConnectStatus.NOT_INSTALLED
                }
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                _lastSyncMessage.value = "Health Connect app update required"
                HealthConnectStatus.NOT_INSTALLED
            }
            else -> {
                _lastSyncMessage.value = "Health Connect not supported on device"
                HealthConnectStatus.NOT_SUPPORTED
            }
        }
        _syncStatus.value = status
        return status
    }

    /**
     * Writes one steps record covering [dayStart, end). Any steps this app already wrote for the same
     * day are deleted first (Health Connect only lets an app delete its own records), so re-syncing
     * replaces the day's total instead of adding to it.
     */
    suspend fun syncDailySteps(stepCount: Long, dayStart: Instant, dayEnd: Instant, end: Instant): Boolean {
        val client = healthConnectClient ?: return false
        if (stepCount <= 0L || !end.isAfter(dayStart)) {
            _lastSyncMessage.value = "No steps to sync yet today"
            return false
        }
        return try {
            client.deleteRecords(StepsRecord::class, TimeRangeFilter.between(dayStart, dayEnd))
            val record = StepsRecord(
                count = stepCount,
                startTime = dayStart,
                startZoneOffset = localOffset(dayStart),
                endTime = end,
                endZoneOffset = localOffset(end)
            )
            client.insertRecords(listOf(record))
            _lastSyncMessage.value = "Synced $stepCount steps to Health Connect"
            true
        } catch (e: Exception) {
            _lastSyncMessage.value = "Step sync: ${e.localizedMessage ?: "Permission required"}"
            false
        }
    }

    suspend fun syncBloodPressureRecord(
        systolicMmHg: Double,
        diastolicMmHg: Double,
        time: Instant
    ): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val record = BloodPressureRecord(
                time = time,
                zoneOffset = localOffset(time),
                systolic = Pressure.millimetersOfMercury(systolicMmHg),
                diastolic = Pressure.millimetersOfMercury(diastolicMmHg)
            )
            client.insertRecords(listOf(record))
            _lastSyncMessage.value = "Synced BP ${systolicMmHg.toInt()}/${diastolicMmHg.toInt()} to Health Connect"
            true
        } catch (e: Exception) {
            _lastSyncMessage.value = "BP sync: ${e.localizedMessage ?: "Permission required"}"
            false
        }
    }

    suspend fun syncHeartRateRecord(bpm: Long, time: Instant): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val sample = HeartRateRecord.Sample(
                time = time,
                beatsPerMinute = bpm
            )
            val record = HeartRateRecord(
                startTime = time.minusSeconds(1),
                startZoneOffset = localOffset(time.minusSeconds(1)),
                endTime = time,
                endZoneOffset = localOffset(time),
                samples = listOf(sample)
            )
            client.insertRecords(listOf(record))
            _lastSyncMessage.value = "Synced HR $bpm bpm to Health Connect"
            true
        } catch (e: Exception) {
            _lastSyncMessage.value = "HR sync: ${e.localizedMessage ?: "Permission required"}"
            false
        }
    }

    private fun localOffset(instant: Instant): ZoneOffset = ZoneId.systemDefault().rules.getOffset(instant)
}
