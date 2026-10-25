package com.syseno.speedash.domain.usecase

import com.syseno.speedash.domain.model.RawGpsSignal
import com.syseno.speedash.domain.model.VehicleData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Transforms a cold stream of raw platform GPS fixes into stabilized [VehicleData].
 *
 * Pipeline stages (per emission):
 * 1. **Validation** – drops malformed fixes, low-accuracy fixes and out-of-order timestamps.
 * 2. **EMA smoothing** – `S_t = α · X_t + (1 − α) · S_(t−1)` with `α = 0.25`.
 * 3. **Noise gate** – smoothed speeds below `3.0 km/h` are clamped to `0.0`.
 * 4. **Haversine geodesy** – great-circle distance and initial (forward-azimuth) bearing
 *    between consecutive fixes. Bearing is only updated when the vehicle is moving and has
 *    travelled at least [Config.minBearingDistanceMeters], otherwise the last bearing is held
 *    to prevent compass jitter while stationary.
 *
 * The use case is stateless between invocations: every call to [invoke] creates an
 * independent pipeline whose state lives entirely inside the returned [Flow].
 *
 * @param config Tunable filter parameters.
 * @param dispatcher Dispatcher used for the math-heavy pipeline (defaults to [Dispatchers.Default]).
 */
class ProcessGpsSignalUseCase(
    private val config: Config = Config(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    /**
     * Filter configuration.
     *
     * @property emaAlpha EMA smoothing factor in (0.0, 1.0]. Higher = more responsive, lower = smoother.
     * @property noiseGateKmh Speeds strictly below this threshold are reported as 0 km/h.
     * @property maxAccuracyMeters Fixes with a worse horizontal accuracy are discarded. `0` or negative disables the check.
     * @property minBearingDistanceMeters Minimum displacement before the bearing is recomputed.
     */
    data class Config(
        val emaAlpha: Double = DEFAULT_EMA_ALPHA,
        val noiseGateKmh: Double = DEFAULT_NOISE_GATE_KMH,
        val maxAccuracyMeters: Float = DEFAULT_MAX_ACCURACY_METERS,
        val minBearingDistanceMeters: Double = DEFAULT_MIN_BEARING_DISTANCE_METERS
    ) {
        init {
            require(emaAlpha > 0.0 && emaAlpha <= 1.0) { "emaAlpha must be in (0.0, 1.0], was $emaAlpha" }
            require(noiseGateKmh >= 0.0) { "noiseGateKmh must be >= 0.0, was $noiseGateKmh" }
            require(minBearingDistanceMeters >= 0.0) {
                "minBearingDistanceMeters must be >= 0.0, was $minBearingDistanceMeters"
            }
        }
    }

    /**
     * Internal accumulator carried through [scan]. Never exposed outside this class.
     */
    private data class PipelineState(
        val emaSpeedKmh: Double,
        val lastLatitude: Double,
        val lastLongitude: Double,
        val lastBearingAnchorLatitude: Double,
        val lastBearingAnchorLongitude: Double,
        val lastBearing: Float,
        val lastTimestampMillis: Long,
        val totalDistanceMeters: Double,
        val output: VehicleData
    )

    /**
     * Builds the reactive pipeline.
     *
     * @param signals Upstream raw GPS fixes (typically a `callbackFlow` from the platform data source).
     * @return A cold [Flow] of processed [VehicleData], conflation-friendly and distinct.
     */
    operator fun invoke(signals: Flow<RawGpsSignal>): Flow<VehicleData> =
        signals
            .filter(::isAcceptable)
            .scan<RawGpsSignal, PipelineState?>(initial = null) { state, signal ->
                if (state == null) initialState(signal) else nextState(state, signal)
            }
            .filterNotNull()
            .map { it.output }
            .distinctUntilChanged()
            .flowOn(dispatcher)

    // ---------------------------------------------------------------------------------------------
    // Stage 1 — Validation
    // ---------------------------------------------------------------------------------------------

    private fun isAcceptable(signal: RawGpsSignal): Boolean {
        if (!signal.isValid) return false
        if (signal.latitude.isNaN() || signal.longitude.isNaN() || signal.speedMps.isNaN()) return false
        if (config.maxAccuracyMeters > 0f && signal.accuracyMeters > config.maxAccuracyMeters) return false
        return true
    }

    // ---------------------------------------------------------------------------------------------
    // Stage 2-4 — State transitions
    // ---------------------------------------------------------------------------------------------

    private fun initialState(signal: RawGpsSignal): PipelineState {
        val rawKmh = signal.speedKmh
        val gatedKmh = applyNoiseGate(rawKmh, config.noiseGateKmh)
        val bearing = signal.bearing?.let(::normalizeBearing) ?: 0.0f

        return PipelineState(
            emaSpeedKmh = rawKmh,
            lastLatitude = signal.latitude,
            lastLongitude = signal.longitude,
            lastBearingAnchorLatitude = signal.latitude,
            lastBearingAnchorLongitude = signal.longitude,
            lastBearing = bearing,
            lastTimestampMillis = signal.timestampMillis,
            totalDistanceMeters = 0.0,
            output = VehicleData(
                latitude = signal.latitude,
                longitude = signal.longitude,
                speedKmh = gatedKmh,
                rawSpeedKmh = rawKmh,
                bearing = bearing,
                altitudeMeters = signal.altitudeMeters,
                accuracyMeters = signal.accuracyMeters,
                timestampMillis = signal.timestampMillis,
                isStationary = gatedKmh == 0.0,
                distanceDeltaMeters = 0.0,
                totalDistanceMeters = 0.0
            )
        )
    }

    private fun nextState(state: PipelineState, signal: RawGpsSignal): PipelineState {
        // Drop duplicate / out-of-order fixes by re-emitting the unchanged state
        // (filtered out downstream by distinctUntilChanged).
        if (signal.timestampMillis <= state.lastTimestampMillis) return state

        // Stage 2 — EMA smoothing (state keeps the un-gated EMA so decay remains continuous).
        val rawKmh = signal.speedKmh
        val emaKmh = exponentialMovingAverage(rawKmh, state.emaSpeedKmh, config.emaAlpha)

        // Stage 3 — Noise gate.
        val gatedKmh = applyNoiseGate(emaKmh, config.noiseGateKmh)
        val isStationary = gatedKmh == 0.0

        // Stage 4a — Haversine distance. Ignore displacement while gated (GPS drift).
        val rawDelta = haversineDistanceMeters(
            state.lastLatitude, state.lastLongitude,
            signal.latitude, signal.longitude
        )
        val distanceDelta = if (isStationary) 0.0 else rawDelta
        val totalDistance = state.totalDistanceMeters + distanceDelta

        // Stage 4b — Bearing, computed from an anchor point so short hops don't cause jitter.
        val anchorDistance = haversineDistanceMeters(
            state.lastBearingAnchorLatitude, state.lastBearingAnchorLongitude,
            signal.latitude, signal.longitude
        )
        val shouldUpdateBearing = !isStationary && anchorDistance >= config.minBearingDistanceMeters
        val bearing: Float
        val anchorLat: Double
        val anchorLon: Double
        if (shouldUpdateBearing) {
            bearing = haversineBearingDegrees(
                state.lastBearingAnchorLatitude, state.lastBearingAnchorLongitude,
                signal.latitude, signal.longitude
            ).toFloat()
            anchorLat = signal.latitude
            anchorLon = signal.longitude
        } else {
            bearing = state.lastBearing
            // While stationary, keep the anchor glued to the current position so that
            // drift accumulated during the stop never produces a bogus heading on departure.
            anchorLat = if (isStationary) signal.latitude else state.lastBearingAnchorLatitude
            anchorLon = if (isStationary) signal.longitude else state.lastBearingAnchorLongitude
        }

        return PipelineState(
            emaSpeedKmh = emaKmh,
            lastLatitude = signal.latitude,
            lastLongitude = signal.longitude,
            lastBearingAnchorLatitude = anchorLat,
            lastBearingAnchorLongitude = anchorLon,
            lastBearing = bearing,
            lastTimestampMillis = signal.timestampMillis,
            totalDistanceMeters = totalDistance,
            output = VehicleData(
                latitude = signal.latitude,
                longitude = signal.longitude,
                speedKmh = gatedKmh,
                rawSpeedKmh = rawKmh,
                bearing = bearing,
                altitudeMeters = signal.altitudeMeters,
                accuracyMeters = signal.accuracyMeters,
                timestampMillis = signal.timestampMillis,
                isStationary = isStationary,
                distanceDeltaMeters = distanceDelta,
                totalDistanceMeters = totalDistance
            )
        )
    }

    companion object {
        const val DEFAULT_EMA_ALPHA: Double = 0.25
        const val DEFAULT_NOISE_GATE_KMH: Double = 3.0
        const val DEFAULT_MAX_ACCURACY_METERS: Float = 50.0f
        const val DEFAULT_MIN_BEARING_DISTANCE_METERS: Double = 2.0

        /** Mean Earth radius (IUGG) in meters. */
        const val EARTH_RADIUS_METERS: Double = 6_371_008.8

        private const val DEG_TO_RAD: Double = kotlin.math.PI / 180.0
        private const val RAD_TO_DEG: Double = 180.0 / kotlin.math.PI

        /** `S_t = α · X_t + (1 − α) · S_(t−1)` */
        fun exponentialMovingAverage(current: Double, previous: Double, alpha: Double): Double =
            alpha * current + (1.0 - alpha) * previous

        /** Returns `0.0` if [speedKmh] is strictly below [thresholdKmh], else [speedKmh]. */
        fun applyNoiseGate(speedKmh: Double, thresholdKmh: Double): Double =
            if (speedKmh < thresholdKmh) 0.0 else speedKmh

        /** Great-circle distance in meters using the Haversine formula. */
        fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val phi1 = lat1 * DEG_TO_RAD
            val phi2 = lat2 * DEG_TO_RAD
            val dPhi = (lat2 - lat1) * DEG_TO_RAD
            val dLambda = (lon2 - lon1) * DEG_TO_RAD

            val sinHalfDPhi = sin(dPhi / 2.0)
            val sinHalfDLambda = sin(dLambda / 2.0)
            val a = sinHalfDPhi * sinHalfDPhi +
                    cos(phi1) * cos(phi2) * sinHalfDLambda * sinHalfDLambda
            val c = 2.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
            return EARTH_RADIUS_METERS * c
        }

        /**
         * Initial great-circle bearing (forward azimuth) from point 1 to point 2, in degrees `[0, 360)`.
         *
         * `θ = atan2(sin Δλ · cos φ2, cos φ1 · sin φ2 − sin φ1 · cos φ2 · cos Δλ)`
         */
        fun haversineBearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val phi1 = lat1 * DEG_TO_RAD
            val phi2 = lat2 * DEG_TO_RAD
            val dLambda = (lon2 - lon1) * DEG_TO_RAD

            val y = sin(dLambda) * cos(phi2)
            val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
            val theta = atan2(y, x) * RAD_TO_DEG
            return (theta + 360.0) % 360.0
        }

        /** Normalizes any angle in degrees into `[0, 360)`. */
        fun normalizeBearing(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f
    }
}
