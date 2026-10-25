package com.syseno.speedash.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VehicleDataTest {

    @Test
    fun rawGpsSignal_convertsMpsToKmhCorrectly() {
        val signal = RawGpsSignal(
            latitude = 10.0,
            longitude = 20.0,
            speedMps = 10.0f, // 10 m/s = 36 km/h
            timestampMillis = 1000L
        )
        assertEquals(36.0, signal.speedKmh, 0.001)
        assertTrue(signal.isValid)
    }

    @Test
    fun rawGpsSignal_validatesCoordinatesAndMps() {
        val invalidLat = RawGpsSignal(latitude = 95.0, longitude = 0.0, speedMps = 0f, timestampMillis = 1L)
        assertFalse(invalidLat.isValid)

        val invalidSpeed = RawGpsSignal(latitude = 0.0, longitude = 0.0, speedMps = -1f, timestampMillis = 1L)
        assertFalse(invalidSpeed.isValid)
    }

    @Test
    fun vehicleData_unitConversions() {
        val vehicle = VehicleData(
            latitude = 0.0,
            longitude = 0.0,
            speedKmh = 100.0,
            rawSpeedKmh = 100.0,
            bearing = 0f,
            timestampMillis = 1000L,
            isStationary = false
        )

        // 100 km/h = ~27.7778 m/s
        assertEquals(27.7778, vehicle.speedMps, 0.001)
        // 100 km/h = ~62.1371 mph
        assertEquals(62.1371, vehicle.speedMph, 0.001)
        // 100 km/h = ~53.9957 knots
        assertEquals(53.9957, vehicle.speedKnots, 0.001)
        // Rounded display speed
        assertEquals(100, vehicle.displaySpeedKmh)
    }

    @Test
    fun cardinalDirection_resolvesCorrectly() {
        assertEquals("N", VehicleData.getCompassDirection(0.0f))
        assertEquals("N", VehicleData.getCompassDirection(355.0f))
        assertEquals("NNE", VehicleData.getCompassDirection(22.5f))
        assertEquals("NE", VehicleData.getCompassDirection(45.0f))
        assertEquals("E", VehicleData.getCompassDirection(90.0f))
        assertEquals("SE", VehicleData.getCompassDirection(135.0f))
        assertEquals("S", VehicleData.getCompassDirection(180.0f))
        assertEquals("SW", VehicleData.getCompassDirection(225.0f))
        assertEquals("W", VehicleData.getCompassDirection(270.0f))
        assertEquals("NW", VehicleData.getCompassDirection(315.0f))
        assertEquals("NNW", VehicleData.getCompassDirection(337.5f))
    }
}
