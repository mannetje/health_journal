# Design: Dutch and English Localization

## Context
There is currently no `app/src/main/res` resource directory at all — only `AndroidManifest.xml` under `app/src/main`. All strings live as literals inside composables. NHG category enums (`NhgBmiCategory`, `NhgBloodPressureCategory`, `NhgGlucoseCategory`) are defined in `:domain` as plain enum constants (e.g. `IMPAIRED_FASTING`, `DIABETES_RANGE`) with no display-label mapping — today the `:app` layer must already be turning these into readable text somewhere in the Log/History screens, which is where the display-label mapping is extracted into resources.

## Goals
- Dutch and English support. Default behavior follows the device's system language; when the device language is neither Dutch nor English, the app falls back to **English**.
- The user can override the app's language at any time from **Profile settings**, independent of the device's system language, and the override takes effect immediately (no restart required).
- Every string a user reads — including clinical category names — comes from a string resource, not a literal.
- Keep the mapping from domain enum → string resource in one place per enum (e.g. a small `@Composable fun NhgGlucoseCategory.label(): String` extension using `stringResource`), so it isn't duplicated across Log/History/Trends screens.

## Non-goals
- No support for languages beyond Dutch and English in this change.
- No translation of clinical guideline source documents (NHG standaarden, DVN pages) — only in-app UI strings are localized.
- No pluralization/ICU-message-format complexity beyond Android's built-in `plurals` resource where naturally needed (e.g. "3 entries" vs "1 entry"); no new i18n library.

## Approach
- Default `values/strings.xml` is **English** (the fallback language); `values-nl/strings.xml` is the Dutch translation. This matches Android's usual "default = base `values`, no qualifier" convention and directly encodes the "unknown device language → English" fallback: an unmatched locale naturally resolves to the qualifier-less `values/` set.
- Group string resource names by screen/feature prefix for readability (e.g. `log_tab_weight`, `history_filter_all`, `profile_export_button`), rather than one flat namespace.
- Add label extensions for the three NHG enums (`NhgBmiCategory`, `NhgBloodPressureCategory`, `NhgGlucoseCategory`) and for `GlucoseContext` (fasting/postprandial), each mapping enum constant → string resource, so any screen (including the future Trends charts) can call `category.label()` and get a localized, consistent name instead of re-deriving text from the enum's Kotlin identifier.
- Numbers/units (mmol/L, mmHg, kg) are not translated — they use the existing `BigDecimal`/formatter output as-is; only surrounding labels are localized.
- **Language setting in Profile**: a small segmented control / dropdown with three options — "System default", "English", "Nederlands" — added to `ProfileScreen.kt`. The selection is persisted in a lightweight local preference store (plain `SharedPreferences`, e.g. a `LanguagePreference` wrapper in `:app`; not part of the domain `Profile`, since it is a device/UI setting, not clinical data).
- **Applying the override**: on app start (and immediately on change), if a language override is stored, wrap the base `Context`'s resources with a `Configuration` carrying the chosen `Locale` (`Context.createConfigurationContext`), and provide that context down the Compose tree via `CompositionLocalProvider(LocalContext provides overriddenContext)` at the root of `HealthJournalApp`'s content, so every `stringResource` call downstream resolves against the overridden locale without needing `AppCompatDelegate`/`androidx.appcompat`. If "System default" is selected (or nothing is stored yet), no override is applied and the OS locale resolution described in Goals applies as-is (which itself falls back to `values/` = English for an unmatched system language).
- Changing the setting recreates the Compose content (not necessarily the whole `Activity`) so all currently visible strings re-resolve immediately.

## Alternatives considered
- **Dutch as the default `values/` locale**: rejected once the requirement became "fall back to English for an unrecognized device language" — keeping English as the unqualified default resource set makes that fallback automatic rather than requiring extra fallback logic.
- **`AppCompatDelegate.setApplicationLocales()` (AndroidX per-app language API)**: considered — it's the officially recommended mechanism and handles persistence/recreation for you, but it requires adding `androidx.appcompat` (not currently a dependency; the project only has `androidx.core.ktx`). Rejected for now per the project's dependency-minimization stance (ADR 0003) in favor of a manual `Configuration` override, which needs no new artifact. If `androidx.appcompat` is ever added for other reasons, revisit this.
- **A third-party i18n/localization library**: rejected per the project's dependency-minimization stance — Android's built-in resource qualifiers plus a manual locale override fully cover a two-language, user-switchable requirement.
