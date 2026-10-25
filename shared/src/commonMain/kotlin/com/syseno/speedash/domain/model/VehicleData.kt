package com.syseno.speedash.domain.model

import kotlin.math.roundToInt

/**
 * Immutable raw GPS fix received directly from the platform's location service
 * (Android FusedLocationProviderClient / iOS CLLocationManager).
 *
 * @property latitude Latitude in decimal degrees (WGS 84, -90.0 .. 90.0).
 * @property longitude Longitude in decimal degrees (WGS 84, -180.0 .. 180.0).
 * @property speedMps Instantaneous ground speed in meters per second (m/s).
 * @property bearing Optional hardware track bearing in degrees (0.0 .. 359.9).
 * @property altitudeMeters Altitude in meters above the WGS 84 ellipsoid, if available.
 * @property accuracyMeters Estimated 68% confidence horizontal accuracy radius in meters.
 * @property timestampMillis Epoch timestamp in milliseconds when fix was acquired.
 */
data class RawGpsSignal(
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float,
    val bearing: Float? = null,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Float = 0.0f,
    val timestampMillis: Long
) {
    /**
     * Raw instantaneous speed converted from meters per second to kilometers per hour (km/h).
     */
    val speedKmh: Double
        get() = (speedMps * 3.6).coerceAtLeast(0.0)

    /**
     * Validates coordinate bounds, non-negative speed, and accuracy metrics.
     */
    val isValid: Boolean
        get() = latitude in -90.0..90.0 &&
                longitude in -180.0..180.0 &&
                speedMps >= 0.0f &&
                accuracyMeters >= 0.0f &&
                timestampMillis > 0L
}

/**
 * Immutable domain telemetry model representing the processed, filtered vehicle status.
 *
 * @property latitude Current smoothed or validated latitude in decimal degrees.
 * @property longitude Current smoothed or validated longitude in decimal degrees.
 * @property speedKmh EMA-smoothed speed in km/h, filtered by the noise gate (< 3.0 km/h = 0).
 * @property rawSpeedKmh Unfiltered instantaneous speed in km/h from the current GPS fix.
 * @property bearing Stabilized forward course / azimuth heading in degrees (0.0 .. 359.9).
 * @property altitudeMeters Ellipsoidal altitude in meters, or null if unavailable.
 * @property accuracyMeters Estimated horizontal accuracy radius in meters.
 * @property timestampMillis Epoch timestamp in milliseconds.
 * @property isStationary True if the vehicle speed is clamped to zero by the noise gate.
 * @property distanceDeltaMeters Great-circle distance in meters traversed since previous fix.
 * @property totalDistanceMeters Cumulative distance in meters traveled during the tracking session.
 */
data class VehicleData(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double,
    val rawSpeedKmh: Double,
    val bearing: Float,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Float = 0.0f,
    val timestampMillis: Long,
    val isStationary: Boolean,
    val distanceDeltaMeters: Double = 0.0,
    val totalDistanceMeters: Double = 0.0
) {
    /** Speed in meters per second (m/s). */
    val speedMps: Double get() = speedKmh / 3.6

    /** Speed in statute miles per hour (mph). */
    val speedMph: Double get() = speedKmh * KMH_TO_MPH

    /** Speed in nautical miles per hour / knots (kn). */
    val speedKnots: Double get() = speedKmh * KMH_TO_KNOTS

    /** Integer-rounded speed for high-performance dashboard rendering. */
    val displaySpeedKmh: Int get() = speedKmh.roundToInt()

    /** 16-point compass cardinal/intercardinal direction code (e.g. N, NNE, NE). */
    val cardinalDirection: String get() = getCompassDirection(bearing)

    companion object {
        private const val KMH_TO_MPH = 0.62137119223733
        private const val KMH_TO_KNOTS = 0.53995680345572

        private val CARDINAL_POINTS = arrayOf(
            "N", "NNE", "NE", "ENE",
            "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW",
            "W", "WNW", "NW", "NNW"
        )

        /**
         * Initial resting vehicle state before first GPS acquisition.
         */
        val INITIAL = VehicleData(
            latitude = 0.0,
            longitude = 0.0,
            speedKmh = 0.0,
            rawSpeedKmh = 0.0,
            bearing = 0.0f,
            altitudeMeters = null,
            accuracyMeters = 0.0f,
            timestampMillis = 0L,
            isStationary = true,
            distanceDeltaMeters = 0.0,
            totalDistanceMeters = 0.0
        )

        /**
         * Converts heading angle in degrees (0.0 .. 359.9) to a 16-point compass heading abbreviation.
         */
        fun getCompassDirection(bearing: Float): String {
            val normalized = ((bearing % 360f) + 360f) % 360f
            val index = (((normalized + 11.25f) / 22.5f).toInt()) % 16
            return CARDINAL_POINTS[index]
        }
    }
}
