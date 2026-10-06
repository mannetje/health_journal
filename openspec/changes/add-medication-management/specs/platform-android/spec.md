## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Local storage with Room
The app SHALL persist data offline in a SQLite database through Room, with the database at schema version 6. Each step SHALL be a real migration that keeps all rows: 1 to 2 adds the optional sex column to the profile table, 2 to 3 adds the waist circumference table, 3 to 4 adds the optional pulse column to blood pressure, 4 to 5 adds the optional comment column to the weight, blood pressure, glucose, waist circumference and activity tables, and 5 to 6 adds the medication, medication schedule, medication time and intake tables. Storage SHALL always be metric.

#### Scenario: Upgrade keeps data
- **WHEN** a device with a version 1, 2, 3, 4 or 5 database installs a build that has version 6
- **THEN** all profiles and entries SHALL still be present, the sex of each profile, the pulse of each earlier blood pressure and the comment of every earlier entry SHALL be unset, the waist circumference history SHALL be empty when upgrading from version 1 or 2, and the medication tables SHALL be empty (ADR 0004)
