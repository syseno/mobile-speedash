package com.syseno.speedash.presentation.util

import androidx.compose.runtime.Composable

/**
 * Platform-independent back handler composable.
 *
 * - Android: intercepts hardware / system gesture back presses.
 * - iOS: no-op since iOS relies on in-screen navigation affordances.
 */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
