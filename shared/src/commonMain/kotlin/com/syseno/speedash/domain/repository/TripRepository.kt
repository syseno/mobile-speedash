package com.syseno.speedash.domain.repository

import com.syseno.speedash.domain.model.TripRecord
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository contract for managing recorded trips.
 */
interface TripRepository {
    val trips: StateFlow<List<TripRecord>>
    fun saveTrip(trip: TripRecord)
    fun getTrips(): List<TripRecord>
    fun deleteTrip(id: String)
}
