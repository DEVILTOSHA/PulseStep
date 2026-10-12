package com.example.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.units.Pressure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

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
                            _lastSyncMessage.value = "Synced with Google Health Connect"
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

    suspend fun syncStepsRecord(stepCount: Long, startTime: Instant, endTime: Instant): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val record = StepsRecord(
                count = stepCount,
                startTime = startTime,
                startZoneOffset = ZoneOffset.UTC,
                endTime = endTime,
                endZoneOffset = ZoneOffset.UTC
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
                zoneOffset = ZoneOffset.UTC,
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
                startZoneOffset = ZoneOffset.UTC,
                endTime = time,
                endZoneOffset = ZoneOffset.UTC,
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

    fun markSimulatedSyncSuccess() {
        _syncStatus.value = HealthConnectStatus.CONNECTED
        _lastSyncMessage.value = "All records synced with Google Health Connect"
    }
}
