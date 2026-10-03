## ADDED Requirements

These requirements are platform-neutral. How each one is met on a platform (permissions, backup flags, key storage, authentication API) is defined in that platform's spec, for example `platform-android`.

### Requirement: Data stays on the device
The system SHALL keep all health and medication data on the device, SHALL NOT transmit it over a network, and SHALL NOT include analytics, advertising or crash-reporting services.

#### Scenario: No network capability
- **WHEN** the application is built for release
- **THEN** it SHALL NOT request or declare any network capability, and the build SHALL fail if it does

#### Scenario: No third-party data collection
- **WHEN** dependencies are reviewed
- **THEN** none SHALL collect or transmit user data

### Requirement: No automatic copies off the device
The system SHALL exclude its database from cloud backup and device-to-device transfer, and SHALL only let data leave the app through an explicit user action (export or share).

#### Scenario: Backup disabled
- **WHEN** the platform's backup or device transfer runs
- **THEN** the health database SHALL NOT be included

#### Scenario: Export is user initiated
- **WHEN** the user exports data
- **THEN** the file SHALL be created only after the user's action and SHALL be saved or shared only to a destination the user chooses

### Requirement: Sensitive details protected on screen
The system SHALL hide medication details in notifications shown on a locked device by default, and SHALL offer an opt-in setting that hides app content in the app switcher and blocks screenshots.

#### Scenario: Secure screen enabled
- **WHEN** the user enables the secure screen setting
- **THEN** the app content SHALL be hidden in the app switcher and screenshots SHALL be blocked

### Requirement: No sensitive logging
The system SHALL NOT write medication names, doses or measurements to the device log in release builds.

#### Scenario: Release build logs
- **WHEN** a release build handles an intake
- **THEN** no medication name or dose SHALL appear in the device log

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

#### Scenario: Reminder tap while locked
- **WHEN** the user taps a reminder while the lock is on
- **THEN** the system SHALL require authentication before showing Today

#### Scenario: Taken action does not open the app
- **WHEN** the user marks an intake as taken from the reminder itself
- **THEN** the system SHALL record the outcome without showing any data, and the setting description SHALL state this

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

#### Scenario: Data cannot move silently
- **WHEN** the app is restored on another device or after data clearing
- **THEN** no encrypted database SHALL be restored from a backup, and the privacy notice SHALL state that CSV export is the way to move data
