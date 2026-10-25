package com.syseno.speedash.android

import android.app.Application
import com.syseno.speedash.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

/**
 * Android Application entry point initializing Koin dependency injection with Android context.
 */
class SpeedashApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger(Level.ERROR)
            androidContext(this@SpeedashApplication)
        }
    }
}
