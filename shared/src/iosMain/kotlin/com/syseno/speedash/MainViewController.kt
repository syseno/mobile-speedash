package com.syseno.speedash

import androidx.compose.ui.window.ComposeUIViewController
import com.syseno.speedash.presentation.dashboard.DashboardScreen
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIViewController

/**
 * iOS entry point creating the root [UIViewController] hosting [DashboardScreen].
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    DashboardScreen(
        onRequestPermission = {
            // On iOS, CLLocationManager automatically triggers the system authorization dialog
            // when startUpdatingLocation() or requestWhenInUseAuthorization() is invoked.
        },
        onOpenSettings = {
            val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
            if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
                UIApplication.sharedApplication.openURL(settingsUrl)
            }
        }
    )
}
