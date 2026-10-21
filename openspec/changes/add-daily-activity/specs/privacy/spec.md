## ADDED Requirements

### Requirement: Daily activity data is protected like all entries
Daily activity entries, including numbers, type and comment, SHALL be stored only in the encrypted database, SHALL NOT be written to any log, notification, preference file or the Android backup, and SHALL leave the device only through the user's own export or encrypted backup. The metric SHALL NOT request any new Android permission (in particular no activity recognition or body sensor permission), SHALL NOT use Health Connect or any sensor, and SHALL NOT use the network. Screens follow the secure-screen setting.

#### Scenario: No log output
- **WHEN** an entry is saved, edited or deleted in a release build
- **THEN** nothing about it appears in the device log

#### Scenario: No new permission
- **WHEN** the merged manifest is checked
- **THEN** it declares no permission beyond those it had before this change and no INTERNET permission

#### Scenario: Stored encrypted
- **WHEN** the database file is read without the key
- **THEN** the activity table content is not readable
