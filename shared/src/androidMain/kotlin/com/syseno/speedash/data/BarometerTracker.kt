package com.syseno.speedash.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

/**
 * Android implementation of [BarometerTracker] utilizing [SensorManager] pressure sensor.
 */
actual class BarometerTracker(context: Context) {

    private val appContext: Context = context.applicationContext
    private val sensorManager: SensorManager? =
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    actual val isAvailable: Boolean
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE) != null

    actual val altitudeFlow: Flow<Float> = callbackFlow {
        if (!isAvailable || sensorManager == null) {
            close()
            return@callbackFlow
        }

        val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (pressureSensor == null) {
            close()
            return@callbackFlow
        }

        var lastAltitude = -9999f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_PRESSURE) {
                    val pressureHpa = event.values[0]
                    val altitudeMeters = SensorManager.getAltitude(
                        SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
                        pressureHpa
                    )

                    if (lastAltitude < -9000f || abs(altitudeMeters - lastAltitude) >= MIN_ALTITUDE_CHANGE_METERS) {
                        lastAltitude = altitudeMeters
                        trySend(altitudeMeters)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, pressureSensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }.buffer(capacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private companion object {
        const val MIN_ALTITUDE_CHANGE_METERS = 0.5f
    }
}
