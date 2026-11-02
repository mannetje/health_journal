## ADDED Requirements

### Requirement: A failed swap is undone
When setting the plain database files aside or moving the encrypted database into place fails, the system SHALL put back every file it already set aside before reporting the failure, and SHALL NOT create a new database while the original is still set aside.

#### Scenario: A rename fails partway through the swap
- **WHEN** the main database file has been set aside and renaming its `-wal` file, or moving the encrypted file in, fails
- **THEN** all set-aside files SHALL be restored, the plain database SHALL be unchanged, and the next start SHALL migrate again

#### Scenario: The original cannot be restored
- **WHEN** the database file is missing and its set-aside copy exists at startup
- **THEN** the system SHALL NOT create a new database file, SHALL start with an empty in-memory database and the "not encrypted" notice, and SHALL leave the set-aside copy for the next start
