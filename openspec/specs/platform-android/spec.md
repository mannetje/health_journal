# platform-android Specification

## Purpose
Android-specific decisions for this project. Everything here is a platform choice, not a business rule: the neutral specs in the other folders stay valid for any platform. A port (for example to iPhone) replaces this spec with its own `platform-<name>` spec and keeps the rest.

## Requirements

### Requirement: Platform baseline
The Android app SHALL build with `minSdk` 26, `targetSdk` 35 and `compileSdk` 36, use the application id `nl.healthjournal`, and be written in Kotlin with Jetpack Compose (Material 3) for the UI.

#### Scenario: Older device
- **WHEN** a device runs Android 8.0 (API 26) or 8.1 (API 27)
- **THEN** the app SHALL run, and the measurement system SHALL be resolved from a fixed list of imperial regions instead of the ICU API that needs API 28 ([units-presentation](../units-presentation/spec.md), ADR 0014)

### Requirement: Module layout follows the hexagonal rule
The project SHALL consist of three Gradle modules: `domain` (pure Kotlin, no Android dependency), `data` (Room, CSV adapters, repositories) and `app` (Compose UI, ViewModels, preferences). Dependencies SHALL point inwards: `app` and `data` depend on `domain`, and `domain` depends on neither.

#### Scenario: Domain stays portable
- **WHEN** the `domain` module is compiled
- **THEN** it SHALL compile without any Android or third-party UI or persistence library (ADR 0002, ADR 0003)

### Requirement: Local storage with Room
The app SHALL persist data offline in a SQLite database through Room, with the database at schema version 2. Version 1 to 2 SHALL be a real migration that adds the optional sex column to the profile table without losing rows. Storage SHALL always be metric.

#### Scenario: Upgrade keeps data
- **WHEN** a device with a version 1 database installs a build that has version 2
- **THEN** all profiles and entries SHALL still be present and the sex of each profile SHALL be unset (ADR 0004)

### Requirement: Preferences in SharedPreferences
Language, region, unit system and glucose unit choices SHALL be stored in private SharedPreferences, not in the health database, so they survive a database reset and never appear in CSV files.

#### Scenario: Preference change
- **WHEN** the user changes language or region
- **THEN** the Activity SHALL recreate so every window resolves the new locale
- **WHEN** the user changes the unit system or glucose unit
- **THEN** the UI SHALL recompose without recreating the Activity (ADR 0013, ADR 0014)

### Requirement: App language on the base context
The app language SHALL be applied by wrapping the Activity base context (`attachBaseContext`) with the chosen locale, so dialogs, pickers and popups resolve the same strings as the main screen.

#### Scenario: Activity result launchers
- **WHEN** the History screen opens the system file picker for CSV import
- **THEN** it SHALL work without a custom `ActivityResultRegistryOwner`, because the context chain still contains a real Activity (ADR 0009, ADR 0013)

### Requirement: Messages are string resources
ViewModels SHALL NOT hold translated text. They SHALL expose a `UiText` value, either a string resource id with arguments or final plain text, and the screen SHALL resolve it against the current locale. Text that comes from the domain layer (validation messages) stays English and is wrapped as plain text.

#### Scenario: Language changes while a banner is visible
- **WHEN** a success banner is showing and the language is switched
- **THEN** the banner SHALL show in the new language (ADR 0015)

### Requirement: Screens fit narrow phones and Dutch text
Segmented buttons, tab rows and filter chips SHALL fit a phone width of about 360 dp in Dutch without overlap or broken words, using scrolling rows or wrapping instead of truncation (ADR 0010).

### Requirement: Theme follows the system
The app SHALL use the DayNight theme with Compose colour schemes that follow the system light or dark setting, and the launcher icon SHALL be an adaptive icon with the navy background and recoloured logo defined in [app-identity](../app-identity/spec.md) (ADR 0011).

### Requirement: Charts
Trend charts SHALL be drawn with Vico 3, with the calendar axis and zoom rules from [health-trends](../health-trends/spec.md).

### Requirement: Distribution and signing
Releases SHALL be debug-signed APKs built by GitHub Actions on a `v*` tag and attached to a GitHub Release. Local and CI builds SHALL be signed with the committed `app/debug.keystore`, so a new APK installs over an older one and keeps user data (ADR 0008). Installing on a phone with an APK signed by a different key requires an uninstall first, which erases the data.

#### Scenario: Update over an existing install
- **WHEN** a device has version N installed and the user installs version N+1 built by this project
- **THEN** the install SHALL succeed as an update and all entries SHALL remain

### Requirement: Versioning
`versionName` SHALL follow `MAJOR.MINOR.PATCH`, `versionCode` SHALL increase with every release, and each release SHALL be tagged `vMAJOR.MINOR.PATCH` and described in `CHANGELOG.md`.
