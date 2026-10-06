package nl.healthjournal.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme base tokens
val PrimaryBlue = Color(0xFF1E88E5)
val PrimaryBlueDark = Color(0xFF1565C0)
val SecondaryTeal = Color(0xFF00897B)
val BackgroundLight = Color(0xFFF8F9FA)
val SurfaceLight = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF212121)
val TextSecondary = Color(0xFF757575)

// Dark theme base tokens
val PrimaryBlueLight = Color(0xFF90CAF9)
val SecondaryTealLight = Color(0xFF80CBC4)
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val OnPrimaryDark = Color(0xFF00325B)
val OnSecondaryDark = Color(0xFF00332D)
val TextPrimaryDark = Color(0xFFECECEC)
val TextSecondaryDark = Color(0xFFB0B0B0)

// Brand navy: the launcher-icon tile and the top app bar, so the logo always sits on the colour it was designed for
val BrandNavy = Color(0xFF0B1D3A)

// Neutral sequential range ramp (light): a darker step is a higher band, never a verdict (range-labels)
val RangeStep0 = Color(0xFF546E7A)
val RangeStep1 = Color(0xFF1E6FB5)
val RangeStep2 = Color(0xFF3949AB)
val RangeStep3 = Color(0xFF283593)
val RangeStep4 = Color(0xFF1A237E)

// Neutral sequential range ramp (dark): lighter steps for contrast on a dark surface, same order
val RangeStep0Dark = Color(0xFFB0BEC5)
val RangeStep1Dark = Color(0xFF64B5F6)
val RangeStep2Dark = Color(0xFF7986CB)
val RangeStep3Dark = Color(0xFF9FA8DA)
val RangeStep4Dark = Color(0xFFC5CAE9)

// Success confirmation green
val SuccessGreen = Color(0xFF2E7D32)
val SuccessGreenDark = Color(0xFF66BB6A)

// Success message container (Profile/Log screens' confirmation card)
val SuccessContainerLight = Color(0xFFE8F5E9)
val SuccessContainerDark = Color(0xFF1B3A1E)

// Info message container (History screen's Libra-import hint card)
val InfoContainerLight = Color(0xFFE3F2FD)
val InfoContainerDark = Color(0xFF152A3D)

/** Theme-aware container color for a success confirmation card. */
val successContainerColor: Color
    @Composable get() = if (isAppDarkTheme()) SuccessContainerDark else SuccessContainerLight

/** Theme-aware text/icon color on a success confirmation card. */
val onSuccessContainerColor: Color
    @Composable get() = if (isAppDarkTheme()) SuccessGreenDark else SuccessGreen

/** Theme-aware container color for an informational hint card. */
val infoContainerColor: Color
    @Composable get() = if (isAppDarkTheme()) InfoContainerDark else InfoContainerLight

/** Theme-aware text/icon color on an informational hint card. */
val onInfoContainerColor: Color
    @Composable get() = if (isAppDarkTheme()) PrimaryBlueLight else PrimaryBlueDark
