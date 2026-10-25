package com.syseno.speedash.di

import com.syseno.speedash.data.BarometerTracker
import com.syseno.speedash.data.CompassTracker
import com.syseno.speedash.data.GpsTracker
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** Android bindings. Requires `androidContext(...)` in the Koin application declaration. */
actual val platformModule: Module = module {
    single { GpsTracker(androidContext()) }
    single { CompassTracker(androidContext()) }
    single { BarometerTracker(androidContext()) }
}
