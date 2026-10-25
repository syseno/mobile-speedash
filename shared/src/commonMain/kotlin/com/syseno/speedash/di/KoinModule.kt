package com.syseno.speedash.di

import com.syseno.speedash.data.repository.BarometerRepositoryImpl
import com.syseno.speedash.data.repository.CompassRepositoryImpl
import com.syseno.speedash.data.repository.LocationRepositoryImpl
import com.syseno.speedash.domain.repository.BarometerRepository
import com.syseno.speedash.domain.repository.CompassRepository
import com.syseno.speedash.domain.repository.LocationRepository
import com.syseno.speedash.domain.usecase.ObserveVehicleTelemetryUseCase
import com.syseno.speedash.domain.usecase.ProcessGpsSignalUseCase
import com.syseno.speedash.presentation.dashboard.DashboardViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Platform bindings (e.g. `GpsTracker`, which needs a `Context` on Android).
 * Provided by `androidMain` / `iosMain`.
 */
expect val platformModule: Module

/** Data layer: repository implementations bound to their domain interfaces. */
val dataModule: Module = module {
    singleOf(::LocationRepositoryImpl) { bind<LocationRepository>() }
    singleOf(::CompassRepositoryImpl) { bind<CompassRepository>() }
    singleOf(::BarometerRepositoryImpl) { bind<BarometerRepository>() }
}

/** Domain layer: stateless use cases (factory — each consumer gets a fresh pipeline). */
val domainModule: Module = module {
    factory { ProcessGpsSignalUseCase() }
    factoryOf(::ObserveVehicleTelemetryUseCase)
}

/** Presentation layer: ViewModels scoped to the platform's ViewModelStoreOwner. */
val presentationModule: Module = module {
    viewModelOf(::DashboardViewModel)
}

/** All shared modules in dependency order. */
fun sharedModules(): List<Module> = listOf(
    platformModule,
    dataModule,
    domainModule,
    presentationModule
)

/**
 * Starts Koin with all shared modules.
 *
 * - Android: `initKoin { androidContext(this@SpeedashApp) }` from `Application.onCreate()`.
 * - iOS: `KoinKt.doInitKoin()` from Swift in the `App` initializer.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(sharedModules())
}

/** Swift-friendly entry point (default lambda parameters are not exported to Objective-C). */
fun doInitKoin(): KoinApplication = initKoin()
