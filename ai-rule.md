# Speedash — AI Agent Rules

> Every AI agent (Antigravity, Copilot, Cursor, Claude, Gemini, etc.) **must read this file before
> generating or modifying code** in this repository.

## 1. Project Identity

| Key            | Value                                   |
|----------------|-----------------------------------------|
| App name       | Speedash (GPS speedometer / dashboard)  |
| Package / ns   | `com.syseno.speedash`                   |
| Architecture   | Kotlin Multiplatform + Compose Multiplatform, Clean Architecture |
| Targets        | Android (minSdk 26, compileSdk 35), iOS (x64, arm64, simulatorArm64) |
| iOS framework  | `SpeedashShared` (static)               |
| JVM target     | 21                                      |

## 2. Tech Stack (single source of truth: `gradle/libs.versions.toml`)

- Kotlin **2.1+**, Compose Compiler plugin (`org.jetbrains.kotlin.plugin.compose`)
- Compose Multiplatform, Material 3
- Koin 4.x (DI) — no Hilt/Dagger
- KotlinX Coroutines + Flow — no RxJava, no LiveData
- Google Play Services Location (Android `FusedLocationProviderClient`)
- CoreLocation via Kotlin/Native interop (iOS)
- Tests: `kotlin.test`, `kotlinx-coroutines-test`, Turbine, `koin-test`

**Never hardcode dependency versions in `build.gradle.kts`.** Add them to the version catalog and
reference via `libs.*`.

## 3. Module & Package Layout

```
shared/src/
├── commonMain/kotlin/com/syseno/speedash/
│   ├── domain/            # Pure Kotlin. NO platform, NO Android, NO Compose imports.
│   │   ├── model/         # Immutable data classes (VehicleData, RawGpsSignal)
│   │   ├── repository/    # Interfaces only
│   │   └── usecase/       # Business logic (ProcessGpsSignalUseCase)
│   ├── data/              # Repository implementations, expect declarations
│   ├── presentation/      # ViewModels, UI state, Compose screens
│   └── di/                # Koin modules
├── androidMain/           # actual implementations (FusedLocationProvider)
├── iosMain/               # actual implementations (CLLocationManager)
└── commonTest/            # Unit tests for domain & data
```

Dependency direction: `presentation → domain ← data`. Domain depends on nothing.

## 4. Domain Invariants (DO NOT change without explicit approval)

| Rule                    | Value                                                     |
|-------------------------|-----------------------------------------------------------|
| EMA smoothing           | `S_t = α·X_t + (1−α)·S_(t−1)`, **α = 0.25**              |
| Noise gate              | Smoothed speed **< 3.0 km/h ⇒ 0.0**                       |
| EMA state               | Stores the **un-gated** EMA so decay stays continuous     |
| Distance                | Haversine, Earth radius `6_371_008.8 m`                   |
| Bearing                 | Forward azimuth, normalized to `[0, 360)`                 |
| Bearing while stopped   | **Held** (not recomputed) to prevent compass jitter       |
| Distance while stopped  | Not accumulated (GPS drift)                               |
| Units                   | Internal speed in **km/h**; m/s ⇒ km/h factor `3.6`       |
| Rejected fixes          | Invalid coords, NaN, accuracy > 50 m, out-of-order timestamps |

## 5. Coding Standards

1. **Immutability**: domain models are `data class` with `val` only. Use `copy()` for changes.
2. **Concurrency**: expose `Flow`/`StateFlow`; never block threads. Inject `CoroutineDispatcher`
   (default `Dispatchers.Default` for math, `Dispatchers.IO` only on platforms that support it).
3. **No `!!`** — use safe calls, `requireNotNull`, or explicit error handling.
4. **No `GlobalScope`**; scope work to `viewModelScope` or an injected `CoroutineScope`.
5. Pure math helpers live in `companion object` and must be unit-testable without coroutines.
6. Validate config with `require(...)` in `init` blocks.
7. Public APIs need KDoc explaining units (km/h, m/s, meters, degrees, epoch ms).
8. Use `expect`/`actual` only when an interface + Koin binding is not sufficient.
9. Prefer `kotlin.math` over platform math libraries in `commonMain`.
10. Format: official Kotlin style, 4-space indent, max line length 120.

## 6. Output Rules for AI Agents

- Write **complete, production-ready code** — no `TODO`, no `...`, no truncated files.
- Keep the package `com.syseno.speedash` and the layout above.
- Don't delete existing comments/KDoc unrelated to your change.
- When adding a dependency, update `libs.versions.toml` **and** explain why.
- When changing a domain invariant (section 4), stop and ask the user first.
- Add/update tests in `commonTest` for any domain or use-case change.
- Location permissions: Android `ACCESS_FINE_LOCATION` (+ foreground service if background);
  iOS `NSLocationWhenInUseUsageDescription` in `Info.plist`.

## 7. Roadmap

- [x] **Step 1** — Version catalog, shared build script, `VehicleData`, `ProcessGpsSignalUseCase`
- [x] **Step 2** — `expect/actual GpsTracker` (Fused 250 ms HIGH_ACCURACY / CLLocationManager BestForNavigation),
      `RawLocation` = typealias of `RawGpsSignal`, Android manifest + iOS `Info.plist` permissions.
      Rules: `GpsTracker` is constructed only in platform code/Koin modules; iOS tracker runs on `Dispatchers.Main`.
- [x] **Step 3** — MVI `DashboardContract` / `DashboardViewModel`, `LocationRepository`, `ObserveVehicleTelemetryUseCase`,
      Koin modules (`platformModule` expect/actual, `dataModule`, `domainModule`, `presentationModule`, `initKoin`).
      Rules: UI talks to the ViewModel **only** via `onIntent(DashboardUiIntent)`; state changes **only** in the pure
      `DashboardReducer`; one-shot events via `effects` (Channel). Location exceptions live in `domain/model`.
      ViewModel uses `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` (KMP).
- [x] **Step 4** — `DashboardScreen.kt`: Pure Compose Multiplatform Canvas dashboard with dark cockpit theme.
      Circular gauge arc (0–240 km/h), dynamic animated needle (`smoothedSpeedKmh`), center HUD with "SPEEDASH"
      branding and live status, outer rotating compass ring (`headingDegree`), and full MVI state/intent integration.
- [x] **Step 5** — Android & iOS app entry points, runtime permissions, and unit tests:
      - Android app (`SpeedashApplication`, `MainActivity` with runtime location permissions and Settings launcher).
      - iOS app (`MainViewController.kt`, SwiftUI `iOSApp.swift`, `ContentView.swift`, `Info.plist`).
      - Complete unit test suite (`ProcessGpsSignalUseCaseTest`, `VehicleDataTest`, `DashboardViewModelTest`).
      - Root Gradle configuration (`settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, Gradle wrapper 8.11.1).
      - All tests verified passing via `./gradlew :shared:testDebugUnitTest` and `:androidApp:assembleDebug` builds cleanly.
- [x] **Step 6** — Hardware Compass Sensor Calibration:
      - Hardware compass sensor tracking via `expect/actual CompassTracker` and `CompassRepository`.
      - Android: `SensorManager` with `Sensor.TYPE_ROTATION_VECTOR` (fallback to accelerometer + magnetometer), screen orientation remapping (`SensorManager.remapCoordinateSystem`), and low-pass jitter filtering.
      - iOS: `CLLocationManager.startUpdatingHeading()` with `kCLHeadingFilterNone`.
      - Heading Sensor Fusion: When in standby or stationary (speed < 3.0 km/h), `headingDegree` is driven by the physical hardware compass sensor in real-time. When actively moving ($\ge 3.0$ km/h), heading follows GPS course-over-ground forward azimuth.
      - MVI integration: `DashboardResult.CompassHeadingUpdated` reduced into `DashboardUiState.compassHeading` and tested in `DashboardViewModelTest`.
- [x] **Step 7** — Hardware Compass Sensor Prioritization & Multiplatform Screen Wake Lock:
      - Heading Sensor Selection: Heading prioritizes the physical compass sensor when available on the device (`CompassTracker.isAvailable`). When no compass sensor is present (tablets, emulators, devices without magnetometer), heading cleanly falls back to GPS tracking course-over-ground forward azimuth (`vehicle.bearing`).
      - Screen Wake Lock: Multiplatform `KeepScreenOn` effect ensures the device screen stays awake while the speedometer is running (`FLAG_KEEP_SCREEN_ON` on Android window, `idleTimerDisabled` on iOS), with an interactive toggle in Dashboard Settings and `android.permission.WAKE_LOCK`.
- [x] **Step 8** — Multilingual Localization (English & Bahasa Indonesia) & Full-Screen Options:
      - Domain model: `AppLanguage` enum (`ENGLISH`, `INDONESIAN`) in `domain/model/AppLanguage.kt`.
      - MVI integration: `DashboardUiState.language`, `DashboardUiIntent.ChangeLanguage`, `DashboardResult.LanguageChanged` reduced in `DashboardReducer` and tested in `DashboardViewModelTest`.
      - Localization system: `AppStrings` strongly typed multiplatform localization with `EnglishStrings` and `IndonesianStrings` implementations covering status badges, GPS metrics, localized cardinal compass points (U, TL, T, TG, S, BD, B, BL vs N, NE, E, SE, S, SW, W, NW), vehicle types, speed units, wake lock toggles, sensor diagnostics, and snackbars.
      - Full-Screen Options: Upgraded the previous modal overlay dialog to a full-screen `DashboardSettingsScreen` with top navigation bar, back button, Android `BackHandler`, interactive language selector cards, vehicle modes, unit pickers, screen wake lock, hardware diagnostics, and smooth `AnimatedContent` screen transition.


