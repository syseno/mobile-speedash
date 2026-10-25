package com.syseno.speedash.domain.usecase

import app.cash.turbine.test
import com.syseno.speedash.domain.model.RawGpsSignal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessGpsSignalUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCase = ProcessGpsSignalUseCase(
        config = ProcessGpsSignalUseCase.Config(
            emaAlpha = 0.25,
            noiseGateKmh = 3.0,
            maxAccuracyMeters = 50.0f,
            minBearingDistanceMeters = 2.0
        ),
        dispatcher = testDispatcher
    )

    private fun rawSignal(
        lat: Double = 37.7749,
        lon: Double = -122.4194,
        speedMps: Float,
        timestampMillis: Long,
        bearing: Float? = null,
        accuracyMeters: Float = 5.0f
    ) = RawGpsSignal(
        latitude = lat,
        longitude = lon,
        speedMps = speedMps,
        bearing = bearing,
        accuracyMeters = accuracyMeters,
        timestampMillis = timestampMillis
    )

    @Test
    fun noiseGate_clampsSpeedBelow3KmhToZero() = runTest(testDispatcher) {
        // 0.5 m/s = 1.8 km/h (< 3.0 km/h threshold)
        val signal = rawSignal(speedMps = 0.5f, timestampMillis = 1000L)

        useCase(flowOf(signal)).test {
            val item = awaitItem()
            assertEquals(0.0, item.speedKmh)
            assertTrue(item.isStationary)
            assertEquals(1.8, item.rawSpeedKmh, 0.01)
            awaitComplete()
        }
    }

    @Test
    fun noiseGate_allowsSpeedAboveThreshold() = runTest(testDispatcher) {
        // 10.0 m/s = 36.0 km/h (>= 3.0 km/h threshold)
        val signal = rawSignal(speedMps = 10.0f, timestampMillis = 1000L)

        useCase(flowOf(signal)).test {
            val item = awaitItem()
            assertEquals(36.0, item.speedKmh, 0.01)
            assertFalse(item.isStationary)
            awaitComplete()
        }
    }

    @Test
    fun emaSmoothing_appliesAlphaFactorCorrectly() = runTest(testDispatcher) {
        // Sample 1: 10 m/s = 36 km/h -> seeds EMA at 36.0
        // Sample 2: 20 m/s = 72 km/h -> EMA = 0.25 * 72 + 0.75 * 36 = 18 + 27 = 45.0 km/h
        val s1 = rawSignal(speedMps = 10.0f, timestampMillis = 1000L)
        val s2 = rawSignal(speedMps = 20.0f, timestampMillis = 2000L)

        useCase(flowOf(s1, s2)).test {
            val item1 = awaitItem()
            assertEquals(36.0, item1.speedKmh, 0.01)

            val item2 = awaitItem()
            assertEquals(45.0, item2.speedKmh, 0.01)
            assertEquals(72.0, item2.rawSpeedKmh, 0.01)
            awaitComplete()
        }
    }

    @Test
    fun bearingCalculation_computesForwardAzimuthWhenMoving() = runTest(testDispatcher) {
        // Moving North: Lat increases, Lon stays constant
        // 37.7749 -> 37.7755 (approx 66 meters displacement)
        val s1 = rawSignal(lat = 37.774900, lon = -122.419400, speedMps = 10.0f, timestampMillis = 1000L)
        val s2 = rawSignal(lat = 37.775500, lon = -122.419400, speedMps = 10.0f, timestampMillis = 2000L)

        useCase(flowOf(s1, s2)).test {
            awaitItem() // First item uses initial bearing
            val item2 = awaitItem()
            // Forward azimuth moving directly North should be ~0.0 degrees
            assertEquals(0.0f, item2.bearing, 1.0f)
            assertTrue(item2.distanceDeltaMeters > 50.0)
            awaitComplete()
        }
    }

    @Test
    fun bearingHold_preservesLastHeadingWhenStationary() = runTest(testDispatcher) {
        // Sample 1: Moving East (90 deg)
        val s1 = rawSignal(lat = 37.774900, lon = -122.419400, speedMps = 10.0f, timestampMillis = 1000L)
        val s2 = rawSignal(lat = 37.774900, lon = -122.418200, speedMps = 10.0f, timestampMillis = 2000L)
        // Sample 3..12: Vehicle comes to a complete stop (0.0 m/s), small GPS jitter (< 2m)
        val stoppedSignals = (1..10).map { i ->
            rawSignal(
                lat = 37.774900 + (i * 0.000001), // negligible jitter ~0.1m
                lon = -122.418200 + (i * 0.000001),
                speedMps = 0.0f,
                timestampMillis = 2000L + (i * 1000L)
            )
        }

        val allSignals = listOf(s1, s2) + stoppedSignals

        val signalFlow = kotlinx.coroutines.flow.flow {
            allSignals.forEach { emit(it) }
        }

        useCase(signalFlow).test {
            awaitItem() // s1
            val movingItem = awaitItem() // s2
            val movingBearing = movingItem.bearing
            assertEquals(90.0f, movingBearing, 2.0f)

            var lastItem = movingItem
            repeat(stoppedSignals.size) {
                lastItem = awaitItem()
            }

            assertTrue(lastItem.isStationary)
            assertEquals(0.0, lastItem.speedKmh)
            // Bearing must NOT jump or jitter; it must retain previous moving heading
            assertEquals(movingBearing, lastItem.bearing)
            awaitComplete()
        }
    }

    @Test
    fun validation_discardsInaccurateAndOutOfOrderFixes() = runTest(testDispatcher) {
        val valid1 = rawSignal(speedMps = 10.0f, timestampMillis = 1000L, accuracyMeters = 10.0f)
        val inaccurate = rawSignal(speedMps = 20.0f, timestampMillis = 2000L, accuracyMeters = 80.0f) // > 50m max
        val outOfOrder = rawSignal(speedMps = 15.0f, timestampMillis = 900L, accuracyMeters = 5.0f) // ts < valid1
        val valid2 = rawSignal(speedMps = 15.0f, timestampMillis = 3000L, accuracyMeters = 5.0f)

        useCase(flowOf(valid1, inaccurate, outOfOrder, valid2)).test {
            val item1 = awaitItem()
            assertEquals(1000L, item1.timestampMillis)

            val item2 = awaitItem()
            assertEquals(3000L, item2.timestampMillis)
            awaitComplete()
        }
    }

    @Test
    fun pureMath_haversineDistanceAndBearing() {
        // Test Paris (48.8566, 2.3522) to London (51.5074, -0.1278)
        val distance = ProcessGpsSignalUseCase.haversineDistanceMeters(
            48.8566, 2.3522,
            51.5074, -0.1278
        )
        // Distance is ~343 km
        assertEquals(343_500.0, distance, 5_000.0)

        // Bearing from Paris to London is Northwest (~330 degrees)
        val bearing = ProcessGpsSignalUseCase.haversineBearingDegrees(
            48.8566, 2.3522,
            51.5074, -0.1278
        )
        assertEquals(330.0, bearing, 5.0)
    }
}
