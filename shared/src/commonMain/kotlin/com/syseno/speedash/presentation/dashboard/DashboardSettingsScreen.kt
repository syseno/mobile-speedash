package com.syseno.speedash.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.syseno.speedash.domain.model.AppLanguage
import com.syseno.speedash.domain.model.TripRecord
import com.syseno.speedash.presentation.localization.AppStrings
import com.syseno.speedash.presentation.util.BackHandler
import kotlin.math.roundToInt

/**
 * Full-screen Settings & Options View.
 *
 * Provides cockpit preferences and hardware telemetry diagnostics:
 * 1. Trip History & Map Inspections.
 * 2. Application Language Selection (English and Bahasa Indonesia).
 * 3. Vehicle Mode Selection (Car, Motorcycle, Train, Ship, Plane).
 * 4. Speed Measurement Unit Selection (KM/H, MPH, Knots).
 * 5. Screen Wake Lock (Stay Awake) toggle.
 * 6. Heading Hardware Sensor Source status.
 * 7. System Architecture and About information.
 */
@Composable
fun DashboardSettingsScreen(
    state: DashboardUiState,
    onIntent: (DashboardUiIntent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept hardware/system back button on Android
    BackHandler(enabled = true, onBack = onDismiss)

    val strings = AppStrings.forLanguage(state.language)
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DashboardTheme.BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Screen Top App Bar / Header
            SettingsTopBar(
                title = strings.settingsTitle,
                subtitle = strings.settingsSubtitle,
                backLabel = strings.backButton,
                onBack = onDismiss
            )

            // Scrollable Settings Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Section 0: Trip History & Map Routing
                TripHistorySection(
                    trips = state.trips,
                    strings = strings,
                    onOpenMap = { lat, lon, label -> onIntent(DashboardUiIntent.OpenMap(lat, lon, label)) },
                    onDeleteTrip = { id -> onIntent(DashboardUiIntent.DeleteTrip(id)) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 1: Multilingual Language Selection (Indonesia & English)
                SectionHeader(
                    title = strings.languageSectionTitle,
                    subtitle = strings.languageSectionSubtitle
                )
                Spacer(modifier = Modifier.height(10.dp))
                LanguageSelectorGrid(
                    currentLanguage = state.language,
                    strings = strings,
                    onSelectLanguage = { onIntent(DashboardUiIntent.ChangeLanguage(it)) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Vehicle Mode Selection
                SectionHeader(
                    title = strings.vehicleSectionTitle,
                    subtitle = strings.vehicleSectionSubtitle
                )
                Spacer(modifier = Modifier.height(10.dp))
                VehicleSelectorRow(
                    selectedVehicle = state.vehicleType,
                    strings = strings,
                    onSelectVehicle = { onIntent(DashboardUiIntent.SelectVehicle(it)) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: Speed Measurement Unit Selection
                SectionHeader(
                    title = strings.unitSectionTitle,
                    subtitle = strings.unitSectionSubtitle
                )
                Spacer(modifier = Modifier.height(10.dp))
                SpeedUnitSelectorRow(
                    selectedUnit = state.speedUnit,
                    strings = strings,
                    onSelectUnit = { onIntent(DashboardUiIntent.ChangeSpeedUnit(it)) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 4: Theme Mode Selection (System Default, Dark, Light)
                SectionHeader(
                    title = strings.themeSectionTitle,
                    subtitle = strings.themeSectionSubtitle
                )
                Spacer(modifier = Modifier.height(10.dp))
                ThemeSelectorRow(
                    selectedTheme = state.themeMode,
                    strings = strings,
                    onSelectTheme = { onIntent(DashboardUiIntent.ChangeThemeMode(it)) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 5: Cockpit Skin & Styling Theme Selection
                SectionHeader(
                    title = strings.skinSectionTitle,
                    subtitle = strings.skinSectionSubtitle
                )
                Spacer(modifier = Modifier.height(10.dp))
                CockpitSkinSelectorGrid(
                    selectedSkin = state.cockpitSkin,
                    strings = strings,
                    onSelectSkin = { onIntent(DashboardUiIntent.SelectSkin(it)) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 6: Dashboard Cluster Layout Style Selection
                SectionHeader(
                    title = strings.layoutSectionTitle,
                    subtitle = strings.layoutSectionSubtitle
                )
                Spacer(modifier = Modifier.height(10.dp))
                GaugeLayoutSelectorRow(
                    selectedLayout = state.gaugeLayout,
                    strings = strings,
                    onSelectLayout = { onIntent(DashboardUiIntent.SelectLayout(it)) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 5: Screen Wake Lock
                SectionHeader(
                    title = strings.wakeLockSectionTitle,
                    subtitle = strings.wakeLockSectionSubtitle
                )
                Spacer(modifier = Modifier.height(10.dp))
                WakeLockCard(
                    keepScreenOn = state.keepScreenOn,
                    strings = strings,
                    onToggleKeepScreenOn = { onIntent(DashboardUiIntent.ToggleKeepScreenOn) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 6: Heading Hardware Sensor Diagnostics
                SectionHeader(
                    title = strings.sensorSectionTitle,
                    subtitle = strings.sensorSectionSubtitle
                )
                Spacer(modifier = Modifier.height(10.dp))
                SensorSourceCard(
                    hasCompassSensor = state.hasCompassSensor,
                    strings = strings
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 7: About Speedash
                SectionHeader(
                    title = strings.aboutSectionTitle,
                    subtitle = strings.aboutSectionSubtitle
                )
                Spacer(modifier = Modifier.height(10.dp))
                AboutAppCard(strings = strings)

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action: Apply & Return Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DashboardTheme.CyanNeon,
                        contentColor = if (DashboardTheme.colors.isDark) DashboardTheme.BackgroundDark else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = strings.applyAndClose,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// =================================================================================================
// TRIP HISTORY SECTION & COMPONENTS
// =================================================================================================

@Composable
private fun TripHistorySection(
    trips: List<TripRecord>,
    strings: AppStrings,
    onOpenMap: (Double, Double, String) -> Unit,
    onDeleteTrip: (String) -> Unit
) {
    SectionHeader(
        title = strings.tripHistoryTitle,
        subtitle = strings.tripHistorySubtitle
    )
    Spacer(modifier = Modifier.height(10.dp))
    if (trips.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DashboardTheme.SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DashboardTheme.SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.noTripsRecorded,
                    style = MaterialTheme.typography.bodyMedium.copy(color = DashboardTheme.TextMuted)
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            trips.forEach { trip ->
                TripRecordCard(
                    trip = trip,
                    strings = strings,
                    onOpenMap = onOpenMap,
                    onDeleteTrip = onDeleteTrip
                )
            }
        }
    }
}

@Composable
private fun TripRecordCard(
    trip: TripRecord,
    strings: AppStrings,
    onOpenMap: (Double, Double, String) -> Unit,
    onDeleteTrip: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DashboardTheme.SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DashboardTheme.SurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trip.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DashboardTheme.CyanNeon
                    )
                )
                Text(
                    text = "🗑",
                    modifier = Modifier
                        .clickable { onDeleteTrip(trip.id) }
                        .padding(4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TripMetricView(label = strings.durationLabel, value = formatDuration(trip.durationMillis))
                TripMetricView(label = strings.distanceLabel, value = "${formatOneDecimal(trip.distanceKm)} km")
                TripMetricView(label = strings.avgSpeedLabel, value = "${trip.avgSpeedKmh.roundToInt()} km/h")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onOpenMap(trip.startLat, trip.startLon, "${trip.name} - Start") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DashboardTheme.BackgroundDark,
                        contentColor = DashboardTheme.TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Text(text = "📍 ${strings.startLocation}", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = { onOpenMap(trip.endLat, trip.endLon, "${trip.name} - End") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DashboardTheme.BackgroundDark,
                        contentColor = DashboardTheme.TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Text(text = "🏁 ${strings.endLocation}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TripMetricView(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = DashboardTheme.TextMuted,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = DashboardTheme.TextPrimary
            )
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val minStr = if (minutes < 10) "0$minutes" else "$minutes"
    val secStr = if (seconds < 10) "0$seconds" else "$seconds"
    return if (hours > 0) {
        "${hours}h ${minStr}m"
    } else {
        "$minStr:$secStr"
    }
}

private fun formatOneDecimal(value: Double): String {
    val rounded = (value * 10).roundToInt()
    val whole = rounded / 10
    val fraction = kotlin.math.abs(rounded % 10)
    return "$whole.$fraction"
}

// =================================================================================================
// SUB-COMPONENTS & SECTIONS
// =================================================================================================

@Composable
private fun SettingsTopBar(
    title: String,
    subtitle: String,
    backLabel: String,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardTheme.SurfaceDark),
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = DashboardTheme.CyanNeon
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DashboardTheme.TextSecondary
                        )
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DashboardTheme.BackgroundDark,
                        contentColor = DashboardTheme.CyanNeon
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DashboardTheme.SurfaceBorder),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = backLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = DashboardTheme.CyanNeon
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                color = DashboardTheme.TextSecondary,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun LanguageSelectorGrid(
    currentLanguage: AppLanguage,
    strings: AppStrings,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LanguageOptionCard(
            title = strings.englishOption,
            subtitle = strings.englishOptionSubtitle,
            isSelected = currentLanguage == AppLanguage.ENGLISH,
            onClick = { onSelectLanguage(AppLanguage.ENGLISH) },
            modifier = Modifier.weight(1f)
        )
        LanguageOptionCard(
            title = strings.indonesianOption,
            subtitle = strings.indonesianOptionSubtitle,
            isSelected = currentLanguage == AppLanguage.INDONESIAN,
            onClick = { onSelectLanguage(AppLanguage.INDONESIAN) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LanguageOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.SurfaceBorder
    val containerColor = if (isSelected) DashboardTheme.BackgroundDark else DashboardTheme.SurfaceDark

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = DashboardTheme.TextSecondary,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun VehicleSelectorRow(
    selectedVehicle: VehicleType,
    strings: AppStrings,
    onSelectVehicle: (VehicleType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VehicleType.entries.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { vehicleType ->
                    val isSelected = selectedVehicle == vehicleType
                    val borderColor = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.SurfaceBorder
                    val containerColor = if (isSelected) DashboardTheme.BackgroundDark else DashboardTheme.SurfaceDark

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectVehicle(vehicleType) }
                            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.vehicleName(vehicleType),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.TextPrimary
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SpeedUnitSelectorRow(
    selectedUnit: SpeedUnit,
    strings: AppStrings,
    onSelectUnit: (SpeedUnit) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SpeedUnit.entries.forEach { unit ->
            val isSelected = selectedUnit == unit
            val borderColor = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.SurfaceBorder
            val containerColor = if (isSelected) DashboardTheme.BackgroundDark else DashboardTheme.SurfaceDark

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectUnit(unit) }
                    .border(1.5.dp, borderColor, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = unit.speedLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.unitDescription(unit),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DashboardTheme.TextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectorRow(
    selectedTheme: ThemeMode,
    strings: AppStrings,
    onSelectTheme: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(ThemeMode.SYSTEM, ThemeMode.DARK, ThemeMode.LIGHT).forEach { theme ->
            val isSelected = selectedTheme == theme
            val borderColor = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.SurfaceBorder
            val containerColor = if (isSelected) DashboardTheme.BackgroundDark else DashboardTheme.SurfaceDark

            val title = when (theme) {
                ThemeMode.SYSTEM -> strings.themeSystemOption
                ThemeMode.DARK -> strings.themeDarkOption
                ThemeMode.LIGHT -> strings.themeLightOption
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectTheme(theme) }
                    .border(1.5.dp, borderColor, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.TextPrimary
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CockpitSkinSelectorGrid(
    selectedSkin: CockpitSkin,
    strings: AppStrings,
    onSelectSkin: (CockpitSkin) -> Unit,
    modifier: Modifier = Modifier
) {
    val skins = CockpitSkin.entries
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (i in skins.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val skin1 = skins[i]
                SkinPreviewCard(
                    skin = skin1,
                    isSelected = skin1 == selectedSkin,
                    strings = strings,
                    onSelect = { onSelectSkin(skin1) },
                    modifier = Modifier.weight(1f)
                )

                if (i + 1 < skins.size) {
                    val skin2 = skins[i + 1]
                    SkinPreviewCard(
                        skin = skin2,
                        isSelected = skin2 == selectedSkin,
                        strings = strings,
                        onSelect = { onSelectSkin(skin2) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SkinPreviewCard(
    skin: CockpitSkin,
    isSelected: Boolean,
    strings: AppStrings,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bg, surface, accent, textCol) = when (skin) {
        CockpitSkin.CYBERPUNK -> Quadruple(Color(0xFF07090E), Color(0xFF10141E), Color(0xFF00F5D4), Color(0xFFF0F4FC))
        CockpitSkin.ELECTRIC_RACING -> Quadruple(Color(0xFF0A0E1A), Color(0xFF121829), Color(0xFFCCFF00), Color(0xFFF8FAFC))
        CockpitSkin.SPORT_GT -> Quadruple(Color(0xFF0A0505), Color(0xFF160B0B), Color(0xFFE60000), Color(0xFFFFFFFF))
        CockpitSkin.STEALTH_GREEN -> Quadruple(Color(0xFF06140E), Color(0xFF0B2017), Color(0xFF00E676), Color(0xFFE8F5E9))
        CockpitSkin.DAYLIGHT_LITE -> Quadruple(Color(0xFFF1F5F9), Color(0xFFFFFFFF), Color(0xFF0284C7), Color(0xFF0F172A))
        CockpitSkin.DEEP_SPACE -> Quadruple(Color(0xFF050B14), Color(0xFF0E1526), Color(0xFF9D4EDD), Color(0xFFF3E8FF))
    }

    val borderColor = if (isSelected) accent else DashboardTheme.SurfaceBorder

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DashboardTheme.SurfaceDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Miniature Dashboard Canvas Preview (matching theme gallery!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height * 0.70f)
                    val radius = size.height * 0.52f

                    // Draw mini gauge arc track
                    drawArc(
                        color = surface,
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw mini active speed arc
                    drawArc(
                        color = accent,
                        startAngle = 140f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw mini needle
                    val angleRad = (140f + 160f) * (PI.toFloat() / 180f)
                    val tipX = center.x + (radius - 3.dp.toPx()) * cos(angleRad)
                    val tipY = center.y + (radius - 3.dp.toPx()) * sin(angleRad)
                    drawLine(
                        color = accent,
                        start = center,
                        end = Offset(tipX, tipY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Mini Digital Speed Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "120",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = textCol
                        )
                    )
                    Text(
                        text = "KM/H",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            color = accent
                        )
                    )
                }

                // Status Badge in top corner
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.2f))
                        .border(1.dp, accent, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSelected) "ACTIVE" else "SELECT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color = accent
                        )
                    )
                }
            }

            // Bottom Label & Description Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = strings.skinName(skin),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) accent else DashboardTheme.TextPrimary,
                        fontSize = 12.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = strings.skinDescription(skin),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DashboardTheme.TextSecondary,
                        fontSize = 10.sp
                    ),
                    maxLines = 2
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun GaugeLayoutSelectorRow(
    selectedLayout: GaugeLayout,
    strings: AppStrings,
    onSelectLayout: (GaugeLayout) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GaugeLayout.entries.forEach { layout ->
            val isSelected = selectedLayout == layout
            val borderColor = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.SurfaceBorder
            val containerColor = if (isSelected) DashboardTheme.BackgroundDark else DashboardTheme.SurfaceDark

            val iconSymbol = when (layout) {
                GaugeLayout.SPORT_RACING -> "🏎️"
                GaugeLayout.CIRCULAR_STANDARD -> "🎯"
                GaugeLayout.MINIMAL_HUD -> "⚡"
                GaugeLayout.DUAL_CLUSTER -> "🧭"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectLayout(layout) }
                    .border(1.5.dp, borderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = iconSymbol,
                        fontSize = 22.sp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.layoutName(layout),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.layoutDescription(layout),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DashboardTheme.TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DashboardTheme.CyanNeon.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = DashboardTheme.CyanNeon,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WakeLockCard(
    keepScreenOn: Boolean,
    strings: AppStrings,
    onToggleKeepScreenOn: () -> Unit
) {
    val borderColor = if (keepScreenOn) DashboardTheme.CyanNeon else DashboardTheme.SurfaceBorder
    val containerColor = if (keepScreenOn) DashboardTheme.BackgroundDark else DashboardTheme.SurfaceDark

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleKeepScreenOn)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (keepScreenOn) strings.wakeLockActive else strings.wakeLockOff,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (keepScreenOn) DashboardTheme.CyanNeon else DashboardTheme.TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (keepScreenOn) strings.wakeLockActiveDescription else strings.wakeLockOffDescription,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DashboardTheme.TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (keepScreenOn) "ON" else "OFF",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = if (keepScreenOn) DashboardTheme.CyanNeon else DashboardTheme.TextMuted
                )
            )
        }
    }
}

@Composable
private fun SensorSourceCard(
    hasCompassSensor: Boolean,
    strings: AppStrings
) {
    val borderColor = if (hasCompassSensor) DashboardTheme.CyanNeon else DashboardTheme.SurfaceBorder
    val containerColor = DashboardTheme.SurfaceDark

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasCompassSensor) strings.compassHardwareTitle else strings.gpsHeadingTitle,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = DashboardTheme.CyanNeon
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (hasCompassSensor) strings.compassHardwareDescription else strings.gpsHeadingDescription,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DashboardTheme.TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun AboutAppCard(strings: AppStrings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardTheme.SurfaceDark),
        border = BorderStroke(1.dp, DashboardTheme.SurfaceBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = strings.aboutAppDescription,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DashboardTheme.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            )
        }
    }
}
