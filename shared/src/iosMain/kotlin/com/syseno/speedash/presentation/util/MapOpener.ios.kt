package com.syseno.speedash.presentation.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openMap(lat: Double, lon: Double, label: String) {
    val encodedLabel = label.replace(" ", "%20")
    val urlString = "http://maps.apple.com/?q=$encodedLabel&ll=$lat,$lon"
    val url = NSURL.URLWithString(urlString)
    if (url != null) {
        UIApplication.sharedApplication.openURL(url)
    }
}
