package com.syseno.speedash.data

import kotlinx.coroutines.flow.Flow

/**
 * Platform hardware compass/magnetometer sensor source.
 *
 * Emits real-time compass heading in degrees [0.0f, 360.0f) representing the physical
 * direction the device is facing, compensating for display rotation.
 *
 * - Android: `SensorManager` with `Sensor.TYPE_ROTATION_VECTOR` (fused sensor).
 * - iOS: `CLLocationManager` with `startUpdatingHeading()`.
 */
expect class CompassTracker {

    /**
     * Whether physical hardware compass/magnetometer sensors are present on the host device.
     */
    val isAvailable: Boolean

    /**
     * Cold stream of device compass heading in degrees [0.0f, 360.0f).
     *
     * Collection activates the sensor listener; cancellation unregisters the hardware sensor.
     */
    val headingFlow: Flow<Float>
}
