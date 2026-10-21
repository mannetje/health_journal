## ADDED Requirements

### Requirement: Fluid intake in the backup document
Each profile in the backup document SHALL hold a `fluidEntries` list with every entry (timestamp, ml, comment or null) and a `fluidDailyLimitMl` value (whole number or null) holding the person's own limit. `schemaVersion` SHALL stay 1: a file without them restores with no entries and no limit, and readers that do not know them ignore them. The quick-add sizes are preferences and are not part of the list. `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors and the recovery page) SHALL describe the list.

#### Scenario: Everything is in the file
- **WHEN** a profile with two entries and a limit of 1500 is backed up and restored into an empty app
- **THEN** both entries are present and identical, including the comment, and the limit is 1500

#### Scenario: No limit set
- **WHEN** a profile without a limit is backed up
- **THEN** `fluidDailyLimitMl` is null and restoring leaves the limit empty

#### Scenario: Older file
- **WHEN** a backup made before this change is restored
- **THEN** it restores as before and the profile has no fluid entries and no limit

#### Scenario: Published format
- **WHEN** the documentation is checked
- **THEN** the JSON schema and the Python recovery script include `fluidEntries` and `fluidDailyLimitMl` and the test vectors contain one entry
