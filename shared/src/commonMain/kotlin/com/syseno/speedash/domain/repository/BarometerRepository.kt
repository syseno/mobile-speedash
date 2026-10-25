package com.syseno.speedash.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing source for physical device barometric altitude from the built-in pressure sensor.
 */
interface BarometerRepository {

    /**
     * Whether a physical hardware barometer/pressure sensor is available on the device.
     */
    val isAvailable: Boolean

    /**
     * Observes real-time physical device barometric altitude in meters above mean sea level.
     */
    fun observeAltitude(): Flow<Float>
}
