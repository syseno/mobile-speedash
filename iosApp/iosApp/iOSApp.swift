import SwiftUI
import SpeedashShared

@main
struct iOSApp: App {
    init() {
        KoinModuleKt.doInitKoin()
        UIApplication.shared.isIdleTimerDisabled = true
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.dark)
        }
    }
}
