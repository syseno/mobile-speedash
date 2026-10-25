package com.syseno.speedash.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syseno.speedash.domain.model.AppLanguage
import com.syseno.speedash.presentation.localization.AppStrings
import com.syseno.speedash.presentation.util.BackHandler

/**
 * Full-screen Settings & Options View.
 *
 * Provides cockpit preferences and hardware telemetry diagnostics:
 * 1. Application Language Selection (English and Bahasa Indonesia).
 * 2. Vehicle Mode Selection (Car, Motorcycle, Train, Ship, Plane).
 * 3. Speed Measurement Unit Selection (KM/H, MPH, Knots).
 * 4. Screen Wake Lock (Stay Awake) toggle.
 * 5. Heading Hardware Sensor Source status.
 * 6. System Architecture and About information.
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
// SUB-COMPONENTS & SECTIONS
// =================================================================================================

@Composable
private fun SettingsTopBar(
    title: String,
    subtitle: String,
    backLabel: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DashboardTheme.SurfaceDark)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(DashboardTheme.SurfaceBorder, Color.Transparent)
                ),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(DashboardTheme.BackgroundDark)
                .border(1.dp, DashboardTheme.SurfaceBorder, RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "←",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = DashboardTheme.CyanNeon,
                    fontWeight = FontWeight.Black
                )
            )
            Text(
                text = backLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = DashboardTheme.TextPrimary,
                    letterSpacing = 0.8.sp
                )
            )
        }

        // Center Title & Icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚙",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = DashboardTheme.CyanNeon
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = DashboardTheme.TextPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    color = DashboardTheme.TextMuted
                )
            )
        }

        // Close / Check Icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DashboardTheme.BackgroundDark)
                .border(1.dp, DashboardTheme.CyanNeon.copy(alpha = 0.35f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DashboardTheme.CyanNeon
                )
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = DashboardTheme.CyanNeon
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = DashboardTheme.TextMuted
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
        AppLanguage.entries.forEach { lang ->
            val isSelected = lang == currentLanguage
            val flagEmoji = lang.flagEmoji
            val optionTitle = when (lang) {
                AppLanguage.ENGLISH -> strings.englishOption
                AppLanguage.INDONESIAN -> strings.indonesianOption
            }
            val optionSubtitle = when (lang) {
                AppLanguage.ENGLISH -> strings.englishOptionSubtitle
                AppLanguage.INDONESIAN -> strings.indonesianOptionSubtitle
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelectLanguage(lang) }
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        DashboardTheme.CyanNeon.copy(alpha = 0.12f)
                    } else {
                        DashboardTheme.SurfaceDark
                    }
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        if (isSelected) {
                            listOf(DashboardTheme.CyanNeon, DashboardTheme.ElectricBlue)
                        } else {
                            listOf(DashboardTheme.SurfaceBorder, DashboardTheme.SurfaceBorder)
                        }
                    )
                ),
                shape = RoundedCornerShape(14.dp)
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
                            text = flagEmoji,
                            fontSize = 24.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) DashboardTheme.CyanNeon.copy(alpha = 0.25f) else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.SurfaceBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isSelected) "ACTIVE" else "SELECT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.TextMuted
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = optionTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = optionSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = DashboardTheme.TextMuted
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun VehicleSelectorRow(
    selectedVehicle: VehicleType,
    strings: AppStrings,
    onSelectVehicle: (VehicleType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DashboardTheme.SurfaceDark)
            .border(1.dp, DashboardTheme.SurfaceBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VehicleType.entries.forEach { vehicle ->
            val isSelected = vehicle == selectedVehicle
            val shortLabel = strings.vehicleShortName(vehicle)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) DashboardTheme.CyanNeon.copy(alpha = 0.22f) else Color.Transparent
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) DashboardTheme.CyanNeon else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelectVehicle(vehicle) }
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    VehicleIconDisplay(
                        vehicleType = vehicle,
                        modifier = Modifier.size(width = 28.dp, height = 18.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = shortLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.TextMuted,
                            letterSpacing = 0.4.sp
                        )
                    )
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DashboardTheme.SurfaceDark)
            .border(1.dp, DashboardTheme.SurfaceBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpeedUnit.entries.forEach { unit ->
            val isSelected = unit == selectedUnit
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) DashboardTheme.CyanNeon.copy(alpha = 0.22f) else Color.Transparent
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) DashboardTheme.CyanNeon else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelectUnit(unit) }
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = unit.speedLabel.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.TextSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.unitDescription(unit),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            color = if (isSelected) DashboardTheme.CyanNeon.copy(alpha = 0.85f) else DashboardTheme.TextMuted
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
    onSelectTheme: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selectedTheme
            val (title, subtitle) = when (mode) {
                ThemeMode.SYSTEM -> strings.themeSystemOption to strings.themeSystemOptionSubtitle
                ThemeMode.DARK -> strings.themeDarkOption to strings.themeDarkOptionSubtitle
                ThemeMode.LIGHT -> strings.themeLightOption to strings.themeLightOptionSubtitle
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelectTheme(mode) }
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DashboardTheme.CyanNeon.copy(alpha = 0.18f) else DashboardTheme.SurfaceDark
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        listOf(
                            if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.SurfaceBorder,
                            DashboardTheme.SurfaceBorder
                        )
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) DashboardTheme.CyanNeon.copy(alpha = 0.22f) else Color.Transparent
                            )
                            .border(
                                width = 2.dp,
                                color = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.TextMuted,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(DashboardTheme.CyanNeon)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DashboardTheme.CyanNeon else DashboardTheme.TextPrimary
                            )
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DashboardTheme.TextMuted
                            )
                        )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DashboardTheme.SurfaceDark)
            .border(1.dp, DashboardTheme.SurfaceBorder, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleKeepScreenOn
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.wakeLockSectionTitle,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DashboardTheme.TextPrimary,
                    letterSpacing = 0.8.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (keepScreenOn) strings.wakeLockActiveDescription else strings.wakeLockOffDescription,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = DashboardTheme.TextMuted
                )
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (keepScreenOn) DashboardTheme.CyanNeon.copy(alpha = 0.22f) else DashboardTheme.BackgroundDark
                )
                .border(
                    1.dp,
                    if (keepScreenOn) DashboardTheme.CyanNeon else DashboardTheme.SurfaceBorder,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (keepScreenOn) strings.wakeLockActive else strings.wakeLockOff,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (keepScreenOn) DashboardTheme.CyanNeon else DashboardTheme.TextMuted,
                    letterSpacing = 1.sp
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DashboardTheme.SurfaceDark)
            .border(1.dp, DashboardTheme.SurfaceBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (hasCompassSensor) strings.compassHardwareTitle else strings.gpsHeadingTitle,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DashboardTheme.TextPrimary,
                    letterSpacing = 0.8.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (hasCompassSensor) strings.compassHardwareDescription else strings.gpsHeadingDescription,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = DashboardTheme.TextMuted
                )
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (hasCompassSensor) DashboardTheme.MintActive.copy(alpha = 0.22f) else DashboardTheme.ElectricBlue.copy(alpha = 0.22f)
                )
                .border(
                    1.dp,
                    if (hasCompassSensor) DashboardTheme.MintActive else DashboardTheme.ElectricBlue,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (hasCompassSensor) strings.sensorCompass else strings.sensorGps,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (hasCompassSensor) DashboardTheme.MintActive else DashboardTheme.ElectricBlue,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
private fun AboutAppCard(strings: AppStrings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardTheme.SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(DashboardTheme.SurfaceBorder, DashboardTheme.BackgroundDark)
            )
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = strings.aboutSectionSubtitle,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DashboardTheme.CyanNeon
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.aboutAppDescription,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = DashboardTheme.TextSecondary,
                    lineHeight = 16.sp
                )
            )
        }
    }
}
