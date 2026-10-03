# Add Dutch and English Localization

## Why
Every user-facing string in the app today is a hard-coded literal inside a Compose composable (~60 `Text("...")` calls across `LogMetricScreen.kt`, `HistoryScreen.kt`, and `ProfileScreen.kt`) — there is no `res/values/strings.xml` at all. The app must support both Dutch and English, matching the target audience (Dutch health guidance, NHG terminology) while remaining usable for English-speaking users.

## What Changes
- Introduce Android's standard string-resource mechanism: `app/src/main/res/values/strings.xml` (default — **English**, since English is the fallback when the device/app language is unknown) and `app/src/main/res/values-nl/strings.xml` (Dutch translations), with every composable reading strings via `stringResource(R.string.xxx)` instead of literals.
- Extract every hard-coded string in `LogMetricScreen.kt`, `HistoryScreen.kt`, `ProfileScreen.kt` (labels, button text, dialog text, error/empty-state messages, tab/filter names) into named string resources.
- NHG/clinical category labels (e.g. blood pressure category names, BMI category names, glucose category names) also become string resources so they display in the user's chosen language, not just the numbers/colors.
- **Add a language setting in the Profile screen**: a Dutch/English selector (defaulting to "follow system", with English as the fallback when the system language is neither Dutch nor English) that the user can override at any time. The chosen language is persisted locally and applied immediately across the whole app, not just on next launch.
- No new external dependency: the language switch is implemented via a per-app locale override (a manually applied `Configuration`/resources override, or `AppCompatDelegate`'s per-app language API if the `androidx.appcompat` artifact is already pulled in transitively — see `design.md` for the exact choice) plus a small local preference store; no third-party i18n/localization library is introduced.

## Capabilities
- **Added Capability:** `localization/dutch-english`

## Impact
- Affected code: every composable file under `app/src/main/java/nl/healthjournal/app/ui/**`, new `app/src/main/res/values/strings.xml` and `app/src/main/res/values-nl/strings.xml`, `ProfileScreen.kt`/`ProfileViewModel.kt` (new language setting UI + persistence), and a new small `LanguagePreference`/locale-override helper in `:app`.
- No domain/data module changes — language preference is an app/device-level UI setting, not a clinical `Profile` field, so it is stored separately from the domain `Profile` model via a lightweight local preference store in `:app`.
- `add-health-trend-visualizations`' new Trends composables should be written against string resources from the start rather than adding new hard-coded literals that would need retrofitting.

## Status
Implemented. The rules are in the canonical specs under `openspec/specs/`.
