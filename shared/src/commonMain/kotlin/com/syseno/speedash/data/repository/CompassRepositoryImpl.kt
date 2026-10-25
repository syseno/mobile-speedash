package com.syseno.speedash.data.repository

import com.syseno.speedash.data.CompassTracker
import com.syseno.speedash.domain.repository.CompassRepository
import kotlinx.coroutines.flow.Flow

/**
 * [CompassRepository] implementation delegating to the platform [CompassTracker].
 */
class CompassRepositoryImpl(
    private val compassTracker: CompassTracker
) : CompassRepository {

    override val isAvailable: Boolean
        get() = compassTracker.isAvailable

    override fun observeCompassHeading(): Flow<Float> = compassTracker.headingFlow
}
