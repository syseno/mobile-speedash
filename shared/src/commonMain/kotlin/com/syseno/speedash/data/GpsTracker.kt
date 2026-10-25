package com.syseno.speedash.data

import com.syseno.speedash.domain.model.LocationPermissionException
import com.syseno.speedash.domain.model.LocationUnavailableException
import com.syseno.speedash.domain.model.RawLocation
import kotlinx.coroutines.flow.Flow

/**
 * Platform GPS source.
 *
 * - Android: `FusedLocationProviderClient`, `PRIORITY_HIGH_ACCURACY`, 250 ms interval.
 * - iOS: `CLLocationManager`, `kCLLocationAccuracyBestForNavigation`.
 *
 * Constructors are platform-specific (Android requires a `Context`), so instances must be
 * created in platform code / platform Koin modules, not in `commonMain`.
 */
expect class GpsTracker {

    /**
     * Cold stream of raw location fixes.
     *
     * - Collecting starts hardware location updates; cancelling the collector stops them.
     * - Each collector gets its own registration — use `shareIn` upstream if several consumers
     *   need the same stream.
     * - Completes exceptionally with [LocationPermissionException] if permission is missing/denied,
     *   or [LocationUnavailableException] if the platform provider fails. Re-collect after the
     *   user grants permission.
     */
    val locationFlow: Flow<RawLocation>
}
