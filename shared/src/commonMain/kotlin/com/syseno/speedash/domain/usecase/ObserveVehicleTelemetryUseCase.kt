package com.syseno.speedash.domain.usecase

import com.syseno.speedash.domain.model.VehicleData
import com.syseno.speedash.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Composes the raw location source with the signal-processing pipeline.
 *
 * `LocationRepository.observeRawLocations()` → [ProcessGpsSignalUseCase] → `Flow<VehicleData>`
 *
 * Every invocation creates an independent, cold pipeline (fresh EMA state and distance counter).
 */
class ObserveVehicleTelemetryUseCase(
    private val locationRepository: LocationRepository,
    private val processGpsSignal: ProcessGpsSignalUseCase
) {
    operator fun invoke(): Flow<VehicleData> = processGpsSignal(locationRepository.observeRawLocations())
}
