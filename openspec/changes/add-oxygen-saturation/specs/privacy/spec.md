## ADDED Requirements

### Requirement: Oxygen saturation data is protected like all entries
Oxygen saturation readings, including pulse, context, site, the oxygen-given tick and comment, SHALL be stored only in the encrypted database, SHALL NOT be written to any log, notification, preference file or the Android backup, and SHALL leave the device only through the user's own export or encrypted backup. Screens follow the secure-screen setting.

#### Scenario: No log output
- **WHEN** a reading is saved, edited or deleted in a release build
- **THEN** nothing about it appears in the device log

#### Scenario: Stored encrypted
- **WHEN** the database file is read without the key
- **THEN** the oxygen table content is not readable
