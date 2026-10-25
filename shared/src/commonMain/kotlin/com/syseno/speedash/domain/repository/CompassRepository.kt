package com.syseno.speedash.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing source for physical device orientation from the built-in compass sensor.
 */
interface CompassRepository {

    /**
     * Whether physical hardware compass/magnetometer sensors are available on the device.
     */
    val isAvailable: Boolean

    /**
     * Observes real-time physical device compass heading in degrees [0.0f, 360.0f).
     */
    fun observeCompassHeading(): Flow<Float>
}
