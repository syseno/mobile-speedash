package com.syseno.speedash.data.repository

import com.syseno.speedash.data.GpsTracker
import com.syseno.speedash.domain.model.RawLocation
import com.syseno.speedash.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow

/**
 * [LocationRepository] backed by the platform [GpsTracker].
 */
class LocationRepositoryImpl(
    private val gpsTracker: GpsTracker
) : LocationRepository {

    override fun observeRawLocations(): Flow<RawLocation> = gpsTracker.locationFlow
}
