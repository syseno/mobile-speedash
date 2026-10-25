package com.syseno.speedash.presentation.localization

import com.syseno.speedash.domain.model.AppLanguage
import com.syseno.speedash.presentation.dashboard.SpeedUnit
import com.syseno.speedash.presentation.dashboard.VehicleType

/**
 * Localization contract providing strongly-typed strings for all user-facing UI elements.
 */
interface AppStrings {
    // Header & Live Status
    val statusStandby: String
    val statusAcquiring: String
    val statusActive: String
    val statusPermissionDenied: String
    val statusGpsError: String
    val noFix: String
    val sensorCompass: String
    val sensorGps: String
    fun accuracyFix(meters: Int): String
    fun altitudeFormat(meters: Int): String

    // Compass Cardinal Directions
    fun cardinalDirection(bearing: Float): String

    // Telemetry Cards
    val maxSpeed: String
    val avgSpeed: String
    val tripDistance: String

    // Bottom Action Controls
    val reset: String
    val startTelemetry: String
    val stopTelemetry: String

    // Settings Screen Navigation & Headers
    val settingsTitle: String
    val settingsSubtitle: String
    val backButton: String
    val applyAndClose: String

    // Language Section
    val languageSectionTitle: String
    val languageSectionSubtitle: String
    val englishOption: String
    val englishOptionSubtitle: String
    val indonesianOption: String
    val indonesianOptionSubtitle: String

    // Theme Mode Section
    val themeSectionTitle: String
    val themeSectionSubtitle: String
    val themeSystemOption: String
    val themeSystemOptionSubtitle: String
    val themeDarkOption: String
    val themeDarkOptionSubtitle: String
    val themeLightOption: String
    val themeLightOptionSubtitle: String

    // Vehicle Mode Section
    val vehicleSectionTitle: String
    val vehicleSectionSubtitle: String
    fun vehicleName(type: VehicleType): String
    fun vehicleShortName(type: VehicleType): String

    // Speed Measurement Unit Section
    val unitSectionTitle: String
    val unitSectionSubtitle: String
    fun unitDescription(unit: SpeedUnit): String

    // Screen Wake Lock Section
    val wakeLockSectionTitle: String
    val wakeLockSectionSubtitle: String
    val wakeLockActive: String
    val wakeLockOff: String
    val wakeLockActiveDescription: String
    val wakeLockOffDescription: String

    // Heading Sensor Source Section
    val sensorSectionTitle: String
    val sensorSectionSubtitle: String
    val compassHardwareTitle: String
    val compassHardwareDescription: String
    val gpsHeadingTitle: String
    val gpsHeadingDescription: String

    // About & System Info Section
    val aboutSectionTitle: String
    val aboutSectionSubtitle: String
    val aboutAppDescription: String

    // Transient Messages / Alerts
    val messagePermissionRequired: String
    val messageGpsUnavailable: String
    val messageUnexpectedError: String

    companion object {
        fun forLanguage(language: AppLanguage): AppStrings = when (language) {
            AppLanguage.ENGLISH -> EnglishStrings
            AppLanguage.INDONESIAN -> IndonesianStrings
        }
    }
}

/**
 * English localization implementation.
 */
internal object EnglishStrings : AppStrings {
    override val statusStandby: String = "STANDBY"
    override val statusAcquiring: String = "ACQUIRING..."
    override val statusActive: String = "TRACKING LIVE"
    override val statusPermissionDenied: String = "NO PERMISSION"
    override val statusGpsError: String = "GPS ERROR"
    override val noFix: String = "NO FIX"
    override val sensorCompass: String = "COMPASS"
    override val sensorGps: String = "GPS"

    override fun accuracyFix(meters: Int): String = "±${meters}m ACCURACY"
    override fun altitudeFormat(meters: Int): String = "$meters m ASL"

    private val CARDINAL_POINTS_EN = arrayOf(
        "N", "NNE", "NE", "ENE",
        "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW",
        "W", "WNW", "NW", "NNW"
    )

    override fun cardinalDirection(bearing: Float): String {
        val normalized = ((bearing % 360f) + 360f) % 360f
        val index = (((normalized + 11.25f) / 22.5f).toInt()) % 16
        return CARDINAL_POINTS_EN[index]
    }

    override val maxSpeed: String = "MAX SPEED"
    override val avgSpeed: String = "AVG SPEED"
    override val tripDistance: String = "TRIP DIST"

    override val reset: String = "RESET"
    override val startTelemetry: String = "START TELEMETRY"
    override val stopTelemetry: String = "STOP TELEMETRY"

    override val settingsTitle: String = "SETTINGS"
    override val settingsSubtitle: String = "Cockpit Preferences & Diagnostics"
    override val backButton: String = "BACK"
    override val applyAndClose: String = "APPLY & RETURN TO DASHBOARD"

    override val languageSectionTitle: String = "LANGUAGE"
    override val languageSectionSubtitle: String = "Select application display language"
    override val englishOption: String = "English"
    override val englishOptionSubtitle: String = "United States / International"
    override val indonesianOption: String = "Bahasa Indonesia"
    override val indonesianOptionSubtitle: String = "Indonesia"

    override val themeSectionTitle: String = "THEME MODE"
    override val themeSectionSubtitle: String = "Choose between dark cockpit, light cockpit, or follow system default."
    override val themeSystemOption: String = "System Default"
    override val themeSystemOptionSubtitle: String = "Automatically adapts to device light/dark mode"
    override val themeDarkOption: String = "Dark Cockpit"
    override val themeDarkOptionSubtitle: String = "Deep space dark theme optimized for night driving"
    override val themeLightOption: String = "Light Cockpit"
    override val themeLightOptionSubtitle: String = "High contrast light theme optimized for daylight driving"

    override val vehicleSectionTitle: String = "VEHICLE MODE"
    override val vehicleSectionSubtitle: String = "Selecting Ship or Plane automatically sets speed unit to Knots."

    override fun vehicleName(type: VehicleType): String = when (type) {
        VehicleType.CAR -> "Car"
        VehicleType.MOTORCYCLE -> "Motorcycle"
        VehicleType.TRAIN -> "Train"
        VehicleType.SHIP -> "Ship"
        VehicleType.PLANE -> "Plane"
    }

    override fun vehicleShortName(type: VehicleType): String = when (type) {
        VehicleType.MOTORCYCLE -> "MOTO"
        else -> type.displayName.uppercase()
    }

    override val unitSectionTitle: String = "SPEED MEASUREMENT UNIT"
    override val unitSectionSubtitle: String = "Customize your measurement unit across all telemetry gauges."

    override fun unitDescription(unit: SpeedUnit): String = when (unit) {
        SpeedUnit.KMH -> "Metric"
        SpeedUnit.MPH -> "Imperial"
        SpeedUnit.KNOTS -> "Nautical"
    }

    override val wakeLockSectionTitle: String = "SCREEN WAKE LOCK"
    override val wakeLockSectionSubtitle: String = "Keep display illuminated while driving or dashboard is active."
    override val wakeLockActive: String = "ACTIVE"
    override val wakeLockOff: String = "OFF"
    override val wakeLockActiveDescription: String = "Display stays awake indefinitely"
    override val wakeLockOffDescription: String = "Display follows system timeout"

    override val sensorSectionTitle: String = "HEADING SENSOR SOURCE"
    override val sensorSectionSubtitle: String = "Compass sensor is prioritized; falls back to GPS tracking if unavailable."
    override val compassHardwareTitle: String = "COMPASS HARDWARE SENSOR"
    override val compassHardwareDescription: String = "Built-in magnetometer detected (tilt-compensated)"
    override val gpsHeadingTitle: String = "GPS TRACKING HEADING"
    override val gpsHeadingDescription: String = "No compass sensor found; using GPS forward azimuth"

    override val aboutSectionTitle: String = "SYSTEM INFORMATION"
    override val aboutSectionSubtitle: String = "Speedash v1.0 • Precision GPS Telemetry"
    override val aboutAppDescription: String =
        "High-precision digital speedometer dashboard built with Kotlin Multiplatform & Compose Multiplatform using Clean Architecture and MVI pattern."

    override val messagePermissionRequired: String = "Location permission is required to measure speed."
    override val messageGpsUnavailable: String = "GPS is unavailable. Check that location services are enabled."
    override val messageUnexpectedError: String = "Tracking stopped due to an unexpected error."
}

/**
 * Indonesian localization implementation (Bahasa Indonesia).
 */
internal object IndonesianStrings : AppStrings {
    override val statusStandby: String = "SIAGA"
    override val statusAcquiring: String = "MENCARI GPS..."
    override val statusActive: String = "PELACAKAN AKTIF"
    override val statusPermissionDenied: String = "IZIN DITOLAK"
    override val statusGpsError: String = "KESALAHAN GPS"
    override val noFix: String = "BELUM ADA SINYAL"
    override val sensorCompass: String = "KOMPAS"
    override val sensorGps: String = "GPS"

    override fun accuracyFix(meters: Int): String = "±${meters}m AKURASI"
    override fun altitudeFormat(meters: Int): String = "$meters mdpl"

    private val CARDINAL_POINTS_ID = arrayOf(
        "U", "UTL", "TL", "TTL",
        "T", "TTG", "TG", "STG",
        "S", "SBD", "BD", "BBD",
        "B", "BBL", "BL", "UBL"
    )

    override fun cardinalDirection(bearing: Float): String {
        val normalized = ((bearing % 360f) + 360f) % 360f
        val index = (((normalized + 11.25f) / 22.5f).toInt()) % 16
        return CARDINAL_POINTS_ID[index]
    }

    override val maxSpeed: String = "KECEPATAN MAKS"
    override val avgSpeed: String = "KECEPATAN RATA2"
    override val tripDistance: String = "JARAK TEMPUH"

    override val reset: String = "RESET"
    override val startTelemetry: String = "MULAI PELACAKAN"
    override val stopTelemetry: String = "HENTIKAN PELACAKAN"

    override val settingsTitle: String = "PENGATURAN"
    override val settingsSubtitle: String = "Preferensi Kokpit & Diagnostik Perangkat"
    override val backButton: String = "KEMBALI"
    override val applyAndClose: String = "TERAPKAN & KEMBALI KE DASHBOARD"

    override val languageSectionTitle: String = "BAHASA"
    override val languageSectionSubtitle: String = "Pilih bahasa tampilan aplikasi Speedash"
    override val englishOption: String = "English"
    override val englishOptionSubtitle: String = "Inggris / Internasional"
    override val indonesianOption: String = "Bahasa Indonesia"
    override val indonesianOptionSubtitle: String = "Indonesia (Default)"

    override val themeSectionTitle: String = "MODE TEMA"
    override val themeSectionSubtitle: String = "Pilih antara tema gelap, tema terang, atau ikuti pengaturan perangkat."
    override val themeSystemOption: String = "Default Sistem"
    override val themeSystemOptionSubtitle: String = "Otomatis menyesuaikan dengan mode terang/gelap perangkat"
    override val themeDarkOption: String = "Kokpit Gelap"
    override val themeDarkOptionSubtitle: String = "Tema gelap yang dioptimalkan untuk berkendara malam"
    override val themeLightOption: String = "Kokpit Terang"
    override val themeLightOptionSubtitle: String = "Tema terang kontras tinggi yang dioptimalkan untuk berkendara siang"

    override val vehicleSectionTitle: String = "MODE KENDARAAN"
    override val vehicleSectionSubtitle: String = "Memilih Kapal atau Pesawat otomatis mengatur satuan ke Knot."

    override fun vehicleName(type: VehicleType): String = when (type) {
        VehicleType.CAR -> "Mobil"
        VehicleType.MOTORCYCLE -> "Sepeda Motor"
        VehicleType.TRAIN -> "Kereta Api"
        VehicleType.SHIP -> "Kapal Laut"
        VehicleType.PLANE -> "Pesawat Terbang"
    }

    override fun vehicleShortName(type: VehicleType): String = when (type) {
        VehicleType.CAR -> "MOBIL"
        VehicleType.MOTORCYCLE -> "MOTOR"
        VehicleType.TRAIN -> "KERETA"
        VehicleType.SHIP -> "KAPAL"
        VehicleType.PLANE -> "PESAWAT"
    }

    override val unitSectionTitle: String = "SATUAN PENGUKURAN KECEPATAN"
    override val unitSectionSubtitle: String = "Sesuaikan satuan pengukuran di seluruh instrumen telemetri."

    override fun unitDescription(unit: SpeedUnit): String = when (unit) {
        SpeedUnit.KMH -> "Metrik"
        SpeedUnit.MPH -> "Imperial"
        SpeedUnit.KNOTS -> "Nautika"
    }

    override val wakeLockSectionTitle: String = "LAYAR TETAP MENYALA (WAKE LOCK)"
    override val wakeLockSectionSubtitle: String = "Jaga layar tetap menyala saat berkendara atau dashboard aktif."
    override val wakeLockActive: String = "AKTIF"
    override val wakeLockOff: String = "MATI"
    override val wakeLockActiveDescription: String = "Layar tetap menyala tanpa batas waktu"
    override val wakeLockOffDescription: String = "Layar mengikuti batas waktu mati sistem"

    override val sensorSectionTitle: String = "SUMBER SENSOR ARAH"
    override val sensorSectionSubtitle: String = "Sensor kompas fisik diprioritaskan; beralih ke GPS jika tidak tersedia."
    override val compassHardwareTitle: String = "SENSOR KOMPAS PERANGKAT"
    override val compassHardwareDescription: String = "Magnetometer internal terdeteksi (kompensasi kemiringan)"
    override val gpsHeadingTitle: String = "ARAH PELACAKAN GPS"
    override val gpsHeadingDescription: String = "Sensor kompas tidak ditemukan; memakai azimuth maju GPS"

    override val aboutSectionTitle: String = "INFORMASI SISTEM"
    override val aboutSectionSubtitle: String = "Speedash v1.0 • Speedometer GPS Presisi"
    override val aboutAppDescription: String =
        "Dashboard speedometer digital presisi tinggi berbasis Kotlin Multiplatform & Compose Multiplatform dengan Clean Architecture dan pola MVI."

    override val messagePermissionRequired: String = "Izin lokasi diperlukan untuk mengukur kecepatan."
    override val messageGpsUnavailable: String = "GPS tidak tersedia. Pastikan layanan lokasi telah aktif."
    override val messageUnexpectedError: String = "Pelacakan terhenti karena kesalahan tak terduga."
}
