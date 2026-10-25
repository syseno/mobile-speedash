package com.syseno.speedash.data.repository

import com.syseno.speedash.data.BarometerTracker
import com.syseno.speedash.domain.repository.BarometerRepository
import kotlinx.coroutines.flow.Flow

/**
 * [BarometerRepository] implementation delegating to platform [BarometerTracker].
 */
class BarometerRepositoryImpl(
    private val barometerTracker: BarometerTracker
) : BarometerRepository {

    override val isAvailable: Boolean
        get() = barometerTracker.isAvailable

    override fun observeAltitude(): Flow<Float> = barometerTracker.altitudeFlow
}
