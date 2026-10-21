## ADDED Requirements

### Requirement: Sleep entries in the backup document
Each profile in the backup document SHALL hold a `sleepLogs` list with every entry (bed time, wake time, quality or null, times woken or null, comment), with times as ISO-8601 UTC timestamps like the CSV export. `schemaVersion` SHALL stay 1: a file without the list restores with no sleep entries and readers that do not know it ignore it. `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors and the recovery page) SHALL describe the list.

#### Scenario: Everything is in the file
- **WHEN** a profile with two sleep entries is backed up and restored into an empty app
- **THEN** both are present and identical, including quality, times woken and comment

#### Scenario: Older file
- **WHEN** a backup made before this change is restored
- **THEN** it restores as before and the profile has no sleep entries

#### Scenario: Published format
- **WHEN** the documentation is checked
- **THEN** the JSON schema and the Python recovery script include `sleepLogs` and the test vectors contain one entry
