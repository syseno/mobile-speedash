package com.syseno.speedash.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Android implementation of [CompassTracker] utilizing [SensorManager] orientation sensors.
 *
 * Implements full 3D tilt compensation and dynamic display orientation remapping:
 * - Uses [Sensor.TYPE_ROTATION_VECTOR] (fusing gyroscope, accelerometer, and magnetometer)
 *   with fallback to [Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR] or dual accelerometer/magnetometer.
 * - Rather than naive Euler angles (which suffer from gimbal lock and 90°/180° inversions when
 *   tilted upright or held in hand), projects the forward line-of-sight vector onto the Earth's
 *   horizontal plane for continuous, accurate heading regardless of device pitch or mount angle.
 * - Dynamically adapts to [DisplayManager] screen rotation (portrait, landscape left/right, inverted).
 */
actual class CompassTracker(context: Context) {

    private val appContext: Context = context.applicationContext
    private val sensorManager: SensorManager? =
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val displayManager: DisplayManager? =
        appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    actual val isAvailable: Boolean
        get() {
            if (sensorManager == null) return false
            val hasRotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
                sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null
            if (hasRotation) return true
            val hasAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
            val hasMag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
            return hasAccel && hasMag
        }

    actual val headingFlow: Flow<Float> = callbackFlow {
        if (!isAvailable || sensorManager == null) {
            close()
            return@callbackFlow
        }

        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

        if (rotationVectorSensor != null) {
            // Path A: Hardware-fused rotation vector (gyro + accel + mag)
            val rotationMatrix = FloatArray(9)
            var lastHeading = -1f

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == rotationVectorSensor.type) {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                        val displayRotation = getDisplayRotation()
                        val headingDeg = computeHeadingFromRotationMatrix(rotationMatrix, displayRotation)
                            ?: return

                        if (lastHeading < 0f || angularDelta(headingDeg, lastHeading) >= MIN_HEADING_CHANGE_DEGREES) {
                            lastHeading = headingDeg
                            trySend(headingDeg)
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            awaitClose { sensorManager.unregisterListener(listener) }
        } else {
            // Path B: Fallback dual-sensor (Accelerometer + Magnetometer)
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            if (accelerometer == null || magnetometer == null) {
                close()
                return@callbackFlow
            }

            val gravity = FloatArray(3)
            val geomagnetic = FloatArray(3)
            val rotationMatrix = FloatArray(9)
            var hasGravity = false
            var hasGeomagnetic = false
            var lastHeading = -1f

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            event.values.copyInto(gravity)
                            hasGravity = true
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            event.values.copyInto(geomagnetic)
                            hasGeomagnetic = true
                        }
                    }

                    if (hasGravity && hasGeomagnetic) {
                        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                        if (success) {
                            val displayRotation = getDisplayRotation()
                            val headingDeg = computeHeadingFromRotationMatrix(rotationMatrix, displayRotation)
                                ?: return

                            if (lastHeading < 0f || angularDelta(headingDeg, lastHeading) >= MIN_HEADING_CHANGE_DEGREES) {
                                lastHeading = headingDeg
                                trySend(headingDeg)
                            }
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
            awaitClose { sensorManager.unregisterListener(listener) }
        }
    }.buffer(capacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private fun getDisplayRotation(): Int {
        return displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
    }

    /**
     * Computes the forward-azimuth heading in degrees [0, 360) projected onto Earth's horizontal plane.
     *
     * In device coordinates:
     * - When flat on a table, the user looks down at the screen, and the forward direction is the top edge (+Y).
     * - When upright in portrait (held in hand or car mount), the line of sight points forward through the back (-Z).
     * - As the device tilts at angle θ, the forward vector is cos(θ)*Y - sin(θ)*Z.
     *
     * In world coordinates, the transformed vector w = R * v directly isolates the horizontal East/North components,
     * ensuring stable, accurate heading that matches native compass apps at any tilt angle without 90°/180° jumps.
     */
    private fun computeHeadingFromRotationMatrix(rotationMatrix: FloatArray, displayRotation: Int): Float? {
        val (vx, vy, vz) = when (displayRotation) {
            Surface.ROTATION_90 -> Triple(rotationMatrix[8], 0f, -rotationMatrix[6])
            Surface.ROTATION_180 -> Triple(0f, -rotationMatrix[8], rotationMatrix[7])
            Surface.ROTATION_270 -> Triple(-rotationMatrix[8], 0f, rotationMatrix[6])
            else -> Triple(0f, rotationMatrix[8], -rotationMatrix[7]) // Surface.ROTATION_0 (Portrait)
        }

        // Project the forward sight vector into world coordinates: w = R * v
        val wx = rotationMatrix[0] * vx + rotationMatrix[1] * vy + rotationMatrix[2] * vz
        val wy = rotationMatrix[3] * vx + rotationMatrix[4] * vy + rotationMatrix[5] * vz

        // Avoid division/singularity when pointing straight at zenith/nadir
        if (hypot(wx, wy) < 0.05f) {
            return null
        }

        val azimuthRad = atan2(wx, wy)
        return ((Math.toDegrees(azimuthRad.toDouble()).toFloat() % 360f) + 360f) % 360f
    }

    private fun angularDelta(a: Float, b: Float): Float {
        val diff = abs(a - b) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    private companion object {
        const val MIN_HEADING_CHANGE_DEGREES = 0.5f
    }
}
