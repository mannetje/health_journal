## ADDED Requirements

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
The system SHALL NOT include the encrypted database or its key material in cloud backup or device transfer, and the privacy notice SHALL state that data cannot be restored from a backup and that CSV export is the way to move data.

#### Scenario: Backup excludes the encrypted database
- **WHEN** the platform's backup or device transfer runs
- **THEN** neither the encrypted database nor its key material SHALL be included

#### Scenario: Restore on another device
- **WHEN** the app is installed on another device or after data clearing
- **THEN** it starts with an empty database and the notice names CSV export as the way to bring data back
