package com.syseno.speedash.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syseno.speedash.domain.model.AppLanguage
import com.syseno.speedash.domain.model.LocationPermissionException
import com.syseno.speedash.domain.model.LocationUnavailableException
import com.syseno.speedash.domain.model.TripRecord
import com.syseno.speedash.domain.model.VehicleData
import com.syseno.speedash.domain.repository.BarometerRepository
import com.syseno.speedash.domain.repository.CompassRepository
import com.syseno.speedash.domain.repository.TripRepository
import com.syseno.speedash.domain.usecase.ObserveVehicleTelemetryUseCase
import com.syseno.speedash.presentation.localization.AppStrings
import com.syseno.speedash.presentation.util.openMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

/**
 * Dashboard ViewModel implementing strict MVI / Unidirectional Data Flow:
 *
 * ```
 *  UI ──onIntent()──▶ Intent Channel ──▶ handleIntent() ──▶ DashboardResult ──▶ DashboardReducer ──▶ StateFlow ──▶ UI
 *                                              │                                                        
 *                                              └──▶ Effect Channel ──▶ UI (one-shot)                    
 *  Telemetry Flow ──────────────────────────────▶ DashboardResult.VehicleUpdated ──▶ (same reducer)      
 * ```
 *
 * - The UI can only send [DashboardUiIntent]s via [onIntent].
 * - Intents are processed sequentially from a single channel (no races between user actions).
 * - State is mutated **only** by the pure [DashboardReducer].
 * - Side effects are delivered exactly once through a [Channel]-backed [effects] flow.
 */
class DashboardViewModel(
    private val observeVehicleTelemetry: ObserveVehicleTelemetryUseCase,
    private val compassRepository: CompassRepository,
    private val barometerRepository: BarometerRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            hasCompassSensor = compassRepository.isAvailable,
            hasBarometerSensor = barometerRepository.isAvailable
        )
    )

    /** Single source of truth for rendering. */
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val intents = Channel<DashboardUiIntent>(capacity = Channel.UNLIMITED)

    private val _effects = Channel<DashboardUiEffect>(capacity = Channel.BUFFERED)

    /** One-shot side effects. Collect once from the UI (e.g. in a `LaunchedEffect`). */
    val effects: Flow<DashboardUiEffect> = _effects.receiveAsFlow()

    private var trackingJob: Job? = null

    init {
        intents.consumeAsFlow()
            .onEach(::handleIntent)
            .launchIn(viewModelScope)

        if (compassRepository.isAvailable) {
            compassRepository.observeCompassHeading()
                .onEach { heading -> dispatch(DashboardResult.CompassHeadingUpdated(heading)) }
                .launchIn(viewModelScope)
        }

        if (barometerRepository.isAvailable) {
            barometerRepository.observeAltitude()
                .onEach { altitude -> dispatch(DashboardResult.BarometerAltitudeUpdated(altitude)) }
                .launchIn(viewModelScope)
        }

        tripRepository.trips
            .onEach { trips -> dispatch(DashboardResult.TripsUpdated(trips)) }
            .launchIn(viewModelScope)
    }

    /** The only public entry point for UI events. Never suspends, never drops. */
    fun onIntent(intent: DashboardUiIntent) {
        intents.trySend(intent)
    }

    // ---------------------------------------------------------------------------------------------
    // Intent processing
    // ---------------------------------------------------------------------------------------------

    private fun handleIntent(intent: DashboardUiIntent) {
        when (intent) {
            DashboardUiIntent.StartTracking -> startTracking()
            DashboardUiIntent.StopTracking -> stopTracking()
            DashboardUiIntent.ToggleTracking -> if (_uiState.value.isTracking) stopTracking() else startTracking()
            is DashboardUiIntent.PermissionResult -> onPermissionResult(intent.granted)
            is DashboardUiIntent.ChangeSpeedUnit -> dispatch(DashboardResult.SpeedUnitChanged(intent.unit))
            DashboardUiIntent.CycleSpeedUnit ->
                dispatch(DashboardResult.SpeedUnitChanged(_uiState.value.speedUnit.next()))
            is DashboardUiIntent.SelectVehicle ->
                dispatch(DashboardResult.VehicleTypeChanged(intent.vehicleType))
            is DashboardUiIntent.ChangeLanguage ->
                dispatch(DashboardResult.LanguageChanged(intent.language))
            is DashboardUiIntent.ChangeThemeMode ->
                dispatch(DashboardResult.ThemeModeChanged(intent.themeMode))
            DashboardUiIntent.CycleThemeMode ->
                dispatch(DashboardResult.ThemeModeChanged(_uiState.value.themeMode.next()))
            DashboardUiIntent.ResetTrip -> dispatch(DashboardResult.TripReset)
            DashboardUiIntent.ToggleKeepScreenOn ->
                dispatch(DashboardResult.KeepScreenOnChanged(!_uiState.value.keepScreenOn))

            DashboardUiIntent.ShowTripNameDialog -> dispatch(DashboardResult.TripNameDialogShown)
            DashboardUiIntent.DismissTripNameDialog -> dispatch(DashboardResult.TripNameDialogDismissed)
            is DashboardUiIntent.StartTripRecording -> {
                if (!_uiState.value.isTracking) {
                    startTracking()
                }
                dispatch(DashboardResult.TripRecordingStarted(intent.name, _uiState.value.vehicle))
            }
            DashboardUiIntent.PauseTripRecording -> dispatch(DashboardResult.TripRecordingPaused)
            DashboardUiIntent.ResumeTripRecording -> dispatch(DashboardResult.TripRecordingResumed)
            DashboardUiIntent.StopTripRecording -> stopAndSaveTrip()
            is DashboardUiIntent.DeleteTrip -> tripRepository.deleteTrip(intent.id)
            is DashboardUiIntent.OpenMap -> openMap(intent.lat, intent.lon, intent.label)
        }
    }

    private fun startTracking() {
        if (trackingJob?.isActive == true) return

        dispatch(DashboardResult.TrackingStarted)
        trackingJob = observeVehicleTelemetry()
            .onEach { vehicle -> dispatch(DashboardResult.VehicleUpdated(vehicle)) }
            .catch { error -> onTrackingError(error) }
            .launchIn(viewModelScope)
    }

    private fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        dispatch(DashboardResult.TrackingStopped)
    }

    private fun stopAndSaveTrip() {
        val state = _uiState.value
        if (state.tripRecordingStatus == TripRecordingStatus.Idle) return

        val endTime = state.vehicle.timestampMillis.takeIf { it > 0L } ?: currentTimeMillis()
        val duration = state.currentTripElapsedMillis.coerceAtLeast(1000L)
        val distanceKm = state.currentTripDistanceMeters / 1000.0
        val hours = duration / 3600_000.0
        val avgSpeed = if (hours > 0.0) distanceKm / hours else 0.0

        val record = TripRecord(
            id = "trip_${endTime}_${(1000..9999).random()}",
            name = state.currentTripName.ifBlank { "Trip - $endTime" },
            startTimeMillis = state.currentTripStartTime,
            endTimeMillis = endTime,
            durationMillis = duration,
            distanceKm = distanceKm,
            avgSpeedKmh = avgSpeed,
            startLat = state.currentTripStartLat,
            startLon = state.currentTripStartLon,
            endLat = state.vehicle.latitude,
            endLon = state.vehicle.longitude
        )

        tripRepository.saveTrip(record)
        dispatch(DashboardResult.TripRecordingStopped)
        emitEffect(DashboardUiEffect.ShowMessage("Trip '${record.name}' saved successfully!"))
    }

    private fun currentTimeMillis(): Long {
        return kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds // or kotlinx time / system millis
    }

    private fun onPermissionResult(granted: Boolean) {
        if (granted) {
            startTracking()
        } else {
            val strings = AppStrings.forLanguage(_uiState.value.language)
            dispatch(DashboardResult.PermissionDenied)
            emitEffect(DashboardUiEffect.ShowMessage(strings.messagePermissionRequired))
        }
    }

    private fun onTrackingError(error: Throwable) {
        trackingJob = null
        val strings = AppStrings.forLanguage(_uiState.value.language)
        when (error) {
            is LocationPermissionException -> {
                if (!_uiState.value.hasRequestedPermission) {
                    dispatch(DashboardResult.PermissionRequested)
                    emitEffect(DashboardUiEffect.RequestLocationPermission)
                } else {
                    dispatch(DashboardResult.PermissionDenied)
                    emitEffect(DashboardUiEffect.OpenAppSettings)
                }
            }

            is LocationUnavailableException -> {
                val message = error.message ?: strings.messageGpsUnavailable
                dispatch(DashboardResult.TrackingFailed(message))
                emitEffect(DashboardUiEffect.ShowMessage(strings.messageGpsUnavailable))
            }

            else -> {
                val message = error.message ?: strings.messageUnexpectedError
                dispatch(DashboardResult.TrackingFailed(message))
                emitEffect(DashboardUiEffect.ShowMessage(strings.messageUnexpectedError))
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // State & effect plumbing
    // ---------------------------------------------------------------------------------------------

    private fun dispatch(result: DashboardResult) {
        _uiState.update { current -> DashboardReducer.reduce(current, result) }
    }

    private fun emitEffect(effect: DashboardUiEffect) {
        _effects.trySend(effect)
    }

    override fun onCleared() {
        trackingJob?.cancel()
        intents.close()
        _effects.close()
        super.onCleared()
    }
}

// =================================================================================================
// Internal results + pure reducer (internal for unit testing in commonTest).
// =================================================================================================

/**
 * Internal state-change events produced by intent handling and by the telemetry stream.
 * Not part of the public contract — the UI never sees these.
 */
internal sealed interface DashboardResult {
    data object TrackingStarted : DashboardResult
    data object TrackingStopped : DashboardResult
    data class VehicleUpdated(val vehicle: VehicleData) : DashboardResult
    data object PermissionRequested : DashboardResult
    data object PermissionDenied : DashboardResult
    data class TrackingFailed(val message: String) : DashboardResult
    data class SpeedUnitChanged(val unit: SpeedUnit) : DashboardResult
    data class VehicleTypeChanged(val vehicleType: VehicleType) : DashboardResult
    data class LanguageChanged(val language: AppLanguage) : DashboardResult
    data class ThemeModeChanged(val themeMode: ThemeMode) : DashboardResult
    data object TripReset : DashboardResult
    data class CompassHeadingUpdated(val heading: Float) : DashboardResult
    data class BarometerAltitudeUpdated(val altitude: Float) : DashboardResult
    data class KeepScreenOnChanged(val enabled: Boolean) : DashboardResult
    data class TripsUpdated(val trips: List<TripRecord>) : DashboardResult
    data object TripNameDialogShown : DashboardResult
    data object TripNameDialogDismissed : DashboardResult
    data class TripRecordingStarted(val name: String, val vehicle: VehicleData) : DashboardResult
    data object TripRecordingPaused : DashboardResult
    data object TripRecordingResumed : DashboardResult
    data object TripRecordingStopped : DashboardResult
}

/**
 * Pure `(State, Result) -> State` function. No side effects, no coroutines, fully deterministic.
 */
internal object DashboardReducer {

    /**
     * Gaps between consecutive fixes longer than this (ms) are not counted as moving time,
     * protecting the average speed from GPS dropouts (tunnels, app suspension).
     */
    const val MAX_SAMPLE_GAP_MILLIS: Long = 5_000L

    fun reduce(state: DashboardUiState, result: DashboardResult): DashboardUiState = when (result) {
        DashboardResult.TrackingStarted -> state.copy(trackingStatus = TrackingStatus.Acquiring)

        DashboardResult.TrackingStopped -> state.copy(
            trackingStatus = TrackingStatus.Idle,
            vehicle = state.vehicle.copy(speedKmh = 0.0, isStationary = true)
        )

        is DashboardResult.VehicleUpdated -> reduceVehicle(state, result.vehicle)

        DashboardResult.PermissionRequested -> state.copy(
            trackingStatus = TrackingStatus.Idle,
            hasRequestedPermission = true
        )

        DashboardResult.PermissionDenied -> state.copy(
            trackingStatus = TrackingStatus.PermissionDenied,
            hasRequestedPermission = true
        )

        is DashboardResult.TrackingFailed -> state.copy(
            trackingStatus = TrackingStatus.Error(result.message),
            vehicle = state.vehicle.copy(speedKmh = 0.0, isStationary = true)
        )

        is DashboardResult.SpeedUnitChanged -> state.copy(speedUnit = result.unit)

        is DashboardResult.VehicleTypeChanged -> state.copy(
            vehicleType = result.vehicleType,
            speedUnit = result.vehicleType.defaultSpeedUnit
        )

        is DashboardResult.LanguageChanged -> state.copy(language = result.language)

        is DashboardResult.ThemeModeChanged -> state.copy(themeMode = result.themeMode)

        is DashboardResult.CompassHeadingUpdated -> state.copy(
            compassHeading = result.heading,
            hasCompassSensor = true
        )

        is DashboardResult.BarometerAltitudeUpdated -> state.copy(
            altitudeMeters = result.altitude,
            hasBarometerSensor = true
        )

        is DashboardResult.KeepScreenOnChanged -> state.copy(keepScreenOn = result.enabled)

        DashboardResult.TripReset -> state.copy(
            maxSpeedKmh = 0.0,
            tripDistanceMeters = 0.0,
            movingTimeMillis = 0L
        )

        is DashboardResult.TripsUpdated -> state.copy(trips = result.trips)
        DashboardResult.TripNameDialogShown -> state.copy(showTripNameDialog = true)
        DashboardResult.TripNameDialogDismissed -> state.copy(showTripNameDialog = false)

        is DashboardResult.TripRecordingStarted -> state.copy(
            showTripNameDialog = false,
            tripRecordingStatus = TripRecordingStatus.Recording,
            currentTripName = result.name,
            currentTripStartTime = result.vehicle.timestampMillis.takeIf { it > 0L } ?: 1L,
            currentTripElapsedMillis = 0L,
            currentTripDistanceMeters = 0.0,
            currentTripStartLat = result.vehicle.latitude,
            currentTripStartLon = result.vehicle.longitude
        )

        DashboardResult.TripRecordingPaused -> state.copy(tripRecordingStatus = TripRecordingStatus.Paused)
        DashboardResult.TripRecordingResumed -> state.copy(tripRecordingStatus = TripRecordingStatus.Recording)

        DashboardResult.TripRecordingStopped -> state.copy(
            tripRecordingStatus = TripRecordingStatus.Idle,
            currentTripName = "",
            currentTripStartTime = 0L,
            currentTripElapsedMillis = 0L,
            currentTripDistanceMeters = 0.0
        )
    }

    private fun reduceVehicle(state: DashboardUiState, vehicle: VehicleData): DashboardUiState {
        val previous = state.vehicle
        val elapsed = if (
            state.trackingStatus == TrackingStatus.Active &&
            previous.timestampMillis > 0L &&
            !vehicle.isStationary
        ) {
            (vehicle.timestampMillis - previous.timestampMillis).coerceIn(0L, MAX_SAMPLE_GAP_MILLIS)
        } else {
            0L
        }

        // Trip recording accumulation if actively recording
        val tripElapsed = if (
            state.tripRecordingStatus == TripRecordingStatus.Recording &&
            previous.timestampMillis > 0L
        ) {
            (vehicle.timestampMillis - previous.timestampMillis).coerceIn(0L, MAX_SAMPLE_GAP_MILLIS)
        } else {
            0L
        }

        val tripDistanceDelta = if (state.tripRecordingStatus == TripRecordingStatus.Recording) {
            vehicle.distanceDeltaMeters
        } else {
            0.0
        }

        return state.copy(
            trackingStatus = TrackingStatus.Active,
            vehicle = vehicle,
            maxSpeedKmh = max(state.maxSpeedKmh, vehicle.speedKmh),
            tripDistanceMeters = state.tripDistanceMeters + vehicle.distanceDeltaMeters,
            movingTimeMillis = state.movingTimeMillis + elapsed,
            currentTripElapsedMillis = state.currentTripElapsedMillis + tripElapsed,
            currentTripDistanceMeters = state.currentTripDistanceMeters + tripDistanceDelta
        )
    }
}
