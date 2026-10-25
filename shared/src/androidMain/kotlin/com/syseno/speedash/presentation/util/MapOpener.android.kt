package com.syseno.speedash.presentation.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private class MapOpenerHelper : KoinComponent {
    val context: Context by inject()
}

actual fun openMap(lat: Double, lon: Double, label: String) {
    try {
        val helper = MapOpenerHelper()
        val encodedLabel = Uri.encode(label)
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($encodedLabel)")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        helper.context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val helper = MapOpenerHelper()
            val fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            helper.context.startActivity(fallbackIntent)
        } catch (_: Exception) {}
    }
}
