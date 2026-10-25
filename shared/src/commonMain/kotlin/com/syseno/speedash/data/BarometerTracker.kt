package com.syseno.speedash.data

import kotlinx.coroutines.flow.Flow

/**
 * Platform hardware barometer/pressure sensor source.
 *
 * Emits real-time atmospheric pressure or calculated barometric altitude above mean sea level (meters).
 *
 * - Android: `SensorManager` with `Sensor.TYPE_PRESSURE`.
 * - iOS: `CMAltimeter`.
 */
expect class BarometerTracker {

    /**
     * Whether a physical hardware barometer/pressure sensor is present on the host device.
     */
    val isAvailable: Boolean

    /**
     * Cold stream of barometric altitude in meters above sea level (mdpl / m ASL).
     *
     * Collection activates the sensor listener; cancellation unregisters the hardware sensor.
     */
    val altitudeFlow: Flow<Float>
}
