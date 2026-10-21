## ADDED Requirements

### Requirement: Daily activity in the backup document
Each profile in the backup document SHALL hold an `activityDays` list with every activity entry (date as an ISO local date, steps or null, active minutes or null, activity type code or null, comment), using the same stable type codes as the CSV export. `schemaVersion` SHALL stay 1: a file without the list restores with no activity entries and readers that do not know it ignore it. A restored list with two entries for one date SHALL keep the later one in the list and report the other as skipped. `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors and the recovery page) SHALL describe the list.

#### Scenario: Everything is in the file
- **WHEN** a profile with two activity entries is backed up and restored into an empty app
- **THEN** both are present and identical, including numbers, type and comment

#### Scenario: Older file
- **WHEN** a backup made before this change is restored
- **THEN** it restores as before and the profile has no activity entries

#### Scenario: Published format
- **WHEN** the documentation is checked
- **THEN** the JSON schema and the Python recovery script include `activityDays` and the test vectors contain one entry
