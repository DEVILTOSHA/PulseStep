package com.example.data.local

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Date keys for [DailyStepRecord], always derived from the device's current local date. */
object DateKeys {
    /** "yyyy-MM-dd" for the current local day. Re-evaluate on every use; never cache across midnight. */
    fun today(zone: ZoneId = ZoneId.systemDefault()): String = LocalDate.now(zone).toString()

    fun startOfDay(dateKey: String, zone: ZoneId = ZoneId.systemDefault()): Instant =
        LocalDate.parse(dateKey).atStartOfDay(zone).toInstant()

    fun startOfNextDay(dateKey: String, zone: ZoneId = ZoneId.systemDefault()): Instant =
        LocalDate.parse(dateKey).plusDays(1).atStartOfDay(zone).toInstant()
}
