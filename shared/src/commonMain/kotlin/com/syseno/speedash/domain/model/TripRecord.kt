package com.syseno.speedash.domain.model

import androidx.compose.runtime.Immutable

/**
 * Immutable domain model representing a recorded trip session.
 *
 * @property id Unique identifier for the trip.
 * @property name User-assigned name for the trip.
 * @property startTimeMillis Epoch timestamp when the trip started.
 * @property endTimeMillis Epoch timestamp when the trip ended.
 * @property durationMillis Total duration of the trip in milliseconds.
 * @property distanceKm Total distance traveled in kilometers.
 * @property avgSpeedKmh Average speed during the trip in km/h.
 * @property startLat Starting latitude coordinate.
 * @property startLon Starting longitude coordinate.
 * @property endLat Ending latitude coordinate.
 * @property endLon Ending longitude coordinate.
 */
@Immutable
data class TripRecord(
    val id: String,
    val name: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long,
    val distanceKm: Double,
    val avgSpeedKmh: Double,
    val startLat: Double,
    val startLon: Double,
    val endLat: Double,
    val endLon: Double
)
