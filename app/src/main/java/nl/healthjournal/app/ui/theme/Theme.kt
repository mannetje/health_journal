package nl.healthjournal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import nl.healthjournal.app.settings.ThemeChoice

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryTeal,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = SurfaceLight,
    onSecondary = SurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    secondary = SecondaryTealLight,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = OnPrimaryDark,
    onSecondary = OnSecondaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

/** Whether the app draws the dark palette. Null means no choice was provided, so the system decides. */
private val LocalDarkTheme = compositionLocalOf<Boolean?> { null }

/** Use this instead of [isSystemInDarkTheme] so the in-app theme choice is respected. */
@Composable
fun isAppDarkTheme(): Boolean = LocalDarkTheme.current ?: isSystemInDarkTheme()

@Composable
fun HealthJournalTheme(
    choice: ThemeChoice = ThemeChoice.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = choice.isDark(isSystemInDarkTheme())
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColorScheme else LightColorScheme,
            content = content
        )
    }
}
