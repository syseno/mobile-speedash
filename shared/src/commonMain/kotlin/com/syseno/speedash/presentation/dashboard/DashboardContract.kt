package com.syseno.speedash.presentation.dashboard

import androidx.compose.runtime.Immutable
import com.syseno.speedash.domain.model.AppLanguage
import com.syseno.speedash.domain.model.TripRecord
import com.syseno.speedash.domain.model.VehicleData
import kotlin.math.roundToInt

// =================================================================================================
// STATE — single immutable snapshot rendered by the UI.
// =================================================================================================

/**
 * Complete, immutable render state for the dashboard screen.
 *
 * Stored fields are the minimal source of truth; everything the UI displays is derived from them
 * through computed properties so state can never become internally inconsistent.
 *
 * @property trackingStatus Lifecycle of the GPS tracking session.
 * @property vehicle Latest processed telemetry (speed in km/h, bearing in degrees, epoch ms timestamp).
 * @property speedUnit User-selected display unit for speed and distance.
 * @property vehicleType User-selected vehicle mode.
 * @property language User-selected application language.
 * @property maxSpeedKmh Highest gated speed observed during the current trip, in km/h.
 * @property tripDistanceMeters Distance accumulated during the current trip, in meters.
 * @property movingTimeMillis Time spent above the noise gate during the current trip, in milliseconds.
 * @property hasRequestedPermission Whether the app already asked the user for location permission
 *   in this session (prevents permission-request loops).
 * @property tripRecordingStatus Current lifecycle state of trip recording.
 * @property currentTripName Name of the active trip being recorded.
 * @property currentTripStartTime Epoch timestamp when the current trip recording started.
 * @property currentTripElapsedMillis Accumulated active time of the current trip in milliseconds.
 * @property currentTripDistanceMeters Accumulated distance of the current trip in meters.
 * @property currentTripStartLat Starting latitude of the current trip.
 * @property currentTripStartLon Starting longitude of the current trip.
 * @property showTripNameDialog Whether the trip naming dialog is currently visible.
 * @property trips List of all saved trip records.
 */
@Immutable
data class DashboardUiState(
    val trackingStatus: TrackingStatus = TrackingStatus.Idle,
    val vehicle: VehicleData = VehicleData.INITIAL,
    val speedUnit: SpeedUnit = SpeedUnit.KMH,
    val vehicleType: VehicleType = VehicleType.CAR,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val maxSpeedKmh: Double = 0.0,
    val tripDistanceMeters: Double = 0.0,
    val movingTimeMillis: Long = 0L,
    val hasRequestedPermission: Boolean = false,
    val hasCompassSensor: Boolean = false,
    val compassHeading: Float? = null,
    val hasBarometerSensor: Boolean = false,
    val altitudeMeters: Float? = null,
    val keepScreenOn: Boolean = true,
    val tripRecordingStatus: TripRecordingStatus = TripRecordingStatus.Idle,
    val currentTripName: String = "",
    val currentTripStartTime: Long = 0L,
    val currentTripElapsedMillis: Long = 0L,
    val currentTripDistanceMeters: Double = 0.0,
    val currentTripStartLat: Double = 0.0,
    val currentTripStartLon: Double = 0.0,
    val showTripNameDialog: Boolean = false,
    val trips: List<TripRecord> = emptyList()
) {
    /** True while location updates are requested (acquiring a fix or actively receiving). */
    val isTracking: Boolean
        get() = trackingStatus == TrackingStatus.Acquiring || trackingStatus == TrackingStatus.Active

    /** True once at least one processed fix has been received in the current session. */
    val hasGpsFix: Boolean
        get() = trackingStatus == TrackingStatus.Active

    /** True while a trip is actively recording or paused. */
    val isTripRecording: Boolean
        get() = tripRecordingStatus != TripRecordingStatus.Idle

    /** True while trip recording is actively accumulating distance/time. */
    val isTripRecordingActive: Boolean
        get() = tripRecordingStatus == TripRecordingStatus.Recording

    /** Trip recording duration in seconds. */
    val tripRecordingDurationSeconds: Long
        get() = currentTripElapsedMillis / MILLIS_PER_SECOND.toLong()

    /** Trip recording distance in kilometers. */
    val tripRecordingDistanceKm: Double
        get() = currentTripDistanceMeters / 1_000.0

    /** Average speed of the recorded trip in km/h. */
    val tripRecordingAvgSpeedKmh: Double
        get() {
            val hours = currentTripElapsedMillis / (MILLIS_PER_SECOND * 3600.0)
            return if (hours > 0.0) tripRecordingDistanceKm / hours else 0.0
        }

    /** EMA-smoothed, noise-gated speed in km/h. Drives the analog gauge needle (0–240 km/h scale). */
    val smoothedSpeedKmh: Double
        get() = vehicle.speedKmh

    /**
     * Calibrated heading in degrees `[0, 360)`. Drives the rotating compass ring and HUD readout.
     * - If there is a compass sensor on the device, uses the real-time compass sensor heading.
     * - If not, uses the GPS tracking bearing (course-over-ground forward azimuth).
     */
    val headingDegree: Float
        get() = if (hasCompassSensor && compassHeading != null) {
            compassHeading
        } else {
            vehicle.bearing
        }

    /** Current speed in [speedUnit], rounded for display. */
    val displaySpeed: Int
        get() = speedUnit.fromKmh(vehicle.speedKmh).roundToInt()

    /** Trip max speed in [speedUnit], rounded for display. */
    val displayMaxSpeed: Int
        get() = speedUnit.fromKmh(maxSpeedKmh).roundToInt()

    /** Average moving speed of the trip in km/h (distance / moving time). */
    val averageSpeedKmh: Double
        get() = if (movingTimeMillis > 0L) {
            (tripDistanceMeters / (movingTimeMillis / MILLIS_PER_SECOND)) * MPS_TO_KMH
        } else {
            0.0
        }

    /** Average moving speed in [speedUnit], rounded for display. */
    val displayAverageSpeed: Int
        get() = speedUnit.fromKmh(averageSpeedKmh).roundToInt()

    /** Trip distance in [speedUnit]'s distance unit (km, mi or nmi). */
    val displayTripDistance: Double
        get() = speedUnit.distanceFromMeters(tripDistanceMeters)

    /** Heading in whole degrees `[0, 359]`. */
    val headingDegrees: Int
        get() = ((headingDegree.roundToInt() % FULL_CIRCLE_DEGREES) + FULL_CIRCLE_DEGREES) % FULL_CIRCLE_DEGREES

    /** 16-point compass label for the current heading (e.g. "NNE"). */
    val cardinalDirection: String
        get() = VehicleData.getCompassDirection(headingDegree)

    /** Horizontal GPS accuracy in meters of the latest fix, or null when no fix is available. */
    val gpsAccuracyMeters: Float?
        get() = if (hasGpsFix) vehicle.accuracyMeters else null

    /** Rounded barometric altitude in meters above mean sea level (mdpl / m ASL). */
    val displayAltitudeMeters: Int?
        get() = altitudeMeters?.roundToInt()

    private companion object {
        const val MILLIS_PER_SECOND = 1000.0
        const val MPS_TO_KMH = 3.6
        const val FULL_CIRCLE_DEGREES = 360
    }
}

/**
 * Lifecycle of the tracking session.
 */
@Immutable
sealed interface TrackingStatus {
    /** Not tracking. Initial state and state after the user stops tracking. */
    data object Idle : TrackingStatus

    /** Location updates requested; waiting for the first valid fix. */
    data object Acquiring : TrackingStatus

    /** Receiving processed fixes. */
    data object Active : TrackingStatus

    /** Location permission denied or restricted by the user / system. */
    data object PermissionDenied : TrackingStatus

    /** Tracking stopped because the location provider failed. */
    data class Error(val message: String) : TrackingStatus
}

/**
 * Lifecycle of trip recording.
 */
@Immutable
sealed interface TripRecordingStatus {
    data object Idle : TripRecordingStatus
    data object Recording : TripRecordingStatus
    data object Paused : TripRecordingStatus
}

/**
 * Supported vehicle types for telemetry tracking.
 *
 * @property displayName Human-readable name.
 * @property defaultSpeedUnit Default speed unit for this vehicle type (e.g. KNOTS for SHIP & PLANE).
 */
enum class VehicleType(
    val displayName: String,
    val defaultSpeedUnit: SpeedUnit
) {
    CAR("Car", SpeedUnit.KMH),
    MOTORCYCLE("Motorcycle", SpeedUnit.KMH),
    TRAIN("Train", SpeedUnit.KMH),
    SHIP("Ship", SpeedUnit.KNOTS),
    PLANE("Plane", SpeedUnit.KNOTS);

    val iconSymbol: String
        get() = when (this) {
            CAR -> "🚗"
            MOTORCYCLE -> "🏍️"
            TRAIN -> "🚆"
            SHIP -> "🚢"
            PLANE -> "✈️"
        }

    fun next(): VehicleType = entries[(ordinal + 1) % entries.size]
}

/**
 * Display unit for speed and its paired distance unit.
 *
 * @property speedLabel Short speed unit label for the UI.
 * @property distanceLabel Short distance unit label for the UI.
 */
enum class SpeedUnit(
    val speedLabel: String,
    val distanceLabel: String,
    private val factorFromKmh: Double,
    private val metersPerDistanceUnit: Double
) {
    KMH(speedLabel = "km/h", distanceLabel = "km", factorFromKmh = 1.0, metersPerDistanceUnit = 1_000.0),
    MPH(speedLabel = "mph", distanceLabel = "mi", factorFromKmh = 0.62137119223733, metersPerDistanceUnit = 1_609.344),
    KNOTS(speedLabel = "kn", distanceLabel = "nmi", factorFromKmh = 0.53995680345572, metersPerDistanceUnit = 1_852.0);

    /** Converts a speed in km/h into this unit. */
    fun fromKmh(speedKmh: Double): Double = speedKmh * factorFromKmh

    /** Converts a distance in meters into this unit's distance unit. */
    fun distanceFromMeters(meters: Double): Double = meters / metersPerDistanceUnit

    /** Next unit in declaration order, wrapping around (KMH → MPH → KNOTS → KMH). */
    fun next(): SpeedUnit = entries[(ordinal + 1) % entries.size]
}

/**
 * Application Theme Mode preference.
 */
enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT;

    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}

// =================================================================================================
// INTENT — every user action the UI can send. The ONLY input into the ViewModel.
// =================================================================================================

sealed interface DashboardUiIntent {
    /** Start GPS tracking. No-op if already tracking. */
    data object StartTracking : DashboardUiIntent

    /** Stop GPS tracking. Trip statistics are preserved. */
    data object StopTracking : DashboardUiIntent

    /** Start if idle, stop if tracking. */
    data object ToggleTracking : DashboardUiIntent

    /** Result of the platform permission dialog launched after [DashboardUiEffect.RequestLocationPermission]. */
    data class PermissionResult(val granted: Boolean) : DashboardUiIntent

    /** Select a specific display unit. */
    data class ChangeSpeedUnit(val unit: SpeedUnit) : DashboardUiIntent

    /** Cycle to the next display unit (e.g. tapping the unit label). */
    data object CycleSpeedUnit : DashboardUiIntent

    /** Select a specific vehicle type (car, motorcycle, train, ship, plane). */
    data class SelectVehicle(val vehicleType: VehicleType) : DashboardUiIntent

    /** Select a specific application language. */
    data class ChangeLanguage(val language: AppLanguage) : DashboardUiIntent

    /** Select a specific theme mode (System Default, Dark, Light). */
    data class ChangeThemeMode(val themeMode: ThemeMode) : DashboardUiIntent

    /** Cycle to the next theme mode. */
    data object CycleThemeMode : DashboardUiIntent

    /** Reset max speed, trip distance and moving time. */
    data object ResetTrip : DashboardUiIntent

    /** Toggle wake lock / keep screen awake while the app is active. */
    data object ToggleKeepScreenOn : DashboardUiIntent

    /** Show the trip naming dialog when starting trip recording. */
    data object ShowTripNameDialog : DashboardUiIntent

    /** Dismiss the trip naming dialog. */
    data object DismissTripNameDialog : DashboardUiIntent

    /** Start recording a trip with the given name. */
    data class StartTripRecording(val name: String) : DashboardUiIntent

    /** Pause the current trip recording. */
    data object PauseTripRecording : DashboardUiIntent

    /** Resume the paused trip recording. */
    data object ResumeTripRecording : DashboardUiIntent

    /** Stop and save the current trip recording. */
    data object StopTripRecording : DashboardUiIntent

    /** Delete a recorded trip by ID. */
    data class DeleteTrip(val id: String) : DashboardUiIntent

    /** Open native map at coordinates with label. */
    data class OpenMap(val lat: Double, val lon: Double, val label: String) : DashboardUiIntent
}

// =================================================================================================
// EFFECT — one-shot side effects that must not be replayed on recomposition / config change.
// =================================================================================================

sealed interface DashboardUiEffect {
    /** UI must launch the platform location-permission request, then send [DashboardUiIntent.PermissionResult]. */
    data object RequestLocationPermission : DashboardUiEffect

    /** Permission was denied after being requested; UI should offer to open the system app settings. */
    data object OpenAppSettings : DashboardUiEffect

    /** Show a transient message (snackbar / toast). */
    data class ShowMessage(val message: String) : DashboardUiEffect
}
