package com.example

import com.example.location.SosLocation
import com.example.location.SosMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SosMessageTest {

    @Test
    fun `no fix never invents coordinates`() {
        val message = SosMessage.build(null)
        assertFalse(message.contains("maps.google.com"))
        assertFalse(message.contains("28.6139"))
        assertTrue(message.contains("could not get a GPS location"))
    }

    @Test
    fun `fresh fix is shared as current location`() {
        val message = SosMessage.build(SosLocation(12.97160, 77.59460, ageMinutes = 0))
        assertTrue(message.contains("My current GPS location"))
        assertTrue(message.contains("https://maps.google.com/?q=12.97160,77.59460"))
    }

    @Test
    fun `old fix is labelled with its age`() {
        val message = SosMessage.build(SosLocation(12.9716, 77.5946, ageMinutes = 45))
        assertTrue(message.contains("last known GPS location (45 min ago)"))
    }

    @Test
    fun `hemispheres follow the sign of the coordinate`() {
        assertEquals("33.86880° S", SosMessage.formatLatitude(-33.8688))
        assertEquals("151.20930° E", SosMessage.formatLongitude(151.2093))
        assertEquals("74.00600° W", SosMessage.formatLongitude(-74.006))
    }
}
