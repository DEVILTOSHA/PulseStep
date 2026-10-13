package com.example.location

import java.util.Locale
import kotlin.math.abs

/** A real location fix to put in an SOS message. [ageMinutes] is how old the fix is. */
data class SosLocation(val latitude: Double, val longitude: Double, val ageMinutes: Long)

object SosMessage {
    /** Fixes older than this are labelled as "last known" rather than current. */
    const val STALE_AFTER_MINUTES = 2L

    /**
     * Builds the distress text. With no fix it says so plainly — it must never invent coordinates,
     * because responders would go to the wrong place.
     */
    fun build(location: SosLocation?): String {
        if (location == null) {
            return "EMERGENCY SOS: Urgent help needed! My phone could not get a GPS location — " +
                "please call me and ask where I am. Sent via PulseStep."
        }
        val coords = formatCoordinates(location.latitude, location.longitude)
        val mapsUrl = "https://maps.google.com/?q=${"%.5f".format(Locale.US, location.latitude)}," +
            "%.5f".format(Locale.US, location.longitude)
        val label = if (location.ageMinutes > STALE_AFTER_MINUTES) {
            "My last known GPS location (${location.ageMinutes} min ago)"
        } else {
            "My current GPS location"
        }
        return "EMERGENCY SOS: Urgent help needed! $label: $mapsUrl ($coords). Sent via PulseStep."
    }

    fun formatLatitude(lat: Double): String =
        "%.5f° %s".format(Locale.US, abs(lat), if (lat >= 0) "N" else "S")

    fun formatLongitude(lng: Double): String =
        "%.5f° %s".format(Locale.US, abs(lng), if (lng >= 0) "E" else "W")

    private fun formatCoordinates(lat: Double, lng: Double): String =
        "${formatLatitude(lat)}, ${formatLongitude(lng)}"
}
