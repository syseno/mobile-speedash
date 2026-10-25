package com.syseno.speedash.domain.repository

import com.syseno.speedash.domain.model.LocationPermissionException
import com.syseno.speedash.domain.model.LocationUnavailableException
import com.syseno.speedash.domain.model.RawLocation
import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing source of raw GPS fixes. Implemented in the data layer.
 */
interface LocationRepository {

    /**
     * Cold stream of raw location fixes. Collection starts hardware updates; cancellation stops them.
     *
     * May complete exceptionally with [LocationPermissionException] or [LocationUnavailableException].
     */
    fun observeRawLocations(): Flow<RawLocation>
}
