package com.syseno.speedash.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreMotion.CMAltimeter
import platform.CoreMotion.CMAltitudeData
import platform.Foundation.NSOperationQueue
import kotlin.math.pow

/**
 * iOS implementation of [BarometerTracker] backed by [CMAltimeter].
 */
actual class BarometerTracker {

    actual val isAvailable: Boolean
        get() = CMAltimeter.isRelativeAltitudeAvailable()

    @OptIn(ExperimentalForeignApi::class)
    actual val altitudeFlow: Flow<Float> = callbackFlow {
        if (!CMAltimeter.isRelativeAltitudeAvailable()) {
            close()
            return@callbackFlow
        }

        val altimeter = CMAltimeter()
        val queue = NSOperationQueue.mainQueue

        altimeter.startRelativeAltitudeUpdatesToQueue(queue) { data: CMAltitudeData?, error ->
            if (data != null) {
                val pressureKpa = data.pressure.doubleValue
                val pressureHpa = pressureKpa * 10.0
                // International Barometric Formula: h = 44330.76 * (1 - (P / P0)^0.190284)
                val altitudeMeters = (44330.76 * (1.0 - (pressureHpa / 1013.25).pow(0.190284))).toFloat()
                trySend(altitudeMeters)
            }
        }

        awaitClose {
            altimeter.stopRelativeAltitudeUpdates()
        }
    }
        .buffer(capacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        .flowOn(Dispatchers.Main)
}
