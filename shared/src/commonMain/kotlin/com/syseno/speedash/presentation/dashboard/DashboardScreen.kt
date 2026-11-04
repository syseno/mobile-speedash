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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
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

private val CyberpunkColors = DashboardColors(
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

private val ElectricRacingColors = DashboardColors(
    isDark = true,
    background = Color(0xFF0A0E1A),
    surface = Color(0xFF121829),
    surfaceBorder = Color(0xFF1E2842),
    gaugeTrack = Color(0xFF182238),
    cyanNeon = Color(0xFFCCFF00), // Volt Lime
    electricBlue = Color(0xFF00BBF9),
    coralWarning = Color(0xFFFF6B00), // Racing Orange
    amberWarning = Color(0xFFFFD600),
    mintActive = Color(0xFFCCFF00),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF475569),
    needleBase = Color(0xFFCCFF00)
)

private val SportGtColors = DashboardColors(
    isDark = true,
    background = Color(0xFF0A0505),
    surface = Color(0xFF160B0B),
    surfaceBorder = Color(0xFF331414),
    gaugeTrack = Color(0xFF221010),
    cyanNeon = Color(0xFFE60000), // Italian Red
    electricBlue = Color(0xFFFF3333),
    coralWarning = Color(0xFFFF0000),
    amberWarning = Color(0xFFFFD700), // Canary Yellow
    mintActive = Color(0xFF00E676),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB39999),
    textMuted = Color(0xFF664D4D),
    needleBase = Color(0xFFE60000)
)

private val StealthGreenColors = DashboardColors(
    isDark = true,
    background = Color(0xFF06140E),
    surface = Color(0xFF0B2017),
    surfaceBorder = Color(0xFF163E2D),
    gaugeTrack = Color(0xFF102D20),
    cyanNeon = Color(0xFF00E676), // Bio Emerald
    electricBlue = Color(0xFF00B0FF),
    coralWarning = Color(0xFFFF5252),
    amberWarning = Color(0xFFFFD600),
    mintActive = Color(0xFF00E676),
    textPrimary = Color(0xFFE8F5E9),
    textSecondary = Color(0xFF81C784),
    textMuted = Color(0xFF388E3C),
    needleBase = Color(0xFF00E676)
)

private val DaylightLiteColors = DashboardColors(
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

private val DeepSpaceColors = DashboardColors(
    isDark = true,
    background = Color(0xFF050B14),
    surface = Color(0xFF0E1526),
    surfaceBorder = Color(0xFF1E2942),
    gaugeTrack = Color(0xFF141E33),
    cyanNeon = Color(0xFF9D4EDD), // Hyper Violet
    electricBlue = Color(0xFF00F5D4),
    coralWarning = Color(0xFFFF0055),
    amberWarning = Color(0xFFFFB703),
    mintActive = Color(0xFF00F5D4),
    textPrimary = Color(0xFFF3E8FF),
    textSecondary = Color(0xFFC084FC),
    textMuted = Color(0xFF6B21A8),
    needleBase = Color(0xFF9D4EDD)
)

private val LocalDashboardColors = staticCompositionLocalOf { CyberpunkColors }

@Composable
fun DashboardThemeWrapper(
    themeMode: ThemeMode,
    cockpitSkin: CockpitSkin = CockpitSkin.CYBERPUNK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isLightMode = when (themeMode) {
        ThemeMode.SYSTEM -> !isSystemDark
        ThemeMode.DARK -> false
        ThemeMode.LIGHT -> true
    }

    val colors = if (isLightMode && cockpitSkin == CockpitSkin.CYBERPUNK) {
        DaylightLiteColors
    } else {
        when (cockpitSkin) {
            CockpitSkin.CYBERPUNK -> CyberpunkColors
            CockpitSkin.ELECTRIC_RACING -> ElectricRacingColors
            CockpitSkin.SPORT_GT -> SportGtColors
            CockpitSkin.STEALTH_GREEN -> StealthGreenColors
            CockpitSkin.DAYLIGHT_LITE -> DaylightLiteColors
            CockpitSkin.DEEP_SPACE -> DeepSpaceColors
        }
    }

    CompositionLocalProvider(LocalDashboardColors provides colors) {
        MaterialTheme(
            colorScheme = if (colors.isDark) darkColorScheme() else lightColorScheme(),
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
        DashboardThemeWrapper(themeMode = state.themeMode, cockpitSkin = state.cockpitSkin) {
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

    DashboardThemeWrapper(themeMode = state.themeMode, cockpitSkin = state.cockpitSkin) {
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
    DashboardThemeWrapper(themeMode = state.themeMode, cockpitSkin = state.cockpitSkin) {
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        when (state.gaugeLayout) {
                            GaugeLayout.SPORT_RACING -> {
                                SportRacingDashboardLayout(
                                    state = state,
                                    strings = strings,
                                    onIntent = onIntent,
                                    onSettingsClick = { isSettingsOpen = true }
                                )
                            }
                            GaugeLayout.CIRCULAR_STANDARD -> {
                                StandardDashboardLayout(
                                    state = state,
                                    strings = strings,
                                    onIntent = onIntent,
                                    onSettingsClick = { isSettingsOpen = true }
                                )
                            }
                            GaugeLayout.MINIMAL_HUD -> {
                                MinimalHudDashboardLayout(
                                    state = state,
                                    strings = strings,
                                    onIntent = onIntent,
                                    onSettingsClick = { isSettingsOpen = true }
                                )
                            }
                            GaugeLayout.DUAL_CLUSTER -> {
                                DualClusterDashboardLayout(
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
// SPEED ALERT & INSTRUMENT CLUSTER UTILITIES
// =================================================================================================

internal enum class SpeedAlertLevel {
    NORMAL,
    WARNING,
    DANGER
}

internal fun getSpeedAlertLevel(
    speedKmh: Double,
    vehicleType: VehicleType
): SpeedAlertLevel {
    if (vehicleType != VehicleType.CAR && vehicleType != VehicleType.MOTORCYCLE) {
        return SpeedAlertLevel.NORMAL
    }
    return when {
        speedKmh >= 100.0 -> SpeedAlertLevel.DANGER
        speedKmh >= 70.0 -> SpeedAlertLevel.WARNING
        else -> SpeedAlertLevel.NORMAL
    }
}

@Composable
private fun SpeedashInstrumentCluster(
    state: DashboardUiState,
    onUnitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val animatedSpeed by animateFloatAsState(
        targetValue = state.smoothedSpeedKmh.toFloat().coerceIn(0f, 240f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "NeedleSpeedAnimation"
    )

    val alertLevel = getSpeedAlertLevel(state.smoothedSpeedKmh, state.vehicleType)
    val alertColor = when (alertLevel) {
        SpeedAlertLevel.NORMAL -> DashboardTheme.NeedleBase
        SpeedAlertLevel.WARNING -> DashboardTheme.AmberWarning
        SpeedAlertLevel.DANGER -> DashboardTheme.CoralWarning
    }

    val gaugeTrackColor = DashboardTheme.GaugeTrack
    val cyanNeonColor = DashboardTheme.CyanNeon
    val electricBlueColor = DashboardTheme.ElectricBlue
    val amberWarningColor = DashboardTheme.AmberWarning
    val coralWarningColor = DashboardTheme.CoralWarning
    val textPrimaryColor = DashboardTheme.TextPrimary
    val textMutedColor = DashboardTheme.TextMuted

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val sizePx = constraints.maxWidth.coerceAtMost(constraints.maxHeight).toFloat()
        val diameterDp = (sizePx / 2.625f).dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val gaugeRadius = (size.minDimension / 2f) - 16.dp.toPx()

            drawSpeedGauge(
                center = center,
                radius = gaugeRadius,
                currentSpeed = animatedSpeed,
                textMeasurer = textMeasurer,
                gaugeTrackColor = gaugeTrackColor,
                cyanNeonColor = cyanNeonColor,
                electricBlueColor = electricBlueColor,
                amberWarningColor = amberWarningColor,
                coralWarningColor = coralWarningColor,
                textPrimaryColor = textPrimaryColor,
                textMutedColor = textMutedColor
            )

            drawNeedle(
                center = center,
                radius = gaugeRadius,
                currentSpeed = animatedSpeed,
                needleColor = alertColor
            )
        }

        CenterDigitalHud(
            state = state,
            alertLevel = alertLevel,
            alertColor = alertColor,
            onUnitClick = onUnitClick,
            modifier = Modifier.size(diameterDp)
        )
    }
}

private const val GAUGE_START_ANGLE = 140f
private const val GAUGE_SWEEP_ANGLE = 260f
private const val GAUGE_MAX_SPEED = 240f

private fun DrawScope.drawSpeedGauge(
    center: Offset,
    radius: Float,
    currentSpeed: Float,
    textMeasurer: TextMeasurer,
    gaugeTrackColor: Color,
    cyanNeonColor: Color,
    electricBlueColor: Color,
    amberWarningColor: Color,
    coralWarningColor: Color,
    textPrimaryColor: Color,
    textMutedColor: Color
) {
    val strokeWidth = 14.dp.toPx()
    val arcRect = Size(radius * 2, radius * 2)
    val arcTopLeft = Offset(center.x - radius, center.y - radius)

    drawArc(
        color = gaugeTrackColor,
        startAngle = GAUGE_START_ANGLE,
        sweepAngle = GAUGE_SWEEP_ANGLE,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcRect,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    val clampedSpeed = currentSpeed.coerceIn(0f, GAUGE_MAX_SPEED)
    val activeSweep = (clampedSpeed / GAUGE_MAX_SPEED) * GAUGE_SWEEP_ANGLE

    if (activeSweep > 0.5f) {
        val speedGradient = Brush.sweepGradient(
            0.35f to cyanNeonColor,
            0.65f to electricBlueColor,
            0.85f to amberWarningColor,
            1.00f to coralWarningColor,
            center = center
        )

        drawArc(
            brush = speedGradient,
            startAngle = GAUGE_START_ANGLE,
            sweepAngle = activeSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcRect,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }

    val totalSteps = 24
    for (i in 0..totalSteps) {
        val speedValue = i * 10
        val isMajor = speedValue % 20 == 0
        val fraction = speedValue / GAUGE_MAX_SPEED
        val angleDeg = GAUGE_START_ANGLE + (fraction * GAUGE_SWEEP_ANGLE)
        val angleRad = angleDeg * (PI.toFloat() / 180f)

        val tickLength = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
        val tickStroke = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
        val tickColor = if (speedValue <= clampedSpeed) cyanNeonColor else textMutedColor

        val outerX = center.x + (radius - 10.dp.toPx()) * cos(angleRad)
        val outerY = center.y + (radius - 10.dp.toPx()) * sin(angleRad)
        val innerX = center.x + (radius - 10.dp.toPx() - tickLength) * cos(angleRad)
        val innerY = center.y + (radius - 10.dp.toPx() - tickLength) * sin(angleRad)

        drawLine(
            color = tickColor,
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )

        if (speedValue % 40 == 0) {
            val textStyle = TextStyle(
                color = if (speedValue <= clampedSpeed) textPrimaryColor else textMutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            val textLayout = textMeasurer.measure(speedValue.toString(), textStyle)
            val textRadius = radius - 28.dp.toPx()
            val textX = center.x + textRadius * cos(angleRad) - (textLayout.size.width / 2f)
            val textY = center.y + textRadius * sin(angleRad) - (textLayout.size.height / 2f)

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(textX, textY)
            )
        }
    }
}

private fun DrawScope.drawNeedle(
    center: Offset,
    radius: Float,
    currentSpeed: Float,
    needleColor: Color
) {
    val clampedSpeed = currentSpeed.coerceIn(0f, GAUGE_MAX_SPEED)
    val fraction = clampedSpeed / GAUGE_MAX_SPEED
    val needleAngleDeg = GAUGE_START_ANGLE + (fraction * GAUGE_SWEEP_ANGLE)
    val needleAngleRad = needleAngleDeg * (PI.toFloat() / 180f)

    val outerTipDistance = radius - 10.dp.toPx()
    val innerBaseDistance = outerTipDistance * 0.52f
    val needleBaseWidth = 4.dp.toPx()

    val perpRad = needleAngleRad + (PI.toFloat() / 2f)
    val perpX = needleBaseWidth * cos(perpRad)
    val perpY = needleBaseWidth * sin(perpRad)

    val tipX = center.x + outerTipDistance * cos(needleAngleRad)
    val tipY = center.y + outerTipDistance * sin(needleAngleRad)

    val baseX = center.x + innerBaseDistance * cos(needleAngleRad)
    val baseY = center.y + innerBaseDistance * sin(needleAngleRad)

    val needlePath = Path().apply {
        moveTo(tipX, tipY)
        lineTo(baseX + perpX, baseY + perpY)
        lineTo(baseX - perpX, baseY - perpY)
        close()
    }

    drawPath(
        path = needlePath,
        color = needleColor.copy(alpha = 0.35f),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    drawPath(
        path = needlePath,
        color = needleColor,
        style = Fill
    )
}

@Composable
private fun CenterDigitalHud(
    state: DashboardUiState,
    alertLevel: SpeedAlertLevel,
    alertColor: Color,
    onUnitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speedTextColor = when (alertLevel) {
        SpeedAlertLevel.NORMAL -> DashboardTheme.TextPrimary
        SpeedAlertLevel.WARNING -> DashboardTheme.AmberWarning
        SpeedAlertLevel.DANGER -> DashboardTheme.CoralWarning
    }

    Column(
        modifier = modifier
            .clip(CircleShape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (alertLevel != SpeedAlertLevel.NORMAL) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(alertColor.copy(alpha = 0.2f))
                    .border(1.dp, alertColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (alertLevel == SpeedAlertLevel.DANGER) "🚨 DANGER" else "⚠️ WARNING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = alertColor
                    )
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.hasGpsFix) DashboardTheme.MintActive else DashboardTheme.AmberWarning
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SPEEDASH",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp,
                        color = DashboardTheme.TextSecondary
                    )
                )
            }
        }

        Text(
            text = "${state.displaySpeed}",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = speedTextColor,
                letterSpacing = (-1.5).sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DashboardTheme.SurfaceDark)
                .border(1.5.dp, alertColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onUnitClick
                )
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = state.speedUnit.speedLabel.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = alertColor,
                    letterSpacing = 1.5.sp
                )
            )
        }
    }
}

@Composable
internal fun VehicleIconDisplay(
    vehicleType: VehicleType,
    color: Color = DashboardTheme.CyanNeon,
    modifier: Modifier = Modifier
) {
    val mutedColor = DashboardTheme.TextMuted
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (vehicleType) {
            VehicleType.CAR -> {
                val carPath = Path().apply {
                    moveTo(w * 0.05f, h * 0.75f)
                    lineTo(w * 0.12f, h * 0.50f)
                    lineTo(w * 0.28f, h * 0.45f)
                    lineTo(w * 0.42f, h * 0.20f)
                    lineTo(w * 0.72f, h * 0.20f)
                    lineTo(w * 0.88f, h * 0.45f)
                    lineTo(w * 0.95f, h * 0.52f)
                    lineTo(w * 0.95f, h * 0.75f)
                    lineTo(w * 0.85f, h * 0.75f)
                    lineTo(w * 0.82f, h * 0.60f)
                    lineTo(w * 0.68f, h * 0.60f)
                    lineTo(w * 0.65f, h * 0.75f)
                    lineTo(w * 0.35f, h * 0.75f)
                    lineTo(w * 0.32f, h * 0.60f)
                    lineTo(w * 0.18f, h * 0.60f)
                    lineTo(w * 0.15f, h * 0.75f)
                    close()
                }
                drawPath(carPath, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(color = color, radius = h * 0.18f, center = Offset(w * 0.25f, h * 0.75f), style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = color, radius = h * 0.18f, center = Offset(w * 0.75f, h * 0.75f), style = Stroke(width = 1.5.dp.toPx()))
            }
            VehicleType.MOTORCYCLE -> {
                drawCircle(color = color, radius = h * 0.22f, center = Offset(w * 0.22f, h * 0.70f), style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = color, radius = h * 0.22f, center = Offset(w * 0.78f, h * 0.70f), style = Stroke(width = 1.5.dp.toPx()))

                val framePath = Path().apply {
                    moveTo(w * 0.22f, h * 0.70f)
                    lineTo(w * 0.45f, h * 0.45f)
                    lineTo(w * 0.70f, h * 0.35f)
                    lineTo(w * 0.78f, h * 0.70f)
                    moveTo(w * 0.70f, h * 0.35f)
                    lineTo(w * 0.65f, h * 0.20f)
                    lineTo(w * 0.52f, h * 0.25f)
                    lineTo(w * 0.35f, h * 0.38f)
                    lineTo(w * 0.25f, h * 0.38f)
                    lineTo(w * 0.45f, h * 0.45f)
                }
                drawPath(framePath, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
            }
            VehicleType.TRAIN -> {
                val trainPath = Path().apply {
                    moveTo(w * 0.05f, h * 0.80f)
                    lineTo(w * 0.05f, h * 0.25f)
                    lineTo(w * 0.65f, h * 0.25f)
                    lineTo(w * 0.92f, h * 0.55f)
                    lineTo(w * 0.95f, h * 0.80f)
                    close()
                }
                drawPath(trainPath, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))

                val windowPath = Path().apply {
                    moveTo(w * 0.68f, h * 0.33f)
                    lineTo(w * 0.88f, h * 0.55f)
                    lineTo(w * 0.72f, h * 0.55f)
                    lineTo(w * 0.62f, h * 0.33f)
                    close()
                }
                drawPath(windowPath, color = color, style = Fill)
                drawLine(
                    color = mutedColor,
                    start = Offset(0f, h * 0.92f),
                    end = Offset(w, h * 0.92f),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            VehicleType.SHIP -> {
                val hullPath = Path().apply {
                    moveTo(w * 0.05f, h * 0.45f)
                    lineTo(w * 0.20f, h * 0.80f)
                    lineTo(w * 0.85f, h * 0.80f)
                    lineTo(w * 0.95f, h * 0.45f)
                    close()
                }
                drawPath(hullPath, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))

                val cabinPath = Path().apply {
                    moveTo(w * 0.25f, h * 0.45f)
                    lineTo(w * 0.25f, h * 0.25f)
                    lineTo(w * 0.65f, h * 0.25f)
                    lineTo(w * 0.75f, h * 0.45f)
                }
                drawPath(cabinPath, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))

                drawLine(
                    color = color,
                    start = Offset(w * 0.45f, h * 0.25f),
                    end = Offset(w * 0.45f, h * 0.12f),
                    strokeWidth = 1.5.dp.toPx()
                )
                val wavePath = Path().apply {
                    moveTo(0f, h * 0.88f)
                    quadraticTo(w * 0.25f, h * 0.80f, w * 0.5f, h * 0.88f)
                    quadraticTo(w * 0.75f, h * 0.96f, w, h * 0.88f)
                }
                drawPath(wavePath, color = mutedColor, style = Stroke(width = 1.dp.toPx()))
            }
            VehicleType.PLANE -> {
                val planePath = Path().apply {
                    moveTo(w * 0.50f, h * 0.08f)
                    lineTo(w * 0.55f, h * 0.35f)
                    lineTo(w * 0.95f, h * 0.58f)
                    lineTo(w * 0.95f, h * 0.68f)
                    lineTo(w * 0.55f, h * 0.55f)
                    lineTo(w * 0.55f, h * 0.82f)
                    lineTo(w * 0.72f, h * 0.92f)
                    lineTo(w * 0.72f, h * 0.98f)
                    lineTo(w * 0.50f, h * 0.92f)
                    lineTo(w * 0.28f, h * 0.98f)
                    lineTo(w * 0.28f, h * 0.92f)
                    lineTo(w * 0.45f, h * 0.82f)
                    lineTo(w * 0.45f, h * 0.55f)
                    lineTo(w * 0.05f, h * 0.68f)
                    lineTo(w * 0.05f, h * 0.58f)
                    lineTo(w * 0.45f, h * 0.35f)
                    close()
                }
                drawPath(planePath, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                drawPath(planePath, color = color.copy(alpha = 0.25f), style = Fill)
            }
        }
    }
}

@Composable
private fun TopStatusHeader(
    trackingStatus: TrackingStatus,
    accuracyMeters: Float?,
    hasFix: Boolean,
    headingDegrees: Int,
    cardinalDirection: String,
    hasCompassSensor: Boolean,
    strings: AppStrings,
    speedAlertLevel: SpeedAlertLevel = SpeedAlertLevel.NORMAL,
    vehicleType: VehicleType = VehicleType.CAR,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (statusText, statusColor) = when (trackingStatus) {
                TrackingStatus.Idle -> strings.statusStandby to DashboardTheme.TextSecondary
                TrackingStatus.Acquiring -> strings.statusAcquiring to DashboardTheme.AmberWarning
                TrackingStatus.Active -> strings.statusActive to DashboardTheme.MintActive
                TrackingStatus.PermissionDenied -> strings.statusPermissionDenied to DashboardTheme.CoralWarning
                is TrackingStatus.Error -> strings.statusGpsError to DashboardTheme.CoralWarning
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DashboardTheme.SurfaceDark)
                    .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        letterSpacing = 1.sp
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val accuracyText = if (hasFix && accuracyMeters != null) {
                    strings.accuracyFix(accuracyMeters.roundToInt())
                } else {
                    strings.noFix
                }

                Text(
                    text = accuracyText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasFix) DashboardTheme.TextSecondary else DashboardTheme.TextMuted
                    )
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DashboardTheme.SurfaceDark)
                        .border(1.dp, DashboardTheme.SurfaceBorder, CircleShape)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚙",
                        style = MaterialTheme.typography.bodyLarge.copy(color = DashboardTheme.CyanNeon)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "${headingDegrees.toString().padStart(3, '0')}° $cardinalDirection",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = DashboardTheme.CyanNeon,
                letterSpacing = 1.5.sp
            )
        )

        Text(
            text = "▲",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = DashboardTheme.CyanNeon
            ),
            modifier = Modifier.padding(top = 2.dp)
        )

        val headerIconColor = if (speedAlertLevel == SpeedAlertLevel.DANGER) DashboardTheme.CoralWarning else if (speedAlertLevel == SpeedAlertLevel.WARNING) DashboardTheme.AmberWarning else DashboardTheme.CyanNeon
        VehicleIconDisplay(
            vehicleType = vehicleType,
            color = headerIconColor,
            modifier = Modifier
                .padding(top = 4.dp)
                .size(width = 36.dp, height = 24.dp)
        )

        if (speedAlertLevel != SpeedAlertLevel.NORMAL) {
            val alertColor = if (speedAlertLevel == SpeedAlertLevel.DANGER) DashboardTheme.CoralWarning else DashboardTheme.AmberWarning
            val alertText = if (speedAlertLevel == SpeedAlertLevel.DANGER) "🚨 DANGER" else "⚠️ WARNING"

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(alertColor.copy(alpha = 0.18f))
                    .border(1.dp, alertColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = alertText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = alertColor,
                        letterSpacing = 1.5.sp
                    )
                )
            }
        }
    }
}

// =================================================================================================
// DASHBOARD CLUSTER LAYOUT STYLES
// =================================================================================================

/**
 * Supercar Sport GT Racing Layout (Matching the user's screenshot!):
 * - Top Tachometer / RPM intensity bar numbered 1..8
 * - Left Telemetry Wing Card (Max Speed, Avg Speed, Trip Dist)
 * - Center Circular Speedometer Gauge with dynamic needle & digital readout
 * - Right Telemetry Wing Card (Altitude, Heading, Accuracy)
 * - Bottom Action Controls
 */
@Composable
private fun SportRacingDashboardLayout(
    state: DashboardUiState,
    strings: AppStrings,
    onIntent: (DashboardUiIntent) -> Unit,
    onSettingsClick: () -> Unit
) {
    val speedAlertLevel = getSpeedAlertLevel(state.smoothedSpeedKmh, state.vehicleType)
    val alertColor = if (speedAlertLevel == SpeedAlertLevel.DANGER) DashboardTheme.CoralWarning else if (speedAlertLevel == SpeedAlertLevel.WARNING) DashboardTheme.AmberWarning else DashboardTheme.CyanNeon

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Header + Tachometer RPM Bar + Turn Indicators
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TopStatusHeaderRow(state = state, strings = strings, onSettingsClick = onSettingsClick)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "◄",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = alertColor,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp)
                )

                TopTachometerRpmBar(
                    currentSpeedKmh = state.smoothedSpeedKmh,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "►",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = alertColor,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // Center Section: Left Wing + Center Gauge + Right Wing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LeftTelemetryWingCard(
                state = state,
                strings = strings,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .weight(1.8f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                SpeedashInstrumentCluster(
                    state = state,
                    onUnitClick = { onIntent(DashboardUiIntent.CycleSpeedUnit) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            RightTelemetryWingCard(
                state = state,
                strings = strings,
                modifier = Modifier.weight(1f)
            )
        }

        // Bottom Section: Action Controls
        BottomTelemetryControls(
            state = state,
            strings = strings,
            onIntent = onIntent,
            onSettingsClick = onSettingsClick
        )
    }
}

/**
 * Top Segmented Tachometer / Speed Intensity LED Bar across the top of the dashboard.
 */
@Composable
private fun TopTachometerRpmBar(
    currentSpeedKmh: Double,
    maxGaugeSpeed: Float = 240f,
    modifier: Modifier = Modifier
) {
    val cyanNeon = DashboardTheme.CyanNeon
    val amberWarning = DashboardTheme.AmberWarning
    val coralWarning = DashboardTheme.CoralWarning
    val surfaceBorder = DashboardTheme.SurfaceBorder
    val textPrimary = DashboardTheme.TextPrimary

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Numbers 1 to 8 above bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..8).forEach { num ->
                Text(
                    text = "$num",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = textPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Segmented LED intensity bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .padding(horizontal = 8.dp)
        ) {
            val totalSegments = 32
            val segmentGap = 3.dp.toPx()
            val totalGaps = (totalSegments - 1) * segmentGap
            val segmentWidth = (size.width - totalGaps) / totalSegments
            val fillFraction = (currentSpeedKmh / maxGaugeSpeed).coerceIn(0.0, 1.0).toFloat()
            val activeSegments = (fillFraction * totalSegments).roundToInt()

            for (i in 0 until totalSegments) {
                val left = i * (segmentWidth + segmentGap)
                val top = 0f
                val right = left + segmentWidth
                val bottom = size.height

                val segFraction = i.toFloat() / totalSegments
                val segmentColor = when {
                    i >= activeSegments -> surfaceBorder.copy(alpha = 0.35f)
                    segFraction >= 0.8f -> coralWarning
                    segFraction >= 0.5f -> amberWarning
                    else -> cyanNeon
                }

                drawRoundRect(
                    color = segmentColor,
                    topLeft = Offset(left, top),
                    size = Size(segmentWidth, bottom),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

/**
 * Left Telemetry Wing Card (Sloped sport card for Max, Avg, Trip Dist).
 */
@Composable
private fun LeftTelemetryWingCard(
    state: DashboardUiState,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 4.dp, bottomEnd = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardTheme.SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(DashboardTheme.CyanNeon, DashboardTheme.SurfaceBorder)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WingMetricItem(
                label = strings.maxSpeed,
                value = "${state.displayMaxSpeed}",
                unit = state.speedUnit.speedLabel
            )
            WingMetricItem(
                label = strings.avgSpeed,
                value = "${state.displayAverageSpeed}",
                unit = state.speedUnit.speedLabel
            )
            WingMetricItem(
                label = strings.tripDistance,
                value = formatOneDecimal(state.displayTripDistance),
                unit = state.speedUnit.distanceLabel
            )
        }
    }
}

/**
 * Right Telemetry Wing Card (Sloped sport card for Altitude, Heading, Accuracy).
 */
@Composable
private fun RightTelemetryWingCard(
    state: DashboardUiState,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, bottomStart = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardTheme.SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(DashboardTheme.SurfaceBorder, DashboardTheme.CyanNeon)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WingMetricItem(
                label = if (state.hasBarometerSensor) "ALTITUDE" else "GPS FIX",
                value = if (state.hasBarometerSensor && state.displayAltitudeMeters != null) "${state.displayAltitudeMeters}" else if (state.hasGpsFix) "FIXED" else "NO FIX",
                unit = if (state.hasBarometerSensor) "m" else ""
            )
            WingMetricItem(
                label = "HEADING",
                value = "${state.headingDegrees}°",
                unit = state.cardinalDirection
            )
            WingMetricItem(
                label = "ACCURACY",
                value = if (state.gpsAccuracyMeters != null) "±${state.gpsAccuracyMeters!!.roundToInt()}" else "--",
                unit = "m"
            )
        }
    }
}

@Composable
private fun WingMetricItem(
    label: String,
    value: String,
    unit: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardTheme.TextMuted,
                letterSpacing = 0.5.sp
            )
        )
        Spacer(modifier = Modifier.height(1.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = DashboardTheme.CyanNeon,
                    fontSize = 14.sp
                )
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DashboardTheme.TextSecondary
                    ),
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

/**
 * Standard Cockpit Layout:
 * - Top Status Header Row
 * - Center Circular Gauge
 * - Barometer Altitude Readout Pill
 * - Bottom Telemetry Cards & Controls
 */
@Composable
private fun StandardDashboardLayout(
    state: DashboardUiState,
    strings: AppStrings,
    onIntent: (DashboardUiIntent) -> Unit,
    onSettingsClick: () -> Unit
) {
    val speedAlertLevel = getSpeedAlertLevel(state.smoothedSpeedKmh, state.vehicleType)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TopStatusHeader(
            trackingStatus = state.trackingStatus,
            accuracyMeters = state.gpsAccuracyMeters,
            hasFix = state.hasGpsFix,
            headingDegrees = state.headingDegrees,
            cardinalDirection = strings.cardinalDirection(state.headingDegree),
            hasCompassSensor = state.hasCompassSensor,
            strings = strings,
            speedAlertLevel = speedAlertLevel,
            vehicleType = state.vehicleType,
            onSettingsClick = onSettingsClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SpeedashInstrumentCluster(
                    state = state,
                    onUnitClick = { onIntent(DashboardUiIntent.CycleSpeedUnit) },
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                )
            }

            if (state.hasBarometerSensor && state.displayAltitudeMeters != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DashboardTheme.SurfaceDark)
                        .border(1.dp, DashboardTheme.SurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(text = "⛰️", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.altitudeFormat(state.displayAltitudeMeters!!),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = DashboardTheme.CyanNeon,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }

        BottomTelemetryControls(
            state = state,
            strings = strings,
            onIntent = onIntent,
            onSettingsClick = onSettingsClick
        )
    }
}

/**
 * Minimalist HUD Layout:
 * - Clean Distraction-Free Layout with massive digital speed readout & sleek speed arc.
 */
@Composable
private fun MinimalHudDashboardLayout(
    state: DashboardUiState,
    strings: AppStrings,
    onIntent: (DashboardUiIntent) -> Unit,
    onSettingsClick: () -> Unit
) {
    val speedAlertLevel = getSpeedAlertLevel(state.smoothedSpeedKmh, state.vehicleType)
    val alertColor = if (speedAlertLevel == SpeedAlertLevel.DANGER) DashboardTheme.CoralWarning else if (speedAlertLevel == SpeedAlertLevel.WARNING) DashboardTheme.AmberWarning else DashboardTheme.CyanNeon

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopStatusHeaderRow(state = state, strings = strings, onSettingsClick = onSettingsClick)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${state.displaySpeed}",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 92.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = alertColor,
                    letterSpacing = (-2.5).sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DashboardTheme.SurfaceDark)
                    .border(1.5.dp, alertColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .clickable { onIntent(DashboardUiIntent.CycleSpeedUnit) }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = state.speedUnit.speedLabel.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = alertColor,
                        letterSpacing = 1.5.sp
                    )
                )
            }
        }

        BottomTelemetryControls(
            state = state,
            strings = strings,
            onIntent = onIntent,
            onSettingsClick = onSettingsClick
        )
    }
}

/**
 * Dual Ring Cluster Layout:
 * - Side-by-Side twin rings: Left ring for Speedometer, Right ring for Compass & Navigation.
 */
@Composable
private fun DualClusterDashboardLayout(
    state: DashboardUiState,
    strings: AppStrings,
    onIntent: (DashboardUiIntent) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TopStatusHeaderRow(state = state, strings = strings, onSettingsClick = onSettingsClick)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Ring: Speedometer Gauge
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                SpeedashInstrumentCluster(
                    state = state,
                    onUnitClick = { onIntent(DashboardUiIntent.CycleSpeedUnit) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Right Ring: Navigation Compass & Altitude Ring
            Card(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = DashboardTheme.SurfaceDark),
                border = BorderStroke(2.dp, DashboardTheme.CyanNeon)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "▲",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = DashboardTheme.CyanNeon
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.headingDegrees}°",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = DashboardTheme.TextPrimary
                        )
                    )
                    Text(
                        text = state.cardinalDirection,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = DashboardTheme.CyanNeon
                        )
                    )
                    if (state.hasBarometerSensor && state.displayAltitudeMeters != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.altitudeFormat(state.displayAltitudeMeters!!),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DashboardTheme.TextSecondary
                            )
                        )
                    }
                }
            }
        }

        BottomTelemetryControls(
            state = state,
            strings = strings,
            onIntent = onIntent,
            onSettingsClick = onSettingsClick
        )
    }
}

/**
 * Reusable Top Status Header Row for custom layouts.
 */
@Composable
private fun TopStatusHeaderRow(
    state: DashboardUiState,
    strings: AppStrings,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (statusText, statusColor) = when (state.trackingStatus) {
            TrackingStatus.Idle -> strings.statusStandby to DashboardTheme.TextSecondary
            TrackingStatus.Acquiring -> strings.statusAcquiring to DashboardTheme.AmberWarning
            TrackingStatus.Active -> strings.statusActive to DashboardTheme.MintActive
            TrackingStatus.PermissionDenied -> strings.statusPermissionDenied to DashboardTheme.CoralWarning
            is TrackingStatus.Error -> strings.statusGpsError to DashboardTheme.CoralWarning
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DashboardTheme.SurfaceDark)
                .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    letterSpacing = 1.sp
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val accuracyText = if (state.hasGpsFix && state.gpsAccuracyMeters != null) {
                strings.accuracyFix(state.gpsAccuracyMeters!!.roundToInt())
            } else {
                strings.noFix
            }

            Text(
                text = accuracyText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.hasGpsFix) DashboardTheme.TextSecondary else DashboardTheme.TextMuted
                )
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(DashboardTheme.SurfaceDark)
                    .border(1.dp, DashboardTheme.SurfaceBorder, CircleShape)
                    .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚙",
                    style = MaterialTheme.typography.bodyLarge.copy(color = DashboardTheme.CyanNeon)
                )
            }
        }
    }
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
