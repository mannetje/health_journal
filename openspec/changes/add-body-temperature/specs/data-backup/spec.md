## ADDED Requirements

### Requirement: Body temperature in the backup document
Each profile in the backup document SHALL hold a `bodyTemperatures` list with every reading (timestamp, temperature in tenths of a degree Celsius as an integer, site code or null, comment), using the same stable site codes as the CSV export. `schemaVersion` SHALL stay 1: a file without the list restores with no readings and readers that do not know it ignore it. `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors and the recovery page) SHALL describe the list.

#### Scenario: Everything is in the file
- **WHEN** a profile with two readings is backed up and restored into an empty app
- **THEN** both are present and identical, including site and comment

#### Scenario: Older file
- **WHEN** a backup made before this change is restored
- **THEN** it restores as before and the profile has no temperature readings

#### Scenario: Published format
- **WHEN** the documentation is checked
- **THEN** the JSON schema and the Python recovery script include `bodyTemperatures` and the test vectors contain one reading
