# privacy Specification

## Purpose
Keeps health data on the device. The app transmits nothing, data leaves only through the user or the backup they control, and nothing sensitive is logged.

## Requirements

### Requirement: The app transmits nothing
The system SHALL keep all health and medication data on the device, SHALL NOT transmit it over a network, and SHALL NOT include analytics, advertising or crash-reporting services.

#### Scenario: No network capability
- **WHEN** the application is built for release
- **THEN** it SHALL NOT request or declare any network capability, and the build SHALL fail if it does

#### Scenario: No third-party data collection
- **WHEN** dependencies are reviewed
- **THEN** none SHALL collect or transmit user data

### Requirement: Data leaves the app only through the user or the platform backup the user controls
The system SHALL only create an export file after an explicit user action and save or share it only to a destination the user chooses. The privacy notice SHALL state that the device's own backup, when the user has it turned on, may include app data, and that CSV export is the way to move data under the user's control.

#### Scenario: Export is user initiated
- **WHEN** the user exports data
- **THEN** the file SHALL be created only after the user's action and SHALL be saved or shared only to a destination the user chooses

#### Scenario: Backup stated in the notice
- **WHEN** the user reads the privacy notice
- **THEN** it states that the platform backup is outside the app's control and names CSV export as the user-controlled copy

### Requirement: No sensitive logging
The system SHALL NOT write medication names, doses or measurements to the device log in release builds.

#### Scenario: Release build logs
- **WHEN** a release build handles an intake
- **THEN** no medication name or dose SHALL appear in the device log

### Requirement: Reminder details hidden on a locked device
The system SHALL hide medication names and doses in notifications shown on a locked device by default, and SHALL offer a setting to show them.

#### Scenario: Lock screen details hidden by default
- **WHEN** a reminder is shown on a locked device with the default setting
- **THEN** it SHALL show only "Medication reminder" and not the medication names or doses

#### Scenario: Details shown by choice
- **WHEN** the user turns on showing details on the lock screen
- **THEN** reminders on a locked device list the medication names and doses
