# Design: Dark Theme Support

## Context
`HealthJournalTheme` wraps every screen in a single `MaterialTheme(colorScheme = LightColorScheme)`. There is no `darkColorScheme()`, no `isSystemInDarkTheme()` check, and no `android:theme`/`Theme.xml` day-night split — the whole app renders light regardless of the device setting. `Color.kt` defines a handful of base tokens (`PrimaryBlue`, `BackgroundLight`, `SurfaceLight`, `TextPrimary`, `TextSecondary`) plus six NHG status colors (`NhgOptimalGreen` … `NhgSevereRed`) that are reused across the Log, History, and (future) Trends screens to color-code clinical categories.

## Goals
- Respect the device's system light/dark setting via `isSystemInDarkTheme()`.
- Keep the NHG category color semantics identical in both themes (e.g. "High Normal" blood pressure is still recognizably the same amber in dark mode as in light mode), only adjusting luminance/contrast, not hue, so a user switching themes doesn't have to relearn the color code.
- Provide the same `background`/`surface`/`onBackground`/`onSurface`/primary/secondary color slots for dark mode that already exist for light mode, so no composable needs an `if (isDark)` branch of its own — everything reads from `MaterialTheme.colorScheme`.

## Non-goals
- No in-app theme override (a manual "Light/Dark/System" setting in Profile) — this change only follows the OS setting. An override can be a follow-up once there's a settings surface to put it in.
- No rebrand of the light theme's existing colors — `LightColorScheme` keeps its current values; only a dark counterpart is added.
- No changes to domain/data modules.

## Approach
- Add `DarkColorScheme = darkColorScheme(...)` in `Theme.kt` mirroring the slots already set on `LightColorScheme`, and select between them with:
  ```kotlin
  val colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
  ```
- Add dark-mode counterparts in `Color.kt` (e.g. `BackgroundDark`, `SurfaceDark`, `TextPrimaryDark`, `TextSecondaryDark`) following Material 3's tonal conventions (dark surface ≈ `#121212`–`#1E1E1E`, not pure black, to match Material guidance and the MyHeart reference screenshot's dark surface tone).
- For the six NHG status colors, keep one canonical hue per category but verify each against WCAG contrast on both a light surface and a dark surface; where a color fails contrast in dark mode (most likely `NhgWarningYellow` against a dark background needs a slightly desaturated/lightened variant), add a dark variant with the same category meaning.
- Audit pass: grep the three existing screens for `Color(0x` literals not already sourced from `Color.kt`/`MaterialTheme.colorScheme`, and convert any found to theme-aware tokens.

## Alternatives considered
- **Manual theme toggle stored in Profile settings**: deferred — no settings screen/storage exists yet for it, and the user's ask was "light and dark theme," not necessarily a manual switch; following the OS setting is the simpler, standard behavior and can be extended later.
- **Distinct hues per theme for NHG categories**: rejected — would break the "same color means the same thing" invariant relied on by `add-health-trend-visualizations`.
