package com.syseno.speedash.presentation.util

import androidx.compose.runtime.Composable

/**
 * Platform-independent composable effect that keeps the device screen awake and prevents
 * the device from sleeping while this composable is in the composition hierarchy.
 *
 * - Android: sets `FLAG_KEEP_SCREEN_ON` on the host Activity Window.
 * - iOS: sets `UIApplication.sharedApplication.idleTimerDisabled = true`.
 */
@Composable
expect fun KeepScreenOn()
