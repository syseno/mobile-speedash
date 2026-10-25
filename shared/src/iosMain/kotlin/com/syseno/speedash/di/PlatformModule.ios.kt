package com.syseno.speedash.di

import com.syseno.speedash.data.BarometerTracker
import com.syseno.speedash.data.CompassTracker
import com.syseno.speedash.data.GpsTracker
import org.koin.core.module.Module
import org.koin.dsl.module

/** iOS bindings. */
actual val platformModule: Module = module {
    single { GpsTracker() }
    single { CompassTracker() }
    single { BarometerTracker() }
}
