package com.syseno.speedash.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
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
            desiredAccuracy = kCLLocationAccuracyBest
            headingFilter = 0.5 // Update every 0.5 degrees
        }

        var isUpdating = false

        fun handleAuthorization(status: CLAuthorizationStatus) {
            when (status) {
                kCLAuthorizationStatusAuthorizedWhenInUse,
                kCLAuthorizationStatusAuthorizedAlways -> {
                    if (!isUpdating) {
                        // iOS requires location updates to be running for continuous trueHeading/heading delivery
                        manager.startUpdatingLocation()
                        manager.startUpdatingHeading()
                        isUpdating = true
                    }
                }

                kCLAuthorizationStatusNotDetermined -> {
                    manager.requestWhenInUseAuthorization()
                }

                else -> Unit
            }
        }

        val delegate = CompassDelegate(
            onHeadingUpdated = { heading ->
                val headingVal = if (heading.trueHeading >= 0.0) {
                    heading.trueHeading
                } else {
                    heading.magneticHeading
                }
                if (headingVal >= 0.0) {
                    trySend(headingVal.toFloat())
                }
            },
            authorizationChanged = ::handleAuthorization
        )

        // CLLocationManager.delegate is a WEAK reference. The delegate must be referenced inside
        // awaitClose so Kotlin/Native keeps it in the suspended coroutine's state; otherwise the
        // GC collects it shortly after launch and heading callbacks silently stop.
        manager.delegate = delegate
        handleAuthorization(manager.authorizationStatus)

        awaitClose {
            if (isUpdating) {
                manager.stopUpdatingHeading()
                manager.stopUpdatingLocation()
            }
            if (manager.delegate === delegate) {
                manager.delegate = null
            }
        }
    }
        .buffer(capacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        .flowOn(Dispatchers.Main)
}

private class CompassDelegate(
    private val onHeadingUpdated: (CLHeading) -> Unit,
    private val authorizationChanged: (CLAuthorizationStatus) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
        onHeadingUpdated(didUpdateHeading)
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        authorizationChanged(manager.authorizationStatus)
    }

    override fun locationManagerShouldDisplayHeadingCalibration(manager: CLLocationManager): Boolean {
        return true
    }
}
