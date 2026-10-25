package com.syseno.speedash.presentation.dashboard

import app.cash.turbine.test
import com.syseno.speedash.domain.model.AppLanguage
import com.syseno.speedash.domain.model.LocationPermissionException
import com.syseno.speedash.domain.model.RawLocation
import com.syseno.speedash.domain.model.TripRecord
import com.syseno.speedash.domain.model.VehicleData
import com.syseno.speedash.domain.repository.BarometerRepository
import com.syseno.speedash.domain.repository.CompassRepository
import com.syseno.speedash.domain.repository.LocationRepository
import com.syseno.speedash.domain.repository.TripRepository
import com.syseno.speedash.domain.usecase.ObserveVehicleTelemetryUseCase
import com.syseno.speedash.domain.usecase.ProcessGpsSignalUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashboardReducerTest {

    private fun vehicle(
        speedKmh: Double,
        timestampMillis: Long,
        distanceDeltaMeters: Double = 0.0
    ) = VehicleData.INITIAL.copy(
        speedKmh = speedKmh,
        rawSpeedKmh = speedKmh,
        timestampMillis = timestampMillis,
        isStationary = speedKmh == 0.0,
        distanceDeltaMeters = distanceDeltaMeters
    )

    @Test
    fun trackingStarted_setsAcquiring() {
        val state = DashboardReducer.reduce(DashboardUiState(), DashboardResult.TrackingStarted)
        assertEquals(TrackingStatus.Acquiring, state.trackingStatus)
        assertTrue(state.isTracking)
    }

    @Test
    fun firstFix_becomesActive_withoutCountingMovingTime() {
        val acquiring = DashboardUiState(trackingStatus = TrackingStatus.Acquiring)
        val state = DashboardReducer.reduce(acquiring, DashboardResult.VehicleUpdated(vehicle(50.0, 1_000L)))
        assertEquals(TrackingStatus.Active, state.trackingStatus)
        assertEquals(0L, state.movingTimeMillis)
        assertEquals(50.0, state.maxSpeedKmh)
    }

    @Test
    fun subsequentFixes_accumulateDistanceTimeAndMaxSpeed() {
        var state = DashboardUiState(trackingStatus = TrackingStatus.Acquiring)
        state = DashboardReducer.reduce(state, DashboardResult.VehicleUpdated(vehicle(36.0, 1_000L)))
        state = DashboardReducer.reduce(state, DashboardResult.VehicleUpdated(vehicle(72.0, 2_000L, 10.0)))
        state = DashboardReducer.reduce(state, DashboardResult.VehicleUpdated(vehicle(54.0, 3_000L, 20.0)))

        assertEquals(72.0, state.maxSpeedKmh)
        assertEquals(30.0, state.tripDistanceMeters)
        assertEquals(2_000L, state.movingTimeMillis)
        // 30 m / 2 s = 15 m/s = 54 km/h
        assertEquals(54, state.displayAverageSpeed)
    }

    @Test
    fun largeGapBetweenFixes_isClamped() {
        var state = DashboardUiState(trackingStatus = TrackingStatus.Acquiring)
        state = DashboardReducer.reduce(state, DashboardResult.VehicleUpdated(vehicle(30.0, 1_000L)))
        state = DashboardReducer.reduce(state, DashboardResult.VehicleUpdated(vehicle(30.0, 61_000L)))
        assertEquals(DashboardReducer.MAX_SAMPLE_GAP_MILLIS, state.movingTimeMillis)
    }

    @Test
    fun stationaryFix_doesNotAddMovingTime() {
        var state = DashboardUiState(trackingStatus = TrackingStatus.Acquiring)
        state = DashboardReducer.reduce(state, DashboardResult.VehicleUpdated(vehicle(20.0, 1_000L)))
        state = DashboardReducer.reduce(state, DashboardResult.VehicleUpdated(vehicle(0.0, 2_000L)))
        assertEquals(0L, state.movingTimeMillis)
    }

    @Test
    fun tripReset_clearsStatisticsButKeepsTracking() {
        val state = DashboardUiState(
            trackingStatus = TrackingStatus.Active,
            maxSpeedKmh = 120.0,
            tripDistanceMeters = 5_000.0,
            movingTimeMillis = 60_000L
        )
        val reset = DashboardReducer.reduce(state, DashboardResult.TripReset)
        assertEquals(0.0, reset.maxSpeedKmh)
        assertEquals(0.0, reset.tripDistanceMeters)
        assertEquals(0L, reset.movingTimeMillis)
    }

    @Test
    fun speedUnitCycle_wrapsCorrectly() {
        val state = DashboardUiState(speedUnit = SpeedUnit.KMH)
        val mph = DashboardReducer.reduce(state, DashboardResult.SpeedUnitChanged(SpeedUnit.MPH))
        assertEquals(SpeedUnit.MPH, mph.speedUnit)

        val knots = DashboardReducer.reduce(mph, DashboardResult.SpeedUnitChanged(SpeedUnit.KNOTS))
        assertEquals(SpeedUnit.KNOTS, knots.speedUnit)

        val kmh = DashboardReducer.reduce(knots, DashboardResult.SpeedUnitChanged(SpeedUnit.KMH))
        assertEquals(SpeedUnit.KMH, kmh.speedUnit)
    }

    @Test
    fun vehicleTypeSelection_setsDefaultSpeedUnit() {
        val state = DashboardUiState(vehicleType = VehicleType.CAR, speedUnit = SpeedUnit.KMH)
        val planeState = DashboardReducer.reduce(state, DashboardResult.VehicleTypeChanged(VehicleType.PLANE))
        assertEquals(VehicleType.PLANE, planeState.vehicleType)
        assertEquals(SpeedUnit.KNOTS, planeState.speedUnit)

        val shipState = DashboardReducer.reduce(planeState, DashboardResult.VehicleTypeChanged(VehicleType.SHIP))
        assertEquals(VehicleType.SHIP, shipState.vehicleType)
        assertEquals(SpeedUnit.KNOTS, shipState.speedUnit)

        val motoState = DashboardReducer.reduce(shipState, DashboardResult.VehicleTypeChanged(VehicleType.MOTORCYCLE))
        assertEquals(VehicleType.MOTORCYCLE, motoState.vehicleType)
        assertEquals(SpeedUnit.KMH, motoState.speedUnit)
    }

    @Test
    fun languageChanged_updatesState() {
        val state = DashboardUiState()
        assertEquals(AppLanguage.ENGLISH, state.language)

        val idState = DashboardReducer.reduce(state, DashboardResult.LanguageChanged(AppLanguage.INDONESIAN))
        assertEquals(AppLanguage.INDONESIAN, idState.language)

        val enState = DashboardReducer.reduce(idState, DashboardResult.LanguageChanged(AppLanguage.ENGLISH))
        assertEquals(AppLanguage.ENGLISH, enState.language)
        assertEquals(AppLanguage.INDONESIAN, AppLanguage.ENGLISH.next())
        assertEquals(AppLanguage.ENGLISH, AppLanguage.INDONESIAN.next())
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private class FakeLocationRepository(private val source: () -> Flow<RawLocation>) : LocationRepository {
        override fun observeRawLocations(): Flow<RawLocation> = source()
    }

    private class FakeCompassRepository(
        private val source: () -> Flow<Float>,
        override val isAvailable: Boolean = true
    ) : CompassRepository {
        override fun observeCompassHeading(): Flow<Float> = source()
    }

    private class FakeBarometerRepository(
        private val source: () -> Flow<Float>,
        override val isAvailable: Boolean = true
    ) : BarometerRepository {
        override fun observeAltitude(): Flow<Float> = source()
    }

    private class FakeTripRepository(
        initialTrips: List<TripRecord> = emptyList()
    ) : TripRepository {
        private val _trips = MutableStateFlow(initialTrips)
        override val trips: StateFlow<List<TripRecord>> = _trips.asStateFlow()

        override fun saveTrip(trip: TripRecord) {
            _trips.value = listOf(trip) + _trips.value
        }

        override fun getTrips(): List<TripRecord> = _trips.value

        override fun deleteTrip(id: String) {
            _trips.value = _trips.value.filter { it.id != id }
        }
    }

    private fun viewModel(
        source: () -> Flow<RawLocation> = { emptyFlow() },
        compassSource: () -> Flow<Float> = { emptyFlow() },
        hasCompass: Boolean = true,
        barometerSource: () -> Flow<Float> = { emptyFlow() },
        hasBarometer: Boolean = true,
        tripRepository: TripRepository = FakeTripRepository()
    ) = DashboardViewModel(
        observeVehicleTelemetry = ObserveVehicleTelemetryUseCase(
            locationRepository = FakeLocationRepository(source),
            processGpsSignal = ProcessGpsSignalUseCase(dispatcher = testDispatcher)
        ),
        compassRepository = FakeCompassRepository(compassSource, isAvailable = hasCompass),
        barometerRepository = FakeBarometerRepository(barometerSource, isAvailable = hasBarometer),
        tripRepository = tripRepository
    )

    @Test
    fun startTracking_emitsActiveStateOnFix() = runTest(testDispatcher) {
        val fixes = MutableSharedFlow<RawLocation>()
        val vm = viewModel(source = { fixes })

        vm.onIntent(DashboardUiIntent.StartTracking)
        advanceUntilIdle()
        assertEquals(TrackingStatus.Acquiring, vm.uiState.value.trackingStatus)

        fixes.emit(
            RawLocation(latitude = 10.0, longitude = 106.0, speedMps = 20f, accuracyMeters = 5f, timestampMillis = 1_000L)
        )
        advanceUntilIdle()

        assertEquals(TrackingStatus.Active, vm.uiState.value.trackingStatus)
    }

    @Test
    fun stopTracking_resetsState() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.onIntent(DashboardUiIntent.StartTracking)
        advanceUntilIdle()
        vm.onIntent(DashboardUiIntent.StopTracking)
        advanceUntilIdle()
        assertEquals(TrackingStatus.Idle, vm.uiState.value.trackingStatus)
    }

    @Test
    fun compassHeadingUpdates_areReflectedInState() = runTest(testDispatcher) {
        val compassUpdates = MutableSharedFlow<Float>()
        val vm = viewModel(
            compassSource = { compassUpdates },
            hasCompass = true
        )

        assertTrue(vm.uiState.value.hasCompassSensor)
        assertEquals(0.0f, vm.uiState.value.headingDegree)

        compassUpdates.emit(45.5f)
        advanceUntilIdle()

        assertEquals(45.5f, vm.uiState.value.headingDegree)
        assertEquals(46, vm.uiState.value.headingDegrees)
        assertEquals("NE", vm.uiState.value.cardinalDirection)
    }

    @Test
    fun barometerAltitudeUpdates_areReflectedInState() = runTest(testDispatcher) {
        val barometerUpdates = MutableSharedFlow<Float>()
        val vm = viewModel(
            barometerSource = { barometerUpdates },
            hasBarometer = true
        )

        assertTrue(vm.uiState.value.hasBarometerSensor)
        assertEquals(null, vm.uiState.value.displayAltitudeMeters)

        barometerUpdates.emit(120.4f)
        advanceUntilIdle()

        assertEquals(120.4f, vm.uiState.value.altitudeMeters)
        assertEquals(120, vm.uiState.value.displayAltitudeMeters)
    }
}
