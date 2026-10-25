package com.syseno.speedash.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.syseno.speedash.domain.model.LocationPermissionException
import com.syseno.speedash.domain.model.LocationUnavailableException
import com.syseno.speedash.domain.model.RawLocation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/**
 * Android GPS source backed by Google Play Services [FusedLocationProviderClient].
 *
 * Requires `ACCESS_FINE_LOCATION` to be granted at runtime **before** collecting [locationFlow].
 *
 * @param context Any context; only the application context is retained (no Activity leaks).
 */
actual class GpsTracker(context: Context) {

    private val appContext: Context = context.applicationContext

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)

    private val locationRequest: LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MILLIS)
            .setMinUpdateIntervalMillis(UPDATE_INTERVAL_MILLIS)
            .setMaxUpdateDelayMillis(0L)          // No batching — deliver every fix immediately.
            .setMinUpdateDistanceMeters(0f)       // Speedometer needs fixes even when barely moving.
            .setWaitForAccurateLocation(false)
            .build()

    actual val locationFlow: Flow<RawLocation> = callbackFlow {
        if (!hasFineLocationPermission()) {
            close(LocationPermissionException("ACCESS_FINE_LOCATION permission is not granted."))
            return@callbackFlow
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    trySend(location.toRawLocation())
                }
            }
        }

        try {
            requestUpdates(callback).addOnFailureListener { error ->
                close(LocationUnavailableException("Fused location request failed.", error))
            }
        } catch (e: SecurityException) {
            // Permission revoked between the check and the request.
            close(LocationPermissionException("Location permission revoked: ${e.message}"))
            return@callbackFlow
        }

        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }.buffer(capacity = BUFFER_CAPACITY, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // Guarded by hasFineLocationPermission() + SecurityException catch.
    private fun requestUpdates(callback: LocationCallback): Task<Void> =
        fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())

    private fun Location.toRawLocation(): RawLocation = RawLocation(
        latitude = latitude,
        longitude = longitude,
        speedMps = if (hasSpeed()) speed else 0f,
        bearing = if (hasBearing()) bearing else null,
        altitudeMeters = if (hasAltitude()) altitude else null,
        accuracyMeters = if (hasAccuracy()) accuracy else 0f,
        timestampMillis = time
    )

    private companion object {
        const val UPDATE_INTERVAL_MILLIS: Long = 250L
        const val BUFFER_CAPACITY: Int = 16
    }
}
