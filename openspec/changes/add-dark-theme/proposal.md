# Add Dark Theme Support

## Why
`HealthJournalTheme` (`app/src/main/java/nl/healthjournal/app/ui/theme/Theme.kt`) currently hard-codes a single `lightColorScheme` and ignores the device's dark-mode setting entirely. This was flagged while designing `add-health-trend-visualizations`, whose reference apps (MyHeart) are dark-themed and whose chart colors need a defined behavior in both light and dark mode — but the gap applies to the whole app, not just the new Trends screens, so it is tracked as its own change.

## What Changes
- Add a `DarkColorScheme` alongside the existing `LightColorScheme` in `Theme.kt`, and switch between them based on `isSystemInDarkTheme()` (with no in-app override for this change — see Non-goals in `design.md`).
- Re-derive `Color.kt` so every color used in a `MaterialTheme` slot (`background`, `surface`, `onBackground`, `onSurface`, primary/secondary and their `on*` counterparts) has both a light and dark value; keep the NHG category status colors (`NhgOptimalGreen`, `NhgWarningYellow`, etc.) but verify/adjust each for sufficient contrast against a dark surface.
- Audit `LogMetricScreen.kt`, `HistoryScreen.kt`, and `ProfileScreen.kt` for any hard-coded `Color(0x...)` literals that bypass `MaterialTheme.colorScheme` (there are a few status/badge colors) and route them through theme-aware tokens where a dark-mode equivalent is needed.
- No new dependency: this uses Compose Material 3's built-in `darkColorScheme()`/`lightColorScheme()` and `isSystemInDarkTheme()`, already part of the existing Compose BOM.

## Capabilities
- **Added Capability:** `theming/dark-mode`

## Impact
- Affected code: `app/src/main/java/nl/healthjournal/app/ui/theme/Theme.kt`, `Color.kt`, plus any screen composable found to hard-code a non-theme-aware color during the audit.
- No domain/data module changes.
- This is a prerequisite for `add-health-trend-visualizations`' shared building blocks (`TrendCard`, `LineTrendChart`, etc.), which must render correctly in both themes; that change's design should reference this one rather than duplicate theme decisions.
