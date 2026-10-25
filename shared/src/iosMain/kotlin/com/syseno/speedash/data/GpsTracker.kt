package com.syseno.speedash.data

import com.syseno.speedash.domain.model.LocationPermissionException
import com.syseno.speedash.domain.model.LocationUnavailableException
import com.syseno.speedash.domain.model.RawLocation
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreLocation.CLActivityTypeAutomotiveNavigation
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLDistanceFilterNone
import platform.CoreLocation.kCLErrorDenied
import platform.CoreLocation.kCLErrorLocationUnknown
import platform.CoreLocation.kCLLocationAccuracyBestForNavigation
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

/**
 * iOS GPS source backed by [CLLocationManager] with [kCLLocationAccuracyBestForNavigation].
 *
 * - Requests "When In Use" authorization automatically on first collection if undetermined.
 * - Runs on [Dispatchers.Main] because `CLLocationManager` must live on a thread with a run loop.
 * - Requires `NSLocationWhenInUseUsageDescription` in the host app's `Info.plist`.
 */
actual class GpsTracker {

    @OptIn(ExperimentalForeignApi::class)
    actual val locationFlow: Flow<RawLocation> = callbackFlow {
        val manager = CLLocationManager().apply {
            desiredAccuracy = kCLLocationAccuracyBestForNavigation
            distanceFilter = kCLDistanceFilterNone
            activityType = CLActivityTypeAutomotiveNavigation
            pausesLocationUpdatesAutomatically = false
        }

        var isUpdating = false

        fun handleAuthorization(status: CLAuthorizationStatus) {
            when (status) {
                kCLAuthorizationStatusAuthorizedWhenInUse,
                kCLAuthorizationStatusAuthorizedAlways -> {
                    if (!isUpdating) {
                        manager.startUpdatingLocation()
                        isUpdating = true
                    }
                }

                kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()

                kCLAuthorizationStatusDenied,
                kCLAuthorizationStatusRestricted -> {
                    close(LocationPermissionException("Location authorization denied or restricted (status=$status)."))
                }

                else -> Unit
            }
        }

        // CLLocationManager.delegate is a weak reference — this local keeps the delegate alive
        // until awaitClose releases it.
        val delegate = LocationDelegate(
            onLocations = { locations ->
                locations.forEach { location ->
                    location.toRawLocationOrNull()?.let { trySend(it) }
                }
            },
            onAuthorizationChanged = ::handleAuthorization,
            onError = { error ->
                when (error.code) {
                    // Transient: CoreLocation keeps trying; do not terminate the stream.
                    kCLErrorLocationUnknown.toLong() -> Unit
                    kCLErrorDenied.toLong() ->
                        close(LocationPermissionException("Location access denied: ${error.localizedDescription}"))
                    else ->
                        close(LocationUnavailableException("CoreLocation error ${error.code}: ${error.localizedDescription}"))
                }
            }
        )

        manager.delegate = delegate
        // Setting the delegate triggers locationManagerDidChangeAuthorization on iOS 14+,
        // but handle the current status explicitly for determinism.
        handleAuthorization(manager.authorizationStatus)

        awaitClose {
            manager.stopUpdatingLocation()
            // Referencing `delegate` here is what actually keeps it alive (see comment above).
            if (manager.delegate === delegate) {
                manager.delegate = null
            }
        }
    }
        .buffer(capacity = BUFFER_CAPACITY, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        .flowOn(Dispatchers.Main)

    private companion object {
        const val BUFFER_CAPACITY: Int = 16
    }
}

/**
 * Maps a [CLLocation] to [RawLocation]. Returns `null` for invalid fixes
 * (negative `horizontalAccuracy` means CoreLocation could not determine the position).
 */
@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toRawLocationOrNull(): RawLocation? {
    if (horizontalAccuracy < 0.0) return null

    val (lat, lon) = coordinate.useContents { latitude to longitude }

    return RawLocation(
        latitude = lat,
        longitude = lon,
        speedMps = if (speed >= 0.0) speed.toFloat() else 0f,      // speed < 0 ⇒ invalid
        bearing = if (course >= 0.0) course.toFloat() else null,    // course < 0 ⇒ invalid
        altitudeMeters = if (verticalAccuracy >= 0.0) altitude else null,
        accuracyMeters = horizontalAccuracy.toFloat(),
        timestampMillis = (timestamp.timeIntervalSince1970 * 1000.0).toLong()
    )
}

/**
 * Bridges Objective-C delegate callbacks into Kotlin lambdas.
 * Callbacks arrive on the thread that created the manager (main).
 */
private class LocationDelegate(
    private val onLocations: (List<CLLocation>) -> Unit,
    private val onAuthorizationChanged: (CLAuthorizationStatus) -> Unit,
    private val onError: (NSError) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        onLocations(didUpdateLocations.filterIsInstance<CLLocation>())
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onError(didFailWithError)
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        onAuthorizationChanged(manager.authorizationStatus)
    }
}
