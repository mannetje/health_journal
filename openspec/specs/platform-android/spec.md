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
The app SHALL persist data offline in a SQLite database through Room, with the database at schema version 6. Each step SHALL be a real migration that keeps all rows: 1 to 2 adds the optional sex column to the profile table, 2 to 3 adds the waist circumference table, 3 to 4 adds the optional pulse column to blood pressure, 4 to 5 adds the optional comment column to the weight, blood pressure, glucose, waist circumference and activity tables, and 5 to 6 adds the medication, medication schedule, medication time and intake tables. Storage SHALL always be metric.

#### Scenario: Upgrade keeps data
- **WHEN** a device with a version 1, 2, 3, 4 or 5 database installs a build that has version 6
- **THEN** all profiles and entries SHALL still be present, the sex of each profile, the pulse of each earlier blood pressure and the comment of every earlier entry SHALL be unset, the waist circumference history SHALL be empty when upgrading from version 1 or 2, and the medication tables SHALL be empty (ADR 0004)

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

#### Scenario: Switching tabs clears banners
- **WHEN** a success, info or error banner is showing and the user switches to another tab
- **THEN** the banners of all screens are cleared; a banner is not carried to another screen

### Requirement: Screens fit narrow phones and Dutch text
Segmented buttons, tab rows and filter chips SHALL fit a phone width of about 360 dp in Dutch without overlap or broken words, using scrolling rows or wrapping instead of truncation (ADR 0010).

#### Scenario: Dutch on a narrow phone
- **WHEN** the language is Dutch and the screen is 360 dp wide
- **THEN** no label overlaps or breaks inside a word

### Requirement: Theme follows the system
The app SHALL use the DayNight theme with Compose colour schemes that follow the system light or dark setting unless the user picked Light or Dark in Profile ([theming](../theming/spec.md)), and the launcher icon SHALL be an adaptive icon with the navy background and recoloured logo defined in [app-identity](../app-identity/spec.md) (ADR 0011).

#### Scenario: Launcher icon
- **WHEN** the app is installed
- **THEN** the launcher shows the adaptive icon with the navy background and recoloured logo

### Requirement: Charts
Trend charts SHALL be drawn with Vico 3, with the calendar axis and zoom rules from [health-trends](../health-trends/spec.md).

#### Scenario: Chart library
- **WHEN** a trend chart is shown
- **THEN** it is drawn with Vico 3 and follows the axis and zoom rules of health-trends

### Requirement: Distribution and signing
Releases SHALL be debug-signed APKs built by GitHub Actions on a `v*` tag and attached to a GitHub Release. Local and CI builds SHALL be signed with the committed `app/debug.keystore`, so a new APK installs over an older one and keeps user data (ADR 0008). Installing on a phone with an APK signed by a different key requires an uninstall first, which erases the data.

#### Scenario: Update over an existing install
- **WHEN** a device has version N installed and the user installs version N+1 built by this project
- **THEN** the install SHALL succeed as an update and all entries SHALL remain

### Requirement: Versioning
`versionName` SHALL follow `MAJOR.MINOR.PATCH`, `versionCode` SHALL increase with every release, and each release SHALL be tagged `vMAJOR.MINOR.PATCH` and described in `CHANGELOG.md`.

#### Scenario: Release tag
- **WHEN** version 1.4.9 is released
- **THEN** `versionName` is 1.4.9, `versionCode` is higher than the previous release, the tag is v1.4.9 and `CHANGELOG.md` describes it

### Requirement: No network permission
The manifest SHALL NOT declare `INTERNET` or any other network permission. A build check SHALL fail if `INTERNET` appears in the merged manifest.

#### Scenario: Merged manifest check
- **WHEN** a dependency adds the INTERNET permission to the merged manifest
- **THEN** the build fails

### Requirement: No sensitive logging in release builds
Release builds SHALL NOT log medication names, doses or measurement values through `android.util.Log` or any other logger.

#### Scenario: Release logging check
- **WHEN** the release build is checked
- **THEN** no logging call includes a medication name or dose

### Requirement: Pill button in the top bar
The top app bar SHALL show a pill button (emoji icon, with a content description in the app language) on every main tab, SHALL open the pillbox screen on top of the current tab, and SHALL handle the system back action by returning to that tab.

#### Scenario: Back from the pillbox
- **WHEN** the user presses the system back button on the pillbox screen
- **THEN** the pillbox closes and the previous tab is shown with its state

### Requirement: Reminders on AlarmManager
The Android app SHALL schedule only the next reminder slot with `AlarmManager`, using an exact alarm when the exact-alarm permission is granted and an inexact alarm with a hint otherwise, and SHALL re-arm after boot, app update, time change, time-zone change and any schedule edit through broadcast receivers. Notification actions Taken all and Snooze SHALL be handled by a `BroadcastReceiver` without opening the Activity. The notification identity SHALL be derived from the slot time so a re-arm never posts a second notification for the same slot.

#### Scenario: Re-armed after reboot
- **WHEN** the device restarts
- **THEN** the boot receiver re-arms the next reminder for the active profile

#### Scenario: Exact alarm not granted
- **WHEN** the exact-alarm permission is not granted
- **THEN** reminders are inexact and the app shows a hint on how to allow exact alarms

#### Scenario: Re-arm does not duplicate
- **WHEN** the app re-arms while a notification for the same slot is already shown
- **THEN** the existing notification is updated and no second one appears

### Requirement: Reminder permissions
The manifest SHALL declare `POST_NOTIFICATIONS` (requested at first reminder setup on Android 13 and higher, with no request on older versions), `RECEIVE_BOOT_COMPLETED`, and the exact-alarm permission as an optional grant, and SHALL continue to declare no network permission.

#### Scenario: Notification permission on older Android
- **WHEN** the device runs Android 12 or lower
- **THEN** no notification permission prompt is shown

### Requirement: Notification text uses the app locale
Notification text and action labels SHALL be built from a context wrapped with the app locale (`withAppLocale`), not the device language, because receivers run without the Activity.

#### Scenario: Receiver builds Dutch text
- **WHEN** a receiver builds a notification while the app language is Dutch
- **THEN** all its text comes from the Dutch resources
