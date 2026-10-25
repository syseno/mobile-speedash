package com.syseno.speedash.data.repository

import com.syseno.speedash.domain.model.TripRecord
import com.syseno.speedash.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory implementation of [TripRepository] managed via Koin.
 */
class TripRepositoryImpl : TripRepository {
    private val _trips = MutableStateFlow<List<TripRecord>>(emptyList())
    override val trips: StateFlow<List<TripRecord>> = _trips.asStateFlow()

    override fun saveTrip(trip: TripRecord) {
        _trips.update { current -> listOf(trip) + current }
    }

    override fun getTrips(): List<TripRecord> {
        return _trips.value
    }

    override fun deleteTrip(id: String) {
        _trips.update { current -> current.filter { it.id != id } }
    }
}
