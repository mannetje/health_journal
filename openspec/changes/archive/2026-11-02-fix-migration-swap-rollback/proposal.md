## Why

A code review found a data-loss path in the plain-to-encrypted database migration. When the plain database is set aside file by file and a later rename fails (for example the `-wal` file after the main file was already moved), the function threw without putting anything back. The caller then saw no database file and Room created a new empty one. At the next start, recovery found an encrypted-looking database next to the set-aside original, took that for a finished migration, and deleted the original.

## What Changes

- **`PlainToEncryptedMigration`**: any failure while setting the plain files aside or moving the encrypted file in now puts back every file already set aside before the failure is reported, so the plain database is where it was and the next start retries.
- **`HealthJournalDatabase.create()`**: when the database file is missing but its set-aside copy exists (a restore that itself failed), no new database is created. The app starts with an empty in-memory database and the "not encrypted" notice, and the original stays set aside for the next start.
- Fault-injection tests fail each rename in turn, check the plain files are back with their data, and check the next migration succeeds.
- No database schema change, no Room migration, no change for users whose migration already succeeded.

## Capabilities

### Modified Capabilities

- `privacy`: a failed swap is undone, and a missing database with a set-aside copy is never replaced by a new one.

## Impact

- `data/src/main/java/nl/healthjournal/data/local/PlainToEncryptedMigration.kt` and `HealthJournalDatabase.kt`.
- `data/src/test/java/nl/healthjournal/data/local/PlainToEncryptedMigrationTest.kt`.
- `CHANGELOG.md`, `docs/dev/privacy-and-security.md`.
