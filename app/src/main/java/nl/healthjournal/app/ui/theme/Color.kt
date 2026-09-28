package nl.healthjournal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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

// NHG Category Status Colors (light) — hue is the category's identity, kept identical across themes
val NhgOptimalGreen = Color(0xFF2E7D32)
val NhgNormalGreen = Color(0xFF43A047)
val NhgWarningYellow = Color(0xFFF9A825)
val NhgOrange = Color(0xFFEF6C00)
val NhgDeepOrange = Color(0xFFD84315)
val NhgRed = Color(0xFFC62828)
val NhgSevereRed = Color(0xFFB71C1C)

// NHG Category Status Colors (dark) — lightened/desaturated for WCAG contrast on a dark surface
val NhgOptimalGreenDark = Color(0xFF66BB6A)
val NhgNormalGreenDark = Color(0xFF81C784)
val NhgWarningYellowDark = Color(0xFFFFD54F)
val NhgOrangeDark = Color(0xFFFF8A50)
val NhgDeepOrangeDark = Color(0xFFFF7043)
val NhgRedDark = Color(0xFFE57373)
val NhgSevereRedDark = Color(0xFFEF5350)

// Success message container (Profile/Log screens' confirmation card)
val SuccessContainerLight = Color(0xFFE8F5E9)
val SuccessContainerDark = Color(0xFF1B3A1E)

// Info message container (History screen's Libra-import hint card)
val InfoContainerLight = Color(0xFFE3F2FD)
val InfoContainerDark = Color(0xFF152A3D)

/** Theme-aware container color for a success confirmation card. */
val successContainerColor: Color
    @Composable get() = if (isSystemInDarkTheme()) SuccessContainerDark else SuccessContainerLight

/** Theme-aware text/icon color on a success confirmation card. */
val onSuccessContainerColor: Color
    @Composable get() = if (isSystemInDarkTheme()) NhgOptimalGreenDark else NhgOptimalGreen

/** Theme-aware container color for an informational hint card. */
val infoContainerColor: Color
    @Composable get() = if (isSystemInDarkTheme()) InfoContainerDark else InfoContainerLight

/** Theme-aware text/icon color on an informational hint card. */
val onInfoContainerColor: Color
    @Composable get() = if (isSystemInDarkTheme()) PrimaryBlueLight else PrimaryBlueDark
