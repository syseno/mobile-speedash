package com.syseno.speedash.presentation.util

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS does not have a global hardware system back button; UI provides in-screen back navigation
}
