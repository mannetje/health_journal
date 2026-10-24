## ADDED Requirements

### Requirement: Migraine data is protected like all entries
Migraine entries, including the medication text, personal tags and comment, SHALL be stored only in the encrypted database, SHALL NOT be written to any log, notification, preference file, crash text or the Android backup, and SHALL leave the device only through the user's own export or encrypted backup. A migraine screen SHALL follow the secure-screen setting.

#### Scenario: No log output
- **WHEN** a migraine is saved, edited or deleted in a release build
- **THEN** nothing about it appears in the device log

#### Scenario: Stored encrypted
- **WHEN** the database file is read without the key
- **THEN** the migraine table content is not readable

#### Scenario: Secure screen
- **WHEN** the secure screen setting is on and the migraine screen is open
- **THEN** screenshots and the recent-apps preview are blocked as elsewhere
