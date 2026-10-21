## MODIFIED Requirements

### Requirement: Data leaves the app only through the user or the platform backup the user controls
The system SHALL only create an export or backup file after an explicit user action and save or share it only to a destination the user chooses. The privacy notice SHALL state that the device's own backup, when the user has it turned on, may include app data, that the encrypted backup file and the CSV export are the ways to move data under the user's control, and that the CSV export is not encrypted.

#### Scenario: Export is user initiated
- **WHEN** the user exports data or creates a backup
- **THEN** the file SHALL be created only after the user's action and SHALL be saved or shared only to a destination the user chooses

#### Scenario: Backup stated in the notice
- **WHEN** the user reads the privacy notice
- **THEN** it states that the platform backup is outside the app's control, names the encrypted backup file as the way to keep and restore everything, and says that CSV files are plain text

### Requirement: Encrypted data does not move silently
The system SHALL NOT include the encrypted database or its key material in cloud backup or device transfer. The privacy notice SHALL state that the platform backup does not restore the data and that the user's own encrypted backup file is the way to bring it back.

#### Scenario: Backup excludes the encrypted database
- **WHEN** the platform's backup or device transfer runs
- **THEN** neither the encrypted database nor its key material SHALL be included

#### Scenario: Restore on another device
- **WHEN** the app is installed on another device or after data clearing
- **THEN** it starts with an empty database and the notice names the encrypted backup file as the way to bring data back
