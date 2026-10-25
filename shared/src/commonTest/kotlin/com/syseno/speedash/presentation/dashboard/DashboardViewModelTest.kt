package com.syseno.speedash.presentation.dashboard

import app.cash.turbine.test
import com.syseno.speedash.domain.model.AppLanguage
import com.syseno.speedash.domain.model.LocationPermissionException
import com.syseno.speedash.domain.model.RawLocation
import com.syseno.speedash.domain.model.VehicleData
import com.syseno.speedash.domain.repository.BarometerRepository
import com.syseno.speedash.domain.repository.CompassRepository
import com.syseno.speedash.domain.repository.LocationRepository
import com.syseno.speedash.domain.usecase.ObserveVehicleTelemetryUseCase
import com.syseno.speedash.domain.usecase.ProcessGpsSignalUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
        assertEquals(TrackingStatus.Active, reset.trackingStatus)
    }

    @Test
    fun stop_zeroesDisplayedSpeed() {
        val state = DashboardUiState(trackingStatus = TrackingStatus.Active, vehicle = vehicle(80.0, 1_000L))
        val stopped = DashboardReducer.reduce(state, DashboardResult.TrackingStopped)
        assertEquals(TrackingStatus.Idle, stopped.trackingStatus)
        assertEquals(0, stopped.displaySpeed)
    }

    @Test
    fun speedUnit_conversionsAndCycle() {
        val state = DashboardUiState(vehicle = vehicle(100.0, 1_000L), tripDistanceMeters = 1_609.344)
        assertEquals(100, state.displaySpeed)
        val mph = DashboardReducer.reduce(state, DashboardResult.SpeedUnitChanged(SpeedUnit.MPH))
        assertEquals(62, mph.displaySpeed)
        assertEquals(1.0, mph.displayTripDistance, 1e-9)
        assertEquals(SpeedUnit.KNOTS, SpeedUnit.MPH.next())
        assertEquals(SpeedUnit.KMH, SpeedUnit.KNOTS.next())
    }

    @Test
    fun compassHeadingUpdated_updatesHeading() {
        val state = DashboardUiState()
        val updated = DashboardReducer.reduce(state, DashboardResult.CompassHeadingUpdated(135.0f))
        assertEquals(135.0f, updated.compassHeading)
        assertEquals(135.0f, updated.headingDegree)
        assertEquals("SE", updated.cardinalDirection)
    }

    @Test
    fun vehicleType_selectionDefaultsToKnotsForShipAndPlane_andAllowsManualChange() {
        val initialState = DashboardUiState()
        assertEquals(VehicleType.CAR, initialState.vehicleType)
        assertEquals(SpeedUnit.KMH, initialState.speedUnit)

        // Select Plane -> defaults to Knots
        val planeState = DashboardReducer.reduce(initialState, DashboardResult.VehicleTypeChanged(VehicleType.PLANE))
        assertEquals(VehicleType.PLANE, planeState.vehicleType)
        assertEquals(SpeedUnit.KNOTS, planeState.speedUnit)

        // User can still manually change unit to MPH for Plane
        val planeMphState = DashboardReducer.reduce(planeState, DashboardResult.SpeedUnitChanged(SpeedUnit.MPH))
        assertEquals(VehicleType.PLANE, planeMphState.vehicleType)
        assertEquals(SpeedUnit.MPH, planeMphState.speedUnit)

        // Select Ship -> defaults to Knots
        val shipState = DashboardReducer.reduce(planeMphState, DashboardResult.VehicleTypeChanged(VehicleType.SHIP))
        assertEquals(VehicleType.SHIP, shipState.vehicleType)
        assertEquals(SpeedUnit.KNOTS, shipState.speedUnit)

        // Select Motorcycle -> defaults to KMH
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

    private fun viewModel(
        source: () -> Flow<RawLocation> = { emptyFlow() },
        compassSource: () -> Flow<Float> = { emptyFlow() },
        hasCompass: Boolean = true,
        barometerSource: () -> Flow<Float> = { emptyFlow() },
        hasBarometer: Boolean = true
    ) = DashboardViewModel(
        observeVehicleTelemetry = ObserveVehicleTelemetryUseCase(
            locationRepository = FakeLocationRepository(source),
            processGpsSignal = ProcessGpsSignalUseCase(dispatcher = testDispatcher)
        ),
        compassRepository = FakeCompassRepository(compassSource, isAvailable = hasCompass),
        barometerRepository = FakeBarometerRepository(barometerSource, isAvailable = hasBarometer)
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
        assertEquals(72, vm.uiState.value.displaySpeed) // 20 m/s = 72 km/h (first sample seeds EMA)

        vm.onIntent(DashboardUiIntent.StopTracking)
        advanceUntilIdle()
        assertEquals(TrackingStatus.Idle, vm.uiState.value.trackingStatus)
    }

    @Test
    fun permissionMissing_requestsOnce_thenOpensSettings() = runTest(testDispatcher) {
        val vm = viewModel(source = { flow { throw LocationPermissionException("denied") } })

        vm.effects.test {
            vm.onIntent(DashboardUiIntent.StartTracking)
            advanceUntilIdle()
            assertEquals(DashboardUiEffect.RequestLocationPermission, awaitItem())
            assertTrue(vm.uiState.value.hasRequestedPermission)

            // User granted, but the source still reports missing permission → do not loop.
            vm.onIntent(DashboardUiIntent.PermissionResult(granted = true))
            advanceUntilIdle()
            assertEquals(DashboardUiEffect.OpenAppSettings, awaitItem())
            assertEquals(TrackingStatus.PermissionDenied, vm.uiState.value.trackingStatus)
        }
    }

    @Test
    fun permissionDenied_showsMessage() = runTest(testDispatcher) {
        val vm = viewModel(source = { flow { } })

        vm.effects.test {
            vm.onIntent(DashboardUiIntent.PermissionResult(granted = false))
            advanceUntilIdle()
            assertTrue(awaitItem() is DashboardUiEffect.ShowMessage)
            assertEquals(TrackingStatus.PermissionDenied, vm.uiState.value.trackingStatus)
        }
    }

    @Test
    fun compassHeading_updatesUiState_whenCompassSensorAvailable() = runTest(testDispatcher) {
        val compassUpdates = MutableSharedFlow<Float>()
        val vm = viewModel(
            source = { kotlinx.coroutines.flow.emptyFlow() },
            compassSource = { compassUpdates },
            hasCompass = true
        )

        advanceUntilIdle()
        assertEquals(0.0f, vm.uiState.value.headingDegree)

        compassUpdates.emit(90.0f)
        advanceUntilIdle()

        assertEquals(90.0f, vm.uiState.value.headingDegree)
        assertEquals("E", vm.uiState.value.cardinalDirection)
    }

    @Test
    fun movingVehicle_usesCompassSensor_whenSensorAvailable() = runTest(testDispatcher) {
        val fixes = MutableSharedFlow<RawLocation>()
        val compassUpdates = MutableSharedFlow<Float>(replay = 1)
        val vm = viewModel(
            source = { fixes },
            compassSource = { compassUpdates },
            hasCompass = true
        )

        advanceUntilIdle()
        compassUpdates.emit(270.0f)
        advanceUntilIdle()
        assertEquals(270.0f, vm.uiState.value.headingDegree)

        vm.onIntent(DashboardUiIntent.StartTracking)
        advanceUntilIdle()

        // Send two fixes moving east (bearing ~90 degrees) at high speed (20 m/s = 72 km/h)
        fixes.emit(RawLocation(latitude = 0.0, longitude = 0.0, speedMps = 20f, accuracyMeters = 5f, timestampMillis = 1_000L))
        advanceUntilIdle()
        fixes.emit(RawLocation(latitude = 0.0, longitude = 0.001, speedMps = 20f, accuracyMeters = 5f, timestampMillis = 2_000L))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isTracking)
        assertTrue(!vm.uiState.value.vehicle.isStationary)
        // With compass sensor present on the device, heading follows the compass sensor
        assertEquals(270.0f, vm.uiState.value.headingDegree)
    }

    @Test
    fun vehicle_usesGpsBearing_whenNoCompassSensor() = runTest(testDispatcher) {
        val fixes = MutableSharedFlow<RawLocation>()
        val vm = viewModel(
            source = { fixes },
            hasCompass = false
        )

        advanceUntilIdle()
        assertEquals(false, vm.uiState.value.hasCompassSensor)

        vm.onIntent(DashboardUiIntent.StartTracking)
        advanceUntilIdle()

        // Send two fixes moving east (bearing ~90 degrees)
        fixes.emit(RawLocation(latitude = 0.0, longitude = 0.0, speedMps = 20f, accuracyMeters = 5f, timestampMillis = 1_000L))
        advanceUntilIdle()
        fixes.emit(RawLocation(latitude = 0.0, longitude = 0.001, speedMps = 20f, accuracyMeters = 5f, timestampMillis = 2_000L))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isTracking)
        // Without compass sensor, heading follows GPS course-over-ground bearing
        assertEquals(vm.uiState.value.vehicle.bearing, vm.uiState.value.headingDegree)
        assertEquals("E", vm.uiState.value.cardinalDirection)
    }

    @Test
    fun toggleKeepScreenOn_updatesState() = runTest(testDispatcher) {
        val vm = viewModel()
        assertTrue(vm.uiState.value.keepScreenOn)

        vm.onIntent(DashboardUiIntent.ToggleKeepScreenOn)
        advanceUntilIdle()
        assertEquals(false, vm.uiState.value.keepScreenOn)

        vm.onIntent(DashboardUiIntent.ToggleKeepScreenOn)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.keepScreenOn)
    }

    @Test
    fun changeLanguage_updatesUiState() = runTest(testDispatcher) {
        val vm = viewModel()
        assertEquals(AppLanguage.ENGLISH, vm.uiState.value.language)

        vm.onIntent(DashboardUiIntent.ChangeLanguage(AppLanguage.INDONESIAN))
        advanceUntilIdle()
        assertEquals(AppLanguage.INDONESIAN, vm.uiState.value.language)

        vm.onIntent(DashboardUiIntent.ChangeLanguage(AppLanguage.ENGLISH))
        advanceUntilIdle()
        assertEquals(AppLanguage.ENGLISH, vm.uiState.value.language)
    }

    @Test
    fun permissionDenied_showsLocalizedMessage_whenIndonesian() = runTest(testDispatcher) {
        val vm = viewModel(source = { flow { } })
        vm.onIntent(DashboardUiIntent.ChangeLanguage(AppLanguage.INDONESIAN))
        advanceUntilIdle()

        vm.effects.test {
            vm.onIntent(DashboardUiIntent.PermissionResult(granted = false))
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is DashboardUiEffect.ShowMessage)
            assertEquals("Izin lokasi diperlukan untuk mengukur kecepatan.", effect.message)
            assertEquals(TrackingStatus.PermissionDenied, vm.uiState.value.trackingStatus)
        }
    }

    @Test
    fun changeThemeMode_updatesUiState() = runTest(testDispatcher) {
        val vm = viewModel()
        assertEquals(ThemeMode.SYSTEM, vm.uiState.value.themeMode)

        vm.onIntent(DashboardUiIntent.ChangeThemeMode(ThemeMode.DARK))
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)

        vm.onIntent(DashboardUiIntent.ChangeThemeMode(ThemeMode.LIGHT))
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)

        vm.onIntent(DashboardUiIntent.CycleThemeMode)
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, vm.uiState.value.themeMode)
    }

    @Test
    fun barometerAltitude_updatesUiState_whenSensorAvailable() = runTest(testDispatcher) {
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

    @Test
    fun speedAlertLevel_calculatesCorrectlyForCarAndMotorcycle() {
        // Under 70 km/h -> NORMAL
        assertEquals(SpeedAlertLevel.NORMAL, getSpeedAlertLevel(69.9, VehicleType.CAR))
        assertEquals(SpeedAlertLevel.NORMAL, getSpeedAlertLevel(0.0, VehicleType.MOTORCYCLE))

        // 70 to 99.9 km/h -> WARNING
        assertEquals(SpeedAlertLevel.WARNING, getSpeedAlertLevel(70.0, VehicleType.CAR))
        assertEquals(SpeedAlertLevel.WARNING, getSpeedAlertLevel(85.0, VehicleType.MOTORCYCLE))
        assertEquals(SpeedAlertLevel.WARNING, getSpeedAlertLevel(99.9, VehicleType.CAR))

        // 100+ km/h -> DANGER
        assertEquals(SpeedAlertLevel.DANGER, getSpeedAlertLevel(100.0, VehicleType.CAR))
        assertEquals(SpeedAlertLevel.DANGER, getSpeedAlertLevel(120.0, VehicleType.MOTORCYCLE))

        // Non-car/motorcycle vehicle types (Train, Ship, Plane) always return NORMAL
        assertEquals(SpeedAlertLevel.NORMAL, getSpeedAlertLevel(150.0, VehicleType.TRAIN))
        assertEquals(SpeedAlertLevel.NORMAL, getSpeedAlertLevel(120.0, VehicleType.SHIP))
        assertEquals(SpeedAlertLevel.NORMAL, getSpeedAlertLevel(500.0, VehicleType.PLANE))
    }
}
