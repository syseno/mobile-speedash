package com.syseno.speedash.presentation.util

/**
 * Multiplatform map opener function.
 * Opens the native map application at the given latitude, longitude, and label.
 *
 * - Android: uses `geo:` intent with fallback to browser maps.
 * - iOS: uses Apple Maps URL scheme (`http://maps.apple.com/?q=...`).
 */
expect fun openMap(lat: Double, lon: Double, label: String)
