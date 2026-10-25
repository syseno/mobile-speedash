package com.syseno.speedash.domain.model

/**
 * Platform-agnostic raw location fix emitted by [com.syseno.speedash.data.GpsTracker].
 *
 * Alias of [RawGpsSignal] so platform trackers feed directly into
 * [com.syseno.speedash.domain.usecase.ProcessGpsSignalUseCase] without any mapping layer.
 */
typealias RawLocation = RawGpsSignal
