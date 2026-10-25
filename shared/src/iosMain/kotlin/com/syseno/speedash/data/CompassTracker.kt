package com.syseno.speedash.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.darwin.NSObject

/**
 * iOS implementation of [CompassTracker] backed by [CLLocationManager]'s heading services.
 */
actual class CompassTracker {

    actual val isAvailable: Boolean
        get() = CLLocationManager.headingAvailable()

    @OptIn(ExperimentalForeignApi::class)
    actual val headingFlow: Flow<Float> = callbackFlow {
        if (!CLLocationManager.headingAvailable()) {
            close()
            return@callbackFlow
        }

        val manager = CLLocationManager().apply {
            headingFilter = 0.5 // Update every 0.5 degrees
        }

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
                // Prefer true heading if calibrated; fall back to magnetic heading
                val heading = if (didUpdateHeading.trueHeading >= 0.0) {
                    didUpdateHeading.trueHeading
                } else {
                    didUpdateHeading.magneticHeading
                }
                if (heading >= 0.0) {
                    trySend(heading.toFloat())
                }
            }
        }

        manager.delegate = delegate
        manager.startUpdatingHeading()

        awaitClose {
            manager.stopUpdatingHeading()
            manager.delegate = null
        }
    }
        .buffer(capacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        .flowOn(Dispatchers.Main)
}
