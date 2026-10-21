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
The system SHALL only create an export or backup file after an explicit user action and save or share it only to a destination the user chooses. The privacy notice SHALL state that the device's own backup, when the user has it turned on, may include app data, that the encrypted backup file and the CSV export are the ways to move data under the user's control, and that the CSV export is not encrypted.

#### Scenario: Export is user initiated
- **WHEN** the user exports data or creates a backup
- **THEN** the file SHALL be created only after the user's action and SHALL be saved or shared only to a destination the user chooses

#### Scenario: Backup stated in the notice
- **WHEN** the user reads the privacy notice
- **THEN** it states that the platform backup is outside the app's control, names the encrypted backup file as the way to keep and restore everything, and says that CSV files are plain text

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

### Requirement: Optional app lock
The system SHALL offer an opt-in setting to lock the app behind the device's own authentication (biometric or device passcode), SHALL store no passcode or secret of its own, and SHALL show no health data while locked.

#### Scenario: Locked on open
- **WHEN** the lock is on and the app is opened or returns from the background after the chosen timeout (immediately, 1 minute or 5 minutes)
- **THEN** the system SHALL show only a locked screen and the authentication prompt, and SHALL show the screens only after successful authentication

#### Scenario: Failed or cancelled authentication
- **WHEN** the user cancels or fails authentication
- **THEN** the app SHALL stay locked and SHALL show no health data

#### Scenario: Device without a screen lock
- **WHEN** the device has no screen lock or enrolled credential
- **THEN** the setting SHALL be unavailable and SHALL explain why

### Requirement: Secure screen
The system SHALL offer an opt-in setting that hides app content in the app switcher and blocks screenshots.

#### Scenario: Secure screen enabled
- **WHEN** the user enables the secure screen setting
- **THEN** the app content SHALL be hidden in the app switcher and screenshots SHALL be blocked

### Requirement: Database encrypted at rest
The system SHALL store all health and medication data in an encrypted database, with a randomly generated key that is stored only wrapped by a non-exportable, hardware-backed platform key store when available, and SHALL NOT require user authentication for that key to be used.

#### Scenario: New installation
- **WHEN** the app is installed and first run
- **THEN** the database SHALL be created encrypted and its file SHALL NOT be readable as plain SQLite

#### Scenario: Migrating an existing plain database
- **WHEN** the app updates and finds a plain database
- **THEN** the system SHALL create the encrypted copy, verify that row counts per table match, and only then remove the plain files

#### Scenario: Migration failure keeps data
- **WHEN** the encrypted copy cannot be created or verification fails
- **THEN** the system SHALL keep the plain database unchanged, show a localized message, and retry on next start

#### Scenario: Biometric change does not lose data
- **WHEN** the user adds or removes a fingerprint or face, or changes the screen lock
- **THEN** the database SHALL remain readable

### Requirement: Encrypted data does not move silently
The system SHALL NOT include the encrypted database or its key material in cloud backup or device transfer. The privacy notice SHALL state that the platform backup does not restore the data and that the user's own encrypted backup file is the way to bring it back.

#### Scenario: Backup excludes the encrypted database
- **WHEN** the platform's backup or device transfer runs
- **THEN** neither the encrypted database nor its key material SHALL be included

#### Scenario: Restore on another device
- **WHEN** the app is installed on another device or after data clearing
- **THEN** it starts with an empty database and the notice names the encrypted backup file as the way to bring data back
