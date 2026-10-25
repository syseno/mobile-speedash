package com.syseno.speedash.presentation.localization

import com.syseno.speedash.domain.model.AppLanguage
import com.syseno.speedash.presentation.dashboard.SpeedUnit
import com.syseno.speedash.presentation.dashboard.VehicleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AppStringsTest {

    @Test
    fun forLanguage_returnsCorrectImplementation() {
        val en = AppStrings.forLanguage(AppLanguage.ENGLISH)
        val id = AppStrings.forLanguage(AppLanguage.INDONESIAN)

        assertEquals("STANDBY", en.statusStandby)
        assertEquals("SIAGA", id.statusStandby)

        assertEquals("SETTINGS", en.settingsTitle)
        assertEquals("PENGATURAN", id.settingsTitle)

        assertEquals("BACK", en.backButton)
        assertEquals("KEMBALI", id.backButton)
    }

    @Test
    fun cardinalDirections_localizedAccurately() {
        val en = AppStrings.forLanguage(AppLanguage.ENGLISH)
        val id = AppStrings.forLanguage(AppLanguage.INDONESIAN)

        // North: 0 deg
        assertEquals("N", en.cardinalDirection(0f))
        assertEquals("U", id.cardinalDirection(0f))

        // East: 90 deg
        assertEquals("E", en.cardinalDirection(90f))
        assertEquals("T", id.cardinalDirection(90f))

        // South: 180 deg
        assertEquals("S", en.cardinalDirection(180f))
        assertEquals("S", id.cardinalDirection(180f))

        // West: 270 deg
        assertEquals("W", en.cardinalDirection(270f))
        assertEquals("B", id.cardinalDirection(270f))

        // Northeast: 45 deg
        assertEquals("NE", en.cardinalDirection(45f))
        assertEquals("TL", id.cardinalDirection(45f))

        // Southeast: 135 deg
        assertEquals("SE", en.cardinalDirection(135f))
        assertEquals("TG", id.cardinalDirection(135f))

        // Southwest: 225 deg
        assertEquals("SW", en.cardinalDirection(225f))
        assertEquals("BD", id.cardinalDirection(225f))

        // Northwest: 315 deg
        assertEquals("NW", en.cardinalDirection(315f))
        assertEquals("BL", id.cardinalDirection(315f))
    }

    @Test
    fun vehicleNames_localizedAccurately() {
        val en = AppStrings.forLanguage(AppLanguage.ENGLISH)
        val id = AppStrings.forLanguage(AppLanguage.INDONESIAN)

        assertEquals("Car", en.vehicleName(VehicleType.CAR))
        assertEquals("Mobil", id.vehicleName(VehicleType.CAR))

        assertEquals("Motorcycle", en.vehicleName(VehicleType.MOTORCYCLE))
        assertEquals("Sepeda Motor", id.vehicleName(VehicleType.MOTORCYCLE))

        assertEquals("Train", en.vehicleName(VehicleType.TRAIN))
        assertEquals("Kereta Api", id.vehicleName(VehicleType.TRAIN))

        assertEquals("Ship", en.vehicleName(VehicleType.SHIP))
        assertEquals("Kapal Laut", id.vehicleName(VehicleType.SHIP))

        assertEquals("Plane", en.vehicleName(VehicleType.PLANE))
        assertEquals("Pesawat Terbang", id.vehicleName(VehicleType.PLANE))
    }

    @Test
    fun speedUnitDescriptions_localizedAccurately() {
        val en = AppStrings.forLanguage(AppLanguage.ENGLISH)
        val id = AppStrings.forLanguage(AppLanguage.INDONESIAN)

        assertEquals("Metric", en.unitDescription(SpeedUnit.KMH))
        assertEquals("Metrik", id.unitDescription(SpeedUnit.KMH))

        assertEquals("Imperial", en.unitDescription(SpeedUnit.MPH))
        assertEquals("Imperial", id.unitDescription(SpeedUnit.MPH))

        assertEquals("Nautical", en.unitDescription(SpeedUnit.KNOTS))
        assertEquals("Nautika", id.unitDescription(SpeedUnit.KNOTS))
    }

    @Test
    fun actionAndStatusButtons_localizedAccurately() {
        val en = AppStrings.forLanguage(AppLanguage.ENGLISH)
        val id = AppStrings.forLanguage(AppLanguage.INDONESIAN)

        assertEquals("START DASH", en.startTelemetry)
        assertEquals("MULAI PELACAKAN", id.startTelemetry)

        assertEquals("STOP DASH", en.stopTelemetry)
        assertEquals("HENTIKAN PELACAKAN", id.stopTelemetry)

        assertEquals("RESET", en.reset)
        assertEquals("RESET", id.reset)

        assertEquals("MAX SPEED", en.maxSpeed)
        assertEquals("KECEPATAN MAKS", id.maxSpeed)

        assertEquals("AVG SPEED", en.avgSpeed)
        assertEquals("KECEPATAN RATA2", id.avgSpeed)

        assertEquals("TRIP DIST", en.tripDistance)
        assertEquals("JARAK TEMPUH", id.tripDistance)
    }

    @Test
    fun accuracyFormatting_matchesExpectedPattern() {
        val en = AppStrings.forLanguage(AppLanguage.ENGLISH)
        val id = AppStrings.forLanguage(AppLanguage.INDONESIAN)

        assertEquals("±5m ACCURACY", en.accuracyFix(5))
        assertEquals("±5m AKURASI", id.accuracyFix(5))
    }
}
