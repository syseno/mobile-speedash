package com.syseno.speedash.presentation.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syseno.speedash.presentation.localization.AppStrings
import com.syseno.speedash.presentation.util.KeepScreenOn
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// =================================================================================================
// PALETTE & STYLING CONSTANTS (Dynamic Cockpit Theme: Dark & Light)
// =================================================================================================

@Immutable
data class DashboardColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceBorder: Color,
    val gaugeTrack: Color,
    val cyanNeon: Color,
    val electricBlue: Color,
    val coralWarning: Color,
    val amberWarning: Color,
    val mintActive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val needleBase: Color
)

private val DarkDashboardColors = DashboardColors(
    isDark = true,
    background = Color(0xFF07090E),
    surface = Color(0xFF10141E),
    surfaceBorder = Color(0xFF1F293D),
    gaugeTrack = Color(0xFF161C28),
    cyanNeon = Color(0xFF00F5D4),
    electricBlue = Color(0xFF00BBF9),
    coralWarning = Color(0xFFFF5964),
    amberWarning = Color(0xFFFFB703),
    mintActive = Color(0xFF38B000),
    textPrimary = Color(0xFFF0F4FC),
    textSecondary = Color(0xFF8E9BB0),
    textMuted = Color(0xFF4C566A),
    needleBase = Color(0xFF00F5D4)
)

private val LightDashboardColors = DashboardColors(
    isDark = false,
    background = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    surfaceBorder = Color(0xFFCBD5E1),
    gaugeTrack = Color(0xFFE2E8F0),
    cyanNeon = Color(0xFF0284C7),
    electricBlue = Color(0xFF0284C7),
    coralWarning = Color(0xFFDC2626),
    amberWarning = Color(0xFFD97706),
    mintActive = Color(0xFF16A34A),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    needleBase = Color(0xFF0284C7)
)

private val LocalDashboardColors = staticCompositionLocalOf { DarkDashboardColors }

@Composable
fun DashboardThemeWrapper(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colors = if (isDark) DarkDashboardColors else LightDashboardColors

    CompositionLocalProvider(LocalDashboardColors provides colors) {
        MaterialTheme(
            colorScheme = if (isDark) darkColorScheme() else lightColorScheme(),
            content = content
        )
    }
}

internal object DashboardTheme {
    val colors: DashboardColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDashboardColors.current

    val BackgroundDark: Color @Composable @ReadOnlyComposable get() = colors.background
    val SurfaceDark: Color @Composable @ReadOnlyComposable get() = colors.surface
    val SurfaceBorder: Color @Composable @ReadOnlyComposable get() = colors.surfaceBorder

    val GaugeTrack: Color @Composable @ReadOnlyComposable get() = colors.gaugeTrack
    val CyanNeon: Color @Composable @ReadOnlyComposable get() = colors.cyanNeon
    val ElectricBlue: Color @Composable @ReadOnlyComposable get() = colors.electricBlue
    val CoralWarning: Color @Composable @ReadOnlyComposable get() = colors.coralWarning
    val AmberWarning: Color @Composable @ReadOnlyComposable get() = colors.amberWarning
    val MintActive: Color @Composable @ReadOnlyComposable get() = colors.mintActive

    val TextPrimary: Color @Composable @ReadOnlyComposable get() = colors.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = colors.textSecondary
    val TextMuted: Color @Composable @ReadOnlyComposable get() = colors.textMuted

    val NeedleBase: Color @Composable @ReadOnlyComposable get() = colors.needleBase
}

// =================================================================================================
// SPLASH SCREEN
// =================================================================================================

/**
 * Branded Splash Screen shown upon entering the app on both iOS and Android.
 */
@Composable
fun SpeedashSplashScreen(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
        kotlinx.coroutines.delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardTheme.BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(DashboardTheme.CyanNeon.copy(alpha = 0.15f))
                    .border(2.dp, DashboardTheme.CyanNeon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    color = DashboardTheme.CyanNeon,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SPEEDASH",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PRECISION GPS TELEMETRY",
                color = DashboardTheme.CyanNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
        }
    }
}

// =================================================================================================
// ROOT COMPOSABLE SCREEN (Koin ViewModel Integration + Side Effect Handling)
// =================================================================================================

/**
 * Top-level Stateful Dashboard Screen connected to [DashboardViewModel].
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
    onRequestPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        DashboardThemeWrapper(themeMode = state.themeMode) {
            SpeedashSplashScreen(onTimeout = { showSplash = false })
        }
        return
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardUiEffect.RequestLocationPermission -> onRequestPermission()
                is DashboardUiEffect.OpenAppSettings -> onOpenSettings()
                is DashboardUiEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    DashboardThemeWrapper(themeMode = state.themeMode) {
        DashboardContent(
            state = state,
            onIntent = viewModel::onIntent,
            snackbarHostState = snackbarHostState
        )
    }
}

/**
 * Pure Stateless Dashboard Content for rendering and previews.
 */
@Composable
fun DashboardContent(
    state: DashboardUiState,
    onIntent: (DashboardUiIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    DashboardThemeWrapper(themeMode = state.themeMode) {
        var isSettingsOpen by remember { mutableStateOf(false) }

        if (state.keepScreenOn) {
            KeepScreenOn()
        }

        val strings = AppStrings.forLanguage(state.language)

        // Trip Name Input Dialog
        TripNameDialog(
            isOpen = state.showTripNameDialog,
            strings = strings,
            onDismiss = { onIntent(DashboardUiIntent.DismissTripNameDialog) },
            onConfirm = { name -> onIntent(DashboardUiIntent.StartTripRecording(name)) }
        )

        AnimatedContent(
            targetState = isSettingsOpen,
            label = "DashboardScreenTransition"
        ) { showSettings ->
            if (showSettings) {
                DashboardSettingsScreen(
                    state = state,
                    onIntent = onIntent,
                    onDismiss = { isSettingsOpen = false },
                    modifier = modifier
                )
            } else {
                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    containerColor = DashboardTheme.BackgroundDark,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Top Status Header & GPS Accuracy / Vehicle HUD
                        DashboardHeader(state = state, strings = strings)

                        // 1.5. Big Compass HUD below header (centered horizontally)
                        CompassHudHeader(state = state)

                        Spacer(modifier = Modifier.height(4.dp))

                        // 2. Center Analog Gauge with Digital Speedometer
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            AnalogSpeedometerGauge(state = state, strings = strings, onIntent = onIntent)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Bottom Trip Telemetry Cards & Action Controls (Start/Stop, Reset, Settings)
                        BottomTelemetryControls(
                            state = state,
                            strings = strings,
                            onIntent = onIntent,
                            onSettingsClick = { isSettingsOpen = true }
                        )
                    }
                }
            }
        }
    }
}

// =================================================================================================
// TRIP NAME DIALOG
// =================================================================================================

@Composable
private fun TripNameDialog(
    isOpen: Boolean,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (!isOpen) return
    var nameInput by remember { mutableStateOf("Trip - ${kotlin.random.Random.nextInt(100, 999)}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = strings.tripNameDialogTitle, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Text(text = strings.tripNameDialogMessage, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nameInput.ifBlank { "Trip" }) },
                colors = ButtonDefaults.buttonColors(containerColor = DashboardTheme.CyanNeon, contentColor = DashboardTheme.BackgroundDark)
            ) {
                Text(text = strings.saveTrip, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DashboardTheme.SurfaceDark, contentColor = DashboardTheme.TextSecondary)
            ) {
                Text(text = strings.backButton, style = MaterialTheme.typography.labelMedium)
            }
        },
        containerColor = DashboardTheme.SurfaceDark
    )
}

// =================================================================================================
// HEADER BAR
// =================================================================================================

@Composable
private fun DashboardHeader(state: DashboardUiState, strings: AppStrings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Badge
        StatusBadge(status = state.trackingStatus, strings = strings)

        // Accuracy & Vehicle Type HUD
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            state.gpsAccuracyMeters?.let { accuracy ->
                BadgeChip(text = strings.accuracyFix(accuracy.roundToInt()), color = DashboardTheme.CyanNeon)
            }
            BadgeChip(text = "${state.vehicleType.iconSymbol} ${state.vehicleType.displayName.uppercase()}", color = DashboardTheme.TextSecondary)
        }
    }
}

@Composable
private fun StatusBadge(status: TrackingStatus, strings: AppStrings) {
    val (text, color) = when (status) {
        TrackingStatus.Idle -> strings.statusStandby to DashboardTheme.TextMuted
        TrackingStatus.Acquiring -> strings.statusAcquiring to DashboardTheme.AmberWarning
        TrackingStatus.Active -> strings.statusActive to DashboardTheme.MintActive
        TrackingStatus.PermissionDenied -> strings.statusPermissionDenied to DashboardTheme.CoralWarning
        is TrackingStatus.Error -> strings.statusGpsError to DashboardTheme.CoralWarning
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(DashboardTheme.SurfaceDark, RoundedCornerShape(20.dp))
            .border(1.dp, DashboardTheme.SurfaceBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = DashboardTheme.TextPrimary
            )
        )
    }
}

@Composable
private fun BadgeChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(DashboardTheme.SurfaceDark, RoundedCornerShape(8.dp))
            .border(1.dp, DashboardTheme.SurfaceBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}

// =================================================================================================
// ANALOG GAUGE & SPEEDOMETER
// =================================================================================================

// =================================================================================================
// COMPASS HUD HEADER
// =================================================================================================

@Composable
private fun CompassHudHeader(state: DashboardUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "▲",
            color = DashboardTheme.CyanNeon,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${state.headingDegrees}° ${state.cardinalDirection}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DashboardTheme.TextPrimary,
                letterSpacing = 1.5.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = state.vehicleType.iconSymbol,
            fontSize = 24.sp
        )
    }
}

// =================================================================================================
// ANALOG GAUGE & SPEEDOMETER
// =================================================================================================

@Composable
private fun AnalogSpeedometerGauge(state: DashboardUiState, strings: AppStrings, onIntent: (DashboardUiIntent) -> Unit) {
    val gaugeTrackColor = DashboardTheme.GaugeTrack
    val textSecondaryColor = DashboardTheme.TextSecondary
    val textMutedColor = DashboardTheme.TextMuted
    val cyanNeonColor = DashboardTheme.CyanNeon
    val electricBlueColor = DashboardTheme.ElectricBlue
    val coralWarningColor = DashboardTheme.CoralWarning

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.0f),
        contentAlignment = Alignment.Center
    ) {
        val size = minOf(maxWidth, maxHeight)
        val textMeasurer = rememberTextMeasurer()

        val animatedSpeed by animateFloatAsState(
            targetValue = state.smoothedSpeedKmh.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "SpeedNeedleAnimation"
        )

        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = minOf(center.x, center.y) * 0.95f

            drawGaugeTicksAndLabels(center, radius * 0.88f, textMeasurer, gaugeTrackColor, textSecondaryColor, textMutedColor)
            drawSpeedArc(center, radius * 0.88f, animatedSpeed, cyanNeonColor, electricBlueColor, coralWarningColor)
            drawSpeedNeedle(center, radius * 0.78f, animatedSpeed, cyanNeonColor)
        }

        // Digital Speedometer readout in center with ASL altitude below
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${state.displaySpeed}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = DashboardTheme.TextPrimary
                    )
                )
            }
            Text(
                text = state.speedUnit.speedLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardTheme.CyanNeon,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onIntent(DashboardUiIntent.CycleSpeedUnit)
                }
            )
            state.displayAltitudeMeters?.let { altitude ->
                Spacer(modifier = Modifier.height(6.dp))
                BadgeChip(text = strings.altitudeFormat(altitude), color = DashboardTheme.ElectricBlue)
            }
        }
    }
}

private fun DrawScope.drawGaugeTicksAndLabels(
    center: Offset,
    radius: Float,
    textMeasurer: TextMeasurer,
    gaugeTrackColor: Color,
    textSecondaryColor: Color,
    textMutedColor: Color
) {
    val startAngle = 135.0
    val totalAngle = 270.0
    val maxSpeed = 240.0
    val step = 20.0

    drawCircle(
        color = gaugeTrackColor,
        radius = radius,
        center = center,
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
    )

    var speed = 0.0
    while (speed <= maxSpeed) {
        val fraction = speed / maxSpeed
        val angleDeg = startAngle + (fraction * totalAngle)
        val angleRad = angleDeg * (PI / 180.0)

        val isMajor = true // Every 20 interval is major with label
        val tickLength = 10.dp.toPx()
        val strokeWidth = 2.dp.toPx()

        val outerX = center.x + radius * cos(angleRad).toFloat()
        val outerY = center.y + radius * sin(angleRad).toFloat()
        val innerX = center.x + (radius - tickLength) * cos(angleRad).toFloat()
        val innerY = center.y + (radius - tickLength) * sin(angleRad).toFloat()

        drawLine(
            color = if (isMajor) textSecondaryColor else textMutedColor,
            start = Offset(outerX, outerY),
            end = Offset(innerX, innerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        if (isMajor) {
            val labelRadius = radius - 26.dp.toPx()
            val labelX = center.x + labelRadius * cos(angleRad).toFloat()
            val labelY = center.y + labelRadius * sin(angleRad).toFloat()

            val textLayoutResult = textMeasurer.measure(
                text = "${speed.toInt()}",
                style = TextStyle(
                    color = textSecondaryColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(labelX - textLayoutResult.size.width / 2f, labelY - textLayoutResult.size.height / 2f)
            )
        }
        speed += step
    }
}

private fun DrawScope.drawSpeedArc(
    center: Offset,
    radius: Float,
    currentSpeed: Float,
    cyanNeonColor: Color,
    electricBlueColor: Color,
    coralWarningColor: Color
) {
    val startAngle = 135.0f
    val totalAngle = 270.0f
    val maxSpeed = 240.0f
    val sweepAngle = (currentSpeed.coerceIn(0f, maxSpeed) / maxSpeed) * totalAngle

    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(cyanNeonColor, electricBlueColor, coralWarningColor)
        ),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawSpeedNeedle(
    center: Offset,
    radius: Float,
    currentSpeed: Float,
    cyanNeonColor: Color
) {
    val startAngle = 135.0
    val totalAngle = 270.0
    val maxSpeed = 240.0f
    val fraction = (currentSpeed.coerceIn(0f, maxSpeed) / maxSpeed).toDouble()
    val angleDeg = startAngle + (fraction * totalAngle)
    val angleRad = angleDeg * (PI / 180.0)

    val tipX = center.x + radius * cos(angleRad).toFloat()
    val tipY = center.y + radius * sin(angleRad).toFloat()
    val baseDistance = radius * 0.4f
    val baseX = center.x + baseDistance * cos(angleRad).toFloat()
    val baseY = center.y + baseDistance * sin(angleRad).toFloat()

    drawLine(
        color = cyanNeonColor,
        start = Offset(baseX, baseY),
        end = Offset(tipX, tipY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
}

// =================================================================================================
// BOTTOM TELEMETRY CONTROLS
// =================================================================================================

@Composable
private fun BottomTelemetryControls(
    state: DashboardUiState,
    strings: AppStrings,
    onIntent: (DashboardUiIntent) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 3-Column Trip Telemetry Card (MAX, AVG, DISTANCE)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DashboardTheme.SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(
                listOf(DashboardTheme.SurfaceBorder, DashboardTheme.SurfaceDark)
            )),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryMetricItem(
                    label = strings.maxSpeed,
                    value = "${state.displayMaxSpeed}",
                    unit = state.speedUnit.speedLabel
                )
                TelemetryMetricDivider()
                TelemetryMetricItem(
                    label = strings.avgSpeed,
                    value = "${state.displayAverageSpeed}",
                    unit = state.speedUnit.speedLabel
                )
                TelemetryMetricDivider()
                TelemetryMetricItem(
                    label = strings.tripDistance,
                    value = formatOneDecimal(state.displayTripDistance),
                    unit = state.speedUnit.distanceLabel
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Trip Recording Panel & Controls
        if (state.isTripRecording) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DashboardTheme.SurfaceDark),
                border = BorderStroke(1.5.dp, if (state.isTripRecordingActive) DashboardTheme.CyanNeon else DashboardTheme.AmberWarning),
                shape = RoundedCornerShape(16.dp)
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
                            text = "🔴 ${state.currentTripName}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DashboardTheme.CyanNeon
                            )
                        )
                        Text(
                            text = if (state.isTripRecordingActive) "RECORDING" else "PAUSED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = if (state.isTripRecordingActive) DashboardTheme.MintActive else DashboardTheme.AmberWarning
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TripMetricView(label = strings.durationLabel, value = formatDuration(state.currentTripElapsedMillis))
                        TripMetricView(label = strings.distanceLabel, value = "${formatOneDecimal(state.tripRecordingDistanceKm)} km")
                        TripMetricView(label = strings.avgSpeedLabel, value = "${state.tripRecordingAvgSpeedKmh.roundToInt()} km/h")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (state.isTripRecordingActive) {
                                    onIntent(DashboardUiIntent.PauseTripRecording)
                                } else {
                                    onIntent(DashboardUiIntent.ResumeTripRecording)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DashboardTheme.SurfaceBorder,
                                contentColor = DashboardTheme.TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text(
                                text = if (state.isTripRecordingActive) strings.pauseTrip else strings.resumeTrip,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Button(
                            onClick = { onIntent(DashboardUiIntent.StopTripRecording) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DashboardTheme.CoralWarning,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text(
                                text = strings.stopTrip,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            // Record Trip Button
            Button(
                onClick = { onIntent(DashboardUiIntent.ShowTripNameDialog) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DashboardTheme.SurfaceDark,
                    contentColor = DashboardTheme.CyanNeon
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DashboardTheme.CyanNeon),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = strings.recordTrip,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Action Controls: Start/Stop Primary Button + Reset Trip + Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            Button(
                onClick = { onIntent(DashboardUiIntent.ResetTrip) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DashboardTheme.SurfaceDark,
                    contentColor = DashboardTheme.TextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(52.dp)
                    .border(1.dp, DashboardTheme.SurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = strings.reset,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Primary Toggle Tracking Button
            val isTracking = state.isTracking
            Button(
                onClick = { onIntent(DashboardUiIntent.ToggleTracking) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) DashboardTheme.CoralWarning else DashboardTheme.CyanNeon,
                    contentColor = if (isTracking) Color.White else DashboardTheme.BackgroundDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text(
                    text = if (isTracking) strings.stopTelemetry else strings.startTelemetry,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                )
            }

            // Quick Settings Launcher Button
            Button(
                onClick = onSettingsClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DashboardTheme.SurfaceDark,
                    contentColor = DashboardTheme.CyanNeon
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(52.dp)
                    .border(1.dp, DashboardTheme.SurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = "⚙",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
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

@Composable
private fun TelemetryMetricItem(
    label: String,
    value: String,
    unit: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardTheme.TextMuted,
                letterSpacing = 0.5.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = DashboardTheme.TextPrimary
                )
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DashboardTheme.TextSecondary
                ),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun TelemetryMetricDivider() {
    Box(
        modifier = Modifier
            .height(28.dp)
            .width(1.dp)
            .background(DashboardTheme.SurfaceBorder)
    )
}

// =================================================================================================
// HELPER UTILITIES (Pure Multiplatform Animation & Formatting)
// =================================================================================================

/**
 * Formats a Double to 1 decimal place without platform-specific String.format.
 */
private fun formatOneDecimal(value: Double): String {
    val rounded = (value * 10).roundToInt()
    val whole = rounded / 10
    val fraction = kotlin.math.abs(rounded % 10)
    return "$whole.$fraction"
}
