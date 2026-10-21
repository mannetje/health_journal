## ADDED Requirements

### Requirement: Fluid intake data is protected like all entries
Fluid intake entries, including amounts, timestamps and comments, and the person's personal limit SHALL be stored only in the encrypted database, SHALL NOT be written to any log, notification, preference file or the Android backup, and SHALL leave the device only through the user's own export or encrypted backup. The preference file MAY hold only the three quick-add sizes and short names, which are settings and not entries; it SHALL NOT hold any amount drunk, any daily total, the personal limit or any timestamp of an entry. Screens follow the secure-screen setting.

#### Scenario: No log output
- **WHEN** an entry is added, undone, edited or deleted in a release build
- **THEN** nothing about it appears in the device log

#### Scenario: Stored encrypted
- **WHEN** the database file is read without the key
- **THEN** the fluid table content is not readable

#### Scenario: Preferences hold no entries
- **WHEN** the preference file is inspected after several quick-adds
- **THEN** it contains the sizes and names only
